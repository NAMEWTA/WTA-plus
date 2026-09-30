package org.namewta.workflow.service.impl;

import cn.dev33.satoken.exception.NotPermissionException;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.NodeType;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.WorkflowAssigneeDirectoryService;
import org.namewta.workflow.common.ConditionalOnEnable;
import org.namewta.workflow.domain.FlowInstanceNodeClient;
import org.namewta.workflow.domain.policy.WorkflowNodeClientPolicy;
import org.namewta.workflow.mapper.FlowInstanceNodeClientMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** 冻结实例节点客户端，并统一人工任务的端与办理人校验。 */
@Service
@ConditionalOnEnable
@RequiredArgsConstructor
public class WorkflowClientScopeService {
    private final FlowInstanceNodeClientMapper mapper;
    private final WorkflowAssigneeDirectoryService directory;

    /** 当前会话必须带有真实客户端主键，不允许超管或缺失上下文绕过。 */
    public Long currentClient() {
        var user = LoginHelper.getLoginUser();
        if (user == null || user.getClientPk() == null) throw new NotPermissionException("缺少办理客户端上下文");
        return user.getClientPk();
    }

    /** 新申请使用可信会话端，后台系统须同时明确端与申请人。 */
    public Long initiatorClient(org.namewta.workflow.domain.bo.StartProcessBo request) {
        Long clientPk = request.getInitiatorClientPk() == null ? currentClient() : request.getInitiatorClientPk();
        String userId = request.getHandler() == null || request.getHandler().isBlank()
            ? LoginHelper.getUserIdStr() : request.getHandler();
        if (userId == null || userId.isBlank()) throw new ServiceException("系统发起流程必须指定申请人");
        requireRecipients(clientPk, List.of(userId));
        return clientPk;
    }

    /** 撤销与重提均归原始发起人和发起端，不能借用其在其他端的会话。 */
    public void requireApplicant(Instance instance, Long clientPk, String userId) {
        if (!Objects.equals(clientPk, scope(instance.getId(), applicantNode(instance.getId())).getClientPk())
            || !Objects.equals(userId, instance.getCreateBy())) {
            throw new NotPermissionException("请由原申请人在发起客户端操作");
        }
    }

    /** 发布时验证申请节点与所有审核节点的 Client 和静态办理人。 */
    public void validateDefinition(Long definitionId) {
        List<Node> nodes = FlowEngine.nodeService().getByDefId(definitionId);
        List<Node> first = FlowEngine.nodeService().getFirstBetweenNode(definitionId, java.util.Map.of());
        if (first.size() != 1 || !NodeType.BETWEEN.getKey().equals(first.getFirst().getNodeType())) {
            throw new ServiceException("流程必须先经过唯一申请人节点");
        }
        String applicant = first.getFirst().getNodeCode();
        for (Node node : nodes) {
            if (!NodeType.BETWEEN.getKey().equals(node.getNodeType())) continue;
            String configured = WorkflowNodeClientPolicy.configuredClient(node.getExt());
            if (applicant.equals(node.getNodeCode())) {
                if (!WorkflowNodeClientPolicy.INITIATOR.equals(configured)) throw new ServiceException("申请节点须选择发起客户端");
                continue;
            }
            Long clientPk = WorkflowNodeClientPolicy.clientPk(configured);
            directory.requireClient(clientPk);
            if (node.getPermissionFlag() == null || node.getPermissionFlag().isBlank()) throw new ServiceException("审核节点未配置办理人");
            for (String token : node.getPermissionFlag().split("@@")) {
                if (token.startsWith("role:")) directory.requireRole(clientPk, WorkflowNodeClientPolicy.clientPk(token.substring(5)));
                else if (token.matches("[0-9]+")) requireRecipients(clientPk, List.of(token));
            }
        }
    }

    /** 在实例启动事务中一次冻结所有人工节点，后续重提不得重写。 */
    public void snapshot(Instance instance, Long initiatorClient) {
        directory.requireClient(initiatorClient);
        DefJson definition = FlowEngine.jsonConvert.strToBean(instance.getDefJson(), DefJson.class);
        for (var node : definition.getNodeList()) {
            if (!NodeType.BETWEEN.getKey().equals(node.getNodeType())) continue;
            String configured = WorkflowNodeClientPolicy.configuredClient(node.getExt());
            boolean applicant = WorkflowNodeClientPolicy.INITIATOR.equals(configured);
            FlowInstanceNodeClient row = new FlowInstanceNodeClient();
            row.setInstanceId(instance.getId());
            row.setNodeCode(node.getNodeCode());
            row.setClientPk(applicant ? initiatorClient : WorkflowNodeClientPolicy.clientPk(configured));
            row.setApplicantNode(applicant);
            row.setVersion(0);
            row.setDelFlag("0");
            mapper.insert(row);
        }
    }

