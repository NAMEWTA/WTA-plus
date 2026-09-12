package org.namewta.profile.person.listener;

import org.namewta.profile.person.domain.application.PersonRebindProcessCommand;
import org.namewta.profile.person.usecase.PersonRebindUseCase;
import org.namewta.workflow.api.event.ProcessEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 验证工作流监听器只负责解析事件并交给换绑用例。
 */
@Tag("dev")
class PersonRebindProcessListenerTest {

    private final PersonRebindUseCase useCase = mock(PersonRebindUseCase.class);
    private final PersonRebindProcessListener listener = new PersonRebindProcessListener(useCase);

    @Test
    void mapsWorkflowEventToUseCaseCommand() {
        ProcessEvent event = event("FINISH", Map.of("snapshotVersion", 3, "profileDecision", " approve "));

        listener.handle(event);

        var command = org.mockito.ArgumentCaptor.forClass(PersonRebindProcessCommand.class);
        verify(useCase).handleProcess(command.capture());
        assertThat(command.getValue().flowCode()).isEqualTo("person-profile");
        assertThat(command.getValue().status()).isEqualTo("FINISH");
        assertThat(command.getValue().decision()).isEqualTo("APPROVE");
        assertThat(command.getValue().snapshotVersion()).isEqualTo(3);
    }

    @Test
    void nullWorkflowEventIsIgnored() {
        listener.handle(null);

        verify(useCase, never()).handleProcess(any());
    }

    private ProcessEvent event(String status, Map<String, Object> params) {
        ProcessEvent event = new ProcessEvent();
        event.setFlowCode("person-profile");
        event.setBusinessId("9001");
        event.setStatus(status);
        event.setParams(params);
        return event;
    }
}
