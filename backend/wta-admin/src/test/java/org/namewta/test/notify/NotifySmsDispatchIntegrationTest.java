package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.handler.InjectionMetaObjectHandler;
import org.namewta.common.notify.core.NotifyDispatcher;
import org.namewta.common.notify.event.NotifyDeliveryEvent;
import org.namewta.common.notify.idempotency.NotifyIdempotencyCoordinator;
import org.namewta.common.notify.idempotency.NotifyIdempotencyProperties;
import org.namewta.common.notify.idempotency.NotifyIdempotencyStore;
import org.namewta.common.notify.idempotency.RedisNotifyIdempotencyStore;
import org.namewta.common.notify.model.*;
import org.namewta.common.notify.registry.NotifyChannelRegistry;
import org.namewta.common.sms.notify.*;
import org.namewta.notify.api.*;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.mapper.*;
import org.namewta.notify.port.NotifyQuotaPort;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotifyOutboxClaimService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.namewta.notify.service.runtime.NotifyDispatchResultService;
import org.namewta.notify.adapter.store.RedisNotifyQuotaAdapter;
import org.namewta.notify.service.runtime.NotificationMonitorService;
import org.namewta.notify.usecase.NotificationMonitorUseCase;
import org.namewta.notify.usecase.NotificationApplicationUseCase;
import org.namewta.notify.usecase.NotifyDispatchResultUseCase;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.provider.factory.BaseProviderFactory;
import org.dromara.sms4j.provider.factory.ProviderFactoryHolder;
import org.dromara.sms4j.tencent.config.TencentConfig;
import org.dromara.sms4j.tencent.config.TencentFactory;
import org.namewta.notify.adapter.provider.Sms4jBlendRegistry;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 隔离 MySQL/Redis 的提交、规划、Dispatcher、SMS Adapter 和结果事务真实链路。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.sms.integration", matches = "true")
@Execution(ExecutionMode.SAME_THREAD)
class NotifySmsDispatchIntegrationTest {
    private static final long SEEDED_SMS_BINDING_ID = 2100630000000000002L;
    private static RedissonClient sharedRedis;
    private static AnnotationConfigApplicationContext sharedContext;
    private static Object previousSpringContext;
    private static Object previousSpringFactory;
    private HikariDataSource pool;
    private DynamicRoutingDataSource routing;
    private JdbcTemplate db;
    private RedissonClient redis;
    private NotifyNotificationDao dao;
    private NotifyConfigDao configDao;
    private NotifyQuotaPort quota;
    private NotifyDispatchResultPort resultPort;
    private NotificationApplicationUseCase application;
    private DispatchNotificationService dispatch;
    private NotifyIdempotencyCoordinator idempotency;
    private final List<NotifyDeliveryEvent> events = new ArrayList<>();
    private final AtomicInteger supplierCalls = new AtomicInteger();
    private final AtomicReference<NotifyContent> supplierContent = new AtomicReference<>();
    private boolean supplierThrows;
    private Runnable duringSupplier = () -> {};
    private String supplierMessageId = "13812345678-1234";
    private String runId;
    private String appId;
    private long accountId;
    private long bindingId;
    private BindingSeed seededBinding;
    private boolean accountInserted;
    private boolean bindingReplaced;

    @BeforeAll
    static void openRedis() {
        String mysqlUrl = System.getProperty("notify.mysql.integration.url", "");
        assertTrue(mysqlUrl.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/namewta_notify_test_[a-zA-Z0-9_]+.*"),
            "only an owned loopback notification database is allowed");
        assertNotNull(System.getenv("T35_MYSQL_PASSWORD"), "private MySQL credential is required");
        String redisPort = System.getProperty("notify.redis.integration.port", "");
        assertTrue(redisPort.matches("[0-9]+"), "owned loopback Redis port is required");
        previousSpringContext = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        previousSpringFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        var redisConfig = new Config();
        redisConfig.setThreads(2).setNettyThreads(2);
        redisConfig.useSingleServer().setAddress("redis://127.0.0.1:" + redisPort)
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        sharedRedis = Redisson.create(redisConfig);
        sharedContext = new AnnotationConfigApplicationContext();
        sharedContext.registerBean(RedissonClient.class, () -> sharedRedis);
        sharedContext.registerBean(SpringUtils.class);
        sharedContext.refresh();
        assertSame(sharedRedis, org.namewta.common.redis.utils.RedisUtils.getClient());
    }

