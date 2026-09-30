package org.namewta.oidc.service;

import lombok.RequiredArgsConstructor;

import org.namewta.common.json.utils.JsonUtils;
import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.support.OidcSecrets;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

/** 浏览器短期交互数据与原始协议参数，绑定浏览器且认证加密。 */
@Service
@RequiredArgsConstructor
public class OidcInteractionService {
    /** 浏览器交互快照；只允许通过认证加密存储，原会话使用摘要。 */
    public record Interaction(
            Map<String, String> parameters,
            String browserHash,
            String previousSessionHash,
            boolean forceLogin,
            long createdAt,
            String csrf) {}

    private final org.namewta.oidc.port.OidcKeyPort keys;
    private final OidcProperties properties;
    private final org.namewta.oidc.port.OidcInteractionStore store;

    /** 创建短期加密交互，绑定浏览器及原会话，返回一次性随机标识。 */
    public String create(
            Map<String, String> parameters, String browser, String previousSession, boolean force) {
        String id = OidcSecrets.random();
        var value =
                new Interaction(
                        Map.copyOf(parameters),
                        OidcSecrets.hash(browser),
                        OidcSecrets.hash(previousSession),
                        force,
                        Instant.now().getEpochSecond(),
                        OidcSecrets.random());
        store.put(
                key(id),
                keys.encrypt("interaction:" + id, JsonUtils.toJsonString(value)),
                properties.getInteractionTtlSeconds());
        return id;
    }

    /** 验证交互标识、缓存期限、认证密文和浏览器绑定，任一失效均拒绝继续。 */
    public Interaction require(String id, String browser) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{43}") || browser == null)
            throw new OAuth2AuthenticationException("invalid_request");
        String raw = store.get(key(id));
        if (raw == null) throw new OAuth2AuthenticationException("invalid_request");
        var value =
                JsonUtils.parseObject(keys.decrypt("interaction:" + id, raw), Interaction.class);
        if (!OidcSecrets.equal(value.browserHash(), OidcSecrets.hash(browser)))
            throw new OAuth2AuthenticationException("invalid_request");
        return value;
    }

    /** 原子消费未过期的一次性凭据，失败时不得恢复可用状态。 */
    public void consume(String id, String browser) {
        require(id, browser);
        String raw = store.get(key(id));
        if (raw == null || !store.remove(key(id), raw))
            throw new OAuth2AuthenticationException("invalid_request");
    }

    /** Redis 仅用随机交互标识的摘要定位，原始标识只交给绑定浏览器。 */
    private String key(String id) {
        return "oidc:interaction:v1:" + OidcSecrets.hash(id);
    }
}
