package org.namewta.test.sso;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
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
import org.namewta.common.redis.config.RedisConfig;
import org.namewta.common.redis.config.properties.RedissonProperties;
import org.namewta.common.web.config.ResourcesConfig;
import org.namewta.common.web.config.properties.CorsProperties;
import org.namewta.sso.adapter.store.RedisSsoSessionStore;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.api.SsoClientView;
import org.namewta.sso.config.SsoAutoConfiguration;
import org.namewta.sso.config.SsoProperties;
import org.namewta.sso.controller.anonymous.SsoOAuthController;
import org.namewta.sso.controller.anonymous.SsoSessionController;
import org.namewta.sso.dao.SsoAuthorizationCodeDaoImpl;
import org.namewta.sso.domain.bo.SsoLoginBo;
import org.namewta.sso.domain.bo.SsoTokenBo;
import org.namewta.sso.mapper.SsoAuthorizationCodeMapper;
import org.namewta.sso.port.SsoBusinessTokenPort;
import org.namewta.sso.port.SsoIdentityPort;
import org.namewta.sso.service.SsoAuthorizationService;
import org.namewta.sso.service.SsoSessionService;
import org.namewta.sso.usecase.impl.SsoOAuthUseCaseImpl;
import org.namewta.sso.usecase.impl.SsoSessionUseCaseImpl;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.password.PasswordPolicyProjection;
import org.namewta.system.password.PasswordCharacterClass;
import org.namewta.system.service.ISysClientService;
import org.namewta.test.support.SqlBaselinePaths;
import org.namewta.web.controller.AuthController;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 构建后的SSO App/Chrome -> HTTPS -> 生产SSO Controller/UseCase/Service -> Redis及真实MySQL DAO。 */
@Tag("dev")
class SsoHttpsSessionIntegrationTest {
    private static final String CLIENT_ID = "sso-security-business-client";
    private static final String ADMIN_CLIENT = "e5cd7e4891bf95d1d19206ce24a7b32e";
    private static final String HOME_CLIENT = "428a8310cd442757ae699df5d894f051";
    private static final String LEGACY_SESSION = "1af730bb2e87131a1af730bb2e87131b";
    @TempDir
    Path directory;

