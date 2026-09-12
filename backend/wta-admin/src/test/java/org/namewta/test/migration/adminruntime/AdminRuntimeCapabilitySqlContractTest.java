package org.namewta.test.migration.adminruntime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;

import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class AdminRuntimeCapabilitySqlContractTest {

    private static final String DDL_START = "-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DDL-001";
    private static final String DDL_END = "-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DDL-001-END";
    private static final String DML_START = "-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DML-001";
    private static final String DML_END = "-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DML-001-END";

    @Test
    void definesReplayableGeneratorSchemaRetirement() throws IOException {
        String block = block(Files.readString(SqlBaselinePaths.file("50-namewta-ddl.sql")), DDL_START, DDL_END);

        assertThat(block)
            .contains("information_schema.tables")
            .contains("information_schema.columns")
            .contains("target_table.table_type <> 'BASE TABLE'")
            .contains("primary_column.column_key = 'PRI'")
            .contains("gen_table_column")
            .contains("gen_table")
            .contains("drop table if exists gen_table_column")
            .contains("drop table if exists gen_table");
    }

    @Test
    void convergesMenusWithoutGrantingOrdinaryRoles() throws IOException {
        String block = block(Files.readString(SqlBaselinePaths.file("60-namewta-dml.sql")), DML_START, DML_END);

        assertThat(block)
            .contains("namewta_admin_runtime_reconcile_dml_001_preflight")
            .contains("2094360621561675776")
            .contains("'OpenAPI管理'")
            .contains("'system/openApi/index'")
            .contains("'system:openApi:self'")
            .contains("2094360621561675790")
            .contains("'Nacos配置中心'")
            .contains("1761400000000000002")
            .contains("'nacos', 'monitor/nacos/index'")
            .contains("'system:nacos:console'")
            .contains("1761400000000000003")
            .contains("'系统工具'")
            .contains("1761400000000001060")
            .containsSubsequence(
                "insert into namewta_admin_runtime_reconcile_dml_001_preflight",
                "delete from sys_role_menu",
                "delete from sys_menu",
                "insert into sys_menu",
                "update sys_menu")
            .doesNotContain("insert into sys_role_menu");
    }

    private static String block(String sql, String startMarker, String endMarker) {
        int start = sql.indexOf(startMarker);
        int end = sql.indexOf(endMarker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return sql.substring(start, end + endMarker.length());
    }

}
