package org.namewta.common.mail.notify;

import cn.hutool.extra.mail.MailAccount;

import java.nio.file.Path;
import java.util.List;

/**
 * MailBuilder 所需的渠道消息。
 */
public record MailNotificationMessage(
    List<String> to,
    List<String> cc,
    List<String> bcc,
    String subject,
    String content,
    boolean html,
    List<Path> attachments,
    MailAccount account
) {

    public MailNotificationMessage {
        to = to == null ? List.of() : List.copyOf(to);
        cc = cc == null ? List.of() : List.copyOf(cc);
        bcc = bcc == null ? List.of() : List.copyOf(bcc);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public MailNotificationMessage(List<String> to, List<String> cc, List<String> bcc,
                                   String subject, String content, boolean html) {
        this(to, cc, bcc, subject, content, html, List.of(), null);
    }

    public MailNotificationMessage(List<String> to, List<String> cc, List<String> bcc,
                                   String subject, String content, boolean html, List<Path> attachments) {
        this(to, cc, bcc, subject, content, html, attachments, null);
    }
}
