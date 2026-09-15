package org.namewta.test.migration.ossaccess;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;

import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OssAccessMigrationContractUnitTest {

    private static final String DDL_MARKER = "NAMEWTA-OSS-ACCESS-DDL-001";
    private static final String DML_MARKER = "NAMEWTA-OSS-ACCESS-DML-001";

    @Test
    void ddlDefinesTwoAuditableProjectTablesAndSafePhysicalEncoding() throws Exception {
        String ddl = Files.readString(SqlBaselinePaths.file("10-cde-base-ddl.sql"));
        String block = suffixFrom(ddl, "-- 变更内容：收敛OSS访问类型并新增可审计的存储边界迁移表");
        assertThat(occurrences(ddl, DDL_MARKER)).isEqualTo(1);
        assertThat(block).contains("-- 变更标识：2026-09-01_00:14:13")
            .contains("access_policy char(1) not null default '0'")
            .contains("0=PRIVATE 2=PUBLIC_READ")
            .contains("create table sys_oss_migration_batch")
            .contains("primary key (oss_migration_batch_id)")
            .contains("create table sys_oss_migration_item")
            .contains("primary key (oss_migration_item_id)")
            .contains("unique key uk_sys_oss_migration_item_batch_oss");
        for (String table : List.of("sys_oss_migration_batch", "sys_oss_migration_item")) {
            String tableDdl = tableDefinition(block, table);
            assertThat(tableDdl).contains("version")
                .contains("create_dept")
                .contains("create_time")
                .contains("create_by")
                .contains("update_time")
                .contains("update_by")
                .contains("del_flag")
                .contains("engine=innodb comment='");
        }
    }

    @Test
    void dmlFailsClosedOnBrokenDefaultAndBackfillsEveryHistoricalMeaningToPrivate() throws Exception {
        String dml = Files.readString(SqlBaselinePaths.file("50-cde-base-dml.sql"));
        String block = suffixFrom(dml, "-- 变更内容：将全部历史OSS访问类型保守回填为PRIVATE");
        assertThat(occurrences(dml, DML_MARKER)).isEqualTo(1);
        assertThat(block).contains("-9223372036854775808, '__oss_preflight__', '0'")
            .contains("where (select count(*) from sys_oss_config where status = 'Y') <> 1")
            .contains("union all")
            .contains("update sys_oss_config")
            .contains("set access_policy = '0'")
            .contains("where access_policy <> '0'")
            .doesNotContain("access_policy = '2'")
            .doesNotContain("access_policy = '1'");
    }

    @Test
    void upstreamSchemaRemainsFrozenAndSysOssGetsNoAccessTypeColumn() throws Exception {
        String job = Files.readString(SqlBaselinePaths.file("20-cde-job.sql"));
        assertThat(job).doesNotContain(DDL_MARKER).doesNotContain(DML_MARKER);
        String ddl = Files.readString(SqlBaselinePaths.file("10-cde-base-ddl.sql"));
        assertThat(ddl.toLowerCase()).doesNotContain("alter table sys_oss add column access_type")
            .doesNotContain("alter table sys_oss add column accesstype");
    }

    private static String suffixFrom(String sql, String marker) {
        int index = sql.indexOf(marker);
        assertThat(index).as(marker).isGreaterThanOrEqualTo(0);
        return sql.substring(index);
    }

    private static String tableDefinition(String block, String table) {
        int start = block.indexOf("create table " + table);
        int end = block.indexOf("engine=innodb", start);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return block.substring(start, block.indexOf(';', end) + 1);
    }

    private static int occurrences(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }

}
