package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.model.NotifyContent;
import org.namewta.common.notify.model.NotifyRequest;
import org.namewta.common.notify.model.NotifyResult;
import org.namewta.common.notify.model.NotifyRichContent;
import org.namewta.common.notify.model.NotifyTemplateContent;
import org.namewta.common.notify.model.NotifyTarget;
import org.namewta.common.notify.exception.NotifyDeliveryException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyAttempt;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.domain.policy.NotificationAggregatePolicy;
import org.namewta.notify.port.NotifyDispatchPort;
import org.namewta.notify.port.NotifyQuotaPort;
import org.namewta.notify.support.NotifySendPlanner;
import org.namewta.notify.support.NotifyTemplateRenderer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * 单条通知投递服务。
 *
 * <p>Provider 调用在数据库事务外执行，完成后以短事务更新 Attempt、Delivery 和 Intent。</p>
 */
@Service
@RequiredArgsConstructor
public class DispatchNotificationService implements NotifyDispatchPort {
    private final NotifyNotificationDao dao;
    private final NotifyClient notifyClient;
    private final ObjectProvider<InAppNotificationPort> inAppPort;
    private final NotifyConfigDao configDao;
    private final NotifyQuotaPort quotaPort;

    /** 执行一个 Outbox 任务。 */
    public void dispatch(NotifyOutbox outbox) {
        NotifyOutbox leased = dao.outbox(outbox.getOutboxId());
        if (!leaseActive(leased, outbox)) return;
        outbox = leased;
        NotifyIntent intent = dao.intent(outbox.getIntentId());
        NotifyDelivery delivery = dao.delivery(outbox.getDeliveryId());
        if (intent == null || delivery == null || !"PENDING".equals(delivery.getStatus())) {
            outbox.setStatus("DONE");
            dao.finishOutbox(outbox);
            return;
        }
        if (!renewLease(outbox)) return;
        RouteDecision route = routeDecision(intent, delivery);
        if (route == RouteDecision.SKIP) {
            delivery.setStatus("CANCELLED");
            dao.update(delivery);
            outbox.setStatus("DONE");
            dao.finishOutbox(outbox);
            refreshIntent(intent.getIntentId());
            return;
        }
        if (route == RouteDecision.WAIT) {
            LocalDateTime next = LocalDateTime.ofInstant(Instant.now().plusSeconds(30), ZoneOffset.UTC);
            outbox.setStatus("READY");
            outbox.setNextAttemptAt(next);
            dao.finishOutbox(outbox);
            return;
        }
        long started = System.nanoTime();
        NotifyResult result = null;
        String errorCode = null;
        String errorMessage = null;
        try {
            if (NotificationChannel.IN_APP.name().equals(delivery.getChannel())) {
                InAppNotificationPort port = inAppPort.getIfAvailable();
                if (port == null) {
                    throw new IllegalStateException("站内通知端口未装配");
                }
                Map<String, Object> templateParams = JsonUtils.parseObject(intent.getTemplateParamsJson(), Map.class);
                String noticeType = templateParams == null ? null : String.valueOf(templateParams.getOrDefault("noticeType", ""));
                List<String> channels = templateParams == null ? List.of(delivery.getChannel())
                    : JsonUtils.parseArray(JsonUtils.toJsonString(templateParams.get("channels")), String.class);
                if (channels.isEmpty()) channels = List.of(delivery.getChannel());
                InAppNotificationPort.InAppSnapshot snapshot = new InAppNotificationPort.InAppSnapshot(
                    intent.getTitleSnapshot(), intent.getContentSnapshot(), intent.getPathSnapshot(),
                    noticeType, channels);
                port.persist(String.valueOf(intent.getIntentId()), snapshot, List.of(delivery.getUserId()));
                delivery.setStatus("DELIVERED");
                try {
                    port.pushRealtime(String.valueOf(intent.getIntentId()), snapshot, List.of(delivery.getUserId()));
                } catch (RuntimeException exception) {
                    // 站内信已经落库，实时提示失败只影响在线体验，不应触发重复写入。
                }
            } else {
                NotifySendPlanner.Plan plan = planChannel(intent, delivery);
                if (!plan.ok()) {
                    delivery.setStatus("FAILED");
                    errorCode = plan.errorCode();
                    errorMessage = plan.errorMessage();
                } else {
                    NotifyRequest request = NotifyRequest.builder()
                        .requestId(String.valueOf(delivery.getDeliveryId()))
                        .bizType(intent.getBizType())
                        .bizId(intent.getBizId())
                        .channel(org.namewta.common.notify.model.NotifyChannel.of(delivery.getChannel().toLowerCase()))
                        .providerKey(plan.providerKey())
                        .targets(List.of(target(delivery)))
                        .content(toContent(plan))
                        .idempotencyKey(String.valueOf(delivery.getDeliveryId()))
                        .build();
                    result = notifyClient.send(request);
                    delivery.setStatus(result.status().name());
                    if (!result.deliveries().isEmpty()) {
                        delivery.setProviderKey(result.providerKey());
                        delivery.setProviderMessageId(result.deliveries().getFirst().providerMessageId());
                        errorCode = result.deliveries().getFirst().errorCode();
                        errorMessage = result.deliveries().getFirst().errorMessage();
                    }
                }
            }
        } catch (NotifyDeliveryException exception) {
            result = exception.result();
            delivery.setStatus(result.status().name());
            if (!result.deliveries().isEmpty()) {
                delivery.setProviderKey(result.providerKey());
                delivery.setProviderMessageId(result.deliveries().getFirst().providerMessageId());
                errorCode = result.deliveries().getFirst().errorCode();
                errorMessage = result.deliveries().getFirst().errorMessage();
            }
        } catch (RuntimeException exception) {
            delivery.setStatus("UNKNOWN");
            errorCode = "DISPATCH_ERROR";
            errorMessage = exception.getClass().getSimpleName();
        }
        // Provider I/O may outlive the lease. A stale worker must not overwrite a newer claim.
        if (!renewLease(outbox)) return;
        delivery.setAttemptCount((delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount()) + 1);
        if (isSuccess(delivery.getStatus())) {
            delivery.setAcceptedAt(LocalDateTime.now(ZoneOffset.UTC));
            if ("DELIVERED".equals(delivery.getStatus())) {
                delivery.setDeliveredAt(LocalDateTime.now(ZoneOffset.UTC));
            }
        }
        delivery.setErrorCode(errorCode);
        delivery.setErrorMessage(errorMessage);
        dao.update(delivery);

        NotifyAttempt attempt = new NotifyAttempt();
        attempt.setAttemptId(org.namewta.common.mybatis.utils.IdGeneratorUtil.nextLongId());
        attempt.setIntentId(intent.getIntentId());
        attempt.setDeliveryId(delivery.getDeliveryId());
        attempt.setAttemptNo(delivery.getAttemptCount());
        attempt.setProviderKey(result == null ? "in-app" : result.providerKey());
        attempt.setStatus(delivery.getStatus());
        attempt.setProviderMessageId(delivery.getProviderMessageId());
        attempt.setErrorCategory(errorCode);
        attempt.setErrorCode(errorCode);
        attempt.setErrorMessage(errorMessage);
        attempt.setCostTime(java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        dao.insert(attempt);

        boolean success = isSuccess(delivery.getStatus());
        boolean failClosed = "FAILED".equals(delivery.getStatus()) && isConfigFailure(errorCode);
        boolean waitForReceipt = "UNKNOWN".equals(delivery.getStatus());
        outbox.setStatus(success || failClosed ? "DONE" : waitForReceipt ? "WAITING_RECEIPT" : "READY");
        outbox.setAttemptCount((outbox.getAttemptCount() == null ? 0 : outbox.getAttemptCount()) + 1);
        outbox.setLastErrorCode(errorCode);
        outbox.setLastErrorMessage(errorMessage);
        if (!success && !waitForReceipt && !failClosed) {
            outbox.setNextAttemptAt(LocalDateTime.ofInstant(Instant.now().plusSeconds(backoff(outbox.getAttemptCount())), ZoneOffset.UTC));
            if (outbox.getAttemptCount() >= outbox.getMaxAttempts()) {
                outbox.setStatus("DEAD_LETTER");
                delivery.setStatus("FAILED");
                dao.update(delivery);
            } else {
                delivery.setStatus("PENDING");
                dao.update(delivery);
            }
        }
        if (dao.finishOutbox(outbox) != 1) return;
        refreshIntent(intent.getIntentId());
    }

    private boolean renewLease(NotifyOutbox outbox) {
        LocalDateTime leaseUntil = LocalDateTime.ofInstant(Instant.now().plusSeconds(60), ZoneOffset.UTC);
        return dao.renewOutbox(outbox.getOutboxId(), outbox.getLeaseOwner(), outbox.getLeaseToken(), leaseUntil) == 1;
    }

    private boolean leaseActive(NotifyOutbox current, NotifyOutbox claimed) {
        return current != null && "PROCESSING".equals(current.getStatus())
            && java.util.Objects.equals(current.getLeaseOwner(), claimed.getLeaseOwner())
            && java.util.Objects.equals(current.getLeaseToken(), claimed.getLeaseToken())
            && current.getLeaseUntil() != null
            && current.getLeaseUntil().isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }

    /** 根据最新投递结果重新计算通知聚合状态，供异步回调使用。 */
    public void refreshAggregate(Long intentId) {
        refreshIntent(intentId);
    }

    private void refreshIntent(Long intentId) {
        NotifyIntent intent = dao.intent(intentId);
        List<NotifyDelivery> all = dao.deliveries(intentId);
        if (all.isEmpty()) return;
        intent.setStatus(NotificationAggregatePolicy.aggregate(all.stream()
            .map(NotifyDelivery::getStatus).toList()).name());
        dao.update(intent);
    }

    private NotifyContent toContent(NotifySendPlanner.Plan plan) {
        if (plan.mail()) {
            return new NotifyRichContent(plan.subject(), plan.body(), plan.html());
        }
        return new NotifyTemplateContent("sms", plan.smsTemplateCode(), plan.smsParams(), "");
    }

    private NotifySendPlanner.Plan planChannel(NotifyIntent intent, NotifyDelivery delivery) {
        String sceneCode = intent.getSceneCode() == null || intent.getSceneCode().isBlank()
            ? intent.getTemplateCode() : intent.getSceneCode();
        NotifySceneBinding binding = configDao.findBinding(sceneCode, delivery.getChannel());
        NotifyChannelAccount account = binding == null ? null : configDao.findAccount(binding.getAccountId());
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = JsonUtils.parseObject(intent.getTemplateParamsJson(), Map.class);
        return NotifySendPlanner.plan(sceneCode, delivery.getChannel(), delivery.getTargetValue(),
            NotifyTemplateRenderer.stringify(raw), binding, account, quotaPort);
    }

    private boolean isConfigFailure(String errorCode) {
        return errorCode != null && (
            errorCode.startsWith("UNBOUND")
                || errorCode.startsWith("ACCOUNT_")
                || errorCode.startsWith("MISSING_")
                || errorCode.startsWith("SMS_")
                || errorCode.endsWith("_QUOTA"));
    }

    private NotifyTarget target(NotifyDelivery delivery) {
        return switch (delivery.getChannel()) {
            case "SMS" -> NotifyTarget.phone(delivery.getTargetValue());
            case "MAIL" -> NotifyTarget.email(delivery.getTargetValue());
            default -> NotifyTarget.user(String.valueOf(delivery.getUserId()));
        };
    }

    private boolean isSuccess(String status) { return "ACCEPTED".equals(status) || "DELIVERED".equals(status); }
    private RouteDecision routeDecision(NotifyIntent intent, NotifyDelivery delivery) {
        if (!"ORDERED_FALLBACK".equals(intent.getStrategy()) && !"ESCALATION".equals(intent.getStrategy())) return RouteDecision.READY;
        List<NotifyDelivery> prior = dao.priorDeliveries(intent.getIntentId(), delivery.getRecipientId(), delivery.getDeliveryId());
        if (prior.isEmpty()) return RouteDecision.READY;
        NotifyDelivery previous = prior.getLast();
        if ("ORDERED_FALLBACK".equals(intent.getStrategy())) {
            return prior.stream().anyMatch(item -> isSuccess(item.getStatus())) ? RouteDecision.SKIP
                : "FAILED".equals(previous.getStatus()) || "UNDELIVERABLE".equals(previous.getStatus())
                ? RouteDecision.READY : RouteDecision.WAIT;
        }
        return "UNDELIVERABLE".equals(previous.getStatus()) || "FAILED".equals(previous.getStatus())
            ? RouteDecision.READY : RouteDecision.WAIT;
    }
    private enum RouteDecision { READY, WAIT, SKIP }
    private long backoff(int attempt) { return Math.min(3600L, 1L << Math.min(attempt, 10)); }
}

