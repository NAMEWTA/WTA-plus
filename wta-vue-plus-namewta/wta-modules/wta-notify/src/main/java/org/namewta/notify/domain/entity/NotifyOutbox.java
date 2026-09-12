package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;
import java.time.LocalDateTime;

/**
 * 可靠异步通知任务。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_outbox")
public class NotifyOutbox extends BaseEntity {
    @TableId private Long outboxId;
    private Long intentId;
    private Long deliveryId;
    private String status;
    private LocalDateTime availableAt;
    private Integer attemptCount;
    private LocalDateTime nextAttemptAt;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    /** 每次领取生成的 fencing token，防止过期 Worker 覆盖新 Worker 的状态。 */
    private String leaseToken;
    private Integer maxAttempts;
    private String lastErrorCode;
    private String lastErrorMessage;
}
