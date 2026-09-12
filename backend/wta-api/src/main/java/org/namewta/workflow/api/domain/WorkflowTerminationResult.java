package org.namewta.workflow.api.domain;

/**
 * 按业务 ID 终止流程的引擎中立结果。
 *
 * @param status     终止状态
 * @param instanceId 被终止或已终态的实例 ID；从未存在时为 {@code null}
 */
public record WorkflowTerminationResult(Status status, Long instanceId) {

    public enum Status {
        TERMINATED,
        NO_ACTIVE_INSTANCE
    }
}
