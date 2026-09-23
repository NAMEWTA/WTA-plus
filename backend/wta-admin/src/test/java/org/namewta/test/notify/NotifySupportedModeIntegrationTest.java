package org.namewta.test.notify;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.satoken.handler.SaTokenExceptionHandler;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationRetryCommand;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.notify.controller.admin.NotificationController;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.namewta.notify.usecase.NotificationApplicationUseCase;
import org.namewta.notify.usecase.NotifyOutboxClaimUseCase;
import org.namewta.system.api.UserService;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Fresh 六 SQL MySQL/Redis + 真实 Claim/DAO/结果事务；仅供应商为替身。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.supported.mode.integration", matches = "true")
class NotifySupportedModeIntegrationTest {
    private static final AtomicLong IDS = new AtomicLong(9_500_000L);
    private final List<Long> ownedIntents = new ArrayList<>();
    private NotifyAtomicResultIntegrationTest fixture;
    private JdbcTemplate db;
    private NotifyNotificationDao dao;
    private NotifyOutboxClaimUseCase claims;
    private DispatchNotificationService dispatch;
    private NotifyClient provider;

    @BeforeEach
    void open() throws Exception {
        assertThat(System.getenv("T50_MYSQL_PASSWORD")).as("owned database password env").isNotBlank();
        assertThat(System.getenv("T36_MYSQL_PASSWORD")).as("shared fixture private password env")
            .isEqualTo(System.getenv("T50_MYSQL_PASSWORD"));
        assertThat(System.getProperty("notify.mysql.integration.url"))
            .matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/namewta_notify_test_[a-zA-Z0-9_]+.*");
        assertThat(System.getProperty("notify.redis.integration.port")).matches("[0-9]+");
        fixture = new NotifyAtomicResultIntegrationTest();
        fixture.open();
        db = field(fixture, "db", JdbcTemplate.class);
        dao = field(fixture, "dao", NotifyNotificationDao.class);
        claims = field(fixture, "claims", NotifyOutboxClaimUseCase.class);
        dispatch = field(fixture, "dispatch", DispatchNotificationService.class);
        provider = field(dispatch, "notifyClient", NotifyClient.class);
    }

