package org.namewta.system.openapi.credential.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Internal projection for the super-admin user search.
 */
@Data
public class OpenApiCredentialUserRow {
    private Long userId;
    private String userName;
    private String nickName;
    private Long credentialId;
    private String appKey;
    private String appName;
    private String status;
    private LocalDateTime expiresAt;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
