package org.namewta.notify.service.runtime;

import com.baomidou.dynamic.datasource.tx.TransactionContext;
import lombok.RequiredArgsConstructor;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.model.NotifyContent;
import org.namewta.common.notify.model.NotifyRequest;
import org.namewta.common.notify.model.NotifyResult;
import org.namewta.common.notify.model.NotifyRichContent;
import org.namewta.common.notify.model.NotifyTemplateContent;
import org.namewta.common.notify.model.NotifyTarget;
import org.namewta.common.notify.model.NotifyTargetResult;
import org.namewta.common.notify.model.NotifyDeliveryStatus;
import org.namewta.common.notify.exception.NotifyDeliveryException;
import org.namewta.common.notify.exception.NotifyIdempotencyUnavailableException;
import org.namewta.common.notify.exception.NotifyValidationException;
import org.namewta.common.notify.exception.NotifyAttachmentSnapshotException;
import org.namewta.common.notify.model.NotifyAuditPolicy;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyIntentAttachment;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.port.NotifyDispatchPort;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.notify.port.NotifyDispatchResultPort.Disposition;
import org.namewta.notify.port.NotifyQuotaPort;
import org.namewta.notify.support.NotifySendPlanner;
import org.namewta.notify.support.NotifyAuditSupport;
import org.namewta.notify.support.NotifyTemplateRenderer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单条通知投递服务。
 *
 * <p>Provider 调用在数据库事务外执行，完成后以短事务更新 Attempt、Delivery 和 Intent。</p>
 */
