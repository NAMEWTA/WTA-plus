package org.namewta.workflow.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.workflow.api.WorkflowTaskReviewService;
import org.namewta.workflow.common.ConditionalOnEnable;
import org.namewta.workflow.domain.bo.CompleteTaskBo;
import org.namewta.workflow.domain.bo.StartProcessBo;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/** 保护人机入口和第三方设计器别名，可信 Java 系统办理不经过此边界。 */
@Aspect
@Component
@ConditionalOnEnable
@Order(5)
@RequiredArgsConstructor
public class WorkflowHttpGuardAspect {
    private static final Set<String> INTERNAL_VARIABLES = Set.of("ignore", "ignoreDepute", "ignoreCooperate", "snapshotVersion", "submissionId", "WorkflowClientPk");
    private final WorkflowClientScopeService scope;
    private final WorkflowTaskReviewService reviews;

    /** 禁止从 HTTP 注入系统办理人或绕过变量。 */
    @Around("execution(* org.namewta.workflow.controller.FlwTaskController.completeTask(..)) || execution(* org.namewta.workflow.controller.FlwTaskController.startWorkFlow(..))")
    public Object humanCommand(ProceedingJoinPoint call) throws Throwable {
        Object command = call.getArgs()[0];
        if (command instanceof CompleteTaskBo complete) {
            requireNoHandler(complete.getHandler());
            requireHumanVariables(complete.getVariables());
        } else if (command instanceof StartProcessBo start) {
            requireNoHandler(start.getHandler());
            requireHumanVariables(start.getVariables());
        }
        scope.currentClient();
        return call.proceed();
    }

    /** 普通实例查询与撤销跟随参与关系；全局监控仍需独立的实例管理权限。 */
    @Around("execution(* org.namewta.workflow.controller.FlwInstanceController.getInfo(..)) || "
        + "execution(* org.namewta.workflow.controller.FlwInstanceController.flowHisTaskList(..)) || "
        + "execution(* org.namewta.workflow.controller.FlwInstanceController.instanceVariable(..)) || "
        + "execution(* org.namewta.workflow.controller.FlwInstanceController.cancelProcessApply(..))")
    public Object instanceEntry(ProceedingJoinPoint call) throws Throwable {
        String name = call.getSignature().getName();
        Object argument = call.getArgs()[0];
        boolean cancel = "cancelProcessApply".equals(name);
        if (!cancel && StpUtil.hasPermission("workflow:instance:list")) return call.proceed();
        org.dromara.warm.flow.core.entity.Instance instance;
        if ("instanceVariable".equals(name)) instance = FlowEngine.insService().getById((Long) argument);
        else {
            String businessId = cancel ? ((org.namewta.workflow.domain.bo.FlowCancelBo) argument).businessId() : argument.toString();
            instance = FlowEngine.insService().list(FlowEngine.newIns().setBusinessId(businessId)).stream().findFirst().orElse(null);
        }
        if (instance == null) throw new ServiceException("流程实例不存在");
        if (cancel) scope.requireApplicant(instance, scope.currentClient(), org.namewta.common.satoken.utils.LoginHelper.getUserIdStr());
        else scope.requireInstanceParticipant(instance.getId());
        return call.proceed();
    }

    /** 变量维护不能重写业务提交快照或启用系统跳过检查。 */
    @Around("execution(* org.namewta.workflow.controller.FlwInstanceController.updateVariable(..))")
    public Object updateVariable(ProceedingJoinPoint call) throws Throwable {
        var command = (org.namewta.workflow.domain.bo.FlowVariableBo) call.getArgs()[0];
        if (INTERNAL_VARIABLES.contains(command.key())) throw new ServiceException("不可修改服务端专用流程变量");
        return call.proceed();
    }

    /** 已发布或已使用定义只能复制为新版本，不能原地改变运行实例的节点。 */
    @Around("execution(* org.namewta.workflow.controller.FlwDefinitionController.edit(..))")
    public Object editDefinition(ProceedingJoinPoint call) throws Throwable {
        requireDraft(((FlowDefinition) call.getArgs()[0]).getId());
        return call.proceed();
    }

    /** 第三方 UI 只复用展示协议，不形成不受约束的第二组任务/定义入口。 */
    @Around("execution(public * org.dromara.warm.flow.ui.controller.WarmFlowController.*(..))")
    public Object vendorEntry(ProceedingJoinPoint call) throws Throwable {
        String method = call.getSignature().getName();
        Object[] args = call.getArgs();
        switch (method) {
            case "saveJson" -> {
                StpUtil.checkPermission("workflow:definition:edit");
                requireDraft(((DefJson) args[0]).getId());
            }
            // 插件内部直接调用引擎，缺少项目事务、锁和业务状态协议；禁止形成第二条写路径。
            case "handle" -> throw new ServiceException("请通过工作流任务办理入口提交");
            case "load" -> reviews.readTaskContext((Long) args[0]);
            case "hisLoad" -> {
                var history = FlowEngine.hisTaskService().getById((Long) args[0]);
                if (history == null) throw new ServiceException("历史任务不存在");
                reviews.readTaskContext(history.getTaskId());
            }
            case "queryFlowChart" -> scope.requireInstanceParticipant((Long) args[0]);
            default -> StpUtil.checkPermissionOr("workflow:definition:query", "workflow:definition:edit");
        }
        return call.proceed();
    }

    /** 检查真实已持久化的状态，不能采用请求内 isPublish。 */
    private void requireDraft(Long id) {
        if (id == null) return;
        var definition = FlowEngine.defService().getById(id);
        if (definition == null) throw new ServiceException("流程定义不存在");
        if (!Integer.valueOf(0).equals(definition.getIsPublish()) || !FlowEngine.insService().getByDefId(id).isEmpty()) {
            throw new ServiceException("已发布或已使用流程请复制新版本后修改");
        }
    }

    private void requireNoHandler(String handler) {
        if (handler != null && !handler.isBlank()) throw new ServiceException("人工办理不允许覆盖登录用户");
    }

    private void requireHumanVariables(Map<?, ?> variables) {
        if (variables != null && variables.keySet().stream().anyMatch(INTERNAL_VARIABLES::contains)) {
            throw new ServiceException("请求包含服务端专用流程变量");
        }
    }
}
