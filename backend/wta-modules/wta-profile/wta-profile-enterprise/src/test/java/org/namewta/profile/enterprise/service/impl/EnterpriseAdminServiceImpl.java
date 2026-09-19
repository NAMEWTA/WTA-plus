package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.dao.EnterpriseAdminDao;
import org.namewta.profile.enterprise.mapper.EnterpriseAdminMapper;
import org.namewta.profile.enterprise.service.EnterpriseAdminService;
import org.namewta.profile.enterprise.usecase.EnterpriseAdminUseCase;
import org.namewta.profile.enterprise.domain.bo.*;
import org.namewta.profile.enterprise.domain.vo.*;
import org.namewta.profile.enterprise.port.EnterpriseApplicationPublicationPort;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.UserService;
import org.namewta.profile.enterprise.port.gateway.EnterpriseWorkflowGateway;

import java.time.Clock;

public class EnterpriseAdminServiceImpl extends EnterpriseAdminService implements EnterpriseAdminUseCase {
    public EnterpriseAdminServiceImpl(EnterpriseAdminMapper mapper,
                                       EnterpriseApplicationPublicationPort applications,
                                       ProfileMaterialPort materials, EnterpriseWorkflowGateway workflow, UserService users,
                                       ProfileService profiles, Clock clock) {
        super(new EnterpriseAdminDao(mapper), applications, materials, workflow, users, profiles, clock);
    }

}
