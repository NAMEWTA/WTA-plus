package org.namewta.notify.support;

import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.port.NotifyQuotaPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
     * @param smsSnapshot     仅包含场景、模板与变量名的安全逻辑快照
     */
    public record Plan(boolean ok, String errorCode, String errorMessage, String providerKey, boolean mail,
                       String subject, String body, boolean html, String smsTemplateCode,
                       Map<String, String> smsParams, String smsSnapshot) {
        public static Plan fail(String code, String message) {
            return new Plan(false, code, message, null, false, null, null, false, null, Map.of(), null);
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
        Plan prepared = preflight(sceneCode, channel, params, binding, account);
        if (!prepared.ok()) return prepared;
        return finish(account, binding, sceneCode, channel, target, quotaPort, prepared);
    }

    /** 提交前和发送前共享纯校验；不预占配额，也不接触供应商。 */
    public static Plan preflight(String sceneCode, String channel, Map<String, String> params,
                                 NotifySceneBinding binding, NotifyChannelAccount account) {
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
            return new Plan(true, null, null, account.getConfigKey(), true, subject, body,
                body != null && body.contains("<"), null, Map.of(), null);
        }
        if (binding.getSmsTemplateCode() == null || binding.getSmsTemplateCode().isBlank()) {
            return Plan.fail("SMS_TEMPLATE_MISSING", "未配置短信供应商模板码");
        }
        Map<String, String> mapping = mapping(binding.getSmsParamMappingJson());
        for (String name : required) {
            if (mapping.get(name) == null || mapping.get(name).isBlank()) {
                return Plan.fail("INVALID_TEMPLATE_PARAMETERS", "短信模板缺少必填变量映射 " + name);
            }
        }
        if (mapping.values().stream().anyMatch(String::isBlank)) {
            return Plan.fail("INVALID_TEMPLATE_PARAMETERS", "短信模板参数映射不能为空");
        }
        for (String logical : mapping.keySet()) {
            if (params == null || params.get(logical) == null || params.get(logical).isBlank()) {
                return Plan.fail("INVALID_TEMPLATE_PARAMETERS", "短信模板映射变量缺失或为空");
            }
        }
        if ("tencent".equalsIgnoreCase(account.getSupplier())) {
            for (int index = 1; index <= mapping.size(); index++) {
                if (!mapping.containsValue(Integer.toString(index))) {
                    return Plan.fail("INVALID_TEMPLATE_PARAMETERS", "腾讯短信参数须映射为连续的1..N位置");
                }
            }
        }
        Map<String, String> providerParams = new LinkedHashMap<>();
        mapping.forEach((logical, providerName) -> providerParams.put(providerName, params.get(logical)));
        return new Plan(true, null, null, account.getConfigKey(), false, null, null, false,
            binding.getSmsTemplateCode(), providerParams,
            NotifyTemplateRenderer.smsSnapshot(sceneCode, binding.getSmsTemplateCode(), mapping.keySet()));
    }

    private static Plan finish(NotifyChannelAccount account, NotifySceneBinding binding, String sceneCode,
                               String channel, String target, NotifyQuotaPort quotaPort, Plan prepared) {
        List<String> held = new ArrayList<>();
        if (!acquireQuota(quotaPort, held, "acct:" + account.getAccountId(), account.getMinuteMax(), Duration.ofMinutes(1))) {
            return Plan.fail("ACCOUNT_QUOTA", "账号每分钟发送上限已用尽");
        }
        int templateMax = binding.getTemplateMinuteMax() == null ? account.getMinuteMax() : binding.getTemplateMinuteMax();
        if (!acquireQuota(quotaPort, held, "tpl:" + sceneCode + ":" + channel, templateMax, Duration.ofMinutes(1))) {
            releaseHeld(quotaPort, held);
            return Plan.fail("TEMPLATE_QUOTA", "模板每分钟发送上限已用尽");
        }
        if ("Y".equals(binding.getRestricted()) && target != null && !target.isBlank()) {
            int minute = binding.getRecipientMinuteMax() == null ? 0 : binding.getRecipientMinuteMax();
            int day = binding.getRecipientDayMax() == null ? 0 : binding.getRecipientDayMax();
            String safeTarget = recipientQuotaToken(target);
            if (!acquireQuota(quotaPort, held, "rcpt-m:" + sceneCode + ":" + channel + ":" + safeTarget, minute, Duration.ofMinutes(1))) {
                releaseHeld(quotaPort, held);
                return Plan.fail("RECIPIENT_MINUTE_QUOTA", "收件人每分钟拦截上限已用尽");
            }
            if (!acquireQuota(quotaPort, held, "rcpt-d:" + sceneCode + ":" + channel + ":" + safeTarget + ":"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), day, Duration.ofDays(1))) {
                releaseHeld(quotaPort, held);
                return Plan.fail("RECIPIENT_DAY_QUOTA", "收件人每天拦截上限已用尽");
            }
        }
        return prepared;
    }

    /**
     * 收件人限额键：规范化后取 SHA-256 前 16 个十六进制字符，避免 hashCode 碰撞与跨 JVM 不稳定。
     *
     * @param target 邮箱或手机号
     * @return 稳定摘要
     */
    public static String recipientQuotaToken(String target) {
        String normalized = target == null ? "" : target.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", digest[i] & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean acquireQuota(NotifyQuotaPort quotaPort, List<String> held, String key, int limit, Duration window) {
        if (quotaPort == null || limit <= 0) {
            return true;
        }
        if (!quotaPort.tryAcquire(key, limit, window)) {
            return false;
        }
        held.add(key);
        return true;
    }

    private static void releaseHeld(NotifyQuotaPort quotaPort, List<String> held) {
        if (quotaPort == null) {
            return;
        }
        for (int i = held.size() - 1; i >= 0; i--) {
            quotaPort.release(held.get(i));
        }
        held.clear();
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
