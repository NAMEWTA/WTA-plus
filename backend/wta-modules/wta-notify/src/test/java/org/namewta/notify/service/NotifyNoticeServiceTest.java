package org.namewta.notify.service;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyNotice;
import org.namewta.system.api.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 公告生命周期不存在目标时的失败合同回归测试。 */
@Tag("dev")
class NotifyNoticeServiceTest {
    @Test
    void publishingUnknownNoticeFailsClearly() {
        NotifyPersistenceDao dao = mock(NotifyPersistenceDao.class);
        when(dao.findForUpdate(404L)).thenReturn(null);

        assertThrows(ServiceException.class,
            () -> new NotifyNoticeService(dao, mock(UserService.class), mock(NotifyNotificationDao.class)).publish(404L));
    }

    @Test
    void zeroRowLifecycleWriteCannotBeTreatedAsPublished() {
        NotifyPersistenceDao dao = mock(NotifyPersistenceDao.class);
        NotifyNotice draft = new NotifyNotice();
        draft.setNoticeId(405L);
        draft.setLifecycle("DRAFT");
        when(dao.findForUpdate(405L)).thenReturn(draft);
        NotifyNoticeService service = new NotifyNoticeService(dao, mock(UserService.class),
            mock(NotifyNotificationDao.class));

        assertThrows(IllegalStateException.class, () -> service.publish(405L));
        verify(dao).update(draft);
        verify(dao, never()).insertSnapshot(org.mockito.ArgumentMatchers.any());
    }
}
