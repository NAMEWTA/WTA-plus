package org.namewta.sso.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
@Tag("dev")
class SsoSessionCookieTest {

    @Test
    void isHttpOnlySsoTokenOnRootPath() {
        ResponseCookie cookie = SsoSessionCookie.create("Sso-Token", "session-1", Duration.ofHours(8));
        assertEquals("Sso-Token", cookie.getName());
        assertEquals("session-1", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals("Lax", cookie.getSameSite());
        assertTrue(cookie.toString().contains("HttpOnly"));
    }
}
