package org.namewta.oidc.domain;

import java.io.Serializable;
import java.security.Principal;
import java.util.Set;

/** 来自真实 SSO 会话的认证快照，仅在加密授权状态中保存。 */
public record OidcPrincipal(
        String name,
        Long userId,
        String sessionId,
        long authTime,
        long expiresAt,
        Set<String> allowedFields,
        Integer applicationVersion)
        implements Principal, Serializable {
    /** 保留仅构造账户披露快照的入口；授权签发必须补齐应用版本。 */
    public OidcPrincipal(
            String name,
            Long userId,
            String sessionId,
            long authTime,
            long expiresAt,
            Set<String> allowedFields) {
        this(name, userId, sessionId, authTime, expiresAt, allowedFields, null);
    }

    @Override
    /** 返回持久公开 subject，禁止使用可变用户名作为标识。 */
    public String getName() {
        return name;
    }
}
