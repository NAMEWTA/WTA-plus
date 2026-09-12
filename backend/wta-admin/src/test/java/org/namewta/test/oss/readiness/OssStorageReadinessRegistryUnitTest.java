package org.namewta.test.oss.readiness;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class OssStorageReadinessRegistryUnitTest {

    @Test
    void springSelectsTheProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(OssStorageReadinessProperties.class);
            context.register(OssStorageReadinessRegistry.class);
            context.refresh();

            assertThat(context.getBean(OssStorageReadinessRegistry.class)).isNotNull();
        }
    }

    @Test
    void onlyRequiredFailuresLowerOverallReadiness() {
        Instant now = Instant.parse("2026-09-01T00:00:00Z");
        OssStorageReadinessRegistry registry = registry(now, Duration.ofMinutes(5));
        registry.replace(Map.of(
            "private", entry("private", true, OssStorageReadinessEntry.Status.SERVING, now),
            "placeholder", entry("placeholder", false, OssStorageReadinessEntry.Status.NOT_SERVING, now)
        ), Set.of("private"), true);

        assertThat(registry.overallServing()).isTrue();
        assertThat(registry.isServing("placeholder")).isFalse();
        registry.requireServing("private");
    }

    @Test
    void missingStaleAndDiscoveryFailureFailClosed() {
        Instant now = Instant.parse("2026-09-01T00:00:00Z");
        OssStorageReadinessRegistry stale = registry(now.plus(Duration.ofMinutes(6)), Duration.ofMinutes(5));
        stale.replace(Map.of("private", entry("private", true,
            OssStorageReadinessEntry.Status.SERVING, now)), Set.of("private"), true);

        assertThat(stale.overallServing()).isFalse();
        assertThat(stale.snapshot().get("private").reason()).isEqualTo(OssStorageReadinessEntry.Reason.STALE);
        assertThatThrownBy(() -> stale.requireServing("private"))
            .isInstanceOf(ServiceException.class).hasMessageContaining("STALE");
        assertThatThrownBy(() -> stale.requireServing("missing"))
            .isInstanceOf(ServiceException.class).hasMessageContaining("MISSING");

        OssStorageReadinessRegistry failed = registry(now, Duration.ofMinutes(5));
        failed.replace(Map.of(), Set.of(), false);
        assertThat(failed.overallServing()).isFalse();
    }

    private OssStorageReadinessRegistry registry(Instant now, Duration maxAge) {
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        properties.setMaxSnapshotAge(maxAge);
        return new OssStorageReadinessRegistry(properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private OssStorageReadinessEntry entry(String key, boolean required,
                                           OssStorageReadinessEntry.Status status, Instant checkedAt) {
        return new OssStorageReadinessEntry(key, AccessPolicy.PRIVATE, required,
            required ? Set.of("DEFAULT") : Set.of(), status,
            status == OssStorageReadinessEntry.Status.SERVING
                ? OssStorageReadinessEntry.Reason.READY : OssStorageReadinessEntry.Reason.DIAGNOSTIC_UNVERIFIED,
            checkedAt);
    }
}
