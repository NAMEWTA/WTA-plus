package org.namewta.common.openapi.protocol;

/**
 * Stable authentication failure that deliberately hides the rejected stage.
 */
public class OpenApiAuthenticationException extends RuntimeException {

    public static final String ERROR_CODE = "OPENAPI_AUTHENTICATION_FAILED";

    public OpenApiAuthenticationException() {
        super("OpenAPI authentication failed");
    }

    public OpenApiAuthenticationException(Throwable cause) {
        super("OpenAPI authentication failed", cause);
    }

}
