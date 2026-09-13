package org.namewta.sso.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.port.SsoIdentityPort;
import org.namewta.sso.port.SsoSessionPort;
import org.namewta.system.api.model.LoginUser;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("local")
@Tag("dev")
class SsoSessionServiceTest {

    @Test
    void passwordLoginCreatesSsoSessionNotBusinessToken() {
        MemoryStore store = new MemoryStore();
        SsoSessionService service = new SsoSessionService(new PasswordIdentity(), store);
        assertThrows(ServiceException.class, () -> service.login("WTA", ""));
        String sessionId = service.login("WTA", "admin123");
        assertEquals("WTA", service.current(sessionId).getUsername());
        service.logout(sessionId);
        assertNull(service.current(sessionId));
    }

    private static final class PasswordIdentity implements SsoIdentityPort {
        @Override
        public SsoAuthenticatedUser verifyPassword(String username, String password) {
            if (!"WTA".equals(username) || !"admin123".equals(password)) {
                throw new ServiceException("用户名或密码错误");
            }
            return new SsoAuthenticatedUser(1L, username);
        }

        @Override
        public void assertClientAccess(Long userId, String clientId) {
        }

        @Override
        public LoginUser buildLoginUser(Long userId, String clientId) {
            throw new UnsupportedOperationException("SSO session must not issue a business token");
        }
    }

    private static final class MemoryStore implements SsoSessionPort {
        private final Map<String, SsoAuthenticatedUser> sessions = new ConcurrentHashMap<>();

        @Override
        public String create(SsoAuthenticatedUser user) {
            String id = UUID.randomUUID().toString();
            sessions.put(id, user);
            return id;
        }

        @Override
        public SsoAuthenticatedUser find(String sessionId) {
            return sessionId == null ? null : sessions.get(sessionId);
        }

        @Override
        public void delete(String sessionId) {
            if (sessionId != null) {
                sessions.remove(sessionId);
            }
        }
    }
}
