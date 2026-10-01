package org.namewta.test.auth.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.zaxxer.hikari.HikariDataSource;
import io.github.linpeilie.Converter;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.mybatis.handler.InjectionMetaObjectHandler;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.system.api.OssService;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.mapper.*;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysUserTypeService;
import org.namewta.system.service.impl.SysUserServiceImpl;
import org.namewta.system.service.impl.SysUserTypeRelServiceImpl;
import org.namewta.web.service.social.ExternalAuthAccountService;
import org.namewta.web.service.social.ExternalAuthAccountTransactionService;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Concurrent real transactions, real identity unique key, and real distributed conflict locks. */
@Tag("dev")
@EnabledIfSystemProperty(named = "external.account.mysql.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExternalAuthAccountMySqlIntegrationTest {
    private DynamicRoutingDataSource routing;
    private AnnotationConfigApplicationContext context;
    private RedissonClient redis;
    private JdbcTemplate db;
    private ExternalAuthAccountService accounts;
    private OssService oss;
    private Object previousFactory, previousContext;

    @BeforeAll
    void open() throws Exception {
        String url = System.getProperty("external.account.mysql.url");
        assertThat(url).startsWith("jdbc:mysql://127.0.0.1:45406/namewta_external_account_test");
        previousFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        previousContext = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        var pool = new HikariDataSource(); pool.setJdbcUrl(url); pool.setUsername("root");
        pool.setPassword("owned-external-auth-test-only"); pool.setMaximumPoolSize(6);
        routing = new DynamicRoutingDataSource(List.of()); routing.setPrimary("master"); routing.addDataSource("master", pool);
        db = new JdbcTemplate(routing);
        var redisConfig = new Config(); redisConfig.setThreads(2).setNettyThreads(2);
        redisConfig.useSingleServer().setAddress("redis://127.0.0.1:45479").setDatabase(10)
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        redis = Redisson.create(redisConfig);
        context = new AnnotationConfigApplicationContext(); context.registerBean(SpringUtils.class);
        context.registerBean(Converter.class, () -> new Converter()); context.refresh();
        var config = new MybatisConfiguration(new Environment("external-account-owned", new SpringManagedTransactionFactory(), routing));
        config.setMapUnderscoreToCamelCase(true);
        GlobalConfigUtils.setGlobalConfig(config, GlobalConfigUtils.defaults().setMetaObjectHandler(new InjectionMetaObjectHandler()));
        for (var mapper : List.of(SysSocialMapper.class, SysUserMapper.class, SysUserTypeMapper.class, SysUserTypeRelMapper.class)) config.addMapper(mapper);
        String xml = "/mapper/system/SysSocialMapper.xml";
        try (var input = getClass().getResourceAsStream(xml)) {
            assertThat(input).isNotNull(); new XMLMapperBuilder(input, config, xml, config.getSqlFragments()).parse();
        }
        var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        var userMapper = sessions.getMapper(SysUserMapper.class);
        var typeService = mock(ISysUserTypeService.class);
        var type = new SysUserTypeVo(); type.setUserTypeId(992001L); type.setStatus("0");
        when(typeService.queryById(992001L)).thenReturn(type);
        var clientSessions = mock(ClientSessionService.class);
        var grants = transactional(new SysUserTypeRelServiceImpl(sessions.getMapper(SysUserTypeRelMapper.class),
            sessions.getMapper(SysUserTypeMapper.class), clientSessions));
        oss = mock(OssService.class);
        var users = transactional(new SysUserServiceImpl(userMapper, mock(SysDeptMapper.class), mock(SysRoleMapper.class),
            mock(SysPostMapper.class), mock(SysUserRoleMapper.class), mock(SysUserPostMapper.class), mock(SysClientMapper.class),
            sessions.getMapper(SysUserTypeMapper.class), clientSessions, grants, oss));
        var transactions = transactional(new ExternalAuthAccountTransactionService(sessions.getMapper(SysSocialMapper.class),
            userMapper, users, grants, typeService));
        accounts = new ExternalAuthAccountService(transactions, redis);
    }

    @BeforeEach
    void seed() {
        reset(oss);
        db.update("delete from sys_social"); db.update("delete from sys_user_type_rel"); db.update("delete from sys_user");
        db.update("delete from sys_user_type");
        db.update("insert into sys_user_type(user_type_id,user_type_code,user_type_name,status) values(992001,'external-test','测试登录域','0')");
        redis.getKeys().deleteByPattern("external-auth:account:*");
    }

    @AfterAll
    void close() throws Exception {
        if (context != null) context.close(); if (redis != null) redis.shutdown(); if (routing != null) routing.destroy();
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousFactory);
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousContext);
    }

    @Test
    void phoneLockRemainsHeldDuringBeforeCommitSoWaiterSeesCommittedConflict() throws Exception {
        var beforeCommit = new CountDownLatch(1); var releaseCommit = new CountDownLatch(1); var once = new AtomicBoolean();
        doAnswer(call -> {
            if (once.compareAndSet(false, true)) {
                TransactionContext.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) {
                        beforeCommit.countDown(); await(releaseCommit);
                    }
                });
            }
            return null;
        }).when(oss).reconcileReferences(anyString(), anyString(), anyList(), anyList());
        try (var threads = Executors.newFixedThreadPool(2)) {
            var first = threads.submit(() -> accounts.register(client(), "corporate", "OIDC", identity("one", null), "13800138001"));
            assertThat(beforeCommit.await(10, TimeUnit.SECONDS)).isTrue();
            var second = threads.submit(() -> accounts.register(client(), "corporate", "OIDC", identity("two", null), "13800138001"));
            try {
                assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                assertThat(count("sys_user")).isZero();
            } finally {
                releaseCommit.countDown();
            }
            assertThat(first.get(10, TimeUnit.SECONDS)).isNotNull();
            assertThat(second.get(10, TimeUnit.SECONDS)).isNull();
        }
        assertCounts(1, 1, 1);
    }

    @Test
    void caseAndAccentEquivalentEmailsCannotCreateTwoUsersWithDifferentPhones() throws Exception {
        try (var threads = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = threads.submit(() -> { await(start); return accounts.register(client(), "corporate", "OIDC", identity("one", "café@example.test"), "13800138001"); });
            var second = threads.submit(() -> { await(start); return accounts.register(client(), "corporate", "OIDC", identity("two", "CAFE@example.test"), "13800138002"); });
            start.countDown();
            var results = java.util.Arrays.asList(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results.stream().filter(java.util.Objects::nonNull).count()).isEqualTo(1);
        }
        assertCounts(1, 1, 1);
    }

    @Test
    void sameIdentityRaceRollsBackLosingUserAndLoginDomainGrant() throws Exception {
        try (var threads = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = threads.submit(() -> { await(start); return raceRegister("13800138001"); });
            var second = threads.submit(() -> { await(start); return raceRegister("13800138002"); });
            start.countDown(); assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(second.get(10, TimeUnit.SECONDS));
        }
        assertCounts(1, 1, 1);
    }

    @Test
    void transactionFailureRollsBackUserAndReleasesConflictLocks() {
        doThrow(new IllegalStateException("injected avatar failure")).when(oss)
            .reconcileReferences(anyString(), anyString(), anyList(), anyList());
        assertThatThrownBy(() -> accounts.register(client(), "corporate", "OIDC", identity("one", "owned@example.test"), "13800138001"))
            .isInstanceOf(IllegalStateException.class);
        assertCounts(0, 0, 0);
        reset(oss);
        assertThat(accounts.register(client(), "corporate", "OIDC", identity("one", "owned@example.test"), "13800138001")).isNotNull();
        assertCounts(1, 1, 1);
    }

    @Test
    void competingUsersCannotTakeOverSameIdentityAndForeignUnbindCannotDeleteIt() throws Exception {
        seedUser(992101, "13800138001"); seedUser(992102, "13800138002");
        try (var threads = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = threads.submit(() -> { await(start); return tryBind(992101L, "one"); });
            var second = threads.submit(() -> { await(start); return tryBind(992102L, "one"); });
            start.countDown(); assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        }
        Long owner = accounts.findUser(identity("one", null), "OIDC");
        Long id = db.queryForObject("select id from sys_social", Long.class);
        assertThatThrownBy(() -> accounts.unbind(owner == 992101L ? 992102L : 992101L, id)).isInstanceOf(ServiceException.class);
        assertThat(count("sys_social")).isEqualTo(1);
        accounts.unbind(owner, id); assertThat(count("sys_social")).isZero();
    }

    @Test
    void competingSubjectsForSameUserAndIssuerAreSerializedByUserRowLock() throws Exception {
        seedUser(992101, "13800138001");
        try (var threads = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = threads.submit(() -> { await(start); return tryBind(992101L, "one"); });
            var second = threads.submit(() -> { await(start); return tryBind(992101L, "two"); });
            start.countDown(); assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        }
        assertThat(count("sys_social")).isEqualTo(1);
    }

    @Test
    void unbindWaitsForTokenIssuanceWithoutKeepingADatabaseTransactionOpen() throws Exception {
        seedUser(992101, "13800138001");
        accounts.bind(992101L, "corporate", "OIDC", identity("one", null));
        Long bindingId = db.queryForObject("select id from sys_social", Long.class);
        var mintEntered = new CountDownLatch(1); var finishMint = new CountDownLatch(1);
        var unbindStarted = new CountDownLatch(1);
        try (var threads = Executors.newFixedThreadPool(2)) {
            var login = threads.submit(() -> accounts.withBoundIdentity(992101L, identity("one", null), "OIDC", () -> {
                assertThat(TransactionContext.getXID()).isNull();
                mintEntered.countDown(); await(finishMint); return "owned-business-token";
            }));
            assertThat(mintEntered.await(10, TimeUnit.SECONDS)).isTrue();
            var unbind = threads.submit(() -> { unbindStarted.countDown(); accounts.unbind(992101L, bindingId); });
            try {
                assertThat(unbindStarted.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> unbind.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                assertThat(count("sys_social")).isEqualTo(1);
            } finally {
                finishMint.countDown();
            }
            assertThat(login.get(10, TimeUnit.SECONDS)).isEqualTo("owned-business-token");
            unbind.get(10, TimeUnit.SECONDS);
        }
        assertThat(count("sys_social")).isZero();
        var lateMint = new AtomicBoolean();
        assertThatThrownBy(() -> accounts.withBoundIdentity(992101L, identity("one", null), "OIDC", () -> {
            lateMint.set(true); return "must-not-be-issued";
        })).isInstanceOf(ServiceException.class).hasMessageContaining("绑定已变更");
        assertThat(lateMint).isFalse();
    }

    @Test
    void identityReassignedAfterLookupCannotMintForThePreviousOwner() {
        seedUser(992101, "13800138001"); seedUser(992102, "13800138002");
        accounts.bind(992101L, "corporate", "OIDC", identity("one", null));
        Long observedOwner = accounts.findUser(identity("one", null), "OIDC");
        accounts.unbind(observedOwner, db.queryForObject("select id from sys_social", Long.class));
        accounts.bind(992102L, "corporate", "OIDC", identity("one", null));
        var mint = new AtomicBoolean();
        assertThatThrownBy(() -> accounts.withBoundIdentity(observedOwner, identity("one", null), "OIDC", () -> {
            mint.set(true); return "must-not-be-issued";
        })).isInstanceOf(ServiceException.class).hasMessageContaining("绑定已变更");
        assertThat(mint).isFalse();
        assertThat(accounts.withBoundIdentity(992102L, identity("one", null), "OIDC", () -> "new-owner-token"))
            .isEqualTo("new-owner-token");
    }

    @Test
    void failedTokenIssuanceReleasesTheUserLockForUnbind() throws Exception {
        seedUser(992101, "13800138001");
        accounts.bind(992101L, "corporate", "OIDC", identity("one", null));
        Long bindingId = db.queryForObject("select id from sys_social", Long.class);
        assertThatThrownBy(() -> accounts.withBoundIdentity(992101L, identity("one", null), "OIDC", () -> {
            throw new IllegalStateException("injected token failure");
        })).isInstanceOf(IllegalStateException.class);
        try (var threads = Executors.newSingleThreadExecutor()) {
            threads.submit(() -> accounts.unbind(992101L, bindingId)).get(2, TimeUnit.SECONDS);
        }
        assertThat(count("sys_social")).isZero();
    }

    private Long raceRegister(String phone) {
        try { return accounts.register(client(), "corporate", "OIDC", identity("same", null), phone); }
        catch (DuplicateKeyException race) { return accounts.findUser(identity("same", null), "OIDC"); }
    }

    private boolean tryBind(Long user, String subject) {
        try { accounts.bind(user, "corporate", "OIDC", identity(subject, null)); return true; }
        catch (ServiceException | DuplicateKeyException race) { return false; }
    }

    private void seedUser(long id, String phone) {
        db.update("insert into sys_user(user_id,user_name,nick_name,phone_number,status) values (?, ?, '测试用户', ?, '0')", id, "owned_" + id, phone);
    }

    private SysClientVo client() {
        var client = new SysClientVo(); client.setRegisterEnabled(true); client.setUserTypeId(992001L);
        client.setClientId("external-account-owned"); return client;
    }

    private OidcIdentity identity(String subject, String email) {
        return new OidcIdentity("https://identity.example.test", subject, null, "测试用户", null, email, null, null, 1L);
    }

    private int count(String table) { return db.queryForObject("select count(*) from " + table, Integer.class); }
    private void assertCounts(int users, int grants, int bindings) {
        assertThat(count("sys_user")).isEqualTo(users); assertThat(count("sys_user_type_rel")).isEqualTo(grants);
        assertThat(count("sys_social")).isEqualTo(bindings);
    }
    private static void await(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("latch timeout"); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
    }
    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target) {
        var factory = new ProxyFactory(target); factory.setProxyTargetClass(true);
        factory.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (T) factory.getProxy();
    }
}
