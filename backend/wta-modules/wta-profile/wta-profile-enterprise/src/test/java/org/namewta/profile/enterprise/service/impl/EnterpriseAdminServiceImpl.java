package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.dao.EnterpriseAdminDao;
import org.namewta.profile.enterprise.mapper.EnterpriseAdminMapper;
import org.namewta.profile.enterprise.service.EnterpriseAdminService;
import org.namewta.profile.enterprise.usecase.EnterpriseAdminUseCase;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.enterprise.domain.bo.*;
import org.namewta.profile.enterprise.domain.vo.*;
import org.namewta.profile.enterprise.service.IEnterpriseApplicationService;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.UserService;
import org.namewta.workflow.api.WorkflowService;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

public class EnterpriseAdminServiceImpl extends EnterpriseAdminService implements EnterpriseAdminUseCase {
    public EnterpriseAdminServiceImpl(EnterpriseAdminMapper mapper, JsonMapper jsonMapper,
                                       IEnterpriseApplicationService applications,
                                       ProfileMaterialPort materials, WorkflowService workflow, UserService users,
                                       ProfileService profiles, Clock clock) {
        super(new EnterpriseAdminDao(mapper), jsonMapper, applications, materials, workflow, users, profiles, clock);
    }

    @Override public EnterpriseAdminResultVo decide(long applicationId, EnterpriseAdminDecisionBo command) {
        return decide(LoginHelper.getUserId(), applicationId, command);
    }
    @Override public EnterpriseAdminResultVo create(EnterpriseAdminCreateBo command) {
        return create(LoginHelper.getUserId(), command);
    }
    @Override public EnterpriseAdminResultVo revise(long profileId, EnterpriseAdminReviseBo command) {
        return revise(LoginHelper.getUserId(), profileId, command);
    }
    @Override public EnterpriseAdminResultVo manageBinding(long profileId, EnterpriseAdminBindingBo command) {
        return manageBinding(LoginHelper.getUserId(), profileId, command);
    }
    @Override public EnterpriseAdminResultVo assign(long profileId, EnterpriseAdminAssignBo command) {
        return assign(LoginHelper.getUserId(), profileId, command);
    }
    @Override public EnterpriseAdminResultVo revoke(long profileId, EnterpriseAdminRevokeBo command) {
        return revoke(LoginHelper.getUserId(), profileId, command);
    }
}
