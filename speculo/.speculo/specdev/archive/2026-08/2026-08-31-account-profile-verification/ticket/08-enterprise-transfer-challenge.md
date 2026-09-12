---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-08
title: 交付企业本人解绑与短信挑战负责人转移
status: done
planning_depth: deep
planning_depth_reason: 短信 credential、实名匹配、Redis 一次性状态和负责人原子切换属于认证安全边界。
ready: true
risk: critical
blocked_by: [T-05, T-07]
contract_ids: [AC-019, AC-020, AC-021, AC-022, AC-023]
owner: codex:/root
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/transfer/**</Path>"]
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/transfer/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/test/java/org/dromara/profile/enterprise/transfer/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/application/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/profile/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-08: 交付企业本人解绑与短信挑战负责人转移

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/08-enterprise-transfer-challenge.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>`

## 1. 战略与来源

- **目标：** 让当前企业负责人无需 workflow 安全退出或把控制权转给合格目标账户。
- **可观察产出：** 本人可即时解绑；正确三要素只向目标当前手机号发 6 位验证码；成功确认原子转移，重放/竞态拒绝。
- **来源：** `US-009`、`US-010`、`AC-019` 至 `AC-023`、`ADR-009`、`ADR-019`。
- **当前事实：** T-05 提供个人 active 资格，T-07 提供企业负责人；现有登录验证码不绑定企业上下文。
- **Planning Depth 原因：** 验证码泄露或上下文复用可转移企业控制权。

## 2. 决策状态

### 已锁定决策

- 仅当前 active 负责人可发送/确认/解绑。
- 目标精确匹配 active 个人档案姓名、证件后四位和 system 手机；匹配失败模糊响应。
- 6 位、5 分钟、60 秒限发、5 次错误、成功消费，绑定企业/双方/手机号/版本。
- 确认时重检目标正常、个人 active、无其他 active/suspended 企业绑定和双方版本。
- 管理员指定不使用本挑战；本人解绑无普通恢复。

### 已采用的低影响假设

- Redis key 使用 profile 专属命名空间，验证码由 CSPRNG 生成并只保存不可恢复校验值。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| self-unbind、target match、send/confirm challenge、原子转移 | ProfileService、UserService、RedisUtils、NotifyClient | 登录验证码、目标登录确认、workflow、管理员指定 |

## 4. 要构建什么

负责人输入三要素创建一次性挑战，系统只向权威手机号发码；确认时不能仅信任发送快照，必须锁定并重检后追加旧 unbound 和新 active。

## 5. 实现契约

- **入口或接缝：** transfer challenge send/confirm 与 responsible self-unbind POST。
- **输入与输出：** 三要素/challengeId+code -> 模糊发送结果/原子转移结果。
- **公共接口变化：** 新增企业负责人自助命令。
- **不变量：** 验证码一次性且上下文绑定；转移不覆盖旧事件、不进入 workflow。
- **状态或数据流：** precise target -> Redis challenge -> Notify -> consume+locks -> binding events。
- **错误与失败行为：** 过期、限频、五次、重放、资格/版本变化均不转移。
- **兼容要求：** 手机格式/发送能力完全沿用 system。
- **安全与隐私要求：** 日志只含 challengeId、双方 userId、企业 ID、结果；通知 REDACT_SENSITIVE。

## 6. 执行路线

1. 固定时钟建立协议、模糊响应和重放测试。
2. 实现目标资格匹配与专用 Redis challenge。
3. 接入 NotifyClient，保证发送失败无可确认挑战。
4. 实现确认重检、锁与原子事件；实现本人解绑。
5. 运行并发、Redis/短信故障、日志与权限回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** enterprise transfer 子树和测试。
- **只读上下文：** T-05/T-07、ruoyi-api、Redis/Notify。
- **共享路径：** 无。
- **保留或不动：** 登录验证码、system 手机字段和 enterprise application 文件。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | service/Redis/MySQL | 正确挑战、确认、本人解绑 | 一次消费并原子切换 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| 失败 | clock/race/fault | 错误/过期/限频/重放/资格变化 | 模糊拒绝，无部分切换 | 同上 |
| 回归 | notify/log/profile | 短信失败、日志捕获、资格查询 | 无有效挑战/泄密，状态正确 | 同上 |

- **Workspace checks：** 定向测试、固定时钟、日志扫描和 Maven 编译。
- **E2E disposition：** required：Redis、通知、MySQL 和当前用户授权需联合验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate，使用隔离 Redis/SMS adapter。
- **Integration evidence：** commit、parent/candidate/result SHA、挑战/绑定审计。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-05/T-07 后部署；短信模板/Provider 就绪后开放。
- **兼容窗口：** challenge 协议独立于登录验证码。
- **监控信号：** 匹配失败、限频、错误耗尽、转移冲突和短信失败。
- **回滚或前向恢复：** 禁用 send/confirm，等待 Redis challenge 过期；已转移事件不回写。
- **不可逆操作与批准点：** 生产短信模板/启用未授权。
- **收缩条件：** 不适用：无旧转移协议。

## 10. 验收标准

- [x] AC-019 至 AC-023 全协议/竞态矩阵通过。
- [x] required E2E、REDACT_SENSITIVE、Evidence 和提交结果完整。
- [x] 未复用登录验证码或引入企业 workflow。
