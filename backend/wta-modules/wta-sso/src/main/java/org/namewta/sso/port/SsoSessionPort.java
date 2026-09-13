package org.namewta.sso.port;

import org.namewta.sso.api.SsoAuthenticatedUser;

/**
 * SSO 域会话，与业务 Token 分离。
 */
public interface SsoSessionPort {

    /**
     * 建立会话。
     *
     * @param user 已认证用户
     * @return 会话标识
     */
    String create(SsoAuthenticatedUser user);

    /**
     * 读取会话。
     *
     * @param sessionId 会话标识
     * @return 用户；无效时为空
     */
    SsoAuthenticatedUser find(String sessionId);

    /**
     * 删除会话。
     *
     * @param sessionId 会话标识
     */
    void delete(String sessionId);
}
