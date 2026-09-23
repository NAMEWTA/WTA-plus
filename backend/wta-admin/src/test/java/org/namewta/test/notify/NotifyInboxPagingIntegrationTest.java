package org.namewta.test.notify;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.StpInterface;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final long ORPHAN_MESSAGE = B_ONLY_MESSAGE + 1;
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
                    FIRST_MESSAGE, ORPHAN_MESSAGE);
                db.update("delete from notify_message where message_id between ? and ?",
                    FIRST_MESSAGE, ORPHAN_MESSAGE);
            }
        } finally {
            if (fixture != null) fixture.close();
        }
    }

    @Test
    void authenticatedOwnerCanPageToMessage501WithoutLosingTotal() throws Exception {
        seedMessages();
        assertThat(dao.inboxTotal(USER_A)).isEqualTo(501);
        assertThat(dao.inboxUnreadTotal(USER_A)).isEqualTo(500);
        assertThat(dao.inboxRows(USER_A, 26, 20, 501)).extracting(row -> row.getMessageId())
            .containsExactly(FIRST_MESSAGE);
        assertThat(dao.inboxDetail(USER_A, FIRST_MESSAGE).getContent()).isEqualTo("Full body " + FIRST_MESSAGE);
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

    @Test
    void ownerDetailAndReadAllUseAllRelationsWithoutChangingOtherUser() throws Exception {
        seedMessages();
        var controller = new NotifyInboxController(new NotifyInboxUseCase(new NotifyInboxService(dao)));
        try (OwnedSaSession sessions = new OwnedSaSession(); OwnedHttp http = new OwnedHttp(controller)) {
            String tokenA = sessions.login(USER_A);
            String tokenB = sessions.login(USER_B);
            assertThat(JsonUtils.parseMap(http.get(null, "/notify/inbox").body()).get("code")).isEqualTo(401);
            assertThat(JsonUtils.parseMap(http.get(null, "/notify/inbox/" + FIRST_MESSAGE).body()).get("code"))
                .isEqualTo(401);

            var owned = http.get(tokenA, "/notify/inbox/" + FIRST_MESSAGE);
            assertThat(JsonUtils.parseMap(owned.body()).get("code")).isEqualTo(200);
            assertThat(owned.body()).contains("Full body " + FIRST_MESSAGE);
            assertThat(http.get(tokenA, 26, 20).body()).doesNotContain("Full body " + FIRST_MESSAGE);
            assertThat(JsonUtils.parseMap(http.get(tokenB, "/notify/inbox/" + (FIRST_MESSAGE + 250)).body())
                .get("code")).isEqualTo(200);
            Map<String, Object> other = JsonUtils.parseMap(http.get(tokenA, "/notify/inbox/" + B_ONLY_MESSAGE).body());
            Map<String, Object> missing = JsonUtils.parseMap(http.get(tokenA, "/notify/inbox/" + ORPHAN_MESSAGE).body());
            assertThat(other.get("code")).isEqualTo(missing.get("code"));
            assertThat(other.get("msg")).isEqualTo(missing.get("msg"));
            assertThat(other.get("code")).isNotEqualTo(200);
            assertThat(other.toString()).doesNotContain("Full body " + B_ONLY_MESSAGE);

            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/" + B_ONLY_MESSAGE + "/read").body())
                .get("code")).isEqualTo(200);
            assertThat(unread(USER_B)).isEqualTo(2);
            sessions.denyWrites(true);
            assertThat(JsonUtils.parseMap(http.post(tokenB, "/notify/inbox/" + B_ONLY_MESSAGE + "/read").body())
                .get("code")).isEqualTo(403);
            assertThat(unread(USER_B)).isEqualTo(2);
            sessions.denyWrites(false);

            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/" + FIRST_MESSAGE + "/read").body())
                .get("code")).isEqualTo(200);
            assertThat(((Number) page(http.get(tokenA, 26, 20)).get("unreadTotal")).longValue()).isEqualTo(499);
            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/read-all").body()).get("code"))
                .isEqualTo(200);
            assertThat(unread(USER_A)).isZero();
            assertThat(unread(USER_B)).isEqualTo(2);
            assertThat(((Number) page(http.get(tokenA, 1, 20)).get("unreadTotal")).longValue()).isZero();
            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/read-all").body()).get("code"))
                .isEqualTo(200);
            assertThat(unread(USER_A)).isZero();
            assertThat(unread(USER_B)).isEqualTo(2);
        }
    }

    @Test
    void readAllAndSingleMarksPreserveFirstInteractionTimes() throws Exception {
        seedMessages();
        long seenOnly = FIRST_MESSAGE + 1;
        long readOnly = FIRST_MESSAGE + 2;
        long complete = FIRST_MESSAGE + 3;
        long singleRead = FIRST_MESSAGE + 4;
        long singleSeen = FIRST_MESSAGE + 5;
        LocalDateTime oldSeen = SHARED_TIME.minusDays(2);
        LocalDateTime oldRead = SHARED_TIME.minusDays(1);
        db.update("update notify_message_recipient set seen_time=? where user_id=? and message_id=?",
            Timestamp.valueOf(oldSeen), USER_A, seenOnly);
        db.update("update notify_message_recipient set read_time=? where user_id=? and message_id=?",
            Timestamp.valueOf(oldRead), USER_A, readOnly);
        db.update("update notify_message_recipient set seen_time=?,read_time=? where user_id=? and message_id=?",
            Timestamp.valueOf(oldSeen), Timestamp.valueOf(oldRead), USER_A, complete);
        db.update("update notify_message_recipient set seen_time=? where user_id=? and message_id=?",
            Timestamp.valueOf(oldSeen), USER_A, singleRead);

        var controller = new NotifyInboxController(new NotifyInboxUseCase(new NotifyInboxService(dao)));
        try (OwnedSaSession sessions = new OwnedSaSession(); OwnedHttp http = new OwnedHttp(controller)) {
            String tokenA = sessions.login(USER_A);
            var beforeB = times(USER_B, FIRST_MESSAGE + 250);
            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/" + singleRead + "/read").body())
                .get("code")).isEqualTo(200);
            var firstRead = times(USER_A, singleRead);
            assertThat(firstRead.seen()).isEqualTo(oldSeen);
            assertThat(firstRead.read()).isNotNull();
            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/" + singleRead + "/read").body())
                .get("code")).isEqualTo(200);
            assertThat(times(USER_A, singleRead)).isEqualTo(firstRead);

            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/" + singleSeen + "/seen").body())
                .get("code")).isEqualTo(200);
            var firstSeen = times(USER_A, singleSeen);
            assertThat(firstSeen.seen()).isNotNull();
            assertThat(firstSeen.read()).isNull();
            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/" + singleSeen + "/seen").body())
                .get("code")).isEqualTo(200);
            assertThat(times(USER_A, singleSeen)).isEqualTo(firstSeen);

            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/read-all").body()).get("code"))
                .isEqualTo(200);
            assertThat(times(USER_A, seenOnly).seen()).isEqualTo(oldSeen);
            assertThat(times(USER_A, seenOnly).read()).isNotNull();
            assertThat(times(USER_A, readOnly).seen()).isNotNull();
            assertThat(times(USER_A, readOnly).read()).isEqualTo(oldRead);
            assertThat(times(USER_A, complete)).isEqualTo(new InteractionTimes(oldSeen, oldRead));
            assertThat(times(USER_A, singleRead)).isEqualTo(firstRead);
            assertThat(times(USER_A, singleSeen).seen()).isEqualTo(firstSeen.seen());
            assertThat(times(USER_A, singleSeen).read()).isNotNull();
            assertThat(times(USER_B, FIRST_MESSAGE + 250)).isEqualTo(beforeB);

            var afterFirstReadAll = times(USER_A, seenOnly);
            var afterFirstReadAllReadOnly = times(USER_A, readOnly);
            var afterFirstReadAllSingleSeen = times(USER_A, singleSeen);
            assertThat(JsonUtils.parseMap(http.post(tokenA, "/notify/inbox/read-all").body()).get("code"))
                .isEqualTo(200);
            assertThat(times(USER_A, seenOnly)).isEqualTo(afterFirstReadAll);
            assertThat(times(USER_A, readOnly)).isEqualTo(afterFirstReadAllReadOnly);
            assertThat(times(USER_A, singleSeen)).isEqualTo(afterFirstReadAllSingleSeen);
            assertThat(times(USER_A, complete)).isEqualTo(new InteractionTimes(oldSeen, oldRead));
            assertThat(times(USER_B, FIRST_MESSAGE + 250)).isEqualTo(beforeB);
        }
    }

    @Test
    void orphanAndExtremePageCannotDistortCountsOrFixedOrder() throws Exception {
        seedMessages();
        db.update("insert into notify_message_recipient(message_recipient_id,message_id,user_id,create_time) "
            + "values(?,?,?,?)", FIRST_RELATION + 503, ORPHAN_MESSAGE, USER_A, Timestamp.valueOf(SHARED_TIME));
        var controller = new NotifyInboxController(new NotifyInboxUseCase(new NotifyInboxService(dao)));
        try (OwnedSaSession sessions = new OwnedSaSession(); OwnedHttp http = new OwnedHttp(controller)) {
            String tokenA = sessions.login(USER_A);
            Map<?, ?> first = page(http.get(tokenA,
                "/notify/inbox?pageNum=1&pageSize=20&orderByColumn=messageId&isAsc=asc"));
            assertThat(((Number) first.get("total")).longValue()).isEqualTo(501);
            assertThat(((Number) first.get("unreadTotal")).longValue()).isEqualTo(500);
            assertThat(first.get("rows")).isInstanceOf(List.class);
            assertThat(((Number) ((Map<?, ?>) ((List<?>) first.get("rows")).getFirst()).get("messageId")).longValue())
                .isEqualTo(LAST_A_MESSAGE);
            Map<?, ?> far = page(http.get(tokenA, "/notify/inbox?pageNum=2147483647&pageSize=100"));
            assertThat(far.get("rows")).isEqualTo(List.of());
            assertThat(((Number) far.get("total")).longValue()).isEqualTo(501);
            for (String query : List.of("pageNum=0&pageSize=20", "pageNum=-1&pageSize=20",
                "pageNum=1&pageSize=0", "pageNum=1&pageSize=101", "pageNum=abc&pageSize=20")) {
                HttpResponse<String> invalid = http.get(tokenA, "/notify/inbox?" + query);
                assertThat(invalid.statusCode() != 200 || !Integer.valueOf(200).equals(
                    JsonUtils.parseMap(invalid.body()).get("code"))).as(query).isTrue();
            }
        }
    }

    private long unread(long userId) {
        return db.queryForObject("select count(*) from notify_message_recipient where user_id=? "
            + "and read_time is null and message_id between ? and ?", Long.class,
            userId, FIRST_MESSAGE, B_ONLY_MESSAGE);
    }

    private InteractionTimes times(long userId, long messageId) {
        return db.queryForObject("select seen_time,read_time from notify_message_recipient where user_id=? and message_id=?",
            (rs, rowNum) -> new InteractionTimes(
                rs.getTimestamp(1) == null ? null : rs.getTimestamp(1).toLocalDateTime(),
                rs.getTimestamp(2) == null ? null : rs.getTimestamp(2).toLocalDateTime()),
            userId, messageId);
    }

    private record InteractionTimes(LocalDateTime seen, LocalDateTime read) { }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> page(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> envelope = JsonUtils.parseMap(response.body());
        assertThat(envelope.get("code")).isEqualTo(200);
        assertThat(envelope.get("data")).isInstanceOf(Map.class);
        return (Map<String, Object>) envelope.get("data");
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
            return get(token, "/notify/inbox?pageNum=" + pageNum + "&pageSize=" + pageSize);
        }

        private HttpResponse<String> get(String token, String path) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10));
            if (token != null) request.header("Authorization", "Bearer " + token);
            return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> post(String token, String path) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token).POST(
                    HttpRequest.BodyPublishers.noBody()).build();
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
        private final StpInterface previousPermissions = SaManager.getStpInterface();
        private final StpLogic previousLogic = StpUtil.getStpLogic();
        private final AtomicBoolean deniedWrites = new AtomicBoolean();

        private OwnedSaSession() {
            byte[] secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer")
                .setJwtSecretKey(Base64.getUrlEncoder().withoutPadding().encodeToString(secret)));
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
            SaManager.setStpInterface(new StpInterface() {
                @Override public List<String> getPermissionList(Object loginId, String loginType) {
                    return deniedWrites.get() ? List.of() : List.of("notify:inbox:seen", "notify:inbox:read");
                }
                @Override public List<String> getRoleList(Object loginId, String loginType) { return List.of(); }
            });
            StpUtil.setStpLogic(new StpLogicJwtForSimple());
        }

        private void denyWrites(boolean denied) { deniedWrites.set(denied); }

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
            SaManager.setStpInterface(previousPermissions);
            StpUtil.setStpLogic(previousLogic);
        }
    }
}
