package org.namewta.test.notify.caller;

import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.system.api.domain.UserDTO;
import org.namewta.workflow.service.impl.FlwCommonServiceImpl;
import org.namewta.workflow.service.impl.WorkflowTaskRecipientResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class WorkflowNotifyCallerUnitTest {
    @Test
    void resolvesUserEmailAndPhoneTargetsWithoutClientScope() {
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        when(notifications.submit(any())).thenReturn(accepted());
        FlwCommonServiceImpl service = new FlwCommonServiceImpl(notifications,
            mock(WorkflowTaskRecipientResolver.class));
        service.sendMessage(List.of("1", "2", "3"), "流程内容", "流程主题",
            List.of(user(7L, "user@example.com", "13812345678")));

        ArgumentCaptor<NotificationCommand> requests = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notifications, times(3)).submit(requests.capture());
        assertEquals(List.of(0, 0, 0), requests.getAllValues().stream()
            .map(NotificationCommand::priority).toList());
        assertEquals(List.of("7"), requests.getAllValues().stream()
            .filter(request -> "USER".equals(request.recipientType())).findFirst().orElseThrow().recipientIds());
        assertEquals(List.of("user@example.com"), requests.getAllValues().stream()
            .filter(request -> "EMAIL".equals(request.recipientType())).findFirst().orElseThrow().recipientIds());
        assertEquals(List.of("13812345678"), requests.getAllValues().stream()
            .filter(request -> "PHONE".equals(request.recipientType())).findFirst().orElseThrow().recipientIds());
    }

    @Test
    void providerFailureDoesNotBreakWorkflowSideEffect() {
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        when(notifications.submit(any())).thenThrow(new IllegalStateException("rejected"));
        FlwCommonServiceImpl service = new FlwCommonServiceImpl(notifications,
            mock(WorkflowTaskRecipientResolver.class));
        assertDoesNotThrow(() -> service.sendMessage(List.of("2"), "内容", "主题",
            List.of(user(7L, "user@example.com", null))));
    }

    private UserDTO user(Long userId, String email, String phone) {
        UserDTO user = new UserDTO();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        return user;
    }

    private NotificationReceipt accepted() {
        return new NotificationReceipt("notification-1", NotificationStatus.ACCEPTED, false, false, List.of());
    }
}
