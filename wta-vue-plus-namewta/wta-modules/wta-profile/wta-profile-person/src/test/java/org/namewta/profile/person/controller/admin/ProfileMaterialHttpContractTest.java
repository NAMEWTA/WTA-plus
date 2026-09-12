package org.namewta.profile.person.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.common.log.annotation.Log;
import org.namewta.profile.person.controller.self.PersonMaterialSelfController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class ProfileMaterialHttpContractTest {

    @Test
    void materialControllersExposeNoPhysicalDeleteRoute() {
        assertThat(PersonMaterialAdminController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/profile/person/materials");
        assertThat(PersonMaterialSelfController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/profile/person/materials");
        assertThat(MaterialTagController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/profile/material-tags");
        assertThat(Arrays.stream(PersonMaterialAdminController.class.getDeclaredMethods()))
            .noneMatch(method -> method.getAnnotation(DeleteMapping.class) != null);
        assertThat(Arrays.stream(PersonMaterialSelfController.class.getDeclaredMethods()))
            .noneMatch(method -> method.getAnnotation(DeleteMapping.class) != null);
        assertThat(Arrays.stream(MaterialTagController.class.getDeclaredMethods()))
            .noneMatch(method -> method.getAnnotation(DeleteMapping.class) != null);
    }

    @Test
    void everyMaterialMutationExcludesRequestAndResponseBodiesFromOperationLogs() {
        assertSafeMutationLogs(PersonMaterialAdminController.class);
        assertSafeMutationLogs(PersonMaterialSelfController.class);
        assertSafeMutationLogs(MaterialTagController.class);
    }

    @Test
    void treeReadIsClosedForMaterialAndAdminCreateCapabilities() throws Exception {
        SaCheckPermission permission = MaterialTagController.class
            .getDeclaredMethod("tree", org.namewta.profile.api.material.ProfileMaterialPort.MaterialScope.class,
                boolean.class)
            .getAnnotation(SaCheckPermission.class);
        assertThat(permission.value()).contains(
            "profile:person:override", "profile:enterprise:override",
            "profile:person:material", "profile:enterprise:material");
    }

    private void assertSafeMutationLogs(Class<?> controller) {
        Arrays.stream(controller.getDeclaredMethods())
            .filter(method -> method.getAnnotation(PostMapping.class) != null)
            .forEach(method -> {
                Log log = method.getAnnotation(Log.class);
                assertThat(log).as(controller.getSimpleName() + "." + method.getName()).isNotNull();
                assertThat(log.isSaveRequestData()).isFalse();
                assertThat(log.isSaveResponseData()).isFalse();
            });
    }
}
