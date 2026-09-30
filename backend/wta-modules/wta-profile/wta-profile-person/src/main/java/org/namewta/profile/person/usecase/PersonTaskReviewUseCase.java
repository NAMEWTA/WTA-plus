package org.namewta.profile.person.usecase;

import org.namewta.profile.person.domain.bo.PersonTaskDecisionBo;
import org.namewta.profile.person.domain.vo.PersonReviewContextVo;
import org.namewta.profile.person.domain.vo.PersonProfileAccessUrl;

/** 任务审核 HTTP 入口可调用的用例。 */
public interface PersonTaskReviewUseCase {
    /** 读取当前或本人已办任务的提交详情。 */
    PersonReviewContextVo review(long taskId);
    /** 读取任务提交材料。 */
    PersonProfileAccessUrl material(long taskId, long materialRefId);
    /** 在事务内办理指定提交版本。 */
    void decide(long operatorId, long taskId, PersonTaskDecisionBo command);
}
