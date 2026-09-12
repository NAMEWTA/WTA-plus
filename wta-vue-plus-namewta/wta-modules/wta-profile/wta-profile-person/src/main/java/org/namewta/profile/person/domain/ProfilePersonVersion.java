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

/** ProfilePersonVersion 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_person_version")
public class ProfilePersonVersion extends BaseEntity {

    @TableId("person_version_id")
    private Long personVersionId;
    private Long personProfileId;
    private Integer versionNo;
    private String sourceType;
    private Long sourceId;
    private String fullName;
    private String documentTypeCode;
    private String documentNumber;
    private String identityKey;
    private String gender;
    private LocalDate birthDate;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long currentProfileId;
    private LocalDateTime publishedTime;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
