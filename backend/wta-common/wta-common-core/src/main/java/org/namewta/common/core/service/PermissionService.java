package org.namewta.common.core.service;

import java.util.Set;

/**
 * 用户权限处理
 */
public interface PermissionService {

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

}
