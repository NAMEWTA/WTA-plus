package org.namewta.web.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.system.service.ISysSocialService;
import org.namewta.web.service.social.ExternalAuthService;

/** 旧 URL 仍可调用，但必须复用新解绑的身份检查、事务与用户锁。 */
@Tag("dev")
class AuthLegacyUnbindBoundaryTest {
    @Test
    void legacyUnbindDelegatesToTheUnifiedAccountBoundary() {
        var legacySocial = mock(ISysSocialService.class);
        var externalAuth = mock(ExternalAuthService.class);
        var controller =
                new AuthController(null, null, legacySocial, null, null, null, null, externalAuth);
        try (var token = mockStatic(StpUtil.class)) {
            controller.unlockSocial(77L);
            token.verify(StpUtil::checkLogin);
        }
        verify(externalAuth).unbind(77L);
        verifyNoInteractions(legacySocial);
    }

    @Test
    void ignoredAuthControllerStillRejectsAnonymousUnbindBeforeDelegation() {
        var externalAuth = mock(ExternalAuthService.class);
        var controller = new AuthController(null, null, null, null, null, null, null, externalAuth);
        try (var token = mockStatic(StpUtil.class)) {
            token.when(StpUtil::checkLogin)
                    .thenThrow(NotLoginException.newInstance("login", "-1", "not logged in", null));
            assertThrows(NotLoginException.class, () -> controller.unlockSocial(77L));
        }
        verifyNoInteractions(externalAuth);
    }
}
