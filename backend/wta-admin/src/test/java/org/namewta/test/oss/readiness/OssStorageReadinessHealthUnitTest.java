package org.namewta.test.oss.readiness;

import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessHealthIndicator;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OssStorageReadinessHealthUnitTest {

    @Test
    void healthContainsActionableCategoriesWithoutProviderSecrets() {
        OssStorageReadinessRegistry registry = new OssStorageReadinessRegistry(new OssStorageReadinessProperties());
        registry.replace(Map.of("public", new OssStorageReadinessEntry(
            "public", AccessPolicy.PUBLIC_READ, true, Set.of("UPLOAD_POLICY:portal"),
            OssStorageReadinessEntry.Status.NOT_SERVING,
            OssStorageReadinessEntry.Reason.PROVIDER_MISMATCH, Instant.now())), Set.of("public"), true);

        var health = new OssStorageReadinessHealthIndicator(registry).health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails().toString())
            .contains("public", "PROVIDER_MISMATCH", "UPLOAD_POLICY:portal")
            .doesNotContain("access-key", "secret", "policyText", "X-Amz-Signature", "endpoint");
    }
}
