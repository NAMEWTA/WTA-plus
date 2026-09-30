package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;
import java.time.Instant;

/** 任务所关联不可变提交快照及申请状态。 */
@Data
public class EnterpriseTaskReviewRow {
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
