package org.namewta.third.adapter.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/** Exposes log-sink failures without making observability failures fail gateway calls. */
@Component("thirdOutboundLog")
@RequiredArgsConstructor
public class ThirdInvocationLogHealthIndicator implements HealthIndicator {
    private final ThirdInvocationRecorderAdapter recorder;

    @Override
    public Health health() {
        long failures = recorder.logSinkFailureCount();
        Health.Builder builder = failures == 0 ? Health.up() : Health.down();
        return builder.withDetail("sinkFailures", failures).build();
    }
}
