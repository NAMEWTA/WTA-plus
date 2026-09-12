package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;

import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.service.PersonApplicationService;
import org.namewta.profile.person.service.IPersonApplicationService;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.usecase.PersonApplicationUseCase;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.ConfigService;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

/** 生产代码使用 PersonApplicationService，本类仅作为测试兼容适配器。 */
public class PersonApplicationServiceImpl extends PersonApplicationService
    implements PersonApplicationUseCase, IPersonApplicationService {
    public PersonApplicationServiceImpl(PersonApplicationMapper mapper, JsonMapper jsonMapper,
                                        ProfileMaterialPort materials, PersonVerificationProviderRegistry providers,
                                        PersonVerificationAttemptService attempts, PersonWorkflowGateway workflow,
                                        ConfigService configService, Clock clock) {
        super(new PersonApplicationDao(mapper), jsonMapper, materials, providers, attempts, workflow, configService, clock);
    }

    @Override public org.namewta.profile.person.domain.vo.PersonApplicationVo current() {
        return current(LoginHelper.getUserId());
    }

    @Override public org.namewta.profile.person.domain.vo.PersonApplicationVo save(
        org.namewta.profile.person.domain.bo.PersonApplicationSaveBo command) {
        return save(LoginHelper.getUserId(), command);
    }

    @Override public org.namewta.profile.person.domain.vo.PersonApplicationVo submit(int expectedVersion) {
        return submit(LoginHelper.getUserId(), expectedVersion);
    }
}
