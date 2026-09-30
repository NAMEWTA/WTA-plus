package org.namewta.profile.enterprise.adapter.api;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.namewta.profile.api.ProfileDisclosureContributor;
import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.enterprise.usecase.EnterpriseDisclosureUseCase;

/** 通过用例提供企业档案字段发布，不直接持有服务或持久化入口。 */
@Component
@RequiredArgsConstructor
public class EnterpriseDisclosureContributor implements ProfileDisclosureContributor {
    private final EnterpriseDisclosureUseCase useCase;

    @Override
    public ProfileType profileType() {
        return ProfileType.ENTERPRISE;
    }

    @Override
    public ProfileDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields) {
        return new ProfileDisclosure(userId, null, useCase.findByUserId(userId, fields));
    }
}

