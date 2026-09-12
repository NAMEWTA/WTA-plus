package org.namewta.common.sms.notify;

import org.namewta.common.notify.model.NotifyContent;

/**
 * 已选定 SMS Provider 的发送接缝。
 */
@FunctionalInterface
public interface SmsNotificationSender {

    SmsNotificationReceipt send(String phone, NotifyContent content);
}