    /** 按实例节点读取不可变快照，缺失代表尚未完成迁移而非全客户端授权。 */
    public FlowInstanceNodeClient scope(Long instanceId, String nodeCode) {
        FlowInstanceNodeClient row = mapper.selectOne(QueryBuilder.lambda(FlowInstanceNodeClient.class)
            .eq(FlowInstanceNodeClient::getInstanceId, instanceId).eq(FlowInstanceNodeClient::getNodeCode, nodeCode).build());
        if (row == null) throw new ServiceException("流程节点缺少客户端快照，请先完成流程配置迁移");
        return row;
    }

    /** 获取存在的任务并核对当前 Client；用户归属由 requireAssignee 或引擎检查。 */
    public Task requireClient(Long taskId) {
        Task task = FlowEngine.taskService().getById(taskId);
        if (task == null) throw new ServiceException("流程任务不存在或已办理");
        if (!Objects.equals(currentClient(), scope(task.getInstanceId(), task.getNodeCode()).getClientPk())) {
            throw new NotPermissionException("请在该节点指定的客户端办理");
        }
        return task;
    }

    /** 校验当前用户是有效审批、委派或转办对象。 */
    public Task requireAssignee(Long taskId) {
        Task task = requireClient(taskId);
        String userId = LoginHelper.getUserIdStr();
        boolean assigned = FlowEngine.userService().getByAssociateds(List.of(taskId)).stream()
            .anyMatch(user -> userId.equals(user.getProcessedBy()) && List.of("1", "2", "3").contains(String.valueOf(user.getType())));
        if (!assigned) throw new NotPermissionException("当前用户不是任务办理人");
        return task;
    }

    /** 转办、委派、加签和动态选人均不得把任务交给无法登录指定 Client 的用户。 */
    public void requireRecipients(Long clientPk, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) throw new ServiceException("审核节点没有有效办理人");
        List<Long> ids = userIds.stream().map(WorkflowNodeClientPolicy::clientPk).distinct().toList();
        if (!directory.eligibleUsers(clientPk, ids).containsAll(ids)) throw new ServiceException("办理人不具备该客户端登录资格");
    }

    /** 由全局监听器直接调用，避免 notify 内部调用 assignment 绕过 Spring AOP。 */
    public void validateAssignments(org.dromara.warm.flow.core.listener.ListenerVariable event) {
        // 新实例的申请人任务先生成，启动事务随后保存节点快照与资格校验。
        if (event.getTask() == null || event.getNextTasks() == null) return;
        for (Task task : event.getNextTasks()) {
            if (!NodeType.BETWEEN.getKey().equals(task.getNodeType())) continue;
            requireRecipients(scope(task.getInstanceId(), task.getNodeCode()).getClientPk(), task.getPermissionList());
        }
    }

    /** 自动审批只有仍处于本客户端的节点才能继续。 */
    public boolean isCurrentClient(Task task) {
        if (WorkflowTrustedExecution.active()) return false;
        var user = LoginHelper.getLoginUser();
        return user != null && user.getClientPk() != null
            && Objects.equals(user.getClientPk(), scope(task.getInstanceId(), task.getNodeCode()).getClientPk());
    }

    /** 查找原申请节点供退回使用。 */
    public String applicantNode(Long instanceId) {
        FlowInstanceNodeClient row = mapper.selectOne(QueryBuilder.lambda(FlowInstanceNodeClient.class)
            .eq(FlowInstanceNodeClient::getInstanceId, instanceId).eq(FlowInstanceNodeClient::getApplicantNode, true).build());
        if (row == null) throw new ServiceException("流程缺少申请节点客户端快照");
        return row.getNodeCode();
    }

    /** 流程图允许本端的发起人、当前办理人或历史办理人查看。 */
    public void requireInstanceParticipant(Long instanceId) {
        var instance = FlowEngine.insService().getById(instanceId);
        if (instance == null) throw new ServiceException("流程实例不存在");
        Long clientPk = currentClient();
        String userId = LoginHelper.getUserIdStr();
        if (userId.equals(instance.getCreateBy()) && Objects.equals(clientPk, scope(instanceId, applicantNode(instanceId)).getClientPk())) return;
        for (Task task : FlowEngine.taskService().getByInsId(instanceId)) {
            if (!Objects.equals(clientPk, scope(instanceId, task.getNodeCode()).getClientPk())) continue;
            if (FlowEngine.userService().getByAssociateds(List.of(task.getId())).stream().anyMatch(user -> userId.equals(user.getProcessedBy()))) return;
        }
        boolean historical = FlowEngine.hisTaskService().getByInsId(instanceId).stream()
            .filter(task -> NodeType.BETWEEN.getKey().equals(task.getNodeType()) && userId.equals(task.getApprover()))
            .anyMatch(task -> Objects.equals(clientPk, scope(instanceId, task.getNodeCode()).getClientPk()));
        if (!historical) throw new NotPermissionException("无权在当前客户端查看该流程");
    }

    /** 删除实例时同步释放本方快照。 */
    public void deleteSnapshots(List<Long> instanceIds) {
        if (instanceIds != null && !instanceIds.isEmpty()) mapper.delete(QueryBuilder.lambda(FlowInstanceNodeClient.class)
            .in(FlowInstanceNodeClient::getInstanceId, instanceIds).build());
    }
}
