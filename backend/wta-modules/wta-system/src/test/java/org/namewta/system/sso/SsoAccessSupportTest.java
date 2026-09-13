package org.namewta.system.sso;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.domain.SysClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
@Tag("dev")
class SsoAccessSupportTest {

    @Test
    void unregisteredClientCannotBind() {
        SysClient client = new SysClient();
        ServiceException exception = assertThrows(ServiceException.class, () -> SsoAccessSupport.requireRegistered(client));
        assertTrue(exception.getMessage().contains("SSO 管理"));
    }

    @Test
    void registeredClientWithBothModeIsBound() {
        assertTrue(SsoAccessSupport.isBound(true, "both", "http://127.0.0.1:4174/sso/callback"));
        assertFalse(SsoAccessSupport.isBound(false, "both", "http://127.0.0.1:4174/sso/callback"));
        assertFalse(SsoAccessSupport.isBound(true, "local", "http://127.0.0.1:4174/sso/callback"));
        assertFalse(SsoAccessSupport.isBound(true, "both", " "));
    }

    @Test
    void bindModeRejectsLocal() {
        assertEquals("both", SsoAccessSupport.requireBindMode(null));
        ServiceException exception = assertThrows(ServiceException.class, () -> SsoAccessSupport.requireBindMode("local"));
        assertTrue(exception.getMessage().contains("sso 或 both"));
    }
}
