package org.namewta.notify.domain.policy;

import org.namewta.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 公告发送范围和渠道规范化回归测试。 */
@Tag("dev")
class NoticeAudiencePolicyTest {

    @Test
    void nullTypeDefaultsToAllAndNullChannelsDefaultsToInApp() {
        var audience = NoticeAudiencePolicy.normalize(null, null, null, null);

        assertEquals(new NoticeAudiencePolicy.Audience("ALL", List.of(), List.of(), List.of("IN_APP")), audience);
    }

    @Test
    void idsArePositiveDeduplicatedAndKeepFirstSeenOrder() {
        var audience = NoticeAudiencePolicy.normalize(" user ", List.of(9L, 3L, 9L, 7L), List.of(), List.of("sms", "SMS"));

        assertEquals(List.of(9L, 3L, 7L), audience.recipientIds());
        assertEquals(List.of("SMS"), audience.channels());
    }

    @Test
    void allAndUserTypeRejectMismatchedTargetLists() {
        assertThrows(ServiceException.class, () -> NoticeAudiencePolicy.normalize("ALL", List.of(1L), List.of(), List.of("IN_APP")));
        assertThrows(ServiceException.class, () -> NoticeAudiencePolicy.normalize("USER_TYPE", List.of(), List.of(), List.of("IN_APP")));
        assertThrows(ServiceException.class, () -> NoticeAudiencePolicy.normalize("USER", List.of(), List.of(), List.of("IN_APP")));
        assertThrows(ServiceException.class, () -> NoticeAudiencePolicy.normalize(" ", List.of(), List.of(), List.of("IN_APP")));
    }

    @Test
    void explicitEmptyOrNullOrUnknownChannelIsRejected() {
        assertThrows(ServiceException.class, () -> NoticeAudiencePolicy.normalize("ALL", List.of(), List.of(), List.of()));
        assertThrows(ServiceException.class, () -> NoticeAudiencePolicy.normalize("ALL", List.of(), List.of(), Arrays.asList("IN_APP", null)));
        assertThrows(ServiceException.class, () -> NoticeAudiencePolicy.normalize("ALL", List.of(), List.of(), List.of("PUSH")));
        assertThrows(ServiceException.class, () -> NoticeAudiencePolicy.normalize("ALL", List.of(-1L), List.of(), null));
    }
}
