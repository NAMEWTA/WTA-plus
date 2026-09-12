package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/**
 * 通知中心站内消息收件人与互动状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_message_recipient")
public class NotifyMessageRecipient extends BaseEntity {
    @TableId private Long messageRecipientId;
    private Long messageId;
    private Long userId;
    private LocalDateTime seenTime;
    private LocalDateTime readTime;
}
