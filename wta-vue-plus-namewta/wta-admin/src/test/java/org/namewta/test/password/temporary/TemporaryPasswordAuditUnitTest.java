package org.namewta.test.password.temporary;

import org.namewta.common.log.event.OperLogEvent;
import org.namewta.system.temporarypassword.TemporaryPasswordAuditEnricher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("dev")
class TemporaryPasswordAuditUnitTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void enrichesAuditWithTargetAndTtlWithoutCredentialMaterial() throws Exception {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        JsonMapper jsonMapper = JsonMapper.builder().build();
        TemporaryPasswordAuditEnricher enricher = new TemporaryPasswordAuditEnricher(jsonMapper);
        enricher.attachTarget(42L);
        OperLogEvent event = new OperLogEvent();
        event.setTitle(TemporaryPasswordAuditEnricher.AUDIT_TITLE);

        enricher.enrich(event);

        Map<String, Object> metadata = jsonMapper.readValue(event.getOperParam(), new TypeReference<>() {
        });
        assertEquals(42, metadata.get("targetUserId"));
        assertEquals(60, metadata.get("expiresInSeconds"));
        assertFalse(event.getOperParam().contains("password"));
        assertFalse(event.getOperParam().contains("hash"));
    }
}
