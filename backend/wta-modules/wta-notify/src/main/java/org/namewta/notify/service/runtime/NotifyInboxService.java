package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.domain.entity.NotifyMessageRecipient;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.vo.NotifyInboxMessageVo;
import org.namewta.notify.domain.vo.NotifyInboxPageVo;
import org.namewta.notify.domain.model.read.NotifyInboxRow;
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

    /** 本人收件箱分页；总数和未读数独立覆盖所有有效消息关系。 */
    public NotifyInboxPageVo list(Long userId, Integer requestedPageNum, Integer requestedPageSize) {
        int pageNum = requestedPageNum == null ? 1 : requestedPageNum;
        int pageSize = requestedPageSize == null ? 20 : requestedPageSize;
        if (userId == null || pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new ServiceException("收件箱分页参数无效");
        }
        long total = dao.inboxTotal(userId);
        long unreadTotal = dao.inboxUnreadTotal(userId);
        var rows = dao.inboxRows(userId, pageNum, pageSize, total).stream().map(this::toVo).toList();
        return new NotifyInboxPageVo(rows, total, unreadTotal);
    }

    /** 详情必须在同一次联表查询中证明当前用户拥有收件关系。 */
    public NotifyInboxMessageVo detail(Long userId, Long messageId) {
        if (userId == null || messageId == null || messageId < 1) throw new ServiceException("消息不存在");
        NotifyInboxRow row = dao.inboxDetail(userId, messageId);
        if (row == null) throw new ServiceException("消息不存在");
        return toVo(row);
    }

    private NotifyInboxMessageVo toVo(NotifyInboxRow row) {
        NotifyInboxMessageVo vo = new NotifyInboxMessageVo();
        vo.setMessageId(row.getMessageId());
        vo.setCategory(row.getCategory());
        vo.setNoticeType(row.getNoticeType());
        vo.setChannels(row.getChannelsJson() == null ? List.of("IN_APP")
            : JsonUtils.parseArray(row.getChannelsJson(), String.class));
        vo.setType(row.getType());
        vo.setSource(row.getSource());
        vo.setTitle(row.getTitle());
        vo.setMessage(row.getMessage());
        vo.setContent(row.getContent());
        vo.setPath(row.getPath());
        vo.setCreateTime(row.getCreateTime());
        vo.setSeenTime(row.getSeenTime());
        vo.setReadTime(row.getReadTime());
        return vo;
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
