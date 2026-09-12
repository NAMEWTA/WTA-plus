package org.namewta.profile.person.config;

import org.namewta.profile.api.ProfileProjectionContributor;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class ProfileApiConfigurationTest {

    @Test
    void assemblesAllAvailableProjectionContributors() {
        try (AnnotationConfigApplicationContext context = contextWith(
            contributor(ProfileType.PERSON, 91L), contributor(ProfileType.ENTERPRISE, 92L))) {
            var summary = context.getBean(ProfileService.class).findByUserId(7L);

            assertThat(summary.person().profileId()).isEqualTo(91L);
            assertThat(summary.enterprise().profileId()).isEqualTo(92L);
        }
    }

    @Test
    void startsWithoutContributorsAndReturnsAnUnverifiedSummary() {
        try (AnnotationConfigApplicationContext context = contextWith()) {
            var summary = context.getBean(ProfileService.class).findByUserId(7L);

            assertThat(summary.personVerified()).isFalse();
            assertThat(summary.enterpriseVerified()).isFalse();
        }
    }

    @Test
    void backsOffWhenAProfileServiceIsProvided() {
        ProfileService custom = userIds -> Map.of();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("customProfileService", ProfileService.class, () -> custom);
            context.register(ProfileApiConfiguration.class);
            context.refresh();

            assertThat(context.getBeansOfType(ProfileService.class)).hasSize(1);
            assertThat(context.getBean(ProfileService.class)).isSameAs(custom);
        }
    }

    private static AnnotationConfigApplicationContext contextWith(ProfileProjectionContributor... contributors) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        for (int index = 0; index < contributors.length; index++) {
            ProfileProjectionContributor contributor = contributors[index];
            context.registerBean("projectionContributor" + index, ProfileProjectionContributor.class,
                () -> contributor);
        }
        context.register(ProfileApiConfiguration.class);
        context.refresh();
        return context;
    }

    private static ProfileProjectionContributor contributor(ProfileType type, long profileId) {
        return new ProfileProjectionContributor() {
            @Override
            public ProfileType profileType() {
                return type;
            }

            @Override
            public Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds) {
                return Map.of(7L, new ProfileBindingSummary(profileId, type, Instant.EPOCH));
            }
        };
    }
}
