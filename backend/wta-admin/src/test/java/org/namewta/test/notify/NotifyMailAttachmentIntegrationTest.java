package org.namewta.test.notify;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff;
import cn.dev33.satoken.context.model.SaTokenContextModelBox;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.namewta.NamewtaApplication;
import org.namewta.common.mail.notify.MailNotificationMessage;
import org.namewta.common.mail.notify.MailNotificationSender;
import org.namewta.common.mail.notify.MailNotifyChannelAdapter;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.notify.attachment.NotifyAttachmentSnapshotService;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.model.NotifyContext;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.notify.adapter.worker.NotifyOutboxWorker;
import org.namewta.notify.adapter.worker.NotifyOutboxWakeSubscriber;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationCancelCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationRetryCommand;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyIntentAttachment;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.mapper.NotifyIntentAttachmentMapper;
import org.namewta.notify.port.NotifyDispatchPort;
import org.namewta.notify.port.NotifyOutboxClaimPort;
import org.namewta.notify.service.runtime.NotifyAttachmentSnapshotTransactions;
import org.namewta.system.api.OssService;
import org.namewta.system.api.model.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 独占 MySQL/Redis/MinIO 的完整应用验收。只有最终 SMTP 物理发送器是捕获替身，
 * 提交、身份、快照、OSS、NotifyClient、Mail adapter 和 Worker 均取生产 Bean。
 * 本独占上下文关闭 Redis Wake 订阅，以手动 poll 控制领取时序；跨进程唤醒由独立回归验证。
 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.mail.attachment.integration", matches = "true")
