package org.namewta.test.notify.attachment;

import org.namewta.common.mail.notify.MailNotificationMessage;
import org.namewta.common.mail.notify.MailNotifyChannelAdapter;
import org.namewta.common.notify.attachment.NotifyAttachmentResource;
import org.namewta.common.notify.attachment.NotifyAttachmentSnapshot;
import org.namewta.common.notify.exception.NotifyAttachmentSnapshotException;
import org.namewta.common.notify.model.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mail Provider 只读取快照并清理本地临时文件。
 */
@Tag("dev")
class MailAttachmentSnapshotUnitTest {

    @Test
    void shouldMaterializeSnapshotsForSenderAndDeleteLocalFilesAfterward() {
        List<Path> senderPaths = new ArrayList<>();
        List<String> senderContents = new ArrayList<>();
        MailNotifyChannelAdapter adapter = new MailNotifyChannelAdapter(message -> {
            senderPaths.addAll(message.attachments());
            message.attachments().forEach(path -> senderContents.add(assertDoesNotThrow(() -> Files.readString(path))));
            return "message-1";
        });
        NotifyRequest request = NotifyRequest.builder()
            .channel(NotifyChannel.MAIL)
            .targets(List.of(NotifyTarget.email("to@example.com", NotifyTargetRole.TO)))
            .content(new NotifyRichContent("subject", "content", false))
            .attachmentOssIds(List.of(10L))
            .build();
        NotifyAttachmentSnapshot snapshot = new NotifyAttachmentSnapshot(10L,
            new NotifyAttachmentResource(110L, "invoice.txt", "text/plain", 7,
                target -> Files.writeString(target, "invoice")));

        NotifyAdapterResult result = adapter.send(new NotifyAdapterRequest(request, NotifyContext.empty(),
            List.of(snapshot)));

        assertEquals(NotifyDeliveryStatus.ACCEPTED, result.deliveries().getFirst().status());
        assertEquals(List.of("invoice"), senderContents);
        assertEquals("invoice.txt", senderPaths.getFirst().getFileName().toString());
        assertTrue(senderPaths.stream().noneMatch(Files::exists));
    }

    @Test
    void messageWithoutAttachmentsMustRemainSourceCompatible() {
        MailNotificationMessage message = new MailNotificationMessage(List.of("to@example.com"), List.of(),
            List.of(), "subject", "content", false);

        assertTrue(message.attachments().isEmpty());
    }

    @Test
    void materializeFailureMustPreventSmtpInvocation() {
        AtomicInteger senderCalls = new AtomicInteger();
        MailNotifyChannelAdapter adapter = new MailNotifyChannelAdapter(message -> {
            senderCalls.incrementAndGet();
            return "message-1";
        });
        NotifyRequest request = NotifyRequest.builder()
            .channel(NotifyChannel.MAIL)
            .targets(List.of(NotifyTarget.email("to@example.com", NotifyTargetRole.TO)))
            .content(new NotifyRichContent("subject", "content", false))
            .attachmentOssIds(List.of(10L))
            .build();
        NotifyAttachmentSnapshot snapshot = new NotifyAttachmentSnapshot(10L,
            new NotifyAttachmentResource(110L, "invoice.txt", "text/plain", 7,
                target -> {
                    throw new java.io.IOException("download failed");
                }));

        NotifyAttachmentSnapshotException exception = assertThrows(NotifyAttachmentSnapshotException.class,
            () -> adapter.send(new NotifyAdapterRequest(request, NotifyContext.empty(), List.of(snapshot))));

        assertEquals("SNAPSHOT_MATERIALIZE_FAILED", exception.code());
        assertEquals(0, senderCalls.get());
    }
}
