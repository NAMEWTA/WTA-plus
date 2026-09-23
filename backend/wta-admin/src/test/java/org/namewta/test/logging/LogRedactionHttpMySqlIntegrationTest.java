package org.namewta.test.logging;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import io.github.linpeilie.Converter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.aspect.LogAspect;
import org.namewta.common.log.event.OperLogEvent;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.common.web.logging.SysLogFilter;
import org.namewta.sso.config.SsoProperties;
import org.namewta.sso.controller.anonymous.SsoOAuthController;
import org.namewta.sso.domain.bo.SsoTokenBo;
import org.namewta.sso.port.SsoBusinessTokenPort.IssuedToken;
import org.namewta.sso.usecase.SsoOAuthUseCase;
import org.namewta.sso.usecase.SsoSessionUseCase;
import org.namewta.system.mapper.SysOperLogMapper;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.controller.monitor.SysUserOnlineController;
import org.namewta.system.domain.SysUserOnline;
import org.namewta.system.service.impl.SysOperLogServiceImpl;
import org.namewta.test.support.SqlBaselinePaths;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 真实 Servlet HTTP -> LogAspect/Event -> 生产 Service/Mapper -> 隔离 MySQL。 */
@Tag("dev")
class LogRedactionHttpMySqlIntegrationTest {

