package org.namewta.web.service.social;

import cn.dev33.satoken.stp.StpUtil;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.common.social.oidc.OidcProtocolClient;
import org.namewta.system.api.model.ExternalAuthRegistration;
import org.namewta.web.domain.vo.LoginVo;
import org.redisson.api.RLock;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/** 本业务后端的 OIDC 会话关联。按 sid 锁定签发与撤销，提前到达的退出通知留下撤销墓碑。 */
@Component
@RequiredArgsConstructor
public class ExternalAuthSessionStore {
    public static final String SESSION_EXTRA = "external_auth_session";
    public static final String SOURCE_EXTRA = "external_auth_source";
    private final SocialSecretCipher cipher;

    public record Session(
            long registrationId,
            String issuer,
            String externalClientId,
            String sid,
            String idToken,
            String endSessionEndpoint,
            String postLogoutRedirectUri) {}

    public record Status(String authSource, boolean globalLogoutAvailable) {}

    public record Logout(String endSessionUrl, String state) {}

    /** 一次本地令牌签发和关联登记在同一 sid 锁内完成；缓存失败立即撤销新令牌。 */
    public LoginVo issue(
            ExternalAuthRegistration registration,
            OidcIdentity identity,
            Function<String, LoginVo> issueToken) {
        if (!"OIDC".equals(registration.protocol()) || identity.sessionId() == null)
            return issueToken.apply(null);
        String group =
                group(identity.issuer(), registration.externalClientId(), identity.sessionId());
        return locked(
                group,
                () -> {
                    if (RedisUtils.isExistsObject(group + ":revoked"))
                        throw new ServiceException("中央会话已退出，请重新登录");
                    String sessionKey = OidcProtocolClient.randomToken();
                    LoginVo result = issueToken.apply(sessionKey);
                    try {
                        Session session =
                                new Session(
                                        registration.id(),
                                        identity.issuer(),
                                        registration.externalClientId(),
                                        identity.sessionId(),
                                        identity.idToken(),
                                        identity.endSessionEndpoint(),
                                        registration.postLogoutRedirectUri());
                        String encrypted =
                                cipher.encrypt("rp-session", JsonUtils.toJsonString(session));
                        long ttl = result.getExpireIn();
                        if (ttl > 0)
                            RedisUtils.setCacheObject(
                                    sessionKey(sessionKey), encrypted, Duration.ofSeconds(ttl));
                        else RedisUtils.setCacheObject(sessionKey(sessionKey), encrypted);
                        boolean first = !RedisUtils.isExistsObject(group + ":tokens");
                        long remaining = RedisUtils.getTimeToLive(group + ":tokens");
                        RedisUtils.addCacheSet(group + ":tokens", result.getAccessToken());
                        if (ttl > 0 && (first || remaining >= 0 && remaining < ttl * 1000))
                            RedisUtils.expire(group + ":tokens", Duration.ofSeconds(ttl));
                        else if (ttl < 0)
                            RedisUtils.getClient().getSet(group + ":tokens").clearExpire();
                        result.setGlobalLogoutAvailable(globalAvailable(session));
                        return result;
                    } catch (RuntimeException failure) {
                        StpUtil.logoutByTokenValue(result.getAccessToken());
                        throw failure;
                    }
                });
    }

    /** 全退按 issuer/client/sid 限定，不撤销该用户其他设备或本地密码会话。 */
    public void revoke(ExternalAuthRegistration registration, String sid) {
        String group = group(registration.issuer(), registration.externalClientId(), sid);
        locked(
                group,
                () -> {
                    // 授权码五分钟、待补填十分钟；一天墓碑覆盖全部已在途回调，不由通知重复延长业务会话。
                    RedisUtils.setCacheObject(group + ":revoked", true, Duration.ofDays(1));
                    for (String token : RedisUtils.<String>getCacheSet(group + ":tokens")) {
                        StpUtil.logoutByTokenValue(token);
                    }
                    RedisUtils.deleteObject(group + ":tokens");
                    return null;
                });
    }

    /** 仅返回 UI 能力，不返回 Provider 令牌或中央 sid。 */
    public Status status() {
        StpUtil.checkLogin();
        Object source = StpUtil.getExtra(SOURCE_EXTRA);
        Session session = current();
        return new Status(source instanceof String text ? text : "LOCAL", globalAvailable(session));
    }

    /** 使用服务端保存的 ID Token hint 和已登记回跳生成标准 RP 发起退出地址。 */
    public Logout beginLogout() {
        StpUtil.checkLogin();
        Session session = current();
        if (!globalAvailable(session)) throw new ServiceException("当前登录未关联可统一退出的SSO会话");
        String state = OidcProtocolClient.randomToken();
        String url =
                OidcProtocolClient.appendQuery(
                        session.endSessionEndpoint(),
                        Map.of(
                                "id_token_hint",
                                session.idToken(),
                                "client_id",
                                session.externalClientId(),
                                "post_logout_redirect_uri",
                                session.postLogoutRedirectUri(),
                                "state",
                                state));
        // 当前应用先真实失效；中央确认与其他应用的失效由标准退出和后台通知继续完成。
        StpUtil.logout();
        return new Logout(url, state);
    }

    private Session current() {
        Object id = StpUtil.getExtra(SESSION_EXTRA);
        if (!(id instanceof String key) || !key.matches("[A-Za-z0-9_-]{43}")) return null;
        String encrypted = RedisUtils.getCacheObject(sessionKey(key));
        return encrypted == null
                ? null
                : JsonUtils.parseObject(cipher.decrypt("rp-session", encrypted), Session.class);
    }

    private static boolean globalAvailable(Session session) {
        return session != null
                && session.endSessionEndpoint() != null
                && session.idToken() != null
                && session.postLogoutRedirectUri() != null
                && !session.postLogoutRedirectUri().isBlank();
    }

    private static String sessionKey(String id) {
        return "auth:external:session:" + id;
    }

    private static String group(String issuer, String clientId, String sid) {
        return "auth:external:sid:"
                + OidcProtocolClient.identityKey(issuer, clientId.length() + ":" + clientId + sid);
    }

    private static <T> T locked(String group, java.util.function.Supplier<T> operation) {
        RLock lock = RedisUtils.getClient().getLock(group + ":lock");
        try {
            if (!lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS))
                throw new ServiceException("登录状态正在更新，请重试");
            try {
                return operation.get();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("登录操作已取消");
        }
    }
}
