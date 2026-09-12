package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/** ProfileOperationAudit 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_operation_audit")
public class ProfileOperationAudit extends BaseEntity {

    @TableId("operation_audit_id")
    private Long operationAuditId;
    private String profileType;
    private Long profileId;
    private Long applicationId;
    private Long bindingId;
    private String operationType;
    private Long operatorUserId;
    private String capability;
    private String reason;
    private String beforeStatus;
    private String afterStatus;
    private String result;
    private String failureCategory;
    private LocalDateTime occurredTime;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
