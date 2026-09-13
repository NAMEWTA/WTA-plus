package org.namewta.web.service;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.sso.ClientAccessPaths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
@Tag("dev")
class HomeClientLoginAccessPathTest {

    @Test
    void passwordLoginExtrasAllowHomeIdentityApis() {
        SysClientVo home = new SysClientVo();
        home.setClientKey("home");
        home.setClientId("428a8310cd442757ae699df5d894f051");
        home.setAccessPath("/home/**");
        home.setTimeout(604800L);
        home.setActiveTimeout(1800L);
        home.setDeviceType("pc");
        SaLoginParameter parameter = IAuthStrategy.buildLoginParameter(home);
        Object extra = parameter.getExtraData() == null
            ? null
            : parameter.getExtraData().get(LoginHelper.CLIENT_ACCESS_PATH_KEY);
        String accessPath = extra == null ? null : String.valueOf(extra);
        assertTrue(ClientAccessPaths.allows(accessPath, "/system/user/getInfo"));
        assertTrue(ClientAccessPaths.allows(accessPath, "/system/menu/getRouters"));
        assertFalse(ClientAccessPaths.allows(accessPath, "/system/client/list"));
    }
}
