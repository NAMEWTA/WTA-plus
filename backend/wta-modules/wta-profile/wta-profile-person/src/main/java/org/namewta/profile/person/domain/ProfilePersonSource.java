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

/** ProfilePersonSource 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_person_source")
public class ProfilePersonSource extends BaseEntity {

    @TableId("person_source_id")
    private Long personSourceId;
    private Long personProfileId;
    private String sourceType;
    private Long operatorUserId;
    private String operationReason;
    private String fullName;
    private String documentTypeCode;
    private String documentNumber;
    private String identityKey;
    private String gender;
    private LocalDate birthDate;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String fieldSnapshotJson;
    private LocalDateTime occurredTime;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
