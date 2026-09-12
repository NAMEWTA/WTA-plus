package org.namewta.profile.enterprise.controller.self;

import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSaveBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationProbeBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationProbeVo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSubmitBo;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseApplicationUseCaseImpl;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.common.log.annotation.Log;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class EnterpriseApplicationHttpContractTest {

    @Test
    void exposesCurrentUserOnlyRoutesAndNeverLogsIdentityBodies() {
        RequestMapping root = EnterpriseApplicationController.class.getAnnotation(RequestMapping.class);
        assertThat(root.value()).containsExactly("/profile/enterprise/application");
        assertThat(EnterpriseApplicationSaveBo.class.getRecordComponents())
            .extracting(component -> component.getName())
            .doesNotContain("userId", "applicantUserId");

        for (Method method : EnterpriseApplicationController.class.getDeclaredMethods()) {
            SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
            assertThat(permission).isNotNull();
            assertThat(permission.value()).containsExactly("profile:enterprise:apply");
            if (method.isAnnotationPresent(PostMapping.class)) {
                Log log = method.getAnnotation(Log.class);
                assertThat(log).isNotNull();
                assertThat(log.isSaveRequestData()).isFalse();
                assertThat(log.isSaveResponseData()).isFalse();
            }
        }
    }

    @Test
    void separatesReadSaveAndSubmitCommands() throws Exception {
        assertThat(EnterpriseApplicationController.class.getMethod("current").getAnnotation(GetMapping.class))
            .isNotNull();
        assertThat(EnterpriseApplicationController.class.getMethod("save", EnterpriseApplicationSaveBo.class)
            .getAnnotation(PostMapping.class).value()).isEmpty();
        assertThat(EnterpriseApplicationController.class.getMethod("submit", EnterpriseApplicationSubmitBo.class)
            .getAnnotation(PostMapping.class).value()).containsExactly("/submit");
        assertThat(EnterpriseApplicationController.class.getMethod("probe", EnterpriseApplicationProbeBo.class)
            .getAnnotation(PostMapping.class).value()).containsExactly("/probe");
        assertThat(EnterpriseApplicationProbeVo.class.getRecordComponents())
            .extracting(component -> component.getName()).containsExactly("status");
        assertThat(EnterpriseApplicationUseCaseImpl.class.getMethod("save", long.class, EnterpriseApplicationSaveBo.class)
            .isAnnotationPresent(DSTransactional.class)).isTrue();
        assertThat(EnterpriseApplicationUseCaseImpl.class.getMethod("submit", long.class, int.class)
            .isAnnotationPresent(DSTransactional.class)).isTrue();
        assertThat(EnterpriseApplicationUseCaseImpl.class.getMethod("handleProcess",
            org.namewta.profile.enterprise.domain.application.EnterpriseApplicationProcessCommand.class)
            .isAnnotationPresent(DSTransactional.class)).isTrue();
    }
}
