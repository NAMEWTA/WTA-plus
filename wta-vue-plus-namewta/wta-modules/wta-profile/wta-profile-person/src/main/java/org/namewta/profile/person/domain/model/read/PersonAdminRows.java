package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/** 个人管理端多结果查询使用的读模型集合。 */
public final class PersonAdminRows {

    /** 禁止实例化读模型容器。 */
    private PersonAdminRows() {
    }

    /** 个人档案管理列表行。 */
    @Data
    public static class ProfileRow {
        private Long profileId;
        private Long previousProfileId;
        private Long currentVersionId;
        private String fullName;
        private String documentTypeCode;
        private String documentNumber;
        private String identityKey;
        private String gender;
        private LocalDate birthDate;
        private LocalDate validFrom;
        private LocalDate validUntil;
        private String status;
        private Long bindingUserId;
        private String bindingStatus;
        private Integer version;
        private Instant createTime;
    }

    /** 个人档案版本列表行。 */
    @Data
    public static class VersionRow {
        private Long versionId;
        private Long profileId;
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
        private Instant publishedTime;
    }

    /** 个人绑定列表行。 */
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

    /** 个人来源列表行。 */
    @Data
    public static class SourceRow {
        private Long sourceId;
        private String sourceType;
        private Long operatorUserId;
        private String reason;
        private String fieldSnapshotJson;
        private Instant occurredTime;
    }

    /** 个人审计列表行。 */
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

    /** 个人审核详情行。 */
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
