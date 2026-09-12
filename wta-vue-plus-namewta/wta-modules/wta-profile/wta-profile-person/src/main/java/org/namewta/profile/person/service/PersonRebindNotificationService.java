package org.namewta.profile.person.service;
import org.namewta.profile.person.dao.PersonNotificationAuditDao;
import org.namewta.profile.person.port.notification.PersonRebindNotificationPort;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.profile.person.domain.model.read.PersonNotificationAuditRow;
import org.namewta.profile.person.event.PersonReboundEvent;
import org.namewta.system.api.UserService;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
/**
 * 创建个人换绑通知服务。
 */
@Slf4j
@Service
public class PersonRebindNotificationService implements PersonRebindNotificationPort {
    static final String INTERNAL_TYPE = "PERSON_REBIND_INTERNAL";
    static final String SMS_TYPE = "PERSON_REBIND_SMS";
    static final String SAFE_TEXT = "您的个人实名认证绑定已变更。如非本人操作，请联系平台。";
    private final PersonNotificationAuditDao audits;
    private final NotificationApplicationService notifications;
    private final UserService users;
    /**
     * 处理personrebindnotificationservice。
     */
    public PersonRebindNotificationService(PersonNotificationAuditDao audits, NotificationApplicationService notifications,
                                            UserService users) {
        this.audits = audits;
        this.notifications = notifications;
        this.users = users;
    }
    /**
     * 暂存通知消息
     */
    public void stage(PersonReboundEvent event) {
        if (event == null) {
            return;
        }
        stageOne(INTERNAL_TYPE, event.personProfileId(), event.personApplicationId(), event.oldUserId());
        stageOne(SMS_TYPE, event.personProfileId(), event.personApplicationId(), event.oldUserId());
    }
    /**
     * 通知原账户绑定变更
     */
    public void notifyOldAccount(PersonReboundEvent event) {
        if (event == null) {
            return;
        }
        deliverStaged(INTERNAL_TYPE, event.personProfileId(), event.personApplicationId(), event.oldUserId());
        deliverStaged(SMS_TYPE, event.personProfileId(), event.personApplicationId(), event.oldUserId());
    }
    /**
     * 重试失败的认证尝试
     */

