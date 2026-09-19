package org.namewta.sso.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class SsoBearerTokensTest {
    @Test
    void usesJdkCryptographicSourceAndEncodesExactly32BytesWithoutPadding() throws Exception {
        var source = SsoBearerTokens.class.getDeclaredField("RANDOM");
        source.setAccessible(true);
        assertInstanceOf(SecureRandom.class, source.get(null));
        String value = SsoBearerTokens.create();
        assertTrue(SsoBearerTokens.isValid(value));
        assertEquals(32, Base64.getUrlDecoder().decode(value).length);
        assertEquals(43, value.length());
        assertFalse(value.contains("="));
        assertFalse(SsoBearerTokens.isValid("1af730bb2e87131a1af730bb2e87131b"));
        assertFalse(SsoBearerTokens.isValid(value + "="));
        assertFalse(SsoBearerTokens.isValid(null));
    }
}
