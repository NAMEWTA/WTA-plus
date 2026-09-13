package org.namewta.sso.dao;

import org.namewta.sso.domain.SsoAuthorizationCode;

/**
 * 一次性授权码持久化。
 */
public interface SsoAuthorizationCodeDao {

    /**
     * 写入授权码。
     *
     * @param code 授权码实体
     */
    void insert(SsoAuthorizationCode code);

    /**
     * 按授权码查询。
     *
     * @param authorizationCode 授权码
     * @return 实体
     */
    SsoAuthorizationCode findByCode(String authorizationCode);

    /**
     * 原子消费。
     *
     * @param authorizationCode 授权码
     * @param version           乐观锁版本
     * @return 是否成功
     */
    boolean consumeIfUnconsumed(String authorizationCode, Integer version);
}
