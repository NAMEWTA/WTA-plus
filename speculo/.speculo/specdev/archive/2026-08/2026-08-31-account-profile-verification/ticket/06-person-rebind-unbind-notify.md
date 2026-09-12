---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-06
title: 交付个人隐私探测、审核后换绑、本人解绑与通知
status: done
planning_depth: deep
planning_depth_reason: 不控制旧手机号的个人换绑涉及账户接管、隐私枚举、版本竞态和事务后通知。
ready: true
risk: critical
blocked_by: [T-05]
contract_ids: [AC-009, AC-010, AC-011, AC-012, AC-013, AC-014, AC-016, AC-017]
owner: codex:/root
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/rebind/**</Path>"]
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/rebind/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/notification/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/test/java/org/dromara/profile/person/rebind/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/application/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-06: 交付个人隐私探测、审核后换绑、本人解绑与通知

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/06-person-rebind-unbind-notify.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>`

## 1. 战略与来源

- **目标：** 允许合法本人通过完整重认证安全换绑，同时阻止资料/手机号枚举和提前剥夺旧绑定。
- **可观察产出：** 普通探测只见状态；完整精确匹配才见手机号掩码；审核通过且版本未变才原子换绑；旧账户收到安全通知；本人可解绑。
- **来源：** `US-006` 至 `US-008`、`US-018`、`AC-009` 至 `AC-017`、`ADR-026`、`ADR-027`。
- **当前事实：** T-05 已有个人发布状态机，但没有 rebindIntent、隐私提示或通知。
- **Planning Depth 原因：** 该切片是账号关系接管防线，错误会泄露 PII 或解除合法旧绑定。

## 2. 决策状态

### 已锁定决策

- 普通个人/企业探测都只返回最小状态；个人完整字段精确匹配是唯一手机号掩码例外。
- 确认只冻结 rebindIntent/profileId/旧 bindingVersion，不立即换绑、不验证旧手机号。
- finish/管理员批准时锁定重检并追加旧 unbound+新 active；任何失败/竞态不切换。
- 本人解绑即时失效、不能恢复；换绑通知 after-commit best-effort、可重试且不泄密。

### 已采用的低影响假设

- 手机掩码保留现有号码前三后四；短号码按不披露策略返回不可识别掩码。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 探测、完整匹配、意图冻结、发布围栏、本人解绑、旧账户通知 | T-05 发布、UserService、NotifyClient、MessageService | 企业顶替、旧手机 OTP、档案注销 |

## 4. 要构建什么

登录申请人无法从证件号接口读取旧档案；只有提交完整且精确的身份字段后可确认掩码。待审保持旧绑定，最终决定在 bindingVersion 围栏内切换。通知失败只进入重试审计。

## 5. 实现契约

- **入口或接缝：** binding probe、rebind confirm/submit、self-unbind、finish hook、after-commit notification。
- **输入与输出：** 完整身份/确认 -> 最小状态或掩码/冻结意图；解绑 -> 当前资格失效。
- **公共接口变化：** 新增个人探测/解绑与换绑确认命令。
- **不变量：** 旧绑定至通过前有效；版本冲突零切换；通知不影响事务。
- **状态或数据流：** precise match -> snapshot intent -> review -> locked atomic switch -> after-commit notify。
- **错误与失败行为：** 字段错、竞态、拒绝/撤销/终止统一不换绑且不泄露匹配字段。
- **兼容要求：** T-05 普通认证与企业负责人协议不变。
- **安全与隐私要求：** 无完整手机号/userId/昵称；通知/日志无身份与新账户信息。

## 6. 执行路线

1. 建立枚举、掩码、待审与竞态攻击矩阵。
2. 实现最小探测与精确匹配确认。
3. 扩展快照意图和发布版本围栏，不改 T-05 共享文件时通过独立 hook/port 接入。
4. 实现本人解绑和 after-commit 双渠道安全通知/重试。
5. 运行事务、日志、通知故障和权限回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** person rebind/notification 与测试。
- **只读上下文：** T-05、system API 与 common notify。
- **共享路径：** 无。
- **保留或不动：** T-05 文件、企业、system 用户与通知公共模块。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | MVC/service/MySQL | 精确匹配、finish 换绑、本人解绑 | 掩码与原子事件正确 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| 失败 | race/privacy/fault | 错字段、拒绝、版本变化、通知失败 | 无泄露/切换；通知可重试 | 同上 |
| 回归 | ProfileService/log scan | 待审/解绑/成功后查询 | 资格正确且无敏感日志 | 同上 |

- **Workspace checks：** 定向测试、并发/日志扫描、Maven 编译。
- **E2E disposition：** required：HTTP+workflow finish+MySQL 原子切换+通知故障必须联合验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** commit、parent/candidate/result SHA、绑定事件与通知结果。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-05 后部署，默认仅授权明确 Client。
- **兼容窗口：** rebind snapshot 字段 additive；旧无意图快照按普通认证处理。
- **监控信号：** 精确匹配失败、版本冲突、换绑量、通知重试。
- **回滚或前向恢复：** 禁用换绑入口；已成功事件不回写，前向纠错。
- **不可逆操作与批准点：** 生产启用未授权；有效换绑属于业务不可逆事件。
- **收缩条件：** 不适用：无旧换绑协议。

## 10. 验收标准

- [x] AC-009 至 AC-017 的隐私、事务和通知矩阵通过。
- [x] required E2E、Evidence、提交和父分支结果完整。
- [x] 无企业换绑、旧手机 OTP 或敏感日志偏差。
