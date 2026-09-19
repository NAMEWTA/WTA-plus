package org.namewta.third.port;

import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.support.ThirdLimitLease;

/** Pre-send rate/concurrency and retry policy boundary. */
public interface ThirdResiliencePort {
    ThirdLimitLease acquire(ThirdProvider provider, ThirdEndpoint endpoint);

    int maxAttempts(ThirdEndpoint endpoint);

    /** Apply committed configuration on existing quota keys, without acquiring a permit. */
    void refresh(ThirdProvider provider, ThirdEndpoint endpoint);
}
