package org.namewta.third.spi;

import org.namewta.third.api.ThirdPartyRequest;

public record ThirdAdapterResponse(ThirdPartyRequest request, int httpStatus, Object body,
                                   long durationMs, String requestId) {
}
