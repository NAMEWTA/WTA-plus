package org.namewta.sso.service;

import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.api.SsoClientView;
import org.namewta.sso.domain.SsoOAuthCommands;
import org.namewta.sso.port.SsoBusinessTokenPort;
import org.namewta.sso.port.SsoClientCatalogPort;
import org.namewta.sso.port.SsoIdentityPort;
import org.namewta.sso.support.PkceS256;
import org.namewta.system.api.model.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Base64;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
@Tag("dev")
class SsoAuthorizationServiceTest {

    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String ADMIN_CLIENT = "e5cd7e4891bf95d1d19206ce24a7b32e";
    private static final String HOME_CLIENT = "428a8310cd442757ae699df5d894f051";
    private static final String REDIRECT = "http://127.0.0.1:4174/sso/callback";

    private InMemorySsoAuthorizationCodeStore store;
    private RecordingTokenPort tokens;
    private MutableClock clock;
    private SsoAuthorizationService service;

    @BeforeEach
    void setUp() {
        store = new InMemorySsoAuthorizationCodeStore();
        tokens = new RecordingTokenPort();
        clock = new MutableClock(Instant.parse("2026-09-13T00:00:00Z"));
        service = new SsoAuthorizationService(store, this::client, new StubIdentity(), tokens, clock, Duration.ofMinutes(5));
    }

    @Test
    void rejectsMissingChallengeNonS256AndWildcardRedirect() {
        SsoAuthenticatedUser user = new SsoAuthenticatedUser(1L, "WTA");
        assertThrows(ServiceException.class, () -> service.authorize(new SsoOAuthCommands.AuthorizeCommand(
            "code", ADMIN_CLIENT, REDIRECT, "state-1", null, "S256", user)));
        assertThrows(ServiceException.class, () -> service.authorize(new SsoOAuthCommands.AuthorizeCommand(
            "code", ADMIN_CLIENT, REDIRECT, "state-1", PkceS256.challenge(VERIFIER), "plain", user)));
        assertThrows(ServiceException.class, () -> service.authorize(new SsoOAuthCommands.AuthorizeCommand(
            "code", ADMIN_CLIENT, "*", "state-1", PkceS256.challenge(VERIFIER), "S256", user)));
        assertThrows(ServiceException.class, () -> service.authorize(new SsoOAuthCommands.AuthorizeCommand(
            "code", ADMIN_CLIENT, "http://evil.example/callback", "state-1", PkceS256.challenge(VERIFIER), "S256", user)));
        assertThrows(ServiceException.class, () -> service.authorize(new SsoOAuthCommands.AuthorizeCommand(
            "code", ADMIN_CLIENT, REDIRECT, null, PkceS256.challenge(VERIFIER), "S256", user)));
    }

    @Test
    void callbackPreservesOpaqueStateAndAlreadyEncodedRegisteredQuery() {
        String redirect = REDIRECT + "?tenant=a%26b&hint=hello+world";
        var catalog = client(ADMIN_CLIENT);
        catalog.setRedirectUris(List.of(redirect));
        var subject = new SsoAuthorizationService(store, ignored -> catalog, new StubIdentity(), tokens, clock, Duration.ofMinutes(5));
        String state = "  +&%中文/雪=尾部  ";
        var result = subject.authorize(new SsoOAuthCommands.AuthorizeCommand("code", ADMIN_CLIENT, redirect,
            state, PkceS256.challenge(VERIFIER), "S256", new SsoAuthenticatedUser(1L, "WTA")));
        String query = URI.create(result.redirectUri()).getRawQuery();
        assertTrue(query.startsWith("tenant=a%26b&hint=hello+world&"));
        assertEquals(4, query.split("&").length);
        String code = extractCode(result.redirectUri());
        assertEquals(state, URLDecoder.decode(query.substring(query.indexOf("&state=") + 7), StandardCharsets.UTF_8));
        assertEquals(state, store.findByCode(code).getState());
        assertEquals(ADMIN_CLIENT, subject.exchange(new SsoOAuthCommands.TokenCommand(
            "authorization_code", code, redirect, ADMIN_CLIENT, VERIFIER)).clientId());
    }

    @Test
    void rejectsFragmentsAndReservedResponseParametersEvenWhenRegistered() {
        for (String suffix : List.of("#fragment", "#", "?code=old", "?state=old", "?%73tate=old", "?error=old", "?error_description=old", "?error_uri=old")) {
            String redirect = REDIRECT + suffix;
            var catalog = client(ADMIN_CLIENT);
            catalog.setRedirectUris(List.of(redirect));
            var subject = new SsoAuthorizationService(store, ignored -> catalog, new StubIdentity(), tokens, clock, Duration.ofMinutes(5));
            assertThrows(ServiceException.class, () -> subject.authorize(new SsoOAuthCommands.AuthorizeCommand(
                "code", ADMIN_CLIENT, redirect, "state", PkceS256.challenge(VERIFIER), "S256", new SsoAuthenticatedUser(1L, "WTA"))), suffix);
        }
    }

