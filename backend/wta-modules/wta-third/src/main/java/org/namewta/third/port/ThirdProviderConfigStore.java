package org.namewta.third.port;

import org.namewta.third.domain.ThirdProvider;

/** Minimal provider read boundary used by the configuration snapshot adapter. */
public interface ThirdProviderConfigStore {
    ThirdProvider findActiveByCode(String providerCode);
}
