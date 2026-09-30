package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 人工任务决定只接受意见、动作和已查看的快照版本，不接受内部引擎参数。 */
public record EnterpriseTaskDecisionBo(
    @NotBlank @Pattern(regexp = "APPROVE|RETURN") String decision,
    @NotBlank @Size(max = 500) String reason,
    @Positive int snapshotVersion
) {
}
