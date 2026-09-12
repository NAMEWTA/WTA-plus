package org.namewta.profile.enterprise.domain;

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
 * 企业认证不可变提交快照实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_enterprise_submission")
public class ProfileEnterpriseSubmission extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("enterprise_submission_id")
    private Long enterpriseSubmissionId;
    private Long enterpriseApplicationId;
    private Integer submissionSeq;
    private String enterpriseName;
    private String unifiedCreditCode;
    private String identityKey;
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
    private String fieldSnapshotJson;
    private LocalDateTime submittedTime;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
