package org.namewta.notify.port;

/** 已鉴权原生查询的结果事务入口，不作为HTTP入口公开。 */
public interface SmsReceiptResultPort {
    /** 仅接受DELIVERED/UNDELIVERABLE，同结果重复确认不得重复生效。 */
    void confirm(String supplier, String providerKey, String messageId, String target, String status);
}
