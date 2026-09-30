package org.namewta.profile.person.dao;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.person.mapper.PersonTaskReviewMapper;
import org.namewta.profile.person.domain.model.read.PersonTaskReviewRow;
import org.springframework.stereotype.Repository;

/** 封装审核快照的读取和待审申请锁。 */
@Repository
@RequiredArgsConstructor
public class PersonTaskReviewDao {
    private final PersonTaskReviewMapper mapper;

    /** 查询任务绑定的提交快照。 */
    public PersonTaskReviewRow selectReview(long applicationId, long submissionId) {
        return mapper.selectReview(applicationId, submissionId);
    }

    /** 在审核事务中锁定最新待审提交。 */
    public PersonTaskReviewRow lockReview(long applicationId, long submissionId) {
        return mapper.lockReview(applicationId, submissionId);
    }

    /** 记录待审快照的退回原因。 */
    public int recordReturnReason(long applicationId, int snapshotVersion, String reason, long operatorId) {
        return mapper.recordReturnReason(applicationId, snapshotVersion, reason, operatorId);
    }
}
