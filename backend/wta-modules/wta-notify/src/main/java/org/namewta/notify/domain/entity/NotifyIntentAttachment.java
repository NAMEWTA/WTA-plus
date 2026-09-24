package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/** 通知持有的源引用与私有快照预约事实，状态不随单条投递删除。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_intent_attachment")
public class NotifyIntentAttachment extends BaseEntity {
    @TableId private Long intentAttachmentId;
    private Long intentId;
    private Integer position;
    private Long sourceOssId;
    private Long snapshotOssId;
    private String sourceService;
    private String sourceKey;
    private String targetService;
    private String targetKey;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    /** QUEUED、COPYING、READY、COPY_UNKNOWN、RELEASED。 */
    private String status;
    private String copyToken;
    private Integer version;
    private String delFlag;
}
