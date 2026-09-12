package org.namewta.third.adapter.resilience;

import org.namewta.third.domain.ThirdEndpoint;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("local")
class ThirdResiliencePolicyAdapterTest {
    private final ThirdResiliencePolicyAdapter policy = new ThirdResiliencePolicyAdapter(null);

    @Test
    void onlyIdempotentEndpointsMayUseBoundedRetries() {
        ThirdEndpoint nonIdempotent = new ThirdEndpoint();
        nonIdempotent.setIdempotent(false);
        nonIdempotent.setRetryCount(3);
        assertEquals(1, policy.maxAttempts(nonIdempotent));

        ThirdEndpoint idempotent = new ThirdEndpoint();
        idempotent.setIdempotent(true);
        idempotent.setRetryCount(2);
        assertEquals(3, policy.maxAttempts(idempotent));

        idempotent.setRetryCount(99);
        assertEquals(4, policy.maxAttempts(idempotent));
    }
}
