---
schema_version: 3
artifact: ticket
change: 2026-08-27-plus-ui-backend-aligned-domains
id: T-01
title: 原子迁移后端模块对齐的前端领域布局
status: done
planning_depth: deep
planning_depth_reason: 破坏性重命名全部领域包、跨域公开入口、App 组合与动态菜单 manifest，属于共享核心路径和高事故半径迁移
ready: true
risk: critical
blocked_by: []
contract_ids: [AC-001, AC-002, AC-003, AC-004, AC-005, AC-006]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/packages/domains/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/**</Path>", "<Path>plus-ui-namewta/packages/platform/auth/README.md</Path>", "<Path>plus-ui-namewta/apps/**</Path>", "<Path>plus-ui-namewta/e2e/**</Path>", "<Path>plus-ui-namewta/tooling/architecture/**</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/README.md</Path>", "<Path>plus-ui-namewta/.codex/skills/plus-ui-domain-development/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/**</Path>", "<Path>plus-ui-namewta/packages/platform/auth/README.md</Path>", "<Path>plus-ui-namewta/apps/**</Path>", "<Path>plus-ui-namewta/e2e/**</Path>", "<Path>plus-ui-namewta/tooling/architecture/**</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/README.md</Path>", "<Path>plus-ui-namewta/.codex/skills/plus-ui-domain-development/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/**</Path>", "<Path>plus-ui-namewta/packages/api-contracts/generated/**</Path>", "<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/packages/adapters/**</Path>"]
shared_paths: ["<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/package.json</Path> => T-01", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-01", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path> => T-01"]
---

# Ticket T-01: 原子迁移后端模块对齐的前端领域布局

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/ticket/01-align-domain-layout.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 用后端模块和 Controller 资源替代语义混合的 domain/web-domain 第一层，同时保持产品行为不变。
- **可观察产出：** 开发者可从模块和 base path 确定定位 API、类型、页面；Admin/Client 全流程继续工作。
- **来源：** `US-001` 至 `US-003`、`AC-001` 至 `AC-006`、用户批准的完整 Plan。
- **当前事实：** `identity-access` 跨 admin/system，`system-admin` 与 `operations` 同属 system，`devtools` 跨 gen/system；请求逻辑仍集中在大入口文件。
- **Planning Depth 原因：** 包名、imports、锁文件和动态 manifest 必须原子切换，不能通过兼容 facade 掩盖遗漏。

## 2. 决策状态

### 已锁定决策

- canonical 包为 admin/system/gen/workflow/demo/ai，web-domain 同名。
- 资源目录按 HTTP base path 的 kebab-case 建立；包根与显式子路径是唯一公开入口。
- 登录跨 admin/system 的流程使用注入端口组合。
- 旧包、旧 imports、旧文档在同一 Ticket 收缩为零。

### 已采用的低影响假设

- 外部 iframe 控制台只有 Admin 一个消费者，保留为 App 私有集成能力。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 包重命名、资源拆分、App 迁移、架构门禁、中文文档 | HTTP adapter、platform、OpenAPI 生成 transport、现有页面行为 | 后端接口、UI 重设计、移动端激活、兼容 facade |

## 4. 要构建什么

Admin 和 Client 从新 `admin/system/gen/workflow/demo/ai` 入口显式组合服务与页面。动态菜单仍使用原 component key 解析；每个已消费 Controller 资源在对应包目录拥有类型、API/服务与测试入口。未知或未选择资源继续失败关闭。

## 5. 实现契约

- **入口或接缝：** package exports、App services、Admin manifest registry、web-domain manifest、OpenAPI aliases。
- **输入与输出：** 所有现有 HTTP 请求与领域返回类型保持语义不变；只改变 TypeScript workspace imports。
- **公共接口变化：** 删除旧 workspace 包名，新增 canonical 包名与显式资源子路径。
- **不变量：** HTTP、Client、Token、权限、组件键、路由和页面交互不变。
- **状态或数据流：** admin/auth 执行认证请求，system/user 与 system/menu 提供身份和菜单端口，App 注入组合。
- **错误与失败行为：** 安全 URL、畸形响应、未选择 manifest 和未知组件继续抛出当前稳定错误。
- **兼容要求：** 产品行为兼容；源码包名不兼容且不保留门面。
- **安全与隐私要求：** 不记录凭据，不弱化加密 header、session 隔离或权限负向路径。

## 6. 执行路线

1. 建立 canonical 包名和资源映射架构测试，锁定旧包为禁止项。
2. 迁移 system/admin/gen headless 能力，拆出 Controller 资源入口并调整跨域端口。
3. 迁移对应 web-domain 资源、外部控制台 App 私有集成和全部 App composition。
4. 整理 workflow/demo/ai 包内 Controller 资源，更新 exports、锁文件与测试。
5. 删除旧包和旧引用，更新中文 README 与前端 Skill。
6. 在 source worktree 运行全部非 E2E 门禁并创建实现 commit。
7. Lead 在 parent-candidate 运行完整非 E2E 与双 App E2E，通过后推进前端 main。

## 7. 路径访问契约

- **预计修改点/可写范围/只读上下文/共享路径：** 以前置 frontmatter 为权威。
- **保留或不动：** 后端、OpenAPI 生成文件、platform 与 adapters 的既有公共合同。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 模块与资源可定位 | architecture tests + 旧引用扫描 | `pnpm architecture:check && pnpm architecture:test`、`rg` | canonical 映射唯一，旧包为零 | `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/evidence/T-01.md</Path>` |
| HTTP/类型回归 | OpenAPI/domain tests | `pnpm openapi:check && pnpm test` | 全部通过 | 同上 |
| App 集成 | lint/type/build | `pnpm lint && pnpm typecheck && pnpm build:dev && pnpm build:prod` | 双 App 构建通过 | 同上 |
| 登录、菜单与页面 | Playwright | `pnpm test:e2e` | 双 App 全套通过 | 同上 |
| 文档当前性 | README/Skill 扫描 | 搜索旧包名与非中文说明 | 无过时说明 | 同上 |

- **Workspace checks：** source worktree 运行全部非 E2E 检查。
- **E2E disposition：** required：动态菜单、跨域认证和全部页面注册均被重命名影响。
- **E2E owner/environment：** Lead / parent-candidate。
- **Integration evidence：** source commit、parent before、candidate/result SHA、父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 新 canonical 内部结构完成并迁移全部消费者后，原子删除旧包。
- **兼容窗口：** 无；用户明确基座无需兼容。
- **监控信号：** architecture/openapi/type/test/build/E2E 任一失败即阻塞集成。
- **回滚或前向恢复：** 候选失败不推进 main；已推进后可回退单一实现 commit。
- **不可逆操作与批准点：** 删除旧包与本地 main 推进已由用户批准；push/deploy 未授权。
- **收缩条件：** `rg`、workspace importer 与 lock importer 均证明旧包名和目录为零。

## 10. 验收标准

- [x] `AC-001` 至 `AC-006` 全部通过并写入 Evidence。
- [x] 实际项目修改未超出可写范围。
- [x] source commit 与 parent-candidate 全门禁通过，前端 main result 已记录。
- [x] E2E 在 parent-candidate 由 Lead 完成。
- [x] 旧包、旧引用与兼容门面为零。
- [x] Ticket、Tickets Map、Goal Plan 和 Evidence 状态一致。
