package org.namewta.workflow.service.impl;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.namewta.workflow.common.ConditionalOnEnable;
import org.namewta.workflow.domain.bo.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/** 在现有任务门面统一执行 Client 约束，避免每个操作形成不同授权语义。 */
@Aspect
@Component
@ConditionalOnEnable
@Order(10)
@RequiredArgsConstructor
public class WorkflowClientGuardAspect {
    private final WorkflowClientScopeService scope;

    /** 人工查询和命令始终使用可信会话中的 Client，不采用请求传来的筛选条件。 */
    @Around("execution(public * org.namewta.workflow.service.impl.FlwTaskServiceImpl.*(..))")
    public Object guard(ProceedingJoinPoint call) throws Throwable {
        String method = call.getSignature().getName();
        Object[] args = call.getArgs();
        if (WorkflowTrustedExecution.active()) return call.proceed();
        switch (method) {
            case "pageByTaskWait", "pageByTaskFinish", "pageByTaskCopy", "pageByAllTaskWait", "pageByAllTaskFinish" ->
                ((FlowTaskBo) args[0]).getParams().put("workflowClientPk", scope.currentClient());
            case "completeTask" -> scope.requireAssignee(((CompleteTaskBo) args[0]).getTaskId());
            case "backProcess" -> scope.requireAssignee(((BackProcessBo) args[0]).getTaskId());
            case "terminationTask" -> scope.requireAssignee(((FlowTerminationBo) args[0]).taskId());
            case "taskOperation" -> {
                TaskOperationBo command = (TaskOperationBo) args[0];
                var task = scope.requireAssignee(command.getTaskId());
                if (!"reductionSignature".equals(args[1])) {
                    List<String> users = command.getUserId() == null ? command.getUserIds() : List.of(command.getUserId());
                    scope.requireRecipients(scope.scope(task.getInstanceId(), task.getNodeCode()).getClientPk(), users);
                }
            }
            case "updateAssignee" -> {
                for (Object id : (Collection<?>) args[0]) {
                    var task = scope.requireClient((Long) id);
                    scope.requireRecipients(scope.scope(task.getInstanceId(), task.getNodeCode()).getClientPk(), List.of((String) args[1]));
                }
            }
            case "urgeTask" -> {
                for (Long id : ((FlowUrgeTaskBo) args[0]).getTaskIdList()) scope.requireClient(id);
            }
            case "selectById", "getBackTaskNode" -> scope.requireClient((Long) args[0]);
            case "getNextNodeList" -> scope.requireClient(((FlowNextNodeBo) args[0]).getTaskId());
            case "currentTaskAllUser" -> {
                for (Object id : (Collection<?>) args[0]) scope.requireClient((Long) id);
            }
            default -> { }
        }
        return call.proceed();
    }

}
