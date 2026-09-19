package org.namewta.common.log.aspect;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.event.OperLogEvent;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class LogAspectRedactionTest {

    @Log(title = "audit metadata", isSaveRequestData = false)
    void audited() {
    }

    @Test
    void sanitizesNestedResponseBeforeCreatingEventWithoutMutatingBusinessResult() throws Exception {
        String canary = "credential-canary-operation";
        Map<String, Object> response = Map.of("code", 200, "data", List.of(Map.of("access_token", canary, "name", "visible")));
        OperLogEvent event = new OperLogEvent();
        Log annotation = getClass().getDeclaredMethod("audited").getAnnotation(Log.class);

        new LogAspect().getControllerMethodDescription(null, annotation, event, response);

        assertFalse(event.getJsonResult().contains(canary), "Credential must not reach the operation event");
        assertTrue(event.getJsonResult().contains("visible"));
        assertTrue(event.getJsonResult().contains("200"));
        assertEquals("audit metadata", event.getTitle());
        assertTrue(response.toString().contains(canary), "Business result must remain unchanged");
    }
}
