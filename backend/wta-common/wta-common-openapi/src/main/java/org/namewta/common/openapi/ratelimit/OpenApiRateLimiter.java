package org.namewta.common.openapi.ratelimit;

import java.time.Duration;

/**
 * Atomic distributed rate limiter for one opaque OpenAPI scope.
 */
@FunctionalInterface
public interface OpenApiRateLimiter {

    boolean acquire(String scope, int limit, Duration interval);

}
