package org.namewta.profile.enterprise.domain.verification;

/** EnterpriseProviderCallbackEnvelope 认证领域模型。 */
public record EnterpriseProviderCallbackEnvelope(
    String providerRequestId,
    long timestampEpochSecond,
    String payload,
    String signature
) {
}
