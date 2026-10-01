package org.namewta.system.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.system.domain.SysAuthRegistration;
import org.namewta.system.domain.vo.SysAuthRegistrationVo;

/** 外部身份接入持久化入口。 */
import org.apache.ibatis.annotations.Param;
import org.namewta.system.domain.read.ExternalAuthRuntimeStamp;
import org.namewta.system.api.model.ExternalAuthEntry;
import java.util.List;

public interface SysAuthRegistrationMapper extends BaseMapperPlus<SysAuthRegistration, SysAuthRegistrationVo> {
    /** 统计全部接入及墓碑，保留身份源退出验证所需的引用。 */
    long countAllForProvider(@Param("providerId") long providerId);
    /** 仅用于退出验证，包含停用及逻辑删除记录。 */
    SysAuthRegistration selectForLogout(@Param("id") long id);
    /** 按公开入口查询当前有效版本，不读取密钥。 */
    ExternalAuthRuntimeStamp selectEnabledStamp(@Param("providerKey") String providerKey,
        @Param("businessClientId") String businessClientId);
    /** 按接入主键复核当前有效版本，不读取密钥。 */
    ExternalAuthRuntimeStamp selectCurrentStamp(@Param("id") long id);
    /** 公共入口仅包含显示元数据。 */
    List<ExternalAuthEntry> selectEnabledEntries(@Param("businessClientId") String businessClientId);
}

