package org.namewta.common.satoken.config;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.satoken.profile.ProfileAccess;
import org.namewta.common.satoken.profile.ProfileVerificationAdvisor;
import org.namewta.common.satoken.profile.annotation.RequirePersonVerified;
import org.namewta.profile.api.ProfileDisclosureContributor;
import org.namewta.profile.api.ProfileDisclosureService;
import org.namewta.profile.api.ProfileProjectionContributor;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.aop.support.AopUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 公共装配独立于个人、企业实现 Jar，并保留按服务类型分别替换的约定。 */
@Tag("dev")
class ProfileQueryAutoConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ProfileQueryAutoConfiguration.class, ProfileAccessAutoConfiguration.class));

    @Test
    void noContributorsRetainsUnverifiedAndEmptyDisclosureWithoutDomainImplementations() {
        context.run(application -> {
            assertThat(application).hasNotFailed().hasSingleBean(ProfileService.class)
                .hasSingleBean(ProfileDisclosureService.class).hasSingleBean(ProfileAccess.class)
                .hasSingleBean(ProfileVerificationAdvisor.class);
            assertThat(application.getBean(ProfileService.class).findByUserId(7L))
                .isEqualTo(ProfileSummary.unverified(7L));
            assertThat(application.getBean(ProfileDisclosureService.class).findByUserId(7L,
                Set.of(ProfileDisclosureField.PERSON_VERIFIED, ProfileDisclosureField.ENTERPRISE_VERIFIED)))
                .isEqualTo(new ProfileDisclosure(7L, null, null));
        });
    }

    @Test
    void eitherDomainCanContributeWithoutTheOther() {
        for (ProfileType type : ProfileType.values()) {
            context.withBean("singleProjection", ProfileProjectionContributor.class, () -> contributor(type))
                .run(application -> {
                    var summary = application.getBean(ProfileService.class).findByUserId(7L);
                    assertThat(summary.personVerified()).isEqualTo(type == ProfileType.PERSON);
                    assertThat(summary.enterpriseVerified()).isEqualTo(type == ProfileType.ENTERPRISE);
                });
        }
    }

    @Test
    void combinesBothDomainsUsingTheExistingBatchContract() {
        context.withBean("personProjection", ProfileProjectionContributor.class, () -> contributor(ProfileType.PERSON))
            .withBean("enterpriseProjection", ProfileProjectionContributor.class, () -> contributor(ProfileType.ENTERPRISE))
            .run(application -> {
                var summaries = application.getBean(ProfileService.class).findByUserIds(List.of(7L, 8L, 7L));
                assertThat(summaries).hasSize(2);
                assertThat(summaries.get(7L).personVerified()).isTrue();
                assertThat(summaries.get(7L).enterpriseVerified()).isTrue();
                assertThat(summaries.get(8L)).isEqualTo(ProfileSummary.unverified(8L));
            });
    }

    @Test
    void customSummaryOnlyReplacesSummaryBean() {
        ProfileService custom = userIds -> Map.of(7L, ProfileSummary.unverified(7L));
        context.withBean("customSummary", ProfileService.class, () -> custom).run(application -> {
            assertThat(application).hasNotFailed().hasSingleBean(ProfileService.class)
                .hasSingleBean(ProfileDisclosureService.class);
            assertThat(application.getBean(ProfileService.class)).isSameAs(custom);
            assertThat(application.getBean(ProfileDisclosureService.class).findByUserId(7L, Set.of()))
                .isEqualTo(new ProfileDisclosure(7L, null, null));
        });
    }

    @Test
    void customDisclosureOnlyReplacesDisclosureBean() {
        ProfileDisclosureService custom = (userId, fields) -> new ProfileDisclosure(userId, null, null);
        context.withBean("customDisclosure", ProfileDisclosureService.class, () -> custom)
            .withBean("enterpriseProjection", ProfileProjectionContributor.class, () -> contributor(ProfileType.ENTERPRISE))
            .run(application -> {
                assertThat(application).hasNotFailed().hasSingleBean(ProfileService.class)
                    .hasSingleBean(ProfileDisclosureService.class);
                assertThat(application.getBean(ProfileDisclosureService.class)).isSameAs(custom);
                assertThat(application.getBean(ProfileService.class).findByUserId(7L).enterpriseVerified()).isTrue();
            });
    }

    @Test
    void explicitlyEmptyFieldsDoNotInvokeDisclosureContributors() {
        var contributor = mock(ProfileDisclosureContributor.class);
        when(contributor.profileType()).thenReturn(ProfileType.PERSON);
        context.withBean("personDisclosure", ProfileDisclosureContributor.class, () -> contributor)
            .run(application -> {
                assertThat(application.getBean(ProfileDisclosureService.class).findByUserId(7L, Set.of()))
                    .isEqualTo(new ProfileDisclosure(7L, null, null));
                org.mockito.Mockito.verify(contributor, org.mockito.Mockito.never())
                    .findByUserId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            });
    }

    @Test
    void autoConfiguredAdvisorActuallyProtectsSpringManagedBusinessBeans() {
        ProfileAccess access = mock(ProfileAccess.class);
        ProtectedService target = new ProtectedService();
        context.withUserConfiguration(ProxyConfiguration.class)
            .withBean("customAccess", ProfileAccess.class, () -> access)
            .withBean("protectedService", ProtectedService.class, () -> target)
            .run(application -> {
                assertThat(application).hasNotFailed();
                ProtectedService service = application.getBean(ProtectedService.class);
                assertThat(AopUtils.isAopProxy(service)).isTrue();
                assertThat(service.execute()).isEqualTo("executed");
                org.mockito.Mockito.verify(access).requireVerified(ProfileType.PERSON);
                assertThat(target.calls).isEqualTo(1);
            });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class ProxyConfiguration {
    }

    @RequirePersonVerified
    public static class ProtectedService {
        private int calls;

        public String execute() {
            calls++;
            return "executed";
        }
    }

    private static ProfileProjectionContributor contributor(ProfileType type) {
        return new ProfileProjectionContributor() {
            @Override
            public ProfileType profileType() { return type; }

            @Override
            public Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds) {
                return userIds.contains(7L)
                    ? Map.of(7L, new ProfileBindingSummary(91L, type, Instant.EPOCH)) : Map.of();
            }
        };
    }
}
