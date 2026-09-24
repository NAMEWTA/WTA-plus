package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Only for the owned, single-class, full-context integration test.
 * It decorates the existing routing "master" entry without replacing or closing its pool.
 * No fault is active unless a test sets both a current-thread role and a one-shot plan.
 */
final class OwnedAttachmentJdbcFaults implements AutoCloseable {
    enum Role { ACK, RACE_A, RACE_B }
    enum AckTarget { SUBMIT_RELATION, MAIL_SEND_RESERVATION }
    enum AckPhase { BEFORE, AFTER }

    private final DynamicRoutingDataSource router;
    private final DataSource original;
    private final DataSource probe;
    private final ThreadLocal<Role> role = new ThreadLocal<>();
    private final AtomicReference<AckPlan> ackPlan = new AtomicReference<>();
    private final AtomicReference<RacePlan> racePlan = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    private OwnedAttachmentJdbcFaults(DynamicRoutingDataSource router, DataSource original) {
        this.router = router;
        this.original = original;
        this.probe = new ProbeDataSource(original);
    }

    static OwnedAttachmentJdbcFaults install(DynamicRoutingDataSource router) {
        Objects.requireNonNull(router);
        if (!router.getDataSources().keySet().equals(Set.of("master"))) {
            throw new IllegalStateException("owned datasource set differs");
        }
        DataSource old = router.getDataSources().get("master");
        if (old == null || old instanceof OwnedAttachmentJdbcFaultsMarker) {
            throw new IllegalStateException("owned master datasource boundary differs");
        }
        var hook = new OwnedAttachmentJdbcFaults(router, old);
        if (!router.getDataSources().replace("master", old, hook.probe)
            || router.getDataSource("master") != hook.probe) {
            router.getDataSources().replace("master", hook.probe, old);
            throw new IllegalStateException("owned master datasource install race");
        }
        return hook;
    }

    private interface OwnedAttachmentJdbcFaultsMarker { }

    private final class ProbeDataSource extends DelegatingDataSource implements OwnedAttachmentJdbcFaultsMarker {
        private ProbeDataSource(DataSource delegate) { super(delegate); }
        @Override public Connection getConnection() throws SQLException { return wrap(original.getConnection()); }
        @Override public Connection getConnection(String username, String password) throws SQLException {
            return wrap(original.getConnection(username, password));
        }
    }

    AckPlan armAck(AckTarget target, AckPhase phase) {
        ensureIdle();
        AckPlan plan = new AckPlan(target, phase);
        if (!ackPlan.compareAndSet(null, plan)) throw new IllegalStateException("owned ACK already armed");
        return plan;
    }

    RacePlan armRace() {
        ensureIdle();
        RacePlan plan = new RacePlan();
        if (!racePlan.compareAndSet(null, plan)) throw new IllegalStateException("owned race already armed");
        return plan;
    }

    void disarm() {
        AckPlan ack = ackPlan.getAndSet(null);
        RacePlan race = racePlan.getAndSet(null);
        if (race != null) race.permitACommit.countDown();
        role.remove();
        if (ack != null) ack.disarmed.set(true);
    }

    <T> T as(Role selected, Callable<T> call) throws Exception {
        if (closed.get() || role.get() != null) throw new IllegalStateException("owned JDBC role nested or closed");
        role.set(selected);
        try { return call.call(); }
        finally { role.remove(); }
    }

    private void ensureIdle() {
        if (closed.get() || ackPlan.get() != null || racePlan.get() != null)
            throw new IllegalStateException("owned JDBC fault plan already active");
    }

