package org.namewta.profile.person.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.person.domain.ProfileNotificationAudit;
import org.namewta.profile.person.domain.model.read.PersonNotificationAuditRow;
import org.apache.ibatis.annotations.Param;

/**
 * PersonNotificationAuditMapper 持久化映射器，负责本能力的数据映射。
 */
public interface PersonNotificationAuditMapper extends BaseMapperPlus<ProfileNotificationAudit, ProfileNotificationAudit> {

    /**
     * 定义新增映射（insertNotificationAudit）。
     */
    int insertNotificationAudit(PersonNotificationAuditRow row);

    /**
     * 定义查询映射（selectRetryable）。
     */
    PersonNotificationAuditRow selectRetryable(@Param("notificationType") String notificationType,
                                               @Param("profileId") long profileId,
                                               @Param("applicationId") long applicationId,
                                               @Param("targetUserId") long targetUserId);

    /**
     * 定义加锁查询映射（lockRetryable）。
     */
    PersonNotificationAuditRow lockRetryable(@Param("auditId") long auditId);

    /**
     * 定义更新映射（updateDelivery）。
     */
    int updateDelivery(PersonNotificationAuditRow row);
}
