package org.namewta.oidc.domain.vo;

import java.util.List;

/** 发行方非机密状态。 */
public record OidcProviderVo(
        String issuer, String discoveryUrl, boolean ready, boolean enabled, List<String> scopes) {}
