package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;
import java.time.LocalDateTime;

/**
 * 用户与渠道构成的通知投递。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_delivery")
public class NotifyDelivery extends BaseEntity {
    @TableId private Long deliveryId;
    private Long intentId;
    private Long recipientId;
    private Long userId;
    private String channel;
    private String targetValue;
    private String status;
    private Integer attemptCount;
    private String providerKey;
    private String providerMessageId;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime acceptedAt;
    /** 原生短信状态下次核对时间，仅调度查询，不授权再次发送。 */
    private LocalDateTime receiptQueryAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private Integer version;
}
