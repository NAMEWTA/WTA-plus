package org.namewta.profile.enterprise.listener;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.enterprise.domain.application.EnterpriseApplicationProcessCommand;
import org.namewta.profile.enterprise.usecase.EnterpriseApplicationUseCase;
import org.namewta.workflow.api.event.ProcessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/** 企业申请工作流事件监听器，仅负责事件解析并转发 UseCase。 */
@Service
@RequiredArgsConstructor
public class EnterpriseApplicationProcessListener {

    private final EnterpriseApplicationUseCase useCase;

    /** 接收工作流事件并转发企业申请用例。 */
    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handle(ProcessEvent event) {
        if (event == null) {
            return;
        }
        Map<String, Object> params = event.getParams();
        useCase.handleProcess(new EnterpriseApplicationProcessCommand(event.getInstanceId(), event.getBusinessId(),
            event.getFlowCode(), event.getStatus(), decision(params), snapshotVersion(params), Instant.now()));
    }

    /** 提取工作流决定。 */
    private String decision(Map<String, Object> params) {
        Object value = params == null ? null : params.get("profileDecision");
        return value == null ? "" : value.toString().strip().toUpperCase(Locale.ROOT);
    }

    /** 提取流程携带的快照版本。 */
    private Integer snapshotVersion(Map<String, Object> params) {
        Object value = params == null ? null : params.get("snapshotVersion");
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
