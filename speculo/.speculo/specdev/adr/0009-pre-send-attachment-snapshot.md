# ADR-0009: 发送前通知附件快照

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-009

## Context

直接引用源业务附件会让历史通知随源文件变化；发送后复制又无法证明快照就是 Provider 实际使用的内容。

## Decision

Dispatcher 预生成 notifyLogId，在调用 Provider 前复制一组通知专用 OSS 对象，并用该快照发送。一个逻辑通知的 Delivery 共用快照，以 `ref_type=sys_notify_log` 和真实 notify log 主键建立引用。复制失败不调用 Provider；Provider 失败保留快照；异步日志失败时快照保持 TEMP，部分复制失败清理已创建副本。

## Consequences

历史附件不依赖源业务生命周期，但同步复制增加延迟和存储。删除通知日志时解除引用，零引用对象重新进入 TEMP；common-notify 不暴露 File/InputStream 或 system 实体。
