package org.namewta.system.service.impl;

import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.bo.SysClientBo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.mapper.SysUserTypeRelMapper;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysUserTypeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("local")
@Tag("dev")
class SysClientServiceSsoUnitTest {

    @Test
    void insertByBoRejectsWildcardRedirect() {
        Fixture fixture = new Fixture();
        SysClientBo bo = fixture.newClient();
        bo.setSsoEnabled(true);
        bo.setSsoRedirectUris("*");
        assertThrows(ServiceException.class, () -> fixture.service.insertByBo(bo));
    }

    @Test
    void insertByBoRejectsUnlistedRedirectBeforePersistence() {
        Fixture fixture = new Fixture();
        SysClientBo bo = fixture.newClient();
        bo.setSsoEnabled(true);
        bo.setSsoRedirectUris("not-a-uri");
        assertThrows(ServiceException.class, () -> fixture.service.insertByBo(bo));
    }

    @Test
    void bindSsoAccessRejectsUnregisteredClient() {
        Fixture fixture = new Fixture();
        SysClient db = new SysClient();
        db.setId(9L);
        db.setSsoRedirectUris(" ");
        when(fixture.clientMapper.selectById(9L)).thenReturn(db);
        ServiceException exception = assertThrows(ServiceException.class,
            () -> fixture.service.bindSsoAccess(9L, "both"));
        assertTrue(exception.getMessage().contains("SSO 管理"));
    }

    private static final class Fixture {
        final SysClientMapper clientMapper = mock(SysClientMapper.class);
        private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        private final ISysUserTypeService userTypeService = mock(ISysUserTypeService.class);
        private final ClientSessionService clientSessionService = mock(ClientSessionService.class);
        private final SysUserTypeRelMapper userTypeRelMapper = mock(SysUserTypeRelMapper.class);
        private final SysClientServiceImpl service = new SysClientServiceImpl(
            clientMapper, roleMapper, userTypeService, clientSessionService, userTypeRelMapper);

        private Fixture() {
            SysUserTypeVo userType = new SysUserTypeVo();
            userType.setUserTypeId(1L);
            userType.setStatus(SystemConstants.NORMAL);
            when(userTypeService.queryById(1L)).thenReturn(userType);
        }

        private SysClientBo newClient() {
            SysClientBo bo = new SysClientBo();
            bo.setClientKey("partner");
            bo.setClientSecret("partner-secret");
            bo.setGrantTypeList(List.of("password"));
            bo.setUserTypeId(1L);
            bo.setStatus(SystemConstants.NORMAL);
            return bo;
        }
    }
}
