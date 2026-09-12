package org.namewta.profile.enterprise.port.gateway;

import org.namewta.workflow.api.event.ProcessEvent;

/** 企业工作流网关，封装流程启动和事件变量读取。 */
public interface EnterpriseWorkflowGateway {

    /** 启动企业档案审批流程。 */
    void start(long applicationId, long submissionId, int snapshotVersion);

    /** 终止指定企业档案审批流程。 */
    default void terminate(String businessId, String reason) {
        throw new UnsupportedOperationException("当前工作流网关不支持终止流程");
    }

    /** 读取流程事件中的持久化快照版本。 */
    default Integer persistedSnapshotVersion(ProcessEvent event) {
        return event == null ? null : persistedSnapshotVersionByInstanceId(event.getInstanceId());
    }

    /** 读取流程实例中的持久化快照版本。 */
    default Integer persistedSnapshotVersionByInstanceId(Long instanceId) {
        return null;
    }
}
