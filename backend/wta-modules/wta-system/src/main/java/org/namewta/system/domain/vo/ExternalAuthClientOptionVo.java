package org.namewta.system.domain.vo;

/** 接入配置专用的跨 App 目录；仅返回公开标识和入口状态，不暴露 Client 密钥或普通业务授权。 */
public record ExternalAuthClientOptionVo(
    String clientId, String clientKey, String status, boolean socialEnabled,
    boolean registerEnabled, long providerCount, long enabledProviderCount, String unavailableReason) {
}
