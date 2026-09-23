package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.github.yulichang.config.MPJInterceptorConfig;
import com.github.yulichang.injector.MPJSqlInjector;
import com.github.yulichang.interceptor.MPJInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.mybatis.handler.InjectionMetaObjectHandler;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyProviderReceiptDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.mapper.*;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotifyDispatchResultService;
import org.namewta.notify.usecase.NotifyDispatchResultUseCase;
import org.namewta.notify.service.runtime.InAppNotificationService;
import org.namewta.notify.usecase.InAppCommittedPushUseCase;
import org.namewta.notify.adapter.event.InAppCommittedPushListener;
import org.namewta.notify.service.runtime.NotifyOutboxClaimService;
import org.namewta.notify.service.runtime.ProviderCallbackService;
import org.namewta.notify.usecase.NotifyOutboxClaimUseCase;
import org.namewta.notify.usecase.ProviderCallbackUseCase;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.common.json.utils.JsonUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.HexFormat;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 隔离六文件 MySQL 基座、真实 Mapper/动态事务与 Redis 外部副作用的结果原子性验收。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.atomic.integration", matches = "true")
class NotifyAtomicResultIntegrationTest {
    private static final long INTENT = 9220001L;
    private static final long DELIVERY = 9220002L;
    private static final long OUTBOX = 9220003L;
    private DynamicRoutingDataSource routing;
    private HikariDataSource pool;
    private RedissonClient redis;
    private JdbcTemplate db;
    private NotifyNotificationDao dao;
    private DispatchNotificationService dispatch;
    private NotifyDispatchResultUseCase results;
    private NotifyOutboxClaimUseCase claims;
    private ProviderCallbackUseCase callbacks;
    private NotifyProviderReceiptDao receipts;
    private NotifyProviderReceiptMapper receiptMapper;
    private NotifyConfigDao configDao;
    private Runnable duringProvider = () -> {};
    private AnnotationConfigApplicationContext eventContext;
    private final AtomicInteger realtimeCalls = new AtomicInteger();
    private boolean realtimeFails = true;
    private final java.util.concurrent.atomic.AtomicReference<String> commitFault = new java.util.concurrent.atomic.AtomicReference<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void open() throws Exception {
        String url = System.getProperty("notify.mysql.integration.url");
        assertThat(url).matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/namewta_notify_test_[a-zA-Z0-9_]+.*");
        String password = System.getenv("T36_MYSQL_PASSWORD");
        assertThat(password).as("private owned MySQL credential").isNotNull();
        pool = new HikariDataSource(); pool.setJdbcUrl(url);
        pool.setUsername(System.getProperty("notify.mysql.integration.username", "root"));
        pool.setPassword(password); pool.setMaximumPoolSize(8);
        routing = new DynamicRoutingDataSource(List.of()); routing.setPrimary("master"); routing.setStrict(true);
        routing.addDataSource("master", new org.springframework.jdbc.datasource.DelegatingDataSource(pool) {
            @Override public java.sql.Connection getConnection() throws java.sql.SQLException {
                java.sql.Connection connection = pool.getConnection();
                return (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{java.sql.Connection.class},
                    (proxy, method, arguments) -> {
                        if ("commit".equals(method.getName())) {
                            String fault = commitFault.getAndSet(null);
                            if ("BEFORE".equals(fault)) {
                                long connectionId;
                                try (var statement = connection.createStatement(); var row = statement.executeQuery("select connection_id()")) {
                                    row.next(); connectionId = row.getLong(1);
                                }
                                try (var killer = pool.getConnection(); var statement = killer.createStatement()) {
                                    statement.execute("kill connection " + connectionId);
                                }
                                // 真正关闭服务端会话，再调用 JDBC commit 验证提交前断连回滚。
                            } else if ("AFTER".equals(fault)) {
                                connection.commit();
                                throw new java.sql.SQLRecoverableException("owned lost commit acknowledgement");
                            }
                        }
                        try { return method.invoke(connection, arguments); }
                        catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                    });
            }
        });
        db = new JdbcTemplate(routing);
        var config = new MybatisConfiguration(new Environment("notify-owned", new SpringManagedTransactionFactory(), routing));
        config.setMapUnderscoreToCamelCase(true);
        var interceptors = new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        var pagination = new PaginationInnerInterceptor();
        pagination.setOverflow(true);
        interceptors.addInnerInterceptor(pagination);
        interceptors.addInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor());
        config.addInterceptor(interceptors);
        GlobalConfigUtils.setGlobalConfig(config, GlobalConfigUtils.defaults()
            .setMetaObjectHandler(new InjectionMetaObjectHandler()).setSqlInjector(new MPJSqlInjector()));
        for (Class<?> mapper : List.of(NotifyIntentMapper.class, NotifyRecipientMapper.class, NotifyDeliveryMapper.class,
            NotifyOutboxMapper.class, NotifyAttemptMapper.class, NotifyMessageMapper.class, NotifyMessageRecipientMapper.class, NotifyProviderReceiptMapper.class, NotifyChannelAccountMapper.class, NotifySceneBindingMapper.class)) config.addMapper(mapper);
        for (String resource : List.of("/mapper/notify/NotifyOutboxMapper.xml", "/mapper/notify/NotifyChannelAccountMapper.xml", "/mapper/notify/NotifyDeliveryMapper.xml")) {
            try (var stream = getClass().getResourceAsStream(resource)) {
                assertThat(stream).isNotNull();
                new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
            }
        }
        var sessionFactory = new MybatisSqlSessionFactoryBuilder().build(config);
        // 手装夹具须与生产MPJ自动配置一样，在factory生成后安装动态结果映射与分页包装。
        new MPJInterceptorConfig(List.of(sessionFactory), new MPJInterceptor(), false);
        var sessions = new SqlSessionTemplate(sessionFactory);
        dao = new NotifyNotificationDao(sessions.getMapper(NotifyIntentMapper.class), sessions.getMapper(NotifyRecipientMapper.class),
            sessions.getMapper(NotifyDeliveryMapper.class), sessions.getMapper(NotifyOutboxMapper.class), sessions.getMapper(NotifyAttemptMapper.class),
            sessions.getMapper(NotifyMessageMapper.class), sessions.getMapper(NotifyMessageRecipientMapper.class));
        configDao = new NotifyConfigDao(sessions.getMapper(NotifyChannelAccountMapper.class), sessions.getMapper(NotifySceneBindingMapper.class));
        receiptMapper = sessions.getMapper(NotifyProviderReceiptMapper.class);
        receipts = new NotifyProviderReceiptDao(receiptMapper);
        var redisConfig = new Config(); redisConfig.setThreads(2).setNettyThreads(2);
        redisConfig.useSingleServer().setAddress("redis://127.0.0.1:" + System.getProperty("notify.redis.integration.port"))
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        redis = Redisson.create(redisConfig); redis.getAtomicLong("owned-t22-provider-calls").delete();
        eventContext = new AnnotationConfigApplicationContext();
        InAppNotificationService inbox = new InAppNotificationService(dao, eventContext) {
            @Override
            public void pushRealtime(String id, InAppSnapshot snapshot, List<Long> users) {
                realtimeCalls.incrementAndGet();
                if (realtimeFails) throw new IllegalStateException("owned push failure");
            }
        };
        eventContext.registerBean(com.baomidou.dynamic.datasource.tx.DsTxEventListenerFactory.class);
        eventContext.registerBean(InAppCommittedPushUseCase.class, () -> new InAppCommittedPushUseCase(inbox));
        eventContext.registerBean(InAppCommittedPushListener.class);
        eventContext.refresh();
        InAppNotificationPort provider = new InAppNotificationPort() {
            public void persist(String id, InAppSnapshot snapshot, List<Long> users) {
                assertThat(TransactionContext.getXID()).as("local inbox writes share the result transaction").isNotNull();
                redis.getAtomicLong("owned-t22-provider-calls").incrementAndGet();
                inbox.persist(id, snapshot, users);
                duringProvider.run();
            }
            public void pushRealtime(String id, InAppSnapshot snapshot, List<Long> users) { inbox.pushRealtime(id, snapshot, users); }
            public void markEngagement(String id, Long user, boolean read) { inbox.markEngagement(id, user, read); }
        };
        ObjectProvider<InAppNotificationPort> providers = mock(ObjectProvider.class); when(providers.getIfAvailable()).thenReturn(provider);
        results = transactional(new NotifyDispatchResultUseCase(new NotifyDispatchResultService(dao)), NotifyDispatchResultUseCase.class);
        claims = transactional(new NotifyOutboxClaimUseCase(new NotifyOutboxClaimService(dao)), NotifyOutboxClaimUseCase.class);
        callbacks = transactional(new ProviderCallbackUseCase(new ProviderCallbackService(dao, results, receipts)), ProviderCallbackUseCase.class);
        dispatch = new DispatchNotificationService(dao, mock(NotifyClient.class), providers, mock(NotifyConfigDao.class), (key, limit, duration) -> true, results);
        db.update("insert into notify_intent(intent_id,app_id,scene_code,template_code,template_params_json,strategy,mode,status,create_time) values(?,'owned-t22','owned','owned','{}','ALL','ASYNC','QUEUED',utc_timestamp())", INTENT);
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,status,create_time) values(?,?,1,7,'IN_APP','PENDING',utc_timestamp())", DELIVERY, INTENT);
        db.update("insert into notify_outbox(outbox_id,intent_id,delivery_id,status,available_at,lease_owner,lease_token,lease_until,create_time) values(?,?,?,'PROCESSING',utc_timestamp(),'worker-a','token-a',timestampadd(second,60,utc_timestamp()),utc_timestamp())", OUTBOX, INTENT, DELIVERY);
    }

    @AfterEach
    void close() {
        try {
            commitFault.set(null);
            if (db != null) {
                db.execute("drop trigger if exists owned_t22_attempt_failure");
                db.execute("drop trigger if exists owned_t22_finish_conflict");
                db.execute("drop trigger if exists owned_t22_write_failure");
                db.execute("drop trigger if exists owned_t23_receipt_failure");
                db.execute("drop trigger if exists owned_t36_write_failure");
                db.update("delete from notify_channel_account where account_id=9238881 and config_key='owned-config'");
                db.update("delete from notify_provider_receipt where delivery_id in (select delivery_id from notify_delivery where intent_id=?)", INTENT);
                for (String table : List.of("notify_attempt", "notify_outbox", "notify_delivery", "notify_intent")) db.update("delete from " + table + " where intent_id=?", INTENT);
                db.update("delete from notify_message_recipient where message_id=?", INTENT);
                db.update("delete from notify_message where message_id=?", INTENT);
            }
        } finally {
            if (TransactionContext.getXID() == null) TransactionContext.removeSynchronizations();
            if (eventContext != null) eventContext.close();
            if (redis != null) redis.shutdown();
            if (routing != null) routing.destroy();
            if (pool != null) pool.close();
        }
    }

    @Test
    void attemptInsertFailureRollsBackDeliveryAndLeavesClaimRecoverable() {
        db.execute("create trigger owned_t22_attempt_failure before insert on notify_attempt for each row signal sqlstate '45000' set message_text='owned attempt failure'");
        assertThatThrownBy(() -> dispatch.dispatch(dao.outbox(OUTBOX))).isInstanceOf(RuntimeException.class);
        unchanged(1);
        assertThat(realtimeCalls.get()).isZero();
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isZero();
    }

    @Test
    void genericResultCannotCommitInAppDeliveryWithoutReservedInboxFact() {
        assertThatThrownBy(() -> results.complete(dao.outbox(OUTBOX), delivered()))
            .isInstanceOf(IllegalArgumentException.class);
        unchanged();
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isZero();
    }

    @Test
    void genericLocalFailureCannotCloseAnAlreadyReservedInAppAttempt() {
        NotifyOutbox lease = dao.outbox(OUTBOX);
        assertThat(results.beginInAppAttempt(lease)).isTrue();

        assertThatThrownBy(() -> results.complete(lease,
            new NotifyDispatchResultPort.Result("FAILED", "in-app", null,
                "LOCAL_DISPATCH_ERROR", "owned local error", 1)))
            .isInstanceOf(IllegalArgumentException.class);
        unchanged(1);
        assertThat(dao.outbox(OUTBOX).getLastErrorCode()).isEqualTo("IN_APP_ATTEMPT_RESERVED");
    }

    @Test
    void invalidLocalRecipientFailsTerminallyBeforeBudgetOrInboxWrite() {
        db.update("update notify_delivery set user_id=null where delivery_id=?", DELIVERY);

        dispatch.dispatch(dao.outbox(OUTBOX));

        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("FAILED");
        assertThat(dao.delivery(DELIVERY).getErrorCode()).isEqualTo("LOCAL_DISPATCH_ERROR");
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("DONE");
        assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isZero();
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isZero();
        assertThat(realtimeCalls.get()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"title,255,true", "title,256,false", "notice,10,true", "notice,11,false", "path,500,true"})
    void inAppSnapshotColumnLimitsAreCheckedBeforeBudget(String field, int length, boolean fits) {
        String value = "😀".repeat(length);
        switch (field) {
            case "title" -> db.update("update notify_intent set title_snapshot=? where intent_id=?", value, INTENT);
            case "notice" -> db.update("update notify_intent set template_params_json=? where intent_id=?",
                JsonUtils.toJsonString(Map.of("noticeType", value)), INTENT);
            case "path" -> db.update("update notify_intent set path_snapshot=? where intent_id=?", value, INTENT);
            default -> throw new IllegalArgumentException(field);
        }

        dispatch.dispatch(dao.outbox(OUTBOX));

        if (fits) {
            assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
            assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(1);
            String column = switch (field) { case "title" -> "title"; case "notice" -> "notice_type"; default -> "path"; };
            assertThat(db.queryForObject("select " + column + " from notify_message where message_id=?", String.class, INTENT))
                .isEqualTo(value);
        } else {
            assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("FAILED");
            assertThat(dao.delivery(DELIVERY).getErrorCode()).isEqualTo("LOCAL_DISPATCH_ERROR");
            assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("DONE");
            assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(1);
            assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isZero();
            assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isZero();
            assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isZero();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"notify_message:insert", "notify_message_recipient:insert",
        "notify_delivery:update", "notify_attempt:insert", "notify_outbox:update", "notify_intent:update"})
    void everyInAppWriteStageRollsBackMessageRecipientAndResult(String stage) {
        String[] parts = stage.split(":");
        String table = parts[0];
        String operation = parts[1];
        if ("notify_outbox".equals(table)) {
            db.execute("create trigger owned_t36_write_failure before update on notify_outbox for each row "
                + "begin if new.status='DONE' then signal sqlstate '45000' set message_text='owned result failure'; end if; end");
        } else {
            db.execute("create trigger owned_t36_write_failure before " + operation + " on " + table
                + " for each row signal sqlstate '45000' set message_text='owned result failure'");
        }
        assertThatThrownBy(() -> dispatch.dispatch(dao.outbox(OUTBOX))).isInstanceOf(RuntimeException.class);
        unchanged(1);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isZero();
        assertThat(realtimeCalls.get()).isZero();
    }

    @Test
    void longUnicodeBodyKeepsFullContentAndBoundedSummary() {
        String body = "A".repeat(999) + "😀" + "B".repeat(1200);
        db.update("update notify_intent set title_snapshot='owned title',content_snapshot=? where intent_id=?", body, INTENT);
        dispatch.dispatch(dao.outbox(OUTBOX));
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        String summary = db.queryForObject("select message from notify_message where message_id=?", String.class, INTENT);
        assertThat(summary).isEqualTo("A".repeat(999) + "😀");
        assertThat(summary.codePointCount(0, summary.length())).isEqualTo(1000);
        assertThat(db.queryForObject("select content from notify_message where message_id=?", String.class, INTENT)).isEqualTo(body);
    }

    @Test
    void afterCommitPushFailureKeepsCommittedFactAndDoesNotRetriggerOnDuplicate() {
        NotifyOutbox first = dao.outbox(OUTBOX);
        dispatch.dispatch(first);
        assertThat(realtimeCalls.get()).as("real DsTx AFTER_COMMIT listener was invoked").isEqualTo(1);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("DONE");
        dispatch.dispatch(first);
        assertThat(realtimeCalls.get()).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isEqualTo(1);
    }

    @Test
    void concurrentDifferentRecipientsShareOneMessageAndHaveDistinctInboxRelations() throws Exception {
        long secondDelivery = DELIVERY + 10;
        long secondOutbox = OUTBOX + 10;
        db.update("update notify_intent set template_params_json=? where intent_id=?",
            JsonUtils.toJsonString(Map.of("channels", List.of("IN_APP", "SMS"))), INTENT);
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,status,create_time) "
            + "values(?,?,2,8,'IN_APP','PENDING',utc_timestamp())", secondDelivery, INTENT);
        db.update("insert into notify_outbox(outbox_id,intent_id,delivery_id,status,available_at,lease_owner,lease_token,"
            + "lease_until,create_time) values(?,?,?,'PROCESSING',utc_timestamp(),'worker-b','token-b',"
            + "timestampadd(second,60,utc_timestamp()),utc_timestamp())", secondOutbox, INTENT, secondDelivery);
        var barrier = new CyclicBarrier(2);
        var connectionIds = new java.util.concurrent.ConcurrentSkipListSet<Long>();
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { dispatchWithPhysicalConnection(OUTBOX, barrier, connectionIds); return true; });
            var second = workers.submit(() -> { dispatchWithPhysicalConnection(secondOutbox, barrier, connectionIds); return true; });
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        }
        assertThat(connectionIds).hasSize(2);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isEqualTo(2);
        assertThat(db.queryForList("select user_id from notify_message_recipient where message_id=? order by user_id", Long.class, INTENT))
            .containsExactly(7L, 8L);
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isEqualTo(2);
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
        assertThat(JsonUtils.parseArray(db.queryForObject("select channels_json from notify_message where message_id=?",
            String.class, INTENT), String.class)).containsExactly("IN_APP", "SMS");
        assertThat(realtimeCalls.get()).isEqualTo(2);
        dispatch.dispatch(dao.outbox(OUTBOX));
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isEqualTo(2);
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isEqualTo(2);
    }

    @Test
    void existingEmptyNoticeTypeSnapshotAcceptsSecondRecipientOfSameIntent() {
        db.update("insert into notify_message(message_id,category,notice_type,channels_json,type,source,"
            + "title,message,content,path,create_time) values(?,'system','', '[\"IN_APP\"]',"
            + "'MESSAGE','BACKEND',null,null,null,null,utc_timestamp())", INTENT);
        db.update("insert into notify_message_recipient(message_recipient_id,message_id,user_id,create_time) "
            + "values(?,?,7,utc_timestamp())", INTENT + 100, INTENT);
        db.update("update notify_delivery set user_id=8 where delivery_id=?", DELIVERY);

        dispatch.dispatch(dao.outbox(OUTBOX));

        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isEqualTo(1);
        assertThat(db.queryForList("select user_id from notify_message_recipient where message_id=? order by user_id", Long.class, INTENT))
            .containsExactly(7L, 8L);
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isEqualTo(1);
    }

    @Test
    void finishZeroRollsBackDeliveryAttemptAndAggregate() {
        db.execute("create trigger owned_t22_finish_conflict after insert on notify_attempt for each row update notify_outbox set lease_token='conflict' where outbox_id=" + OUTBOX);
        assertThatThrownBy(() -> dispatch.dispatch(dao.outbox(OUTBOX))).isInstanceOf(RuntimeException.class);
        unchanged(1);
        assertThat(dao.outbox(OUTBOX).getLeaseToken()).isEqualTo("token-a");
    }

    @Test
    void expiredOwnerCannotRenewOrFinishWithoutAReclaim() {
        NotifyOutbox claimed = dao.outbox(OUTBOX);
        db.update("update notify_outbox set lease_until=timestampadd(second,-1,utc_timestamp()) where outbox_id=?", OUTBOX);
        assertThat(dao.renewOutbox(OUTBOX, "worker-a", "token-a")).isZero();
        claimed.setStatus("DONE"); assertThat(dao.finishOutbox(claimed)).isZero();
        unchanged();
    }

    @Test
    void providerCompletingAfterExpiryDoesNotReviveTheLease() {
        duringProvider = () -> db.update("update notify_outbox set lease_until=timestampadd(second,-1,utc_timestamp()) where outbox_id=?", OUTBOX);
        assertThatThrownBy(() -> dispatch.dispatch(dao.outbox(OUTBOX))).isInstanceOf(RuntimeException.class);
        unchanged(1);
    }

    @Test
    void successAndReclaimAfterRollbackKeepOneInboxFactAndOneCommittedAttempt() {
        db.execute("create trigger owned_t22_attempt_failure before insert on notify_attempt for each row signal sqlstate '45000' set message_text='owned attempt failure'");
        assertThatThrownBy(() -> dispatch.dispatch(dao.outbox(OUTBOX))).isInstanceOf(RuntimeException.class);
        db.execute("drop trigger owned_t22_attempt_failure");
        expire();
        var reclaimed = claims.claim("worker-b"); assertThat(reclaimed).hasSize(1);
        dispatch.dispatch(reclaimed.getFirst());
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.delivery(DELIVERY).getAttemptCount()).isEqualTo(1);
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("DONE");
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isEqualTo(1);
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).as("I/O can repeat after a DB rollback; no external exactly-once claim").isEqualTo(2);
        assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(2);
        assertThat(realtimeCalls.get()).isEqualTo(1);
    }

    @Test
    void reservedLastSlotCannotBeReenteredAndNextLeaseExhaustsWithoutPersist() {
        db.update("update notify_outbox set max_attempts=1 where outbox_id=?", OUTBOX);
        NotifyOutbox first = dao.outbox(OUTBOX);
        assertThat(results.beginInAppAttempt(first)).isTrue();
        assertThat(results.beginInAppAttempt(first)).isFalse();
        assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isZero();
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isZero();

        expire();
        var next = claims.claim("worker-b");
        assertThat(next).hasSize(1);
        assertThat(next.getFirst().getLastErrorCode()).as("claim returns an old candidate snapshot").isEqualTo("IN_APP_ATTEMPT_RESERVED");
        assertThat(dao.outbox(OUTBOX).getLastErrorCode()).isNull();
        dispatch.dispatch(next.getFirst());
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("FAILED");
        assertThat(dao.delivery(DELIVERY).getErrorCode()).isEqualTo("IN_APP_RETRY_EXHAUSTED");
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("DEAD_LETTER");
        assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isZero();
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isZero();
    }

    @Test
    void externalChannelClaimPreservesSameLiteralErrorCode() {
        db.update("update notify_delivery set channel='MAIL' where delivery_id=?", DELIVERY);
        db.update("update notify_outbox set status='READY',lease_owner=null,lease_token=null,lease_until=null,"
            + "last_error_code='IN_APP_ATTEMPT_RESERVED' where outbox_id=?", OUTBOX);

        var next = claims.claim("worker-b");

        assertThat(next).hasSize(1);
        assertThat(dao.outbox(OUTBOX).getLastErrorCode()).isEqualTo("IN_APP_ATTEMPT_RESERVED");
    }

    @Test
    void simultaneousSameLeaseReservationGetsOnlyOnePhysicalAttempt() throws Exception {
        db.update("update notify_outbox set max_attempts=1 where outbox_id=?", OUTBOX);
        NotifyOutbox lease = dao.outbox(OUTBOX);
        var barrier = new CyclicBarrier(2);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { barrier.await(5, TimeUnit.SECONDS); return results.beginInAppAttempt(lease); });
            var second = workers.submit(() -> { barrier.await(5, TimeUnit.SECONDS); return results.beginInAppAttempt(lease); });
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        }
        assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(1);
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("PROCESSING");
        dispatch.dispatch(lease);
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"BEFORE", "AFTER"})
    void uncertainBudgetCommitNeverEntersPersistBeforeReclaim(String phase) {
        db.update("update notify_outbox set max_attempts=2 where outbox_id=?", OUTBOX);
        commitFault.set(phase);
        assertThatThrownBy(() -> results.beginInAppAttempt(dao.outbox(OUTBOX))).isInstanceOf(RuntimeException.class);
        int durableBudget = "AFTER".equals(phase) ? 1 : 0;
        unchanged(durableBudget);
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isZero();
        assertThat(realtimeCalls.get()).isZero();
        expire();
        var next = claims.claim("worker-b");
        assertThat(next).hasSize(1);
        dispatch.dispatch(next.getFirst());
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(durableBudget + 1);
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BEFORE", "AFTER"})
    void uncertainResultCommitUsesDurableFactsToPreventDuplicateInbox(String phase) {
        db.update("update notify_outbox set max_attempts=2 where outbox_id=?", OUTBOX);
        duringProvider = () -> commitFault.set(phase);
        NotifyOutbox first = dao.outbox(OUTBOX);
        assertThatThrownBy(() -> dispatch.dispatch(first)).isInstanceOf(RuntimeException.class);
        assertThat(TransactionContext.getSynchronizations()).as("failed proxy commit cannot poison the worker thread").isEmpty();
        assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(1);
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isEqualTo(1);
        if ("AFTER".equals(phase)) {
            assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
            assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("DONE");
            dispatch.dispatch(first);
            assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isEqualTo(1);
        } else {
            unchanged(1);
            assertThat(realtimeCalls.get()).isZero();
            expire();
            var next = claims.claim("worker-b");
            assertThat(next).hasSize(1);
            duringProvider = () -> {};
            dispatch.dispatch(next.getFirst());
            assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(2);
            assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isEqualTo(2);
        }
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isEqualTo(1);
        // 首笔故障已经取证；下一笔验证正常提交后的线程恢复，不再注入故障。
        duringProvider = () -> {};
        int priorPushes = realtimeCalls.get();
        long nextDelivery = DELIVERY + 20, nextOutbox = OUTBOX + 20;
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,status,create_time) "
            + "values(?,?,2,8,'IN_APP','PENDING',utc_timestamp())", nextDelivery, INTENT);
        db.update("insert into notify_outbox(outbox_id,intent_id,delivery_id,status,available_at,lease_owner,lease_token,"
            + "lease_until,create_time) values(?,?,?,'PROCESSING',utc_timestamp(),'worker-c','token-c',"
            + "timestampadd(second,60,utc_timestamp()),utc_timestamp())", nextOutbox, INTENT, nextDelivery);
        // 新增 PENDING 投递必须同步恢复聚合为 PROCESSING；DELIVERED Intent 是旧快照矛盾形状。
        db.update("update notify_intent set status='PROCESSING' where intent_id=?", INTENT);
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("PROCESSING");
        dispatch.dispatch(dao.outbox(nextOutbox));
        assertThat(realtimeCalls.get()).as("later normal commit on the same thread must emit AFTER_COMMIT")
            .isEqualTo(priorPushes + 1);
        assertThat(db.queryForObject("select count(*) from notify_message_recipient where message_id=?", Integer.class, INTENT)).isEqualTo(2);
    }

    @Test
    void twoConcurrentClaimersOnlyDispatchOneCommittedResult() throws Exception {
        db.update("update notify_outbox set status='READY',lease_owner=null,lease_token=null,lease_until=null where outbox_id=?", OUTBOX);
        var barrier = new CyclicBarrier(2);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { barrier.await(5, TimeUnit.SECONDS); return claims.claim("worker-a"); });
            var second = workers.submit(() -> { barrier.await(5, TimeUnit.SECONDS); return claims.claim("worker-b"); });
            var all = new java.util.ArrayList<>(first.get(10, TimeUnit.SECONDS)); all.addAll(second.get(10, TimeUnit.SECONDS));
            assertThat(all).hasSize(1);
            dispatch.dispatch(all.getFirst()); dispatch.dispatch(all.getFirst());
        }
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isEqualTo(1);
        assertThat(dao.delivery(DELIVERY).getAttemptCount()).isEqualTo(1);
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("DONE");
    }

    @Test
    void oldWorkerCannotOverwriteAReclaimedAndCompletedResult() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        pauseFirstBeforeInAppPort(entered, release);
        NotifyOutbox firstLease = dao.outbox(OUTBOX);
        try (var workers = Executors.newSingleThreadExecutor()) {
            var first = workers.submit(() -> dispatch.dispatch(firstLease));
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue(); expire();
                var second = claims.claim("worker-b"); assertThat(second).hasSize(1);
                assertThat(second.getFirst().getLeaseToken()).isNotEqualTo(firstLease.getLeaseToken());
                dispatch.dispatch(second.getFirst());
            } finally { release.countDown(); }
            first.get(10, TimeUnit.SECONDS);
        }
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.delivery(DELIVERY).getAttemptCount()).isEqualTo(1);
        assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(1);
        assertThat(redis.getAtomicLong("owned-t22-provider-calls").get()).isEqualTo(1);
    }

    @Test
    void deliveredCallbackCannotBeOverwrittenByLateAcceptedResult() throws Exception {
        db.update("update notify_delivery set provider_key='owned-smtp',provider_message_id='owned-message',channel='MAIL' where delivery_id=?", DELIVERY);
        NotifyOutbox lease = dao.outbox(OUTBOX);
        callback("DELIVERED", "owned-message", "event-delivered");
        var deliveredAt = dao.delivery(DELIVERY).getDeliveredAt();
        results.complete(lease, new NotifyDispatchResultPort.Result("ACCEPTED", "owned-smtp", "owned-message", null, null, 1));
        callback("ACCEPTED", "owned-message", "event-late-accepted");
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.delivery(DELIVERY).getDeliveredAt()).isEqualTo(deliveredAt);
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("DONE");
        assertThat(db.queryForObject("select status from notify_attempt where delivery_id=?", String.class, DELIVERY)).isEqualTo("ACCEPTED");
    }

    @Test
    void concurrentResultsForDifferentDeliveriesLeaveCurrentAggregate() throws Exception {
        long otherDelivery = DELIVERY + 10, otherOutbox = OUTBOX + 10;
        db.update("update notify_delivery set channel='MAIL' where delivery_id=?", DELIVERY);
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,status,create_time) values(?,?,2,8,'MAIL','PENDING',utc_timestamp())", otherDelivery, INTENT);
        db.update("insert into notify_outbox(outbox_id,intent_id,delivery_id,status,available_at,lease_owner,lease_token,lease_until,create_time) values(?,?,?,'PROCESSING',utc_timestamp(),'worker-b','token-b',timestampadd(second,60,utc_timestamp()),utc_timestamp())", otherOutbox, INTENT, otherDelivery);
        var firstLease = dao.outbox(OUTBOX); var secondLease = dao.outbox(otherOutbox); var barrier = new CyclicBarrier(2);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { barrier.await(5, TimeUnit.SECONDS); results.complete(firstLease, delivered()); return true; });
            var second = workers.submit(() -> { barrier.await(5, TimeUnit.SECONDS); results.complete(secondLease, delivered()); return true; });
            assertThat(first.get(10, TimeUnit.SECONDS)).isTrue(); assertThat(second.get(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isEqualTo(2);
    }

    @Test
    void anyResultWriteFailureRollsBackAllOtherRows() {
        db.update("update notify_delivery set channel='MAIL' where delivery_id=?", DELIVERY);
        for (String table : List.of("notify_delivery", "notify_outbox", "notify_intent")) {
            db.execute("create trigger owned_t22_write_failure before update on " + table + " for each row signal sqlstate '45000' set message_text='owned write failure'");
            try {
                assertThatThrownBy(() -> results.complete(dao.outbox(OUTBOX), delivered())).isInstanceOf(RuntimeException.class);
                unchanged();
            } finally { db.execute("drop trigger owned_t22_write_failure"); }
        }
    }

    @Test
    void leaseExpiringWhileWaitingForIntentLockCannotRenewOrComplete() throws Exception {
        db.update("update notify_delivery set channel='MAIL' where delivery_id=?", DELIVERY);
        for (boolean renewal : List.of(true, false)) {
            db.update("update notify_outbox set lease_until=timestampadd(second,2,utc_timestamp()) where outbox_id=?", OUTBOX);
            NotifyOutbox lease = dao.outbox(OUTBOX);
            try (var connection = pool.getConnection(); var workers = Executors.newSingleThreadExecutor()) {
                connection.setAutoCommit(false);
                try (var lock = connection.prepareStatement("select intent_id from notify_intent where intent_id=? for update")) {
                    lock.setLong(1, INTENT); try (var row = lock.executeQuery()) { assertThat(row.next()).isTrue(); }
                }
                var started = new CountDownLatch(1);
                var waiting = workers.submit(() -> { started.countDown(); if (renewal) return results.renew(lease); results.complete(lease, delivered()); return false; });
                try {
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    while (dao.databaseNow().isBefore(lease.getLeaseUntil()) && System.nanoTime() < deadline) Thread.sleep(20);
                    assertThat(dao.databaseNow()).isAfterOrEqualTo(lease.getLeaseUntil());
                } finally { connection.rollback(); }
                assertThat(waiting.get(10, TimeUnit.SECONDS)).isFalse();
            }
            unchanged();
        }
    }

    @Test
    void expiredLeaseCannotSkipCancelOrDefer() {
        var lease = dao.outbox(OUTBOX); expire();
        for (var disposition : NotifyDispatchResultPort.Disposition.values()) results.settle(lease, disposition);
        unchanged();
    }

    @ParameterizedTest
    @CsvSource({
        "ACCEPTED,,5,ACCEPTED,DONE,ACCEPTED",
        "UNKNOWN,DISPATCH_ERROR,5,UNKNOWN,WAITING_RECEIPT,UNKNOWN",
        "FAILED,UNBOUND_CHANNEL,5,FAILED,DONE,FAILED",
        "FAILED,PROVIDER_UNAVAILABLE,5,PENDING,READY,PROCESSING",
        "FAILED,PROVIDER_UNAVAILABLE,1,FAILED,DEAD_LETTER,FAILED"
    })
    void retryReceiptAndFailClosedOutcomesKeepExistingSemantics(String status, String error, int maxAttempts,
            String expectedDelivery, String expectedOutbox, String expectedIntent) {
        db.update("update notify_delivery set channel='MAIL' where delivery_id=?", DELIVERY);
        db.update("update notify_outbox set max_attempts=? where outbox_id=?", maxAttempts, OUTBOX);
        results.complete(dao.outbox(OUTBOX), new NotifyDispatchResultPort.Result(status, "owned-provider", null, error, null, 1));
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo(expectedDelivery);
        assertThat(dao.delivery(DELIVERY).getErrorCode()).isEqualTo(error);
        assertThat(dao.delivery(DELIVERY).getAttemptCount()).isEqualTo(1);
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo(expectedOutbox);
        assertThat(dao.outbox(OUTBOX).getLeaseToken()).isNull();
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo(expectedIntent);
        assertThat(db.queryForObject("select status from notify_attempt where delivery_id=?", String.class, DELIVERY)).isEqualTo(status);
    }

    @Test
    void cancelledBeforeInAppTransactionCannotWriteInboxFact() throws Exception {
        var runtime = new org.namewta.notify.service.runtime.NotificationApplicationRuntimeService(
            dao, mock(org.namewta.system.api.UserService.class), dispatch, event -> {});
        var application = transactional(new org.namewta.notify.usecase.NotificationApplicationUseCase(runtime),
            org.namewta.notify.usecase.NotificationApplicationUseCase.class);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        pauseFirstBeforeInAppPort(entered, release);
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = worker.submit(() -> dispatch.dispatch(dao.outbox(OUTBOX)));
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                application.cancel(new org.namewta.notify.api.NotificationCancelCommand(String.valueOf(INTENT), "owned cancel"));
            } finally { release.countDown(); }
            pending.get(10, TimeUnit.SECONDS);
        }
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("CANCELLED");
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("CANCELLED");
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("DONE");
        assertThat(db.queryForObject("select count(*) from notify_attempt where delivery_id=?", Integer.class, DELIVERY)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_message where message_id=?", Integer.class, INTENT)).isZero();
    }

    @Test
    void callbackRevalidatesCorrelationAfterWaitingForCurrentResult() throws Exception {
        db.update("update notify_delivery set provider_key='owned-smtp',provider_message_id='old-message',channel='MAIL' where delivery_id=?", DELIVERY);
        var lookedUp = new CountDownLatch(1); var resume = new CountDownLatch(1);
        var observedDao = spy(dao);
        doAnswer(invocation -> {
            Object value = invocation.callRealMethod(); lookedUp.countDown(); await(resume); return value;
        }).when(observedDao).deliveriesByProvider("MAIL", "owned-smtp", "old-message", "");
        callbacks = transactional(new ProviderCallbackUseCase(new ProviderCallbackService(observedDao, results, receipts)), ProviderCallbackUseCase.class);
        try (var worker = Executors.newSingleThreadExecutor()) {
            var oldCallback = worker.submit(() -> { callback("DELIVERED", "old-message", "old-event"); return true; });
            try {
                assertThat(lookedUp.await(5, TimeUnit.SECONDS)).isTrue();
                results.complete(dao.outbox(OUTBOX), new NotifyDispatchResultPort.Result("ACCEPTED", "owned-smtp", "new-message", null, null, 1));
            } finally { resume.countDown(); }
            assertThatThrownBy(() -> oldCallback.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(org.namewta.common.core.exception.ServiceException.class);
        }
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("ACCEPTED");
        assertThat(dao.delivery(DELIVERY).getProviderMessageId()).isEqualTo("new-message");
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void callbackRollbackMustAllowSameEventRetry() throws Exception {
        db.update("update notify_delivery set provider_key='owned-smtp',provider_message_id='owned-message',channel='MAIL' where delivery_id=?", DELIVERY);
        db.execute("create trigger owned_t22_write_failure before update on notify_intent for each row signal sqlstate '45000' set message_text='owned callback aggregate failure'");
        assertThatThrownBy(() -> callback("DELIVERED", "owned-message", "retry-event")).isInstanceOf(RuntimeException.class);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("PENDING");
        db.execute("drop trigger owned_t22_write_failure");
        callback("DELIVERED", "owned-message", "retry-event");
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void sameEventFromDifferentAccountsMustNotCollide() throws Exception {
        db.update("update notify_delivery set provider_key='owned-smtp',provider_message_id='owned-message',channel='MAIL' where delivery_id=?", DELIVERY);
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,status,provider_key,provider_message_id,create_time) values(?,?,2,8,'MAIL','PENDING','other-account','owned-message',utc_timestamp())", DELIVERY + 10, INTENT);
        callback("DELIVERED", "owned-message", "shared-event");
        signedCallback("other-account", "DELIVERED", "owned-message", "shared-event");
        assertThat(dao.delivery(DELIVERY + 10).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void duplicateEventWithChangedFactsMustBeRejected() throws Exception {
        db.update("update notify_delivery set provider_key='owned-smtp',provider_message_id='owned-message',channel='MAIL' where delivery_id=?", DELIVERY);
        callback("ACCEPTED", "owned-message", "conflicting-event");
        assertThatThrownBy(() -> callback("DELIVERED", "owned-message", "conflicting-event"))
            .isInstanceOf(org.namewta.common.core.exception.ServiceException.class);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void twoInstancesRaceOnTheSameEventAndOnlyRefreshOnce() throws Exception {
        prepareCallbackDelivery();
        var barrier = new CyclicBarrier(2);
        var observedReceipts = spy(receipts);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            barrier.await(5, TimeUnit.SECONDS);
            return result;
        }).when(observedReceipts).find(anyString());
        var observedService = spy(new NotifyDispatchResultService(dao));
        var observedResults = transactional(new NotifyDispatchResultUseCase(observedService), NotifyDispatchResultUseCase.class);
        var first = transactional(new ProviderCallbackUseCase(new ProviderCallbackService(dao, observedResults, observedReceipts)), ProviderCallbackUseCase.class);
        var second = transactional(new ProviderCallbackUseCase(new ProviderCallbackService(dao, observedResults, observedReceipts)), ProviderCallbackUseCase.class);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var a = workers.submit(() -> { callOn(first, "MAIL", "owned-smtp", "DELIVERED", "owned-message", "race-event", ""); return true; });
            var b = workers.submit(() -> { callOn(second, "MAIL", "owned-smtp", "DELIVERED", "owned-message", "race-event", ""); return true; });
            assertThat(a.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(b.get(10, TimeUnit.SECONDS)).isTrue();
        }
        verify(observedService, times(1)).refreshAggregate(INTENT);
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void earlyCallbackDoesNotConsumeReceiptAndCanRetryAfterDispatchResult() throws Exception {
        assertThatThrownBy(() -> callback("DELIVERED", "owned-message", "early-event"))
            .isInstanceOf(org.namewta.common.core.exception.ServiceException.class);
        assertThat(receiptCount()).isZero();
        prepareCallbackDelivery();
        callback("DELIVERED", "owned-message", "early-event");
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void receiptInsertFailureAndStatusCasMissBothRollbackAndAllowRetry() throws Exception {
        prepareCallbackDelivery();
        db.execute("create trigger owned_t23_receipt_failure before insert on notify_provider_receipt for each row signal sqlstate '45000' set message_text='owned receipt failure'");
        assertThatThrownBy(() -> callback("DELIVERED", "owned-message", "storage-event")).isInstanceOf(RuntimeException.class);
        assertThat(receiptCount()).isZero();
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("PENDING");
        db.execute("drop trigger owned_t23_receipt_failure");
        var failingDao = spy(dao);
        doReturn(0).when(failingDao).updateDeliveryStatus(eq(DELIVERY), eq("PENDING"), any(org.namewta.notify.domain.entity.NotifyDelivery.class));
        var failing = transactional(new ProviderCallbackUseCase(new ProviderCallbackService(failingDao, results, receipts)), ProviderCallbackUseCase.class);
        assertThatThrownBy(() -> callOn(failing, "MAIL", "owned-smtp", "DELIVERED", "owned-message", "storage-event", ""))
            .isInstanceOf(org.namewta.common.core.exception.ServiceException.class);
        assertThat(receiptCount()).isZero();
        callback("DELIVERED", "owned-message", "storage-event");
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void sharedMessageRequiresTargetAndNeverUpdatesOtherRecipients() throws Exception {
        prepareCallbackDelivery();
        db.update("update notify_delivery set target_value='first@example.test' where delivery_id=?", DELIVERY);
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,target_value,status,provider_key,provider_message_id,create_time) values(?,?,2,8,'MAIL','second@example.test','PENDING','owned-smtp','owned-message',utc_timestamp())", DELIVERY + 10, INTENT);
        assertThatThrownBy(() -> callback("DELIVERED", "owned-message", "shared-message-event"))
            .isInstanceOf(org.namewta.common.core.exception.ServiceException.class);
        assertThat(receiptCount()).isZero();
        callOn(callbacks, "MAIL", "owned-smtp", "DELIVERED", "owned-message", "shared-message-event", "first@example.test");
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.delivery(DELIVERY + 10).getStatus()).isEqualTo("PENDING");
        assertThat(receiptCount()).isEqualTo(1);
        assertThatThrownBy(() -> callOn(callbacks, "MAIL", "owned-smtp", "DELIVERED", "owned-message", "shared-message-event", "second@example.test"))
            .isInstanceOf(org.namewta.common.core.exception.ServiceException.class);
    }

    @Test
    void sameAccountKeyAndEventAcrossChannelsRemainIndependent() throws Exception {
        prepareCallbackDelivery();
        db.update("insert into notify_delivery(delivery_id,intent_id,recipient_id,user_id,channel,status,provider_key,provider_message_id,create_time) values(?,?,2,8,'SMS','PENDING','owned-smtp','owned-message',utc_timestamp())", DELIVERY + 10, INTENT);
        callback("DELIVERED", "owned-message", "channel-event");
        callOn(callbacks, "SMS", "owned-smtp", "DELIVERED", "owned-message", "channel-event", "");
        assertThat(receiptCount()).isEqualTo(2);
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void restartedJvmsSeeCommittedReceiptAndRejectChangedFacts() throws Exception {
        prepareCallbackDelivery();
        assertThat(probe("ACCEPTED")).isZero();
        assertThat(probe("ACCEPTED")).isZero();
        assertThat(probe("DELIVERED")).isEqualTo(3);
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("ACCEPTED");
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void receiptAuditFieldsVersionAndLogicalDeleteFollowTheBaseContract() throws Exception {
        prepareCallbackDelivery();
        callback("ACCEPTED", "owned-message", "base-event");
        Long id = db.queryForObject("select provider_receipt_id from notify_provider_receipt where delivery_id=?", Long.class, DELIVERY);
        var current = receiptMapper.selectById(id);
        var stale = receiptMapper.selectById(id);
        assertThat(current.getCreateTime()).isNotNull();
        assertThat(current.getCreateDept()).isEqualTo(-1L);
        assertThat(current.getCreateBy()).isEqualTo(-1L);
        assertThat(current.getUpdateBy()).isEqualTo(-1L);
        assertThat(current.getVersion()).isZero();
        assertThat(receiptMapper.updateById(current)).isEqualTo(1);
        assertThat(receiptMapper.updateById(stale)).isZero();
        assertThat(receiptMapper.selectById(id).getVersion()).isEqualTo(1);
        assertThat(receiptMapper.selectById(id).getUpdateTime()).isNotNull();
        assertThat(receiptMapper.deleteById(id)).isEqualTo(1);
        assertThat(receiptMapper.selectById(id)).isNull();
        assertThat(db.queryForObject("select del_flag from notify_provider_receipt where provider_receipt_id=?", String.class, id)).isEqualTo("1");
        // 即使有人绕过应用逻辑删除凭据，唯一键仍阻止同事件被再次消费。
        assertThatThrownBy(() -> callback("ACCEPTED", "owned-message", "base-event"))
            .isInstanceOf(org.namewta.common.core.exception.ServiceException.class);
        assertThat(receiptCount()).isEqualTo(1);
    }

    @Test
    void freshPresetsAreDisabledAndDeletedAccountNamespaceRemainsReserved() {
        var all = configDao.listAccounts(null);
        assertThat(all).hasSize(5);
        for (var account : all) {
            assertThat(account.getEnabled()).isEqualTo("N");
            assertThat(account.getMailPass()).isEmpty();
            assertThat(account.getAccessKeySecret()).isEmpty();
            assertThat(account.getVersion()).isZero();
            assertThat(account.getDelFlag()).isEqualTo("0");
            assertThat(configDao.existsNamespace(account.getChannel(), account.getConfigKey())).isTrue();
        }
        assertThat(configDao.listBindings()).hasSize(10).allSatisfy(binding -> assertThat(binding.getAccountId()).isNull());
        var account = configDao.findAccount("MAIL", "mail-qq");
        account.setAccountId(9238881L); account.setConfigKey("owned-config");
        account.setMailUser("owned@example.test"); account.setMailFrom("owned@example.test"); account.setMailPass("owned-test-only");
        assertThat(configDao.insert(account)).isEqualTo(1);
        var current = configDao.findAccount(9238881L); var stale = configDao.findAccount(9238881L);
        assertThat(configDao.update(current)).isEqualTo(1);
        assertThat(configDao.update(stale)).isZero();
        var service = new org.namewta.notify.service.NotifyConfigService(configDao, mock(org.namewta.notify.port.SmsBlendRegistryPort.class));
        assertThat(service.resolve("owned-config")).isNull();
        assertThat(service.changeStatus(9238881L, "Y")).isEqualTo(1);
        assertThat(service.resolve("owned-config").getHost()).isEqualTo("smtp.qq.com");
        assertThat(service.resolve("owned-config").getPort()).isEqualTo(465);
        assertThat(service.removeAccount(9238881L)).isEqualTo(1);
        assertThat(configDao.findAccount(9238881L)).isNull();
        assertThat(service.resolve("owned-config")).isNull();
        assertThat(configDao.existsNamespace("MAIL", "owned-config")).isTrue();
        var bo = new org.namewta.notify.domain.bo.NotifyChannelAccountBo(); bo.setChannel("MAIL"); bo.setConfigKey("owned-config");
        assertThatThrownBy(() -> service.addAccount(bo)).isInstanceOf(org.namewta.common.core.exception.ServiceException.class);
        assertThat(db.queryForObject("select del_flag from notify_channel_account where account_id=9238881", String.class)).isEqualTo("1");
    }

    @ParameterizedTest
    @CsvSource({"BEFORE,0,PENDING", "AFTER,1,DELIVERED"})
    void commitDisconnectOrLostAcknowledgementCanRetryWithoutLosingTheEvent(String phase, int committed, String status) throws Exception {
        prepareCallbackDelivery();
        commitFault.set(phase);
        assertThatThrownBy(() -> callback("DELIVERED", "owned-message", "commit-event")).isInstanceOf(Exception.class);
        assertThat(commitFault.get()).as("failure was injected at JDBC commit").isNull();
        assertThat(receiptCount()).isEqualTo(committed);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo(status);
        callback("DELIVERED", "owned-message", "commit-event");
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void realHttpCallbackVerifiesRawBodyAndReturnsRetryableStatus() throws Exception {
        try (var http = new CallbackHttp(callbacks)) {
            String body = " \r\n" + JsonUtils.toJsonString(Map.of("eventId", "<b>http-event</b>", "timestamp", java.time.Instant.now().toString(),
                "providerKey", "owned-smtp", "providerMessageId", "owned-message", "status", "DELIVERED")) + " \n";
            assertThat(http.send(body, sign(body), "application/json").statusCode()).isEqualTo(503);
            assertThat(receiptCount()).isZero();
            prepareCallbackDelivery();
            assertThat(http.send(body, sign(body), "application/json").statusCode()).isEqualTo(200);
            assertThat(http.send(body, sign(body), "application/json").statusCode()).isEqualTo(200);
            assertThat(receiptCount()).isEqualTo(1);
            assertThat(dao.intent(INTENT).getStatus()).isEqualTo("DELIVERED");
            assertThat(http.send(body + " ", sign(body), "application/json").statusCode()).isEqualTo(401);
            String conflict = body.replace("DELIVERED", "FAILED");
            assertThat(http.send(conflict, sign(conflict), "application/json").statusCode()).isEqualTo(409);
            String stale = JsonUtils.toJsonString(Map.of("eventId", "stale", "timestamp", java.time.Instant.now().minusSeconds(600).toString(),
                "providerKey", "owned-smtp", "providerMessageId", "owned-message", "status", "DELIVERED"));
            assertThat(http.send(stale, sign(stale), "application/json").statusCode()).isEqualTo(400);
            assertThat(http.send(body, sign(body), "text/plain").statusCode()).isEqualTo(415);
            assertThat(http.send("x".repeat(2 * 1024 * 1024 + 1), "00", "application/json").statusCode()).isEqualTo(413);
            assertThat(receiptCount()).isEqualTo(1);
        }
    }

    @Test
    void callbackDatabaseFailureRemainsRetryableWithTheProductionGlobalExceptionHandler() throws Exception {
        prepareCallbackDelivery();
        db.update("update notify_delivery set target_value='owned-recipient@example.test' where delivery_id=?", DELIVERY);
        try (var http = new CallbackHttp(callbacks, true)) {
            String body = JsonUtils.toJsonString(Map.of("target", "owned-recipient@example.test", "eventId", "http-rollback-event", "timestamp", java.time.Instant.now().toString(),
                "providerKey", "owned-smtp", "providerMessageId", "owned-message", "status", "DELIVERED"));
            db.execute("create trigger owned_t22_write_failure before update on notify_intent for each row signal sqlstate '45000' set message_text='owned http transaction failure'");
            var failed = http.send(body, sign(body), "application/json");
            assertThat(failed.statusCode()).isEqualTo(503);
            assertThat(failed.body()).doesNotContain("owned http transaction failure");
            assertThat(receiptCount()).isZero();
            assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("PENDING");
            db.execute("drop trigger owned_t22_write_failure");
            var retried = http.send(body, sign(body), "application/json");
            assertThat(retried.statusCode()).isEqualTo(200);
            assertThat(JsonUtils.parseMap(retried.body()).get("code")).isEqualTo(200);
            assertThat(receiptCount()).isEqualTo(1);
            assertThat(http.events).isNotEmpty();
            assertThat(JsonUtils.toJsonString(http.events)).doesNotContain("owned-recipient@example.test", "http-rollback-event", sign(body));
        }
    }

    @org.springframework.context.annotation.Configuration
    @org.springframework.web.servlet.config.annotation.EnableWebMvc
    static class CallbackWebConfiguration { }

    private static final class CallbackHttp implements AutoCloseable {
        private final org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(new java.net.InetSocketAddress("127.0.0.1", 0));
        private final org.springframework.web.context.support.AnnotationConfigWebApplicationContext context = new org.springframework.web.context.support.AnnotationConfigWebApplicationContext();
        private final java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().build();
        private final int port;
        final java.util.List<Map<String, Object>> events = new java.util.concurrent.CopyOnWriteArrayList<>();

        CallbackHttp(ProviderCallbackUseCase useCase) throws Exception { this(useCase, false); }
        CallbackHttp(ProviderCallbackUseCase useCase, boolean withGlobalHandler) throws Exception {
            try {
                var controller = new org.namewta.notify.controller.anonymous.ProviderCallbackController(useCase);
                org.springframework.test.util.ReflectionTestUtils.setField(controller, "secret", "owned-t22-only");
                var handler = new org.eclipse.jetty.ee11.servlet.ServletContextHandler(); handler.setContextPath("/");
                context.setServletContext(handler.getServletContext());
                context.register(CallbackWebConfiguration.class);
                context.addBeanFactoryPostProcessor(factory -> {
                    factory.registerSingleton("providerCallbackController", controller);
                    if (withGlobalHandler) factory.registerSingleton("globalExceptionHandler", new org.namewta.common.web.handler.GlobalExceptionHandler());
                });
                context.refresh();
                handler.addFilter(new org.eclipse.jetty.ee11.servlet.FilterHolder(new org.namewta.common.web.filter.RepeatableFilter(2 * 1024 * 1024)), "/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));
                handler.addFilter(new org.eclipse.jetty.ee11.servlet.FilterHolder(new org.namewta.common.web.logging.SysLogFilter(1024, 2 * 1024 * 1024, events::add)), "/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));
                handler.addFilter(new org.eclipse.jetty.ee11.servlet.FilterHolder(new org.namewta.common.web.filter.XssFilter(new org.namewta.common.web.config.properties.XssProperties(), 2 * 1024 * 1024)), "/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));
                handler.addServlet(new org.eclipse.jetty.ee11.servlet.ServletHolder(new org.springframework.web.servlet.DispatcherServlet(context)), "/");
                server.setHandler(handler); server.start();
                port = ((org.eclipse.jetty.server.ServerConnector) server.getConnectors()[0]).getLocalPort();
            } catch (Exception failure) { close(); throw failure; }
        }

        java.net.http.HttpResponse<String> send(String body, String signature, String contentType) throws Exception {
            return client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + port + "/notify/callback/MAIL"))
                .timeout(java.time.Duration.ofSeconds(10)).header("Content-Type", contentType).header("X-Notify-Signature", signature)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        }
        @Override public void close() throws Exception { try { server.stop(); } finally { context.close(); client.close(); } }
    }

    private static String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec("owned-t22-only".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private int probe(String status) throws Exception {
        var output = java.nio.file.Files.createTempFile("owned-notify-receipt-probe-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(java.nio.file.Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-Xmx128m", "-XX:ActiveProcessorCount=2",
                "-Dnotify.mysql.integration.username=" + System.getProperty("notify.mysql.integration.username", "root"),
                "-cp", System.getProperty("java.class.path"),
                "org.namewta.test.notify.NotifyCallbackProcessProbe", System.getProperty("notify.mysql.integration.url"), status)
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("owned callback JVM exits").isTrue();
            assertThat(java.nio.file.Files.readString(output)).contains("OWNED_CALLBACK_PROBE");
            return process.exitValue();
        } finally {
            if (process != null && process.isAlive()) { process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS); }
            java.nio.file.Files.deleteIfExists(output);
        }
    }

    private int receiptCount() {
        return db.queryForObject("select count(*) from notify_provider_receipt where delivery_id in (select delivery_id from notify_delivery where intent_id=?)", Integer.class, INTENT);
    }

    @Test
    void nativeSmsQueryUsesShortClaimAndResultTransactionsWithoutSending() throws Exception {
        prepareNativeSms();
        AtomicInteger queried = new AtomicInteger();
        var useCase = nativeQueries(dao, (account, delivery, now) -> {
            assertThat(TransactionContext.getXID()).as("native HTTP query outside transaction").isNull();
            assertThat(account.getConfigKey()).isEqualTo("owned-config");
            queried.incrementAndGet();
            return org.namewta.common.sms.notify.SmsDeliveryQueryClient.Status.DELIVERED;
        });
        var claimed = useCase.claim();
        assertThat(claimed).isNotNull();
        assertThat(dao.delivery(DELIVERY).getReceiptQueryAt()).isEqualTo(claimed.getReceiptQueryAt());
        assertThat(useCase.claim()).isNull();
        useCase.query(claimed);
        assertThat(queried).hasValue(1);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(receiptCount()).isEqualTo(1);
        callbacks.confirm("tencent", "owned-config", "owned-message", "13800000000", "DELIVERED");
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("PROCESSING");
        assertThat(dao.delivery(DELIVERY).getAttemptCount()).isZero();
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isZero();
        assertThatThrownBy(() -> callOn(callbacks, "SMS", "owned-config", "DELIVERED", "owned-message", "native-sms:untrusted", "13800000000"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("保留前缀");
    }

    @Test
    void twoNativeWorkersReserveOnlyOneDueSnapshotAndRecoverAbandonedClaim() throws Exception {
        prepareNativeSms();
        var bothRead = new CountDownLatch(2);
        var observed = spy(dao);
        doAnswer(call -> {
            var candidate = call.callRealMethod(); bothRead.countDown(); await(bothRead); return candidate;
        }).when(observed).smsReceiptCandidate(any());
        var first = nativeQueries(observed, (a, d, n) -> org.namewta.common.sms.notify.SmsDeliveryQueryClient.Status.WAITING);
        var second = nativeQueries(observed, (a, d, n) -> org.namewta.common.sms.notify.SmsDeliveryQueryClient.Status.WAITING);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var a = workers.submit(first::claim); var b = workers.submit(second::claim);
            var claimedA = a.get(10, TimeUnit.SECONDS); var claimedB = b.get(10, TimeUnit.SECONDS);
            assertThat((claimedA == null ? 0 : 1) + (claimedB == null ? 0 : 1)).isEqualTo(1);
        }
        var restored = nativeQueries(dao, (a, d, n) -> org.namewta.common.sms.notify.SmsDeliveryQueryClient.Status.WAITING);
        assertThat(restored.claim()).isNull();
        db.update("update notify_delivery set receipt_query_at=timestampadd(second,-1,utc_timestamp()) where delivery_id=?", DELIVERY);
        assertThat(restored.claim()).isNotNull();
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("ACCEPTED");
        assertThat(receiptCount()).isZero();
    }

    @Test
    void nativeReceiptWriteFailureRollsBackAndSameResultRetriesAfterReservation() {
        prepareNativeSms();
        var useCase = nativeQueries(dao, (a, d, n) -> org.namewta.common.sms.notify.SmsDeliveryQueryClient.Status.DELIVERED);
        var claimed = useCase.claim();
        db.execute("create trigger owned_t23_receipt_failure before insert on notify_provider_receipt for each row signal sqlstate '45000' set message_text='owned native receipt failure'");
        assertThatThrownBy(() -> useCase.query(claimed)).isInstanceOf(RuntimeException.class);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("ACCEPTED");
        assertThat(receiptCount()).isZero();
        assertThat(useCase.claim()).isNull();
        db.execute("drop trigger owned_t23_receipt_failure");
        db.update("update notify_delivery set receipt_query_at=timestampadd(second,-1,utc_timestamp()) where delivery_id=?", DELIVERY);
        useCase.query(useCase.claim());
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
        assertThat(receiptCount()).isEqualTo(1);
    }

    @Test
    void nativeQueryExcludesDisabledMissingIdentityMailAndExpiredDeliveries() {
        prepareNativeSms();
        var provider = mock(org.namewta.notify.port.SmsReceiptProviderPort.class);
        var useCase = nativeQueries(dao, provider);
        db.update("update notify_channel_account set enabled='N' where account_id=9238881");
        assertThat(useCase.claim()).isNull();
        db.update("update notify_channel_account set enabled='Y' where account_id=9238881");
        db.update("update notify_delivery set provider_message_id=null where delivery_id=?", DELIVERY);
        assertThat(useCase.claim()).isNull();
        db.update("update notify_delivery set provider_message_id='owned-message',channel='MAIL' where delivery_id=?", DELIVERY);
        assertThat(useCase.claim()).isNull();
        db.update("update notify_delivery set channel='SMS',accepted_at=timestampadd(hour,-72,utc_timestamp()) where delivery_id=?", DELIVERY);
        assertThat(useCase.claim()).isNull();
        db.update("update notify_delivery set accepted_at=timestampadd(second,-120,utc_timestamp()) where delivery_id=?", DELIVERY);
        var claimed = useCase.claim(); assertThat(claimed).isNotNull();
        db.update("update notify_channel_account set enabled='N' where account_id=9238881");
        useCase.query(claimed);
        db.update("update notify_channel_account set enabled='Y',del_flag='1' where account_id=9238881");
        useCase.query(claimed);
        verifyNoInteractions(provider);
        assertThat(receiptCount()).isZero();
    }

    @Test
    void nativeQueryCredentialEditOrReadFailureDoesNotApplyResult() {
        prepareNativeSms();
        var useCase = nativeQueries(dao, (a, d, n) -> {
            db.update("update notify_channel_account set version=version+1 where account_id=9238881");
            return org.namewta.common.sms.notify.SmsDeliveryQueryClient.Status.DELIVERED;
        });
        useCase.query(useCase.claim());
        assertThat(receiptCount()).isZero();
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("ACCEPTED");
        db.update("update notify_delivery set receipt_query_at=timestampadd(second,-1,utc_timestamp()) where delivery_id=?", DELIVERY);
        var failing = nativeQueries(dao, (a, d, n) -> { throw new IllegalStateException("owned query timeout"); });
        var claim = failing.claim();
        assertThatThrownBy(() -> failing.query(claim)).isInstanceOf(IllegalStateException.class);
        assertThat(failing.claim()).isNull();
        assertThat(receiptCount()).isZero();
        assertThat(dao.delivery(DELIVERY).getAttemptCount()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"BEFORE,0,ACCEPTED", "AFTER,1,DELIVERED"})
    void nativeReceiptCommitFailureCanRetrySameInternalIdentity(String fault, int count, String status) {
        prepareNativeSms();
        commitFault.set(fault);
        assertThatThrownBy(() -> callbacks.confirm("tencent", "owned-config", "owned-message", "13800000000", "DELIVERED"))
            .isInstanceOf(RuntimeException.class);
        assertThat(receiptCount()).isEqualTo(count);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo(status);
        callbacks.confirm("tencent", "owned-config", "owned-message", "13800000000", "DELIVERED");
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("DELIVERED");
    }

    private org.namewta.notify.usecase.SmsReceiptQueryUseCase nativeQueries(NotifyNotificationDao persistence,
            org.namewta.notify.port.SmsReceiptProviderPort provider) {
        return transactional(new org.namewta.notify.usecase.SmsReceiptQueryUseCase(
            new org.namewta.notify.service.runtime.SmsReceiptQueryService(persistence, configDao, provider, callbacks)),
            org.namewta.notify.usecase.SmsReceiptQueryUseCase.class);
    }

    private void prepareNativeSms() {
        db.update("insert into notify_channel_account(account_id,channel,config_key,enabled,supplier,access_key_id,access_key_secret,sdk_app_id,create_time) values(9238881,'SMS','owned-config','Y','tencent','fixture-key','fixture-secret','1400000000',utc_timestamp())");
        db.update("update notify_delivery set channel='SMS',status='ACCEPTED',provider_key='owned-config',provider_message_id='owned-message',target_value='13800000000',accepted_at=timestampadd(second,-120,utc_timestamp()) where delivery_id=?", DELIVERY);
    }

    private void prepareCallbackDelivery() {
        db.update("update notify_delivery set provider_key='owned-smtp',provider_message_id='owned-message',channel='MAIL' where delivery_id=?", DELIVERY);
    }

    private NotifyDispatchResultPort.Result delivered() {
        return new NotifyDispatchResultPort.Result("DELIVERED", "in-app", null, null, null, 1);
    }
    private void expire() { db.update("update notify_outbox set lease_until=timestampadd(second,-1,utc_timestamp()) where outbox_id=?", OUTBOX); }
    private void callback(String status, String message, String event) throws Exception {
        signedCallback("owned-smtp", status, message, event);
    }
    private void signedCallback(String account, String status, String message, String event) throws Exception {
        callOn(callbacks, "MAIL", account, status, message, event, "");
    }
    private static void callOn(ProviderCallbackUseCase useCase, String channel, String account, String status, String message, String event, String target) throws Exception {
        String body = JsonUtils.toJsonString(Map.of("eventId", event, "timestamp", java.time.Instant.now().toString(),
            "providerKey", account, "providerMessageId", message, "status", status, "target", target));
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec("owned-t22-only".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        useCase.apply(channel, HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8))), body, "owned-t22-only");
    }
    private static void await(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Owned worker barrier timeout"); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
    }

    private void dispatchWithPhysicalConnection(long outboxId, CyclicBarrier barrier,
                                                java.util.Set<Long> connectionIds) throws Exception {
        try (var held = pool.getConnection(); var statement = held.createStatement();
             var row = statement.executeQuery("select connection_id()")) {
            assertThat(row.next()).isTrue();
            connectionIds.add(row.getLong(1));
            barrier.await(5, TimeUnit.SECONDS);
            dispatch.dispatch(dao.outbox(outboxId));
        }
    }

    @SuppressWarnings("unchecked")
    private void pauseFirstBeforeInAppPort(CountDownLatch entered, CountDownLatch release) {
        ObjectProvider<InAppNotificationPort> original = (ObjectProvider<InAppNotificationPort>)
            org.springframework.test.util.ReflectionTestUtils.getField(dispatch, "inAppPort");
        InAppNotificationPort port = original.getIfAvailable();
        ObjectProvider<InAppNotificationPort> blocked = mock(ObjectProvider.class);
        AtomicInteger visits = new AtomicInteger();
        when(blocked.getIfAvailable()).thenAnswer(invocation -> {
            if (visits.incrementAndGet() == 1) { entered.countDown(); await(release); }
            return port;
        });
        org.springframework.test.util.ReflectionTestUtils.setField(dispatch, "inAppPort", blocked);
    }

    private void unchanged() { unchanged(0); }

    private void unchanged(int expectedBudget) {
        assertThat(dao.delivery(DELIVERY).getStatus()).isEqualTo("PENDING");
        assertThat(dao.delivery(DELIVERY).getAttemptCount()).isZero();
        assertThat(dao.outbox(OUTBOX).getStatus()).isEqualTo("PROCESSING");
        assertThat(dao.outbox(OUTBOX).getAttemptCount()).isEqualTo(expectedBudget);
        assertThat(dao.intent(INTENT).getStatus()).isEqualTo("QUEUED");
        assertThat(db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, INTENT)).isZero();
    }

    private static <T> T transactional(T target, Class<T> type) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return type.cast(proxy.getProxy());
    }
}
