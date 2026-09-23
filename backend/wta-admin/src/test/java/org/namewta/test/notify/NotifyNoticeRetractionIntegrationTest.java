package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.mapper.NotifyNoticeMapper;
import org.namewta.notify.mapper.NotifyNoticeSnapshotMapper;
import org.namewta.notify.service.NotifyNoticePublisherService;
import org.namewta.notify.service.NotifyNoticeService;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.namewta.notify.usecase.NotificationApplicationUseCase;
import org.namewta.notify.usecase.NotifyNoticeUseCase;
import org.namewta.notify.usecase.NotifyOutboxClaimUseCase;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 真实六 SQL、动态事务与 Worker：撤回必须在站内投递领取前建立版本栅栏。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.notice.retraction.integration", matches = "true")
class NotifyNoticeRetractionIntegrationTest {
    private static final long POSITIVE_NOTICE = 9_640_000_001L;
    private static final long RETRACTED_NOTICE = 9_640_000_002L;
    private static final long USER = 7L;

    private NotifyAtomicResultIntegrationTest fixture;
    private JdbcTemplate db;
    private NotifyNotificationDao runtimeDao;
    private NotifyOutboxClaimUseCase claims;
    private DispatchNotificationService dispatch;
    private NotifyNoticeUseCase notices;
    private RedissonClient redis;
    private AtomicInteger realtimeCalls;

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
        var users = mock(UserService.class);
        when(users.selectNotificationUsers(List.of(USER))).thenReturn(List.of(user));
        var application = transactional(new NotificationApplicationUseCase(
            new NotificationApplicationRuntimeService(runtimeDao, users, dispatch, event -> {})),
            NotificationApplicationUseCase.class);
        notices = transactional(new NotifyNoticeUseCase(new NotifyNoticeService(noticeDao, users),
            new NotifyNoticePublisherService(application, noticeDao, users)), NotifyNoticeUseCase.class);
    }

    @AfterEach
    void close() {
        try {
            if (db != null) {
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

    private void insertDraft(long noticeId, String title) {
        db.update("insert into notify_notice(notice_id,notice_title,notice_type,notice_content,recipient_type,"
                + "recipient_ids_json,user_type_ids_json,channels_json,status,lifecycle,create_time) "
                + "values(?,?,'1',?,'USER','[7]','[]','[\"IN_APP\"]','1','DRAFT',utc_timestamp())",
            noticeId, title, "Owned synthetic notice body " + noticeId);
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
