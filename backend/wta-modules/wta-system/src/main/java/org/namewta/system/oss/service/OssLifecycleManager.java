package org.namewta.system.oss.service;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.OssPresignedRequest;
import org.namewta.system.api.OssService;
import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.system.oss.domain.SysOssRef;
import org.namewta.system.oss.exception.OssLifecycleError;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.namewta.system.oss.mapper.SysOssRefMapper;
import org.namewta.system.oss.provider.OssObjectStore;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 通过 sys_oss 行锁协调引用变更、删除和临时对象清理。
 */
@Service
public class OssLifecycleManager {

    private static final String DELETE_PENDING = "PENDING";
    private static final Pattern PHYSICAL_TABLE = Pattern.compile("[a-z][a-z0-9_]{0,63}");
    private static final Pattern PHYSICAL_ID = Pattern.compile("[^\\s]{1,64}");

    private final SysOssMapper ossMapper;
    private final SysOssRefMapper refMapper;
    private final OssObjectStore objectStore;
    private final OssLifecycleProperties properties;
    private final OssStorageReadinessRegistry readinessRegistry;

    @Autowired
    public OssLifecycleManager(SysOssMapper ossMapper, SysOssRefMapper refMapper, OssObjectStore objectStore,
                               OssLifecycleProperties properties,
                               OssStorageReadinessRegistry readinessRegistry) {
        this.ossMapper = ossMapper;
        this.refMapper = refMapper;
        this.objectStore = objectStore;
        this.properties = properties;
        this.readinessRegistry = readinessRegistry;
    }

    /**
     * 保留既有构造签名，供不涉及访问 URL 的嵌入式调用与测试平滑升级。
     */
    public OssLifecycleManager(SysOssMapper ossMapper, SysOssRefMapper refMapper, OssObjectStore objectStore,
                               OssLifecycleProperties properties) {
        this(ossMapper, refMapper, objectStore, properties,
            new OssStorageReadinessRegistry(new OssStorageReadinessProperties()));
    }

    @DSTransactional
    public OssService.OssReferenceState bind(Long ossId, String refType, String refId) {
        validateReference(refType, refId);
        SysOss oss = requireLocked(ossId);
        long before = refMapper.countActiveByOssId(ossId);
        if (!refMapper.existsActive(ossId, refType, refId)) {
            if (refMapper.restoreReference(ossId, refType, refId) == 0) {
                SysOssRef reference = new SysOssRef();
                reference.setOssId(ossId);
                reference.setRefType(refType);
                reference.setRefId(refId);
                reference.setDelFlag("0");
                refMapper.insert(reference);
            }
            before++;
        }
        if ("Y".equals(oss.getIsTemp()) || oss.getExpireTime() != null
            || DELETE_PENDING.equals(oss.getDeleteState())) {
            ossMapper.updateLifecycle(ossId, "N", null);
        }
        return new OssService.OssReferenceState(ossId, false, null, before);
    }

    @DSTransactional
    public OssService.OssReferenceState unbind(Long ossId, String refType, String refId) {
        validateReference(refType, refId);
        SysOss oss = requireLocked(ossId);
        long count = refMapper.countActiveByOssId(ossId);
        boolean deactivated = refMapper.existsActive(ossId, refType, refId)
            && refMapper.deactivateReference(ossId, refType, refId) > 0;
        if (!deactivated) {
            return new OssService.OssReferenceState(ossId, "Y".equals(oss.getIsTemp()),
                oss.getExpireTime(), count);
        }
        count--;
        if (count == 0) {
            LocalDateTime expireTime = LocalDateTime.now().plus(properties.getTempRetention());
            ossMapper.updateLifecycle(ossId, "Y", expireTime);
            return new OssService.OssReferenceState(ossId, true, expireTime, 0);
        }
        return new OssService.OssReferenceState(ossId, false, null, count);
    }

