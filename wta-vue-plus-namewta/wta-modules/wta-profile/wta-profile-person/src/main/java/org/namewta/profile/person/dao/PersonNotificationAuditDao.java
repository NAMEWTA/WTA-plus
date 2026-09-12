package org.namewta.profile.person.dao;

import java.time.Instant;
import org.namewta.profile.person.domain.ProfileNotificationAudit;
import org.namewta.profile.person.domain.model.read.PersonNotificationAuditRow;
import org.namewta.profile.person.mapper.PersonNotificationAuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 个人通知审计数据访问对象，统一封装审计记录查询和锁语义。
 *
 * <p>本能力的查询条件、锁语义和 Mapper 调用均收敛在此边界。</p>
 */
@RequiredArgsConstructor
@Repository
public class PersonNotificationAuditDao {

    private final PersonNotificationAuditMapper mapper;

    /**
     * 新增持久化数据（insertNotificationAudit）。
     */
    public int insertNotificationAudit(PersonNotificationAuditRow row) {
        return mapper.insertNotificationAudit(row);
    }

    /**
     * 查询持久化数据（selectRetryable）。
     */
    public PersonNotificationAuditRow selectRetryable(String notificationType, long profileId, long applicationId, long targetUserId) {
        return mapper.selectRetryable(notificationType, profileId, applicationId, targetUserId);
    }

    /**
     * 加锁查询持久化数据（lockRetryable）。
     */
    public PersonNotificationAuditRow lockRetryable(long auditId) {
        return mapper.lockRetryable(auditId);
    }

    /**
     * 更新持久化数据（updateDelivery）。
     */
    public int updateDelivery(PersonNotificationAuditRow row) {
        return mapper.updateDelivery(row);
    }
}
