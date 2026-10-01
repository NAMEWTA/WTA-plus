package org.namewta.profile.person.config;

import org.namewta.profile.api.ProfileProjectionContributor;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.ProfileDisclosureService;
import org.namewta.common.satoken.config.ProfileQueryAutoConfiguration;
import org.namewta.common.satoken.config.ProfileAccessAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class ProfileApiConfigurationTest {

    @Test
    void legacyImportAndNewAutoConfigurationRegisterEachPublicServiceOnce() {
        new ApplicationContextRunner()
            .withUserConfiguration(ProfileApiConfiguration.class)
            .withConfiguration(AutoConfigurations.of(
                ProfileQueryAutoConfiguration.class, ProfileAccessAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(ProfileService.class)
                    .hasSingleBean(ProfileDisclosureService.class);
                assertThat(context.getBean(ProfileService.class).findByUserId(7L))
                    .isEqualTo(org.namewta.profile.api.domain.ProfileSummary.unverified(7L));
            });
    }

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

    @Test
    void legacyImportDefersUntilTheImportingConfigurationsSummaryBeanIsRegistered() {
        try (var context = new AnnotationConfigApplicationContext(CustomSummaryConfiguration.class)) {
            assertThat(context.getBeansOfType(ProfileService.class)).containsOnlyKeys("customSummary");
            assertThat(context.getBeansOfType(ProfileDisclosureService.class)).hasSize(1);
        }
    }

    @Test
    void legacyImportDefersUntilTheImportingConfigurationsDisclosureBeanIsRegistered() {
        try (var context = new AnnotationConfigApplicationContext(CustomDisclosureConfiguration.class)) {
            assertThat(context.getBeansOfType(ProfileDisclosureService.class)).containsOnlyKeys("customDisclosure");
            assertThat(context.getBeansOfType(ProfileService.class)).hasSize(1);
        }
    }

    @Test
    void customBeanMethodsWorkWithBothTheLegacyImportAndNewAutoConfiguration() {
        new ApplicationContextRunner()
            .withUserConfiguration(CustomSummaryConfiguration.class, CustomDisclosureConfiguration.class)
            .withConfiguration(AutoConfigurations.of(
                ProfileQueryAutoConfiguration.class, ProfileAccessAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(ProfileService.class)
                    .hasSingleBean(ProfileDisclosureService.class);
                assertThat(context.getBeansOfType(ProfileService.class)).containsOnlyKeys("customSummary");
                assertThat(context.getBeansOfType(ProfileDisclosureService.class)).containsOnlyKeys("customDisclosure");
            });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ProfileApiConfiguration.class)
    static class CustomSummaryConfiguration {
        @Bean
        ProfileService customSummary() {
            return userIds -> Map.of();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ProfileApiConfiguration.class)
    static class CustomDisclosureConfiguration {
        @Bean
        ProfileDisclosureService customDisclosure() {
            return (userId, fields) -> new org.namewta.profile.api.domain.ProfileDisclosure(userId, null, null);
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
