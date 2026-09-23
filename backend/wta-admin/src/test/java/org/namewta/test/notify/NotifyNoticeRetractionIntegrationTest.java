package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.model.NotifyRequest;
import org.namewta.common.notify.model.NotifyResult;
import org.namewta.common.notify.model.NotifyRichContent;
import org.namewta.common.notify.model.NotifyStatus;
import org.namewta.common.notify.model.NotifyTargetResult;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationRetryCommand;
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.mapper.NotifyNoticeMapper;
import org.namewta.notify.mapper.NotifyNoticeSnapshotMapper;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.notify.service.NotifyNoticePublisherService;
import org.namewta.notify.service.NotifyNoticeService;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.namewta.notify.usecase.NotificationApplicationUseCase;
import org.namewta.notify.usecase.NotifyNoticeUseCase;
import org.namewta.notify.usecase.NotifyOutboxClaimUseCase;
import org.namewta.notify.support.NotifyNoticeVersionFence;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/** 真实六 SQL、动态事务与 Worker：撤回必须在站内投递领取前建立版本栅栏。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.notice.retraction.integration", matches = "true")
class NotifyNoticeRetractionIntegrationTest {
    private static final long POSITIVE_NOTICE = 9_640_000_001L;
    private static final long RETRACTED_NOTICE = 9_640_000_002L;
    private static final long USER = 7L;
    private static final long MAIL_ACCOUNT = 9_640_000_003L;
    private static final long MAIL_BINDING = 2_100_630_000_000_000_009L;
    private static final long FOREIGN_INTENT = 9_640_000_004L;

    private NotifyAtomicResultIntegrationTest fixture;
    private JdbcTemplate db;
    private NotifyNotificationDao runtimeDao;
    private NotifyOutboxClaimUseCase claims;
    private DispatchNotificationService dispatch;
    private NotifyNoticeUseCase notices;
    private RedissonClient redis;
    private AtomicInteger realtimeCalls;
    private Long restoreStaticSeed;
    private boolean restoreMailBinding;
    private NotificationApplicationService application;
    private final AtomicReference<NotificationCommand> publishedCommand = new AtomicReference<>();
    private final AtomicInteger wakeEvents = new AtomicInteger();

    @BeforeEach
    void open() throws Exception {
        assertThat(System.getenv("T40_MYSQL_PASSWORD")).as("owned MySQL password in child environment").isNotBlank();
        assertThat(System.getenv("T36_MYSQL_PASSWORD")).as("shared fixture password in child environment")
            .isEqualTo(System.getenv("T40_MYSQL_PASSWORD"));
        assertThat(System.getProperty("notify.mysql.integration.url"))
            .matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/namewta_notify_test_[a-zA-Z0-9_]+.*");
        assertThat(System.getProperty("notify.redis.integration.port")).matches("[0-9]+");

        fixture = new NotifyAtomicResultIntegrationTest();
        fixture.open();
        db = field("db", JdbcTemplate.class);
        runtimeDao = field("dao", NotifyNotificationDao.class);
        claims = field("claims", NotifyOutboxClaimUseCase.class);
        dispatch = field("dispatch", DispatchNotificationService.class);
        redis = field("redis", RedissonClient.class);
        realtimeCalls = field("realtimeCalls", AtomicInteger.class);

        var noticeDao = new NotifyPersistenceDao(fixture.sessions.getMapper(NotifyNoticeMapper.class),
            fixture.sessions.getMapper(NotifyNoticeSnapshotMapper.class));
        var user = new UserDTO();
        user.setUserId(USER);
        user.setStatus("0");
        user.setEmail("t40-owned@example.test");
        var users = mock(UserService.class);
        when(users.selectNotificationUsers(List.of(USER))).thenReturn(List.of(user));
        application = transactional(new NotificationApplicationUseCase(
            new NotificationApplicationRuntimeService(runtimeDao, users, dispatch,
                event -> wakeEvents.incrementAndGet())),
            NotificationApplicationUseCase.class);
        NotificationApplicationService publication = mock(NotificationApplicationService.class);
        when(publication.submit(any(NotificationCommand.class))).thenAnswer(invocation -> {
            NotificationCommand command = invocation.getArgument(0);
            publishedCommand.set(command);
            return application.submit(command);
        });
        notices = transactional(new NotifyNoticeUseCase(new NotifyNoticeService(noticeDao, users, runtimeDao),
            new NotifyNoticePublisherService(publication, noticeDao, users, runtimeDao)), NotifyNoticeUseCase.class);
    }

    @AfterEach
    void close() {
        try {
            if (db != null) {
                db.execute("drop trigger if exists owned_t40_path_failure");
                db.execute("drop trigger if exists owned_t40_retract_failure");
                if (restoreMailBinding) {
                    db.update("update notify_scene_binding set account_id=null where binding_id=? and account_id=?",
                        MAIL_BINDING, MAIL_ACCOUNT);
                    db.update("delete from notify_channel_account where account_id=?", MAIL_ACCOUNT);
                }
                if (restoreStaticSeed != null) db.update("update notify_notice set lifecycle='PUBLISHED',"
                    + "retracted_at=null where notice_id=?", restoreStaticSeed);
                for (long noticeId : List.of(POSITIVE_NOTICE, RETRACTED_NOTICE)) {
                    List<Long> ids = db.queryForList("select intent_id from notify_intent "
                        + "where app_id='notify' and biz_type='NOTICE_PUBLISHED' and biz_id=?", Long.class,
                        String.valueOf(noticeId));
                    for (Long intentId : ids) {
                        for (String table : List.of("notify_attempt", "notify_outbox", "notify_delivery", "notify_recipient")) {
                            db.update("delete from " + table + " where intent_id=?", intentId);
                        }
                        db.update("delete from notify_message_recipient where message_id=?", intentId);
                        db.update("delete from notify_message where message_id=?", intentId);
                        db.update("delete from notify_intent where intent_id=?", intentId);
                    }
                    db.update("delete from notify_notice_snapshot where notice_id=?", noticeId);
                    db.update("delete from notify_notice where notice_id=?", noticeId);
                }
            }
        } finally {
            if (fixture != null) fixture.close();
        }
    }

    @Test
    void retractBeforeWorkerClaimStopsCurrentVersionAfterPositiveDeliveryControl() {
        insertDraft(POSITIVE_NOTICE, "T40 positive notice");
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent positive = intent(POSITIVE_NOTICE);
        assertThat(positive).as("real publication created an Intent").isNotNull();
        dispatch.dispatch(claimOne(positive.getIntentId(), "t40-positive"));
        assertThat(count("notify_message", positive.getIntentId())).isEqualTo(1);
        assertThat(count("notify_message_recipient", positive.getIntentId())).isEqualTo(1);
        assertThat(db.queryForObject("select category from notify_message where message_id=?", String.class,
            positive.getIntentId())).isEqualTo("notice");
        assertDeepLink(positive.getIntentId(), POSITIVE_NOTICE, 1);
        long persistedCalls = redis.getAtomicLong("owned-t22-provider-calls").get();
        int pushed = realtimeCalls.get();
        assertThat(persistedCalls).isEqualTo(1);
        assertThat(pushed).isEqualTo(1);

        insertDraft(RETRACTED_NOTICE, "T40 retracted notice");
        notices.publish(RETRACTED_NOTICE);
        NotifyIntent withdrawn = intent(RETRACTED_NOTICE);
        assertThat(withdrawn).as("real publication created the retractable version").isNotNull();
        long outboxId = db.queryForObject("select outbox_id from notify_outbox where intent_id=?", Long.class,
            withdrawn.getIntentId());
        assertThat(runtimeDao.outbox(outboxId).getStatus()).isEqualTo("READY");
        assertThat(count("notify_message", withdrawn.getIntentId())).isZero();
        assertThat(count("notify_message_recipient", withdrawn.getIntentId())).isZero();
        assertDeepLink(withdrawn.getIntentId(), RETRACTED_NOTICE, 1);

        assertThat(notices.retract(RETRACTED_NOTICE)).isEqualTo(1);
        assertThat(db.queryForObject("select lifecycle from notify_notice where notice_id=?", String.class,
            RETRACTED_NOTICE)).isEqualTo("RETRACTED");
        dispatch.dispatch(claimOne(withdrawn.getIntentId(), "t40-after-retract"));

        // 旧实现会在撤回后照常持久化站内消息；此处是第一条预期业务红灯。
        assertThat(count("notify_message", withdrawn.getIntentId())).isZero();
        assertThat(count("notify_message_recipient", withdrawn.getIntentId())).isZero();
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class,
            withdrawn.getIntentId())).isZero();
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isEqualTo(persistedCalls);
        assertThat(realtimeCalls.get()).isEqualTo(pushed);
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class,
            withdrawn.getIntentId())).isEqualTo("CANCELLED");
        assertThat(runtimeDao.outbox(outboxId).getStatus()).isEqualTo("DONE");
        assertThat(count("notify_message", positive.getIntentId())).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(longs = {1761800000000000001L, 1761800000000000002L})
    void onlyExactStaticSeedMayRetractWithoutAnIntent(long seedId) {
        assertThat(db.queryForObject("select lifecycle from notify_notice where notice_id=?", String.class, seedId))
            .isEqualTo("PUBLISHED");
        assertThat(db.queryForObject("select count(*) from notify_notice_snapshot where notice_id=?", Integer.class,
            seedId)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_intent where biz_type='NOTICE_PUBLISHED' "
            + "and biz_id=?", Integer.class, String.valueOf(seedId))).isZero();
        restoreStaticSeed = seedId;
        assertThat(notices.retract(seedId)).isEqualTo(1);
        assertThat(notices.retract(seedId)).as("verified seed repeat is idempotent").isEqualTo(1);
        assertThat(db.queryForObject("select lifecycle from notify_notice where notice_id=?", String.class, seedId))
            .isEqualTo("RETRACTED");
        assertThat(db.queryForObject("select count(*) from notify_notice_snapshot where notice_id=?", Integer.class,
            seedId)).isEqualTo(1);

        insertPublishedWithoutIntent(POSITIVE_NOTICE);
        assertThatThrownBy(() -> notices.retract(POSITIVE_NOTICE)).isInstanceOf(RuntimeException.class);
        assertThat(db.queryForObject("select lifecycle from notify_notice where notice_id=?", String.class,
            POSITIVE_NOTICE)).isEqualTo("PUBLISHED");
    }

    @Test
    void lateSnapshotPathFailureRollsBackNoticeIntentAndOutbox() {
        insertDraft(POSITIVE_NOTICE, "T40 rollback notice");
        List<Integer> before = List.of("notify_intent", "notify_recipient", "notify_delivery", "notify_outbox",
            "notify_attempt", "notify_message", "notify_message_recipient").stream()
            .map(table -> db.queryForObject("select count(*) from " + table, Integer.class)).toList();
        db.execute("create trigger owned_t40_path_failure before update on notify_notice_snapshot "
            + "for each row signal sqlstate '45000' set message_text='owned snapshot update failure'");

        assertThatThrownBy(() -> notices.publish(POSITIVE_NOTICE)).isInstanceOf(RuntimeException.class);
        assertThat(db.queryForObject("select lifecycle from notify_notice where notice_id=?", String.class,
            POSITIVE_NOTICE)).isEqualTo("DRAFT");
        assertThat(db.queryForObject("select count(*) from notify_notice_snapshot where notice_id=?", Integer.class,
            POSITIVE_NOTICE)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_intent where app_id='notify' "
            + "and biz_type='NOTICE_PUBLISHED' and biz_id=?", Integer.class,
            String.valueOf(POSITIVE_NOTICE))).isZero();
        assertThat(List.of("notify_intent", "notify_recipient", "notify_delivery", "notify_outbox",
            "notify_attempt", "notify_message", "notify_message_recipient").stream()
            .map(table -> db.queryForObject("select count(*) from " + table, Integer.class)).toList())
            .containsExactlyElementsOf(before);
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isZero();
        assertThat(realtimeCalls.get()).isZero();
    }

    @Test
    void lateNoticeLifecycleFailureRollsBackEarlierVersionMetadataWrite() {
        insertDraft(POSITIVE_NOTICE, "T40 retract rollback");
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent current = intent(POSITIVE_NOTICE);
        String beforeMetadata = db.queryForObject("select metadata_json from notify_intent where intent_id=?",
            String.class, current.getIntentId());
        db.execute("create trigger owned_t40_retract_failure before update on notify_notice "
            + "for each row signal sqlstate '45000' set message_text='owned retract update failure'");

        assertThatThrownBy(() -> notices.retract(POSITIVE_NOTICE)).isInstanceOf(RuntimeException.class);
        assertThat(db.queryForObject("select lifecycle from notify_notice where notice_id=?", String.class,
            POSITIVE_NOTICE)).isEqualTo("PUBLISHED");
        assertThat(db.queryForObject("select metadata_json from notify_intent where intent_id=?", String.class,
            current.getIntentId())).isEqualTo(beforeMetadata);
        assertThat(NotifyNoticeVersionFence.state(runtimeDao.intent(current.getIntentId())))
            .isEqualTo(NotifyNoticeVersionFence.State.ACTIVE);
        assertThat(db.queryForObject("select status from notify_outbox where intent_id=?", String.class,
            current.getIntentId())).isEqualTo("READY");
    }

    @Test
    void externalOnlyConflictingIdempotencyIdentityRollsBackNewNoticePublication() {
        insertDraft(POSITIVE_NOTICE, "T40 conflicting external", "[\"MAIL\"]");
        String wrongSnapshot = JsonUtils.toJsonString(NotifyNoticeVersionFence.initial(
            POSITIVE_NOTICE, 9_640_000_005L, 1));
        assertThat(db.update("insert into notify_intent(intent_id,app_id,scene_code,biz_type,biz_id,"
            + "template_code,template_params_json,strategy,mode,status,idempotency_key,metadata_json,create_time) "
            + "values(?,'notify','notice-published','NOTICE_PUBLISHED',?,'notice-published','{}',"
            + "'ALL','ASYNC','QUEUED',?,?,utc_timestamp())", FOREIGN_INTENT,
            String.valueOf(POSITIVE_NOTICE), NotifyNoticeVersionFence.idempotencyKey(POSITIVE_NOTICE, 1),
            wrongSnapshot)).isEqualTo(1);

        assertThatThrownBy(() -> notices.publish(POSITIVE_NOTICE)).isInstanceOf(RuntimeException.class);
        assertThat(db.queryForObject("select lifecycle from notify_notice where notice_id=?", String.class,
            POSITIVE_NOTICE)).isEqualTo("DRAFT");
        assertThat(db.queryForObject("select count(*) from notify_notice_snapshot where notice_id=?",
            Integer.class, POSITIVE_NOTICE)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_outbox where intent_id=?", Integer.class,
            FOREIGN_INTENT)).isZero();
        assertThat(db.queryForObject("select metadata_json from notify_intent where intent_id=?", String.class,
            FOREIGN_INTENT)).contains("noticeVersion");
        assertThat(NotifyNoticeVersionFence.state(runtimeDao.intent(FOREIGN_INTENT)))
            .isEqualTo(NotifyNoticeVersionFence.State.ACTIVE);
    }

    @Test
    void republishedVersionCanDeliverWhileRetractedVersionAndSnapshotStaySeparate() {
        insertDraft(POSITIVE_NOTICE, "T40 first version");
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent first = intent(POSITIVE_NOTICE);
        assertThat(first).isNotNull();
        assertThat(notices.retract(POSITIVE_NOTICE)).isEqualTo(1);
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent second = runtimeDao.intentByIdempotency("notify",
            "notice-published:" + POSITIVE_NOTICE + ":2");
        assertThat(second).isNotNull();
        assertThat(second.getIntentId()).isNotEqualTo(first.getIntentId());
        assertThat(NotifyNoticeVersionFence.state(runtimeDao.intent(first.getIntentId())))
            .isEqualTo(NotifyNoticeVersionFence.State.RETRACTED);
        assertThat(NotifyNoticeVersionFence.state(second)).isEqualTo(NotifyNoticeVersionFence.State.ACTIVE);
        assertDeepLink(first.getIntentId(), POSITIVE_NOTICE, 1);
        assertDeepLink(second.getIntentId(), POSITIVE_NOTICE, 2);

        List<NotifyOutbox> claimed = claims.claim("t40-two-versions").stream()
            .filter(row -> row.getIntentId().equals(first.getIntentId())
                || row.getIntentId().equals(second.getIntentId())).toList();
        assertThat(claimed).hasSize(2);
        claimed.forEach(dispatch::dispatch);
        assertThat(count("notify_message", first.getIntentId())).isZero();
        assertThat(count("notify_message", second.getIntentId())).isEqualTo(1);
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class,
            first.getIntentId())).isEqualTo("CANCELLED");
        assertRetractedMetadata(first.getIntentId());
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class,
            second.getIntentId())).isEqualTo("DELIVERED");
        assertThat(db.queryForObject("select count(*) from notify_notice_snapshot where notice_id=?", Integer.class,
            POSITIVE_NOTICE)).isEqualTo(2);
    }

    @Test
    void exactDuplicateKeepsOriginalReceiptButManualRetryCannotWakeRetractedVersion() {
        insertDraft(POSITIVE_NOTICE, "T40 duplicate notice");
        notices.publish(POSITIVE_NOTICE);
        NotificationCommand original = publishedCommand.get();
        assertThat(original).isNotNull();
        NotifyIntent first = intent(POSITIVE_NOTICE);
        long deliveryId = db.queryForObject("select delivery_id from notify_delivery where intent_id=?",
            Long.class, first.getIntentId());
        int wakeBefore = wakeEvents.get();
        assertThat(notices.retract(POSITIVE_NOTICE)).isEqualTo(1);
        assertThat(notices.retract(POSITIVE_NOTICE)).as("the same version remains withdrawn").isEqualTo(1);

        NotificationReceipt duplicate = application.submit(original);
        assertThat(duplicate.notificationId()).isEqualTo(String.valueOf(first.getIntentId()));
        assertThatThrownBy(() -> application.retry(new NotificationRetryCommand(
            String.valueOf(first.getIntentId()), String.valueOf(deliveryId), "owned retry", null)))
            .hasMessageContaining("公告版本");
        assertThat(wakeEvents.get()).isEqualTo(wakeBefore);
        assertThat(db.queryForObject("select count(*) from notify_outbox where intent_id=?", Integer.class,
            first.getIntentId())).isEqualTo(1);
        assertThat(db.queryForObject("select status from notify_outbox where intent_id=?", String.class,
            first.getIntentId())).isEqualTo("READY");
    }

    @Test
    void legacyAuditOnlyVersionIsUnverifiedAndNeverBlindlyDelivered() {
        insertDraft(POSITIVE_NOTICE, "T40 legacy metadata");
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent current = intent(POSITIVE_NOTICE);
        assertThat(db.update("update notify_intent set metadata_json='{\"audit\":\"NOTICE_SNAPSHOT\"}' "
            + "where intent_id=?", current.getIntentId())).isEqualTo(1);
        assertThat(NotifyNoticeVersionFence.state(runtimeDao.intent(current.getIntentId())))
            .isEqualTo(NotifyNoticeVersionFence.State.UNVERIFIED);
        assertThatThrownBy(() -> application.retry(new NotificationRetryCommand(
            String.valueOf(current.getIntentId()), null, "owned legacy", null)))
            .hasMessageContaining("公告版本");
        int wakeBefore = wakeEvents.get();
        dispatch.dispatch(claimOne(current.getIntentId(), "t40-legacy-unverified"));
        assertThat(count("notify_message", current.getIntentId())).isZero();
        assertThat(count("notify_message_recipient", current.getIntentId())).isZero();
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class,
            current.getIntentId())).isEqualTo("CANCELLED");
        assertThat(db.queryForObject("select error_code from notify_delivery where intent_id=?", String.class,
            current.getIntentId())).isEqualTo("NOTICE_VERSION_UNVERIFIED");
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class,
            current.getIntentId())).isZero();
        assertThat(wakeEvents.get()).isEqualTo(wakeBefore);
        assertThat(realtimeCalls.get()).isZero();
    }

    @Test
    void legacyExternalWithoutUnsentProvenanceWaitsForReconciliationInsteadOfResend() {
        bindOwnedMailAccount();
        insertDraft(POSITIVE_NOTICE, "T40 legacy external", "[\"MAIL\"]");
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent current = intent(POSITIVE_NOTICE);
        assertThat(db.update("update notify_intent set metadata_json='{\"audit\":\"NOTICE_SNAPSHOT\"}' "
            + "where intent_id=?", current.getIntentId())).isEqualTo(1);
        assertThat(db.update("update notify_outbox set last_error_code=null where intent_id=?",
            current.getIntentId())).isEqualTo(1);
        AtomicInteger sends = new AtomicInteger();
        NotifyClient provider = mock(NotifyClient.class);
        when(provider.send(any(NotifyRequest.class))).thenAnswer(invocation -> {
            sends.incrementAndGet();
            throw new AssertionError("historical provenance cannot authorize a new external call");
        });
        mailDispatcher(provider).dispatch(claimOne(current.getIntentId(), "t40-legacy-external"));
        assertThat(sends.get()).isZero();
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class,
            current.getIntentId())).isEqualTo("UNKNOWN");
        assertThat(db.queryForObject("select error_code from notify_delivery where intent_id=?", String.class,
            current.getIntentId())).isEqualTo("NOTICE_VERSION_OUTCOME_UNKNOWN");
        assertThat(db.queryForObject("select status from notify_outbox where intent_id=?", String.class,
            current.getIntentId())).isEqualTo("WAITING_RECEIPT");
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class,
            current.getIntentId())).isZero();
    }

    @Test
    void oldLeaseOwnerCannotSettleRetractedNoticeAfterRealReclaim() {
        insertDraft(POSITIVE_NOTICE, "T40 stale owner");
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent current = intent(POSITIVE_NOTICE);
        NotifyOutbox old = claimOne(current.getIntentId(), "t40-old-owner");
        assertThat(db.update("update notify_outbox set lease_until=timestampadd(second,-1,utc_timestamp()) "
            + "where outbox_id=? and lease_token=?", old.getOutboxId(), old.getLeaseToken())).isEqualTo(1);
        NotifyOutbox fresh = claimOne(current.getIntentId(), "t40-new-owner");
        assertThat(fresh.getLeaseToken()).isNotEqualTo(old.getLeaseToken());
        assertThat(notices.retract(POSITIVE_NOTICE)).isEqualTo(1);
        dispatch.dispatch(old);
        assertThat(db.queryForObject("select status from notify_outbox where outbox_id=?", String.class,
            old.getOutboxId())).isEqualTo("PROCESSING");
        assertThat(db.queryForObject("select lease_token from notify_outbox where outbox_id=?", String.class,
            old.getOutboxId())).isEqualTo(fresh.getLeaseToken());
        assertThat(count("notify_message", current.getIntentId())).isZero();
        dispatch.dispatch(fresh);
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class,
            current.getIntentId())).isEqualTo("CANCELLED");
        assertThat(db.queryForObject("select status from notify_outbox where outbox_id=?", String.class,
            old.getOutboxId())).isEqualTo("DONE");
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class,
            current.getIntentId())).isZero();
    }

    @Test
    void realMailPlannerUsesGenericPathWithoutInAppAndPersonalPathWhenMixed() {
        bindOwnedMailAccount();
        var sent = new ArrayList<NotifyRequest>();
        NotifyClient provider = mock(NotifyClient.class);
        when(provider.send(any(NotifyRequest.class))).thenAnswer(invocation -> {
            NotifyRequest request = invocation.getArgument(0);
            sent.add(request);
            return new NotifyResult(request.requestId(), request.channel(), request.providerKey(),
                NotifyStatus.ACCEPTED, List.of(NotifyTargetResult.accepted(request.targets().getFirst(),
                    "owned-t40-mail-receipt", 1)));
        });
        var mailDispatch = mailDispatcher(provider);

        insertDraft(POSITIVE_NOTICE, "T40 external only", "[\"MAIL\"]");
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent external = intent(POSITIVE_NOTICE);
        assertThat(external).isNotNull();
        assertGenericLink(external.getIntentId(), POSITIVE_NOTICE);
        mailDispatch.dispatch(claimOne(external.getIntentId(), "t40-external-mail"));
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().content()).isInstanceOf(NotifyRichContent.class);
        assertThat(((NotifyRichContent) sent.getFirst().content()).content()).contains("/notify/inbox")
            .doesNotContain("messageId=");

        insertDraft(RETRACTED_NOTICE, "T40 mixed", "[\"IN_APP\",\"MAIL\"]");
        notices.publish(RETRACTED_NOTICE);
        NotifyIntent mixed = intent(RETRACTED_NOTICE);
        assertThat(mixed).isNotNull();
        assertDeepLink(mixed.getIntentId(), RETRACTED_NOTICE, 1);
        List<NotifyOutbox> mixedTasks = claims.claim("t40-mixed-mail").stream()
            .filter(row -> row.getIntentId().equals(mixed.getIntentId())).toList();
        assertThat(mixedTasks).hasSize(2);
        NotifyOutbox mail = mixedTasks.stream().filter(row -> "MAIL".equals(
            runtimeDao.delivery(row.getDeliveryId()).getChannel())).findFirst().orElseThrow();
        mailDispatch.dispatch(mail);
        assertThat(sent).hasSize(2);
        assertThat(((NotifyRichContent) sent.get(1).content()).content())
            .contains("/notify/inbox?messageId=" + mixed.getIntentId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCEPTED", "OUTCOME_UNKNOWN", "UNSENT_TERMINAL"})
    void retractionAfterProviderGatePreservesActualExternalResult(String outcome) throws Exception {
        bindOwnedMailAccount();
        insertDraft(POSITIVE_NOTICE, "T40 in-flight external", "[\"MAIL\"]");
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent current = intent(POSITIVE_NOTICE);
        NotifyOutbox leased = claimOne(current.getIntentId(), "t40-before-retract");
        CountDownLatch enteredProvider = new CountDownLatch(1);
        CountDownLatch finishProvider = new CountDownLatch(1);
        AtomicInteger sends = new AtomicInteger();
        NotifyClient provider = mock(NotifyClient.class);
        when(provider.send(any(NotifyRequest.class))).thenAnswer(invocation -> {
            NotifyRequest request = invocation.getArgument(0);
            assertThat(TransactionContext.getXID()).as("external Provider I/O stays outside the DB transaction")
                .isNull();
            sends.incrementAndGet();
            enteredProvider.countDown();
            assertThat(finishProvider.await(10, TimeUnit.SECONDS)).isTrue();
            NotifyTargetResult target = switch (outcome) {
                case "ACCEPTED" -> NotifyTargetResult.accepted(request.targets().getFirst(), "owned-result", 1);
                case "OUTCOME_UNKNOWN" -> NotifyTargetResult.outcomeUnknown(request.targets().getFirst(), 1);
                default -> NotifyTargetResult.unsentTerminal(request.targets().getFirst(), "OWNED_REJECT", 1);
            };
            return new NotifyResult(request.requestId(), request.channel(), request.providerKey(),
                "ACCEPTED".equals(outcome) ? NotifyStatus.ACCEPTED : NotifyStatus.FAILED, List.of(target));
        });
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var delivery = workers.submit(() -> mailDispatcher(provider).dispatch(leased));
            try {
                assertThat(enteredProvider.await(10, TimeUnit.SECONDS))
                    .as("the first DB gate finished before retraction, and Provider was entered").isTrue();
                assertThat(notices.retract(POSITIVE_NOTICE)).isEqualTo(1);
            } finally {
                finishProvider.countDown();
            }
            delivery.get(10, TimeUnit.SECONDS);
        }
        assertThat(sends.get()).isEqualTo(1);
        String expected = switch (outcome) {
            case "ACCEPTED" -> "ACCEPTED";
            case "OUTCOME_UNKNOWN" -> "UNKNOWN";
            default -> "FAILED";
        };
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class,
            current.getIntentId())).isEqualTo(expected);
        assertThat(db.queryForObject("select status from notify_attempt where intent_id=?", String.class,
            current.getIntentId())).isEqualTo(expected);
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class,
            current.getIntentId())).isEqualTo(1);
        assertRetractedMetadata(current.getIntentId());
        assertThat(db.queryForObject("select status from notify_outbox where intent_id=?", String.class,
            current.getIntentId())).isEqualTo("OUTCOME_UNKNOWN".equals(outcome) ? "WAITING_RECEIPT" : "DONE");
    }

    @Test
    void retractionBeforeSecondThreadGateStopsItsClaimWithoutInAppSideEffects() throws Exception {
        insertDraft(POSITIVE_NOTICE, "T40 gate after retract");
        notices.publish(POSITIVE_NOTICE);
        NotifyIntent current = intent(POSITIVE_NOTICE);
        NotifyOutbox leased = claimOne(current.getIntentId(), "t40-gate-after");
        CountDownLatch workerReady = new CountDownLatch(1);
        CountDownLatch beginGate = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var delivery = workers.submit(() -> {
                workerReady.countDown();
                assertThat(beginGate.await(10, TimeUnit.SECONDS)).isTrue();
                dispatch.dispatch(leased);
                return null;
            });
            assertThat(workerReady.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                assertThat(notices.retract(POSITIVE_NOTICE)).isEqualTo(1);
            } finally {
                beginGate.countDown();
            }
            delivery.get(10, TimeUnit.SECONDS);
        }
        assertThat(count("notify_message", current.getIntentId())).isZero();
        assertThat(count("notify_message_recipient", current.getIntentId())).isZero();
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class,
            current.getIntentId())).isZero();
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class,
            current.getIntentId())).isEqualTo("CANCELLED");
        assertThat(realtimeCalls.get()).isZero();
    }

    private void bindOwnedMailAccount() {
        assertThat(db.update("insert into notify_channel_account(account_id,channel,config_key,enabled,"
            + "minute_max,create_time) values(?,'MAIL','owned-t40-mail','Y',60,utc_timestamp())",
            MAIL_ACCOUNT)).isEqualTo(1);
        restoreMailBinding = true;
        assertThat(db.update("update notify_scene_binding set account_id=? where binding_id=? and account_id is null",
            MAIL_ACCOUNT, MAIL_BINDING)).isEqualTo(1);
    }

    private DispatchNotificationService mailDispatcher(NotifyClient provider) {
        @SuppressWarnings("unchecked")
        ObjectProvider<InAppNotificationPort> inApp = mock(ObjectProvider.class);
        return new DispatchNotificationService(runtimeDao, provider, inApp,
            field("configDao", NotifyConfigDao.class), (key, limit, window) -> true,
            field("results", NotifyDispatchResultPort.class));
    }

    private void insertPublishedWithoutIntent(long noticeId) {
        db.update("insert into notify_notice(notice_id,notice_title,notice_type,notice_content,recipient_type,"
                + "recipient_ids_json,user_type_ids_json,channels_json,status,lifecycle,published_at,create_time) "
                + "values(?,'Owned missing task','1','Owned body','ALL','[]','[]','[\"IN_APP\"]','0','PUBLISHED',"
                + "utc_timestamp(),utc_timestamp())", noticeId);
        db.update("insert into notify_notice_snapshot(snapshot_id,notice_id,snapshot_version,title_snapshot,"
                + "content_snapshot,notice_type,path_snapshot,published_at,create_time) "
                + "select notice_id,notice_id,1,notice_title,notice_content,notice_type,'/notify/inbox',"
                + "published_at,create_time from notify_notice where notice_id=?", noticeId);
    }

    private void assertDeepLink(long intentId, long noticeId, int version) {
        String path = "/notify/inbox?messageId=" + intentId;
        assertThat(db.queryForObject("select path_snapshot from notify_intent where intent_id=?", String.class,
            intentId)).isEqualTo(path);
        assertThat(db.queryForObject("select json_unquote(json_extract(template_params_json,'$.path')) "
            + "from notify_intent where intent_id=?", String.class, intentId)).isEqualTo(path);
        assertThat(db.queryForObject("select path_snapshot from notify_notice_snapshot "
            + "where notice_id=? and snapshot_version=?", String.class, noticeId, version)).isEqualTo(path);
    }

    private void assertRetractedMetadata(long intentId) {
        String json = db.queryForObject("select metadata_json from notify_intent where intent_id=?", String.class,
            intentId);
        Map<?, ?> metadata = JsonUtils.parseObject(json, Map.class);
        assertThat(metadata.get("noticeVersion")).isInstanceOf(String.class);
        Map<?, ?> version = JsonUtils.parseObject((String) metadata.get("noticeVersion"), Map.class);
        assertThat(version.get("retracted")).isEqualTo(Boolean.TRUE);
        assertThat(NotifyNoticeVersionFence.state(runtimeDao.intent(intentId)))
            .isEqualTo(NotifyNoticeVersionFence.State.RETRACTED);
    }

    private void assertGenericLink(long intentId, long noticeId) {
        assertThat(db.queryForObject("select path_snapshot from notify_intent where intent_id=?", String.class,
            intentId)).isEqualTo("/notify/inbox");
        assertThat(db.queryForObject("select json_unquote(json_extract(template_params_json,'$.path')) "
            + "from notify_intent where intent_id=?", String.class, intentId)).isEqualTo("/notify/inbox");
        assertThat(db.queryForObject("select path_snapshot from notify_notice_snapshot where notice_id=?",
            String.class, noticeId)).isEqualTo("/notify/inbox");
    }

    private void insertDraft(long noticeId, String title) {
        insertDraft(noticeId, title, "[\"IN_APP\"]");
    }

    private void insertDraft(long noticeId, String title, String channels) {
        db.update("insert into notify_notice(notice_id,notice_title,notice_type,notice_content,recipient_type,"
                + "recipient_ids_json,user_type_ids_json,channels_json,status,lifecycle,create_time) "
                + "values(?,?,'1',?,'USER','[7]','[]',?,'1','DRAFT',utc_timestamp())",
            noticeId, title, "Owned synthetic notice body " + noticeId, channels);
    }

    private NotifyIntent intent(long noticeId) {
        return runtimeDao.intentByIdempotency("notify", "notice-published:" + noticeId + ":1");
    }

    private NotifyOutbox claimOne(long intentId, String owner) {
        List<NotifyOutbox> owned = claims.claim(owner).stream()
            .filter(row -> row.getIntentId().equals(intentId)).toList();
        assertThat(owned).as("the published notice has one real leased task").hasSize(1);
        return owned.getFirst();
    }

    private int count(String table, long intentId) {
        return db.queryForObject("select count(*) from " + table + " where message_id=?", Integer.class, intentId);
    }

    private <T> T field(String name, Class<T> type) {
        return type.cast(ReflectionTestUtils.getField(fixture, name));
    }

    private static <T> T transactional(T target, Class<T> type) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(
            new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return type.cast(proxy.getProxy());
    }
}
