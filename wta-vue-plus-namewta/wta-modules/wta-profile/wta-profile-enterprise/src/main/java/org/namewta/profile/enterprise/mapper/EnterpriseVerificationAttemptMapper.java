package org.namewta.profile.enterprise.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.enterprise.domain.ProfileVerificationAttempt;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationAttempt;

import org.apache.ibatis.annotations.Param;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationAttemptRow;

import java.time.Instant;

/**
 * EnterpriseVerificationAttemptMapper 持久化映射器，负责本能力的数据映射。
 */
public interface EnterpriseVerificationAttemptMapper extends BaseMapperPlus<ProfileVerificationAttempt, EnterpriseVerificationAttempt> {

    /**
     * 定义加锁查询映射（lockApplication）。
     */
    EnterpriseVerificationApplicationRow lockApplication(@Param("applicationId") long applicationId);

    /**
     * 定义计算下一个映射（nextAttemptNo）。
     */
    Integer nextAttemptNo(@Param("applicationId") long applicationId);

    /**
     * 定义新增映射（insertAttempt）。
     */
    int insertAttempt(EnterpriseVerificationAttemptRow row);

    /**
     * 定义加锁查询映射（lockByProviderRequest）。
     */
    EnterpriseVerificationAttemptRow lockByProviderRequest(@Param("providerCode") String providerCode,
                                                            @Param("providerRequestId") String providerRequestId);

    /**
     * 定义完成映射（completeAttempt）。
     */
    int completeAttempt(@Param("verificationAttemptId") long verificationAttemptId,
                        @Param("status") String status,
                        @Param("normalizedResultJson") String normalizedResultJson,
                        @Param("providerEvidenceJson") String providerEvidenceJson,
                        @Param("errorCode") String errorCode,
                        @Param("completedTime") Instant completedTime);

    /**
     * 定义新增映射（insertSecurityAudit）。
     */
    int insertSecurityAudit(@Param("auditId") long auditId,
                            @Param("applicationId") Long applicationId,
                            @Param("result") String result,
                            @Param("failureCategory") String failureCategory,
                            @Param("occurredTime") Instant occurredTime);
}
