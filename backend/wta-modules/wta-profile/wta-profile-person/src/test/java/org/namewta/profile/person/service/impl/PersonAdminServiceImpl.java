package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.dao.PersonAdminDao;
import org.namewta.profile.person.mapper.PersonAdminMapper;
import org.namewta.profile.person.domain.bo.*;
import org.namewta.profile.person.domain.vo.*;
import org.namewta.profile.person.port.PersonApplicationPublicationPort;
import org.namewta.profile.person.service.PersonAdminService;
import org.namewta.profile.person.usecase.PersonAdminUseCase;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.UserService;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;

import java.time.Clock;

public class PersonAdminServiceImpl extends PersonAdminService implements PersonAdminUseCase {
    public PersonAdminServiceImpl(PersonAdminMapper mapper,
                                  PersonApplicationPublicationPort applications, ProfileMaterialPort materials,
                                  PersonWorkflowGateway workflow, UserService users, Clock clock) {
        super(new PersonAdminDao(mapper), applications, materials, workflow, users, clock);
    }
    public PersonAdminServiceImpl(PersonAdminMapper mapper,
                                  PersonApplicationPublicationPort applications, ProfileMaterialPort materials,
                                  PersonWorkflowGateway workflow, UserService users) {
        super(new PersonAdminDao(mapper), applications, materials, workflow, users);
    }

}
