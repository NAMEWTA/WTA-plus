package org.namewta.profile.person.mapper;

import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.person.domain.ProfilePerson;
import org.namewta.profile.person.domain.model.read.PersonDisclosureRow;

/** 只查询当前有效绑定与当前版本的明确白名单字段，不读取材料或历史。 */
public interface PersonDisclosureMapper extends BaseMapperPlus<ProfilePerson, ProfilePerson> {
    /**
     * 查询一名已认证主体的当前有效档案字段。
     * @param userId 已认证账户主键
     * @param fields 服务端字段枚举名称集合，不用于 SQL 文本拼接
     * @return 有效投影；没有当前有效档案时为 null
     */
    PersonDisclosureRow selectDisclosure(@Param("userId") long userId, @Param("fields") Set<String> fields);
}

