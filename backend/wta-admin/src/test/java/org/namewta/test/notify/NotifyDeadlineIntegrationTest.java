package org.namewta.test.notify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.model.NotifyResult;
import org.namewta.common.notify.model.NotifyStatus;
import org.namewta.common.notify.model.NotifyTargetResult;
import org.namewta.notify.api.NotificationRetryCommand;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.namewta.notify.usecase.NotifyOutboxClaimUseCase;
import org.namewta.system.api.UserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** owned 六 SQL + 真实 claim/事务结果，不使用真实供应商。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.deadline.integration", matches = "true")
class NotifyDeadlineIntegrationTest {
    private static final AtomicLong IDS = new AtomicLong(9_390_000L);
    private final List<Long> ownedIntents = new ArrayList<>();
    private final List<Long> ownedAccounts = new ArrayList<>();
    private final List<Long> ownedBindings = new ArrayList<>();
    private NotifyAtomicResultIntegrationTest fixture;
    private JdbcTemplate db;
    private NotifyNotificationDao dao;
    private NotifyOutboxClaimUseCase claims;
    private DispatchNotificationService dispatch;

    @BeforeEach
    void open() throws Exception {
        assertThat(System.getenv("T39_MYSQL_PASSWORD")).as("owned private MySQL credential").isNotBlank();
        assertThat(System.getProperty("notify.mysql.integration.url"))
            .matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/namewta_notify_test_[a-zA-Z0-9_]+.*");
        fixture = new NotifyAtomicResultIntegrationTest();
        fixture.open(); // 与 T36 共用实际六 SQL / Mapper / 动态事务装配；driver 给同一 owned 密码两个 env 名。
        db = field("db", JdbcTemplate.class);
        dao = field("dao", NotifyNotificationDao.class);
        claims = field("claims", NotifyOutboxClaimUseCase.class);
        dispatch = field("dispatch", DispatchNotificationService.class);
    }

    @AfterEach
    void close() {
        try {
            if (db != null) {
                db.execute("drop trigger if exists owned_t39_intent_failure");
                for (Long id : ownedBindings) db.update("delete from notify_scene_binding where binding_id=?", id);
                for (Long id : ownedAccounts) db.update("delete from notify_channel_account where account_id=?", id);
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

    @Test
    void expiredNewReadyIsFailedDoneWithoutProviderOrAttempt() {
        Task task = task("MAIL", "READY", NotifyOutbox.DEADLINE_UNSENT_READY, -1, 0);
        var claimed = claims.claim("owned-t39-ready");
        assertThat(claimed).extracting(NotifyOutbox::getOutboxId).contains(task.outboxId());
        assertThat(claimed.stream().filter(row -> row.getOutboxId().equals(task.outboxId())).findFirst().orElseThrow()
            .getClaimedFromReady()).isTrue();
        dispatch.dispatch(claimed.stream().filter(row -> row.getOutboxId().equals(task.outboxId())).findFirst().orElseThrow());
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("FAILED");
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("NOTIFICATION_EXPIRED");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, task.intentId())).isZero();
        verify(field("notifyClient", NotifyClient.class), never()).send(any());
    }

    @Test
    void historicalReadyWithoutProofBecomesUnknownWithoutProvider() {
        Task task = task("MAIL", "READY", null, -1, 0);
        dispatch.dispatch(claim(task, "owned-t39-historical"));
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("UNKNOWN");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("WAITING_RECEIPT");
        verify(field("notifyClient", NotifyClient.class), never()).send(any());
    }

    @Test
    void reclaimedExternalProcessingBeforeDeadlineNeverReturnsReadyOrResends() {
        Task task = task("MAIL", "PROCESSING", NotifyOutbox.DEADLINE_UNSENT_READY, 60, 0);
        db.update("update notify_outbox set lease_owner='old',lease_token='old',lease_until="
            + "timestampadd(second,-1,utc_timestamp()) where outbox_id=?", task.outboxId());
        NotifyOutbox reclaimed = claim(task, "owned-t39-reclaimed");
        assertThat(reclaimed.getClaimedFromReady()).isFalse();
        dispatch.dispatch(reclaimed);
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("UNKNOWN");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("WAITING_RECEIPT");
        verify(field("notifyClient", NotifyClient.class), never()).send(any());
    }

    @Test
    void futureAvailableAtIsNotClaimedEarly() {
        Task task = task("MAIL", "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 90, 0);
        db.update("update notify_outbox set available_at=timestampadd(second,60,utc_timestamp()),"
            + "next_attempt_at=timestampadd(second,60,utc_timestamp()) where outbox_id=?", task.outboxId());
        assertThat(claims.claim("owned-t39-future")).noneMatch(row -> row.getOutboxId().equals(task.outboxId()));
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("READY");
    }

    @Test
    void expiredInAppWithDurableRecipientRecoversDeliveredWithoutNewPush() {
        Task task = task("IN_APP", "READY", null, -1, 0);
        db.update("insert into notify_message(message_id,category,notice_type,channels_json,type,source,title,message,content,create_time) "
            + "values(?,'system','','[\"IN_APP\"]','MESSAGE','BACKEND','Owned title','Owned content','Owned content',utc_timestamp())",
            task.intentId());
        db.update("insert into notify_message_recipient(message_recipient_id,message_id,user_id,create_time) "
            + "values(?,?,7,utc_timestamp())", IDS.incrementAndGet(), task.intentId());
        dispatch.dispatch(claim(task, "owned-t39-inapp"));
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class,
            task.intentId())).isEqualTo(1);
        assertThat(field("realtimeCalls", java.util.concurrent.atomic.AtomicInteger.class).get()).isZero();
    }

