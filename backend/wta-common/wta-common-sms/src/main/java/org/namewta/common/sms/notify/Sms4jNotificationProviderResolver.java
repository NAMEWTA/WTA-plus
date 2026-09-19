package org.namewta.common.sms.notify;

import org.namewta.common.notify.exception.NotifyValidationException;
import org.namewta.common.notify.model.NotifyContent;
import org.namewta.common.notify.model.NotifyTemplateContent;
import org.namewta.common.notify.model.NotifyTextContent;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SMS4J Provider 解析实现。
 */
public final class Sms4jNotificationProviderResolver implements SmsNotificationProviderResolver {

    @Override
    public SmsNotificationProvider resolve(String requestedProviderKey) {
        if (requestedProviderKey == null || requestedProviderKey.isBlank()) {
            throw new NotifyValidationException("UNKNOWN_PROVIDER", "短信发送缺少渠道账号");
        }
        SmsBlend blend = SmsFactory.getSmsBlend(requestedProviderKey);
        if (blend == null) {
            throw new NotifyValidationException("UNKNOWN_PROVIDER", "未找到可用的 SMS Provider");
        }
        String providerKey = blend.getConfigId();
        if (providerKey == null || providerKey.isBlank()) {
            providerKey = requestedProviderKey == null || requestedProviderKey.isBlank()
                ? blend.getSupplier() : requestedProviderKey;
        }
        SmsBlend selectedBlend = blend;
        return new SmsNotificationProvider(providerKey, (phone, content) -> send(selectedBlend, phone, content));
    }

    private SmsNotificationReceipt send(SmsBlend blend, String phone, NotifyContent content) {
        SmsResponse response;
        if (content instanceof NotifyTemplateContent template) {
            response = blend.sendMessage(phone, template.providerTemplateCode(),
                templateParams(blend.getSupplier(), template.params()));
        } else if (content instanceof NotifyTextContent text) {
            response = blend.sendMessage(phone, text.text());
        } else {
            response = blend.sendMessage(phone, content.contentSnapshot());
        }
        return response != null && response.isSuccess()
            ? SmsNotificationReceipt.accepted(providerMessageId(blend.getSupplier(), phone, response.getData()))
            : SmsNotificationReceipt.failed("PROVIDER_REJECTED", "SMS Provider 未接受请求");
    }

    /** 腾讯位置参数不能依赖不可变Map的迭代顺序；阿里仍保留供应商参数名。 */
    private LinkedHashMap<String, String> templateParams(String supplier, Map<String, String> params) {
        if (!"tencent".equals(supplier)) return new LinkedHashMap<>(params);
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        for (int index = 1; index <= params.size(); index++) {
            String key = Integer.toString(index);
            String value = params.get(key);
            if (value == null || value.isBlank()) {
                throw new NotifyValidationException("INVALID_TEMPLATE_PARAMETERS", "腾讯短信参数须映射为连续的1..N位置且值不能为空");
            }
            ordered.put(key, value);
        }
        return ordered;
    }

    /**
     * 按实际SMS4J 3.3.5成功响应读取供应商流水号，绝不用RequestId替代。
     * 成功响应缺少可确认的ID时仍保持已受理，避免将已发送短信判失败而自动重发。
     */
    private String providerMessageId(String supplier, String phone, Object data) {
        if (!(data instanceof Map<?, ?> body)) return null;
        if ("alibaba".equals(supplier)) return nonblankString(body.get("BizId"));
        if (!"tencent".equals(supplier) || !(body.get("Response") instanceof Map<?, ?> response)
            || !(response.get("SendStatusSet") instanceof Iterable<?> statuses)) return null;
        // 与SMS4J调用时的号码规则一致：含区号分隔符则去掉分隔符，否则补国内+86前缀。
        String requestedPhone = phone.contains("-") ? phone.replace("-", "")
            : phone.startsWith("+86") ? phone : "+86" + phone;
        String messageId = null;
        int matches = 0;
        for (Object item : statuses) {
            if (item instanceof Map<?, ?> status && requestedPhone.equals(status.get("PhoneNumber"))
                && "Ok".equals(status.get("Code"))) {
                messageId = nonblankString(status.get("SerialNo"));
                matches++;
            }
        }
        return matches == 1 ? messageId : null;
    }

    private String nonblankString(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
