package org.namewta.test.oss.owner.system;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import io.github.linpeilie.mapstruct.SpringConverterFactory;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.namewta.system.api.OssService;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.SysUserPost;
import org.namewta.system.domain.SysUserRole;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.bo.SysUserBoToSysUserMapperImpl;
import org.namewta.system.mapper.*;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.namewta.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class SystemUserAvatarOssOwnerUnitTest {

    @TempDir
    Path temporary;

    /** MapstructUtils 缓存首次 Converter；自有 JVM 避免临时 Spring 工厂污染后续测试。 */
    @Test
    void registrationReconcilesPersistedAvatarAfterGeneratedUserId() throws Exception {
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Path log = temporary.resolve("avatar-owner.log");
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-Xms32m", "-Xmx384m", "-cp", classpath, getClass().getName(), "owned-avatar-probe")
            .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(child.waitFor(45, TimeUnit.SECONDS), "头像引用测试子进程应在限定时间内完成");
            assertEquals(0, child.exitValue(), () -> readLog(log));
            assertTrue(Files.readString(log).contains("OWNED_AVATAR_ASSERTIONS_PASSED"));
        } finally {
            if (child.isAlive()) child.destroyForcibly();
            assertTrue(child.waitFor(5, TimeUnit.SECONDS), "头像引用测试子进程必须退出");
        }
    }

    /** 显式使用真实生成 mapper；DefaultConverterFactory 只扫目录，package 的依赖位于 JAR。 */
    public static void main(String[] args) {
        if (args.length != 1 || !"owned-avatar-probe".equals(args[0])) throw new IllegalArgumentException("Unknown probe");
        Object previousFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        Object previousContext = ReflectionTestUtils.getField(SpringUtil.class, "applicationContext");
        try (var context = new GenericApplicationContext()) {
            context.registerBean(SpringUtils.class);
            context.registerBean(SysUserBoToSysUserMapperImpl.class);
            context.registerBean(Converter.class, () -> new Converter(new SpringConverterFactory(context)));
            context.refresh();
            new SystemUserAvatarOssOwnerUnitTest().verifyRegistrationAvatar();
            System.out.println("OWNED_AVATAR_ASSERTIONS_PASSED");
        } finally {
            ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", previousFactory);
            ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", previousContext);
        }
    }

    private static String readLog(Path log) {
        try { return Files.readString(log); }
        catch (java.io.IOException failure) { return "Cannot read owned avatar probe output"; }
    }

    private void verifyRegistrationAvatar() {
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