    @DSTransactional
    public void reconcileReferences(String refType, String refId,
                                    Collection<Long> previousOssIds, Collection<Long> currentOssIds) {
        validateReference(refType, refId);
        List<Long> previous = normalizeOssIds(previousOssIds);
        List<Long> current = normalizeOssIds(currentOssIds);
        Set<Long> previousSet = new HashSet<>(previous);
        Set<Long> currentSet = new HashSet<>(current);

        current.stream().filter(ossId -> !previousSet.contains(ossId))
            .forEach(ossId -> bind(ossId, refType, refId));
        previous.stream().filter(ossId -> !currentSet.contains(ossId))
            .forEach(ossId -> unbind(ossId, refType, refId));
    }

    public OssService.OssLifecycleSnapshot snapshot(Long ossId) {
        SysOss oss = require(ossId);
        List<OssService.OssReference> references = refMapper.selectActiveByOssId(ossId).stream()
            .map(ref -> new OssService.OssReference(ref.getRefType(), ref.getRefId()))
            .toList();
        return new OssService.OssLifecycleSnapshot(ossId, "Y".equals(oss.getIsTemp()),
            oss.getExpireTime(), references);
    }

    public OssService.OssAccessUrl resolveAccessUrl(Long ossId) {
        SysOss oss = requireDownloadable(ossId);
        AccessPolicy policy = accessPolicy(oss);
        if (policy == AccessPolicy.PUBLIC_READ) {
            try {
                String url = objectStore.publicUrl(oss);
                if (url == null || url.isBlank()) {
                    throw new IllegalStateException("empty public URL");
                }
                return new OssService.OssAccessUrl("PUBLIC", url, null, oss.getOriginalName());
            } catch (RuntimeException ex) {
                throw accessFailure(ossId, ex);
            }
        }
        OssService.OssDownloadUrl signed = privateDownload(oss, resolveDownloadTtl(null));
        return new OssService.OssAccessUrl("PRIVATE", signed.url(), signed.expiresAt(), signed.fileName());
    }

    public OssService.OssDownloadUrl presignDownload(Long ossId) {
        return presignDownload(ossId, null);
    }

    public OssService.OssDownloadUrl presignDownload(Long ossId, String policyName) {
        SysOss oss = requireDownloadable(ossId);
        if (accessPolicy(oss) != AccessPolicy.PRIVATE) {
            throw new OssLifecycleException(OssLifecycleError.PUBLIC_PRESIGN_FORBIDDEN,
                "公共 OSS 对象不能生成私有签名: " + ossId);
        }
        return privateDownload(oss, resolveDownloadTtl(policyName));
    }

    private Duration resolveDownloadTtl(String policyName) {
        try {
            return properties.resolveDownloadTtl(policyName);
        } catch (RuntimeException ex) {
            throw new OssLifecycleException(OssLifecycleError.DOWNLOAD_POLICY_INVALID,
                "OSS 下载策略无效", ex);
        }
    }

