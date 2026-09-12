package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/**
 * 通知逻辑接收者快照。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_recipient")
public class NotifyRecipient extends BaseEntity {
    @TableId private Long recipientId;
    private Long intentId;
    private String recipientType;
    private String recipientKey;
    private Long userId;
    private String targetSnapshotJson;
    private String status;
    private String suppressReason;
}
