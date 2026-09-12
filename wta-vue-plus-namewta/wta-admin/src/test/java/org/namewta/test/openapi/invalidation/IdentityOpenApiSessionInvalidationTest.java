package org.namewta.test.openapi.invalidation;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.namewta.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.system.domain.SysUserTypeRel;
import org.namewta.system.domain.vo.SysUserTypeRelVo;
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.mapper.SysUserTypeMapper;
import org.namewta.system.mapper.SysUserTypeRelMapper;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysUserTypeService;
import org.namewta.system.service.impl.SysClientServiceImpl;
import org.namewta.system.service.impl.SysUserServiceImpl;
import org.namewta.system.service.impl.SysUserTypeRelServiceImpl;
import org.namewta.system.service.impl.SysUserTypeServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class IdentityOpenApiSessionInvalidationTest {

    @Test
    @SuppressWarnings("unchecked")
    void relationGrantInvalidatesTheExactUserAfterTheAuthoritativeInsert() {
        SysUserTypeRelMapper relations = mock(SysUserTypeRelMapper.class);
        LambdaCrudChainWrapper<SysUserTypeRel, SysUserTypeRelVo> query =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(relations.lambda()).thenReturn(query);
        when(query.eq(any(SFunction.class), any())).thenReturn(query);
        when(query.exists()).thenReturn(false);
        when(relations.insert(any(SysUserTypeRel.class))).thenReturn(1);
        OpenApiMachineSessionInvalidator invalidator = mock(OpenApiMachineSessionInvalidator.class);
        SysUserTypeRelServiceImpl service = new SysUserTypeRelServiceImpl(
            relations, mock(SysUserTypeMapper.class), mock(ClientSessionService.class));
        ReflectionTestUtils.setField(service, "openApiSessionInvalidator", invalidator);

        assertThat(service.grantUserType(41L, 7L, "admin")).isTrue();

        InOrder order = inOrder(relations, invalidator);
        order.verify(relations).insert(any(SysUserTypeRel.class));
        order.verify(invalidator).invalidateByUserId(41L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void unchangedRelationDoesNotCreateAnInvalidationAndFailuresAreNotSwallowed() {
        SysUserTypeRelMapper relations = mock(SysUserTypeRelMapper.class);
        LambdaCrudChainWrapper<SysUserTypeRel, SysUserTypeRelVo> query =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(relations.lambda()).thenReturn(query);
        when(query.eq(any(SFunction.class), any())).thenReturn(query);
        when(query.exists()).thenReturn(true);
        OpenApiMachineSessionInvalidator invalidator = mock(OpenApiMachineSessionInvalidator.class);
        SysUserTypeRelServiceImpl service = new SysUserTypeRelServiceImpl(
            relations, mock(SysUserTypeMapper.class), mock(ClientSessionService.class));
        ReflectionTestUtils.setField(service, "openApiSessionInvalidator", invalidator);

        assertThat(service.grantUserType(41L, 7L, "admin")).isFalse();
        verifyNoInteractions(invalidator);

        when(query.exists()).thenReturn(false);
        when(relations.insert(any(SysUserTypeRel.class))).thenReturn(1);
        IllegalStateException failure = new IllegalStateException("redis down");
        when(invalidator.invalidateByUserId(41L)).thenThrow(failure);
        assertThatThrownBy(() -> service.grantUserType(41L, 7L, "admin")).isSameAs(failure);
    }

    @Test
    @SuppressWarnings("unchecked")
    void clientChangesResolveOnlyRelatedUsersAndDeduplicateThem() {
        SysUserTypeRelMapper relations = mock(SysUserTypeRelMapper.class);
        LambdaCrudChainWrapper<SysUserTypeRel, SysUserTypeRelVo> query =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(relations.lambda()).thenReturn(query);
        when(query.in(any(SFunction.class), any(Collection.class))).thenReturn(query);
        when(query.eq(any(SFunction.class), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(relation(41L, 7L), relation(41L, 8L), relation(42L, 8L)));
        OpenApiMachineSessionInvalidator invalidator = mock(OpenApiMachineSessionInvalidator.class);
        SysClientServiceImpl service = new SysClientServiceImpl(
            mock(SysClientMapper.class), mock(SysRoleMapper.class), mock(ISysUserTypeService.class),
            mock(ClientSessionService.class), relations);
        ReflectionTestUtils.setField(service, "openApiSessionInvalidator", invalidator);

        ReflectionTestUtils.invokeMethod(service, "invalidateUsersForUserTypes", List.of(7L, 8L));

        verify(invalidator, times(1)).invalidateByUserId(41L);
        verify(invalidator, times(1)).invalidateByUserId(42L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginDomainChangesResolveOnlyRelatedUsersAndDeduplicateThem() {
        SysUserTypeRelMapper relations = mock(SysUserTypeRelMapper.class);
        LambdaCrudChainWrapper<SysUserTypeRel, SysUserTypeRelVo> query =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(relations.lambda()).thenReturn(query);
        when(query.eq(any(SFunction.class), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(relation(41L, 7L), relation(41L, 7L), relation(42L, 7L)));
        OpenApiMachineSessionInvalidator invalidator = mock(OpenApiMachineSessionInvalidator.class);
        SysUserTypeServiceImpl service = new SysUserTypeServiceImpl(
            mock(SysUserTypeMapper.class), relations, mock(SysClientMapper.class),
            mock(ClientSessionService.class));
        ReflectionTestUtils.setField(service, "openApiSessionInvalidator", invalidator);

        ReflectionTestUtils.invokeMethod(service, "invalidateUsersForUserType", 7L);

        verify(invalidator, times(1)).invalidateByUserId(41L);
        verify(invalidator, times(1)).invalidateByUserId(42L);
    }

    @Test
    void everyCompositeIdentityWriteUsesTheDynamicDatasourceTransactionBoundary() throws Exception {
        assertTransactional(SysUserServiceImpl.class, "insertUserAuth", Long.class, Long[].class, Long.class);
        assertTransactional(SysUserServiceImpl.class, "updateUserStatus", Long.class, String.class);
        assertTransactional(SysUserServiceImpl.class, "deleteUserById", Long.class);
        assertTransactional(SysUserServiceImpl.class, "deleteUserByIds", Long[].class);
        assertTransactional(SysClientServiceImpl.class, "insertByBo", org.namewta.system.domain.bo.SysClientBo.class);
        assertTransactional(SysClientServiceImpl.class, "updateByBo", org.namewta.system.domain.bo.SysClientBo.class);
        assertTransactional(SysClientServiceImpl.class, "updateClientStatus", String.class, String.class);
        assertTransactional(SysClientServiceImpl.class, "deleteWithValidByIds", java.util.Collection.class, Boolean.class);
        assertTransactional(SysUserTypeServiceImpl.class, "updateByBo", org.namewta.system.domain.bo.SysUserTypeBo.class);
        assertTransactional(SysUserTypeServiceImpl.class, "updateStatus", Long.class, String.class);
        assertTransactional(SysUserTypeRelServiceImpl.class, "coverUserTypes", Long.class,
            java.util.Collection.class, String.class);
        assertTransactional(SysUserTypeRelServiceImpl.class, "grantUserType", Long.class, Long.class, String.class);
        assertTransactional(SysUserTypeRelServiceImpl.class, "deleteByUserIds", java.util.Collection.class);
    }

    @Test
    void callSiteMatrixUsesOnlyTheNarrowInvalidatorAndNeverTouchesTokensDirectly() throws IOException {
        String user = source("SysUserServiceImpl.java");
        String client = source("SysClientServiceImpl.java");
        String userType = source("SysUserTypeServiceImpl.java");
        String relation = source("SysUserTypeRelServiceImpl.java");

        assertThat(user).contains("openApiSessionInvalidator.invalidateByUserId(userId)");
        assertThat(client).contains("invalidateUsersForUserTypes")
            .contains("getDefaultRoleId()")
            .contains("getUserTypeId()")
            .contains("getStatus()");
        assertThat(userType).contains("invalidateUsersForUserType");
        assertThat(relation).contains("openApiSessionInvalidator.invalidateByUserId(userId)");

        for (String source : List.of(user, client, userType, relation)) {
            assertThat(source).doesNotContain("logoutByTokenValue", "RedisUtils", "StpUtil");
        }
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

    private static SysUserTypeRel relation(Long userId, Long userTypeId) {
        SysUserTypeRel relation = new SysUserTypeRel();
        relation.setUserId(userId);
        relation.setUserTypeId(userTypeId);
        return relation;
    }

}
