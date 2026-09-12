package org.namewta.notify.port;

/** 供应商回调更新端口，验签和事务由 UseCase 负责。 */
public interface ProviderCallbackPort {
    void apply(String channel, String providerKey, String providerMessageId, String status, String eventId);
}
