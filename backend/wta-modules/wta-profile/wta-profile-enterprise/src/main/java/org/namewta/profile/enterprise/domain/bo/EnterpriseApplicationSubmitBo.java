package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.PositiveOrZero;

/** EnterpriseApplicationSubmitBo 请求参数模型。 */
public record EnterpriseApplicationSubmitBo(@PositiveOrZero int expectedVersion) {
}
