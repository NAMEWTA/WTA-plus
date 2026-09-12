package org.namewta.test.profile.schema;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class ProfileSchemaContractTest {

    private static final String DDL_MARKER = "NAMEWTA-PROFILE-DDL-001";
    private static final String DML_MARKER = "NAMEWTA-PROFILE-DML-001";
    private static final List<String> TABLES = List.of(
        "profile_identity_guard", "profile_document_type", "profile_material_node",
        "profile_material_requirement", "profile_person", "profile_person_version",
        "profile_person_application", "profile_person_submission", "profile_person_source",
        "profile_person_binding", "profile_person_binding_event", "profile_enterprise",
        "profile_enterprise_version", "profile_enterprise_application", "profile_enterprise_submission",
        "profile_enterprise_source", "profile_enterprise_binding", "profile_enterprise_binding_event",
        "profile_material_ref", "profile_verification_attempt", "profile_decision_record",
        "profile_operation_audit", "profile_notification_audit", "profile_enterprise_transfer_record"
    );
    private static final List<String> BASE_COLUMNS = List.of(
        "version", "create_dept", "create_time", "create_by", "update_time", "update_by", "del_flag"
    );

    @Test
    void ddlDefinesTheCompleteAppendOnlyProfileModelWithProjectBaseColumns() throws Exception {
        String ddl = readSql("50-namewta-ddl.sql");
        String block = suffixFrom(ddl, DDL_MARKER);

        assertThat(occurrences(ddl, DDL_MARKER)).isEqualTo(1);
        assertThat(block.toLowerCase()).doesNotContain("drop table").doesNotContain("delete from");
        for (String table : TABLES) {
            String definition = tableDefinition(block, table);
            assertThat(definition).contains("primary key (").contains("engine=innodb comment='");
            for (String baseColumn : BASE_COLUMNS) {
                assertThat(definition).as(table + "." + baseColumn).contains(baseColumn);
            }
        }
    }

    @Test
    void generatedGuardsEnforceActiveIdentityApplicationAndBindingCardinality() throws Exception {
        String ddl = suffixFrom(readSql("50-namewta-ddl.sql"), DDL_MARKER);

        assertThat(tableDefinition(ddl, "profile_identity_guard"))
            .contains("active_guard_key")
            .contains("unique key uk_profile_identity_guard_active (active_guard_key)");
        assertThat(tableDefinition(ddl, "profile_person"))
            .contains("active_identity_key")
            .contains("unique key uk_profile_person_active_identity (active_identity_key)");
        assertThat(tableDefinition(ddl, "profile_enterprise"))
            .contains("active_credit_code")
            .contains("unique key uk_profile_enterprise_active_credit (active_credit_code)");
        assertThat(tableDefinition(ddl, "profile_person_version"))
            .contains("current_profile_id")
            .contains("unique key uk_profile_person_version_current (current_profile_id)");
        assertThat(tableDefinition(ddl, "profile_enterprise_version"))
            .contains("current_profile_id")
            .contains("unique key uk_profile_enterprise_version_current (current_profile_id)");
        assertThat(tableDefinition(ddl, "profile_material_node"))
            .contains("node_depth")
            .contains("constraint chk_profile_material_node_shape check");
        assertThat(tableDefinition(ddl, "profile_material_ref"))
            .contains("active_owner_oss_key")
            .contains("unique key uk_profile_material_ref_active_owner_oss (active_owner_oss_key)");
        for (String table : List.of("profile_person_application", "profile_enterprise_application")) {
            assertThat(tableDefinition(ddl, table))
                .contains("open_user_id")
                .contains("open_identity_key")
                .contains("unique key uk_" + table + "_open_user")
                .contains("unique key uk_" + table + "_open_identity");
        }
        for (String table : List.of("profile_person_binding", "profile_enterprise_binding")) {
            assertThat(tableDefinition(ddl, table))
                .contains("effective_user_id")
                .contains("effective_profile_id")
                .contains("unique key uk_" + table + "_effective_user")
                .contains("unique key uk_" + table + "_effective_profile");
        }
    }

    @Test
    void adminSourcesKeepImmutableStructuredFieldsForVersionPublication() throws Exception {
        String ddl = suffixFrom(readSql("50-namewta-ddl.sql"), DDL_MARKER);

        assertThat(tableDefinition(ddl, "profile_person_source")).contains(
            "full_name", "document_type_code", "document_number", "identity_key", "gender",
            "birth_date", "valid_from", "valid_until", "field_snapshot_json", "occurred_time"
        );
        assertThat(tableDefinition(ddl, "profile_enterprise_source")).contains(
            "enterprise_name", "unified_credit_code", "identity_key", "enterprise_type",
            "legal_representative_name", "legal_document_type_code", "legal_document_number",
            "established_date", "business_term_from", "business_term_until", "registered_address",
            "business_scope", "contact_name", "contact_phone", "field_snapshot_json", "occurred_time"
        );
    }

    @Test
    void dmlSeedsClosedRegionalDocumentsProtectedMaterialRulesAndCompleteCapabilities() throws Exception {
        String dml = readSql("60-namewta-dml.sql");
        String block = suffixFrom(dml, DML_MARKER);

        assertThat(occurrences(dml, DML_MARKER)).isEqualTo(1);
        assertThat(block).contains(
            "CN_RESIDENT_ID", "HK_RESIDENT_ID", "MO_RESIDENT_ID", "TW_RESIDENT_ID",
            "HK_MACAO_RESIDENCE_PERMIT", "TW_RESIDENCE_PERMIT",
            "MAINLAND_TRAVEL_PERMIT_HK_MACAO", "MAINLAND_TRAVEL_PERMIT_TW",
            "CN_PASSPORT", "HK_PASSPORT", "MO_PASSPORT", "TW_TRAVEL_DOCUMENT"
        ).contains(
            "PERSON_ID_CARD_PORTRAIT", "PERSON_ID_CARD_EMBLEM", "PERSON_IDENTITY_FRONT",
            "PERSON_IDENTITY_BACK", "PERSON_PASSPORT_DATA_PAGE", "ENTERPRISE_BUSINESS_LICENSE",
            "ENTERPRISE_LEGAL_REPRESENTATIVE_DOCUMENT", "ENTERPRISE_AUTHORIZATION_LETTER"
        ).contains("system_required", "PERSON", "ENTERPRISE", "COMMON")
            .contains("profile.person.flowCode", "profile.enterprise.flowCode")
            .contains("profile_person_verification", "profile_enterprise_verification")
            .contains("profile/person/review", "profile/enterprise/review")
            .contains("insert into flow_definition", "insert into flow_node", "insert into flow_skip")
            .contains("profile.person.provider.default", "profile.enterprise.provider.default", "manual")
            .contains("profile_subject_status", "profile_application_status", "profile_binding_status");

        assertThat(block)
            .contains("'100,100|100,100'", "'300,100|300,100'", "'500,100|500,100'")
            .doesNotContain("'100,100', 'N'", "'300,100', 'Y'", "'500,100', 'N'");
        assertThat(block)
            .contains("'提交', 'PASS', '120,100;250,100'", "'完成', 'PASS', '350,100;480,100'")
            .doesNotContain("'提交', 'PASS', '200,100'", "'完成', 'PASS', '400,100'");

        for (String domain : List.of("person", "enterprise")) {
            for (String capability : List.of("apply", "query", "material", "review", "manage", "override")) {
                assertThat(block).contains("profile:" + domain + ":" + capability);
            }
        }
        assertThat(block).contains("profile:material-tag:query", "profile:material-tag:manage")
            .doesNotContain("profile:person:remove")
            .doesNotContain("profile:enterprise:remove")
            .doesNotContain("profile:person:export")
            .doesNotContain("profile:enterprise:export");
    }

    @Test
    void backendSqlCopiesRemainRetiredAndProfileSqlContainsNoPhysicalHistoryDeletion() throws Exception {
        assertThat(repositoryRoot().resolve("script")).doesNotExist();
        String upstream = Files.readString(sqlDirectory().resolve("10-wta-base.sql"));
        assertThat(upstream).doesNotContain(DDL_MARKER).doesNotContain(DML_MARKER);
        String ddl = suffixFrom(readSql("50-namewta-ddl.sql"), DDL_MARKER).toLowerCase();
        String dml = suffixFrom(readSql("60-namewta-dml.sql"), DML_MARKER).toLowerCase();
        assertThat(ddl).doesNotContain("on delete cascade");
        assertThat(dml).doesNotContain("delete from profile_");
    }

    private static String readSql(String file) throws Exception {
        return Files.readString(sqlDirectory().resolve(file));
    }

    private static Path sqlDirectory() {
        String configuredRoot = System.getProperty("profile.schema.sql.root");
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return Path.of(configuredRoot).resolve("release-artifacts/docker/infrastructure/mysql/init");
        }
        Path current = repositoryRoot();
        while (current != null) {
            Path directory = current.resolve("release-artifacts/docker/infrastructure/mysql/init");
            if (Files.isDirectory(directory)) {
                return directory;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate the aggregate MySQL initialization directory");
    }

    private static String suffixFrom(String sql, String marker) {
        int index = sql.indexOf(marker);
        assertThat(index).as(marker).isGreaterThanOrEqualTo(0);
        return sql.substring(index);
    }

    private static String tableDefinition(String block, String table) {
        int start = block.indexOf("create table " + table + " (");
        int end = block.indexOf("engine=innodb", start);
        assertThat(start).as(table).isGreaterThanOrEqualTo(0);
        assertThat(end).as(table).isGreaterThan(start);
        return block.substring(start, block.indexOf(';', end) + 1);
    }

    private static int occurrences(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }

    private static Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"));
        return current.getFileName().toString().equals("wta-admin") ? current.getParent() : current;
    }
}