    @Test
    void successfulExchangeUsesBusinessClientNotSsoAndRejectsNegatives() {
        SsoAuthenticatedUser user = new SsoAuthenticatedUser(1L, "WTA");
        SsoOAuthCommands.AuthorizeResult authorized = service.authorize(new SsoOAuthCommands.AuthorizeCommand(
            "code", ADMIN_CLIENT, REDIRECT, "csrf-state", PkceS256.challenge(VERIFIER), "S256", user));
        assertFalse(authorized.loginRequired());
        String code = extractCode(authorized.redirectUri());
        assertEquals(32, Base64.getUrlDecoder().decode(code).length);
        assertTrue(code.matches("[A-Za-z0-9_-]{43}"));

        assertThrows(ServiceException.class, () -> service.exchange(new SsoOAuthCommands.TokenCommand(
            "authorization_code", code, REDIRECT, ADMIN_CLIENT, VERIFIER + "nope")));

        SsoBusinessTokenPort.IssuedToken issued = service.exchange(new SsoOAuthCommands.TokenCommand(
            "authorization_code", code, REDIRECT, ADMIN_CLIENT, VERIFIER));
        assertEquals(ADMIN_CLIENT, issued.clientId());
        assertNotEquals("sso", issued.clientId());
        assertEquals(ADMIN_CLIENT, tokens.lastClientId);

        assertThrows(ServiceException.class, () -> service.exchange(new SsoOAuthCommands.TokenCommand(
            "authorization_code", code, REDIRECT, ADMIN_CLIENT, VERIFIER)));
    }

    @Test
    void rejectsExpiredAndReboundCode() {
        SsoAuthenticatedUser user = new SsoAuthenticatedUser(1L, "WTA");
        SsoOAuthCommands.AuthorizeResult authorized = service.authorize(new SsoOAuthCommands.AuthorizeCommand(
            "code", ADMIN_CLIENT, REDIRECT, "csrf-state", PkceS256.challenge(VERIFIER), "S256", user));
        String code = extractCode(authorized.redirectUri());
        clock.plus(Duration.ofMinutes(6));
        assertThrows(ServiceException.class, () -> service.exchange(new SsoOAuthCommands.TokenCommand(
            "authorization_code", code, REDIRECT, ADMIN_CLIENT, VERIFIER)));

        clock.plus(Duration.ofMinutes(-6));
        SsoOAuthCommands.AuthorizeResult fresh = service.authorize(new SsoOAuthCommands.AuthorizeCommand(
            "code", ADMIN_CLIENT, REDIRECT, "csrf-state", PkceS256.challenge(VERIFIER), "S256", user));
        String rebound = extractCode(fresh.redirectUri());
        assertThrows(ServiceException.class, () -> service.exchange(new SsoOAuthCommands.TokenCommand(
            "authorization_code", rebound, REDIRECT, HOME_CLIENT, VERIFIER)));
        assertThrows(ServiceException.class, () -> service.exchange(new SsoOAuthCommands.TokenCommand(
            "authorization_code", rebound, "http://127.0.0.1:4175/sso/callback", ADMIN_CLIENT, VERIFIER)));
    }

    @Test
    void concurrentExchangeAllowsAtMostOneSuccess() throws InterruptedException {
        SsoAuthenticatedUser user = new SsoAuthenticatedUser(1L, "WTA");
        String code = extractCode(service.authorize(new SsoOAuthCommands.AuthorizeCommand(
            "code", ADMIN_CLIENT, REDIRECT, "csrf-state", PkceS256.challenge(VERIFIER), "S256", user)).redirectUri());
        int workers = 16;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicInteger success = new AtomicInteger();
        for (int i = 0; i < workers; i++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    service.exchange(new SsoOAuthCommands.TokenCommand(
                        "authorization_code", code, REDIRECT, ADMIN_CLIENT, VERIFIER));
                    success.incrementAndGet();
                } catch (RuntimeException | InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            thread.start();
        }
        start.countDown();
        assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(1, success.get());
        assertEquals(1, tokens.issued.size());
    }

    @Test
    void revokeOnlyTouchesSubmittedToken() {
        tokens.remaining.add("admin-token");
        tokens.remaining.add("home-token");
        service.revoke("admin-token");
        assertEquals(List.of("home-token"), tokens.remaining);
    }

    private SsoClientView client(String clientId) {
        SsoClientView view = new SsoClientView();
        view.setId("sso".equals(clientId) ? 5L : ADMIN_CLIENT.equals(clientId) ? 1L : 2L);
        view.setClientId(clientId);
        view.setClientKey(ADMIN_CLIENT.equals(clientId) ? "pc" : HOME_CLIENT.equals(clientId) ? "home" : "sso");
        view.setStatus(SystemConstants.NORMAL);
        view.setSsoEnabled(true);
        view.setRedirectUris(List.of(REDIRECT, "http://127.0.0.1:4175/sso/callback"));
        return view;
    }

    private static String extractCode(String redirect) {
        int start = redirect.indexOf("code=") + 5;
        int end = redirect.indexOf('&', start);
        return redirect.substring(start, end);
    }

    private static final class StubIdentity implements SsoIdentityPort {
        @Override
        public org.namewta.sso.api.SsoAuthenticatedUser verifyPassword(String username, String password) {
            return new SsoAuthenticatedUser(1L, username);
        }

        @Override
        public void assertClientAccess(Long userId, String clientId) {
            if ("sso".equals(clientId)) {
                throw new ServiceException("center client");
            }
        }

        @Override
        public LoginUser buildLoginUser(Long userId, String clientId) {
            LoginUser user = new LoginUser();
            user.setUserId(userId);
            user.setUsername("WTA");
            user.setUserType("sys_user");
            user.setClientKey(clientId);
            return user;
        }
    }

    private static final class RecordingTokenPort implements SsoBusinessTokenPort {
        private final CopyOnWriteArrayList<String> issued = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<String> remaining = new CopyOnWriteArrayList<>();
        private volatile String lastClientId;

        @Override
        public IssuedToken issue(LoginUser user, SsoClientView client) {
            lastClientId = client.getClientId();
            String token = client.getClientId() + "-token-" + issued.size();
            issued.add(token);
            remaining.add(token);
            return new IssuedToken(token, 1800L, client.getClientId());
        }

        @Override
        public void revoke(String token) {
            remaining.remove(token);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void plus(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
