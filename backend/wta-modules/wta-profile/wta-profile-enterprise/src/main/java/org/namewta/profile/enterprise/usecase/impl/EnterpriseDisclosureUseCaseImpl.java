package org.namewta.profile.enterprise.usecase.impl;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.namewta.profile.api.domain.EnterpriseDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.enterprise.service.EnterpriseDisclosureService;
import org.namewta.profile.enterprise.usecase.EnterpriseDisclosureUseCase;

/** 经服务发布企业当前档案字段，保持 API 到 Mapper 的完整分层。 */
@Service
@RequiredArgsConstructor
public class EnterpriseDisclosureUseCaseImpl implements EnterpriseDisclosureUseCase {
    private final EnterpriseDisclosureService service;

    @Override
    public EnterpriseDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields) {
        return service.findByUserId(userId, fields);
    }
}

