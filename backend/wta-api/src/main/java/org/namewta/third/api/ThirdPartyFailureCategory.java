package org.namewta.third.api;

/** 第三方调用的稳定失败分类。 */
public enum ThirdPartyFailureCategory {
    NONE,
    PROVIDER_DISABLED,
    ENDPOINT_DISABLED,
    RATE_LIMITED,
    CONFIG_UNAVAILABLE,
    REJECTED,
    TRANSPORT,
    TIMEOUT,
    HTTP,
    PROVIDER,
    DECODE
}
