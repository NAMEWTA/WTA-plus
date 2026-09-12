package org.namewta.profile.person.adapter.gateway;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.system.api.ConfigService;
import org.namewta.workflow.api.WorkflowService;
import org.namewta.workflow.api.domain.StartProcessDTO;
import org.namewta.workflow.api.event.ProcessEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/** 基于 Spring 依赖注入的个人工作流网关适配器。 */
@Component
public class SpringPersonWorkflowGateway implements PersonWorkflowGateway {
    public static final String FLOW_CODE_KEY = "profile.person.flowCode";
    private final ObjectProvider<WorkflowService> workflowProvider;
    private final ConfigService configService;
    /** 创建个人工作流网关适配器。 */
    public SpringPersonWorkflowGateway(ObjectProvider<WorkflowService> workflowProvider,
                                       ConfigService configService) {
        this.workflowProvider = workflowProvider;
        this.configService = configService;
    }
    /** 启动个人档案审批流程，并传递快照版本变量。 */
    @Override
    public void start(long applicationId, long submissionId, int snapshotVersion) {
        WorkflowService workflow = workflowProvider.getIfAvailable();
        String flowCode = configService.getConfigValue(FLOW_CODE_KEY);
        if (workflow == null || flowCode == null || flowCode.isBlank()) {
            throw new PersonApplicationException("PERSON_WORKFLOW_UNAVAILABLE");
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("profileType", "PERSON");
        variables.put("snapshotVersion", snapshotVersion);
        variables.put("submissionId", submissionId);
        StartProcessDTO process = new StartProcessDTO();
        process.setBusinessId(String.valueOf(applicationId));
        process.setFlowCode(flowCode.strip());
        process.setVariables(variables);
        try {
            if (!workflow.startCompleteTask(process)) {
                throw new PersonApplicationException("PERSON_WORKFLOW_START_FAILED");
            }
        } catch (PersonApplicationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PersonApplicationException("PERSON_WORKFLOW_START_FAILED", exception);
        }
    }
    /** 终止个人档案审批流程。 */
    @Override
    public void terminate(String businessId, String reason) {
        WorkflowService workflow = workflowProvider.getIfAvailable();
        if (workflow == null) {
            throw new PersonApplicationException("PERSON_WORKFLOW_UNAVAILABLE");
        }
        try {
            workflow.terminateInstance(businessId, reason);
        } catch (PersonApplicationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PersonApplicationException("PERSON_WORKFLOW_TERMINATE_FAILED", exception);
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
            throw new PersonApplicationException("PERSON_WORKFLOW_UNAVAILABLE");
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
            throw new PersonApplicationException("PERSON_WORKFLOW_SNAPSHOT_UNAVAILABLE");
        } catch (PersonApplicationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PersonApplicationException("PERSON_WORKFLOW_SNAPSHOT_UNAVAILABLE", exception);
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
