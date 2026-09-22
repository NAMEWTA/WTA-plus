package org.namewta.system.oss.upload;

import lombok.RequiredArgsConstructor;
import org.namewta.common.oss.model.OssCompletedPart;
import org.namewta.common.oss.model.OssMultipartPart;
import org.namewta.common.oss.model.OssObjectStat;
import org.namewta.common.oss.model.OssPresignedRequest;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import static org.namewta.system.oss.upload.OssUploadContracts.*;

/**
 * 浏览器直传控制面。文件字节只在 magic 校验时读取有限前缀。
 */
@Service
@RequiredArgsConstructor
public class OssUploadService {

    private static final Pattern CONTENT_TYPE = Pattern.compile("[a-z0-9.+-]+/[a-z0-9.+-]+");
    private static final int MAGIC_PREFIX_LENGTH = 16;
    private static final Map<String, byte[]> MAGIC_PREFIXES = Map.of(
        "image/png", hex("89504e470d0a1a0a"),
        "image/jpeg", hex("ffd8ff"),
        "image/gif", "GIF8".getBytes(StandardCharsets.US_ASCII),
        "application/pdf", "%PDF-".getBytes(StandardCharsets.US_ASCII),
        "application/zip", hex("504b0304"),
        "audio/ogg", "OggS".getBytes(StandardCharsets.US_ASCII),
        "video/ogg", "OggS".getBytes(StandardCharsets.US_ASCII),
        "audio/webm", hex("1a45dfa3"),
        "video/webm", hex("1a45dfa3")
    );

    private final OssUploadProperties properties;
    private final OssUploadIdentityResolver identityResolver;
    private final OssUploadTicketStore ticketStore;
    private final OssUploadObjectStore objectStore;
    private final OssUploadMetadataStore metadataStore;
    private final OssStorageReadinessRegistry readinessRegistry;
    private final OssDefaultStorageKey defaultStorageKey;

    public InitResponse init(InitRequest request) {
        validateInitRequest(request);
        OssUploadProperties.Policy policy = properties.requirePolicy(request.policy());
        OssUploadIdentityResolver.Identity identity = identityResolver.resolve();
        authorizePolicy(policy, identity);
        String contentType = normalizeContentType(request.contentType());
        if (request.fileSize() > policy.getMaxSize() || !policy.allowsContentType(contentType)) {
            throw new OssUploadException(OssUploadError.INVALID_FILE, "文件大小或 Content-Type 不符合上传策略");
        }
        String storageConfigKey = currentDefaultStorageKey();
        requireStorageRoute(policy, storageConfigKey);
        OssUploadMode mode = policy.resolveMode(request.fileSize());
        long partSize = mode == OssUploadMode.MULTIPART ? policy.getPartSize() : 0;
        int partCount = mode == OssUploadMode.MULTIPART
            ? Math.toIntExact(ceilDiv(request.fileSize(), partSize)) : 0;
        String token = UUID.randomUUID().toString();
        String fingerprintDigest = sha256(request.fingerprint());
        OssUploadObjectStore.PreparedUpload prepared;
        try {
            prepared = objectStore.prepare(storageConfigKey, policy.getExpectedAccessPolicy(),
                policy.getObjectPrefix(), request.fileName(), contentType, fingerprintDigest, mode,
                properties.getPresignTtl());
        } catch (OssUploadException e) {
            throw e;
        } catch (RuntimeException e) {
            throw providerFailure("初始化 OSS 上传失败", e);
        }
        long now = System.currentTimeMillis();
        long expiresAt = now + properties.getTicketTtl().toMillis();
        OssUploadTicket ticket = new OssUploadTicket(token, request.policy(), mode, OssUploadState.INITIALIZED,
            prepared.service(), prepared.bucket(), prepared.objectKey(), prepared.uploadId(), request.fileName(),
            suffix(request.fileName()), request.fileSize(), contentType, request.fingerprint(), fingerprintDigest,
            identity.userId(), identity.clientPk(), partSize, partCount, now, expiresAt, null);
        OssUploadCleanupRecord cleanup = cleanupRecord(ticket);
        try {
            ticketStore.create(ticket, cleanup, properties.getTicketTtl(), properties.getCleanupRecordTtl());
        } catch (RuntimeException e) {
            cleanupPrepared(cleanup);
            throw stateFailure("保存 UploadTicket 失败", e);
        }
        return new InitResponse(token, mode, Instant.ofEpochMilli(expiresAt), prepared.presignedRequest(),
            mode == OssUploadMode.MULTIPART ? partSize : null,
            mode == OssUploadMode.MULTIPART ? partCount : null);
    }

