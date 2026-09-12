package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.adapter.api.EnterpriseMaterialOwnerContributor;
import org.namewta.profile.enterprise.service.EnterpriseProfileApiService;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseProfileApiUseCaseImpl;
import org.namewta.profile.enterprise.adapter.api.EnterpriseMaterialOwnerContributor;

import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import org.namewta.profile.enterprise.dao.EnterpriseApplicationDao;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class EnterpriseMaterialOwnerContributorTest {

    private final EnterpriseApplicationMapper mapper = mock(EnterpriseApplicationMapper.class);
    private final EnterpriseMaterialOwnerContributor contributor =
        new EnterpriseMaterialOwnerContributor(new EnterpriseProfileApiUseCaseImpl(
            new EnterpriseProfileApiService(new EnterpriseApplicationDao(mapper))));

    @Test
    void resolvesMutableAndImmutableEnterpriseOwnersWithoutLeakingSnapshotData() {
        MaterialOwnerKey working = owner(MaterialOwnerType.WORKING, 101L);
        MaterialOwnerKey submission = owner(MaterialOwnerType.SUBMISSION, 201L);
        MaterialOwnerKey source = owner(MaterialOwnerType.SOURCE, 301L);
        MaterialOwnerKey version = owner(MaterialOwnerType.VERSION, 401L);
        when(mapper.lockMaterialWorkingOwner(101L)).thenReturn(11L);
        when(mapper.lockMaterialSubmissionOwner(201L)).thenReturn(11L);
        when(mapper.lockMaterialSourceOwner(301L)).thenReturn(301L);
        when(mapper.lockMaterialVersionOwner(401L)).thenReturn(401L);

        assertThat(contributor.profileType()).isEqualTo(ProfileType.ENTERPRISE);
        assertThat(contributor.lockOwner(working)).get().extracting(owner -> owner.applicantUserId()).isEqualTo(11L);
        assertThat(contributor.lockOwner(submission)).get().extracting(owner -> owner.applicantUserId()).isEqualTo(11L);
        assertThat(contributor.lockOwner(source)).get().extracting(owner -> owner.applicantUserId()).isNull();
        assertThat(contributor.lockOwner(version)).get().extracting(owner -> owner.applicantUserId()).isNull();
    }

    @Test
    void delegatesEditableAndAllSupportedSnapshotRelationshipsToEnterpriseSql() {
        MaterialOwnerKey working = owner(MaterialOwnerType.WORKING, 101L);
        MaterialOwnerKey submission = owner(MaterialOwnerType.SUBMISSION, 201L);
        MaterialOwnerKey source = owner(MaterialOwnerType.SOURCE, 301L);
        MaterialOwnerKey version = owner(MaterialOwnerType.VERSION, 401L);
        when(mapper.countEditableMaterialWorkingOwner(101L)).thenReturn(1L);
        when(mapper.countWorkingSubmissionRelationship(101L, 201L)).thenReturn(1L);
        when(mapper.countSubmissionVersionRelationship(201L, 401L)).thenReturn(1L);
        when(mapper.countSourceVersionRelationship(301L, 401L)).thenReturn(1L);

        assertThat(contributor.isWorkingEditable(working)).isTrue();
        assertThat(contributor.hasSnapshotRelationship(new SnapshotRelationship(working, submission))).isTrue();
        assertThat(contributor.hasSnapshotRelationship(new SnapshotRelationship(submission, version))).isTrue();
        assertThat(contributor.hasSnapshotRelationship(new SnapshotRelationship(source, version))).isTrue();
    }

    @Test
    void rejectsOwnersBelongingToAnotherProfileModuleAndNonWorkingEditChecks() {
        MaterialOwnerKey person = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.WORKING, 101L);
        MaterialOwnerKey version = owner(MaterialOwnerType.VERSION, 401L);

        assertThatThrownBy(() -> contributor.lockOwner(person)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contributor.isWorkingEditable(person)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contributor.isWorkingEditable(version)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mapper);
    }

    private MaterialOwnerKey owner(MaterialOwnerType type, long id) {
        return new MaterialOwnerKey(ProfileType.ENTERPRISE, type, id);
    }
}
