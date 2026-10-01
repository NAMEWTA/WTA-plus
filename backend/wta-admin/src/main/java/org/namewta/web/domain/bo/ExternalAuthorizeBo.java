package org.namewta.web.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 发起本站第三方登录或当前用户绑定；目的由服务端事务固定。 */
public record ExternalAuthorizeBo(
        @NotBlank @Size(max = 100) String providerKey,
        @NotBlank @Size(max = 64) String clientId,
        @Pattern(regexp = "LOGIN|BIND") @NotBlank String purpose,
        @Size(max = 2048) String returnPath) {}
