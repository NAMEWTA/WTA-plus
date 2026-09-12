package org.namewta.test.system.service;

import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.vo.SysRoleVo;
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.service.impl.SysClientDefaultRoleResolverServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class SysClientDefaultRoleResolverServiceImplUnitTest {

    private final SysClientMapper clientMapper = mock(SysClientMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysClientDefaultRoleResolverServiceImpl service =
        new SysClientDefaultRoleResolverServiceImpl(clientMapper, roleMapper);

    @Test
    void resolveRoleIdReturnsNullWithoutClientId() {
        assertNull(service.resolveRoleId(null));

        verifyNoInteractions(clientMapper, roleMapper);
    }

    @Test
    void resolveRoleIdRejectsMissingOrDisabledClient() {
        ServiceException missing = assertThrows(ServiceException.class, () -> service.resolveRoleId(10L));

        SysClient disabledClient = client(11L, 101L, SystemConstants.DISABLE);
        when(clientMapper.selectById(11L)).thenReturn(disabledClient);
        ServiceException disabled = assertThrows(ServiceException.class, () -> service.resolveRoleId(11L));

        assertEquals("客户端不存在或已停用", missing.getMessage());
        assertEquals("客户端不存在或已停用", disabled.getMessage());
        verifyNoInteractions(roleMapper);
    }

    @Test
    void resolveRoleRejectsMissingOrDisabledClient() {
        ServiceException missing = assertThrows(ServiceException.class, () -> service.resolveRole(null));
        ServiceException disabled = assertThrows(ServiceException.class,
            () -> service.resolveRole(client(10L, 100L, SystemConstants.DISABLE)));

        assertEquals("客户端不存在或已停用", missing.getMessage());
        assertEquals("客户端不存在或已停用", disabled.getMessage());
        verifyNoInteractions(clientMapper, roleMapper);
    }

    @Test
    void resolveRoleReturnsNullWithoutConfiguredDefaultRole() {
        SysClient client = client(10L, null, SystemConstants.NORMAL);

        assertNull(service.resolveRole(client));

        verify(roleMapper, never()).selectVoById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resolveRoleRejectsMissingDisabledOrCrossClientRole() {
        SysClient client = client(10L, 100L, SystemConstants.NORMAL);
        ServiceException missing = assertThrows(ServiceException.class, () -> service.resolveRole(client));

        SysRoleVo disabledRole = role(100L, 10L, SystemConstants.DISABLE);
        when(roleMapper.selectVoById(100L)).thenReturn(disabledRole);
        ServiceException disabled = assertThrows(ServiceException.class, () -> service.resolveRole(client));

        SysRoleVo crossClientRole = role(100L, 20L, SystemConstants.NORMAL);
        when(roleMapper.selectVoById(100L)).thenReturn(crossClientRole);
        ServiceException crossClient = assertThrows(ServiceException.class, () -> service.resolveRole(client));

        SysClient missingClientId = client(null, 100L, SystemConstants.NORMAL);
        ServiceException missingClient = assertThrows(ServiceException.class,
            () -> service.resolveRole(missingClientId));

        assertEquals("客户端默认角色配置无效", missing.getMessage());
        assertEquals("客户端默认角色配置无效", disabled.getMessage());
        assertEquals("客户端默认角色配置无效", crossClient.getMessage());
        assertEquals("客户端默认角色配置无效", missingClient.getMessage());
    }

    @Test
    void resolveRoleAndRoleIdReturnConfiguredRole() {
        SysClient client = client(10L, 100L, SystemConstants.NORMAL);
        SysRoleVo role = role(100L, 10L, SystemConstants.NORMAL);
        when(clientMapper.selectById(10L)).thenReturn(client);
        when(roleMapper.selectVoById(100L)).thenReturn(role);

        assertSame(role, service.resolveRole(client));
        assertEquals(100L, service.resolveRoleId(10L));
    }

    private SysClient client(Long clientId, Long defaultRoleId, String status) {
        SysClient client = new SysClient();
        client.setId(clientId);
        client.setDefaultRoleId(defaultRoleId);
        client.setStatus(status);
        return client;
    }

    private SysRoleVo role(Long roleId, Long clientId, String status) {
        SysRoleVo role = new SysRoleVo();
        role.setRoleId(roleId);
        role.setClientId(clientId);
        role.setStatus(status);
        return role;
    }
}
