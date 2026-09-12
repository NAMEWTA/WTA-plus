package org.namewta.test.third.schema;

import org.namewta.test.support.SqlBaselinePaths;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class ThirdSchemaMySqlIntegrationTest {

    private static final List<String> LOGICAL_TABLES = List.of(
        "third_provider", "third_endpoint", "third_credential", "third_invocation", "third_statistic");

    @Test
    void mysql84EnforcesOwnerScopeAndAggregateDimensions() throws Exception {
        String url = setting("third.mysql.integration.url", "THIRD_MYSQL_INTEGRATION_URL", null);
        Assumptions.assumeTrue(url != null && !url.isBlank(), "requires a disposable or isolated MySQL JDBC URL");
        String username = setting("third.mysql.integration.username", "THIRD_MYSQL_INTEGRATION_USERNAME", "root");
        String password = setting("third.mysql.integration.password", "THIRD_MYSQL_INTEGRATION_PASSWORD", "");
        boolean exactSchema = Boolean.getBoolean("third.mysql.integration.exact-schema");
        String token = exactSchema ? "third" : "t3it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        List<String> tables = LOGICAL_TABLES.stream()
            .map(name -> exactSchema ? name : name.replace("third", token))
            .toList();

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            try {
                dropTables(connection, tables);
                String baseline = Files.readString(SqlBaselinePaths.file("50-namewta-ddl.sql"));
                String ddl = ThirdSchemaContractTest.block(baseline, "NAMEWTA-THIRD-HTTP-DDL-001");
                executeBlock(connection, exactSchema ? ddl : isolate(ddl, token));

                assertThat(scalar(connection, "select version()" )).startsWith("8.4.");
                assertThat(scalar(connection, "select count(*) from information_schema.tables"
                    + " where table_schema=database() and table_name in (" + quoted(tables) + ")"))
                    .isEqualTo("5");
                verifyConstraints(connection, tables);
            } finally {
                dropTables(connection, tables);
            }
        }
    }

    private static void verifyConstraints(Connection connection, List<String> tables) throws Exception {
        String provider = tables.get(0);
        String endpoint = tables.get(1);
        String credential = tables.get(2);
        String statistic = tables.get(4);

        execute(connection, "insert into " + provider
            + "(provider_id,provider_code,provider_name,base_url) values"
            + "(1,'qcc','QCC','https://example.test'),(2,'other','Other','https://other.test')");
        assertThatThrownBy(() -> execute(connection, "insert into " + provider
            + "(provider_id,provider_code,provider_name,base_url) values(3,'qcc','Duplicate','https://duplicate.test')"))
            .isInstanceOf(SQLException.class);

        execute(connection, "insert into " + endpoint
            + "(endpoint_id,provider_id,provider_code,endpoint_code,endpoint_name,http_method,relative_path)"
            + " values(10,1,'qcc','company-basic','Company Basic','GET','/company/{id}')");
        assertThatThrownBy(() -> execute(connection, "insert into " + endpoint
            + "(endpoint_id,provider_id,provider_code,endpoint_code,endpoint_name,http_method,relative_path)"
            + " values(11,1,'qcc','company-basic','Duplicate','GET','/duplicate')"))
            .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> execute(connection, "insert into " + endpoint
            + "(endpoint_id,provider_id,provider_code,endpoint_code,endpoint_name,http_method,relative_path)"
            + " values(12,99,'missing','orphan','Orphan','GET','/orphan')"))
            .isInstanceOf(SQLException.class);

        insertCredential(connection, credential, 100, 1L, null, "PROVIDER", "API_KEY", "0");
        assertThatThrownBy(() -> insertCredential(connection, credential, 101, 1L, null,
            "PROVIDER", "API_KEY", "0")).isInstanceOf(SQLException.class);
        execute(connection, "update " + credential + " set del_flag='1' where credential_id=100");
        insertCredential(connection, credential, 102, 1L, null, "PROVIDER", "API_KEY", "0");
        insertCredential(connection, credential, 103, 1L, 10L, "ENDPOINT", "API_KEY", "0");
        assertThatThrownBy(() -> insertCredential(connection, credential, 104, 2L, 10L,
            "ENDPOINT", "SECRET", "0")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> insertCredential(connection, credential, 105, null, 10L,
            "ENDPOINT", "SECRET", "0")).isInstanceOf(SQLException.class);

        execute(connection, statisticInsert(statistic, 200, null));
        assertThatThrownBy(() -> execute(connection, statisticInsert(statistic, 201, null)))
            .isInstanceOf(SQLException.class);
        execute(connection, statisticInsert(statistic, 202, "company-basic"));
        assertThatThrownBy(() -> execute(connection, statisticInsert(statistic, 203, "company-basic")))
            .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> execute(connection, statisticInsert(statistic, 204, "")))
            .isInstanceOf(SQLException.class);

        assertThat(scalar(connection, "select count(*) from information_schema.columns"
            + " where table_schema=database() and table_name='" + credential + "'"
            + " and column_name in ('secret','secret_json','plaintext')")).isEqualTo("0");
    }

    private static void insertCredential(Connection connection, String table, long id, Long providerId,
                                         Long endpointId, String scope, String type, String delFlag) throws Exception {
        execute(connection, "insert into " + table
            + "(credential_id,provider_id,endpoint_id,scope_type,credential_type,ciphertext,nonce,auth_tag,kek_version,del_flag) values("
            + id + "," + sqlNumber(providerId) + "," + sqlNumber(endpointId) + ",'" + scope + "','" + type
            + "',x'01',unhex('000102030405060708090a0b'),unhex('000102030405060708090a0b0c0d0e0f'),'v1','"
            + delFlag + "')");
    }

    private static String statisticInsert(String table, long id, String endpointCode) {
        String endpoint = endpointCode == null ? "null" : "'" + endpointCode + "'";
        return "insert into " + table
            + "(statistic_id,provider_code,endpoint_code,stat_date) values(" + id + ",'qcc'," + endpoint + ",'2026-09-05')";
    }

    private static String isolate(String ddl, String token) {
        String isolated = ddl;
        for (String table : LOGICAL_TABLES) isolated = isolated.replace(table, table.replace("third", token));
        return isolated
            .replace("uk_third_", "uk_" + token + "_")
            .replace("idx_third_", "idx_" + token + "_")
            .replace("ck_third_", "ck_" + token + "_")
            .replace("fk_third_", "fk_" + token + "_");
    }

    private static void executeBlock(Connection connection, String sql) throws Exception {
        StringBuilder statement = new StringBuilder();
        for (String line : sql.lines().toList()) {
            if (line.stripLeading().startsWith("--")) continue;
            statement.append(line).append('\n');
        }
        for (String part : statement.toString().split(";")) {
            if (!part.isBlank()) execute(connection, part);
        }
    }

    private static void dropTables(Connection connection, List<String> tables) throws Exception {
        for (int index = tables.size() - 1; index >= 0; index--) {
            execute(connection, "drop table if exists " + tables.get(index));
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String scalar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) throw new SQLException("Query returned no rows");
            return result.getString(1);
        }
    }

    private static String quoted(List<String> values) {
        return values.stream().map(value -> "'" + value + "'").reduce((left, right) -> left + "," + right).orElse("");
    }

    private static String sqlNumber(Long value) {
        return value == null ? "null" : value.toString();
    }

    private static String setting(String property, String environment, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        if (value == null || value.isBlank()) return defaultValue;
        String normalized = value.trim();
        if (normalized.length() >= 2
            && ((normalized.startsWith("\"") && normalized.endsWith("\""))
            || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }
}
