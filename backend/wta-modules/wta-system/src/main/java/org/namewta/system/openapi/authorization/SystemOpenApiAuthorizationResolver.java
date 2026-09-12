package org.namewta.system.openapi.authorization;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.openapi.spi.OpenApiAuthorizationResolver;
import org.namewta.system.api.domain.RoleDTO;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.mapper.SysOpenApiAuthorizationMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a read-only union of authority reachable through every legal Client.
 */
@Service
@RequiredArgsConstructor
public class SystemOpenApiAuthorizationResolver implements OpenApiAuthorizationResolver {

    private static final String MACHINE_USER_TYPE = "openapi";

    private final SysOpenApiAuthorizationMapper authorizationMapper;

    @Override
    public LoginUser resolve(Long userId) {
        if (userId == null) {
            throw unavailable();
        }
        OpenApiAuthorizationUser user = authorizationMapper.selectActiveUser(userId);
        if (user == null || authorizationMapper.countLegalClients(userId) == 0) {
            throw unavailable();
        }

        List<OpenApiAuthorizationRole> roleRows = authorizationMapper.selectActiveRoles(userId);
        List<OpenApiAuthorizationPermission> permissionRows = authorizationMapper.selectActivePermissions(userId);
        Map<Long, RoleDTO> rolesById = mapRoles(roleRows);
        Set<String> roleKeys = new LinkedHashSet<>();
        roleRows.forEach(role -> addIfText(roleKeys, role.getRoleKey()));
        Set<String> permissions = new LinkedHashSet<>();
        Map<String, List<Long>> dataScopeRoleMap = new LinkedHashMap<>();
        for (OpenApiAuthorizationPermission row : permissionRows) {
            if (row.getPermission() == null || row.getPermission().isBlank() || !rolesById.containsKey(row.getRoleId())) {
                continue;
            }
            permissions.add(row.getPermission());
            dataScopeRoleMap.computeIfAbsent(row.getPermission(), ignored -> new ArrayList<>()).add(row.getRoleId());
        }
        if (SystemConstants.SUPER_ADMIN_USER_ID.equals(userId)) {
            roleKeys.add(SystemConstants.SUPER_ADMIN_ROLE_KEY);
            permissions.add("*:*:*");
        }

        LoginUser snapshot = new LoginUser();
        snapshot.setUserId(user.getUserId());
        snapshot.setDeptId(user.getDeptId());
        snapshot.setUsername(user.getUserName());
        snapshot.setNickname(user.getNickName());
        snapshot.setDeptName(user.getDeptName());
        snapshot.setDeptCategory(user.getDeptCategory());
        snapshot.setUserType(MACHINE_USER_TYPE);
        snapshot.setClientPk(null);
        snapshot.setClientKey(null);
        snapshot.setRolePermission(Collections.unmodifiableSet(roleKeys));
        snapshot.setMenuPermission(Collections.unmodifiableSet(permissions));
        snapshot.setRoles(List.copyOf(rolesById.values()));
        snapshot.setDataScopeRoleMap(immutableDataScopeMap(dataScopeRoleMap));
        snapshot.setPosts(List.of());
        return snapshot;
    }

    private static Map<Long, RoleDTO> mapRoles(List<OpenApiAuthorizationRole> rows) {
        Map<Long, RoleDTO> roles = new LinkedHashMap<>();
        for (OpenApiAuthorizationRole row : rows) {
            if (row.getRoleId() == null) {
                continue;
            }
            RoleDTO role = new RoleDTO();
            role.setRoleId(row.getRoleId());
            role.setRoleName(row.getRoleName());
            role.setRoleKey(row.getRoleKey());
            role.setDataScope(row.getDataScope());
            roles.putIfAbsent(row.getRoleId(), role);
        }
        return roles;
    }

    private static Map<String, List<Long>> immutableDataScopeMap(Map<String, List<Long>> source) {
        Map<String, List<Long>> result = new LinkedHashMap<>();
        source.forEach((permission, roleIds) -> result.put(permission,
            roleIds.stream().distinct().toList()));
        return Collections.unmodifiableMap(result);
    }

    private static void addIfText(Set<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value);
        }
    }

    private static ServiceException unavailable() {
        return new ServiceException("OpenAPI authorization snapshot is unavailable");
    }

}
