package org.namewta.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.utils.StreamUtils;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Resolves workflow task recipients for system side effects.
 *
 * <p>This component intentionally does not apply the user-facing task read
 * permission check. Notification listeners can run without a login context;
 * callers must provide task IDs from the trusted workflow event path.</p>
 */
@Component
@RequiredArgsConstructor
public class WorkflowTaskRecipientResolver {

    private final UserService userService;

    /**
     * Resolves users associated with the supplied workflow tasks.
     *
     * @param taskIds workflow task IDs
     * @return active system user projections
     */
    public List<UserDTO> resolve(List<Long> taskIds) {
        if (CollUtil.isEmpty(taskIds)) {
            return Collections.emptyList();
        }
        List<Long> validTaskIds = taskIds.stream()
            .filter(ObjectUtil::isNotNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(validTaskIds)) {
            return Collections.emptyList();
        }
        List<User> associatedUsers = FlowEngine.userService().getByAssociateds(validTaskIds);
        if (CollUtil.isEmpty(associatedUsers)) {
            return Collections.emptyList();
        }
        return userService.selectListByIds(StreamUtils.toSet(associatedUsers,
            user -> Convert.toLong(user.getProcessedBy(), null)));
    }
}
