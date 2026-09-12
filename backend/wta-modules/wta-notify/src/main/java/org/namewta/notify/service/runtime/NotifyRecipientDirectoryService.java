package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.vo.NotifyRecipientUserVo;
import org.namewta.notify.port.NotifyRecipientDirectoryPort;
import org.namewta.system.api.UserService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/** 用户目录服务，负责把 system 用户合同转换为通知领域视图。 */
@Service
@RequiredArgsConstructor
public class NotifyRecipientDirectoryService implements NotifyRecipientDirectoryPort {
    private final UserService userService;

    @Override
    public List<NotifyRecipientUserVo> search(String keyword, int limit) {
        return userService.searchActiveUsers(keyword, limit).stream().map(NotifyRecipientUserVo::from).toList();
    }

    @Override
    public List<NotifyRecipientUserVo> byIds(Collection<Long> userIds) {
        return userService.selectNotificationUsers(userIds).stream().map(NotifyRecipientUserVo::from).toList();
    }
}
