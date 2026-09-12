package org.namewta.common.redis.cache;

/**
 * Indicates that a local or cluster cache invalidation was not confirmed.
 */
public class ClusterCacheInvalidationException extends RuntimeException {

    public ClusterCacheInvalidationException(String message) {
        super(message);
    }

    public ClusterCacheInvalidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
