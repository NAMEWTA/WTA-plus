package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/** ProfilePersonBindingEvent 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_person_binding_event")
public class ProfilePersonBindingEvent extends BaseEntity {

    @TableId("person_binding_event_id")
    private Long personBindingEventId;
    private Long personBindingId;
    private Long personProfileId;
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
