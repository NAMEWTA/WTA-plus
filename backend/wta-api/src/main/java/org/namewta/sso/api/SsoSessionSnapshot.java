package org.namewta.sso.api;

import java.time.Instant;

/**
 * 中央会话原始认证时间与固定到期时间，不得以读取时间代替认证时间。
 * @param userId 账户主键
 * @param username 认证时账户名
 * @param authenticatedAt 原始密码认证时间
 * @param expiresAt 中央会话固定到期时间
 */
public record SsoSessionSnapshot(Long userId, String username, Instant authenticatedAt, Instant expiresAt) {
}

