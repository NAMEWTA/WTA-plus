package org.namewta.sso.api;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * SSO 认人结果，不含业务 Token。
 */
public class SsoAuthenticatedUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private Instant authenticatedAt;
    private Instant expiresAt;

    public SsoAuthenticatedUser() {
    }

    public SsoAuthenticatedUser(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /** 返回首次认证时间；旧会话为空，不能据此声明近期认证。 */
    public Instant getAuthenticatedAt() {
        return authenticatedAt;
    }

    /** 保存首次认证时间，仅由会话创建边界写入。 */
    public void setAuthenticatedAt(Instant authenticatedAt) {
        this.authenticatedAt = authenticatedAt;
    }

    /** 返回会话创建时确定的到期时间。 */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /** 保存会话固定到期时间，不在读取时延期。 */
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
