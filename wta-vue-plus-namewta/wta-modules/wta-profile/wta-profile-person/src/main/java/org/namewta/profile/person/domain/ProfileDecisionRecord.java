package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/** ProfileDecisionRecord 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_decision_record")
public class ProfileDecisionRecord extends BaseEntity {

    @TableId("decision_record_id")
    private Long decisionRecordId;
    private String profileType;
    private Long applicationId;
    private Long submissionId;
    private Integer decisionVersion;
    private String decisionSource;
    private String decisionResult;
    private String decisionStatus;
    private String workflowEventId;
    private Long operatorUserId;
    private String reason;
    private LocalDateTime occurredTime;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
