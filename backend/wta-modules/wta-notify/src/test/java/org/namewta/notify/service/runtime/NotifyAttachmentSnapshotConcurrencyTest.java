package org.namewta.notify.service.runtime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.notify.exception.NotifyAttachmentSnapshotException;
import org.namewta.common.notify.model.NotifyContext;
import org.namewta.notify.adapter.NotifyAttachmentSnapshotAdapter;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyIntentAttachment;
import org.namewta.notify.usecase.NotifyAttachmentSnapshotUseCase;
import org.namewta.system.api.OssService;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 多收件人共用快照时，复制中的等待与复制未知必须保持不同的恢复语义。 */
@Tag("dev")
class NotifyAttachmentSnapshotConcurrencyTest {

    @Test
    void concurrentRecipientWaitsThenReusesOneReadySnapshotWithoutRecopy() throws Exception {
        Fixture fixture = fixture("QUEUED");
        var reservation = new OssService.NotificationCopyReservation(10L, 77L, 88L,
            "source", "source-key", "private", "target-key", "owned.txt", "text/plain", 5L, 7L, 8L);
        when(fixture.oss.reserveNotificationSnapshot(10L, 77L, 7L, 8L)).thenReturn(reservation);
        CountDownLatch copying = new CountDownLatch(1);
        CountDownLatch finishCopy = new CountDownLatch(1);
        when(fixture.oss.copyNotificationSnapshot(reservation)).thenAnswer(call -> {
            copying.countDown();
            assertTrue(finishCopy.await(5, TimeUnit.SECONDS));
            return new OssService.NotificationCopyResult(5L, "owned-sha256");
        });
        var pool = Executors.newSingleThreadExecutor();
        try {
            var first = pool.submit(() -> fixture.adapter.createSnapshots(1L, List.of(77L), NotifyContext.empty()));
            assertTrue(copying.await(5, TimeUnit.SECONDS));
            NotifyAttachmentSnapshotException waiting = assertThrows(NotifyAttachmentSnapshotException.class,
                () -> fixture.adapter.createSnapshots(1L, List.of(77L), NotifyContext.empty()));
            assertEquals("ATTACHMENT_COPY_IN_PROGRESS", waiting.code());
            assertEquals("COPYING", fixture.relation.getStatus());
            verify(fixture.oss, times(1)).copyNotificationSnapshot(reservation);

            finishCopy.countDown();
            assertEquals(88L, first.get(5, TimeUnit.SECONDS).getFirst().resource().ossId());
            assertEquals("READY", fixture.relation.getStatus());
            var second = fixture.adapter.createSnapshots(1L, List.of(77L), NotifyContext.empty());
            assertEquals(88L, second.getFirst().resource().ossId());
            verify(fixture.oss, times(1)).reserveNotificationSnapshot(10L, 77L, 7L, 8L);
            verify(fixture.oss, times(1)).copyNotificationSnapshot(reservation);
            verify(fixture.oss, times(1)).confirmNotificationSnapshot(any(), any());
        } finally {
            finishCopy.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void unknownCopyRemainsClosedAndNeverCreatesAnotherRemoteWrite() {
        Fixture fixture = fixture("COPY_UNKNOWN");

        NotifyAttachmentSnapshotException rejected = assertThrows(NotifyAttachmentSnapshotException.class,
            () -> fixture.adapter.createSnapshots(1L, List.of(77L), NotifyContext.empty()));

        assertEquals("ATTACHMENT_RESERVATION_FAILED", rejected.code());
        assertEquals("COPY_UNKNOWN", fixture.relation.getStatus());
        verify(fixture.oss, never()).reserveNotificationSnapshot(any(), any(), any(), any());
        verify(fixture.oss, never()).copyNotificationSnapshot(any());
        verify(fixture.dao, never()).saveAttachment(any());
    }

    @Test
    void abandonedCopyAtDeadlineBecomesUnknownWithoutRecopyAndRejectsLateConfirmation() {
        Fixture fixture = fixture("COPYING");
        var now = java.time.LocalDateTime.of(2026, 9, 26, 12, 0);
        fixture.relation.setUpdateTime(now.minusSeconds(60));
        fixture.relation.setCopyToken("original-token");
        fixture.relation.setSnapshotOssId(88L);

        NotifyAttachmentSnapshotException rejected = assertThrows(NotifyAttachmentSnapshotException.class,
            () -> fixture.adapter.createSnapshots(1L, List.of(77L), NotifyContext.empty()));

        assertEquals("ATTACHMENT_COPY_UNCERTAIN", rejected.code());
        assertEquals("COPY_UNKNOWN", fixture.relation.getStatus());
        assertEquals("original-token", fixture.relation.getCopyToken());
        assertEquals(88L, fixture.relation.getSnapshotOssId());
        verify(fixture.dao).saveAttachment(fixture.relation);
        verify(fixture.oss, never()).copyNotificationSnapshot(any());
        var reservation = new OssService.NotificationCopyReservation(10L, 77L, 88L,
            "source", "source-key", "private", "target-key", "owned.txt", "text/plain", 5L, 7L, 8L);
        var service = new NotifyAttachmentSnapshotService(fixture.dao, fixture.oss);
        var prepared = new org.namewta.notify.port.NotifyAttachmentSnapshotPort.Prepared(fixture.relation, reservation);
        assertThrows(org.namewta.common.core.exception.ServiceException.class,
            () -> service.confirm(1L, 10L, "original-token", prepared, 5L, "owned-sha256"));
        verify(fixture.oss, never()).confirmNotificationSnapshot(any(), any());
    }

    @Test
    void copyingWithoutTrustedStartTimeBecomesUnknownInsteadOfWaitingForever() {
        Fixture fixture = fixture("COPYING");
        fixture.relation.setUpdateTime(null);

        NotifyAttachmentSnapshotException rejected = assertThrows(NotifyAttachmentSnapshotException.class,
            () -> fixture.adapter.createSnapshots(1L, List.of(77L), NotifyContext.empty()));

        assertEquals("ATTACHMENT_COPY_UNCERTAIN", rejected.code());
        assertEquals("COPY_UNKNOWN", fixture.relation.getStatus());
        verify(fixture.dao).saveAttachment(fixture.relation);
        verify(fixture.oss, never()).copyNotificationSnapshot(any());
    }

    private Fixture fixture(String status) {
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        OssService oss = mock(OssService.class);
        NotifyIntent intent = new NotifyIntent();
        intent.setIntentId(1L);
        intent.setAttachmentActorUserId(7L);
        intent.setAttachmentActorClientPk(8L);
        NotifyIntentAttachment relation = new NotifyIntentAttachment();
        relation.setIntentAttachmentId(10L);
        relation.setIntentId(1L);
        relation.setSourceOssId(77L);
        relation.setFileName("owned.txt");
        relation.setContentType("text/plain");
        relation.setFileSize(5L);
        relation.setStatus(status);
        when(dao.lockIntent(1L)).thenReturn(intent);
        when(dao.attachments(1L)).thenReturn(List.of(relation));
        when(dao.lockAttachments(1L)).thenReturn(List.of(relation));
        var now = java.time.LocalDateTime.of(2026, 9, 26, 12, 0);
        when(dao.databaseNow()).thenReturn(now);
        relation.setUpdateTime(now);
        when(dao.saveAttachment(any())).thenReturn(1);
        when(dao.saveAttachmentCopyReservation(any())).thenAnswer(call -> {
            relation.setUpdateTime(now);
            return 1;
        });
        var service = new NotifyAttachmentSnapshotService(dao, oss);
        var adapter = new NotifyAttachmentSnapshotAdapter(new NotifyAttachmentSnapshotUseCase(service), oss);
        return new Fixture(adapter, dao, oss, relation);
    }

    private record Fixture(NotifyAttachmentSnapshotAdapter adapter, NotifyNotificationDao dao,
                           OssService oss, NotifyIntentAttachment relation) { }
}
