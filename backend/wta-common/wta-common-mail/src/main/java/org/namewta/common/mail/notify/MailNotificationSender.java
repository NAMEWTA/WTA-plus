package org.namewta.common.mail.notify;

/**
 * 邮件 Provider 调用接缝。
 */
@FunctionalInterface
public interface MailNotificationSender {

    String send(MailNotificationMessage message);
}
