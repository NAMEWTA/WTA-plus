package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** EnterpriseTransferSendBo 请求参数模型。 */
public record EnterpriseTransferSendBo(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Pattern(regexp = "[0-9A-Za-z]{4}") String documentLastFour,
    @NotBlank @Size(max = 32) String phone
) {
}
