package org.namewta.system.openapi.credential.model;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Credential creation input. The owner is deliberately absent.
 */
public record OpenApiCredentialCreateRequest(
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 100, message = "应用名称不能超过{max}个字符")
    String appName,
    @Future(message = "过期时间必须晚于当前时间")
    LocalDateTime expiresAt,
    @Size(max = 500, message = "备注不能超过{max}个字符")
    String remark
) {
}
