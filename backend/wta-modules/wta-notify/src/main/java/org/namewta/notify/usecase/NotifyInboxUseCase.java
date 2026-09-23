package org.namewta.notify.usecase;

import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.vo.NotifyInboxMessageVo;
import org.namewta.notify.domain.vo.NotifyInboxPageVo;
import org.namewta.notify.service.runtime.NotifyInboxService;
import org.springframework.stereotype.Service;

/** 当前用户收件箱用例。 */
@Service
@RequiredArgsConstructor
public class NotifyInboxUseCase {
    private final NotifyInboxService inboxService;

    public NotifyInboxPageVo list(Long userId, Integer pageNum, Integer pageSize) {
        return inboxService.list(userId, pageNum, pageSize);
    }
    public NotifyInboxMessageVo detail(Long messageId, Long userId) { return inboxService.detail(userId, messageId); }
    public void seen(Long messageId, Long userId) { inboxService.mark(messageId, userId, false); }
    public void read(Long messageId, Long userId) { inboxService.mark(messageId, userId, true); }
    public void readAll(Long userId) { inboxService.markAll(userId); }
}