    @AfterAll
    static void closeRedis() {
        if (sharedContext != null) sharedContext.close();
        if (sharedRedis != null) sharedRedis.shutdown();
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousSpringContext);
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousSpringFactory);
    }

    @BeforeEach
    void open() throws Exception {
        String url = System.getProperty("notify.mysql.integration.url", "");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/namewta_notify_test_[a-zA-Z0-9_]+.*"),
            "only an owned loopback notification database is allowed");
        String password = System.getenv("T35_MYSQL_PASSWORD");
        assertNotNull(password, "T35_MYSQL_PASSWORD is required in the private child environment");
        runId = UUID.randomUUID().toString().replace("-", "");
        appId = "owned-t35-" + runId;
        accountId = Math.abs(UUID.randomUUID().getMostSignificantBits() >>> 1);
        bindingId = SEEDED_SMS_BINDING_ID;

        pool = new HikariDataSource();
        pool.setJdbcUrl(url);
        pool.setUsername(System.getProperty("notify.mysql.integration.username", "root"));
        pool.setPassword(password);
        pool.setMaximumPoolSize(6);
        routing = new DynamicRoutingDataSource(List.of());
        routing.setPrimary("master");
        routing.setStrict(true);
        routing.addDataSource("master", pool);
        db = new JdbcTemplate(routing);

        var config = new MybatisConfiguration(new Environment("t35-owned", new SpringManagedTransactionFactory(), routing));
        config.setMapUnderscoreToCamelCase(true);
        var interceptors = new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        interceptors.addInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor());
        config.addInterceptor(interceptors);
        GlobalConfigUtils.setGlobalConfig(config, GlobalConfigUtils.defaults().setMetaObjectHandler(new InjectionMetaObjectHandler()));
        for (Class<?> mapper : List.of(NotifyIntentMapper.class, NotifyRecipientMapper.class, NotifyDeliveryMapper.class,
            NotifyOutboxMapper.class, NotifyAttemptMapper.class, NotifyMessageMapper.class,
            NotifyMessageRecipientMapper.class, NotifyChannelAccountMapper.class, NotifySceneBindingMapper.class)) {
            config.addMapper(mapper);
        }
        for (String resource : List.of("/mapper/notify/NotifyOutboxMapper.xml",
            "/mapper/notify/NotifyChannelAccountMapper.xml", "/mapper/notify/NotifyDeliveryMapper.xml")) {
            try (var stream = getClass().getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
            }
        }
        var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        dao = new NotifyNotificationDao(sessions.getMapper(NotifyIntentMapper.class),
            sessions.getMapper(NotifyRecipientMapper.class), sessions.getMapper(NotifyDeliveryMapper.class),
            sessions.getMapper(NotifyOutboxMapper.class), sessions.getMapper(NotifyAttemptMapper.class),
            sessions.getMapper(NotifyMessageMapper.class), sessions.getMapper(NotifyMessageRecipientMapper.class));
        configDao = new NotifyConfigDao(sessions.getMapper(NotifyChannelAccountMapper.class),
            sessions.getMapper(NotifySceneBindingMapper.class));

        redis = sharedRedis;
        idempotency = new NotifyIdempotencyCoordinator(new RedisNotifyIdempotencyStore(redis),
            new NotifyIdempotencyProperties());
        quota = new RedisNotifyQuotaAdapter();
        SmsNotifyChannelAdapter adapter = new SmsNotifyChannelAdapter(key -> new SmsNotificationProvider(key,
            (phone, content) -> {
                supplierCalls.incrementAndGet();
                supplierContent.set(content);
                duringSupplier.run();
                if (supplierThrows) throw new IllegalStateException("synthetic supplier I/O ambiguity");
                return SmsNotificationReceipt.accepted(supplierMessageId);
            }));
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, events::add, idempotency);
        resultPort = transactional(new NotifyDispatchResultUseCase(new NotifyDispatchResultService(dao)),
            NotifyDispatchResultUseCase.class);
        dispatch = new DispatchNotificationService(dao, dispatcher, null, configDao, quota, resultPort);
        application = transactional(new NotificationApplicationUseCase(new NotificationApplicationRuntimeService(
            dao, null, dispatch, event -> {})), NotificationApplicationUseCase.class);

        db.update("insert into notify_channel_account(account_id,channel,config_key,enabled,supplier,minute_max,create_time) "
            + "values(?,'SMS',?,'Y','aliyun',10,utc_timestamp())", accountId, "owned-" + runId);
        accountInserted = true;
        seededBinding = db.queryForObject("select account_id,sms_template_code,sms_param_mapping_json,"
            + "template_minute_max,restricted from notify_scene_binding "
            + "where binding_id=? and scene_code='auth-captcha' and channel='SMS'", (rs, row) ->
            new BindingSeed(rs.getObject("account_id", Long.class), rs.getString("sms_template_code"),
                rs.getString("sms_param_mapping_json"), rs.getInt("template_minute_max"),
                rs.getString("restricted")), bindingId);
        assertNotNull(seededBinding, "six-file base must contain the known SMS binding");
        assertNull(seededBinding.accountId(), "owned fixture only replaces the unbound seed");
        assertEquals(1, db.update("update notify_scene_binding set account_id=?,sms_template_code='SMS_OWNED',"
            + "sms_param_mapping_json=?,template_minute_max=10,restricted='N' "
            + "where binding_id=? and scene_code='auth-captcha' and channel='SMS' and account_id is null",
            accountId, JsonUtils.toJsonString(Map.of("code", "code", "expireMinutes", "min")), bindingId));
        bindingReplaced = true;
    }

    @AfterEach
    void close() {
        try {
            if (db != null && appId != null) {
                for (Long intentId : db.queryForList("select intent_id from notify_intent where app_id=?", Long.class, appId)) {
                    for (Long deliveryId : db.queryForList("select delivery_id from notify_delivery where intent_id=?",
                        Long.class, intentId)) {
                        redis.getBucket(idempotency.storageKey(NotifyRequest.builder().channel(NotifyChannel.SMS)
                            .idempotencyKey(String.valueOf(deliveryId)).build())).delete();
                    }
                    db.update("delete from notify_attempt where intent_id=?", intentId);
                    db.update("delete from notify_outbox where intent_id=?", intentId);
                    db.update("delete from notify_delivery where intent_id=?", intentId);
                    db.update("delete from notify_recipient where intent_id=?", intentId);
                    db.update("delete from notify_intent where intent_id=?", intentId);
                }
                if (bindingReplaced) {
                    assertEquals(1, db.update("update notify_scene_binding set account_id=?,sms_template_code=?,"
                        + "sms_param_mapping_json=?,template_minute_max=?,restricted=? "
                        + "where binding_id=? and scene_code='auth-captcha' and channel='SMS'",
                        seededBinding.accountId(), seededBinding.smsTemplateCode(), seededBinding.mappingJson(),
                        seededBinding.templateMinuteMax(), seededBinding.restricted(), bindingId));
                }
                if (accountInserted) {
                    db.update("delete from notify_channel_account where account_id=? and config_key=?",
                        accountId, "owned-" + runId);
                }
            }
            if (redis != null) {
                if (accountInserted) redis.getAtomicLong("acct:" + accountId).delete();
                if (bindingReplaced) redis.getAtomicLong("tpl:auth-captcha:SMS").delete();
            }
        } finally {
            if (routing != null) routing.destroy();
            if (pool != null) pool.close();
        }
    }

    @Test
    void validSmsUsesRealStorePlannerDispatcherAdapterAndResultTransaction() {
        NotificationCommand command = command("1234");
        NotificationReceipt receipt = application.submit(command);
        long intentId = Long.parseLong(receipt.notificationId());
        assertTrue(receipt.outboxQueued());
        claimAndDispatch(intentId);

        assertEquals(1, supplierCalls.get());
        assertEquals("ACCEPTED", db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals("DONE", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
        assertEquals(1, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));
        assertEquals(supplierMessageId, db.queryForObject("select provider_message_id from notify_delivery where intent_id=?",
            String.class, intentId), "raw supplier correlation remains internal");
        assertEquals(1, redis.getAtomicLong("acct:" + accountId).get());
        NotifyTemplateContent sent = assertInstanceOf(NotifyTemplateContent.class, supplierContent.get());
        assertEquals(Map.of("code", "1234", "min", "5"), sent.params());
        assertFalse(sent.contentSnapshot().contains("1234"));
        assertEquals(NotifyAuditPolicy.REDACT_SENSITIVE, events.getFirst().request().auditPolicy());
        assertFalse(JsonUtils.toJsonString(events.getFirst()).contains("1234"));
        assertFalse(JsonUtils.toJsonString(events.getFirst()).contains("13812345678"));
        assertNull(application.query(new NotificationQuery(receipt.notificationId(), false))
            .deliveries().getFirst().providerMessageId());
        var monitor = new NotificationMonitorUseCase(application, new NotificationMonitorService(dao));
        var monitorItem = monitor.deliveries(null, "SMS", null).stream()
            .filter(item -> item.intentId() == intentId).findFirst().orElseThrow();
        assertNull(monitorItem.providerMessageId());
        db.update("update notify_delivery set error_code=? where intent_id=?", "raw-error-1234", intentId);
        assertNull(monitor.deliveries(null, "SMS", null).stream()
            .filter(item -> item.intentId() == intentId).findFirst().orElseThrow().errorCode());

        assertEquals(1, db.update("update notify_scene_binding set account_id=null where binding_id=? and account_id=?",
            bindingId, accountId));
        NotificationReceipt duplicate = application.submit(command);
        assertEquals(receipt.notificationId(), duplicate.notificationId(),
            "an existing idempotent request keeps its receipt after binding changes");
        assertNull(duplicate.deliveries().getFirst().providerMessageId());
        assertEquals(1, supplierCalls.get());

        db.update("update notify_intent set metadata_json='{}' where intent_id=?", intentId);
        assertEquals(supplierMessageId, application.query(new NotificationQuery(receipt.notificationId(), false))
            .deliveries().getFirst().providerMessageId(), "FULL keeps its existing public projection");
        assertEquals(supplierMessageId, monitor.deliveries(null, "SMS", null).stream()
            .filter(item -> item.intentId() == intentId).findFirst().orElseThrow().providerMessageId());
        assertEquals("raw-error-1234", monitor.deliveries(null, "SMS", null).stream()
            .filter(item -> item.intentId() == intentId).findFirst().orElseThrow().errorCode());
    }

    @Test
    void missingVariableIsRejectedBeforeAnyIntentQuotaOrProviderCall() {
        assertThrows(ServiceException.class, () -> application.submit(command("")));
        assertEquals(0, db.queryForObject("select count(*) from notify_intent where app_id=?", Integer.class, appId));
        assertEquals(0, supplierCalls.get());
        assertFalse(redis.getAtomicLong("acct:" + accountId).isExists());
    }

    @Test
    void supplierIoAmbiguityStaysUnknownAndNeverRequeues() {
        supplierThrows = true;
        long intentId = Long.parseLong(application.submit(command("5678")).notificationId());
        claimAndDispatch(intentId);

        assertEquals(1, supplierCalls.get());
        assertEquals("UNKNOWN", db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals("WAITING_RECEIPT", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
        assertEquals("PROVIDER_OUTCOME_UNKNOWN", db.queryForObject(
            "select error_code from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals(1, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));
    }

    @Test
    void removedBindingBeforeDispatchFailsAndClosesOutboxWithoutProviderCall() {
        long intentId = Long.parseLong(application.submit(command("9012")).notificationId());
        assertEquals(1, db.update("update notify_scene_binding set account_id=null where binding_id=? and account_id=?",
            bindingId, accountId));
        claimAndDispatch(intentId);

        assertEquals(0, supplierCalls.get());
        assertEquals("FAILED", db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals("DONE", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
        assertEquals("UNBOUND_CHANNEL", db.queryForObject("select error_code from notify_delivery where intent_id=?", String.class, intentId));
    }

    @Test
    void dispatcherInvalidTargetIsLocalFailureAndNeverRequeues() {
        long intentId = Long.parseLong(application.submit(command("3456")).notificationId());
        db.update("update notify_delivery set target_value='' where intent_id=?", intentId);
        claimAndDispatch(intentId);

        assertEquals(0, supplierCalls.get());
        assertEquals("FAILED", db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals("DONE", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
        assertEquals("SMS_LOCAL_VALIDATION_INVALID_TARGET", db.queryForObject(
            "select error_code from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals(1, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));
    }

    @Test
    void brokenTemplateQuotaReleasesAccountThenRetriesAfterRecovery() {
        long intentId = Long.parseLong(application.submit(command("4567")).notificationId());
        String templateKey = "tpl:auth-captcha:SMS";
        redis.getBucket(templateKey).set("synthetic-not-an-integer");
        claimAndDispatch(intentId);

        assertEquals(0, supplierCalls.get());
        assertEquals("PENDING", db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals("READY", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
        assertEquals("PREPARATION_RETRYABLE", db.queryForObject(
            "select error_code from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals(0, redis.getAtomicLong("acct:" + accountId).get(), "held account quota must be released");
        assertEquals(1, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));

        redis.getBucket(templateKey).delete();
        claimAndDispatch(intentId);
        assertEquals(1, supplierCalls.get());
        assertEquals("ACCEPTED", db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals("DONE", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
        assertEquals(1, redis.getAtomicLong("acct:" + accountId).get());
        assertEquals(2, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));
    }

    @Test
    void corruptRedisIdempotencyStateFailsAcquireBeforeSupplierAndRecovers() {
        long intentId = Long.parseLong(application.submit(command("6789")).notificationId());
        String bucketKey = idempotencyBucket(intentId);
        redis.getBucket(bucketKey).set("synthetic-invalid-stored-state");
        claimAndDispatch(intentId);

        assertEquals(0, supplierCalls.get());
        assertEquals("PENDING", db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals("READY", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
        assertEquals("PREPARATION_RETRYABLE", db.queryForObject(
            "select error_code from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals(1, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));

        redis.getBucket(bucketKey).delete();
        claimAndDispatch(intentId);
        assertEquals(1, supplierCalls.get());
        assertEquals("ACCEPTED", db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals("DONE", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
        assertEquals(2, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));
    }

    @Test
    void redisCompletionCasFailureAfterSupplierRemainsUnknownWithoutResend() {
        long intentId = Long.parseLong(application.submit(command("7890")).notificationId());
        String bucketKey = idempotencyBucket(intentId);
        duringSupplier = () -> redis.getBucket(bucketKey).set("synthetic-cas-conflict");
        claimAndDispatch(intentId);

        assertEquals(1, supplierCalls.get());
        assertEquals("UNKNOWN", db.queryForObject("select status from notify_delivery where intent_id=?", String.class, intentId));
        assertEquals("WAITING_RECEIPT", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
        assertEquals(1, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));
        long outboxId = db.queryForObject("select outbox_id from notify_outbox where intent_id=?", Long.class, intentId);
        dispatch.dispatch(dao.outbox(outboxId));
        assertEquals(1, supplierCalls.get(), "WAITING_RECEIPT re-entry must not resend");
        assertEquals("WAITING_RECEIPT", db.queryForObject("select status from notify_outbox where intent_id=?", String.class, intentId));
    }

    @Test
    void retryableTransitionLostAckAfterRealRedisCasStillWaitsWithoutResend() {
        RedisNotifyIdempotencyStore realStore = new RedisNotifyIdempotencyStore(redis);
        NotifyIdempotencyStore lostAckStore = new NotifyIdempotencyStore() {
            @Override
            public Claim acquire(String key, String digest, String requestId, java.time.Duration window) {
                return realStore.acquire(key, digest, requestId, window);
            }

            @Override
            public void complete(Acquired owner, NotifyResult result) { realStore.complete(owner, result); }

            @Override
            public void markRetryable(Acquired owner) {
                realStore.markRetryable(owner);
                throw new IllegalStateException("synthetic lost Redis transition ACK");
            }

            @Override
            public void release(Acquired owner) { realStore.release(owner); }
        };
        SmsNotifyChannelAdapter adapter = new SmsNotifyChannelAdapter(key -> new SmsNotificationProvider(key,
            (phone, content) -> {
                supplierCalls.incrementAndGet();
                return SmsNotificationReceipt.unsentRetryable("PROVIDER_RATE_LIMIT_30S");
            }));
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, events::add,
            new NotifyIdempotencyCoordinator(lostAckStore, new NotifyIdempotencyProperties()));
        dispatch = new DispatchNotificationService(dao, dispatcher, null, configDao, quota, resultPort);

        long intentId = Long.parseLong(application.submit(command("2468")).notificationId());
        claimAndDispatch(intentId);
        assertEquals(1, supplierCalls.get());
        assertTrue(String.valueOf(redis.getBucket(idempotencyBucket(intentId)).get())
            .contains("\"state\":\"RETRYABLE\""), "real Redis CAS completed before the injected lost ACK");
        assertEquals("UNKNOWN", db.queryForObject("select status from notify_delivery where intent_id=?",
            String.class, intentId));
        assertEquals("WAITING_RECEIPT", db.queryForObject("select status from notify_outbox where intent_id=?",
            String.class, intentId));
        assertEquals(1, db.queryForObject("select count(*) from notify_attempt where intent_id=?",
            Integer.class, intentId));
        long outboxId = db.queryForObject("select outbox_id from notify_outbox where intent_id=?",
            Long.class, intentId);
        dispatch.dispatch(dao.outbox(outboxId));
        assertEquals(1, supplierCalls.get(), "lost ACK must not make a waiting outbox resend");
    }

    @Test
    void exactSingleAttemptTencentRateLimitReclaimsAndCallsTheSelectedBlendTwice() {
        Sms4jBlendRegistry registry = new Sms4jBlendRegistry();
        ProviderFactoryHolder.registerFactory(tencentResponseFactory());
        String configKey = "owned-" + runId;
        try {
            db.update("update notify_channel_account set supplier='tencent' where account_id=?", accountId);
            db.update("update notify_scene_binding set sms_param_mapping_json=? where binding_id=?",
                JsonUtils.toJsonString(Map.of("code", "1", "expireMinutes", "2")), bindingId);
            var account = new org.namewta.notify.domain.entity.NotifyChannelAccount();
            account.setConfigKey(configKey);
            account.setSupplier("tencent");
            account.setAccessKeyId("owned-ak");
            account.setAccessKeySecret("owned-sk");
            account.setSignature("owned-sign");
            account.setSdkAppId("owned-app");
            registry.upsert(account);
            assertTrue(registry.isSingleAttempt(SmsFactory.getSmsBlend(configKey)));
            SmsNotifyChannelAdapter adapter = new SmsNotifyChannelAdapter(
                new Sms4jNotificationProviderResolver(registry));
            NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
                NotifyContext::empty, events::add, idempotency);
            dispatch = new DispatchNotificationService(dao, dispatcher, null, configDao, quota, resultPort);

            long intentId = Long.parseLong(application.submit(command("t37-code")).notificationId());
            long outboxId = db.queryForObject("select outbox_id from notify_outbox where intent_id=?", Long.class, intentId);
            NotifyOutboxClaimService claims = new NotifyOutboxClaimService(dao);
            var first = claims.claim("owned-first-" + runId).stream()
                .filter(item -> item.getOutboxId() == outboxId).findFirst().orElseThrow();
            dispatch.dispatch(first);
            assertEquals(1, supplierCalls.get());
            assertEquals("PENDING", db.queryForObject("select status from notify_delivery where intent_id=?",
                String.class, intentId));
            assertEquals("READY", db.queryForObject("select status from notify_outbox where intent_id=?",
                String.class, intentId));
            assertEquals("PROVIDER_UNSENT_RETRYABLE", db.queryForObject(
                "select error_code from notify_delivery where intent_id=?", String.class, intentId));
            assertEquals(1, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));
            assertTrue(claims.claim("owned-too-early-" + runId).isEmpty(), "Outbox owns retry cadence");

            assertEquals(1, db.update("update notify_outbox set next_attempt_at=timestampadd(second,-1,utc_timestamp()) "
                + "where outbox_id=? and status='READY'", outboxId));
            var second = claims.claim("owned-second-" + runId).stream()
                .filter(item -> item.getOutboxId() == outboxId).findFirst().orElseThrow();
            dispatch.dispatch(second);
            assertEquals(2, supplierCalls.get(), "second logical attempt must enter the actual SMS4J blend");
            assertEquals("ACCEPTED", db.queryForObject("select status from notify_delivery where intent_id=?",
                String.class, intentId));
            assertEquals("DONE", db.queryForObject("select status from notify_outbox where intent_id=?",
                String.class, intentId));
            assertEquals(2, db.queryForObject("select count(*) from notify_attempt where intent_id=?", Integer.class, intentId));
            dispatch.dispatch(second);
            assertEquals(2, supplierCalls.get(), "completed/outdated lease must never resend");
            assertFalse(JsonUtils.toJsonString(events).contains("t37-code"));
        } finally {
            registry.remove(configKey);
            ProviderFactoryHolder.registerFactory(TencentFactory.instance());
        }
    }

    private BaseProviderFactory<SmsBlend, TencentConfig> tencentResponseFactory() {
        return new BaseProviderFactory<>() {
            @Override
            public SmsBlend createSms(TencentConfig config) {
                assertEquals(0, config.getMaxRetries(), "owned Registry must disable SMS4J internal retries");
                SmsBlend blend = mock(SmsBlend.class);
                when(blend.getConfigId()).thenReturn(config.getConfigId());
                when(blend.getSupplier()).thenReturn("tencent");
                when(blend.sendMessage(anyString(), anyString(), any(LinkedHashMap.class)))
                    .thenAnswer(invocation -> {
                        int call = supplierCalls.incrementAndGet();
                        SmsResponse response = new SmsResponse();
                        response.setConfigId(config.getConfigId());
                        response.setSuccess(call != 1);
                        String code = call == 1 ? "LimitExceeded.PhoneNumberThirtySecondLimit" : "Ok";
                        String serial = call == 1 ? "" : "owned-serial";
                        int fee = call == 1 ? 0 : 1;
                        response.setData(JSONUtil.parseObj("{\"Response\":{\"RequestId\":\"owned-request\","
                            + "\"SendStatusSet\":[{\"PhoneNumber\":\"+8613812345678\",\"SerialNo\":\""
                            + serial + "\",\"Fee\":" + fee + ",\"Code\":\"" + code + "\"}]}}"));
                        return response;
                    });
                return blend;
            }

            @Override
            public Class<TencentConfig> getConfigClass() { return TencentConfig.class; }

            @Override
            public String getSupplier() { return "tencent"; }
        };
    }

    private NotificationCommand command(String code) {
        return new NotificationCommand(appId, "auth-captcha", "auth_captcha", "13812345678",
            "PHONE", List.of("13812345678"), "auth-captcha",
            Map.of("code", code, "expireMinutes", "5"), List.of(NotificationChannel.SMS),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0, null, null,
            "idem-" + runId, Map.of("audit", "REDACT_SENSITIVE"));
    }

    private void claimAndDispatch(long intentId) {
        long outboxId = db.queryForObject("select outbox_id from notify_outbox where intent_id=?", Long.class, intentId);
        assertEquals(1, db.update("update notify_outbox set status='PROCESSING',lease_owner='owned-t35',"
            + "lease_token=?,lease_until=timestampadd(second,60,utc_timestamp()) where outbox_id=? and status='READY'",
            runId, outboxId));
        dispatch.dispatch(dao.outbox(outboxId));
    }

    private String idempotencyBucket(long intentId) {
        long deliveryId = db.queryForObject("select delivery_id from notify_delivery where intent_id=?", Long.class, intentId);
        return idempotency.storageKey(NotifyRequest.builder().channel(NotifyChannel.SMS)
            .idempotencyKey(String.valueOf(deliveryId)).build());
    }

    private static <T> T transactional(T target, Class<T> type) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true),
            DSTransactional.class));
        return type.cast(proxy.getProxy());
    }

    private record BindingSeed(Long accountId, String smsTemplateCode, String mappingJson,
                               int templateMinuteMax, String restricted) { }
}
