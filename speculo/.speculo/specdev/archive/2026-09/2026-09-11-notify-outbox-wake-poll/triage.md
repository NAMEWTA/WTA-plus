---
schema_version: 1
artifact: triage
change: 2026-09-11-notify-outbox-wake-poll
mode: reconcile
source: <Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/source.md</Path>
classification: feature
risk: medium
route: specdev/archive-and-consolidate
ready_for_implementation: true
external_action: not-applicable
updated_at: 2026-09-11T23:41:05+08:00
---

# Triage: 通知 Outbox 写入唤醒与慢速兜底轮询

## 当前判定

- **影响：** `ruoyi-notify` Outbox Worker 调度（写入后跨进程唤醒 + 默认 60s 兜底）；不改 common-notify / 通知配置页。
- **紧急度：** scheduled
- **当前证据：** 来源为对话（`source_type: conversation`）；无远程 Issue。T-01/T-02/T-03 均为 done；Evidence 已 Lead accept；CR-001 approved；CTO 当面确认完成并批准归档。
- **相关代码/工件：** Tickets Map、Goal Plan、Evidence T-01–T-03、reviews/CR-001、submodule commits `2e05d989…` / `5936be32…` / `0badb07e…` / `8a8316b…`

## 未知项

- **可发现事实：** 无
- **需要用户决定：** 无
- **低影响实现细节：** AC-002 接缝为双 Redis 客户端而非双 OS 进程；AC-004 为 JVM CAS 非 live SKIP LOCKED（已记 Evidence / CR-001，不阻塞归档）

## 路由

- **下一 Work：** `<Path>{roots.workflows}/specdev/A-archive-and-consolidate/A-archive-and-consolidate.md</Path>`
- **理由：** 本地完成门已满足；来源是对话，没有可关闭的远程 Issue / 远程动作。

## 外部动作

- **远程目标：** 无
- **关闭能力：** not-applicable
- **当前状态：** not-applicable
- **授权记录：** 无
- **尝试与结果：** 无

外部动作只投影最终完成，不替代本地状态、Ticket、Map 或 Evidence。本 change 无远程关闭动作，故 `external_action: not-applicable`。
