---
schema_version: 3
artifact: ticket
change: 2026-08-20-namewta-client-rbac-review
id: T-03
title: 修复前端用户角色与认证上下文消费
status: done
planning_depth: deep
planning_depth_reason: 同步破坏性 HTTP 合同并修改复杂用户表单和认证入口。
ready: true
risk: high
blocked_by: [T-02]
contract_ids: [AC-002, AC-003]
owner: codex-root
expected_changes: ["<Path>plus-ui-namewta/src/api/**</Path>", "<Path>plus-ui-namewta/src/views/system/user/**</Path>", "<Path>plus-ui-namewta/src/views/login.vue</Path>", "<Path>plus-ui-namewta/src/views/register.vue</Path>"]
writable_paths: ["<Path>plus-ui-namewta/src/api/**</Path>", "<Path>plus-ui-namewta/src/views/system/user/**</Path>", "<Path>plus-ui-namewta/src/views/login.vue</Path>", "<Path>plus-ui-namewta/src/views/register.vue</Path>"]
read_only_paths: ["<Path>docs/upstream/customization-map.md</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 修复前端用户角色与认证上下文消费

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/ticket/03-fix-frontend-contracts.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 消费严格 scoped 后端合同并移除 Boolean/字段 fallback。
- **可观察产出：** 用户角色按 Client 分组显示；认证页只在严格 Boolean 上下文下可用。
- **来源：** `AC-002`、`AC-003`、`CR-001`。
- **当前事实：** 用户页声明后端不存在的字段，认证 API 接受数字/字符串并默认 Client 启用。
- **Planning Depth 原因：** 复杂表单与认证入口同步变更。

## 2. 决策状态

### 已锁定决策

- 不新增聚合端点；逐 Client 顺序加载并缓存角色上下文。
- 前端注册字段保持不变；认证响应执行 unknown narrowing。

### 已采用的低影响假设

- 当前 Client 数量有限；顺序加载优先保证正确性和可控并发。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| user API/types/pages、login/register | request adapter、现有 hooks 和 Client API | 新 store、测试框架、UI 重设计 |

## 4. 要构建什么

用户编辑先取得基础信息，再按所选登录域逐 Client 加载显式/默认角色；过期响应不覆盖新表单。登录/注册页面只有在严格合法上下文时开放操作。

## 5. 实现契约

- **入口或接缝：** user API/types、用户编辑页、authRole 页、login API 和认证页。
- **输入与输出：** Long Client PK；真实 `roles/roleIds`；严格 Boolean ClientAuthContext。
- **公共接口变化：** getAuthRole clientId 必填；删除虚假 response fields。
- **不变量：** Client 必须属于表单所选登录域；默认角色只读；畸形认证响应失败关闭。
- **状态或数据流：** 基础详情 -> scoped context cache -> 合并显式角色 -> 提交完整快照。
- **错误与失败行为：** 任一必要上下文加载失败时不部分提交；旧请求结果被丢弃。
- **兼容要求：** 与 T-02 后端合同同步。
- **安全与隐私要求：** UI 过滤不替代服务端验证。

## 6. 执行路线

1. 修正 API 类型和必填 Client 参数。
2. 改造用户表单 scoped 加载、缓存、摘要和失效控制。
3. 改造 authRole 初始加载。
4. 增加严格认证上下文 parser 与页面 fail-closed 状态。
5. 运行 lint、补充 typecheck 诊断和生产构建。

## 7. 路径访问契约

- **预计修改点：** frontmatter 所列前端 API 与页面。
- **可写范围：** 仅列出的前端路径。
- **只读上下文：** customization map。
- **共享路径：** 无。
- **保留或不动：** request adapter、store、lockfile、生成声明。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 多 Client 角色 | 用户页交互 | 切换登录域/Client | 分组准确且不串域 | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-03.md</Path>` |
| 畸形认证上下文 | API/page | 缺字段、数字、停用 | 登录不可用、注册不开放 | 同上 |
| 过期响应 | dialog lifecycle | 快速切换/关闭 | 旧结果不覆盖 | 同上 |
| 回归 | frontend gates | lint/type diagnostic/build | active gates exit 0 | 同上 |

- **Workspace checks：** current workspace，lint、build、supplemental vue-tsc、diff-check。
- **E2E disposition：** required：跨前后端角色与认证 UI；T-04 统一执行。
- **E2E owner/environment：** Lead/current-workspace/browser + backend。
- **Integration evidence：** implementation commits、parent before/result SHA。

## 9. 发布、迁移与恢复

- **迁移顺序：** 后端合同提交后开发；部署时前端先于严格后端。
- **兼容窗口：** 新前端兼容整改前后端当前 scoped 参数能力。
- **监控信号：** 认证页不可用提示和 API 错误。
- **回滚或前向恢复：** revert 前端聚焦提交。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 虚假字段和 Boolean coercion 搜索结果为零。

## 10. 验收标准

- [x] `AC-002`、`AC-003` 的前端合同满足。
- [x] 验证矩阵写入 Evidence。
- [x] 修改不越界且 lockfile 不变。
- [x] 形成聚焦 implementation commits，direct-parent 验证通过。
- [x] E2E 明确委托 T-04 完成并回填。
- [x] 无未批准偏差。
- [x] Ticket、Map、Evidence 一致。
