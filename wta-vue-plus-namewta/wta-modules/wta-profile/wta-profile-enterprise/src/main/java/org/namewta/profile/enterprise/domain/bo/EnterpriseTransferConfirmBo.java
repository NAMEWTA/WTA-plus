package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** EnterpriseTransferConfirmBo 请求参数模型。 */
public record EnterpriseTransferConfirmBo(
    @NotBlank @Size(max = 64) String challengeId,
    @NotBlank @Pattern(regexp = "\\d{6}") String code
) {
}
