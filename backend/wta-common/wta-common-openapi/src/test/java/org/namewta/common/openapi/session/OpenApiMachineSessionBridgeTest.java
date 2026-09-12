package org.namewta.common.openapi.session;

import org.namewta.system.api.model.LoginUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class OpenApiMachineSessionBridgeTest {

    private static final VerifiedOpenApiIdentity IDENTITY = new VerifiedOpenApiIdentity(7L, 9L);

    @Test
    void convergesConcurrentMissesAndReusesTheSessionHit() throws Exception {
        AtomicInteger resolutions = new AtomicInteger();
        FakeOperations operations = new FakeOperations();
        OpenApiMachineSessionBridge bridge = new OpenApiMachineSessionBridge(userId -> {
            resolutions.incrementAndGet();
            return snapshot(userId);
        }, operations, Duration.ofHours(8));
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<Long>> futures = new ArrayList<>();
            for (int index = 0; index < 16; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return bridge.execute(IDENTITY, IDENTITY::ownerUserId);
                }));
            }
            start.countDown();
            for (Future<Long> future : futures) {
                assertThat(future.get()).isEqualTo(9L);
            }
        }

        assertThat(bridge.execute(IDENTITY, () -> 17L)).isEqualTo(17L);
        assertThat(resolutions).hasValue(1);
        assertThat(operations.creations).hasValue(1);
        assertThat(operations.scopes).hasValue(17);
    }

    @Test
    void rejectsSnapshotThatDependsOnABrowserClient() {
        LoginUser invalid = snapshot(9L);
        invalid.setClientPk(100L);
        FakeOperations operations = new FakeOperations();
        OpenApiMachineSessionBridge bridge = new OpenApiMachineSessionBridge(
            ignored -> invalid, operations, Duration.ofHours(8));

        assertThatThrownBy(() -> bridge.execute(IDENTITY, () -> "unreachable"))
            .isInstanceOf(OpenApiMachineSessionException.class)
            .hasMessage("OpenAPI machine session is unavailable");
        assertThat(operations.creations).hasValue(0);
        assertThat(operations.scopes).hasValue(0);
    }

    @Test
    void invalidatorSerializesWithCreationAndPropagatesFailures() {
        FakeOperations operations = new FakeOperations();
        operations.invalidated = 2;
        DefaultOpenApiMachineSessionInvalidator invalidator = new DefaultOpenApiMachineSessionInvalidator(operations);

        assertThat(invalidator.invalidateByUserId(9L)).isEqualTo(2);
        assertThat(operations.lockEntries).hasValue(1);

        operations.failure = new IllegalStateException("redis unavailable");
        assertThatThrownBy(() -> invalidator.invalidateByUserId(9L))
            .isInstanceOf(OpenApiMachineSessionException.class)
            .hasMessage("OpenAPI machine session is unavailable")
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    private static LoginUser snapshot(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUserType("openapi");
        loginUser.setClientPk(null);
        loginUser.setClientKey(null);
        return loginUser;
    }

    private static final class FakeOperations implements OpenApiMachineSessionOperations {

        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicInteger creations = new AtomicInteger();
        private final AtomicInteger scopes = new AtomicInteger();
        private final AtomicInteger lockEntries = new AtomicInteger();
        private volatile LoginUser loginUser;
        private volatile int invalidated;
        private volatile RuntimeException failure;

        @Override
        public LoginUser find(VerifiedOpenApiIdentity identity) {
            return loginUser;
        }

        @Override
        public void create(VerifiedOpenApiIdentity identity, LoginUser value, Duration ttl) {
            creations.incrementAndGet();
            loginUser = value;
        }

        @Override
        public <T> T inRequestScope(VerifiedOpenApiIdentity identity, Supplier<T> callback) {
            scopes.incrementAndGet();
            return callback.get();
        }

        @Override
        public <T> T withUserLock(Long userId, Supplier<T> callback) {
            lock.lock();
            lockEntries.incrementAndGet();
            try {
                return callback.get();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int invalidateByUserId(Long userId) {
            if (failure != null) {
                throw failure;
            }
            return invalidated;
        }

    }

}
