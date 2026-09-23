package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.notify.api.*;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.entity.NotifyRecipient;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.support.outbox.NotifyOutboxWakeRequestedEvent;
import org.namewta.notify.support.NotifyAuditSupport;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 统一通知应用服务实现。
 *
 * <p>提交阶段只写业务事实和 Outbox，不执行 Provider I/O，保证业务事务可恢复。</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationApplicationRuntimeService {
    private static final Set<String> PRE_SEND_FAILURES = Set.of(
        "UNBOUND_CHANNEL", "ACCOUNT_DISABLED", "ACCOUNT_CHANNEL_MISMATCH", "MISSING_VARIABLE",
        "ACCOUNT_QUOTA", "TEMPLATE_QUOTA", "RECIPIENT_MINUTE_QUOTA", "RECIPIENT_DAY_QUOTA");
    private static final Set<String> SMS_PRE_SEND_FAILURES = Set.of(
        "SMS_TEMPLATE_MISSING", "INVALID_TEMPLATE_PARAMETERS");

    private final NotifyNotificationDao dao;
    private final UserService userService;
    private final DispatchNotificationService dispatchService;
    private final ApplicationEventPublisher events;

    public NotificationReceipt submit(NotificationCommand command) {
        validate(command);
        NotifyIntent duplicated = findDuplicate(command);
        if (duplicated != null) {
            requireUnexpired(duplicated);
            return receipt(duplicated, dao.deliveries(duplicated.getIntentId()));
        }

        dispatchService.validateSubmission(command);

        List<ResolvedRecipient> users = resolveUsers(command).stream().distinct().toList();
        if (users.isEmpty()) throw new ServiceException("所选范围没有可接收通知的正常用户");
        long intentId = IdGeneratorUtil.nextLongId();
        NotifyIntent intent = new NotifyIntent();
        intent.setIntentId(intentId);
        intent.setAppId(command.appId());
        intent.setSceneCode(command.sceneCode());
        intent.setBizType(command.bizType());
        intent.setBizId(command.bizId());
        intent.setTemplateCode(command.templateCode());
        intent.setTemplateParamsJson(JsonUtils.toJsonString(command.templateParams()));
        intent.setStrategy(command.strategy().name());
        intent.setMode(command.mode().name());
        intent.setPriority(command.priority());
        intent.setScheduledAt(toLocal(ceilSecond(command.scheduledAt())));
        intent.setExpiresAt(toLocal(floorSecond(command.expiresAt())));
        intent.setIdempotencyKey(command.idempotencyKey());
        intent.setStatus(NotificationStatus.QUEUED.name());
        intent.setTitleSnapshot(stringValue(command.templateParams(), "title", command.templateCode()));
        intent.setContentSnapshot(stringValue(command.templateParams(), "content", command.templateCode()));
        intent.setPathSnapshot(stringValue(command.templateParams(), "path", null));
        intent.setMetadataJson(JsonUtils.toJsonString(command.metadata()));
        intent.setVersion(0);
        // 解析收件人/配置可能耗时，提交前再用数据库时钟核对同一持久截止。
        requireUnexpired(intent);
        try {
            dao.insert(intent);
        } catch (DuplicateKeyException duplicate) {
            NotifyIntent existing = findDuplicate(command);
            if (existing != null) {
                requireUnexpired(existing);
                return receipt(existing, dao.deliveries(existing.getIntentId()));
            }
            throw duplicate;
        }

        List<NotifyDelivery> deliveries = new ArrayList<>();
        List<NotifyOutbox> outboxes = new ArrayList<>();
        // 秒精度列可能向上舍入；立即任务取数据库当前整秒，保证提交后的首次 wake 已可 claim。
        LocalDateTime availableAt = command.scheduledAt() == null
            ? dao.databaseNow().withNano(0) : intent.getScheduledAt();
        for (ResolvedRecipient user : users) {
            NotifyRecipient recipient = new NotifyRecipient();
            recipient.setRecipientId(IdGeneratorUtil.nextLongId());
            recipient.setIntentId(intentId);
            recipient.setRecipientType(command.recipientType());
            recipient.setRecipientKey(user.key());
            recipient.setUserId(user.userId());
            recipient.setTargetSnapshotJson(JsonUtils.toJsonString(Map.of(
                "phone", Objects.toString(user.phone(), ""),
                "email", Objects.toString(user.email(), ""))));
            recipient.setStatus("ACTIVE");
            dao.insert(recipient);
            for (NotificationChannel channel : command.channels()) {
                NotifyDelivery delivery = new NotifyDelivery();
                delivery.setDeliveryId(IdGeneratorUtil.nextLongId());
                delivery.setIntentId(intentId);
                delivery.setRecipientId(recipient.getRecipientId());
                delivery.setUserId(user.userId());
                delivery.setChannel(channel.name());
                String targetValue = targetValue(channel, user);
                delivery.setTargetValue(targetValue);
                delivery.setStatus(blank(targetValue) ? "UNDELIVERABLE" : "PENDING");
                delivery.setAttemptCount(0);
                delivery.setVersion(0);
                dao.insert(delivery);
                deliveries.add(delivery);

                if (!"PENDING".equals(delivery.getStatus())) {
                    delivery.setErrorCode("TARGET_UNAVAILABLE");
                    delivery.setErrorMessage("通知目标缺少有效联系方式");
                    dao.update(delivery);
                    continue;
                }

                NotifyOutbox outbox = new NotifyOutbox();
                outbox.setOutboxId(IdGeneratorUtil.nextLongId());
                outbox.setIntentId(intentId);
                outbox.setDeliveryId(delivery.getDeliveryId());
                outbox.setStatus("READY");
                outbox.setAvailableAt(availableAt);
                outbox.setAttemptCount(0);
                outbox.setNextAttemptAt(outbox.getAvailableAt());
                outbox.setMaxAttempts(5);
                if (channel != NotificationChannel.IN_APP) {
                    outbox.setLastErrorCode(NotifyOutbox.DEADLINE_UNSENT_READY);
                }
                dao.insert(outbox);
                outboxes.add(outbox);
            }
        }
        if (outboxes.isEmpty()) {
            dispatchService.refreshAggregate(intentId);
        } else {
            requestOutboxWake(outboxes.getFirst().getOutboxId());
        }
        return receipt(dao.intent(intentId), dao.deliveries(intentId));
    }

    public NotificationSnapshot query(NotificationQuery query) {
        if (query == null || query.notificationId() == null) {
            throw new ServiceException("通知编号不能为空");
        }
        Long notificationId = parsePositiveId(query.notificationId());
        NotifyIntent intent = dao.intent(notificationId);
        if (intent == null) {
            throw new ServiceException("通知不存在");
        }
        List<NotifyDelivery> deliveries = dao.deliveries(intent.getIntentId());
        return new NotificationSnapshot(String.valueOf(intent.getIntentId()),
            status(intent.getStatus()), parseTime(intent.getCreateTime()),
            deliveries.stream().map(item -> new NotificationReceipt.DeliveryReceipt(
                item.getUserId() == null ? null : String.valueOf(item.getUserId()), NotificationChannel.valueOf(item.getChannel()),
                status(item.getStatus()), NotifyAuditSupport.publicProviderMessageId(intent, item.getProviderMessageId()))).toList());
    }

    public RetryReceipt retry(NotificationRetryCommand command) {
        if (command == null || command.notificationId() == null) {
            throw new ServiceException("通知编号不能为空");
        }
        if (!blank(command.idempotencyKey())) {
            throw new ServiceException("人工重试暂不支持幂等键");
        }
        Long deliveryId = command.deliveryId() == null ? null : parsePositiveId(command.deliveryId());
        NotifyIntent intent = dao.lockIntent(parsePositiveId(command.notificationId()));
        if (intent == null) {
            throw new ServiceException("通知不存在");
        }
        requireUnexpired(intent);
        // 先按不可变归属读取 ID，再按 Intent→Outbox→Delivery 锁序取得当前行；不能先锁 Delivery。
        if (deliveryId != null) {
            NotifyDelivery selected = dao.delivery(deliveryId);
            if (selected == null || !Objects.equals(selected.getIntentId(), intent.getIntentId())) {
                throw new ServiceException("投递不属于当前通知");
            }
        }
        if ("CANCELLED".equals(intent.getStatus()) || "EXPIRED".equals(intent.getStatus())
            || "DELIVERED".equals(intent.getStatus())) {
            return new RetryReceipt(String.valueOf(intent.getIntentId()), status(intent.getStatus()), 0);
        }
        List<Long> deliveryIds = (deliveryId == null ? dao.deliveries(intent.getIntentId()).stream()
            .map(NotifyDelivery::getDeliveryId).sorted().toList() : List.of(deliveryId));
        List<NotifyOutbox> outboxes = new ArrayList<>();
        for (int offset = 0; offset < deliveryIds.size(); offset += 500) {
            outboxes.addAll(dao.lockOutboxes(intent.getIntentId(),
                deliveryIds.subList(offset, Math.min(offset + 500, deliveryIds.size()))));
        }
        NotifyDelivery lockedTarget = deliveryId == null ? null : dao.lockDelivery(deliveryId);
        if (deliveryId != null && lockedTarget == null) throw new ServiceException("投递不属于当前通知");
        List<NotifyDelivery> deliveries = deliveryId == null ? dao.lockDeliveries(intent.getIntentId())
            : List.of(lockedTarget);
        if (deliveryId != null && !Objects.equals(deliveries.getFirst().getIntentId(), intent.getIntentId())) {
            throw new ServiceException("投递不属于当前通知");
        }
        Map<Long, List<NotifyOutbox>> byDelivery = new HashMap<>();
        for (NotifyOutbox outbox : outboxes) {
            byDelivery.computeIfAbsent(outbox.getDeliveryId(), ignored -> new ArrayList<>()).add(outbox);
        }
        List<RetryCandidate> eligible = new ArrayList<>();
        for (NotifyDelivery delivery : deliveries.stream().sorted(Comparator.comparing(NotifyDelivery::getDeliveryId)).toList()) {
            if ("UNKNOWN".equals(delivery.getStatus()) && !"IN_APP".equals(delivery.getChannel())) {
                throw new ServiceException("外部投递结果未知，不能重新发送");
            }
            List<NotifyOutbox> tasks = byDelivery.getOrDefault(delivery.getDeliveryId(), List.of());
            if (tasks.size() != 1 || delivery.getProviderMessageId() != null || delivery.getErrorCode() == null) continue;
            NotifyOutbox task = tasks.getFirst();
            if (!Objects.equals(task.getIntentId(), intent.getIntentId())
                || !Objects.equals(task.getDeliveryId(), delivery.getDeliveryId())
                || !Objects.equals(task.getLastErrorCode(), delivery.getErrorCode())
                || task.getLeaseOwner() != null || task.getLeaseToken() != null || task.getLeaseUntil() != null
                || task.getAttemptCount() == null || task.getMaxAttempts() == null
                || task.getAttemptCount() < 0 || task.getMaxAttempts() <= 0
                || task.getAttemptCount() >= task.getMaxAttempts()) continue;
            if ("FAILED".equals(delivery.getStatus()) && "DONE".equals(task.getStatus())
                && safeLocalFailure(delivery)) {
                eligible.add(new RetryCandidate(delivery, task));
            } else if ("IN_APP".equals(delivery.getChannel()) && "UNKNOWN".equals(delivery.getStatus())
                && "DISPATCH_ERROR".equals(delivery.getErrorCode())
                && "WAITING_RECEIPT".equals(task.getStatus()) && safeInAppRetry(intent, delivery)) {
                eligible.add(new RetryCandidate(delivery, task));
            }
        }
        int queuedCount = 0;
        Long wakeHint = null;
        if (!eligible.isEmpty()) {
            LocalDateTime now = dao.databaseNow().withNano(0);
            for (RetryCandidate candidate : eligible) {
                if (dao.requeueOutbox(candidate.outbox(), now) != 1
                    || dao.markDeliveryForRetry(intent.getIntentId(), candidate.delivery().getDeliveryId(),
                    candidate.delivery().getStatus(), candidate.delivery().getErrorCode()) != 1) {
                    throw new IllegalStateException("通知重试并发状态冲突");
                }
                if (wakeHint == null) wakeHint = candidate.outbox().getOutboxId();
                queuedCount++;
            }
            intent.setStatus(NotificationStatus.QUEUED.name());
            if (dao.update(intent) != 1) throw new IllegalStateException("通知重试聚合写入冲突");
            requestOutboxWake(wakeHint);
        }
        return new RetryReceipt(String.valueOf(intent.getIntentId()), status(intent.getStatus()), queuedCount);
    }

    /** 只认当前发送编排明确在 Provider 前生成的固定分类，不从 FAILED 或宽泛前缀推断。 */
    private boolean safeLocalFailure(NotifyDelivery delivery) {
        String code = delivery.getErrorCode();
        if ("IN_APP".equals(delivery.getChannel())) return "LOCAL_DISPATCH_ERROR".equals(code);
        if (!"SMS".equals(delivery.getChannel()) && !"MAIL".equals(delivery.getChannel())) return false;
        return PRE_SEND_FAILURES.contains(code)
            || ("SMS".equals(delivery.getChannel()) && SMS_PRE_SEND_FAILURES.contains(code));
    }

    /** T-36 按 Intent 主键复用消息、按本人关系去重并仅对新关系发提交后提示。 */
    private boolean safeInAppRetry(NotifyIntent intent, NotifyDelivery delivery) {
        if (delivery.getUserId() == null || delivery.getUserId() <= 0) return false;
        var recipient = dao.messageRecipient(intent.getIntentId(), delivery.getUserId());
        return recipient == null || dao.message(intent.getIntentId()) != null;
    }

    private record RetryCandidate(NotifyDelivery delivery, NotifyOutbox outbox) { }

    public CancelReceipt cancel(NotificationCancelCommand command) {
        if (command == null || command.notificationId() == null) {
            throw new ServiceException("通知编号不能为空");
        }
        NotifyIntent intent = dao.lockIntent(parsePositiveId(command.notificationId()));
        if (intent == null) {
            throw new ServiceException("通知不存在");
        }
        if (Set.of("DELIVERED", "CANCELLED", "FAILED").contains(intent.getStatus())) {
            throw new ServiceException("当前通知状态不允许取消");
        }
        intent.setStatus(NotificationStatus.CANCELLED.name());
        dao.update(intent);
        dao.updateDeliveryStatus(intent.getIntentId(), "PENDING", "CANCELLED");
        return new CancelReceipt(command.notificationId(), NotificationStatus.CANCELLED);
    }

    /**
     * 在当前 {@code @DSTransactional} 内登记提交后唤醒；真正的 Redis 发布由 AFTER_COMMIT 监听器执行。
     *
     * @param outboxIdHint 可选 Outbox 主键 hint，允许为 {@code null}
     */
    private void requestOutboxWake(Long outboxIdHint) {
        events.publishEvent(new NotifyOutboxWakeRequestedEvent(outboxIdHint));
    }

    private void validate(NotificationCommand command) {
        if (command == null || blank(command.appId()) || blank(command.sceneCode())
            || blank(command.templateCode()) || command.channels().isEmpty()) {
            throw new ServiceException("通知应用、场景、模板和渠道不能为空");
        }
        if (command.channels().stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("通知渠道不能为空");
        }
        String recipientType = command.recipientType() == null
            ? "" : command.recipientType().trim().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("ALL", "USER", "PHONE", "EMAIL").contains(recipientType)) {
            throw new ServiceException("接收者类型不受支持");
        }
        List<String> recipientIds = command.recipientIds();
        if ("ALL".equals(recipientType) && !recipientIds.isEmpty()) {
            throw new ServiceException("全部用户通知不能同时指定接收者编号");
        }
        if (!"ALL".equals(recipientType) && recipientIds.isEmpty()) {
            throw new ServiceException("接收者不能为空");
        }
        if ("USER".equals(recipientType)) {
            try {
                if (recipientIds.stream().anyMatch(value -> Long.parseLong(value) <= 0)) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                throw new ServiceException("用户编号必须为正整数");
            }
        }
        Instant expiresAt = floorSecond(command.expiresAt());
        Instant scheduledAt = ceilSecond(command.scheduledAt());
        if (expiresAt != null) {
            if (scheduledAt != null && !expiresAt.isAfter(scheduledAt)) {
                throw new ServiceException("通知截止时间必须晚于计划时间");
            }
            if (!toLocal(expiresAt).isAfter(dao.databaseNow())) {
                throw new ServiceException("通知已过截止时间");
            }
        }
    }

    private NotifyIntent findDuplicate(NotificationCommand command) {
        if (blank(command.idempotencyKey())) {
            return null;
        }
        return dao.intentByIdempotency(command.appId(), command.idempotencyKey());
    }

    private List<ResolvedRecipient> resolveUsers(NotificationCommand command) {
        if ("USER".equalsIgnoreCase(command.recipientType())) {
            List<Long> ids = command.recipientIds().stream().map(Long::valueOf).toList();
            return userService.selectNotificationUsers(ids).stream().filter(Objects::nonNull)
                .map(user -> new ResolvedRecipient(user.getUserId(), String.valueOf(user.getUserId()), user.getPhoneNumber(), user.getEmail()))
                .toList();
        }
        if ("ALL".equalsIgnoreCase(command.recipientType())) {
            if (command.recipientIds().isEmpty()) {
                List<ResolvedRecipient> recipients = new ArrayList<>();
                int offset = 0;
                List<UserDTO> batch;
                do {
                    batch = Objects.requireNonNullElse(userService.selectAllActiveUsers(offset, 1_000), List.of());
                    recipients.addAll(batch.stream().map(user -> new ResolvedRecipient(user.getUserId(), String.valueOf(user.getUserId()), user.getPhoneNumber(), user.getEmail())).toList());
                    if (recipients.size() > 100_000) throw new ServiceException("全体用户通知超过单次发送上限");
                    offset += batch.size();
                } while (!batch.isEmpty());
                return recipients;
            }
            throw new ServiceException("全部用户通知不能同时指定接收者编号");
        }
        if ("PHONE".equalsIgnoreCase(command.recipientType()) || "SMS".equalsIgnoreCase(command.recipientType())) {
            return command.recipientIds().stream().map(value -> new ResolvedRecipient(null, value, value, null)).toList();
        }
        if ("EMAIL".equalsIgnoreCase(command.recipientType()) || "MAIL".equalsIgnoreCase(command.recipientType())) {
            return command.recipientIds().stream().map(value -> new ResolvedRecipient(null, value, null, value)).toList();
        }
        throw new ServiceException("接收者类型仅支持 USER、ALL、PHONE 和 EMAIL");
    }

    private String targetValue(NotificationChannel channel, ResolvedRecipient user) {
        return switch (channel) {
            case IN_APP -> user.userId() == null ? null : String.valueOf(user.userId());
            case SMS -> user.phone();
            case MAIL -> user.email();
        };
    }

    private NotificationReceipt receipt(NotifyIntent intent, List<NotifyDelivery> deliveries) {
        boolean queued = deliveries.stream().anyMatch(item -> "PENDING".equals(item.getStatus()));
        boolean followUpRequired = deliveries.stream().anyMatch(item -> !"IN_APP".equals(item.getChannel())
            && ("ACCEPTED".equals(item.getStatus()) || "UNKNOWN".equals(item.getStatus())));
        return new NotificationReceipt(String.valueOf(intent.getIntentId()), status(intent.getStatus()),
            queued, followUpRequired, deliveries.stream().map(item ->
            new NotificationReceipt.DeliveryReceipt(item.getUserId() == null ? null : String.valueOf(item.getUserId()),
                NotificationChannel.valueOf(item.getChannel()), status(item.getStatus()),
                NotifyAuditSupport.publicProviderMessageId(intent, item.getProviderMessageId()))).toList());
    }

    /** 将持久化状态转换为稳定的应用层状态，避免内部状态泄漏到公共 API。 */
    private NotificationStatus status(String value) {
        if (value == null) return NotificationStatus.UNKNOWN;
        if ("PENDING".equals(value) || "READY".equals(value)) {
            return NotificationStatus.QUEUED;
        }
        try {
            return NotificationStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return NotificationStatus.UNKNOWN;
        }
    }
    private String stringValue(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
    private LocalDateTime toLocal(Instant value) { return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC); }
    /** 按数据库 datetime 秒精度向下取截止，避免截断后实际发晚于声明截止。 */
    private Instant floorSecond(Instant value) { return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS); }
    /** 按数据库 datetime 秒精度向上取计划时间，避免任务比声明时间早领取。 */
    private Instant ceilSecond(Instant value) {
        if (value == null) return null;
        Instant floor = floorSecond(value);
        return value.equals(floor) ? floor : floor.plusSeconds(1);
    }
    /** 复用持久原截止并以数据库时钟判断，重复提交和人工重试均不得续期。 */
    private void requireUnexpired(NotifyIntent intent) {
        if (intent.getExpiresAt() != null && !intent.getExpiresAt().isAfter(dao.databaseNow())) {
            throw new ServiceException("通知已过截止时间");
        }
    }
    private Instant parseTime(java.time.LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }

    private Long parsePositiveId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new ServiceException("通知编号必须为正整数");
        }
    }

    private record ResolvedRecipient(Long userId, String key, String phone, String email) {
    }
}
