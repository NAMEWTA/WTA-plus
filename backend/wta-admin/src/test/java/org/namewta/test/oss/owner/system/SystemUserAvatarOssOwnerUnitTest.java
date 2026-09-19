package org.namewta.test.oss.owner.system;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import org.namewta.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.namewta.system.api.OssService;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.SysUserPost;
import org.namewta.system.domain.SysUserRole;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.mapper.*;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.namewta.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class SystemUserAvatarOssOwnerUnitTest {

    @BeforeAll
    static void configureMapstructConverter() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("converter", new Converter());
        new SpringUtil().postProcessBeanFactory(beanFactory);
    }

    @Test
    void registrationReconcilesPersistedAvatarAfterGeneratedUserId() {
        OssService ossService = mock(OssService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class, RETURNS_DEEP_STUBS);
        SysUserServiceImpl service = userService(userMapper, ossService);
        SysUserBo user = new SysUserBo();
        user.setAvatar(77L);
        user.setPhoneNumber("13800138000");
        doAnswer(invocation -> {
            invocation.getArgument(0, SysUser.class).setUserId(100L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        assertTrue(service.registerUser(user));

        verify(ossService).reconcileReferences("sys_user", "100", List.of(), List.of(77L));
    }

    private SysUserServiceImpl userService(SysUserMapper userMapper, OssService ossService) {
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        @SuppressWarnings("unchecked")
        LambdaCrudChainWrapper<SysUserRole, SysUserRole> roleChain =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(userRoleMapper.lambda()).thenReturn(roleChain);
        when(roleChain.eq(any(SFunction.class), any())).thenReturn(roleChain);

        SysUserPostMapper userPostMapper = mock(SysUserPostMapper.class);
        @SuppressWarnings("unchecked")
        LambdaCrudChainWrapper<SysUserPost, SysUserPost> postChain =
            mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(userPostMapper.lambda()).thenReturn(postChain);
        when(postChain.eq(any(SFunction.class), any())).thenReturn(postChain);

        ISysUserTypeRelService userTypeRelService = mock(ISysUserTypeRelService.class);
        when(userTypeRelService.selectByUserId(any())).thenReturn(List.of());
        return new SysUserServiceImpl(
            userMapper,
            mock(SysDeptMapper.class, RETURNS_DEEP_STUBS),
            mock(SysRoleMapper.class, RETURNS_DEEP_STUBS),
            mock(SysPostMapper.class, RETURNS_DEEP_STUBS),
            userRoleMapper,
            userPostMapper,
            mock(SysClientMapper.class, RETURNS_DEEP_STUBS),
            mock(SysUserTypeMapper.class, RETURNS_DEEP_STUBS),
            mock(ClientSessionService.class),
            userTypeRelService,
            ossService
        );
    }
}
