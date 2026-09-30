package org.namewta.workflow.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.workflow.api.WorkflowTaskReviewService;
import org.namewta.workflow.api.domain.WorkflowTaskReviewContext;
import org.namewta.workflow.common.ConditionalOnEnable;
import org.namewta.workflow.domain.bo.BackProcessBo;
import org.namewta.workflow.domain.bo.CompleteTaskBo;
import org.namewta.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 业务审核只经当前任务身份办理，流程客户端与快照值均由服务端读取。 */
@Service
@ConditionalOnEnable
@RequiredArgsConstructor
public class WorkflowTaskReviewServiceImpl implements WorkflowTaskReviewService {
    private final WorkflowClientScopeService scope;
    private final IFlwTaskService tasks;

    @Override
    public WorkflowTaskReviewContext readTaskContext(Long taskId) {
        if (FlowEngine.taskService().getById(taskId) != null) {
            try {
                return requireTaskContext(taskId);
            } catch (cn.dev33.satoken.exception.NotPermissionException denied) {
                // 会签或委派后原任务仍活动，本人的已办记录仍可按历史快照读取。
            }
        }
        var history = FlowEngine.hisTaskService().list(FlowEngine.newHisTask().setTaskId(taskId)).stream()
            .filter(row -> org.namewta.common.satoken.utils.LoginHelper.getUserIdStr().equals(row.getApprover()))
            .filter(row -> java.util.Objects.equals(scope.currentClient(), scope.scope(row.getInstanceId(), row.getNodeCode()).getClientPk()))
            .max(java.util.Comparator.comparing(org.dromara.warm.flow.core.entity.HisTask::getId))
            .orElseThrow(() -> new cn.dev33.satoken.exception.NotPermissionException("无权查看该审核任务"));
        var instance = FlowEngine.insService().getById(history.getInstanceId());
        var definition = FlowEngine.defService().getById(history.getDefinitionId());
        var variables = FlowEngine.jsonConvert.strToMap(history.getVariable());
        Long snapshot = number(variables == null ? null : variables.get("snapshotVersion"));
        return new WorkflowTaskReviewContext(taskId, instance.getId(), instance.getBusinessId(), definition.getFlowCode(),
            scope.scope(instance.getId(), history.getNodeCode()).getClientPk(), snapshot == null ? null : Math.toIntExact(snapshot),
            number(variables == null ? null : variables.get("submissionId")));
    }

    @Override
    public WorkflowTaskReviewContext requireTaskContext(Long taskId) {
        var task = scope.requireAssignee(taskId);
        var instance = FlowEngine.insService().getById(task.getInstanceId());
        var definition = FlowEngine.defService().getById(task.getDefinitionId());
        var variables = instance.getVariableMap();
        Long snapshot = number(variables.get("snapshotVersion"));
        return new WorkflowTaskReviewContext(taskId, instance.getId(), instance.getBusinessId(), definition.getFlowCode(),
            scope.scope(instance.getId(), task.getNodeCode()).getClientPk(), snapshot == null ? null : Math.toIntExact(snapshot),
            number(variables.get("submissionId")));
    }

    @Override
    @DSTransactional
    public void approve(Long taskId, String message) {
        requireTaskContext(taskId);
        CompleteTaskBo command = new CompleteTaskBo();
        command.setTaskId(taskId);
        command.setMessage(opinion(message));
        command.setMessageType(List.of("1"));
        tasks.completeTask(command);
    }

    @Override
    @DSTransactional
    public void returnToApplicant(Long taskId, String message) {
        var context = requireTaskContext(taskId);
        BackProcessBo command = new BackProcessBo();
        command.setTaskId(taskId);
        command.setNodeCode(scope.applicantNode(context.instanceId()));
        command.setMessage(opinion(message));
        command.setMessageType(List.of("1"));
        tasks.backProcess(command);
    }

    /** 意见为空时不创建没有业务说明的历史。 */
    private String opinion(String message) {
        if (message == null || message.isBlank()) throw new ServiceException("请填写审核意见");
        return message.strip();
    }

    /** 可选的业务快照变量保留空值，损坏值明确拒绝。 */
    private Long number(Object value) {
        if (value == null) return null;
        try { return Long.valueOf(String.valueOf(value)); }
        catch (NumberFormatException failure) { throw new ServiceException("流程业务快照编号无效", failure); }
    }
}
