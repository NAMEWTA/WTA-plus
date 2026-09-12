package org.namewta.profile.enterprise.adapter.gateway;
import org.namewta.profile.enterprise.domain.exception.EnterpriseApplicationException;
import org.namewta.profile.enterprise.port.gateway.EnterpriseWorkflowGateway;
import org.namewta.system.api.ConfigService;
import org.namewta.workflow.api.WorkflowService;
import org.namewta.workflow.api.domain.StartProcessDTO;
import org.namewta.workflow.api.event.ProcessEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/** 基于 Spring 依赖注入的企业工作流网关适配器。 */
@Component
public class SpringEnterpriseWorkflowGateway implements EnterpriseWorkflowGateway {
    public static final String FLOW_CODE_KEY = "profile.enterprise.flowCode";
    private final ObjectProvider<WorkflowService> workflowProvider;
    private final ConfigService configService;
    /** 创建企业工作流网关适配器。 */
    public SpringEnterpriseWorkflowGateway(ObjectProvider<WorkflowService> workflowProvider,
                                       ConfigService configService) {
        this.workflowProvider = workflowProvider;
        this.configService = configService;
    }
    /** 启动企业档案审批流程，并传递快照版本变量。 */
    @Override
    public void start(long applicationId, long submissionId, int snapshotVersion) {
        WorkflowService workflow = workflowProvider.getIfAvailable();
        String flowCode = configService.getConfigValue(FLOW_CODE_KEY);
        if (workflow == null || flowCode == null || flowCode.isBlank()) {
            throw new EnterpriseApplicationException("ENTERPRISE_WORKFLOW_UNAVAILABLE");
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("profileType", "ENTERPRISE");
        variables.put("snapshotVersion", snapshotVersion);
        variables.put("submissionId", submissionId);
        StartProcessDTO process = new StartProcessDTO();
        process.setBusinessId(String.valueOf(applicationId));
        process.setFlowCode(flowCode.strip());
        process.setVariables(variables);
        try {
            if (!workflow.startCompleteTask(process)) {
                throw new EnterpriseApplicationException("ENTERPRISE_WORKFLOW_START_FAILED");
            }
        } catch (EnterpriseApplicationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new EnterpriseApplicationException("ENTERPRISE_WORKFLOW_START_FAILED", exception);
        }
    }
    /** 终止企业档案审批流程。 */
    @Override
    public void terminate(String businessId, String reason) {
        WorkflowService workflow = workflowProvider.getIfAvailable();
        if (workflow == null) {
            throw new EnterpriseApplicationException("ENTERPRISE_WORKFLOW_UNAVAILABLE");
        }
        try {
            workflow.terminateInstance(businessId, reason);
        } catch (EnterpriseApplicationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new EnterpriseApplicationException("ENTERPRISE_WORKFLOW_TERMINATE_FAILED", exception);
        }
    }
    /** 读取流程事件中的持久化快照版本。 */
    @Override
    public Integer persistedSnapshotVersion(ProcessEvent event) {
        return event == null ? null : persistedSnapshotVersionByInstanceId(event.getInstanceId());
    }
    /** 读取流程实例中的持久化快照版本。 */
    @Override
    public Integer persistedSnapshotVersionByInstanceId(Long instanceId) {
        if (instanceId == null || instanceId <= 0) {
            return null;
        }
        WorkflowService workflow = workflowProvider.getIfAvailable();
        if (workflow == null) {
            throw new EnterpriseApplicationException("ENTERPRISE_WORKFLOW_UNAVAILABLE");
        }
        try {
            Map<String, Object> variables = workflow.instanceVariable(instanceId);
            Object entries = variables == null ? null : variables.get("variableList");
            if (entries instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof Map<?, ?> item && "snapshotVersion".equals(item.get("key"))) {
                        Integer snapshotVersion = positiveInteger(item.get("value"));
                        if (snapshotVersion != null) {
                            return snapshotVersion;
                        }
                        break;
                    }
                }
            }
            throw new EnterpriseApplicationException("ENTERPRISE_WORKFLOW_SNAPSHOT_UNAVAILABLE");
        } catch (EnterpriseApplicationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new EnterpriseApplicationException("ENTERPRISE_WORKFLOW_SNAPSHOT_UNAVAILABLE", exception);
        }
    }
    /** 校验正整数编号。 */
    private Integer positiveInteger(Object value) {
        try {
            int parsed = value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
