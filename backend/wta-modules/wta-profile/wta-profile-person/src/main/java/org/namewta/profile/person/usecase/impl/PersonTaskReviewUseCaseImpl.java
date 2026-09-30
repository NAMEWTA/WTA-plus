package org.namewta.profile.person.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.profile.person.domain.bo.PersonTaskDecisionBo;
import org.namewta.profile.person.domain.vo.PersonReviewContextVo;
import org.namewta.profile.person.domain.vo.PersonProfileAccessUrl;
import org.namewta.profile.person.service.PersonTaskReviewService;
import org.namewta.profile.person.usecase.PersonTaskReviewUseCase;
import org.springframework.stereotype.Service;

/** 为审核快照、材料和流程迁移提供统一事务边界。 */
@Service
@RequiredArgsConstructor
public class PersonTaskReviewUseCaseImpl implements PersonTaskReviewUseCase {
    private final PersonTaskReviewService service;

    /** 读取任务提交快照与材料。 */
    @Override
    @DSTransactional
    public PersonReviewContextVo review(long taskId) { return service.review(taskId); }

    /** 读取经任务授权的材料地址。 */
    @Override
    @DSTransactional
    public PersonProfileAccessUrl material(long taskId, long materialRefId) {
        return service.material(taskId, materialRefId);
    }

    /** 退回原因和流程状态迁移必须一同回滚或提交。 */
    @Override
    @DSTransactional
    public void decide(long operatorId, long taskId, PersonTaskDecisionBo command) {
        service.decide(operatorId, taskId, command);
    }
}