    @Test
    void expiredInAppWithoutFactsStopsBeforeBudgetAndMessage() {
        Task task = task("IN_APP", "READY", null, -1, 0);
        dispatch.dispatch(claim(task, "owned-t39-inapp-empty"));
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("FAILED");
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("NOTIFICATION_EXPIRED");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        assertThat(dao.outbox(task.outboxId()).getAttemptCount()).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, task.intentId())).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class,
            task.intentId())).isZero();
        assertThat(field("realtimeCalls", java.util.concurrent.atomic.AtomicInteger.class).get()).isZero();
    }

    @Test
    void expiredInAppOrphanRelationIsLocalFailureNotExternalWait() {
        Task task = task("IN_APP", "READY", null, -1, 0);
        db.update("insert into notify_message_recipient(message_recipient_id,message_id,user_id,create_time) "
            + "values(?,?,7,utc_timestamp())", IDS.incrementAndGet(), task.intentId());
        dispatch.dispatch(claim(task, "owned-t39-inapp-orphan"));
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("FAILED");
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("IN_APP_FACTS_INCONSISTENT");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class,
            task.intentId())).isEqualTo(1);
    }

    @Test
    void waitSettlementRechecksCurrentDeadlineUnderLease() {
        Task task = task("MAIL", "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 60, 0);
        NotifyOutbox lease = claim(task, "owned-t39-wait");
        db.update("update notify_intent set expires_at=timestampadd(second,-1,utc_timestamp()) where intent_id=?", task.intentId());
        field("results", org.namewta.notify.port.NotifyDispatchResultPort.class)
            .settle(lease, org.namewta.notify.port.NotifyDispatchResultPort.Disposition.WAIT);
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("NOTIFICATION_EXPIRED");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
    }

    @Test
    void oldLeaseCannotTerminalizeAfterTokenChanges() {
        Task task = task("MAIL", "READY", NotifyOutbox.DEADLINE_UNSENT_READY, -1, 0);
        NotifyOutbox old = claim(task, "owned-t39-old");
        db.update("update notify_outbox set lease_owner='new-owner',lease_token='new-token' where outbox_id=?", task.outboxId());
        dispatch.dispatch(old);
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("PENDING");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void cancelledIntentCannotBeRevivedByAClaimedTask() {
        Task task = task("MAIL", "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 60, 0);
        db.update("update notify_intent set status='CANCELLED' where intent_id=?", task.intentId());
        dispatch.dispatch(claim(task, "owned-t39-cancelled"));
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("CANCELLED");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        assertThat(dao.intent(task.intentId()).getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void submitRoundsScheduleUpAndExpiryDownWithoutEarlyClaim() {
        Instant base = dao.databaseNow().toInstant(ZoneOffset.UTC);
        Instant schedule = base.plusSeconds(80).plusNanos(123_000_000);
        Instant expiry = base.plusSeconds(100).plusNanos(987_000_000);
        var runtime = new NotificationApplicationRuntimeService(dao, mock(UserService.class), dispatch, event -> {});
        var command = new NotificationCommand("owned-t39", "owned", "owned", "owned-schedule", "EMAIL",
            List.of("synthetic@example.test"), "owned", Map.of("code", "synthetic"), List.of(NotificationChannel.MAIL),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0, schedule, expiry,
            "owned-t39-" + IDS.incrementAndGet(), Map.of(), java.util.List.of());
        var receipt = runtime.submit(command);
        long id = Long.parseLong(receipt.notificationId()); ownedIntents.add(id);
        assertThat(dao.intent(id).getScheduledAt()).isEqualTo(schedule.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1)
            .atOffset(ZoneOffset.UTC).toLocalDateTime());
        assertThat(dao.intent(id).getExpiresAt()).isEqualTo(expiry.truncatedTo(ChronoUnit.SECONDS)
            .atOffset(ZoneOffset.UTC).toLocalDateTime());
        assertThat(dao.outbox(db.queryForObject("select outbox_id from notify_outbox where intent_id=?", Long.class, id))
            .getAvailableAt()).isEqualTo(dao.intent(id).getScheduledAt());
        assertThat(claims.claim("owned-t39-too-early")).noneMatch(row -> row.getIntentId().equals(id));
    }

    @Test
    void duplicateSubmissionCannotExtendPersistedDeadline() {
        Instant base = dao.databaseNow().toInstant(ZoneOffset.UTC);
        String key = "owned-t39-duplicate-" + IDS.incrementAndGet();
        var runtime = new NotificationApplicationRuntimeService(dao, mock(UserService.class), dispatch, event -> {});
        var original = new NotificationCommand("owned-t39", "owned", "owned", "owned-duplicate", "EMAIL",
            List.of("synthetic@example.test"), "owned", Map.of(), List.of(NotificationChannel.MAIL),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0, null, base.plusSeconds(80), key, Map.of(), java.util.List.of());
        long id = Long.parseLong(runtime.submit(original).notificationId()); ownedIntents.add(id);
        db.update("update notify_intent set expires_at=timestampadd(second,-1,utc_timestamp()) where intent_id=?", id);
        var attemptedExtension = new NotificationCommand("owned-t39", "owned", "owned", "owned-duplicate", "EMAIL",
            List.of("synthetic@example.test"), "owned", Map.of(), List.of(NotificationChannel.MAIL),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0, null, base.plusSeconds(300), key, Map.of(), java.util.List.of());
        assertThatThrownBy(() -> runtime.submit(attemptedExtension)).isInstanceOf(ServiceException.class);
        assertThat(db.queryForObject("select count(*) from notify_outbox where intent_id=?", Integer.class, id)).isEqualTo(1);
        assertThat(dao.intent(id).getExpiresAt()).isBefore(dao.databaseNow());
    }

    @Test
    void providerAcceptedAfterDeadlineStillPersistsActualAcceptedResult() {
        Task task = task("MAIL", "READY", NotifyOutbox.DEADLINE_UNSENT_READY, 90, 0);
        long accountId = IDS.incrementAndGet(); ownedAccounts.add(accountId);
        long bindingId = IDS.incrementAndGet(); ownedBindings.add(bindingId);
        db.update("insert into notify_channel_account(account_id,channel,config_key,enabled,minute_max,create_time) "
            + "values(?,'MAIL','owned-t39-mail','Y',60,utc_timestamp())", accountId);
        db.update("insert into notify_scene_binding(binding_id,scene_code,channel,account_id,mail_subject,mail_body,create_time) "
            + "values(?,'owned','MAIL',?,'Owned title','Owned body',utc_timestamp())", bindingId, accountId);
        NotifyClient sender = mock(NotifyClient.class);
        when(sender.send(any())).thenAnswer(call -> {
            var request = call.getArgument(0, org.namewta.common.notify.model.NotifyRequest.class);
            db.update("update notify_intent set expires_at=timestampadd(second,-1,utc_timestamp()) where intent_id=?",
                task.intentId());
            return new NotifyResult(request.requestId(), request.channel(), "owned-t39-mail", NotifyStatus.ACCEPTED,
                List.of(NotifyTargetResult.accepted(request.targets().getFirst(), "owned-receipt", 1)));
        });
        var actual = new DispatchNotificationService(dao, sender,
            mock(org.springframework.beans.factory.ObjectProvider.class), field("configDao", NotifyConfigDao.class),
            (key, limit, window) -> true, field("results", org.namewta.notify.port.NotifyDispatchResultPort.class));

        actual.dispatch(claim(task, "owned-t39-post-io"));
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("ACCEPTED");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("DONE");
        verify(sender, times(1)).send(any());
    }

    @Test
    void expirationResultRollbackLeavesDeliveryAndLeaseIntact() {
        Task task = task("MAIL", "READY", NotifyOutbox.DEADLINE_UNSENT_READY, -1, 0);
        db.execute("create trigger owned_t39_intent_failure before update on notify_intent for each row "
            + "signal sqlstate '45000' set message_text='owned deadline write failure'");
        NotifyOutbox claimed = claim(task, "owned-t39-rollback");
        assertThatThrownBy(() -> dispatch.dispatch(claimed)).isInstanceOf(RuntimeException.class);
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo("PENDING");
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("PROCESSING");
        assertThat(dao.outbox(task.outboxId()).getLeaseToken()).isEqualTo(claimed.getLeaseToken());
        assertThat(dao.intent(task.intentId()).getStatus()).isEqualTo("QUEUED");
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class,
            task.intentId())).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class,
            task.intentId())).isZero();
    }

    @Test
    void expiredPersistedIntentRejectsManualRetryWithoutRequeue() {
        Task task = task("MAIL", "READY", NotifyOutbox.DEADLINE_UNSENT_READY, -1, 0);
        var wakeEvents = new java.util.concurrent.atomic.AtomicInteger();
        var runtime = new NotificationApplicationRuntimeService(dao, mock(UserService.class), dispatch,
            event -> wakeEvents.incrementAndGet());
        assertThatThrownBy(() -> runtime.retry(new NotificationRetryCommand(String.valueOf(task.intentId()),
            String.valueOf(task.deliveryId()), "owned retry", null))).isInstanceOf(ServiceException.class);
        assertThat(dao.outbox(task.outboxId()).getStatus()).isEqualTo("READY");
        assertThat(wakeEvents.get()).isZero();
    }

    private Task task(String channel, String outboxStatus, String code, int expiryOffsetSeconds, int attemptCount) {
        long intentId = IDS.incrementAndGet(); ownedIntents.add(intentId);
        long recipientId = IDS.incrementAndGet();
        long deliveryId = IDS.incrementAndGet();
        long outboxId = IDS.incrementAndGet();
        db.update("insert into notify_intent(intent_id,app_id,scene_code,template_code,template_params_json,strategy,mode,status,"
            + "expires_at,title_snapshot,content_snapshot,create_time) values(?,'owned-t39','owned','owned','{}','ALL','ASYNC',"
            + "'QUEUED',timestampadd(second,?,utc_timestamp()),'Owned title','Owned content',utc_timestamp())",
            intentId, expiryOffsetSeconds);
        db.update("insert into notify_recipient(recipient_id,intent_id,recipient_type,recipient_key,user_id,status,create_time) "
            + "values(?,?,'USER',?,7,'ACTIVE',utc_timestamp())", recipientId, intentId, String.valueOf(recipientId));
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,target_value,status,"
            + "attempt_count,create_time) values(?,?,?,7,?,?,'PENDING',?,utc_timestamp())",
            deliveryId, intentId, recipientId, channel, "IN_APP".equals(channel) ? "7" : "synthetic@example.test", attemptCount);
        db.update("insert into notify_outbox(outbox_id,intent_id,delivery_id,status,available_at,next_attempt_at,"
            + "attempt_count,max_attempts,last_error_code,create_time) values(?,?,?, ?,utc_timestamp(),utc_timestamp(),"
            + "?,5,?,utc_timestamp())", outboxId, intentId, deliveryId, outboxStatus, attemptCount, code);
        return new Task(intentId, deliveryId, outboxId);
    }

    private NotifyOutbox claim(Task task, String owner) {
        return claims.claim(owner).stream().filter(row -> row.getOutboxId().equals(task.outboxId()))
            .findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private <T> T field(String name, Class<T> type) {
        Object target = "notifyClient".equals(name) ? dispatch : fixture;
        return (T) type.cast(ReflectionTestUtils.getField(target, name));
    }

    private record Task(long intentId, long deliveryId, long outboxId) { }
}
