---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-08
title: 通知附件发送前快照
status: done
planning_depth: deep
planning_depth_reason: 跨 common SPI 与 system OSS 生命周期复制附件，决定 Provider 实际发送内容和失败补偿顺序。
ready: true
risk: high
blocked_by: [T-03, T-07]
contract_ids: [AC-023, AC-024, AC-025, AC-026]
owner: cursor-agent
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/config/MailConfig.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/notify/MailNotificationMessage.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/notify/MailNotifyChannelAdapter.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/attachment/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/config/MailConfig.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/notify/MailNotificationMessage.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/notify/MailNotifyChannelAdapter.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/attachment/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/attachment/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-08: 通知附件发送前快照

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/08-notify-attachment-snapshots.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-08.md</Path>`

## 1. 战略与来源

- **目标：** 保证 Provider 实际使用的附件与通知历史保留的附件是同一组独立快照。
- **可观察产出：** 每个源附件每次逻辑通知只复制一份；所有 Delivery 共用；复制失败时 Provider 零调用。
- **来源：** `ADR-005/009`、`AC-023/024/025/026`。
- **当前事实：** Mail 可直接读附件，但源对象变化/删除会破坏历史一致性；common 不能依赖 system。
- **Planning Depth 原因：** 跨模块 SPI、OSS Copy、预生成 notifyLogId、生命周期引用和补偿顺序均为高风险。

## 2. 决策状态

### 已锁定决策

- 请求只传 attachmentOssIds；common 通过资源/快照 SPI，不暴露 File/InputStream/system entity。
- Dispatcher 在 Provider 前预生成 notifyLogId 并完成全部复制；发送使用快照对象。
- 快照引用 `ref_type=sys_notify_log`、`ref_id=真实 notify_log_id`。
- 部分复制失败清除已复制副本或留 TEMP 清理；Provider 失败保留快照；Listener 失败时未绑定快照 24 小时后清理。

### 已采用的低影响假设

- 同一请求重复列出相同 ossId 时规范化去重并保持首次顺序。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| attachment SPI、snapshot orchestration、system adapter、复制/补偿、Adapter 使用快照 | T-01 Copy、T-03 lifecycle/ref、T-07 Dispatcher | 日志 Mapper/UI、异步持久化、源业务授权推导、附件病毒扫描 |

## 4. 要构建什么

发送带附件请求时，Dispatcher 先验证并为该通知复制所有源对象；任一失败立即终止且不调用 Provider。复制全部成功后，将快照资源交给 Mail Adapter，所有目标共用同一组。事件携带 snapshot ossIds；随后 T-09 成功落库时绑定引用，监听失败则让 TEMP 清理回收。

## 5. 实现契约

- **入口或接缝：** NotifyAttachmentResource/Snapshot SPI、system adapter、Dispatcher/Adapter。
- **输入与输出：** source ossIds + notifyLogId -> snapshot descriptors/ossIds；Provider 只接收 snapshot resources。
- **公共接口变化：** additive attachment SPI、typed snapshot exception 和 Event snapshot 字段。
- **不变量：** Provider 调用前原子语义是“全部快照成功”；一个逻辑通知一组快照；ref_type 为真实 `sys_notify_log` 表名。
- **状态或数据流：** validate -> copy all -> send -> event -> T-09 bind；failure -> cleanup/TEMP。
- **错误与失败行为：** 源缺失、无权、copy/head 失败均使 Provider 零调用；清理幂等。
- **兼容要求：** 无附件请求行为与 T-07 相同。
- **安全与隐私要求：** Event/日志只保存 ossId，不保存临时 URL或 credential；调用者须先有权使用源附件。

## 6. 执行路线

1. 建立全部成功、部分复制失败、Provider 失败和 Listener 未绑定测试。
2. 扩展 common attachment SPI、事件和 Dispatcher 顺序。
3. 用 system OssService 实现 Copy/HEAD/cleanup adapter。
4. 让 Mail Adapter 仅消费快照资源，验证多目标共享。
5. 运行 OSS+Provider+lifecycle 集成并记录孤儿回收路径。

## 7. 路径访问契约

- **预计修改点：** common-notify、Mail Adapter 精确文件与 system `notify/attachment/**`。
- **可写范围：** 仅 frontmatter 两个路径。
- **只读上下文：** T-03 OssService、T-01 common-oss。
- **共享路径：** 无；common-notify 由 T-07 完成后顺序接管，system notify 由 T-09 后续接管。
- **保留或不动：** SQL、monitor API/UI、源业务表。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | OSS Copy+Provider 集成 | 多附件、多目标发送 | 每源一副本，发送使用副本 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-08.md</Path>` |
| 失败路径 | Copy/Listener 注入 | 第 N 个复制失败、Provider 失败、未绑定 | Provider 零调用或快照按规则保留/回收 | 同上 |
| 回归 | Maven tests | 无附件 Mail/SMS | 行为不变 | 同上 |

- **Workspace checks：** current 在 `current-workspace`；required 在 source-worktree 跑非 E2E，Lead 在 parent-candidate 集成。
- **E2E disposition：** not-required：用户明确不执行 E2E；以 ruoyi-admin 的 OSS Copy/Provider/lifecycle 集成测试覆盖。
- **E2E owner/environment：** 不适用；Lead 在 current-workspace 核对成功、部分复制和监听失败清理证据。
- **Integration evidence：** common/system source commits、parent before、candidate/result SHA 和对象清单 Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01/T-03/T-07 -> attachment SPI/adapter -> T-09 listener 绑定 -> T-10 调用迁移。
- **兼容窗口：** 无附件路径兼容；带附件新调用必须等 T-09 合并后启用。
- **监控信号：** copy 数量/耗时/失败、未绑定 snapshot TEMP、补偿失败。
- **回滚或前向恢复：** 停止带附件 Notify 调用；遗留快照由 TEMP 清理，不能删除已绑定历史附件。
- **不可逆操作与批准点：** 首次启用 Provider 前确认快照实际读取 Evidence。
- **收缩条件：** T-09 证明事件落库与引用绑定一致后允许生产带附件调用。

## 10. 验收标准

- [x] `AC-023/024/025/026` 的快照顺序和补偿通过。
- [x] ref_type 固定真实表名 `sys_notify_log`，Provider 只使用快照。
- [x] common 未依赖 system 实现类型。
- [x] ruoyi-admin 集成测试、提交 SHA 与 Evidence 完整。
