package org.namewta.third.spi;

import org.namewta.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ThirdProviderAdapterRegistry {
    private final Map<String, ThirdProviderAdapter> adapters;

    public ThirdProviderAdapterRegistry(List<ThirdProviderAdapter> values) {
        this.adapters = values.stream().collect(Collectors.toUnmodifiableMap(
            value -> canonical(value.providerCode()), Function.identity(), (left, right) -> {
                throw new ServiceException("Duplicate third provider adapter: " + left.providerCode());
            }));
    }

    public ThirdProviderAdapter find(String providerCode) {
        return providerCode == null ? null : adapters.get(providerCode.trim());
    }

    public ThirdProviderAdapter find(String providerCode, String endpointCode, String configuredAdapterCode) {
        ThirdProviderAdapter adapter = find(providerCode);
        if (adapter == null) {
            if (configuredAdapterCode != null && !configuredAdapterCode.isBlank()) {
                throw new ServiceException("Third endpoint adapter is unavailable");
            }
            return null;
        }
        if (configuredAdapterCode != null && !configuredAdapterCode.isBlank()
            && !canonical(configuredAdapterCode).equals(canonical(adapter.adapterCode()))) {
            throw new ServiceException("Third endpoint adapter is not owned by provider");
        }
        if (endpointCode == null || endpointCode.isBlank() || !adapter.supportsEndpoint(endpointCode.trim())) {
            throw new ServiceException("Third endpoint adapter does not support endpoint");
        }
        return adapter;
    }

    private static String canonical(String providerCode) {
        String value = providerCode == null ? "" : providerCode.trim();
        if (value.isBlank()) throw new ServiceException("Third provider adapter code is required");
        return value;
    }
}
