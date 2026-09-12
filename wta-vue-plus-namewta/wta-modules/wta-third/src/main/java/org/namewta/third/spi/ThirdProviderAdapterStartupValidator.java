package org.namewta.third.spi;

import lombok.RequiredArgsConstructor;
import org.namewta.third.port.ThirdEndpointConfigStore;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/** Validates every database endpoint that explicitly requires a provider adapter. */
@Component
@RequiredArgsConstructor
public class ThirdProviderAdapterStartupValidator implements SmartInitializingSingleton {
    private final ThirdEndpointConfigStore endpointStore;
    private final ThirdProviderAdapterRegistry adapterRegistry;

    @Override
    public void afterSingletonsInstantiated() {
        endpointStore.findAllWithAdapter().forEach(endpoint -> adapterRegistry.find(
            endpoint.getProviderCode(), endpoint.getEndpointCode(), endpoint.getAdapterCode()));
    }
}
