package org.namewta.profile.enterprise.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.common.log.annotation.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class EnterpriseAdminHttpContractTest {

    @Test
    void exposesClosedCapabilitiesAndSafeWriteAudit() {
        assertThat(EnterpriseAdminController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/profile/enterprise/archive");
        assertPermission("page", GetMapping.class, "profile:enterprise:query");
        assertPermission("detail", GetMapping.class, "profile:enterprise:query");
        assertPermission("reviewContext", GetMapping.class, "profile:enterprise:review");
        assertPermission("reviewMaterial", GetMapping.class, "profile:enterprise:review");
        assertPermission("material", GetMapping.class, "profile:enterprise:material");
        assertPermission("eligibleUsers", GetMapping.class, "profile:enterprise:override");
        assertPermission("decide", PostMapping.class, "profile:enterprise:override");
        assertPermission("create", PostMapping.class, "profile:enterprise:override");
        assertPermission("revise", PostMapping.class, "profile:enterprise:override");
        assertPermission("assign", PostMapping.class, "profile:enterprise:override");
        assertThat(method("assign").getAnnotation(Log.class).title()).isEqualTo("管理员指定企业档案负责人");
        assertPermission("manageBinding", PostMapping.class, "profile:enterprise:manage");
        assertPermission("revoke", PostMapping.class, "profile:enterprise:override");

        Arrays.stream(EnterpriseAdminController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .forEach(method -> {
                Log log = method.getAnnotation(Log.class);
                assertThat(log).as(method.getName()).isNotNull();
                assertThat(log.isSaveRequestData()).isFalse();
                assertThat(log.isSaveResponseData()).isFalse();
            });
        assertThat(Arrays.stream(EnterpriseAdminController.class.getDeclaredMethods())
            .map(Method::getName)).doesNotContain("export", "delete");
    }

    private void assertPermission(String methodName, Class<?> mappingType, String permission) {
        Method method = method(methodName);
        assertThat(method.isAnnotationPresent((Class) mappingType)).isTrue();
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
    }

    private Method method(String methodName) {
        return Arrays.stream(EnterpriseAdminController.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
    }
}
