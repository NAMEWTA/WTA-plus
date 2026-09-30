package org.namewta.oidc.usecase;

import lombok.RequiredArgsConstructor;

import org.namewta.common.json.utils.JsonUtils;
import org.namewta.oidc.adapter.codec.OidcAuthorizationCodec;
import org.namewta.oidc.domain.*;
import org.namewta.oidc.service.*;
import org.namewta.oidc.support.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.*;

/** 授权状态及当前身份校验，存储完整框架状态前始终认证加密。 */
@Service
@RequiredArgsConstructor
public class OidcAuthorizationWorkflow {
    private final OidcAuthorizationPersistenceService dao;
    private final OidcApplicationService apps;
    private final OidcIdentityService identities;
    private final OidcKeyService keys;

    /** 读取标识对应的当前持久记录，不存在时返回空。 */
    public OAuth2Authorization find(String id) {
        var row = dao.find(id);
        return active(row);
    }

    /** 通过凭据摘要查询授权，实时校验应用和账户状态。 */
    public OAuth2Authorization token(String raw) {
        if (raw == null) return null;
        return active(dao.token(OidcSecrets.hash(raw)));
    }

    /** 结合主库撤销状态、有效期及当前账户会话判断授权是否仍可用。 */
    private OAuth2Authorization active(OidcAuthorization row) {
        if (row == null || !"ACTIVE".equals(row.getStatus()) || !row.getExpiresAt().isAfter(now()))
            return null;
        var app = apps.find(row.getApplicationId());
        if (app == null || !Boolean.TRUE.equals(app.getEnabled())) return null;
        var value = decode(row);
        identities.require(principal(value));
        return value;
    }

    /** 从已解密框架授权中提取受信的 WTA 身份快照，拒绝其他主体类型。 */
    public OidcPrincipal principal(OAuth2Authorization value) {
        Authentication authentication = value.getAttribute(Principal.class.getName());
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcPrincipal p))
            throw new OAuth2AuthenticationException("invalid_grant");
        return p;
    }

    /** 保存授权码及字段快照，完整协议状态经认证加密后落库。 */
    @com.baomidou.dynamic.datasource.annotation.DSTransactional
    public void saveInitial(OAuth2Authorization value) {
        var p = principal(value);
        identities.require(p);
        var app = apps.lockClient(value.getRegisteredClientId());
        if (app == null || !Boolean.TRUE.equals(app.getEnabled()))
            throw new OAuth2AuthenticationException("invalid_client");
        var old = dao.find(value.getId());
        if (old != null) {
            if (value.getAccessToken() != null && value.getAccessToken().isInvalidated()) {
                dao.revoke(value.getId());
                return;
            }
            throw new OAuth2AuthenticationException("invalid_grant");
        }
        var request =
                value
                        .<org.springframework.security.oauth2.core.endpoint
                                        .OAuth2AuthorizationRequest>
                                getAttribute(
                                        org.springframework.security.oauth2.core.endpoint
                                                .OAuth2AuthorizationRequest.class
                                                .getName());
        if (!java.util.Objects.equals(p.applicationVersion(), app.getVersion())
                || request == null
                || !apps.values(app.getRedirectUrisJson()).contains(request.getRedirectUri())) {
            throw new OAuth2AuthenticationException("invalid_grant");
        }
        var code = value.getToken(OAuth2AuthorizationCode.class);
        if (code == null) throw new OAuth2AuthenticationException("invalid_grant");
        var row = new OidcAuthorization();
        row.setFrameworkId(value.getId());
        row.setApplicationId(app.getApplicationId());
        row.setUserId(p.userId());
        row.setSessionHash(OidcSecrets.hash(p.sessionId()));
        row.setSubject(p.name());
        row.setAllowedFieldsJson(JsonUtils.toJsonString(p.allowedFields()));
        row.setAuthorizedScopes(String.join(" ", value.getAuthorizedScopes()));
        row.setStatus("ACTIVE");
        row.setCodeHash(OidcSecrets.hash(code.getToken().getTokenValue()));
        row.setCodeExpiresAt(time(code.getToken().getExpiresAt()));
        row.setCodeConsumed(false);
        row.setExpiresAt(time(Instant.ofEpochSecond(p.expiresAt())));
        row.setVersion(0);
        row.setDelFlag("0");
        row.setAuthorizationJson(keys.encrypt(value.getId(), OidcAuthorizationCodec.encode(value)));
        dao.insert(row);
    }

    /** 原子消费未过期的一次性凭据，失败时不得恢复可用状态。 */
    public boolean consume(String raw, String clientId) {
        var row = dao.token(OidcSecrets.hash(raw));
        var app = apps.client(clientId);
        if (row == null || app == null || !app.getApplicationId().equals(row.getApplicationId()))
            return false;
        if (!dao.consume(OidcSecrets.hash(raw))) {
            dao.revoke(row.getFrameworkId());
            return false;
        }
        return true;
    }

    /** 完成最终状态校验或退出确认，拒绝复活已撤销授权。 */
    public void finish(OAuth2Authorization value) {
        var p = principal(value);
        identities.require(p);
        var app = apps.client(value.getRegisteredClientId());
        if (app == null || !Boolean.TRUE.equals(app.getEnabled()))
            throw new OAuth2AuthenticationException("invalid_grant");
        var row = dao.find(value.getId());
        if (row == null || !"PENDING".equals(row.getStatus()))
            throw new OAuth2AuthenticationException("invalid_grant");
        row.setAccessTokenHash(OidcSecrets.hash(value.getAccessToken().getToken().getTokenValue()));
        row.setAccessExpiresAt(time(value.getAccessToken().getToken().getExpiresAt()));
        var id = value.getToken(OidcIdToken.class);
        row.setIdTokenHash(id == null ? null : OidcSecrets.hash(id.getToken().getTokenValue()));
        row.setAuthorizationJson(keys.encrypt(value.getId(), OidcAuthorizationCodec.encode(value)));
        if (!dao.finish(row)) throw new OAuth2AuthenticationException("invalid_grant");
    }

    /** 以实际签发的 ID Token 摘要定位历史授权，仅供退出校验。 */
    public OAuth2Authorization logoutHint(String raw) {
        var row = dao.token(OidcSecrets.hash(raw));
        if (row == null || !OidcSecrets.hash(raw).equals(row.getIdTokenHash()))
            throw new OAuth2AuthenticationException("invalid_token");
        return decode(row);
    }

    /** 先验证密文的发行方和授权用途，再反序列化框架状态。 */
    private OAuth2Authorization decode(OidcAuthorization row) {
        return OidcAuthorizationCodec.decode(
                keys.decrypt(row.getFrameworkId(), row.getAuthorizationJson()));
    }

    /** 持久撤销指定授权，状态变化增加乐观版本。 */
    public void revoke(String id) {
        dao.revoke(id);
    }

    /** 持久撤销一个应用下的全部现存授权。 */
    public void revokeApplication(Long id) {
        dao.revokeApplication(id);
    }

    /** 持久撤销当前 SSO 会话关联的授权。 */
    public void revokeSession(String sid) {
        dao.revokeSession(OidcSecrets.hash(sid));
    }

    /** 数据库凭据时钟统一使用 UTC，避免节点时区造成兑换偏差。 */
    private LocalDateTime time(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    /** 返回与持久化授权期限一致的 UTC 当前时刻。 */
    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
