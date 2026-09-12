package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.enums.PushSourceEnum;
import org.namewta.common.core.enums.PushTypeEnum;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.push.helper.PushHelper;
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.domain.entity.NotifyMessage;
import org.namewta.notify.domain.entity.NotifyMessageRecipient;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.system.api.domain.PushPayloadDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知中心站内信适配器。
 *
 * <p>先写入 Notify 收件箱，再发出实时提示；提示失败不回滚已持久化的通知事实。</p>
 */
@Service
@RequiredArgsConstructor
public class InAppNotificationService implements InAppNotificationPort {
    private final NotifyNotificationDao dao;

    /** 持久化站内信快照和收件关系。 */
    @Override
    public void persist(String notificationId, InAppSnapshot snapshot, List<Long> userIds) {
        Long messageId = Long.valueOf(notificationId);
        if (dao.message(messageId) == null) {
            NotifyMessage message = new NotifyMessage();
            message.setMessageId(messageId);
            message.setCategory(resolveCategory(snapshot.path()));
            message.setType(PushTypeEnum.MESSAGE.getType());
            message.setSource(PushSourceEnum.BACKEND.getSource());
            message.setNoticeType(snapshot.noticeType());
            message.setChannelsJson(org.namewta.common.json.utils.JsonUtils.toJsonString(snapshot.channels()));
            message.setTitle(snapshot.title());
            message.setMessage(snapshot.content());
            message.setContent(snapshot.content());
            message.setPath(snapshot.path());
            message.setSendUserIds(userIds == null ? "0" : StringUtils.joinComma(userIds));
            dao.insert(message);
        }
        if (userIds == null) return;
        for (Long userId : userIds) {
            boolean exists = dao.recipientExists(messageId, userId);
            if (exists) continue;
            NotifyMessageRecipient recipient = new NotifyMessageRecipient();
            recipient.setMessageRecipientId(IdGeneratorUtil.nextLongId());
            recipient.setMessageId(messageId);
            recipient.setUserId(userId);
            recipient.setCreateTime(LocalDateTime.now());
            dao.insert(recipient);
        }
    }

    /** 在收件箱事实落库后发送实时提示。 */
    @Override
    public void pushRealtime(String notificationId, InAppSnapshot snapshot, List<Long> userIds) {
        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", notificationId);
        data.put("title", snapshot.title());
        data.put("content", snapshot.content());
        data.put("path", snapshot.path());
        PushPayloadDTO payload = PushPayloadDTO.of(PushTypeEnum.MESSAGE, PushSourceEnum.BACKEND,
            snapshot.title() == null || snapshot.title().isBlank() ? "您有一条新通知" : snapshot.title(), data,
            snapshot.path());
        if (userIds == null || userIds.isEmpty()) PushHelper.publishAll(payload);
        else PushHelper.publishMessage(userIds, payload);
    }

    /** 记录用户已见或已读状态。 */
    @Override
    public void markEngagement(String notificationId, Long userId, boolean read) {
        NotifyMessageRecipient recipient = dao.messageRecipient(Long.valueOf(notificationId), userId);
        if (recipient == null) return;
        LocalDateTime now = LocalDateTime.now();
        if (recipient.getSeenTime() == null) recipient.setSeenTime(now);
        if (read && recipient.getReadTime() == null) recipient.setReadTime(now);
        dao.update(recipient);
    }

    private String resolveCategory(String path) {
        if (path != null && path.startsWith("/workflow")) return "workflow";
        if (path != null && path.startsWith("/notify/notice")) return "notice";
        return "system";
    }
}

