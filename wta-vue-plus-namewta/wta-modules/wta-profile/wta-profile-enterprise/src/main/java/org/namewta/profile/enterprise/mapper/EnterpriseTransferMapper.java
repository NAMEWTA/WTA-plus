package org.namewta.profile.enterprise.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.enterprise.domain.ProfileEnterpriseTransferRecord;

import org.namewta.profile.enterprise.domain.model.read.EnterpriseTransferOwnerRow;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

/**
 * EnterpriseTransferMapper 持久化映射器，负责本能力的数据映射。
 */
public interface EnterpriseTransferMapper
    extends BaseMapperPlus<ProfileEnterpriseTransferRecord, ProfileEnterpriseTransferRecord> {

    /**
     * 定义查询映射（selectActiveOwner）。
     */
    EnterpriseTransferOwnerRow selectActiveOwner(@Param("userId") long userId);

    /**
     * 定义加锁查询映射（lockActiveOwner）。
     */
    EnterpriseTransferOwnerRow lockActiveOwner(@Param("userId") long userId);

    /**
     * 定义统计映射（countEffectiveBinding）。
     */
    int countEffectiveBinding(@Param("userId") long userId);

    /**
     * 定义新增映射（insertTransferRecord）。
     */
    int insertTransferRecord(@Param("recordId") long recordId,
                             @Param("profileId") long profileId,
                             @Param("sourceBindingId") long sourceBindingId,
                             @Param("sourceUserId") long sourceUserId,
                             @Param("targetUserId") long targetUserId,
                             @Param("challengeId") String challengeId,
                             @Param("bindingVersion") int bindingVersion,
                             @Param("expiresTime") Instant expiresTime,
                             @Param("occurredTime") Instant occurredTime);

    /**
     * 定义确认映射（confirmTransferRecord）。
     */
    int confirmTransferRecord(@Param("challengeId") String challengeId,
                              @Param("profileId") long profileId,
                              @Param("sourceBindingId") long sourceBindingId,
                              @Param("sourceUserId") long sourceUserId,
                              @Param("targetUserId") long targetUserId,
                              @Param("bindingVersion") int bindingVersion,
                              @Param("occurredTime") Instant occurredTime,
                              @Param("operatorId") long operatorId);

    /**
     * 定义加锁查询映射（lockEffectiveBindingId）。
     */
    Long lockEffectiveBindingId(@Param("userId") long userId);

    /**
     * 定义解除绑定映射（unbindSource）。
     */
    int unbindSource(@Param("bindingId") long bindingId,
                     @Param("profileId") long profileId,
                     @Param("userId") long userId,
                     @Param("bindingVersion") int bindingVersion,
                     @Param("occurredTime") Instant occurredTime,
                     @Param("operatorId") long operatorId);

    /**
     * 定义新增映射（insertBinding）。
     */
    int insertBinding(@Param("bindingId") long bindingId,
                      @Param("profileId") long profileId,
                      @Param("userId") long userId,
                      @Param("sourceType") String sourceType,
                      @Param("sourceId") long sourceId,
                      @Param("occurredTime") Instant occurredTime,
                      @Param("operatorId") long operatorId);

    /**
     * 定义新增映射（insertEvent）。
     */
    int insertEvent(@Param("eventId") long eventId,
                    @Param("bindingId") long bindingId,
                    @Param("profileId") long profileId,
                    @Param("userId") long userId,
                    @Param("eventType") String eventType,
                    @Param("bindingVersion") int bindingVersion,
                    @Param("sourceType") String sourceType,
                    @Param("sourceId") long sourceId,
                    @Param("reason") String reason,
                    @Param("occurredTime") Instant occurredTime,
                    @Param("operatorId") long operatorId);
}
