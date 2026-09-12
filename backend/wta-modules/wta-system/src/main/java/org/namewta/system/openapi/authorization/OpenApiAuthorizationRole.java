package org.namewta.system.openapi.authorization;

import lombok.Data;

/**
 * Active default or explicit role reachable through a legal Client.
 */
@Data
public class OpenApiAuthorizationRole {

    private Long roleId;
    private String roleName;
    private String roleKey;
    private String dataScope;

}
