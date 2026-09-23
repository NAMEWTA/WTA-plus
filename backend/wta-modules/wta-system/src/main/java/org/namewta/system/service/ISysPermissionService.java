package org.namewta.system.service;

import org.namewta.system.api.domain.RoleDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户权限处理
 */
public interface ISysPermissionService {

    /**
     * 获取角色数据权限
     *
     * @param userId   用户id
     * @param clientId 客户端主键
     * @return 角色权限信息
     */
    Set<String> getRolePermission(Long userId, Long clientId);

    /**
     * 获取菜单数据权限
     *
     * @param userId   用户id
     * @param clientId 客户端主键
     * @return 菜单权限信息
     */
    Set<String> getMenuPermission(Long userId, Long clientId);

    /**
     * 根据角色列表构建数据权限角色映射
     *
     * @param roles 角色列表
     * @return key 为权限码 value 为命中的角色ID列表
     */
    Map<String, List<Long>> getDataScopeRoleMap(List<RoleDTO> roles);

}
