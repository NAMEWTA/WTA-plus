package org.namewta.test.authorization.session;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.service.ClientSessionService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ClientSessionServiceUnitTest {

    @Test
    void invalidatesEveryTokenInOneClientWithoutTouchingAnotherClient() {
        FakeTokenOperations operations = new FakeTokenOperations()
            .add("token-a-1", loginUser(1L, 10L))
            .add("token-a-2", loginUser(2L, 10L))
            .add("token-b-1", loginUser(1L, 20L));
        ClientSessionService service = new ClientSessionService(operations);

        ClientSessionService.InvalidationResult result = service.kickoutClient(10L);

        assertEquals(2, result.matchedTokenCount());
        assertEquals(2, result.invalidatedTokenCount());
        assertTrue(result.clusterAcknowledged());
        assertEquals(List.of("token-a-1", "token-a-2"), operations.invalidatedTokens);
    }

    @Test
    void invalidatesEveryTokenForOneUserAndClientOnly() {
        FakeTokenOperations operations = new FakeTokenOperations()
            .add("token-a-1", loginUser(1L, 10L))
            .add("token-a-2", loginUser(1L, 10L))
            .add("token-a-other-user", loginUser(2L, 10L))
            .add("token-b-same-user", loginUser(1L, 20L));
        ClientSessionService service = new ClientSessionService(operations);

        ClientSessionService.InvalidationResult result = service.kickoutUserClient(1L, 10L);

        assertEquals(2, result.matchedTokenCount());
        assertEquals(List.of("token-a-1", "token-a-2"), operations.invalidatedTokens);
    }

    @Test
    void rejectsMissingAuthorizationTargetInsteadOfSilentlySucceeding() {
        ClientSessionService service = new ClientSessionService(new FakeTokenOperations());

        assertThrows(ServiceException.class, () -> service.kickoutClient(null));
        assertThrows(ServiceException.class, () -> service.kickoutUserClient(1L, null));
        assertThrows(ServiceException.class, () -> service.kickoutUserClient(null, 10L));
    }

    @Test
    void propagatesPartialFailureAndConvergesOnIdempotentRetry() {
        FakeTokenOperations operations = new FakeTokenOperations()
            .add("token-a-1", loginUser(1L, 10L))
            .add("token-a-2", loginUser(1L, 10L));
        operations.failOnceOnToken = "token-a-2";
        ClientSessionService service = new ClientSessionService(operations);

        ServiceException failure = assertThrows(ServiceException.class, () -> service.kickoutClient(10L));
        assertTrue(failure.getMessage().contains("会话失效失败"));

        ClientSessionService.InvalidationResult retry = service.kickoutClient(10L);
        assertEquals(1, retry.matchedTokenCount());
        assertEquals(1, retry.invalidatedTokenCount());
        assertEquals(List.of("token-a-1", "token-a-2"), operations.invalidatedTokens);
    }

    private static LoginUser loginUser(Long userId, Long clientPk) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setClientPk(clientPk);
        return loginUser;
    }

    private static final class FakeTokenOperations implements ClientSessionService.TokenOperations {

        private final Map<String, LoginUser> sessions = new LinkedHashMap<>();
        private final Set<String> clusterSessions = new LinkedHashSet<>();
        private final List<String> invalidatedTokens = new ArrayList<>();
        private String failOnceOnToken;

        private FakeTokenOperations add(String token, LoginUser loginUser) {
            sessions.put(token, loginUser);
            clusterSessions.add(token);
            return this;
        }

        @Override
        public List<String> activeTokenValues() {
            return new ArrayList<>(sessions.keySet());
        }

        @Override
        public LoginUser getLoginUser(String tokenValue) {
            return sessions.get(tokenValue);
        }

        @Override
        public void logout(String tokenValue) {
            sessions.remove(tokenValue);
            if (tokenValue.equals(failOnceOnToken)) {
                failOnceOnToken = null;
                throw new IllegalStateException("injected cluster acknowledgement failure");
            }
            if (clusterSessions.remove(tokenValue)) {
                invalidatedTokens.add(tokenValue);
            }
        }
    }
}
