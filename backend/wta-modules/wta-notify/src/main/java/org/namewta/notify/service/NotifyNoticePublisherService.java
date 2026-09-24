package org.namewta.notify.service;

import lombok.RequiredArgsConstructor;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.notify.api.*;
import org.namewta.notify.domain.entity.NotifyNotice;
import org.namewta.notify.domain.entity.NotifyNoticeSnapshot;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.policy.NoticeAudiencePolicy;
import org.namewta.notify.support.NotifyNoticeVersionFence;
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
    private final NotifyNotificationDao notificationDao;

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
        snapshot.setPathSnapshot("/notify/inbox");
        snapshot.setPublishedAt(notice.getPublishedAt());
        snapshot.setCreateTime(notice.getPublishedAt());
        if (dao.insertSnapshot(snapshot) != 1) throw new IllegalStateException("公告发布快照写入冲突");
        Map<String, Object> params = new HashMap<>();
        params.put("title", Objects.toString(notice.getNoticeTitle(), ""));
        params.put("content", Objects.toString(notice.getNoticeContent(), ""));
        params.put("path", snapshot.getPathSnapshot());
        params.put("noticeType", notice.getNoticeType());
        params.put("channels", audience.channels());
        NotificationReceipt receipt = notificationService.submit(new NotificationCommand("notify", "notice-published", "NOTICE_PUBLISHED",
            String.valueOf(notice.getNoticeId()), recipientType, recipientIds.stream().map(String::valueOf).toList(), "notice-published", params,
            channels,
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0, null, null,
            NotifyNoticeVersionFence.idempotencyKey(notice.getNoticeId(), version),
            NotifyNoticeVersionFence.initial(notice.getNoticeId(), snapshot.getSnapshotId(), version), java.util.List.of()));
        long intentId = receiptId(receipt);
        NotifyIntent intent = notificationDao.lockNoticeIntent(
            NotifyNoticeVersionFence.idempotencyKey(notice.getNoticeId(), version));
        if (intent == null || !Objects.equals(intent.getIntentId(), intentId)) {
            throw new IllegalStateException("公告发布回执与意图不一致");
        }
        NotifyNoticeVersionFence.requirePublishedIdentity(intent, notice.getNoticeId(), snapshot.getSnapshotId(), version);
        if (channels.contains(NotificationChannel.IN_APP)) {
            Map<String, Object> persistedParams = JsonUtils.parseObject(intent.getTemplateParamsJson(), Map.class);
            if (persistedParams == null || !"/notify/inbox".equals(persistedParams.get("path"))) {
                throw new IllegalStateException("公告发布路径快照不一致");
            }
            String path = "/notify/inbox?messageId=" + intentId;
            Map<String, Object> deepParams = new HashMap<>(persistedParams);
            deepParams.put("path", path);
            if (notificationDao.updateNoticePath(intent, JsonUtils.toJsonString(deepParams), path) != 1
                || dao.updateSnapshotPath(snapshot, path) != 1) {
                throw new IllegalStateException("公告发布深链写入冲突");
            }
        }
    }

    private long receiptId(NotificationReceipt receipt) {
        String id = receipt == null ? null : receipt.notificationId();
        if (id == null || !id.matches("[1-9][0-9]*")) throw new IllegalStateException("公告发布回执编号无效");
        try { return Long.parseLong(id); }
        catch (NumberFormatException invalid) { throw new IllegalStateException("公告发布回执编号无效", invalid); }
    }

}
