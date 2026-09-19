package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.port.verification.PersonVerificationService;

import org.namewta.profile.person.port.provider.PersonVerificationProviderRegistryPort;

import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.dao.PersonRebindDao;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.mapper.PersonRebindMapper;
import org.namewta.profile.person.service.PersonRebindService;
import org.namewta.profile.person.port.notification.PersonRebindNotificationPort;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.usecase.PersonRebindUseCase;
import org.namewta.profile.person.domain.application.PersonRebindProcessCommand;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.UserService;

import java.time.Clock;

public class PersonRebindServiceImpl extends PersonRebindService implements PersonRebindUseCase {
    public PersonRebindServiceImpl(PersonRebindMapper mapper, PersonApplicationMapper applicationMapper,
                                   ProfileMaterialPort materials,
                                   PersonVerificationProviderRegistryPort providers,
                                   PersonVerificationService attempts,
                                   PersonWorkflowGateway workflow, UserService users, Clock clock) {
        super(new PersonRebindDao(mapper), new PersonApplicationDao(applicationMapper), materials,
            providers, attempts, workflow, users, clock);
    }

    /** 创建带工作流事件副作用的测试适配器。 */
    public PersonRebindServiceImpl(PersonRebindMapper mapper, PersonApplicationMapper applicationMapper,
                                   ProfileMaterialPort materials,
                                   PersonVerificationProviderRegistryPort providers,
                                   PersonVerificationService attempts,
                                   PersonWorkflowGateway workflow, UserService users, Clock clock,
                                   org.namewta.system.api.ConfigService configService,
                                   PersonRebindNotificationPort notifications,
                                   org.springframework.context.ApplicationEventPublisher events) {
        super(new PersonRebindDao(mapper), new PersonApplicationDao(applicationMapper), materials,
            providers, attempts, workflow, users, clock, configService, notifications, events);
    }

    @Override public java.util.Optional<org.namewta.profile.person.domain.application.PersonRebindPublication>
        publishApproved(long applicationId, int snapshotVersion, java.time.Instant finishedTime) {
        return publishApprovedRebind(applicationId, snapshotVersion, finishedTime);
    }

    /** 测试夹具，将工作流命令交给实际服务实现。 */
    @Override public void handleProcess(PersonRebindProcessCommand command) {
        super.handleProcess(command);
    }
}
