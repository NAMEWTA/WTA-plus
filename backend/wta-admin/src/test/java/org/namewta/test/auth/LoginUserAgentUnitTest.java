package org.namewta.test.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.constant.Constants;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.log.event.LoginInfoEvent;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.domain.UserOnlineDTO;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.domain.SysLoginInfo;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.mapper.SysLoginInfoMapper;
import org.namewta.system.service.ISysClientService;
import org.namewta.system.service.impl.SysLoginInfoServiceImpl;
import org.namewta.web.event.UserLoginSuccessEvent;
import org.namewta.web.listener.UserLoginSuccessListener;
import org.namewta.web.service.SysLoginService;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 无 User-Agent 仍完成登录上下文、在线会话和异步审计的真实消费方法回归。 */
@Tag("dev")
class LoginUserAgentUnitTest {
    private static final String CHROME_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
        + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
    private static final String TOKEN = "synthetic-unit-token";

    private static GenericApplicationContext context;
    private static ApplicationContext previousContext;
    private static ConfigurableListableBeanFactory previousFactory;

    @BeforeAll
    static void prepareStaticUtilities() {
        previousContext = SpringUtils.getApplicationContext();
        try {
            previousFactory = SpringUtils.getConfigurableBeanFactory();
        } catch (RuntimeException ignored) {
            previousFactory = null;
        }
        context = new GenericApplicationContext();
        context.registerBean(SpringUtils.class);
        context.registerBean(Converter.class, () -> new Converter());
        context.registerBean(RedissonClient.class, () -> mock(RedissonClient.class));
        context.refresh();
    }

    @AfterAll
    static void restoreStaticUtilities() {
        if (context != null) {
            context.close();
        }
        SpringUtils spring = new SpringUtils();
        spring.postProcessBeanFactory(previousFactory);
        spring.setApplicationContext(previousContext);
    }

    static Stream<Arguments> userAgents() {
        return Stream.of(
            Arguments.of(null, "Unknown", "Unknown"),
            Arguments.of("   ", "Unknown", "Unknown"),
            Arguments.of(CHROME_UA, "Chrome", "Linux")
        );
    }

    @ParameterizedTest
    @MethodSource("userAgents")
    void publicLoginPreservesExistingContextAndFillsBrowserAndOs(String userAgent, String browser, String os) {
        withRequest(userAgent, () -> {
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(42L);
            loginUser.setUserType("system");
            loginUser.setUsername("owned-user");
            loginUser.setIpaddr("existing-ip");
            loginUser.setLoginLocation("existing-location");
            loginUser.setDeviceType("existing-device");
            SaSession session = mock(SaSession.class);
            try (var stp = mockStatic(StpUtil.class)) {
                stp.when(StpUtil::getTokenSession).thenReturn(session);
                assertDoesNotThrow(() -> LoginHelper.login(loginUser, new SaLoginParameter()));
                stp.verify(() -> StpUtil.login(eq("system:42"), any(SaLoginParameter.class)));
                verify(session).set(LoginHelper.LOGIN_USER_KEY, loginUser);
            }
            assertThat(loginUser.getBrowser()).isEqualTo(browser);
            assertThat(loginUser.getOs()).isEqualTo(os);
            assertThat(loginUser.getIpaddr()).isEqualTo("existing-ip");
            assertThat(loginUser.getLoginLocation()).isEqualTo("existing-location");
            assertThat(loginUser.getDeviceType()).isEqualTo("existing-device");
        });
    }

    @Test
    void publicLoginKeepsPreviouslyKnownBrowserAndOsWithoutHeader() {
        withRequest(null, () -> {
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(42L);
            loginUser.setUserType("system");
            loginUser.setBrowser("existing-browser");
            loginUser.setOs("existing-os");
            loginUser.setIpaddr("existing-ip");
            loginUser.setLoginLocation("existing-location");
            try (var stp = mockStatic(StpUtil.class)) {
                stp.when(StpUtil::getTokenSession).thenReturn(mock(SaSession.class));
                assertDoesNotThrow(() -> LoginHelper.login(loginUser, new SaLoginParameter()));
            }
            assertThat(loginUser.getBrowser()).isEqualTo("existing-browser");
            assertThat(loginUser.getOs()).isEqualTo("existing-os");
        });
    }

