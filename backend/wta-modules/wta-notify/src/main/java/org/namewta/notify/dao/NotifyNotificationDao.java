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
    /**
     * 在动态事务内锁定聚合根；所有结果、回调、取消和重试先取得此锁。
     * @param id 通知意图主键
     * @return 当前意图；不存在时为 null
     */
    public NotifyIntent lockIntent(Long id) {
        return intentMapper.selectOne(new LambdaQueryWrapper<NotifyIntent>().eq(NotifyIntent::getIntentId, id).last("for update"));
    }
    /**
     * 在 Intent 锁之后取得任务锁。
     * @param id Outbox 主键
     * @return 当前任务；不存在时为 null
     */
    public NotifyOutbox lockOutbox(Long id) {
        return outboxMapper.selectOne(new LambdaQueryWrapper<NotifyOutbox>().eq(NotifyOutbox::getOutboxId, id).last("for update"));
    }
    /**
     * 当前读投递行，不能在等待 Intent 锁后继续使用旧快照。
     * @param id 投递主键
     * @return 当前投递；不存在时为 null
     */
    public NotifyDelivery lockDelivery(Long id) {
        return deliveryMapper.selectOne(new LambdaQueryWrapper<NotifyDelivery>().eq(NotifyDelivery::getDeliveryId, id).last("for update"));
    }
    /**
     * 聚合必须使用锁定当前读，避免同一通知不同 delivery 的并发结果丢失。
     * @param intentId 已锁定的通知意图主键
     * @return 按主键排序的当前投递行
     */
    public List<NotifyDelivery> lockDeliveries(Long intentId) {
        return deliveryMapper.selectList(new LambdaQueryWrapper<NotifyDelivery>().eq(NotifyDelivery::getIntentId, intentId)
            .orderByAsc(NotifyDelivery::getDeliveryId).last("for update"));
    }
    /** 所有租约比较使用数据库 UTC 时钟。
     * @return 当前数据库 UTC 时间
     */
    public LocalDateTime databaseNow() { return outboxMapper.databaseNow(); }
    /**
     * 显式写回可空结果字段，成功结果可清除旧错误，不覆盖目标和归属。
     * @param value 已锁定投递的结果及计数
     * @return 更新行数，调用方须将非 1 视为事务失败
     */
    public int saveDeliveryResult(NotifyDelivery value) {
        return deliveryMapper.update(null, new LambdaUpdateWrapper<NotifyDelivery>()
            .eq(NotifyDelivery::getDeliveryId, value.getDeliveryId())
            .eq(NotifyDelivery::getIntentId, value.getIntentId())
            .set(NotifyDelivery::getStatus, value.getStatus()).set(NotifyDelivery::getAttemptCount, value.getAttemptCount())
            .set(NotifyDelivery::getProviderKey, value.getProviderKey()).set(NotifyDelivery::getProviderMessageId, value.getProviderMessageId())
            .set(NotifyDelivery::getErrorCode, value.getErrorCode()).set(NotifyDelivery::getErrorMessage, value.getErrorMessage())
            .set(NotifyDelivery::getAcceptedAt, value.getAcceptedAt()).set(NotifyDelivery::getDeliveredAt, value.getDeliveredAt()));
    }

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
    /** 原生查询候选；启用账号及71小时窗口由XML控制，不扫描邮件/缺流水号投递。 */
    public NotifyDelivery smsReceiptCandidate(LocalDateTime now) { return deliveryMapper.selectSmsReceiptCandidate(now); }
    /**
     * CAS预约下次核对；其他节点不能同时领取相同的到期快照。
     * 不修改status、attempt或Outbox，失败后预约到期即可再次读取供应商状态。
     */
    public int reserveSmsReceiptQuery(NotifyDelivery value, LocalDateTime next) {
        var update = new LambdaUpdateWrapper<NotifyDelivery>()
            .eq(NotifyDelivery::getDeliveryId, value.getDeliveryId()).eq(NotifyDelivery::getStatus, "ACCEPTED")
            .eq(NotifyDelivery::getProviderKey, value.getProviderKey())
            .eq(NotifyDelivery::getProviderMessageId, value.getProviderMessageId());
        if (value.getReceiptQueryAt() == null) update.isNull(NotifyDelivery::getReceiptQueryAt);
        else update.eq(NotifyDelivery::getReceiptQueryAt, value.getReceiptQueryAt());
        return deliveryMapper.update(null, update.set(NotifyDelivery::getReceiptQueryAt, next));
    }
    /** 最多返回两条候选，调用方必须拒绝未指明收件人或历史消息重用导致的歧义。 */
    public List<NotifyDelivery> deliveriesByProvider(String channel, String providerKey, String providerMessageId, String target) {
        return deliveryMapper.selectList(new LambdaQueryWrapper<NotifyDelivery>()
            .eq(NotifyDelivery::getChannel, channel)
            .eq(NotifyDelivery::getProviderKey, providerKey)
            .eq(NotifyDelivery::getProviderMessageId, providerMessageId)
            .eq(target != null && !target.isEmpty(), NotifyDelivery::getTargetValue, target)
            .orderByAsc(NotifyDelivery::getDeliveryId).last("limit 2"));
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
    /**
     * 活租约才可续期；调用方先持有行锁并核对数据库时间，防止等锁期间过期。
     * @param id 已锁定的任务主键
     * @param owner 本次领取者
     * @param token 本次领取的 fencing token
     * @return 成功续期为 1，失效租约为 0
     */
    public int renewOutbox(Long id, String owner, String token) {
        return outboxMapper.renew(id, owner, token);
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
