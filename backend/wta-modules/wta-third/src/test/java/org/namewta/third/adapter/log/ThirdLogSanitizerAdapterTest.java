package org.namewta.third.adapter.log;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.json.utils.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("local")
class ThirdLogSanitizerAdapterTest {
    @Test
    void redactsCoreAndEndpointSensitiveFields() {
        String result = ThirdLogSanitizerAdapter.json(JsonUtils.getJsonMapper().readTree("{\"name\":\"ok\",\"apiKey\":\"hidden\",\"phone\":\"13800000000\"}"), Set.of("phone"));
        assertTrue(result.contains("\"apiKey\":\"***\""));
        assertTrue(result.contains("\"phone\":\"***\""));
        assertTrue(!result.contains("hidden"));
    }

    @Test
    void truncationIsBoundedByUtf8Bytes() {
        String result = ThirdLogSanitizerAdapter.text("中".repeat(20_000));
        assertTrue(result.getBytes(StandardCharsets.UTF_8).length <= 16 * 1024);
        assertTrue(result.endsWith("...[truncated]"));
    }

    @Test
    void queryValuesAreSanitizedByName() {
        Map<String, Object> values = ThirdLogSanitizerAdapter.values(Map.of("token", "hidden", "page", 1));
        assertEquals("***", values.get("token"));
        assertEquals("1", values.get("page"));
    }

    @Test
    void persistenceValueIsAlwaysValidBoundedJson() {
        String value = ThirdLogSanitizerAdapter.jsonValue("plain text", Set.of());
        assertEquals("\"plain text\"", value);
        assertTrue(value.getBytes(StandardCharsets.UTF_8).length <= 16 * 1024);
    }
}
