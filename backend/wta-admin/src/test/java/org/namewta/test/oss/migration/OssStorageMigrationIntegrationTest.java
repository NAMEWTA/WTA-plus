package org.namewta.test.oss.migration;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.oss.client.DefaultOssClientImpl;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.config.AccessControlPolicyConfig;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.exception.OssErrorCode;
import org.namewta.common.oss.exception.S3StorageException;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.mapper.SysOssConfigMapper;
import org.namewta.system.domain.bo.SysOssConfigBo;
import org.namewta.system.service.impl.SysOssConfigServiceImpl;
import org.namewta.system.oss.migration.*;
import org.namewta.system.oss.migration.mapper.SysOssMigrationBatchMapper;
import org.namewta.system.oss.migration.mapper.SysOssMigrationItemMapper;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.test.support.SqlBaselineScripts;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.aop.framework.ProxyFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.namewta.system.oss.migration.OssMigrationContracts.MigrationRequest;
import static org.mockito.Mockito.mockStatic;

/**
 * 真实 MySQL 与双 Bucket 的迁移闭环验证，仅在 Lead 提供隔离环境参数时执行。
 */
@Tag("dev")
class OssStorageMigrationIntegrationTest {

    private static final String PRIVATE_ROUTE = "migration-private";
    private static final String PUBLIC_ROUTE = "migration-public";

    @Test
    void migratesWithProductionStoreAndDualBucketsThenCleansUpOrRollsBack() throws Exception {
        String mysqlUrl = System.getProperty("oss.migration.mysql.integration.url");
        String endpoint = System.getProperty("oss.minio.integration.endpoint");
        Assumptions.assumeTrue(mysqlUrl != null && !mysqlUrl.isBlank(), "需要一次性隔离 MySQL JDBC URL");
        Assumptions.assumeTrue(endpoint != null && !endpoint.isBlank(), "需要一次性 MinIO endpoint");

        PooledDataSource dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", mysqlUrl,
            System.getProperty("oss.migration.mysql.integration.username", "root"),
            System.getProperty("oss.migration.mysql.integration.password", ""));
        URI endpointUri = URI.create(endpoint);
        String accessKey = System.getProperty("oss.minio.integration.access-key", "namewta");
        String secretKey = System.getProperty("oss.minio.integration.secret-key", "namewta123");
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String privateBucket = "namewta-migration-private-" + suffix;
        String publicBucket = "namewta-migration-public-" + suffix;
        String cleanupKey = "migration/cleanup-" + suffix + ".txt";
        String rollbackKey = "migration/rollback-" + suffix + ".txt";

