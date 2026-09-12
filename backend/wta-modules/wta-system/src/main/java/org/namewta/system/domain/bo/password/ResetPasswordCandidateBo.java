package org.namewta.system.domain.bo.password;

import jakarta.validation.constraints.NotNull;

/**
 * 永久密码重置候选请求。
 *
 * @param userId 目标用户 ID
 */
public record ResetPasswordCandidateBo(
    @NotNull(message = "用户ID不能为空") Long userId) {
}
