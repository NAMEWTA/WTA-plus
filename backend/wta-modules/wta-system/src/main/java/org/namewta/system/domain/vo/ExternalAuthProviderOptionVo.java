package org.namewta.system.domain.vo;

import java.util.Map;

/** 接入编辑所需的最小身份源目录，不暴露完整管理选项或编辑版本。 */
public record ExternalAuthProviderOptionVo(Long id, String providerKey, String name, String protocol,
    String issuer, boolean enabled, Map<String, String> options) {
}
