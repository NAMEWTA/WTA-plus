package org.namewta.sso.support;

import java.time.Duration;
import java.time.Instant;
import org.namewta.sso.api.SsoAuthenticatedUser;

/** 创建会话持久化副本，以服务端原始时间覆盖输入上可能携带的旧认证元数据。 */
public final class SsoSessionRecords {
    private SsoSessionRecords() {
    }

    /**
     * 生成独立持久化用户副本，旧两参数用户合同仍可正常登录。
     * @param user 已由密码服务认证的用户
     * @param authenticatedAt 服务端原始认证时间
     * @param ttl 固定会话有效期
     * @return 带原始时间的持久化副本
     */
    public static SsoAuthenticatedUser create(SsoAuthenticatedUser user, Instant authenticatedAt, Duration ttl) {
        if (user == null || user.getUserId() == null || authenticatedAt == null || ttl == null
            || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("Valid authenticated subject and session lifetime are required");
        }
        SsoAuthenticatedUser stored = new SsoAuthenticatedUser(user.getUserId(), user.getUsername());
        stored.setAuthenticatedAt(authenticatedAt);
        stored.setExpiresAt(authenticatedAt.plus(ttl));
        return stored;
    }
}
