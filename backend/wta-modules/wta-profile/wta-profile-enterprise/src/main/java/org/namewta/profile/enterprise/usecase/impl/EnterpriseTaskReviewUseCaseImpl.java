package org.namewta.profile.enterprise.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTaskDecisionBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseReviewContextVo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseProfileAccessUrl;
import org.namewta.profile.enterprise.service.EnterpriseTaskReviewService;
import org.namewta.profile.enterprise.usecase.EnterpriseTaskReviewUseCase;
import org.springframework.stereotype.Service;

/** 为审核快照、材料和流程迁移提供统一事务边界。 */
@Service
@RequiredArgsConstructor
public class EnterpriseTaskReviewUseCaseImpl implements EnterpriseTaskReviewUseCase {
    private final EnterpriseTaskReviewService service;

    /** 读取任务提交快照与材料。 */
    @Override
    @DSTransactional
    public EnterpriseReviewContextVo review(long taskId) { return service.review(taskId); }

    /** 读取经任务授权的材料地址。 */
    @Override
    @DSTransactional
    public EnterpriseProfileAccessUrl material(long taskId, long materialRefId) {
        return service.material(taskId, materialRefId);
    }

    /** 退回原因和流程状态迁移必须一同回滚或提交。 */
    @Override
    @DSTransactional
    public void decide(long operatorId, long taskId, EnterpriseTaskDecisionBo command) {
        service.decide(operatorId, taskId, command);
    }
}
