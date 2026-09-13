package org.namewta.system.sso;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.bo.SysClientBo;
import org.namewta.system.domain.vo.SysClientVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
@Tag("dev")
class SsoClientFieldsSupportTest {

    @Test
    void enabledClientRejectsWildcardRedirect() {
        SysClientBo bo = baseBo();
        bo.setSsoEnabled(true);
        bo.setSsoRedirectUris("*");
        ServiceException exception = assertThrows(ServiceException.class,
            () -> SsoClientFieldsSupport.apply(bo, new SysClient(), null));
        assertTrue(exception.getMessage().contains("通配符"));
    }

    @Test
    void confidentialSecretIsHashedAndPlaintextIsReturnedOnce() {
        SysClientBo bo = baseBo();
        bo.setSsoEnabled(true);
        bo.setSsoClientKind("confidential");
        bo.setSsoRedirectUriList(List.of("http://127.0.0.1:9000/callback"));
        bo.setSsoSecret("once-only-secret");
        SysClient entity = new SysClient();
        String issued = SsoClientFieldsSupport.apply(bo, entity, null);
        assertEquals("once-only-secret", issued);
        assertNotEquals("once-only-secret", entity.getSsoSecretHash());
        assertTrue(SsoSecretHasher.matches("once-only-secret", entity.getSsoSecretHash()));
        SysClientVo vo = new SysClientVo();
        vo.setSsoSecretHash(entity.getSsoSecretHash());
        vo.setSsoRedirectUris(entity.getSsoRedirectUris());
        SsoClientFieldsSupport.fillView(vo, true);
        assertNull(vo.getSsoSecretHash());
        assertTrue(vo.getSsoSecretConfigured());
        assertFalse(Boolean.TRUE.equals(vo.getSsoEnabled()) && "once-only-secret".equals(vo.getSsoSecretOnce()));
    }

    @Test
    void confidentialWithoutSecretGetsGeneratedHash() {
        SysClientBo bo = baseBo();
        bo.setSsoEnabled(true);
        bo.setSsoClientKind("confidential");
        bo.setSsoRedirectUriList(List.of("https://partner.example.com/oauth/callback"));
        SysClient entity = new SysClient();
        String issued = SsoClientFieldsSupport.apply(bo, entity, null);
        assertNotNull(issued);
        assertTrue(SsoSecretHasher.matches(issued, entity.getSsoSecretHash()));
    }

    private static SysClientBo baseBo() {
        SysClientBo bo = new SysClientBo();
        bo.setClientKey("ext");
        bo.setSsoAuthMode("both");
        return bo;
    }
}
