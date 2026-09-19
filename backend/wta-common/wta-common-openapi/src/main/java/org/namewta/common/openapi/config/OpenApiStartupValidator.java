package org.namewta.common.openapi.config;

import org.namewta.common.openapi.config.properties.OpenApiProperties;
import org.springframework.beans.factory.InitializingBean;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validates security-sensitive OpenAPI settings before any request can be served.
 */
final class OpenApiStartupValidator implements InitializingBean {

    private static final int KEK_BYTES = 32;
    private static final Pattern KEK_VERSION = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final OpenApiProperties properties;

    OpenApiStartupValidator(OpenApiProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public void afterPropertiesSet() {
        properties.maxBodyBytes();
        validateKekVersion(properties.getKekVersion());
        validateKek(properties.getKek());
        requirePositive(properties.getClockSkew(), "openapi.clock-skew");
        requirePositive(properties.getNonceTtl(), "openapi.nonce-ttl");
        requirePositive(properties.getMachineSessionTtl(), "openapi.machine-session-ttl");
        requirePositive(properties.getAppRateLimitPerMinute(), "openapi.app-rate-limit-per-minute");
        requirePositive(properties.getInterfaceRateLimitPerMinute(),
            "openapi.interface-rate-limit-per-minute");
    }

    private static void validateKekVersion(String version) {
        if (version == null || !KEK_VERSION.matcher(version).matches()) {
            throw new IllegalStateException(
                "openapi.kek-version must contain 1-64 letters, digits, dots, underscores, or hyphens");
        }
    }

    private static void validateKek(String encodedKek) {
        if (encodedKek == null || encodedKek.isBlank()) {
            throw new IllegalStateException("openapi.kek must be a Base64-encoded 32-byte key");
        }
        byte[] decoded = null;
        try {
            decoded = Base64.getDecoder().decode(encodedKek);
            if (decoded.length != KEK_BYTES) {
                throw new IllegalStateException("openapi.kek must be a Base64-encoded 32-byte key");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("openapi.kek must be a Base64-encoded 32-byte key", exception);
        } finally {
            if (decoded != null) {
                Arrays.fill(decoded, (byte) 0);
            }
        }
    }

    private static void requirePositive(Duration value, String property) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(property + " must be positive");
        }
    }

    private static void requirePositive(int value, String property) {
        if (value <= 0) {
            throw new IllegalStateException(property + " must be positive");
        }
    }
}
