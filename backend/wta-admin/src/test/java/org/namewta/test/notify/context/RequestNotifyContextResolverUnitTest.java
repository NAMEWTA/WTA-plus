package org.namewta.test.notify.context;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff;
import cn.dev33.satoken.context.model.SaTokenContextModelBox;
import org.namewta.common.notify.model.NotifyContext;
import org.namewta.common.notify.config.NotifyAutoConfiguration;
import org.namewta.common.notify.spi.NotifyContextResolver;
import org.namewta.system.api.model.LoginUser;
import org.namewta.web.config.NotifyContextConfiguration;
import org.namewta.web.config.RequestNotifyContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class RequestNotifyContextResolverUnitTest {

    @Test
    void actualApplicationBeanResolvesBackgroundThreadWithoutWebContext() {
        SaTokenContextModelBox previous = SaTokenContextForThreadLocalStaff.getModelBoxOrNull();
        RequestAttributes previousRequest = RequestContextHolder.getRequestAttributes();
        try {
            SaTokenContextForThreadLocalStaff.clearModelBox();
            RequestContextHolder.resetRequestAttributes();
            assertThat(SaManager.getSaTokenContext().isValid()).isFalse();
            new ApplicationContextRunner()
                .withUserConfiguration(NotifyContextConfiguration.class)
                .run(context -> assertThat(context.getBean(NotifyContextResolver.class).resolve())
                    .isEqualTo(NotifyContext.empty()));
        } finally {
            if (previous == null) SaTokenContextForThreadLocalStaff.clearModelBox();
            else SaTokenContextForThreadLocalStaff.setModelBox(previous.getRequest(), previous.getResponse(), previous.getStorage());
            if (previousRequest == null) RequestContextHolder.resetRequestAttributes();
            else RequestContextHolder.setRequestAttributes(previousRequest);
        }
    }

    @Test
    void applicationConfigurationShouldReplaceEmptyAutoConfigurationFallback() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotifyAutoConfiguration.class))
            .withUserConfiguration(NotifyContextConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(NotifyContextResolver.class);
                assertThat(context.getBean(NotifyContextResolver.class))
                    .isInstanceOf(RequestNotifyContextResolver.class);
            });
    }

    @Test
    void shouldSnapshotAuthenticatedRequestSource() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(12L);
        loginUser.setClientPk(34L);
        RequestNotifyContextResolver resolver = new RequestNotifyContextResolver(() -> loginUser, () -> "trace-1");

        NotifyContext context = resolver.resolve();

        assertThat(context.userId()).isEqualTo(12L);
        assertThat(context.clientPk()).isEqualTo(34L);
        assertThat(context.traceId()).isEqualTo("trace-1");
    }

    @Test
    void shouldAllowBackgroundExecutionWithoutAuthenticationOrClientScope() {
        RequestNotifyContextResolver resolver = new RequestNotifyContextResolver(() -> null, () -> null);

        NotifyContext context = resolver.resolve();

        assertThat(context).isEqualTo(NotifyContext.empty());
    }
}
