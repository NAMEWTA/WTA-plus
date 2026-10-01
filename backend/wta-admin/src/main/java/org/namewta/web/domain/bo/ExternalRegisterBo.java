package org.namewta.web.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 已验证外部身份的资料补全，不接受浏览器提交 userId、sub 或角色。 */
public record ExternalRegisterBo(
        @NotBlank String clientId,
        @NotBlank @Size(max = 100) String registrationTicket,
        @NotBlank @Size(max = 100) String transactionKey,
        @NotBlank @Size(max = 32) String phoneNumber) {}
