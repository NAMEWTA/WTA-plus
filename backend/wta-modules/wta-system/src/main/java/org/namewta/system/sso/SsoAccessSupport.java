package org.namewta.system.sso;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.system.domain.SysClient;

/**
 * 客户端管理接入态：必须已在 SSO 管理登记（有精确回调）。
 */
public final class SsoAccessSupport {

    private SsoAccessSupport() {
    }

    /**
     * 未登记则拒绝接入。
     *
     * @param client 已加载实体
     */
    public static void requireRegistered(SysClient client) {
        if (client == null) {
            throw new ServiceException("客户端不存在");
        }
        if (StringUtils.isBlank(client.getSsoRedirectUris())) {
            throw new ServiceException("未在 SSO 管理登记，无法接入");
        }
    }

    /**
     * 接入成功态要求 authMode 为 sso 或 both。
     *
     * @param authMode 登录模式
     * @return 规范化模式
     */
    public static String requireBindMode(String authMode) {
        String mode = StringUtils.blankToDefault(authMode, "both");
        if (!"sso".equals(mode) && !"both".equals(mode)) {
            throw new ServiceException("接入成功态要求登录模式为 sso 或 both");
        }
        return mode;
    }

    /**
     * 是否已接入（可与红色「没有接入」区分）。
     *
     * @param enabled     开关
     * @param authMode    模式
     * @param redirectUris 回调
     * @return 已接入
     */
    public static boolean isBound(Boolean enabled, String authMode, String redirectUris) {
        return Boolean.TRUE.equals(enabled)
            && StringUtils.isNotBlank(redirectUris)
            && ("sso".equals(authMode) || "both".equals(authMode));
    }
}
