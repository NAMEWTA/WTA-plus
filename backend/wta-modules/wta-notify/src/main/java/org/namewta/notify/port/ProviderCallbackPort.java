package org.namewta.notify.port;

/** 供应商回调更新端口，验签和事务由 UseCase 负责。 */
public interface ProviderCallbackPort {
    /** 多收件人共享消息编号时，target 必须准确标识收件人；空值仅允许唯一投递。 */
    void apply(String channel, String providerKey, String providerMessageId, String status, String eventId, String target);
}
