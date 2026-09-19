package org.namewta.sso.adapter.gateway;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.sso.api.SsoClientView;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
@Tag("dev")
class SsoTokenExtrasTest {

    @Test
    void bindsBusinessClientAndRejectsSsoCenter() {
        SsoClientView admin = new SsoClientView();
        admin.setId(1L);
        admin.setClientId("e5cd7e4891bf95d1d19206ce24a7b32e");
        admin.setClientKey("pc");
        SaLoginParameter parameter = SsoTokenExtras.bind(admin);
        Object extra = parameter.getExtraData() == null
            ? null
            : parameter.getExtraData().get(LoginHelper.CLIENT_KEY);
        assertEquals("e5cd7e4891bf95d1d19206ce24a7b32e", extra);
        assertNotEquals("sso", extra);
        SsoClientView home = new SsoClientView();
        home.setId(2L);
        home.setClientId("428a8310cd442757ae699df5d894f051");
        home.setClientKey("home");
        home.setAccessPath("/home/**,/system/user/getInfo,/system/menu/getRouters,/auth/logout,/profile/**");
        Object homePath = SsoTokenExtras.bind(home).getExtraData() == null
            ? null
            : SsoTokenExtras.bind(home).getExtraData().get(LoginHelper.CLIENT_ACCESS_PATH_KEY);
        assertTrue(String.valueOf(homePath).contains("/system/user/getInfo"));
        assertTrue(String.valueOf(homePath).contains("/system/menu/getRouters"));

        SsoClientView sso = new SsoClientView();
        sso.setClientId("sso");
        sso.setClientKey("sso");
        assertThrows(ServiceException.class, () -> SsoTokenExtras.bind(sso));
    }
}
