package org.namewta.common.openapi.session;

/**
 * Narrow invalidation capability shared by credential and authorization writers.
 */
@FunctionalInterface
public interface OpenApiMachineSessionInvalidator {

    int invalidateByUserId(Long userId);

}
