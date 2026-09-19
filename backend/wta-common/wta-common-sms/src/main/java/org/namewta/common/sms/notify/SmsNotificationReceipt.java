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

    /** 已受理；null流水号表示无法关联回执，不能由此推导应重新发送。 */
    public static SmsNotificationReceipt accepted(String providerMessageId) {
        return new SmsNotificationReceipt(true, providerMessageId, null, null);
    }

    public static SmsNotificationReceipt failed(String errorCode, String errorMessage) {
        return new SmsNotificationReceipt(false, null, errorCode, errorMessage);
    }
}
