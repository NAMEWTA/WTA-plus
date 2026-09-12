package org.namewta.common.openapi.session;

/**
 * Stable fail-closed error for the internal machine-session boundary.
 */
public class OpenApiMachineSessionException extends RuntimeException {

    public OpenApiMachineSessionException() {
        super("OpenAPI machine session is unavailable");
    }

    public OpenApiMachineSessionException(Throwable cause) {
        super("OpenAPI machine session is unavailable", cause);
    }

}
