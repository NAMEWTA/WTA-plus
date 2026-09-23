package org.namewta.notify.service;

import lombok.RequiredArgsConstructor;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.notify.api.*;
import org.namewta.notify.domain.entity.NotifyNotice;
import org.namewta.notify.domain.entity.NotifyNoticeSnapshot;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.notify.domain.policy.NoticeAudiencePolicy;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.api.UserService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 发布时冻结目录解析结果，提交唯一版本的内容与渠道投递快照。 */
@Service
@RequiredArgsConstructor
public class NotifyNoticePublisherService {
    private final NotificationApplicationService notificationService;
    private final NotifyPersistenceDao dao;
    private final UserService userService;

    public void publish(NotifyNotice notice) {
        var audience = NoticeAudiencePolicy.normalize(notice.getRecipientType(),
            JsonUtils.parseArray(notice.getRecipientIdsJson(), Long.class),
            JsonUtils.parseArray(notice.getUserTypeIdsJson(), Long.class),
            notice.getChannelsJson() == null ? null : JsonUtils.parseArray(notice.getChannelsJson(), String.class));
        List<Long> recipientIds = switch (audience.recipientType()) {
            case "ALL" -> List.of();
            case "USER" -> userService.selectNotificationUsers(audience.recipientIds()).stream().map(user -> user.getUserId()).distinct().toList();
            case "USER_TYPE" -> userService.selectUsersByUserTypeIds(audience.userTypeIds()).stream().map(user -> user.getUserId()).distinct().toList();
            default -> throw new ServiceException("发送对象类型不合法");
        };
        String recipientType = "ALL".equals(audience.recipientType()) ? "ALL" : "USER";
        if ("USER".equals(recipientType) && recipientIds.isEmpty()) throw new ServiceException("所选范围没有可接收通知的正常用户");
        List<NotificationChannel> channels = audience.channels().stream().map(NotificationChannel::valueOf).toList();
        NotifyNoticeSnapshot current = dao.latestSnapshot(notice.getNoticeId());
        int version = current == null || current.getSnapshotVersion() == null ? 1 : current.getSnapshotVersion() + 1;
        NotifyNoticeSnapshot snapshot = new NotifyNoticeSnapshot();
        snapshot.setSnapshotId(IdGeneratorUtil.nextLongId());
        snapshot.setNoticeId(notice.getNoticeId());
        snapshot.setSnapshotVersion(version);
        snapshot.setTitleSnapshot(notice.getNoticeTitle());
        snapshot.setContentSnapshot(notice.getNoticeContent());
        snapshot.setNoticeType(notice.getNoticeType());
        snapshot.setPathSnapshot("/notify/notice?noticeId=" + notice.getNoticeId());
        snapshot.setPublishedAt(notice.getPublishedAt());
        snapshot.setCreateTime(notice.getPublishedAt());
        dao.insertSnapshot(snapshot);
        Map<String, Object> params = new HashMap<>();
        params.put("title", Objects.toString(notice.getNoticeTitle(), ""));
        params.put("content", Objects.toString(notice.getNoticeContent(), ""));
        params.put("path", snapshot.getPathSnapshot());
        params.put("noticeType", notice.getNoticeType());
        params.put("channels", audience.channels());
        notificationService.submit(new NotificationCommand("notify", "notice-published", "NOTICE_PUBLISHED",
            String.valueOf(notice.getNoticeId()), recipientType, recipientIds.stream().map(String::valueOf).toList(), "notice-published", params,
            channels,
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0, null, null,
            "notice-published:" + notice.getNoticeId() + ":" + version, Map.of("audit", "NOTICE_SNAPSHOT")));
    }

}
