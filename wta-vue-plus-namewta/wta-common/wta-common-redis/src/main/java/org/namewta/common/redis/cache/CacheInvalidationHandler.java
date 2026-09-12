package org.namewta.common.redis.cache;

/**
 * Evicts one process-local cache without exposing its backing store.
 */
public interface CacheInvalidationHandler {

    /**
     * Invalidates the local key matching a SHA-256 fingerprint.
     *
     * @param keyFingerprint key fingerprint
     */
    void invalidate(String keyFingerprint);

    /**
     * Clears the local namespace.
     */
    void clear();
}
