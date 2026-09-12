package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.port.provider.PersonVerificationProvider;

import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonProviderAttemptStatus;
import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonProviderStartCommand;
import org.namewta.profile.person.domain.verification.PersonProviderStartResult;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.namewta.profile.person.domain.verification.PersonVerifiedCallback;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

public final class PersonDeterministicTestProvider implements PersonVerificationProvider {

    private static final long MAX_SKEW_SECONDS = 300;
    private final byte[] secret;

    public PersonDeterministicTestProvider(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String providerCode() {
        return "test-provider";
    }

    @Override
    public PersonProviderStartResult start(PersonProviderStartCommand command) {
        return new PersonProviderStartResult(
            "person-" + command.applicationId() + "-" + command.attemptNo(),
            PersonProviderAttemptStatus.PENDING, null, null, null, null);
    }

    @Override
    public PersonVerifiedCallback authenticate(PersonProviderCallbackEnvelope callback, Instant receivedAt) {
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
        return new PersonVerifiedCallback(
            callback.providerRequestId(),
            HexFormat.of().formatHex(sha256(callback.payload())),
            PersonProviderAttemptStatus.SUCCEEDED,
            callback.payload(),
            "{\"provider\":\"test-provider\"}",
            null,
            receivedAt);
    }

    public String sign(String providerRequestId, long timestampEpochSecond, String payload) {
        return HexFormat.of().formatHex(hmac(
            providerRequestId + "." + timestampEpochSecond + "." + payload));
    }

    private String canonical(PersonProviderCallbackEnvelope callback) {
        return callback.providerRequestId() + "." + callback.timestampEpochSecond() + "." + callback.payload();
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException failure) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.PROVIDER_FAILURE,
                "Test provider authentication is unavailable",
                failure);
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException failure) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.PROVIDER_FAILURE,
                "Test provider digest is unavailable",
                failure);
        }
    }

    private PersonVerificationException invalidSignature() {
        return new PersonVerificationException(
            PersonVerificationFailureCategory.INVALID_SIGNATURE, "Person provider signature is invalid");
    }

    private PersonVerificationException expired() {
        return new PersonVerificationException(
            PersonVerificationFailureCategory.EXPIRED_CALLBACK, "Person provider callback is expired");
    }
}
