package org.namewta.test.migration.ossaccess;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.system.domain.SysOssConfig;
import org.namewta.system.domain.bo.SysOssConfigBo;
import org.namewta.system.mapper.SysOssConfigMapper;
import org.namewta.system.service.impl.SysOssConfigServiceImpl;
import org.namewta.test.support.SqlBaselinePaths;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OSS访问类型变更在真实MySQL上的fresh、upgrade、重复执行和失败关闭验证。
 */
@Tag("dev")
class OssAccessMigrationMySqlIntegrationTest {

    private static final String CONFIG_TABLE = "sys_oss_config";
    private static final String OSS_TABLE = "sys_oss";
    private static final String BATCH_TABLE = "sys_oss_migration_batch";
    private static final String ITEM_TABLE = "sys_oss_migration_item";

    @Test
    void migratesFreshAndUpgradeSafelyAndFailsBeforeBackfillWhenDefaultIsAmbiguous() throws Exception {
        String url = System.getProperty("oss.access.migration.mysql.integration.url");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "需要一次性隔离MySQL JDBC URL");
        PooledDataSource dataSource = new PooledDataSource(
            "com.mysql.cj.jdbc.Driver",
            url,
            System.getProperty("oss.access.migration.mysql.integration.username", "root"),
            System.getProperty("oss.access.migration.mysql.integration.password", "")
        );
        try {
            freshAndRepeat(dataSource);
            upgradePreservesObjectIdentityAndOwnership(dataSource);
            ambiguousDefaultFailsBeforeBackfill(dataSource);
            missingDefaultFailsBeforeBackfill(dataSource);
            serviceProtectsReferencedBoundaryAndAllowsCredentialRotation(dataSource);
        } finally {
            dropTables(dataSource);
            dataSource.forceCloseAll();
        }
    }

    private void freshAndRepeat(PooledDataSource dataSource) throws Exception {
        prepareBaseline(dataSource);
        execute(dataSource, "insert into " + CONFIG_TABLE
            + "(oss_config_id,config_key,access_policy,status) values"
            + "(1,'minio','1','Y'),(2,'qiniu','1','N'),(3,'aliyun','1','N')");
        executeBlock(dataSource, ddlBlock());
        executeBlock(dataSource, dmlBlock());
        executeBlock(dataSource, dmlBlock());

        assertEquals("3", scalar(dataSource,
            "select count(*) from " + CONFIG_TABLE + " where access_policy='0'"));
        assertEquals("1", scalar(dataSource,
            "select count(*) from " + CONFIG_TABLE + " where status='Y'"));
        assertMigrationSchema(dataSource, BATCH_TABLE, "OSS存储边界迁移批次表");
        assertMigrationSchema(dataSource, ITEM_TABLE, "OSS存储边界迁移明细表");
        assertEquals("1", scalar(dataSource,
            "select count(distinct index_name) from information_schema.statistics where table_schema=database()"
                + " and table_name='" + ITEM_TABLE + "'"
                + " and index_name='uk_sys_oss_migration_item_batch_oss' and non_unique=0"));
    }

    private void upgradePreservesObjectIdentityAndOwnership(PooledDataSource dataSource) throws Exception {
        prepareBaseline(dataSource);
        execute(dataSource, "insert into " + CONFIG_TABLE
            + "(oss_config_id,config_key,access_policy,status) values"
            + "(11,'private-old','0','Y'),(12,'legacy-public','1','N'),"
            + "(13,'legacy-custom','2','N'),(14,'unknown','9','N')");
        execute(dataSource, "insert into " + OSS_TABLE + "(oss_id,service) values"
            + "(101,'private-old'),(102,'legacy-public'),(103,'unknown')");
        executeBlock(dataSource, ddlBlock());
        executeBlock(dataSource, dmlBlock());

        assertEquals("4", scalar(dataSource,
            "select count(*) from " + CONFIG_TABLE + " where access_policy='0'"));
        assertEquals("0", scalar(dataSource,
            "select count(*) from " + CONFIG_TABLE + " where access_policy<>'0'"));
        assertEquals("101:private-old,102:legacy-public,103:unknown", scalar(dataSource,
            "select group_concat(concat(oss_id,':',service) order by oss_id separator ',') from " + OSS_TABLE));
    }

    private void ambiguousDefaultFailsBeforeBackfill(PooledDataSource dataSource) throws Exception {
        prepareBaseline(dataSource);
        execute(dataSource, "insert into " + CONFIG_TABLE
            + "(oss_config_id,config_key,access_policy,status) values"
            + "(21,'first','1','Y'),(22,'second','2','Y')");
        executeBlock(dataSource, ddlBlock());

        assertThrows(SQLException.class, () -> executeBlock(dataSource, dmlBlock()));
        assertEquals("1,2", scalar(dataSource,
            "select group_concat(access_policy order by oss_config_id separator ',') from " + CONFIG_TABLE));
    }

    private void missingDefaultFailsBeforeBackfill(PooledDataSource dataSource) throws Exception {
        prepareBaseline(dataSource);
        executeBlock(dataSource, ddlBlock());
        assertThrows(SQLException.class, () -> executeBlock(dataSource, dmlBlock()));
        assertEquals("0", scalar(dataSource, "select count(*) from " + CONFIG_TABLE));
    }

    private void serviceProtectsReferencedBoundaryAndAllowsCredentialRotation(PooledDataSource dataSource)
        throws Exception {
        prepareBaseline(dataSource);
        execute(dataSource, "insert into " + CONFIG_TABLE
            + "(oss_config_id,config_key,access_key,secret_key,bucket_name,endpoint,is_https,access_policy,status)"
            + " values(31,'private-main','old-access','old-secret','private-bucket','old.endpoint','Y','0','N'),"
            + "(32,'private-default','default-access','default-secret','default-bucket','default.endpoint','Y','0','Y')");
        execute(dataSource, "insert into " + OSS_TABLE + "(oss_id,service) values(301,'private-main')");
        SqlSessionFactory factory = sqlSessionFactory(dataSource);
        try (SqlSession session = factory.openSession(true)) {
            SysOssConfigMapper mapper = session.getMapper(SysOssConfigMapper.class);
            SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
            SysOssConfigBo boundaryEdit = editRequest("public-bucket", "old.endpoint", null);
            assertThrows(ServiceException.class, () -> service.updateByBo(boundaryEdit));
            assertEquals("private-bucket", scalar(dataSource,
                "select bucket_name from " + CONFIG_TABLE + " where oss_config_id=31"));
            assertThrows(ServiceException.class, () -> service.deleteWithValidByIds(java.util.List.of(31L), true));
            assertEquals("1", scalar(dataSource,
                "select count(*) from " + CONFIG_TABLE + " where oss_config_id=31"));

            SysOssConfigBo safeRotation = editRequest("private-bucket", "new.endpoint", null);
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
                context.registerBean(SpringUtils.class);
                context.refresh();
                assertTrue(service.updateByBo(safeRotation));
            }
            assertEquals("new.endpoint", scalar(dataSource,
                "select endpoint from " + CONFIG_TABLE + " where oss_config_id=31"));
            assertEquals("old-secret", scalar(dataSource,
                "select secret_key from " + CONFIG_TABLE + " where oss_config_id=31"));
            SysOssConfig persisted = mapper.selectById(31L);
            assertEquals("private-bucket", persisted.getBucketName());
            assertEquals("0", persisted.getAccessPolicy());
        }
    }

    private SysOssConfigBo editRequest(String bucketName, String endpoint, String secretKey) {
        SysOssConfigBo bo = new SysOssConfigBo();
        bo.setOssConfigId(31L);
        bo.setConfigKey("private-main");
        bo.setAccessKey("new-access");
        bo.setSecretKey(secretKey);
        bo.setBucketName(bucketName);
        bo.setEndpoint(endpoint);
        bo.setIsHttps("Y");
        bo.setAccessPolicy("0");
        bo.setStatus("N");
        return bo;
    }

    private void prepareBaseline(PooledDataSource dataSource) throws Exception {
        dropTables(dataSource);
        execute(dataSource, "create table " + CONFIG_TABLE + " ("
            + "oss_config_id bigint not null,config_key varchar(20) not null default '',"
            + "access_key varchar(255) default '',secret_key varchar(255) default '',"
            + "bucket_name varchar(255) default '',prefix varchar(255) default '',"
            + "endpoint varchar(255) default '',domain_url varchar(255) default '',"
            + "is_https char(1) default 'N',region varchar(255) default '',"
            + "access_policy char(1) not null default '1',status char(1) default 'N',"
            + "ext1 varchar(255) default '',create_dept bigint default null,create_by bigint default null,"
            + "create_time datetime default null,update_by bigint default null,update_time datetime default null,"
            + "remark varchar(500) default null,"
            + "primary key(oss_config_id)) engine=innodb comment='OSS测试配置表'");
        execute(dataSource, "create table " + OSS_TABLE + " ("
            + "oss_id bigint not null,service varchar(20) not null,primary key(oss_id)) engine=innodb");
    }

    private SqlSessionFactory sqlSessionFactory(PooledDataSource dataSource) {
        Environment environment = new Environment("oss-config-test", new JdbcTransactionFactory(), dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        GlobalConfig globalConfig = GlobalConfigUtils.defaults();
        globalConfig.setBanner(false);
        GlobalConfigUtils.setGlobalConfig(configuration, globalConfig);
        configuration.addMapper(SysOssConfigMapper.class);
        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    private void assertMigrationSchema(PooledDataSource dataSource, String table, String expectedComment)
        throws Exception {
        assertEquals(expectedComment, scalar(dataSource,
            "select table_comment from information_schema.tables where table_schema=database()"
                + " and table_name='" + table + "'"));
        for (String column : new String[]{"version", "create_dept", "create_time", "create_by",
            "update_time", "update_by", "del_flag"}) {
            assertEquals("1", scalar(dataSource,
                "select count(*) from information_schema.columns where table_schema=database()"
                    + " and table_name='" + table + "' and column_name='" + column + "'"
                    + " and column_comment<>''"));
        }
    }

    private String ddlBlock() throws Exception {
        return rewriteTableNames(block(SqlBaselinePaths.file("50-namewta-ddl.sql"),
            "-- 变更内容：收敛OSS访问类型并新增可审计的存储边界迁移表"));
    }

    private String dmlBlock() throws Exception {
        return rewriteTableNames(block(SqlBaselinePaths.file("60-namewta-dml.sql"),
            "-- 变更内容：将全部历史OSS访问类型保守回填为PRIVATE"));
    }

    private static String block(Path path, String marker) throws Exception {
        String sql = Files.readString(path);
        int start = sql.indexOf(marker);
        assertTrue(start >= 0, marker);
        return sql.substring(start);
    }

    private static String rewriteTableNames(String sql) {
        return sql.replace("sys_oss_migration_batch", BATCH_TABLE)
            .replace("sys_oss_migration_item", ITEM_TABLE)
            .replace("sys_oss_config", CONFIG_TABLE);
    }

    private static void executeBlock(PooledDataSource dataSource, String sql) throws Exception {
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
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next(), sql);
            return resultSet.getString(1);
        }
    }

    private static void dropTables(PooledDataSource dataSource) throws Exception {
        for (String table : new String[]{ITEM_TABLE, BATCH_TABLE, OSS_TABLE, CONFIG_TABLE}) {
            try {
                execute(dataSource, "drop table if exists " + table);
            } catch (SQLException exception) {
                if (!exception.getMessage().toLowerCase(Locale.ROOT).contains("unknown table")) {
                    throw exception;
                }
            }
        }
    }

}
