package org.namewta.test.profile.schema;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;
import org.namewta.test.support.SqlBaselineScripts;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ProfileSchemaMySqlIntegrationTest {

    @Test
    void freshSchemaAndSeedsEnforceReleaseAndRecreateSemantics() throws Exception {
        String url = System.getProperty("profile.schema.mysql.integration.url");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "需要一次性隔离MySQL JDBC URL");
        PooledDataSource dataSource = new PooledDataSource(
            "com.mysql.cj.jdbc.Driver", url,
            System.getProperty("profile.schema.mysql.integration.username", "root"),
            System.getProperty("profile.schema.mysql.integration.password", "")
        );
        boolean ownsSchema = false;
        try {
            assertTrue(scalar(dataSource, "select database()").startsWith("namewta_profile_schema_test_"),
                "必须使用本测试专属数据库");
            assertEquals("0", scalar(dataSource,
                "select count(*) from information_schema.tables where table_schema=database()"));
            ownsSchema = true;
            // 完整业务基座是唯一事实源；不复制简化表，也不把字面量内分号当作语句边界。
            for (String file : List.of("10-cde-base-ddl.sql", "20-cde-job.sql", "30-cde-workflow.sql",
                "40-cde-ai.sql", "50-cde-base-dml.sql")) {
                SqlBaselineScripts.execute(dataSource, Files.readString(SqlBaselinePaths.file(file)));
            }
            assertEquals("24", scalar(dataSource,
                "select count(*) from information_schema.tables where table_schema=database() and table_name like 'profile\\_%'"));
            assertEquals("14", scalar(dataSource,
                "select count(*) from sys_menu where client_id=1762000000000000001 and perms like 'profile:%'"));
            assertEquals("4", scalar(dataSource,
                "select count(*) from sys_menu where client_id=1762000000000000002 and perms like 'profile:%'"));
            assertEquals("0", scalar(dataSource,
                "select count(*) from sys_menu where client_id=1762000000000000002 and perms like 'profile:%'"
                    + " and perms not in ('profile:person:apply','profile:person:material',"
                    + "'profile:enterprise:apply','profile:enterprise:material')"));
            assertEquals("12", scalar(dataSource, "select count(*) from profile_document_type"));
            assertEquals("8", scalar(dataSource,
                "select count(*) from profile_material_node where node_type='TAG' and system_required='Y'"));
            assertEquals("3", scalar(dataSource,
                "select count(*) from sys_dict_type where dict_type like 'profile\\_%'"));
            assertEquals("12", scalar(dataSource,
                "select count(*) from sys_dict_data where dict_type like 'profile\\_%'"));
            assertEquals("2", scalar(dataSource,
                "select count(*) from flow_definition where flow_code like 'profile\\_%\\_verification'"
                    + " and is_publish=1 and activity_status=1"));
            assertEquals("2", scalar(dataSource,
                "select count(*) from flow_node where node_code in ('person_review','enterprise_review')"
                    + " and permission_flag='role:1761300000000000001' and form_custom='Y'"));
            assertEquals("6", scalar(dataSource,
                "select count(*) from flow_node where definition_id in (2100600000000000001,2100600000000000002)"
                    + " and coordinate in ('100,100|100,100','300,100|300,100','500,100|500,100')"));
            assertEquals("0", scalar(dataSource,
                "select count(*) from flow_node where definition_id in (2100600000000000001,2100600000000000002)"
                    + " and coordinate in ('100,100','300,100','500,100')"));
            assertEquals("4", scalar(dataSource,
                "select count(*) from flow_skip where definition_id in (2100600000000000001,2100600000000000002)"
                    + " and coordinate in ('120,100;250,100','350,100;480,100')"));
            assertEquals("0", scalar(dataSource,
                "select count(*) from flow_skip where definition_id in (2100600000000000001,2100600000000000002)"
                    + " and coordinate in ('200,100','400,100')"));

            assertIdentityGuard(dataSource);
            assertApplicationGuard(dataSource);
            assertBindingGuard(dataSource);
            assertEnterpriseGuards(dataSource);
            assertSingleActiveMaterialTag(dataSource);
            assertMaterialTreeShape(dataSource);
        } finally {
            try {
                if (ownsSchema) {
                    dropTables(dataSource);
                }
            } finally {
                dataSource.forceCloseAll();
            }
        }
    }

    private void assertIdentityGuard(PooledDataSource dataSource) throws Exception {
        execute(dataSource, "insert into profile_identity_guard"
            + "(identity_guard_id,profile_type,identity_key,owner_type,owner_id,status)"
            + " values(1,'PERSON','CN_RESIDENT_ID:TEST0001','APPLICATION',101,'ACTIVE')");
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_identity_guard"
            + "(identity_guard_id,profile_type,identity_key,owner_type,owner_id,status)"
            + " values(2,'PERSON','CN_RESIDENT_ID:TEST0001','APPLICATION',102,'ACTIVE')"));
        execute(dataSource, "update profile_identity_guard set status='RELEASED' where identity_guard_id=1");
        execute(dataSource, "insert into profile_identity_guard"
            + "(identity_guard_id,profile_type,identity_key,owner_type,owner_id,status)"
            + " values(2,'PERSON','CN_RESIDENT_ID:TEST0001','PROFILE',201,'ACTIVE')");
    }

    private void assertApplicationGuard(PooledDataSource dataSource) throws Exception {
        execute(dataSource, "insert into profile_person_application"
            + "(person_application_id,applicant_user_id,status,document_type_code,document_number,identity_key,provider_code)"
            + " values(11,901,'DRAFT','CN_RESIDENT_ID','TEST1001','CN_RESIDENT_ID:TEST1001','manual')");
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_person_application"
            + "(person_application_id,applicant_user_id,status,document_type_code,document_number,identity_key,provider_code)"
            + " values(12,901,'BACK','CN_RESIDENT_ID','TEST1002','CN_RESIDENT_ID:TEST1002','manual')"));
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_person_application"
            + "(person_application_id,applicant_user_id,status,document_type_code,document_number,identity_key,provider_code)"
            + " values(13,902,'WAITING','CN_RESIDENT_ID','TEST1001','CN_RESIDENT_ID:TEST1001','manual')"));
        execute(dataSource, "update profile_person_application set status='FINISH' where person_application_id=11");
        execute(dataSource, "insert into profile_person_application"
            + "(person_application_id,applicant_user_id,status,document_type_code,document_number,identity_key,provider_code)"
            + " values(12,901,'DRAFT','CN_RESIDENT_ID','TEST1001','CN_RESIDENT_ID:TEST1001','manual')");
    }

    private void assertBindingGuard(PooledDataSource dataSource) throws Exception {
        execute(dataSource, "insert into profile_person_binding"
            + "(person_binding_id,person_profile_id,user_id,status,binding_version,bound_time)"
            + " values(21,301,1001,'ACTIVE',1,sysdate())");
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_person_binding"
            + "(person_binding_id,person_profile_id,user_id,status,binding_version,bound_time)"
            + " values(22,302,1001,'SUSPENDED',1,sysdate())"));
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_person_binding"
            + "(person_binding_id,person_profile_id,user_id,status,binding_version,bound_time)"
            + " values(23,301,1002,'ACTIVE',1,sysdate())"));
        execute(dataSource, "update profile_person_binding set status='UNBOUND',binding_version=2 where person_binding_id=21");
        execute(dataSource, "insert into profile_person_binding"
            + "(person_binding_id,person_profile_id,user_id,status,binding_version,bound_time)"
            + " values(22,302,1001,'ACTIVE',1,sysdate())");
    }

    private void assertEnterpriseGuards(PooledDataSource dataSource) throws Exception {
        execute(dataSource, "insert into profile_enterprise_application"
            + "(enterprise_application_id,applicant_user_id,status,unified_credit_code,identity_key,provider_code)"
            + " values(31,1901,'DRAFT','TEST-CREDIT-1','TEST-CREDIT-1','manual')");
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_enterprise_application"
            + "(enterprise_application_id,applicant_user_id,status,unified_credit_code,identity_key,provider_code)"
            + " values(32,1901,'WAITING','TEST-CREDIT-2','TEST-CREDIT-2','manual')"));
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_enterprise_application"
            + "(enterprise_application_id,applicant_user_id,status,unified_credit_code,identity_key,provider_code)"
            + " values(33,1902,'WAITING','TEST-CREDIT-1','TEST-CREDIT-1','manual')"));

        execute(dataSource, "insert into profile_enterprise_binding"
            + "(enterprise_binding_id,enterprise_profile_id,user_id,status,binding_version,bound_time)"
            + " values(41,1301,2001,'ACTIVE',1,sysdate())");
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_enterprise_binding"
            + "(enterprise_binding_id,enterprise_profile_id,user_id,status,binding_version,bound_time)"
            + " values(42,1302,2001,'SUSPENDED',1,sysdate())"));
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_enterprise_binding"
            + "(enterprise_binding_id,enterprise_profile_id,user_id,status,binding_version,bound_time)"
            + " values(43,1301,2002,'ACTIVE',1,sysdate())"));
    }

    private void assertSingleActiveMaterialTag(PooledDataSource dataSource) throws Exception {
        execute(dataSource, "insert into profile_material_ref"
            + "(material_ref_id,owner_type,owner_id,profile_type,oss_id,material_node_id,material_tag_code,"
            + "material_tag_name,file_name,file_size,file_extension,mime_type,status,attached_time)"
            + " values(51,'WORKING',501,'PERSON',601,2100200000000000101,'PERSON_ID_CARD_PORTRAIT',"
            + "'居民身份证人像面','front.jpg',1024,'jpg','image/jpeg','ATTACHED',sysdate())");
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_material_ref"
            + "(material_ref_id,owner_type,owner_id,profile_type,oss_id,material_node_id,material_tag_code,"
            + "material_tag_name,file_name,file_size,file_extension,mime_type,status,attached_time)"
            + " values(52,'WORKING',501,'PERSON',601,2100200000000000102,'PERSON_ID_CARD_EMBLEM',"
            + "'居民身份证国徽面','front.jpg',1024,'jpg','image/jpeg','ATTACHED',sysdate())"));
        execute(dataSource, "update profile_material_ref set status='DETACHED',detached_time=sysdate()"
            + " where material_ref_id=51");
        execute(dataSource, "insert into profile_material_ref"
            + "(material_ref_id,owner_type,owner_id,profile_type,oss_id,material_node_id,material_tag_code,"
            + "material_tag_name,file_name,file_size,file_extension,mime_type,status,attached_time)"
            + " values(52,'WORKING',501,'PERSON',601,2100200000000000102,'PERSON_ID_CARD_EMBLEM',"
            + "'居民身份证国徽面','front.jpg',1024,'jpg','image/jpeg','ATTACHED',sysdate())");
    }

    private void assertMaterialTreeShape(PooledDataSource dataSource) {
        assertThrows(SQLException.class, () -> execute(dataSource, "insert into profile_material_node"
            + "(material_node_id,parent_id,node_type,node_depth,profile_type,material_tag_code,node_name,"
            + "system_required,status,order_num) values"
            + "(99,2100200000000000011,'TAG',4,'PERSON','INVALID_DEPTH','非法深度','N','0',99)"));
    }

    private static void execute(PooledDataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String scalar(PooledDataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next(), sql);
            return resultSet.getString(1);
        }
    }

    private static void dropTables(PooledDataSource dataSource) throws Exception {
        // 只有验证过为空且归本测试所有的 schema 才会进入清理；不处理其他数据库。
        List<String> tables = new ArrayList<>();
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery(
                 "select table_name from information_schema.tables where table_schema=database()")) {
            while (result.next()) {
                tables.add(result.getString(1));
            }
        }
        for (String table : tables) {
            assertTrue(table.matches("[a-zA-Z0-9_]+"), "基座表名只能包含字母、数字与下划线");
            execute(dataSource, "drop table `" + table + "`");
        }
    }
}
