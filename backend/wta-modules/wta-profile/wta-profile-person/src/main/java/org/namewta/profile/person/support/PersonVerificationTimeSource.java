package org.namewta.profile.person.support;

import java.time.Instant;

/**
 * PersonVerificationTimeSource Profile 辅助组件，提供局部无状态能力。
 */
@FunctionalInterface
public interface PersonVerificationTimeSource {

    /**
     * 处理 now 业务步骤。
     */
    Instant now();
}
