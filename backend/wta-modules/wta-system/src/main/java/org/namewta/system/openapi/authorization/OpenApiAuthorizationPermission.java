package org.namewta.system.openapi.authorization;

import lombok.Data;

/**
 * Active menu permission projected with the role that grants its data scope.
 */
@Data
public class OpenApiAuthorizationPermission {

    private Long roleId;
    private String permission;

}
