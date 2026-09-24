package org.namewta.test.migration.ossnotify;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.regex.Pattern;

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
        assertFalse(hasColumn(ossRef, "client_pk"));

        String notify = createTable(ddl, "notify_intent");
        assertCoreFields(notify);
        assertTrue(notify.contains("intent_id"));
        assertTrue(notify.contains("idempotency_key"));
        assertTrue(notify.contains("content_snapshot"));
        assertTrue(notify.contains("metadata_json"));
        assertTrue(hasColumn(notify, "attachment_actor_client_pk"));
        assertFalse(hasColumn(notify, "client_pk"));

        String delivery = createTable(ddl, "notify_delivery");
        assertCoreFields(delivery);
        assertTrue(delivery.contains("delivery_id"));
        assertTrue(delivery.contains("recipient_id"));
        assertTrue(delivery.contains("target_value"));
        assertTrue(delivery.contains("provider_message_id"));
        // 批量短信与邮件允许多个收件人共享流水号，幂等唯一约束属于事件凭据。
        assertTrue(delivery.contains("key idx_notify_delivery_account_message (provider_key, channel, provider_message_id)"));
        assertFalse(delivery.contains("unique key uk_notify_delivery_provider_message"));
        String receipt = createTable(ddl, "notify_provider_receipt");
        assertBaseFields(receipt);
        assertTrue(receipt.contains("unique key uk_notify_provider_receipt_event (event_key)"));
        assertTrue(receipt.contains("facts_hash"));
        assertBaseFields(createTable(ddl, "notify_channel_account"));
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
        assertFalse(containsStandaloneIdentifier(dml.substring(dml.indexOf(DSL_MARKER)), "client_pk"));
    }

    private boolean hasColumn(String table, String column) {
        return Pattern.compile("(?m)^\\s*`?" + column + "`?\\s+").matcher(table).find();
    }

    private boolean containsStandaloneIdentifier(String sql, String identifier) {
        return Pattern.compile("(?<![a-z0-9_])" + identifier + "(?![a-z0-9_])").matcher(sql).find();
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
