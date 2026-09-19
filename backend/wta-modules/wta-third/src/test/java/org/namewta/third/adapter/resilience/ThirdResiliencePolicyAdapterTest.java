package org.namewta.third.adapter.resilience;

import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
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

    @Test
    void permitBudgetCoversEveryConfiguredAttemptAndUsesLongArithmetic() {
        ThirdProvider provider = new ThirdProvider();
        ThirdEndpoint endpoint = new ThirdEndpoint();
        assertEquals(14000, policy.leaseMillis(provider, endpoint));
        provider.setTimeoutConnectMs(120000);
        provider.setTimeoutReadMs(300000);
        endpoint.setIdempotent(true); endpoint.setRetryCount(3);
        assertEquals(1681000, policy.leaseMillis(provider, endpoint));
        provider.setTimeoutConnectMs(Integer.MAX_VALUE); provider.setTimeoutReadMs(Integer.MAX_VALUE);
        assertEquals(17179870176L, policy.leaseMillis(provider, endpoint));
    }
}
