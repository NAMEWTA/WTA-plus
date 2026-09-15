package org.namewta.test.nacos.menu;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.test.support.SqlBaselinePaths;

import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class NacosMenuContractUnitTest {

    private static final String START = "-- NAMEWTA-NACOS-CONSOLE-DML-001";
    private static final String END = "-- NAMEWTA-NACOS-CONSOLE-DML-001-END";

    @Test
    void definesOneIdempotentMenuWithoutGrantingOrdinaryRoles() throws IOException {
        String dml = Files.readString(SqlBaselinePaths.file("50-cde-base-dml.sql"));
        String block = block(dml);

        assertThat(block)
            .contains("2094360621561675790")
            .contains("1761400000000000001")
            .contains("'配置中心'")
            .contains("'nacos', 'monitor/nacos/index'")
            .contains("'system:nacos:console'")
            .contains("where not exists (select 1 from sys_menu where menu_id = 2094360621561675790)")
            .doesNotContain("insert into sys_role_menu");
    }

    private static String block(String sql) {
        int start = sql.indexOf(START);
        int end = sql.indexOf(END);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return sql.substring(start, end + END.length());
    }

}
