package org.namewta.test.openapi.invalidation;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.namewta.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.SysRoleMenu;
import org.namewta.system.domain.SysUserRole;
import org.namewta.system.domain.SysUserTypeRel;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeRelVo;
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysRoleMenuMapper;
import org.namewta.system.mapper.SysUserRoleMapper;
import org.namewta.system.mapper.SysUserTypeRelMapper;
import org.namewta.system.service.impl.SysMenuServiceImpl;
import org.namewta.system.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class RbacOpenApiSessionInvalidationTest {

    @Test
    @SuppressWarnings("unchecked")
    void roleChangesIncludeExplicitAndDefaultRoleUsersWithoutDuplicates() {
        SysUserRoleMapper userRoles = mock(SysUserRoleMapper.class);
        LambdaCrudChainWrapper<SysUserRole, SysUserRole> userRoleQuery =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(userRoles.lambda()).thenReturn(userRoleQuery);
        when(userRoleQuery.in(any(SFunction.class), any(Collection.class))).thenReturn(userRoleQuery);
        when(userRoleQuery.list()).thenReturn(List.of(userRole(41L, 7L), userRole(42L, 7L)));

        SysClientMapper clients = mock(SysClientMapper.class);
        LambdaCrudChainWrapper<SysClient, SysClientVo> clientQuery =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(clients.lambda()).thenReturn(clientQuery);
        when(clientQuery.in(any(SFunction.class), any(Collection.class))).thenReturn(clientQuery);
        when(clientQuery.eq(any(SFunction.class), any())).thenReturn(clientQuery);
        when(clientQuery.list()).thenReturn(List.of(client(7L, 11L)));

        SysUserTypeRelMapper userTypes = mock(SysUserTypeRelMapper.class);
        LambdaCrudChainWrapper<SysUserTypeRel, SysUserTypeRelVo> userTypeQuery =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(userTypes.lambda()).thenReturn(userTypeQuery);
        when(userTypeQuery.in(any(SFunction.class), any(Collection.class))).thenReturn(userTypeQuery);
        when(userTypeQuery.eq(any(SFunction.class), any())).thenReturn(userTypeQuery);
        when(userTypeQuery.list()).thenReturn(List.of(userType(42L, 11L), userType(43L, 11L)));

        SysRoleServiceImpl service = service(SysRoleServiceImpl.class);
        ReflectionTestUtils.setField(service, "userRoleMapper", userRoles);
        ReflectionTestUtils.setField(service, "clientMapper", clients);
        ReflectionTestUtils.setField(service, "userTypeRelMapper", userTypes);

        Set<Long> affected = ReflectionTestUtils.invokeMethod(service, "findAffectedUserIdsByRoleIds", Set.of(7L));

        assertThat(affected).containsExactly(41L, 42L, 43L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void menuChangesResolveTheirRolesBeforeRelationsCanBeDeleted() {
        SysRoleMenuMapper roleMenus = mock(SysRoleMenuMapper.class);
        LambdaCrudChainWrapper<SysRoleMenu, SysRoleMenu> roleMenuQuery =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(roleMenus.lambda()).thenReturn(roleMenuQuery);
        when(roleMenuQuery.in(any(SFunction.class), any(Collection.class))).thenReturn(roleMenuQuery);
        when(roleMenuQuery.list()).thenReturn(List.of(roleMenu(7L, 70L), roleMenu(8L, 70L)));

        SysUserRoleMapper userRoles = mock(SysUserRoleMapper.class);
        LambdaCrudChainWrapper<SysUserRole, SysUserRole> userRoleQuery =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(userRoles.lambda()).thenReturn(userRoleQuery);
        when(userRoleQuery.in(any(SFunction.class), any(Collection.class))).thenReturn(userRoleQuery);
        when(userRoleQuery.list()).thenReturn(List.of(userRole(41L, 7L), userRole(42L, 8L)));

        SysClientMapper clients = mock(SysClientMapper.class);
        LambdaCrudChainWrapper<SysClient, SysClientVo> clientQuery =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(clients.lambda()).thenReturn(clientQuery);
        when(clientQuery.in(any(SFunction.class), any(Collection.class))).thenReturn(clientQuery);
        when(clientQuery.eq(any(SFunction.class), any())).thenReturn(clientQuery);
        when(clientQuery.list()).thenReturn(List.of());

        SysMenuServiceImpl service = service(SysMenuServiceImpl.class);
        ReflectionTestUtils.setField(service, "roleMenuMapper", roleMenus);
        ReflectionTestUtils.setField(service, "userRoleMapper", userRoles);
        ReflectionTestUtils.setField(service, "clientMapper", clients);

        Set<Long> affected = ReflectionTestUtils.invokeMethod(service, "findAffectedUserIdsByMenuIds", Set.of(70L));

        assertThat(affected).containsExactly(41L, 42L);
    }

    @Test
    void invalidationIsDeduplicatedAndFailuresAreNeverSwallowed() {
        OpenApiMachineSessionInvalidator invalidator = mock(OpenApiMachineSessionInvalidator.class);
        IllegalStateException failure = new IllegalStateException("redis down");
        when(invalidator.invalidateByUserId(42L)).thenThrow(failure);
        SysRoleServiceImpl service = service(SysRoleServiceImpl.class);
        ReflectionTestUtils.setField(service, "openApiSessionInvalidator", invalidator);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
            service, "invalidateUsers", List.of(41L, 41L, 42L))).isSameAs(failure);

        verify(invalidator, times(1)).invalidateByUserId(41L);
        verify(invalidator, times(1)).invalidateByUserId(42L);
    }

    @Test
    void everyCompositeRbacWriteUsesTheDynamicDatasourceTransactionBoundary() throws Exception {
        assertTransactional(SysRoleServiceImpl.class, "insertRole", org.namewta.system.domain.bo.SysRoleBo.class);
        assertTransactional(SysRoleServiceImpl.class, "updateRoleBaseInfo", org.namewta.system.domain.bo.SysRoleBo.class);
        assertTransactional(SysRoleServiceImpl.class, "updateRolePermission", org.namewta.system.domain.bo.SysRoleBo.class);
        assertTransactional(SysRoleServiceImpl.class, "updateRoleStatus", Long.class, String.class);
        assertTransactional(SysRoleServiceImpl.class, "deleteRoleById", Long.class);
        assertTransactional(SysRoleServiceImpl.class, "deleteRoleByIds", Collection.class);
        assertTransactional(SysRoleServiceImpl.class, "deleteAuthUser", SysUserRole.class);
        assertTransactional(SysRoleServiceImpl.class, "deleteAuthUsers", Long.class, Collection.class);
        assertTransactional(SysRoleServiceImpl.class, "insertAuthUsers", Long.class, Collection.class);
        assertTransactional(SysMenuServiceImpl.class, "updateMenu", org.namewta.system.domain.bo.SysMenuBo.class);
        assertTransactional(SysMenuServiceImpl.class, "deleteMenuById", Long.class);
        assertTransactional(SysMenuServiceImpl.class, "deleteMenuById", Collection.class);
    }

    @Test
    void callSiteMatrixKeepsMachineInvalidationInServicesAndBrowserKickoutIntact() throws IOException {
        String role = source("SysRoleServiceImpl.java");
        String menu = source("SysMenuServiceImpl.java");
        String controller = controllerSource();

        assertThat(role).contains("findAffectedUserIdsByRoleIds")
            .contains("openApiSessionInvalidator.invalidateByUserId")
            .contains("clientSessionService.kickoutUserClient");
        assertThat(menu).contains("findAffectedUserIdsByMenuIds")
            .contains("openApiSessionInvalidator::invalidateByUserId")
            .contains("clientSessionService.kickoutClient");
        assertThat(controller).contains("kickRoleClient(role.getRoleId())")
            .doesNotContain("OpenApiMachineSessionInvalidator");
    }

    private static void assertTransactional(Class<?> type, String methodName, Class<?>... parameters) throws Exception {
        Method method = type.getMethod(methodName, parameters);
        assertThat(method.getAnnotation(DSTransactional.class))
            .as(type.getSimpleName() + "." + methodName)
            .isNotNull();
    }

    private static String source(String fileName) throws IOException {
        Path repository = Path.of(System.getProperty("user.dir")).getParent();
        return Files.readString(repository.resolve("wta-modules/wta-system/src/main/java/org/dromara/system/service/impl")
            .resolve(fileName));
    }

    private static String controllerSource() throws IOException {
        Path repository = Path.of(System.getProperty("user.dir")).getParent();
        return Files.readString(repository.resolve(
            "wta-modules/wta-system/src/main/java/org/dromara/system/controller/system/SysRoleController.java"));
    }

    private static <T> T service(Class<T> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructors()[0];
            Object[] arguments = Arrays.stream(constructor.getParameterTypes()).map(org.mockito.Mockito::mock).toArray();
            return type.cast(constructor.newInstance(arguments));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static SysUserRole userRole(Long userId, Long roleId) {
        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        return relation;
    }

    private static SysClient client(Long defaultRoleId, Long userTypeId) {
        SysClient client = new SysClient();
        client.setDefaultRoleId(defaultRoleId);
        client.setUserTypeId(userTypeId);
        return client;
    }

    private static SysUserTypeRel userType(Long userId, Long userTypeId) {
        SysUserTypeRel relation = new SysUserTypeRel();
        relation.setUserId(userId);
        relation.setUserTypeId(userTypeId);
        return relation;
    }

    private static SysRoleMenu roleMenu(Long roleId, Long menuId) {
        SysRoleMenu relation = new SysRoleMenu();
        relation.setRoleId(roleId);
        relation.setMenuId(menuId);
        return relation;
    }

}
