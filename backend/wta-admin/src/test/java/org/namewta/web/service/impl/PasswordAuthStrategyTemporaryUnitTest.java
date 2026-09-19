package org.namewta.web.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import org.namewta.common.core.enums.LoginType;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.exception.user.UserException;
import org.namewta.common.web.config.properties.CaptchaProperties;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ClientUserTypeAccessService;
import org.namewta.system.temporarypassword.TemporaryPasswordService;
import org.namewta.system.temporarypassword.TemporaryPasswordStore;
import org.namewta.web.service.SysLoginService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class PasswordAuthStrategyTemporaryUnitTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void permanentPasswordWinsForLegacyEmptyPhoneWithoutReadingOrConsumingTemporaryValue(String phone) {
        Fixture fixture = new Fixture();
        SysUserVo user = fixture.user("Permanent1!");
        user.setPhoneNumber(phone);
        when(fixture.access.requireLoginAccess(user.getUserId(), fixture.client)).thenReturn(fixture.userType);

        assertSame(fixture.userType,
            fixture.strategy.authenticate(user, fixture.client, user.getUserName(), "Permanent1!"));

        verify(fixture.temporary, never()).verify(any(), any());
        verify(fixture.temporary, never()).consume(any());
        verify(fixture.login).loginSucceeded(user.getUserName());
    }

    @Test
    void validTemporaryValueIsNotConsumedWhenClientAccessFails() {
        Fixture fixture = new Fixture();
        SysUserVo user = fixture.user("Permanent1!");
        TemporaryPasswordService.VerifiedPassword verified = fixture.verified(user.getUserId(), "Temporary1!");
        when(fixture.temporary.verify(user.getUserId(), "Temporary1!")).thenReturn(java.util.Optional.of(verified));
        when(fixture.access.requireLoginAccess(user.getUserId(), fixture.client))
            .thenThrow(new ServiceException("当前账号不具备该应用的登录域"));

        assertThrows(ServiceException.class,
            () -> fixture.strategy.authenticate(user, fixture.client, user.getUserName(), "Temporary1!"));

        verify(fixture.temporary, never()).consume(any());
        verify(fixture.login, never()).loginSucceeded(any());
        verify(fixture.login, never()).loginFailed(any(), any());
    }

    @Test
    void validTemporaryValueConsumesAfterAccessAndCasLoserCountsOneFailure() {
        Fixture fixture = new Fixture();
        SysUserVo user = fixture.user("Permanent1!");
        TemporaryPasswordService.VerifiedPassword verified = fixture.verified(user.getUserId(), "Temporary1!");
        when(fixture.temporary.verify(user.getUserId(), "Temporary1!")).thenReturn(java.util.Optional.of(verified));
        when(fixture.access.requireLoginAccess(user.getUserId(), fixture.client)).thenReturn(fixture.userType);
        when(fixture.temporary.consume(verified)).thenReturn(false);
        when(fixture.login.loginFailed(LoginType.PASSWORD, user.getUserName()))
            .thenReturn(new UserException("user.password.retry.limit.count", 1));

        assertThrows(UserException.class,
            () -> fixture.strategy.authenticate(user, fixture.client, user.getUserName(), "Temporary1!"));

        verify(fixture.temporary).consume(verified);
        verify(fixture.login).loginFailed(LoginType.PASSWORD, user.getUserName());
        verify(fixture.login, never()).loginSucceeded(any());
    }

    private static final class Fixture {
        private final SysLoginService login = mock(SysLoginService.class);
        private final ClientUserTypeAccessService access = mock(ClientUserTypeAccessService.class);
        private final TemporaryPasswordService temporary = mock(TemporaryPasswordService.class);
        private final SysClientVo client = new SysClientVo();
        private final SysUserTypeVo userType = new SysUserTypeVo();
        private final PasswordAuthStrategy strategy = new PasswordAuthStrategy(new CaptchaProperties(), login,
            mock(SysUserMapper.class), access, temporary);

        private SysUserVo user(String permanentPassword) {
            SysUserVo user = new SysUserVo();
            user.setUserId(42L);
            user.setUserName("temporary-user");
            user.setPassword(BCrypt.hashpw(permanentPassword));
            return user;
        }

        private TemporaryPasswordService.VerifiedPassword verified(Long userId, String password) {
            PasswordPolicyService policy = mock(PasswordPolicyService.class);
            when(policy.generateTemporaryPassword()).thenReturn(password);
            CapturingStore store = new CapturingStore();
            TemporaryPasswordService service = new TemporaryPasswordService(policy, store);
            service.issue(userId);
            return service.verify(userId, password).orElseThrow();
        }
    }

    private static final class CapturingStore implements TemporaryPasswordStore {
        private String value;

        @Override
        public void store(Long userId, String passwordHash, Duration timeToLive) {
            value = passwordHash;
        }

        @Override
        public String read(Long userId) {
            return value;
        }

        @Override
        public boolean compareAndDelete(Long userId, String expectedHash) {
            return false;
        }
    }
}
