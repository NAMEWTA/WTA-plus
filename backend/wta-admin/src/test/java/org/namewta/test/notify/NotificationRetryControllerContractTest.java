package org.namewta.test.notify;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.controller.admin.NotificationController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** HTTP 主键必须来自路径；冲突请求不能抵达业务服务。 */
@Tag("dev")
class NotificationRetryControllerContractTest {

    @Test
    void retryRejectsConflictingBodyIntentBeforeCallingService() {
        NotificationApplicationService service = mock(NotificationApplicationService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service)).build();

        Throwable failure = catchThrowable(() -> mvc.perform(post("/notify/notification/101/retry")
            .contentType("application/json")
            .content("{\"notificationId\":\"202\",\"deliveryId\":\"303\",\"reason\":\"manual\"}")));

        verifyNoInteractions(service);
        assertThat(failure).isNotNull().hasRootCauseInstanceOf(ServiceException.class);
    }
}