        try (S3Client bootstrap = bootstrap(endpointUri, accessKey, secretKey)) {
            prepareDatabase(dataSource, cleanupKey, rollbackKey, privateBucket, publicBucket);
            prepareBuckets(bootstrap, privateBucket, publicBucket, cleanupKey, rollbackKey);
            try (OssClient privateClient = client(PRIVATE_ROUTE, endpointUri, accessKey, secretKey,
                privateBucket, AccessPolicy.PRIVATE);
                 OssClient publicClient = client(PUBLIC_ROUTE, endpointUri, accessKey, secretKey,
                     publicBucket, AccessPolicy.PUBLIC_READ);
                 MockedStatic<OssFactory> factory = mockStatic(OssFactory.class)) {
                factory.when(() -> OssFactory.instance(PRIVATE_ROUTE)).thenReturn(privateClient);
                factory.when(() -> OssFactory.instance(PUBLIC_ROUTE)).thenReturn(publicClient);

                DynamicRoutingDataSource routing = new DynamicRoutingDataSource(List.of());
                routing.setPrimary("master");
                routing.setStrict(true);
                AtomicReference<String> armedReservationXid = new AtomicReference<>();
                java.util.concurrent.atomic.AtomicInteger reservationArmCount = new java.util.concurrent.atomic.AtomicInteger();
                java.util.concurrent.atomic.AtomicInteger reservationCommitHit = new java.util.concurrent.atomic.AtomicInteger();
                routing.addDataSource("master", new org.springframework.jdbc.datasource.DelegatingDataSource(dataSource) {
                    @Override public Connection getConnection() throws SQLException {
                        Connection connection = dataSource.getConnection();
                        String connectionXid = TransactionContext.getXID();
                        return (Connection) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                                if ("commit".equals(method.getName())
                                    && connectionXid != null
                                    && connectionXid.equals(armedReservationXid.get())
                                    && reservationCommitHit.get() == 0) {
                                    connection.commit();
                                    reservationCommitHit.incrementAndGet();
                                    throw new java.sql.SQLRecoverableException("owned commit acknowledgement lost");
                                }
                                try { return method.invoke(connection, args); }
                                catch (java.lang.reflect.InvocationTargetException error) { throw error.getCause(); }
                            });
                    }
                });
                SqlSessionTemplate sessions = new SqlSessionTemplate(sqlSessionFactory(routing));
                SysOssMapper ossMapper = sessions.getMapper(SysOssMapper.class);
                AtomicBoolean rejectItemSwitch = new AtomicBoolean();
                AtomicBoolean rejectPointer = new AtomicBoolean();
                MybatisOssMigrationStore store = new MybatisOssMigrationStore(ossMapper,
                    sessions.getMapper(SysOssMigrationBatchMapper.class),
                    sessions.getMapper(SysOssMigrationItemMapper.class),
                    sessions.getMapper(SysOssConfigMapper.class)) {
                    @Override public boolean updateItem(SysOssMigrationItem item, int version,
                                                        OssMigrationStatus status) {
                        if (item.getStage() == OssMigrationStage.SERVICE_SWITCHED
                            && rejectItemSwitch.compareAndSet(true, false)) return false;
                        boolean updated = super.updateItem(item, version, status);
                        if (updated && OssMigrationAtomicService.CLEANUP_OUTCOME_UNKNOWN.equals(item.getErrorMessage())) {
                            String xid = TransactionContext.getXID();
                            assertThat(xid).as("reservation XID after UNKNOWN CAS").isNotBlank();
                            assertThat(armedReservationXid.compareAndSet(null, xid)).isTrue();
                            reservationArmCount.incrementAndGet();
                        }
                        return updated;
                    }
                    @Override public boolean compareAndSetService(Long id, String expected, String target) {
                        if (rejectPointer.compareAndSet(true, false)) return false;
                        return super.compareAndSetService(id, expected, target);
                    }
                };
                MutableClock clock = new MutableClock(Instant.parse("2026-09-01T00:00:00Z"));
                OssStorageMigrationProperties properties = new OssStorageMigrationProperties();
                properties.setCleanupDelay(Duration.ofMinutes(1));
                properties.setMaxVerifyBytes(1024 * 1024);
                HttpClient http = HttpClient.newHttpClient();
                OssMigrationAccessVerifier verifier = ossId -> {
                    try {
                        String key = scalar(dataSource, "select file_name from sys_oss where oss_id=" + ossId);
                        int status = rawGet(http, endpointUri, publicBucket, key);
                        if (status != 200) {
                            throw new IllegalStateException("anonymous public access failed");
                        }
                    } catch (Exception ex) {
                        throw new IllegalStateException("anonymous public access failed", ex);
                    }
                };
                OssMigrationAtomicService atomic = transactional(new OssMigrationAtomicService(store, clock));
                java.util.concurrent.atomic.AtomicInteger providerDeletes = new java.util.concurrent.atomic.AtomicInteger();
                DefaultOssMigrationObjectStore objects = new DefaultOssMigrationObjectStore() {
                    @Override public void delete(String service, String key, Duration timeout) {
                        providerDeletes.incrementAndGet();
                        super.delete(service, key, timeout);
                    }
                };
                OssStorageMigrationService service = new OssStorageMigrationService(store,
                    objects, verifier, readiness(clock), properties, clock, atomic);

