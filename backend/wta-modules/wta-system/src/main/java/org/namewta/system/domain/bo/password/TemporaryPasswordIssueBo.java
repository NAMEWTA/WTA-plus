package org.namewta.system.domain.bo.password;

import jakarta.validation.constraints.NotNull;

/**
 * 临时密码签发请求。
 *
 * @param userId 目标用户 ID
 */
public record TemporaryPasswordIssueBo(
    @NotNull(message = "用户ID不能为空") Long userId) {
}
