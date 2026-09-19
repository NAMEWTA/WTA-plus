package org.namewta.common.web.config;

import org.namewta.common.web.logging.SysLogFilter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class SysLogConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SysLogConfig.class))
        .withBean(JsonMapper.class, () -> JsonMapper.builder().build());

    @Test
    void enablesFilterByDefaultAndAllowsExplicitDisable() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SysLogFilter.class);
            assertThat(context.getBean(SysLogProperties.class).getMaxBodySize().toBytes())
                .isEqualTo(1024 * 1024);
        });

        contextRunner.withPropertyValues("sys.log.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SysLogFilter.class);
        });
    }

    @Test
    void rejectsZeroAndNegativeBodyLimits() {
        contextRunner.withPropertyValues("sys.log.max-body-size=0B")
            .run(context -> assertThat(context).hasFailed());
        contextRunner.withPropertyValues("sys.log.max-body-size=-1B")
            .run(context -> assertThat(context).hasFailed());
        contextRunner.withPropertyValues("sys.log.max-body-size=not-a-size")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void ordinaryIngressLimitHasAnIndependentDefaultAndValidatedOverride() {
        contextRunner.run(context -> assertThat(context.getBean(
            org.namewta.common.web.config.properties.RequestBodyProperties.class).maxBytes()).isEqualTo(2 * 1024 * 1024));
        contextRunner.withPropertyValues("namewta.web.request-body.max-size=3MB", "sys.log.max-body-size=16KB")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(org.namewta.common.web.config.properties.RequestBodyProperties.class).maxBytes())
                    .isEqualTo(3 * 1024 * 1024);
                assertThat(context.getBean(SysLogProperties.class).maxBodyBytes()).isEqualTo(16 * 1024);
            });
        for (String value : new String[]{"0B", "-1B", "3GB", "invalid"}) {
            contextRunner.withPropertyValues("namewta.web.request-body.max-size=" + value)
                .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    void filterOrderKeepsLoggingAfterRepeatableAndBeforeXss() {
        int repeatableOrder = registration(FilterConfig.class, "repeatableFilter").order();
        int sysLogOrder = registration(SysLogConfig.class, "sysLogFilter").order();
        int xssOrder = registration(FilterConfig.class, "xssFilter").order();

        assertThat(repeatableOrder).isEqualTo(FilterRegistrationBean.HIGHEST_PRECEDENCE + 2);
        assertThat(sysLogOrder).isEqualTo(repeatableOrder + 1);
        assertThat(xssOrder).isEqualTo(sysLogOrder + 1);
    }

    private FilterRegistration registration(Class<?> configurationClass, String methodName) {
        for (Method method : configurationClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method.getAnnotation(FilterRegistration.class);
            }
        }
        throw new AssertionError("Missing filter registration method " + methodName);
    }
}
