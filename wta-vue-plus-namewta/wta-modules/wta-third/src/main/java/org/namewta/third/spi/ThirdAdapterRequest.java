package org.namewta.third.spi;

import org.namewta.third.api.ThirdPartyRequest;
import org.namewta.third.port.ThirdConfigSnapshot;

import java.util.Map;

public record ThirdAdapterRequest(ThirdPartyRequest request, ThirdConfigSnapshot snapshot,
                                  Map<String, String> headers, Object body) {
}
