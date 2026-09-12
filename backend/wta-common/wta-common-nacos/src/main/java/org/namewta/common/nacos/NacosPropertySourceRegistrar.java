package org.namewta.common.nacos;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

final class NacosPropertySourceRegistrar {

    private static final String COMMAND_LINE_PROPERTY_SOURCE_NAME = "commandLineArgs";

    private NacosPropertySourceRegistrar() {
    }

    static void register(ConfigurableEnvironment environment, NacosConfigPropertySource propertySource) {
        MutablePropertySources sources = environment.getPropertySources();
        sources.remove(NacosConfigConstants.PROPERTY_SOURCE_NAME);
        if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
        } else if (sources.contains(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, propertySource);
        } else if (sources.contains(COMMAND_LINE_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(COMMAND_LINE_PROPERTY_SOURCE_NAME, propertySource);
        } else {
            sources.addFirst(propertySource);
        }
    }
}
