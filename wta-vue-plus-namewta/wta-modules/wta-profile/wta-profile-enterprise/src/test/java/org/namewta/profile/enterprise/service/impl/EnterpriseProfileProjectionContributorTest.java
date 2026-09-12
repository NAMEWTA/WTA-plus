package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.adapter.api.EnterpriseProfileProjectionContributor;
import org.namewta.profile.enterprise.service.EnterpriseProfileApiService;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseProfileApiUseCaseImpl;
import org.namewta.profile.enterprise.adapter.api.EnterpriseProfileProjectionContributor;

import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseActiveProjectionRow;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import org.namewta.profile.enterprise.dao.EnterpriseApplicationDao;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class EnterpriseProfileProjectionContributorTest {

    @Test
    void returnsOnlyRepositoryApprovedActiveProjection() {
        EnterpriseApplicationMapper mapper = mock(EnterpriseApplicationMapper.class);
        EnterpriseActiveProjectionRow row = new EnterpriseActiveProjectionRow();
        row.setUserId(101L);
        row.setEnterpriseProfileId(9001L);
        row.setVerifiedAt(Instant.parse("2026-09-01T12:00:00Z"));
        when(mapper.selectActiveProjections(Set.of(101L, 102L))).thenReturn(List.of(row));

        EnterpriseProfileProjectionContributor contributor = new EnterpriseProfileProjectionContributor(
            new EnterpriseProfileApiUseCaseImpl(new EnterpriseProfileApiService(new EnterpriseApplicationDao(mapper))));

        assertThat(contributor.profileType()).isEqualTo(ProfileType.ENTERPRISE);
        assertThat(contributor.findActiveBindings(Set.of(101L, 102L))).containsOnlyKeys(101L);
        assertThat(contributor.findActiveBindings(Set.of(101L, 102L)).get(101L).profileId()).isEqualTo(9001L);
    }
}