    public boolean retryFailed(long auditId) {
        if (auditId <= 0) {
            return false;
        }
        PersonNotificationAuditRow row = audits.lockRetryable(auditId);
        if (row == null || !isKnownType(row.getNotificationType())) {
            return false;
        }
        Delivery delivery = deliver(row.getNotificationType(), row.getProfileId(), row.getApplicationId(),
            row.getTargetUserId());
        row.setNotifyRequestId(delivery.requestId());
        row.setStatus(delivery.status());
        row.setFailureCategory(delivery.failureCategory());
        row.setOccurredTime(Instant.now());
        return audits.updateDelivery(row) == 1 && !"FAILED".equals(delivery.status());
    }
    /**
     * 执行第一阶段处理
     */
    private void stageOne(String type, long profileId, long applicationId, long userId) {
        PersonNotificationAuditRow row = new PersonNotificationAuditRow();
        row.setNotificationAuditId(IdGeneratorUtil.nextLongId());
        row.setNotificationType(type);
        row.setProfileId(profileId);
        row.setApplicationId(applicationId);
        row.setTargetUserId(userId);
        row.setNotifyRequestId(requestId(type, applicationId));
        row.setStatus("PENDING");
        row.setOccurredTime(Instant.now());
        if (audits.insertNotificationAudit(row) != 1) {
            throw new IllegalStateException("个人换绑通知待办写入失败");
        }
    }
    /**
     * 投递暂存通知消息
     */
    private void deliverStaged(String type, long profileId, long applicationId, long userId) {
        try {
            PersonNotificationAuditRow row = audits.selectRetryable(type, profileId, applicationId, userId);
            if (row == null) {
                log.error("个人换绑通知待办不存在，channel={}，category=PENDING_AUDIT_MISSING", type);
                return;
            }
            Delivery delivery = deliver(type, profileId, applicationId, userId);
            row.setNotifyRequestId(delivery.requestId());
            row.setStatus(delivery.status());
            row.setFailureCategory(delivery.failureCategory());
            row.setOccurredTime(Instant.now());
            if (audits.updateDelivery(row) != 1) {
                log.error("个人换绑通知结果更新失败，channel={}，category=AUDIT_UPDATE_CONFLICT", type);
            }
        } catch (RuntimeException exception) {
            log.error("个人换绑通知处理失败，channel={}，category=DELIVERY_PROCESS_FAILED", type);
        }
    }
    /**
     * 投递通知消息
     */
    private Delivery deliver(String type, long profileId, long applicationId, long userId) {
        try {
            if (INTERNAL_TYPE.equals(type)) {
                notifications.submit(new NotificationCommand("profile", "person-rebind", INTERNAL_TYPE,
                    Long.toString(applicationId), "USER", List.of(Long.toString(userId)), "person-rebind",
                    java.util.Map.of("title", "实名认证绑定变更通知", "content", SAFE_TEXT), List.of(NotificationChannel.IN_APP),
                    NotificationStrategy.ALL, NotificationMode.ASYNC, 60, null, null,
                    internalRequestId(applicationId), java.util.Map.of("audit", "SAFE_TEXT")));
                return new Delivery(internalRequestId(applicationId), "ACCEPTED", null);
            }
            if (SMS_TYPE.equals(type)) {
                return sendSms(profileId, applicationId, userId);
            }
            return new Delivery(null, "FAILED", "UNSUPPORTED_CHANNEL");
        } catch (RuntimeException exception) {
            log.warn("个人换绑通知失败，channel={}，category=DELIVERY_FAILED", type);
            return new Delivery(requestId(type, applicationId), "FAILED", "DELIVERY_FAILED");
        }
    }
    /**
     * 发送短信通知
     */
    private Delivery sendSms(long profileId, long applicationId, long userId) {
        String phone = text(users.selectPhonenumberById(userId));
        String requestId = requestId(SMS_TYPE, applicationId);
        if (phone == null) {
            return new Delivery(requestId, "SKIPPED", "TARGET_PHONE_UNAVAILABLE");
        }
        NotificationReceipt result = notifications.submit(new NotificationCommand("profile", "person-rebind", SMS_TYPE,
            Long.toString(applicationId), "PHONE", List.of(phone), "person-rebind",
            java.util.Map.of(), List.of(NotificationChannel.SMS),
            NotificationStrategy.ALL, NotificationMode.SYNC, 60, null, null,
            "profile:person:rebind:" + profileId + ":" + applicationId + ":sms", java.util.Map.of("audit", "REDACT_SENSITIVE")));
        if (result == null || result.status() == null) {
            return new Delivery(requestId, "FAILED", "EMPTY_NOTIFY_RESULT");
        }
        if (result.status() == NotificationStatus.ACCEPTED || result.status() == NotificationStatus.DELIVERED) {
            return new Delivery(result.notificationId(), "ACCEPTED", null);
        }
        return new Delivery(result.notificationId(), "FAILED", "NOTIFY_" + result.status().name());
    }
    /**
     * 生成内部请求编号
     */
    private String internalRequestId(long applicationId) {
        return "internal-person-rebind-" + applicationId;
    }
    /**
     * 生成外部请求编号
     */
    private String requestId(String type, long applicationId) {
        return (SMS_TYPE.equals(type) ? "sms" : "notify") + "-person-rebind-" + applicationId;
    }
    /**
     * 判断证件类型是否已知
     */
    private boolean isKnownType(String type) {
        return INTERNAL_TYPE.equals(type) || SMS_TYPE.equals(type);
    }
    /**
     * 规范化文本内容
     */
    private String text(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
    /**
     * 承载Delivery业务规则的领域服务。
     */
    private record Delivery(String requestId, String status, String failureCategory) {
    }
}
