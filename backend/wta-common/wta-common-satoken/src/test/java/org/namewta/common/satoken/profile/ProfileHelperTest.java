package org.namewta.common.satoken.profile;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 静态调用与注入调用共享行为，不依赖首次初始化的 Spring 容器或会话。 */
@Tag("dev")
class ProfileHelperTest {
    @Test
    void everyCallResolvesTheCurrentBeanWithoutCapturingTheFirstContainer() {
        ProfileAccess first = mock(ProfileAccess.class);
        ProfileAccess second = mock(ProfileAccess.class);
        ProfileSummary firstSummary = ProfileSummary.unverified(7L);
        ProfileSummary secondSummary = ProfileSummary.unverified(8L);
        when(first.currentSummary()).thenReturn(firstSummary);
        when(second.currentSummary()).thenReturn(secondSummary);
        // SpringUtils 继承的静态 getBean 实际声明在 SpringUtil，须拦截声明类。
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(ProfileAccess.class)).thenReturn(first, second);
            assertThat(ProfileHelper.currentSummary()).isSameAs(firstSummary);
            assertThat(ProfileHelper.currentSummary()).isSameAs(secondSummary);
            spring.verify(() -> SpringUtil.getBean(ProfileAccess.class), times(2));
        }
        verify(first).currentSummary();
        verify(second).currentSummary();
    }

    @Test
    void allConvenienceMethodsDelegateTheirExactContracts() {
        ProfileAccess access = mock(ProfileAccess.class);
        ProfileType[] types = {ProfileType.PERSON, ProfileType.ENTERPRISE};
        Set<ProfileDisclosureField> fields = Set.of(ProfileDisclosureField.PERSON_FULL_NAME);
        ProfileSummary summary = ProfileSummary.unverified(7L);
        ProfileDisclosure disclosure = new ProfileDisclosure(7L, null, null);
        when(access.isPersonVerified()).thenReturn(true);
        when(access.isEnterpriseVerified()).thenReturn(false);
        when(access.requireVerified(types)).thenReturn(summary);
        when(access.currentDisclosure(fields)).thenReturn(disclosure);
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(ProfileAccess.class)).thenReturn(access);
            assertThat(ProfileHelper.isPersonVerified()).isTrue();
            assertThat(ProfileHelper.isEnterpriseVerified()).isFalse();
            assertThat(ProfileHelper.requireVerified(types)).isSameAs(summary);
            assertThat(ProfileHelper.currentDisclosure(fields)).isSameAs(disclosure);
            spring.verify(() -> SpringUtil.getBean(ProfileAccess.class), times(4));
        }
        verify(access).isPersonVerified();
        verify(access).isEnterpriseVerified();
        verify(access).requireVerified(types);
        verify(access).currentDisclosure(fields);
        verifyNoMoreInteractions(access);
    }

    @Test
    void missingServiceAndAccessFailuresAreNeverConvertedToSuccessfulChecks() {
        ProfileAccess access = mock(ProfileAccess.class);
        RuntimeException queryFailure = new IllegalStateException("owned query failed");
        when(access.isPersonVerified()).thenThrow(queryFailure);
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(ProfileAccess.class)).thenReturn(access);
            assertThatThrownBy(ProfileHelper::isPersonVerified).isSameAs(queryFailure);
            var missing = new NoSuchBeanDefinitionException(ProfileAccess.class);
            spring.when(() -> SpringUtil.getBean(ProfileAccess.class)).thenThrow(missing);
            assertThatThrownBy(ProfileHelper::currentSummary).isSameAs(missing);
        }
    }
}
