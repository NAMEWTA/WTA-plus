package org.namewta.test.logging;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
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
import org.namewta.common.core.domain.R;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.aspect.LogAspect;
import org.namewta.common.log.event.OperLogEvent;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.common.web.logging.SysLogFilter;
import org.namewta.sso.config.SsoProperties;
import org.namewta.sso.controller.anonymous.SsoOAuthController;
import org.namewta.sso.domain.bo.SsoTokenBo;
import org.namewta.sso.port.SsoBusinessTokenPort.IssuedToken;
import org.namewta.sso.usecase.SsoOAuthUseCase;
import org.namewta.sso.usecase.SsoSessionUseCase;
import org.namewta.system.mapper.SysOperLogMapper;
import org.namewta.system.service.impl.SysOperLogServiceImpl;
import org.namewta.test.support.SqlBaselinePaths;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

/** 真实 Servlet HTTP -> LogAspect/Event -> 生产 Service/Mapper -> 隔离 MySQL。签发 UseCase 仅提供确定性凭据。 */
@Tag("dev")
class LogRedactionHttpMySqlIntegrationTest {

    @Test
    void credentialsNeverReachHttpEventsOperationEventsOrMySql() throws Exception {
        String url = System.getProperty("log.mysql.integration.url");
        assumeTrue(url != null && !url.isBlank(), "需要一次性日志验收 MySQL URL");
        PooledDataSource dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", url,
            System.getProperty("log.mysql.integration.username", "root"),
            System.getProperty("log.mysql.integration.password", ""));
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