    @ParameterizedTest
    @MethodSource("userAgents")
    void successListenerStillWritesOnlineStateAndLoginAudit(String userAgent, String browser, String os) {
        withRequest(userAgent, () -> {
            SysLoginService loginService = mock(SysLoginService.class);
            SaLoginParameter parameter = new SaLoginParameter().setTimeout(-1).setDeviceType("existing-device")
                .setExtra(LoginHelper.USER_NAME_KEY, "owned-user")
                .setExtra(LoginHelper.USER_KEY, 42L)
                .setExtra(LoginHelper.CLIENT_KEY, "owned-client")
                .setExtra(LoginHelper.DEPT_NAME_KEY, "existing-dept");
            UserLoginSuccessEvent event = new UserLoginSuccessEvent("system:42", TOKEN, parameter);
            ArgumentCaptor<UserOnlineDTO> online = ArgumentCaptor.forClass(UserOnlineDTO.class);
            try (var redis = mockStatic(RedisUtils.class)) {
                assertDoesNotThrow(() -> new UserLoginSuccessListener(loginService).handleLoginSuccess(event));
                redis.verify(() -> RedisUtils.setCacheObject(eq(CacheNames.ONLINE_TOKEN_KEY + TOKEN),
                    online.capture()));
            }
            assertThat(online.getValue().getBrowser()).isEqualTo(browser);
            assertThat(online.getValue().getOs()).isEqualTo(os);
            assertThat(online.getValue().getTokenId()).isEqualTo(TOKEN);
            assertThat(online.getValue().getUserName()).isEqualTo("owned-user");
            assertThat(online.getValue().getDeptName()).isEqualTo("existing-dept");
            assertThat(online.getValue().getClientKey()).isEqualTo("owned-client");
            assertThat(online.getValue().getDeviceType()).isEqualTo("existing-device");
            verify(loginService).recordLoginInfo(eq("owned-user"), eq(Constants.LOGIN_SUCCESS), any(String.class));
            verify(loginService).updateLastLoginInfo(42L, "127.0.0.1");
        });
    }

    @ParameterizedTest
    @MethodSource("userAgents")
    void loginInfoConsumerStillPersistsSuccessAndClientFields(String userAgent, String browser, String os) {
        SysLoginInfoMapper mapper = mock(SysLoginInfoMapper.class);
        ISysClientService clients = mock(ISysClientService.class);
        SysClientVo client = new SysClientVo();
        client.setClientKey("existing-client-key");
        client.setDeviceType("existing-device");
        when(clients.queryByClientId("owned-client")).thenReturn(client);
        LoginInfoEvent event = new LoginInfoEvent();
        event.setUsername("owned-user");
        event.setStatus(Constants.LOGIN_SUCCESS);
        event.setMessage("existing-success-message");
        event.setIp("127.0.0.1");
        event.setClientId("owned-client");
        event.setUserAgent(userAgent);

        assertDoesNotThrow(() -> new SysLoginInfoServiceImpl(mapper, clients).recordLoginInfo(event));
        ArgumentCaptor<SysLoginInfo> saved = ArgumentCaptor.forClass(SysLoginInfo.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue().getBrowser()).isEqualTo(browser);
        assertThat(saved.getValue().getOs()).isEqualTo(os);
        assertThat(saved.getValue().getUserName()).isEqualTo("owned-user");
        assertThat(saved.getValue().getStatus()).isEqualTo(Constants.SUCCESS);
        assertThat(saved.getValue().getClientKey()).isEqualTo("existing-client-key");
        assertThat(saved.getValue().getDeviceType()).isEqualTo("existing-device");
        assertThat(saved.getValue().getMsg()).isEqualTo("existing-success-message");
    }

    private static void withRequest(String userAgent, Runnable assertion) {
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        if (userAgent != null) {
            request.addHeader("User-Agent", userAgent);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            assertion.run();
        } finally {
            if (previous == null) {
                RequestContextHolder.resetRequestAttributes();
            } else {
                RequestContextHolder.setRequestAttributes(previous);
            }
        }
    }
}
