package org.namewta.system.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.system.domain.SysSocial;
import org.namewta.system.domain.vo.SysSocialVo;

/**
 * 社会化关系Mapper接口
 */
public interface SysSocialMapper extends BaseMapperPlus<SysSocial, SysSocialVo> {

    /** 绑定与解绑按用户串行；调用方必须已开启数据库事务。 */
    Long lockUser(@org.apache.ibatis.annotations.Param("userId") Long userId);

}
