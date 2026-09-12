package org.namewta.common.nacos;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class NacosYamlOverlay {

    private NacosYamlOverlay() {
    }

    static Map<String, Object> parse(String yaml) {
        if (!StringUtils.hasText(yaml)) {
            return Map.of();
        }
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(
                NacosConfigConstants.PROPERTY_SOURCE_NAME,
                new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8))
            );
            Map<String, Object> values = new LinkedHashMap<>();
            for (PropertySource<?> source : sources) {
                if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
                    throw new NacosConfigValidationException("INVALID_YAML");
                }
                for (String name : enumerable.getPropertyNames()) {
                    Object value = enumerable.getProperty(name);
                    if (value != null) {
                        values.put(name, value);
                    }
                }
            }
            return Map.copyOf(values);
        } catch (NacosConfigValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new NacosConfigValidationException("INVALID_YAML", ex);
        }
    }
}
