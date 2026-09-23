package org.namewta.notify.domain.model.read;

import lombok.Data;

import java.time.LocalDateTime;

/** 本人收件关系与消息快照的联表读投影，不作为 HTTP 响应。 */
@Data
public class NotifyInboxRow {
    private Long messageId;
    private String category;
    private String noticeType;
    private String channelsJson;
    private String type;
    private String source;
    private String title;
    private String message;
    private String content;
    private String path;
    /** 收件关系创建时间决定排序和公开显示时间。 */
    private LocalDateTime createTime;
    private LocalDateTime seenTime;
    private LocalDateTime readTime;
}