    private Connection wrap(Connection actual) {
        Session session = new Session();
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                if ("commit".equals(method.getName()) && method.getParameterCount() == 0) {
                    interceptCommit(actual, session);
                    return null;
                }
                Object result = invoke(actual, method, args);
                if ("prepareStatement".equals(method.getName()) && args != null && args.length > 0
                    && args[0] instanceof String sql && result instanceof PreparedStatement statement) {
                    return wrapStatement(statement, sql, session);
                }
                return result;
            });
    }

    private PreparedStatement wrapStatement(PreparedStatement actual, String sql, Session session) {
        String normalized = normalize(sql);
        OwnedAttachmentSendReservationSqlProbe reservation = new OwnedAttachmentSendReservationSqlProbe(sql);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                String name = method.getName();
                boolean execute = name.equals("execute") || name.equals("executeUpdate")
                    || name.equals("executeLargeUpdate") || name.equals("executeQuery")
                    || name.equals("executeBatch") || name.equals("executeLargeBatch");
                Role current = role.get();
                RacePlan race = racePlan.get();
                if (execute && current == Role.RACE_B && race != null && isIntentInsert(normalized)) {
                    race.bInsertEntered.countDown();
                }
                Object result = invoke(actual, method, args);
                if (name.startsWith("set") && args != null && args.length >= 2
                    && args[0] instanceof Integer index) {
                    reservation.bind(index, name.equals("setNull") ? null : args[1]);
                } else if (name.equals("clearParameters")) {
                    reservation.clearParameters();
                } else if (name.equals("addBatch")) {
                    reservation.addBatch();
                } else if (name.equals("clearBatch")) {
                    reservation.clearBatch();
                }
                if (execute) {
                    if (isAttachmentInsert(normalized)) session.sawAttachmentInsert = true;
                    long rows = name.equals("executeUpdate") || name.equals("executeLargeUpdate")
                        ? ((Number) result).longValue() : name.equals("execute") ? actual.getUpdateCount() : -1L;
                    if (rows == 1L && reservation.isSingleRowReservationUpdate(1))
                        session.sawSendReservedUpdate = true;
                    if (current == Role.RACE_A && isIntentInsert(normalized)) session.sawIntentInsert = true;
                    if (current == Role.RACE_B && race != null && isIdempotencyCurrentRead(normalized)) {
                        race.currentReadCount.incrementAndGet();
                    }
                }
                return result;
            });
    }

    private void interceptCommit(Connection actual, Session session) throws Exception {
        Role current = role.get();
        AckPlan ack = ackPlan.get();
        if (current == Role.ACK && ack != null && !ack.disarmed.get()
            && session.matches(ack.target) && ack.fired.compareAndSet(false, true)) {
            if (ack.phase == AckPhase.AFTER) {
                actual.commit();
                ack.actualCommitted.set(true);
                throw new SQLRecoverableException("owned lost commit acknowledgement");
            }
            terminateOwnedSession(actual);
            ack.beforeDisconnectAcknowledged.set(true);
            // Must call the real driver so this case proves server-side disconnect before commit.
            ack.beforeDriverCommitCalled.set(true);
            actual.commit();
            ack.beforeUnexpectedlyCommitted.set(true);
            throw new SQLException("owned pre-commit disconnect did not stop commit");
        }
        RacePlan race = racePlan.get();
        if (current == Role.RACE_A && race != null && session.sawIntentInsert
            && race.aCommitHeld.compareAndSet(false, true)) {
            race.aCommitEntered.countDown();
            try {
                if (!race.permitACommit.await(15, TimeUnit.SECONDS))
                    throw new SQLTimeoutException("owned A commit wait timed out");
            } catch (InterruptedException interruption) {
                Thread.currentThread().interrupt();
                throw new SQLTimeoutException("owned A commit interrupted");
            }
        }
        actual.commit();
    }

    private void terminateOwnedSession(Connection target) throws SQLException {
        long connectionId;
        try (Statement statement = target.createStatement();
             ResultSet row = statement.executeQuery("select connection_id()")) {
            if (!row.next()) throw new SQLException("owned connection id absent");
            connectionId = row.getLong(1);
        }
        if (connectionId <= 0) throw new SQLException("owned connection id invalid");
        try (Connection killer = original.getConnection(); Statement statement = killer.createStatement()) {
            statement.execute("kill connection " + connectionId);
        }
    }

    private static String normalize(String sql) {
        return sql.replace("`", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
    private static boolean isIntentInsert(String sql) {
        return sql.startsWith("insert into notify_intent ") || sql.startsWith("insert into notify_intent(");
    }
    private static boolean isAttachmentInsert(String sql) {
        return sql.startsWith("insert into notify_intent_attachment ")
            || sql.startsWith("insert into notify_intent_attachment(");
    }
    private static boolean isIdempotencyCurrentRead(String sql) {
        return sql.startsWith("select ") && sql.contains(" from notify_intent ")
            && sql.contains("idempotency_key") && sql.endsWith("for update");
    }
    private static Object invoke(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        disarm();
        if (!router.getDataSources().replace("master", probe, original)) {
            throw new IllegalStateException("owned master datasource restore race");
        }
        // The router remains owner of the original pool and closes it at context shutdown.
    }

    private static final class Session {
        boolean sawAttachmentInsert;
        boolean sawSendReservedUpdate;
        boolean sawIntentInsert;
        boolean matches(AckTarget target) {
            return target == AckTarget.SUBMIT_RELATION ? sawAttachmentInsert : sawSendReservedUpdate;
        }
    }

    static final class AckPlan {
        final AckTarget target;
        final AckPhase phase;
        final AtomicBoolean fired = new AtomicBoolean();
        final AtomicBoolean disarmed = new AtomicBoolean();
        final AtomicBoolean actualCommitted = new AtomicBoolean();
        final AtomicBoolean beforeDisconnectAcknowledged = new AtomicBoolean();
        final AtomicBoolean beforeDriverCommitCalled = new AtomicBoolean();
        final AtomicBoolean beforeUnexpectedlyCommitted = new AtomicBoolean();
        AckPlan(AckTarget target, AckPhase phase) {
            this.target = Objects.requireNonNull(target);
            this.phase = Objects.requireNonNull(phase);
        }
        boolean hitExactlyOnce() {
            return fired.get() && !beforeUnexpectedlyCommitted.get()
                && (phase == AckPhase.BEFORE
                    ? beforeDisconnectAcknowledged.get() && beforeDriverCommitCalled.get()
                    : actualCommitted.get());
        }
    }

    static final class RacePlan {
        final CountDownLatch aCommitEntered = new CountDownLatch(1);
        final CountDownLatch bInsertEntered = new CountDownLatch(1);
        final CountDownLatch permitACommit = new CountDownLatch(1);
        final AtomicBoolean aCommitHeld = new AtomicBoolean();
        final AtomicInteger currentReadCount = new AtomicInteger();
        boolean awaitA(Duration timeout) throws InterruptedException {
            return aCommitEntered.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        boolean awaitB(Duration timeout) throws InterruptedException {
            return bInsertEntered.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        void releaseA() { permitACommit.countDown(); }
    }
}
