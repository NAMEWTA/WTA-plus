package org.namewta.oidc.domain.bo;

import jakarta.validation.constraints.NotBlank;

/** 密钥材料只写入，不在管理响应返回。生成请求只需要kind。 */
public record OidcKeyBo(@NotBlank String kind, String material) {}
