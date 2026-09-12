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
 * 企业档案不可变版本实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_enterprise_version")
public class ProfileEnterpriseVersion extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("enterprise_version_id")
    private Long enterpriseVersionId;
    private Long enterpriseProfileId;
    private Integer versionNo;
    private String sourceType;
    private Long sourceId;
    private String enterpriseName;
    private String unifiedCreditCode;
    private String enterpriseType;
    private String legalRepresentativeName;
    private String legalDocumentTypeCode;
    private String legalDocumentNumber;
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
    private String status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long currentProfileId;
    private LocalDateTime publishedTime;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
