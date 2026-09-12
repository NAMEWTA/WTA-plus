---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-10
title: 建立前端 system domain 的 OpenAPI 类型与请求合同
status: done
planning_depth: deep
planning_depth_reason: self/admin 两种 scope 共用一套安全合同，一次性 secret 和运行时响应收窄必须在 domain 层先固化。
ready: true
risk: high
blocked_by: [T-04, T-06]
contract_ids: [AC-009, AC-020, AC-021, AC-022, AC-024, AC-026, AC-027]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/packages/domains/system/src/open-api/**</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/index.ts</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/domains/system/src/open-api/**</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/package.json</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/README.md</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/platform/src/http/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/**</Path>"
shared_paths:
  - "<Path>plus-ui-namewta/packages/domains/system/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/package.json</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/README.md</Path>"
shared_path_owners:
  - "<Path>plus-ui-namewta/packages/domains/system/src/index.ts</Path> => T-10"
  - "<Path>plus-ui-namewta/packages/domains/system/package.json</Path> => T-10"
  - "<Path>plus-ui-namewta/packages/domains/system/README.md</Path> => T-10"
---

# Ticket T-10: 建立前端 system domain 的 OpenAPI 类型与请求合同

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/10-frontend-openapi-domain.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>`

## 1. 战略与来源

- **目标：** 在 system domain 固化 catalog、self credential 与 admin credential 的类型、运行时校验和请求函数。
- **可观察产出：** web-domain 不拼 URL、不猜响应；一次性 secret 只存在于命令结果对象，不进入持久状态模型。
- **来源：** `AC-009`、`AC-020` 至 `AC-022`、`AC-024`、`AC-026`、`AC-027`。
- **当前事实：** 前端遵循 App -> web-domain -> domain，platform 提供统一 HTTP 适配；system domain 是后端 system 合同唯一归属。
- **Planning Depth 原因：** current/target scope 和一次性敏感响应若建模错误，会把后端安全边界泄漏到 UI。

## 2. 决策状态

### 已锁定决策

- GET 只使用 query，所有生命周期 mutation 使用 POST，与后端 Spec 路径逐字一致。
- self 请求不接受 userId；admin 请求显式接受 target userId。
- 所有响应在 domain 边界做 runtime narrowing，不直接断言 unknown 为业务类型。
- secret 仅存在 create/reset 成功结果类型，不进入摘要、store、cache 或列表 model。

### 已采用的低影响假设

- 复用 system domain 现有 request/result/error 命名和测试风格，不新增跨域通用抽象。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| catalog、self/admin credential types、narrowing、query/command functions 与 exports | platform HTTP、system domain 约定 | Vue UI/菜单 manifest（T-11）、后端实现、浏览器持久化 |

## 4. 要构建什么

新增 `open-api` domain slice，完整表达 catalog 分组/元数据、credential 安全摘要、状态与 create/reset 一次性 secret。暴露 current-user 和 target-user 两组明确函数，并在 package root 稳定导出。

## 5. 实现契约

- **入口或接缝：** system domain public exports 与 platform HTTP client。
- **输入与输出：** scope-specific query/command -> narrowed catalog/summary/one-time result。
- **公共接口变化：** `@namewta/domain-system` 新增 OpenAPI contracts/functions。
- **不变量：** self 无 userId；admin 有 target；摘要永无 secret；未知状态/字段触发可控解析错误。
- **状态或数据流：** web-domain command -> domain HTTP -> runtime narrow -> immutable result。
- **错误与失败行为：** disabled/forbidden/not-found/conflict/expired 保留可判别错误，不折叠为假成功或空对象。
- **兼容要求：** 不改变 system domain 既有 exports 语义。
- **安全与隐私要求：** 不写 localStorage/sessionStorage，不在 debug 日志打印 one-time result。

## 6. 执行路线

1. 从后端已锁定 DTO/路径建立类型和响应 fixture。
2. 写 runtime narrowing、错误分支和 self/admin URL/参数测试。
3. 实现 query/command functions，确保 GET/POST 和 scope 不变量。
4. 更新 root exports/package 文档并跑 domain 测试、typecheck、lint。

## 7. 路径访问契约

- **预计修改点/可写范围：** system domain `open-api` slice 及 root exports/package/README。
- **只读上下文：** platform HTTP 与后端 Controller 合同。
- **共享路径：** root exports/package/README 由 T-10 独占；T-11 只消费。
- **保留或不动：** web-domain、App profile、后端与其他 domain。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| runtime narrowing | domain unit tests | `pnpm --filter @namewta/domain-system test` | 合法 fixture 收窄，畸形/敏感响应拒绝 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>` |
| scope 与 method/path | mocked HTTP contract tests | 同上 | self 无 userId、admin 有 target、GET/POST 正确 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>` |
| public exports | typecheck/lint | `pnpm --filter @namewta/domain-system typecheck && pnpm --filter @namewta/domain-system lint` | 新 API 可消费且无架构越界 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>` |

- **Workspace checks：** source-worktree/current-workspace 执行 domain test/typecheck/lint。
- **E2E disposition：** not-required：本票只交付 domain 合同；用户工作流由 T-11 组件/App 集成测试覆盖。
- **E2E owner/environment：** Frontend owner / current-workspace。
- **Integration evidence：** implementation/source、parent before、candidate/result SHA 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 后端 catalog/credential 合同稳定后实现，先于 T-11。
- **兼容窗口：** additive exports；未消费前无运行时变化。
- **监控信号：** runtime narrowing 错误与 HTTP error code 分布由 UI 层处理。
- **回滚或前向恢复：** 移除 UI 消费即可；不保留或迁移本地 secret 状态。
- **不可逆操作与批准点：** 无。
- **收缩条件：** root export 唯一、scope/method/path 测试全绿。

## 10. 验收标准

- [ ] `AC-009`、`AC-020` 至 `AC-022`、`AC-024`、`AC-026`、`AC-027` 的前端合同可消费。
- [ ] self/admin scope、GET/POST 和运行时收窄有单测。
- [ ] secret 只出现在 create/reset 一次性结果且不持久化。
- [ ] system domain 测试、typecheck、lint 与 Evidence/集成 SHA 完整。