    @AfterEach
    void close() {
        try {
            if (db != null) {
                db.execute("drop trigger if exists owned_t50_intent_failure");
                // HTTP 正向请求在服务端分配主键；仅回收本用例固定命名空间产生的行。
                ownedIntents.addAll(db.queryForList("select intent_id from notify_intent "
                    + "where app_id='owned-t50' and scene_code='owned-http'", Long.class));
                for (Long id : ownedIntents) {
                    for (String table : List.of("notify_attempt", "notify_outbox", "notify_delivery", "notify_recipient")) {
                        db.update("delete from " + table + " where intent_id=?", id);
                    }
                    db.update("delete from notify_message_recipient where message_id=?", id);
                    db.update("delete from notify_message where message_id=?", id);
                    db.update("delete from notify_intent where intent_id=?", id);
                }
            }
        } finally {
            if (fixture != null) fixture.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ORDERED_FALLBACK", "ESCALATION"})
    void oldOrderedWaitClosesWithoutProviderOrRepeatedReady(String strategy) {
        Task task = task("MAIL", strategy, "ASYNC", 0, "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 90);
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,status,attempt_count,create_time) "
            + "values(?,?,?,7,'SMS','PENDING',0,utc_timestamp())",
            task.deliveryId() - 1, task.intentId(), task.recipientId());
        dispatch.dispatch(claim(task, "owned-t50-wait"));

        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("FAILED");
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("UNSUPPORTED_MODE_UNSENT");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        assertThat(dao.outbox(task.outboxId()).getAttemptCount()).isZero();
        assertThat(attempts(task)).isZero();
        assertThat(claims.claim("owned-t50-again")).noneMatch(row -> row.getOutboxId().equals(task.outboxId()));
        verify(provider, never()).send(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SYNC", "PRIORITY"})
    void oldModeOrPriorityWithNewProofTerminatesWithoutIOMigration(String unsupported) {
        Task task = "SYNC".equals(unsupported)
            ? task("MAIL", "ALL", "SYNC", 0, "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 90)
            : task("MAIL", "ALL", "ASYNC", 80, "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 90);
        dispatch.dispatch(claim(task, "owned-t50-unsupported"));
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("UNSUPPORTED_MODE_UNSENT");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        assertThat(attempts(task)).isZero();
        verify(provider, never()).send(any());
    }

    @Test
    void markerlessReadyCannotProveNoHistoricalProviderCall() {
        Task task = task("MAIL", "ORDERED_FALLBACK", "ASYNC", 0, "READY", null, 90);
        dispatch.dispatch(claim(task, "owned-t50-markerless"));
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("UNKNOWN");
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("UNSUPPORTED_MODE_OUTCOME_UNKNOWN");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("WAITING_RECEIPT");
        assertThat(attempts(task)).isZero();
        verify(provider, never()).send(any());
    }

    @Test
    void reclaimedProcessingRetainsT39UncertaintyBeforeUnsupportedClassification() {
        Task task = task("MAIL", "ESCALATION", "ASYNC", 0, "PROCESSING", NotifyOutbox.DEADLINE_UNSENT_READY, 90);
        db.update("update notify_outbox set lease_owner='old',lease_token='old',lease_until="
            + "timestampadd(second,-1,utc_timestamp()) where outbox_id=?", task.outboxId());
        NotifyOutbox reclaimed = claim(task, "owned-t50-reclaimed");
        assertThat(reclaimed.getClaimedFromReady()).isFalse();
        dispatch.dispatch(reclaimed);
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("UNKNOWN");
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("RECLAIMED_OUTCOME_UNKNOWN");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("WAITING_RECEIPT");
        verify(provider, never()).send(any());
    }

    @Test
    void existingInAppRecipientRecoversDeliveryWithoutDuplicateMessageOrPush() {
        Task task = task("IN_APP", "ORDERED_FALLBACK", "ASYNC", 0, "READY", null, 90);
        db.update("insert into notify_message(message_id,category,notice_type,channels_json,type,source,title,message,content,create_time) "
            + "values(?,'system','','[\"IN_APP\"]','MESSAGE','BACKEND','Owned title','Owned content','Owned content',utc_timestamp())",
            task.intentId());
        db.update("insert into notify_message_recipient(message_recipient_id,message_id,user_id,create_time) "
            + "values(?,?,7,utc_timestamp())", IDS.incrementAndGet(), task.intentId());
        dispatch.dispatch(claim(task, "owned-t50-inapp-existing"));
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        assertThat(dao.outbox(task.outboxId()).getAttemptCount()).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class,
            task.intentId())).isEqualTo(1);
        assertThat(field(fixture, "realtimeCalls", AtomicInteger.class).get()).isZero();
        assertThat(attempts(task)).isZero();
    }

    @Test
    void inAppOrphanIsLocalFailureAndEmptyTaskConsumesNoBudget() {
        Task orphan = task("IN_APP", "ESCALATION", "ASYNC", 0, "READY", null, 90);
        db.update("insert into notify_message_recipient(message_recipient_id,message_id,user_id,create_time) "
            + "values(?,?,7,utc_timestamp())", IDS.incrementAndGet(), orphan.intentId());
        dispatch.dispatch(claim(orphan, "owned-t50-inapp-orphan"));
        assertThat(dao.delivery(orphan.deliveryId()).getStatus()).isEqualTo("FAILED");
        assertThat(dao.delivery(orphan.deliveryId()).getErrorCode()).isEqualTo("IN_APP_FACTS_INCONSISTENT");
        assertThat(dao.outbox(orphan.outboxId()).getStatus()).isEqualTo("DONE");
        Task empty = task("IN_APP", "ALL", "ASYNC", 1, "READY", null, 90);
        dispatch.dispatch(claim(empty, "owned-t50-inapp-empty"));
        assertThat(dao.delivery(empty.deliveryId()).getErrorCode()).isEqualTo("UNSUPPORTED_MODE_UNSENT");
        assertThat(dao.outbox(empty.outboxId()).getAttemptCount()).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class,
            empty.intentId())).isZero();
        assertThat(field(fixture, "realtimeCalls", AtomicInteger.class).get()).isZero();
    }

    @Test
    void unsupportedRetryRejectsPreviouslySafeLocalFailureWithoutWake() {
        Task task = task("MAIL", "ALL", "ASYNC", -1, "DONE", "UNBOUND_CHANNEL", 90);
        db.update("update notify_delivery set status='FAILED',error_code='UNBOUND_CHANNEL' where delivery_id=?",
            task.deliveryId());
        AtomicInteger wakes = new AtomicInteger();
        var runtime = new NotificationApplicationRuntimeService(dao, mock(UserService.class), dispatch,
            event -> wakes.incrementAndGet());
        assertThatThrownBy(() -> runtime.retry(new NotificationRetryCommand(String.valueOf(task.intentId()),
            String.valueOf(task.deliveryId()), "owned retry", null))).isInstanceOf(ServiceException.class);
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        assertThat(dao.outbox(task.outboxId()).getAttemptCount()).isZero();
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("FAILED");
        assertThat(wakes.get()).isZero();
    }

    @Test
    void invalidDuplicateSubmissionRejectsBeforeReadingOriginalReceiptOrWritingRows() {
        Task original = task("MAIL", "ALL", "ASYNC", 0, "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 90);
        db.update("update notify_intent set idempotency_key='owned-t50-duplicate' where intent_id=?",
            original.intentId());
        var runtime = new NotificationApplicationRuntimeService(dao, mock(UserService.class), dispatch, event -> {});
        var unsupported = new NotificationCommand("owned-t50", "owned", "owned", "owned", "EMAIL",
            List.of("synthetic@example.test"), "owned", Map.of(), List.of(NotificationChannel.MAIL),
            NotificationStrategy.ESCALATION, NotificationMode.ASYNC, 0,
            null, null, "owned-t50-duplicate", Map.of());
        assertThatThrownBy(() -> runtime.submit(unsupported)).isInstanceOf(ServiceException.class);
        assertThat(db.queryForObject("select count(*) from notify_intent where app_id='owned-t50'", Integer.class))
            .isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_outbox where intent_id=?", Integer.class,
            original.intentId())).isEqualTo(1);
        assertThat(dao.intent(original.intentId()).getStatus()).isEqualTo("QUEUED");
    }

