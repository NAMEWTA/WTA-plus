package org.namewta.test.oss.readiness;

import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OssStorageReadinessPropertiesUnitTest {

    @Test
    void defaultsFailClosedAndUseBoundedSnapshots() throws Exception {
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        properties.afterPropertiesSet();

        assertThat(properties.isAllowEndpointDomainFallback()).isFalse();
        assertThat(properties.boundedDiagnosticTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getRefreshInterval()).isEqualTo("PT1M");
        assertThat(properties.resolvedMaxSnapshotAge()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void invalidOptionalValuesAreReportedByDiagnosisWithoutAbortingStartup() {
        OssStorageReadinessProperties timeout = new OssStorageReadinessProperties();
        timeout.setDiagnosticTimeout("PT1M");
        timeout.afterPropertiesSet();
        assertThat(timeout.diagnosticConfigurationValid()).isFalse();

        OssStorageReadinessProperties object = new OssStorageReadinessProperties();
        object.setDiagnosticObjects(Map.of("public", "../secret"));
        object.afterPropertiesSet();
        assertThat(object.diagnosticConfigurationValid()).isFalse();

        OssStorageReadinessProperties staleBeforeRefresh = new OssStorageReadinessProperties();
        staleBeforeRefresh.setRefreshInterval("PT5M");
        staleBeforeRefresh.afterPropertiesSet();
        assertThat(staleBeforeRefresh.diagnosticConfigurationValid()).isFalse();

        OssStorageReadinessProperties busyLoop = new OssStorageReadinessProperties();
        busyLoop.setRefreshInterval("PT0.05S");
        busyLoop.afterPropertiesSet();
        assertThat(busyLoop.diagnosticConfigurationValid()).isFalse();
    }

    @Test
    void realBinderAcceptsBadOptionalDurationWithoutConvertingItOrProbingProvider() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("oss.readiness.diagnostic-timeout", "not-a-duration")
            .withProperty("oss.readiness.refresh-interval", "also-invalid")
            .withProperty("oss.readiness.max-snapshot-age", "bad");
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        Binder.get(environment).bind("oss.readiness", Bindable.ofInstance(properties));
        properties.afterPropertiesSet();
        assertThat(properties.getDiagnosticTimeout()).isEqualTo("not-a-duration");
        assertThat(properties.diagnosticConfigurationValid()).isFalse();
        assertThat(properties.resolvedMaxSnapshotAge()).isEqualTo(Duration.ofMinutes(5));

        properties.setDiagnosticTimeout("3s");
        properties.setRefreshInterval("1m");
        properties.setMaxSnapshotAge("5m");
        assertThat(properties.diagnosticConfigurationValid()).isTrue();
    }
}
