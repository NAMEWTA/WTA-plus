package org.namewta.test.oss.migration;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.common.log.annotation.Log;
import org.namewta.system.controller.system.SysOssMigrationController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OssMigrationHttpContractUnitTest {

    @Test
    void exposesOnlyDedicatedAdminGetAndPostRoutes() {
        RequestMapping root = SysOssMigrationController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/resource/oss/migrations");
        Map<String, String> methods = Arrays.stream(SysOssMigrationController.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(GetMapping.class) != null
                || method.getAnnotation(PostMapping.class) != null)
            .collect(java.util.stream.Collectors.toMap(Method::getName, this::httpMethod));

        assertThat(methods).containsExactlyInAnyOrderEntriesOf(Map.of(
            "batch", "GET", "items", "GET", "dryRun", "POST", "start", "POST",
            "retry", "POST", "rollback", "POST", "cleanup", "POST"));
        assertThat(Arrays.stream(SysOssMigrationController.class.getDeclaredMethods()))
            .noneMatch(method -> method.getAnnotation(PutMapping.class) != null
                || method.getAnnotation(DeleteMapping.class) != null);
    }

    @Test
    void writeRoutesHaveDedicatedPermissionAndSafeAuditLog() {
        for (Method method : SysOssMigrationController.class.getDeclaredMethods()) {
            if (method.getAnnotation(PostMapping.class) == null) {
                continue;
            }
            SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
            Log log = method.getAnnotation(Log.class);
            assertThat(permission).isNotNull();
            assertThat(permission.value()).singleElement().asString().startsWith("system:ossMigration:");
            assertThat(log).isNotNull();
            assertThat(log.isSaveRequestData()).isFalse();
        }
    }

    private String httpMethod(Method method) {
        return method.getAnnotation(GetMapping.class) == null ? "POST" : "GET";
    }
}
