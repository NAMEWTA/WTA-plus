package org.namewta.profile.person.usecase.impl;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.namewta.profile.api.domain.PersonDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.person.service.PersonDisclosureService;
import org.namewta.profile.person.usecase.PersonDisclosureUseCase;

/** 经服务发布个人当前档案字段，保持 API 到 Mapper 的完整分层。 */
@Service
@RequiredArgsConstructor
public class PersonDisclosureUseCaseImpl implements PersonDisclosureUseCase {
    private final PersonDisclosureService service;

    @Override
    public PersonDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields) {
        return service.findByUserId(userId, fields);
    }
}

