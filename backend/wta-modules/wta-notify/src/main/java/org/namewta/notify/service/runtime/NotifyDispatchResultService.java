package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyAttempt;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.policy.NotificationAggregatePolicy;
import org.namewta.notify.domain.policy.NotificationDeliveryPolicy;
import org.namewta.notify.port.NotifyDispatchResultPort.Disposition;
import org.namewta.notify.port.NotifyDispatchResultPort.Result;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** 结果写回规则；调用方必须经过 NotifyDispatchResultUseCase 的动态事务代理。 */
@Service
@RequiredArgsConstructor
public class NotifyDispatchResultService {
    private static final String IN_APP_ATTEMPT_RESERVED = "IN_APP_ATTEMPT_RESERVED";
    private final NotifyNotificationDao dao;

    /** 锁序为 Intent → Outbox → Delivery，随后取数据库时间验证租约，阻止等锁期间过期的 owner。 */
    private NotifyOutbox lockActive(NotifyOutbox lease) {
        if (dao.lockIntent(lease.getIntentId()) == null) return null;
        NotifyOutbox current = dao.lockOutbox(lease.getOutboxId());
        if (current == null || !"PROCESSING".equals(current.getStatus())
            || lease.getLeaseOwner() == null || lease.getLeaseToken() == null
            || !Objects.equals(current.getIntentId(), lease.getIntentId())
            || !Objects.equals(current.getDeliveryId(), lease.getDeliveryId())
            || !Objects.equals(current.getLeaseOwner(), lease.getLeaseOwner())
            || !Objects.equals(current.getLeaseToken(), lease.getLeaseToken())) return null;
        return current;
    }

    /** 取完行锁后独立查询时钟，不能使用可能在等待锁之前固定的 SQL NOW 值。 */
    private boolean unexpired(NotifyOutbox outbox) {
        return outbox.getLeaseUntil() != null && outbox.getLeaseUntil().isAfter(dao.databaseNow());
    }

    /**
     * 续租也先锁后取时钟，防止 UPDATE 在等待锁前固定的 NOW 值复活租约。
     * @param lease 原领取的任务与 owner/token
     * @return 只有仍有效的租约才返回 true
     */
    public boolean renew(NotifyOutbox lease) {
        NotifyOutbox current = lockActive(lease);
        return current != null && unexpired(current)
            && dao.renewOutbox(current.getOutboxId(), current.getLeaseOwner(), current.getLeaseToken()) == 1;
    }

    /**
     * 站内信预算必须早于消息结果事务独立提交，故结果事务回滚也只会消耗一次有上限的预算。
     * 同一租约的重入先检查预留标志，再检查上限；新租约领取时只清此前的内部标志。
     * @param lease 当前领取的 owner/token
     * @return 获得本租约一次持久化执行权时为 true
     */
    public boolean beginInAppAttempt(NotifyOutbox lease) {
        NotifyOutbox outbox = lockActive(lease);
        if (outbox == null) return false;
        NotifyDelivery delivery = dao.lockDelivery(outbox.getDeliveryId());
        if (!unexpired(outbox) || delivery == null || !Objects.equals(delivery.getIntentId(), outbox.getIntentId())
            || !"IN_APP".equals(delivery.getChannel()) || !"PENDING".equals(delivery.getStatus())) return false;
        if (IN_APP_ATTEMPT_RESERVED.equals(outbox.getLastErrorCode())) return false;
        Integer attempts = outbox.getAttemptCount();
        Integer maximum = outbox.getMaxAttempts();
        if (attempts == null || maximum == null || attempts < 0 || maximum <= 0 || attempts >= maximum) {
            delivery.setStatus("FAILED");
            delivery.setErrorCode("IN_APP_RETRY_EXHAUSTED");
            delivery.setErrorMessage("站内投递重试次数已用尽");
            outbox.setStatus("DEAD_LETTER");
            outbox.setLastErrorCode("IN_APP_RETRY_EXHAUSTED");
            outbox.setLastErrorMessage("站内投递重试次数已用尽");
            requireOne(dao.saveDeliveryResult(delivery));
            finish(outbox);
            refreshAggregate(outbox.getIntentId());
            return false;
        }
        requireOne(dao.reserveInAppOutbox(outbox.getOutboxId(), outbox.getLeaseOwner(), outbox.getLeaseToken()));
        return true;
    }

