package org.namewta.notify.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.*;
import org.namewta.notify.mapper.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** 通知运行时持久化边界，业务服务不得直接依赖 MyBatis Mapper。 */
@Repository
@RequiredArgsConstructor
public class NotifyNotificationDao {
    private final NotifyIntentMapper intentMapper;
    private final NotifyRecipientMapper recipientMapper;
    private final NotifyDeliveryMapper deliveryMapper;
    private final NotifyOutboxMapper outboxMapper;
    private final NotifyAttemptMapper attemptMapper;
    private final NotifyMessageMapper messageMapper;
    private final NotifyMessageRecipientMapper messageRecipientMapper;

    public NotifyIntent intent(Long id) { return intentMapper.selectById(id); }
    public NotifyIntent intentByIdempotency(String appId, String key) {
        return intentMapper.selectOne(new LambdaQueryWrapper<NotifyIntent>()
            .eq(NotifyIntent::getAppId, appId).eq(NotifyIntent::getIdempotencyKey, key).last("limit 1"));
    }
    public int insert(NotifyIntent value) { return intentMapper.insert(value); }
    public int update(NotifyIntent value) { return intentMapper.updateById(value); }
    public NotifyRecipient insert(NotifyRecipient value) { recipientMapper.insert(value); return value; }
    public int insert(NotifyDelivery value) { return deliveryMapper.insert(value); }
    public int update(NotifyDelivery value) { return deliveryMapper.updateById(value); }
    public NotifyDelivery delivery(Long id) { return deliveryMapper.selectById(id); }
    public NotifyDelivery deliveryByProvider(String channel, String providerKey, String providerMessageId) {
        return deliveryMapper.selectOne(new LambdaQueryWrapper<NotifyDelivery>()
            .eq(NotifyDelivery::getChannel, channel)
            .eq(NotifyDelivery::getProviderKey, providerKey)
            .eq(NotifyDelivery::getProviderMessageId, providerMessageId));
    }
    public List<NotifyDelivery> deliveries(Long intentId) {
        return deliveryMapper.selectList(new LambdaQueryWrapper<NotifyDelivery>().eq(NotifyDelivery::getIntentId, intentId));
    }
    public List<NotifyDelivery> monitorDeliveries(Long userId, String channel, String status, int limit) {
        return deliveryMapper.selectList(new LambdaQueryWrapper<NotifyDelivery>()
            .eq(userId != null, NotifyDelivery::getUserId, userId)
            .eq(channel != null && !channel.isBlank(), NotifyDelivery::getChannel, channel)
            .eq(status != null && !status.isBlank(), NotifyDelivery::getStatus, status)
            .orderByDesc(NotifyDelivery::getCreateTime).last("limit " + Math.clamp(limit, 1, 500)));
    }
    public List<NotifyDelivery> priorDeliveries(Long intentId, Long recipientId, Long deliveryId) {
        return deliveryMapper.selectList(new LambdaQueryWrapper<NotifyDelivery>()
            .eq(NotifyDelivery::getIntentId, intentId).eq(NotifyDelivery::getRecipientId, recipientId)
            .lt(NotifyDelivery::getDeliveryId, deliveryId).orderByAsc(NotifyDelivery::getDeliveryId));
    }
    public int updateDeliveryStatus(Long intentId, String from, String to) {
        return deliveryMapper.update(null, new LambdaUpdateWrapper<NotifyDelivery>()
            .eq(NotifyDelivery::getIntentId, intentId).eq(NotifyDelivery::getStatus, from)
            .set(NotifyDelivery::getStatus, to));
    }
    public int updateDeliveryStatus(Long id, String from, NotifyDelivery value) {
        return deliveryMapper.update(null, new LambdaUpdateWrapper<NotifyDelivery>()
            .eq(NotifyDelivery::getDeliveryId, id).eq(NotifyDelivery::getStatus, from)
            .set(NotifyDelivery::getStatus, value.getStatus())
            .set(NotifyDelivery::getAcceptedAt, value.getAcceptedAt())
            .set(NotifyDelivery::getDeliveredAt, value.getDeliveredAt()));
    }
    public int markDeliveryForRetry(Long deliveryId) {
        return deliveryMapper.update(null, new LambdaUpdateWrapper<NotifyDelivery>()
            .eq(NotifyDelivery::getDeliveryId, deliveryId)
            .in(NotifyDelivery::getStatus, List.of("FAILED", "UNKNOWN"))
            .set(NotifyDelivery::getStatus, "PENDING")
            .set(NotifyDelivery::getErrorCode, null)
            .set(NotifyDelivery::getErrorMessage, null));
    }
    public int insert(NotifyOutbox value) { return outboxMapper.insert(value); }
    public NotifyOutbox outbox(Long id) { return outboxMapper.selectById(id); }
    public List<NotifyOutbox> claimCandidates(LocalDateTime now, int limit) {
        return outboxMapper.selectClaimable(now, limit);
    }
    public int claimOutbox(Long id, String owner, String token, LocalDateTime until, LocalDateTime now) {
        return outboxMapper.claim(id, owner, token, until, now);
    }
    public int renewOutbox(Long id, String owner, String token, LocalDateTime until) {
        return outboxMapper.renew(id, owner, token, until, LocalDateTime.now());
    }
    public int finishOutbox(NotifyOutbox value) {
        return outboxMapper.finish(value.getOutboxId(), value.getLeaseOwner(), value.getLeaseToken(), value.getStatus(),
            value.getAttemptCount(), value.getNextAttemptAt(), value.getLastErrorCode(), value.getLastErrorMessage());
    }
    public int requeueOutbox(Long deliveryId, LocalDateTime now) { return outboxMapper.requeue(deliveryId, now); }
    public int insert(NotifyAttempt value) { return attemptMapper.insert(value); }
    public List<NotifyMessageRecipient> messageRecipients(Long userId, int limit) {
        return messageRecipientMapper.selectList(new LambdaQueryWrapper<NotifyMessageRecipient>()
            .eq(NotifyMessageRecipient::getUserId, userId)
            .orderByDesc(NotifyMessageRecipient::getCreateTime)
            .orderByDesc(NotifyMessageRecipient::getMessageId)
            .last("limit " + Math.clamp(limit, 1, 500)));
    }
    public List<NotifyMessage> messages(Collection<Long> ids) { return messageMapper.selectBatchIds(ids); }
    public NotifyMessage message(Long id) { return messageMapper.selectById(id); }
    public int insert(NotifyMessage value) { return messageMapper.insert(value); }
    public boolean recipientExists(Long messageId, Long userId) {
        return messageRecipientMapper.selectCount(new LambdaQueryWrapper<NotifyMessageRecipient>()
            .eq(NotifyMessageRecipient::getMessageId, messageId).eq(NotifyMessageRecipient::getUserId, userId)) > 0;
    }
    public int insert(NotifyMessageRecipient value) { return messageRecipientMapper.insert(value); }
    public NotifyMessageRecipient messageRecipient(Long messageId, Long userId) {
        return messageRecipientMapper.selectOne(new LambdaQueryWrapper<NotifyMessageRecipient>()
            .eq(NotifyMessageRecipient::getMessageId, messageId).eq(NotifyMessageRecipient::getUserId, userId));
    }
    public int update(NotifyMessageRecipient value) { return messageRecipientMapper.updateById(value); }
    public int markAllMessages(Long userId, LocalDateTime now) {
        return messageRecipientMapper.update(null, new LambdaUpdateWrapper<NotifyMessageRecipient>()
            .eq(NotifyMessageRecipient::getUserId, userId)
            .and(wrapper -> wrapper.isNull(NotifyMessageRecipient::getSeenTime).or().isNull(NotifyMessageRecipient::getReadTime))
            .set(NotifyMessageRecipient::getSeenTime, now).set(NotifyMessageRecipient::getReadTime, now));
    }
}
