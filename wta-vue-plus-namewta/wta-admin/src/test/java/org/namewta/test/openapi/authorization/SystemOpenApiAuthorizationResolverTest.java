package org.namewta.test.openapi.authorization;

import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.mapper.SysOpenApiAuthorizationMapper;
import org.namewta.system.openapi.authorization.OpenApiAuthorizationPermission;
import org.namewta.system.openapi.authorization.OpenApiAuthorizationRole;
import org.namewta.system.openapi.authorization.OpenApiAuthorizationUser;
import org.namewta.system.openapi.authorization.SystemOpenApiAuthorizationResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class SystemOpenApiAuthorizationResolverTest {

    @Test
    void unionsDefaultAndExplicitRolesWithoutClientFallback() {
        SysOpenApiAuthorizationMapper mapper = mock(SysOpenApiAuthorizationMapper.class);
        OpenApiAuthorizationUser user = user(9L);
        when(mapper.selectActiveUser(9L)).thenReturn(user);
        when(mapper.countLegalClients(9L)).thenReturn(2L);
        when(mapper.selectActiveRoles(9L)).thenReturn(List.of(
            role(10L, "default", "1"), role(20L, "operator", "3"), role(20L, "operator", "3")));
        when(mapper.selectActivePermissions(9L)).thenReturn(List.of(
            permission(10L, "system:config:list"),
            permission(20L, "system:config:list"),
            permission(20L, "system:user:list")));

        var snapshot = new SystemOpenApiAuthorizationResolver(mapper).resolve(9L);

        assertThat(snapshot.getClientPk()).isNull();
        assertThat(snapshot.getClientKey()).isNull();
        assertThat(snapshot.getUserType()).isEqualTo("openapi");
        assertThat(snapshot.getRolePermission()).containsExactly("default", "operator");
        assertThat(snapshot.getMenuPermission()).containsExactly("system:config:list", "system:user:list");
        assertThat(snapshot.getRoles()).extracting(role -> role.getRoleId()).containsExactly(10L, 20L);
        assertThat(snapshot.getDataScopeRoleMap().get("system:config:list")).containsExactly(10L, 20L);
        verify(mapper).countLegalClients(9L);
    }

    @Test
    void failsClosedWhenUserOrLegalClientIsMissing() {
        SysOpenApiAuthorizationMapper mapper = mock(SysOpenApiAuthorizationMapper.class);
        when(mapper.selectActiveUser(8L)).thenReturn(null);
        when(mapper.selectActiveUser(9L)).thenReturn(user(9L));
        when(mapper.countLegalClients(9L)).thenReturn(0L);

        assertThatThrownBy(() -> new SystemOpenApiAuthorizationResolver(mapper).resolve(8L))
            .isInstanceOf(ServiceException.class)
            .hasMessage("OpenAPI authorization snapshot is unavailable");
        assertThatThrownBy(() -> new SystemOpenApiAuthorizationResolver(mapper).resolve(9L))
            .isInstanceOf(ServiceException.class)
            .hasMessage("OpenAPI authorization snapshot is unavailable");
    }

    @Test
    void preservesSuperAdminSemantics() {
        long userId = SystemConstants.SUPER_ADMIN_USER_ID;
        SysOpenApiAuthorizationMapper mapper = mock(SysOpenApiAuthorizationMapper.class);
        when(mapper.selectActiveUser(userId)).thenReturn(user(userId));
        when(mapper.countLegalClients(userId)).thenReturn(1L);
        when(mapper.selectActiveRoles(userId)).thenReturn(List.of());
        when(mapper.selectActivePermissions(userId)).thenReturn(List.of());

        var snapshot = new SystemOpenApiAuthorizationResolver(mapper).resolve(userId);

        assertThat(snapshot.getRolePermission()).containsExactly(SystemConstants.SUPER_ADMIN_ROLE_KEY);
        assertThat(snapshot.getMenuPermission()).containsExactly("*:*:*");
    }

    @Test
    void returnsDefensiveAuthorizationCollections() {
        SysOpenApiAuthorizationMapper mapper = mock(SysOpenApiAuthorizationMapper.class);
        when(mapper.selectActiveUser(9L)).thenReturn(user(9L));
        when(mapper.countLegalClients(9L)).thenReturn(1L);
        when(mapper.selectActiveRoles(9L)).thenReturn(List.of(role(10L, "operator", "3")));
        when(mapper.selectActivePermissions(9L)).thenReturn(List.of(permission(10L, "system:user:list")));

        var snapshot = new SystemOpenApiAuthorizationResolver(mapper).resolve(9L);

        assertThatThrownBy(() -> snapshot.getRolePermission().add("admin"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.getMenuPermission().add("*:*:*"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.getRoles().clear())
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.getDataScopeRoleMap().clear())
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.getDataScopeRoleMap().get("system:user:list").add(20L))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private static OpenApiAuthorizationUser user(Long userId) {
        OpenApiAuthorizationUser user = new OpenApiAuthorizationUser();
        user.setUserId(userId);
        user.setUserName("owner");
        user.setNickName("Owner");
        return user;
    }

    private static OpenApiAuthorizationRole role(Long roleId, String roleKey, String dataScope) {
        OpenApiAuthorizationRole role = new OpenApiAuthorizationRole();
        role.setRoleId(roleId);
        role.setRoleName(roleKey);
        role.setRoleKey(roleKey);
        role.setDataScope(dataScope);
        return role;
    }

    private static OpenApiAuthorizationPermission permission(Long roleId, String value) {
        OpenApiAuthorizationPermission permission = new OpenApiAuthorizationPermission();
        permission.setRoleId(roleId);
        permission.setPermission(value);
        return permission;
    }

}
