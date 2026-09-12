package org.namewta.profile.person.controller.admin;

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
class PersonAdminHttpContractTest {

    @Test
    void exposesClosedCapabilitiesAndSafeWriteAudit() {
        assertThat(PersonAdminController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/profile/person/archive");
        assertPermission("page", GetMapping.class, "profile:person:query");
        assertPermission("detail", GetMapping.class, "profile:person:query");
        assertPermission("reviewContext", GetMapping.class, "profile:person:review");
        assertPermission("reviewMaterial", GetMapping.class, "profile:person:review");
        assertPermission("material", GetMapping.class, "profile:person:material");
        assertPermission("eligibleUsers", GetMapping.class, "profile:person:override");
        assertPermission("decide", PostMapping.class, "profile:person:override");
        assertPermission("create", PostMapping.class, "profile:person:override");
        assertPermission("revise", PostMapping.class, "profile:person:override");
        assertPermission("assign", PostMapping.class, "profile:person:override");
        assertPermission("manageBinding", PostMapping.class, "profile:person:manage");
        assertPermission("revoke", PostMapping.class, "profile:person:override");

        Arrays.stream(PersonAdminController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .forEach(method -> {
                Log log = method.getAnnotation(Log.class);
                assertThat(log).as(method.getName()).isNotNull();
                assertThat(log.isSaveRequestData()).isFalse();
                assertThat(log.isSaveResponseData()).isFalse();
            });
        assertThat(Arrays.stream(PersonAdminController.class.getDeclaredMethods())
            .map(Method::getName)).doesNotContain("export", "delete");
    }

    private void assertPermission(String methodName, Class<?> mappingType, String permission) {
        Method method = Arrays.stream(PersonAdminController.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
        assertThat(method.isAnnotationPresent((Class) mappingType)).isTrue();
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
    }
}
