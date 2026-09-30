package org.namewta.profile.person.port.gateway;

import org.namewta.workflow.api.domain.WorkflowTaskReviewContext;

/** 个人审核使用的工作流端口。 */
public interface PersonTaskReviewGateway {
    /** 校验当前用户、客户端、业务流程及任务读写范围。 */
    WorkflowTaskReviewContext context(long taskId, boolean writable);
    /** 通过本级任务。 */
    void approve(long taskId, String reason);
    /** 退回原申请节点，重提后从头审核。 */
    void returnToApplicant(long taskId, String reason);
}
