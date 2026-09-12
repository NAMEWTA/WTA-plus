# ADR-0025: HTTP 运行日志凭据脱敏与失败关闭

- **Status:** Accepted
- **Date:** 2026-08-31
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-29-login-password-policy-runtime-and-http-log-redaction</Path>`
- **Supersedes:** ADR-0017

## Context

ADR-0017 曾允许专用 HTTP logger 原样持久化密码、token、验证码和 secret，以换取完整原值排障能力。真实登录故障日志随后确认 `Authorization`、`Cookie` 与 JSON 凭据会进入持久日志，使日志和备份成为可重放凭据副本。当前实现和回归测试已经撤销这项安全例外。

## Decision

HTTP 运行日志保留普通请求/响应元数据和非敏感正文信息，但不得持久化认证、会话、密码、token、secret、captcha 等凭据原值。请求与响应敏感头以及请求参数按归一化名称替换为 `[REDACTED]`；JSON 日志副本递归脱敏敏感字段，非法或截断 JSON 整段失败关闭为 `[REDACTED]`。脱敏只作用于日志副本，不改变 Servlet request/response、认证决策或加解密正文。

## Consequences

运行日志不再提供可重放凭据，日志访问、备份和传输的暴露面随之降低。敏感字段及无法可靠解析的 JSON 正文会失去原值可见性，但请求关联、状态、耗时、普通头、非敏感字段和符合媒体策略的正文仍可用于排障。任何恢复凭据原样记录的变化都必须形成新的安全决策并提供明确风险批准。
