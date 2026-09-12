package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.domain.exception.EnterpriseVerificationException;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderAttemptStatus;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderCallbackEnvelope;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartCommand;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartResult;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationFailureCategory;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerifiedCallback;
import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProvider;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

public final class EnterpriseDeterministicTestProvider implements EnterpriseVerificationProvider {

    private static final long MAX_SKEW_SECONDS = 300;
    private final byte[] secret;

    public EnterpriseDeterministicTestProvider(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String providerCode() {
        return "test-provider";
    }

    @Override
    public EnterpriseProviderStartResult start(EnterpriseProviderStartCommand command) {
        return new EnterpriseProviderStartResult(
            "enterprise-" + command.applicationId() + "-" + command.attemptNo(),
            EnterpriseProviderAttemptStatus.PENDING, null, null, null, null);
    }

    @Override
    public EnterpriseVerifiedCallback authenticate(EnterpriseProviderCallbackEnvelope callback, Instant receivedAt) {
        long skew;
        try {
            skew = Math.abs(Math.subtractExact(receivedAt.getEpochSecond(), callback.timestampEpochSecond()));
        } catch (ArithmeticException failure) {
            throw expired();
        }
        if (skew > MAX_SKEW_SECONDS) {
            throw expired();
        }
        byte[] expected = hmac(canonical(callback));
        byte[] supplied;
        try {
            supplied = HexFormat.of().parseHex(callback.signature());
        } catch (RuntimeException failure) {
            throw invalidSignature();
        }
        if (!MessageDigest.isEqual(expected, supplied)) {
            throw invalidSignature();
        }
        return new EnterpriseVerifiedCallback(
            callback.providerRequestId(),
            HexFormat.of().formatHex(sha256(callback.payload())),
            EnterpriseProviderAttemptStatus.SUCCEEDED,
            callback.payload(),
            "{\"provider\":\"test-provider\"}",
            null,
            receivedAt);
    }

    public String sign(String providerRequestId, long timestampEpochSecond, String payload) {
        return HexFormat.of().formatHex(hmac(
            providerRequestId + "." + timestampEpochSecond + "." + payload));
    }

    private String canonical(EnterpriseProviderCallbackEnvelope callback) {
        return callback.providerRequestId() + "." + callback.timestampEpochSecond() + "." + callback.payload();
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException failure) {
            throw new EnterpriseVerificationException(
                EnterpriseVerificationFailureCategory.PROVIDER_FAILURE,
                "Test provider authentication is unavailable",
                failure);
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException failure) {
            throw new EnterpriseVerificationException(
                EnterpriseVerificationFailureCategory.PROVIDER_FAILURE,
                "Test provider digest is unavailable",
                failure);
        }
    }

    private EnterpriseVerificationException invalidSignature() {
        return new EnterpriseVerificationException(
            EnterpriseVerificationFailureCategory.INVALID_SIGNATURE,
            "Enterprise provider signature is invalid");
    }

    private EnterpriseVerificationException expired() {
        return new EnterpriseVerificationException(
            EnterpriseVerificationFailureCategory.EXPIRED_CALLBACK,
            "Enterprise provider callback is expired");
    }
}
