---
schema_version: 3
artifact: spec
change: 2026-08-24-notify-eli5
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:统一通知控制面直接切换实施
  - ADR-014:ruoyi-notify 新监控合同
  - ADR-015:持久化 at-least-once
---

# Spec: 统一通知控制面

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-24-notify-eli5/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-24-notify-eli5/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-24-notify-eli5/CONTEXT.md</Path>`

## 1. 问题与目标

当前邮件、短信、站内消息、公告和实时提示由多个入口分别实现，发送状态、接收人和阅读状态无法统一查询。目标是在模块化单体内建立唯一通知控制面，统一提交、编排、持久化、投递、回执、收件箱和监控。

目标渠道为 `IN_APP`、`SMS`、`MAIL`；MQTT、FCM/APNs 和媒体推流保持非目标技术通道。所有用户通知必须经 `NotificationApplicationService`，所有发送事实必须进入 `notify_*` 统一日志模型。

## 2. 解决方案与外部行为

调用链固定为 `Controller/Listener/API Adapter -> UseCase -> Service -> DAO/Port -> Provider/Gateway/Store Adapter`。同步模式返回 Provider 本次接受结果；异步模式在 Intent、Delivery 和 Outbox 持久化成功后返回 `QUEUED`。Provider I/O 不在长事务内执行。

站内通知由 notify 自有收件箱表，先持久化再通过现有 `PushHelper` 做 SSE/WebSocket 提示。公告草稿必须显式发布，发布时生成不可变快照；撤回使用独立操作。模板、路由和渠道账号均以发布版本参与发送。

## 3. 用户故事

- **US-001**：业务模块提交一次通知意图，系统按模板、偏好和路由统一投递。
- **US-002**：管理员在一个监控页面查看通知、接收人、渠道、Attempt、回执和阅读状态。
- **US-003**：用户离线后仍能从收件箱获取站内通知，并在多设备同步已读状态。
- **US-004**：供应商失败、超时或进程重启后，系统能够按策略恢复、重试或进入死信。
- **US-005**：管理员发布公告时，已发送内容不会因后续编辑而变化。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 合法应用、模板和接收人 | submit | 返回 notificationId 和聚合状态，写入 Intent/Delivery | API/UseCase 单测 |
| AC-002 | ASYNC 请求 | submit 后进程重启 | Outbox 可重新领取，任务不丢失 | MySQL 集成测试 |
| AC-003 | 任意渠道投递 | Provider 成功、失败、超时 | Attempt、Delivery、Intent 状态正确归并 | 领域策略测试 |
| AC-004 | 站内通知 | 投递并建立实时连接 | 收件箱先落库，随后提示；已读可跨设备查询 | system 集成测试 |
| AC-005 | 公告草稿 | publish/retract | 只发布不可变快照，撤回为独立事件 | Controller/Service 测试 |
| AC-006 | 供应商回调 | 重复、乱序、伪造回调 | 验签失败拒绝，合法回调幂等归并 | 回调契约测试 |
| AC-007 | 重复请求 | 相同幂等键和摘要再次提交 | 复用原通知；不同摘要返回冲突 | 幂等测试 |
| AC-008 | 管理查询 | 按应用、业务、接收人、渠道查询 | 一个页面展示完整发送链路 | API/E2E 测试 |
| AC-009 | 全仓业务代码 | 静态扫描 | 无直接 SDK、NotifyClient、PushHelper 用户通知调用 | architecture test |

## 5. 范围

### IN

`ruoyi-api` 合同、`ruoyi-notify` 模块、notify_* 数据模型、Outbox Worker、Mail/SMS/In-App Adapter、公告发布、system 收件箱投影、统一监控和 notify 前端。

### REUSE

现有 `ruoyi-common-notify`、Mail/SMS Adapter、`PushHelper`、`RedisUtils`、`JsonUtils`、`LoginHelper`、OSS 附件快照、OpenAPI 机器认证和 `@Log`。

### OUT

- **OOS-001**：MQTT、FCM/APNs、媒体推流和独立微服务部署。
- **OOS-002**：旧 `sys_notify_*`、旧 `MessageService` 和旧监控页面兼容。

## 6. 已锁定实现约束

- **DEC-001**：`ruoyi-common-notify` 是数据面，`ruoyi-notify` 是控制面。来源：`ADR-001`。
- **DEC-002**：异步采用 MySQL Outbox、租约和至少一次投递。来源：`ADR-004`、`ADR-015`。
- **DEC-003**：新 `notify_*` 是发送日志唯一权威。来源：`ADR-014`。
- **DEC-004**：模板、路由和渠道配置采用不可变发布快照。来源：`ADR-007`、`ADR-008`、`ADR-009`。
- **DEC-005**：站内信先持久化再实时提示。来源：用户锁定决策。

## 7. 数据、接口与兼容

- **公共接口变化：** 新增 `NotificationApplicationService` 及 command/receipt/query/port 合同，删除旧用户通知入口。
- **数据模型与持久化：** Notify 拥有公告、消息、收件关系、发送链路和收件箱表；初始化时迁移并删除 `sys_notice`、`sys_message` 旧表及相关菜单/API。
- **兼容要求：** 无；当前环境无需保留外部兼容。
- **迁移要求：** 保留已有公告和消息内容，写入 Notify 表后删除旧表；不保留旧 HTTP/API 兼容入口。
- **发布或运维影响：** Worker 和回执处理随 ruoyi-admin 同进程装配。

## 8. 非功能要求

- **NFR-001 安全与隐私：** Secret 加密且不回显；目标、正文、回调按审计策略脱敏；回调必须验签。
- **NFR-002 性能与容量：** 受理和 Worker 指标按 ADR-015 验收；全量公告使用批量扩展。
- **NFR-003 可用性与可靠性：** Outbox 可恢复、租约可过期、重试有上限、死信可重放。
- **NFR-004 可观测性与运营：** 统一日志可按业务、接收人、渠道、Provider、状态和 TraceId 查询。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| API records/mapper | unit | AC-001/007 | Maven module tests | test output |
| domain planner/state | unit | AC-003/007 | notify module tests | test output |
| Outbox/lease | integration | AC-002 | MySQL 8.4 admin tests | integration output |
| Provider callback | contract | AC-006 | controller tests | test output |
| Admin pages | E2E | AC-004/005/008 | pnpm test/build | UI output |

## 10. 风险、假设与未决问题

### 风险

Provider 超时可能导致未知结果；通过 Attempt 幂等键和 UNKNOWN 状态处理。大批量公告可能造成瞬时写入压力；通过固定快照和分批展开处理。

### 已采用的低影响假设

MySQL 是唯一主要业务数据源；首期同 JVM 部署；短信和邮件 Provider 继续复用现有 common Adapter。

### 未决问题

无。
