package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/**
 * 一次实际 Provider 调用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_attempt")
public class NotifyAttempt extends BaseEntity {
    @TableId private Long attemptId;
    private Long intentId;
    private Long deliveryId;
    private Integer attemptNo;
    private String providerKey;
    private String status;
    private String providerMessageId;
    private String errorCategory;
    private String errorCode;
    private String errorMessage;
    private Long costTime;
}
