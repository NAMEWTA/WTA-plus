package org.namewta.test.third.schema;

import org.namewta.test.support.SqlBaselinePaths;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class ThirdSchemaContractTest {

    @Test
    void baselineOwnsNullSafeCredentialAndStatisticUniqueness() throws Exception {
        String ddl = Files.readString(SqlBaselinePaths.file("10-cde-base-ddl.sql"));
        String block = block(ddl, "NAMEWTA-THIRD-HTTP-DDL-001").toLowerCase();

        assertThat(block)
            .contains("create table third_provider")
            .contains("create table third_endpoint")
            .contains("create table third_credential")
            .contains("create table third_invocation")
            .contains("create table third_statistic")
            .contains("active_scope_key varchar(192) generated always as")
            .contains("unique key uk_third_credential_active_scope(active_scope_key)")
            .contains("foreign key(provider_id, endpoint_id) references third_endpoint(provider_id, endpoint_id)")
            .contains("endpoint_dimension varchar(64) generated always as (ifnull(endpoint_code, '')) stored")
            .contains("unique key uk_third_statistic_dimension(provider_code, endpoint_dimension, stat_date)")
            .doesNotContain("unique key uk_third_credential_scope(scope_type, provider_id, endpoint_id, credential_type)")
            .doesNotContain("unique key uk_third_statistic_dimension(provider_code, endpoint_code, stat_date)");
    }

    static String block(String sql, String marker) {
        int start = sql.indexOf("-- " + marker);
        if (start < 0) throw new IllegalArgumentException("Missing SQL marker " + marker);
        int next = sql.indexOf("\n-- NAMEWTA-", start + marker.length() + 3);
        return next < 0 ? sql.substring(start) : sql.substring(start, next);
    }
}
