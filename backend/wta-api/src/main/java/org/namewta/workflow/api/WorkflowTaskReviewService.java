package org.namewta.workflow.api;

import org.namewta.workflow.api.domain.WorkflowTaskReviewContext;

/** 面向业务审核页的人工任务合同；始终校验当前用户及节点指定客户端。 */
public interface WorkflowTaskReviewService {
    /** 读取本人当前待办或本人已办任务；已办返回当次历史变量而非最新提交。 */
    WorkflowTaskReviewContext readTaskContext(Long taskId);

    /** 返回当前可办理任务及不可变业务快照标识；任务不存在或身份不匹配时拒绝。 */
    WorkflowTaskReviewContext requireTaskContext(Long taskId);

    /** 通过当前节点；意见必填，后续节点按自己的客户端配置生成。 */
    void approve(Long taskId, String message);

    /** 将当前流程退回原申请节点，申请人修改后从头重审。 */
    void returnToApplicant(Long taskId, String message);
}
