package org.namewta.common.mail.config;

import cn.hutool.extra.mail.MailAccount;
import org.namewta.common.mail.config.properties.MailProperties;
import org.namewta.common.mail.core.MailBuilder;
import org.namewta.common.mail.notify.MailAccountResolver;
import org.namewta.common.mail.notify.MailNotificationSender;
import org.namewta.common.mail.notify.MailNotifyChannelAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.File;

/**
 * JavaMail 配置。运行时发件账户由通知控制面解析，不再依赖 YAML mail.enabled。
 */
@AutoConfiguration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    /**
     * 按消息携带的 SMTP 账户发送邮件。
     *
     * @return 邮件发送器
     */
    @Bean
    @ConditionalOnMissingBean
    public MailNotificationSender mailNotificationSender() {
        return message -> {
            MailAccount account = message.account();
            MailBuilder builder = account == null ? MailBuilder.of() : MailBuilder.of(account);
            return builder
                .to(message.to())
                .cc(message.cc())
                .bcc(message.bcc())
                .subject(message.subject())
                .content(message.content(), message.html())
                .files(message.attachments().stream().map(path -> path.toFile()).toArray(File[]::new))
                .send();
        };
    }

    /**
     * 注册邮件渠道 Adapter。
     *
     * @param sender    发送器
     * @param resolvers 控制面账户解析器
     * @return 邮件 Adapter
     */
    @Bean
    @ConditionalOnMissingBean
    public MailNotifyChannelAdapter mailNotifyChannelAdapter(MailNotificationSender sender,
                                                             ObjectProvider<MailAccountResolver> resolvers) {
        return new MailNotifyChannelAdapter(sender, resolvers.getIfAvailable());
    }

}
