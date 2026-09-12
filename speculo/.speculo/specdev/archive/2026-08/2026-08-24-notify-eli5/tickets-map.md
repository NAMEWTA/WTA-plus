---
schema_version: 3
artifact: tickets-map
change: 2026-08-24-notify-eli5
status: ready
---

# Tickets Map: 统一通知控制面

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `ticket/01-api-module.md` | API 合同、模块和装配 | — | standard | high | yes | codex | AC-001/007 | Wave 1 | done |
| T-02 | `ticket/02-schema-core.md` | notify_* 表、DAO、状态模型 | T-01 | deep | high | yes | codex | AC-001/002/003 | Wave 2 | done |
| T-03 | `ticket/03-routing-provider.md` | 模板、路由、偏好和 Provider | T-01/T-02 | deep | high | yes | codex | AC-003/006/007 | Wave 3 | done |
| T-04 | `ticket/04-inbox.md` | system 收件箱和 In-App Port | T-01/T-02 | deep | high | yes | codex | AC-004 | Wave 3 | done |
| T-05 | `ticket/05-outbox-worker.md` | Outbox、租约、重试、死信 | T-02/T-03 | deep | high | yes | codex | AC-002/003 | Wave 4 | done |
| T-06 | `ticket/06-notice-migration.md` | 公告发布、调用方迁移、旧入口删除 | T-03/T-04/T-05 | deep | high | yes | codex | AC-005/009 | Wave 5 | done |
| T-07 | `ticket/07-monitor-frontend.md` | 统一监控、收件箱和管理页面 | T-02/T-04/T-06 | deep | high | yes | codex | AC-004/005/008 | Wave 6 | done |
| T-08 | `ticket/08-verification.md` | 全量测试、架构审查和交付证据 | T-01..T-07 | deep | high | yes | codex | AC-001..AC-009 | Gate | done |

依赖 DAG：`T-01 -> T-02 -> (T-03,T-04) -> T-05 -> T-06 -> T-07 -> T-08`。

新 `notify_*`、system 收件箱和 notify 前端分别由对应 Ticket 独占写入；当前 workspace 串行执行，Lead 负责 SpecDev 工件、审查、验证和 Evidence。
