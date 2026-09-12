package org.namewta.test.notify.core;

import org.namewta.common.notify.model.*;
import org.namewta.common.sms.notify.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 短信通知 Adapter 测试。
 */
@Tag("dev")
class SmsNotifyChannelAdapterUnitTest {

    @Test
    void shouldResolveExplicitProviderAndAttemptEveryPhone() {
        List<String> phones = new ArrayList<>();
        SmsNotifyChannelAdapter adapter = new SmsNotifyChannelAdapter(requestedProvider ->
            new SmsNotificationProvider(requestedProvider == null ? "default-sms" : requestedProvider,
                (phone, content) -> {
                    phones.add(phone);
                    return phone.startsWith("139")
                        ? SmsNotificationReceipt.failed("PROVIDER_REJECTED", "provider rejected")
                        : SmsNotificationReceipt.accepted();
                }));
        NotifyRequest request = NotifyRequest.builder()
            .channel(NotifyChannel.SMS)
            .providerKey("sms-b")
            .targets(List.of(
                NotifyTarget.phone("13800000000"),
                NotifyTarget.phone("13900000000"),
                NotifyTarget.phone("13700000000")
            ))
            .content(new NotifyTextContent("subject", "content"))
            .build();

        NotifyAdapterResult result = adapter.send(new NotifyAdapterRequest(request, NotifyContext.empty()));

        assertEquals("sms-b", result.providerKey());
        assertEquals(List.of("13800000000", "13900000000", "13700000000"), phones);
        assertEquals(NotifyDeliveryStatus.FAILED, result.deliveries().get(1).status());
    }
}
