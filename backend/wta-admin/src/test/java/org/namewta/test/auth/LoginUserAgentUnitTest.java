package org.namewta.test.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 无 User-Agent 仍完成登录上下文、在线会话和异步审计的真实消费方法回归。 */
@Tag("dev")
public class LoginUserAgentUnitTest {
    private static final String CHROME_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
        + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
    private static final String TOKEN = "synthetic-unit-token";
    private static final long CHILD_TIMEOUT_SECONDS = 45;

    @TempDir
    Path temporary;

    private static GenericApplicationContext context;
    private static ApplicationContext previousContext;
    private static ConfigurableListableBeanFactory previousFactory;

    static Stream<String> userAgents() {
        return Stream.of("missing", "blank", "chrome");
    }

    @ParameterizedTest
    @MethodSource("userAgents")
    void publicLoginPreservesExistingContextAndFillsBrowserAndOs(String userAgentCase) throws Exception {
        runIsolated("login", userAgentCase);
    }

    @Test
    void publicLoginKeepsPreviouslyKnownBrowserAndOsWithoutHeader() throws Exception {
        runIsolated("existing", "missing");
    }

    @ParameterizedTest
    @MethodSource("userAgents")
    void successListenerStillWritesOnlineStateAndLoginAudit(String userAgentCase) throws Exception {
        runIsolated("listener", userAgentCase);
    }

    @ParameterizedTest
    @MethodSource("userAgents")
    void loginInfoConsumerStillPersistsSuccessAndClientFields(String userAgentCase) throws Exception {
        runIsolated("audit", userAgentCase);
    }

