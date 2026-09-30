package org.namewta.profile.enterprise.dao;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.enterprise.mapper.EnterpriseTaskReviewMapper;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseTaskReviewRow;
import org.springframework.stereotype.Repository;

/** 封装审核快照的读取和待审申请锁。 */
@Repository
@RequiredArgsConstructor
public class EnterpriseTaskReviewDao {
    private final EnterpriseTaskReviewMapper mapper;

    /** 查询任务绑定的提交快照。 */
    public EnterpriseTaskReviewRow selectReview(long applicationId, long submissionId) {
        return mapper.selectReview(applicationId, submissionId);
    }

    /** 在审核事务中锁定最新待审提交。 */
    public EnterpriseTaskReviewRow lockReview(long applicationId, long submissionId) {
        return mapper.lockReview(applicationId, submissionId);
    }

    /** 记录待审快照的退回原因。 */
    public int recordReturnReason(long applicationId, int snapshotVersion, String reason, long operatorId) {
        return mapper.recordReturnReason(applicationId, snapshotVersion, reason, operatorId);
    }
}
