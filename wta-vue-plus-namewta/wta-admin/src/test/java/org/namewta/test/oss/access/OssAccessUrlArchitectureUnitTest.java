package org.namewta.test.oss.access;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OssAccessUrlArchitectureUnitTest {

    @Test
    void urlGenerationHasOneClassificationPathAndNoAnonymousDiscoveryRoute() throws Exception {
        Path root = repositoryRoot();
        String service = Files.readString(root.resolve(
            "wta-modules/wta-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java"));
        String lifecycle = Files.readString(root.resolve(
            "wta-modules/wta-system/src/main/java/org/dromara/system/oss/service/OssLifecycleManager.java"));
        String controller = Files.readString(root.resolve(
            "wta-modules/wta-system/src/main/java/org/dromara/system/controller/system/SysOssController.java"));

        assertThat(service)
            .contains("lifecycleManager.resolveAccessUrl")
            .doesNotContain("matchingUrl", "presignGetUrl", "AccessPolicy.PRIVATE");
        assertThat(lifecycle).contains("readinessRegistry.requireServing", "objectStore.accessPolicy");
        assertThat(controller)
            .contains("@SaCheckPermission(\"system:oss:download\")", "resolveAccessUrl")
            .doesNotContain("anonymous", "public/{ossId}");
    }

    private Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"));
        return current.getFileName().toString().equals("wta-admin") ? current.getParent() : current;
    }
}
