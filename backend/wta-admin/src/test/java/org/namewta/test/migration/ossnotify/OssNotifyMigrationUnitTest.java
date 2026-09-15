package org.namewta.test.migration.ossnotify;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class OssNotifyMigrationUnitTest {

    private static final String DDL_MARKER = "namewta-oss-notify-ddl-001";
    private static final String DSL_MARKER = "namewta-oss-notify-dsl-001";

    @Test
    void ddlDefinesConservativeOssLifecycleAndProjectOwnedTables() throws IOException {
        String ddl = readSql("10-cde-base-ddl.sql");

        assertTrue(ddl.contains(DDL_MARKER));
        String oss = createTable(ddl, "sys_oss");
        assertTrue(oss.contains("is_temp"));
        assertTrue(oss.contains("expire_time"));
        assertTrue(ddl.contains("idx_sys_oss_temp_expire"));
        assertFalse(ddl.contains("add column is_temp"));

        String ossRef = createTable(ddl, "sys_oss_ref");
        assertBaseFields(ossRef);
        assertTrue(ossRef.contains("oss_ref_id"));
        assertTrue(ossRef.contains("ref_type"));
        assertTrue(ossRef.contains("实际物理表名"));
        assertTrue(ossRef.contains("ref_id"));
        assertTrue(ossRef.contains("uk_sys_oss_ref_object"));
        assertFalse(ossRef.contains("client_pk"));

        String notify = createTable(ddl, "notify_intent");
        assertCoreFields(notify);
        assertTrue(notify.contains("intent_id"));
        assertTrue(notify.contains("idempotency_key"));
        assertTrue(notify.contains("content_snapshot"));
        assertTrue(notify.contains("metadata_json"));
        assertFalse(notify.contains("client_pk"));

        String delivery = createTable(ddl, "notify_delivery");
        assertCoreFields(delivery);
        assertTrue(delivery.contains("delivery_id"));
        assertTrue(delivery.contains("recipient_id"));
        assertTrue(delivery.contains("target_value"));
        assertTrue(delivery.contains("provider_message_id"));
        assertTrue(delivery.contains("uk_notify_delivery_provider_message"));
    }

    @Test
    void dmlDefinesIdempotentNotificationMenusAndPermissions() throws IOException {
        String dml = readSql("50-cde-base-dml.sql");

        assertTrue(dml.contains(DSL_MARKER));
        assertTrue(dml.contains("notify/monitor/index"));
        assertTrue(dml.contains("notify:monitor:list"));
        assertTrue(dml.contains("notify:monitor:query"));
        assertTrue(dml.contains("notify:notice:list"));
        assertTrue(dml.contains("notify:inbox:list"));
        assertTrue(dml.contains("notify:notification:submit"));
        assertTrue(dml.contains("perms like 'system:notify:%'"));
        assertTrue(dml.contains("where not exists"));
        assertFalse(dml.substring(dml.indexOf(DSL_MARKER)).contains("client_pk"));
    }

    private void assertBaseFields(String table) {
        for (String field : new String[]{"version", "create_dept", "create_time", "create_by", "update_time", "update_by", "del_flag"}) {
            assertTrue(table.contains(field), () -> "missing project base field: " + field);
        }
    }

    private void assertCoreFields(String table) {
        for (String field : new String[]{"version", "create_dept", "create_time", "create_by", "update_time", "update_by"}) {
            assertTrue(table.contains(field), () -> "missing notification base field: " + field);
        }
    }

    private String createTable(String sql, String tableName) {
        String startToken = "create table " + tableName + " (";
        int start = sql.indexOf(startToken);
        assertTrue(start >= 0, () -> "missing table " + tableName);
        int end = sql.indexOf(") engine=innodb", start);
        assertTrue(end > start, () -> "unterminated table " + tableName);
        return sql.substring(start, end);
    }

    private String readSql(String fileName) throws IOException {
        return Files.readString(SqlBaselinePaths.file(fileName))
            .toLowerCase(Locale.ROOT);
    }
}
