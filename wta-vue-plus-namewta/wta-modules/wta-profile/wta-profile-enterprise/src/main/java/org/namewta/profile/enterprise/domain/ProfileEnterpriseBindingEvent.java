package org.namewta.profile.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 企业负责人不可变绑定事件实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_enterprise_binding_event")
public class ProfileEnterpriseBindingEvent extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("enterprise_binding_event_id")
    private Long enterpriseBindingEventId;
    private Long enterpriseBindingId;
    private Long enterpriseProfileId;
    private Long userId;
    private String eventType;
    private Integer bindingVersion;
    private String sourceType;
    private Long sourceId;
    private String reason;
    private LocalDateTime occurredTime;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
