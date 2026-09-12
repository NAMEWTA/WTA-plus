package org.namewta.profile.enterprise.dao;

import java.time.Instant;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.enterprise.domain.ProfileEnterpriseTransferRecord;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseTransferOwnerRow;
import org.namewta.profile.enterprise.mapper.EnterpriseTransferMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 企业转移数据访问对象，统一封装转移查询条件和锁语义。
 *
 * <p>本能力的查询条件、锁语义和 Mapper 调用均收敛在此边界。</p>
 */
@RequiredArgsConstructor
@Repository
public class EnterpriseTransferDao {

    private final EnterpriseTransferMapper mapper;

    /**
     * 查询持久化数据（selectActiveOwner）。
     */
    public EnterpriseTransferOwnerRow selectActiveOwner(long userId) {
        return mapper.selectActiveOwner(userId);
    }

    /**
     * 加锁查询持久化数据（lockActiveOwner）。
     */
    public EnterpriseTransferOwnerRow lockActiveOwner(long userId) {
        return mapper.lockActiveOwner(userId);
    }

    /**
     * 统计持久化数据（countEffectiveBinding）。
     */
    public int countEffectiveBinding(long userId) {
        return mapper.countEffectiveBinding(userId);
    }

    /**
     * 新增持久化数据（insertTransferRecord）。
     */
    public int insertTransferRecord(long recordId, long profileId, long sourceBindingId, long sourceUserId, long targetUserId, String challengeId, int bindingVersion, Instant expiresTime, Instant occurredTime) {
        return mapper.insertTransferRecord(recordId, profileId, sourceBindingId, sourceUserId, targetUserId, challengeId, bindingVersion, expiresTime, occurredTime);
    }

    /**
     * 确认持久化数据（confirmTransferRecord）。
     */
    public int confirmTransferRecord(String challengeId, long profileId, long sourceBindingId, long sourceUserId, long targetUserId, int bindingVersion, Instant occurredTime, long operatorId) {
        return mapper.confirmTransferRecord(challengeId, profileId, sourceBindingId, sourceUserId, targetUserId, bindingVersion, occurredTime, operatorId);
    }

    /**
     * 加锁查询持久化数据（lockEffectiveBindingId）。
     */
    public Long lockEffectiveBindingId(long userId) {
        return mapper.lockEffectiveBindingId(userId);
    }

    /**
     * 解除绑定持久化数据（unbindSource）。
     */
    public int unbindSource(long bindingId, long profileId, long userId, int bindingVersion, Instant occurredTime, long operatorId) {
        return mapper.unbindSource(bindingId, profileId, userId, bindingVersion, occurredTime, operatorId);
    }

    /**
     * 新增持久化数据（insertBinding）。
     */
    public int insertBinding(long bindingId, long profileId, long userId, String sourceType, long sourceId, Instant occurredTime, long operatorId) {
        return mapper.insertBinding(bindingId, profileId, userId, sourceType, sourceId, occurredTime, operatorId);
    }

    /**
     * 新增持久化数据（insertEvent）。
     */
    public int insertEvent(long eventId, long bindingId, long profileId, long userId, String eventType, int bindingVersion, String sourceType, long sourceId, String reason, Instant occurredTime, long operatorId) {
        return mapper.insertEvent(eventId, bindingId, profileId, userId, eventType, bindingVersion, sourceType, sourceId, reason, occurredTime, operatorId);
    }
}
