package org.namewta.test.oss.readiness;

import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class OssStorageReadinessPropertiesUnitTest {

    @Test
    void defaultsFailClosedAndUseBoundedSnapshots() throws Exception {
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        properties.afterPropertiesSet();

        assertThat(properties.isAllowEndpointDomainFallback()).isFalse();
        assertThat(properties.getDiagnosticTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getRefreshInterval()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.getMaxSnapshotAge()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void rejectsUnsafeDurationsAndDiagnosticObjectKeys() {
        OssStorageReadinessProperties timeout = new OssStorageReadinessProperties();
        timeout.setDiagnosticTimeout(Duration.ofMinutes(1));
        assertThatThrownBy(timeout::afterPropertiesSet).isInstanceOf(IllegalStateException.class);

        OssStorageReadinessProperties object = new OssStorageReadinessProperties();
        object.setDiagnosticObjects(Map.of("public", "../secret"));
        assertThatThrownBy(object::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("诊断对象配置无效");

        OssStorageReadinessProperties staleBeforeRefresh = new OssStorageReadinessProperties();
        staleBeforeRefresh.setRefreshInterval(Duration.ofMinutes(5));
        assertThatThrownBy(staleBeforeRefresh::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("refreshInterval");

        OssStorageReadinessProperties busyLoop = new OssStorageReadinessProperties();
        busyLoop.setRefreshInterval(Duration.ofMillis(50));
        assertThatThrownBy(busyLoop::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("refreshInterval");
    }
}
