package org.namewta.workflow.api.domain;

/** 人工审核已授权上下文；Client 是 sys_client 主键，快照标识来自持久化实例变量。 */
public record WorkflowTaskReviewContext(Long taskId, Long instanceId, String businessId,
                                        String flowCode, Long clientPk, Integer snapshotVersion,
                                        Long submissionId) {
}
