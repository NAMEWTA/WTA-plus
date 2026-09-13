package org.namewta.sso.support;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.sso.api.SsoClientView;

/**
 * 业务 Token extras 必须写入目标业务 Client，禁止 sso 中心 Client。
 */
public final class SsoTokenExtras {

    private SsoTokenExtras() {
    }

    /**
     * 按目标业务 Client 绑定 Sa-Token extras。
     *
     * @param client 目标业务 Client
     * @return 登录参数
     */
    public static SaLoginParameter bind(SsoClientView client) {
        if (client == null || StringUtils.isBlank(client.getClientId())) {
            throw new ServiceException("目标业务 Client 不能为空");
        }
        if ("sso".equalsIgnoreCase(client.getClientKey()) || "sso".equalsIgnoreCase(client.getClientId())) {
            throw new ServiceException("禁止签发 SSO 中心 Client 业务票");
        }
        SaLoginParameter model = new SaLoginParameter();
        if (client.getDeviceType() != null) {
            model.setDeviceType(client.getDeviceType());
        }
        if (client.getTimeout() != null) {
            model.setTimeout(client.getTimeout());
        }
        if (client.getActiveTimeout() != null) {
            model.setActiveTimeout(client.getActiveTimeout());
        }
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        model.setExtra(LoginHelper.CLIENT_PK_KEY, client.getId());
        model.setExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY, client.getAccessPath());
        model.setExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY, client.getIpWhitelist());
        return model;
    }
}
