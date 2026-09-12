package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.service.EnterpriseVerificationAttemptService;

import org.namewta.profile.enterprise.adapter.provider.EnterpriseVerificationProviderRegistry;

import org.namewta.profile.enterprise.dao.EnterpriseApplicationDao;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import org.namewta.profile.enterprise.service.EnterpriseApplicationService;
import org.namewta.profile.enterprise.service.IEnterpriseApplicationService;
import org.namewta.profile.enterprise.port.gateway.EnterpriseWorkflowGateway;
import org.namewta.profile.enterprise.usecase.EnterpriseApplicationUseCase;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.ConfigService;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

public class EnterpriseApplicationServiceImpl extends EnterpriseApplicationService
    implements EnterpriseApplicationUseCase, IEnterpriseApplicationService {
    public EnterpriseApplicationServiceImpl(EnterpriseApplicationMapper mapper, JsonMapper jsonMapper,
                                             ProfileMaterialPort materials,
                                             EnterpriseVerificationProviderRegistry providers,
                                             EnterpriseVerificationAttemptService attempts,
                                             EnterpriseWorkflowGateway workflow, ConfigService configService,
                                             Clock clock) {
        super(new EnterpriseApplicationDao(mapper), jsonMapper, materials, providers, attempts, workflow,
            configService, clock);
    }

    @Override public org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationVo current() {
        return current(LoginHelper.getUserId());
    }
    @Override public org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationVo save(
        org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSaveBo command) {
        return save(LoginHelper.getUserId(), command);
    }
    @Override public org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationVo submit(int expectedVersion) {
        return submit(LoginHelper.getUserId(), expectedVersion);
    }
}
