# ADR-0017: 专用 HTTP 运行日志原样记录认证凭证

> **Superseded by [ADR-0025](./0025-http-runtime-log-credential-redaction.md)**

- **Status:** Accepted
- **Date:** 2026-08-28
- **Source:** `2026-08-26-current-log-system-eli5` ADR-001

## Context

系统日志 Filter 位于现有 `CryptoFilter` 下游，可以看到解密后的请求正文和外层响应加密前的正文。默认安全规范禁止把密码、token、验证码和 secret 写入持久日志，但当前产品决定优先保证完整原值排障能力，并接受日志成为可重放凭证副本的风险。

## Decision

仅对专用 logger `org.dromara.system.http`，在每方向 1 MiB 日志副本上限内，普通 JSON/文本正文中的密码、验证码、access/refresh token、client secret 及其他认证凭证不脱敏、不排除，原样写入 `sys-console.log`。文件、multipart、二进制和流式正文仍只记录元数据；该例外不得扩展到 `@Log`、数据库审计或其他日志链路。

## Consequences

日志访问者在凭证有效期内可能重放凭证，备份和最多 60 天/40GB 的归档也承载认证数据。必须严格控制日志访问、备份和传输，并保留外部 `sys.log.enabled=false` 止损开关。实现者不得静默恢复脱敏，也不得弱化大小和媒体排除边界；修改这项决定需要新的安全 ADR。
