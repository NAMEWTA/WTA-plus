package org.namewta.system.service;

import org.namewta.system.domain.vo.SysUserTypeRelVo;
import org.namewta.system.domain.vo.SysUserTypeVo;

import java.util.Collection;
import java.util.List;

/**
 * 用户登录域关系Service接口
 */
public interface ISysUserTypeRelService {

    /**
     * 查询用户拥有的登录域关系
     *
     * @param userId 用户ID
     * @return 登录域关系列表
     */
    List<SysUserTypeRelVo> selectByUserId(Long userId);

    /**
     * 批量查询用户拥有的登录域关系
     *
     * @param userIds 用户ID集合
     * @return 登录域关系列表
     */
    List<SysUserTypeRelVo> selectByUserIds(Collection<Long> userIds);

    /** 查询拥有任一指定且启用登录域的正常关系用户 ID。 */
    List<Long> selectUserIdsByUserTypeIds(Collection<Long> userTypeIds);

    /**
     * 判断用户是否拥有指定且正常的登录域
     *
     * @param userId     用户ID
     * @param userTypeId 登录域ID
     * @return 是否拥有
     */
    boolean hasUserType(Long userId, Long userTypeId);

    /**
     * 查询用户在指定登录域上的有效关系，并校验登录域本身启用。
     *
     * @param userId     用户ID
     * @param userTypeId 登录域ID
     * @return 登录域，不存在或停用时返回 null
     */
    SysUserTypeVo getActiveUserType(Long userId, Long userTypeId);

    /**
     * 覆盖更新用户登录域。删除未包含的关系，为新增关系写入授权来源。
     *
     * @param userId      用户ID
     * @param userTypeIds 目标登录域ID集合，空集合表示清空
     * @param grantSource 新增关系的授权来源
     * @return 被移除的登录域编码列表，便于会话清理
     */
    List<String> coverUserTypes(Long userId, Collection<Long> userTypeIds, String grantSource);

    /**
     * 为用户追加一个登录域（已存在则忽略）。
     *
     * @param userId      用户ID
     * @param userTypeId  登录域ID
     * @param grantSource 授权来源
     * @return 是否新增
     */
    boolean grantUserType(Long userId, Long userTypeId, String grantSource);

    /**
     * 删除用户全部登录域关系
     *
     * @param userIds 用户ID集合
     */
    void deleteByUserIds(Collection<Long> userIds);

}
