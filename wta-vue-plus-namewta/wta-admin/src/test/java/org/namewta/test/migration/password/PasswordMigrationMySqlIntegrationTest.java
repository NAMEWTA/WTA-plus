package org.namewta.test.migration.password;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.namewta.system.password.PasswordDefaultMode;
import org.namewta.system.password.PasswordPolicyConfigParser;
import org.namewta.test.support.SqlBaselinePaths;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NAMEWTA-PASSWORD-DSL-001 在真实 MySQL 上的 fresh、upgrade、失败关闭与恢复演练。
 */
@Tag("dev")
class PasswordMigrationMySqlIntegrationTest {

    private static final String CONFIG_TABLE = "namewta_t08_password_config";
    private static final String MENU_TABLE = "namewta_t08_password_menu";
    private static final String PREFLIGHT_TABLE = "namewta_t08_password_preflight";
    private static final long POLICY_ID = 2093282875312267265L;
    private static final long PERMISSION_ID = 2093282875312267266L;
    private static final Pattern COMPLIANT_LEGACY = Pattern.compile(
        "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[@$!%*?&])[A-Za-z0-9@$!%*?&]{8,30}$");

    @Test
    void migratesFreshAndUpgradeDatabasesIdempotentlyAndSupportsRecovery() throws Exception {
        String url = System.getProperty("password.migration.mysql.integration.url");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "需要一次性 MySQL JDBC URL");
        PooledDataSource dataSource = new PooledDataSource(
            "com.mysql.cj.jdbc.Driver",
            url,
            System.getProperty("password.migration.mysql.integration.username", "root"),
            System.getProperty("password.migration.mysql.integration.password", "")
        );

