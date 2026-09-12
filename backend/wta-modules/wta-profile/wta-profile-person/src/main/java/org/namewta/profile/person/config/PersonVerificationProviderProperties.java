package org.namewta.profile.person.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/** PersonVerificationProviderProperties 配置模型。 */
@Component
@ConfigurationProperties(prefix = "profile.person.verification")
public class PersonVerificationProviderProperties {

    private Set<String> enabledProviders = new LinkedHashSet<>(Set.of("manual"));

    /** 返回已启用的认证提供方配置。 */
    public Set<String> getEnabledProviders() {
        return Set.copyOf(enabledProviders);
    }

    /** 更新已启用的认证提供方配置。 */
    public void setEnabledProviders(Set<String> enabledProviders) {
        this.enabledProviders = enabledProviders == null
            ? new LinkedHashSet<>()
            : new LinkedHashSet<>(enabledProviders);
    }
}