    @Test
    void credentialsNeverReachHttpEventsOperationEventsOrMySql() throws Exception {
        String url = System.getProperty("log.mysql.integration.url");
        assumeTrue(url != null && !url.isBlank(), "需要一次性日志验收 MySQL URL");
        assumeTrue(System.getenv("T02_MYSQL_PASSWORD") != null, "需要隔离测试的子进程 MySQL 密码环境");
        PooledDataSource dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", url,
            System.getProperty("log.mysql.integration.username", "root"),
            System.getenv("T02_MYSQL_PASSWORD"));
        Object oldContext = ReflectionTestUtils.getField(SpringUtil.class, "applicationContext");
        Object oldFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        var oldSaContext = SaManager.getSaTokenContext();
        Server server = new Server(new InetSocketAddress("127.0.0.1", 0));
        boolean ownsTable = false;
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                try (var database = statement.executeQuery("select database()")) {
                    assertThat(database.next()).isTrue();
                    assertThat(database.getString(1)).startsWith("namewta_log_test_");
                }
                String ddl = Files.readString(SqlBaselinePaths.file("10-cde-base-ddl.sql"));
                int start = ddl.indexOf("create table sys_oper_log (");
                assertThat(start).isGreaterThanOrEqualTo(0);
                statement.execute(ddl.substring(start, ddl.indexOf(';', start) + 1));
                ownsTable = true;
            }
            MybatisConfiguration configuration = new MybatisConfiguration(new Environment("log-test",
                new JdbcTransactionFactory(), dataSource));
            configuration.setMapUnderscoreToCamelCase(true);
            GlobalConfigUtils.setGlobalConfig(configuration, GlobalConfigUtils.defaults());
            configuration.addMapper(SysOperLogMapper.class);
            var sessions = new MybatisSqlSessionFactoryBuilder().build(configuration);
            List<OperLogEvent> operations = Collections.synchronizedList(new ArrayList<>());
            List<Map<String, Object>> httpEvents = Collections.synchronizedList(new ArrayList<>());
            AtomicReference<Throwable> persistenceFailure = new AtomicReference<>();
            context.registerBean(SpringUtils.class);
            context.registerBean(Converter.class, () -> new Converter());
            context.addApplicationListener(event -> {
                if (event instanceof PayloadApplicationEvent<?> payload && payload.getPayload() instanceof OperLogEvent operation) {
                    operations.add(operation);
                    try (var session = sessions.openSession(true)) {
                        new SysOperLogServiceImpl(session.getMapper(SysOperLogMapper.class)).recordOper(operation);
                    } catch (RuntimeException failure) {
                        persistenceFailure.set(failure);
                        throw failure;
                    }
                }
            });
            context.refresh();
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            String canary = "credential-canary-" + UUID.randomUUID();
            SsoOAuthUseCase useCase = mock(SsoOAuthUseCase.class);
            when(useCase.exchange(any())).thenReturn(new IssuedToken(canary, 300L, "test-client"));
            SsoOAuthController tokenController = proxy(new SsoOAuthController(useCase, mock(SsoSessionUseCase.class), new SsoProperties()));
            AuditController audit = proxy(new AuditController());
            ServletContextHandler handler = new ServletContextHandler();
            handler.setContextPath("/");
            handler.addFilter(new FilterHolder(new SysLogFilter(8192, 2 * 1024 * 1024, httpEvents::add)), "/*", EnumSet.of(DispatcherType.REQUEST));
            handler.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
                    response.setContentType("application/json");
                    try {
                        String raw = new String(request.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        Object result;
                        if (request.getRequestURI().equals("/sso/oauth2/token")) {
                            result = tokenController.token(JsonUtils.parseObject(raw, SsoTokenBo.class));
                        } else if (request.getRequestURI().equals("/failure")) {
                            result = audit.fail(JsonUtils.parseMap(raw));
                        } else {
                            result = audit.echo(JsonUtils.parseMap(raw));
                        }
                        response.getWriter().write(JsonUtils.toJsonString(result));
                    } catch (RuntimeException failure) {
                        response.getWriter().write(JsonUtils.toJsonString(new GlobalExceptionHandler().handleRuntimeException(failure, request)));
                    } finally {
                        RequestContextHolder.resetRequestAttributes();
                    }
                }
            }), "/*");
            server.setHandler(handler);
            server.start();
            int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            try (HttpClient client = HttpClient.newHttpClient()) {
                String tokenRequest = "{\"code\":\"" + canary + "\",\"code_verifier\":\"" + canary + "\",\"client_id\":\"test-client\"}";
                assertThat(post(client, port, "/sso/oauth2/token", tokenRequest)).contains(canary);
                String body = "{\"password\":\"" + canary + "\",\"name\":\"visible\",\"code\":\"business-code\"}";
                assertThat(post(client, port, "/echo", body)).contains(canary, "visible", "business-code");
                assertThat(post(client, port, "/failure", body)).doesNotContain(canary);
            }
            assertThat(persistenceFailure.get()).isNull();
            assertThat(httpEvents).hasSize(6);
            assertThat(JsonUtils.toJsonString(httpEvents)).doesNotContain(canary);
            assertThat(operations).hasSize(3);
            assertThat(JsonUtils.toJsonString(operations)).doesNotContain(canary).contains("visible", "business-code");
            assertThat(operations.getFirst().getJsonResult()).isNull();
            assertThat(operations.getLast().getStatus()).isEqualTo(1);
            try (var session = sessions.openSession(true)) {
                var rows = session.getMapper(SysOperLogMapper.class).selectList(null);
                assertThat(rows).hasSize(3);
                assertThat(JsonUtils.toJsonString(rows)).doesNotContain(canary).contains("visible", "business-code");
                assertThat(rows).allSatisfy(row -> {
                    assertThat(row.getTitle()).isNotBlank();
                    assertThat(row.getCostTime()).isNotNegative();
                });
            }
        } finally {
            server.stop();
            server.destroy();
            SaManager.setSaTokenContext(oldSaContext);
            ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", oldContext);
            ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", oldFactory);
            try {
                if (ownsTable) {
                    try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                        statement.execute("drop table sys_oper_log");
                    }
                }
            } finally {
                dataSource.forceCloseAll();
            }
        }
    }

    @Test
    void onlineDeviceTokenFromRealIssuerNeverReachesHttpOrErrorAudit() throws Exception {
        String url = System.getProperty("log.mysql.integration.url");
        assumeTrue(url != null && !url.isBlank(), "需要一次性日志验收 MySQL URL");
        String password = System.getenv("T02_MYSQL_PASSWORD");
        assumeTrue(password != null, "需要隔离测试的子进程 MySQL 密码环境");
        PooledDataSource dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", url,
            System.getProperty("log.mysql.integration.username", "root"), password);
        Object oldContext = ReflectionTestUtils.getField(SpringUtil.class, "applicationContext");
        Object oldFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        var oldSaContext = SaManager.getSaTokenContext();
        var oldSaConfig = SaManager.getConfig();
        var oldSaDao = SaManager.getSaTokenDao();
        StpLogic oldLogic = StpUtil.stpLogic;
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> errorLogs = new ListAppender<>();
        errorLogs.start();
        root.addAppender(errorLogs);
        Server server = new Server(new InetSocketAddress("127.0.0.1", 0));
        boolean ownsTable = false;
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                try (var database = statement.executeQuery("select database()")) {
                    assertThat(database.next()).isTrue();
                    assertThat(database.getString(1)).startsWith("namewta_log_test_");
                }
                String ddl = Files.readString(SqlBaselinePaths.file("10-cde-base-ddl.sql"));
                int start = ddl.indexOf("create table sys_oper_log (");
                assertThat(start).isGreaterThanOrEqualTo(0);
                statement.execute(ddl.substring(start, ddl.indexOf(';', start) + 1));
                ownsTable = true;
            }
            MybatisConfiguration configuration = new MybatisConfiguration(new Environment("online-log-test",
                new JdbcTransactionFactory(), dataSource));
            configuration.setMapUnderscoreToCamelCase(true);
            GlobalConfigUtils.setGlobalConfig(configuration, GlobalConfigUtils.defaults());
            configuration.addMapper(SysOperLogMapper.class);
            var sessions = new MybatisSqlSessionFactoryBuilder().build(configuration);
            List<OperLogEvent> operations = Collections.synchronizedList(new ArrayList<>());
            List<Map<String, Object>> httpEvents = Collections.synchronizedList(new ArrayList<>());
            AtomicReference<Throwable> persistenceFailure = new AtomicReference<>();
            context.registerBean(SpringUtils.class);
            context.registerBean(Converter.class, () -> new Converter());
            context.addApplicationListener(event -> {
                if (event instanceof PayloadApplicationEvent<?> payload && payload.getPayload() instanceof OperLogEvent operation) {
                    operations.add(operation);
                    try (var session = sessions.openSession(true)) {
                        new SysOperLogServiceImpl(session.getMapper(SysOperLogMapper.class)).recordOper(operation);
                    } catch (RuntimeException failure) {
                        persistenceFailure.set(failure);
                        throw failure;
                    }
                }
            });
            context.refresh();
            SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer"));
            SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            StpUtil.setStpLogic(new StpLogic("login"));
            String selfToken = issueOwnedToken(99101L);
            String adminToken = issueOwnedToken(99102L);
            assertThat(selfToken).isNotBlank().isNotEqualTo(adminToken);
            assertThat(StpUtil.getLoginIdByToken(selfToken)).isEqualTo("sys_user:99101");
            assertThat(StpUtil.getLoginIdByToken(adminToken)).isEqualTo("sys_user:99102");
            SysUserOnline self = onlineRow(selfToken, "owned-self");
            SysUserOnline admin = onlineRow(adminToken, "owned-admin");
            SysUserOnlineController controller = proxy(new SysUserOnlineController());
            ServletContextHandler handler = new ServletContextHandler();
            handler.setContextPath("/");
            handler.addFilter(new FilterHolder(new SysLogFilter(8192, 2 * 1024 * 1024, httpEvents::add)),
                "/*", EnumSet.of(DispatcherType.REQUEST));
            handler.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
                    response.setContentType("application/json");
                    try {
                        response.getWriter().write(JsonUtils.toJsonString(R.ok(PageResult.build(List.of(self, admin)))));
                    } finally {
                        RequestContextHolder.resetRequestAttributes();
                    }
                }

                @Override
                protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
                    response.setContentType("application/json");
                    try {
                        String uri = request.getRequestURI();
                        Object result;
                        if (uri.startsWith("/monitor/online/myself/")) {
                            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/monitor/online/myself/{tokenId}");
                            result = controller.remove(uri.substring("/monitor/online/myself/".length()));
                        } else {
                            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/monitor/online/{tokenId}");
                            result = controller.forceLogout(uri.substring("/monitor/online/".length()));
                        }
                        // 真实操作后模拟本请求的下游异常，覆盖 GlobalExceptionHandler 的 URI 日志入口。
                        new GlobalExceptionHandler().handleRuntimeException(new IllegalStateException("owned downstream failure"), request);
                        response.getWriter().write(JsonUtils.toJsonString(result));
                    } finally {
                        RequestContextHolder.resetRequestAttributes();
                    }
                }
            }), "/*");
            server.setHandler(handler);
            server.start();
            int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            try (HttpClient client = HttpClient.newHttpClient()) {
                String list = get(client, port, "/monitor/online/list");
                assertThat(list).contains(selfToken, adminToken, "owned-self", "owned-admin");
                assertThat(get(client, port, "/monitor/online")).contains(selfToken, adminToken);
                assertThat(postWithToken(client, port, "/monitor/online/" + adminToken, selfToken)).contains("code");
                assertThat(StpUtil.getLoginIdByToken(adminToken)).isNull();
                assertThat(StpUtil.getLoginIdByToken(selfToken)).isEqualTo("sys_user:99101");
                assertThat(postWithToken(client, port, "/monitor/online/myself/" + selfToken, selfToken)).contains("code");
                assertThat(StpUtil.getLoginIdByToken(selfToken)).isNull();
            }
            assertThat(persistenceFailure.get()).isNull();
            assertThat(httpEvents).hasSize(8);
            String httpAudit = JsonUtils.toJsonString(httpEvents);
            assertThat(httpAudit.contains(selfToken) || httpAudit.contains(adminToken)).isFalse();
            assertThat(httpAudit).contains("/monitor/online/list", "owned-self", "owned-admin");
            assertThat(operations).hasSize(2);
            assertThat(operations.getFirst().getOperName()).isEqualTo("owned-online-99101");
            String operationAudit = JsonUtils.toJsonString(operations);
            assertThat(operationAudit.contains(selfToken) || operationAudit.contains(adminToken)).isFalse();
            assertThat(operationAudit).contains("/monitor/online/myself/{tokenId}", "/monitor/online/{tokenId}");
            try (var session = sessions.openSession(true)) {
                var rows = session.getMapper(SysOperLogMapper.class).selectList(null);
                assertThat(rows).hasSize(2).allSatisfy(row -> {
                    assertThat(row.getTitle()).isNotBlank();
                    assertThat(row.getCostTime()).isNotNegative();
                });
                String storedAudit = JsonUtils.toJsonString(rows);
                assertThat(storedAudit.contains(selfToken) || storedAudit.contains(adminToken)).isFalse();
            }
            var handlerErrors = errorLogs.list.stream().filter(event ->
                GlobalExceptionHandler.class.getName().equals(event.getLoggerName())).toList();
            assertThat(handlerErrors).hasSize(2);
            assertThat(handlerErrors.stream().anyMatch(event ->
                event.getFormattedMessage().contains(selfToken) || event.getFormattedMessage().contains(adminToken))).isFalse();
        } finally {
            root.detachAppender(errorLogs);
            errorLogs.stop();
            server.stop();
            server.destroy();
            StpUtil.setStpLogic(oldLogic);
            SaManager.setSaTokenDao(oldSaDao);
            SaManager.setConfig(oldSaConfig);
            SaManager.setSaTokenContext(oldSaContext);
            ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", oldContext);
            ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", oldFactory);
            try {
                if (ownsTable) {
                    try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                        statement.execute("drop table sys_oper_log");
                    }
                }
            } finally {
                dataSource.forceCloseAll();
            }
        }
    }

    private static String issueOwnedToken(long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 OwnedOnlineAudit/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        try {
            LoginUser user = new LoginUser();
            user.setUserId(userId);
            user.setUserType("sys_user");
            user.setUsername("owned-online-" + userId);
            user.setIpaddr("127.0.0.1");
            user.setLoginLocation("owned fixture");
            // Sa-Token 按公开登录参数签发真实有效会话，同时使用可供隔离驱动识别的合成前缀。
            LoginHelper.login(user, new SaLoginParameter().setTimeout(300)
                .setToken("credential-canary-online-" + UUID.randomUUID()));
            return StpUtil.getTokenValue();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private static SysUserOnline onlineRow(String token, String name) {
        SysUserOnline row = new SysUserOnline();
        row.setTokenId(token);
        row.setUserName(name);
        return row;
    }

    private static <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new LogAspect());
        return factory.getProxy();
    }

    private static String post(HttpClient client, int port, String path, String body) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private static String postWithToken(HttpClient client, int port, String path, String token) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private static String get(HttpClient client, int port, String path) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    public static class AuditController {
        @Log(title = "普通字段审计")
        public R<Map<String, Object>> echo(Map<String, Object> body) {
            return R.ok(body);
        }

        @Log(title = "失败审计")
        public R<Void> fail(Map<String, Object> body) {
            throw new IllegalStateException(String.valueOf(body.get("password")));
        }
    }
}
