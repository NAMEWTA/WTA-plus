package org.namewta.test.notify;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.satoken.handler.SaTokenExceptionHandler;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.notify.controller.admin.NotifyInboxController;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.service.runtime.NotifyInboxService;
import org.namewta.notify.usecase.NotifyInboxUseCase;
import org.namewta.system.api.model.LoginUser;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 独占六 SQL 数据库、实际登录会话与 HTTP 收件箱分页合同。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.inbox.paging.integration", matches = "true")
class NotifyInboxPagingIntegrationTest {
    private static final long USER_A = 9_410_001L;
    private static final long USER_B = 9_410_002L;
    private static final long FIRST_MESSAGE = 9_610_001L;
    private static final long LAST_A_MESSAGE = FIRST_MESSAGE + 500;
    private static final long B_ONLY_MESSAGE = FIRST_MESSAGE + 501;
    private static final long FIRST_RELATION = 9_620_001L;
    private static final LocalDateTime SHARED_TIME = LocalDateTime.of(2026, 9, 23, 10, 0);

    private NotifyAtomicResultIntegrationTest fixture;
    private JdbcTemplate db;
    private NotifyNotificationDao dao;

    @BeforeEach
    void open() throws Exception {
        assertThat(System.getenv("T41_MYSQL_PASSWORD")).as("owned MySQL password in child environment").isNotBlank();
        assertThat(System.getenv("T36_MYSQL_PASSWORD")).as("shared fixture password in child environment")
            .isEqualTo(System.getenv("T41_MYSQL_PASSWORD"));
        assertThat(System.getProperty("notify.mysql.integration.url"))
            .matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/namewta_notify_test_[a-zA-Z0-9_]+.*");
        assertThat(System.getProperty("notify.redis.integration.port")).matches("[0-9]+");
        fixture = new NotifyAtomicResultIntegrationTest();
        fixture.open();
        db = field("db", JdbcTemplate.class);
        dao = field("dao", NotifyNotificationDao.class);
    }

    @AfterEach
    void close() {
        try {
            if (db != null) {
                db.update("delete from notify_message_recipient where message_id between ? and ?",
                    FIRST_MESSAGE, B_ONLY_MESSAGE);
                db.update("delete from notify_message where message_id between ? and ?",
                    FIRST_MESSAGE, B_ONLY_MESSAGE);
            }
        } finally {
            if (fixture != null) fixture.close();
        }
    }

    @Test
    void authenticatedOwnerCanPageToMessage501WithoutLosingTotal() throws Exception {
        seedMessages();
        var controller = new NotifyInboxController(new NotifyInboxUseCase(new NotifyInboxService(dao)));
        try (OwnedSaSession sessions = new OwnedSaSession(); OwnedHttp http = new OwnedHttp(controller)) {
            String tokenA = sessions.login(USER_A);
            String tokenB = sessions.login(USER_B);
            assertThat(StpUtil.getLoginIdByToken(tokenA)).isEqualTo("sys_user:" + USER_A);
            assertThat(StpUtil.getLoginIdByToken(tokenB)).isEqualTo("sys_user:" + USER_B);

            var bPage = http.get(tokenB, 1, 20);
            assertThat(bPage.statusCode()).isEqualTo(200);
            assertThat(JsonUtils.parseMap(bPage.body()).get("code")).isEqualTo(200);
            assertThat(bPage.body()).contains(String.valueOf(B_ONLY_MESSAGE), String.valueOf(FIRST_MESSAGE + 250));
            assertThat(bPage.body()).doesNotContain(String.valueOf(FIRST_MESSAGE + 249));

            var aFirst = http.get(tokenA, 1, 20);
            assertThat(aFirst.statusCode()).isEqualTo(200);
            assertThat(JsonUtils.parseMap(aFirst.body()).get("code")).isEqualTo(200);
            assertThat(aFirst.body()).contains(String.valueOf(LAST_A_MESSAGE));
            assertThat(aFirst.body()).doesNotContain(String.valueOf(B_ONLY_MESSAGE));

            var aLast = http.get(tokenA, 26, 20);
            assertThat(aLast.statusCode()).isEqualTo(200);
            Map<String, Object> response = JsonUtils.parseMap(aLast.body());
            assertThat(response.get("code")).isEqualTo(200);
            assertThat(response.get("data") instanceof Map)
                .as("page 26 has rows, total and unreadTotal, not the old 500-item array").isTrue();
            Map<?, ?> page = (Map<?, ?>) response.get("data");
            assertThat(((Number) page.get("total")).longValue()).isEqualTo(501);
            assertThat(((Number) page.get("unreadTotal")).longValue()).isEqualTo(500);
            assertThat(page.get("rows")).isInstanceOf(List.class);
            List<?> rows = (List<?>) page.get("rows");
            assertThat(rows).hasSize(1);
            assertThat(((Number) ((Map<?, ?>) rows.getFirst()).get("messageId")).longValue())
                .isEqualTo(FIRST_MESSAGE);
        }
    }

