package org.namewta.notify.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** 当前用户收件箱消息及其阅读状态。 */
@Data
public class NotifyInboxMessageVo {
    private Long messageId;
    private String category;
    private String noticeType;
    private List<String> channels;
    private String type;
    private String source;
    private String title;
    private String message;
    private String content;
    private String path;
    private LocalDateTime createTime;
    private LocalDateTime seenTime;
    private LocalDateTime readTime;
}
