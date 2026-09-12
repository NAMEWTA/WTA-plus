package org.namewta.system.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.system.domain.SysOssConfig;
import org.namewta.system.domain.vo.SysOssConfigVo;

/**
 * 对象存储配置Mapper接口
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */
public interface SysOssConfigMapper extends BaseMapperPlus<SysOssConfig, SysOssConfigVo> {

    @Select("select * from sys_oss_config where oss_config_id = #{ossConfigId} for update")
    SysOssConfig selectByIdForUpdate(@Param("ossConfigId") Long ossConfigId);

    @Select("select count(*) from sys_oss where service = #{configKey}")
    long countOssReferences(@Param("configKey") String configKey);

    @Select("select count(*) from sys_oss_config where config_key = #{configKey} "
        + "and oss_config_id <> #{ossConfigId}")
    long countConfigKeyConflicts(@Param("configKey") String configKey,
                                 @Param("ossConfigId") Long ossConfigId);

    @Select("select count(*) from sys_oss_config where status = 'Y'")
    long countDefaultConfigs();

    @Update("update sys_oss_config set status = 'N' where status = 'Y' and oss_config_id <> #{ossConfigId}")
    int clearOtherDefaultStatuses(@Param("ossConfigId") Long ossConfigId);

}