    @Test
    void supportedSubmissionStillPersistsPriorityZeroAndOriginalDeadline() {
        var runtime = new NotificationApplicationRuntimeService(dao, mock(UserService.class), dispatch, event -> {});
        var expiry = dao.databaseNow().withNano(0).plusMinutes(5).toInstant(ZoneOffset.UTC);
        var command = new NotificationCommand("owned-t50", "owned", "owned", "owned-positive", "EMAIL",
            List.of("synthetic@example.test"), "owned", Map.of(), List.of(NotificationChannel.MAIL),
            null, null, 0, null, expiry, "owned-t50-positive-" + IDS.incrementAndGet(), Map.of());
        long id = Long.parseLong(runtime.submit(command).notificationId());
        ownedIntents.add(id);
        assertThat(dao.intent(id).getStrategy()).isEqualTo("ALL");
        assertThat(dao.intent(id).getMode()).isEqualTo("ASYNC");
        assertThat(dao.intent(id).getPriority()).isZero();
        assertThat(dao.intent(id).getExpiresAt()).isEqualTo(expiry.atOffset(ZoneOffset.UTC).toLocalDateTime());
        assertThat(db.queryForObject("select count(*) from notify_outbox where intent_id=?", Integer.class, id))
            .isEqualTo(1);
    }

