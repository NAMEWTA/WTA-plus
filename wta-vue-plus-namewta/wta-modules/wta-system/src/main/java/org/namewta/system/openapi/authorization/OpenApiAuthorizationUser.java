package org.namewta.system.openapi.authorization;

import lombok.Data;

/**
 * Minimal active-user projection used to build an OpenAPI authorization snapshot.
 */
@Data
public class OpenApiAuthorizationUser {

    private Long userId;
    private Long deptId;
    private String userName;
    private String nickName;
    private String deptName;
    private String deptCategory;

}
