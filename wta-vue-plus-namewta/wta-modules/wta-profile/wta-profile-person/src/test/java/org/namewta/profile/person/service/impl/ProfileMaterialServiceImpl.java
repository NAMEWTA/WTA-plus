package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.dao.ProfileMaterialDao;
import org.namewta.profile.person.mapper.ProfileMaterialMapper;
import org.namewta.profile.person.port.security.ProfileMaterialAccessPolicy;
import org.namewta.profile.person.service.ProfileMaterialService;
import org.namewta.profile.person.usecase.ProfileMaterialUseCase;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.OssService;

import java.time.Clock;
import java.util.List;

public class ProfileMaterialServiceImpl extends ProfileMaterialService
    implements ProfileMaterialUseCase, ProfileMaterialPort {
    public ProfileMaterialServiceImpl(ProfileMaterialMapper mapper, OssService ossService,
                                      ProfileMaterialAccessPolicy accessPolicy,
                                      List<ProfileMaterialOwnerContributor> ownerContributors, Clock clock) {
        super(new ProfileMaterialDao(mapper), ossService, accessPolicy, ownerContributors, clock);
    }

    public ProfileMaterialServiceImpl(ProfileMaterialMapper mapper, OssService ossService,
                                      ProfileMaterialAccessPolicy accessPolicy,
                                      List<ProfileMaterialOwnerContributor> ownerContributors) {
        this(mapper, ossService, accessPolicy, ownerContributors, Clock.systemUTC());
    }
}
