package org.namewta.test.sso;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.oidc.adapter.api.OidcSessionRevocationAdapter;
import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.dao.OidcApplicationDao;
import org.namewta.oidc.dao.OidcAuthorizationDao;
import org.namewta.oidc.dao.OidcLogoutOutboxDao;
import org.namewta.oidc.domain.OidcAuthorization;
import org.namewta.oidc.domain.OidcLogoutOutbox;
import org.namewta.oidc.mapper.OidcApplicationMapper;
import org.namewta.oidc.mapper.OidcAuthorizationMapper;
import org.namewta.oidc.mapper.OidcLogoutOutboxMapper;
import org.namewta.oidc.port.OidcLogoutDeliveryPort;
import org.namewta.oidc.service.OidcApplicationService;
import org.namewta.oidc.service.OidcAuthorizationPersistenceService;
import org.namewta.oidc.service.OidcLogoutService;
import org.namewta.oidc.usecase.OidcLogoutUseCase;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.api.SsoSessionRevocationParticipant;
import org.namewta.sso.config.SsoProperties;
import org.namewta.sso.dao.SsoSessionDao;
import org.namewta.sso.mapper.SsoBusinessSessionMapper;
import org.namewta.sso.mapper.SsoSessionMapper;
import org.namewta.sso.service.SsoPersistentSessionService;
import org.namewta.sso.service.SsoSessionService;
import org.namewta.sso.support.SsoBearerTokens;
import org.namewta.sso.support.SsoSessionHashes;
import org.namewta.sso.support.SsoSessionRecords;
import org.namewta.sso.usecase.impl.SsoSessionUseCaseImpl;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.*;

