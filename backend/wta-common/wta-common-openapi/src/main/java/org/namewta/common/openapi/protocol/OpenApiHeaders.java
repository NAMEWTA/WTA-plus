package org.namewta.common.openapi.protocol;

/**
 * NAMEWTA v1 request header names.
 */
public final class OpenApiHeaders {

    public static final String VERSION = "X-OpenAPI-Version";
    public static final String APP_KEY = "X-App-Key";
    public static final String TIMESTAMP = "X-Timestamp";
    public static final String NONCE = "X-Nonce";
    public static final String SIGNATURE = "X-Signature";

    private OpenApiHeaders() {
    }

}
