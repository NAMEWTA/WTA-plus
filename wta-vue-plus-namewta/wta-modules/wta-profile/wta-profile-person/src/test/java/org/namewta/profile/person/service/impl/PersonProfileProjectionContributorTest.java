package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.adapter.api.PersonProfileProjectionContributor;
import org.namewta.profile.person.usecase.impl.PersonProfileApiUseCaseImpl;
import org.namewta.profile.person.service.PersonProfileApiService;

import org.namewta.profile.person.domain.model.read.PersonActiveProjectionRow;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.api.domain.ProfileType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class PersonProfileProjectionContributorTest {

    @Test
    void returnsOnlyRepositoryApprovedActiveProjection() {
        PersonApplicationMapper mapper = mock(PersonApplicationMapper.class);
        PersonActiveProjectionRow row = new PersonActiveProjectionRow();
        row.setUserId(101L);
        row.setPersonProfileId(9001L);
        row.setVerifiedAt(Instant.parse("2026-09-01T12:00:00Z"));
        when(mapper.selectActiveProjections(Set.of(101L, 102L))).thenReturn(List.of(row));

        PersonProfileProjectionContributor contributor = new PersonProfileProjectionContributor(
            new PersonProfileApiUseCaseImpl(new PersonProfileApiService(new PersonApplicationDao(mapper))));

        assertThat(contributor.profileType()).isEqualTo(ProfileType.PERSON);
        assertThat(contributor.findActiveBindings(Set.of(101L, 102L))).containsOnlyKeys(101L);
        assertThat(contributor.findActiveBindings(Set.of(101L, 102L)).get(101L).profileId()).isEqualTo(9001L);
    }
}
