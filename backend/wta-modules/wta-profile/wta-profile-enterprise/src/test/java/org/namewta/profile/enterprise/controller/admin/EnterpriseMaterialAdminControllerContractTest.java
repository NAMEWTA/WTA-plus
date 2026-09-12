package org.namewta.profile.enterprise.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.enterprise.usecase.ProfileMaterialUseCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Tag("dev")
class EnterpriseMaterialAdminControllerContractTest {

    @Test
    void exposesReadOnlyAdministrativeMaterialRoutes() {
        RequestMapping mapping = EnterpriseMaterialAdminController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/profile/enterprise/materials");
        Method[] methods = EnterpriseMaterialAdminController.class.getDeclaredMethods();
        assertThat(Arrays.stream(methods).filter(method -> method.isAnnotationPresent(GetMapping.class))).hasSize(2);
        assertThat(Arrays.stream(methods)).noneMatch(method ->
            method.isAnnotationPresent(PostMapping.class) || method.isAnnotationPresent(DeleteMapping.class));
        Arrays.stream(methods).filter(method -> method.isAnnotationPresent(GetMapping.class)).forEach(method -> {
            SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
            assertThat(permission.mode()).isEqualTo(SaMode.OR);
            assertThat(permission.value()).containsExactlyInAnyOrder(
                "profile:enterprise:material", "profile:enterprise:query", "profile:enterprise:review",
                "profile:enterprise:manage", "profile:enterprise:override");
        });
    }

    @Test
    void fixesMaterialOwnersToEnterpriseProfileType() {
        EnterpriseMaterialAdminController controller =
            new EnterpriseMaterialAdminController(mock(ProfileMaterialUseCase.class));
        assertThat(controller.profileType()).isEqualTo(ProfileType.ENTERPRISE);
    }
}
