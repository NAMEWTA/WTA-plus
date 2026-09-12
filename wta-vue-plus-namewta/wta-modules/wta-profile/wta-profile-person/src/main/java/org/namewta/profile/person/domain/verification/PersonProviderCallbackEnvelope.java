package org.namewta.profile.person.domain.verification;

/** PersonProviderCallbackEnvelope 认证领域模型。 */
public record PersonProviderCallbackEnvelope(
    String providerRequestId,
    long timestampEpochSecond,
    String payload,
    String signature
) {
}
