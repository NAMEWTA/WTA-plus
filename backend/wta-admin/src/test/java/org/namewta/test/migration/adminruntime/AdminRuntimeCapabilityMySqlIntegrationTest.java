package org.namewta.test.migration.adminruntime;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class AdminRuntimeCapabilityMySqlIntegrationTest {

    private static final String DML_START = "-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DML-001";
    private static final String DML_END = "-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DML-001-END";

    private PooledDataSource dataSource;

    @BeforeEach
    void connect() {
        String url = System.getProperty("admin.runtime.mysql.integration.url");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "需要一次性隔离 MySQL JDBC URL");
        dataSource = new PooledDataSource(
            "com.mysql.cj.jdbc.Driver",
            url,
            System.getProperty("admin.runtime.mysql.integration.username", "root"),
            System.getProperty("admin.runtime.mysql.integration.password", "")
        );
    }

    @AfterEach
    void cleanup() throws SQLException {
        if (dataSource == null) {
            return;
        }
        dropSchema();
        dataSource.forceCloseAll();
    }

    @Test
    void convergesMixedUpgradeStateAndReplays() throws Exception {
        createSchema();
        insertParentMenus();
        insertGeneratorMenus();
        insertHistoricalNacosMenu();

        executeBlock("50-cde-base-dml.sql", DML_START, DML_END);
        assertFinalState();

        executeBlock("50-cde-base-dml.sql", DML_START, DML_END);
        assertFinalState();
    }

    @Test
    void convergesFreshHistoricalMenuState() throws Exception {
        createSchema();
        insertParentMenus();
        insertHistoricalOpenApiMenus();
        insertHistoricalNacosMenu();

        executeBlock("50-cde-base-dml.sql", DML_START, DML_END);

        assertFinalState();
    }

    @Test
    void rejectsMenuConflictBeforeDeletingGeneratorState() throws Exception {
        createSchema();
        insertParentMenus();
        insertGeneratorMenus();
        insertHistoricalNacosMenu();
        execute("insert into sys_menu(menu_id,client_id,menu_name,parent_id,order_num,path,component,menu_type,perms) "
            + "values(2094360621561675776,1762000000000000001,'冲突菜单',1761400000000000001,13,"
            + "'openApi','other/component','C','system:openApi:list')");

        assertThatThrownBy(() -> executeBlock("50-cde-base-dml.sql", DML_START, DML_END))
            .isInstanceOf(SQLException.class);

        assertThat(queryInt("select count(*) from sys_menu where menu_id in ("
            + "1761400000000000003,1761400000000000115,1761400000000000116,"
            + "1761400000000001055,1761400000000001056,1761400000000001057,"
            + "1761400000000001058,1761400000000001059,1761400000000001060)"))
            .isEqualTo(9);
        assertThat(queryString("select menu_name from sys_menu where menu_id=2094360621561675790"))
            .isEqualTo("配置中心");
        assertThat(queryInt("select count(*) from sys_role_menu"))
            .isEqualTo(10);
    }

    @Test
    void rejectsCorruptedCompleteOpenApiMenuSetBeforeWrites() throws Exception {
        createSchema();
        insertParentMenus();
        insertGeneratorMenus();
        insertHistoricalOpenApiMenus();
        insertHistoricalNacosMenu();
        execute("update sys_menu set component='other/component' where menu_id=2094360621561675778");

        assertThatThrownBy(() -> executeBlock("50-cde-base-dml.sql", DML_START, DML_END))
            .isInstanceOf(SQLException.class);

        assertThat(queryInt("select count(*) from sys_menu where menu_id in ("
            + "1761400000000000003,1761400000000000115,1761400000000000116,"
            + "1761400000000001055,1761400000000001056,1761400000000001057,"
            + "1761400000000001058,1761400000000001059,1761400000000001060)"))
            .isEqualTo(9);
        assertThat(queryString("select component from sys_menu where menu_id=2094360621561675778"))
            .isEqualTo("other/component");
    }

    @Test
    void rejectsGeneratorMenuIdentityConflictBeforeWrites() throws Exception {
        createSchema();
        insertParentMenus();
        insertGeneratorMenus();
        insertHistoricalNacosMenu();
        execute("update sys_menu set menu_name='非生成器目录' where menu_id=1761400000000000003");

        assertThatThrownBy(() -> executeBlock("50-cde-base-dml.sql", DML_START, DML_END))
            .isInstanceOf(SQLException.class);

        assertThat(queryInt("select count(*) from sys_menu where menu_id in ("
            + "1761400000000000003,1761400000000000115,1761400000000000116,"
            + "1761400000000001055,1761400000000001056,1761400000000001057,"
            + "1761400000000001058,1761400000000001059,1761400000000001060)"))
            .isEqualTo(9);
        assertThat(queryString("select menu_name from sys_menu where menu_id=1761400000000000003"))
            .isEqualTo("非生成器目录");
    }

    private void assertFinalState() throws SQLException {
        assertThat(tableExists("gen_table")).isFalse();
        assertThat(tableExists("gen_table_column")).isFalse();
        assertThat(queryInt("select count(*) from sys_menu where menu_id between "
            + "2094360621561675776 and 2094360621561675781"))
            .isEqualTo(6);
        assertThat(queryString("select concat(menu_name,'|',parent_id,'|',component,'|',perms) "
            + "from sys_menu where menu_id=2094360621561675776"))
            .isEqualTo("OpenAPI管理|1761400000000000001|system/openApi/index|system:openApi:list");
        assertThat(queryString("select concat(menu_name,'|',parent_id,'|',order_num,'|',component,'|',perms) "
            + "from sys_menu where menu_id=2094360621561675790"))
            .isEqualTo("Nacos配置中心|1761400000000000002|8|monitor/nacos/index|system:nacos:console");
        assertThat(queryInt("select count(*) from sys_menu where menu_id in ("
            + "1761400000000000003,1761400000000000115,1761400000000000116,"
            + "1761400000000001055,1761400000000001056,1761400000000001057,"
            + "1761400000000001058,1761400000000001059,1761400000000001060)"))
            .isZero();
        assertThat(queryInt("select count(*) from sys_role_menu where menu_id in ("
            + "1761400000000000003,1761400000000000115,1761400000000000116,"
            + "1761400000000001055,1761400000000001056,1761400000000001057,"
            + "1761400000000001058,1761400000000001059,1761400000000001060,"
            + "2094360621561675776,2094360621561675777,2094360621561675778,"
            + "2094360621561675779,2094360621561675780,2094360621561675781,"
            + "2094360621561675790)"))
            .isZero();
        assertThat(queryInt("select count(*) from sys_role_menu where role_id=999 and menu_id=1761400000000000001"))
            .isEqualTo(1);
    }

    private void createSchema() throws SQLException {
        dropSchema();
        execute(
            "create table sys_menu ("
                + "menu_id bigint not null primary key, client_id bigint null, menu_name varchar(100) null, "
                + "parent_id bigint null, order_num int null, path varchar(255) null, component varchar(255) null, "
                + "query_param varchar(255) null, is_frame char(1) null, is_cache char(1) null, "
                + "menu_type char(1) null, visible char(1) null, status char(1) null, perms varchar(255) null, "
                + "icon varchar(100) null, active_menu varchar(255) null, ext varchar(255) null, "
                + "create_dept bigint null, create_by bigint null, create_time datetime null, "
                + "update_by bigint null, update_time datetime null, remark varchar(500) null)",
            "create table sys_role_menu (role_id bigint not null, menu_id bigint not null, "
                + "primary key(role_id,menu_id))"
        );
    }

    private void insertParentMenus() throws SQLException {
        execute(
            "insert into sys_menu(menu_id,menu_name,parent_id,menu_type) "
                + "values(1761400000000000001,'系统管理',0,'M')",
            "insert into sys_menu(menu_id,menu_name,parent_id,menu_type) "
                + "values(1761400000000000002,'系统监控',0,'M')",
            "insert into sys_role_menu(role_id,menu_id) values(999,1761400000000000001)"
        );
    }

    private void insertGeneratorMenus() throws SQLException {
        execute(
            generatorMenu(1761400000000000003L, "系统工具", 0, "tool", "M", "", "", ""),
            generatorMenu(1761400000000000115L, "代码生成", 1761400000000000003L, "gen", "C",
                "tool/gen/index", "tool:gen:list", ""),
            generatorMenu(1761400000000000116L, "修改生成配置", 1761400000000000003L,
                "gen-edit/index/:tableId", "C", "tool/gen/editTable", "tool:gen:edit", "/tool/gen"),
            generatorMenu(1761400000000001055L, "生成查询", 1761400000000000115L, "#", "F",
                "", "tool:gen:query", ""),
            generatorMenu(1761400000000001056L, "生成修改", 1761400000000000115L, "#", "F",
                "", "tool:gen:edit", ""),
            generatorMenu(1761400000000001057L, "生成删除", 1761400000000000115L, "#", "F",
                "", "tool:gen:remove", ""),
            generatorMenu(1761400000000001058L, "导入代码", 1761400000000000115L, "#", "F",
                "", "tool:gen:import", ""),
            generatorMenu(1761400000000001059L, "预览代码", 1761400000000000115L, "#", "F",
                "", "tool:gen:preview", ""),
            generatorMenu(1761400000000001060L, "生成代码", 1761400000000000115L, "#", "F",
                "", "tool:gen:code", "")
        );
        for (int index = 0; index < 9; index++) {
            execute("insert into sys_role_menu(role_id,menu_id) values("
                + (100L + index) + "," + generatorMenuId(index) + ")");
        }
    }

    private void insertHistoricalOpenApiMenus() throws SQLException {
        execute(
            openApiMenu(2094360621561675776L, "应用开放管理", 1761400000000000001L, 13,
                "openApi", "system/openApi/index", "C", "system:openApi:list"),
            openApiMenu(2094360621561675777L, "开放应用查询", 2094360621561675776L, 1,
                "", "", "F", "system:openApi:query"),
            openApiMenu(2094360621561675778L, "开放应用新增", 2094360621561675776L, 2,
                "", "", "F", "system:openApi:add"),
            openApiMenu(2094360621561675779L, "开放应用修改", 2094360621561675776L, 3,
                "", "", "F", "system:openApi:edit"),
            openApiMenu(2094360621561675780L, "开放应用删除", 2094360621561675776L, 4,
                "", "", "F", "system:openApi:remove"),
            openApiMenu(2094360621561675781L, "个人开放应用", 2094360621561675776L, 5,
                "", "", "F", "system:openApi:self")
        );
    }

    private void insertHistoricalNacosMenu() throws SQLException {
        execute("insert into sys_menu(menu_id,client_id,menu_name,parent_id,order_num,path,component,query_param,"
            + "is_frame,is_cache,menu_type,visible,status,perms,icon,active_menu,ext) "
            + "values(2094360621561675790,1762000000000000001,'配置中心',1761400000000000001,14,"
            + "'nacos','monitor/nacos/index','','N','Y','C','0','0','system:nacos:console','server','','')");
    }

    private String generatorMenu(long id, String name, long parentId, String path, String type,
                                 String component, String permission, String activeMenu) {
        return "insert into sys_menu(menu_id,client_id,menu_name,parent_id,path,menu_type,component,perms,active_menu) "
            + "values(" + id + ",1762000000000000001,'" + name + "'," + parentId + ",'" + path + "','"
            + type + "','" + component + "','" + permission + "','" + activeMenu + "')";
    }

    private String openApiMenu(long id, String name, long parentId, int order, String path,
                               String component, String type, String permission) {
        String icon = "C".equals(type) ? "api" : "#";
        return "insert into sys_menu(menu_id,client_id,menu_name,parent_id,order_num,path,component,query_param,"
            + "is_frame,is_cache,menu_type,visible,status,perms,icon,active_menu,ext) "
            + "values(" + id + ",1762000000000000001,'" + name + "'," + parentId + "," + order
            + ",'" + path + "','" + component + "','','N','Y','" + type + "','0','0','"
            + permission + "','" + icon + "','','')";
    }

    private long generatorMenuId(int index) {
        long[] ids = {
            1761400000000000003L,
            1761400000000000115L, 1761400000000000116L,
            1761400000000001055L, 1761400000000001056L, 1761400000000001057L,
            1761400000000001058L, 1761400000000001059L, 1761400000000001060L
        };
        return ids[index];
    }

    private void executeBlock(String file, String startMarker, String endMarker) throws Exception {
        String sql = Files.readString(SqlBaselinePaths.file(file));
        int start = sql.indexOf(startMarker);
        int end = sql.indexOf(endMarker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String rawStatement : sql.substring(start, end).split(";")) {
                String statementSql = rawStatement.lines()
                    .filter(line -> !line.stripLeading().startsWith("--"))
                    .reduce("", (left, right) -> left + "\n" + right)
                    .trim();
                if (!statementSql.isEmpty()) {
                    statement.execute(statementSql);
                }
            }
        }
    }

    private void execute(String... sqlStatements) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : sqlStatements) {
                statement.execute(sql);
            }
        }
    }

    private int queryInt(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getInt(1);
        }
    }

    private String queryString(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection(); ResultSet tables = connection.getMetaData()
            .getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private void dropSchema() throws SQLException {
        execute(
            "drop table if exists sys_role_menu",
            "drop table if exists sys_menu",
            "drop table if exists gen_table_column",
            "drop table if exists gen_table"
        );
    }

}
