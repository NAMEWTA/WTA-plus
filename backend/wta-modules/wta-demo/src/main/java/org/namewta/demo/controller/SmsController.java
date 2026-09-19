package org.namewta.demo.controller;

import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.log.annotation.Log;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 短信演示案例
 * 请先阅读文档 否则无法使用
 *
 * @author Lion Li
 * @version 4.2.0
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/sms")
public class SmsController {

    private final NotificationApplicationService notificationService;
    /**
     * 发送短信Aliyun
     *
     * @param phones     电话号
     * @param templateId 模板ID
     */
    @PostMapping("/sendAliyun")
    @Log(title = "短信演示发送", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false)
    public R<Object> sendAliyun(String phones, String templateId) {
        return sendTemplate(phones);
    }

    /**
     * 发送短信Tencent
     *
     * @param phones     电话号
     * @param templateId 模板ID
     */
    @PostMapping("/sendTencent")
    @Log(title = "短信演示发送", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false)
    public R<Object> sendTencent(String phones, String templateId) {
        return sendTemplate(phones);
    }

    /**
     * 添加黑名单
     *
     * @param phone 手机号
     */
    @PostMapping("/addBlacklist")
    @Log(title = "短信黑名单", businessType = BusinessType.INSERT, isSaveRequestData = false, isSaveResponseData = false)
    public R<Object> addBlacklist(String phone) {
        SmsBlend smsBlend = SmsFactory.getSmsBlend("config1");
        smsBlend.joinInBlacklist(phone);
        return R.ok();
    }

    /**
     * 移除黑名单
     *
     * @param phone 手机号
     */
    @PostMapping("/removeBlacklist")
    @Log(title = "短信黑名单", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    public R<Object> removeBlacklist(String phone) {
        SmsBlend smsBlend = SmsFactory.getSmsBlend("config1");
        smsBlend.removeFromBlacklist(phone);
        return R.ok();
    }

    private R<Object> sendTemplate(String phones) {
        List<String> targets = Arrays.stream(phones.split(","))
            .map(String::trim)
            .filter(phone -> !phone.isEmpty())
            .toList();
        return R.ok(notificationService.submit(new NotificationCommand("demo", "auth-captcha", "demo_sms",
            String.join(",", targets), "PHONE", targets, "auth-captcha",
            Map.of("code", "1234", "expireMinutes", "5"),
            List.of(NotificationChannel.SMS), NotificationStrategy.ALL, NotificationMode.ASYNC, 20,
            null, null, null, Map.of())));
    }

}
