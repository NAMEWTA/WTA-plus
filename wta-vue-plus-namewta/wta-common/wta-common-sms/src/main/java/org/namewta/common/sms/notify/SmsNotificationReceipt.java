package org.namewta.common.sms.notify;

/**
 * 单次短信 Provider 调用结果。
 */
public record SmsNotificationReceipt(
    boolean success,
    String providerMessageId,
    String errorCode,
    String errorMessage
) {

    public static SmsNotificationReceipt accepted() {
        return new SmsNotificationReceipt(true, null, null, null);
    }

    public static SmsNotificationReceipt failed(String errorCode, String errorMessage) {
        return new SmsNotificationReceipt(false, null, errorCode, errorMessage);
    }
}
