package org.namewta.test.notify.core;

import org.namewta.common.mail.config.MailConfig;
import org.namewta.common.notify.config.NotifyAutoConfiguration;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.model.NotifyChannel;
import org.namewta.common.notify.registry.NotifyChannelRegistry;
import org.namewta.common.sms.config.SmsAutoConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 统一通知自动装配测试。
 */
@Tag("dev")
class NotifyAutoConfigurationUnitTest {

    @Test
    void shouldDiscoverEnabledMailAndSmsAdapters() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                NotifyAutoConfiguration.class,
                MailConfig.class,
                SmsAutoConfiguration.class))
            .withPropertyValues(
                "mail.enabled=true",
                "mail.host=localhost",
                "mail.port=25",
                "mail.auth=false",
                "mail.starttls-enable=false",
                "mail.ssl-enable=false",
                "mail.timeout=5000",
                "mail.connection-timeout=5000",
                "mail.from=no-reply@example.com")
            .run(context -> {
                context.getBean(NotifyClient.class);
                NotifyChannelRegistry registry = context.getBean(NotifyChannelRegistry.class);
                assertDoesNotThrow(() -> registry.require(NotifyChannel.MAIL));
                assertDoesNotThrow(() -> registry.require(NotifyChannel.SMS));
            });
    }
}
