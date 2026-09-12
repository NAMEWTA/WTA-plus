package org.namewta.notify.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知中心公告展示对象。
 */
@Data
public class NotifyNoticeVo {
    private Long noticeId;
    private String noticeTitle;
    private String noticeType;
    private String noticeContent;
    private String recipientType;
    private List<Long> recipientIds;
    private List<Long> userTypeIds;
    private List<String> channels;
    private String status;
    private String lifecycle;
    private LocalDateTime publishedAt;
    private LocalDateTime retractedAt;
    private String remark;
    private Long createBy;
    private LocalDateTime createTime;
}
