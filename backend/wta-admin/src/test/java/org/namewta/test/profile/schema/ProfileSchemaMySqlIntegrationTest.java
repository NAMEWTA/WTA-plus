package org.namewta.test.profile.schema;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ProfileSchemaMySqlIntegrationTest {

    private static final List<String> PROFILE_TABLES = List.of(
        "profile_enterprise_transfer_record", "profile_notification_audit", "profile_operation_audit",
        "profile_decision_record", "profile_verification_attempt", "profile_material_ref",
        "profile_enterprise_binding_event", "profile_enterprise_binding", "profile_enterprise_source",
        "profile_enterprise_submission", "profile_enterprise_application", "profile_enterprise_version",
        "profile_enterprise", "profile_person_binding_event", "profile_person_binding",
        "profile_person_source", "profile_person_submission", "profile_person_application",
        "profile_person_version", "profile_person", "profile_material_requirement", "profile_material_node",
        "profile_document_type", "profile_identity_guard"
    );

    @Test
    void freshSchemaAndSeedsEnforceReleaseAndRecreateSemantics() throws Exception {
        String url = System.getProperty("profile.schema.mysql.integration.url");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "需要一次性隔离MySQL JDBC URL");
        PooledDataSource dataSource = new PooledDataSource(
            "com.mysql.cj.jdbc.Driver", url,
            System.getProperty("profile.schema.mysql.integration.username", "root"),
            System.getProperty("profile.schema.mysql.integration.password", "")
        );
        try {
            prepareReferenceTables(dataSource);
            executeBlock(dataSource, sqlBlock("10-cde-base-ddl.sql", "NAMEWTA-PROFILE-DDL-001"));
            executeBlock(dataSource, sqlBlock("50-cde-base-dml.sql", "NAMEWTA-PROFILE-DML-001"));
            assertEquals("24", scalar(dataSource,
                "select count(*) from information_schema.tables where table_schema=database() and table_name like 'profile\\_%'"));
            assertEquals("14", scalar(dataSource,
                "select count(*) from sys_menu where perms like 'profile:%'"));
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
            dropTables(dataSource);
            dataSource.forceCloseAll();
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

    private static void prepareReferenceTables(PooledDataSource dataSource) throws Exception {
        dropTables(dataSource);
        execute(dataSource, "create table sys_menu (menu_id bigint not null,client_id bigint null,"
            + "menu_name varchar(50) not null,parent_id bigint default 0,order_num int default 0,path varchar(200) default '',"
            + "component varchar(255),query_param varchar(255),is_frame char(1),is_cache char(1),menu_type char(1),"
            + "visible char(1),status char(1),perms varchar(100),icon varchar(100),active_menu varchar(255),"
            + "ext varchar(2000),create_dept bigint,create_by bigint,create_time datetime,update_by bigint,"
            + "update_time datetime,remark varchar(500),primary key(menu_id)) engine=innodb");
        execute(dataSource, "create table sys_config (config_id bigint not null,config_name varchar(100),"
            + "config_key varchar(100),config_value varchar(500),config_type char(1),create_dept bigint,create_by bigint,"
            + "create_time datetime,update_by bigint,update_time datetime,remark varchar(500),primary key(config_id)) engine=innodb");
        execute(dataSource, "create table sys_dict_type (dict_id bigint not null,dict_name varchar(100),"
            + "dict_type varchar(100),create_dept bigint,create_by bigint,create_time datetime,update_by bigint,"
            + "update_time datetime,remark varchar(500),primary key(dict_id),unique key uk_test_dict_type(dict_type)) engine=innodb");
        execute(dataSource, "create table sys_dict_data (dict_code bigint not null,dict_sort int,dict_label varchar(100),"
            + "dict_value varchar(100),dict_type varchar(100),css_class varchar(100),list_class varchar(100),"
            + "is_default char(1),create_dept bigint,create_by bigint,create_time datetime,update_by bigint,"
            + "update_time datetime,remark varchar(500),primary key(dict_code)) engine=innodb");
        execute(dataSource, "create table flow_definition (id bigint not null,flow_code varchar(40) not null,"
            + "flow_name varchar(100) not null,model_value varchar(40),category varchar(100),version varchar(20) not null,"
            + "is_publish tinyint not null,form_custom char(1),form_path varchar(100),activity_status tinyint not null,"
            + "listener_type varchar(100),listener_path varchar(400),ext varchar(500),create_time datetime,"
            + "create_by varchar(64),update_time datetime,update_by varchar(64),del_flag char(1),tenant_id varchar(40),"
            + "primary key(id)) engine=innodb");
        execute(dataSource, "create table flow_node (id bigint not null,node_type tinyint not null,"
            + "definition_id bigint not null,node_code varchar(100) not null,node_name varchar(100),"
            + "permission_flag varchar(200),node_ratio varchar(200),coordinate varchar(100),any_node_skip varchar(100),"
            + "listener_type varchar(100),listener_path varchar(400),form_custom char(1),form_path varchar(100),"
            + "version varchar(20) not null,create_time datetime,create_by varchar(64),update_time datetime,"
            + "update_by varchar(64),ext text,del_flag char(1),tenant_id varchar(40),primary key(id)) engine=innodb");
        execute(dataSource, "create table flow_skip (id bigint not null,definition_id bigint not null,"
            + "now_node_code varchar(100) not null,now_node_type tinyint,next_node_code varchar(100) not null,"
            + "next_node_type tinyint,skip_name varchar(100),skip_type varchar(40),skip_condition varchar(200),"
            + "coordinate varchar(100),create_time datetime,create_by varchar(64),update_time datetime,"
            + "update_by varchar(64),del_flag char(1),tenant_id varchar(40),primary key(id)) engine=innodb");
    }

    private static String sqlBlock(String file, String marker) throws Exception {
        String sql = Files.readString(sqlDirectory().resolve(file));
        int markerIndex = sql.indexOf(marker);
        assertTrue(markerIndex >= 0, marker);
        int start = sql.lastIndexOf("--", markerIndex);
        assertTrue(start >= 0, marker + " comment start");
        return sql.substring(start);
    }

    private static void executeBlock(PooledDataSource dataSource, String sql) throws Exception {
        String executable = Arrays.stream(sql.split("\\R"))
            .filter(line -> !line.stripLeading().startsWith("--"))
            .collect(Collectors.joining("\n"));
        for (String statement : executable.split(";")) {
            if (!statement.isBlank()) {
                execute(dataSource, statement);
            }
        }
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
        for (String table : PROFILE_TABLES) {
            execute(dataSource, "drop table if exists " + table);
        }
        execute(dataSource, "drop table if exists sys_menu");
        execute(dataSource, "drop table if exists sys_config");
        execute(dataSource, "drop table if exists sys_dict_data");
        execute(dataSource, "drop table if exists sys_dict_type");
        execute(dataSource, "drop table if exists flow_skip");
        execute(dataSource, "drop table if exists flow_node");
        execute(dataSource, "drop table if exists flow_definition");
    }

    private static Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"));
        return current.getFileName().toString().equals("wta-admin") ? current.getParent() : current;
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
}
