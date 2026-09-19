package org.namewta.system.phone;

import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.namewta.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.namewta.system.domain.vo.SysUserVo;
import java.util.stream.Stream;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.api.OssService;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.mapper.*;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.namewta.system.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class UserPhoneWriteServiceTest {
    private static Object previousFactory;

    @BeforeAll
    static void mapping() {
        previousFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("converter", new Converter());
        new SpringUtil().postProcessBeanFactory(factory);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "phone-test"), SysUser.class);
    }

    @AfterAll
    static void restore() {
        ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", previousFactory);
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "registration"})
    void newAccountCannotBePersistedWithoutPhone(String entry) {
        SysUserMapper mapper = mock(SysUserMapper.class);
        when(mapper.insert(any(SysUser.class))).thenReturn(1);
        SysUserServiceImpl service = service(mapper);
        SysUserBo user = user();

        assertThatThrownBy(() -> {
            if (entry.equals("admin")) service.insertUser(user);
            else service.registerUser(user);
        }).isInstanceOf(ServiceException.class).hasMessage("手机号码不能为空");
        verify(mapper, never()).insert(any(SysUser.class));
    }

    @ParameterizedTest
    @MethodSource("invalidUpdates")
    void invalidFinalPhoneRejectsBeforeAnyWrite(String entry, String requested, String previous) {
        SysUserMapper mapper = mock(SysUserMapper.class);
        SysUser existing = new SysUser(); existing.setUserId(42L); existing.setPhoneNumber(previous);
        when(mapper.selectById(42L)).thenReturn(existing);
        when(mapper.updateById(any(SysUser.class))).thenReturn(1);
        when(mapper.currentModelClass()).thenReturn(SysUser.class);
        when(mapper.lambda()).thenAnswer(ignored -> new LambdaCrudChainWrapper<SysUser, SysUserVo>(mapper));
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        SysUserServiceImpl service = service(mapper);
        SysUserBo user = user(); user.setPhoneNumber(requested);

        assertThatThrownBy(() -> {
            if (entry.equals("admin")) service.updateUser(user);
            else service.updateUserProfile(user);
        }).isInstanceOf(ServiceException.class).hasMessageContaining("手机号码");
        verify(mapper, never()).updateById(any(SysUser.class));
        verify(mapper, never()).update(any(), any(Wrapper.class));
    }

    static Stream<Arguments> invalidUpdates() {
        return Stream.of("admin", "profile").flatMap(entry -> Stream.of(
            Arguments.of(entry, null, null), Arguments.of(entry, null, ""),
            Arguments.of(entry, null, "12345"), Arguments.of(entry, "", "13800138000"),
            Arguments.of(entry, " ", "13800138000"), Arguments.of(entry, "12345", "13800138000")));
    }

    private static SysUserBo user() {
        SysUserBo user = new SysUserBo();
        user.setUserId(42L); user.setUserName("phone-user"); user.setNickName("Phone User");
        return user;
    }

    private static SysUserServiceImpl service(SysUserMapper mapper) {
        return new SysUserServiceImpl(mapper, mock(SysDeptMapper.class), mock(SysRoleMapper.class),
            mock(SysPostMapper.class), mock(SysUserRoleMapper.class), mock(SysUserPostMapper.class),
            mock(SysClientMapper.class), mock(SysUserTypeMapper.class), mock(ClientSessionService.class),
            mock(ISysUserTypeRelService.class), mock(OssService.class));
    }
}
