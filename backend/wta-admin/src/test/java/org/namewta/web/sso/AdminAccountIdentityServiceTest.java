package org.namewta.web.sso;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.system.domain.SysUser;
import org.namewta.system.mapper.SysUserMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("local")
@Tag("dev")
class AdminAccountIdentityServiceTest {
    @Test
    void normalAccountNeedsNoBusinessLoginDomainAndPublishesOnlyAccountContract() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        SysUser user = new SysUser();
        user.setUserId(17L);
        user.setUserName("ordinary");
        user.setNickName("昵称");
        user.setStatus("0");
        user.setDelFlag("0");
        user.setPassword("must-not-leave-system");
        user.setEmail("account@example.test");
        user.setPhoneNumber("13800000000");
        when(mapper.selectById(17L)).thenReturn(user);
        var result = new AdminAccountIdentityService(mapper).findActiveById(17L);
        assertThat(result.userId()).isEqualTo(17L);
        assertThat(result.email()).isEqualTo("account@example.test");
        assertThat(result.phoneNumber()).isEqualTo("13800000000");
        assertThat(java.util.Arrays.stream(result.getClass().getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
            .doesNotContain("password", "roles", "permissions", "userTypeId", "clientPk");
    }

    @Test
    void disabledDeletedMissingAndInvalidAccountsFailClosed() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        SysUser user = new SysUser();
        user.setUserId(17L);
        user.setStatus("1");
        user.setDelFlag("0");
        when(mapper.selectById(17L)).thenReturn(user);
        var service = new AdminAccountIdentityService(mapper);
        assertThat(service.findActiveById(17L)).isNull();
        user.setStatus("0");
        user.setDelFlag("2");
        assertThat(service.findActiveById(17L)).isNull();
        assertThat(service.findActiveById(99L)).isNull();
        assertThat(service.findActiveById(null)).isNull();
        assertThat(service.findActiveById(0L)).isNull();
        verify(mapper, never()).selectById(0L);
    }
}

