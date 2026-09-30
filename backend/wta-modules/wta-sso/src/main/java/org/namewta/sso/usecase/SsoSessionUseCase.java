package org.namewta.sso.usecase;

import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.api.SsoSessionSnapshot;

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
     * 读取带可信原始认证时间的会话，用于身份协议。
     * @param sessionId 中央会话标识
     * @return 有效快照；旧实现无此能力时返回 null，要求重新认证
     */
    default SsoSessionSnapshot currentSnapshot(String sessionId) {
        return null;
    }

    /**
     * 注销。
     *
     * @param sessionId 会话标识
     */
    void logout(String sessionId);
}
