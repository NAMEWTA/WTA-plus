package org.namewta.third.port;

import org.namewta.third.api.ThirdPartyRequest;
import org.namewta.third.api.ThirdPartyResponse;

import java.util.Set;

/** Logical invocation and outbound audit recording boundary. */
public interface ThirdInvocationRecorderPort {
    void record(ThirdPartyRequest request, ThirdPartyResponse<?> response, long durationMs, int attempts);

    /** Records one physical outbound attempt; implementations must remain best effort. */
    default void recordAttempt(ThirdOutboundAttempt attempt) {
    }

    default void record(ThirdPartyRequest request, ThirdPartyResponse<?> response, long durationMs, int attempts,
                        Set<String> additionalSensitiveFields) {
        record(request, response, durationMs, attempts);
    }
}
