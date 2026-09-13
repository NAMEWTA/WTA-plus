package org.namewta.sso.api;

/**
 * SSO 模块读取一层应用目录的公开合同。
 */
public interface SsoClientCatalog {

    /**
     * 按 OAuth client_id 查询客户端。
     *
     * @param clientId OAuth 客户端标识
     * @return 目录视图；不存在时返回 {@code null}
     */
    SsoClientView findByClientId(String clientId);
}
