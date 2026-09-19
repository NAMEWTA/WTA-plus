package org.namewta.test.contracts;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.log.aspect.LogAspect;
import org.namewta.common.satoken.core.service.SaPermissionImpl;
import org.namewta.common.satoken.handler.SaTokenExceptionHandler;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.security.config.SecurityConfig;
import org.namewta.common.security.config.properties.SecurityProperties;
import org.namewta.common.security.handler.AllUrlHandler;
import org.namewta.system.api.model.LoginUser;
import org.namewta.workflow.controller.FlwTaskController;
import org.namewta.workflow.domain.bo.FlowNextNodeBo;
import org.namewta.workflow.service.IFlwTaskService;
import org.namewta.web.controller.AuthController;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** 真Jetty/DispatcherServlet/SecurityConfig验证传输与授权；业务依赖由受控边界替身承接，不访问供应商。 */
@Tag("dev")
class CrudHttpDispatchIntegrationTest {
    record Migration(String controller, String operation, String oldMethod, String oldPath, String newMethod, String newPath) { }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class Mvc {
        @Bean
        WebMvcConfigurer securityAndSelectionProbe() {
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    var properties = new SecurityProperties(); properties.setExcludes(new String[0]);
                    new SecurityConfig(properties).addInterceptors(registry);
                    registry.addInterceptor(new HandlerInterceptor() {
                        @Override
                        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                                 jakarta.servlet.http.HttpServletResponse response, Object handler) {
                            if (handler instanceof HandlerMethod method) response.setHeader("X-Contract-Handler",
                                method.getMethod().getDeclaringClass().getSimpleName() + "." + method.getMethod().getName());
                            return true;
                        }
                    });
                }
            };
        }
    }

    @Test
    void legacyMethodsAreRejectedAndNewMappingsRetainPermissionClientAndQueryBinding() throws Exception {
        List<Migration> migrations;
        try (var input = getClass().getResourceAsStream("/contracts/crud-http-migration.json")) {
            assertThat(input).isNotNull();
            migrations = JsonUtils.parseArray(new String(input.readAllBytes(), StandardCharsets.UTF_8), Migration.class);
        }
        assertThat(migrations).hasSize(91);
        var previousConfig = SaManager.getConfig(); var previousContext = SaManager.getSaTokenContext();
        var previousPermissions = SaManager.getStpInterface(); var previousLogic = StpUtil.getStpLogic();
        var previousDao = SaManager.getSaTokenDao(); var previousRequest = RequestContextHolder.getRequestAttributes();
        var previousMatcher = cn.dev33.satoken.strategy.SaStrategy.instance.routeMatcher;
        Object previousSpring = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        Object previousFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        var ownedDao = new SaTokenDaoDefaultImpl();
        var server = new Server(new QueuedThreadPool(16, 2));
        var connector = new ServerConnector(server); connector.setHost("127.0.0.1"); connector.setPort(0); server.addConnector(connector);
        // Rejected methods may leave request bodies unread; each isolated probe owns its connection.
        connector.getConnectionFactory(org.eclipse.jetty.server.HttpConnectionFactory.class).getHttpConfiguration().setPersistentConnectionsEnabled(false);
        var handler = new ServletContextHandler(); handler.setContextPath("/"); server.setHandler(handler);
        var query = new AtomicReference<FlowNextNodeBo>();
        var observations = new ArrayList<Map<String, Object>>();
        try (var context = new GenericWebApplicationContext(); var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer")
                .setJwtSecretKey("owned-crud-http-fixture-secret-at-least-32-characters"));
            SaManager.setSaTokenDao(ownedDao); SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            new cn.dev33.satoken.spring.SaTokenContextRegister();
            SaManager.setStpInterface(new SaPermissionImpl()); StpUtil.setStpLogic(new StpLogicJwtForSimple());
            context.setServletContext(handler.getServletContext());
            new AnnotatedBeanDefinitionReader(context).register(Mvc.class);
            context.registerBean(SpringUtils.class); context.registerBean(SaTokenExceptionHandler.class);
            context.registerBean(org.namewta.common.web.handler.GlobalExceptionHandler.class);
            for (String className : migrations.stream().map(Migration::controller).distinct().toList()) {
                Class<?> type = Class.forName(className);
                Object target;
                if (type == FlwTaskController.class) {
                    var tasks = mock(IFlwTaskService.class);
                    when(tasks.getNextNodeList(any())).thenAnswer(call -> { query.set(call.getArgument(0)); return List.of(); });
                    target = new FlwTaskController(tasks);
                } else if (type == AuthController.class) {
                    var constructor = type.getConstructors()[0];
                    target = constructor.newInstance(Arrays.stream(constructor.getParameterTypes()).map(parameter -> mock(parameter)).toArray());
                } else {
                    target = mock(type, call -> call.getMethod().getReturnType() == R.class ? R.ok() : RETURNS_DEFAULTS.answer(call));
                }
                var proxy = new AspectJProxyFactory(target); proxy.setProxyTargetClass(true); proxy.addAspect(new LogAspect());
                Object controller = proxy.getProxy();
                register(context, type, controller);
            }
            context.refresh();
            var allUrls = new AllUrlHandler(); allUrls.setUrls(List.of("/**"));
            context.getBeanFactory().registerSingleton("allUrlHandler", allUrls);
            handler.addServlet(new ServletHolder(new DispatcherServlet(context)), "/");
            server.start();
            String allowed = login(26001L, Set.of("*:*:*"), Set.of(SystemConstants.SUPER_ADMIN_ROLE_KEY));
            String denied = login(26002L, Set.of(), Set.of());
            RequestContextHolder.resetRequestAttributes();
            String origin = "http://127.0.0.1:" + connector.getLocalPort();
            int permissionChecks = 0;
            for (Migration migration : migrations) {
                var old = send(client, origin, migration.oldMethod(), sample(migration.oldPath()), allowed, "owned-crud", "{}");
                assertThat(JsonUtils.parseMap(old.body()).getInt("code")).as(migration.toString()).isEqualTo(405);
                assertThat(old.headers().firstValue("X-Contract-Handler")).isEmpty();
                var current = send(client, origin, migration.newMethod(), sample(migration.newPath()), allowed, "owned-crud", "{}");
                String expected = Class.forName(migration.controller()).getSimpleName() + "." + migration.operation();
                assertThat(current.headers().firstValue("X-Contract-Handler")).as(migration.toString()).contains(expected);
                var method = Arrays.stream(Class.forName(migration.controller()).getDeclaredMethods())
                    .filter(value -> value.getName().equals(migration.operation()) && value.isAnnotationPresent(SaCheckPermission.class)).findFirst();
                if (method.isPresent()) {
                    var forbidden = send(client, origin, migration.newMethod(), sample(migration.newPath()), denied, "owned-crud", "{}");
                    assertThat(JsonUtils.parseMap(forbidden.body()).getInt("code")).as(migration.toString()).isEqualTo(403);
                    assertThat(forbidden.headers().firstValue("X-Contract-Handler")).isEmpty();
                    permissionChecks++;
                }
                observations.add(Map.of("old", migration.oldMethod() + " " + migration.oldPath(), "old_status", old.statusCode(), "old_code", 405,
                    "new", migration.newMethod() + " " + migration.newPath(), "selected_handler", expected,
                    "authorized_status", current.statusCode(), "permission_checked", method.isPresent()));
            }
            assertThat(permissionChecks).isGreaterThan(45);
            var anonymous = send(client, origin, "POST", "/system/dept/update", null, "owned-crud", "{}");
            assertThat(JsonUtils.parseMap(anonymous.body()).getInt("code")).isEqualTo(401);
            var wrongClient = send(client, origin, "POST", "/system/dept/update", allowed, "other-client", "{}");
            assertThat(JsonUtils.parseMap(wrongClient.body()).getInt("code")).isEqualTo(401);
            var unbind = send(client, origin, "POST", "/auth/unlock/17", null, "owned-crud", "{}");
            assertThat(JsonUtils.parseMap(unbind.body()).getInt("code")).isEqualTo(401);
            String variables = "{\"amount\":12.5,\"approved\":true,\"nested\":{\"names\":[\"甲\",\"乙\"]}}";
            var next = send(client, origin, "GET", "/workflow/task/getNextNodeList?taskId=17&variables="
                + URLEncoder.encode(variables, StandardCharsets.UTF_8), allowed, "owned-crud", null);
            assertThat(next.statusCode()).isEqualTo(200);
            assertThat(query.get().getTaskId()).isEqualTo(17L);
            assertThat(JsonUtils.toJsonString(query.get().getVariables())).isEqualTo(variables);
            for (String invalid : List.of("null", "[]", "{invalid")) {
                query.set(null);
                var rejected = send(client, origin, "GET", "/workflow/task/getNextNodeList?taskId=17&variables="
                    + URLEncoder.encode(invalid, StandardCharsets.UTF_8), allowed, "owned-crud", null);
                assertThat(JsonUtils.parseMap(rejected.body()).getInt("code")).isNotEqualTo(200);
                assertThat(query.get()).isNull();
            }
            String output = System.getProperty("crud.http.output");
            if (output != null) Files.writeString(Path.of(output), JsonUtils.toJsonString(Map.of(
                "observations", observations, "permission_checks", permissionChecks, "query_binding", "nested JSON types preserved",
                "scope", "real Jetty/Spring MVC/security; business boundaries controlled; not persistence E2E")));
        } finally {
            if (server.isStarted() || server.isStarting()) server.stop();
            server.destroy();
            ownedDao.destroy(); SaManager.setSaTokenDao(previousDao); SaManager.setConfig(previousConfig);
            SaManager.setSaTokenContext(previousContext); SaManager.setStpInterface(previousPermissions); StpUtil.setStpLogic(previousLogic);
            cn.dev33.satoken.strategy.SaStrategy.instance.routeMatcher = previousMatcher;
            RequestContextHolder.setRequestAttributes(previousRequest);
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousSpring);
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousFactory);
        }
    }

    private static <T> void register(GenericWebApplicationContext context, Class<T> type, Object bean) {
        context.registerBean(type.getName(), type, () -> type.cast(bean));
    }

    private static String sample(String path) { return path.replaceAll("\\{[^}]+}", "17"); }

    static HttpResponse<String> send(HttpClient client, String origin, String method, String path,
                                             String token, String clientId, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(origin + path)).timeout(Duration.ofSeconds(10))
            .header("clientid", clientId).header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null || method.equals("GET") ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String login(long id, Set<String> permissions, Set<String> roles) {
        var request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 OwnedCrudFixture/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        var user = new LoginUser(); user.setUserId(id); user.setUsername("owned-crud-" + id); user.setUserType("sys_user");
        user.setClientPk(1L); user.setClientKey("owned-crud"); user.setDeptId(1L); user.setIpaddr("127.0.0.1"); user.setLoginLocation("owned fixture");
        user.setMenuPermission(permissions); user.setRolePermission(roles);
        LoginHelper.login(user, new SaLoginParameter().setTimeout(300).setExtra(LoginHelper.CLIENT_PK_KEY, 1L).setExtra(LoginHelper.CLIENT_KEY, "owned-crud"));
        return StpUtil.getTokenValue();
    }
}
