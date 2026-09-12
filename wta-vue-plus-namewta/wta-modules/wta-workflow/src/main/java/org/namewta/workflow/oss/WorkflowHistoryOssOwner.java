package org.namewta.workflow.oss;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.system.api.OssService;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.namewta.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Maintains OSS references owned by persisted WarmFlow history tasks.
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Service
public class WorkflowHistoryOssOwner {

    private static final String HISTORY_TASK_TABLE = "flow_his_task";

    private final FlowHisTaskMapper flowHisTaskMapper;
    private final OssService ossService;

    public PendingHistoryAttachments capture(Long taskId, String fileIds) {
        if (taskId == null) {
            throw new ServiceException("流程任务ID不能为空");
        }
        Set<Long> expectedOssIds = parseOssIds(fileIds);
        if (expectedOssIds.isEmpty()) {
            return new PendingHistoryAttachments(taskId, Set.of(), Set.of());
        }
        Set<Long> previousHistoryIds = new TreeSet<>();
        for (FlowHisTask history : historiesByTaskId(taskId)) {
            if (history.getId() == null) {
                throw new ServiceException("流程历史任务主键不能为空");
            }
            previousHistoryIds.add(history.getId());
        }
        return new PendingHistoryAttachments(taskId, previousHistoryIds, expectedOssIds);
    }

    public void reconcileCreated(PendingHistoryAttachments pending) {
        Objects.requireNonNull(pending, "历史附件协调上下文不能为空");
        if (pending.expectedOssIds().isEmpty()) {
            return;
        }
        List<FlowHisTask> created = historiesByTaskId(pending.taskId()).stream()
            .filter(history -> history.getId() != null)
            .filter(history -> !pending.previousHistoryIds().contains(history.getId()))
            .sorted(Comparator.comparing(FlowHisTask::getId))
            .toList();
        if (created.isEmpty()) {
            throw new ServiceException("流程办理未生成预期的历史任务");
        }
        boolean attachmentHistoryFound = false;
        for (FlowHisTask history : created) {
            Set<Long> actualOssIds = parseOssIds(history.getExt());
            if (actualOssIds.isEmpty()) {
                continue;
            }
            if (!actualOssIds.equals(pending.expectedOssIds())) {
                throw new ServiceException("流程历史任务附件与办理请求不一致");
            }
            ossService.reconcileReferences(HISTORY_TASK_TABLE, String.valueOf(history.getId()),
                List.of(), actualOssIds);
            attachmentHistoryFound = true;
        }
        if (!attachmentHistoryFound) {
            throw new ServiceException("流程历史任务未持久化附件");
        }
    }

    public void releaseByInstanceIds(Collection<Long> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return;
        }
        List<Long> ids = instanceIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
        if (ids.isEmpty()) {
            return;
        }
        List<FlowHisTask> histories = flowHisTaskMapper.selectList(QueryBuilder.lambda(FlowHisTask.class)
                .in(FlowHisTask::getInstanceId, ids)
                .build())
            .stream()
            .sorted(Comparator.comparing(FlowHisTask::getId, Comparator.nullsFirst(Long::compareTo)))
            .toList();
        for (FlowHisTask history : histories) {
            Set<Long> ossIds = parseOssIds(history.getExt());
            if (ossIds.isEmpty()) {
                continue;
            }
            if (history.getId() == null) {
                throw new ServiceException("流程历史任务主键不能为空");
            }
            ossService.reconcileReferences(HISTORY_TASK_TABLE, String.valueOf(history.getId()),
                ossIds, List.of());
        }
    }

    private List<FlowHisTask> historiesByTaskId(Long taskId) {
        return flowHisTaskMapper.selectList(QueryBuilder.lambda(FlowHisTask.class)
            .eq(FlowHisTask::getTaskId, taskId)
            .build());
    }

    private Set<Long> parseOssIds(String fileIds) {
        if (StringUtils.isBlank(fileIds)) {
            return Set.of();
        }
        Set<Long> ossIds = new LinkedHashSet<>();
        for (String value : fileIds.split(",", -1)) {
            String normalized = value.trim();
            if (normalized.isEmpty()) {
                throw new ServiceException("流程附件ID格式非法");
            }
            try {
                long ossId = Long.parseLong(normalized);
                if (ossId <= 0) {
                    throw new ServiceException("流程附件ID必须为正数");
                }
                ossIds.add(ossId);
            } catch (NumberFormatException ex) {
                throw new ServiceException("流程附件ID格式非法");
            }
        }
        return Set.copyOf(ossIds);
    }

    public record PendingHistoryAttachments(Long taskId, Set<Long> previousHistoryIds, Set<Long> expectedOssIds) {
        public PendingHistoryAttachments {
            previousHistoryIds = Set.copyOf(previousHistoryIds);
            expectedOssIds = Set.copyOf(expectedOssIds);
        }
    }
}
