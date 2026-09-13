package org.namewta.sso.usecase;

import org.namewta.sso.domain.SsoOAuthCommands;
import org.namewta.sso.port.SsoBusinessTokenPort;

/**
 * OAuth 授权码用例。
 */
public interface SsoOAuthUseCase {

    /**
     * 授权。
     *
     * @param command 授权请求
     * @return 结果
     */
    SsoOAuthCommands.AuthorizeResult authorize(SsoOAuthCommands.AuthorizeCommand command);

    /**
     * 换票。
     *
     * @param command 换票请求
     * @return 令牌
     */
    SsoBusinessTokenPort.IssuedToken exchange(SsoOAuthCommands.TokenCommand command);

    /**
     * 撤销单个令牌。
     *
     * @param token 令牌
     */
    void revoke(String token);
}
