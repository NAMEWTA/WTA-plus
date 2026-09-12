package org.namewta.test.openapi.credential;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OpenApiCredentialSqlContractTest {

    @Test
    void sqlContainsCredentialSchemaAndPermissions() throws Exception {
        byte[] ddl = Files.readAllBytes(SqlBaselinePaths.file("50-namewta-ddl.sql"));
        byte[] dml = Files.readAllBytes(SqlBaselinePaths.file("60-namewta-dml.sql"));

        String ddlText = new String(ddl);
        assertThat(ddlText).contains("create table sys_open_api_credential", "open_api_credential_id",
            "active_owner_user_id", "secret_ciphertext", "secret_nonce", "secret_tag", "kek_version",
            "unique key uk_sys_open_api_credential_active_owner", "unique key uk_sys_open_api_credential_app_key");
        assertThat(ddlText).containsSubsequence(
            "-- NAMEWTA-OPENAPI-CREDENTIAL-DDL-001",
            "-- 变更内容：新增每用户唯一的 OpenAPI 凭据表",
            "-- 变更标识：2026-08-31_22:02:33",
            "version", "create_dept", "create_time", "create_by", "update_time", "update_by", "del_flag");
        String dmlText = new String(dml);
        assertThat(dmlText).contains("system/openApi/index", "system:openApi:list", "system:openApi:query",
            "system:openApi:add", "system:openApi:edit", "system:openApi:remove", "system:openApi:self");
        assertThat(dmlText).containsSubsequence(
            "-- NAMEWTA-OPENAPI-CREDENTIAL-DML-001",
            "-- 变更内容：新增应用开放管理菜单、管理员按钮与个人开放应用权限",
            "-- 变更标识：2026-08-31_22:02:33");
    }

}
