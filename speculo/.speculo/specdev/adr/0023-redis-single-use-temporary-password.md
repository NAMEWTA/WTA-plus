# ADR-0023: 临时密码采用 Redis 一次性凭据

- **Status:** Accepted
- **Date:** 2026-08-29
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-28-user-password-policy-temporary-credentials/ADR.md</Path>` ADR-002

## Context

临时登录需要避免覆盖用户永久密码，同时限制凭据泄露后的重放窗口。该能力还必须在集群中保持一致的 60 秒有效期、支持新签发覆盖旧值，并保证并发认证时最多一次成功消费。

## Decision

临时密码不写入 `sys_user.password`，只把不可逆校验值存入 Redis 并设置 60 秒 TTL。凭据按用户作用域保存，新签发覆盖旧值；只有成功认证才能通过原子校验与删除消费。认证仍执行用户状态、验证码、目标 Client 和登录域准入，临时密码不能扩大账号原本的 Client 资格。

## Consequences

Redis 不可用时临时密码签发与校验失败关闭，永久密码认证保持原合同。错误密码或 Client 拒绝不得消费凭据；过期、覆盖、并发唯一消费和日志脱敏均是长期安全合同。该设计避免数据库敏感凭据生命周期和定时清理，但把一次性认证的可用性明确依赖于 Redis。
