package org.namewta.profile.enterprise.support;

import java.time.Instant;

/**
 * EnterpriseVerificationTimeSource Profile 辅助组件，提供局部无状态能力。
 */
@FunctionalInterface
public interface EnterpriseVerificationTimeSource {

    /**
     * 处理 now 业务步骤。
     */
    Instant now();
}
