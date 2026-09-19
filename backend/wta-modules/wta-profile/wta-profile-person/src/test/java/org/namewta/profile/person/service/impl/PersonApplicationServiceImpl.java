package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.port.verification.PersonVerificationService;

import org.namewta.profile.person.port.provider.PersonVerificationProviderRegistryPort;

import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.service.PersonApplicationService;
import org.namewta.profile.person.port.PersonApplicationPublicationPort;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.usecase.PersonApplicationUseCase;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.ConfigService;

import java.time.Clock;

/** 将 Mapper 装配为实际 DAO 的测试夹具；身份参数沿用正式 UseCase。 */
public class PersonApplicationServiceImpl extends PersonApplicationService
    implements PersonApplicationUseCase, PersonApplicationPublicationPort {
    public PersonApplicationServiceImpl(PersonApplicationMapper mapper,
                                        ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers,
                                        PersonVerificationService attempts, PersonWorkflowGateway workflow,
                                        ConfigService configService, Clock clock) {
        super(new PersonApplicationDao(mapper), materials, providers, attempts, workflow, configService, clock);
    }

}
