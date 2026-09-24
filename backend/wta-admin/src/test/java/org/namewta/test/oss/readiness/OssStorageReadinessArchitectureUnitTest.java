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

        assertReadOnlyDiagnosticClosure(common);
        assertThat(readiness).doesNotContain("accessKey", "secretKey", "getEndpoint");
        assertThat(health).doesNotContain("endpoint", "domainUrl", "secretKey", "accessKey", "policyText");
    }

    @Test
    void diagnosticClosureRejectsObjectAndBucketWrites() throws Exception {
        String common = Files.readString(repositoryRoot().resolve(
            "wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java"));
        String diagnostic = diagnosticClosure(common);
        assertThat(common).contains("public PutObjectResult uploadBounded(");
        for (String mutation : new String[]{"putObject(", "deleteObject(", "putBucketPolicy(",
            "deleteBucketPolicy(", "createBucket("}) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> assertReadOnlyDiagnosticClosure(
                common.replace(diagnostic, diagnostic + "\n s3AsyncClient." + mutation)))
                .isInstanceOf(AssertionError.class);
        }
    }

    private void assertReadOnlyDiagnosticClosure(String common) {
        String diagnostic = diagnosticClosure(common);
        assertThat(diagnostic)
            .contains("getBucketPolicy", "getBucketAcl", "HttpRequest.BodyPublishers.noBody()")
            .doesNotContain("putObject(", "deleteObject(", "putBucketPolicy(",
                "deleteBucketPolicy(", "createBucket(");
    }

    private String diagnosticClosure(String common) {
        int start = common.indexOf("public OssAccessDiagnostic diagnoseAccess(");
        int end = common.indexOf("public OssBucketConfiguration bucketConfiguration()", start);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        // 当前诊断入口及其只读辅助方法连续定义；后面的 bounded upload 不属于诊断闭包。
        return common.substring(start, end);
    }

    private Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"));
        return current.getFileName().toString().equals("wta-admin") ? current.getParent() : current;
    }
}
