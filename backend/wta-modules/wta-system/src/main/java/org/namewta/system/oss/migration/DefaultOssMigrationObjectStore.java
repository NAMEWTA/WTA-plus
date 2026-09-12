package org.namewta.system.oss.migration;

import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.exception.OssErrorCode;
import org.namewta.common.oss.exception.S3StorageException;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.common.oss.model.OssChecksumAlgorithm;
import org.namewta.common.oss.model.OssObjectStat;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Component
public class DefaultOssMigrationObjectStore implements OssMigrationObjectStore {

    @Override
    public Inspection inspect(String sourceService, String targetService, String objectKey, long maxVerifyBytes) {
        OssClient source = client(sourceService, AccessPolicy.PRIVATE);
        OssClient target = client(targetService, AccessPolicy.PUBLIC_READ);
        Optional<OssObjectStat> sourceStat = head(source, objectKey);
        Optional<OssObjectStat> targetStat = head(target, objectKey);
        boolean conflict = sourceStat.isPresent() && targetStat.isPresent()
            && !matches(source, target, objectKey, sourceStat.get(), targetStat.get(), maxVerifyBytes);
        return new Inspection(sourceStat.isPresent(), targetStat.isPresent(), conflict,
            sourceStat.map(OssObjectStat::size).orElse(0L), sourceStat.map(OssObjectStat::eTag).orElse(null));
    }

    @Override
    public Transfer transferAndVerify(String sourceService, String targetService, String objectKey,
                                      long maxVerifyBytes) {
        OssClient source = client(sourceService, AccessPolicy.PRIVATE);
        OssClient target = client(targetService, AccessPolicy.PUBLIC_READ);
        OssObjectStat sourceStat = head(source, objectKey).orElseThrow(() ->
            new OssMigrationException(OssMigrationError.OBJECT_NOT_FOUND, "来源对象不存在"));
        Optional<OssObjectStat> existing = head(target, objectKey);
        if (existing.isPresent() && !matches(source, target, objectKey, sourceStat, existing.get(), maxVerifyBytes)) {
            throw new OssMigrationException(OssMigrationError.TARGET_CONFLICT, "目标对象内容冲突");
        }
        if (existing.isEmpty()) {
            String sourceBucket = bucket(source);
            String targetBucket = bucket(target);
            try {
                target.bucketCopyObject(sourceBucket, objectKey, targetBucket, objectKey);
            } catch (RuntimeException ex) {
                throw new OssMigrationException(OssMigrationError.COPY_FAILED, "OSS 对象复制失败", ex);
            }
        }
        OssObjectStat targetStat = head(target, objectKey).orElseThrow(() ->
            new OssMigrationException(OssMigrationError.COPY_FAILED, "复制后目标对象不存在"));
        if (!matches(source, target, objectKey, sourceStat, targetStat, maxVerifyBytes)) {
            throw new OssMigrationException(OssMigrationError.CONTENT_MISMATCH, "复制后内容校验失败");
        }
        return new Transfer(sourceStat.size(), targetStat.size(), sourceStat.eTag(), targetStat.eTag());
    }

    @Override
    public boolean exists(String service, String objectKey) {
        return head(OssFactory.instance(service), objectKey).isPresent();
    }

    @Override
    public void delete(String service, String objectKey) {
        OssFactory.instance(service).delete(objectKey);
    }

    private OssClient client(String service, AccessPolicy expected) {
        OssClient client = OssFactory.instance(service);
        if (client.config().accessControlPolicyConfig().accessPolicy() != expected) {
            throw new OssMigrationException(OssMigrationError.ACCESS_POLICY_MISMATCH,
                "迁移存储实际访问类型不匹配");
        }
        return client;
    }

    private Optional<OssObjectStat> head(OssClient client, String key) {
        try {
            return Optional.of(client.headObject(key));
        } catch (S3StorageException ex) {
            if (ex.code() == OssErrorCode.OBJECT_NOT_FOUND) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    private boolean matches(OssClient source, OssClient target, String key, OssObjectStat left,
                            OssObjectStat right, long maxVerifyBytes) {
        if (left.size() != right.size()) {
            return false;
        }
        if (matchingChecksum(left.checksums(), right.checksums())) {
            return true;
        }
        if (trustedEtag(left.eTag()) && left.eTag().equals(right.eTag())) {
            return true;
        }
        if (left.size() > maxVerifyBytes) {
            throw new OssMigrationException(OssMigrationError.CONTENT_MISMATCH,
                "对象缺少可信校验值且超过流式校验上限");
        }
        return digest(source, key).equals(digest(target, key));
    }

    private boolean matchingChecksum(Map<OssChecksumAlgorithm, String> left,
                                     Map<OssChecksumAlgorithm, String> right) {
        return left.entrySet().stream().anyMatch(entry -> entry.getValue() != null
            && entry.getValue().equals(right.get(entry.getKey())));
    }

    private boolean trustedEtag(String etag) {
        return etag != null && !etag.isBlank() && !etag.contains("-");
    }

    private String digest(OssClient client, String key) {
        return client.download(key, (result, input) -> sha256(input));
    }

    private String sha256(InputStream input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new OssMigrationException(OssMigrationError.CONTENT_MISMATCH, "对象流式校验失败", e);
        }
    }

    private String bucket(OssClient client) {
        return client.config().bucket().orElseThrow(() ->
            new OssMigrationException(OssMigrationError.INVALID_REQUEST, "迁移存储未配置 Bucket"));
    }
}
