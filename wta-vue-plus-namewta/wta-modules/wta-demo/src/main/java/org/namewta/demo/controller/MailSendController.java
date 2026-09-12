package org.namewta.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


/**
 * 邮件发送案例
 *
 * @author Michelle.Chung
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/mail")
public class MailSendController {

    private final NotificationApplicationService notificationService;

    /**
     * 发送邮件
     *
     * @param to      接收人
     * @param subject 标题
     * @param text    内容
     */
    @GetMapping("/sendSimpleMessage")
    public R<Void> sendSimpleMessage(String to, String subject, String text) {
        send(to, subject, text, List.of());
        return R.ok();
    }

    /**
     * 发送邮件（带附件）
     *
     * @param to      接收人
     * @param subject 标题
     * @param text    内容
     */
    @GetMapping("/sendMessageWithAttachment")
    @SaCheckPermission("system:oss:download")
    public R<Void> sendMessageWithAttachment(String to, String subject, String text, Long ossId) {
        send(to, subject, text, List.of(ossId));
        return R.ok();
    }

    /**
     * 发送邮件（多附件）
     *
     * @param to      接收人
     * @param subject 标题
     * @param text    内容
     */
    @GetMapping("/sendMessageWithAttachments")
    @SaCheckPermission("system:oss:download")
    public R<Void> sendMessageWithAttachments(String to, String subject, String text, List<Long> ossIds) {
        send(to, subject, text, ossIds);
        return R.ok();
    }

    private void send(String to, String subject, String text, List<Long> ossIds) {
        notificationService.submit(new NotificationCommand("demo", "notice-published", "demo_mail", to,
            "EMAIL", List.of(to), "notice-published", Map.of("title", subject, "content", text, "path", "",
            "attachmentOssIds", ossIds), List.of(NotificationChannel.MAIL), NotificationStrategy.ALL,
            NotificationMode.SYNC, 20, null, null, null, Map.of()));
    }

}
