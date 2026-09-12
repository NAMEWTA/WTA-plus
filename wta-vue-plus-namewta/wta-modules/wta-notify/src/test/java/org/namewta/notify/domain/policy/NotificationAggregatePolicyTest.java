package org.namewta.notify.domain.policy;

import org.namewta.notify.api.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 通知聚合状态策略测试。 */
@Tag("dev")
class NotificationAggregatePolicyTest {
    @Test
    void acceptedIsNotDelivered() {
        assertEquals(NotificationStatus.ACCEPTED,
            NotificationAggregatePolicy.aggregate(List.of("ACCEPTED")));
    }

    @Test
    void deliveredAndFailureIsPartialFailure() {
        assertEquals(NotificationStatus.PARTIAL_FAILURE,
            NotificationAggregatePolicy.aggregate(List.of("DELIVERED", "FAILED")));
    }

    @Test
    void cancelledFallbackDoesNotHideSuccessfulChannel() {
        assertEquals(NotificationStatus.ACCEPTED,
            NotificationAggregatePolicy.aggregate(List.of("ACCEPTED", "CANCELLED")));
    }
}
