---
schema_version: 3
artifact: spec
change: 2026-08-20-namewta-client-rbac-review
status: ready
ready_for_tickets: true
sources:
  - REVIEW:CR-001
  - USER-DECISION:approved-remediation-plan
  - USER-DECISION:optional-phone-email-backend-only
  - USER-DECISION:runtime-e2e-not-required
---

# Spec: Client RBAC Review 整改

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/spec.md</Path>`
- **当前 ADR：** 不适用：关键决定已由用户批准的整改计划锁定。
- **当前领域上下文：** `<Path>plan/update.md</Path>` 与 `<Path>docs/upstream/customization-map.md</Path>`。

## 1. 问题与目标

### 问题陈述

当前实现仍有无 Client 角色查询、无效 Client/登录域授权 fail-open、前后端角色响应不一致、认证上下文兼容兜底、注册唯一性缺口、会话接口超范围和父仓库快照不可复现问题。

### 目标用户与场景

系统管理员按 Client 管理用户角色；终端用户只在所属登录域和 Client 内登录、读取菜单和权限；发布人员可以从父仓库复现通过验收的前后端组合。

### 成功标准

所有角色读取与写入显式携带 Long Client PK；所有无效配置失败关闭；前后端合同一致；认证上下文只接受 Boolean；注册可选手机/邮箱在提供时校验；会话接口收缩；双端静态检查、测试、构建与父仓库快照证据完整。用户明确决定本 change 不要求 disposable SQL、HTTP、Token 或浏览器 runtime E2E。

### 非目标

不新增依赖、测试框架、数据库列、Feature Flag、兼容层或生产部署；不重写已发布历史。

## 2. 解决方案与外部行为

### 解决方案摘要

后端移除全局角色 fallback 并集中校验角色、Client、登录域和默认角色不变量；用户详情按 Client 返回角色上下文。前端逐 Client 加载并组合角色摘要。认证与注册边界严格校验，用户全域会话通过既有登录域关系逐域清理。

### 主要流程

管理员加载用户基础资料后，只对该用户已拥有登录域对应的启用 Client 请求角色上下文；提交时后端在同事务内更新关系并验证完整显式角色快照。登录/注册页必须先取得合法 Client Boolean 上下文。

### 边界、失败与稳定错误行为

缺 Client、Client 停用、Client 无登录域、登录域停用、用户无域、角色缺失/停用/跨 Client、默认角色无效或响应 Boolean 畸形均显式失败，不返回全局数据或默认启用。

### 状态转换与不变量

有效角色等于当前 Client 默认角色加当前用户在该 Client 的显式角色；默认角色不写 `sys_user_role`；Token 的 `userType` 与 `clientPk` 只来自登录快照；用户停用/删除清理其全部登录域 Token。

## 3. 用户故事

- **US-001**：作为管理员，我希望按 Client 查看和分配用户角色，以便不会跨应用泄漏授权。
- **US-002**：作为终端用户，我希望 Client 或登录域异常时请求被拒绝，以便身份和权限不会模糊降级。
- **US-003**：作为发布人员，我希望父仓库固定通过验证的前后端提交，以便可重复构建和回滚。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 任一角色读取或分配 | 缺 Client 或使用无效/跨 Client 关系 | 明确拒绝且不读写其他 Client 数据 | controller/service/mapper 负向检查 |
| AC-002 | 用户具有多个登录域 | 打开用户编辑并切换 Client | 分组展示登录域、显式角色和默认角色，提交保持 Client 隔离 | 用户详情接口与 Vue 表单 |
| AC-003 | Token 或 Client 上下文缺失/畸形 | 登录、权限读取或打开认证页 | 无 legacy fallback；登录不可用且注册隐藏/跳转 | LoginHelper、认证上下文 parser |
| AC-004 | 注册请求可选携带手机/邮箱 | 值重复或合法唯一 | 重复拒绝并提示登录；未携带仍可注册 | RegisterBody 与注册事务 |
| AC-005 | 用户全局停用或删除 | 执行管理操作 | 该用户所有登录域 Token 失效且公开会话接口仅三个 | 用户服务与 Token 状态 |
| AC-006 | 代码候选完成 | 执行双端静态检查、测试、构建并核对父仓库快照 | 结果可复现且父仓库固定最终 child SHA | Maven、pnpm、git tree |

## 5. 范围

### IN

后端 RBAC/用户/认证/注册/会话代码，前端用户与认证页/API 类型，长期合同文档，后端 upstream 同步和父仓库 gitlink。

### REUSE

现有 BO/VO/entity、MyBatis Plus/MPJ、Sa-Token、事务、前端 request adapter、现有 Client/角色 API 和构建工具链。

### OUT

- **OOS-001**：不新增测试源码；使用现有静态检查、测试和构建命令，runtime E2E 不作为本 change 的完成门。
- **OOS-002**：不推送、部署或操作生产数据库。
- **OOS-003**：不修复既有 `logininfo/loginInfo` 大小写 typecheck 基线问题。

## 6. 已锁定实现约束

- **DEC-001**：注册后端接受可选手机/邮箱并在提供时校验，当前前端不采集。来源：用户明确决定。
- **DEC-002**：已发布历史不重写；以新增聚焦提交整改。来源：Git 远端事实与用户批准计划。
- **DEC-003**：current workspace 严格串行，Lead 为唯一 Speculo 与集成 owner。来源：用户批准计划。
- **DEC-004**：disposable SQL、HTTP、Token 和浏览器 runtime E2E 对本 change 为 `not-required`；不得把未执行的 E2E 写成通过。来源：用户于 2026-08-21 明确决定“E2E 不需要管”。

## 7. 数据、接口与兼容

- **公共接口变化：** `authRole`、角色列表/导出要求 Long `clientId`；用户详情只有显式 Client 上下文才返回角色；RegisterBody 新增可选手机/邮箱。
- **数据模型与持久化：** 不改 schema，复用现有用户字段。
- **兼容要求：** 后端合同先落地、前端同步适配；不保留无 Client fallback。
- **迁移要求：** 无结构迁移；本 change 不修改 SQL，runtime SQL 初始化不作为完成门。
- **发布或运维影响：** 前端先发布、后端随后发布；父仓库最后固定组合。

## 8. 非功能要求

- **NFR-001 安全与隐私：** Client 与登录域失败关闭，响应不泄露 secret，手机/邮箱不写日志。
- **NFR-002 性能与容量：** 前端顺序加载有限 Client 角色上下文，不发起无界并发。
- **NFR-003 可用性与可靠性：** 过期对话框请求不得覆盖新用户表单；事务内关系保持一致。
- **NFR-004 可观测性与运营：** 使用稳定业务异常与构建/人工 Evidence，不新增监控依赖。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 后端编译/现有测试 | reactor | AC-001, AC-003-005 | `./mvnw clean package`; opt-in test | 命令与退出码 |
| 前端 lint/build/type 诊断 | 静态/构建 | AC-002-003 | `pnpm lint`; `pnpm build:prod`; supplemental vue-tsc | 命令与退出码 |
| 父仓库快照 | Git | AC-006 | child HEAD 与 gitlink、commit ancestry | SHA 与 tree 核对 |
| Runtime E2E | 不要求 | AC-001-006 | 用户明确豁免 | 记录 `not-required` 及理由，不声称通过 |

## 10. 风险、假设与未决问题

### 风险

严格 Client 参数会使旧前端调用失败；协调发布和父仓库固定组合避免兼容窗口。Runtime E2E 未执行，残余运行时风险由用户接受，不得将其表述为已验证。

### 已采用的低影响假设

本地提交和父仓库指针更新由用户的 “Implement the plan” 授权；push、部署和真实迁移未授权。

### 未决问题

无。
