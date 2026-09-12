package org.namewta.third.port;

import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.api.ThirdPartyRequest;

import java.util.Map;
import java.util.Set;

/** Sanitizable details for one physical HTTP send, separate from logical invocation persistence. */
public record ThirdOutboundAttempt(
    ThirdPartyRequest request,
    String requestId,
    int attempt,
    String relativePath,
    Map<String, String> effectiveHeaders,
    Object requestBody,
    Integer httpStatus,
    ThirdPartyFailureCategory category,
    Object responseBody,
    long durationMs,
    boolean completed,
    Set<String> additionalSensitiveFields
) {
    public ThirdOutboundAttempt {
        effectiveHeaders = effectiveHeaders == null ? Map.of() : Map.copyOf(effectiveHeaders);
        additionalSensitiveFields = additionalSensitiveFields == null ? Set.of() : Set.copyOf(additionalSensitiveFields);
    }
}
