package org.namewta.notify.service.runtime;

import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AC-004：多 owner 并发 claim 时至多一个获得 lease，完成写回也至多一次。
 */
@Tag("dev")
class NotifyOutboxClaimConcurrencyTest {

    @Test
    void concurrentOwnersAtMostOneLeaseAndOneFinishWriteback() throws Exception {
        LeaseStore store = new LeaseStore();
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        when(dao.claimCandidates(any(), anyInt())).thenAnswer(invocation -> List.of(readyRow()));
        when(dao.deliveryChannels(any())).thenReturn(java.util.Map.of());
        when(dao.claimOutbox(anyLong(), anyString(), anyString(), any(), any(), any(boolean.class))).thenAnswer(invocation ->
            store.claim(invocation.getArgument(1), invocation.getArgument(2)));
        when(dao.finishOutbox(any())).thenAnswer(invocation ->
            store.finish(invocation.getArgument(0)));

        when(dao.databaseNow()).thenAnswer(invocation -> LocalDateTime.now(ZoneOffset.UTC));
        NotifyOutboxClaimService service = new NotifyOutboxClaimService(dao);
        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CyclicBarrier barrier = new CyclicBarrier(n);
        List<Callable<List<NotifyOutbox>>> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String owner = "owner-" + i;
            tasks.add(() -> {
                barrier.await(2, TimeUnit.SECONDS);
                return service.claim(owner);
            });
        }
        List<NotifyOutbox> won;
        try {
            List<Future<List<NotifyOutbox>>> futures = pool.invokeAll(tasks);
            won = new ArrayList<>();
            for (Future<List<NotifyOutbox>> future : futures) {
                won.addAll(future.get(5, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, won.size());
        assertEquals(1, store.claimWins.get());
        NotifyOutbox holder = won.getFirst();
        NotifyOutbox impostor = readyRow();
        impostor.setStatus("PROCESSING");
        impostor.setLeaseOwner("other-owner");
        impostor.setLeaseToken("other-token");
        assertEquals(1, dao.finishOutbox(holder));
        assertEquals(0, dao.finishOutbox(impostor));
        assertEquals(0, dao.finishOutbox(holder));
        assertEquals(1, store.finishWins.get());
        assertTrue(holder.getLeaseOwner() != null && holder.getLeaseToken() != null);
    }

    private static NotifyOutbox readyRow() {
        NotifyOutbox row = new NotifyOutbox();
        row.setOutboxId(7L);
        row.setStatus("READY");
        row.setAvailableAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        row.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        row.setAttemptCount(0);
        row.setMaxAttempts(5);
        return row;
    }

    private static final class LeaseStore {
        private final Object lock = new Object();
        private String owner;
        private String token;
        private boolean finished;
        private final AtomicInteger claimWins = new AtomicInteger();
        private final AtomicInteger finishWins = new AtomicInteger();

        private int claim(String owner, String token) {
            synchronized (lock) {
                if (this.owner != null) {
                    return 0;
                }
                this.owner = owner;
                this.token = token;
                claimWins.incrementAndGet();
                return 1;
            }
        }

        private int finish(NotifyOutbox outbox) {
            synchronized (lock) {
                if (finished
                    || owner == null
                    || !owner.equals(outbox.getLeaseOwner())
                    || !token.equals(outbox.getLeaseToken())) {
                    return 0;
                }
                finished = true;
                finishWins.incrementAndGet();
                return 1;
            }
        }
    }
}
