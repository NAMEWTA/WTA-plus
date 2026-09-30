package org.namewta.profile.enterprise.usecase;

import org.namewta.profile.enterprise.domain.bo.EnterpriseTaskDecisionBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseReviewContextVo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseProfileAccessUrl;

/** 任务审核 HTTP 入口可调用的用例。 */
public interface EnterpriseTaskReviewUseCase {
    /** 读取当前或本人已办任务的提交详情。 */
    EnterpriseReviewContextVo review(long taskId);
    /** 读取任务提交材料。 */
    EnterpriseProfileAccessUrl material(long taskId, long materialRefId);
    /** 在事务内办理指定提交版本。 */
    void decide(long operatorId, long taskId, EnterpriseTaskDecisionBo command);
}
