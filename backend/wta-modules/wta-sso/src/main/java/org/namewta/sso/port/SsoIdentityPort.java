package org.namewta.sso.port;

import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.system.api.model.LoginUser;

/**
 * 认人与按目标 Client 组装登录态。
 */
public interface SsoIdentityPort {

    /**
     * 校验本仓密码。
     *
     * @param username 用户名
     * @param password 密码
     * @return 已认证用户
     */
    SsoAuthenticatedUser verifyPassword(String username, String password);

    /**
     * 校验目标 Client 登录域。
     *
     * @param userId   用户主键
     * @param clientId 目标业务 Client
     */
    void assertClientAccess(Long userId, String clientId);

    /**
     * 组装业务 LoginUser。
     *
     * @param userId   用户主键
     * @param clientId 目标业务 Client
     * @return 登录态
     */
    LoginUser buildLoginUser(Long userId, String clientId);
}
