package org.namewta.test.oss.readiness;

import org.namewta.system.oss.readiness.OssStorageReadinessSchedulingConfiguration;
import org.namewta.system.oss.readiness.OssStorageReadinessService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OssStorageReadinessSchedulingUnitTest {

    @Test
    void continuouslyRenewsSnapshotBeforeItCanBecomeStale() throws Exception {
        assertThat(OssStorageReadinessSchedulingConfiguration.class.isAnnotationPresent(EnableScheduling.class))
            .isTrue();

        Method refresh = OssStorageReadinessService.class.getDeclaredMethod("refresh");
        Scheduled scheduled = refresh.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.initialDelayString()).isEqualTo("${oss.readiness.refresh-interval:PT1M}");
        assertThat(scheduled.fixedDelayString()).isEqualTo("${oss.readiness.refresh-interval:PT1M}");
    }
}
