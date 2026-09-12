package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** ProfilePersonSubmission 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_person_submission")
public class ProfilePersonSubmission extends BaseEntity {

    @TableId("person_submission_id")
    private Long personSubmissionId;
    private Long personApplicationId;
    private Integer submissionSeq;
    private String fullName;
    private String documentTypeCode;
    private String documentNumber;
    private String identityKey;
    private String gender;
    private LocalDate birthDate;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String providerCode;
    private String rebindIntent;
    private Long targetProfileId;
    private Long expectedBindingId;
    private Integer expectedBindingVersion;
    private String fieldSnapshotJson;
    private LocalDateTime submittedTime;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
