package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;
import java.time.LocalDateTime;

/**
 * 通知意图事实。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_intent")
public class NotifyIntent extends BaseEntity {
    @TableId private Long intentId;
    private String appId;
    private String sceneCode;
    private String bizType;
    private String bizId;
    private String templateCode;
    private String templateParamsJson;
    private String strategy;
    private String mode;
    private Integer priority;
    private LocalDateTime scheduledAt;
    private LocalDateTime expiresAt;
    private String idempotencyKey;
    private String status;
    private String titleSnapshot;
    private String contentSnapshot;
    private String pathSnapshot;
    private String metadataJson;
    /** 非空附件的原提交者；不能由 HTTP 请求指定。 */
    private Long attachmentActorUserId;
    private Long attachmentActorClientPk;
    private Integer version;
}
