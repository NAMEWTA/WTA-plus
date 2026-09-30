package org.namewta.profile.person.domain.model.read;

import lombok.Data;
import java.time.Instant;

/** 任务所关联不可变提交快照及申请状态。 */
@Data
public class PersonTaskReviewRow {
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
