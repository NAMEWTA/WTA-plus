package org.namewta.profile.person.mapper;

import org.apache.ibatis.annotations.Param;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.person.domain.ProfilePersonApplication;
import org.namewta.profile.person.domain.model.read.PersonTaskReviewRow;

/** 按任务已经确认的业务主键和提交主键查询审核快照。 */
public interface PersonTaskReviewMapper extends BaseMapperPlus<ProfilePersonApplication, ProfilePersonApplication> {
    /** 查询对应提交版本，支持已办历史而不读取最新提交代替。 */
    PersonTaskReviewRow selectReview(@Param("applicationId") long applicationId,
        @Param("submissionId") long submissionId);

    /** 锁定仍待审且为最新提交版本的申请。 */
    PersonTaskReviewRow lockReview(@Param("applicationId") long applicationId,
        @Param("submissionId") long submissionId);

    /** 保存本次退回原因，随后由同一事务内的流程事件更新申请状态与版本。 */
    int recordReturnReason(@Param("applicationId") long applicationId,
        @Param("snapshotVersion") int snapshotVersion, @Param("reason") String reason,
        @Param("operatorId") long operatorId);
}
