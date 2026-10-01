package org.namewta.oidc.port;

import org.namewta.oidc.domain.OidcLogoutOutbox;

/** 标准协议网络投递，执行时不持有数据库事务。 */
public interface OidcLogoutDeliveryPort {
    /** 返回HTTP状态；可恢复网络故障返回0，不记录凭据。 */
    int deliver(OidcLogoutOutbox row);
}
