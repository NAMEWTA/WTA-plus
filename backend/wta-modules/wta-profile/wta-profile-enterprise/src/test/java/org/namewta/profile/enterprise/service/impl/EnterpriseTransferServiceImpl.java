package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.port.security.EnterpriseTransferCodePort;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.profile.api.person.PersonIdentityLookupService;
import org.namewta.profile.enterprise.dao.EnterpriseTransferDao;
import org.namewta.profile.enterprise.mapper.EnterpriseTransferMapper;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore;
import org.namewta.profile.enterprise.service.EnterpriseTransferService;
import org.namewta.profile.enterprise.usecase.EnterpriseTransferUseCase;
import org.namewta.system.api.UserService;

import java.time.Clock;

public class EnterpriseTransferServiceImpl extends EnterpriseTransferService implements EnterpriseTransferUseCase {
    public EnterpriseTransferServiceImpl(EnterpriseTransferMapper mapper, EnterpriseTransferChallengeStore challenges,
                                          EnterpriseTransferCodePort codes,
                                          PersonIdentityLookupService personIdentities, UserService users,
                                          NotificationApplicationService notify, Clock clock) {
        super(new EnterpriseTransferDao(mapper), challenges, codes, personIdentities, users, notify, clock);
    }

}
