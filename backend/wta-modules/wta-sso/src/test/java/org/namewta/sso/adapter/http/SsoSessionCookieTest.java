package org.namewta.sso.adapter.http;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
@Tag("dev")
class SsoSessionCookieTest {

    @Test
    void isHttpOnlySsoTokenOnRootPath() {
        ResponseCookie cookie = SsoSessionCookie.create("Sso-Token", "session-1", Duration.ofHours(8), true);
        assertEquals("Sso-Token", cookie.getName());
        assertEquals("session-1", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("/", cookie.getPath());
        assertEquals("Lax", cookie.getSameSite());
        assertTrue(cookie.toString().contains("HttpOnly"));
        assertFalse(cookie.toString().contains("Domain="));
    }

    @Test
    void logoutMatchesCreationScopeAndOnlyExplicitHttpModeClearsSecure() {
        for (boolean secure : new boolean[]{true, false}) {
            ResponseCookie login = SsoSessionCookie.create("Sso-Token", "session", Duration.ofHours(8), secure);
            ResponseCookie logout = SsoSessionCookie.create("Sso-Token", "", Duration.ZERO, secure);
            assertEquals(login.getName(), logout.getName());
            assertEquals(login.getPath(), logout.getPath());
            assertEquals(login.getDomain(), logout.getDomain());
            assertEquals(login.getSameSite(), logout.getSameSite());
            assertEquals(login.isHttpOnly(), logout.isHttpOnly());
            assertEquals(secure, login.isSecure());
            assertEquals(login.isSecure(), logout.isSecure());
            assertEquals(Duration.ZERO, logout.getMaxAge());
            assertEquals("", logout.getValue());
        }
    }
}
