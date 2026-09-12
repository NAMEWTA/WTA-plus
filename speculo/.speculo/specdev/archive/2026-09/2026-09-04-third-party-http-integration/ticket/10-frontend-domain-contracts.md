---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-10
title: 建立 third 前端 Domain 合同与权限投影
status: done
planning_depth: standard
planning_depth_reason: 新增独立前端领域包及四类资源 transport/model/permission，但不涉及 App 路由组合或页面交互。
ready: true
risk: medium
blocked_by: [T-03, T-04, T-05, T-09]
contract_ids: [AC-015, AC-016]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/packages/domains/third/**</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/domains/third/**</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/domains/system/**</Path>"
  - "<Path>plus-ui-namewta/packages/domains/profile/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/**</Path>"
shared_paths:
  - "<Path>plus-ui-namewta/packages/domains/third/**</Path>"
shared_path_owners:
  - "<Path>plus-ui-namewta/packages/domains/third/**</Path> => T-10"
---

# Ticket T-10: 建立 third 前端 Domain 合同与权限投影

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/10-frontend-domain-contracts.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>`

## 1. 战略与来源

- **目标：** 将 Provider、Endpoint、Invocation、Statistics 的前端模型、transport、service 和权限集中到独立 third domain。
- **可观察产出：** web-domain/App 只能通过 package exports 调用类型安全服务；权限码与后端 `third:<resource>:<action>` 精确一致，无深层导入。
- **来源：** `US-008`、`AC-015`、`AC-016`、`ADR-012`、项目 plus-ui frontend conventions。
- **当前事实：** monorepo 有 system/profile 等 domain 先例，但没有 `@namewta/domain-third`。
- **Planning Depth 原因：** 多文件跨四资源但沿用成熟包模式，风险主要是 transport 与权限漂移。

## 2. 决策状态

### 已锁定决策

- third 是独立业务 domain，不放入 system；package root 明确导出四资源与公共 runtime。
- 只映射后端脱敏 VO；前端类型不得出现 plaintext secret、ciphertext 或完整 raw body。
- API 前缀与 GET/POST 语义严格跟随后端；权限常量保持精确字符串。

### 已采用的低影响假设

- 包脚本、tsconfig、AGENTS 和 README 复制邻近 domain 的结构后按 third 事实裁剪。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/architecture.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/naming-and-layout.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/implementation.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/permission-routing.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/crud-resource-slices.md</Path>。
- **目录与代码最低要求：** domain-third 只放领域类型、transport mapper、service/factory 和资源元数据；按 packages/domains/third/src/<resource> 组织，资源使用 kebab-case，index.ts 薄入口，公开资源必须在 package.json exports 显式声明。每个含 package.json 的新包必须有同目录 AGENTS.md、README、tsconfig 和 test 脚本。
- **依赖与类型要求：** App → web-domain → domain → platform；domain 不依赖 Vue/DOM/具体请求库，不深导入 system 或其他 workspace。API-contracts 传输类型在 domain 边界映射为 domain-owned model；禁止 catch-all export、跨工作区相对导入和把 secret/ciphertext/raw body 放进类型。
- **执行停止条件：** 在 App 建 src/api 复制接口、把页面/DOM 放入 domain、自动副作用注册、漏写 exports/AGENTS、权限字符串与后端不一致或为“可能复用”提前放入 platform/web-kit 时立即停止。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| domain package、四资源 types/transport/service/permission、mapper 测试 | platform contracts/runtime、现有 domain 组织模式 | Vue 页面、App 注册、菜单 SQL、锁文件 |

## 4. 要构建什么

前端调用者可从 `@namewta/domain-third` 或显式子路径获得 Provider/Endpoint CRUD、凭据替换、调用明细和统计查询服务；请求与后端方法一致，响应经边界 mapper 转为领域模型，非法/未知状态不会静默映射为启用。

## 5. 实现契约

- **入口或接缝：** package exports、runtime service factory、resource transport。
- **输入与输出：** typed query/command DTO → typed domain result；敏感写入值只存在于命令生命周期。
- **公共接口变化：** 新增 `@namewta/domain-third` workspace 包。
- **不变量：** 不依赖 web-domain/system；无 deep import；权限码单一来源；只读模型无 secret/ciphertext。
- **状态或数据流：** web runtime 注入 HTTP → transport → boundary mapper → domain service。
- **错误与失败行为：** 未知 enum/缺字段以显式错误处理；不伪造 enabled/success。
- **兼容要求：** App 尚未组合时新包不改变现有终端。
- **安全与隐私要求：** secret command 禁止序列化到持久 store/日志，response 类型无敏感字段。

## 6. 执行路线

1. 建立 exports、transport method/path 和敏感字段负向类型/映射测试。
2. 创建 package/AGENTS/tsconfig/runtime 和四资源垂直切片。
3. 实现权限常量、模型与 boundary mapper。
4. 运行 domain test、typecheck、lint 和 architecture check。

## 7. 路径访问契约

- **预计修改点/可写范围：** 整个新 `packages/domains/third`，由 T-10 唯一写。
- **只读上下文：** system/profile domain 和后端 Admin Controller。
- **共享路径：** 新 domain 包为前端消费者共享合同，后续 Ticket 只通过 exports 消费。
- **保留或不动：** pnpm-lock、Admin package/services/manifest、web-domain。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | transport/mapper Vitest | 四资源正常响应和命令映射 | method/path/model 与后端一致 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>` |
| 失败路径 | boundary tests/type scan | 未知状态、错误响应、响应含敏感字段 | 明确失败且类型不暴露 secret/ciphertext/raw body | 同上 |
| 回归 | package gates | domain test/typecheck/lint + architecture check | 无 deep import 或跨域反向依赖 | 同上 |

- **Workspace checks：** source/current workspace 运行 package test/typecheck/lint 和 monorepo architecture check。
- **E2E disposition：** not-required：本 Ticket 只建立 headless domain 合同，浏览器组合由 T-12 验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 不适用：additive 未组合 package。
- **兼容窗口：** package 未由 App 消费前无运行时影响。
- **监控信号：** 不适用：无独立运行时。
- **回滚或前向恢复：** App 组合前可整体回滚；组合后保持 exports 并前向修复。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用：无旧前端 third 合同。

## 10. 验收标准

- [x] `AC-015/016` 所需四资源 transport、权限与脱敏类型已稳定导出。
- [x] domain 测试、类型、lint 和架构门禁通过。
- [x] writable/shared 路径仅由 T-10 修改并记录集成 Evidence。
- [x] E2E not-required 理由成立，无未批准偏差。
