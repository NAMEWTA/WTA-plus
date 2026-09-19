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
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.port.NotifyDispatchPort;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.notify.port.NotifyDispatchResultPort.Disposition;
import org.namewta.notify.port.NotifyQuotaPort;
import org.namewta.notify.support.NotifySendPlanner;
import org.namewta.notify.support.NotifyTemplateRenderer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;

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
    private final NotifyDispatchResultPort resultPort;

    /**
     * 执行一个 Outbox 任务，Provider I/O 在结果事务外；失效 owner 不写回。
     * @param outbox 本次领取的任务与 fencing token
     * @throws IllegalStateException 结果事务检测到写入冲突
     */
    public void dispatch(NotifyOutbox outbox) {
        NotifyOutbox leased = dao.outbox(outbox.getOutboxId());
        if (!leaseActive(leased, outbox)) return;
        outbox = leased;
        NotifyIntent intent = dao.intent(outbox.getIntentId());
        NotifyDelivery delivery = dao.delivery(outbox.getDeliveryId());
        if (intent == null || delivery == null || !"PENDING".equals(delivery.getStatus())) {
            resultPort.settle(outbox, Disposition.CLOSE);
            return;
        }
        if (!renewLease(outbox)) return;
        RouteDecision route = routeDecision(intent, delivery);
        if (route == RouteDecision.SKIP) {
            resultPort.settle(outbox, Disposition.SKIP);
            return;
        }
        if (route == RouteDecision.WAIT) {
            resultPort.settle(outbox, Disposition.WAIT);
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
        resultPort.complete(outbox, new NotifyDispatchResultPort.Result(delivery.getStatus(),
            result == null ? (NotificationChannel.IN_APP.name().equals(delivery.getChannel()) ? "in-app" : delivery.getProviderKey()) : result.providerKey(),
            delivery.getProviderMessageId(), errorCode, errorMessage,
            java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)));
    }

    /** 调用前续租也由数据库判断有效期，不能复活已经过期的 owner。 */
    private boolean renewLease(NotifyOutbox outbox) {
        return resultPort.renew(outbox);
    }

    /** 预检只减少无效 I/O；最终权限以锁后的结果事务为准。 */
    private boolean leaseActive(NotifyOutbox current, NotifyOutbox claimed) {
        return current != null && "PROCESSING".equals(current.getStatus())
            && claimed.getLeaseOwner() != null && claimed.getLeaseToken() != null
            && java.util.Objects.equals(current.getLeaseOwner(), claimed.getLeaseOwner())
            && java.util.Objects.equals(current.getLeaseToken(), claimed.getLeaseToken())
            && current.getLeaseUntil() != null && current.getLeaseUntil().isAfter(dao.databaseNow());
    }

    /** 聚合重算同样经过短事务代理，供提交用例复用。 */
    public void refreshAggregate(Long intentId) { resultPort.refreshAggregate(intentId); }

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
}