                long cleanupBatch = service.start(new MigrationRequest(List.of(101L), PUBLIC_ROUTE));
                assertMigrated(dataSource, privateClient, publicClient, http, endpointUri, privateBucket,
                    publicBucket, cleanupKey, cleanupBatch);
                assertThatThrownBy(() -> service.cleanup(cleanupBatch, true))
                    .isInstanceOfSatisfying(OssMigrationException.class,
                        ex -> assertThat(ex.error()).isEqualTo(OssMigrationError.CLEANUP_WINDOW_OPEN));
                clock.advance(Duration.ofMinutes(2));
                Throwable acknowledgementFailure = catchThrowable(() -> service.cleanup(cleanupBatch, true));
                assertThat(acknowledgementFailure).isNotNull();
                assertThat(causedBy(acknowledgementFailure, java.sql.SQLRecoverableException.class,
                    "owned commit acknowledgement lost")).isTrue();
                assertThat(armedReservationXid.get()).isNotBlank();
                assertThat(reservationArmCount).hasValue(1);
                assertThat(reservationCommitHit).hasValue(1);
                assertThat(scalar(dataSource, "select error_message from sys_oss_migration_item where oss_id=101"))
                    .isEqualTo(OssMigrationAtomicService.CLEANUP_OUTCOME_UNKNOWN);
                SysOssConfigMapper configMapper = sessions.getMapper(SysOssConfigMapper.class);
                assertThat(configMapper.countOssReferences(PRIVATE_ROUTE)).isPositive();
                assertThat(configMapper.countOssReferences(PUBLIC_ROUTE)).isPositive();
                SysOssConfigBo changedTarget = configEdit(302L, PUBLIC_ROUTE, publicBucket, "2");
                changedTarget.setEndpoint("different-storage.invalid");
                assertThatThrownBy(() -> new SysOssConfigServiceImpl(configMapper).updateByBo(changedTarget))
                    .isInstanceOf(org.namewta.common.core.exception.ServiceException.class)
                    .hasMessageContaining("物理存储身份");
                assertThat(providerDeletes).hasValue(0);
                assertThatThrownBy(() -> service.cleanup(cleanupBatch, true))
                    .isInstanceOf(OssMigrationException.class);
                assertThat(providerDeletes).hasValue(0);
                assertThat(privateClient.headObject(cleanupKey).size()).isPositive();
                privateClient.delete(cleanupKey);
                service.cleanup(cleanupBatch, true);
                assertThat(providerDeletes).hasValue(0);
                assertThat(service.batch(cleanupBatch).status()).isEqualTo(OssMigrationStatus.COMPLETED);
                assertThatThrownBy(() -> privateClient.headObject(cleanupKey));
                assertThat(publicClient.headObject(cleanupKey).size()).isPositive();

                long rollbackBatch = service.start(new MigrationRequest(List.of(102L), PUBLIC_ROUTE));
                service.rollback(rollbackBatch);
                assertThat(service.batch(rollbackBatch).status()).isEqualTo(OssMigrationStatus.ROLLED_BACK);
                assertThat(scalar(dataSource, "select service from sys_oss where oss_id=102"))
                    .isEqualTo(PRIVATE_ROUTE);
                assertThat(privateClient.headObject(rollbackKey).size()).isPositive();
                assertThat(publicClient.headObject(rollbackKey).size()).isPositive();
                assertThat(rawGet(http, endpointUri, privateBucket, rollbackKey)).isEqualTo(403);
                assertThat(store.activeConfigKeys()).isEmpty();

