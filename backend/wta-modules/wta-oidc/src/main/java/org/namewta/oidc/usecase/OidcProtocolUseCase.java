package org.namewta.oidc.usecase;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.domain.*;
import org.namewta.oidc.domain.vo.OidcApplicationVo;
import org.namewta.oidc.service.*;
import org.namewta.sso.api.SsoSessionSnapshot;
import org.springframework.stereotype.Component;

import java.util.*;

/** 协议适配器访问领域身份与应用的唯一入口。 */
@Component
@RequiredArgsConstructor
public class OidcProtocolUseCase {
    private final OidcApplicationService apps;
    private final OidcIdentityService identities;
    private final OidcSubjectService subjects;

    /** 读取协议客户端对应的应用登记。 */
    public OidcApplication app(String clientId) {
        return apps.client(clientId);
    }

    /** 构造独立应用的非机密协议视图。 */
    public OidcApplicationVo view(OidcApplication app) {
        return apps.vo(app);
    }

    /** 解析持久配置中的字符串列表，缺失值视为空列表。 */
    public List<String> values(String json) {
        return apps.values(json);
    }

    /** 读取可信 SSO 会话快照，缺失或失效时为空。 */
    public SsoSessionSnapshot session(String sid) {
        return identities.session(sid);
    }

    /** 根据当前正常账户和真实 SSO 认证时间构造协议身份。 */
    public OidcPrincipal principal(String sid, Set<String> allow) {
        var session = identities.session(sid);
        if (session == null)
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                    "login_required");
        return identities.principal(sid, allow, subjects.subject(session.userId()));
    }

    /** 校验当前身份或记录仍然有效，失效时拒绝继续。 */
    public void require(OidcPrincipal principal) {
        identities.require(principal);
    }

    /** 删除指定 SSO 会话，不扩大到其他浏览器。 */
    public void logout(String sid) {
        identities.logout(sid);
    }

    /** 以授权快照、当前字段策略与 scope 的交集发布当前资料。 */
    public Map<String, Object> userInfo(
            OidcPrincipal principal, String clientId, Set<String> scopes) {
        var app = apps.client(clientId);
        if (app == null || !Boolean.TRUE.equals(app.getEnabled()))
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                    "invalid_token");
        return identities.userInfo(principal, apps.values(app.getAllowedFieldsJson()), scopes);
    }
}
