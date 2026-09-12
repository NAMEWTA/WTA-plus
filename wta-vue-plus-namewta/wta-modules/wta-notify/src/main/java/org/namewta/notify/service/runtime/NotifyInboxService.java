package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.NotifyMessage;
import org.namewta.notify.domain.entity.NotifyMessageRecipient;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.vo.NotifyInboxMessageVo;
import org.namewta.common.json.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知中心收件箱服务，先读取持久化消息，再执行已见已读状态变更。
 */
@Service
@RequiredArgsConstructor
public class NotifyInboxService {
    private final NotifyNotificationDao dao;

    /** 查询用户收件箱。 */
    public List<NotifyInboxMessageVo> list(Long userId) {
        List<NotifyMessageRecipient> recipients = dao.messageRecipients(userId, 500);
        if (recipients.isEmpty()) return List.of();
        var messages = dao.messages(recipients.stream().map(NotifyMessageRecipient::getMessageId).toList()).stream()
            .collect(java.util.stream.Collectors.toMap(NotifyMessage::getMessageId, java.util.function.Function.identity()));
        return recipients.stream().filter(recipient -> messages.containsKey(recipient.getMessageId()))
            .map(recipient -> {
                NotifyMessage message = messages.get(recipient.getMessageId());
                NotifyInboxMessageVo vo = new NotifyInboxMessageVo();
                vo.setMessageId(message.getMessageId());
                vo.setCategory(message.getCategory());
                vo.setNoticeType(message.getNoticeType());
                vo.setChannels(message.getChannelsJson() == null ? List.of("IN_APP") : JsonUtils.parseArray(message.getChannelsJson(), String.class));
                vo.setType(message.getType());
                vo.setSource(message.getSource());
                vo.setTitle(message.getTitle());
                vo.setMessage(message.getMessage());
                vo.setContent(message.getContent());
                vo.setPath(message.getPath());
                vo.setCreateTime(message.getCreateTime());
                if (recipient != null) {
                    vo.setSeenTime(recipient.getSeenTime());
                    vo.setReadTime(recipient.getReadTime());
                }
                return vo;
            }).toList();
    }

    /** 标记消息已见或已读，重复调用保持幂等。 */
    public boolean mark(Long messageId, Long userId, boolean read) {
        NotifyMessageRecipient item = dao.messageRecipient(messageId, userId);
        if (item == null) return false;
        if (item.getSeenTime() == null) item.setSeenTime(LocalDateTime.now());
        if (read && item.getReadTime() == null) item.setReadTime(LocalDateTime.now());
        return dao.update(item) > 0;
    }

    /** 将当前用户全部收件消息标记为已见已读。 */
    public int markAll(Long userId) {
        return dao.markAllMessages(userId, LocalDateTime.now());
    }
}

