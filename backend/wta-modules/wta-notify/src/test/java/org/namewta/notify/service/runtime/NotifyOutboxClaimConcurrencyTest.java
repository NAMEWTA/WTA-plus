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

    @Test
    void serialWorkerClaimsNextExternalTaskOnlyWhenItCanStart() {
        var clock = new java.util.concurrent.atomic.AtomicReference<>(LocalDateTime.of(2026, 9, 26, 12, 0));
        List<NotifyOutbox> rows = new ArrayList<>();
        for (long id = 1; id <= 3; id++) {
            NotifyOutbox row = readyRow();
            row.setOutboxId(id);
            row.setDeliveryId(id);
            rows.add(row);
        }
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        when(dao.databaseNow()).thenAnswer(call -> clock.get());
        when(dao.claimCandidates(any(), anyInt())).thenAnswer(call -> rows.stream()
            .filter(row -> "READY".equals(row.getStatus())
                || "PROCESSING".equals(row.getStatus()) && !row.getLeaseUntil().isAfter(clock.get()))
            .limit(call.<Integer>getArgument(1)).toList());
        when(dao.deliveryChannels(any())).thenReturn(java.util.Map.of(1L, "MAIL", 2L, "MAIL", 3L, "MAIL"));
        when(dao.claimOutbox(anyLong(), anyString(), anyString(), any(), any(), any(boolean.class)))
            .thenReturn(1);
        var claim = new org.namewta.notify.usecase.NotifyOutboxClaimUseCase(new NotifyOutboxClaimService(dao));
        AtomicInteger sent = new AtomicInteger();
        org.namewta.notify.port.NotifyDispatchPort dispatch = org.mockito.Mockito.mock(
            org.namewta.notify.port.NotifyDispatchPort.class);
        org.mockito.Mockito.doAnswer(call -> {
            NotifyOutbox row = call.getArgument(0);
            assertTrue(row.getLeaseUntil().isAfter(clock.get()), "尚未外呼的任务必须拥有新领取的有效租约");
            assertEquals(Boolean.TRUE, row.getClaimedFromReady());
            // 三个单次小于租约的 I/O 累计超过 60 秒；后排任务不得提前消耗租约。
            clock.set(clock.get().plusSeconds(31));
            row.setStatus("DONE");
            sent.incrementAndGet();
            return null;
        }).when(dispatch).dispatch(any());

        new org.namewta.notify.adapter.worker.NotifyOutboxWorker(claim, dispatch).poll();

        assertEquals(3, sent.get());
        assertTrue(rows.stream().allMatch(row -> "DONE".equals(row.getStatus())));
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