/** 真MySQL+dynamic-datasource事务，严格限制为本任务独占数据库。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "oidc.logout.mysql.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SsoOidcLogoutMySqlIntegrationTest {
    private DynamicRoutingDataSource routing;
    private JdbcTemplate db;
    private SsoPersistentSessionService central;
    private OidcAuthorizationDao grants;
    private OidcLogoutOutboxDao outbox;
    private OidcSessionRevocationAdapter participant;
    private final List<String> created = new ArrayList<>();
    private final List<Long> applications = new ArrayList<>();

    @BeforeAll
    void open() throws Exception {
        String url = System.getProperty("oidc.logout.mysql.url");
        assertThat(url).startsWith("jdbc:mysql://127.0.0.1:45406/namewta_oidc_logout_test");
        var pool = new HikariDataSource();
        pool.setJdbcUrl(url);
        pool.setUsername("root");
        pool.setPassword("owned-external-auth-test-only");
        pool.setMaximumPoolSize(5);
        routing = new DynamicRoutingDataSource(List.of());
        routing.setPrimary("master");
        routing.addDataSource("master", pool);
        db = new JdbcTemplate(routing);
        var configuration =
                new MybatisConfiguration(
                        new Environment(
                                "oidc-logout-owned",
                                new SpringManagedTransactionFactory(),
                                routing));
        configuration.setMapUnderscoreToCamelCase(true);
        GlobalConfigUtils.setGlobalConfig(configuration, GlobalConfigUtils.defaults());
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        configuration.addInterceptor(interceptor);
        for (var mapper :
                List.of(
                        SsoSessionMapper.class,
                        SsoBusinessSessionMapper.class,
                        OidcAuthorizationMapper.class,
                        OidcApplicationMapper.class,
                        OidcLogoutOutboxMapper.class)) configuration.addMapper(mapper);
        try (var xml = getClass().getResourceAsStream("/mapper/oidc/OidcApplicationMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(
                            xml,
                            configuration,
                            "mapper/oidc/OidcApplicationMapper.xml",
                            configuration.getSqlFragments())
                    .parse();
        }
        var sql =
                new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
        central =
                new SsoPersistentSessionService(
                        new SsoSessionDao(
                                sql.getMapper(SsoSessionMapper.class),
                                sql.getMapper(SsoBusinessSessionMapper.class)),
                        new SocialSecretCipher(Base64.getEncoder().encodeToString(new byte[32])));
        grants = new OidcAuthorizationDao(sql.getMapper(OidcAuthorizationMapper.class));
        outbox = new OidcLogoutOutboxDao(sql.getMapper(OidcLogoutOutboxMapper.class));
        var properties = new OidcProperties();
        properties.setIssuer("https://sso.example");
        var apps =
                new OidcApplicationService(
                        new OidcApplicationDao(sql.getMapper(OidcApplicationMapper.class)),
                        properties);
        participant =
                new OidcSessionRevocationAdapter(
                        transactional(
                                new OidcLogoutUseCase(
                                        new OidcAuthorizationPersistenceService(grants),
                                        apps,
                                        new OidcLogoutService(outbox),
                                        properties,
                                        mock(OidcLogoutDeliveryPort.class))));
    }

    @AfterEach
    void cleanOwnedRows() {
        if (db != null) {
            for (String hash : created) {
                db.update("delete from oidc_logout_outbox where session_hash=?", hash);
                db.update("delete from oidc_authorization where session_hash=?", hash);
                db.update("delete from sso_business_session where session_hash=?", hash);
                db.update("delete from sso_session where session_hash=?", hash);
            }
            for (Long id : applications)
                db.update("delete from oidc_application where application_id=?", id);
        }
        created.clear();
        applications.clear();
    }

    @AfterAll
    void close() throws Exception {
        if (routing != null) routing.destroy();
    }

    @Test
    void sessionGrantAndOutboxRollbackTogetherThenRetryIsIdempotent() {
        String sid = session();
        var grant = grant(sid);
        grants.insert(grant);
        SsoSessionRevocationParticipant failure =
                hash -> {
                    throw new IllegalStateException("fault after outbox");
                };
        assertThatThrownBy(() -> logout(List.of(participant, failure)).logout(sid))
                .isInstanceOf(IllegalStateException.class);
        assertThat(central.current(sid)).isNotNull();
        assertThat(grants.find(grant.getFrameworkId()).getStatus()).isEqualTo("ACTIVE");
        assertThat(count(sid)).isZero();
        var useCase = logout(List.of(participant));
        useCase.logout(sid);
        useCase.logout(sid);
        assertThat(central.current(sid)).isNull();
        assertThat(count(sid)).isEqualTo(1);
        assertThat(grants.find(grant.getFrameworkId()).getSessionClosed()).isTrue();
        var task = outbox.claim("first-worker");
        assertThat(task).isNotNull();
        assertThat(outbox.claim("second-worker")).isNull();
        var stale = new OidcLogoutOutbox();
        stale.setLogoutOutboxId(task.getLogoutOutboxId());
        stale.setLeaseToken("stale");
        assertThat(outbox.result(stale, "DONE", null, LocalDateTime.now(ZoneOffset.UTC))).isFalse();
        assertThat(outbox.result(task, "DONE", null, LocalDateTime.now(ZoneOffset.UTC))).isTrue();
    }

    @Test
    void concurrentGrantMustFinishBeforeLogoutEnumeratesItsRecipient() throws Exception {
        String sid = session();
        var grant = grant(sid);
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var issuance = transactional(new GrantProbe(central, grants));
        try (var pool = Executors.newFixedThreadPool(2)) {
            var issued = pool.submit(() -> issuance.insert(sid, grant, locked, release));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var revoked = pool.submit(() -> logout(List.of(participant)).logout(sid));
            release.countDown();
            issued.get(10, TimeUnit.SECONDS);
            revoked.get(10, TimeUnit.SECONDS);
        }
        assertThat(count(sid)).isEqualTo(1);
        assertThat(central.current(sid)).isNull();
        assertThat(grants.find(grant.getFrameworkId()).getStatus()).isEqualTo("REVOKED");
    }

    private String session() {
        String sid = SsoBearerTokens.create();
        created.add(SsoSessionHashes.hash(sid));
        central.create(
                sid,
                SsoSessionRecords.create(
                        new SsoAuthenticatedUser(7L, "test"),
                        Instant.now(),
                        Duration.ofMinutes(10)));
        return sid;
    }

    private OidcAuthorization grant(String sid) {
        long id = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        applications.add(id);
        db.update(
                "insert into"
                    + " oidc_application(application_id,name,client_id,client_secret_hash,redirect_uris_json,post_logout_redirect_uris_json,allowed_fields_json,backchannel_logout_uri)"
                    + " values(?,?,?,?,?,?,?,?)",
                id,
                "test",
                "rp-" + id,
                "test",
                "[]",
                "[]",
                "[]",
                "https://rp.example/logout");
        var grant = new OidcAuthorization();
        grant.setAuthorizationId(id);
        grant.setFrameworkId("grant-" + id);
        grant.setApplicationId(id);
        grant.setUserId(7L);
        grant.setSessionHash(SsoSessionHashes.hash(sid));
        grant.setSubject("subject");
        grant.setAllowedFieldsJson("[]");
        grant.setAuthorizedScopes("openid");
        grant.setStatus("ACTIVE");
        grant.setIdTokenHash(org.namewta.oidc.support.OidcSecrets.hash("id-" + id));
        grant.setAuthorizationJson("owned-fixture-cipher");
        grant.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        grant.setVersion(0);
        grant.setDelFlag("0");
        return grant;
    }

    private long count(String sid) {
        return db.queryForObject(
                "select count(*) from oidc_logout_outbox where session_hash=?",
                Long.class,
                SsoSessionHashes.hash(sid));
    }

    private SsoSessionUseCaseImpl logout(List<SsoSessionRevocationParticipant> participants) {
        return transactional(
                new SsoSessionUseCaseImpl(
                        mock(SsoSessionService.class), central, participants, new SsoProperties()));
    }

    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvisor(
                new DynamicDataSourceAnnotationAdvisor(
                        new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (T) proxy.getProxy();
    }

    public static class GrantProbe {
        private final SsoPersistentSessionService sessions;
        private final OidcAuthorizationDao grants;

        public GrantProbe(SsoPersistentSessionService sessions, OidcAuthorizationDao grants) {
            this.sessions = sessions;
            this.grants = grants;
        }

        @DSTransactional
        public void insert(
                String sid,
                OidcAuthorization grant,
                CountDownLatch locked,
                CountDownLatch release) {
            sessions.requireLocked(sid);
            locked.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            grants.insert(grant);
        }
    }
}
