package org.namewta.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StreamUtils;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.system.domain.SysUserType;
import org.namewta.system.domain.SysUserTypeRel;
import org.namewta.system.domain.vo.SysUserTypeRelVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.mapper.SysUserTypeMapper;
import org.namewta.system.mapper.SysUserTypeRelMapper;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 用户登录域关系Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class SysUserTypeRelServiceImpl implements ISysUserTypeRelService {

    private final SysUserTypeRelMapper userTypeRelMapper;
    private final SysUserTypeMapper userTypeMapper;
    private final ClientSessionService clientSessionService;
    private OpenApiMachineSessionInvalidator openApiSessionInvalidator = ignored -> 0;

    @Autowired(required = false)
    void setOpenApiSessionInvalidator(OpenApiMachineSessionInvalidator openApiSessionInvalidator) {
        this.openApiSessionInvalidator = Objects.requireNonNull(openApiSessionInvalidator);
    }

    /**
     * 查询用户拥有的登录域关系
     *
     * @param userId 用户ID
     * @return 登录域关系列表
     */
    @Override
    public List<SysUserTypeRelVo> selectByUserId(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return List.of();
        }
        return userTypeRelMapper.selectVoListByUserId(userId);
    }

    /**
     * 批量查询用户拥有的登录域关系
     *
     * @param userIds 用户ID集合
     * @return 登录域关系列表
     */
    @Override
    public List<SysUserTypeRelVo> selectByUserIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        return userTypeRelMapper.selectVoListByUserIds(userIds);
    }

    @Override
    public List<Long> selectUserIdsByUserTypeIds(Collection<Long> userTypeIds) {
        if (CollUtil.isEmpty(userTypeIds)) return List.of();
        Set<Long> activeIds = new HashSet<>(userTypeMapper.lambda()
            .select(SysUserType::getUserTypeId)
            .in(SysUserType::getUserTypeId, userTypeIds)
            .eq(SysUserType::getStatus, SystemConstants.NORMAL)
            .list().stream().map(SysUserType::getUserTypeId).toList());
        if (!activeIds.containsAll(userTypeIds)) throw new ServiceException("所选用户类型不存在或已停用");
        return userTypeRelMapper.lambda()
            .select(SysUserTypeRel::getUserId)
            .in(SysUserTypeRel::getUserTypeId, activeIds)
            .eq(SysUserTypeRel::getStatus, SystemConstants.NORMAL)
            .list().stream().map(SysUserTypeRel::getUserId).distinct().toList();
    }

    /**
     * 判断用户是否拥有指定且正常的登录域
     *
     * @param userId     用户ID
     * @param userTypeId 登录域ID
     * @return 是否拥有
     */
    @Override
    public boolean hasUserType(Long userId, Long userTypeId) {
        return ObjectUtil.isNotNull(getActiveUserType(userId, userTypeId));
    }

    /**
     * 查询用户在指定登录域上的有效关系，并校验登录域本身启用。
     *
     * @param userId     用户ID
     * @param userTypeId 登录域ID
     * @return 登录域，不存在或停用时返回 null
     */
    @Override
    public SysUserTypeVo getActiveUserType(Long userId, Long userTypeId) {
        if (ObjectUtil.isNull(userId) || ObjectUtil.isNull(userTypeId)) {
            return null;
        }
        boolean owned = userTypeRelMapper.lambda()
            .eq(SysUserTypeRel::getUserId, userId)
            .eq(SysUserTypeRel::getUserTypeId, userTypeId)
            .eq(SysUserTypeRel::getStatus, SystemConstants.NORMAL)
            .exists();
        if (!owned) {
            return null;
        }
        SysUserTypeVo userType = userTypeMapper.selectVoById(userTypeId);
        if (ObjectUtil.isNull(userType) || !SystemConstants.NORMAL.equals(userType.getStatus())) {
            return null;
        }
        return userType;
    }

    /**
     * 覆盖更新用户登录域。
     *
     * @param userId      用户ID
     * @param userTypeIds 目标登录域ID集合
     * @param grantSource 新增关系的授权来源
     * @return 被移除的登录域编码列表
     */
    @DSTransactional
    @Override
    public List<String> coverUserTypes(Long userId, Collection<Long> userTypeIds, String grantSource) {
        if (ObjectUtil.isNull(userId)) {
            throw new ServiceException("用户ID不能为空");
        }
        List<SysUserTypeRelVo> current = userTypeRelMapper.selectVoListByUserId(userId);
        Set<Long> targetIds = CollUtil.isEmpty(userTypeIds) ? Set.of() : new HashSet<>(userTypeIds);
        if (CollUtil.isNotEmpty(targetIds)) {
            long count = userTypeMapper.lambda().in(SysUserType::getUserTypeId, targetIds).count();
            if (count != targetIds.size()) {
                throw new ServiceException("存在无效的登录域");
            }
        }
        List<String> removedCodes = new ArrayList<>();
        boolean changed = false;
        for (SysUserTypeRelVo rel : current) {
            if (!targetIds.contains(rel.getUserTypeId())) {
                userTypeRelMapper.deleteById(rel.getRelId());
                removedCodes.add(rel.getUserTypeCode());
                changed = true;
            }
        }
        Set<Long> currentIds = StreamUtils.toSet(current, SysUserTypeRelVo::getUserTypeId);
        for (Long userTypeId : targetIds) {
            if (!currentIds.contains(userTypeId)) {
                changed = insertUserTypeGrant(userId, userTypeId, grantSource) || changed;
            }
        }
        for (String removedCode : removedCodes) {
            clientSessionService.kickoutUserType(userId, removedCode);
        }
        if (changed) {
            openApiSessionInvalidator.invalidateByUserId(userId);
        }
        return removedCodes;
    }

    /**
     * 为用户追加一个登录域（已存在则忽略）。
     *
     * @param userId      用户ID
     * @param userTypeId  登录域ID
     * @param grantSource 授权来源
     * @return 是否新增
     */
    @Override
    @DSTransactional
    public boolean grantUserType(Long userId, Long userTypeId, String grantSource) {
        boolean inserted = insertUserTypeGrant(userId, userTypeId, grantSource);
        if (inserted) {
            openApiSessionInvalidator.invalidateByUserId(userId);
        }
        return inserted;
    }

    private boolean insertUserTypeGrant(Long userId, Long userTypeId, String grantSource) {
        boolean exist = userTypeRelMapper.lambda()
            .eq(SysUserTypeRel::getUserId, userId)
            .eq(SysUserTypeRel::getUserTypeId, userTypeId)
            .exists();
        if (exist) {
            return false;
        }
        SysUserTypeRel rel = new SysUserTypeRel();
        rel.setUserId(userId);
        rel.setUserTypeId(userTypeId);
        rel.setGrantSource(grantSource);
        rel.setStatus(SystemConstants.NORMAL);
        return userTypeRelMapper.insert(rel) > 0;
    }

    /**
     * 删除用户全部登录域关系
     *
     * @param userIds 用户ID集合
     */
    @Override
    @DSTransactional
    public void deleteByUserIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        boolean deleted = userTypeRelMapper.lambda().in(SysUserTypeRel::getUserId, userIds).delete();
        if (deleted) {
            userIds.stream().filter(ObjectUtil::isNotNull).distinct()
                .forEach(openApiSessionInvalidator::invalidateByUserId);
        }
    }

}
