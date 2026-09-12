package org.namewta.common.nacos;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Arrays;

record NacosConfigSettings(boolean enabled, String serverAddr, String username, String password,
                           String group, String dataId, long timeoutMs, String profile, String namespace) {

    static NacosConfigSettings from(Environment environment) {
        String profile = resolveProfile(environment);
        String namespace = environment.getProperty("nacos.config.namespaces." + profile, profile);
        return new NacosConfigSettings(
            environment.getProperty("nacos.config.enabled", Boolean.class, false),
            valueOrDefault(environment.getProperty("nacos.config.server-addr"), NacosConfigConstants.DEFAULT_SERVER_ADDR),
            environment.getProperty("nacos.config.username", ""),
            environment.getProperty("nacos.config.password", ""),
            valueOrDefault(environment.getProperty("nacos.config.group"), NacosConfigConstants.DEFAULT_GROUP),
            valueOrDefault(environment.getProperty("nacos.config.data-id"), NacosConfigConstants.DEFAULT_DATA_ID),
            positiveTimeout(environment.getProperty("nacos.config.timeout-ms", Long.class,
                NacosConfigConstants.DEFAULT_TIMEOUT_MS)),
            profile,
            valueOrDefault(namespace, profile)
        );
    }

    private static String resolveProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
            .filter(NacosConfigConstants.SUPPORTED_PROFILES::contains)
            .findFirst()
            .orElseGet(() -> {
                String configured = environment.getProperty("spring.profiles.active", "local");
                return Arrays.stream(configured.split(","))
                    .map(String::trim)
                    .filter(NacosConfigConstants.SUPPORTED_PROFILES::contains)
                    .findFirst()
                    .orElse("local");
            });
    }

    private static String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static long positiveTimeout(Long timeout) {
        return timeout != null && timeout > 0 ? timeout : NacosConfigConstants.DEFAULT_TIMEOUT_MS;
    }
}
