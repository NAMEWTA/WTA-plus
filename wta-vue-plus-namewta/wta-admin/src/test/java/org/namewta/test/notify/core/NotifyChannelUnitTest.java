package org.namewta.test.notify.core;

import org.namewta.common.notify.model.NotifyChannel;
import org.namewta.common.notify.model.NotifyResult;
import org.namewta.common.notify.model.NotifyStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通知渠道值对象测试。
 */
@Tag("dev")
class NotifyChannelUnitTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void shouldNormalizeStandardAndCustomChannels() {
        NotifyChannel custom = NotifyChannel.of(" FeiShu_Bot ");

        assertAll(
            () -> assertEquals(NotifyChannel.SMS, NotifyChannel.of(" SMS ")),
            () -> assertEquals(NotifyChannel.MAIL, NotifyChannel.of("mail")),
            () -> assertEquals("feishu_bot", custom.value()),
            () -> assertEquals("feishu_bot", custom.toString()),
            () -> assertEquals("value", Map.of(custom, "value").get(NotifyChannel.of("FEISHU_BOT")))
        );
    }

    @Test
    void shouldRejectInvalidChannelNames() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> NotifyChannel.of(null)),
            () -> assertThrows(IllegalArgumentException.class, () -> NotifyChannel.of("  ")),
            () -> assertThrows(IllegalArgumentException.class, () -> NotifyChannel.of("1sms")),
            () -> assertThrows(IllegalArgumentException.class, () -> NotifyChannel.of("sms/mail")),
            () -> assertThrows(IllegalArgumentException.class, () -> NotifyChannel.of("a".repeat(33))),
            () -> assertDoesNotThrow(() -> NotifyChannel.of("a".repeat(32)))
        );
    }

    @Test
    void shouldSerializeAsScalarJson() {
        String json = JSON.writeValueAsString(NotifyChannel.SMS);
        NotifyChannel restored = JSON.readValue("\" MAIL \"", NotifyChannel.class);
        NotifyResult result = new NotifyResult("request-1", NotifyChannel.SMS, "provider-a",
            NotifyStatus.ACCEPTED, java.util.List.of());
        String resultJson = JSON.writeValueAsString(result);
        NotifyResult restoredResult = JSON.readValue(resultJson, NotifyResult.class);

        assertAll(
            () -> assertEquals("\"sms\"", json),
            () -> assertEquals(NotifyChannel.MAIL, restored),
            () -> assertTrue(resultJson.contains("\"channel\":\"sms\"")),
            () -> assertEquals(result, restoredResult)
        );
    }
}
