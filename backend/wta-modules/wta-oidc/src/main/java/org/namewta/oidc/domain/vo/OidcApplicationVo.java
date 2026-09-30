package org.namewta.oidc.domain.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 非机密应用详情，不含任何凭据摘要。 */
public record OidcApplicationVo(
        String applicationId,
        String name,
        String clientId,
        List<String> redirectUris,
        List<String> postLogoutRedirectUris,
        List<String> allowedFields,
        List<String> allowedScopes,
        String clientAuthenticationMethod,
        boolean pkceRequired,
        boolean enabled,
        Integer version,
        LocalDateTime createTime) {}
