package org.namewta.oidc.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.oidc.domain.OidcApplication;

/** OIDC 领域记录的 MyBatis 入口，仅由 DAO 使用。 */
public interface OidcApplicationMapper extends BaseMapperPlus<OidcApplication, OidcApplication> {
    /** 历史退出需要保留已逻辑删除的应用端点；仅供协议全退使用。 */
    org.namewta.oidc.domain.OidcApplication findForLogout(
            @org.apache.ibatis.annotations.Param("id") Long id);
}
