package org.namewta.sso.port;

import org.namewta.sso.api.SsoClientView;

/**
 * 读取一层应用目录。
 */
public interface SsoClientCatalogPort {

    /**
     * 按 OAuth client_id 查询。
     *
     * @param clientId 客户端标识
     * @return 目录视图
     */
    SsoClientView findByClientId(String clientId);
}
