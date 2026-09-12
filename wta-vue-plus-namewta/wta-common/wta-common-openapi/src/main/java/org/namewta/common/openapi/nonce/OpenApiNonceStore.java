package org.namewta.common.openapi.nonce;

import java.time.Duration;

/**
 * Atomically registers one credential nonce for its replay-protection window.
 */
@FunctionalInterface
public interface OpenApiNonceStore {

    boolean register(String appKey, String nonce, Duration ttl);

}
