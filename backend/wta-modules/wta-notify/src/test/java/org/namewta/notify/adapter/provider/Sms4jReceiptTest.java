package org.namewta.notify.adapter.provider;

import cn.hutool.json.JSONUtil;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.notify.exception.NotifyValidationException;
import org.namewta.common.notify.model.NotifyTemplateContent;
import org.namewta.common.notify.model.NotifyDeliveryStatus;
import org.namewta.common.sms.notify.Sms4jNotificationProviderResolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 实际Factory与项目发送边界；外部发送响应为按供应商协议构造的受控替身。 */
@Tag("dev")
class Sms4jReceiptTest {
    @Test
    void alibabaPreservesBizIdInsteadOfTheRequestId() throws Exception {
        withBlend("alibaba", "{\"Code\":\"OK\",\"BizId\":\"owned-biz\",\"RequestId\":\"not-message-id\"}", true,
            (blend, key) -> {
                var result = new Sms4jNotificationProviderResolver().resolve(key).sender().send("13800000000", template(Map.of("code", "123456")));
                assertTrue(result.success());
                assertEquals("owned-biz", result.providerMessageId());
            });
    }

    @Test
    void tencentCorrelatesTheRequestedPhoneAndSortsNumericTemplatePositions() throws Exception {
        withBlend("tencent", "{\"Response\":{\"RequestId\":\"request\",\"SendStatusSet\":["
            + "{\"PhoneNumber\":\"+8613900000000\",\"SerialNo\":\"other\",\"Code\":\"Ok\"},"
            + "{\"PhoneNumber\":\"+8613800000000\",\"SerialNo\":\"owned-serial\",\"Code\":\"Ok\"}]}}", true,
            (blend, key) -> {
                LinkedHashMap<String, String> unordered = new LinkedHashMap<>();
                for (int i = 12; i > 0; i--) unordered.put(Integer.toString(i), "v" + i);
                var result = new Sms4jNotificationProviderResolver().resolve(key).sender().send("13800000000", template(unordered));
                assertEquals("owned-serial", result.providerMessageId());
                verify(blend).sendMessage(eq("13800000000"), eq("owned-template"), argThat(params ->
                    List.copyOf(params.values()).equals(java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(i -> "v" + i).toList())));
            });
    }

    @Test
    void tencentInvalidPositionsAreRejectedBeforeTheProviderCall() throws Exception {
        withBlend("tencent", "{}", true, (blend, key) -> {
            var sender = new Sms4jNotificationProviderResolver().resolve(key).sender();
            for (Map<String, String> params : List.of(Map.of("code", "123456"), Map.of("2", "gap"), Map.of("1", " "))) {
                assertThrows(NotifyValidationException.class, () -> sender.send("13800000000", template(params)));
            }
            verify(blend, never()).sendMessage(anyString(), anyString(), any(LinkedHashMap.class));
        });
    }

    @Test
    void acceptedWithoutAUsableMessageIdDoesNotBecomeAFailureOrAnotherTargetsId() throws Exception {
        for (String payload : List.of("{}", "{\"Response\":{\"SendStatusSet\":["
            + "{\"PhoneNumber\":\"+8613900000000\",\"SerialNo\":\"other\",\"Code\":\"Ok\"}]}}")) {
            withBlend("tencent", payload, true, (blend, key) -> {
                var result = new Sms4jNotificationProviderResolver().resolve(key).sender().send("13800000000", template(Map.of("1", "code")));
                assertTrue(result.success());
                assertNull(result.providerMessageId());
                verify(blend, times(1)).sendMessage(anyString(), anyString(), any(LinkedHashMap.class));
            });
        }
    }

    @Test
    void explicitProviderRejectionRemainsFailed() throws Exception {
        withBlend("alibaba", "{\"Code\":\"isv.INVALID_PARAMETERS\"}", false, (blend, key) -> {
            var result = new Sms4jNotificationProviderResolver().resolve(key).sender().send("13800000000", template(Map.of()));
            assertFalse(result.success());
            assertNull(result.providerMessageId());
        });
    }

