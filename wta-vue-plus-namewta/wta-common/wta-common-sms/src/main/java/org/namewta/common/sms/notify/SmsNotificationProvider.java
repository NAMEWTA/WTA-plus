package org.namewta.common.sms.notify;

/**
 * 一次逻辑通知选定的 SMS Provider。
 */
public record SmsNotificationProvider(String providerKey, SmsNotificationSender sender) {
}
