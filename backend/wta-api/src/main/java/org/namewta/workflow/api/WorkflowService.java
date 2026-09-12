package org.namewta.workflow.api;

import org.namewta.workflow.api.domain.CompleteTaskDTO;
import org.namewta.workflow.api.domain.StartProcessDTO;
import org.namewta.workflow.api.domain.StartProcessReturnDTO;
import org.namewta.workflow.api.domain.WorkflowTerminationResult;

import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务
 *
 * @author may
 */
public interface WorkflowService {

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param businessIds 业务id
     * @return 结果
     */
    boolean deleteInstance(List<String> businessIds);

    /**
     * 按业务 ID 终止活动实例并保留流程历史。
     *
     * @param businessId 业务 ID
     * @param reason     终止原因
     * @return 终止结果；无实例或实例已终态时返回幂等结果
     */
    WorkflowTerminationResult terminateInstance(String businessId, String reason);

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     * @return 状态
     */
    String getBusinessStatusByTaskId(Long taskId);

    /**
     * 获取当前流程状态
     *
     * @param businessId 业务id
     * @return 状态
     */
    String getBusinessStatus(String businessId);

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例id
     * @param variable   流程变量
     */
    void setVariable(Long instanceId, Map<String, Object> variable);

    /**
     * 获取流程变量
     *
     * @param instanceId 流程实例id
     * @return 流程变量详情
     */
    Map<String, Object> instanceVariable(Long instanceId);

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessId 业务id
     * @return 结果
     */
    Long getInstanceIdByBusinessId(String businessId);

    /**
     * 启动流程
     *
     * @param startProcess 参数
     * @return 启动后的流程实例与首任务信息
     */
    StartProcessReturnDTO startWorkFlow(StartProcessDTO startProcess);

    /**
     * 办理任务
     * 系统后台发起审批 无用户信息 需要忽略权限
     * completeTask.getVariables().put("ignore", true);
     *
     * @param completeTask 参数
     * @return 办理成功返回 {@code true}
     */
    boolean completeTask(CompleteTaskDTO completeTask);

    /**
     * 办理任务
     *
     * @param taskId  任务ID
     * @param message 办理意见
     * @return 办理成功返回 {@code true}
     */
    boolean completeTask(Long taskId, String message);

    /**
     * 启动流程并办理第一个任务
     *
     * @param startProcess 参数
     * @return 首节点办理成功返回 {@code true}
     */
    boolean startCompleteTask(StartProcessDTO startProcess);
}
