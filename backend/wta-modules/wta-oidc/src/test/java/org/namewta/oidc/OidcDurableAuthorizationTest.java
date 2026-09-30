package org.namewta.oidc;

import static org.assertj.core.api.Assertions.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.namewta.oidc.dao.OidcAuthorizationDao;
import org.namewta.oidc.domain.OidcAuthorization;
import org.namewta.oidc.mapper.OidcAuthorizationMapper;
import org.namewta.oidc.support.OidcSecrets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import tools.jackson.databind.json.JsonMapper;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** 只在明确指定的隔离库运行，所有写入均为本测试随机主键；绝不清空已有表。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "oidc.test.config", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OidcDurableAuthorizationTest {
    private JdbcTemplate jdbc;
    private org.springframework.transaction.support.TransactionTemplate transactions;
    private OidcAuthorizationDao first, second;
    private org.namewta.oidc.dao.OidcApplicationDao applications;
    private final List<Long> createdApplications = new ArrayList<>();
    private final List<String> created = new ArrayList<>();

    @BeforeAll
    void start() throws Exception {
        var config =
                JsonMapper.builder()
                        .build()
                        .readTree(Files.readString(Path.of(System.getProperty("oidc.test.config"))))
                        .path("spring")
                        .path("datasource")
                        .path("dynamic")
                        .path("datasource")
                        .path("master");
        var source =
                new DriverManagerDataSource(
                        config.path("url").asText(),
                        config.path("username").asText(),
                        config.path("password").asText());
        jdbc = new JdbcTemplate(source);
        transactions =
                new org.springframework.transaction.support.TransactionTemplate(
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                                source));
        first = instance(source);
        second = instance(source);
    }

    private OidcAuthorizationDao instance(DriverManagerDataSource source) throws Exception {
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(false);
        var global =
                new GlobalConfig()
                        .setBanner(false)
                        .setDbConfig(new GlobalConfig.DbConfig())
                        .setSqlInjector(new DefaultSqlInjector());
        GlobalConfigUtils.setGlobalConfig(configuration, global);
        configuration.addMapper(OidcAuthorizationMapper.class);
        configuration.addMapper(org.namewta.oidc.mapper.OidcApplicationMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(configuration);
        factory.setGlobalConfig(global);
        factory.setMapperLocations(
                new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                        .getResources("classpath*:mapper/oidc/OidcAuthorizationMapper.xml"));
        var sessions = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        applications =
                new org.namewta.oidc.dao.OidcApplicationDao(
                        sessions.getMapper(org.namewta.oidc.mapper.OidcApplicationMapper.class));
        return new OidcAuthorizationDao(sessions.getMapper(OidcAuthorizationMapper.class));
    }

    @AfterAll
    void clean() {
        if (jdbc != null) {
            for (String id : created)
                jdbc.update("DELETE FROM oidc_authorization WHERE framework_id=?", id);
            for (Long id : createdApplications)
                jdbc.update("DELETE FROM oidc_application WHERE application_id=?", id);
        }
    }

    private OidcAuthorization fixture() {
        var row = new OidcAuthorization();
        row.setAuthorizationId(ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE));
        row.setFrameworkId("oidc-test-" + UUID.randomUUID());
        row.setApplicationId(ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE));
        row.setUserId(1L);
        row.setSubject("test-sub");
        row.setSessionHash(OidcSecrets.hash(UUID.randomUUID().toString()));
        row.setAllowedFieldsJson("[]");
        row.setAuthorizedScopes("openid");
        row.setStatus("ACTIVE");
        row.setCodeHash(OidcSecrets.hash(UUID.randomUUID().toString()));
        row.setCodeExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        row.setCodeConsumed(false);
        row.setAuthorizationJson("test-ciphertext");
        row.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        row.setVersion(0);
        row.setDelFlag("0");
        first.insert(row);
        created.add(row.getFrameworkId());
        return row;
    }

    @Test
    void independentInstancesHaveExactlyOneCodeConsumer() throws Exception {
        var row = fixture();
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a =
                    pool.submit(
                            () -> {
                                start.await();
                                return first.consume(row.getCodeHash());
                            });
            var b =
                    pool.submit(
                            () -> {
                                start.await();
                                return second.consume(row.getCodeHash());
                            });
            start.countDown();
            assertThat(
                            (a.get(10, TimeUnit.SECONDS) ? 1 : 0)
                                    + (b.get(10, TimeUnit.SECONDS) ? 1 : 0))
                    .isEqualTo(1);
        }
        assertThat(first.find(row.getFrameworkId()).getCodeConsumed()).isTrue();
        assertThat(first.find(row.getFrameworkId()).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void applicationDisableWinsAgainstAlreadyConsumedGrant() {
        var row = fixture();
        assertThat(first.consume(row.getCodeHash())).isTrue();
        var pending = first.find(row.getFrameworkId());
        second.revokeApplication(row.getApplicationId());
        pending.setAccessTokenHash(OidcSecrets.hash("access-" + UUID.randomUUID()));
        pending.setAccessExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        assertThat(first.finish(pending)).isFalse();
        assertThat(first.find(row.getFrameworkId()).getStatus()).isEqualTo("REVOKED");
    }

    @Test
    void oldEncryptedStateCannotResurrectConsumedOrRevokedCredential() {
        var row = fixture();
        assertThat(first.consume(row.getCodeHash())).isTrue();
        jdbc.update(
                "UPDATE oidc_authorization SET authorization_json=? WHERE framework_id=?",
                row.getAuthorizationJson(),
                row.getFrameworkId());
        assertThat(second.consume(row.getCodeHash())).isFalse();
        second.revoke(row.getFrameworkId());
        var latest = first.find(row.getFrameworkId());
        assertThat(first.finish(latest)).isFalse();
    }

    @Test
    void expiredCodeCannotBeConsumed() {
        var row = fixture();
        jdbc.update(
                "UPDATE oidc_authorization SET code_expires_at=? WHERE framework_id=?",
                LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1),
                row.getFrameworkId());
        assertThat(second.consume(row.getCodeHash())).isFalse();
    }

    @Test
    void cleanupIsBoundedAndIncludesEveryExpiredState() {
        transactions.executeWithoutResult(
                ignored -> {
                    var cutoff = LocalDateTime.of(1001, 1, 1, 0, 0);
                    assertThat(
                                    jdbc.queryForObject(
                                            "SELECT COUNT(*) FROM oidc_authorization WHERE"
                                                    + " expires_at < ?",
                                            Long.class,
                                            cutoff))
                            .as("isolated historical range must contain no existing data")
                            .isZero();
                    var current = fixture();
                    for (String status : List.of("ACTIVE", "PENDING", "REVOKED")) {
                        var expired = fixture();
                        jdbc.update(
                                "UPDATE oidc_authorization SET expires_at=?,status=? WHERE"
                                        + " framework_id=?",
                                cutoff.minusDays(1),
                                status,
                                expired.getFrameworkId());
                    }
                    assertThat(first.purgeExpired(cutoff, 2)).isEqualTo(2);
                    assertThat(second.purgeExpired(cutoff, 2)).isEqualTo(1);
                    assertThat(first.find(current.getFrameworkId())).isNotNull();
                });
    }

    @Test
    void applicationLockOrdersNewCodeBeforePolicyRevocation() throws Exception {
        long applicationId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        String clientId = "lock-test-" + UUID.randomUUID();
        jdbc.update(
                "INSERT INTO"
                    + " oidc_application(application_id,name,client_id,client_secret_hash,redirect_uris_json,post_logout_redirect_uris_json,allowed_fields_json)"
                    + " VALUES (?,?,?,?,?,?,?)",
                applicationId,
                "Concurrent test",
                clientId,
                "unused-test-hash",
                "[]",
                "[]",
                "[]");
        createdApplications.add(applicationId);
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var updating = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var granting =
                    pool.submit(
                            () ->
                                    transactions.execute(
                                            status -> {
                                                assertThat(
                                                                applications
                                                                        .lockClient(clientId)
                                                                        .getVersion())
                                                        .isZero();
                                                locked.countDown();
                                                try {
                                                    assertThat(release.await(10, TimeUnit.SECONDS))
                                                            .isTrue();
                                                } catch (InterruptedException failure) {
                                                    Thread.currentThread().interrupt();
                                                    throw new IllegalStateException(failure);
                                                }
                                                var row = fixture();
                                                jdbc.update(
                                                        "UPDATE oidc_authorization SET"
                                                                + " application_id=? WHERE"
                                                                + " framework_id=?",
                                                        applicationId,
                                                        row.getFrameworkId());
                                                return row.getFrameworkId();
                                            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            var changing =
                    pool.submit(
                            () ->
                                    transactions.executeWithoutResult(
                                            status -> {
                                                updating.countDown();
                                                jdbc.update(
                                                        "UPDATE oidc_application SET"
                                                                + " version=version+1 WHERE"
                                                                + " application_id=?",
                                                        applicationId);
                                                second.revokeApplication(applicationId);
                                            }));
            assertThat(updating.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                assertThatThrownBy(() -> changing.get(150, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
            } finally {
                release.countDown();
            }
            var authorizationId = granting.get(10, TimeUnit.SECONDS);
            changing.get(10, TimeUnit.SECONDS);
            assertThat(first.find(authorizationId).getStatus()).isEqualTo("REVOKED");
        }
    }
}