    /** RedisUtils/MapstructUtils 缓存 Spring Bean，故父 Surefire JVM 只管理隔离进程。 */
    private void runIsolated(String consumer, String userAgentCase) throws Exception {
        Path result = temporary.resolve(consumer + '-' + userAgentCase + ".result");
        Path log = temporary.resolve(consumer + '-' + userAgentCase + ".log");
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        assertThat(classpath).as("Surefire test classpath").isNotBlank();
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-Xms32m", "-Xmx384m", "-cp", classpath, LoginUserAgentUnitTest.class.getName(),
            consumer, userAgentCase, result.toString())
            .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            boolean finished = child.waitFor(CHILD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(finished).as("isolated %s/%s completed within %s seconds",
                consumer, userAgentCase, CHILD_TIMEOUT_SECONDS).isTrue();
            String outcome = Files.isRegularFile(result) ? Files.readString(result).trim() : "NO_RESULT";
            assertThat(child.exitValue()).as("isolated %s/%s outcome=%s", consumer, userAgentCase, outcome).isZero();
            assertThat(outcome).as("isolated %s/%s", consumer, userAgentCase).isEqualTo("OK");
        } finally {
            boolean stopped;
            try {
                stopped = stopChildTree(child);
            } finally {
                Files.deleteIfExists(result);
                Files.deleteIfExists(log);
            }
            assertThat(stopped).as("isolated child and descendants stopped").isTrue();
        }
    }

    private static boolean stopChildTree(Process child) throws InterruptedException {
        List<ProcessHandle> descendants = child.toHandle().descendants().toList();
        if (child.isAlive()) {
            child.destroyForcibly();
        }
        descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
        boolean parentStopped = child.waitFor(5, TimeUnit.SECONDS);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (descendants.stream().anyMatch(ProcessHandle::isAlive) && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }
        return parentStopped && descendants.stream().noneMatch(ProcessHandle::isAlive);
    }

    /** Child entrypoint: only fixed test cases and fixed failure categories cross the JVM boundary. */
    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !Stream.of("login", "existing", "listener", "audit").anyMatch(args[0]::equals)
            || !Stream.of("missing", "blank", "chrome").anyMatch(args[1]::equals)
            || ("existing".equals(args[0]) && !"missing".equals(args[1]))) {
            throw new IllegalArgumentException("Unlisted login UA child case");
        }
        Path result = Path.of(args[2]);
        String outcome = "OK";
        try {
            prepareStaticUtilities();
            UserAgentCase sample = sample(args[1]);
            switch (args[0]) {
                case "login" -> checkPublicLogin(sample);
                case "existing" -> checkExistingFields();
                case "listener" -> checkSuccessListener(sample);
                case "audit" -> checkLoginInfo(sample);
                default -> throw new IllegalArgumentException("Unlisted login UA child consumer");
            }
        } catch (RuntimeException | AssertionError failure) {
            outcome = failure instanceof NullPointerException ? "NPE"
                : failure instanceof AssertionError ? "ASSERTION" : "HARNESS";
        } finally {
            try {
                restoreStaticUtilities();
            } catch (RuntimeException cleanupFailure) {
                outcome = "CLEANUP";
            }
            Files.writeString(result, outcome);
        }
        if (!"OK".equals(outcome)) {
            System.exit(1);
        }
    }

    private static UserAgentCase sample(String name) {
        return switch (name) {
            case "missing" -> new UserAgentCase(null, "Unknown", "Unknown");
            case "blank" -> new UserAgentCase("   ", "Unknown", "Unknown");
            case "chrome" -> new UserAgentCase(CHROME_UA, "Chrome", "Linux");
            default -> throw new IllegalArgumentException("Unlisted login UA case");
        };
    }

    private static void prepareStaticUtilities() {
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

    private static void restoreStaticUtilities() {
        if (context != null) {
            context.close();
        }
        SpringUtils spring = new SpringUtils();
        spring.postProcessBeanFactory(previousFactory);
        spring.setApplicationContext(previousContext);
    }

    private static void checkPublicLogin(UserAgentCase sample) {
        withRequest(sample.header(), () -> {
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
                LoginHelper.login(loginUser, new SaLoginParameter());
                stp.verify(() -> StpUtil.login(eq("system:42"), any(SaLoginParameter.class)));
                verify(session).set(LoginHelper.LOGIN_USER_KEY, loginUser);
            }
            assertThat(loginUser.getBrowser()).isEqualTo(sample.browser());
            assertThat(loginUser.getOs()).isEqualTo(sample.os());
            assertThat(loginUser.getIpaddr()).isEqualTo("existing-ip");
            assertThat(loginUser.getLoginLocation()).isEqualTo("existing-location");
            assertThat(loginUser.getDeviceType()).isEqualTo("existing-device");
        });
    }

    private static void checkExistingFields() {
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
                LoginHelper.login(loginUser, new SaLoginParameter());
            }
            assertThat(loginUser.getBrowser()).isEqualTo("existing-browser");
            assertThat(loginUser.getOs()).isEqualTo("existing-os");
        });
    }

    private static void checkSuccessListener(UserAgentCase sample) {
        withRequest(sample.header(), () -> {
            SysLoginService loginService = mock(SysLoginService.class);
            SaLoginParameter parameter = new SaLoginParameter().setTimeout(-1).setDeviceType("existing-device")
                .setExtra(LoginHelper.USER_NAME_KEY, "owned-user")
                .setExtra(LoginHelper.USER_KEY, 42L)
                .setExtra(LoginHelper.CLIENT_KEY, "owned-client")
                .setExtra(LoginHelper.DEPT_NAME_KEY, "existing-dept");
            UserLoginSuccessEvent event = new UserLoginSuccessEvent("system:42", TOKEN, parameter);
            ArgumentCaptor<UserOnlineDTO> online = ArgumentCaptor.forClass(UserOnlineDTO.class);
            try (var redis = mockStatic(RedisUtils.class)) {
                new UserLoginSuccessListener(loginService).handleLoginSuccess(event);
                redis.verify(() -> RedisUtils.setCacheObject(eq(CacheNames.ONLINE_TOKEN_KEY + TOKEN),
                    online.capture()));
            }
            assertThat(online.getValue().getBrowser()).isEqualTo(sample.browser());
            assertThat(online.getValue().getOs()).isEqualTo(sample.os());
            assertThat(online.getValue().getTokenId()).isEqualTo(TOKEN);
            assertThat(online.getValue().getUserName()).isEqualTo("owned-user");
            assertThat(online.getValue().getDeptName()).isEqualTo("existing-dept");
            assertThat(online.getValue().getClientKey()).isEqualTo("owned-client");
            assertThat(online.getValue().getDeviceType()).isEqualTo("existing-device");
            verify(loginService).recordLoginInfo(eq("owned-user"), eq(Constants.LOGIN_SUCCESS), any(String.class));
            verify(loginService).updateLastLoginInfo(42L, "127.0.0.1");
        });
    }

    private static void checkLoginInfo(UserAgentCase sample) {
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
        event.setUserAgent(sample.header());

        new SysLoginInfoServiceImpl(mapper, clients).recordLoginInfo(event);
        ArgumentCaptor<SysLoginInfo> saved = ArgumentCaptor.forClass(SysLoginInfo.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue().getBrowser()).isEqualTo(sample.browser());
        assertThat(saved.getValue().getOs()).isEqualTo(sample.os());
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

    private record UserAgentCase(String header, String browser, String os) {
    }
}
