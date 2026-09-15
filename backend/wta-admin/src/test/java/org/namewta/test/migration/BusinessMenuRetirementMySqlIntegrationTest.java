package org.namewta.test.migration;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 占位业务菜单下线迁移的真实 MySQL 幂等验证。
 */
@Tag("dev")
class BusinessMenuRetirementMySqlIntegrationTest {

    private static final String MENU_TABLE = "namewta_test_menu_retirement";
    private static final String ROLE_MENU_TABLE = "namewta_test_role_menu_retirement";
    private static final long[] MENU_IDS = {
        1761400000000002001L,
        1761400000000002002L,
        1761400000000002003L
    };

    @Test
    void disablesPlaceholderMenusAndRemovesDefaultRoleAssignmentsIdempotently() throws Exception {
        String url = System.getProperty("notify.mysql.integration.url");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "需要一次性 MySQL JDBC URL");
        PooledDataSource dataSource = new PooledDataSource(
            "com.mysql.cj.jdbc.Driver",
            url,
            System.getProperty("notify.mysql.integration.username", "root"),
            System.getProperty("notify.mysql.integration.password", "")
        );
        try {
            prepareSchema(dataSource);
            String migration = migrationSql();
            executeMigration(dataSource, migration);
            executeMigration(dataSource, migration);

            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                 var menus = statement.executeQuery(
                     "select count(*) from " + MENU_TABLE + " where menu_id in (1761400000000002001,1761400000000002002,1761400000000002003) and visible='1' and status='1'")) {
                assertTrue(menus.next());
                assertEquals(3, menus.getInt(1));
            }
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                 var assignments = statement.executeQuery("select count(*) from " + ROLE_MENU_TABLE)) {
                assertTrue(assignments.next());
                assertEquals(0, assignments.getInt(1));
            }
        } finally {
            dropSchema(dataSource);
            dataSource.forceCloseAll();
        }
    }

    private void prepareSchema(PooledDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists " + ROLE_MENU_TABLE);
            statement.execute("drop table if exists " + MENU_TABLE);
            statement.execute("create table " + MENU_TABLE + " (menu_id bigint primary key, visible char(1), status char(1), update_by bigint null, update_time datetime null)");
            statement.execute("create table " + ROLE_MENU_TABLE + " (role_id bigint not null, menu_id bigint not null, primary key (role_id, menu_id))");
            for (int index = 0; index < MENU_IDS.length; index++) {
                statement.execute("insert into " + MENU_TABLE + "(menu_id,visible,status) values (" + MENU_IDS[index] + ",'0','0')");
                statement.execute("insert into " + ROLE_MENU_TABLE + "(role_id,menu_id) values (" + (1761300000000000010L + index) + "," + MENU_IDS[index] + ")");
            }
        }
    }

    private void dropSchema(PooledDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists " + ROLE_MENU_TABLE);
            statement.execute("drop table if exists " + MENU_TABLE);
        }
    }

    private String migrationSql() throws Exception {
        String dml = Files.readString(SqlBaselinePaths.file("50-cde-base-dml.sql"));
        int start = dml.indexOf("delete from sys_role_menu", dml.indexOf("NAMEWTA-BASE-DSL-003"));
        int end = dml.indexOf("-- NAMEWTA-BASE-DSL-003-END", start);
        assertTrue(start >= 0 && end > start, "missing NAMEWTA-BASE-DSL-003");
        return dml.substring(start, end)
            .replace("sys_role_menu", ROLE_MENU_TABLE)
            .replace("sys_menu", MENU_TABLE);
    }

    private void executeMigration(PooledDataSource dataSource, String migration) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : migration.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql.trim());
                }
            }
        }
    }

}
