package org.namewta.common.satoken.profile;

import cn.dev33.satoken.annotation.SaIgnore;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.satoken.profile.annotation.RequireEnterpriseVerified;
import org.namewta.common.satoken.profile.annotation.RequirePersonVerified;
import org.namewta.profile.api.domain.ProfileType;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** 使用实际 Spring 代理验证认证要求的合并及明确支持的调用边界。 */
@Tag("dev")
class ProfileVerificationAdvisorTest {

    private final ProfileAccess access = mock(ProfileAccess.class);
    private final ObjectProvider<ProfileAccess> provider = provider(access);
    private final ProfileVerificationAdvisor advisor = new ProfileVerificationAdvisor(provider);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void combinesTypeAndMethodRequirementsInOneCall(boolean classProxy) {
        CombinedService target = new CombinedService();
        Action proxy = proxy(target, Action.class, classProxy);

        assertThat(proxy.execute()).isEqualTo("done");

        verify(access).requireVerified(ProfileType.PERSON, ProfileType.ENTERPRISE);
        verifyNoMoreInteractions(access);
        assertThat(target.calls.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void denialStopsTheActualServiceBody(boolean classProxy) {
        CombinedService target = new CombinedService();
        Action proxy = proxy(target, Action.class, classProxy);
        ServiceException denial = new ServiceException("认证不足", 403);
        doThrow(denial).when(access).requireVerified(ProfileType.PERSON, ProfileType.ENTERPRISE);

        assertThatThrownBy(proxy::execute).isSameAs(denial);

        assertThat(target.calls.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void combinesParentTypeAndInheritedInterfaceType(boolean classProxy) {
        Action proxy = proxy(new InheritedTypeService(), PersonAction.class, classProxy);

        proxy.execute();

        verify(access).requireVerified(ProfileType.PERSON, ProfileType.ENTERPRISE);
        verifyNoMoreInteractions(access);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void combinesOverriddenParentMethodAndInheritedInterfaceMethod(boolean classProxy) {
        Action proxy = proxy(new InheritedMethodService(), EnterpriseAction.class, classProxy);

        proxy.execute();

        verify(access).requireVerified(ProfileType.PERSON, ProfileType.ENTERPRISE);
        verifyNoMoreInteractions(access);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void resolvesGenericInterfaceAndImplementationBridgeMethods(boolean classProxy) {
        GenericAction<String> proxy = proxy(new StringAction(), GenericAction.class, classProxy);

        assertThat(proxy.execute("value")).isEqualTo("value");

        verify(access).requireVerified(ProfileType.PERSON, ProfileType.ENTERPRISE);
        verifyNoMoreInteractions(access);
    }

    @Test
    void saIgnoreDoesNotDisableTheIndependentVerificationAdvisor() {
        Action proxy = proxy(new IgnoredService(), Action.class, true);

        proxy.execute();

        verify(access).requireVerified(ProfileType.PERSON);
    }

    @Test
    void constructingAndMatchingTheAdvisorDoesNotResolveAccessBean() throws Exception {
        assertThat(advisor.getPointcut().getMethodMatcher().matches(
            CombinedService.class.getMethod("execute"), CombinedService.class)).isTrue();
        Action proxy = proxy(new CombinedService(), Action.class, true);

        verify(provider, never()).getObject();
        verifyNoInteractions(access);

        proxy.execute();

        verify(provider).getObject();
    }

    @Test
    void unrelatedMethodsAreNotGuardedAndDoNotResolveAccessBean() {
        BoundaryService target = new BoundaryService();
        BoundaryService proxy = proxy(target, BoundaryService.class, true);

        assertThat(proxy.unrelated()).isEqualTo("unrelated");

        verify(provider, never()).getObject();
        verifyNoInteractions(access);
    }

    @Test
    void infrastructureFailurePropagatesWithoutExecutingBusinessCode() {
        CombinedService target = new CombinedService();
        Action proxy = proxy(target, Action.class, true);
        IllegalStateException failure = new IllegalStateException("store unavailable");
        doThrow(failure).when(access).requireVerified(ProfileType.PERSON, ProfileType.ENTERPRISE);

        assertThatThrownBy(proxy::execute).isSameAs(failure);
        assertThat(target.calls.get()).isZero();
    }

    @Test
    void jdkProxyCanGuardAFinalImplementationMethod() {
        Action proxy = proxy(new FinalImplementation(), Action.class, false);

        assertThat(proxy.execute()).isEqualTo("final");

        verify(access).requireVerified(ProfileType.PERSON);
    }

    @Test
    void classProxyFinalMethodIsAnExplicitlyUnsupportedBoundary() {
        BoundaryService proxy = proxy(new BoundaryService(), BoundaryService.class, true);

        assertThat(proxy.finalMethod()).isEqualTo("final");

        verifyNoInteractions(access);
    }

    @Test
    void selfInvocationIsAnExplicitlyUnsupportedBoundary() {
        BoundaryService target = new BoundaryService();
        BoundaryService proxy = proxy(target, BoundaryService.class, true);

        assertThat(proxy.invokeSelf()).isEqualTo("guarded");

        assertThat(target.calls.get()).isEqualTo(1);
        verifyNoInteractions(access);
        proxy.guarded();
        verify(access).requireVerified(ProfileType.PERSON);
    }

    @Test
    void privateStaticAndObjectMethodsDoNotMatchTheProxyContract() throws Exception {
        var matcher = advisor.getPointcut().getMethodMatcher();

        assertThat(matcher.matches(BoundaryService.class.getDeclaredMethod("privateMethod"),
            BoundaryService.class)).isFalse();
        assertThat(matcher.matches(BoundaryService.class.getMethod("staticMethod"),
            BoundaryService.class)).isFalse();
        assertThat(matcher.matches(Object.class.getMethod("toString"), CombinedService.class)).isFalse();
        assertThat(matcher.matches(CombinedService.class.getMethod("toString"), CombinedService.class)).isFalse();
        assertThat(BoundaryService.staticMethod()).isEqualTo("static");
        verifyNoInteractions(access);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ProfileAccess> provider(ProfileAccess access) {
        ObjectProvider<ProfileAccess> result = mock(ObjectProvider.class);
        when(result.getObject()).thenReturn(access);
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Object target, Class<?> contract, boolean classProxy) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(classProxy);
        if (!classProxy) {
            factory.setInterfaces(contract);
        }
        factory.addAdvisor(advisor);
        Object proxy = factory.getProxy();
        assertThat(AopUtils.isCglibProxy(proxy)).isEqualTo(classProxy);
        assertThat(AopUtils.isJdkDynamicProxy(proxy)).isEqualTo(!classProxy);
        return (T) proxy;
    }

    public interface Action {
        String execute();
    }

    @RequirePersonVerified
    public static class CombinedService implements Action {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        @RequireEnterpriseVerified
        public String execute() {
            calls.incrementAndGet();
            return "done";
        }

        @Override
        public String toString() {
            return "combined service";
        }
    }

    @RequirePersonVerified
    public interface PersonAction extends Action {
    }

    @RequireEnterpriseVerified
    public static class EnterpriseBase {
    }

    public static class InheritedTypeService extends EnterpriseBase implements PersonAction {
        @Override
        public String execute() {
            return "inherited";
        }
    }

    public interface EnterpriseAction extends Action {
        @Override
        @RequireEnterpriseVerified
        String execute();
    }

    public static class PersonMethodBase {
        @RequirePersonVerified
        public String execute() {
            return "base";
        }
    }

    public static class InheritedMethodService extends PersonMethodBase implements EnterpriseAction {
        @Override
        public String execute() {
            return "override";
        }
    }

    public interface GenericAction<T> {
        @RequireEnterpriseVerified
        T execute(T value);
    }

    public static class StringAction implements GenericAction<String> {
        @Override
        @RequirePersonVerified
        public String execute(String value) {
            return value;
        }
    }

    @SaIgnore
    public static class IgnoredService implements Action {
        @Override
        @SaIgnore
        @RequirePersonVerified
        public String execute() {
            return "ignored only by Sa-Token";
        }
    }

    public static final class FinalImplementation implements Action {
        @Override
        @RequirePersonVerified
        public final String execute() {
            return "final";
        }
    }

    public static class BoundaryService {
        final AtomicInteger calls = new AtomicInteger();

        public String unrelated() {
            return "unrelated";
        }

        public String invokeSelf() {
            return guarded();
        }

        @RequirePersonVerified
        public String guarded() {
            calls.incrementAndGet();
            return "guarded";
        }

        @RequirePersonVerified
        public final String finalMethod() {
            return "final";
        }

        @RequirePersonVerified
        private String privateMethod() {
            return "private";
        }

        @RequirePersonVerified
        public static String staticMethod() {
            return "static";
        }
    }
}