    private void seedMessages() {
        db.batchUpdate("insert into notify_message(message_id,category,notice_type,channels_json,type,source,title,message,content,create_time) "
            + "values(?,'system','','[\"IN_APP\"]','MESSAGE','BACKEND',?,?,?,?)",
            new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override public int getBatchSize() { return 502; }
                @Override public void setValues(java.sql.PreparedStatement statement, int index) throws java.sql.SQLException {
                    long messageId = FIRST_MESSAGE + index;
                    statement.setLong(1, messageId);
                    statement.setString(2, "Owned inbox " + messageId);
                    statement.setString(3, "Summary " + messageId);
                    statement.setString(4, "Full body " + messageId);
                    statement.setTimestamp(5, Timestamp.valueOf(SHARED_TIME));
                }
            });
        db.batchUpdate("insert into notify_message_recipient(message_recipient_id,message_id,user_id,read_time,create_time) "
            + "values(?,?,?,?,?)", new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override public int getBatchSize() { return 503; }
                @Override public void setValues(java.sql.PreparedStatement statement, int index) throws java.sql.SQLException {
                    boolean b = index >= 501;
                    long messageId = b ? (index == 501 ? FIRST_MESSAGE + 250 : B_ONLY_MESSAGE) : FIRST_MESSAGE + index;
                    statement.setLong(1, FIRST_RELATION + index);
                    statement.setLong(2, messageId);
                    statement.setLong(3, b ? USER_B : USER_A);
                    if (index == 250) statement.setTimestamp(4, Timestamp.valueOf(SHARED_TIME));
                    else statement.setNull(4, java.sql.Types.TIMESTAMP);
                    statement.setTimestamp(5, Timestamp.valueOf(SHARED_TIME));
                }
            });
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where user_id=? "
            + "and message_id between ? and ?", Integer.class, USER_A, FIRST_MESSAGE, B_ONLY_MESSAGE)).isEqualTo(501);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where user_id=? "
            + "and message_id between ? and ?", Integer.class, USER_B, FIRST_MESSAGE, B_ONLY_MESSAGE)).isEqualTo(2);
    }

    @SuppressWarnings("unchecked")
    private <T> T field(String name, Class<T> type) {
        return (T) type.cast(ReflectionTestUtils.getField(fixture, name));
    }

    @Configuration
    @EnableWebMvc
    static class OwnedWebConfiguration implements WebMvcConfigurer {
        @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(new SaInterceptor()); }
    }

    /** 真实 Servlet/MVC/SaInterceptor；只注册本票 Controller，不接触其他业务入口。 */
    private static final class OwnedHttp implements AutoCloseable {
        private final Server server = new Server(new InetSocketAddress("127.0.0.1", 0));
        private final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        private final HttpClient client = HttpClient.newHttpClient();
        private int port;

        private OwnedHttp(NotifyInboxController controller) throws Exception {
            try {
                var handler = new ServletContextHandler();
                handler.setContextPath("/");
                context.setServletContext(handler.getServletContext());
                context.register(OwnedWebConfiguration.class);
                context.addBeanFactoryPostProcessor(factory -> {
                    factory.registerSingleton("notifyInboxController", controller);
                    factory.registerSingleton("globalExceptionHandler", new GlobalExceptionHandler());
                    factory.registerSingleton("saTokenExceptionHandler", new SaTokenExceptionHandler());
                });
                context.refresh();
                handler.addServlet(new ServletHolder(new DispatcherServlet(context)), "/");
                server.setHandler(handler);
                server.start();
                port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            } catch (Exception failure) {
                close();
                throw failure;
            }
        }

        private HttpResponse<String> get(String token, int pageNum, int pageSize) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                    + "/notify/inbox?pageNum=" + pageNum + "&pageSize=" + pageSize))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        @Override public void close() throws Exception {
            try { server.stop(); }
            finally { context.close(); client.close(); }
        }
    }

    /** 私有 Sa-Token DAO 承载 LoginHelper 签发的 A/B 会话，退出时完整还原全局入口。 */
    private static final class OwnedSaSession implements AutoCloseable {
        private final RequestAttributes previousRequest = RequestContextHolder.getRequestAttributes();
        private final SaTokenConfig previousConfig = SaManager.getConfig();
        private final cn.dev33.satoken.context.SaTokenContext previousContext = SaManager.getSaTokenContext();
        private final cn.dev33.satoken.dao.SaTokenDao previousDao = SaManager.getSaTokenDao();
        private final StpLogic previousLogic = StpUtil.getStpLogic();

        private OwnedSaSession() {
            byte[] secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer")
                .setJwtSecretKey(Base64.getUrlEncoder().withoutPadding().encodeToString(secret)));
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
            StpUtil.setStpLogic(new StpLogicJwtForSimple());
        }

        private String login(long userId) {
            var request = new MockHttpServletRequest();
            request.addHeader("User-Agent", "Mozilla/5.0 OwnedInbox/1.0");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
            var user = new LoginUser();
            user.setUserId(userId);
            user.setUsername("owned-inbox-" + userId);
            user.setUserType("sys_user");
            user.setIpaddr("127.0.0.1");
            user.setLoginLocation("owned fixture");
            LoginHelper.login(user, new SaLoginParameter().setTimeout(300));
            assertThat(LoginHelper.getUserId()).as("JWT extra carries the actual inbox owner").isEqualTo(userId);
            return StpUtil.getTokenValue();
        }

        @Override public void close() {
            if (previousRequest == null) RequestContextHolder.resetRequestAttributes();
            else RequestContextHolder.setRequestAttributes(previousRequest);
            SaManager.setConfig(previousConfig);
            SaManager.setSaTokenContext(previousContext);
            SaManager.setSaTokenDao(previousDao);
            StpUtil.setStpLogic(previousLogic);
        }
    }
}
