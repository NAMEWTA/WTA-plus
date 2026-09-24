package org.namewta.common.mybatis.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.namewta.common.mybatis.config.properties.SqlLogProperties;

import java.lang.reflect.InvocationTargetException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** 只记录 SQL 执行元数据；BoundSql 文本及参数可能含凭据，不能进入日志链。 */
@Slf4j(topic = "SQL_FULL")
@Intercepts({
    @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method = "queryCursor", args = {Statement.class})
})
public class SqlLogInterceptor implements Interceptor {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile(
        "[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*");
    private static final String UNSAFE_IDENTIFIER = "[REDACTED]";

    private final SqlLogProperties sqlLogProperties;

    public SqlLogInterceptor(SqlLogProperties sqlLogProperties) {
        this.sqlLogProperties = sqlLogProperties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long started = System.nanoTime();
        try {
            Object result = invocation.proceed();
            logSafely(invocation.getTarget(), elapsedMillis(started), null);
            return result;
        } catch (Exception executionFailure) {
            logSafely(invocation.getTarget(), elapsedMillis(started), executionFailure);
            throw executionFailure;
        }
    }

    /** 日志是旁路，元数据提取或输出失败不能改写数据库操作的结果和原异常。 */
    private void logSafely(Object target, long elapsedMillis, Exception executionFailure) {
        try {
            StatementHandler handler = PluginUtils.realTarget(target);
            MappedStatement statement = PluginUtils.mpStatementHandler(handler).mappedStatement();
            SqlCommandType command = statement.getSqlCommandType();
            String message = "Consume Time：" + elapsedMillis + " ms Mapper ID："
                + safeIdentifier(statement.getId()) + " Command Type："
                + (command == null ? "UNKNOWN" : command.name());
            if (executionFailure != null) {
                Throwable original = executionFailure instanceof InvocationTargetException wrapper
                    && wrapper.getTargetException() != null ? wrapper.getTargetException() : executionFailure;
                message += " Execute Error：" + safeIdentifier(original.getClass().getName());
            }
            if ("log".equalsIgnoreCase(sqlLogProperties.getOutput())) {
                log.info(message);
            } else {
                System.err.println(message);
            }
        } catch (RuntimeException logFailure) {
            // 不能让日志设施故障遮盖成功结果或替换原数据库异常。
        }
    }

    private static long elapsedMillis(long started) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
    }

    private static String safeIdentifier(String value) {
        return value != null && value.length() <= 160 && SAFE_IDENTIFIER.matcher(value).matches()
            ? value : UNSAFE_IDENTIFIER;
    }
}
