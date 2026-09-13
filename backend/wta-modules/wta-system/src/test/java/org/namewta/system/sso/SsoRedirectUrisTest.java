package org.namewta.system.sso;

import org.namewta.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
@Tag("dev")
class SsoRedirectUrisTest {

    @Test
    void rejectsWildcardAndUnlistedRedirects() {
        ServiceException wildcard = assertThrows(ServiceException.class,
            () -> SsoRedirectUris.validateExact(List.of("*")));
        assertTrue(wildcard.getMessage().contains("通配符"));
        ServiceException nested = assertThrows(ServiceException.class,
            () -> SsoRedirectUris.validateOne("https://app.example.com/callback/*"));
        assertTrue(nested.getMessage().contains("通配符"));
        List<String> allowed = List.of("http://127.0.0.1:4174/sso/callback");
        assertTrue(SsoRedirectUris.matches(allowed, "http://127.0.0.1:4174/sso/callback"));
        assertFalse(SsoRedirectUris.matches(allowed, "http://127.0.0.1:4174/other"));
    }

    @Test
    void parsesAndJoinsExactUris() {
        List<String> parsed = SsoRedirectUris.parse("http://127.0.0.1:4174/sso/callback\nhttp://127.0.0.1:4175/sso/callback", null);
        assertEquals(2, parsed.size());
        assertEquals("http://127.0.0.1:4174/sso/callback,http://127.0.0.1:4175/sso/callback", SsoRedirectUris.join(parsed));
    }
}
