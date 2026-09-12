package org.namewta.common.openapi.spi;

/**
 * Optional metering hook. Implementations must not change an already determined HTTP response.
 */
@FunctionalInterface
public interface OpenApiCallEventPublisher {

    void publish(OpenApiCallEvent event);

}
