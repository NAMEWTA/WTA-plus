package org.namewta.profile.enterprise.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 企业认证申请工作副本实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_enterprise_application")
public class ProfileEnterpriseApplication extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("enterprise_application_id")
    private Long enterpriseApplicationId;
    private Long applicantUserId;
    private Long targetProfileId;
    private String status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long openUserId;
    private String enterpriseName;
    private String unifiedCreditCode;
    private String identityKey;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String openIdentityKey;
    private String enterpriseType;
    private String legalRepresentativeName;
    private String legalDocumentTypeCode;
    private String legalDocumentNumber;
    private String handlerIsLegalRepresentative;
    private LocalDate establishedDate;
    private LocalDate businessTermFrom;
    private LocalDate businessTermUntil;
    private String registeredAddress;
    private String businessScope;
    private String contactName;
    private String contactPhone;
    private String email;
    private BigDecimal registeredCapital;
    private String industryCode;
    private String website;
    private String providerCode;
    private Integer submissionSeq;
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
