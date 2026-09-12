package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

/** 企业管理端多结果查询使用的读模型集合。 */
public final class EnterpriseAdminRows {

    /** 禁止实例化读模型容器。 */
    private EnterpriseAdminRows() {
    }

    /** 企业档案管理列表行。 */
    @Data
    public static class ProfileRow {
        private Long profileId;
        private Long previousProfileId;
        private Long currentVersionId;
        private String enterpriseName;
        private String unifiedCreditCode;
        private String identityKey;
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
        private Long bindingUserId;
        private String bindingStatus;
        private Integer version;
        private Instant createTime;
    }

    /** 企业档案版本列表行。 */
    @Data
    public static class VersionRow {
        private Long versionId;
        private Long profileId;
        private Integer versionNo;
        private String sourceType;
        private Long sourceId;
        private String enterpriseName;
        private String unifiedCreditCode;
        private String identityKey;
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
        private Instant publishedTime;
    }

    /** 企业绑定列表行。 */
    @Data
    public static class BindingRow {
        private Long bindingId;
        private Long profileId;
        private Long userId;
        private String status;
        private Integer bindingVersion;
        private String sourceType;
        private Long sourceId;
        private Instant boundTime;
        private Instant unboundTime;
    }

    /** 企业来源列表行。 */
    @Data
    public static class SourceRow {
        private Long sourceId;
        private String sourceType;
        private Long operatorUserId;
        private String reason;
        private String fieldSnapshotJson;
        private Instant occurredTime;
    }

    /** 企业审计列表行。 */
    @Data
    public static class AuditRow {
        private Long auditId;
        private String operationType;
        private Long operatorUserId;
        private String capability;
        private String reason;
        private String beforeStatus;
        private String afterStatus;
        private String result;
        private String failureCategory;
        private Instant occurredTime;
    }

    /** 企业审核详情行。 */
    @Data
    public static class ReviewRow {
        private Long applicationId;
        private Long applicantUserId;
        private String status;
        private Integer submissionSeq;
        private Integer decisionVersion;
        private Integer version;
        private Long submissionId;
        private String fieldSnapshotJson;
        private Instant submittedTime;
    }
}
