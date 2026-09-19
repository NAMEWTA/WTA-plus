package org.namewta.profile.person.port.gateway;

import org.namewta.workflow.api.event.ProcessEvent;

/** 个人工作流网关，封装流程启动和事件变量读取。 */
public interface PersonWorkflowGateway {

    /** 启动个人档案审批流程。 */
    void start(long applicationId, long submissionId, int snapshotVersion);

    /** 终止指定个人档案审批流程。 */
    void terminate(String businessId, String reason);

    /** 读取流程事件中的持久化快照版本。 */
    default Integer persistedSnapshotVersion(ProcessEvent event) {
        return event == null ? null : persistedSnapshotVersionByInstanceId(event.getInstanceId());
    }

    /** 读取流程实例中的持久化快照版本。 */
    Integer persistedSnapshotVersionByInstanceId(Long instanceId);
}