    @DSTransactional
    public boolean deleteObjects(Collection<Long> ossIds) {
        if (ossIds == null || ossIds.isEmpty()) {
            return false;
        }
        List<Long> orderedIds = ossIds.stream().distinct().sorted().toList();
        List<SysOss> objects = ossMapper.selectByIdsForUpdate(orderedIds).stream()
            .sorted(Comparator.comparing(SysOss::getOssId)).toList();
        if (objects.size() != orderedIds.size()) {
            throw new OssLifecycleException(OssLifecycleError.OBJECT_NOT_FOUND, "部分 OSS 对象不存在");
        }
        for (SysOss oss : objects) {
            if (refMapper.countActiveByOssId(oss.getOssId()) > 0) {
                throw new OssLifecycleException(OssLifecycleError.OBJECT_REFERENCED,
                    "OSS 对象仍被业务数据引用: " + oss.getOssId());
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (SysOss oss : objects) {
            ossMapper.markDeletePending(oss.getOssId(), now);
        }
        return true;
    }

    @DSTransactional
    public boolean cleanupExpired(Long ossId, LocalDateTime now, boolean dryRun) {
        SysOss oss = requireLocked(ossId);
        if (!"Y".equals(oss.getIsTemp()) || oss.getExpireTime() == null || oss.getExpireTime().isAfter(now)) {
            return false;
        }
        if (refMapper.countActiveByOssId(ossId) > 0) {
            ossMapper.updateLifecycle(ossId, "N", null);
            return false;
        }
        if (dryRun) {
            return true;
        }
        if (!DELETE_PENDING.equals(oss.getDeleteState())) {
            ossMapper.markDeletePending(ossId, now);
            return true;
        }
        objectStore.delete(oss);
        return ossMapper.deleteById(ossId) > 0;
    }

    public List<Long> findExpiredTempIds(LocalDateTime now) {
        int limit = Math.max(1, properties.getCleanupBatchSize());
        return new ArrayList<>(ossMapper.selectExpiredTempIds(now, limit));
    }

    private SysOss requireLocked(Long ossId) {
        SysOss oss = ossMapper.selectByIdForUpdate(ossId);
        if (oss == null) {
            throw new OssLifecycleException(OssLifecycleError.OBJECT_NOT_FOUND, "OSS 对象不存在: " + ossId);
        }
        return oss;
    }

    private SysOss require(Long ossId) {
        SysOss oss = ossMapper.selectById(ossId);
        if (oss == null) {
            throw new OssLifecycleException(OssLifecycleError.OBJECT_NOT_FOUND, "OSS 对象不存在: " + ossId);
        }
        return oss;
    }

    private SysOss requireDownloadable(Long ossId) {
        SysOss oss = require(ossId);
        if (DELETE_PENDING.equals(oss.getDeleteState())) {
            throw new OssLifecycleException(OssLifecycleError.OBJECT_DELETE_PENDING,
                "OSS 对象正在删除: " + ossId);
        }
        try {
            readinessRegistry.requireServing(oss.getService());
        } catch (RuntimeException ex) {
            throw new OssLifecycleException(OssLifecycleError.STORAGE_NOT_SERVING,
                "OSS 存储配置当前不可服务: " + oss.getService(), ex);
        }
        return oss;
    }

    private AccessPolicy accessPolicy(SysOss oss) {
        try {
            AccessPolicy policy = objectStore.accessPolicy(oss);
            if (policy == null) {
                throw new IllegalStateException("missing access policy");
            }
            return policy;
        } catch (RuntimeException ex) {
            throw new OssLifecycleException(OssLifecycleError.ACCESS_POLICY_INVALID,
                "OSS 访问类型不可用: " + oss.getOssId(), ex);
        }
    }

    private OssService.OssDownloadUrl privateDownload(SysOss oss, Duration ttl) {
        try {
            OssPresignedRequest request = objectStore.presign(oss, ttl);
            if (request == null || request.url() == null || request.url().isBlank() || request.expiresAt() == null) {
                throw new IllegalStateException("incomplete presigned response");
            }
            return new OssService.OssDownloadUrl(request.url(), request.expiresAt(), oss.getOriginalName());
        } catch (RuntimeException ex) {
            throw accessFailure(oss.getOssId(), ex);
        }
    }

    private OssLifecycleException accessFailure(Long ossId, RuntimeException cause) {
        if (cause instanceof OssLifecycleException lifecycleException) {
            return lifecycleException;
        }
        return new OssLifecycleException(OssLifecycleError.PROVIDER_ACCESS_FAILED,
            "OSS Provider 访问授权失败: " + ossId, cause);
    }

    private void validateReference(String refType, String refId) {
        if (refType == null || !PHYSICAL_TABLE.matcher(refType).matches()
            || refId == null || !PHYSICAL_ID.matcher(refId).matches()) {
            throw new OssLifecycleException(OssLifecycleError.INVALID_REFERENCE,
                "refType 必须是实际物理表名，refId 必须是真实主键");
        }
    }

    private List<Long> normalizeOssIds(Collection<Long> ossIds) {
        if (ossIds == null || ossIds.isEmpty()) {
            return List.of();
        }
        if (ossIds.stream().anyMatch(ossId -> ossId == null || ossId <= 0)) {
            throw new OssLifecycleException(OssLifecycleError.INVALID_REFERENCE,
                "ossId 必须是正数");
        }
        return ossIds.stream().distinct().sorted().toList();
    }
}
