package org.namewta.demo.controller;

import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.log.annotation.Log;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


/**
 * 邮件发送案例
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
    @PostMapping("/sendSimpleMessage")
    @Log(title = "邮件演示发送", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false)
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
    @PostMapping("/sendMessageWithAttachment")
    @SaCheckPermission("system:oss:download")
    @Log(title = "邮件演示发送", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false)
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
    @PostMapping("/sendMessageWithAttachments")
    @SaCheckPermission("system:oss:download")
    @Log(title = "邮件演示发送", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> sendMessageWithAttachments(String to, String subject, String text, List<Long> ossIds) {
        send(to, subject, text, ossIds);
        return R.ok();
    }

    private void send(String to, String subject, String text, List<Long> ossIds) {
        notificationService.submit(new NotificationCommand("demo", "demo-mail", "demo_mail", to,
            "EMAIL", List.of(to), "demo-mail", Map.of("title", subject, "content", text),
            List.of(NotificationChannel.MAIL), NotificationStrategy.ALL,
            NotificationMode.ASYNC, 0, null, null, null, Map.of(),
            ossIds.stream().map(String::valueOf).toList()));
    }

}
