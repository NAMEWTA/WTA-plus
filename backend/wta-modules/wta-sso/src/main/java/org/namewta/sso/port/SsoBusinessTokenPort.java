package org.namewta.sso.port;

import org.namewta.sso.api.SsoClientView;
import org.namewta.system.api.model.LoginUser;

/**
 * 签发与撤销目标业务 Client 的现有 Sa-Token。
 */
public interface SsoBusinessTokenPort {

    /**
     * 签发业务 Token。
     *
     * @param user   登录态
     * @param client 目标业务 Client
     * @return 令牌与过期秒数
     */
    IssuedToken issue(LoginUser user, SsoClientView client);

    /**
     * 只撤销提交的令牌，不等于 SLO。
     *
     * @param token 业务 Token
     */
    void revoke(String token);

    /**
     * 已签发的业务令牌。
     *
     * @param accessToken 令牌
     * @param expireIn    过期秒数
     * @param clientId    目标业务 Client
     */
    record IssuedToken(String accessToken, Long expireIn, String clientId) {
    }
}
