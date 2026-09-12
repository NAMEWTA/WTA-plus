package org.namewta.system.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.common.core.utils.StreamUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.RoleService;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.SysMenu;
import org.namewta.system.domain.SysRole;
import org.namewta.system.domain.SysRoleDept;
import org.namewta.system.domain.SysRoleMenu;
import org.namewta.system.domain.SysUserRole;
import org.namewta.system.domain.SysUserType;
import org.namewta.system.domain.SysUserTypeRel;
import org.namewta.system.domain.bo.SysRoleBo;
import org.namewta.system.domain.vo.SysRoleVo;
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysMenuMapper;
import org.namewta.system.mapper.SysRoleDeptMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.mapper.SysRoleMenuMapper;
import org.namewta.system.mapper.SysUserRoleMapper;
import org.namewta.system.mapper.SysUserTypeRelMapper;
import org.namewta.system.mapper.SysUserTypeMapper;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysClientDefaultRoleResolverService;
import org.namewta.system.service.ISysRoleService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 角色 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRoleServiceImpl implements ISysRoleService, RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysMenuMapper menuMapper;
    private final SysClientMapper clientMapper;
    private final SysUserTypeMapper userTypeMapper;
    private final ClientSessionService clientSessionService;
    private final ISysUserTypeRelService userTypeRelService;
    private final ISysClientDefaultRoleResolverService defaultRoleResolverService;
    private final SysUserTypeRelMapper userTypeRelMapper;
    private OpenApiMachineSessionInvalidator openApiSessionInvalidator = ignored -> 0;

    @Autowired(required = false)
    void setOpenApiSessionInvalidator(OpenApiMachineSessionInvalidator openApiSessionInvalidator) {
        this.openApiSessionInvalidator = Objects.requireNonNull(openApiSessionInvalidator);
    }

    /**
     * 分页查询角色列表
     *
     * @param role      查询条件
     * @param pageQuery 分页参数
     * @return 角色分页列表
     */
    @Override
    public PageResult<SysRoleVo> selectPageRoleList(SysRoleBo role, PageQuery pageQuery) {
        SysClient client = requireActiveClient(role.getClientId());
        Page<SysRoleVo> page = roleMapper.selectPageRoleList(pageQuery.build(), this.buildQueryWrapper(role));
        markClientDefault(page.getRecords(), client);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 根据条件查询角色数据
     *
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    @Override
    public List<SysRoleVo> selectRoleList(SysRoleBo role) {
        SysClient client = requireActiveClient(role.getClientId());
        List<SysRoleVo> roles = roleMapper.selectRoleList(this.buildQueryWrapper(role));
        markClientDefault(roles, client);
        return roles;
    }

    /**
     * 构造角色列表查询条件。
     *
     * @param bo 角色筛选条件
     * @return 包含名称、权限字符、状态和创建时间区间的查询包装器
     */
    private Wrapper<SysRole> buildQueryWrapper(SysRoleBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SysRole.class)
            .eqIfPresent(SysRole::getRoleId, bo.getRoleId())
            .eqIfPresent(SysRole::getClientId, bo.getClientId())
            .likeIfText(SysRole::getRoleName, bo.getRoleName())
            .eqIfText(SysRole::getStatus, bo.getStatus())
            .likeIfText(SysRole::getRoleKey, bo.getRoleKey())
            .betweenParams(SysRole::getCreateTime, params, "beginTime", "endTime")
            .orderByAsc(SysRole::getRoleSort, SysRole::getCreateTime)
            .build();
    }

    /**
     * 根据用户ID查询角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRolesByUserId(Long userId, Long clientId) {
        SysClient client = requireActiveClient(clientId);
        List<SysRoleVo> roles = new ArrayList<>(roleMapper.selectRolesByUserId(userId, clientId));
        mergeDefaultRole(roles, client);
        return roles;
    }

    /**
     * 根据用户ID和客户端查询角色列表(包含被授权状态)
     *
     * @param userId   用户ID
     * @param clientId 客户端主键
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRolesAuthByUserId(Long userId, Long clientId) {
        List<SysRoleVo> userRoles = roleMapper.selectRolesByUserId(userId, clientId);
        List<SysRoleVo> roles = selectRoleAll(clientId);
        Set<Long> userRoleIds = StreamUtils.toSet(userRoles, SysRoleVo::getRoleId);
        for (SysRoleVo role : roles) {
            if (userRoleIds.contains(role.getRoleId())) {
                role.setFlag(true);
            }
        }
        return roles;
    }

    /**
     * 根据用户ID和客户端查询权限
     *
     * @param userId   用户ID
     * @param clientId 客户端主键
     * @return 权限列表
     */
    @Override
    public Set<String> selectRolePermissionByUserId(Long userId, Long clientId) {
        List<SysRoleVo> perms = selectRolesByUserId(userId, clientId);
        Set<String> permsSet = new HashSet<>();
        for (SysRoleVo perm : perms) {
            if (ObjectUtil.isNotNull(perm)) {
                permsSet.addAll(StringUtils.splitList(perm.getRoleKey().trim()));
            }
        }
        return permsSet;
    }

    /**
     * 查询指定客户端下的全部角色
     *
     * @param clientId 客户端主键
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRoleAll(Long clientId) {
        SysRoleBo bo = new SysRoleBo();
        bo.setClientId(clientId);
        return this.selectRoleList(bo);
    }

    /**
     * 根据用户ID和客户端获取角色选择框列表。
     *
     * @param userId   用户ID
     * @param clientId 客户端主键
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId, Long clientId) {
        requireActiveClient(clientId);
        List<SysRoleVo> list = roleMapper.selectRolesByUserId(userId, clientId);
        return StreamUtils.toList(list, SysRoleVo::getRoleId);
    }

    /**
     * 通过角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    @Override
    public SysRoleVo selectRoleById(Long roleId) {
        return roleMapper.selectRoleById(roleId);
    }

    /**
     * 通过角色ID串查询角色
     *
     * @param roleIds 角色ID串
     * @return 角色列表信息
     */
    @Override
    public List<SysRoleVo> selectRoleByIds(Collection<Long> roleIds) {
        return roleMapper.selectRoleList(roleMapper.lambda()
            .eq(SysRole::getStatus, SystemConstants.NORMAL)
            .inIfNotEmpty(SysRole::getRoleId, roleIds)
            .build());
    }

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleNameUnique(SysRoleBo role) {
        fillClientIdIfAbsent(role);
        boolean exist = roleMapper.lambda()
            .eq(SysRole::getRoleName, role.getRoleName())
            .eq(SysRole::getClientId, role.getClientId())
            .neIfPresent(SysRole::getRoleId, role.getRoleId())
            .exists();
        return !exist;
    }

    /**
     * 校验角色权限是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleKeyUnique(SysRoleBo role) {
        fillClientIdIfAbsent(role);
        boolean exist = roleMapper.lambda()
            .eq(SysRole::getRoleKey, role.getRoleKey())
            .eq(SysRole::getClientId, role.getClientId())
            .neIfPresent(SysRole::getRoleId, role.getRoleId())
            .exists();
        return !exist;
    }

    /**
     * 校验角色是否允许操作
     *
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(SysRoleBo role) {
        if (ObjectUtil.isNotNull(role.getRoleId()) && SystemConstants.SUPER_ADMIN_ROLE_ID.equals(role.getRoleId())) {
            throw new ServiceException("不允许操作超级管理员角色");
        }
        String[] keys = new String[]{SystemConstants.SUPER_ADMIN_ROLE_KEY};
        // 新增不允许使用 管理员标识符
        if (ObjectUtil.isNull(role.getRoleId())
            && StringUtils.equalsAny(role.getRoleKey(), keys)) {
            throw new ServiceException("不允许使用系统内置管理员角色标识符!");
        }
        // 修改不允许修改 管理员标识符
        if (ObjectUtil.isNotNull(role.getRoleId())) {
            SysRole sysRole = roleMapper.selectById(role.getRoleId());
            // 如果标识符不相等 判断为修改了管理员标识符
            if (!StringUtils.equals(sysRole.getRoleKey(), role.getRoleKey())) {
                if (StringUtils.equalsAny(sysRole.getRoleKey(), keys)) {
                    throw new ServiceException("不允许修改系统内置管理员角色标识符!");
                } else if (StringUtils.equalsAny(role.getRoleKey(), keys)) {
                    throw new ServiceException("不允许使用系统内置管理员角色标识符!");
                }
            }
        }
    }

    /**
     * 编辑时若未传客户端，则回填库中已有归属，保证 Client 内唯一校验。
     *
     * @param role 角色信息
     */
    private void fillClientIdIfAbsent(SysRoleBo role) {
        if (ObjectUtil.isNotNull(role.getClientId()) || ObjectUtil.isNull(role.getRoleId())) {
            return;
        }
        SysRole dbRole = roleMapper.selectById(role.getRoleId());
        if (ObjectUtil.isNotNull(dbRole)) {
            role.setClientId(dbRole.getClientId());
        }
    }

    /**
     * 校验角色是否有数据权限
     *
     * @param roleId 角色id
     */
    @Override
    public void checkRoleDataScope(Long roleId) {
        if (ObjectUtil.isNull(roleId)) {
            return;
        }
        this.checkRoleDataScope(Collections.singletonList(roleId));
    }

    /**
     * 校验角色是否有数据权限
     *
     * @param roleIds 角色ID列表（支持传单个ID）
     */
    @Override
    public void checkRoleDataScope(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds) || LoginHelper.isSuperAdmin()) {
            return;
        }
        long count = roleMapper.selectRoleCount(roleIds);
        if (count != roleIds.size()) {
            throw new ServiceException("没有权限访问部分角色数据！");
        }
    }

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public long countUserRoleByRoleId(Long roleId) {
        return userRoleMapper.lambda().eq(SysUserRole::getRoleId, roleId).count();
    }

    /**
     * 客户端默认角色未写入 sys_user_role，禁止停用或删除以免悬挂。
     *
     * @param roleId 角色ID
     * @param action 操作描述
     */
    private void checkNotClientDefaultRole(Long roleId, String action) {
        boolean used = clientMapper.lambda()
            .eq(SysClient::getDefaultRoleId, roleId)
            .exists();
        if (used) {
            throw new ServiceException("角色已被客户端设为默认角色，不能" + action + "!");
        }
    }

    /**
     * 新增保存角色信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public int insertRole(SysRoleBo bo) {
        SysRole role = MapstructUtils.convert(bo, SysRole.class);
        if (ObjectUtil.isNull(role.getClientId())) {
            throw new ServiceException("客户端不能为空");
        }
        // 新增角色信息
        roleMapper.insert(role);
        bo.setRoleId(role.getRoleId());
        return insertRoleMenu(bo);
    }

    /**
     * 修改角色基础信息（不更新菜单与数据权限）。
     *
     * @param bo 角色信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public int updateRoleBaseInfo(SysRoleBo bo) {
        SysRole role = MapstructUtils.convert(bo, SysRole.class);
        SysRole dbRole = roleMapper.selectById(role.getRoleId());
        if (ObjectUtil.isNotNull(dbRole)) {
            role.setClientId(dbRole.getClientId());
        }

        if (SystemConstants.DISABLE.equals(role.getStatus())) {
            checkNotClientDefaultRole(role.getRoleId(), "禁用");
        }
        if (SystemConstants.DISABLE.equals(role.getStatus()) && this.countUserRoleByRoleId(role.getRoleId()) > 0) {
            throw new ServiceException("角色已分配，不能禁用!");
        }
        Set<Long> affectedUserIds = hasAuthorizationChange(dbRole, role)
            ? findAffectedUserIdsByRoleIds(Set.of(role.getRoleId())) : Set.of();
        // 仅更新角色基础字段，避免影响权限分配。
        int rows = roleMapper.updateById(role);
        if (rows > 0) {
            invalidateUsers(affectedUserIds);
        }
        return rows;
    }

    /**
     * 修改角色权限信息（菜单权限 + 数据权限）。
     *
     * @param bo 角色权限参数
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, key = "#bo.roleId")
    @Override
    @DSTransactional
    public int updateRolePermission(SysRoleBo bo) {
        SysRole role = MapstructUtils.convert(bo, SysRole.class);
        SysRole dbRole = roleMapper.selectById(role.getRoleId());
        if (ObjectUtil.isNotNull(dbRole)) {
            role.setClientId(dbRole.getClientId());
        }
        Set<Long> affectedUserIds = findAffectedUserIdsByRoleIds(Set.of(role.getRoleId()));
        // 更新权限相关配置字段（数据范围、树联动）。
        roleMapper.updateById(role);
        // 先清理旧菜单权限，再重建。
        roleMenuMapper.lambda().eq(SysRoleMenu::getRoleId, role.getRoleId()).delete();
        insertRoleMenu(bo);
        // 先清理旧数据权限，再按当前配置重建。
        roleDeptMapper.lambda().eq(SysRoleDept::getRoleId, role.getRoleId()).delete();
        int rows = insertRoleDept(bo);
        invalidateUsers(affectedUserIds);
        return rows;
    }

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态
     * @return 结果
     */
    @Override
    @DSTransactional
    public int updateRoleStatus(Long roleId, String status) {
        if (SystemConstants.DISABLE.equals(status)) {
            checkNotClientDefaultRole(roleId, "禁用");
        }
        if (SystemConstants.DISABLE.equals(status) && this.countUserRoleByRoleId(roleId) > 0) {
            throw new ServiceException("角色已分配，不能禁用!");
        }
        SysRole current = roleMapper.selectById(roleId);
        Set<Long> affectedUserIds = ObjectUtil.isNotNull(current) && !ObjectUtil.equal(current.getStatus(), status)
            ? findAffectedUserIdsByRoleIds(Set.of(roleId)) : Set.of();
        int rows = roleMapper.lambda()
            .set(SysRole::getStatus, status)
            .eq(SysRole::getRoleId, roleId)
            .updateCount();
        if (rows > 0) {
            invalidateUsers(affectedUserIds);
        }
        return rows;
    }


    /**
     * 新增角色菜单信息
     *
     * @param role 角色对象
     */
    private int insertRoleMenu(SysRoleBo role) {
        int rows = 1;
        if (role.getMenuIds() == null || role.getMenuIds().length == 0) {
            return rows;
        }
        SysRole dbRole = roleMapper.selectById(role.getRoleId());
        Long clientId = dbRole == null ? role.getClientId() : dbRole.getClientId();
        List<Long> menuIds = Arrays.asList(role.getMenuIds());
        long sameClientCount = menuMapper.lambda()
            .in(SysMenu::getMenuId, menuIds)
            .eq(SysMenu::getClientId, clientId)
            .count();
        if (sameClientCount != menuIds.size()) {
            throw new ServiceException("菜单必须属于当前角色所在客户端");
        }
        List<SysRoleMenu> list = new ArrayList<>();
        for (Long menuId : role.getMenuIds()) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(role.getRoleId());
            rm.setMenuId(menuId);
            list.add(rm);
        }
        if (CollUtil.isNotEmpty(list)) {
            rows = roleMenuMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * 新增角色部门信息(数据权限)
     *
     * @param role 角色对象
     */
    private int insertRoleDept(SysRoleBo role) {
        int rows = 1;
        // 新增角色与部门（数据权限）管理
        List<SysRoleDept> list = new ArrayList<>();
        for (Long deptId : role.getDeptIds()) {
            SysRoleDept rd = new SysRoleDept();
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            list.add(rd);
        }
        if (CollUtil.isNotEmpty(list)) {
            rows = roleDeptMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * 通过角色ID删除角色
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, key = "#roleId")
    @Override
    @DSTransactional
    public int deleteRoleById(Long roleId) {
        checkNotClientDefaultRole(roleId, "删除");
        Set<Long> affectedUserIds = findAffectedUserIdsByRoleIds(Set.of(roleId));
        // 删除角色与菜单关联
        roleMenuMapper.lambda().eq(SysRoleMenu::getRoleId, roleId).delete();
        // 删除角色与部门关联
        roleDeptMapper.lambda().eq(SysRoleDept::getRoleId, roleId).delete();
        int rows = roleMapper.deleteById(roleId);
        if (rows > 0) {
            invalidateUsers(affectedUserIds);
        }
        return rows;
    }

    /**
     * 批量删除角色信息
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, allEntries = true)
    @Override
    @DSTransactional
    public int deleteRoleByIds(Collection<Long> roleIds) {
        this.checkRoleDataScope(roleIds);
        List<SysRole> roles = roleMapper.selectByIds(roleIds);
        for (SysRole role : roles) {
            checkRoleAllowed(BeanUtil.toBean(role, SysRoleBo.class));
            checkNotClientDefaultRole(role.getRoleId(), "删除");
            if (countUserRoleByRoleId(role.getRoleId()) > 0) {
                throw new ServiceException(String.format("%1$s已分配，不能删除!", role.getRoleName()));
            }
        }
        Set<Long> affectedUserIds = findAffectedUserIdsByRoleIds(roleIds);
        // 删除角色与菜单关联
        roleMenuMapper.lambda().in(SysRoleMenu::getRoleId, roleIds).delete();
        // 删除角色与部门关联
        roleDeptMapper.lambda().in(SysRoleDept::getRoleId, roleIds).delete();
        int rows = roleMapper.deleteByIds(roleIds);
        if (rows > 0) {
            invalidateUsers(affectedUserIds);
        }
        return rows;
    }

    /**
     * 取消授权用户角色
     *
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public int deleteAuthUser(SysUserRole userRole) {
        if (LoginHelper.getUserId().equals(userRole.getUserId())) {
            throw new ServiceException("不允许修改当前用户角色!");
        }
        SysRole role = requireRoleWithClient(userRole.getRoleId());
        int rows = userRoleMapper.lambda()
            .eq(SysUserRole::getRoleId, userRole.getRoleId())
            .eq(SysUserRole::getUserId, userRole.getUserId())
            .deleteCount();
        if (rows > 0) {
            clientSessionService.kickoutUserClient(userRole.getUserId(), role.getClientId());
            openApiSessionInvalidator.invalidateByUserId(userRole.getUserId());
        }
        return rows;
    }

    /**
     * 批量取消授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    @Override
    @DSTransactional
    public int deleteAuthUsers(Long roleId, Collection<Long> userIds) {
        if (userIds.contains(LoginHelper.getUserId())) {
            throw new ServiceException("不允许修改当前用户角色!");
        }
        SysRole role = requireRoleWithClient(roleId);
        int rows = userRoleMapper.lambda()
            .eq(SysUserRole::getRoleId, roleId)
            .in(SysUserRole::getUserId, userIds)
            .deleteCount();
        if (rows > 0) {
            for (Long userId : userIds) {
                clientSessionService.kickoutUserClient(userId, role.getClientId());
            }
            invalidateUsers(userIds);
        }
        return rows;
    }

    /**
     * 批量选择授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要授权的用户数据ID
     * @return 结果
     */
    @Override
    @DSTransactional
    public int insertAuthUsers(Long roleId, Collection<Long> userIds) {
        // 新增用户与角色管理
        int rows = 1;
        if (userIds.contains(LoginHelper.getUserId())) {
            throw new ServiceException("不允许修改当前用户角色!");
        }
        SysRole role = requireRoleWithClient(roleId);
        validateUsersHaveRoleClientType(role, userIds);
        List<SysUserRole> list = StreamUtils.toList(userIds, userId -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        });
        if (CollUtil.isNotEmpty(list)) {
            rows = userRoleMapper.insertBatch(list) ? list.size() : 0;
        }
        if (rows > 0) {
            for (Long userId : userIds) {
                clientSessionService.kickoutUserClient(userId, role.getClientId());
            }
            invalidateUsers(userIds);
        }
        return rows;
    }

    private boolean hasAuthorizationChange(SysRole current, SysRole update) {
        if (ObjectUtil.isNull(current)) {
            return false;
        }
        return changed(current.getRoleKey(), update.getRoleKey())
            || changed(current.getStatus(), update.getStatus())
            || changed(current.getDataScope(), update.getDataScope())
            || changed(current.getMenuCheckStrictly(), update.getMenuCheckStrictly())
            || changed(current.getDeptCheckStrictly(), update.getDeptCheckStrictly())
            || changed(current.getClientId(), update.getClientId());
    }

    private boolean changed(Object current, Object update) {
        return ObjectUtil.isNotNull(update) && !ObjectUtil.equal(current, update);
    }

    private Set<Long> findAffectedUserIdsByRoleIds(Collection<Long> roleIds) {
        Set<Long> ids = new LinkedHashSet<>();
        roleIds.stream().filter(Objects::nonNull).forEach(ids::add);
        if (ids.isEmpty()) {
            return Set.of();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        userRoleMapper.lambda().in(SysUserRole::getRoleId, ids).list().stream()
            .map(SysUserRole::getUserId)
            .filter(Objects::nonNull)
            .forEach(userIds::add);
        Set<Long> userTypeIds = new LinkedHashSet<>();
        clientMapper.lambda()
            .in(SysClient::getDefaultRoleId, ids)
            .eq(SysClient::getStatus, SystemConstants.NORMAL)
            .list()
            .stream()
            .map(SysClient::getUserTypeId)
            .filter(Objects::nonNull)
            .forEach(userTypeIds::add);
        if (!userTypeIds.isEmpty()) {
            userTypeRelMapper.lambda()
                .in(SysUserTypeRel::getUserTypeId, userTypeIds)
                .eq(SysUserTypeRel::getStatus, SystemConstants.NORMAL)
                .list()
                .stream()
                .map(SysUserTypeRel::getUserId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        }
        return userIds;
    }

    private void invalidateUsers(Collection<Long> userIds) {
        userIds.stream().filter(Objects::nonNull).distinct()
            .forEach(openApiSessionInvalidator::invalidateByUserId);
    }

    private SysRole requireRoleWithClient(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        if (role.getClientId() == null) {
            throw new ServiceException("角色未关联客户端");
        }
        return role;
    }

    /**
     * 根据角色ID清除该角色关联的所有在线用户的登录状态（踢出在线用户）
     *
     * <p>
     * 先判断角色是否绑定用户，若无绑定则直接返回
     * 然后遍历当前所有在线Token，查找拥有该角色的用户并强制登出
     * 注意：在线用户量过大时，操作可能导致 Redis 阻塞，需谨慎调用
     * </p>
     *
     * @param roleId 角色ID
     */
    @Override
    public void cleanOnlineUserByRole(Long roleId) {
        // 如果角色未绑定用户 直接返回
        Long num = userRoleMapper.lambda().eq(SysUserRole::getRoleId, roleId).count();
        if (num == 0) {
            return;
        }
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        if (CollUtil.isEmpty(keys)) {
            return;
        }
        // 角色关联的在线用户量过大会导致redis阻塞卡顿 谨慎操作
        keys.parallelStream().forEach(key -> {
            String token = StringUtils.substringAfterLast(key, StringUtils.COLON);
            // 如果已经过期则跳过
            if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) < -1) {
                return;
            }
            LoginUser loginUser = LoginHelper.getLoginUser(token);
            if (ObjectUtil.isNull(loginUser) || CollUtil.isEmpty(loginUser.getRoles())) {
                return;
            }
            if (loginUser.getRoles().stream().anyMatch(r -> r.getRoleId().equals(roleId))) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (NotLoginException ignored) {
                }
            }
        });
    }

    /**
     * 根据用户ID列表清除对应在线用户的登录状态（踢出指定用户）
     *
     * <p>
     * 遍历当前所有在线Token，匹配用户ID列表中的用户，强制登出
     * 注意：在线用户量过大时，操作可能导致 Redis 阻塞，需谨慎调用
     * </p>
     *
     * @param userIds 需要清除的用户ID列表
     */
    @Override
    public void cleanOnlineUser(Collection<Long> userIds) {
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        if (CollUtil.isEmpty(keys)) {
            return;
        }
        // 角色关联的在线用户量过大会导致redis阻塞卡顿 谨慎操作
        keys.parallelStream().forEach(key -> {
            String token = StringUtils.substringAfterLast(key, StringUtils.COLON);
            // 如果已经过期则跳过
            if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) < -1) {
                return;
            }
            LoginUser loginUser = LoginHelper.getLoginUser(token);
            if (ObjectUtil.isNull(loginUser)) {
                return;
            }
            if (userIds.contains(loginUser.getUserId())) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (NotLoginException ignored) {
                }
            }
        });
    }

    /**
     * 根据角色 ID 列表查询角色名称映射关系
     *
     * @param roleIds 角色 ID 列表
     * @return Map，其中 key 为角色 ID，value 为对应的角色名称
     */
    @Override
    public Map<Long, String> selectRoleNamesByIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyMap();
        }
        List<SysRole> list = roleMapper.lambda()
            .select(SysRole::getRoleId, SysRole::getRoleName)
            .in(SysRole::getRoleId, roleIds)
            .list();
        return StreamUtils.toMap(list, SysRole::getRoleId, SysRole::getRoleName);
    }

    /**
     * 在有效角色中合并客户端默认角色，不写入 sys_user_role。
     *
     * @param roles  用户显式角色
     * @param client 已校验的客户端
     */
    private void mergeDefaultRole(List<SysRoleVo> roles, SysClient client) {
        SysRoleVo defaultRole = defaultRoleResolverService.resolveRole(client);
        if (ObjectUtil.isNull(defaultRole)) {
            return;
        }
        for (SysRoleVo role : roles) {
            if (defaultRole.getRoleId().equals(role.getRoleId())) {
                role.setClientDefault(true);
                return;
            }
        }
        defaultRole.setClientDefault(true);
        roles.add(defaultRole);
    }

    /**
     * 标记当前客户端的默认角色，便于管理端只读展示且不写入 sys_user_role。
     *
     * @param roles  角色列表
     * @param client 已校验的客户端
     */
    private void markClientDefault(List<SysRoleVo> roles, SysClient client) {
        SysRoleVo defaultRole = defaultRoleResolverService.resolveRole(client);
        if (ObjectUtil.isNull(defaultRole) || CollUtil.isEmpty(roles)) {
            return;
        }
        for (SysRoleVo role : roles) {
            role.setClientDefault(defaultRole.getRoleId().equals(role.getRoleId()));
        }
    }

    /**
     * 校验客户端及其登录域可用于角色管理和运行时授权。
     */
    private SysClient requireActiveClient(Long clientId) {
        if (ObjectUtil.isNull(clientId)) {
            throw new ServiceException("请选择客户端");
        }
        SysClient client = clientMapper.selectById(clientId);
        if (ObjectUtil.isNull(client) || !SystemConstants.NORMAL.equals(client.getStatus())) {
            throw new ServiceException("客户端不存在或已停用");
        }
        if (ObjectUtil.isNull(client.getUserTypeId())) {
            throw new ServiceException("客户端未配置登录域");
        }
        SysUserType userType = userTypeMapper.selectById(client.getUserTypeId());
        if (ObjectUtil.isNull(userType) || !SystemConstants.NORMAL.equals(userType.getStatus())) {
            throw new ServiceException("客户端登录域不存在或已停用");
        }
        return client;
    }

    /**
     * 授权角色前校验用户已具备该角色客户端要求的登录域。
     *
     * @param role    角色
     * @param userIds 用户ID集合
     */
    private void validateUsersHaveRoleClientType(SysRole role, Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        if (ObjectUtil.isNull(role) || !SystemConstants.NORMAL.equals(role.getStatus())
            || ObjectUtil.isNull(role.getClientId())) {
            throw new ServiceException("角色不存在、已停用或未归属客户端");
        }
        SysClient client = requireActiveClient(role.getClientId());
        if (role.getRoleId().equals(client.getDefaultRoleId())) {
            throw new ServiceException("客户端默认角色由系统自动授予，不能显式分配");
        }
        for (Long userId : userIds) {
            if (!userTypeRelService.hasUserType(userId, client.getUserTypeId())) {
                throw new ServiceException("用户不具备该角色所属客户端的登录域");
            }
        }
    }

}