    private String currentDefaultStorageKey() {
        String storageConfigKey = defaultStorageKey.current();
        if (storageConfigKey == null || storageConfigKey.isBlank()) {
            throw new OssUploadException(OssUploadError.STORAGE_NOT_SERVING, "默认 OSS 配置不存在");
        }
        return storageConfigKey;
    }

    private void requireStorageRoute(OssUploadProperties.Policy policy, String storageConfigKey) {
        OssStorageReadinessEntry entry = readinessRegistry.snapshot().get(storageConfigKey);
        if (entry == null || entry.status() != OssStorageReadinessEntry.Status.SERVING) {
            throw new OssUploadException(OssUploadError.STORAGE_NOT_SERVING, "OSS 上传目标当前不可服务");
        }
        if (entry.accessPolicy() != policy.getExpectedAccessPolicy()) {
            throw new OssUploadException(OssUploadError.STORAGE_ACCESS_POLICY_MISMATCH,
                "OSS 上传目标访问策略与服务端策略不一致");
        }
    }

    public SignPartsResponse signParts(String token, SignPartsRequest request) {
        return locked(token, () -> {
            OssUploadTicket ticket = requireOwned(token);
            requireMultipart(ticket);
            if (request == null || request.partNumbers() == null || request.partNumbers().isEmpty()
                || request.partNumbers().size() > properties.getMaxSignParts()) {
                throw new OssUploadException(OssUploadError.INVALID_PARTS, "Part 签名窗口无效");
            }
            List<Integer> numbers = request.partNumbers().stream().distinct().sorted().toList();
            if (numbers.size() != request.partNumbers().size()
                || numbers.stream().anyMatch(number -> number == null || number < 1 || number > ticket.partCount())) {
                throw new OssUploadException(OssUploadError.INVALID_PARTS, "Part 编号无效或重复");
            }
            try {
                saveTicket(ticket.withState(OssUploadState.UPLOADING));
                return new SignPartsResponse(objectStore.signParts(ticket, numbers, properties.getPresignTtl()));
            } catch (OssUploadException e) {
                throw e;
            } catch (RuntimeException e) {
                throw providerFailure("生成 Part 签名失败", e);
            }
        });
    }

    public ResumeResponse resume(String token, String fingerprint) {
        OssUploadTicket ticket = requireOwned(token);
        if (!Objects.equals(ticket.fingerprint(), fingerprint)) {
            throw new OssUploadException(OssUploadError.FINGERPRINT_MISMATCH, "文件指纹不匹配");
        }
        if (ticket.state() == OssUploadState.COMPLETED && ticket.ossId() != null) {
            return new ResumeResponse(token, ticket.mode(), ticket.state(), ticket.ossId().toString(),
                ticket.originalName(), ticket.fileSize(), ticket.contentType(), ticket.partSize(), ticket.partCount(),
                Instant.ofEpochMilli(ticket.expiresAt()), null, List.of());
        }
        List<UploadedPart> uploaded = List.of();
        if (ticket.mode() == OssUploadMode.MULTIPART) {
            try {
                uploaded = objectStore.listParts(ticket).stream()
                    .map(part -> new UploadedPart(part.partNumber(), part.eTag(), part.size())).toList();
            } catch (RuntimeException e) {
                throw providerFailure("查询已上传 Part 失败", e);
            }
        }
        OssPresignedRequest presignedRequest = ticket.mode() == OssUploadMode.SINGLE
            ? objectStore.presignSingle(ticket, properties.getPresignTtl()) : null;
        return new ResumeResponse(token, ticket.mode(), ticket.state(), null, ticket.originalName(), ticket.fileSize(),
            ticket.contentType(), ticket.partSize(), ticket.partCount(), Instant.ofEpochMilli(ticket.expiresAt()),
            presignedRequest, uploaded);
    }

