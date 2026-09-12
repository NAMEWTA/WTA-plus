package org.namewta.third.spi;

import org.namewta.third.api.ThirdPartyResponse;

/** Provider-specific signing, encryption and business-error mapping extension. */
public interface ThirdProviderAdapter {
    String providerCode();

    /**
     * Optional endpoint hook identifier. A provider remains a single Spring bean;
     * this identifier lets endpoint metadata assert that the configured hook is
     * owned by that provider adapter without enabling class-name reflection.
     */
    default String adapterCode() {
        return providerCode();
    }

    /** Provider adapters may explicitly limit the endpoint codes they own. */
    default boolean supportsEndpoint(String endpointCode) {
        return true;
    }

    default ThirdAdapterRequest prepare(ThirdAdapterRequest request) {
        return request;
    }

    default ThirdPartyResponse<?> mapResponse(ThirdAdapterResponse response) {
        return null;
    }
}
