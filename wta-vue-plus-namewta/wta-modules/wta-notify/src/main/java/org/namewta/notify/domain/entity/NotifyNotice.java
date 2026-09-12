package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/**
 * 通知中心公告草稿与发布状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_notice")
public class NotifyNotice extends BaseEntity {
    @TableId
    private Long noticeId;
    private String noticeTitle;
    private String noticeType;
    private String noticeContent;
    private String recipientType;
    private String recipientIdsJson;
    private String userTypeIdsJson;
    private String channelsJson;
    private String status;
    private String lifecycle;
    private java.time.LocalDateTime publishedAt;
    @com.baomidou.mybatisplus.annotation.TableField(updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private java.time.LocalDateTime retractedAt;
    private String remark;
}
