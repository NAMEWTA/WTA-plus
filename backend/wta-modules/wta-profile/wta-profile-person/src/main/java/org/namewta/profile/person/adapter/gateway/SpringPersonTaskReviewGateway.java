package org.namewta.profile.person.adapter.gateway;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.profile.person.port.gateway.PersonTaskReviewGateway;
import org.namewta.system.api.ConfigService;
import org.namewta.workflow.api.WorkflowTaskReviewService;
import org.namewta.workflow.api.domain.WorkflowTaskReviewContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** 将档案审核委托给可选工作流模块，禁止业务模块访问引擎持久化实现。 */
@Component
@RequiredArgsConstructor
public class SpringPersonTaskReviewGateway implements PersonTaskReviewGateway {
    private final ObjectProvider<WorkflowTaskReviewService> reviews;
    private final ConfigService configService;

    /** 校验任务属于本类认证流程。 */
    @Override
    public WorkflowTaskReviewContext context(long taskId, boolean writable) {
        var context = writable ? service().requireTaskContext(taskId) : service().readTaskContext(taskId);
        String expected = configService.getConfigValue("profile.person.flowCode");
        if (expected == null || expected.isBlank() || !expected.strip().equals(context.flowCode())) {
            throw new PersonApplicationException("PERSON_REVIEW_TASK_MISMATCH");
        }
        return context;
    }

    /** 当前人工审核通过后由引擎生成下一节点。 */
    @Override
    public void approve(long taskId, String reason) {
        service().approve(taskId, reason);
    }

    /** 将本次审核退回原发起人节点。 */
    @Override
    public void returnToApplicant(long taskId, String reason) {
        service().returnToApplicant(taskId, reason);
    }

    /** core 组合缺少工作流时明确报告能力不可用。 */
    private WorkflowTaskReviewService service() {
        var value = reviews.getIfAvailable();
        if (value == null) throw new PersonApplicationException("PERSON_WORKFLOW_UNAVAILABLE");
        return value;
    }
}
