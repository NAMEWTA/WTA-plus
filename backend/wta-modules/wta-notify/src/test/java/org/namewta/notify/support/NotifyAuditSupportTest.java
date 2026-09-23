package org.namewta.notify.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.notify.domain.entity.NotifyIntent;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class NotifyAuditSupportTest {
    @Test
    void missingOrDamagedIntentFailsClosedButLegacyFullRemainsVisible() {
        assertNull(NotifyAuditSupport.publicProviderMessageId(null, "13812345678"));
        NotifyIntent intent = new NotifyIntent();
        intent.setMetadataJson("{broken");
        assertTrue(NotifyAuditSupport.redactSensitive(intent));
        assertNull(NotifyAuditSupport.publicProviderMessageId(intent, "1234"));
        intent.setMetadataJson("{\"audit\":\"unexpected\"}");
        assertNull(NotifyAuditSupport.publicProviderMessageId(intent, "1234"));
        intent.setMetadataJson("{}");
        assertFalse(NotifyAuditSupport.redactSensitive(intent));
        assertEquals("1234", NotifyAuditSupport.publicProviderMessageId(intent, "1234"));
    }
}
