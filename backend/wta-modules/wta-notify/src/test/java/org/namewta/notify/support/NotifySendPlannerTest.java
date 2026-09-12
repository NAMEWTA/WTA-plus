package org.namewta.notify.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 收件人限额键使用规范化 SHA-256 摘要，而不是 {@code hashCode}。
 */
@Tag("dev")
class NotifySendPlannerTest {

    @Test
    void recipientQuotaTokenIsStableDigestNotHashCode() {
        String mixed = NotifySendPlanner.recipientQuotaToken(" User@Example.com ");
        String folded = NotifySendPlanner.recipientQuotaToken("user@example.com");
        String other = NotifySendPlanner.recipientQuotaToken("other@example.com");
        String hashCodeHex = Integer.toHexString("user@example.com".hashCode());

        assertEquals(mixed, folded);
        assertEquals(16, mixed.length());
        assertNotEquals(other, folded);
        assertNotEquals(hashCodeHex, folded);
        assertEquals(folded, NotifySendPlanner.recipientQuotaToken("user@example.com"));
    }
}