    @Test
    void syncJsonStringIsRejectedByHttpBindingWithoutAWrite() throws Exception {
        var runtime = new NotificationApplicationRuntimeService(dao, mock(UserService.class), dispatch, event -> {});
        var mvc = MockMvcBuilders.standaloneSetup(new NotificationController(new NotificationApplicationUseCase(runtime)))
            .build();
        var response = mvc.perform(post("/notify/notification").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"appId":"owned-t50","sceneCode":"owned","bizType":"owned","bizId":"owned",
                 "recipientType":"EMAIL","recipientIds":["synthetic@example.test"],"templateCode":"owned",
                 "channels":["MAIL"],"strategy":"ALL","mode":"SYNC","priority":0}
                """)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(db.queryForObject("select count(*) from notify_intent where app_id='owned-t50'", Integer.class))
            .isZero();
    }

    @Test
    void loopbackHttpEnforcesSubmitPermissionAndSupportedValuesBeforeAnyWrite() throws Exception {
        var runtime = new NotificationApplicationRuntimeService(dao, mock(UserService.class), dispatch, event -> {});
        var useCase = new NotificationApplicationUseCase(runtime);
        var proxy = new ProxyFactory(useCase);
        proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(
            new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        try (OwnedSaSession sa = new OwnedSaSession();
             OwnedHttp http = new OwnedHttp(new NotificationController(
                 (NotificationApplicationUseCase) proxy.getProxy()))) {
            String allowed = sa.token(95_001L);
            String denied = sa.token(95_002L);
            assertThat(JsonUtils.parseMap(http.post(allowed, commandJson("ALL", "ASYNC", "0")).body())
                .get("code")).isEqualTo(200);
            assertThat(JsonUtils.parseMap(http.post(allowed, commandJson(null, null, "0")).body())
                .get("code")).isEqualTo(200);
            List<Long> positive = db.queryForList("select intent_id from notify_intent "
                + "where app_id='owned-t50' and scene_code='owned-http'", Long.class);
            assertThat(positive).hasSize(2);
            assertThat(db.queryForList("select concat(strategy,':',mode,':',priority) from notify_intent "
                + "where app_id='owned-t50' and scene_code='owned-http'", String.class))
                .containsExactlyInAnyOrder("ALL:ASYNC:0", "ALL:ASYNC:0");
            List<Integer> before = ownedTableCounts();
            for (String[] invalid : List.of(
                new String[] {"ORDERED_FALLBACK", "ASYNC", "0"},
                new String[] {"ESCALATION", "ASYNC", "0"},
                new String[] {"ALL", "ASYNC", "1"},
                new String[] {"ALL", "ASYNC", "-1"})) {
                assertThat(JsonUtils.parseMap(http.post(allowed,
                    commandJson(invalid[0], invalid[1], invalid[2])).body()).get("code"))
                    .isNotEqualTo(200);
                assertThat(ownedTableCounts()).containsExactlyElementsOf(before);
            }
            assertThat(JsonUtils.parseMap(http.post(allowed, commandJson("ALL", "SYNC", "0"))
                .body()).get("code")).isEqualTo(400);
            assertThat(ownedTableCounts()).containsExactlyElementsOf(before);
            // Jackson 3 默认拒绝缺失的 primitive int；策略/模式的构造器默认不放宽该绑定规则。
            assertThat(JsonUtils.parseMap(http.post(allowed, commandJson(null, null, null))
                .body()).get("code")).isEqualTo(400);
            assertThat(ownedTableCounts()).containsExactlyElementsOf(before);
            assertThat(JsonUtils.parseMap(http.post(denied, commandJson("ALL", "ASYNC", "0"))
                .body()).get("code")).isEqualTo(403);
            assertThat(ownedTableCounts()).containsExactlyElementsOf(before);
            assertThat(JsonUtils.parseMap(http.post(null, commandJson("ALL", "ASYNC", "0"))
                .body()).get("code")).isEqualTo(401);
            assertThat(ownedTableCounts()).containsExactlyElementsOf(before);
        }
        verify(provider, never()).send(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"STALE_TOKEN", "EXPIRED_LEASE"})
    void unsupportedHistoricalOwnerMustNotSettleAfterLosingItsFence(String reason) {
        Task task = task("MAIL", "ESCALATION", "ASYNC", 0,
            "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 90);
        NotifyOutbox old = claim(task, "owned-t50-stale");
        if ("STALE_TOKEN".equals(reason)) {
            db.update("update notify_outbox set lease_token='new-owner-token' where outbox_id=?", task.outboxId());
        } else {
            db.update("update notify_outbox set lease_until=timestampadd(second,-1,utc_timestamp()) "
                + "where outbox_id=?", task.outboxId());
        }
        dispatch.dispatch(old);
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("PENDING");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("PROCESSING");
        assertThat(attempts(task)).isZero();
        verify(provider, never()).send(any());
    }

    @Test
    void unsupportedClosurePreservesOtherAcceptedAndUnknownDeliveryFacts() {
        Task task = task("MAIL", "ESCALATION", "ASYNC", 0,
            "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 90);
        long acceptedRecipient = IDS.incrementAndGet();
        long unknownRecipient = IDS.incrementAndGet();
        long acceptedId = IDS.incrementAndGet();
        long unknownId = IDS.incrementAndGet();
        db.update("insert into notify_recipient(recipient_id,intent_id,recipient_type,recipient_key,user_id,status,"
            + "create_time) values(?,?,'USER',?,8,'ACTIVE',utc_timestamp())",
            acceptedRecipient, task.intentId(), String.valueOf(acceptedRecipient));
        db.update("insert into notify_recipient(recipient_id,intent_id,recipient_type,recipient_key,user_id,status,"
            + "create_time) values(?,?,'USER',?,9,'ACTIVE',utc_timestamp())",
            unknownRecipient, task.intentId(), String.valueOf(unknownRecipient));
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,status,"
                + "provider_message_id,attempt_count,create_time) values(?,?,?,8,'MAIL','ACCEPTED',?,1,utc_timestamp())",
            acceptedId, task.intentId(), acceptedRecipient, "owned-external-receipt");
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,status,"
                + "error_code,attempt_count,create_time) values(?,?,?,9,'MAIL','UNKNOWN',?,1,utc_timestamp())",
            unknownId, task.intentId(), unknownRecipient, "OUTCOME_UNKNOWN");
        dispatch.dispatch(claim(task, "owned-t50-mixed"));
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("UNSUPPORTED_MODE_UNSENT");
        assertThat(dao.delivery(acceptedId).getStatus()).isEqualTo("ACCEPTED");
        assertThat(dao.delivery(acceptedId).getProviderMessageId()).isEqualTo("owned-external-receipt");
        assertThat(dao.delivery(unknownId).getStatus()).isEqualTo("UNKNOWN");
        assertThat(dao.delivery(unknownId).getErrorCode()).isEqualTo("OUTCOME_UNKNOWN");
        assertThat(attempts(task)).isZero();
        verify(provider, never()).send(any());
    }

    private List<Integer> ownedTableCounts() {
        List<Integer> counts = new ArrayList<>();
        counts.add(db.queryForObject("select count(*) from notify_intent where app_id='owned-t50'",
            Integer.class));
        for (String table : List.of("notify_recipient", "notify_delivery", "notify_outbox", "notify_attempt")) {
            counts.add(db.queryForObject("select count(*) from " + table
                + " where intent_id in (select intent_id from notify_intent where app_id='owned-t50')",
                Integer.class));
        }
        return counts;
    }

    private static String commandJson(String strategy, String mode, String priority) {
        return "{\"appId\":\"owned-t50\",\"sceneCode\":\"owned-http\","
            + "\"bizType\":\"owned\",\"bizId\":\"owned\",\"recipientType\":\"EMAIL\","
            + "\"recipientIds\":[\"synthetic@example.test\"],\"templateCode\":\"owned\","
            + "\"channels\":[\"MAIL\"],\"templateParams\":{},\"metadata\":{},"
            + "\"idempotencyKey\":\"owned-t50-http-" + IDS.incrementAndGet() + "\""
            + (strategy == null ? "" : ",\"strategy\":\"" + strategy + "\"")
            + (mode == null ? "" : ",\"mode\":\"" + mode + "\"")
            + (priority == null ? "" : ",\"priority\":" + priority) + "}";
    }

    @Test
    void deadlineRemainsPriorToUnsupportedDisposition() {
        Task task = task("MAIL", "ESCALATION", "ASYNC", 0, "READY", NotifyOutbox.DEADLINE_UNSENT_READY, -1);
        dispatch.dispatch(claim(task, "owned-t50-expired"));
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("NOTIFICATION_EXPIRED");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        verify(provider, never()).send(any());
    }

    @Test
    void terminalDeliveryKeepsAcceptedFactWhenOldOutboxIsClosed() {
        Task task = task("MAIL", "ESCALATION", "ASYNC", 0, "READY", null, 90);
        db.update("update notify_delivery set status='ACCEPTED',provider_message_id='owned-accepted' where delivery_id=?",
            task.deliveryId());
        dispatch.dispatch(claim(task, "owned-t50-accepted"));
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("ACCEPTED");
        assertThat(dao.delivery(task.deliveryId()).getProviderMessageId()).isEqualTo("owned-accepted");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        verify(provider, never()).send(any());
    }

    @Test
    void failedFinalAggregateWriteRollsBackDeliveryAndOutboxSettlement() {
        Task task = task("MAIL", "ESCALATION", "ASYNC", 0, "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 90);
        db.execute("create trigger owned_t50_intent_failure before update on notify_intent for each row "
            + "signal sqlstate '45000' set message_text='owned unsupported aggregate failure'");
        NotifyOutbox lease = claim(task, "owned-t50-rollback");
        assertThatThrownBy(() -> dispatch.dispatch(lease)).isInstanceOf(RuntimeException.class);
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("PENDING");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("PROCESSING");
        assertThat(dao.outbox(task.outboxId()).getLeaseToken()).isEqualTo(lease.getLeaseToken());
        assertThat(dao.intent(task.intentId()).getStatus()).isEqualTo("QUEUED");
        assertThat(attempts(task)).isZero();
    }

    private Task task(String channel, String strategy, String mode, int priority,
                      String status, String code, int expiryOffsetSeconds) {
        long intentId = IDS.incrementAndGet(); ownedIntents.add(intentId);
        long recipientId = IDS.incrementAndGet();
        long deliveryId = IDS.incrementAndGet();
        long outboxId = IDS.incrementAndGet();
        db.update("insert into notify_intent(intent_id,app_id,scene_code,template_code,template_params_json,"
            + "strategy,mode,priority,status,expires_at,title_snapshot,content_snapshot,create_time) "
            + "values(?,'owned-t50','owned','owned','{}',?,?,?,'QUEUED',timestampadd(second,?,utc_timestamp()),"
            + "'Owned title','Owned content',utc_timestamp())", intentId, strategy, mode, priority, expiryOffsetSeconds);
        db.update("insert into notify_recipient(recipient_id,intent_id,recipient_type,recipient_key,user_id,status,create_time) "
            + "values(?,?,'USER',?,7,'ACTIVE',utc_timestamp())", recipientId, intentId, String.valueOf(recipientId));
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,target_value,status,"
            + "attempt_count,create_time) values(?,?,?,7,?,?,'PENDING',0,utc_timestamp())",
            deliveryId, intentId, recipientId, channel, "IN_APP".equals(channel) ? "7" : "synthetic@example.test");
        db.update("insert into notify_outbox(outbox_id,intent_id,delivery_id,status,available_at,next_attempt_at,"
            + "attempt_count,max_attempts,last_error_code,create_time) values(?,?,?, ?,utc_timestamp(),utc_timestamp(),"
            + "0,5,?,utc_timestamp())", outboxId, intentId, deliveryId, status, code);
        return new Task(intentId, recipientId, deliveryId, outboxId);
    }

    private NotifyOutbox claim(Task task, String owner) {
        return claims.claim(owner).stream().filter(row -> row.getOutboxId().equals(task.outboxId()))
            .findFirst().orElseThrow();
    }

    private int attempts(Task task) {
        return db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, task.intentId());
    }

    @Configuration
    @EnableWebMvc
    static class OwnedWebConfiguration implements WebMvcConfigurer {
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new SaInterceptor());
        }
    }

    /** 使用真实 HTTP/Servlet/SaInterceptor，但身份和权限只由本测试的私有会话提供。 */
    private static final class OwnedHttp implements AutoCloseable {
        private final Server server = new Server(new InetSocketAddress("127.0.0.1", 0));
        private final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        private final HttpClient client = HttpClient.newBuilder().build();
        private int port;

        private OwnedHttp(NotificationController controller) throws Exception {
            try {
                var handler = new ServletContextHandler();
                handler.setContextPath("/");
                context.setServletContext(handler.getServletContext());
                context.register(OwnedWebConfiguration.class);
                context.addBeanFactoryPostProcessor(factory -> {
                    factory.registerSingleton("notificationController", controller);
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

        private HttpResponse<String> post(String token, String body) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/notify/notification"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json");
            if (token != null) request.header("Authorization", "Bearer " + token);
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
        }

        @Override
        public void close() throws Exception {
            try {
                server.stop();
            } finally {
                context.close();
                client.close();
            }
        }
    }

    /** 隔离 SaToken 全局入口，测试后还原此前配置，避免影响同 JVM 的其他用例。 */
    private static final class OwnedSaSession implements AutoCloseable {
        private final RequestAttributes previousRequestAttributes = RequestContextHolder.getRequestAttributes();
        private final SaTokenConfig previousConfig = SaManager.getConfig();
        private final cn.dev33.satoken.context.SaTokenContext previousContext = SaManager.getSaTokenContext();
        private final cn.dev33.satoken.dao.SaTokenDao previousDao = SaManager.getSaTokenDao();
        private final StpInterface previousPermissions = SaManager.getStpInterface();
        private final StpLogic previousLogic = StpUtil.getStpLogic();

        private OwnedSaSession() {
            SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer"));
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
            SaManager.setStpInterface(new StpInterface() {
                @Override
                public List<String> getPermissionList(Object loginId, String loginType) {
                    return "95001".equals(String.valueOf(loginId))
                        ? List.of("notify:notification:submit") : List.of();
                }
                @Override
                public List<String> getRoleList(Object loginId, String loginType) { return List.of(); }
            });
            StpUtil.setStpLogic(new StpLogic("login"));
        }

        private String token(long loginId) {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest(), new MockHttpServletResponse()));
            StpUtil.login(loginId);
            return StpUtil.getTokenValue();
        }

        @Override
        public void close() {
            if (previousRequestAttributes == null) RequestContextHolder.resetRequestAttributes();
            else RequestContextHolder.setRequestAttributes(previousRequestAttributes);
            SaManager.setConfig(previousConfig);
            SaManager.setSaTokenContext(previousContext);
            SaManager.setSaTokenDao(previousDao);
            SaManager.setStpInterface(previousPermissions);
            StpUtil.setStpLogic(previousLogic);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name, Class<T> type) {
        return (T) type.cast(ReflectionTestUtils.getField(target, name));
    }

    private record Task(long intentId, long recipientId, long deliveryId, long outboxId) { }
}
