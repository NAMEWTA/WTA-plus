package org.namewta.sso.mapper;

import org.apache.ibatis.annotations.Param;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.sso.domain.SsoAuthorizationCode;

/**
 * 一次性授权码 Mapper。
 */
public interface SsoAuthorizationCodeMapper extends BaseMapperPlus<SsoAuthorizationCode, SsoAuthorizationCode> {

    /**
     * 仅当未消费时标记已消费。
     *
     * @param authorizationCode 授权码
     * @param version           乐观锁版本
     * @return 更新行数
     */
    int consumeIfUnconsumed(@Param("authorizationCode") String authorizationCode, @Param("version") Integer version);
}
