package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.DsTxEventListenerFactory;
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
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.support.outbox.NotifyOutboxWakeRequestedEvent;
import org.namewta.notify.usecase.NotificationApplicationUseCase;
import org.namewta.notify.usecase.NotifyOutboxClaimUseCase;
import org.namewta.system.api.UserService;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/** Fresh owned MySQL/Redis、真实 Mapper 与动态事务中的人工重试边界。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.manual.retry.integration", matches = "true")
class NotifyManualRetryIntegrationTest {
    private static final AtomicLong IDS = new AtomicLong(9_380_000L);
    private final List<Long> intents = new ArrayList<>();
    private final List<Long> accountIds = new ArrayList<>();
    private final List<Long> bindingIds = new ArrayList<>();
    private NotifyAtomicResultIntegrationTest fixture;
    private JdbcTemplate db;
    private NotifyNotificationDao dao;
    private NotificationApplicationUseCase application;
    private NotifyOutboxClaimUseCase claims;
    private DispatchNotificationService dispatch;
    private AnnotationConfigApplicationContext wakeContext;
    private WakeCounter wakes;

    @BeforeEach
    void open() throws Exception {
        assertThat(System.getenv("T38_MYSQL_PASSWORD")).as("owned database password env").isNotBlank();
        assertThat(System.getProperty("notify.mysql.integration.url"))
            .matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/namewta_notify_test_[a-zA-Z0-9_]+.*");
        assertThat(System.getProperty("notify.mysql.integration.username")).isNotBlank();
        assertThat(System.getProperty("notify.redis.integration.port")).matches("[0-9]+");
        fixture = new NotifyAtomicResultIntegrationTest();
        fixture.open(); // 复用现有真实六 SQL / MyBatis / 事务 / Redis 装配；driver 给同一 owned 密码两个 env 名。
        db = field("db", JdbcTemplate.class);
        dao = field("dao", NotifyNotificationDao.class);
        claims = field("claims", NotifyOutboxClaimUseCase.class);
        dispatch = field("dispatch", DispatchNotificationService.class);
        wakeContext = new AnnotationConfigApplicationContext();
        wakeContext.registerBean(DsTxEventListenerFactory.class);
        wakeContext.registerBean(WakeCounter.class);
        wakeContext.refresh();
        wakes = wakeContext.getBean(WakeCounter.class);
        var runtime = new org.namewta.notify.service.runtime.NotificationApplicationRuntimeService(
            dao, mock(UserService.class), dispatch, wakeContext);
        application = transactional(new NotificationApplicationUseCase(runtime), NotificationApplicationUseCase.class);
    }

    @AfterEach
    void close() {
        try {
            if (db != null) {
                db.execute("drop trigger if exists owned_t38_delivery_failure");
                for (Long id : bindingIds) db.update("delete from notify_scene_binding where binding_id=?", id);
                for (Long id : accountIds) db.update("delete from notify_channel_account where account_id=?", id);
                for (Long id : intents) {
                    db.update("delete from notify_attempt where intent_id=?", id);
                    db.update("delete from notify_outbox where intent_id=?", id);
                    db.update("delete from notify_delivery where intent_id=?", id);
                    db.update("delete from notify_recipient where intent_id=?", id);
                    db.update("delete from notify_message_recipient where message_id=?", id);
                    db.update("delete from notify_message where message_id=?", id);
                    db.update("delete from notify_intent where intent_id=?", id);
                }
            }
        } finally {
            if (wakeContext != null) wakeContext.close();
            if (fixture != null) fixture.close();
        }
    }

    @Test
    void targetedFailedLocalPreflightOnlyQueuesItsDelivery() {
        long intent = intent("FAILED");
        Task a = task(intent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 1, 5);
        Task b = task(intent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 2, 5);

        var receipt = application.retry(command(intent, a.deliveryId()));
        assertThat(receipt.notificationId()).isEqualTo(String.valueOf(intent));
        assertThat(receipt.status().name()).isEqualTo("QUEUED");
        assertThat(receipt.queuedCount()).isEqualTo(1);
        assertTask(a, "PENDING", "READY", 1);
        assertTask(b, "FAILED", "DONE", 2);
        assertThat(wakes.count.get()).isEqualTo(1);

        var duplicate = application.retry(command(intent, a.deliveryId()));
        assertThat(duplicate.queuedCount()).isZero();
        assertThat(duplicate.status().name()).isEqualTo("QUEUED");
        assertThat(wakes.count.get()).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_outbox where delivery_id=?", Integer.class, a.deliveryId()))
            .isEqualTo(1);
        var claimed = claims.claim("owned-t38-worker");
        assertThat(claimed.stream().map(NotifyOutbox::getOutboxId)).containsExactly(a.outboxId());
        assertTask(a, "PENDING", "PROCESSING", 1);
        var activeToken = dao.outbox(a.outboxId()).getLeaseToken();
        assertThat(application.retry(command(intent, a.deliveryId())).queuedCount()).isZero();
        assertThat(dao.outbox(a.outboxId()).getLeaseToken()).isEqualTo(activeToken);
    }

    @Test
    void foreignDeliveryAndExternalUnknownAreRejectedWithoutWrites() {
        long ownIntent = intent("FAILED");
        Task own = task(ownIntent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 1, 5);
        long foreignIntent = intent("FAILED");
        Task foreign = task(foreignIntent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 1, 5);
        assertThatThrownBy(() -> application.retry(command(ownIntent, foreign.deliveryId())))
            .isInstanceOf(ServiceException.class);
        assertTask(own, "FAILED", "DONE", 1);
        assertTask(foreign, "FAILED", "DONE", 1);

        long uncertainIntent = intent("UNKNOWN");
        Task uncertain = task(uncertainIntent, "SMS", "UNKNOWN", "DISPATCH_ERROR", "WAITING_RECEIPT", 1, 5);
        assertThatThrownBy(() -> application.retry(command(uncertainIntent, uncertain.deliveryId())))
            .isInstanceOf(ServiceException.class);
        assertTask(uncertain, "UNKNOWN", "WAITING_RECEIPT", 1);
        assertThat(wakes.count.get()).isZero();
    }

    @Test
    void batchRetryRejectsExternalUnknownBeforeChangingSafeSibling() {
        long intent = intent("UNKNOWN");
        Task safe = task(intent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 1, 5);
        Task uncertain = task(intent, "SMS", "UNKNOWN", "DISPATCH_ERROR", "WAITING_RECEIPT", 1, 5);

        assertThatThrownBy(() -> application.retry(new NotificationRetryCommand(
            String.valueOf(intent), null, "owned batch", null))).isInstanceOf(ServiceException.class);
        assertTask(safe, "FAILED", "DONE", 1);
        assertTask(uncertain, "UNKNOWN", "WAITING_RECEIPT", 1);
        assertThat(dao.intent(intent).getStatus()).isEqualTo("UNKNOWN");
        assertThat(wakes.count.get()).isZero();
        assertThat(application.retry(command(intent, safe.deliveryId())).queuedCount()).isEqualTo(1);
        assertTask(safe, "PENDING", "READY", 1);
        assertTask(uncertain, "UNKNOWN", "WAITING_RECEIPT", 1);
        assertThat(wakes.count.get()).isEqualTo(1);
    }

    @Test
    void exhaustedBudgetAndMultipleOutboxesDoNotCreateAnotherTask() {
        long exhaustedIntent = intent("FAILED");
        Task exhausted = task(exhaustedIntent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 5, 5);
        assertThat(application.retry(command(exhaustedIntent, exhausted.deliveryId())).queuedCount()).isZero();
        assertTask(exhausted, "FAILED", "DONE", 5);

        long ambiguousIntent = intent("FAILED");
        Task ambiguous = task(ambiguousIntent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 1, 5);
        long activeId = IDS.incrementAndGet();
        db.update("insert into notify_outbox(outbox_id,intent_id,delivery_id,status,available_at,attempt_count,max_attempts,"
            + "lease_owner,lease_token,lease_until,create_time) values(?,?,?,'PROCESSING',utc_timestamp(),1,5,"
            + "'owned-worker','owned-fence',timestampadd(second,60,utc_timestamp()),utc_timestamp())",
            activeId, ambiguousIntent, ambiguous.deliveryId());
        assertThat(application.retry(command(ambiguousIntent, ambiguous.deliveryId())).queuedCount()).isZero();
        assertTask(ambiguous, "FAILED", "DONE", 1);
        assertThat(db.queryForObject("select count(*) from notify_outbox where delivery_id=?", Integer.class,
            ambiguous.deliveryId())).isEqualTo(2);
        assertThat(db.queryForObject("select lease_token from notify_outbox where outbox_id=?", String.class,
            activeId)).isEqualTo("owned-fence");
        assertThat(wakes.count.get()).isZero();
    }

    @Test
    void completedCancelledAndUnsupportedFailureNeverCreateWork() {
        long completeIntent = intent("DELIVERED");
        Task complete = task(completeIntent, "MAIL", "DELIVERED", null, "DONE", 1, 5);
        var zero = application.retry(command(completeIntent, complete.deliveryId()));
        assertThat(zero.status().name()).isEqualTo("DELIVERED");
        assertThat(zero.queuedCount()).isZero();
        long cancelledIntent = intent("CANCELLED");
        Task cancelled = task(cancelledIntent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 1, 5);
        assertThat(application.retry(command(cancelledIntent, cancelled.deliveryId())).queuedCount()).isZero();
        assertThat(dao.intent(cancelledIntent).getStatus()).isEqualTo("CANCELLED");
        assertTask(cancelled, "FAILED", "DONE", 1);
        long oldFailureIntent = intent("FAILED");
        Task oldFailure = task(oldFailureIntent, "MAIL", "FAILED", "PROVIDER_ERROR", "DONE", 1, 5);
        assertThat(application.retry(command(oldFailureIntent, oldFailure.deliveryId())).queuedCount()).isZero();
        assertTask(oldFailure, "FAILED", "DONE", 1);
        assertThat(wakes.count.get()).isZero();
    }

    @Test
    void retryUsesRemainingBudgetAndRollsBackIfDeliveryUpdateFails() {
        long intent = intent("FAILED");
        Task a = task(intent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 4, 5);
        db.execute("create trigger owned_t38_delivery_failure before update on notify_delivery for each row "
            + "signal sqlstate '45000' set message_text='owned retry delivery failure'");
        assertThatThrownBy(() -> application.retry(command(intent, a.deliveryId()))).isInstanceOf(RuntimeException.class);
        assertTask(a, "FAILED", "DONE", 4);
        assertThat(dao.intent(intent).getStatus()).isEqualTo("FAILED");
        assertThat(wakes.count.get()).isZero();
        db.execute("drop trigger owned_t38_delivery_failure");
        assertThat(application.retry(command(intent, a.deliveryId())).queuedCount()).isEqualTo(1);
        assertTask(a, "PENDING", "READY", 4);
    }

    @Test
    void concurrentRetrySerializesOnIntentAndKeepsOneOutbox() throws Exception {
        long intent = intent("FAILED");
        Task task = task(intent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 1, 5);
        var observed = spy(dao);
        var firstOwnsIntent = new CountDownLatch(1);
        var secondEnteredLock = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var arrivals = new AtomicInteger();
        doAnswer(invocation -> {
            int number = arrivals.incrementAndGet();
            if (number == 2) secondEnteredLock.countDown();
            var locked = invocation.callRealMethod();
            if (number == 1) {
                firstOwnsIntent.countDown();
                assertThat(releaseFirst.await(5, TimeUnit.SECONDS)).isTrue();
            }
            return locked;
        }).when(observed).lockIntent(eq(intent));
        var racing = transactional(new NotificationApplicationUseCase(
            new org.namewta.notify.service.runtime.NotificationApplicationRuntimeService(
                observed, mock(UserService.class), dispatch, wakeContext)), NotificationApplicationUseCase.class);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> racing.retry(command(intent, task.deliveryId())));
            assertThat(firstOwnsIntent.await(5, TimeUnit.SECONDS)).isTrue();
            var second = workers.submit(() -> racing.retry(command(intent, task.deliveryId())));
            assertThat(secondEnteredLock.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(first.isDone()).isFalse();
            releaseFirst.countDown();
            int queued = first.get(10, TimeUnit.SECONDS).queuedCount() + second.get(10, TimeUnit.SECONDS).queuedCount();
            assertThat(queued).isEqualTo(1);
        } finally {
            releaseFirst.countDown();
        }
        assertTask(task, "PENDING", "READY", 1);
        assertThat(db.queryForObject("select count(*) from notify_outbox where delivery_id=?", Integer.class, task.deliveryId()))
            .isEqualTo(1);
        assertThat(wakes.count.get()).isEqualTo(1);
    }

    @Test
    void claimDuringRetryCommitCannotTakeUncommittedTaskOrStealNewLease() throws Exception {
        long intent = intent("FAILED");
        Task task = task(intent, "MAIL", "FAILED", "UNBOUND_CHANNEL", "DONE", 1, 5);
        var observed = spy(dao);
        var requeuedWithinTransaction = new CountDownLatch(1);
        var releaseCommit = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object updated = invocation.callRealMethod();
            requeuedWithinTransaction.countDown();
            assertThat(releaseCommit.await(5, TimeUnit.SECONDS)).isTrue();
            return updated;
        }).when(observed).requeueOutbox(any(NotifyOutbox.class), any(java.time.LocalDateTime.class));
        var racing = transactional(new NotificationApplicationUseCase(
            new org.namewta.notify.service.runtime.NotificationApplicationRuntimeService(
                observed, mock(UserService.class), dispatch, wakeContext)), NotificationApplicationUseCase.class);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var retry = workers.submit(() -> racing.retry(command(intent, task.deliveryId())));
            assertThat(requeuedWithinTransaction.await(5, TimeUnit.SECONDS)).isTrue();
            var prematureClaim = workers.submit(() -> claims.claim("owned-t38-premature"));
            assertThat(prematureClaim.get(5, TimeUnit.SECONDS)).isEmpty();
            releaseCommit.countDown();
            assertThat(retry.get(10, TimeUnit.SECONDS).queuedCount()).isEqualTo(1);
        } finally {
            releaseCommit.countDown();
        }
        var claimed = claims.claim("owned-t38-after-commit");
        assertThat(claimed.stream().map(NotifyOutbox::getOutboxId)).containsExactly(task.outboxId());
        String token = dao.outbox(task.outboxId()).getLeaseToken();
        assertThat(application.retry(command(intent, task.deliveryId())).queuedCount()).isZero();
        assertThat(dao.outbox(task.outboxId()).getLeaseToken()).isEqualTo(token);
        assertThat(db.queryForObject("select count(*) from notify_outbox where delivery_id=?", Integer.class,
            task.deliveryId())).isEqualTo(1);
    }

    @Test
    void inAppUnknownReplaysExistingMessageWithoutAnotherRelationOrPush() {
        long intent = intent("UNKNOWN");
        Task task = task(intent, "IN_APP", "UNKNOWN", "DISPATCH_ERROR", "WAITING_RECEIPT", 1, 5);
        db.update("insert into notify_message(message_id,category,notice_type,channels_json,type,source,title,message,content,create_time) "
            + "values(?,'system','', '[\"IN_APP\"]','MESSAGE','BACKEND','Owned title','Owned content','Owned content',utc_timestamp())", intent);
        db.update("insert into notify_message_recipient(message_recipient_id,message_id,user_id,create_time) "
            + "values(?,?,7,utc_timestamp())", IDS.incrementAndGet(), intent);

        assertThat(application.retry(command(intent, task.deliveryId())).queuedCount()).isEqualTo(1);
        assertTask(task, "PENDING", "READY", 1);
        var claimed = claims.claim("owned-t38-in-app");
        assertThat(claimed.stream().map(NotifyOutbox::getOutboxId)).containsExactly(task.outboxId());
        dispatch.dispatch(claimed.getFirst());
        assertTask(task, "DELIVERED", "DONE", 2);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, intent)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=? and user_id=7", Integer.class, intent)).isEqualTo(1);
        assertThat(field("realtimeCalls", AtomicInteger.class).get()).isZero();
    }

    @Test
    void inAppUnknownAddsMissingRelationAndPushesOnlyOnce() {
        long intent = intent("UNKNOWN");
        Task task = task(intent, "IN_APP", "UNKNOWN", "DISPATCH_ERROR", "WAITING_RECEIPT", 1, 5);
        db.update("insert into notify_message(message_id,category,notice_type,channels_json,type,source,title,message,content,create_time) "
            + "values(?,'system','', '[\"IN_APP\"]','MESSAGE','BACKEND','Owned title','Owned content','Owned content',utc_timestamp())", intent);

        assertThat(application.retry(command(intent, task.deliveryId())).queuedCount()).isEqualTo(1);
        var claimed = claims.claim("owned-t38-in-app-new-relation");
        assertThat(claimed.stream().map(NotifyOutbox::getOutboxId)).containsExactly(task.outboxId());
        dispatch.dispatch(claimed.getFirst());
        assertTask(task, "DELIVERED", "DONE", 2);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, intent)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=? and user_id=7", Integer.class, intent)).isEqualTo(1);
        assertThat(field("realtimeCalls", AtomicInteger.class).get()).isEqualTo(1);
        assertThat(application.retry(command(intent, task.deliveryId())).queuedCount()).isZero();
        dispatch.dispatch(claimed.getFirst());
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=? and user_id=7", Integer.class, intent)).isEqualTo(1);
        assertThat(field("realtimeCalls", AtomicInteger.class).get()).isEqualTo(1);
    }

    @Test
    void inAppUnknownRejectsOrphanRelationWithoutChangingTask() {
        long intent = intent("UNKNOWN");
        Task task = task(intent, "IN_APP", "UNKNOWN", "DISPATCH_ERROR", "WAITING_RECEIPT", 1, 5);
        db.update("insert into notify_message_recipient(message_recipient_id,message_id,user_id,create_time) "
            + "values(?,?,7,utc_timestamp())", IDS.incrementAndGet(), intent);

        assertThat(application.retry(command(intent, task.deliveryId())).queuedCount()).isZero();
        assertTask(task, "UNKNOWN", "WAITING_RECEIPT", 1);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, intent)).isZero();
        assertThat(wakes.count.get()).isZero();
        assertThat(field("realtimeCalls", AtomicInteger.class).get()).isZero();
    }

    @Test
    void inAppUnknownWithoutMessageCreatesOneAtomicMessageAndRelation() {
        long intent = intent("UNKNOWN");
        Task task = task(intent, "IN_APP", "UNKNOWN", "DISPATCH_ERROR", "WAITING_RECEIPT", 1, 5);
        assertThat(application.retry(command(intent, task.deliveryId())).queuedCount()).isEqualTo(1);
        var claimed = claims.claim("owned-t38-in-app-new-message");
        assertThat(claimed.stream().map(NotifyOutbox::getOutboxId)).containsExactly(task.outboxId());
        dispatch.dispatch(claimed.getFirst());
        assertTask(task, "DELIVERED", "DONE", 2);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, intent)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=? and user_id=7", Integer.class, intent)).isEqualTo(1);
        assertThat(field("realtimeCalls", AtomicInteger.class).get()).isEqualTo(1);
        dispatch.dispatch(claimed.getFirst());
        assertThat(field("realtimeCalls", AtomicInteger.class).get()).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=? and user_id=7", Integer.class, intent)).isEqualTo(1);
    }

    @Test
    void actualPlannerFailureCanBeRepairedAndThenCallsControlledSenderOnce() {
        long intent = intent("QUEUED");
        Task task = task(intent, "MAIL", "PENDING", null, "READY", 0, 5);
        AtomicInteger sends = new AtomicInteger();
        NotifyClient sender = mock(NotifyClient.class);
        when(sender.send(any())).thenAnswer(invocation -> {
            var request = invocation.getArgument(0, org.namewta.common.notify.model.NotifyRequest.class);
            sends.incrementAndGet();
            return new NotifyResult(request.requestId(), request.channel(), "owned-t38-mail", NotifyStatus.ACCEPTED,
                List.of(NotifyTargetResult.accepted(request.targets().getFirst(), "owned-accepted", 1)));
        });
        DispatchNotificationService actualDispatch = new DispatchNotificationService(dao, sender,
            mock(org.springframework.beans.factory.ObjectProvider.class), field("configDao", NotifyConfigDao.class),
            (key, limit, duration) -> true, field("results", NotifyDispatchResultPort.class));

        var first = claims.claim("owned-t38-before-repair");
        assertThat(first.stream().map(NotifyOutbox::getOutboxId)).containsExactly(task.outboxId());
        actualDispatch.dispatch(first.getFirst());
        assertTask(task, "FAILED", "DONE", 1);
        assertThat(dao.delivery(task.deliveryId()).getErrorCode()).isEqualTo("UNBOUND_CHANNEL");
        assertThat(sends.get()).isZero();

        long accountId = IDS.incrementAndGet(); accountIds.add(accountId);
        long bindingId = IDS.incrementAndGet(); bindingIds.add(bindingId);
        db.update("insert into notify_channel_account(account_id,channel,config_key,enabled,minute_max,create_time) "
            + "values(?,'MAIL','owned-t38-mail','Y',60,utc_timestamp())", accountId);
        db.update("insert into notify_scene_binding(binding_id,scene_code,channel,account_id,mail_subject,mail_body,"
            + "create_time) values(?,'owned','MAIL',?,'Owned title','Owned content',utc_timestamp())", bindingId, accountId);

        assertThat(application.retry(command(intent, task.deliveryId())).queuedCount()).isEqualTo(1);
        assertTask(task, "PENDING", "READY", 1);
        var second = claims.claim("owned-t38-after-repair");
        assertThat(second.stream().map(NotifyOutbox::getOutboxId)).containsExactly(task.outboxId());
        actualDispatch.dispatch(second.getFirst());
        assertTask(task, "ACCEPTED", "DONE", 2);
        assertThat(sends.get()).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_attempt where delivery_id=?", Integer.class,
            task.deliveryId())).isEqualTo(2);
    }

    private long intent(String status) {
        long id = IDS.incrementAndGet(); intents.add(id);
        db.update("insert into notify_intent(intent_id,app_id,scene_code,template_code,template_params_json,strategy,mode,status,"
            + "title_snapshot,content_snapshot,create_time) values(?,'owned-t38','owned','owned','{}','ALL','ASYNC',"
            + "?,'Owned title','Owned content',utc_timestamp())", id, status);
        return id;
    }

    private Task task(long intentId, String channel, String status, String code, String outboxStatus, int count, int max) {
        long recipientId = IDS.incrementAndGet();
        long deliveryId = IDS.incrementAndGet();
        long outboxId = IDS.incrementAndGet();
        db.update("insert into notify_recipient(recipient_id,intent_id,recipient_type,recipient_key,user_id,status,create_time) "
            + "values(?,?,'USER',?,7,'ACTIVE',utc_timestamp())", recipientId, intentId, String.valueOf(recipientId));
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,target_value,status,"
            + "attempt_count,error_code,create_time) values(?,?,?,7,?,?,?,?,?,utc_timestamp())",
            deliveryId, intentId, recipientId, channel, "IN_APP".equals(channel) ? "7" : "synthetic@example.test", status, count, code);
        db.update("insert into notify_outbox(outbox_id,intent_id,delivery_id,status,available_at,attempt_count,max_attempts,"
            + "last_error_code,create_time) values(?,?,?, ?,utc_timestamp(),?,?,?,utc_timestamp())",
            outboxId, intentId, deliveryId, outboxStatus, count, max, code);
        return new Task(deliveryId, outboxId);
    }

    private void assertTask(Task task, String deliveryStatus, String outboxStatus, int attempts) {
        assertThat(dao.delivery(task.deliveryId()).getStatus()).isEqualTo(deliveryStatus);
        var outbox = dao.outbox(task.outboxId());
        assertThat(outbox.getStatus()).isEqualTo(outboxStatus);
        assertThat(outbox.getAttemptCount()).isEqualTo(attempts);
    }

    private NotificationRetryCommand command(long intent, long delivery) {
        return new NotificationRetryCommand(String.valueOf(intent), String.valueOf(delivery), "owned manual", null);
    }

    @SuppressWarnings("unchecked")
    private <T> T field(String name, Class<T> type) {
        return (T) type.cast(ReflectionTestUtils.getField(fixture, name));
    }

    private static <T> T transactional(T target, Class<T> type) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return type.cast(proxy.getProxy());
    }

    private record Task(long deliveryId, long outboxId) { }

    public static class WakeCounter {
        final AtomicInteger count = new AtomicInteger();
        @DsTxEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void afterCommit(NotifyOutboxWakeRequestedEvent ignored) { count.incrementAndGet(); }
    }
}
