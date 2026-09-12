package org.namewta.system.openapi.credential.model;

/**
 * Super-admin search row with an optional safe credential summary.
 */
public record OpenApiCredentialUserSummary(
    Long userId,
    String userName,
    String nickName,
    OpenApiCredentialSummary credential
) {
}
