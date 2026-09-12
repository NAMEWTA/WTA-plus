package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** ProfilePersonApplication 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_person_application")
public class ProfilePersonApplication extends BaseEntity {

    @TableId("person_application_id")
    private Long personApplicationId;
    private Long applicantUserId;
    private Long targetProfileId;
    private String status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long openUserId;
    private String fullName;
    private String documentTypeCode;
    private String documentNumber;
    private String identityKey;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String openIdentityKey;
    private String gender;
    private LocalDate birthDate;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String providerCode;
    private Integer submissionSeq;
    private String rebindIntent;
    private Long expectedBindingId;
    private Integer expectedBindingVersion;
    private Integer decisionVersion;
    private String decisionSource;
    private String decisionResult;
    private String decisionReason;
    private LocalDateTime submittedTime;
    private LocalDateTime finishedTime;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
