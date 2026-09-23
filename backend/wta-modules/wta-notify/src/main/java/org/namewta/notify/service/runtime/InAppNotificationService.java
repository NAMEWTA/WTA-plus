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
import org.namewta.notify.port.InAppDeliveryCommittedEvent;
import org.namewta.system.api.domain.PushPayloadDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通知中心站内信适配器。
 *
 * <p>先写入 Notify 收件箱，再发出实时提示；提示失败不回滚已持久化的通知事实。</p>
 */
@Service
@RequiredArgsConstructor
public class InAppNotificationService implements InAppNotificationPort {
    private final NotifyNotificationDao dao;
    private final ApplicationEventPublisher events;

    /** 调用方须在结果事务中持久化；只为本次新增的关系登记提交后提示事件。 */
    @Override
    public void persist(String notificationId, InAppSnapshot snapshot, List<Long> userIds) {
        Long messageId = Long.valueOf(notificationId);
        NotifyMessage existing = dao.message(messageId);
        if (existing == null) {
            NotifyMessage message = new NotifyMessage();
            message.setMessageId(messageId);
            message.setCategory(resolveCategory(snapshot.path()));
            message.setType(PushTypeEnum.MESSAGE.getType());
            message.setSource(PushSourceEnum.BACKEND.getSource());
            message.setNoticeType(snapshot.noticeType());
            message.setChannelsJson(org.namewta.common.json.utils.JsonUtils.toJsonString(snapshot.channels()));
            message.setTitle(snapshot.title());
            message.setMessage(summary(snapshot.content()));
            message.setContent(snapshot.content());
            message.setPath(snapshot.path());
            message.setSendUserIds(userIds == null ? "0" : StringUtils.joinComma(userIds));
            if (dao.insert(message) != 1) throw new IllegalStateException("站内消息写入未影响一行");
        } else if (!Objects.equals(existing.getTitle(), snapshot.title())
            || !Objects.equals(existing.getContent(), snapshot.content())
            || !Objects.equals(existing.getPath(), snapshot.path())
            || !Objects.equals(existing.getNoticeType(), snapshot.noticeType())
            || !Objects.equals(org.namewta.common.json.utils.JsonUtils.parseArray(existing.getChannelsJson(), String.class),
                snapshot.channels())) {
            throw new IllegalStateException("同一站内消息主键的内容快照不一致");
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
            if (dao.insert(recipient) != 1) throw new IllegalStateException("站内收件关系写入未影响一行");
            events.publishEvent(new InAppDeliveryCommittedEvent(messageId, userId));
        }
    }

    /**
     * 只在提交后的事件入口调用，从已提交的本人关系和消息重新取得提示内容。
     * @param messageId 已提交消息主键
     * @param userId 本人收件关系主键
     */
    public void pushCommitted(Long messageId, Long userId) {
        if (dao.messageRecipient(messageId, userId) == null) return;
        NotifyMessage message = dao.message(messageId);
        if (message == null) return;
        List<String> channels = org.namewta.common.json.utils.JsonUtils.parseArray(message.getChannelsJson(), String.class);
        if (channels == null || channels.isEmpty()) channels = List.of("IN_APP");
        pushRealtime(String.valueOf(messageId), new InAppSnapshot(message.getTitle(), message.getContent(),
            message.getPath(), message.getNoticeType(), channels), List.of(userId));
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
        if (path != null && path.startsWith("/notify/inbox?messageId=")) return "notice";
        return "system";
    }

    /** 摘要列是 varchar(1000)，按 Unicode 码点截取，完整正文仍保存在 content。 */
    private String summary(String content) {
        if (content == null) return null;
        int codePoints = content.codePointCount(0, content.length());
        return codePoints <= 1000 ? content : content.substring(0, content.offsetByCodePoints(0, 1000));
    }
}