    /**
     * 仅已预留的活租约可在本事务写站内事实和结果；DB异常直接回滚，不转作外部 UNKNOWN。
     * @param lease 已预留的 owner/token
     * @param port 本地收件箱持久化端口
     * @param snapshot 站内内容快照
     * @param userId 当前收件人
     * @param costTime 毫秒耗时
     */
    public void completeInApp(NotifyOutbox lease, InAppNotificationPort port,
                              InAppNotificationPort.InAppSnapshot snapshot, Long userId, long costTime) {
        NotifyOutbox outbox = lockActive(lease);
        if (outbox == null) return;
        NotifyDelivery delivery = dao.lockDelivery(outbox.getDeliveryId());
        if (!unexpired(outbox) || delivery == null || !Objects.equals(delivery.getIntentId(), outbox.getIntentId())
            || !"IN_APP".equals(delivery.getChannel()) || !"PENDING".equals(delivery.getStatus())
            || !IN_APP_ATTEMPT_RESERVED.equals(outbox.getLastErrorCode())
            || !Objects.equals(delivery.getUserId(), userId)) return;
        port.persist(String.valueOf(outbox.getIntentId()), snapshot, java.util.List.of(userId));
        if (!unexpired(outbox)) throw new IllegalStateException("站内投递写入期间租约已失效");
        writeResult(outbox, delivery, new Result("DELIVERED", "in-app", null, null, null, costTime));
    }

    /**
     * 原子保存尝试与当前结果；finish=0和任意写入冲突必须抛出以回滚整个短事务。
     * @param lease 原领取的任务与 owner/token
     * @param result 事务外 Provider 结果快照
     * @throws IllegalStateException 持有有效租约后的任何写入未影响一行
     */
    public void complete(NotifyOutbox lease, Result result) {
        NotifyOutbox outbox = lockActive(lease);
        if (outbox == null) return;
        NotifyDelivery delivery = dao.lockDelivery(outbox.getDeliveryId());
        if (!unexpired(outbox)) return;
        if (delivery == null || !Objects.equals(delivery.getIntentId(), outbox.getIntentId())) {
            outbox.setStatus("DONE"); finish(outbox); return;
        }
        if ("IN_APP".equals(delivery.getChannel())
            && (IN_APP_ATTEMPT_RESERVED.equals(outbox.getLastErrorCode())
                || !("FAILED".equals(result.status()) && "LOCAL_DISPATCH_ERROR".equals(result.errorCode())))) {
            throw new IllegalArgumentException("站内预留后结果必须经原子持久化入口");
        }
        writeResult(outbox, delivery, result);
    }

    /** 持有统一锁与有效租约后，共用原有 Delivery/Attempt/Outbox/Intent 写入次序。 */
    private void writeResult(NotifyOutbox outbox, NotifyDelivery delivery, Result result) {
        boolean advanced = !"PENDING".equals(delivery.getStatus());
        if (!advanced || NotificationDeliveryPolicy.canAdvance(delivery.getStatus(), result.status())) {
            delivery.setStatus(result.status());
            delivery.setProviderKey(result.providerKey());
            delivery.setProviderMessageId(result.providerMessageId());
            delivery.setErrorCode(result.errorCode());
            delivery.setErrorMessage(result.errorMessage());
            if (success(delivery.getStatus()) && delivery.getAcceptedAt() == null) delivery.setAcceptedAt(dao.databaseNow());
            if ("DELIVERED".equals(delivery.getStatus()) && delivery.getDeliveredAt() == null) delivery.setDeliveredAt(dao.databaseNow());
        }
        int attemptNo = (delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount()) + 1;
        delivery.setAttemptCount(attemptNo);
        NotifyAttempt attempt = new NotifyAttempt();
        attempt.setAttemptId(IdGeneratorUtil.nextLongId()); attempt.setIntentId(outbox.getIntentId());
        attempt.setDeliveryId(delivery.getDeliveryId()); attempt.setAttemptNo(attemptNo);
        attempt.setProviderKey(result.providerKey()); attempt.setProviderMessageId(result.providerMessageId());
        attempt.setStatus(result.status()); attempt.setErrorCategory(result.errorCode());
        attempt.setErrorCode(result.errorCode()); attempt.setErrorMessage(result.errorMessage()); attempt.setCostTime(result.costTime());

        boolean failClosed = "FAILED".equals(delivery.getStatus()) && configFailure(result.errorCode());
        boolean waiting = "UNKNOWN".equals(delivery.getStatus());
        // IN_APP 的预算已在独立短事务预留；外部渠道仍按结果提交次数计数。
        if (!"IN_APP".equals(delivery.getChannel()) || !IN_APP_ATTEMPT_RESERVED.equals(outbox.getLastErrorCode())) {
            outbox.setAttemptCount((outbox.getAttemptCount() == null ? 0 : outbox.getAttemptCount()) + 1);
        }
        outbox.setLastErrorCode(result.errorCode()); outbox.setLastErrorMessage(result.errorMessage());
        outbox.setStatus(advanced || success(delivery.getStatus()) || failClosed ? "DONE" : waiting ? "WAITING_RECEIPT" : "READY");
        if ("READY".equals(outbox.getStatus())) {
            outbox.setNextAttemptAt(dao.databaseNow().plusSeconds(Math.min(3600L, 1L << Math.min(outbox.getAttemptCount(), 10))));
            if (outbox.getAttemptCount() >= outbox.getMaxAttempts()) {
                outbox.setStatus("DEAD_LETTER"); delivery.setStatus("FAILED");
            } else delivery.setStatus("PENDING");
        }
        requireOne(dao.saveDeliveryResult(delivery));
        requireOne(dao.insert(attempt));
        finish(outbox);
        refreshAggregate(outbox.getIntentId());
    }

