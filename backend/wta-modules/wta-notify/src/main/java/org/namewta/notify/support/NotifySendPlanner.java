package org.namewta.notify.support;

import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.port.NotifyQuotaPort;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析场景绑定、渲染文案并检查三层限额。不依赖渠道 SPI 类型。
 */
public final class NotifySendPlanner {

    private NotifySendPlanner() {
    }

    /**
     * 发送计划。
     *
     * @param ok              是否可发送
     * @param errorCode       失败码
     * @param errorMessage    失败说明
     * @param providerKey     账号标识
     * @param mail            是否邮件
     * @param subject         邮件主题
     * @param body            邮件正文
     * @param html            是否 HTML
     * @param smsTemplateCode 短信模板码
     * @param smsParams       短信参数
     */
    public record Plan(boolean ok, String errorCode, String errorMessage, String providerKey, boolean mail,
                       String subject, String body, boolean html, String smsTemplateCode,
                       Map<String, String> smsParams) {
        public static Plan fail(String code, String message) {
            return new Plan(false, code, message, null, false, null, null, false, null, Map.of());
        }
    }

    /**
     * 生成 MAIL/SMS 发送计划。
     *
     * @param sceneCode 场景
     * @param channel   渠道
     * @param target    收件人
     * @param params    变量
     * @param binding   绑定
     * @param account   账号
     * @param quotaPort 限额
     * @return 计划
     */
    public static Plan plan(String sceneCode, String channel, String target, Map<String, String> params,
                            NotifySceneBinding binding, NotifyChannelAccount account, NotifyQuotaPort quotaPort) {
        if (binding == null || binding.getAccountId() == null) {
            return Plan.fail("UNBOUND_CHANNEL", "场景未绑定渠道账号");
        }
        if (account == null || !"Y".equals(account.getEnabled())) {
            return Plan.fail("ACCOUNT_DISABLED", "渠道账号未启用或不存在");
        }
        if (!channel.equals(account.getChannel())) {
            return Plan.fail("ACCOUNT_CHANNEL_MISMATCH", "绑定账号渠道不匹配");
        }
        List<String> required = NotifySceneCatalog.requiredNames(sceneCode);
        for (String name : required) {
            if (params == null || params.get(name) == null || params.get(name).isBlank()) {
                return Plan.fail("MISSING_VARIABLE", "缺少必填变量 " + name);
            }
        }
        if ("MAIL".equals(channel)) {
            String subject = NotifyTemplateRenderer.render(binding.getMailSubject(), params);
            String body = NotifyTemplateRenderer.render(binding.getMailBody(), params);
            return finish(account, binding, sceneCode, channel, target, quotaPort, true, subject, body,
                body != null && body.contains("<"), null, Map.of());
        }
        if (binding.getSmsTemplateCode() == null || binding.getSmsTemplateCode().isBlank()) {
            return Plan.fail("SMS_TEMPLATE_MISSING", "未配置短信供应商模板码");
        }
        Map<String, String> mapping = mapping(binding.getSmsParamMappingJson());
        Map<String, String> providerParams = new LinkedHashMap<>();
        mapping.forEach((logical, providerName) -> {
            if (params != null) {
                providerParams.put(providerName, params.getOrDefault(logical, ""));
            }
        });
        return finish(account, binding, sceneCode, channel, target, quotaPort, false, null, null, false,
            binding.getSmsTemplateCode(), providerParams);
    }

    private static Plan finish(NotifyChannelAccount account, NotifySceneBinding binding, String sceneCode,
                               String channel, String target, NotifyQuotaPort quotaPort, boolean mail,
                               String subject, String body, boolean html, String smsTemplateCode,
                               Map<String, String> smsParams) {
        if (!tryQuota(quotaPort, "acct:" + account.getAccountId(), account.getMinuteMax(), Duration.ofMinutes(1))) {
            return Plan.fail("ACCOUNT_QUOTA", "账号每分钟发送上限已用尽");
        }
        int templateMax = binding.getTemplateMinuteMax() == null ? account.getMinuteMax() : binding.getTemplateMinuteMax();
        if (!tryQuota(quotaPort, "tpl:" + sceneCode + ":" + channel, templateMax, Duration.ofMinutes(1))) {
            return Plan.fail("TEMPLATE_QUOTA", "模板每分钟发送上限已用尽");
        }
        if ("Y".equals(binding.getRestricted()) && target != null && !target.isBlank()) {
            int minute = binding.getRecipientMinuteMax() == null ? 0 : binding.getRecipientMinuteMax();
            int day = binding.getRecipientDayMax() == null ? 0 : binding.getRecipientDayMax();
            String safeTarget = Integer.toHexString(target.hashCode());
            if (!tryQuota(quotaPort, "rcpt-m:" + sceneCode + ":" + channel + ":" + safeTarget, minute, Duration.ofMinutes(1))) {
                return Plan.fail("RECIPIENT_MINUTE_QUOTA", "收件人每分钟拦截上限已用尽");
            }
            if (!tryQuota(quotaPort, "rcpt-d:" + sceneCode + ":" + channel + ":" + safeTarget + ":"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), day, Duration.ofDays(1))) {
                return Plan.fail("RECIPIENT_DAY_QUOTA", "收件人每天拦截上限已用尽");
            }
        }
        return new Plan(true, null, null, account.getConfigKey(), mail, subject, body, html, smsTemplateCode, smsParams);
    }

    private static boolean tryQuota(NotifyQuotaPort quotaPort, String key, int limit, Duration window) {
        if (quotaPort == null) {
            return true;
        }
        return quotaPort.tryAcquire(key, limit, window);
    }

    private static Map<String, String> mapping(String json) {
        var parsed = JsonUtils.parseMap(json);
        if (parsed == null) {
            return Map.of();
        }
        Map<String, String> mapping = new LinkedHashMap<>();
        parsed.forEach((key, value) -> mapping.put(key, value == null ? "" : String.valueOf(value)));
        return mapping;
    }
}
