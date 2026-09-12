package org.namewta.profile.enterprise.controller.self;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.common.log.annotation.Log;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class EnterpriseTransferHttpContractTest {

    @Test
    void exposesOnlyThreeSelfServicePostsUnderTheEnterpriseApplyCapability() {
        RequestMapping root = EnterpriseTransferController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/profile/enterprise/transfer");
        Method[] posts = Arrays.stream(EnterpriseTransferController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .toArray(Method[]::new);
        assertThat(posts).hasSize(3);
        assertThat(posts).allSatisfy(method -> {
            assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("profile:enterprise:apply");
            Log log = method.getAnnotation(Log.class);
            assertThat(log).isNotNull();
            assertThat(log.isSaveRequestData()).isFalse();
            assertThat(log.isSaveResponseData()).isFalse();
        });
        assertThat(Arrays.stream(posts)
            .flatMap(method -> Arrays.stream(method.getAnnotation(PostMapping.class).value())))
            .containsExactlyInAnyOrder("/send", "/confirm", "/unbind");
    }
}
