package org.namewta.test.migration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 当前基座删除退役菜单、角色、Client 的真实 MySQL 幂等验证。 */
@Tag("dev")
class BusinessMenuRetirementMySqlIntegrationTest {

    @Test
    void removesRetiredClientsRolesAndMenusWhilePreservingCurrentAssignments() throws Exception {
        String url = System.getProperty("notify.mysql.integration.url");
        assumeTrue(url != null && !url.isBlank(), "需要一次性 MySQL JDBC URL");
        String prefix = "t_menu_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        List<String> created = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url,
            System.getProperty("notify.mysql.integration.username", "root"),
            System.getProperty("notify.mysql.integration.password", ""))) {
            assertThat(scalar(connection, "select database()")).startsWith("namewta_ci");
            try {
                create(connection, created, prefix + "_menu", "menu_id bigint primary key, client_id bigint");
                create(connection, created, prefix + "_role_menu", "role_id bigint, menu_id bigint");
                create(connection, created, prefix + "_user_role", "user_id bigint, role_id bigint");
                create(connection, created, prefix + "_role", "role_id bigint primary key");
                create(connection, created, prefix + "_client", "id bigint primary key");
                execute(connection, "insert into " + prefix + "_client values"
                    + " (1762000000000000001),(1762000000000000002),(1762000000000000003),(1762000000000000004)");
                execute(connection, "insert into " + prefix + "_role values"
                    + " (1761300000000000001),(1761300000000000010),(1761300000000000003),"
                    + " (1761300000000000004),(1761300000000000011),(1761300000000000012)");
                execute(connection, "insert into " + prefix + "_menu values"
                    + " (1761400000000002001,1762000000000000001),(1761400000000002002,1762000000000000001),"
                    + " (1761400000000002003,1762000000000000001),(888,1762000000000000004),(999,1762000000000000002)");
                execute(connection, "insert into " + prefix + "_role_menu values"
                    + " (1761300000000000010,1761400000000002001),(1761300000000000010,1761400000000002002),"
                    + " (1761300000000000010,1761400000000002003),(1761300000000000010,999),"
                    + " (1761300000000000003,888),(1761300000000000004,999),"
                    + " (1761300000000000011,999),(1761300000000000012,999)");
                execute(connection, "insert into " + prefix + "_user_role select 1, role_id from " + prefix + "_role");

                String sql = retirementSql(prefix);
                for (int iteration = 0; iteration < 2; iteration++) {
                    for (String statement : sql.split(";")) {
                        if (!statement.isBlank()) execute(connection, statement);
                    }
                    assertThat(scalar(connection, "select group_concat(menu_id) from " + prefix + "_menu")).isEqualTo("999");
                    assertThat(scalar(connection, "select group_concat(concat(role_id,':',menu_id)) from " + prefix + "_role_menu"))
                        .isEqualTo("1761300000000000010:999");
                    assertThat(scalar(connection, "select group_concat(role_id order by role_id) from " + prefix + "_role"))
                        .isEqualTo("1761300000000000001,1761300000000000010");
                    assertThat(scalar(connection, "select group_concat(role_id order by role_id) from " + prefix + "_user_role"))
                        .isEqualTo("1761300000000000001,1761300000000000010");
                    assertThat(scalar(connection, "select group_concat(id order by id) from " + prefix + "_client"))
                        .isEqualTo("1762000000000000001,1762000000000000002");
                }
            } finally {
                for (String table : created.reversed()) execute(connection, "drop table " + table);
            }
        }
    }

    private static String retirementSql(String prefix) throws Exception {
        String dml = Files.readString(SqlBaselinePaths.file("50-cde-base-dml.sql"));
        int marker = dml.indexOf("-- NAMEWTA-BASE-DSL-004");
        assertThat(marker).isGreaterThanOrEqualTo(0);
        int start = dml.indexOf("delete from sys_role_menu", marker);
        int end = dml.indexOf("insert into sys_menu", start);
        assertThat(start).isGreaterThan(marker);
        assertThat(end).isGreaterThan(start);
        String sql = dml.substring(start, end);
        for (String table : List.of("role_menu", "user_role", "menu", "role", "client")) {
            sql = sql.replace("sys_" + table, prefix + "_" + table);
        }
        assertThat(sql.split(";").length).isGreaterThanOrEqualTo(5);
        return sql;
    }

    private static void create(Connection connection, List<String> created, String name, String columns) throws Exception {
        execute(connection, "create table " + name + " (" + columns + ")");
        created.add(name);
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }

    private static String scalar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }
}
