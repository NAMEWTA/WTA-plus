package org.namewta.test.notify.core;

import org.namewta.common.mail.notify.MailNotificationMessage;
import org.namewta.common.mail.notify.MailNotifyChannelAdapter;
import org.namewta.common.notify.model.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 邮件通知 Adapter 测试。
 */
@Tag("dev")
class MailNotifyChannelAdapterUnitTest {

    @Test
    void shouldKeepMailRecipientRolesAndShareProviderMessageId() {
        List<MailNotificationMessage> messages = new java.util.ArrayList<>();
        MailNotifyChannelAdapter adapter = new MailNotifyChannelAdapter(message -> {
            messages.add(message);
            return "mail-message-id";
        });
        NotifyRequest request = NotifyRequest.builder()
            .channel(NotifyChannel.MAIL)
            .targets(List.of(
                NotifyTarget.email("to@example.com", NotifyTargetRole.TO),
                NotifyTarget.email("cc@example.com", NotifyTargetRole.CC),
                NotifyTarget.email("bcc@example.com", NotifyTargetRole.BCC)
            ))
            .content(new NotifyRichContent("subject", "<b>content</b>", true))
            .build();

        NotifyAdapterResult result = adapter.send(new NotifyAdapterRequest(request, NotifyContext.empty()));

        assertEquals("smtp", result.providerKey());
        assertEquals(List.of("to@example.com"), messages.getFirst().to());
        assertEquals(List.of("cc@example.com"), messages.getFirst().cc());
        assertEquals(List.of("bcc@example.com"), messages.getFirst().bcc());
        assertEquals(3, result.deliveries().size());
        result.deliveries().forEach(item -> assertEquals("mail-message-id", item.providerMessageId()));
    }

    @Test
    void resolverRequiresProviderKeyAndResolvedAccount() {
        MailNotifyChannelAdapter adapter = new MailNotifyChannelAdapter(message -> "id", key -> null);
        NotifyRequest request = NotifyRequest.builder()
            .channel(NotifyChannel.MAIL)
            .targets(List.of(NotifyTarget.email("to@example.com")))
            .content(new NotifyRichContent("subject", "content", false))
            .build();
        org.namewta.common.notify.exception.NotifyValidationException missing = org.junit.jupiter.api.Assertions.assertThrows(
            org.namewta.common.notify.exception.NotifyValidationException.class,
            () -> adapter.send(new NotifyAdapterRequest(request, NotifyContext.empty())));
        assertEquals("UNKNOWN_PROVIDER", missing.code());
    }

    @Test
    void preSendDatabaseGateClosesBeforePhysicalSenderAndPreservesSqlFailure() {
        AtomicInteger sent = new AtomicInteger();
        MailNotifyChannelAdapter adapter = new MailNotifyChannelAdapter(message -> {
            sent.incrementAndGet();
            return "should-not-send";
        });
        NotifyRequest.PreSendGate closed = new NotifyRequest.PreSendGate(() -> {
            throw new NotifyRequest.PreSendClosed();
        });
        NotifyRequest request = mailWithGate(closed);
        assertThrows(NotifyRequest.PreSendClosed.class,
            () -> adapter.send(new NotifyAdapterRequest(request, NotifyContext.empty())));
        assertEquals(0, sent.get());
        assertEquals(true, closed.failed());

        IllegalStateException databaseFailure = new IllegalStateException("owned database commit failed");
        NotifyRequest.PreSendGate failed = new NotifyRequest.PreSendGate(() -> { throw databaseFailure; });
        assertSame(databaseFailure, assertThrows(IllegalStateException.class,
            () -> adapter.send(new NotifyAdapterRequest(mailWithGate(failed), NotifyContext.empty()))));
        assertEquals(0, sent.get());
        assertEquals(true, failed.failed());
    }

    private NotifyRequest mailWithGate(NotifyRequest.PreSendGate gate) {
        return NotifyRequest.builder().channel(NotifyChannel.MAIL)
            .targets(List.of(NotifyTarget.email("to@example.com")))
            .content(new NotifyRichContent("subject", "content", false))
            .preSendGate(gate).build();
    }
}
