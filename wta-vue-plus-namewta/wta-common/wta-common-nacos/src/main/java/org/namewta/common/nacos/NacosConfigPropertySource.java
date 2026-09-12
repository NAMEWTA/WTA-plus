package org.namewta.common.nacos;

import org.springframework.core.env.EnumerablePropertySource;

final class NacosConfigPropertySource extends EnumerablePropertySource<NacosConfigManager> {

    NacosConfigPropertySource(NacosConfigManager source) {
        super(NacosConfigConstants.PROPERTY_SOURCE_NAME, source);
    }

    @Override
    public String[] getPropertyNames() {
        return source.overlay().keySet().toArray(String[]::new);
    }

    @Override
    public Object getProperty(String name) {
        return source.overlay().get(name);
    }
}
