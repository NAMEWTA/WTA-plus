package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.dao.PersonAdminDao;
import org.namewta.profile.person.mapper.PersonAdminMapper;
import org.namewta.profile.person.domain.bo.*;
import org.namewta.profile.person.domain.vo.*;
import org.namewta.profile.person.service.IPersonApplicationService;
import org.namewta.profile.person.service.PersonAdminService;
import org.namewta.profile.person.usecase.PersonAdminUseCase;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.UserService;
import org.namewta.workflow.api.WorkflowService;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

public class PersonAdminServiceImpl extends PersonAdminService implements PersonAdminUseCase {
    public PersonAdminServiceImpl(PersonAdminMapper mapper, JsonMapper jsonMapper,
                                  IPersonApplicationService applications, ProfileMaterialPort materials,
                                  WorkflowService workflow, UserService users, Clock clock) {
        super(new PersonAdminDao(mapper), jsonMapper, applications, materials, workflow, users, clock);
    }
    public PersonAdminServiceImpl(PersonAdminMapper mapper, JsonMapper jsonMapper,
                                  IPersonApplicationService applications, ProfileMaterialPort materials,
                                  WorkflowService workflow, UserService users) {
        super(new PersonAdminDao(mapper), jsonMapper, applications, materials, workflow, users);
    }

    @Override public PersonAdminResultVo decide(long applicationId, PersonAdminDecisionBo command) {
        return decide(LoginHelper.getUserId(), applicationId, command);
    }
    @Override public PersonAdminResultVo create(PersonAdminCreateBo command) {
        return create(LoginHelper.getUserId(), command);
    }
    @Override public PersonAdminResultVo revise(long profileId, PersonAdminReviseBo command) {
        return revise(LoginHelper.getUserId(), profileId, command);
    }
    @Override public PersonAdminResultVo manageBinding(long profileId, PersonAdminBindingBo command) {
        return manageBinding(LoginHelper.getUserId(), profileId, command);
    }
    @Override public PersonAdminResultVo assign(long profileId, PersonAdminAssignBo command) {
        return assign(LoginHelper.getUserId(), profileId, command);
    }
    @Override public PersonAdminResultVo revoke(long profileId, PersonAdminRevokeBo command) {
        return revoke(LoginHelper.getUserId(), profileId, command);
    }
}
