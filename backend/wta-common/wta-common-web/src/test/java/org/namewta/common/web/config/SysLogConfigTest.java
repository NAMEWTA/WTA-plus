package org.namewta.common.web.config;

import org.namewta.common.encrypt.config.ApiDecryptAutoConfiguration;
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
    void filterOrderKeepsLoggingInsideCryptoAndBeforeXss() throws Exception {
        int cryptoOrder = registration(ApiDecryptAutoConfiguration.class, "cryptoFilter").order();
        int repeatableOrder = registration(FilterConfig.class, "repeatableFilter").order();
        int sysLogOrder = registration(SysLogConfig.class, "sysLogFilter").order();
        int xssOrder = registration(FilterConfig.class, "xssFilter").order();

        assertThat(cryptoOrder).isEqualTo(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        assertThat(repeatableOrder).isEqualTo(cryptoOrder + 1);
        assertThat(sysLogOrder).isEqualTo(cryptoOrder + 2);
        assertThat(xssOrder).isEqualTo(cryptoOrder + 3);
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
