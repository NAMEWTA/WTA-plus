package org.namewta.sso.api;

import org.namewta.system.api.model.LoginUser;

/**
 * SSO 认人与按目标业务 Client 组装登录态。
 */
public interface SsoIdentityService {

    /**
     * 校验本仓用户名密码，不签发业务 Token。
     *
     * @param username 用户名
     * @param password 密码
     * @return 已认证用户
     */
    SsoAuthenticatedUser verifyPassword(String username, String password);

    /**
     * 校验用户是否具备目标 Client 登录域。
     *
     * @param userId   用户主键
     * @param clientId OAuth 客户端标识
     */
    void assertClientAccess(Long userId, String clientId);

    /**
     * 为目标业务 Client 组装 LoginUser。
     *
     * @param userId   用户主键
     * @param clientId 目标业务 Client
     * @return 登录态
     */
    LoginUser buildLoginUser(Long userId, String clientId);
}
