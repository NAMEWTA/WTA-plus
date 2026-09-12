package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.adapter.security.EnterpriseTransferCodeGenerator;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.profile.api.person.PersonIdentityLookupService;
import org.namewta.profile.enterprise.dao.EnterpriseTransferDao;
import org.namewta.profile.enterprise.mapper.EnterpriseTransferMapper;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore;
import org.namewta.profile.enterprise.service.EnterpriseTransferService;
import org.namewta.profile.enterprise.usecase.EnterpriseTransferUseCase;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.UserService;

import java.time.Clock;

public class EnterpriseTransferServiceImpl extends EnterpriseTransferService implements EnterpriseTransferUseCase {
    public EnterpriseTransferServiceImpl(EnterpriseTransferMapper mapper, EnterpriseTransferChallengeStore challenges,
                                          EnterpriseTransferCodeGenerator codes,
                                          PersonIdentityLookupService personIdentities, UserService users,
                                          NotificationApplicationService notify, Clock clock) {
        super(new EnterpriseTransferDao(mapper), challenges, codes, personIdentities, users, notify, clock);
    }

    @Override public org.namewta.profile.enterprise.domain.vo.EnterpriseTransferVo send(
        org.namewta.profile.enterprise.domain.bo.EnterpriseTransferSendBo command) {
        return send(LoginHelper.getUserId(), command);
    }
    @Override public org.namewta.profile.enterprise.domain.vo.EnterpriseTransferVo confirm(
        org.namewta.profile.enterprise.domain.bo.EnterpriseTransferConfirmBo command) {
        return confirm(LoginHelper.getUserId(), command);
    }
    @Override public org.namewta.profile.enterprise.domain.vo.EnterpriseTransferVo unbind() {
        return unbind(LoginHelper.getUserId());
    }
}
