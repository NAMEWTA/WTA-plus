package org.namewta.test.oss.release;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.system.controller.system.SysOssController;
import org.namewta.system.controller.system.SysOssMigrationController;
import org.namewta.system.oss.upload.OssUploadContracts.InitRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 发布候选的禁止能力与授权面收缩检查。
 */
@Tag("dev")
class OssReleaseContractionUnitTest {

    @Test
    void exposesOnlyTwoPoliciesAndKeepsStorageRoutingServerOwned() {
        assertThat(AccessPolicy.values()).extracting(Enum::name)
            .containsExactly("PRIVATE", "PUBLIC_READ");

        Set<String> initFields = Arrays.stream(InitRequest.class.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        assertThat(initFields)
            .containsExactlyInAnyOrder("policy", "fileName", "fileSize", "contentType", "fingerprint")
            .doesNotContain("service", "storageConfigKey", "configKey", "bucket", "accessPolicy", "ttl");
    }

    @Test
    void protectsEveryOssManagementRouteAndKeepsMigrationCommandsPostOnly() {
        assertProtected(SysOssController.class);
        assertProtected(SysOssMigrationController.class);
        assertThat(SysOssController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/resource/oss");
        assertThat(SysOssMigrationController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/resource/oss/migrations");

        Set<String> commandNames = Set.of("dryRun", "start", "retry", "rollback", "cleanup");
        Arrays.stream(SysOssMigrationController.class.getDeclaredMethods())
            .filter(method -> commandNames.contains(method.getName()))
            .forEach(method -> {
                assertThat(method.getAnnotation(PostMapping.class)).as(method.getName()).isNotNull();
                Log log = method.getAnnotation(Log.class);
                assertThat(log).as(method.getName()).isNotNull();
                assertThat(log.isSaveRequestData()).as(method.getName()).isFalse();
            });
    }

    private void assertProtected(Class<?> controller) {
        Arrays.stream(controller.getDeclaredMethods())
            .filter(method -> !method.isBridge())
            .forEach(method -> assertThat(method.getAnnotation(SaCheckPermission.class))
                .as(controller.getSimpleName() + "." + method.getName()).isNotNull());
    }
}
