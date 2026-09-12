package org.namewta.notify.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.notify.domain.entity.NotifyNotice;
import org.namewta.notify.service.NotifyNoticePublisherService;
import org.namewta.notify.service.NotifyNoticeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 公告用例的发布幂等和事务边界回归测试。 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class NotifyNoticeUseCaseTest {
    @Mock
    private NotifyNoticeService noticeService;
    @Mock
    private NotifyNoticePublisherService publisher;

    @Test
    void alreadyPublishedNoticeSkipsPublisher() {
        when(noticeService.publish(12L)).thenReturn(null);
        new NotifyNoticeUseCase(noticeService, publisher).publish(12L);

        verify(noticeService).publish(12L);
        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void newlyPublishedNoticeIsSubmittedToPublisher() {
        NotifyNotice notice = new NotifyNotice();
        notice.setNoticeId(12L);
        when(noticeService.publish(12L)).thenReturn(notice);
        new NotifyNoticeUseCase(noticeService, publisher).publish(12L);

        verify(publisher).publish(notice);
    }

    @Test
    void saveRetractAndRemoveAreTransactionalBoundaries() throws Exception {
        for (String methodName : new String[]{"save", "retract", "remove"}) {
            Method method = java.util.Arrays.stream(NotifyNoticeUseCase.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
            assertTrue(method.isAnnotationPresent(DSTransactional.class), methodName + " must be transactional");
        }
    }
}
