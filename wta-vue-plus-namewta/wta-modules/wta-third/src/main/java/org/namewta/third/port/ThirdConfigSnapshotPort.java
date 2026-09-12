package org.namewta.third.port;

/** Provides an effective provider/endpoint snapshot and invalidation boundary. */
public interface ThirdConfigSnapshotPort {
    ThirdConfigSnapshot get(String providerCode, String endpointCode);

    void evict(String providerCode, String endpointCode);
}
