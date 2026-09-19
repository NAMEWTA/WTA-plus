package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.port.verification.EnterpriseVerificationService;

import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProviderRegistryPort;

import org.namewta.profile.enterprise.dao.EnterpriseApplicationDao;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import org.namewta.profile.enterprise.service.EnterpriseApplicationService;
import org.namewta.profile.enterprise.port.EnterpriseApplicationPublicationPort;
import org.namewta.profile.enterprise.port.gateway.EnterpriseWorkflowGateway;
import org.namewta.profile.enterprise.usecase.EnterpriseApplicationUseCase;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.ConfigService;

import java.time.Clock;

public class EnterpriseApplicationServiceImpl extends EnterpriseApplicationService
    implements EnterpriseApplicationUseCase, EnterpriseApplicationPublicationPort {
    public EnterpriseApplicationServiceImpl(EnterpriseApplicationMapper mapper,
                                             ProfileMaterialPort materials,
                                             EnterpriseVerificationProviderRegistryPort providers,
                                             EnterpriseVerificationService attempts,
                                             EnterpriseWorkflowGateway workflow, ConfigService configService,
                                             Clock clock) {
        super(new EnterpriseApplicationDao(mapper), materials, providers, attempts, workflow,
            configService, clock);
    }

}
