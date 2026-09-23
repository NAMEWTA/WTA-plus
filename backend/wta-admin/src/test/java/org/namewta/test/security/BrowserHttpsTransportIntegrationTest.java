package org.namewta.test.security;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.web.config.properties.CaptchaProperties;
import org.namewta.common.web.filter.RepeatableFilter;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.common.web.logging.SysLogFilter;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.sso.config.SsoProperties;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.password.PasswordPolicy;
import org.namewta.system.password.PasswordPolicyConfigParser;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysClientService;
import org.namewta.system.service.ISysConfigService;
import org.namewta.system.service.ISysUserService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.namewta.system.service.ISysUserTypeService;
import org.namewta.web.controller.AuthController;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.IAuthStrategy;
import org.namewta.web.service.SysLoginService;
import org.namewta.web.service.SysRegisterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Production App -> HTTPS -> Spring MVC/AuthController/SysRegisterService/password policy and audit filters.
 * User persistence, password grant/token minting, menus, captcha and export payloads are explicit fixtures;
 * this does not claim production authorization, database transactions or full System integration.
 */
@Tag("dev")
class BrowserHttpsTransportIntegrationTest {
    private static final String ADMIN = "e5cd7e4891bf95d1d19206ce24a7b32e";
    private static final String HOME = "428a8310cd442757ae699df5d894f051";
    private static final String PASSWORD = "OwnedPass!9";
    @TempDir Path directory;

