package org.namewta.common.openapi.spi;

/**
 * Resolves an enabled, non-expired credential without revealing why a lookup failed.
 */
@FunctionalInterface
public interface OpenApiCredentialResolver {

    OpenApiCredential resolve(String appKey);

}
