package org.namewta.profile.enterprise.dao;

import java.time.Instant;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.enterprise.domain.ProfileVerificationAttempt;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationAttempt;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationAttemptRow;
import org.namewta.profile.enterprise.mapper.EnterpriseVerificationAttemptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 企业认证尝试数据访问对象，统一封装认证记录查询和锁语义。
 *
 * <p>本能力的查询条件、锁语义和 Mapper 调用均收敛在此边界。</p>
 */
@RequiredArgsConstructor
@Repository
public class EnterpriseVerificationAttemptDao {

    private final EnterpriseVerificationAttemptMapper mapper;

    /**
     * 加锁查询持久化数据（lockApplication）。
     */
    public EnterpriseVerificationApplicationRow lockApplication(long applicationId) {
        return mapper.lockApplication(applicationId);
    }

    /**
     * 计算下一个持久化数据（nextAttemptNo）。
     */
    public Integer nextAttemptNo(long applicationId) {
        return mapper.nextAttemptNo(applicationId);
    }

    /**
     * 新增持久化数据（insertAttempt）。
     */
    public int insertAttempt(EnterpriseVerificationAttemptRow row) {
        return mapper.insertAttempt(row);
    }

    /**
     * 加锁查询持久化数据（lockByProviderRequest）。
     */
    public EnterpriseVerificationAttemptRow lockByProviderRequest(String providerCode, String providerRequestId) {
        return mapper.lockByProviderRequest(providerCode, providerRequestId);
    }

    /**
     * 完成持久化数据（completeAttempt）。
     */
    public int completeAttempt(long verificationAttemptId, String status, String normalizedResultJson, String providerEvidenceJson, String errorCode, Instant completedTime) {
        return mapper.completeAttempt(verificationAttemptId, status, normalizedResultJson, providerEvidenceJson, errorCode, completedTime);
    }

    /**
     * 新增持久化数据（insertSecurityAudit）。
     */
    public int insertSecurityAudit(long auditId, Long applicationId, String result, String failureCategory, Instant occurredTime) {
        return mapper.insertSecurityAudit(auditId, applicationId, result, failureCategory, occurredTime);
    }
}
