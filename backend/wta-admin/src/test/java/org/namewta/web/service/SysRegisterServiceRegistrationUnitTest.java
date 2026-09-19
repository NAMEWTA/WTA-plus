package org.namewta.web.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.web.config.properties.CaptchaProperties;
import org.namewta.system.api.model.RegisterBody;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysClientService;
import org.namewta.system.service.ISysUserService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.namewta.system.service.ISysUserTypeService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 前端入口不授予注册权限，直接调用仍必须先由服务端 Client 策略拒绝。 */
@Tag("dev")
class SysRegisterServiceRegistrationUnitTest {

    @Test
    void disabledRegistrationRejectsBeforeCaptchaConsumptionOrPersistence() {
        assertRegistrationDenied(Boolean.FALSE);
    }

    @Test
    void missingRegistrationFlagFailsClosedBeforeCaptchaConsumptionOrPersistence() {
        assertRegistrationDenied(null);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "12345", "12800138000"})
    void invalidPhoneRejectsBeforeCaptchaConsumptionOrPersistence(String phone) {
        ISysClientService clients = mock(ISysClientService.class);
        ISysUserService users = mock(ISysUserService.class);
        ISysUserTypeService types = mock(ISysUserTypeService.class);
        ISysUserTypeRelService grants = mock(ISysUserTypeRelService.class);
        PasswordPolicyService policy = mock(PasswordPolicyService.class);
        CaptchaProperties captcha = new CaptchaProperties();
        captcha.setEnable(true);
        SysClientVo client = new SysClientVo();
        client.setStatus("0"); client.setRegisterEnabled(true); client.setUserTypeId(9L);
        SysUserTypeVo type = new SysUserTypeVo(); type.setStatus("0");
        when(clients.queryByClientId("phone-client")).thenReturn(client);
        when(types.queryById(9L)).thenReturn(type);
        SysRegisterService service = new SysRegisterService(users, captcha, clients, types, grants, policy);
        RegisterBody body = new RegisterBody();
        body.setClientId("phone-client"); body.setUsername("phone-user"); body.setPassword("OwnedPass!9");
        body.setPhoneNumber(phone);

        assertThatThrownBy(() -> service.register(body)).isInstanceOf(ServiceException.class)
            .hasMessageContaining("手机号码");
        verifyNoInteractions(users, grants, policy);
    }

    private void assertRegistrationDenied(Boolean enabled) {
        ISysClientService clients = mock(ISysClientService.class);
        ISysUserService users = mock(ISysUserService.class);
        ISysUserTypeService types = mock(ISysUserTypeService.class);
        ISysUserTypeRelService grants = mock(ISysUserTypeRelService.class);
        PasswordPolicyService policy = mock(PasswordPolicyService.class);
        CaptchaProperties captcha = new CaptchaProperties();
        captcha.setEnable(true);
        SysClientVo client = new SysClientVo();
        client.setStatus("0");
        client.setRegisterEnabled(enabled);
        client.setUserTypeId(9L);
        when(clients.queryByClientId("owned-disabled-client")).thenReturn(client);
        SysRegisterService service = new SysRegisterService(users, captcha, clients, types, grants, policy);
        RegisterBody body = new RegisterBody();
        body.setClientId("owned-disabled-client");
        body.setUsername("owned-user");
        body.setPassword("OwnedPass!9");
        body.setCode("1234");
        body.setUuid("unconsumed-challenge");

        // 不配置 Redis/Spring 单例；任何验证码消费或后续写入都会使该测试失败。
        assertThatThrownBy(() -> service.register(body)).isInstanceOf(ServiceException.class)
            .hasMessage("当前应用未开放注册");
        verifyNoInteractions(users, types, grants, policy);
    }
}