                // Item CAS 失败时，同事务内已经执行的 Object 指针 CAS 必须回滚。
                rejectItemSwitch.set(true);
                assertThatThrownBy(() -> service.publish(102L, PUBLIC_ROUTE))
                    .isInstanceOf(OssMigrationException.class);
                assertThat(scalar(dataSource, "select service from sys_oss where oss_id=102"))
                    .isEqualTo(PRIVATE_ROUTE);
                SysOssMigrationItem failedSwitch = store.findLatest(102L);
                assertThat(failedSwitch.getStage()).isEqualTo(OssMigrationStage.CONTENT_VERIFIED);
                assertThat(failedSwitch.getStatus()).isEqualTo(OssMigrationStatus.RUNNING);
                int durableVersion = failedSwitch.getVersion();
                rejectPointer.set(true);
                assertThatThrownBy(() -> atomic.switchToTarget(failedSwitch.getOssMigrationItemId(), durableVersion))
                    .isInstanceOf(OssMigrationException.class);
                assertThat(scalar(dataSource, "select service from sys_oss where oss_id=102"))
                    .isEqualTo(PRIVATE_ROUTE);
                assertThat(store.findLatest(102L).getVersion()).isEqualTo(durableVersion);
            } finally {
                deleteBuckets(bootstrap, privateBucket, publicBucket, cleanupKey, rollbackKey);
                dropTables(dataSource);
            }
        } finally {
            dataSource.forceCloseAll();
        }
    }

    @Test
    void restoreAndCleanupSerializeOnRealObjectRowAndPreserveCurrentSource() throws Exception {
        String mysqlUrl = System.getProperty("oss.migration.mysql.integration.url");
        String endpoint = System.getProperty("oss.minio.integration.endpoint");
        Assumptions.assumeTrue(mysqlUrl != null && endpoint != null, "需要隔离 MySQL 与 MinIO");
        PooledDataSource dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", mysqlUrl,
            System.getProperty("oss.migration.mysql.integration.username", "root"),
            System.getProperty("oss.migration.mysql.integration.password", ""));
        URI endpointUri = URI.create(endpoint);
        String accessKey = System.getProperty("oss.minio.integration.access-key", "namewta");
        String secretKey = System.getProperty("oss.minio.integration.secret-key", "namewta123");
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String privateBucket = "namewta-migration-private-" + suffix;
        String publicBucket = "namewta-migration-public-" + suffix;
        String missingBucket = "missing-" + suffix;
        String cleanupKey = "migration/cleanup-" + suffix + ".txt";
        String rollbackKey = "migration/rollback-" + suffix + ".txt";
        try (S3Client bootstrap = bootstrap(endpointUri, accessKey, secretKey)) {
            prepareDatabase(dataSource, cleanupKey, rollbackKey, privateBucket, publicBucket);
            prepareBuckets(bootstrap, privateBucket, publicBucket, cleanupKey, rollbackKey);
            try (OssClient privateClient = client(PRIVATE_ROUTE, endpointUri, accessKey, secretKey,
                     privateBucket, AccessPolicy.PRIVATE);
                 OssClient publicClient = client(PUBLIC_ROUTE, endpointUri, accessKey, secretKey,
                     publicBucket, AccessPolicy.PUBLIC_READ);
                 OssClient missingSource = client(PRIVATE_ROUTE, endpointUri, accessKey, secretKey,
                     missingBucket, AccessPolicy.PRIVATE)) {
                DynamicRoutingDataSource routing = new DynamicRoutingDataSource(List.of());
                routing.setPrimary("master"); routing.setStrict(true); routing.addDataSource("master", dataSource);
                SqlSessionTemplate sessions = new SqlSessionTemplate(sqlSessionFactory(routing));
                CountDownLatch objectLocked = new CountDownLatch(1);
                CountDownLatch releaseLock = new CountDownLatch(1);
                CountDownLatch configLocked = new CountDownLatch(1);
                CountDownLatch releaseConfig = new CountDownLatch(1);
                AtomicBoolean pauseConfig = new AtomicBoolean();
                AtomicBoolean pauseRestore = new AtomicBoolean();
                AtomicReference<Long> lockedConnectionId = new AtomicReference<>();
                MybatisOssMigrationStore store = new MybatisOssMigrationStore(sessions.getMapper(SysOssMapper.class),
                    sessions.getMapper(SysOssMigrationBatchMapper.class),
                    sessions.getMapper(SysOssMigrationItemMapper.class),
                    sessions.getMapper(SysOssConfigMapper.class)) {
                    @Override public void lockMigrationConfigs(ConfigIdentity source, ConfigIdentity target) {
                        super.lockMigrationConfigs(source, target);
                        if (pauseConfig.compareAndSet(true, false)) {
                            configLocked.countDown();
                            try {
                                assertThat(releaseConfig.await(10, TimeUnit.SECONDS)).isTrue();
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("test config lock wait interrupted", interrupted);
                            }
                        }
                    }
                    @Override public org.namewta.system.domain.SysOss lockObject(Long id) {
                        var object = super.lockObject(id);
                        if (pauseRestore.compareAndSet(true, false)) {
                            assertThat(TransactionContext.getXID()).isNotNull();
                            lockedConnectionId.set(new org.springframework.jdbc.core.JdbcTemplate(routing)
                                .queryForObject("select connection_id()", Long.class));
                            objectLocked.countDown();
                            try {
                                assertThat(releaseLock.await(10, TimeUnit.SECONDS)).isTrue();
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("test lock wait interrupted", interrupted);
                            }
                        }
                        return object;
                    }
                };
                MutableClock clock = new MutableClock(Instant.parse("2026-09-01T00:00:00Z"));
                OssStorageMigrationProperties properties = new OssStorageMigrationProperties();
                properties.setCleanupDelay(Duration.ofMinutes(1));
                properties.setIoTimeout(Duration.ofSeconds(5));
                properties.setMaxVerifyBytes(1024 * 1024);
                OssMigrationAccessVerifier verifier = id -> {
                    try {
                        String key = scalar(dataSource, "select file_name from sys_oss where oss_id=" + id);
                        assertThat(rawGet(HttpClient.newHttpClient(), endpointUri, publicBucket, key)).isEqualTo(200);
                    } catch (Exception failure) {
                        throw new IllegalStateException("owned public verification failed", failure);
                    }
                };
                CountDownLatch deleteEntered = new CountDownLatch(1);
                CountDownLatch releaseDelete = new CountDownLatch(1);
                CountDownLatch sourceHeadEntered = new CountDownLatch(1);
                CountDownLatch releaseSourceHead = new CountDownLatch(1);
                AtomicBoolean pauseSourceHead = new AtomicBoolean();
                java.util.concurrent.atomic.AtomicInteger physicalDeletes = new java.util.concurrent.atomic.AtomicInteger();
                AtomicReference<Future<?>> lateProvider = new AtomicReference<>();
                ExecutorService provider = Executors.newSingleThreadExecutor();
                DefaultOssMigrationObjectStore objects = new DefaultOssMigrationObjectStore() {
                    @Override public boolean exists(String service, String key, Duration timeout) {
                        boolean present = super.exists(service, key, timeout);
                        if (PRIVATE_ROUTE.equals(service) && cleanupKey.equals(key)
                            && pauseSourceHead.compareAndSet(true, false)) {
                            sourceHeadEntered.countDown();
                            try {
                                assertThat(releaseSourceHead.await(10, TimeUnit.SECONDS)).isTrue();
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("test HEAD wait interrupted", interrupted);
                            }
                        }
                        return present;
                    }
                    @Override public void delete(String service, String key, Duration timeout) {
                        physicalDeletes.incrementAndGet();
                        Future<?> pending = provider.submit(() -> withClients(privateClient, publicClient, () -> {
                            deleteEntered.countDown();
                            try {
                                assertThat(releaseDelete.await(10, TimeUnit.SECONDS)).isTrue();
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("test delete wait interrupted", interrupted);
                            }
                            physicalDelete(service, key, timeout);
                            return null;
                        }));
                        lateProvider.set(pending);
                        try {
                            pending.get(150, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException expected) {
                            throw new IllegalStateException("owned provider acknowledgement timeout", expected);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("test delete wait interrupted", interrupted);
                        } catch (ExecutionException failure) {
                            throw new IllegalStateException("owned provider failed", failure);
                        }
                    }
                    private void physicalDelete(String service, String key, Duration timeout) {
                        super.delete(service, key, timeout);
                    }
                };
                OssStorageMigrationService service = new OssStorageMigrationService(store, objects, verifier,
                    readiness(clock), properties, clock, transactional(new OssMigrationAtomicService(store, clock)));
                ExecutorService callers = Executors.newFixedThreadPool(2);
                try {
                    long rollbackBatch = withClients(privateClient, publicClient,
                        () -> service.publish(102L, PUBLIC_ROUTE));
                    clock.advance(Duration.ofMinutes(2));
                    pauseRestore.set(true);
                    Future<?> restore = callers.submit(() -> withClients(privateClient, publicClient, () -> {
                        service.unpublish(102L); return null;
                    }));
                    assertThat(objectLocked.await(5, TimeUnit.SECONDS)).isTrue();
                    try (Connection second = dataSource.getConnection(); Statement statement = second.createStatement()) {
                        long secondId;
                        try (ResultSet id = statement.executeQuery("select connection_id()")) {
                            assertThat(id.next()).isTrue(); secondId = id.getLong(1);
                        }
                        assertThat(secondId).isNotEqualTo(lockedConnectionId.get());
                        assertThatThrownBy(() -> statement.executeQuery(
                            "select oss_id from sys_oss where oss_id=102 for update nowait"))
                            .isInstanceOfSatisfying(SQLException.class,
                                failure -> assertThat(failure.getErrorCode()).isEqualTo(3572));
                    }
                    releaseLock.countDown();
                    restore.get(5, TimeUnit.SECONDS);
                    assertThat(scalar(dataSource, "select service from sys_oss where oss_id=102"))
                        .isEqualTo(PRIVATE_ROUTE);
                    assertThatThrownBy(() -> withClients(privateClient, publicClient, () -> {
                        service.cleanup(rollbackBatch, true); return null;
                    })).isInstanceOf(OssMigrationException.class);
                    assertThat(privateClient.headObject(rollbackKey).size()).isPositive();

                    long cleanupBatch = withClients(privateClient, publicClient,
                        () -> service.publish(101L, PUBLIC_ROUTE));
                    clock.advance(Duration.ofMinutes(2));
                    pauseSourceHead.set(true);
                    Future<RuntimeException> staleRestore = callers.submit(() -> withClients(privateClient,
                        publicClient, () -> {
                            try { service.unpublish(101L); return null; }
                            catch (RuntimeException rejected) { return rejected; }
                        }));
                    assertThat(sourceHeadEntered.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThatThrownBy(() -> withClients(privateClient, publicClient, () -> {
                        service.cleanup(cleanupBatch, true); return null;
                    })).isInstanceOf(OssMigrationException.class);
                    assertThat(deleteEntered.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThat(scalar(dataSource,
                        "select error_message from sys_oss_migration_item where oss_id=101"))
                        .isEqualTo(OssMigrationAtomicService.CLEANUP_OUTCOME_UNKNOWN);
                    // 对象 HEAD 的 404 来自不存在的 Bucket，绝不可据此完成原工单。
                    assertThatThrownBy(() -> withClients(missingSource, publicClient, () -> {
                        service.cleanup(cleanupBatch, true); return null;
                    })).isInstanceOf(OssMigrationException.class);
                    assertThat(scalar(dataSource,
                        "select error_message from sys_oss_migration_item where oss_id=101"))
                        .isEqualTo(OssMigrationAtomicService.CLEANUP_OUTCOME_UNKNOWN);
                    assertThat(physicalDeletes).hasValue(1);
                    releaseSourceHead.countDown();
                    assertThat(staleRestore.get(5, TimeUnit.SECONDS)).isInstanceOf(OssMigrationException.class);
                    assertThatThrownBy(() -> withClients(privateClient, publicClient, () -> {
                        service.unpublish(101L); return null;
                    })).isInstanceOf(OssMigrationException.class);
                    // 101 的供应商删除尚未完成；不同对象 102 仍可独立建工单并前进。
                    pauseConfig.set(true);
                    Future<Long> unrelated = callers.submit(() -> withClients(privateClient, publicClient,
                        () -> service.publish(102L, PUBLIC_ROUTE)));
                    assertThat(configLocked.await(5, TimeUnit.SECONDS)).isTrue();
                    try (Connection second = dataSource.getConnection(); Statement statement = second.createStatement()) {
                        assertThatThrownBy(() -> statement.executeQuery(
                            "select oss_config_id from sys_oss_config where oss_config_id=302 for update nowait"))
                            .isInstanceOfSatisfying(SQLException.class,
                                failure -> assertThat(failure.getErrorCode()).isEqualTo(3572));
                    }
                    releaseConfig.countDown();
                    assertThat(unrelated.get(5, TimeUnit.SECONDS)).isPositive();
                    assertThat(scalar(dataSource, "select service from sys_oss where oss_id=102"))
                        .isEqualTo(PUBLIC_ROUTE);
                    releaseDelete.countDown();
                    lateProvider.get().get(5, TimeUnit.SECONDS);
                    withClients(privateClient, publicClient, () -> {
                        service.cleanup(cleanupBatch, true); return null;
                    });
                    assertThat(physicalDeletes).hasValue(1);
                    assertThat(scalar(dataSource, "select service from sys_oss where oss_id=101"))
                        .isEqualTo(PUBLIC_ROUTE);
                    assertThatThrownBy(() -> privateClient.headObject(cleanupKey))
                        .isInstanceOfSatisfying(S3StorageException.class,
                            failure -> assertThat(failure.code()).isEqualTo(OssErrorCode.OBJECT_NOT_FOUND));
                    assertThat(publicClient.headObject(cleanupKey).size()).isPositive();
                } finally {
                    releaseLock.countDown(); releaseConfig.countDown();
                    releaseSourceHead.countDown(); releaseDelete.countDown();
                    provider.shutdownNow();
                    callers.shutdownNow();
                    assertThat(provider.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
                    assertThat(callers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
                }
            } finally {
                deleteBuckets(bootstrap, privateBucket, publicBucket, cleanupKey, rollbackKey);
                dropTables(dataSource);
            }
        } finally {
            dataSource.forceCloseAll();
        }
    }

    private <T> T withClients(OssClient source, OssClient target, java.util.concurrent.Callable<T> action) {
        try (MockedStatic<OssFactory> factory = mockStatic(OssFactory.class)) {
            factory.when(() -> OssFactory.instance(PRIVATE_ROUTE)).thenReturn(source);
            factory.when(() -> OssFactory.instance(PUBLIC_ROUTE)).thenReturn(target);
            try {
                return action.call();
            } catch (RuntimeException runtime) {
                throw runtime;
            } catch (Exception checked) {
                throw new IllegalStateException("owned migration action failed", checked);
            }
        }
    }

    private void assertMigrated(PooledDataSource dataSource, OssClient privateClient, OssClient publicClient,
                                HttpClient http, URI endpoint, String privateBucket, String publicBucket,
                                String key, long batchId) throws Exception {
        assertThat(scalar(dataSource, "select service from sys_oss where oss_id=101"))
            .isEqualTo(PUBLIC_ROUTE);
        assertThat(scalar(dataSource, "select count(*) from sys_oss_ref where oss_id=101")).isEqualTo("1");
        assertThat(scalar(dataSource, "select status from sys_oss_migration_item where oss_id=101"))
            .isEqualTo(OssMigrationStatus.CLEANUP_ELIGIBLE.name());
        assertThat(Integer.parseInt(scalar(dataSource,
            "select version from sys_oss_migration_item where oss_id=101"))).isPositive();
        assertThat(privateClient.headObject(key).size()).isEqualTo(publicClient.headObject(key).size());
        assertThat(rawGet(http, endpoint, privateBucket, key)).isEqualTo(403);
        assertThat(rawGet(http, endpoint, publicBucket, key)).isEqualTo(200);
        assertThat(scalar(dataSource, "select count(*) from sys_oss_migration_batch where "
            + "oss_migration_batch_id=" + batchId)).isEqualTo("1");
    }

    private OssStorageReadinessRegistry readiness(Clock clock) {
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        properties.setMaxSnapshotAge("PT10M");
        OssStorageReadinessRegistry registry = new OssStorageReadinessRegistry(properties, clock);
        Instant now = clock.instant();
        registry.replace(Map.of(
            PRIVATE_ROUTE, serving(PRIVATE_ROUTE, AccessPolicy.PRIVATE, now),
            PUBLIC_ROUTE, serving(PUBLIC_ROUTE, AccessPolicy.PUBLIC_READ, now)
        ), Set.of(PRIVATE_ROUTE, PUBLIC_ROUTE), true);
        return registry;
    }

    private OssStorageReadinessEntry serving(String key, AccessPolicy policy, Instant now) {
        return new OssStorageReadinessEntry(key, policy, true, Set.of("OSS_MIGRATION"),
            OssStorageReadinessEntry.Status.SERVING, OssStorageReadinessEntry.Reason.READY, now);
    }

    private SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        Environment environment = new Environment("oss-migration-test", new SpringManagedTransactionFactory(), dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        GlobalConfig globalConfig = GlobalConfigUtils.defaults();
        globalConfig.setBanner(false);
        GlobalConfigUtils.setGlobalConfig(configuration, globalConfig);
        String resource = "mapper/system/SysOssMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        configuration.addMapper(SysOssMigrationBatchMapper.class);
        configuration.addMapper(SysOssMigrationItemMapper.class);
        configuration.addMapper(SysOssConfigMapper.class);
        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    private OssMigrationAtomicService transactional(OssMigrationAtomicService target) {
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(
            new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (OssMigrationAtomicService) proxy.getProxy();
    }

    private void prepareDatabase(PooledDataSource dataSource, String cleanupKey, String rollbackKey,
                                 String privateBucket, String publicBucket) throws Exception {
        dropTables(dataSource);
        for (String table : List.of("sys_oss_config", "sys_oss", "sys_oss_ref")) {
            executeBlock(dataSource, SqlBaselineScripts.createTable(table));
        }
        executeBlock(dataSource, migrationDdlBlock());
        execute(dataSource, "insert into sys_oss_config(oss_config_id,config_key,bucket_name,endpoint,"
            + "access_policy,status) values "
            + "(301,'" + PRIVATE_ROUTE + "','" + privateBucket + "','owned-minio','0','Y'),"
            + "(302,'" + PUBLIC_ROUTE + "','" + publicBucket + "','owned-minio','2','N')");
        execute(dataSource, "insert into sys_oss(oss_id,file_name,original_name,file_suffix,url,service) values"
            + "(101,'" + cleanupKey + "','cleanup.txt','txt','private://cleanup','" + PRIVATE_ROUTE + "'),"
            + "(102,'" + rollbackKey + "','rollback.txt','txt','private://rollback','" + PRIVATE_ROUTE + "')");
        execute(dataSource, "insert into sys_oss_ref(oss_ref_id,oss_id,ref_type,ref_id) values"
            + "(201,101,'portal_asset','A-101'),(202,102,'portal_asset','A-102')");
    }

    private static SysOssConfigBo configEdit(Long id, String key, String bucket, String policy) {
        SysOssConfigBo bo = new SysOssConfigBo();
        bo.setOssConfigId(id);
        bo.setConfigKey(key);
        bo.setBucketName(bucket);
        bo.setEndpoint("owned-minio");
        bo.setIsHttps("N");
        bo.setRegion("");
        bo.setAccessPolicy(policy);
        bo.setStatus("N");
        bo.setAccessKey("owned-access");
        bo.setSecretKey("owned-secret");
        return bo;
    }

    private void prepareBuckets(S3Client client, String privateBucket, String publicBucket,
                                String cleanupKey, String rollbackKey) throws Exception {
        client.createBucket(builder -> builder.bucket(privateBucket));
        client.createBucket(builder -> builder.bucket(publicBucket));
        client.putBucketPolicy(builder -> builder.bucket(publicBucket).policy(publicReadPolicy(publicBucket)));
        client.putObject(builder -> builder.bucket(privateBucket).key(cleanupKey),
            RequestBody.fromString("cleanup-object", StandardCharsets.UTF_8));
        client.putObject(builder -> builder.bucket(privateBucket).key(rollbackKey),
            RequestBody.fromString("rollback-object", StandardCharsets.UTF_8));
    }

    private void deleteBuckets(S3Client client, String privateBucket, String publicBucket,
                               String cleanupKey, String rollbackKey) {
        for (String key : List.of(cleanupKey, rollbackKey)) {
            try { client.deleteObject(builder -> builder.bucket(privateBucket).key(key)); } catch (RuntimeException ignored) { }
            try { client.deleteObject(builder -> builder.bucket(publicBucket).key(key)); } catch (RuntimeException ignored) { }
        }
        try { client.deleteBucketPolicy(builder -> builder.bucket(publicBucket)); } catch (RuntimeException ignored) { }
        try { client.deleteBucket(builder -> builder.bucket(publicBucket)); } catch (RuntimeException ignored) { }
        try { client.deleteBucket(builder -> builder.bucket(privateBucket)); } catch (RuntimeException ignored) { }
    }

    private int rawGet(HttpClient http, URI endpoint, String bucket, String key) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(endpoint + "/" + bucket + "/" + key)).GET().build(),
            HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private S3Client bootstrap(URI endpoint, String accessKey, String secretKey) {
        return S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
    }

    private OssClient client(String configKey, URI endpoint, String accessKey, String secretKey, String bucket,
                             AccessPolicy policy) {
        OssClientConfig config = OssClientConfig.builder()
            .endpoint(endpoint.getAuthority()).useHttps(false).usePathStyleAccess(true)
            .accessKey(accessKey).secretKey(secretKey).bucket(bucket).region(Region.US_EAST_1).prefix("")
            .accessControlPolicyConfig(AccessControlPolicyConfig.builder()
                .enabled(true).accessPolicy(policy).build())
            .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT).build();
        return new DefaultOssClientImpl(configKey, config);
    }

    private String publicReadPolicy(String bucket) {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{"
            + "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},"
            + "\"Action\":[\"s3:GetObject\"],"
            + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }

    private String migrationDdlBlock() throws Exception {
        return SqlBaselineScripts.createTable("sys_oss_migration_batch") + "\n"
            + SqlBaselineScripts.createTable("sys_oss_migration_item");
    }

    private void executeBlock(PooledDataSource dataSource, String sql) throws Exception {
        SqlBaselineScripts.execute(dataSource, sql);
    }

    private static void execute(PooledDataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String scalar(PooledDataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private static boolean causedBy(Throwable failure, Class<? extends Throwable> type, String message) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (type.isInstance(cause) && message.equals(cause.getMessage())) return true;
        }
        return false;
    }

    private static void dropTables(PooledDataSource dataSource) throws Exception {
        for (String table : List.of("sys_oss_migration_item", "sys_oss_migration_batch", "sys_oss_ref",
            "sys_oss", "sys_oss_config")) {
            execute(dataSource, "drop table if exists " + table);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) { this.now = now; }
        private void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
