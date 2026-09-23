package org.namewta.common.sms.notify;

import org.namewta.common.notify.model.NotifyDeliveryStatus;

/**
 * 单次短信 Provider 调用结果。
 */
public record SmsNotificationReceipt(
    boolean success,
    String providerMessageId,
    String errorCode,
    String errorMessage,
    NotifyDeliveryStatus outcome
) {

    /** 已受理；null流水号表示无法关联回执，不能由此推导应重新发送。 */
    public static SmsNotificationReceipt accepted(String providerMessageId) {
        return new SmsNotificationReceipt(true, providerMessageId, null, null, NotifyDeliveryStatus.ACCEPTED);
    }

    public static SmsNotificationReceipt failed(String errorCode, String errorMessage) {
        return new SmsNotificationReceipt(false, null, errorCode, errorMessage, NotifyDeliveryStatus.FAILED);
    }

    /** 仅受控单次请求收到供应商结构化明确拒绝时创建，不接受自由错误文本推断。 */
    public static SmsNotificationReceipt unsentRetryable(String fixedCode) {
        return new SmsNotificationReceipt(false, null, fixedCode, "SMS Provider 明确未受理",
            NotifyDeliveryStatus.UNSENT_RETRYABLE);
    }
}
