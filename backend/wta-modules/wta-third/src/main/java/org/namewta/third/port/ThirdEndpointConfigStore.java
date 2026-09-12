package org.namewta.third.port;

import org.namewta.third.domain.ThirdEndpoint;

import java.util.List;

/** Minimal endpoint read boundary used by the configuration snapshot adapter. */
public interface ThirdEndpointConfigStore {
    ThirdEndpoint findActiveByProviderAndCode(Long providerId, String endpointCode);

    List<ThirdEndpoint> findAllByProviderCode(String providerCode);

    List<ThirdEndpoint> findAllWithAdapter();
}
