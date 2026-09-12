package org.namewta.common.openapi.gateway;

/**
 * Non-authentication gateway rejection with a stable public category.
 */
public final class OpenApiGatewayException extends RuntimeException {

    public static final String FORBIDDEN = "OPENAPI_FORBIDDEN";
    public static final String RATE_LIMITED = "OPENAPI_RATE_LIMITED";
    public static final String UNAVAILABLE = "OPENAPI_UNAVAILABLE";

    private final int status;
    private final String errorCode;

    private OpenApiGatewayException(int status, String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static OpenApiGatewayException forbidden() {
        return new OpenApiGatewayException(403, FORBIDDEN, null);
    }

    public static OpenApiGatewayException rateLimited() {
        return new OpenApiGatewayException(429, RATE_LIMITED, null);
    }

    public static OpenApiGatewayException unavailable(Throwable cause) {
        return new OpenApiGatewayException(503, UNAVAILABLE, cause);
    }

    public int status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }
}
