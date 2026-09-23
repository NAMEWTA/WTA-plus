package org.namewta.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.service.PermissionService;
import org.namewta.common.core.utils.StreamUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.domain.RoleDTO;
import org.namewta.system.service.ISysMenuService;
import org.namewta.system.service.ISysPermissionService;
import org.namewta.system.service.ISysRoleService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 用户权限处理
 */
@RequiredArgsConstructor
@Service
public class SysPermissionServiceImpl implements ISysPermissionService, PermissionService {

    private final ISysRoleService roleService;
    private final ISysMenuService menuService;

    /**
     * 获取角色数据权限
     *
     * @param userId   用户id
     * @param clientId 客户端主键
     * @return 角色权限信息
     */
    @Override
    public Set<String> getRolePermission(Long userId, Long clientId) {
        Set<String> roles = new HashSet<>();
        // 管理员拥有所有权限标识，角色加载仍按当前 Client 收敛
        if (LoginHelper.isSuperAdmin(userId)) {
            roles.add(SystemConstants.SUPER_ADMIN_ROLE_KEY);
        }
        roles.addAll(roleService.selectRolePermissionByUserId(userId, clientId));
        return roles;
    }

    /**
     * 获取菜单数据权限
     *
     * @param userId   用户id
     * @param clientId 客户端主键
     * @return 菜单权限信息
     */
    @Override
    public Set<String> getMenuPermission(Long userId, Long clientId) {
        Set<String> perms = new HashSet<>();
        // 管理员拥有所有权限，菜单加载仍按当前 Client 收敛
        if (LoginHelper.isSuperAdmin(userId)) {
            perms.add("*:*:*");
        }
        perms.addAll(menuService.selectMenuPermsByUserId(userId, clientId));
        return perms;
    }

    /**
     * 按权限标识汇总具备数据权限的角色集合。
     *
     * @param roles 角色传输对象列表
     * @return key 为权限标识、value 为拥有该权限的角色主键列表
     */
    @Override
    public Map<String, List<Long>> getDataScopeRoleMap(List<RoleDTO> roles) {
        if (CollUtil.isEmpty(roles)) {
            return Map.of();
        }
        List<Long> roleIds = StreamUtils.toList(roles, RoleDTO::getRoleId);
        Map<Long, Set<String>> permsRoleIds = menuService.selectMenuPermsByRoleIds(roleIds);
        Map<String, List<Long>> rolePermsMap = new LinkedHashMap<>();
        permsRoleIds.forEach((roleId, perms) ->
            perms.forEach(perm ->
                rolePermsMap.computeIfAbsent(perm, key -> new ArrayList<>()).add(roleId)
            )
        );
        return rolePermsMap;
    }
}
