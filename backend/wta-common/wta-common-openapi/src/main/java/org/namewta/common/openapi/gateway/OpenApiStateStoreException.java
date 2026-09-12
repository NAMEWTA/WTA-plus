package org.namewta.common.openapi.gateway;

/**
 * Stable fail-closed wrapper for nonce and rate-limit infrastructure failures.
 */
public class OpenApiStateStoreException extends RuntimeException {

    public OpenApiStateStoreException(Throwable cause) {
        super("OpenAPI state store unavailable", cause);
    }
}
