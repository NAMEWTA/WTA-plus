package org.namewta.profile.person.domain.application;

import java.time.Instant;

/**
 * 工作流回调转成的个人申请应用命令。
 *
 * @param processInstanceId 流程实例编号
 * @param businessId 流程业务编号
 * @param flowCode 流程编码
 * @param status 流程状态
 * @param decision 流程决定
 * @param snapshotVersion 流程携带的快照版本
 * @param occurredTime 事件发生时间
 */
public record PersonApplicationProcessCommand(Long processInstanceId, String businessId, String flowCode,
                                              String status, String decision, Integer snapshotVersion,
                                              Instant occurredTime) {
}
