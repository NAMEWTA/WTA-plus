package org.namewta.third.api;

/** 脱敏后的第三方调用响应，不携带原始凭据或完整 raw body。 */
public record ThirdPartyResponse<T>(
    String requestId,
    String providerCode,
    String endpointCode,
    int httpStatus,
    ThirdPartyFailureCategory category,
    String providerMessage,
    T body
) {
    public boolean isSuccess() {
        return category == ThirdPartyFailureCategory.NONE && httpStatus >= 200 && httpStatus < 300;
    }
}