@Service
@RequiredArgsConstructor
public class DispatchNotificationService implements NotifyDispatchPort {
    private static final Set<String> LOCAL_VALIDATION_CODES = Set.of(
        "ATTACHMENT_SNAPSHOT_NOT_CONFIGURED", "CHANNEL_REQUIRED", "CONTENT_REQUIRED",
        "CONTENT_SNAPSHOT_REQUIRED", "IDEMPOTENCY_WINDOW_OUT_OF_RANGE", "INVALID_ATTACHMENT_OSS_ID",
        "INVALID_TARGET", "INVALID_TEMPLATE_PARAMETERS", "LOGICAL_TARGET_NOT_SUPPORTED", "REQUEST_REQUIRED",
        "TARGET_REQUIRED", "UNKNOWN_CHANNEL", "UNKNOWN_PROVIDER", "UNSUPPORTED_TARGET_TYPE");
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
        leased.setClaimedFromReady(outbox.getClaimedFromReady());
        outbox = leased;
        NotifyIntent intent = dao.intent(outbox.getIntentId());
        NotifyDelivery delivery = dao.delivery(outbox.getDeliveryId());
        if (intent == null || delivery == null || !"PENDING".equals(delivery.getStatus())) {
            resultPort.settle(outbox, Disposition.CLOSE);
            return;
        }
        if (!renewLease(outbox)) return;
        if (!resultPort.deadlineGate(outbox)) return;
        RouteDecision route = routeDecision(intent, delivery);
        if (route == RouteDecision.SKIP) {
            resultPort.settle(outbox, Disposition.SKIP);
            return;
        }
        if (route == RouteDecision.WAIT) {
            resultPort.settle(outbox, Disposition.WAIT);
            return;
        }
        if (NotificationChannel.IN_APP.name().equals(delivery.getChannel())) {
            dispatchInApp(outbox, intent, delivery);
            return;
        }
        long started = System.nanoTime();
        NotifyResult result = null;
        String errorCode = null;
        String errorMessage = null;
        boolean smsClientEntered = false;
        boolean deadlineGateInProgress = false;
        NotifyRequest.PreSendGate mailPreSendGate = null;
        try {
            NotifySendPlanner.Plan plan = planChannel(intent, delivery);
            if (!plan.ok()) {
                delivery.setStatus("FAILED");
                errorCode = plan.errorCode();
                errorMessage = plan.errorMessage();
            } else {
                if (NotificationChannel.MAIL.name().equals(delivery.getChannel())) {
                    NotifyOutbox fencedOutbox = outbox;
                    mailPreSendGate = new NotifyRequest.PreSendGate(() -> {
                        if (!resultPort.deadlineGate(fencedOutbox)) throw new NotifyRequest.PreSendClosed();
                    });
                }
                NotifyRequest request = NotifyRequest.builder()
                    .requestId(String.valueOf(delivery.getDeliveryId()))
                    .bizType(intent.getBizType())
                    .bizId(intent.getBizId())
                    .channel(org.namewta.common.notify.model.NotifyChannel.of(delivery.getChannel().toLowerCase()))
                    .providerKey(plan.providerKey())
                    .targets(List.of(target(delivery)))
                    .content(toContent(plan))
                    .attachmentOssIds(NotificationChannel.MAIL.name().equals(delivery.getChannel())
                        && intent.getAttachmentActorUserId() != null
                        ? dao.attachments(intent.getIntentId()).stream()
                            .map(NotifyIntentAttachment::getSourceOssId).toList() : List.of())
                    .attachmentOwnerIntentId(NotificationChannel.MAIL.name().equals(delivery.getChannel())
                        && intent.getAttachmentActorUserId() != null ? intent.getIntentId() : null)
                    .preSendGate(mailPreSendGate)
                    .auditPolicy(NotifyAuditSupport.redactSensitive(intent)
                        ? NotifyAuditPolicy.REDACT_SENSITIVE : NotifyAuditPolicy.FULL)
                    .idempotencyKey(String.valueOf(delivery.getDeliveryId()))
                    .build();
                deadlineGateInProgress = true;
                if (!resultPort.deadlineGate(outbox)) return;
                deadlineGateInProgress = false;
                smsClientEntered = NotificationChannel.SMS.name().equals(delivery.getChannel());
                result = notifyClient.send(request);
                ProviderOutcome outcome = providerOutcome(result, delivery);
                errorCode = outcome.errorCode();
                errorMessage = outcome.errorMessage();
            }
        } catch (NotifyRequest.PreSendClosed closed) {
            // deadlineGate 已收敛终态；租约丢失时也绝不补写或进入 SMTP。
            return;
        } catch (NotifyDeliveryException exception) {
            if (deadlineGateInProgress) throw exception;
            result = exception.result();
            ProviderOutcome outcome = providerOutcome(result, delivery);
            errorCode = outcome.errorCode();
            errorMessage = outcome.errorMessage();
        } catch (NotifyAttachmentSnapshotException exception) {
            if (deadlineGateInProgress) throw exception;
            // 快照阶段尚未进入 SMTP；保留可核对 COPY_UNKNOWN，不等外部回执也不盲重发。
            delivery.setStatus("FAILED");
            errorCode = "ATTACHMENT_SNAPSHOT_UNAVAILABLE";
            errorMessage = "附件私有快照未确认";
        } catch (NotifyValidationException exception) {
            if (deadlineGateInProgress) throw exception;
            if (NotificationChannel.SMS.name().equals(delivery.getChannel())) {
                delivery.setStatus("FAILED");
                errorCode = "SMS_LOCAL_VALIDATION_" + safeValidationCode(exception.code());
                errorMessage = "短信请求本地校验失败";
            } else {
                delivery.setStatus("UNKNOWN");
                errorCode = "DISPATCH_ERROR";
                errorMessage = "供应商调用结果未知";
            }
        } catch (NotifyIdempotencyUnavailableException exception) {
            if (deadlineGateInProgress) throw exception;
            if (NotificationChannel.SMS.name().equals(delivery.getChannel())
                && "ACQUIRE".equals(exception.phase())) {
                // 幂等占位读取失败发生在供应商调用前，可走现有有界 Outbox 重试。
                delivery.setStatus("FAILED");
                errorCode = "PREPARATION_RETRYABLE";
                errorMessage = "短信发送准备暂不可用";
            } else {
                // COMPLETE 已在供应商调用之后；未知阶段和其他渠道沿用保守状态。
                delivery.setStatus("UNKNOWN");
                errorCode = "DISPATCH_ERROR";
                errorMessage = "供应商调用结果未知";
            }
        } catch (RuntimeException exception) {
            if (deadlineGateInProgress || mailPreSendGate != null && mailPreSendGate.failed()) throw exception;
            if (NotificationChannel.SMS.name().equals(delivery.getChannel()) && !smsClientEntered) {
                // 尚未进入 NotifyClient，供应商必定未被调用；保留有界 Outbox 重试。
                delivery.setStatus("FAILED");
                errorCode = "PREPARATION_RETRYABLE";
                errorMessage = "短信发送准备暂不可用";
            } else {
                delivery.setStatus("UNKNOWN");
                errorCode = "DISPATCH_ERROR";
                errorMessage = "供应商调用结果未知";
            }
        }
        resultPort.complete(outbox, new NotifyDispatchResultPort.Result(delivery.getStatus(),
            result == null ? delivery.getProviderKey() : result.providerKey(),
            delivery.getProviderMessageId(), errorCode, errorMessage,
            java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)));
    }

    /** 本地站内信经已代理结果端口提交；事务/commit异常原样外溢，不能伪装成待供应商回执。 */
    private void dispatchInApp(NotifyOutbox outbox, NotifyIntent intent, NotifyDelivery delivery) {
        long started = System.nanoTime();
        InAppNotificationPort port;
        InAppNotificationPort.InAppSnapshot snapshot;
        try {
            port = inAppPort.getIfAvailable();
            if (port == null || delivery.getUserId() == null || delivery.getUserId() <= 0) {
                throw new IllegalArgumentException("站内通知端口或收件人无效");
            }
            Map<String, Object> params = JsonUtils.parseObject(intent.getTemplateParamsJson(), Map.class);
            Object channelValues = params == null ? null : params.get("channels");
            List<String> channels = channelValues == null ? List.of(delivery.getChannel())
                : JsonUtils.parseArray(JsonUtils.toJsonString(channelValues), String.class);
            if (channels == null || channels.isEmpty()) channels = List.of(delivery.getChannel());
            String noticeType = params == null ? null : String.valueOf(params.getOrDefault("noticeType", ""));
            snapshot = new InAppNotificationPort.InAppSnapshot(intent.getTitleSnapshot(), intent.getContentSnapshot(),
                intent.getPathSnapshot(), noticeType, channels);
            if (!fitsColumn(snapshot.title(), 255) || !fitsColumn(snapshot.noticeType(), 10)
                || !fitsColumn(snapshot.path(), 500)) {
                throw new IllegalArgumentException("站内通知快照超过持久化字段上限");
            }
        } catch (RuntimeException invalidLocalInput) {
            resultPort.complete(outbox, new NotifyDispatchResultPort.Result("FAILED", "in-app", null,
                "LOCAL_DISPATCH_ERROR", "站内通知参数或端口无效", 0));
            return;
        }
        boolean standaloneBegin = standaloneResultCall();
        boolean reserved;
        try {
            reserved = resultPort.beginInAppAttempt(outbox);
        } catch (RuntimeException | Error failure) {
            clearFailedResultSynchronizations(standaloneBegin);
            throw failure;
        }
        if (!reserved) {
            NotifyDelivery current = dao.delivery(outbox.getDeliveryId());
            if (current == null || !"PENDING".equals(current.getStatus())) {
                resultPort.settle(outbox, Disposition.CLOSE);
            }
            return;
        }
        boolean standaloneComplete = standaloneResultCall();
        try {
            resultPort.completeInApp(outbox, port, snapshot, delivery.getUserId(),
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        } catch (RuntimeException | Error failure) {
            clearFailedResultSynchronizations(standaloneComplete);
            throw failure;
        }
    }

    private boolean standaloneResultCall() {
        return TransactionContext.getXID() == null && TransactionContext.getSynchronizations().isEmpty();
    }

    /** 提交故障可能跳过 afterCompletion；仅原本无上下文的调用清理其新留在本线程的回调。 */
    private void clearFailedResultSynchronizations(boolean standaloneAtEntry) {
        if (standaloneAtEntry && TransactionContext.getXID() == null) TransactionContext.removeSynchronizations();
    }

    private boolean fitsColumn(String value, int maximumCodePoints) {
        return value == null || value.codePointCount(0, value.length()) <= maximumCodePoints;
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
        return new NotifyTemplateContent("sms", plan.smsTemplateCode(), plan.smsParams(), plan.smsSnapshot());
    }

    /** 新意图提交前只检查短信绑定和参数，不占用额度或发起 Provider I/O。 */
    public void validateSubmission(NotificationCommand command) {
        if (!command.channels().contains(NotificationChannel.SMS)) return;
        NotifySceneBinding binding = configDao.findBinding(command.sceneCode(), NotificationChannel.SMS.name());
        NotifyChannelAccount account = binding == null || binding.getAccountId() == null
            ? null : configDao.findAccount(binding.getAccountId());
        NotifySendPlanner.Plan plan = NotifySendPlanner.preflight(command.sceneCode(), NotificationChannel.SMS.name(),
            NotifyTemplateRenderer.stringify(command.templateParams()), binding, account);
        if (!plan.ok()) throw new ServiceException(plan.errorMessage());
    }

    /** 只有单目标的明确未受理事实可以给 Outbox 重发权；旧 FAILED 和邮件/短信异常均未知。 */
    private ProviderOutcome providerOutcome(NotifyResult result, NotifyDelivery delivery) {
        delivery.setProviderKey(result.providerKey());
        if (result.deliveries().size() != 1) {
            if (result.status() == org.namewta.common.notify.model.NotifyStatus.ACCEPTED) {
                delivery.setStatus("ACCEPTED");
                return new ProviderOutcome(null, null);
            }
            delivery.setStatus("UNKNOWN");
            return new ProviderOutcome("PROVIDER_OUTCOME_UNKNOWN", "供应商调用结果未知");
        }
        NotifyTargetResult target = result.deliveries().getFirst();
        delivery.setProviderMessageId(target.providerMessageId());
        NotifyDeliveryStatus status = target.status();
        if (status == NotifyDeliveryStatus.ACCEPTED) {
            delivery.setStatus("ACCEPTED");
            return new ProviderOutcome(null, null);
        }
        if (status == NotifyDeliveryStatus.UNSENT_RETRYABLE) {
            delivery.setStatus("FAILED");
            return new ProviderOutcome("PROVIDER_UNSENT_RETRYABLE", "供应商明确未受理，可在预算内重试");
        }
        if (status == NotifyDeliveryStatus.UNSENT_TERMINAL) {
            delivery.setStatus("FAILED");
            return new ProviderOutcome("PROVIDER_UNSENT_TERMINAL", "供应商明确未受理，不能自动恢复");
        }
        delivery.setStatus("UNKNOWN");
        return new ProviderOutcome("PROVIDER_OUTCOME_UNKNOWN", "供应商调用结果未知");
    }

    private record ProviderOutcome(String errorCode, String errorMessage) {}

    private String safeValidationCode(String code) {
        return code != null && LOCAL_VALIDATION_CODES.contains(code) ? code : "VALIDATION_ERROR";
    }

    private NotifySendPlanner.Plan planChannel(NotifyIntent intent, NotifyDelivery delivery) {
        String sceneCode = intent.getSceneCode() == null || intent.getSceneCode().isBlank()
            ? intent.getTemplateCode() : intent.getSceneCode();
        NotifySceneBinding binding = configDao.findBinding(sceneCode, delivery.getChannel());
        NotifyChannelAccount account = binding == null || binding.getAccountId() == null
            ? null : configDao.findAccount(binding.getAccountId());
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
