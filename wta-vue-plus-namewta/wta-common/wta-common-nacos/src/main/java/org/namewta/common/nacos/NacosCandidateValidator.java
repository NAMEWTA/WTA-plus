package org.namewta.common.nacos;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class NacosCandidateValidator {

    private static final Set<String> BOOLEAN_KEYS = Set.of("captcha.enable");
    private static final Set<String> POSITIVE_INTEGER_KEYS = Set.of(
        "captcha.numberlength",
        "captcha.charlength"
    );
    private static final Set<String> POSITIVE_DURATION_KEYS = Set.of(
        "notify.idempotency.defaultwindow",
        "notify.idempotency.minwindow",
        "notify.idempotency.maxwindow",
        "oss.lifecycle.downloadttl"
    );

    private NacosCandidateValidator() {
    }

    static void validate(Map<String, Object> candidate, Environment environment) {
        candidate.forEach((key, value) -> {
            String normalized = normalize(key);
            if (normalized.equals("nacos.config") || normalized.startsWith("nacos.config.")
                || normalized.equals("spring.profiles") || normalized.startsWith("spring.profiles.")) {
                throw new NacosConfigValidationException("PROTECTED_KEY");
            }
            validateKnownScalar(normalized, value);
        });
        validateKnownParticipants(candidate, environment);
    }

    private static void validateKnownParticipants(Map<String, Object> candidate, Environment environment) {
        Map<String, Object> normalized = candidate.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(entry -> normalize(entry.getKey()), Map.Entry::getValue));
        try {
            if (normalized.keySet().stream().anyMatch(key -> key.startsWith("notify.idempotency."))) {
                Duration defaultWindow = duration(normalized, environment,
                    "notify.idempotency.defaultwindow", "notify.idempotency.default-window", "5m");
                Duration minWindow = duration(normalized, environment,
                    "notify.idempotency.minwindow", "notify.idempotency.min-window", "30s");
                Duration maxWindow = duration(normalized, environment,
                    "notify.idempotency.maxwindow", "notify.idempotency.max-window", "24h");
                if (!positive(defaultWindow) || !positive(minWindow) || !positive(maxWindow)
                    || maxWindow.compareTo(minWindow) < 0 || defaultWindow.compareTo(minWindow) < 0
                    || defaultWindow.compareTo(maxWindow) > 0) {
                    throw new IllegalArgumentException("invalid notify window");
                }
            }
            if (normalized.containsKey("oss.lifecycle.downloadttl")) {
                Duration downloadTtl = duration(normalized, environment,
                    "oss.lifecycle.downloadttl", "oss.lifecycle.download-ttl", "2m");
                Duration minTtl = duration(normalized, environment,
                    "oss.lifecycle.downloadttlmin", "oss.lifecycle.download-ttl-min", "1m");
                Duration maxTtl = duration(normalized, environment,
                    "oss.lifecycle.downloadttlmax", "oss.lifecycle.download-ttl-max", "10m");
                if (!positive(downloadTtl) || !positive(minTtl) || !positive(maxTtl)
                    || minTtl.compareTo(maxTtl) > 0 || downloadTtl.compareTo(minTtl) < 0
                    || downloadTtl.compareTo(maxTtl) > 0) {
                    throw new IllegalArgumentException("invalid OSS download TTL");
                }
            }
        } catch (RuntimeException ex) {
            throw new NacosConfigValidationException("PARTICIPANT_REJECTED", ex);
        }
    }

    private static Duration duration(Map<String, Object> candidate, Environment environment,
                                     String normalizedKey, String propertyKey, String defaultValue) {
        Object value = candidate.get(normalizedKey);
        if (value == null) {
            value = environment.getProperty(propertyKey, defaultValue);
        }
        Duration duration = ApplicationConversionService.getSharedInstance().convert(value, Duration.class);
        if (duration == null) {
            throw new IllegalArgumentException("duration is missing");
        }
        return duration;
    }

    private static boolean positive(Duration value) {
        return !value.isZero() && !value.isNegative();
    }

    private static void validateKnownScalar(String key, Object value) {
        try {
            if (BOOLEAN_KEYS.contains(key)) {
                String text = String.valueOf(value);
                if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
                    throw new IllegalArgumentException("not a boolean");
                }
            } else if (POSITIVE_INTEGER_KEYS.contains(key)) {
                if (Integer.parseInt(String.valueOf(value)) <= 0) {
                    throw new IllegalArgumentException("not positive");
                }
            } else if (POSITIVE_DURATION_KEYS.contains(key)) {
                Duration duration = ApplicationConversionService.getSharedInstance()
                    .convert(value, Duration.class);
                if (duration == null || duration.isZero() || duration.isNegative()) {
                    throw new IllegalArgumentException("not positive");
                }
            }
        } catch (RuntimeException ex) {
            throw new NacosConfigValidationException("KNOWN_TYPE_INVALID", ex);
        }
    }

    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }
}
