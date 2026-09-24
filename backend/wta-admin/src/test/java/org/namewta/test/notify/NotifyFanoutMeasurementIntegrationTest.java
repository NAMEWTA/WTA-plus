package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.namewta.NamewtaApplication;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.adapter.worker.NotifyOutboxWakeSubscriber;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.policy.NotificationAggregatePolicy;
import org.namewta.notify.port.NotifyDispatchPort;
import org.namewta.notify.port.NotifyOutboxClaimPort;
import org.namewta.notify.usecase.NotifyNoticeUseCase;
import org.namewta.system.api.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-43 未优化基线：每个方法由私有 runner 在一套全新六 SQL/MySQL/Redis 和独立 JVM 中运行。
 * 探针只计 JDBC 客户端 API 执行和返回行；executeBatch 不是网络往返或服务端语句数。
 */
@Tag("dev")
@EnabledIfSystemProperty(named = "notify.fanout.measurement", matches = "true")
@SpringBootTest(classes = NamewtaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(NotifyFanoutMeasurementIntegrationTest.OssBoundaryConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotifyFanoutMeasurementIntegrationTest {
    private static final long USER_BASE = 8_643_000_000_000_000_000L;
    private static final long NOTICE_ID = 8_643_100_000_000_000_000L;
    private static final String TRIGGER = "owned_t43_fanout_failure";

    @Autowired private JdbcTemplate db;
    @Autowired private DynamicRoutingDataSource routing;
    @Autowired private NotifyNoticeUseCase notices;
    @Autowired private NotifyOutboxClaimPort claims;
    @Autowired private NotifyDispatchPort dispatch;
    @Autowired private NotifyOutboxWakeSubscriber wakeSubscriber;
    @Autowired private OssBoundaryCounter ossBoundary;
    @Autowired private OssService ossService;

    private JdbcMeter jdbc;
    private int size;
    private String marker;
    private String method;
    private final Map<String, Object> measurements = new HashMap<>();

    @TestConfiguration(proxyBeanMethods = false)
    static class OssBoundaryConfiguration {
        @Bean OssBoundaryCounter ossBoundaryCounter() { return new OssBoundaryCounter(); }
    }

    /** 只观察真实 System OSS/附件快照入口，不替换生产 Bean 或供应商调用。 */
    @Aspect
    static class OssBoundaryCounter {
        private final AtomicLong calls = new AtomicLong();
        private final AtomicLong wakeCalls = new AtomicLong();
        private final ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);

        @Around("(target(org.namewta.system.api.OssService) || "
            + "target(org.namewta.common.notify.attachment.NotifyAttachmentSnapshotService)) "
            + "&& execution(* *(..))")
        public Object observe(ProceedingJoinPoint invocation) throws Throwable {
            if (active.get()) calls.incrementAndGet();
            return invocation.proceed();
        }

        @Around("execution(* org.namewta.notify.adapter.event.NotifyOutboxWakePublisher.publishAfterCommit(..))")
        public Object observeWake(ProceedingJoinPoint invocation) throws Throwable {
            if (active.get()) wakeCalls.incrementAndGet();
            return invocation.proceed();
        }

        void start() { calls.set(0); wakeCalls.set(0); active.set(true); }
        long stop() { active.remove(); return calls.get(); }
        long wakeCalls() { return wakeCalls.get(); }
    }

    @BeforeAll
    void requireOwnedApplication() {
        marker = System.getProperty("t43.owned.run");
        assertThat(marker).matches("[a-zA-Z0-9_-]{6,64}");
        assertThat(System.getProperty("t43.mysql.url"))
            .matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/wta-plus.*");
        assertThat(System.getProperty("t43.redis.host")).isEqualTo("127.0.0.1");
        assertThat(System.getProperty("t43.redis.port")).matches("[0-9]+");
        assertThat(System.getProperty("t43.redis.password")).isNotNull();
        assertThat(System.getProperty("t43.metrics.path")).startsWith("/tmp/wta-t43/");
        assertThat(System.getProperty("notify.outbox.poll-delay-ms")).isEqualTo("3600000");
        size = Integer.parseInt(System.getProperty("t43.fanout.size", "-1"));
        assertThat(size).isIn(100, 1_000, 10_000);
        assertThat(db.queryForObject("select count(*) from information_schema.tables where table_schema=database()",
            Integer.class)).isEqualTo(104);
        // 私有 JVM 只由本测试手动 claim/dispatch。自动唤醒行为由已有 Worker 回归覆盖。
        wakeSubscriber.destroy();
        jdbc = JdbcMeter.install(routing);
        // 已知的真实 PreparedStatement/ResultSet 正控制，防止探针未接入而无声输出零成本。
        JdbcMeter.Sample control = jdbc.start();
        try {
            assertThat(db.queryForObject("select count(*) from sys_user where status=? and del_flag=?",
                Integer.class, "0", "0")).isNotNull();
        } finally {
            control.stop();
        }
        assertThat(control.executeQueryCalls).isPositive();
        assertThat(control.returnedRows).isPositive();
        // 独立于 JDBC 的 OSS Advice 正控制；空 ID 列表是纯只读且不会访问远端对象。
        ossBoundary.start();
        try {
            assertThat(ossService.selectByIds("")).isEmpty();
        } finally {
            assertThat(ossBoundary.stop()).as("OSS 边界 Advice 必须实际织入").isEqualTo(1L);
        }
    }

    @AfterAll
    void restoreDataSource() {
        try {
            if (db != null) db.execute("drop trigger if exists " + TRIGGER);
        } finally {
            if (jdbc != null) jdbc.close();
        }
    }

    @BeforeEach
    void clearMetrics() {
        measurements.clear();
    }

    @AfterEach
    void writeSafeMetrics() throws Exception {
        assertThat(method).as("一个 owned JVM 必须只执行一个明确 selector").isNotBlank();
        Path path = Path.of(System.getProperty("t43.metrics.path"));
        assertThat(path.isAbsolute()).isTrue();
        Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Map<String, Object> envelope = Map.of("fanout_size", size, "run_marker", marker,
            "method", method, "measurements", new HashMap<>(measurements));
        Files.writeString(path, JsonUtils.toJsonString(envelope), StandardCharsets.UTF_8);
    }

    @Test
    void measurePublishedAllInAppFanout() {
        method = "measurePublishedAllInAppFanout";
        seedExactlyActiveUsers();
        insertDraft();
        publishMeasured();
        long intent = assertPublishedRows();
        measurements.put("intent_rows", 1);
        measurements.put("recipient_rows", count("notify_recipient", intent));
        measurements.put("delivery_rows", count("notify_delivery", intent));
        measurements.put("outbox_rows", count("notify_outbox", intent));
        measurements.put("attempt_rows", count("notify_attempt", intent));
        measurements.put("message_rows", count("notify_message", intent));
        measurements.put("message_recipient_rows", count("notify_message_recipient", intent));
    }

    @Test
    void measureBoundedInAppResultAggregation() {
        method = "measureBoundedInAppResultAggregation";
        seedExactlyActiveUsers();
        insertDraft();
        // 相同未优化发布输入，但只对领取后的十次结果提交计时。
        ossBoundary.start();
        try {
            notices.publish(NOTICE_ID);
        } finally {
            measurements.put("setup_oss_boundary_calls", ossBoundary.stop());
        }
        assertThat(measurements.get("setup_oss_boundary_calls")).isEqualTo(0L);
        long intent = assertPublishedRows();
        List<NotifyOutbox> owned = claims.claim("owned-t43-result-" + marker).stream()
            .filter(row -> intent == row.getIntentId()).toList();
        assertThat(owned).hasSize(50);
        assertThat(owned.stream().map(NotifyOutbox::getLeaseToken).toList()).doesNotContainNull();
        Map<String, Long> lockBefore = readLockStatus();
        JdbcMeter.Sample result = jdbc.start();
        ossBoundary.start();
        try {
            for (NotifyOutbox lease : owned.subList(0, 10)) dispatch.dispatch(lease);
        } finally {
            result.stop();
            measurements.put("oss_boundary_calls", ossBoundary.stop());
            recordJdbc("result", result);
            recordLockStatus(lockBefore);
        }
        assertThat(measurements.get("oss_boundary_calls")).isEqualTo(0L);
        assertThat(result.deliveryForUpdateCalls).as("真实聚合锁查询必须命中 JDBC 探针").isPositive();
        assertThat(result.deliveryForUpdateRows).as("十次聚合锁读不可悄然丢行").isGreaterThanOrEqualTo(10L * size);
        assertThat(count("notify_message", intent)).isEqualTo(1);
        assertThat(count("notify_message_recipient", intent)).isEqualTo(10);
        assertThat(db.queryForObject("select count(*) from notify_delivery where intent_id=? and status='DELIVERED'",
            Integer.class, intent)).isEqualTo(10);
        assertThat(db.queryForObject("select count(*) from notify_outbox where intent_id=? and status='DONE'",
            Integer.class, intent)).isEqualTo(10);
        List<String> states = db.queryForList("select status from notify_delivery where intent_id=?",
            String.class, intent);
        assertThat(states).hasSize(size);
        String expected = NotificationAggregatePolicy.aggregate(states).name();
        assertThat(db.queryForObject("select status from notify_intent where intent_id=?", String.class, intent))
            .isEqualTo(expected);
        measurements.put("dispatched_count", 10);
        measurements.put("recipient_rows", count("notify_recipient", intent));
        measurements.put("delivery_rows", count("notify_delivery", intent));
        measurements.put("outbox_rows", count("notify_outbox", intent));
        measurements.put("attempt_rows", count("notify_attempt", intent));
        measurements.put("message_rows", count("notify_message", intent));
        measurements.put("message_recipient_rows", count("notify_message_recipient", intent));
    }

    @Test
    void failedFanoutInsertRollsBackNoticeAndWake() {
        method = "failedFanoutInsertRollsBackNoticeAndWake";
        assertThat(size).isEqualTo(10_000);
        seedExactlyActiveUsers();
        insertDraft();
        List<Long> ordered = activeUserIds();
        long failingUser = ordered.get(1_000);
        Map<String, Integer> baseline = relationTableCounts();
        db.execute("create trigger " + TRIGGER + " before insert on notify_recipient for each row "
            + "begin if new.user_id=" + failingUser + " then signal sqlstate '45000' "
            + "set message_text='owned_t43_insert_failure'; end if; end");
        warmUpReadOnly();
        Map<String, Long> lockBefore = readLockStatus();
        JdbcMeter.Sample publish = jdbc.start();
        ossBoundary.start();
        try {
            assertThatThrownBy(() -> notices.publish(NOTICE_ID))
                .satisfies(failure -> assertThat(hasSqlState(failure, "45000"))
                    .as("真实 owned MySQL trigger 必须是失败根因").isTrue());
        } finally {
            publish.stop();
            measurements.put("oss_boundary_calls", ossBoundary.stop());
            measurements.put("wake_after_commit_calls", ossBoundary.wakeCalls());
            recordJdbc("publish", publish);
            recordLockStatus(lockBefore);
            db.execute("drop trigger if exists " + TRIGGER);
        }
        assertThat(measurements.get("oss_boundary_calls")).isEqualTo(0L);
        assertThat(measurements.get("wake_after_commit_calls")).isEqualTo(0L);
        assertThat(publish.rollbackCalls).as("失败发布必须经真实 JDBC rollback").isPositive();
        assertThat(db.queryForObject("select lifecycle from notify_notice where notice_id=?", String.class,
            NOTICE_ID)).isEqualTo("DRAFT");
        assertThat(db.queryForObject("select count(*) from notify_notice_snapshot where notice_id=?",
            Integer.class, NOTICE_ID)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_intent where biz_type='NOTICE_PUBLISHED' and biz_id=?",
            Integer.class, String.valueOf(NOTICE_ID))).isZero();
        assertThat(relationTableCounts()).as("事务内已写关系必须全部回滚").isEqualTo(baseline);
        measurements.put("recipient_rows", 0);
        measurements.put("delivery_rows", 0);
        measurements.put("outbox_rows", 0);
        measurements.put("attempt_rows", 0);
    }

    private void seedExactlyActiveUsers() {
        List<Long> original = activeUserIds();
        assertThat(original.size()).isLessThan(size);
        int missing = size - original.size();
        for (int offset = 0; offset < missing; offset += 500) {
            int from = offset;
            int until = Math.min(missing, offset + 500);
            db.batchUpdate("insert into sys_user(user_id,user_name,nick_name,status,del_flag,create_time) "
                + "values(?,?,?,'0','0',utc_timestamp())", new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                    @Override public void setValues(PreparedStatement ps, int index) throws SQLException {
                        int value = from + index;
                        ps.setLong(1, USER_BASE + value);
                        ps.setString(2, "t43_user_" + value);
                        ps.setString(3, "T43 user " + value);
                    }
                    @Override public int getBatchSize() { return until - from; }
                });
        }
        List<Long> actual = activeUserIds();
        assertThat(actual).hasSize(size).doesNotHaveDuplicates();
        Set<Long> expected = new HashSet<>(original);
        for (int i = 0; i < missing; i++) expected.add(USER_BASE + i);
        assertThat(new HashSet<>(actual)).isEqualTo(expected);
    }

    private List<Long> activeUserIds() {
        return db.queryForList("select user_id from sys_user where status='0' and del_flag='0' order by user_id",
            Long.class);
    }

    private void insertDraft() {
        assertThat(db.update("insert into notify_notice(notice_id,notice_title,notice_type,notice_content,"
            + "recipient_type,recipient_ids_json,user_type_ids_json,channels_json,status,lifecycle,create_time) "
            + "values(?,'T43 fixed fanout notice','1','T43 fixed body','ALL','[]','[]','[\"IN_APP\"]',"
            + "'1','DRAFT',utc_timestamp())", NOTICE_ID)).isEqualTo(1);
    }

    private void publishMeasured() {
        warmUpReadOnly();
        Map<String, Long> lockBefore = readLockStatus();
        JdbcMeter.Sample publish = jdbc.start();
        ossBoundary.start();
        try {
            notices.publish(NOTICE_ID);
        } finally {
            publish.stop();
            measurements.put("oss_boundary_calls", ossBoundary.stop());
            measurements.put("wake_after_commit_calls", ossBoundary.wakeCalls());
            recordJdbc("publish", publish);
            recordLockStatus(lockBefore);
        }
        assertThat(measurements.get("oss_boundary_calls")).isEqualTo(0L);
        assertThat(measurements.get("wake_after_commit_calls")).as("已提交发布须产生真实唤醒正控制")
            .isEqualTo(1L);
        assertThat(publish.commitCalls).as("已发布公告必须真实提交事务").isPositive();
        assertThat(publish.executeUpdateCalls + publish.executeBatchCalls)
            .as("真实发布必须发生被探针观察到的 JDBC 写入").isPositive();
    }

    private long assertPublishedRows() {
        assertThat(db.queryForObject("select lifecycle from notify_notice where notice_id=?", String.class,
            NOTICE_ID)).isEqualTo("PUBLISHED");
        assertThat(db.queryForObject("select count(*) from notify_notice_snapshot where notice_id=?",
            Integer.class, NOTICE_ID)).isEqualTo(1);
        Long intent = db.queryForObject("select intent_id from notify_intent where app_id='notify' "
            + "and idempotency_key=?", Long.class, "notice-published:" + NOTICE_ID + ":1");
        assertThat(intent).isNotNull();
        assertThat(count("notify_recipient", intent)).isEqualTo(size);
        assertThat(count("notify_delivery", intent)).isEqualTo(size);
        assertThat(count("notify_outbox", intent)).isEqualTo(size);
        assertThat(count("notify_attempt", intent)).isZero();
        assertThat(count("notify_intent_attachment", intent)).isZero();
        assertThat(count("notify_message", intent)).isZero();
        assertThat(count("notify_message_recipient", intent)).isZero();
        assertThat(db.queryForObject("select status from notify_intent where intent_id=?", String.class,
            intent)).isEqualTo("QUEUED");
        assertThat(db.queryForObject("select count(distinct user_id) from notify_recipient where intent_id=?",
            Integer.class, intent)).isEqualTo(size);
        assertThat(db.queryForObject("select count(*) from notify_delivery d left join notify_recipient r "
            + "on r.recipient_id=d.recipient_id where d.intent_id=? and r.recipient_id is null", Integer.class,
            intent)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_outbox o left join notify_delivery d "
            + "on d.delivery_id=o.delivery_id where o.intent_id=? and d.delivery_id is null", Integer.class,
            intent)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_delivery where intent_id=? and status='PENDING'",
            Integer.class, intent)).isEqualTo(size);
        assertThat(db.queryForObject("select count(*) from notify_outbox where intent_id=? and status='READY'",
            Integer.class, intent)).isEqualTo(size);
        String path = "/notify/inbox?messageId=" + intent;
        assertThat(db.queryForObject("select path_snapshot from notify_notice_snapshot where notice_id=?",
            String.class, NOTICE_ID)).isEqualTo(path);
        assertThat(db.queryForObject("select path_snapshot from notify_intent where intent_id=?", String.class,
            intent)).isEqualTo(path);
        assertThat(db.queryForObject("select json_unquote(json_extract(template_params_json,'$.path')) "
            + "from notify_intent where intent_id=?", String.class, intent)).isEqualTo(path);
        return intent;
    }

    private int count(String table, long intent) {
        String column = table.equals("notify_message") || table.equals("notify_message_recipient")
            ? "message_id" : "intent_id";
        return db.queryForObject("select count(*) from " + table + " where " + column + "=?", Integer.class, intent);
    }

    private void recordJdbc(String prefix, JdbcMeter.Sample sample) {
        measurements.put(prefix + "_elapsed_ns", sample.elapsedNs);
        measurements.put("jdbc_execute_query_calls", sample.executeQueryCalls);
        measurements.put("jdbc_execute_update_calls", sample.executeUpdateCalls);
        measurements.put("jdbc_execute_batch_calls", sample.executeBatchCalls);
        measurements.put("jdbc_add_batch_calls", sample.addBatchCalls);
        measurements.put("jdbc_returned_rows", sample.returnedRows);
        measurements.put("jdbc_delivery_for_update_calls", sample.deliveryForUpdateCalls);
        measurements.put("jdbc_delivery_for_update_rows", sample.deliveryForUpdateRows);
        measurements.put("jdbc_commit_calls", sample.commitCalls);
        measurements.put("jdbc_rollback_calls", sample.rollbackCalls);
        measurements.put("heap_used_before_bytes", sample.heapBefore);
        measurements.put("heap_used_after_bytes", sample.heapAfter);
        // 各堆池分别在阶段开始 reset，求和并非同一时刻的全堆瞬时峰值。
        measurements.put("heap_pool_individual_peaks_sum_bytes", sample.heapPoolPeak);
        measurements.put("gc_collection_count_delta", sample.gcCountDelta);
        measurements.put("gc_collection_time_ms_delta", sample.gcTimeDelta);
    }

    private void warmUpReadOnly() {
        // 同一公告正文和目录输入的只读预热，不能先做一次发布改变待测状态。
        for (int i = 0; i < 3; i++) {
            assertThat(notices.get(NOTICE_ID).getNoticeId()).isEqualTo(NOTICE_ID);
        }
    }

    private Map<String, Integer> relationTableCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String table : List.of("notify_recipient", "notify_delivery", "notify_outbox", "notify_attempt")) {
            counts.put(table, db.queryForObject("select count(*) from " + table, Integer.class));
        }
        return counts;
    }

    private static boolean hasSqlState(Throwable failure, String expected) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && expected.equals(sql.getSQLState())) return true;
        }
        return false;
    }

    private Map<String, Long> readLockStatus() {
        try {
            Map<String, Long> values = new HashMap<>();
            for (Map<String, Object> row : db.queryForList("show global status where Variable_name "
                + "in ('Innodb_row_lock_waits','Innodb_row_lock_time')")) {
                values.put(String.valueOf(row.get("Variable_name")), Long.parseLong(String.valueOf(row.get("Value"))));
            }
            return values;
        } catch (org.springframework.dao.DataAccessException | NumberFormatException unavailable) {
            return Map.of();
        }
    }

    private void recordLockStatus(Map<String, Long> before) {
        Map<String, Long> after = readLockStatus();
        Long waitsBefore = before.get("Innodb_row_lock_waits");
        Long waitsAfter = after.get("Innodb_row_lock_waits");
        Long timeBefore = before.get("Innodb_row_lock_time");
        Long timeAfter = after.get("Innodb_row_lock_time");
        measurements.put("row_lock_waits_delta", waitsBefore == null || waitsAfter == null
            ? null : waitsAfter - waitsBefore);
        measurements.put("row_lock_time_ms_delta", timeBefore == null || timeAfter == null
            ? null : timeAfter - timeBefore);
        measurements.put("row_lock_status_available", waitsBefore != null && waitsAfter != null
            && timeBefore != null && timeAfter != null);
    }

    /** 只保留计数；SQL 文本和参数只在此调用栈内用于分类，绝不入字段或日志。 */
    private static final class JdbcMeter implements AutoCloseable {
        private final DynamicRoutingDataSource routing;
        private final DataSource original;
        private final DataSource wrapped;
        private final ThreadLocal<Sample> active = new ThreadLocal<>();

        private JdbcMeter(DynamicRoutingDataSource routing, DataSource original) {
            this.routing = routing;
            this.original = original;
            this.wrapped = new DelegatingDataSource(original) {
                @Override public Connection getConnection() throws SQLException {
                    return wrapConnection(original.getConnection());
                }
                @Override public Connection getConnection(String username, String password) throws SQLException {
                    return wrapConnection(original.getConnection(username, password));
                }
            };
        }

        static JdbcMeter install(DynamicRoutingDataSource routing) {
            assertThat(routing.getDataSources().keySet()).containsExactly("master");
            DataSource original = routing.getDataSources().get("master");
            assertThat(original).isNotNull();
            JdbcMeter meter = new JdbcMeter(routing, original);
            assertThat(routing.getDataSources().replace("master", original, meter.wrapped)).isTrue();
            assertThat(routing.getDataSource("master")).isSameAs(meter.wrapped);
            return meter;
        }

        Sample start() {
            assertThat(active.get()).as("nested JDBC sample").isNull();
            Sample sample = new Sample(this);
            active.set(sample);
            return sample;
        }

        @Override public void close() {
            active.remove();
            assertThat(routing.getDataSources().replace("master", wrapped, original)).isTrue();
        }

        private Connection wrapConnection(Connection actual) {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    String name = method.getName();
                    Sample sample = active.get();
                    if (sample != null && name.equals("commit")) sample.commitCalls++;
                    if (sample != null && name.equals("rollback")) sample.rollbackCalls++;
                    Object result = invoke(actual, method, args);
                    if (name.equals("prepareStatement") && args != null && args.length > 0
                        && args[0] instanceof String sql && result instanceof PreparedStatement statement) {
                        return wrapStatement(statement, sql);
                    }
                    return result;
                });
        }

        private PreparedStatement wrapStatement(PreparedStatement actual, String sql) {
            // SQL 只用于这两个布尔分类，不存储或输出；含用户输入的动态 SQL 也不外泄。
            String lower = sql.toLowerCase(Locale.ROOT);
            boolean deliveryLock = lower.contains("notify_delivery") && lower.contains("for update");
            return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                    String name = method.getName();
                    Sample sample = active.get();
                    if (sample != null) {
                        switch (name) {
                            case "addBatch" -> sample.addBatchCalls++;
                            case "executeQuery" -> {
                                sample.executeQueryCalls++;
                                if (deliveryLock) sample.deliveryForUpdateCalls++;
                            }
                            case "executeUpdate", "executeLargeUpdate" -> sample.executeUpdateCalls++;
                            case "executeBatch", "executeLargeBatch" -> sample.executeBatchCalls++;
                            default -> { }
                        }
                    }
                    Object result = invoke(actual, method, args);
                    if (sample != null && name.equals("execute")) {
                        if (Boolean.TRUE.equals(result)) {
                            sample.executeQueryCalls++;
                            if (deliveryLock) sample.deliveryForUpdateCalls++;
                        } else {
                            sample.executeUpdateCalls++;
                        }
                    }
                    if (result instanceof ResultSet rows && (name.equals("executeQuery") || name.equals("getResultSet"))) {
                        return wrapRows(rows, sample, deliveryLock);
                    }
                    return result;
                });
        }

        private ResultSet wrapRows(ResultSet actual, Sample sample, boolean deliveryLock) {
            return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class}, (proxy, method, args) -> {
                    Object result = invoke(actual, method, args);
                    if (sample != null && method.getName().equals("next") && Boolean.TRUE.equals(result)) {
                        sample.returnedRows++;
                        if (deliveryLock) sample.deliveryForUpdateRows++;
                    }
                    return result;
                });
        }

        private static Object invoke(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
            try { return method.invoke(target, args); }
            catch (InvocationTargetException failure) { throw failure.getCause(); }
        }

        private static final class Sample {
            private final JdbcMeter meter;
            private final long started = System.nanoTime();
            private final long heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            private final long gcCountBefore = gcValue(false);
            private final long gcTimeBefore = gcValue(true);
            private long elapsedNs;
            private long heapAfter;
            private long heapPoolPeak;
            private long gcCountDelta;
            private long gcTimeDelta;
            private long executeQueryCalls;
            private long executeUpdateCalls;
            private long executeBatchCalls;
            private long addBatchCalls;
            private long returnedRows;
            private long deliveryForUpdateCalls;
            private long deliveryForUpdateRows;
            private long commitCalls;
            private long rollbackCalls;

            private Sample(JdbcMeter meter) {
                this.meter = meter;
                for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                    if (pool.getType() == java.lang.management.MemoryType.HEAP) {
                        try { pool.resetPeakUsage(); }
                        catch (UnsupportedOperationException unsupported) { /* 明示峰值可能不受此 JVM 支持。 */ }
                    }
                }
            }
            private void stop() {
                elapsedNs = System.nanoTime() - started;
                heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
                heapPoolPeak = ManagementFactory.getMemoryPoolMXBeans().stream()
                    .filter(pool -> pool.getType() == java.lang.management.MemoryType.HEAP)
                    .map(MemoryPoolMXBean::getPeakUsage).filter(java.util.Objects::nonNull)
                    .mapToLong(java.lang.management.MemoryUsage::getUsed).sum();
                gcCountDelta = gcValue(false) - gcCountBefore;
                gcTimeDelta = gcValue(true) - gcTimeBefore;
                meter.active.remove();
            }

            private static long gcValue(boolean time) {
                return ManagementFactory.getGarbageCollectorMXBeans().stream()
                    .mapToLong(bean -> Math.max(0, time ? bean.getCollectionTime() : bean.getCollectionCount())).sum();
            }
        }
    }
}