    @Test
    void productionAppsUseOrdinaryJsonOverHttpsWithRealMvcAndRedactedAudit() throws Exception {
        assumeTrue(Boolean.getBoolean("browser.transport.integration"), "owned production App builds required");
        Path root = Path.of(System.getProperty("namewta.repo.root")).toAbsolutePath();
        var fixture = new Fixture();
        var events = new CopyOnWriteArrayList<Map<String, Object>>();
        var violations = new CopyOnWriteArrayList<String>();
        var context = new AnnotationConfigWebApplicationContext();
        var server = new Server();
        try {
            command(root, List.of("keytool", "-genkeypair", "-alias", "transport", "-keyalg", "RSA", "-keysize", "2048",
                "-storetype", "PKCS12", "-keystore", directory.resolve("server.p12").toString(), "-storepass",
                "owned-transport-only", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-validity", "1"), Map.of());
            var admin = https(server);
            var home = https(server);
            var apps = Map.of(admin.getLocalPort(), root.resolve("frontend/apps/admin-web/dist"),
                home.getLocalPort(), root.resolve("frontend/apps/home-web/dist"));
            for (Path dist : apps.values()) assertThat(dist.resolve("index.html")).isRegularFile();
            context.register(MvcConfiguration.class);
            context.addBeanFactoryPostProcessor(factory -> {
                factory.registerSingleton("authController", fixture.authController());
                factory.registerSingleton("passwordAuthStrategy", (IAuthStrategy) fixture::login);
                factory.registerSingleton("peripheralFixtureController", new PeripheralController(fixture));
            });
            var handler = new ServletContextHandler();
            handler.setContextPath("/");
            handler.addFilter(new FilterHolder((request, response, chain) -> {
                var http = (HttpServletRequest) request;
                if (!request.isSecure()) violations.add("insecure request");
                if (http.getHeader("encrypt-key") != null || http.getHeader("isEncrypt") != null) {
                    violations.add("retired request header");
                }
                chain.doFilter(request, response);
                if (((HttpServletResponse) response).getHeader("encrypt-key") != null) violations.add("retired response header");
            }), "/prod-api/*", EnumSet.of(DispatcherType.REQUEST));
            handler.addFilter(new FilterHolder(new RepeatableFilter(2 * 1024 * 1024)), "/prod-api/*", EnumSet.of(DispatcherType.REQUEST));
            handler.addFilter(new FilterHolder(new SysLogFilter(1024 * 1024, 2 * 1024 * 1024, events::add)), "/prod-api/*", EnumSet.of(DispatcherType.REQUEST));
            var mvc = new ServletHolder(new DispatcherServlet(context));
            mvc.setInitOrder(1);
            handler.addServlet(mvc, "/prod-api/*");
            handler.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    Path dist = apps.get(request.getLocalPort());
                    Path file = dist.resolve(request.getRequestURI().substring(1)).normalize();
                    if (!file.startsWith(dist)) { response.setStatus(404); return; }
                    if (!Files.isRegularFile(file)) file = dist.resolve("index.html");
                    String mime = java.net.URLConnection.guessContentTypeFromName(file.toString());
                    response.setContentType(file.toString().endsWith(".js") ? "application/javascript"
                        : mime == null ? "application/octet-stream" : mime);
                    Files.copy(file, response.getOutputStream());
                }
            }), "/");
            server.setHandler(handler);
            server.start();
            command(root.resolve("frontend"), List.of("corepack", "pnpm", "exec", "playwright", "test", "--config",
                "playwright.transport.config.ts", "--max-failures=1"), Map.of(
                "TRANSPORT_ADMIN_ORIGIN", "https://127.0.0.1:" + admin.getLocalPort(),
                "TRANSPORT_HOME_ORIGIN", "https://127.0.0.1:" + home.getLocalPort()));
            assertThat(violations).isEmpty();
            assertThat(fixture.users).hasSize(4).containsKeys("registered-admin", "registered-home");
            assertThat(fixture.grantedUsers).hasSize(2);
            for (String username : List.of("registered-admin", "registered-home")) {
                assertThat(BCrypt.checkpw(PASSWORD, fixture.users.get(username).getPassword())).isTrue();
            }
            assertThat(events).isNotEmpty();
            String audit = JsonUtils.toJsonString(events);
            assertThat(audit.contains(PASSWORD) || audit.contains("WrongPass!8")).isFalse();
            for (String token : fixture.tokens.keySet()) assertThat(audit.contains(token)).isFalse();
            System.out.println("T-11: production App HTTPS, MVC JSON binding, real registration/policy and audit passed; persistence/password grant/token/export are fixtures");
        } finally {
            server.stop();
            context.close();
            ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", null);
            ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", null);
            ReflectionTestUtils.setField(JsonUtils.class, "JSON_MAPPER", null);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class MvcConfiguration {
        @Bean static SpringUtils springUtils() { return new SpringUtils(); }
        @Bean JsonMapper jsonMapper() { return JsonMapper.builder().build(); }
        @Bean GlobalExceptionHandler globalExceptionHandler() { return new GlobalExceptionHandler(); }
        @Bean StaticMessageSource messageSource() {
            var source = new StaticMessageSource();
            source.setUseCodeAsDefaultMessage(true);
            return source;
        }
    }

    private static class Fixture {
        private final Map<String, SysUserBo> users = new ConcurrentHashMap<>();
        private final Map<String, String> tokens = new ConcurrentHashMap<>();
        private final List<Long> grantedUsers = new CopyOnWriteArrayList<>();
        private final AtomicLong ids = new AtomicLong(40);
        private final ISysClientService clients = mock(ISysClientService.class);
        private final ISysUserService userService = mock(ISysUserService.class);
        private final ISysUserTypeService types = mock(ISysUserTypeService.class);
        private final ISysUserTypeRelService grants = mock(ISysUserTypeRelService.class);
        private final CaptchaProperties captcha = new CaptchaProperties();
        private final PasswordPolicyService policy;

        Fixture() {
            captcha.setEnable(false);
            for (String id : List.of(ADMIN, HOME)) {
                var client = new SysClientVo();
                client.setClientId(id); client.setStatus("0"); client.setGrantType("password");
                client.setRegisterEnabled(true); client.setUserTypeId(9L); client.setSsoAuthMode("local");
                when(clients.queryByClientId(id)).thenReturn(client);
            }
            var type = new SysUserTypeVo(); type.setStatus("0");
            when(types.queryById(9L)).thenReturn(type);
            when(userService.checkUserNameUnique(any())).thenAnswer(call -> !users.containsKey(call.<SysUserBo>getArgument(0).getUserName()));
            when(userService.checkPhoneUnique(any())).thenAnswer(call -> users.values().stream()
                .noneMatch(user -> call.<SysUserBo>getArgument(0).getPhoneNumber().equals(user.getPhoneNumber())));
            when(userService.registerUser(any())).thenAnswer(call -> {
                SysUserBo user = call.getArgument(0); user.setUserId(ids.incrementAndGet());
                return users.putIfAbsent(user.getUserName(), user) == null;
            });
            org.mockito.Mockito.doAnswer(call -> { grantedUsers.add(call.getArgument(0)); return true; })
                .when(grants).grantUserType(any(), any(), any());
            var config = mock(ISysConfigService.class);
            when(config.selectConfigByKey(PasswordPolicy.CONFIG_KEY)).thenReturn("""
                {"version":1,"minimumLength":8,"maximumLength":30,"requireUppercase":true,"requireLowercase":true,
                "requireDigit":true,"requireSpecial":true,"allowedSpecialCharacters":"@$!%*?&",
                "generator":{"length":12,"uppercaseCharacters":"ABC","lowercaseCharacters":"abc","digitCharacters":"123","specialCharacters":"!"},
                "defaultPassword":{"mode":"RANDOM"}}
                """);
            policy = new PasswordPolicyService(config, new PasswordPolicyConfigParser(JsonMapper.builder().build()));
            for (String name : List.of("owned-admin", "owned-home")) {
                var user = new SysUserBo(); user.setUserId(ids.incrementAndGet()); user.setUserName(name);
                user.setPassword(BCrypt.hashpw(PASSWORD)); users.put(name, user);
            }
        }

        AuthController authController() {
            var registration = new SysRegisterService(userService, captcha, clients, types, grants, policy);
            return new AuthController(null, mock(SysLoginService.class), registration, null, clients,
                mock(NotificationApplicationService.class), policy, new SsoProperties());
        }

        LoginVo login(String body, SysClientVo client) {
            var input = JsonUtils.getJsonMapper().readTree(body);
            var user = users.get(input.get("username").asString());
            if (user == null || !BCrypt.checkpw(input.get("password").asString(), user.getPassword())) {
                throw new ServiceException("账号或密码错误");
            }
            String token = UUID.randomUUID().toString(); tokens.put(token, client.getClientId());
            var result = new LoginVo(); result.setAccessToken(token); result.setClientId(client.getClientId()); result.setExpireIn(300L);
            return result;
        }

        void requireSession(HttpServletRequest request) {
            String authorization = request.getHeader("Authorization");
            String token = authorization == null ? "" : authorization.replaceFirst("^Bearer ", "");
            if (!request.getHeader("clientid").equals(tokens.get(token))) throw new ServiceException("fixture session rejected", 401);
        }
    }

    @RestController
    static class PeripheralController {
        private final Fixture fixture;
        PeripheralController(Fixture fixture) { this.fixture = fixture; }
        @GetMapping("/auth/code") R<?> code() { return R.ok(Map.of("captchaEnabled", false)); }
        @GetMapping("/system/user/getInfo") R<?> info(HttpServletRequest request) {
            fixture.requireSession(request);
            return R.ok(Map.of("user", Map.of("userId", 7L, "userName", "owned-user", "nickName", "Owned user", "avatarUrl", ""),
                "roles", List.of("operator"), "permissions", List.of("system:user:list", "system:user:export")));
        }
        @GetMapping("/system/menu/getRouters") R<?> menus(HttpServletRequest request) {
            fixture.requireSession(request);
            boolean admin = ADMIN.equals(request.getHeader("clientid"));
            return R.ok(List.of(Map.of("path", admin ? "/transport-users" : "/profile", "name", admin ? "TransportUsers" : "ProfileCenter",
                "component", admin ? "system/user/index" : "profile/center/index", "meta", Map.of("title", admin ? "用户列表" : "档案中心"))));
        }
        @GetMapping("/notify/inbox") R<?> inbox() {
            return R.ok(Map.of("rows", List.of(), "total", 0, "unreadTotal", 0));
        }
        @GetMapping({"/system/dept/treeselect", "/system/dict/data/type/{type}"}) R<?> empty() { return R.ok(List.of()); }
        @GetMapping("/system/user/list") Map<String, Object> users() { return Map.of("code", 200, "rows", List.of(), "total", 0); }
        @PostMapping("/system/user/export") void export(HttpServletRequest request, HttpServletResponse response) throws IOException {
            fixture.requireSession(request);
            response.setContentType("application/octet-stream");
            response.getOutputStream().write(new byte[]{0, 1, 2, 127, (byte) 128, (byte) 255, 10});
        }
        @PostMapping("/system/user/importTemplate") R<?> failedDownload() { return R.fail("下载测试失败"); }
    }

    private ServerConnector https(Server server) throws IOException {
        var tls = new SslContextFactory.Server(); tls.setKeyStorePath(directory.resolve("server.p12").toString());
        tls.setKeyStorePassword("owned-transport-only");
        var config = new HttpConfiguration(); config.addCustomizer(new SecureRequestCustomizer());
        var connector = new ServerConnector(server, new SslConnectionFactory(tls, "http/1.1"), new HttpConnectionFactory(config));
        connector.setHost("127.0.0.1"); connector.setPort(0); server.addConnector(connector); connector.open();
        return connector;
    }

    private void command(Path cwd, List<String> command, Map<String, String> environment) throws Exception {
        Path log = Files.createTempFile(directory, "command-", ".log");
        var builder = new ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().putAll(environment);
        var process = builder.start();
        try {
            assertThat(process.waitFor(180, TimeUnit.SECONDS)).as("owned command timeout").isTrue();
            String output = Files.readString(log);
            if (!environment.isEmpty()) System.out.println(output);
            assertThat(process.exitValue()).as(command + "\n" + output).isZero();
        } finally {
            if (process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly(); process.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }
}
