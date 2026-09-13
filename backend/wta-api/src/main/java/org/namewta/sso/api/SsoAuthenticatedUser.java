package org.namewta.sso.api;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSO 认人结果，不含业务 Token。
 */
public class SsoAuthenticatedUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;

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
}
