package org.namewta.profile.person.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.person.domain.ProfileVerificationAttempt;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.person.domain.model.read.PersonVerificationApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonVerificationAttemptRow;

import java.time.Instant;

/**
 * PersonVerificationAttemptMapper 持久化映射器，负责本能力的数据映射。
 */
public interface PersonVerificationAttemptMapper extends BaseMapperPlus<ProfileVerificationAttempt, ProfileVerificationAttempt> {

    /**
     * 定义加锁查询映射（lockApplication）。
     */
    PersonVerificationApplicationRow lockApplication(@Param("applicationId") long applicationId);

    /**
     * 定义计算下一个映射（nextAttemptNo）。
     */
    Integer nextAttemptNo(@Param("applicationId") long applicationId);

    /**
     * 定义新增映射（insertAttempt）。
     */
    int insertAttempt(PersonVerificationAttemptRow row);

    /**
     * 定义加锁查询映射（lockByProviderRequest）。
     */
    PersonVerificationAttemptRow lockByProviderRequest(@Param("providerCode") String providerCode,
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
