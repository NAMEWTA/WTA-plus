package org.namewta.web.service.social;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.common.social.oidc.OidcProtocolClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** RP 自有短期事务缓存，与 Provider Redis 无共享合同。内容加密，消费使用原子比较删除。 */
@Component
@RequiredArgsConstructor
public class ExternalAuthStateStore {
    private final SocialSecretCipher cipher;
    private final ExternalAuthCache cache;

    public record Transaction(
            String providerKey,
            String clientId,
            long registrationId,
            long version,
            long providerVersion,
            String purpose,
            Long bindingUserId,
            String returnPath,
            String browserHash,
            String nonce,
            String verifier) {}

    public record PendingRegistration(Transaction transaction, OidcIdentity identity) {}

    /** 保存授权事务，未完成的事务最多保留五分钟。 */
    public void save(String state, Transaction transaction) {
        put("transaction", state, transaction, Duration.ofMinutes(5));
    }

    /** 检查浏览器及用途后单次消费，失败不得回退普通社交登录。 */
    public Transaction consume(
            String state,
            String browserKey,
            String clientId,
            String providerKey,
            String purpose,
            Long bindingUserId) {
        String key = key("transaction", state);
        String encrypted = cache.read(key);
        Transaction value = decode("transaction", encrypted, Transaction.class);
        requireBrowser(value, browserKey, clientId);
        if (!value.providerKey().equals(providerKey)
                || !value.purpose().equals(purpose)
                || !java.util.Objects.equals(value.bindingUserId(), bindingUserId)) throw expired();
        if (!cache.consume(key, encrypted)) throw expired();
        return value;
    }

    /** 已验证身份等待补填时保留十分钟，不将 ID Token 返回浏览器。 */
    public String pending(Transaction transaction, OidcIdentity identity) {
        String ticket = OidcProtocolClient.randomToken();
        put(
                "registration",
                ticket,
                new PendingRegistration(transaction, identity),
                Duration.ofMinutes(10));
        return ticket;
    }

    /** 手机号格式验证完成后才消费票据；重复请求必须重新授权。 */
    public PendingRegistration consumeRegistration(
            String ticket, String browserKey, String clientId) {
        String key = key("registration", ticket);
        String encrypted = cache.read(key);
        PendingRegistration pending = decode("registration", encrypted, PendingRegistration.class);
        requireBrowser(pending.transaction(), browserKey, clientId);
        if (!cache.consume(key, encrypted)) throw expired();
        return pending;
    }

    private void put(String purpose, String token, Object value, Duration ttl) {
        cache.put(key(purpose, token), cipher.encrypt(purpose, JsonUtils.toJsonString(value)), ttl);
    }

    private <T> T decode(String purpose, String encrypted, Class<T> type) {
        if (encrypted == null) throw expired();
        try {
            return JsonUtils.parseObject(cipher.decrypt(purpose, encrypted), type);
        } catch (IllegalArgumentException e) {
            throw expired();
        }
    }

    private static void requireBrowser(
            Transaction transaction, String browserKey, String clientId) {
        if (browserKey == null
                || !browserKey.matches("[A-Za-z0-9_-]{43}")
                || !transaction.browserHash().equals(OidcProtocolClient.challenge(browserKey))
                || !transaction.clientId().equals(clientId)) throw expired();
    }

    private static String key(String purpose, String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw expired();
        return "auth:external:" + purpose + ":" + OidcProtocolClient.challenge(token);
    }

    private static ServiceException expired() {
        return new ServiceException("登录事务已过期或不属于当前浏览器，请重新发起登录");
    }
}
