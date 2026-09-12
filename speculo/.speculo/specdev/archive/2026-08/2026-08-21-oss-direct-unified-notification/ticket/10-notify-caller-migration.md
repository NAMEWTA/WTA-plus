---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-10
title: 现有通知调用收口
status: done
planning_depth: standard
planning_depth_reason: 跨 admin、workflow、demo 迁移已知调用方并保留各业务失败语义，但不再改变公共契约。
ready: true
risk: medium
blocked_by: [T-09]
contract_ids: [AC-015, AC-016, AC-017, AC-023, AC-024, AC-025, AC-031]
owner: cursor-agent
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/CaptchaController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/config/*NotifyContext*.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwCommonServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/controller/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/CaptchaController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/config/*NotifyContext*.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/caller/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/context/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwCommonServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/controller/MailSendController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/controller/SmsController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/pom.xml</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-10: 现有通知调用收口

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/10-notify-caller-migration.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-10.md</Path>`

## 1. 战略与来源

- **目标：** 将仓库内已知 Mail/SMS 业务调用迁移到 NotifyClient，证明公共能力可在真实业务入口工作。
- **可观察产出：** Captcha、Workflow、Demo 通过统一入口发送，Adapter 外不再直接调用 MailBuilder/SmsFactory/SDK。
- **来源：** `ADR-006/007/008/009`、`AC-015/016/017/023/024/025/031`。
- **当前事实：** 三个模块直接依赖具体邮件/短信能力，失败处理和监控不统一。
- **Planning Depth 原因：** 多模块行为迁移，但公共合同和 schema 已由前置 Ticket 冻结。

## 2. 决策状态

### 已锁定决策

- 调用方负责将用户/业务数据解析成 PHONE/EMAIL，不向 common 传 USER。
- 业务根据场景选择是否提供 idempotencyKey；Client 不要求显式 scope，也不参与路由。
- 各入口显式决定如何传播 `NotifyDeliveryException`，不得丢失部分成功结果。
- 模板短信提供 providerTemplateCode、params 和完整 contentSnapshot。

### 已采用的低影响假设

- 验证码使用业务可稳定构造的幂等 Key，Workflow/Demo 仅在确有重复业务语义时启用。
- Demo 邮件附件路由保留，原硬编码本地占位路径改为请求 `ossId/ossIds`，由通知快照发送。
- SMS 黑名单增删是 Provider 管理操作，不属于通知发送；继续使用 common-sms 原子能力，不计入发送直调扫描。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Captcha、Workflow、Demo 调用与最小 POM 依赖、调用扫描 | T-09 完整 Notify 栈、ruoyi-api 用户解析能力 | 未知仓库外调用、站内信、自动重试、新业务功能 |

## 4. 要构建什么

每个现有入口在业务层解析目标、构造一个 Channel 的请求并同步调用 NotifyClient，保持原有对外成功/失败合同，同时让 Event 产生统一监控。验证码/模板内容具备完整快照；后台或 Workflow 无 Token Client 时可发送并记录 null client_pk。最终静态扫描仅允许 Adapter 内直接使用原子 Builder/SDK。

## 5. 实现契约

- **入口或接缝：** CaptchaController、FlwCommonServiceImpl、Demo Mail/Sms Controller。
- **输入与输出：** 现有业务请求/响应保持，内部改为 NotifyRequest/Result/typed exception。
- **公共接口变化：** 无；只迁移内部依赖。
- **不变量：** 物理目标由调用方解析；模板有 contentSnapshot；失败不伪装成功。
- **状态或数据流：** business input -> resolve target -> NotifyClient -> result/exception -> existing response。
- **错误与失败行为：** Provider/partial/idempotency/validation 按各入口既有合同映射并保留可观测原因。
- **兼容要求：** 外部 endpoint 与成功语义保持；附件 Demo 以 ossId 替代无效本地占位路径；通知发送的直接 Builder/SDK 调用收缩为 Adapter 内。
- **安全与隐私要求：** 不在业务日志新增 OTP/Token 输出；监控明文由 T-09 承担。

## 6. 执行路线

1. 为三个入口记录当前响应/失败行为并建立回归测试。
2. 补最小 common-notify Maven 依赖，移除不再需要的具体依赖。
3. 迁移 Captcha、Workflow、Demo 的目标解析、模板快照和异常映射。
4. 验证后台无 Client 发送与监控来源字段。
5. 扫描 Adapter 外 MailBuilder/SmsFactory/SDK 直接调用并运行 reactor 测试。

## 7. 路径访问契约

- **预计修改点：** 三个模块的精确调用文件和 POM。
- **可写范围：** frontmatter 所列；公共 notify/system 只读。
- **只读上下文：** T-06 至 T-09 契约。
- **共享路径：** 无。
- **保留或不动：** 公共模块、SQL、前端和仓库外消费者。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 三入口集成 | 验证码、Workflow 邮件、Demo Mail/SMS | 统一发送并写监控 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-10.md</Path>` |
| 失败路径 | Provider/partial/无 Client | 注入失败与后台 Job | 业务合同稳定，null client_pk 可用 | 同上 |
| 回归 | Maven + rg | 受影响模块测试；扫描直接调用 | 仅 Adapter 内保留直接 SDK | 同上 |

- **Workspace checks：** current 在 `current-workspace`；required 在 source-worktree 跑非 E2E，Lead 在候选父分支做业务集成。
- **E2E disposition：** not-required：用户明确不执行 E2E；代表性 Captcha、Workflow、Demo 路径由 ruoyi-admin 聚合集成测试验证。
- **E2E owner/environment：** 不适用；Lead 在 current-workspace 核对调用扫描和集成结果。
- **Integration evidence：** 各模块 source commit、parent before/result SHA、调用扫描和 Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-09 完整栈可用后按三个入口迁移并在同一候选合并。
- **兼容窗口：** 原子 Builder API 保留供 Adapter；业务直接调用在本 Ticket 收缩。
- **监控信号：** 三入口成功/失败、validation/idempotency/provider 分类和 monitoring 行。
- **回滚或前向恢复：** 可按入口回退调用实现，但不得破坏公共 module；优先前向修复异常映射。
- **不可逆操作与批准点：** 无。
- **收缩条件：** `rg` 与编译证明 Adapter 外已知通知发送直调为零；Demo SMS 黑名单 Provider 管理调用明确排除。

## 10. 验收标准

- [x] `AC-015/016/017/023/024/025/031` 在代表性入口验证。
- [x] Captcha/Workflow/Demo 对外合同保持，Adapter 外直接调用为零。
- [x] 无 Client 后台路径正常且不伪造 SYSTEM/client scope。
- [x] ruoyi-admin 聚合集成测试、提交 SHA 与 Evidence 完整。
