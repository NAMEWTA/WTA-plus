package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.DsTxEventListenerFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.notify.adapter.event.NotifyOutboxWakePublisher;
import org.namewta.notify.adapter.worker.NotifyOutboxWakeSubscriber;
import org.namewta.notify.adapter.worker.NotifyOutboxWorker;
import org.namewta.notify.api.*;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.namewta.notify.support.outbox.NotifyOutboxWakeChannels;
import org.namewta.notify.support.outbox.NotifyOutboxWakeSignal;
import org.namewta.notify.usecase.NotificationApplicationUseCase;
import org.namewta.notify.usecase.NotifyOutboxClaimUseCase;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 真实提交代理、六文件 MySQL、Redis 发布/订阅与 T-22 结果事务的唤醒验收。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.wake.integration", matches = "true")
class NotifyWakeIntegrationTest {
    @Test
    void commitIsIndependentOfSlowProviderAndPollRecoversLostOrTimedOutWake() throws Exception {
        // 只复用测试夹具，生产可见性与 API 不为测试扩大。
        var fixture = new NotifyAtomicResultIntegrationTest();
        Object previousContext = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        Object previousFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        RedissonClient otherClient = null;
        NotifyOutboxWakeSubscriber subscriber = null;
        Integer otherListener = null;
        var releaseProvider = new CountDownLatch(1);
        try {
            fixture.open();
            var redis = field(fixture, "redis", RedissonClient.class);
            var dao = field(fixture, "dao", NotifyNotificationDao.class);
            var db = field(fixture, "db", JdbcTemplate.class);
            var claims = field(fixture, "claims", NotifyOutboxClaimUseCase.class);
            var dispatch = field(fixture, "dispatch", DispatchNotificationService.class);
            var workerA = new NotifyOutboxWorker(claims, dispatch);
            var workerB = new NotifyOutboxWorker(claims, dispatch);
            var providerEntered = new CountDownLatch(1);
            var providerThread = new AtomicReference<Thread>();
            var firstProviderAt = new AtomicLong();
            ReflectionTestUtils.setField(fixture, "duringProvider", (Runnable) () -> {
                providerThread.compareAndSet(null, Thread.currentThread());
                firstProviderAt.compareAndSet(0, System.nanoTime()); providerEntered.countDown();
                try {
                    if (!releaseProvider.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("owned provider barrier timeout");
                } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
            });
            var config = new Config(); config.setThreads(2).setNettyThreads(2);
            config.useSingleServer().setAddress("redis://127.0.0.1:" + System.getProperty("notify.redis.integration.port"))
                .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
            otherClient = Redisson.create(config);
            try (var context = new AnnotationConfigApplicationContext()) {
                context.registerBean(RedissonClient.class, () -> redis);
                context.registerBean(SpringUtils.class);
                context.registerBean(DsTxEventListenerFactory.class);
                context.registerBean(NotifyOutboxWakePublisher.class);
                context.refresh();
                assertThat(RedisUtils.getClient()).isSameAs(redis);
                subscriber = new NotifyOutboxWakeSubscriber(workerA); subscriber.afterPropertiesSet();
                otherListener = otherClient.getTopic(NotifyOutboxWakeChannels.REDIS_CHANNEL)
                    .addListener(NotifyOutboxWakeSignal.class, (channel, signal) -> workerB.onWake(signal));
                var user = new UserDTO(); user.setUserId(7L);
                var users = mock(UserService.class); when(users.selectNotificationUsers(List.of(7L))).thenReturn(List.of(user));
                var runtime = new NotificationApplicationRuntimeService(dao, users, dispatch, context);
                var proxy = new ProxyFactory(new NotificationApplicationUseCase(runtime)); proxy.setProxyTargetClass(true);
                proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
                var application = (NotificationApplicationUseCase) proxy.getProxy();

                long started = System.nanoTime();
                var receipt = application.submit(command("slow"));
                long commitMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                assertThat(commitMillis).as("commit does not wait for a blocked Provider").isLessThan(1000);
                assertThat(receipt.outboxQueued()).isTrue();
                assertThat(providerEntered.await(3, TimeUnit.SECONDS)).isTrue();
                assertThat(providerThread.get()).isNotSameAs(Thread.currentThread());
                long slowId = Long.parseLong(receipt.notificationId());
                assertThat(dao.intent(slowId).getStatus()).isEqualTo("QUEUED");
                long firstDeliveryMillis = TimeUnit.NANOSECONDS.toMillis(firstProviderAt.get() - started);
                for (int index = 0; index < 100; index++) {
                    otherClient.getTopic(NotifyOutboxWakeChannels.REDIS_CHANNEL).publishAsync(NotifyOutboxWakeSignal.wake(null)).get(1, TimeUnit.SECONDS);
                }
                assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isEqualTo(1);
                releaseProvider.countDown();
                await(() -> "DELIVERED".equals(dao.intent(slowId).getStatus()));
                assertOneResult(db, slowId);

                // 没有任何 worker 订阅时，发布成功仍不能当作投递成功；真实 poll 负责补偿。
                subscriber.destroy(); subscriber = null;
                otherClient.getTopic(NotifyOutboxWakeChannels.REDIS_CHANNEL).removeListener(otherListener); otherListener = null;
                ReflectionTestUtils.setField(fixture, "duringProvider", (Runnable) () -> {});
                var lost = application.submit(command("lost")); long lostId = Long.parseLong(lost.notificationId());
                assertThat(db.queryForObject("select status from notify_outbox where intent_id=?", String.class, lostId)).isEqualTo("READY");
                workerA.poll();
                await(() -> "DELIVERED".equals(dao.intent(lostId).getStatus())); assertOneResult(db, lostId);

                // 只暂停 runner 所有的随机 loopback Redis，服务在 1200ms 后自动恢复。
                pauseOwnedRedis(1200);
                started = System.nanoTime();
                var timedOut = application.submit(command("redis-timeout"));
                long timeoutCommitMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                long timeoutId = Long.parseLong(timedOut.notificationId());
                assertThat(timeoutCommitMillis).isBetween(200L, 1000L);
                assertThat(db.queryForObject("select status from notify_outbox where intent_id=?", String.class, timeoutId)).isEqualTo("READY");
                assertThat(dao.intent(timeoutId)).isNotNull();
                redis.getKeys().count(); // 有界暂停结束后重新确认 Redis 可用，再触发真实兜底 tick。
                workerB.poll();
                await(() -> "DELIVERED".equals(dao.intent(timeoutId).getStatus())); assertOneResult(db, timeoutId);
                System.out.println("T-28: commitMs=" + commitMillis + ", firstProviderMs=" + firstDeliveryMillis
                    + ", pausedRedisCommitMs=" + timeoutCommitMillis + ", scenarios=slow-provider,wake-storm,lost-wake,redis-timeout,poll-recovery");
            }
        } finally {
            releaseProvider.countDown();
            if (subscriber != null) subscriber.destroy();
            if (otherClient != null) {
                if (otherListener != null) otherClient.getTopic(NotifyOutboxWakeChannels.REDIS_CHANNEL).removeListener(otherListener);
                otherClient.shutdown();
            }
            var db = field(fixture, "db", JdbcTemplate.class);
            if (db != null) {
                for (long id : db.queryForList("select intent_id from notify_intent where app_id='owned-t28'", Long.class)) {
                    for (String table : List.of("notify_attempt", "notify_outbox", "notify_delivery", "notify_recipient", "notify_intent")) {
                        db.update("delete from " + table + " where intent_id=?", id);
                    }
                    db.update("delete from notify_message_recipient where message_id=?", id);
                    db.update("delete from notify_message where message_id=?", id);
                }
            }
            fixture.close();
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousContext);
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousFactory);
        }
    }

    private static NotificationCommand command(String key) {
        return new NotificationCommand("owned-t28", "owned", "OWNED", key, "USER", List.of("7"), "owned",
            Map.of("title", "Owned wake", "content", "Owned fixture"), List.of(NotificationChannel.IN_APP),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0, null, null, "owned-t28-" + key, Map.of(), java.util.List.of());
    }

    private static void assertOneResult(JdbcTemplate db, long id) {
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, id)).isEqualTo(1);
        assertThat(db.queryForObject("select status from notify_outbox where intent_id=?", String.class, id)).isEqualTo("DONE");
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, id)).isEqualTo(1);
    }

    private static void pauseOwnedRedis(int milliseconds) throws Exception {
        int port = Integer.parseInt(System.getProperty("notify.redis.integration.port"));
        try (var socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(3000);
            String duration = String.valueOf(milliseconds);
            String command = "*4\r\n$6\r\nCLIENT\r\n$5\r\nPAUSE\r\n$" + duration.length() + "\r\n" + duration + "\r\n$3\r\nALL\r\n";
            socket.getOutputStream().write(command.getBytes(StandardCharsets.US_ASCII)); socket.getOutputStream().flush();
            assertThat(new String(socket.getInputStream().readNBytes(5), StandardCharsets.US_ASCII)).isEqualTo("+OK\r\n");
        }
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static <T> T field(Object fixture, String name, Class<T> type) {
        return type.cast(ReflectionTestUtils.getField(fixture, name));
    }
}
