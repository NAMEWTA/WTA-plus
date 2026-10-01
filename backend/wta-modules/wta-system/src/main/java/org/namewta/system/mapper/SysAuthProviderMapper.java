package org.namewta.system.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.system.domain.SysAuthProvider;
import org.namewta.system.domain.vo.SysAuthProviderVo;

/** 外部身份源持久化入口。 */
public interface SysAuthProviderMapper extends BaseMapperPlus<SysAuthProvider, SysAuthProviderVo> {
    /** 锁定当前身份源，串行化接入登记及身份源删除。 */
    SysAuthProvider selectForUpdate(@org.apache.ibatis.annotations.Param("id") long id);
    /** 退出验证允许读取停用及逻辑删除的身份源。 */
    SysAuthProvider selectForLogout(@org.apache.ibatis.annotations.Param("id") long id);
}

