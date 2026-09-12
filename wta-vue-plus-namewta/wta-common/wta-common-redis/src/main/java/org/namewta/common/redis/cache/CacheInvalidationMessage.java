package org.namewta.common.redis.cache;

/**
 * Secret-free invalidation message exchanged between application nodes.
 *
 * @param requestId     unique invalidation request
 * @param namespace     registered local cache namespace
 * @param action        key or namespace invalidation
 * @param keyFingerprint SHA-256 key fingerprint for {@link CacheInvalidationAction#KEY}
 */
public record CacheInvalidationMessage(
    String requestId,
    String namespace,
    CacheInvalidationAction action,
    String keyFingerprint
) {
}
