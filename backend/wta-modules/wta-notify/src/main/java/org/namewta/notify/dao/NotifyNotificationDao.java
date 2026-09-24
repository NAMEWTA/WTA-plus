package org.namewta.notify.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.mybatis.core.query.LambdaJoinQueryBuilder;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.notify.domain.entity.*;
import org.namewta.notify.domain.model.read.NotifyInboxRow;
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
    private final NotifyIntentAttachmentMapper attachmentMapper;

    /** 附件顺序和归属由物理关系行决定；空集合永不退化为全表扫描。 */
    public List<NotifyIntentAttachment> attachments(Long intentId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<NotifyIntentAttachment>()
            .eq(NotifyIntentAttachment::getIntentId, intentId)
            .eq(NotifyIntentAttachment::getDelFlag, "0")
            .orderByAsc(NotifyIntentAttachment::getPosition));
    }

    /** 已锁定 Intent 后按关系 PK 顺序取得当前行，用于复制权和安全回收。 */
    public List<NotifyIntentAttachment> lockAttachments(Long intentId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<NotifyIntentAttachment>()
            .eq(NotifyIntentAttachment::getIntentId, intentId)
            .eq(NotifyIntentAttachment::getDelFlag, "0")
            .orderByAsc(NotifyIntentAttachment::getIntentAttachmentId).last("for update"));
    }

    /** 幂等唯一键竞争后按提交顺序当前读关系，不能沿用 REPEATABLE READ 旧快照。 */
    public List<NotifyIntentAttachment> lockAttachmentsByPosition(Long intentId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<NotifyIntentAttachment>()
            .eq(NotifyIntentAttachment::getIntentId, intentId)
            .eq(NotifyIntentAttachment::getDelFlag, "0")
            .orderByAsc(NotifyIntentAttachment::getPosition).last("for update"));
    }

    public int insert(NotifyIntentAttachment value) { return attachmentMapper.insert(value); }

    public int saveAttachment(NotifyIntentAttachment value) {
        return attachmentMapper.updateById(value);
    }

    /** 后台回收每轮只读有限候选，以主键游标遍历而不扫描全表后截断。 */
    public List<NotifyIntentAttachment> attachmentReleaseCandidates(long afterId, int limit) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<NotifyIntentAttachment>()
            .gt(NotifyIntentAttachment::getIntentAttachmentId, afterId)
            .in(NotifyIntentAttachment::getStatus, "QUEUED", "READY")
            .eq(NotifyIntentAttachment::getDelFlag, "0")
            .orderByAsc(NotifyIntentAttachment::getIntentAttachmentId)
            .last("limit " + Math.clamp(limit, 1, 100)));
    }

    /** 唯一键冲突后的幂等出口必须使用当前读，不能复用事务旧快照。 */
    public NotifyIntent lockIntentByIdempotency(String appId, String key) {
        return intentMapper.selectOne(new LambdaQueryWrapper<NotifyIntent>()
            .eq(NotifyIntent::getAppId, appId).eq(NotifyIntent::getIdempotencyKey, key)
            .last("for update"));
    }

    public NotifyIntent intent(Long id) { return intentMapper.selectById(id); }
    /** 仅按本次有界投递结果的 ID 批量取策略；空集合绝不可退化为全表查询。 */
    public List<NotifyIntent> intents(Collection<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of() : intentMapper.selectBatchIds(ids);
    }
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
    /** 人工重试也遵守 Intent→Outbox→Delivery 锁序；同一投递的所有旧任务均在锁内核对。 */
    public List<NotifyOutbox> lockOutboxes(Long intentId, Collection<Long> deliveryIds) {
        if (deliveryIds == null || deliveryIds.isEmpty()) return List.of();
        return outboxMapper.selectList(new LambdaQueryWrapper<NotifyOutbox>()
            .eq(NotifyOutbox::getIntentId, intentId)
            .in(NotifyOutbox::getDeliveryId, deliveryIds)
            .orderByAsc(NotifyOutbox::getOutboxId).last("for update"));
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
    /** 公告先锁 Notice，再只锁该发布版本的 Intent；不得由 Worker 反向取 Notice 锁。 */
    public NotifyIntent lockNoticeIntent(String key) {
        return intentMapper.selectOne(new LambdaQueryWrapper<NotifyIntent>()
            .eq(NotifyIntent::getAppId, "notify").eq(NotifyIntent::getIdempotencyKey, key)
            .last("for update"));
    }
    /** 固定 DML 静态公告的缺 Intent 例外还须排除相同业务公告的其他任务。 */
    public long countNoticeBusinessIntents(Long noticeId) {
        return intentMapper.selectCount(new LambdaQueryWrapper<NotifyIntent>()
            .eq(NotifyIntent::getAppId, "notify").eq(NotifyIntent::getBizType, "NOTICE_PUBLISHED")
            .eq(NotifyIntent::getBizId, String.valueOf(noticeId)));
    }
    /** 持有精确版本 Intent 锁时只写版本栅栏，不覆盖结果事务可能更新的聚合列。 */
    public int saveNoticeMetadata(NotifyIntent intent, String metadataJson) {
        return intentMapper.update(null, new LambdaUpdateWrapper<NotifyIntent>()
            .eq(NotifyIntent::getIntentId, intent.getIntentId())
            .eq(NotifyIntent::getAppId, "notify").eq(NotifyIntent::getIdempotencyKey, intent.getIdempotencyKey())
            .set(NotifyIntent::getMetadataJson, metadataJson));
    }
    /** 发布事务内同时改路径快照与外部模板变量；整笔事务失败则三处均回滚。 */
    public int updateNoticePath(NotifyIntent intent, String templateParamsJson, String path) {
        return intentMapper.update(null, new LambdaUpdateWrapper<NotifyIntent>()
            .eq(NotifyIntent::getIntentId, intent.getIntentId())
            .eq(NotifyIntent::getAppId, "notify").eq(NotifyIntent::getIdempotencyKey, intent.getIdempotencyKey())
            .eq(NotifyIntent::getPathSnapshot, "/notify/inbox")
            .set(NotifyIntent::getPathSnapshot, path)
            .set(NotifyIntent::getTemplateParamsJson, templateParamsJson));
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
    public int markDeliveryForRetry(Long intentId, Long deliveryId, String status, String errorCode) {
        return deliveryMapper.update(null, new LambdaUpdateWrapper<NotifyDelivery>()
            .eq(NotifyDelivery::getDeliveryId, deliveryId)
            .eq(NotifyDelivery::getIntentId, intentId)
            .eq(NotifyDelivery::getStatus, status)
            .eq(NotifyDelivery::getErrorCode, errorCode)
            .isNull(NotifyDelivery::getProviderMessageId)
            .set(NotifyDelivery::getStatus, "PENDING")
            .set(NotifyDelivery::getErrorCode, null)
            .set(NotifyDelivery::getErrorMessage, null));
    }
    public int insert(NotifyOutbox value) { return outboxMapper.insert(value); }
    public NotifyOutbox outbox(Long id) { return outboxMapper.selectById(id); }
    public List<NotifyOutbox> claimCandidates(LocalDateTime now, int limit) {
        return outboxMapper.selectClaimable(now, limit);
    }
    /** 同一批候选仅一次普通读获取不可变渠道，claim UPDATE 无需在锁住 Outbox 后再读 Delivery。 */
    public java.util.Map<Long, String> deliveryChannels(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Map.of();
        return deliveryMapper.selectBatchIds(ids).stream().collect(java.util.stream.Collectors.toMap(
            NotifyDelivery::getDeliveryId, NotifyDelivery::getChannel));
    }
    public int claimOutbox(Long id, String owner, String token, LocalDateTime until, LocalDateTime now,
                           boolean inApp) {
        return outboxMapper.claim(id, owner, token, until, now, inApp);
    }

    /** 已持有统一锁序时，按当前 owner/token 预留站内投递预算。 */
    public int reserveInAppOutbox(Long id, String owner, String token) {
        return outboxMapper.reserveInApp(id, owner, token);
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
    public int requeueOutbox(NotifyOutbox outbox, LocalDateTime now) {
        return outboxMapper.requeue(outbox.getOutboxId(), outbox.getIntentId(), outbox.getDeliveryId(),
            outbox.getStatus(), outbox.getLastErrorCode(), now);
    }
    public int insert(NotifyAttempt value) { return attemptMapper.insert(value); }

    /** 同一有效本人收件集合的总数；孤儿关系不计入公开页。 */
    public long inboxTotal(Long userId) {
        return messageRecipientMapper.selectJoinCount(inboxJoin(userId).build());
    }

    /** 全量未读计数使用与列表相同的本人消息联表条件。 */
    public long inboxUnreadTotal(Long userId) {
        var query = inboxJoin(userId).build();
        query.isNull("r", NotifyMessageRecipient::getReadTime);
        return messageRecipientMapper.selectJoinCount(query);
    }

    /** 页码先按 long 偏移与总数比较，避免极大页码溢出或回卷第一页。 */
    public List<NotifyInboxRow> inboxRows(Long userId, int pageNum, int pageSize, long total) {
        long offset = ((long) pageNum - 1) * pageSize;
        if (offset >= total) return List.of();
        var page = new PageQuery(pageSize, pageNum).<NotifyInboxRow>build();
        page.setSearchCount(false);
        return messageRecipientMapper.selectJoinPage(page, NotifyInboxRow.class, inboxProjection(userId, false)
            .orderByDesc("r", NotifyMessageRecipient::getCreateTime)
            .orderByDesc("r", NotifyMessageRecipient::getMessageId).build()).getRecords();
    }

    /** 详情通过同一条本人 JOIN 获取正文，不先读取他人的消息再判断归属。 */
    public NotifyInboxRow inboxDetail(Long userId, Long messageId) {
        return messageRecipientMapper.selectJoinOne(NotifyInboxRow.class, inboxProjection(userId, true)
            .eq("r", NotifyMessageRecipient::getMessageId, messageId).build());
    }

    private LambdaJoinQueryBuilder<NotifyMessageRecipient> inboxJoin(Long userId) {
        return QueryBuilder.lambdaJoin("r", NotifyMessageRecipient.class)
            .leftJoin(NotifyMessage.class, "m", NotifyMessage::getMessageId, NotifyMessageRecipient::getMessageId)
            .isNotNull("m", NotifyMessage::getMessageId)
            .eq("r", NotifyMessageRecipient::getUserId, userId);
    }

    private LambdaJoinQueryBuilder<NotifyMessageRecipient> inboxProjection(Long userId, boolean detail) {
        var query = inboxJoin(userId)
            .select("m", NotifyMessage::getMessageId, NotifyMessage::getCategory,
                NotifyMessage::getNoticeType, NotifyMessage::getChannelsJson, NotifyMessage::getType,
                NotifyMessage::getSource, NotifyMessage::getTitle, NotifyMessage::getMessage,
                NotifyMessage::getPath)
            .select("r", NotifyMessageRecipient::getSeenTime, NotifyMessageRecipient::getReadTime)
            .selectAs("r", NotifyMessageRecipient::getCreateTime, NotifyInboxRow::getCreateTime);
        if (detail) query.select("m", NotifyMessage::getContent);
        return query;
    }

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
    /** 单条互动时间只填首次空值；数据库条件更新避免并发读改写覆盖首次时间。 */
    public int markMessage(Long messageId, Long userId, boolean read, LocalDateTime now) {
        var update = new LambdaUpdateWrapper<NotifyMessageRecipient>()
            .eq(NotifyMessageRecipient::getMessageId, messageId)
            .eq(NotifyMessageRecipient::getUserId, userId);
        if (read) {
            update.and(wrapper -> wrapper.isNull(NotifyMessageRecipient::getSeenTime)
                .or().isNull(NotifyMessageRecipient::getReadTime))
                .setSql("seen_time = COALESCE(seen_time, {0}), read_time = COALESCE(read_time, {1})", now, now);
        } else {
            update.isNull(NotifyMessageRecipient::getSeenTime)
                .setSql("seen_time = COALESCE(seen_time, {0})", now);
        }
        return messageRecipientMapper.update(null, update);
    }

    /** 本人全部收件关系在同一 SQL 内补齐空时间，重复调用保留首次时间。 */
    public int markAllMessages(Long userId, LocalDateTime now) {
        return messageRecipientMapper.update(null, new LambdaUpdateWrapper<NotifyMessageRecipient>()
            .eq(NotifyMessageRecipient::getUserId, userId)
            .and(wrapper -> wrapper.isNull(NotifyMessageRecipient::getSeenTime).or().isNull(NotifyMessageRecipient::getReadTime))
            .setSql("seen_time = COALESCE(seen_time, {0}), read_time = COALESCE(read_time, {1})", now, now));
    }
}