    public String complete(String token, CompleteRequest request) {
        return locked(token, () -> completeLocked(token, request == null ? new CompleteRequest(List.of()) : request));
    }

    public void abort(String token) {
        locked(token, () -> {
            OssUploadTicket ticket = ticketStore.get(token);
            if (ticket == null) {
                return null;
            }
            requireOwner(ticket);
            if (ticket.state() == OssUploadState.COMPLETED) {
                ticketStore.removeCompletedCleanup(token);
                return null;
            }
            cleanupSession(ticket, true);
            return null;
        });
    }

    public boolean cleanupExpired(String token, boolean dryRun) {
        return locked(token, () -> {
            OssUploadCleanupRecord cleanup = ticketStore.getCleanup(token);
            if (cleanup == null || cleanup.expiresAt() > System.currentTimeMillis()) {
                return false;
            }
            OssUploadTicket ticket = ticketStore.get(token);
            if (ticket != null && ticket.state() == OssUploadState.COMPLETED) {
                ticketStore.removeCompletedCleanup(token);
                return false;
            }
            if (metadataStore.findByObject(cleanup.service(), cleanup.objectKey()) != null) {
                ticketStore.removeSession(token);
                return false;
            }
            if (dryRun) {
                return true;
            }
            try {
                objectStore.abort(cleanup);
                objectStore.deleteObject(cleanup);
            } catch (RuntimeException e) {
                scheduleCleanupQuietly(token, System.currentTimeMillis() + Duration.ofMinutes(5).toMillis());
                throw providerFailure("清理过期上传失败", e);
            }
            try {
                ticketStore.removeSession(token);
                return true;
            } catch (RuntimeException e) {
                scheduleCleanupQuietly(token, System.currentTimeMillis() + Duration.ofMinutes(5).toMillis());
                throw stateFailure("移除过期 UploadTicket 失败", e);
            }
        });
    }

    private String completeLocked(String token, CompleteRequest request) {
        OssUploadTicket ticket = requireOwned(token);
        if (ticket.state() == OssUploadState.COMPLETED && ticket.ossId() != null) {
            ticketStore.removeCompletedCleanup(token);
            return ticket.ossId().toString();
        }
        Long existingOssId = metadataStore.findByObject(ticket.service(), ticket.objectKey());
        if (existingOssId != null) {
            markCompleted(ticket, existingOssId);
            return existingOssId.toString();
        }
        saveTicket(ticket.withState(OssUploadState.COMPLETING));
        Long ossId;
        try {
            Optional<OssObjectStat> existingObject = objectStore.headIfPresent(ticket);
            if (ticket.mode() == OssUploadMode.MULTIPART && existingObject.isEmpty()) {
                List<OssCompletedPart> parts = validateMultipartParts(ticket, request.parts());
                objectStore.completeMultipart(ticket, parts);
                existingObject = objectStore.headIfPresent(ticket);
            } else if (ticket.mode() == OssUploadMode.SINGLE && !request.parts().isEmpty()) {
                throw new OssUploadException(OssUploadError.INVALID_PARTS, "SINGLE Complete 不接受 Part");
            }
            OssObjectStat stat = existingObject.orElseThrow(() ->
                new OssUploadException(OssUploadError.COMPLETE_VALIDATION_FAILED, "OSS 对象不存在"));
            validateCompletedObject(ticket, stat);
            ossId = metadataStore.registerTemporary(ticket);
        } catch (OssUploadException e) {
            cleanupAfterFailure(ticket);
            throw e;
        } catch (RuntimeException e) {
            cleanupAfterFailure(ticket);
            throw providerFailure("完成 OSS 上传失败", e);
        }
        markCompleted(ticket, ossId);
        return ossId.toString();
    }

