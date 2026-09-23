package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    /** 仅新建且确定未进外部 Provider 的任务持有；一旦写回结果不可恢复此标记。 */
    public static final String DEADLINE_UNSENT_READY = "DEADLINE_UNSENT_READY";
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
    /** IN_APP_ATTEMPT_RESERVED 是预算预留；DEADLINE_UNSENT_READY 是新外部任务的未外呼证据。 */
    private String lastErrorCode;
    private String lastErrorMessage;
    /** 领取前状态仅在本次内存对象携带，绝不持久化或公开序列化。 */
    @JsonIgnore
    @TableField(exist = false)
    private Boolean claimedFromReady;
}
