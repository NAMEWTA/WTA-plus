package org.namewta.sso.adapter.gateway;

import cn.dev33.satoken.stp.StpUtil;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.sso.api.SsoClientView;
import org.namewta.sso.port.SsoBusinessTokenPort;
import org.namewta.system.api.model.LoginUser;
import org.springframework.stereotype.Component;

/** 用现有 Sa-Token 签发/撤销目标业务 Client 令牌。 */
@Component
public class SaTokenSsoBusinessTokenAdapter implements SsoBusinessTokenPort {

    /** {@inheritDoc} */
    @Override
    public IssuedToken issue(LoginUser user, SsoClientView client) {
        LoginHelper.login(user, SsoTokenExtras.bind(client));
        return new IssuedToken(
                StpUtil.getTokenValue(), StpUtil.getTokenTimeout(), client.getClientId());
    }

    /** 第一方业务票记录中央会话摘要，原始sid不进入浏览器或业务Token。 */
    @Override
    public IssuedToken issue(LoginUser user, SsoClientView client, String sessionHash) {
        var parameter = SsoTokenExtras.bind(client);
        parameter.setExtra("sso_session_hash", sessionHash);
        LoginHelper.login(user, parameter);
        return new IssuedToken(
                StpUtil.getTokenValue(), StpUtil.getTokenTimeout(), client.getClientId());
    }

    /** {@inheritDoc} */
    @Override
    public void revoke(String token) {
        if (StringUtils.isBlank(token)) {
            throw new ServiceException("缺少 token");
        }
        StpUtil.logoutByTokenValue(token);
    }
}