    @Test
    void browserSessionPkceAndCookieContractsHoldWithRealStores() throws Exception {
        String jdbc = System.getProperty("sso.mysql.integration.url");
        int redisPort = Integer.getInteger("sso.redis.integration.port", -1);
        assumeTrue(jdbc != null && redisPort > 0, "owned MySQL/Redis and built SSO App required");
        Path root = Path.of(System.getProperty("namewta.repo.root")).toAbsolutePath();
        Path dist = root.resolve("frontend/apps/sso-web/dist");
        boolean journey = Boolean.getBoolean("sso.journey.integration");
        boolean release = Boolean.getBoolean("sso.release.integration");
        Set<String> clients = journey || release ? Set.of(ADMIN_CLIENT, HOME_CLIENT) : Set.of(CLIENT_ID);
        assertThat(dist.resolve("index.html")).isRegularFile();
        var dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", jdbc, "root", "owned-sso-test-only");
        var context = new AnnotationConfigApplicationContext();
        RedissonClient redis = null;
        var server = new Server();
        boolean ownsTable = false;
        try {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                try (var database = statement.executeQuery("select database()")) {
                    assertThat(database.next()).isTrue();
                    assertThat(database.getString(1)).startsWith("namewta_sso_test_");
                }
                String ddl = Files.readString(SqlBaselinePaths.file("10-cde-base-ddl.sql"));
                int start = ddl.indexOf("create table sso_authorization_code (");
                assertThat(start).isGreaterThanOrEqualTo(0);
                statement.execute(ddl.substring(start, ddl.indexOf(';', start) + 1));
                ownsTable = true;
            }
            var configuration = new MybatisConfiguration(new Environment("sso-test", new JdbcTransactionFactory(), dataSource));
            configuration.setMapUnderscoreToCamelCase(true);
            GlobalConfigUtils.setGlobalConfig(configuration, GlobalConfigUtils.defaults());
            try (var xml = getClass().getResourceAsStream("/mapper/sso/SsoAuthorizationCodeMapper.xml")) {
                assertThat(xml).isNotNull();
                new XMLMapperBuilder(xml, configuration, "mapper/sso/SsoAuthorizationCodeMapper.xml", configuration.getSqlFragments()).parse();
            }
            var sessions = new MybatisSqlSessionFactoryBuilder().build(configuration);
            var mapper = (SsoAuthorizationCodeMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{SsoAuthorizationCodeMapper.class}, (proxy, method, args) -> {
                    try (var session = sessions.openSession(true)) {
                        try {
                            return method.invoke(session.getMapper(SsoAuthorizationCodeMapper.class), args);
                        } catch (InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                    }
                });
            var redisProperties = new RedissonProperties();
            redisProperties.setKeyPrefix("sso-test-" + UUID.randomUUID());
            redisProperties.setThreads(2);
            redisProperties.setNettyThreads(2);
            var redisConfig = new RedisConfig();
            ReflectionTestUtils.setField(redisConfig, "redissonProperties", redisProperties);
            var config = new Config();
            redisConfig.redissonCustomizer().customize(config);
            config.useSingleServer().setAddress("redis://127.0.0.1:" + redisPort)
                .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
            redis = Redisson.create(config);
            RedissonClient finalRedis = redis;
            context.registerBean(RedissonClient.class, () -> finalRedis);
            context.registerBean(SpringUtils.class);
            context.refresh();
            redis.getBucket("sso:session:" + LEGACY_SESSION).set(new SsoAuthenticatedUser(7L, "legacy-user"), Duration.ofMinutes(5));

            var secureProperties = new SsoProperties();
            var devProperties = new SsoProperties();
            devProperties.setCookieSecure(false);
            var devEnvironment = new MockEnvironment();
            devEnvironment.setActiveProfiles("dev");
            new SsoAutoConfiguration(devProperties, devEnvironment);
            var identity = new FixtureIdentity(clients);
            var store = new RedisSsoSessionStore(secureProperties);
            var sessionUseCase = new SsoSessionUseCaseImpl(new SsoSessionService(identity, store));
            var secureSessions = new SsoSessionController(sessionUseCase, secureProperties);
            var devSessions = new SsoSessionController(sessionUseCase, devProperties);
            var clock = new MutableClock();
            AtomicReference<Map<String, List<String>>> redirects = new AtomicReference<>(Map.of());
            AtomicInteger issued = new AtomicInteger();
            SsoBusinessTokenPort tokenPort = new SsoBusinessTokenPort() {
                @Override
                public IssuedToken issue(LoginUser user, SsoClientView client) {
                    assertThat(user.getUserId()).isEqualTo(7L);
                    assertThat(clients).contains(client.getClientId());
                    issued.incrementAndGet();
                    return new IssuedToken("owned-business-test-token-" + client.getClientId(), 300L, client.getClientId());
                }

                @Override
                public void revoke(String token) { }
            };
            var authorization = new SsoAuthorizationService(new SsoAuthorizationCodeDaoImpl(mapper), clientId -> {
                if (!clients.contains(clientId)) return null;
                var client = new SsoClientView();
                client.setId(9L);
                client.setClientId(clientId);
                client.setClientKey("business-test");
                client.setStatus("0");
                client.setSsoEnabled(true);
                client.setRedirectUris(redirects.get().get(clientId));
                return client;
            }, identity, tokenPort, clock, secureProperties.getCodeTtl());
            var oauth = new SsoOAuthController(new SsoOAuthUseCaseImpl(authorization), sessionUseCase, secureProperties);

            command(root, List.of("keytool", "-genkeypair", "-alias", "sso-test", "-keyalg", "RSA", "-keysize", "2048",
                "-storetype", "PKCS12", "-keystore", directory.resolve("server.p12").toString(),
                "-storepass", "owned-sso-test-only", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-validity", "1"), Map.of());
            var https = httpsConnector(server);
            var http = new ServerConnector(server);
            http.setHost("127.0.0.1");
            http.setPort(0);
            server.addConnector(http);
            http.open();
            String httpsOrigin = "https://localhost:" + https.getLocalPort();
            String httpOrigin = "http://localhost:" + http.getLocalPort();
            Map<Integer, AppFiles> apps = new HashMap<>();
            Map<String, String> browserEnvironment = new HashMap<>(Map.of("SSO_TEST_HTTPS_ORIGIN", httpsOrigin, "SSO_TEST_HTTP_ORIGIN", httpOrigin));
            if (release) {
                // 仅隔离夹具的Docker bridge网关接收反代；控制接口仍使用127.0.0.1。
                var proxy = new ServerConnector(server);
                proxy.setHost(System.getProperty("sso.release.backend.host"));
                proxy.setPort(Integer.getInteger("sso.release.backend.port"));
                server.addConnector(proxy);
                String ssoOrigin = System.getProperty("sso.release.sso.origin");
                String adminOrigin = System.getProperty("sso.release.admin.origin");
                String homeOrigin = System.getProperty("sso.release.home.origin");
                secureProperties.setWebOrigin(ssoOrigin);
                secureProperties.setWebBasePath("/sso-app/");
                browserEnvironment.put("SSO_TEST_HTTPS_ORIGIN", ssoOrigin);
                browserEnvironment.put("SSO_TEST_AUTHORIZE_URL", ssoOrigin + "/sso-app/authorize");
                browserEnvironment.put("SSO_TEST_ADMIN_ORIGIN", adminOrigin + "/admin-app");
                browserEnvironment.put("SSO_TEST_HOME_ORIGIN", homeOrigin + "/home-app");
                browserEnvironment.put("SSO_TEST_CONTROL_ORIGIN", httpOrigin);
                redirects.set(Map.of(ADMIN_CLIENT, List.of(adminOrigin + "/admin-app/sso/callback"),
                    HOME_CLIENT, List.of(homeOrigin + "/home-app/sso/callback")));
            } else if (journey) {
                var admin = httpsConnector(server);
                var home = httpsConnector(server);
                String adminOrigin = "https://127.0.0.1:" + admin.getLocalPort();
                String homeOrigin = "https://127.0.0.1:" + home.getLocalPort();
                apps.put(admin.getLocalPort(), new AppFiles(root.resolve("frontend/apps/admin-web/dist"), "/admin/"));
                apps.put(home.getLocalPort(), new AppFiles(root.resolve("frontend/apps/home-web/dist"), "/home/"));
                for (AppFiles app : apps.values()) assertThat(app.dist().resolve("index.html")).isRegularFile();
                browserEnvironment.put("SSO_TEST_ADMIN_ORIGIN", adminOrigin + "/admin");
                browserEnvironment.put("SSO_TEST_HOME_ORIGIN", homeOrigin + "/home");
                redirects.set(Map.of(ADMIN_CLIENT, List.of(adminOrigin + "/admin/sso/callback"), HOME_CLIENT, List.of(homeOrigin + "/home/sso/callback")));
            } else {
                redirects.set(Map.of(CLIENT_ID, List.of(httpsOrigin + "/callback", httpOrigin + "/callback")));
            }
            var cors = new CorsProperties();
            cors.setAllowedOrigins(release ? List.of(browserEnvironment.get("SSO_TEST_HTTPS_ORIGIN"),
                System.getProperty("sso.release.admin.origin"), System.getProperty("sso.release.home.origin"))
                : List.of(httpsOrigin, httpOrigin));
            var clientService = mock(ISysClientService.class);
            for (String clientId : clients) {
                var client = new SysClientVo();
                client.setClientId(clientId);
                client.setStatus("0");
                client.setRegisterEnabled(false);
                client.setSsoEnabled(true);
                client.setSsoAuthMode("both");
                when(clientService.queryByClientId(clientId)).thenReturn(client);
            }
            var passwordPolicy = mock(PasswordPolicyService.class);
            when(passwordPolicy.publicProjection()).thenReturn(new PasswordPolicyProjection(8, 30,
                List.of(PasswordCharacterClass.UPPERCASE, PasswordCharacterClass.LOWERCASE,
                    PasswordCharacterClass.DIGIT, PasswordCharacterClass.SPECIAL), "@$!%*?&"));
            var authContext = new AuthController(null, null, null, null, clientService, null,
                passwordPolicy, secureProperties);
            var handler = new ServletContextHandler();
            handler.setContextPath("/");
            handler.addFilter(new FilterHolder(new ResourcesConfig().corsFilter(cors)), "/*", EnumSet.of(DispatcherType.REQUEST));
            handler.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    // 两个fixture配置分别代表HTTPS部署和显式dev HTTP；生产配置不按请求降级。
                    var controller = release || request.isSecure() ? secureSessions : devSessions;
                    Object result;
                    try {
                        String path = request.getRequestURI();
                        if (path.startsWith("/api/")) path = path.substring(4);
                        if (release && path.startsWith("/__test/") && !"127.0.0.1".equals(request.getLocalAddr())) {
                            response.setStatus(404);
                            return;
                        }
                        result = switch (path) {
                            case "/auth/client/context" -> authContext.clientContext(request.getParameter("clientId"), request.getHeader("clientid"));
                            case "/sso/login" -> controller.login(JsonUtils.parseObject(request.getInputStream().readAllBytes(), SsoLoginBo.class), response);
                            case "/sso/session" -> controller.session(request);
                            case "/sso/logout" -> controller.logout(request, response);
                            case "/sso/oauth2/authorize" -> oauth.authorize(request.getParameter("response_type"), request.getParameter("client_id"),
                                request.getParameter("redirect_uri"), request.getParameter("state"), request.getParameter("code_challenge"),
                                request.getParameter("code_challenge_method"), request);
                            case "/sso/oauth2/token" -> oauth.token(JsonUtils.parseObject(request.getInputStream().readAllBytes(), SsoTokenBo.class));
                            case "/__test/advance-clock" -> {
                                clock.advance(Duration.ofMinutes(6));
                                yield R.ok();
                            }
                            case "/__test/expire-session" -> {
                                expireSession(finalRedis, request);
                                yield R.ok();
                            }
                            default -> null;
                        };
                        if (result == null) {
                            if (release) {
                                response.setStatus(404);
                                return;
                            }
                            AppFiles app = apps.get(request.getLocalPort());
                            serveApp(app == null ? dist : app.dist(), request, response, app == null ? "/" : app.contextPath());
                            return;
                        }
                    } catch (ServiceException failure) {
                        result = R.fail(failure.getMessage());
                    }
                    response.setContentType("application/json");
                    response.getWriter().write(JsonUtils.toJsonString(result));
                }
            }), "/*");
            server.setHandler(handler);
            server.start();
            command(root.resolve("frontend"), release
                ? List.of("corepack", "pnpm", "exec", "playwright", "test", "--config", "playwright.sso.config.ts", "--grep", "T-08", "--max-failures=1")
                : journey
                ? List.of("corepack", "pnpm", "exec", "playwright", "test", "--config", "playwright.sso.config.ts", "--grep", "T-07")
                : List.of("corepack", "pnpm", "exec", "playwright", "test", "--config", "apps/sso-web/playwright.security.config.ts"), browserEnvironment);
            int expectedConsumed = release ? 6 : journey ? 12 : 1;
            assertThat(issued).hasValue(expectedConsumed);
            assertThat(redis.getBucket("sso:session:" + LEGACY_SESSION).isExists()).isTrue();
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
                 var results = statement.executeQuery("select count(*) from sso_authorization_code where consumed=1")) {
                assertThat(results.next()).isTrue();
                assertThat(results.getInt(1)).isEqualTo(expectedConsumed);
            }
            System.out.println(release ? "T-08: 5 real Chrome scenarios passed through production Nginx templates with Redis/MySQL SSO"
                : journey ? "T-07: 12 real Chrome App callback/recovery scenarios passed with Redis/MySQL SSO"
                : "T-06: 2 real Chrome scenarios passed with production SSO methods, Redis sessions, MySQL consume CAS and HTTPS cookies");
        } finally {
            server.stop();
            context.close();
            if (redis != null) redis.shutdown();
            if (ownsTable) {
                try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                    statement.execute("drop table sso_authorization_code");
                }
            }
            dataSource.forceCloseAll();
            ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", null);
            ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", null);
        }
    }

    private void expireSession(RedissonClient redis, HttpServletRequest request) throws IOException {
        for (Cookie cookie : request.getCookies()) {
            if ("Sso-Token".equals(cookie.getName())) {
                var bucket = redis.getBucket("sso:session:v2:" + cookie.getValue());
                assertThat(bucket.expire(Duration.ofMillis(1))).isTrue();
                long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
                while (bucket.isExists() && System.nanoTime() < deadline) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException failure) {
                        Thread.currentThread().interrupt();
                        throw new IOException(failure);
                    }
                }
                assertThat(bucket.isExists()).isFalse();
                return;
            }
        }
        throw new IOException("Missing owned fixture session");
    }

    private record AppFiles(Path dist, String contextPath) { }

    private ServerConnector httpsConnector(Server server) throws IOException {
        var tls = new SslContextFactory.Server();
        tls.setKeyStorePath(directory.resolve("server.p12").toString());
        tls.setKeyStorePassword("owned-sso-test-only");
        var config = new HttpConfiguration();
        config.addCustomizer(new SecureRequestCustomizer());
        var connector = new ServerConnector(server, new SslConnectionFactory(tls, "http/1.1"), new HttpConnectionFactory(config));
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);
        connector.open();
        return connector;
    }

    private void serveApp(Path dist, HttpServletRequest request, HttpServletResponse response, String contextPath) throws IOException {
        if ("/callback".equals(request.getRequestURI())) {
            response.setContentType("text/html");
            response.getWriter().write("<!doctype html><title>Owned callback</title><p>callback complete</p>");
            return;
        }
        if (!request.getRequestURI().startsWith(contextPath)) {
            response.setStatus(404);
            return;
        }
        Path file = dist.resolve(request.getRequestURI().substring(contextPath.length())).normalize();
        if (!file.startsWith(dist)) {
            response.setStatus(404);
            return;
        }
        if (!Files.isRegularFile(file)) file = dist.resolve("index.html");
        String mime = java.net.URLConnection.guessContentTypeFromName(file.toString());
        response.setContentType(file.toString().endsWith(".js") ? "application/javascript" : mime == null ? "application/octet-stream" : mime);
        Files.copy(file, response.getOutputStream());
    }

    private void command(Path cwd, List<String> command, Map<String, String> environment) throws Exception {
        Path output = Files.createTempFile(directory, "command-", ".log");
        var builder = new ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().putAll(environment);
        var process = builder.start();
        if (!process.waitFor(180, TimeUnit.SECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            throw new IOException("Owned test command timed out: " + command.getFirst());
        }
        String result = Files.readString(output);
        assertThat(process.exitValue()).as(command + "\n" + result).isZero();
        if (!environment.isEmpty()) System.out.println(result);
    }

    private static class FixtureIdentity implements SsoIdentityPort {
        private final Set<String> clients;

        FixtureIdentity(Set<String> clients) { this.clients = clients; }
        @Override
        public SsoAuthenticatedUser verifyPassword(String username, String password) {
            if (!"sso-test-user".equals(username) || !"owned-sso-test-only".equals(password)) throw new ServiceException("invalid fixture credentials");
            return new SsoAuthenticatedUser(7L, username);
        }

        @Override
        public void assertClientAccess(Long userId, String clientId) {
            if (userId != 7L || !clients.contains(clientId)) throw new ServiceException("invalid fixture client");
        }

        @Override
        public LoginUser buildLoginUser(Long userId, String clientId) {
            assertClientAccess(userId, clientId);
            var user = new LoginUser();
            user.setUserId(userId);
            user.setUsername("sso-test-user");
            return user;
        }
    }

    private static class MutableClock extends Clock {
        private final AtomicReference<Instant> instant = new AtomicReference<>(Instant.now());

        void advance(Duration duration) { instant.updateAndGet(value -> value.plus(duration)); }

        @Override
        public ZoneId getZone() { return ZoneId.systemDefault(); }

        @Override
        public Clock withZone(ZoneId zone) { return this; }

        @Override
        public Instant instant() { return instant.get(); }
    }
}
