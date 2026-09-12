package org.namewta.profile.person.listener;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.person.domain.application.PersonRebindProcessCommand;
import org.namewta.profile.person.usecase.PersonRebindUseCase;
import org.namewta.workflow.api.event.ProcessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * PersonRebindProcessListener Profile 业务组件。
 */
@Service
@RequiredArgsConstructor
public class PersonRebindProcessListener {

    private final PersonRebindUseCase useCase;

    /**
     * 处理 handle 业务步骤。
     */
    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handle(ProcessEvent event) {
        if (event == null) {
            return;
        }
        useCase.handleProcess(new PersonRebindProcessCommand(event.getInstanceId(), event.getBusinessId(),
            event.getFlowCode(), event.getStatus(), normalizeDecision(event.getParams()),
            snapshotVersion(event.getParams()), Instant.now()));
    }

    /**
     * 处理 snapshotVersion 业务步骤。
     */
    private Integer snapshotVersion(Map<String, Object> params) {
        if (params == null) {
            return null;
        }
        Object value = params.get("snapshotVersion");
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** 规范化工作流决定。 */
    private String normalizeDecision(Map<String, Object> params) {
        Object value = params == null ? null : params.get("profileDecision");
        return value == null ? "" : value.toString().strip().toUpperCase(Locale.ROOT);
    }
}
