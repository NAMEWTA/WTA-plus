package org.namewta.notify.service;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.system.api.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 公告生命周期不存在目标时的失败合同回归测试。 */
@Tag("dev")
class NotifyNoticeServiceTest {
    @Test
    void publishingUnknownNoticeFailsClearly() {
        NotifyPersistenceDao dao = mock(NotifyPersistenceDao.class);
        when(dao.findForUpdate(404L)).thenReturn(null);

        assertThrows(ServiceException.class,
            () -> new NotifyNoticeService(dao, mock(UserService.class)).publish(404L));
    }
}
