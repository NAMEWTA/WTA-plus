package org.namewta.notify.usecase;

import lombok.RequiredArgsConstructor;
import org.namewta.notify.port.InAppCommittedPushPort;
import org.namewta.notify.service.runtime.InAppNotificationService;
import org.springframework.stereotype.Service;

/** 提交后按持久化的本人关系发送一次在线刷新提示，不参与结果事务。 */
@Service
@RequiredArgsConstructor
public class InAppCommittedPushUseCase implements InAppCommittedPushPort {
    private final InAppNotificationService service;

    /**
     * 读取已提交快照并推送给指定收件人。
     * @param messageId 消息主键
     * @param userId 收件人主键
     */
    @Override
    public void push(Long messageId, Long userId) {
        service.pushCommitted(messageId, userId);
    }
}
