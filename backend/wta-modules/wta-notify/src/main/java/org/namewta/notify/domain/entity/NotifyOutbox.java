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
    /** IN_APP 为已开始且持久预留的自动尝试预算；外部渠道为已提交结果次数。 */
    private Integer attemptCount;
    private LocalDateTime nextAttemptAt;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    /** 每次领取生成的 fencing token，防止过期 Worker 覆盖新 Worker 的状态。 */
    private String leaseToken;
    private Integer maxAttempts;
    /** IN_APP_ATTEMPT_RESERVED 仅是活租约的内部预留标记，新租约领取时清除。 */
    private String lastErrorCode;
    private String lastErrorMessage;
}
