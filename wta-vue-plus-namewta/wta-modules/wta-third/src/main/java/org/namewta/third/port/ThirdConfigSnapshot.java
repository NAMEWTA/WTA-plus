package org.namewta.third.port;

import lombok.Data;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;

@Data
public class ThirdConfigSnapshot {
    private ThirdProvider provider;
    private ThirdEndpoint endpoint;

    public ThirdConfigSnapshot() {
    }

    public ThirdConfigSnapshot(ThirdProvider provider, ThirdEndpoint endpoint) {
        this.provider = provider;
        this.endpoint = endpoint;
    }
}
