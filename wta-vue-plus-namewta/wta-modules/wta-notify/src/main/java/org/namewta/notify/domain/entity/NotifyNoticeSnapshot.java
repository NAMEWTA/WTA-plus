package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已发布公告不可变内容快照。
 */
@Data
@TableName("notify_notice_snapshot")
public class NotifyNoticeSnapshot {
    @TableId private Long snapshotId;
    private Long noticeId;
    private Integer snapshotVersion;
    private String titleSnapshot;
    private String contentSnapshot;
    private String noticeType;
    private String pathSnapshot;
    private LocalDateTime publishedAt;
    private Long createBy;
    private LocalDateTime createTime;
}
