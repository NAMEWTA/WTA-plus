package org.namewta.test.oss.readiness;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OssStorageReadinessArchitectureUnitTest {

    @Test
    void productionReadinessDoesNotMutateProviderOrExposeAnonymousEndpoints() throws Exception {
        Path root = repositoryRoot();
        String common = Files.readString(root.resolve(
            "wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java"));
        String readiness = Files.readString(root.resolve(
            "wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessService.java"));
        String health = Files.readString(root.resolve(
            "wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessHealthIndicator.java"));

        assertThat(common)
            .contains("getBucketPolicy", "getBucketAcl", "HttpRequest.BodyPublishers.noBody()")
            .doesNotContain("putBucketPolicy", "deleteBucketPolicy", "createBucket(", "putObject(");
        assertThat(readiness).doesNotContain("accessKey", "secretKey", "getEndpoint");
        assertThat(health).doesNotContain("endpoint", "domainUrl", "secretKey", "accessKey", "policyText");
    }

    private Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"));
        return current.getFileName().toString().equals("wta-admin") ? current.getParent() : current;
    }
}