    /**
     * 无外部调用的编排也服从相同租约和事务，不能绕过结果 fence。
     * @param lease 原领取的任务与 owner/token
     * @param disposition 编排得出的等待、取消或关闭决定
     * @throws IllegalStateException 有效租约下的写入冲突
     */
    public void settle(NotifyOutbox lease, Disposition disposition) {
        NotifyOutbox outbox = lockActive(lease);
        if (outbox == null) return;
        NotifyDelivery delivery = dao.lockDelivery(outbox.getDeliveryId());
        if (!unexpired(outbox)) return;
        outbox.setStatus("DONE");
        if (delivery != null && "PENDING".equals(delivery.getStatus())) {
            if (disposition == Disposition.WAIT) {
                outbox.setStatus("READY"); outbox.setNextAttemptAt(dao.databaseNow().plusSeconds(30));
            } else if (disposition == Disposition.SKIP) {
                delivery.setStatus("CANCELLED"); requireOne(dao.saveDeliveryResult(delivery));
            }
        }
        finish(outbox);
        refreshAggregate(outbox.getIntentId());
    }

    /**
     * Intent 锁串行化同一通知的所有聚合；锁定读避免早先普通查询创建的 RR 快照。
     * @param intentId 通知意图主键，缺失时不写入
     * @throws IllegalStateException 聚合写入未影响一行
     */
    public void refreshAggregate(Long intentId) {
        NotifyIntent intent = dao.lockIntent(intentId);
        if (intent == null) return;
        var deliveries = dao.lockDeliveries(intentId);
        if (deliveries.isEmpty()) return;
        intent.setStatus(NotificationAggregatePolicy.aggregate(deliveries.stream().map(NotifyDelivery::getStatus).toList()).name());
        requireOne(dao.update(intent));
    }

    /** 完成 SQL 再次校验数据库租约时间；不得静默忽略零行结果。 */
    private void finish(NotifyOutbox outbox) { requireOne(dao.finishOutbox(outbox)); }

    /** 乐观冲突或缺行等同提交失败，交给动态事务恢复。 */
    private void requireOne(int count) {
        if (count != 1) throw new IllegalStateException("通知结果提交冲突");
    }

    /** 接受与送达是成功调度结果，接受本身并非最终送达。 */
    private boolean success(String status) { return "ACCEPTED".equals(status) || "DELIVERED".equals(status); }

    /** 调用前确定的配置/请求错误立即结束，不能伪装成待回执或自动重试。 */
    private boolean configFailure(String code) {
        return code != null && (code.startsWith("UNBOUND") || code.startsWith("ACCOUNT_") || code.startsWith("MISSING_")
            || code.startsWith("SMS_") || code.endsWith("_QUOTA")
            || "CONTENT_SNAPSHOT_REQUIRED".equals(code) || "UNKNOWN_PROVIDER".equals(code)
            || "INVALID_TEMPLATE_PARAMETERS".equals(code) || "LOCAL_DISPATCH_ERROR".equals(code)
            || "PROVIDER_UNSENT_TERMINAL".equals(code));
    }
}
