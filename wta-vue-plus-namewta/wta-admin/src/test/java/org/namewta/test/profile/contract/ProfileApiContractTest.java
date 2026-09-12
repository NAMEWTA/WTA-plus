package org.namewta.test.profile.contract;

import org.namewta.profile.api.CompositeProfileService;
import org.namewta.profile.api.ProfileProjectionContributor;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.system.api.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class ProfileApiContractTest {

    @Test
    void activeUserCandidateSearchIsAnOptionalCompatibleSystemSeam() throws Exception {
        var method = UserService.class.getMethod("searchActiveUsers", String.class, int.class);

        assertThat(method.isDefault()).isTrue();
        assertThat(method.getReturnType()).isEqualTo(List.class);
    }

    @Test
    void combinesContributorsInOneBatchAndDefaultsMissingTypesToUnverified() {
        Instant verifiedAt = Instant.parse("2026-08-31T12:00:00Z");
        ProfileProjectionContributor person = contributor(ProfileType.PERSON,
            Map.of(7L, new ProfileBindingSummary(91L, ProfileType.PERSON, verifiedAt)));
        CompositeProfileService service = new CompositeProfileService(List.of(person));

        Map<Long, ProfileSummary> summaries = service.findByUserIds(List.of(7L, 8L, 7L));

        assertThat(summaries).containsOnlyKeys(7L, 8L);
        assertThat(summaries.get(7L).personVerified()).isTrue();
        assertThat(summaries.get(7L).enterpriseVerified()).isFalse();
        assertThat(summaries.get(8L)).isEqualTo(ProfileSummary.unverified(8L));
        assertThatThrownBy(() -> summaries.put(9L, ProfileSummary.unverified(9L)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyBatchDoesNotInvokeContributorsAndSingleLookupUsesBatchContract() {
        RecordingContributor contributor = new RecordingContributor();
        CompositeProfileService service = new CompositeProfileService(List.of(contributor));

        assertThat(service.findByUserIds(List.of())).isEmpty();
        assertThat(contributor.invocations).isZero();
        assertThat(service.findByUserId(12L)).isEqualTo(ProfileSummary.unverified(12L));
        assertThat(contributor.requestedIds).containsExactly(12L);
    }

    @Test
    void publicSummaryRecordsContainOnlyTheNonSensitiveWhitelist() {
        assertThat(componentNames(ProfileBindingSummary.class))
            .containsExactlyInAnyOrder("profileId", "profileType", "verifiedAt");
        assertThat(componentNames(ProfileSummary.class))
            .containsExactlyInAnyOrder("userId", "person", "enterprise");

        String exposed = String.join(" ", componentNames(ProfileBindingSummary.class))
            + " " + String.join(" ", componentNames(ProfileSummary.class));
        assertThat(exposed.toLowerCase()).doesNotContain(
            "name", "identity", "credential", "document", "material", "application", "phone", "mobile");
    }

    @Test
    void rejectsInvalidIdsAndContributorTypeMismatches() {
        CompositeProfileService empty = new CompositeProfileService(List.of());
        assertThatThrownBy(() -> empty.findByUserId(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> empty.findByUserIds(Arrays.asList(1L, null)))
            .isInstanceOf(IllegalArgumentException.class);

        ProfileProjectionContributor invalid = contributor(ProfileType.PERSON,
            Map.of(1L, new ProfileBindingSummary(2L, ProfileType.ENTERPRISE, Instant.EPOCH)));
        CompositeProfileService service = new CompositeProfileService(List.of(invalid));
        assertThatThrownBy(() -> service.findByUserIds(List.of(1L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PERSON");
    }

    private static ProfileProjectionContributor contributor(ProfileType type,
                                                              Map<Long, ProfileBindingSummary> projections) {
        return new ProfileProjectionContributor() {
            @Override
            public ProfileType profileType() {
                return type;
            }

            @Override
            public Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds) {
                return projections;
            }
        };
    }

    private static Set<String> componentNames(Class<?> recordType) {
        return List.of(recordType.getRecordComponents()).stream()
            .map(RecordComponent::getName)
            .collect(Collectors.toSet());
    }

    private static final class RecordingContributor implements ProfileProjectionContributor {
        private int invocations;
        private Set<Long> requestedIds = Set.of();

        @Override
        public ProfileType profileType() {
            return ProfileType.PERSON;
        }

        @Override
        public Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds) {
            invocations++;
            requestedIds = userIds;
            return Map.of();
        }
    }
}
