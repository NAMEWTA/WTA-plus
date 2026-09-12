package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/**
 * 通知中心站内消息内容快照。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_message")
public class NotifyMessage extends BaseEntity {
    @TableId private Long messageId;
    private String category;
    private String noticeType;
    private String channelsJson;
    private String type;
    private String source;
    private String title;
    private String message;
    private String content;
    private String dataJson;
    private String path;
    private String sendUserIds;
}
