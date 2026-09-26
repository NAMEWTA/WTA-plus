package org.namewta.system.oss.service;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.exception.OssLifecycleError;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.namewta.system.oss.mapper.SysOssRefMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/** 普通对象清理的短事务；供应商调用只能在预约已提交后进行。 */
@Service
@RequiredArgsConstructor
public class OssCleanupAtomicService {
    public static final String DELETING = "DELETING";
    private final SysOssMapper ossMapper;
    private final SysOssRefMapper refMapper;

    /** 锁内预约唯一删除权。未知结果只允许查询，不重新授权 DELETE。 */
    @DSTransactional
    public Plan reserve(Long ossId, LocalDateTime now, boolean dryRun) {
        SysOss oss = ossMapper.selectByIdForUpdate(ossId);
        if (oss == null) return null;
        if (!"Y".equals(oss.getIsTemp()) || oss.getExpireTime() == null || oss.getExpireTime().isAfter(now)) {
            return null;
        }
        if (DELETING.equals(oss.getDeleteState())) {
            return new Plan(oss, false, !dryRun);
        }
        if (!"ACTIVE".equals(oss.getDeleteState()) && !"PENDING".equals(oss.getDeleteState())) return null;
        if (refMapper.countActiveByOssId(ossId) > 0) {
            if (!dryRun) requireUpdated(ossMapper.updateLifecycle(ossId, "N", null));
            return null;
        }
        if (dryRun) return new Plan(oss, false, false);
        if ("ACTIVE".equals(oss.getDeleteState())) {
            requireUpdated(ossMapper.markDeletePending(ossId, now));
            return new Plan(oss, false, false);
        }
        requireUpdated(ossMapper.reserveDeletion(ossId));
        oss.setDeleteState(DELETING);
        return new Plan(oss, true, true);
    }

    /** 删除已确认或有界 HEAD 证明缺失后，重锁并核对预约身份再移除元数据。 */
    @DSTransactional
    public boolean complete(SysOss reservation) {
        SysOss current = ossMapper.selectByIdForUpdate(reservation.getOssId());
        if (current == null) return true;
        if (!DELETING.equals(current.getDeleteState())
            || !Objects.equals(current.getService(), reservation.getService())
            || !Objects.equals(current.getFileName(), reservation.getFileName())
            || refMapper.countActiveByOssId(current.getOssId()) != 0) {
            throw new OssLifecycleException(OssLifecycleError.PROVIDER_DELETE_FAILED, "OSS 清理预约已变化");
        }
        requireUpdated(ossMapper.deleteById(current.getOssId()));
        return true;
    }

    private void requireUpdated(int count) {
        if (count != 1) throw new OssLifecycleException(OssLifecycleError.PROVIDER_DELETE_FAILED,
            "OSS 清理状态更新失败");
    }

    public record Plan(SysOss object, boolean deleteRequired, boolean checkProvider) { }
}
