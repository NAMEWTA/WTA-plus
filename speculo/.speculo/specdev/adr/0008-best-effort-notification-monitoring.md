# ADR-0008: Best-effort 通知监控

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-008

## Context

Spring Event 适合解耦监控落库，但不具备可靠队列语义；通知监控同时涉及正文、目标和敏感凭据的审计取舍。

## Decision

Dispatcher 在同步线程快照 userId、traceId 和可空 `client_pk`，再发布 Event；异步 Listener 只做 best-effort 全局监控，不重新读取 ThreadLocal。普通通知可用 FULL 审计，验证码和重置 Token 等 credential-like 内容必须 `REDACT_SENSITIVE`，不保存主题、正文、参数或可恢复目标。列表目标脱敏，HTML 不执行；Provider Secret、Authorization 和无关异常不入库。

## Consequences

应用宕机窗口允许丢监控，不能因缺日志重试 Provider。通知监控不按 Client 隔离；高权限明文详情和删除/清理权限需要严格运维控制。