        try {
            freshAndRepeat(dataSource);
            upgradeAndRollback(dataSource);
            conflictingPreflightStopsBeforeWrites(dataSource);
        } finally {
            dropTables(dataSource);
            dataSource.forceCloseAll();
        }
    }

    private void freshAndRepeat(PooledDataSource dataSource) throws Exception {
        prepareBaseline(dataSource, "123456", "upstream baseline");
        executeMigration(dataSource);

        String firstLegacy = scalar(dataSource,
            "select config_value from " + CONFIG_TABLE + " where config_key='sys.user.initPassword'");
        assertTrue(COMPLIANT_LEGACY.matcher(firstLegacy).matches());
        assertNotEquals("123456", firstLegacy);
        assertPolicyAndPermission(dataSource);

        executeMigration(dataSource);
        assertEquals(firstLegacy, scalar(dataSource,
            "select config_value from " + CONFIG_TABLE + " where config_key='sys.user.initPassword'"));
        assertEquals("1", scalar(dataSource,
            "select count(*) from " + CONFIG_TABLE + " where config_key='sys.user.passwordPolicy'"));
        assertEquals("1", scalar(dataSource,
            "select count(*) from " + MENU_TABLE + " where perms='system:user:temporaryPassword'"));
    }

    private void upgradeAndRollback(PooledDataSource dataSource) throws Exception {
        String previousValue = "Upgrade9!Legacy";
        String previousRemark = "operator-owned compatibility value";
        prepareBaseline(dataSource, previousValue, previousRemark);
        execute(dataSource, "insert into " + CONFIG_TABLE
            + "(config_id,config_name,config_key,config_value,config_type,remark)"
            + " values(777,'sentinel','namewta.test.sentinel','preserve-me','N','upgrade data')");

        executeMigration(dataSource);
        String migratedValue = scalar(dataSource,
            "select config_value from " + CONFIG_TABLE + " where config_key='sys.user.initPassword'");
        assertTrue(COMPLIANT_LEGACY.matcher(migratedValue).matches());
        assertNotEquals(previousValue, migratedValue);
        assertEquals("preserve-me", scalar(dataSource,
            "select config_value from " + CONFIG_TABLE + " where config_id=777"));
        assertPolicyAndPermission(dataSource);

        execute(dataSource, "delete from " + MENU_TABLE + " where menu_id=" + PERMISSION_ID);
        execute(dataSource, "delete from " + CONFIG_TABLE + " where config_id=" + POLICY_ID);
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(
            "update " + CONFIG_TABLE + " set config_value=?,remark=? where config_key='sys.user.initPassword'")) {
            statement.setString(1, previousValue);
            statement.setString(2, previousRemark);
            assertEquals(1, statement.executeUpdate());
        }

        assertEquals(previousValue, scalar(dataSource,
            "select config_value from " + CONFIG_TABLE + " where config_key='sys.user.initPassword'"));
        assertEquals(previousRemark, scalar(dataSource,
            "select remark from " + CONFIG_TABLE + " where config_key='sys.user.initPassword'"));
        assertEquals("0", scalar(dataSource,
            "select count(*) from " + CONFIG_TABLE + " where config_id=" + POLICY_ID));
        assertEquals("0", scalar(dataSource,
            "select count(*) from " + MENU_TABLE + " where menu_id=" + PERMISSION_ID));
        assertEquals("preserve-me", scalar(dataSource,
            "select config_value from " + CONFIG_TABLE + " where config_id=777"));
    }

    private void conflictingPreflightStopsBeforeWrites(PooledDataSource dataSource) throws Exception {
        prepareBaseline(dataSource, "Before9!Conflict", "must stay unchanged");
        execute(dataSource, "insert into " + CONFIG_TABLE
            + "(config_id,config_name,config_key,config_value,config_type)"
            + " values(888,'collision','sys.user.passwordPolicy','{}','Y')");

        assertThrows(SQLException.class, () -> executeMigration(dataSource));
        assertEquals("Before9!Conflict", scalar(dataSource,
            "select config_value from " + CONFIG_TABLE + " where config_key='sys.user.initPassword'"));
        assertEquals("0", scalar(dataSource,
            "select count(*) from " + MENU_TABLE + " where menu_id=" + PERMISSION_ID));

        prepareBaseline(dataSource, "Before8!Null", "null collision must stop");
        execute(dataSource, "insert into " + MENU_TABLE + "(menu_id,menu_name) values("
            + PERMISSION_ID + ",'null permission collision')");
        assertThrows(SQLException.class, () -> executeMigration(dataSource));
        assertEquals("Before8!Null", scalar(dataSource,
            "select config_value from " + CONFIG_TABLE + " where config_key='sys.user.initPassword'"));
        assertEquals("0", scalar(dataSource,
            "select count(*) from " + CONFIG_TABLE + " where config_id=" + POLICY_ID));
    }

    private void assertPolicyAndPermission(PooledDataSource dataSource) throws Exception {
        String json = scalar(dataSource,
            "select config_value from " + CONFIG_TABLE + " where config_id=" + POLICY_ID);
        assertTrue(json.length() < 500);
        assertEquals(PasswordDefaultMode.RANDOM,
            new PasswordPolicyConfigParser(JsonMapper.builder().build()).parse(json).defaultPassword().mode());
        assertEquals("system:user:temporaryPassword", scalar(dataSource,
            "select perms from " + MENU_TABLE + " where menu_id=" + PERMISSION_ID));
        assertEquals("1761400000000000100", scalar(dataSource,
            "select parent_id from " + MENU_TABLE + " where menu_id=" + PERMISSION_ID));
    }

    private void prepareBaseline(PooledDataSource dataSource, String legacyValue, String legacyRemark)
        throws Exception {
        dropTables(dataSource);
        execute(dataSource, "create table " + CONFIG_TABLE + " ("
            + "config_id bigint not null primary key,config_name varchar(100),config_key varchar(100),"
            + "config_value varchar(500),config_type char(1),create_dept bigint,create_by bigint,"
            + "create_time datetime,update_by bigint,update_time datetime,remark varchar(500)) engine=innodb");
        execute(dataSource, "create table " + MENU_TABLE + " ("
            + "menu_id bigint not null primary key,client_id bigint,menu_name varchar(50),parent_id bigint,"
            + "order_num int,path varchar(200),component varchar(255),query_param varchar(255),"
            + "is_frame char(1),is_cache char(1),menu_type char(1),visible char(1),status char(1),"
            + "perms varchar(100),icon varchar(100),active_menu varchar(255),ext varchar(2000),"
            + "create_dept bigint,create_by bigint,create_time datetime,remark varchar(500)) engine=innodb");
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(
            "insert into " + CONFIG_TABLE
                + "(config_id,config_name,config_key,config_value,config_type,remark)"
                + " values(1761700000000000001,'legacy','sys.user.initPassword',?,'Y',?)")) {
            statement.setString(1, legacyValue);
            statement.setString(2, legacyRemark);
            statement.executeUpdate();
        }
        execute(dataSource, "insert into " + MENU_TABLE
            + "(menu_id,client_id,menu_name,parent_id,menu_type,perms)"
            + " values(1761400000000000100,1762000000000000001,'user',1761400000000000001,'C','system:user:list')");
    }

    private void executeMigration(PooledDataSource dataSource) throws Exception {
        String sql = migrationSql();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try {
                for (String command : sql.split(";")) {
                    if (!command.isBlank()) {
                        statement.execute(command.trim());
                    }
                }
            } finally {
                statement.execute("drop temporary table if exists " + PREFLIGHT_TABLE);
            }
        }
    }

    private String migrationSql() throws Exception {
        String dml = Files.readString(SqlBaselinePaths.file("60-namewta-dml.sql"));
        int start = dml.indexOf("-- NAMEWTA-PASSWORD-DSL-001\n");
        int end = dml.indexOf("-- NAMEWTA-PASSWORD-DSL-001-END", start);
        assertTrue(start >= 0 && end > start);
        return dml.substring(start, end).lines()
            .filter(line -> !line.stripLeading().startsWith("--"))
            .collect(Collectors.joining("\n"))
            .replace("namewta_password_dsl_001_preflight", PREFLIGHT_TABLE)
            .replace("sys_config", CONFIG_TABLE)
            .replace("sys_menu", MENU_TABLE);
    }

    private void execute(PooledDataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String scalar(PooledDataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            assertTrue(result.next(), () -> "query returned no row: " + sql.toLowerCase(Locale.ROOT));
            return result.getString(1);
        }
    }

    private void dropTables(PooledDataSource dataSource) throws Exception {
        execute(dataSource, "drop table if exists " + MENU_TABLE);
        execute(dataSource, "drop table if exists " + CONFIG_TABLE);
    }

}
