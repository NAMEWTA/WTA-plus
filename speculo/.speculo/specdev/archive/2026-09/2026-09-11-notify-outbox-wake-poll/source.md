---
schema_version: 1
artifact: source
change: 2026-09-11-notify-outbox-wake-poll
source_type: conversation
canonical_locator: null
captured_at: 2026-09-11T15:09:05+08:00
content_sha256: 8b000886c61c2c853d344153b46d9baf99456a43d9c99c01332af4b511d94473
remote_state: not-applicable
close_capability: not-applicable
---

# Source: 通知 Outbox 唤醒与兜底轮询

## Capture Metadata

- **Capture method:** conversation
- **Author:** user
- **Created / updated:** 2026-09-11T15:09:05+08:00
- **Labels or classification supplied by source:** 激活 `specdev/G-grill-with-docs`；要求先深度调研代码仓再交流形成 ADR / LOG / CONTEXT
- **Attachments:** 用户引用 RVP·访谈；相关类 `NotifyOutboxWorker`
- **Redactions:** none

## Original Content

本地开发观察：ruoyi-vue-plus-namewta 的 NotifyOutboxWorker 以 @Scheduled(fixedDelayString = "${notify.outbox.poll-delay-ms:1000}") 每秒空转 claim Outbox。用户认为这样循环询问不合理。

倾向演进（非取消 Outbox）：写入唤醒 + 慢速兜底扫描（有任务被叫醒，没任务几乎睡；轮询只做兜底）。可选路径含应用内事件 / Redis pub-sub / 自适应退避；多实例高峰再考虑 Outbox+队列 Relay。

请激活 G-grill-with-docs：先对代码仓库做深度调研（NotifyOutboxWorker、claim/lease、现有 ADR-0006/0007 与通知投递路径），并结合外部成熟模式，再访谈形成本 change 的 ADR / LOG / CONTEXT / design-tree。范围小：只讨论 Outbox 唤醒与轮询策略，不扩到通知配置页或渠道账号管理。

## Source Comments

- 用户明确要求开独立小 change，并激活 RVP·访谈做深度调研。
- 同会话另有活跃 change `2026-09-10-notify-channel-config`（通知配置运维面），主题不同；本 change 不接管、不改其状态。
- Lead 建议候选方向：提交后唤醒 + 可配置慢速兜底；本地可先调大 poll-delay，但不替代正式设计访谈。
