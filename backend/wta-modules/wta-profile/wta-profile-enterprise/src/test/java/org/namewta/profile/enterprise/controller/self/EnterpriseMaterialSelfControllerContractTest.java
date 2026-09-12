package org.namewta.profile.enterprise.controller.self;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.common.log.annotation.Log;
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
class EnterpriseMaterialSelfControllerContractTest {

    @Test
    void exposesOnlyAuthenticatedSelfServiceMaterialWrites() {
        RequestMapping mapping = EnterpriseMaterialSelfController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/profile/enterprise/materials");
        Method[] methods = EnterpriseMaterialSelfController.class.getDeclaredMethods();
        Method[] posts = Arrays.stream(methods)
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .toArray(Method[]::new);
        assertThat(posts).hasSize(2);
        assertThat(Arrays.stream(methods)).noneMatch(method ->
            method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(DeleteMapping.class));
        assertThat(posts).allSatisfy(method -> {
            assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("profile:enterprise:material");
            Log log = method.getAnnotation(Log.class);
            assertThat(log).isNotNull();
            assertThat(log.isSaveRequestData()).isFalse();
            assertThat(log.isSaveResponseData()).isFalse();
        });
    }

    @Test
    void fixesMaterialOwnersToEnterpriseProfileType() {
        EnterpriseMaterialSelfController controller =
            new EnterpriseMaterialSelfController(mock(ProfileMaterialUseCase.class));
        assertThat(controller.profileType()).isEqualTo(ProfileType.ENTERPRISE);
    }
}
