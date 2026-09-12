package org.namewta.third.support;

import org.namewta.third.api.ThirdPartyFailureCategory;

public class ThirdRejectedException extends RuntimeException {
    private final ThirdPartyFailureCategory category;

    public ThirdRejectedException(ThirdPartyFailureCategory category, String message) {
        super(message);
        this.category = category;
    }

    public ThirdPartyFailureCategory category() {
        return category;
    }
}
