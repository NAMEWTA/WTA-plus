package org.namewta.profile.enterprise.mapper;

import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.enterprise.domain.ProfileEnterprise;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseDisclosureRow;

/** 只查询当前有效绑定与当前版本的明确白名单字段，不读取材料或历史。 */
public interface EnterpriseDisclosureMapper extends BaseMapperPlus<ProfileEnterprise, ProfileEnterprise> {
    /**
     * 查询一名已认证主体的当前有效档案字段。
     * @param userId 已认证账户主键
     * @param fields 服务端字段枚举名称集合，不用于 SQL 文本拼接
     * @return 有效投影；没有当前有效档案时为 null
     */
    EnterpriseDisclosureRow selectDisclosure(@Param("userId") long userId, @Param("fields") Set<String> fields);
}

