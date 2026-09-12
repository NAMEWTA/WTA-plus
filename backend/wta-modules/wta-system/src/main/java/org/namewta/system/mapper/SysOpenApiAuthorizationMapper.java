package org.namewta.system.mapper;

import org.apache.ibatis.annotations.Param;
import org.namewta.system.openapi.authorization.OpenApiAuthorizationPermission;
import org.namewta.system.openapi.authorization.OpenApiAuthorizationRole;
import org.namewta.system.openapi.authorization.OpenApiAuthorizationUser;

import java.util.List;

/**
 * Read-only authority projection for global OpenAPI identities.
 */
public interface SysOpenApiAuthorizationMapper {

    OpenApiAuthorizationUser selectActiveUser(@Param("userId") Long userId);

    long countLegalClients(@Param("userId") Long userId);

    List<OpenApiAuthorizationRole> selectActiveRoles(@Param("userId") Long userId);

    List<OpenApiAuthorizationPermission> selectActivePermissions(@Param("userId") Long userId);

}
