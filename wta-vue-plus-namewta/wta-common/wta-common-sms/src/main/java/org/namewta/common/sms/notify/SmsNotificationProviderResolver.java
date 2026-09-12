package org.namewta.common.sms.notify;

/**
 * SMS Provider 选择接缝。
 */
@FunctionalInterface
public interface SmsNotificationProviderResolver {

    SmsNotificationProvider resolve(String requestedProviderKey);
}
