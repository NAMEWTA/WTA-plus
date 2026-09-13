package org.namewta.sso.usecase;

import org.namewta.sso.api.SsoAuthenticatedUser;

/**
 * SSO 域会话用例。
 */
public interface SsoSessionUseCase {

    /**
     * 密码登录。
     *
     * @param username 用户名
     * @param password 密码
     * @return 会话标识
     */
    String login(String username, String password);

    /**
     * 当前会话。
     *
     * @param sessionId 会话标识
     * @return 用户
     */
    SsoAuthenticatedUser current(String sessionId);

    /**
     * 注销。
     *
     * @param sessionId 会话标识
     */
    void logout(String sessionId);
}
