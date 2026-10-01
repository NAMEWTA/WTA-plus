package org.namewta.web.service.social;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.common.social.oidc.OidcProtocolClient;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 验证浏览器/用途/用户绑定和单次消费，缓存适配器以原子比较删除的内存替身隔离网络。 */
@Tag("dev")
class ExternalAuthStateStoreTest {
    private final OwnedCache cache = new OwnedCache();
    private final ExternalAuthStateStore store =
            new ExternalAuthStateStore(
                    new SocialSecretCipher(Base64.getEncoder().encodeToString(new byte[32])),
                    cache);
    private final String state = OidcProtocolClient.randomToken();
    private final String browser = OidcProtocolClient.randomToken();

    @Test
    void wrongBrowserClientPurposeAndUserCannotConsumeTheValidTransaction() {
        store.save(state, transaction("BIND", 4L));
        assertThatThrownBy(
                        () ->
                                store.consume(
                                        state,
                                        OidcProtocolClient.randomToken(),
                                        "home",
                                        "sso",
                                        "BIND",
                                        4L))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> store.consume(state, browser, "admin", "sso", "BIND", 4L))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> store.consume(state, browser, "home", "sso", "LOGIN", null))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> store.consume(state, browser, "home", "sso", "BIND", 5L))
                .isInstanceOf(ServiceException.class);
        assertThat(store.consume(state, browser, "home", "sso", "BIND", 4L).bindingUserId())
                .isEqualTo(4L);
        assertThatThrownBy(() -> store.consume(state, browser, "home", "sso", "BIND", 4L))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void snapshotsAreEncryptedAndPendingIdentityCannotBeSubstitutedByBrowser() {
        var identity =
                new OidcIdentity(
                        "https://sso.example",
                        "sub-a",
                        "sid-a",
                        "用户",
                        null,
                        null,
                        "sensitive-id-token",
                        "https://sso.example/logout",
                        1L);
        String ticket = store.pending(transaction("LOGIN", null), identity);
        assertThat(cache.values.values())
                .allMatch(
                        value -> !value.contains("sensitive-id-token") && !value.contains("sub-a"));
        assertThat(cache.ttl).isEqualTo(Duration.ofMinutes(10));
        assertThatThrownBy(() -> store.consumeRegistration(ticket, browser, "admin"))
                .isInstanceOf(ServiceException.class);
        assertThat(store.consumeRegistration(ticket, browser, "home").identity().subject())
                .isEqualTo("sub-a");
        assertThatThrownBy(() -> store.consumeRegistration(ticket, browser, "home"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void lostAtomicConsumptionAndExpiredStateFailClosed() {
        store.save(state, transaction("LOGIN", null));
        assertThat(cache.ttl).isEqualTo(Duration.ofMinutes(5));
        cache.allowConsumption = false;
        assertThatThrownBy(() -> store.consume(state, browser, "home", "sso", "LOGIN", null))
                .isInstanceOf(ServiceException.class);
        cache.values.clear();
        assertThatThrownBy(() -> store.consume(state, browser, "home", "sso", "LOGIN", null))
                .isInstanceOf(ServiceException.class);
    }

    private ExternalAuthStateStore.Transaction transaction(String purpose, Long userId) {
        return new ExternalAuthStateStore.Transaction(
                "sso",
                "home",
                1,
                2,
                3,
                purpose,
                userId,
                "/profile",
                OidcProtocolClient.challenge(browser),
                "nonce",
                "private-verifier");
    }

    private static class OwnedCache extends ExternalAuthCache {
        final Map<String, String> values = new ConcurrentHashMap<>();
        Duration ttl;
        boolean allowConsumption = true;

        @Override
        public void put(String key, String encrypted, Duration ttl) {
            values.put(key, encrypted);
            this.ttl = ttl;
        }

        @Override
        public String read(String key) {
            return values.get(key);
        }

        @Override
        public boolean consume(String key, String expected) {
            return allowConsumption && values.remove(key, expected);
        }
    }
}
