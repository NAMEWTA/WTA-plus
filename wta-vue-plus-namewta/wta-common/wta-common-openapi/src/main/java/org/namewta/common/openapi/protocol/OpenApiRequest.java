package org.namewta.common.openapi.protocol;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable input to NAMEWTA v1 canonicalization.
 */
public record OpenApiRequest(
    String appKey,
    String timestamp,
    String nonce,
    String method,
    String rawPath,
    String rawQuery,
    byte[] body
) {

    public OpenApiRequest {
        Objects.requireNonNull(appKey, "appKey");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(nonce, "nonce");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(rawPath, "rawPath");
        rawQuery = rawQuery == null ? "" : rawQuery;
        body = body == null ? new byte[0] : Arrays.copyOf(body, body.length);
    }

    @Override
    public byte[] body() {
        return Arrays.copyOf(body, body.length);
    }

}
