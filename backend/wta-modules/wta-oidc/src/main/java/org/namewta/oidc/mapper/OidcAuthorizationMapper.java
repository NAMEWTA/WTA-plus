package org.namewta.oidc.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.oidc.domain.OidcAuthorization;

/** OIDC 领域记录的 MyBatis 入口，仅由 DAO 使用。 */
public interface OidcAuthorizationMapper
        extends BaseMapperPlus<OidcAuthorization, OidcAuthorization> {
    /** 物理清除过期保留期后的少量授权记录，不触及稳定 subject。 */
    int purgeExpired(
            @org.apache.ibatis.annotations.Param("cutoff") java.time.LocalDateTime cutoff,
            @org.apache.ibatis.annotations.Param("limit") int limit);
}
