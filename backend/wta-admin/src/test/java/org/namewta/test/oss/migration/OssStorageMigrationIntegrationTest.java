package org.namewta.test.oss.migration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.common.oss.client.DefaultOssClientImpl;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.config.AccessControlPolicyConfig;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.migration.*;
import org.namewta.system.oss.migration.mapper.SysOssMigrationBatchMapper;
import org.namewta.system.oss.migration.mapper.SysOssMigrationItemMapper;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.test.support.SqlBaselinePaths;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
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
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
            prepareDatabase(dataSource, cleanupKey, rollbackKey);
            prepareBuckets(bootstrap, privateBucket, publicBucket, cleanupKey, rollbackKey);
            try (OssClient privateClient = client(PRIVATE_ROUTE, endpointUri, accessKey, secretKey,
                privateBucket, AccessPolicy.PRIVATE);
                 OssClient publicClient = client(PUBLIC_ROUTE, endpointUri, accessKey, secretKey,
                     publicBucket, AccessPolicy.PUBLIC_READ);
                 SqlSession session = sqlSessionFactory(dataSource).openSession(true);
                 MockedStatic<OssFactory> factory = mockStatic(OssFactory.class)) {
                factory.when(() -> OssFactory.instance(PRIVATE_ROUTE)).thenReturn(privateClient);
                factory.when(() -> OssFactory.instance(PUBLIC_ROUTE)).thenReturn(publicClient);

                SysOssMapper ossMapper = session.getMapper(SysOssMapper.class);
                MybatisOssMigrationStore store = new MybatisOssMigrationStore(ossMapper,
                    session.getMapper(SysOssMigrationBatchMapper.class),
                    session.getMapper(SysOssMigrationItemMapper.class));
                MutableClock clock = new MutableClock(Instant.parse("2026-09-01T00:00:00Z"));
                OssStorageMigrationProperties properties = new OssStorageMigrationProperties();
                properties.setCleanupDelay(Duration.ofSeconds(1));
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
                OssStorageMigrationService service = new OssStorageMigrationService(store,
                    new DefaultOssMigrationObjectStore(), verifier, readiness(clock), properties, clock);

                long cleanupBatch = service.start(new MigrationRequest(List.of(101L), PUBLIC_ROUTE));
                assertMigrated(dataSource, privateClient, publicClient, http, endpointUri, privateBucket,
                    publicBucket, cleanupKey, cleanupBatch);
                assertThatThrownBy(() -> service.cleanup(cleanupBatch, true))
                    .isInstanceOfSatisfying(OssMigrationException.class,
                        ex -> assertThat(ex.error()).isEqualTo(OssMigrationError.CLEANUP_WINDOW_OPEN));
                clock.advance(Duration.ofSeconds(2));
                service.cleanup(cleanupBatch, true);
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
            } finally {
                deleteBuckets(bootstrap, privateBucket, publicBucket, cleanupKey, rollbackKey);
                dropTables(dataSource);
            }
        } finally {
            dataSource.forceCloseAll();
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
        assertThat(scalar(dataSource, "select version from sys_oss_migration_item where oss_id=101"))
            .isEqualTo("1");
        assertThat(privateClient.headObject(key).size()).isEqualTo(publicClient.headObject(key).size());
        assertThat(rawGet(http, endpoint, privateBucket, key)).isEqualTo(403);
        assertThat(rawGet(http, endpoint, publicBucket, key)).isEqualTo(200);
        assertThat(scalar(dataSource, "select count(*) from sys_oss_migration_batch where "
            + "oss_migration_batch_id=" + batchId)).isEqualTo("1");
    }

    private OssStorageReadinessRegistry readiness(Clock clock) {
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        properties.setMaxSnapshotAge(Duration.ofMinutes(10));
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

    private SqlSessionFactory sqlSessionFactory(PooledDataSource dataSource) throws Exception {
        Environment environment = new Environment("oss-migration-test", new JdbcTransactionFactory(), dataSource);
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
        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    private void prepareDatabase(PooledDataSource dataSource, String cleanupKey, String rollbackKey) throws Exception {
        dropTables(dataSource);
        execute(dataSource, "create table sys_oss_config (oss_config_id bigint not null,"
            + "config_key varchar(20) not null, access_policy char(1) not null default '0',"
            + "primary key(oss_config_id)) engine=innodb");
        execute(dataSource, "create table sys_oss (oss_id bigint not null,file_name varchar(255) not null default '',"
            + "original_name varchar(255) not null default '',file_suffix varchar(10) not null default '',"
            + "url varchar(500) not null,ext1 text default null,create_dept bigint default null,"
            + "create_time datetime default null,create_by bigint default null,update_time datetime default null,"
            + "update_by bigint default null,service varchar(20) not null,is_temp char(1) not null default 'N',"
            + "expire_time datetime default null,delete_state varchar(16) not null default 'ACTIVE',"
            + "primary key(oss_id)) engine=innodb");
        execute(dataSource, "create table sys_oss_ref (oss_ref_id bigint not null,oss_id bigint not null,"
            + "ref_type varchar(64) not null,ref_id varchar(64) not null,version int default 0,"
            + "create_dept bigint default null,create_time datetime default null,create_by bigint default null,"
            + "update_time datetime default null,update_by bigint default null,del_flag char(1) default '0',"
            + "primary key(oss_ref_id)) engine=innodb");
        executeBlock(dataSource, migrationDdlBlock());
        execute(dataSource, "insert into sys_oss(oss_id,file_name,original_name,file_suffix,url,service) values"
            + "(101,'" + cleanupKey + "','cleanup.txt','txt','private://cleanup','" + PRIVATE_ROUTE + "'),"
            + "(102,'" + rollbackKey + "','rollback.txt','txt','private://rollback','" + PRIVATE_ROUTE + "')");
        execute(dataSource, "insert into sys_oss_ref(oss_ref_id,oss_id,ref_type,ref_id) values"
            + "(201,101,'portal_asset','A-101'),(202,102,'portal_asset','A-102')");
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
        String sql = Files.readString(SqlBaselinePaths.file("10-cde-base-ddl.sql"));
        String marker = "-- 变更内容：收敛OSS访问类型并新增可审计的存储边界迁移表";
        int start = sql.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        return sql.substring(start);
    }

    private void executeBlock(PooledDataSource dataSource, String sql) throws Exception {
        String executable = Arrays.stream(sql.split("\\R"))
            .filter(line -> !line.stripLeading().startsWith("--"))
            .collect(Collectors.joining("\n"));
        for (String statement : executable.split(";")) {
            if (!statement.isBlank()) {
                execute(dataSource, statement);
            }
        }
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
