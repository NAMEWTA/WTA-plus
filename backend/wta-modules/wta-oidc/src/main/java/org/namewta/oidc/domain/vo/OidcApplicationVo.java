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
        LocalDateTime createTime,
        String backchannelLogoutUri,
        boolean backchannelLogoutSessionRequired) {
    /** 保留原Java响应构造器，旧调用方默认未登记后端退出地址。 */
    public OidcApplicationVo(
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
            LocalDateTime createTime) {
        this(
                applicationId,
                name,
                clientId,
                redirectUris,
                postLogoutRedirectUris,
                allowedFields,
                allowedScopes,
                clientAuthenticationMethod,
                pkceRequired,
                enabled,
                version,
                createTime,
                null,
                true);
    }
}
