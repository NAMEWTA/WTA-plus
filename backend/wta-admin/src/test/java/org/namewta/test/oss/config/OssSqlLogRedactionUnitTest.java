package org.namewta.test.oss.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.common.mybatis.config.properties.SqlLogProperties;
import org.namewta.common.mybatis.interceptor.SqlLogInterceptor;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 从真实 MyBatis StatementHandler/Invocation 边界检查 SQL 日志副本，不连接数据库。 */
@Tag("dev")
@ResourceLock(Resources.SYSTEM_ERR)
class OssSqlLogRedactionUnitTest {

    private static final String MAPPER_ID = "org.namewta.test.SecretMapper.update";
    private static final String LITERAL = "SENTINEL_LITERAL_OSS_7741";
    private static final String COMMENT = "SENTINEL_COMMENT_OSS_7741";
    private static final String FRAGMENT = "SENTINEL_FRAGMENT_OSS_7741";
    private static final String BOUND = "SENTINEL_BOUND_OSS_7741";
    private static final String ADDITIONAL = "SENTINEL_ADDITIONAL_OSS_7741";
    private static final String ERROR = "SENTINEL_ERROR_OSS_7741";

    @ParameterizedTest
    @ValueSource(strings = {"console", "log"})
    void logsOnlySafeMetadataWhileProceedingWithOriginalBoundSqlAndFailure(String output) throws Throwable {
        String sql = "UPDATE sys_oss_config SET access_key = ?, secret_key = '" + LITERAL
            + "' WHERE config_key = ? /* " + COMMENT + " */ AND marker = '" + FRAGMENT + "'";
        Fixture fixture = fixture(MAPPER_ID, sql);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(statement.getUpdateCount()).thenReturn(3);
        SQLException failure = new SQLException(ERROR + "\nFORGED-SQL-LOG-LINE");
        when(statement.execute()).thenReturn(true).thenThrow(failure);
        SqlLogProperties properties = new SqlLogProperties();
        properties.setEnabled(true);
        properties.setOutput(output);
        SqlLogInterceptor interceptor = new SqlLogInterceptor(properties);

        Captured captured = capture(() -> {
            Object result = interceptor.intercept(invocation(fixture.handler(), statement));
            assertThat(result).isEqualTo(3);
            Throwable thrown = catchThrowable(() -> interceptor.intercept(invocation(fixture.handler(), statement)));
            assertThat(thrown).isInstanceOf(InvocationTargetException.class);
            assertThat(thrown.getCause()).isSameAs(failure);
        });

        verify(statement, times(2)).execute();
        assertThat(fixture.boundSql().getSql()).isEqualTo(sql);
        assertThat(fixture.boundSql().getParameterObject()).isSameAs(fixture.parameters());
        assertThat(fixture.boundSql().getAdditionalParameter("configKey")).isEqualTo(ADDITIONAL);
        assertThat(captured.all()).contains(MAPPER_ID, "UPDATE", "Consume Time");
        assertThat(captured.all()).contains(SQLException.class.getName());
        assertThat(captured.all()).doesNotContain(LITERAL, COMMENT, FRAGMENT, BOUND, ADDITIONAL,
            ERROR, "FORGED-SQL-LOG-LINE", "secret_key =", "access_key =");
    }

    @ParameterizedTest
    @ValueSource(strings = {"console", "log"})
    void hostileMapperIdAndControlCharactersCannotInjectLogLines(String output) throws Throwable {
        String id = "org.namewta.test.SecretMapper.update\nSENTINEL_MAPPER_OSS_7741\u0001"
            + "x".repeat(300);
        Fixture fixture = fixture(id, "UPDATE sys_oss_config SET access_key = ?");
        PreparedStatement statement = mock(PreparedStatement.class);
        when(statement.getUpdateCount()).thenReturn(1);
        SqlLogProperties properties = new SqlLogProperties();
        properties.setEnabled(true);
        properties.setOutput(output);

        Captured captured = capture(() ->
            assertThat(new SqlLogInterceptor(properties).intercept(invocation(fixture.handler(), statement)))
                .isEqualTo(1));

        verify(statement).execute();
        assertThat(captured.all()).contains("Mapper ID", "UPDATE");
        assertThat(captured.all()).doesNotContain("SENTINEL_MAPPER_OSS_7741", "access_key =", BOUND);
    }

    private static Fixture fixture(String mapperId, String sql) {
        Configuration configuration = new Configuration();
        List<ParameterMapping> mappings = List.of(
            new ParameterMapping.Builder(configuration, "accessKey", String.class).build(),
            new ParameterMapping.Builder(configuration, "configKey", String.class).build());
        Map<String, String> parameters = Map.of("accessKey", BOUND, "configKey", BOUND);
        MappedStatement mapped = new MappedStatement.Builder(configuration, mapperId,
            new StaticSqlSource(configuration, sql, mappings), SqlCommandType.UPDATE).build();
        BoundSql boundSql = mapped.getBoundSql(parameters);
        boundSql.setAdditionalParameter("configKey", ADDITIONAL);
        StatementHandler handler = configuration.newStatementHandler(mock(Executor.class), mapped,
            parameters, RowBounds.DEFAULT, null, boundSql);
        return new Fixture(handler, boundSql, parameters);
    }

    private static Invocation invocation(StatementHandler handler, PreparedStatement statement) throws Exception {
        return new Invocation(handler, StatementHandler.class.getMethod("update", Statement.class),
            new Object[]{statement});
    }

    private static Captured capture(ThrowingOperation operation) throws Throwable {
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        Logger logger = (Logger) LoggerFactory.getLogger("SQL_FULL");
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try (PrintStream capturedErr = new PrintStream(console, true, StandardCharsets.UTF_8)) {
            System.setErr(capturedErr);
            operation.run();
        } finally {
            System.setErr(originalErr);
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }
        String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
            .reduce("", (left, right) -> left + "\n" + right);
        return new Captured(console.toString(StandardCharsets.UTF_8), logged);
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws Throwable;
    }

    private record Fixture(StatementHandler handler, BoundSql boundSql, Map<String, String> parameters) {
    }

    private record Captured(String console, String logger) {
        String all() {
            return console + logger;
        }
    }
}
