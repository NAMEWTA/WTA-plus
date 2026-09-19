package org.namewta.common.json.utils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class LogSanitizerTest {
    private static final String CANARY = "credential-canary-json";

    @Test
    void redactsNestedAliasesArraysAndCustomExclusions() {
        String raw = "{\"code\":\"business-code\",\"items\":[{\"ACCESS_TOKEN\":\"" + CANARY
            + "\",\"old-password\":\"" + CANARY + "\",\"privateNote\":\"" + CANARY + "\",\"visible\":12}]}";
        String result = LogSanitizer.json(raw, "/business", "privateNote");
        assertFalse(result.contains(CANARY));
        assertTrue(result.contains("business-code"));
        assertTrue(result.contains("12"));
    }

    @Test
    void preservesTreeAndMapInputs() {
        JsonNode input = JsonUtils.getJsonMapper().readTree("{\"password\":\"" + CANARY + "\"}");
        assertFalse(LogSanitizer.object(input, "/normal").contains(CANARY));
        assertEquals(CANARY, input.get("password").asText());
        Map<String, String> map = Map.of("password", CANARY);
        assertFalse(LogSanitizer.object(map, null).contains(CANARY));
        assertEquals(CANARY, map.get("password"));
    }

    @Test
    void protectsOAuthFieldsOnlyInOAuthContext() {
        String raw = "{\"code\":\"" + CANARY + "\",\"code_verifier\":\"" + CANARY
            + "\",\"redirectUri\":\"https://example.invalid?code=" + CANARY + "\"}";
        assertFalse(LogSanitizer.json(raw, "/sso/oauth2/token").contains(CANARY));
        assertTrue(LogSanitizer.json(raw, "/business").contains(CANARY));
        assertTrue(LogSanitizer.omitResponseBody("/sso/oauth2/token;session=ignored/"));
    }

    @Test
    void failsClosedOnMalformedTrailingOrUnstructuredJson() {
        for (String raw : new String[]{"{\"password\":\"" + CANARY, "{} " + CANARY, "\"" + CANARY + "\"", CANARY}) {
            assertEquals(LogSanitizer.REDACTED, LogSanitizer.json(raw, null));
        }
    }

    @Test
    void protectsNestedFormNamesAndCredentialHeaders() {
        for (String name : new String[]{"user[password]", "items[0].ACCESS_TOKEN", "new_password", "client-secret"}) {
            assertTrue(LogSanitizer.isSensitiveName(name, "/normal"), name);
        }
        assertTrue(LogSanitizer.isSensitiveName("code", "/sso/oauth2/token"));
        assertFalse(LogSanitizer.isSensitiveName("code", "/business"));
        for (String name : new String[]{"Authorization", "X-Notify-Signature", "Location", "Referer"}) {
            assertTrue(LogSanitizer.isSensitiveHeader(name), name);
        }
    }

    @Test
    void failureSummaryContainsNoMessageOrCause() {
        Exception failure = new IllegalStateException(CANARY, new RuntimeException(CANARY));
        assertEquals(IllegalStateException.class.getName(), LogSanitizer.failure(failure));
    }

    @Test
    void errorTextCannotEchoCredentialsInOtherwiseValidJson() {
        assertFalse(LogSanitizer.json("{\"code\":500,\"msg\":\"" + CANARY + "\",\"error\":{\"value\":\"" + CANARY + "\"}}", "/normal").contains(CANARY));
    }

    @Test
    void notificationCallbacksKeepOnlyMetadataWithoutChangingBusinessValues() {
        Map<String, String> body = Map.of("target", "owned-recipient@example.test", "eventId", CANARY);
        for (String path : new String[]{"/notify/callback/MAIL", "/notify/callback/SMS;ignored=x/"}) {
            assertEquals(LogSanitizer.REDACTED, LogSanitizer.object(body, path));
            assertEquals(LogSanitizer.REDACTED, LogSanitizer.json(JsonUtils.toJsonString(body), path));
            assertTrue(LogSanitizer.isSensitiveName("target", path));
            assertTrue(LogSanitizer.isSensitiveName("unknown", path));
        }
        assertEquals("owned-recipient@example.test", body.get("target"));
        assertTrue(LogSanitizer.object(body, "/notify/callback-settings").contains(CANARY));
    }
}