    private List<OssCompletedPart> validateMultipartParts(OssUploadTicket ticket, List<CompletedPart> requested) {
        if (requested == null || requested.size() != ticket.partCount()) {
            throw new OssUploadException(OssUploadError.INVALID_PARTS, "Complete Part 数量不完整");
        }
        List<CompletedPart> ordered = requested.stream().sorted(Comparator.comparingInt(CompletedPart::partNumber)).toList();
        for (int index = 0; index < ordered.size(); index++) {
            CompletedPart part = ordered.get(index);
            if (part.partNumber() != index + 1 || part.eTag() == null || part.eTag().isBlank()) {
                throw new OssUploadException(OssUploadError.INVALID_PARTS, "Complete Part 必须连续且 ETag 非空");
            }
        }
        List<OssMultipartPart> providerParts = objectStore.listParts(ticket).stream()
            .sorted(Comparator.comparingInt(OssMultipartPart::partNumber)).toList();
        if (providerParts.size() != ordered.size()) {
            throw new OssUploadException(OssUploadError.INVALID_PARTS, "Provider Part 数量不完整");
        }
        long totalSize = 0;
        List<OssCompletedPart> result = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            CompletedPart supplied = ordered.get(index);
            OssMultipartPart actual = providerParts.get(index);
            if (actual.partNumber() != supplied.partNumber() || !sameEtag(actual.eTag(), supplied.eTag())) {
                throw new OssUploadException(OssUploadError.INVALID_PARTS, "Provider Part 与 Complete 请求不一致");
            }
            if (index < ordered.size() - 1 && actual.size() != ticket.partSize()) {
                throw new OssUploadException(OssUploadError.INVALID_PARTS, "非末尾 Part 大小不正确");
            }
            totalSize += actual.size();
            result.add(new OssCompletedPart(actual.partNumber(), actual.eTag(), actual.checksums()));
        }
        if (totalSize != ticket.fileSize()) {
            throw new OssUploadException(OssUploadError.INVALID_PARTS, "Part 总大小与原文件不一致");
        }
        return result;
    }

    private void validateCompletedObject(OssUploadTicket ticket, OssObjectStat stat) {
        if (stat.size() != ticket.fileSize() || stat.contentType() == null
            || !ticket.contentType().equalsIgnoreCase(normalizeContentType(stat.contentType()))
            || !Objects.equals(ticket.fingerprintDigest(), stat.metadata().get("upload-fingerprint"))) {
            throw new OssUploadException(OssUploadError.COMPLETE_VALIDATION_FAILED,
                "OSS HEAD 元数据与 UploadTicket 不一致");
        }
        byte[] expected = MAGIC_PREFIXES.get(ticket.contentType());
        if (expected != null) {
            byte[] actual = objectStore.readPrefix(ticket, MAGIC_PREFIX_LENGTH);
            if (!startsWith(actual, expected)) {
                throw new OssUploadException(OssUploadError.COMPLETE_VALIDATION_FAILED,
                    "文件 magic bytes 与 Content-Type 不一致");
            }
        } else if ("image/webp".equals(ticket.contentType())) {
            byte[] actual = objectStore.readPrefix(ticket, MAGIC_PREFIX_LENGTH);
            if (actual.length < 12 || !startsWith(actual, "RIFF".getBytes(StandardCharsets.US_ASCII))
                || !startsWith(Arrays.copyOfRange(actual, 8, actual.length), "WEBP".getBytes(StandardCharsets.US_ASCII))) {
                throw new OssUploadException(OssUploadError.COMPLETE_VALIDATION_FAILED,
                    "文件 magic bytes 与 Content-Type 不一致");
            }
        } else if ("video/mp4".equals(ticket.contentType()) || "audio/mp4".equals(ticket.contentType())) {
            byte[] actual = objectStore.readPrefix(ticket, MAGIC_PREFIX_LENGTH);
            if (actual.length < 8 || !Arrays.equals(Arrays.copyOfRange(actual, 4, 8), "ftyp".getBytes(StandardCharsets.US_ASCII))) {
                throw new OssUploadException(OssUploadError.COMPLETE_VALIDATION_FAILED,
                    "文件 magic bytes 与 Content-Type 不一致");
            }
        } else if ("audio/mpeg".equals(ticket.contentType())) {
            byte[] actual = objectStore.readPrefix(ticket, MAGIC_PREFIX_LENGTH);
            boolean id3 = startsWith(actual, "ID3".getBytes(StandardCharsets.US_ASCII));
            boolean frame = actual.length >= 2 && (actual[0] & 0xff) == 0xff && (actual[1] & 0xe0) == 0xe0;
            if (!id3 && !frame) {
                throw new OssUploadException(OssUploadError.COMPLETE_VALIDATION_FAILED,
                    "文件 magic bytes 与 Content-Type 不一致");
            }
        } else if ("audio/wav".equals(ticket.contentType())) {
            byte[] actual = objectStore.readPrefix(ticket, MAGIC_PREFIX_LENGTH);
            if (actual.length < 12 || !startsWith(actual, "RIFF".getBytes(StandardCharsets.US_ASCII))
                || !startsWith(Arrays.copyOfRange(actual, 8, actual.length), "WAVE".getBytes(StandardCharsets.US_ASCII))) {
                throw new OssUploadException(OssUploadError.COMPLETE_VALIDATION_FAILED,
                    "文件 magic bytes 与 Content-Type 不一致");
            }
        }
    }

    private OssUploadTicket requireOwned(String token) {
        OssUploadTicket ticket;
        try {
            ticket = ticketStore.get(token);
        } catch (RuntimeException e) {
            throw stateFailure("读取 UploadTicket 失败", e);
        }
        if (ticket == null) {
            throw new OssUploadException(OssUploadError.SESSION_NOT_FOUND, "上传会话不存在");
        }
        if (ticket.expiresAt() <= System.currentTimeMillis()) {
            throw new OssUploadException(OssUploadError.SESSION_EXPIRED, "上传会话已过期");
        }
        requireOwner(ticket);
        if (ticket.state() == OssUploadState.ABORTED || ticket.state() == OssUploadState.EXPIRED) {
            throw new OssUploadException(OssUploadError.INVALID_STATE, "上传会话不可再使用");
        }
        return ticket;
    }

    private void requireOwner(OssUploadTicket ticket) {
        OssUploadIdentityResolver.Identity current = identityResolver.resolve();
        if (!Objects.equals(ticket.userId(), current.userId())
            || !Objects.equals(ticket.clientPk(), current.clientPk())) {
            throw new OssUploadException(OssUploadError.SESSION_OWNER_MISMATCH, "上传会话不属于当前用户或 Client");
        }
    }

    private void authorizePolicy(OssUploadProperties.Policy policy, OssUploadIdentityResolver.Identity identity) {
        if (!identityResolver.hasPermission(policy.getRequiredPermission())) {
            throw new OssUploadException(OssUploadError.ACCESS_DENIED, "缺少上传策略要求的权限");
        }
        if (policy.getAllowedClientPks() != null && !policy.getAllowedClientPks().isEmpty()
            && !policy.getAllowedClientPks().contains(identity.clientPk())) {
            throw new OssUploadException(OssUploadError.ACCESS_DENIED, "当前 Client 入口不允许使用该上传策略");
        }
    }

    private void validateInitRequest(InitRequest request) {
        if (request == null || request.policy() == null || request.policy().isBlank()
            || request.fileName() == null || request.fileName().isBlank() || request.fileName().length() > 255
            || request.fileName().contains("/") || request.fileName().contains("\\")
            || request.fileName().chars().anyMatch(Character::isISOControl)
            || request.fileSize() <= 0 || request.fingerprint() == null || request.fingerprint().isBlank()
            || request.fingerprint().length() > 512) {
            throw new OssUploadException(OssUploadError.INVALID_FILE, "上传文件元数据无效");
        }
        String suffix = suffix(request.fileName());
        if (suffix.length() > 10) {
            throw new OssUploadException(OssUploadError.INVALID_FILE, "文件后缀过长");
        }
    }

    private void requireMultipart(OssUploadTicket ticket) {
        if (ticket.mode() != OssUploadMode.MULTIPART || ticket.uploadId() == null) {
            throw new OssUploadException(OssUploadError.INVALID_STATE, "当前会话不是 Multipart");
        }
    }

    private void markCompleted(OssUploadTicket ticket, Long ossId) {
        try {
            ticketStore.save(ticket.completed(ossId), remaining(ticket));
            ticketStore.removeCompletedCleanup(ticket.token());
        } catch (RuntimeException e) {
            throw stateFailure("保存 Complete 幂等结果失败", e);
        }
    }

    private void saveTicket(OssUploadTicket ticket) {
        try {
            ticketStore.save(ticket, remaining(ticket));
        } catch (RuntimeException e) {
            throw stateFailure("更新 UploadTicket 失败", e);
        }
    }

    private <T> T locked(String token, java.util.function.Supplier<T> action) {
        try {
            return ticketStore.locked(token, action);
        } catch (OssUploadException e) {
            throw e;
        } catch (RuntimeException e) {
            throw stateFailure("锁定 UploadTicket 失败", e);
        }
    }

    private void cleanupAfterFailure(OssUploadTicket ticket) {
        cleanupSession(ticket, false);
    }

    private void cleanupSession(OssUploadTicket ticket, boolean propagateFailure) {
        OssUploadCleanupRecord cleanup = cleanupRecord(ticket);
        try {
            objectStore.abort(cleanup);
            objectStore.deleteObject(cleanup);
        } catch (RuntimeException e) {
            scheduleCleanupQuietly(ticket.token(), System.currentTimeMillis());
            if (propagateFailure) {
                throw providerFailure("取消上传的 Provider 清理失败", e);
            }
            return;
        }
        try {
            ticketStore.removeSession(ticket.token());
        } catch (RuntimeException e) {
            scheduleCleanupQuietly(ticket.token(), System.currentTimeMillis());
            if (propagateFailure) {
                throw stateFailure("移除 UploadTicket 失败", e);
            }
        }
    }

    private void scheduleCleanupQuietly(String token, long whenEpochMilli) {
        try {
            ticketStore.scheduleCleanup(token, whenEpochMilli);
        } catch (RuntimeException ignored) {
            // Provider Lifecycle 是 Redis 不可用时的最终清理兜底。
        }
    }

    private void cleanupPrepared(OssUploadCleanupRecord cleanup) {
        try {
            objectStore.abort(cleanup);
            objectStore.deleteObject(cleanup);
        } catch (RuntimeException ignored) {
            // Ticket 尚未建立，调用方会得到初始化失败；Provider Lifecycle 是最后兜底。
        }
    }

    private OssUploadCleanupRecord cleanupRecord(OssUploadTicket ticket) {
        return new OssUploadCleanupRecord(ticket.token(), ticket.mode(), ticket.service(), ticket.objectKey(),
            ticket.uploadId(), ticket.expiresAt());
    }

    private Duration remaining(OssUploadTicket ticket) {
        return Duration.ofMillis(Math.max(1000, ticket.expiresAt() - System.currentTimeMillis()));
    }

    private long ceilDiv(long value, long divisor) {
        return 1 + (value - 1) / divisor;
    }

    private String normalizeContentType(String contentType) {
        String value = contentType == null ? "" : contentType.strip().toLowerCase(Locale.ROOT);
        int parameters = value.indexOf(';');
        if (parameters >= 0) {
            value = value.substring(0, parameters).strip();
        }
        if (!CONTENT_TYPE.matcher(value).matches()) {
            throw new OssUploadException(OssUploadError.INVALID_FILE, "Content-Type 无效");
        }
        return value;
    }

    private String suffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    private boolean sameEtag(String first, String second) {
        return trimQuotes(first).equals(trimQuotes(second));
    }

    private String trimQuotes(String value) {
        return value == null ? "" : value.replace("\"", "");
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 不支持 SHA-256", e);
        }
    }

    private static byte[] hex(String value) {
        return HexFormat.of().parseHex(value);
    }

    private boolean startsWith(byte[] actual, byte[] expected) {
        return actual != null && actual.length >= expected.length
            && Arrays.equals(Arrays.copyOf(actual, expected.length), expected);
    }

    private OssUploadException providerFailure(String message, RuntimeException cause) {
        return cause instanceof OssUploadException uploadException ? uploadException
            : new OssUploadException(OssUploadError.PROVIDER_FAILURE, message, cause);
    }

    private OssUploadException stateFailure(String message, RuntimeException cause) {
        return cause instanceof OssUploadException uploadException ? uploadException
            : new OssUploadException(OssUploadError.STATE_STORE_FAILURE, message, cause);
    }
}