    @Test
    void tencentThirtySecondRejectionRequiresTheExactSelectedSingleAttemptBlend() throws Exception {
        String response = "{\"Response\":{\"RequestId\":\"owned-request\",\"SendStatusSet\":["
            + "{\"PhoneNumber\":\"+8613800000000\",\"SerialNo\":\"\",\"Fee\":0,"
            + "\"Code\":\"LimitExceeded.PhoneNumberThirtySecondLimit\"}]}}";
        withBlend("tencent", response, false, (blend, key) -> {
            var content = template(Map.of("1", "123456"));
            assertEquals(NotifyDeliveryStatus.FAILED,
                new Sms4jNotificationProviderResolver().resolve(key).sender().send("13800000000", content).outcome());
            assertEquals(NotifyDeliveryStatus.UNSENT_RETRYABLE,
                new Sms4jNotificationProviderResolver(selected -> selected == blend)
                    .resolve(key).sender().send("13800000000", content).outcome());
            assertEquals(NotifyDeliveryStatus.FAILED,
                new Sms4jNotificationProviderResolver(selected -> false)
                    .resolve(key).sender().send("13800000000", content).outcome());
            verify(blend, times(3)).sendMessage(anyString(), anyString(), any(LinkedHashMap.class));
        });
    }

    @Test
    void tencentAmbiguousOrMalformedRejectionsNeverGainRetryAuthority() throws Exception {
        String status = "{\"PhoneNumber\":\"+8613800000000\",\"SerialNo\":\"\",\"Fee\":0,"
            + "\"Code\":\"LimitExceeded.PhoneNumberThirtySecondLimit\"}";
        String valid = "{\"Response\":{\"RequestId\":\"owned-request\",\"SendStatusSet\":[" + status + "]}}";
        for (String response : List.of(
            valid.replace("\"RequestId\":\"owned-request\",", ""),
            valid.replace("+8613800000000", "+8613900000000"),
            valid.replace("[" + status + "]", "[" + status + "," + status + "]"),
            valid.replace("\"RequestId\"", "\"Error\":{\"Code\":\"LimitExceeded\"},\"RequestId\""),
            valid.replace("\"SerialNo\":\"\"", "\"SerialNo\":\"accepted-id\""),
            valid.replace("\"SerialNo\":\"\"", "\"SerialNo\":\" \""),
            valid.replace("\"Fee\":0", "\"Fee\":0.5"),
            valid.replace("\"Fee\":0", "\"Fee\":4294967296"),
            valid.replace("\"Code\":\"LimitExceeded.PhoneNumberThirtySecondLimit\"",
                "\"Code\":\"InternalError.Timeout\""))) {
            withBlend("tencent", response, false, (blend, key) -> assertEquals(NotifyDeliveryStatus.FAILED,
                new Sms4jNotificationProviderResolver(selected -> selected == blend)
                    .resolve(key).sender().send("13800000000", template(Map.of("1", "123456"))).outcome()));
        }
    }

    private NotifyTemplateContent template(Map<String, String> params) {
        return new NotifyTemplateContent("owned", "owned-template", params, "");
    }

    private void withBlend(String supplier, String payload, boolean success, Scenario scenario) throws Exception {
        String key = "owned-t23-" + UUID.randomUUID();
        SmsBlend blend = mock(SmsBlend.class);
        when(blend.getConfigId()).thenReturn(key);
        when(blend.getSupplier()).thenReturn(supplier);
        SmsResponse response = new SmsResponse();
        response.setSuccess(success);
        response.setConfigId(key);
        response.setData(JSONUtil.parseObj(payload));
        when(blend.sendMessage(anyString(), anyString(), any(LinkedHashMap.class))).thenReturn(response);
        SmsFactory.register(blend);
        try { scenario.run(blend, key); }
        finally { SmsFactory.unregister(key); }
    }

    @FunctionalInterface
    interface Scenario { void run(SmsBlend blend, String key) throws Exception; }
}
