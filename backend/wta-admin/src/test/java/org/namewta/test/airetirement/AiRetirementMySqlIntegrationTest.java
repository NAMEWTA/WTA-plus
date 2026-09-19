package org.namewta.test.airetirement;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/** 只读验收 CI 受保护初始化器实际创建的六文件基座，不向已有数据库重放 SQL。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "ai.retirement.mysql.url", matches = ".+")
class AiRetirementMySqlIntegrationTest {
    @Test
    void freshProtectedInitializationHasNoVendorTablesOrMenusAndValidSeedPhones() throws Exception {
        String url = System.getProperty("ai.retirement.mysql.url");
        URI uri = URI.create(url.substring("jdbc:".length()));
        assertThat(uri.getScheme()).isEqualTo("mysql");
        assertThat(uri.getHost()).isEqualTo("127.0.0.1");
        assertThat(uri.getPath()).isEqualTo("/wta-plus");
        try (Connection connection = DriverManager.getConnection(url,
            System.getProperty("ai.retirement.mysql.username"),
            System.getProperty("ai.retirement.mysql.password"))) {
            assertThat(count(connection, "select count(*) from information_schema.tables where table_schema=database()"))
                .isEqualTo(103);
            assertThat(count(connection, "select count(*) from information_schema.tables where table_schema=database()"
                + " and left(table_name,4)='sai_'")).isZero();
            assertThat(count(connection, "select count(*) from information_schema.tables where table_schema='nacos'"))
                .isEqualTo(10);
            assertThat(count(connection, "select count(*) from sys_menu where component in ('ai/chat/index','monitor/snailai/index')"))
                .isZero();
            assertThat(count(connection, "select count(*) from sys_menu where component='monitor/snailjob/index'"))
                .isEqualTo(1);
            assertThat(count(connection, "select count(*) from sys_user")).isGreaterThan(0);
            assertThat(count(connection, "select count(*) from sys_user where phone_number is null or phone_number not regexp '^1[3-9][0-9]{9}$'"))
                .isZero();
        }
    }

    private static int count(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getInt(1);
        }
    }
}
