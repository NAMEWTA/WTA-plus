package org.namewta.common.sms.notify;

import org.namewta.common.notify.exception.NotifyValidationException;
import org.namewta.common.notify.model.NotifyContent;
import org.namewta.common.notify.model.NotifyTemplateContent;
import org.namewta.common.notify.model.NotifyTextContent;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;

import java.util.LinkedHashMap;

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
                new LinkedHashMap<>(template.params()));
        } else if (content instanceof NotifyTextContent text) {
            response = blend.sendMessage(phone, text.text());
        } else {
            response = blend.sendMessage(phone, content.contentSnapshot());
        }
        return response != null && response.isSuccess()
            ? SmsNotificationReceipt.accepted()
            : SmsNotificationReceipt.failed("PROVIDER_REJECTED", "SMS Provider 未接受请求");
    }
}
