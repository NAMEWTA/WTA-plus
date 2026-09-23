package org.namewta.system.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.system.domain.SysUserType;
import org.namewta.system.domain.SysUserTypeRel;
import org.namewta.system.domain.vo.SysUserTypeRelVo;

import java.util.Collection;
import java.util.List;

/**
 * 用户登录域关系Mapper接口
 */
public interface SysUserTypeRelMapper extends BaseMapperPlus<SysUserTypeRel, SysUserTypeRelVo>, MPJBaseMapper<SysUserTypeRel> {

    /**
     * 查询用户拥有的登录域（含登录域编码、名称、状态）。
     *
     * @param userId 用户ID
     * @return 登录域关系列表
     */
    default List<SysUserTypeRelVo> selectVoListByUserId(Long userId) {
        return this.selectJoinList(SysUserTypeRelVo.class, QueryBuilder.lambdaJoin("r", SysUserTypeRel.class)
            .selectAll(SysUserTypeRel.class)
            .selectAs(SysUserType::getUserTypeCode, SysUserTypeRelVo::getUserTypeCode)
            .selectAs(SysUserType::getUserTypeName, SysUserTypeRelVo::getUserTypeName)
            .selectAs(SysUserType::getStatus, SysUserTypeRelVo::getUserTypeStatus)
            .leftJoin(SysUserType.class, "t", SysUserType::getUserTypeId, SysUserTypeRel::getUserTypeId)
            .eq("r", SysUserTypeRel::getUserId, userId)
            .orderByAsc("t", SysUserType::getOrderNum)
            .build());
    }

    /**
     * 批量查询用户拥有的登录域（含登录域编码、名称、状态）。
     *
     * @param userIds 用户ID集合
     * @return 登录域关系列表
     */
    default List<SysUserTypeRelVo> selectVoListByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return this.selectJoinList(SysUserTypeRelVo.class, QueryBuilder.lambdaJoin("r", SysUserTypeRel.class)
            .selectAll(SysUserTypeRel.class)
            .selectAs(SysUserType::getUserTypeCode, SysUserTypeRelVo::getUserTypeCode)
            .selectAs(SysUserType::getUserTypeName, SysUserTypeRelVo::getUserTypeName)
            .selectAs(SysUserType::getStatus, SysUserTypeRelVo::getUserTypeStatus)
            .leftJoin(SysUserType.class, "t", SysUserType::getUserTypeId, SysUserTypeRel::getUserTypeId)
            .in("r", SysUserTypeRel::getUserId, userIds)
            .orderByAsc("t", SysUserType::getOrderNum)
            .build());
    }

}