@SpringBootTest(classes = NamewtaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(NotifyMailAttachmentIntegrationTest.CaptureMailConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotifyMailAttachmentIntegrationTest {
    private static final long USER = 1761100000000000001L;
    private static final long CLIENT = 1762000000000000001L;
    private static final long ACCOUNT = 9_642_000_001L;
    private static final long BINDING = 2100630000000000011L;
    private final List<Long> ownedIntents = new CopyOnWriteArrayList<>();
    private final List<Long> ownedSources = new ArrayList<>();
    private final List<OwnedObject> ownedKeys = new ArrayList<>();
    private final HttpClient controlClient = HttpClient.newHttpClient();
    private volatile boolean workerFixturePoisoned;

    @Autowired private JdbcTemplate db;
    @Autowired private NotificationApplicationService notifications;
    @Autowired private NotifyOutboxWorker worker;
    @Autowired private NotifyOutboxWakeSubscriber wakeSubscriber;
    @Autowired private NotifyNotificationDao dao;
    @Autowired private NotifyIntentAttachmentMapper attachmentMapper;
    @Autowired private NotifyOutboxClaimPort claimPort;
    @Autowired private NotifyDispatchPort dispatchPort;
    @Autowired private NotifyClient notifyClient;
    @Autowired private NotifyAttachmentSnapshotService snapshotService;
    @Autowired private OssService ossService;
    @Autowired private NotifyAttachmentSnapshotTransactions snapshotTransactions;
    @Autowired private MailNotifyChannelAdapter mailAdapter;
    @Autowired private CaptureSender sender;

    @TestConfiguration(proxyBeanMethods = false)
    static class CaptureMailConfiguration {
        @Bean
        @org.springframework.context.annotation.Primary
        CaptureSender captureSender() { return new CaptureSender(); }
    }

    static final class CaptureSender implements MailNotificationSender {
        final List<CapturedMail> sent = new CopyOnWriteArrayList<>();
        volatile SendBarrier barrier;
        @Override public String send(MailNotificationMessage message) {
            try {
                SendBarrier current = barrier;
                if (current != null && message.content().contains("reserved-physical-send")) current.awaitRelease();
                List<byte[]> bytes = new ArrayList<>();
                for (var path : message.attachments()) bytes.add(Files.readAllBytes(path));
                sent.add(new CapturedMail(message.subject(), message.content(), List.copyOf(bytes)));
                return "owned-smtp-accepted-" + sent.size();
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("捕获本地邮件附件失败", exception);
            }
        }
    }

    static final class SendBarrier {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicBoolean waiting = new AtomicBoolean();

        void awaitRelease() {
            waiting.set(true);
            entered.countDown();
            try {
                if (!release.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("受控邮件发送器未获释放");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("受控邮件发送器被中断", interrupted);
            } finally {
                waiting.set(false);
            }
        }

        boolean entered(Duration timeout) throws InterruptedException {
            return entered.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        boolean waiting() { return waiting.get(); }

        void release() { release.countDown(); }
    }

    record CapturedMail(String subject, String body, List<byte[]> attachments) { }
    record OwnedObject(String service, String key) { }

    @BeforeEach
    void requireStoppedPreviousWorker() {
        assertThat(workerFixturePoisoned).as("上一测试的受控 Worker 必须已经停止").isFalse();
    }

    @BeforeAll
    void prepareOwnedApplication() {
        assertThat(System.getProperty("t42.owned.run")).matches("[a-zA-Z0-9_-]{6,64}");
        assertThat(System.getProperty("t42.mysql.url"))
            .matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/wta-plus.*");
        assertThat(System.getProperty("t42.redis.host")).isEqualTo("127.0.0.1");
        assertThat(System.getProperty("t42.minio.endpoint")).startsWith("http://127.0.0.1:");
        assertThat(System.getProperty("t42.s3.count-url")).matches("http://127\\.0\\.0\\.1:[0-9]+/count");
        wakeSubscriber.destroy();
        assertThat(notifications).isNotNull();
        assertThat(notifyClient).isNotNull();
        assertThat(snapshotService).isNotNull();
        assertThat(ossService).isNotNull();
        assertThat(snapshotTransactions).isNotNull();
        assertThat(mailAdapter).isNotNull();
        assertThat(sender).isNotNull();
        assertThat(db.queryForObject("select count(*) from information_schema.tables "
            + "where table_schema=database()", Integer.class)).isEqualTo(104);
        db.update("insert into notify_channel_account(account_id,channel,config_key,enabled,host,port,mail_from,"
            + "mail_user,mail_pass,minute_max,version,create_time,del_flag) values(?,'MAIL','owned-t42-mail','Y',"
            + "'127.0.0.1',2525,'sender@example.test','sender@example.test','unused',100,0,utc_timestamp(),'0')", ACCOUNT);
        assertThat(db.update("update notify_scene_binding set account_id=? where binding_id=?", ACCOUNT, BINDING))
            .isEqualTo(1);
    }

    @AfterAll
    void clearOwnedFacts() {
        if (workerFixturePoisoned) {
            // 仍有后台事务时不可与它并发删除事实；独占 runner 负责销毁 owned 容器。
            sender.sent.clear();
            controlClient.close();
            return;
        }
        try {
            for (Long intentId : ownedIntents) {
                db.update("delete from notify_attempt where intent_id=?", intentId);
                db.update("delete from notify_outbox where intent_id=?", intentId);
                db.update("delete from notify_delivery where intent_id=?", intentId);
                db.update("delete from notify_recipient where intent_id=?", intentId);
                for (NotifyIntentAttachment relation : dao.attachments(intentId)) {
                    db.update("delete from sys_oss_ref where ref_type='notify_intent_attachment' and ref_id=?",
                        String.valueOf(relation.getIntentAttachmentId()));
                    if (relation.getSnapshotOssId() != null) {
                        db.update("delete from sys_oss where oss_id=?", relation.getSnapshotOssId());
                    }
                }
                db.update("delete from notify_intent_attachment where intent_id=?", intentId);
                db.update("delete from notify_intent where intent_id=?", intentId);
            }
            for (Long sourceId : ownedSources) {
                db.update("delete from sys_oss_ref where oss_id=?", sourceId);
                db.update("delete from sys_oss where oss_id=?", sourceId);
            }
            for (OwnedObject object : ownedKeys) OssFactory.instance(object.service()).delete(object.key());
            db.update("update notify_scene_binding set account_id=null where binding_id=?", BINDING);
            db.update("delete from notify_channel_account where account_id=?", ACCOUNT);
        } finally {
            sender.sent.clear();
            controlClient.close();
        }
    }

    @Test @Order(1)
    void noAttachmentSubmitAndWorkerEachMakeZeroOssRequests() throws Exception {
        int before = s3Count();
        NotificationReceipt receipt = submit(List.of(), "no-attachment");
        assertThat(s3Count()).as("无附件提交阶段零OSS").isEqualTo(before);
        long intentId = Long.parseLong(receipt.notificationId());
        assertThat(dao.attachments(intentId)).isEmpty();
        makeDue(intentId);
        int workerBefore = s3Count();
        int sentBefore = sender.sent.size();
        worker.poll();
        assertThat(s3Count()).as("无附件Worker阶段零OSS").isEqualTo(workerBefore);
        assertThat(sender.sent).as("无附件投递状态：%s", deliveryStates(intentId)).hasSize(sentBefore + 1);
        assertThat(sender.sent.getLast().body()).contains("no-attachment");
        assertThat(sender.sent.getLast().attachments()).isEmpty();
    }

    @Test @Order(2)
    void authorizedSourceIsPrivateSnapshotWithRealBytesAndStableOwner() throws Exception {
        byte[] original = "owned-t42-first-attachment".getBytes(StandardCharsets.UTF_8);
        long source = source(original, "first.txt");
        int sentBefore = sender.sent.size();
        NotificationReceipt receipt = submit(List.of(String.valueOf(source)), "one-attachment");
        long intentId = Long.parseLong(receipt.notificationId());
        List<NotifyIntentAttachment> pending = dao.attachments(intentId);
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().getStatus()).isEqualTo("QUEUED");
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? and ref_type='notify_intent_attachment' "
            + "and ref_id=?", Integer.class, source, String.valueOf(pending.getFirst().getIntentAttachmentId())))
            .isEqualTo(1);
        makeDue(intentId);
        worker.poll();
        assertThat(sender.sent).as("首个附件投递状态：%s", deliveryStates(intentId)).hasSize(sentBefore + 1);
        assertThat(sender.sent.getLast().attachments()).hasSize(1);
        assertThat(sender.sent.getLast().attachments().getFirst()).containsExactly(original);
        NotifyIntentAttachment ready = dao.attachments(intentId).getFirst();
        assertThat(ready.getStatus()).isEqualTo("READY");
        assertThat(ready.getSnapshotOssId()).isPositive();
        assertThat(ready.getSha256()).matches("[0-9a-f]{64}");
        assertThat(db.queryForObject("select service from sys_oss where oss_id=?", String.class,
            ready.getSnapshotOssId())).isEqualTo("minio");
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? and ref_type='notify_intent_attachment' "
            + "and ref_id=?", Integer.class, ready.getSnapshotOssId(),
            String.valueOf(ready.getIntentAttachmentId()))).isEqualTo(1);
    }

    @Test @Order(3)
    void duplicateKeepsOneIntentAndRejectsChangedAttachmentSet() {
        long first = source("first".getBytes(StandardCharsets.UTF_8), "first.txt");
        long second = source("second".getBytes(StandardCharsets.UTF_8), "second.txt");
        String key = "owned-duplicate-" + UUID.randomUUID();
        NotificationReceipt original = submit(List.of(String.valueOf(first), String.valueOf(first),
            String.valueOf(second)), "duplicate", key);
        assertThat(dao.attachments(Long.parseLong(original.notificationId())).stream()
            .map(NotifyIntentAttachment::getSourceOssId).toList()).containsExactly(first, second);
        NotificationReceipt same = submit(List.of(String.valueOf(first), String.valueOf(second)), "duplicate", key);
        assertThat(same.notificationId()).isEqualTo(original.notificationId());
        assertThatThrownBy(() -> submit(List.of(String.valueOf(second), String.valueOf(first)), "duplicate", key))
            .isInstanceOf(RuntimeException.class);
        assertThat(db.queryForObject("select count(*) from notify_intent where app_id='owned-t42' and idempotency_key=?",
            Integer.class, key)).isEqualTo(1);
    }

    @Test @Order(4)
    void invalidOwnerAndQueuedSourceRevocationNeverReachSmtp() {
        long foreign = source("foreign-owner".getBytes(StandardCharsets.UTF_8), "foreign.txt");
        db.update("update sys_oss set create_by=? where oss_id=?", USER + 1, foreign);
        int before = sender.sent.size();
        assertThatThrownBy(() -> submit(List.of(String.valueOf(foreign)), "foreign-source"))
            .isInstanceOf(RuntimeException.class);
        assertThat(sender.sent).hasSize(before);
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? and ref_type='notify_intent_attachment'",
            Integer.class, foreign)).isZero();

        long wrongClient = source("wrong-client".getBytes(StandardCharsets.UTF_8), "wrong-client.txt");
        db.update("update sys_oss set ext1=? where oss_id=?",
            "{\"fileSize\":12,\"contentType\":\"text/plain\",\"uploaderClientPk\":" + (CLIENT + 1) + "}", wrongClient);
        assertThatThrownBy(() -> submit(List.of(String.valueOf(wrongClient)), "wrong-client"))
            .isInstanceOf(RuntimeException.class);
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? and ref_type='notify_intent_attachment'",
            Integer.class, wrongClient)).isZero();

        NotificationCommand unauthenticated = new NotificationCommand("owned-t42", "demo-mail", "demo",
            "no-session", "EMAIL", List.of("recipient@example.test"), "demo-mail",
            Map.of("title", "T42", "content", "no-session"), List.of(NotificationChannel.MAIL),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0, Instant.now().plusSeconds(3600), null,
            "owned-no-session-" + UUID.randomUUID(), Map.of(), List.of(String.valueOf(wrongClient)));
        assertThatThrownBy(() -> notifications.submit(unauthenticated)).isInstanceOf(RuntimeException.class);

        long owned = source("revoked-after-queue".getBytes(StandardCharsets.UTF_8), "revoked.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(owned)), "revoked-source").notificationId());
        db.update("update sys_oss set delete_state='PENDING' where oss_id=?", owned);
        try {
            makeDue(intentId);
            worker.poll();
            assertThat(sender.sent).hasSize(before);
            assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("QUEUED");

            long grantSource = source("revoked-login-domain".getBytes(StandardCharsets.UTF_8), "grant.txt");
            long grantIntent = Long.parseLong(submit(List.of(String.valueOf(grantSource)), "revoked-grant")
                .notificationId());
            assertThat(db.update("update sys_user_type_rel set status='1' where user_id=? and user_type_id=?",
                USER, 1762100000000000001L)).isEqualTo(1);
            makeDue(grantIntent);
            worker.poll();
            assertThat(sender.sent).hasSize(before);
            assertThat(dao.attachments(grantIntent).getFirst().getStatus()).isEqualTo("QUEUED");
        } finally {
            db.update("update sys_user_type_rel set status='0' where user_id=? and user_type_id=?",
                USER, 1762100000000000001L);
            db.update("update sys_oss set delete_state='ACTIVE' where oss_id=?", owned);
        }
    }

    @Test @Order(5)
    void twoRecipientsShareOneOrderedPrivateSnapshot() {
        byte[] original = "shared-private-snapshot".getBytes(StandardCharsets.UTF_8);
        long source = source(original, "shared.txt");
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        SaTokenContextModelBox previousSa = SaTokenContextForThreadLocalStaff.getModelBoxOrNull();
        long intentId;
        try {
            login();
            NotificationCommand command = new NotificationCommand("owned-t42", "demo-mail", "demo", "shared",
                "EMAIL", List.of("first@example.test", "second@example.test"), "demo-mail",
                Map.of("title", "T42 owned shared", "content", "shared"), List.of(NotificationChannel.MAIL),
                NotificationStrategy.ALL, NotificationMode.ASYNC, 0, Instant.now().plusSeconds(3600), null,
                "owned-shared-" + UUID.randomUUID(), Map.of(), List.of(String.valueOf(source)));
            intentId = Long.parseLong(notifications.submit(command).notificationId());
        } finally {
            restoreRequestContext(previous, previousSa);
        }
        ownedIntents.add(intentId);
        assertThat(db.queryForObject("select count(*) from notify_delivery where intent_id=?", Integer.class,
            intentId)).isEqualTo(2);
        int sentBefore = sender.sent.size();
        makeDue(intentId);
        worker.poll();
        assertThat(sender.sent).hasSize(sentBefore + 2);
        assertThat(sender.sent.get(sentBefore).attachments().getFirst()).containsExactly(original);
        assertThat(sender.sent.get(sentBefore + 1).attachments().getFirst()).containsExactly(original);
        assertThat(dao.attachments(intentId)).hasSize(1);
        assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("READY");
        assertThat(db.queryForObject("select count(*) from sys_oss where file_name like ?",
            Integer.class, "notify-attachment/" + dao.attachments(intentId).getFirst().getIntentAttachmentId() + "/%"))
            .isEqualTo(1);
    }

    @Test @Order(6)
    void relationResultWriteFailureRetainsOwnedUnknownAndNoSmtp() {
        long source = source("db-result-fault".getBytes(StandardCharsets.UTF_8), "fault.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(source)), "db-result-fault").notificationId());
        db.execute("create trigger owned_t42_ready_failure before update on notify_intent_attachment for each row "
            + "begin if new.status='READY' then signal sqlstate '45000' set message_text='owned ready failure'; "
            + "end if; end");
        int before = sender.sent.size();
        try {
            makeDue(intentId);
            worker.poll();
            assertThat(sender.sent).hasSize(before);
            NotifyIntentAttachment relation = dao.attachments(intentId).getFirst();
            assertThat(relation.getStatus()).isEqualTo("COPY_UNKNOWN");
            assertThat(relation.getSnapshotOssId()).isPositive();
            assertThat(db.queryForObject("select delete_state from sys_oss where oss_id=?",
                String.class, relation.getSnapshotOssId())).isEqualTo("NOT_READY");
            assertThatThrownBy(() -> ossService.objectMetadata(relation.getSnapshotOssId()))
                .isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> ossService.resolveAccessUrl(relation.getSnapshotOssId()))
                .isInstanceOf(RuntimeException.class);
            assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? and ref_type='notify_intent_attachment'",
                Integer.class, relation.getSnapshotOssId())).isEqualTo(1);
        } finally {
            db.execute("drop trigger if exists owned_t42_ready_failure");
        }
    }

    @Test @Order(7)
    void lostPutAcknowledgementStaysUnknownWithoutResendOrUnownedSnapshot() throws Exception {
        long source = source("lost-put-ack".getBytes(StandardCharsets.UTF_8), "lost.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(source)), "lost-put-ack").notificationId());
        int before = sender.sent.size();
        int droppedBefore = statusNumber("dropped");
        assertThat(control("/arm-drop-put")).isEqualTo("DROP_PUT_RESPONSE");
        try {
            makeDue(intentId);
            worker.poll();
            NotifyIntentAttachment relation = dao.attachments(intentId).getFirst();
            assertThat(relation.getStatus()).isEqualTo("COPY_UNKNOWN");
            assertThat(relation.getSnapshotOssId()).isPositive();
            assertThat(statusNumber("dropped")).isGreaterThan(droppedBefore);
            assertThat(awaitExactTarget(relation, "lost-put-ack".getBytes(StandardCharsets.UTF_8))).isTrue();
            assertThat(sender.sent).hasSize(before);
            worker.poll();
            assertThat(sender.sent).hasSize(before);
            assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("COPY_UNKNOWN");
            assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? and ref_type='notify_intent_attachment'",
                Integer.class, relation.getSnapshotOssId())).isEqualTo(1);
        } finally {
            assertThat(control("/disarm")).isEqualTo("NORMAL");
        }
    }

    @Test @Order(8)
    void copyCrossingLeaseFenceCannotInvokePhysicalMailSender() throws Exception {
        long source = source("lease-fence".getBytes(StandardCharsets.UTF_8), "lease.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(source)), "lease-fence").notificationId());
        int before = sender.sent.size();
        int heldBefore = statusNumber("held");
        assertThat(control("/arm-hold-put")).isEqualTo("HOLD_PUT_FORWARD");
        try (HeldWorker running = new HeldWorker()) {
            makeDue(intentId);
            running.start();
            awaitStatusAtLeast("held", heldBefore + 1, Duration.ofSeconds(10));
            assertThat(db.update("update notify_outbox set lease_token='owned-stolen-token' where intent_id=?",
                intentId)).isEqualTo(1);
            running.release();
            running.awaitDone();
            assertThat(sender.sent).hasSize(before);
            assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("READY");
        }
    }

    @Test @Order(9)
    void latePutAfterSharedDeadlineRemainsOwnedUnknownAndCannotSend() throws Exception {
        long source = source("late-remote-put".getBytes(StandardCharsets.UTF_8), "late.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(source)), "late-put").notificationId());
        int sentBefore = sender.sent.size();
        int heldBefore = statusNumber("held");
        int releasedBefore = statusNumber("released");
        assertThat(control("/arm-hold-put")).isEqualTo("HOLD_PUT_FORWARD");
        try (HeldWorker running = new HeldWorker()) {
            makeDue(intentId);
            running.start();
            awaitStatusAtLeast("held", heldBefore + 1, Duration.ofSeconds(10));
            // 只有真实共享截止结束后才让远端 PUT 完成，不能把 late byte 当 READY。
            running.awaitDone();
            NotifyIntentAttachment uncertain = dao.attachments(intentId).getFirst();
            assertThat(uncertain.getStatus()).isEqualTo("COPY_UNKNOWN");
            assertThat(uncertain.getSnapshotOssId()).isPositive();
            assertThat(sender.sent).hasSize(sentBefore);
            running.release();
            awaitStatusAtLeast("released", releasedBefore + 1, Duration.ofSeconds(10));
            assertThat(awaitExactTarget(uncertain, "late-remote-put".getBytes(StandardCharsets.UTF_8))).isTrue();
            assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("COPY_UNKNOWN");
            assertThat(sender.sent).hasSize(sentBefore);
            assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? "
                + "and ref_type='notify_intent_attachment'", Integer.class, uncertain.getSnapshotOssId()))
                .isEqualTo(1);
        }
    }

    @Test @Order(10)
    void publicReadSourceCopiesIntoCurrentPrivateTargetAndMissingPrivateFailsClosed() {
        db.update("update sys_oss_config set access_policy='2' where config_key='image'");
        try {
            byte[] firstBytes = "public-source".getBytes(StandardCharsets.UTF_8);
            long first = source(firstBytes, "public.txt", "image");
            long firstIntent = Long.parseLong(submit(List.of(String.valueOf(first)), "public-source").notificationId());
            int before = sender.sent.size();
            makeDue(firstIntent);
            worker.poll();
            assertThat(sender.sent).hasSize(before + 1);
            assertThat(sender.sent.getLast().attachments().getFirst()).containsExactly(firstBytes);
            NotifyIntentAttachment ready = dao.attachments(firstIntent).getFirst();
            assertThat(ready.getSourceService()).isEqualTo("image");
            assertThat(ready.getTargetService()).isEqualTo("minio");

            long second = source("no-private-target".getBytes(StandardCharsets.UTF_8), "no-private.txt", "image");
            long secondIntent = Long.parseLong(submit(List.of(String.valueOf(second)), "no-private").notificationId());
            db.update("update sys_oss_config set access_policy='2' where config_key='minio'");
            try {
                makeDue(secondIntent);
                worker.poll();
                assertThat(sender.sent).hasSize(before + 1);
                assertThat(dao.attachments(secondIntent).getFirst().getStatus()).isEqualTo("QUEUED");
            } finally {
                db.update("update sys_oss_config set access_policy='0' where config_key='minio'");
            }
        } finally {
            db.update("update sys_oss_config set access_policy='0' where config_key='image'");
        }
    }

    @Test @Order(11)
    void multipleAttachmentsReachMailSenderInSubmissionOrder() {
        byte[] firstBytes = "first-ordered".getBytes(StandardCharsets.UTF_8);
        byte[] secondBytes = "second-ordered".getBytes(StandardCharsets.UTF_8);
        long first = source(firstBytes, "first.txt");
        long second = source(secondBytes, "second.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(second), String.valueOf(first)),
            "ordered-attachments").notificationId());
        int sentBefore = sender.sent.size();
        makeDue(intentId);
        worker.poll();
        assertThat(sender.sent).hasSize(sentBefore + 1);
        assertThat(sender.sent.getLast().attachments()).hasSize(2);
        assertThat(sender.sent.getLast().attachments().get(0)).containsExactly(secondBytes);
        assertThat(sender.sent.getLast().attachments().get(1)).containsExactly(firstBytes);
        assertThat(dao.attachments(intentId).stream().map(NotifyIntentAttachment::getSourceOssId).toList())
            .containsExactly(second, first);
    }

    @Test @Order(12)
    void sameIdempotencyKeyConcurrentSubmissionReturnsOneOwnedRelation() throws Exception {
        long source = source("concurrent-duplicate".getBytes(StandardCharsets.UTF_8), "concurrent.txt");
        String key = "owned-concurrent-" + UUID.randomUUID();
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<NotificationReceipt>> replies = new ArrayList<>();
            for (int index = 0; index < 2; index++) replies.add(pool.submit(() -> {
                ready.countDown();
                assertThat(start.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                return submit(List.of(String.valueOf(source)), "same-request", key);
            }));
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            String first = replies.get(0).get(20, java.util.concurrent.TimeUnit.SECONDS).notificationId();
            String second = replies.get(1).get(20, java.util.concurrent.TimeUnit.SECONDS).notificationId();
            assertThat(first).isEqualTo(second);
            assertThat(dao.attachments(Long.parseLong(first))).hasSize(1);
            assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? "
                + "and ref_type='notify_intent_attachment'", Integer.class, source)).isEqualTo(1);
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test @Order(13)
    void copyCrossingPersistedExpiryCannotInvokePhysicalMailSender() throws Exception {
        long source = source("expiry-fence".getBytes(StandardCharsets.UTF_8), "expiry.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(source)), "expiry-fence").notificationId());
        int sentBefore = sender.sent.size();
        int heldBefore = statusNumber("held");
        assertThat(control("/arm-hold-put")).isEqualTo("HOLD_PUT_FORWARD");
        try (HeldWorker running = new HeldWorker()) {
            makeDue(intentId);
            running.start();
            awaitStatusAtLeast("held", heldBefore + 1, Duration.ofSeconds(10));
            // 第一轮 DB gate 已放行且远端复制已开始；此后持久截止越过当前数据库时间。
            assertThat(db.update("update notify_intent set expires_at=timestampadd(second,-1,utc_timestamp()) "
                + "where intent_id=?", intentId)).isEqualTo(1);
            running.release();
            running.awaitDone();
            assertThat(sender.sent).hasSize(sentBefore);
            assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("READY");
            assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class,
                intentId)).isZero();
        }
    }

    @Test @Order(14)
    void readySnapshotRechecksCurrentClientGrantAndPrivateTarget() throws Exception {
        long source = source("ready-recheck".getBytes(StandardCharsets.UTF_8), "ready.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(source)), "ready-recheck").notificationId());
        makeDue(intentId);
        worker.poll();
        assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("READY");
        var snapshots = snapshotService.createSnapshots(intentId, List.of(source), NotifyContext.empty());
        assertThat(snapshots).hasSize(1);
        java.nio.file.Path output = Files.createTempFile("owned-t42-ready-", ".txt");
        try {
            db.update("update sys_client set status='1' where id=?", CLIENT);
            assertThatThrownBy(() -> snapshots.getFirst().resource().materializer().materialize(output))
                .isInstanceOf(java.io.IOException.class);
            db.update("update sys_client set status='0' where id=?", CLIENT);

            db.update("update sys_oss_config set access_policy='2' where config_key='minio'");
            assertThatThrownBy(() -> snapshots.getFirst().resource().materializer().materialize(output))
                .isInstanceOf(java.io.IOException.class);
            db.update("update sys_oss_config set access_policy='0',status='N' where config_key='minio'");
            snapshots.getFirst().resource().materializer().materialize(output);
            assertThat(Files.readAllBytes(output)).containsExactly("ready-recheck".getBytes(StandardCharsets.UTF_8));
        } finally {
            db.update("update sys_client set status='0' where id=?", CLIENT);
            db.update("update sys_oss_config set access_policy='0',status='Y' where config_key='minio'");
            Files.deleteIfExists(output);
        }
    }

    @Test @Order(15)
    void cancelledUnsentIntentReleasesOnlyItsReferencesAndCannotRequeueReleasedSnapshot() {
        long source = source("cancelled-unsent".getBytes(StandardCharsets.UTF_8), "cancelled.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(source)), "cancelled-unsent").notificationId());
        NotifyIntentAttachment pending = dao.attachments(intentId).getFirst();
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? and ref_type='notify_intent_attachment'",
            Integer.class, source)).isEqualTo(1);
        int sentBefore = sender.sent.size();
        notifications.cancel(new NotificationCancelCommand(String.valueOf(intentId), "owned-cancel"));
        makeDue(intentId);
        worker.poll();
        assertThat(snapshotTransactions.releaseIfSafe(intentId)).isTrue();
        assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("RELEASED");
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? and ref_type='notify_intent_attachment' "
            + "and ref_id=? and del_flag='0'", Integer.class, source, String.valueOf(pending.getIntentAttachmentId()))).isZero();
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? and ref_type='notify_intent_attachment' "
            + "and ref_id=? and del_flag='1'", Integer.class, source, String.valueOf(pending.getIntentAttachmentId()))).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from sys_oss where oss_id=? and delete_state='ACTIVE'",
            Integer.class, source)).isEqualTo(1);
        assertThat(notifications.retry(new NotificationRetryCommand(String.valueOf(intentId), null,
            "owned-retry-after-release", null)).queuedCount()).isZero();
        worker.poll();
        assertThat(sender.sent).hasSize(sentBefore);
    }

    @Test @Order(16)
    void undeliverableMailWithoutOutboxReleasesItsSourceReference() {
        long source = source("no-email-recipient".getBytes(StandardCharsets.UTF_8), "no-email.txt");
        String originalEmail = db.queryForObject("select email from sys_user where user_id=?", String.class, USER);
        int sentBefore = sender.sent.size();
        long intentId;
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        SaTokenContextModelBox previousSa = SaTokenContextForThreadLocalStaff.getModelBoxOrNull();
        try {
            assertThat(db.update("update sys_user set email='' where user_id=?", USER)).isEqualTo(1);
            login();
            NotificationCommand command = new NotificationCommand("owned-t42", "demo-mail", "demo",
                "undeliverable-user", "USER", List.of(String.valueOf(USER)), "demo-mail",
                Map.of("title", "T42", "content", "undeliverable-user"), List.of(NotificationChannel.MAIL),
                NotificationStrategy.ALL, NotificationMode.ASYNC, 0, Instant.now().plusSeconds(3600), null,
                "owned-undeliverable-" + UUID.randomUUID(), Map.of(), List.of(String.valueOf(source)));
            intentId = Long.parseLong(notifications.submit(command).notificationId());
            ownedIntents.add(intentId);
        } finally {
            db.update("update sys_user set email=? where user_id=?", originalEmail, USER);
            restoreRequestContext(previous, previousSa);
        }
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId))
            .isEqualTo("UNDELIVERABLE");
        assertThat(db.queryForObject("select count(*) from notify_outbox where intent_id=?", Integer.class, intentId))
            .isZero();
        NotifyIntentAttachment relation = dao.attachments(intentId).getFirst();
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? "
            + "and ref_type='notify_intent_attachment' and ref_id=?", Integer.class, source,
            String.valueOf(relation.getIntentAttachmentId()))).isEqualTo(1);
        assertThat(snapshotTransactions.releaseIfSafe(intentId)).isTrue();
        assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("RELEASED");
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? "
            + "and ref_type='notify_intent_attachment' and ref_id=? and del_flag='0'", Integer.class, source,
            String.valueOf(relation.getIntentAttachmentId()))).isZero();
        assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? "
            + "and ref_type='notify_intent_attachment' and ref_id=? and del_flag='1'", Integer.class, source,
            String.valueOf(relation.getIntentAttachmentId()))).isEqualTo(1);
        assertThat(sender.sent).hasSize(sentBefore);
    }

    @Test @Order(17)
    void reservedPhysicalSendSurvivesCancelAndExpiredLeaseReclaim() throws Exception {
        long source = source("reserved-physical-send".getBytes(StandardCharsets.UTF_8), "reserved.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(source)), "reserved-physical-send")
            .notificationId());
        int sentBefore = sender.sent.size();
        try (BlockedMailWorker running = new BlockedMailWorker()) {
            running.arm();
            makeDue(intentId);
            running.start();
            assertThat(running.barrier.entered(Duration.ofSeconds(10))).isTrue();
            assertThat(running.barrier.waiting()).isTrue();
            NotifyIntentAttachment relation = dao.attachments(intentId).getFirst();
            assertThat(relation.getStatus()).isEqualTo("READY");
            assertThat(relation.getSendReserved()).isTrue();
            assertThat(sender.sent).hasSize(sentBefore);
            assertThat(notifications.cancel(new NotificationCancelCommand(String.valueOf(intentId),
                "owned-cancel-after-provider-entry"))).isNotNull();
            String firstToken = db.queryForObject("select lease_token from notify_outbox where intent_id=?",
                String.class, intentId);
            assertThat(firstToken).isNotBlank();
            assertThat(db.update("update notify_outbox set lease_until=timestampadd(second,-1,utc_timestamp()) "
                + "where intent_id=? and status='PROCESSING'", intentId)).isEqualTo(1);
            // 第二 owner 真实领取并确认 fencing token 已变，再经生产分发与结果事务关闭旧任务。
            String secondOwner = "owned-t42-reclaimer-" + UUID.randomUUID();
            List<NotifyOutbox> claimed = claimPort.claim(secondOwner);
            NotifyOutbox reclaimed = claimed.stream().filter(row -> row.getIntentId().equals(intentId))
                .findFirst().orElseThrow();
            assertThat(reclaimed.getLeaseOwner()).isEqualTo(secondOwner);
            assertThat(reclaimed.getLeaseToken()).isNotEqualTo(firstToken);
            claimed.forEach(dispatchPort::dispatch);
            assertThat(db.queryForObject("select status from notify_outbox where intent_id=?",
                String.class, intentId)).isEqualTo("DONE");
            assertThat(snapshotTransactions.releaseIfSafe(intentId)).isFalse();
            assertThat(running.barrier.waiting()).isTrue();
            assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id in (?,?) "
                + "and ref_type='notify_intent_attachment' and ref_id=?", Integer.class, source,
                relation.getSnapshotOssId(), String.valueOf(relation.getIntentAttachmentId()))).isEqualTo(2);
            assertThat(dao.attachments(intentId).getFirst().getStatus()).isEqualTo("READY");
            running.release();
            running.awaitDone();
            assertThat(sender.sent).hasSize(sentBefore + 1);
            assertThat(dao.attachments(intentId).getFirst().getSendReserved()).isTrue();
        }
    }

    @Test @Order(18)
    void attachmentMapperRejectsStaleVersionAndHidesLogicalDelete() {
        long source = source("mapper-version".getBytes(StandardCharsets.UTF_8), "version.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(source)), "mapper-version")
            .notificationId());
        long relationId = dao.attachments(intentId).getFirst().getIntentAttachmentId();
        NotifyIntentAttachment fresh = attachmentMapper.selectById(relationId);
        NotifyIntentAttachment stale = attachmentMapper.selectById(relationId);
        int initialVersion = fresh.getVersion();
        assertThat(initialVersion).isPositive();
        assertThat(stale.getVersion()).isEqualTo(initialVersion);
        fresh.setFileName("first-version.txt");
        assertThat(dao.saveAttachment(fresh)).isEqualTo(1);
        assertThat(attachmentMapper.selectById(relationId).getVersion()).isEqualTo(initialVersion + 1);
        stale.setFileName("stale-version.txt");
        assertThat(dao.saveAttachment(stale)).isZero();
        assertThat(attachmentMapper.selectById(relationId).getFileName()).isEqualTo("first-version.txt");
        assertThat(attachmentMapper.deleteById(relationId)).isEqualTo(1);
        assertThat(attachmentMapper.selectById(relationId)).isNull();
        assertThat(db.queryForObject("select del_flag from notify_intent_attachment where intent_attachment_id=?",
            String.class, relationId)).isEqualTo("1");
    }

    @Test @Order(19)
    void secondAttachmentConfirmationFailureKeepsFirstReadyAndAllReferences() {
        long first = source("first-confirmed".getBytes(StandardCharsets.UTF_8), "first-confirmed.txt");
        long second = source("second-unknown".getBytes(StandardCharsets.UTF_8), "second-unknown.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(first), String.valueOf(second)),
            "second-confirm-failure").notificationId());
        int sentBefore = sender.sent.size();
        db.execute("create trigger owned_t42_second_ready_failure before update on notify_intent_attachment "
            + "for each row begin if new.status='READY' and old.position=1 then "
            + "signal sqlstate '45000' set message_text='owned second ready failure'; end if; end");
        try {
            makeDue(intentId);
            worker.poll();
            List<NotifyIntentAttachment> relations = dao.attachments(intentId);
            assertThat(relations).hasSize(2);
            assertThat(relations.get(0).getStatus()).isEqualTo("READY");
            assertThat(relations.get(1).getStatus()).isEqualTo("COPY_UNKNOWN");
            assertThat(relations).allSatisfy(row -> {
                assertThat(row.getSendReserved()).isFalse();
                assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? "
                    + "and ref_type='notify_intent_attachment' and ref_id=?", Integer.class,
                    row.getSourceOssId(), String.valueOf(row.getIntentAttachmentId()))).isEqualTo(1);
                assertThat(db.queryForObject("select count(*) from sys_oss_ref where oss_id=? "
                    + "and ref_type='notify_intent_attachment' and ref_id=?", Integer.class,
                    row.getSnapshotOssId(), String.valueOf(row.getIntentAttachmentId()))).isEqualTo(1);
            });
            assertThat(sender.sent).hasSize(sentBefore);
            assertThat(snapshotTransactions.releaseIfSafe(intentId)).isFalse();
        } finally {
            db.execute("drop trigger if exists owned_t42_second_ready_failure");
        }
    }

    @Test @Order(20)
    void failedSecondSendReservationRollsBackEveryRelationAndNeverCallsSender() {
        long first = source("first-reserved".getBytes(StandardCharsets.UTF_8), "first-reserved.txt");
        long second = source("second-reserved".getBytes(StandardCharsets.UTF_8), "second-reserved.txt");
        long intentId = Long.parseLong(submit(List.of(String.valueOf(first), String.valueOf(second)),
            "second-reservation-failure").notificationId());
        int sentBefore = sender.sent.size();
        db.execute("create trigger owned_t42_send_reservation_failure before update on notify_intent_attachment "
            + "for each row begin if new.send_reserved=1 and old.position=1 then "
            + "signal sqlstate '45000' set message_text='owned send reservation failure'; end if; end");
        try {
            makeDue(intentId);
            assertThatThrownBy(worker::poll).isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertThat(sender.sent).hasSize(sentBefore);
            assertThat(dao.attachments(intentId)).hasSize(2)
                .allSatisfy(row -> {
                    assertThat(row.getStatus()).isEqualTo("READY");
                    assertThat(row.getSendReserved()).isFalse();
                });
            assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?",
                Integer.class, intentId)).isZero();
        } finally {
            db.execute("drop trigger if exists owned_t42_send_reservation_failure");
        }
    }

    private NotificationReceipt submit(List<String> ids, String body) {
        return submit(ids, body, "owned-t42-" + UUID.randomUUID());
    }

    private NotificationReceipt submit(List<String> ids, String body, String idempotencyKey) {
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        SaTokenContextModelBox previousSa = SaTokenContextForThreadLocalStaff.getModelBoxOrNull();
        try {
            login();
            NotificationCommand command = new NotificationCommand("owned-t42", "demo-mail", "demo", body,
                "EMAIL", List.of("recipient@example.test"), "demo-mail",
                Map.of("title", "T42 owned mail", "content", body), List.of(NotificationChannel.MAIL),
                NotificationStrategy.ALL, NotificationMode.ASYNC, 0, Instant.now().plusSeconds(3600), null,
                idempotencyKey, Map.of(), ids);
            NotificationReceipt receipt = notifications.submit(command);
            ownedIntents.add(Long.parseLong(receipt.notificationId()));
            return receipt;
        } finally {
            restoreRequestContext(previous, previousSa);
        }
    }

    private void login() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        // Sa-Token 1.45 的生产 Servlet filter 使用独立线程上下文，不能只设置 Spring holder。
        SaTokenContextForThreadLocalStaff.setModelBox(new SaRequestForServlet(request),
            new SaResponseForServlet(response), new SaStorageForServlet(request));
        LoginUser user = new LoginUser();
        user.setUserId(USER);
        user.setUsername("WTA");
        user.setUserType("sys_user");
        user.setUserTypeId(1762100000000000001L);
        user.setClientPk(CLIENT);
        LoginHelper.login(user, new SaLoginParameter().setDeviceType("pc"));
        LoginUser authenticated = LoginHelper.getLoginUser();
        assertThat(authenticated).isNotNull();
        assertThat(authenticated.getUserId()).isEqualTo(USER);
        assertThat(authenticated.getClientPk()).isEqualTo(CLIENT);
    }

    private void restoreRequestContext(RequestAttributes previous, SaTokenContextModelBox previousSa) {
        if (previous == null) RequestContextHolder.resetRequestAttributes();
        else RequestContextHolder.setRequestAttributes(previous);
        if (previousSa == null) SaTokenContextForThreadLocalStaff.clearModelBox();
        else SaTokenContextForThreadLocalStaff.setModelBox(previousSa.getRequest(),
            previousSa.getResponse(), previousSa.getStorage());
    }

    private long source(byte[] bytes, String originalName) {
        return source(bytes, originalName, "minio");
    }

    private long source(byte[] bytes, String originalName, String service) {
        long id = IdGeneratorUtil.nextLongId();
        String key = "t42-source/" + UUID.randomUUID() + "/" + originalName;
        OssFactory.instance(service).uploadBounded(key, bytes, "text/plain", Duration.ofSeconds(10));
        ownedSources.add(id);
        ownedKeys.add(new OwnedObject(service, key));
        db.update("insert into sys_oss(oss_id,file_name,original_name,file_suffix,url,ext1,create_time,create_by,"
                + "service,is_temp,delete_state) values(?,?,?,'txt','',?,utc_timestamp(),?,?,'N','ACTIVE')",
            id, key, originalName,
            "{\"fileSize\":" + bytes.length + ",\"contentType\":\"text/plain\",\"uploaderClientPk\":" + CLIENT + "}", USER, service);
        return id;
    }

    private void makeDue(long intentId) {
        db.update("update notify_intent set scheduled_at=timestampadd(second,-1,utc_timestamp()) where intent_id=?",
            intentId);
        db.update("update notify_outbox set available_at=timestampadd(second,-1,utc_timestamp()),"
            + "next_attempt_at=timestampadd(second,-1,utc_timestamp()) where intent_id=?", intentId);
    }

    private List<Map<String, Object>> deliveryStates(long intentId) {
        // 仅输出固定状态/错误码，不把收件地址、正文或凭据带入失败报告。
        return db.queryForList("select d.status as delivery_status,d.error_code,o.status as outbox_status "
            + "from notify_delivery d left join notify_outbox o on o.delivery_id=d.delivery_id where d.intent_id=?", intentId);
    }

    private int s3Count() throws Exception {
        URI endpoint = URI.create(System.getProperty("t42.s3.count-url"));
        assertThat(endpoint.getHost()).isEqualTo("127.0.0.1");
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5)).GET().build();
        HttpResponse<String> response = controlClient.send(request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.US_ASCII));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).matches("[0-9]+");
        return Integer.parseInt(response.body());
    }

    private String control(String path) throws Exception {
        HttpResponse<String> response = controlResponse(path);
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private HttpResponse<String> controlResponse(String path) throws Exception {
        URI endpoint = URI.create(System.getProperty("t42.s3.count-url")).resolve(path);
        if (!"127.0.0.1".equals(endpoint.getHost())) throw new IllegalStateException("受控S3地址不是回环地址");
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.noBody()).build();
        return controlClient.send(request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.US_ASCII));
    }

    private int statusNumber(String name) throws Exception {
        Object value = statusFacts().get(name);
        assertThat(value).isInstanceOf(Number.class);
        return ((Number) value).intValue();
    }

    private Map<String, Object> statusFacts() throws Exception {
        URI endpoint = URI.create(System.getProperty("t42.s3.count-url")).resolve("/status");
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5)).GET().build();
        HttpResponse<String> response = controlClient.send(request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.US_ASCII));
        if (response.statusCode() != 200) throw new IllegalStateException("受控S3状态查询失败");
        return JsonUtils.parseMap(response.body());
    }

    /** 仅本测试持有的 Worker；失败退出也要放行代理、取消线程并确认真实退出。 */
    private final class HeldWorker implements AutoCloseable {
        private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "owned-t42-held-worker");
            thread.setDaemon(true);
            return thread;
        });
        private Future<?> running;
        private boolean released;

        void start() {
            if (running != null) throw new IllegalStateException("受控 Worker 已启动");
            running = executor.submit(worker::poll);
        }

        void release() throws Exception {
            assertThat(control("/release-hold")).isEqualTo("NORMAL");
            released = true;
        }

        void awaitDone() throws Exception {
            if (running == null) throw new IllegalStateException("受控 Worker 尚未启动");
            running.get(40, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            Exception cleanupFailure = null;
            if (!released) {
                try {
                    Object mode = statusFacts().get("mode");
                    if ("HOLD_PUT_FORWARD".equals(mode)) {
                        HttpResponse<String> response = controlResponse("/release-hold");
                        if (response.statusCode() != 200 || !"NORMAL".equals(response.body())) {
                            throw new IllegalStateException("受控S3未释放仍在等待的PUT");
                        }
                    } else if (!"HOLD_CANCELLED".equals(mode) && !"NORMAL".equals(mode)) {
                        throw new IllegalStateException("受控S3处于未知持有状态");
                    }
                } catch (Exception failure) {
                    cleanupFailure = failure;
                }
            }
            if (running != null && !running.isDone()) running.cancel(true);
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    workerFixturePoisoned = true;
                    cleanupFailure = combine(cleanupFailure, new IllegalStateException("受控 Worker 未停止"));
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                workerFixturePoisoned = true;
                cleanupFailure = combine(cleanupFailure, interrupted);
            }
            try {
                HttpResponse<String> response = controlResponse("/disarm");
                if (response.statusCode() != 200 || !"NORMAL".equals(response.body())) {
                    throw new IllegalStateException("受控S3未复位");
                }
            } catch (Exception failure) {
                cleanupFailure = combine(cleanupFailure, failure);
            }
            if (cleanupFailure != null) {
                workerFixturePoisoned = true;
                throw cleanupFailure;
            }
        }

        private Exception combine(Exception first, Exception next) {
            if (first == null) return next;
            first.addSuppressed(next);
            return first;
        }
    }

    /** 只阻塞最底层捕获 SMTP；释放失败时毒化 fixture，禁止后续业务测试清理活线程。 */
    private final class BlockedMailWorker implements AutoCloseable {
        private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "owned-t42-blocked-mail-worker");
            thread.setDaemon(true);
            return thread;
        });
        private final SendBarrier barrier = new SendBarrier();
        private Future<?> running;

        void arm() { sender.barrier = barrier; }

        void start() {
            if (running != null) throw new IllegalStateException("受控邮件 Worker 已启动");
            if (sender.barrier != barrier) throw new IllegalStateException("受控邮件发送屏障尚未装配");
            running = executor.submit(worker::poll);
        }

        void release() { barrier.release(); }

        void awaitDone() throws Exception {
            if (running == null) throw new IllegalStateException("受控邮件 Worker 尚未启动");
            running.get(10, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            barrier.release();
            if (running != null && !running.isDone()) running.cancel(true);
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    workerFixturePoisoned = true;
                    throw new IllegalStateException("受控邮件 Worker 未停止");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                workerFixturePoisoned = true;
                throw interrupted;
            } finally {
                sender.barrier = null;
            }
        }
    }

    private void awaitStatusAtLeast(String name, int minimum, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        do {
            if (statusNumber(name) >= minimum) return;
            Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("受控S3状态未在期限内到达：" + name);
    }

    private boolean awaitExactTarget(NotifyIntentAttachment relation, byte[] expected) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        do {
            try {
                byte[] actual = OssFactory.instance(relation.getTargetService()).downloadBounded(
                    relation.getTargetKey(), expected.length, Duration.ofSeconds(2));
                assertThat(actual).containsExactly(expected);
                return true;
            } catch (RuntimeException notYetVisible) {
                if (System.nanoTime() >= deadline) throw notYetVisible;
                Thread.sleep(50);
            }
        } while (true);
    }
}
