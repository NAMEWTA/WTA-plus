package org.namewta.test.support;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.regex.Pattern;

/** 从唯一基座选取完整建表语句，并保留 SQL 字符串内的分号和同连接事务状态。 */
public final class SqlBaselineScripts {

    private SqlBaselineScripts() {
    }

    /** 只选中指定表，避免旧变更标记后的其他领域 SQL 被一并执行。 */
    public static String createTable(String table) throws IOException {
        String sql = Files.readString(SqlBaselinePaths.file("10-cde-base-ddl.sql"));
        var matcher = Pattern.compile("(?ims)^create\\s+table\\s+" + Pattern.quote(table)
            + "\\s*\\(.*?^\\)\\s*engine\\s*=\\s*innodb[^;]*;").matcher(sql);
        if (!matcher.find()) {
            throw new IllegalArgumentException("基座缺少完整建表语句：" + table);
        }
        String statement = matcher.group();
        if (matcher.find()) {
            throw new IllegalArgumentException("基座存在重复建表语句：" + table);
        }
        return statement;
    }

    /** 使用 SQL 解析器处理引号与注释，遇到错误立即失败并保留原 JDBC 异常。 */
    public static void execute(DataSource dataSource, String sql) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            var resource = new EncodedResource(
                new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            ScriptUtils.executeSqlScript(connection, resource);
        } catch (ScriptException exception) {
            if (exception.getCause() instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw exception;
        }
    }
}
