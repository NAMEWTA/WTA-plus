package org.namewta.system.sso;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.domain.bo.SysClientBo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
@Tag("dev")
class SsoAppRegistrationTest {

    @Test
    void prepareForcesSsoEnabledAndRejectsWildcard() {
        SysClientBo bo = new SysClientBo();
        bo.setSsoEnabled(false);
        bo.setSsoRedirectUris("*");
        ServiceException exception = assertThrows(ServiceException.class, () -> SsoAppRegistration.prepare(bo));
        assertTrue(exception.getMessage().contains("通配符"));
        assertTrue(Boolean.TRUE.equals(bo.getSsoEnabled()));
    }

    @Test
    void prepareAcceptsOwnAppExactCallback() {
        SysClientBo bo = new SysClientBo();
        bo.setClientKey("admin-app");
        bo.setSsoRedirectUris("http://127.0.0.1:4174/sso/callback");
        SsoAppRegistration.prepare(bo);
        assertTrue(Boolean.TRUE.equals(bo.getSsoEnabled()));
        assertEquals("both", bo.getSsoAuthMode());
        assertEquals("public", bo.getSsoClientKind());
        assertTrue(Boolean.TRUE.equals(bo.getSsoPkceRequired()));
    }

    @Test
    void prepareAcceptsExternalConfidentialRegistration() {
        SysClientBo bo = new SysClientBo();
        bo.setClientKey("partner-app");
        bo.setSsoClientKind("confidential");
        bo.setSsoRedirectUriList(List.of("https://partner.example.com/oauth/callback"));
        SsoAppRegistration.prepare(bo);
        assertEquals("confidential", bo.getSsoClientKind());
        assertTrue(Boolean.TRUE.equals(bo.getSsoEnabled()));
    }

    @Test
    void prepareRejectsEmptyRedirects() {
        SysClientBo bo = new SysClientBo();
        bo.setClientKey("blank");
        ServiceException exception = assertThrows(ServiceException.class, () -> SsoAppRegistration.prepare(bo));
        assertTrue(exception.getMessage().contains("回调"));
    }
}
