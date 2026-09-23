package org.namewta.test.notify.caller;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.service.ISysClientService;
import org.namewta.web.controller.AuthController;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.IAuthStrategy;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 实际登录控制器发出的通知命令须保持原用户目标与幂等域，优先级使用已支持值。 */
@Tag("dev")
class AuthNotifyCallerUnitTest {
    @Test
    void successfulLoginEnqueuesSupportedModeForTheLoggedInUser() {
        ISysClientService clients = mock(ISysClientService.class);
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        SysClientVo client = new SysClientVo();
        client.setClientId("owned-client");
        client.setGrantType("password");
        client.setStatus(SystemConstants.NORMAL);
        when(clients.queryByClientId("owned-client")).thenReturn(client);
        AuthController controller = new AuthController(null, null, null, null, clients, notifications, null, null);
        LoginVo loginResult = new LoginVo();
        // Controller 仍执行真实 LoginBody 校验；仅给静态 SpringUtil 一个本地 Validator/JSON mapper。
        try (var validatorFactory = Validation.buildDefaultValidatorFactory();
             MockedStatic<SpringUtil> spring = mockStatic(SpringUtil.class);
             MockedStatic<IAuthStrategy> auth = mockStatic(IAuthStrategy.class);
             MockedStatic<LoginHelper> session = mockStatic(LoginHelper.class)) {
            spring.when(() -> SpringUtil.getBean(Validator.class)).thenReturn(validatorFactory.getValidator());
            spring.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            auth.when(() -> IAuthStrategy.login(anyString(), eq(client), eq("password"))).thenReturn(loginResult);
            session.when(LoginHelper::getUserId).thenReturn(7L);

            var response = controller.login("{\"clientId\":\"owned-client\",\"grantType\":\"password\"}");
            assertThat(response.getData()).isSameAs(loginResult);
        }

        ArgumentCaptor<NotificationCommand> sent = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notifications).submit(sent.capture());
        NotificationCommand command = sent.getValue();
        assertThat(command.appId()).isEqualTo("admin-web");
        assertThat(command.sceneCode()).isEqualTo("auth-login");
        assertThat(command.recipientType()).isEqualTo("USER");
        assertThat(command.recipientIds()).isEqualTo(List.of("7"));
        assertThat(command.priority()).isZero();
        assertThat(command.idempotencyKey()).startsWith("login-welcome:7:");
    }
}
