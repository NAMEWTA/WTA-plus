package org.namewta.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.lock.annotation.Lock4j;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.enums.BusinessStatusEnum;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.namewta.workflow.api.WorkflowService;
import org.namewta.workflow.api.domain.CompleteTaskDTO;
import org.namewta.workflow.api.domain.StartProcessDTO;
import org.namewta.workflow.api.domain.StartProcessReturnDTO;
import org.namewta.workflow.api.domain.WorkflowTerminationResult;
import org.namewta.workflow.common.ConditionalOnEnable;
import org.namewta.workflow.common.enums.MessageTypeEnum;
import org.namewta.workflow.common.enums.TaskStatusEnum;
import org.namewta.workflow.domain.FlowInstanceBizExt;
import org.namewta.workflow.domain.bo.CompleteTaskBo;
import org.namewta.workflow.domain.bo.StartProcessBo;
import org.namewta.workflow.service.IFlwInstanceService;
import org.namewta.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务实现
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final IFlwInstanceService flwInstanceService;
    private final IFlwTaskService flwTaskService;
    private final TaskService taskService;

    /**
     * 删除流程实例
     *
     * @param businessIds 业务id
     * @return 结果
     */
    @Override
    public boolean deleteInstance(List<String> businessIds) {
        return flwInstanceService.deleteByBusinessIds(businessIds);
    }

    @Override
    @Lock4j(keys = {"'workflow:terminate:' + #businessId"})
    @DSTransactional
    public WorkflowTerminationResult terminateInstance(String businessId, String reason) {
        String normalizedBusinessId = requireText(businessId, "businessId");
        String normalizedReason = requireText(reason, "reason");
        FlowInstance instance = flwInstanceService.selectInstByBusinessId(normalizedBusinessId);
        if (instance == null) {
            return new WorkflowTerminationResult(WorkflowTerminationResult.Status.NO_ACTIVE_INSTANCE, null);
        }
        if (BusinessStatusEnum.finishStatus().contains(instance.getFlowStatus())) {
            return new WorkflowTerminationResult(
                WorkflowTerminationResult.Status.NO_ACTIVE_INSTANCE, instance.getId());
        }
        if (!BusinessStatusEnum.runningStatus().contains(instance.getFlowStatus())) {
            throw new ServiceException("Unsupported workflow status for termination");
        }

        FlowParams flowParams = FlowParams.build()
            .message(normalizedReason)
            .flowStatus(BusinessStatusEnum.TERMINATION.getStatus())
            .hisStatus(TaskStatusEnum.TERMINATION.getStatus())
            .ignore(true);
        Instance terminated = taskService.terminationByInsId(instance.getId(), flowParams);
        if (terminated == null) {
            throw new ServiceException("Workflow engine did not confirm termination");
        }
        return new WorkflowTerminationResult(WorkflowTerminationResult.Status.TERMINATED, instance.getId());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     * @return 任务关联流程实例的业务状态，未查询到时返回空字符串
     */
    @Override
    public String getBusinessStatusByTaskId(Long taskId) {
        FlowInstance flowInstance = flwInstanceService.selectByTaskId(taskId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 获取当前流程状态
     *
     * @param businessId 业务id
     * @return 业务单据对应的流程状态，未查询到时返回空字符串
     */
    @Override
    public String getBusinessStatus(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例id
     * @param variables  流程变量
     */
    @Override
    public void setVariable(Long instanceId, Map<String, Object> variables) {
        flwInstanceService.setVariable(instanceId, variables);
    }

    /**
     * 获取流程变量
     *
     * @param instanceId 流程实例id
     * @return 实例变量信息
     */
    @Override
    public Map<String, Object> instanceVariable(Long instanceId) {
        return flwInstanceService.instanceVariable(instanceId);
    }

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessId 业务id
     * @return 结果
     */
    @Override
    public Long getInstanceIdByBusinessId(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getId() : null;
    }

    /**
     * 启动流程
     *
     * @param startProcess 参数
     * @return 启动后的流程实例和首个任务信息
     */
    @Override
    public StartProcessReturnDTO startWorkFlow(StartProcessDTO startProcess) {
        return flwTaskService.startWorkFlow(BeanUtil.toBean(startProcess, StartProcessBo.class));
    }

    /**
     * 办理任务
     * 系统后台发起审批 无用户信息 需要忽略权限
     * completeTask.getVariables().put("ignore", true);
     *
     * @param completeTask 参数
     * @return 办理成功返回 {@code true}
     */
    @Override
    public boolean completeTask(CompleteTaskDTO completeTask) {
        return flwTaskService.completeTask(BeanUtil.toBean(completeTask, CompleteTaskBo.class));
    }

    /**
     * 办理任务
     *
     * @param taskId  任务ID
     * @param message 办理意见
     * @return 办理成功返回 {@code true}
     */
    @Override
    public boolean completeTask(Long taskId, String message) {
        CompleteTaskBo completeTask = new CompleteTaskBo();
        completeTask.setTaskId(taskId);
        completeTask.setMessage(message);
        // 忽略权限(系统后台发起审批 无用户信息 需要忽略权限)
        completeTask.getVariables().put("ignore", true);
        return flwTaskService.completeTask(completeTask);
    }

    /**
     * 启动流程并办理第一个任务
     *
     * @param startProcess 参数
     * @return 首节点办理成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startCompleteTask(StartProcessDTO startProcess) {
        StartProcessBo processBo = new StartProcessBo();
        processBo.setBusinessId(startProcess.getBusinessId());
        processBo.setFlowCode(startProcess.getFlowCode());
        processBo.setVariables(startProcess.getVariables());
        processBo.setHandler(startProcess.getHandler());
        processBo.setBizExt(BeanUtil.toBean(startProcess.getBizExt(), FlowInstanceBizExt.class));

        StartProcessReturnDTO result = flwTaskService.startWorkFlow(processBo);
        CompleteTaskBo taskBo = new CompleteTaskBo();
        taskBo.setTaskId(result.taskId());
        taskBo.setMessageType(Collections.singletonList(MessageTypeEnum.SYSTEM_MESSAGE.getCode()));
        taskBo.setVariables(startProcess.getVariables());
        taskBo.setHandler(startProcess.getHandler());
        return flwTaskService.completeTask(taskBo);
    }
}
