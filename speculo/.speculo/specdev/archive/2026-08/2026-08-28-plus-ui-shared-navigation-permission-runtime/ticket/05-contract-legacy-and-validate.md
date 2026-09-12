---
schema_version: 3
artifact: ticket
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
id: T-05
title: 收缩遗留入口并完成全量验收
status: done
planning_depth: deep
planning_depth_reason: 零兼容收缩会删除最后的旧动态入口并修改全局架构规则、长期源码地图和父子仓库交付状态，且承担最终认证权限 E2E Gate。
ready: true
risk: high
blocked_by: [T-04]
contract_ids: [AC-009, AC-013, AC-014, AC-015]
owner: codex:leadership-epoch-1
expected_changes: ["<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>", "<Path>plus-ui-namewta/tooling/architecture/**</Path>", "<Path>plus-ui-namewta/README.md</Path>", "<Path>plus-ui-namewta/.codex/skills/plus-ui-domain-development/**</Path>", "<Path>plus-ui-namewta/.claude/agents/plus-ui-domain-development.md</Path>", "<Path>README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>", "<Path>plus-ui-namewta/tooling/architecture/**</Path>", "<Path>plus-ui-namewta/README.md</Path>", "<Path>plus-ui-namewta/.codex/skills/plus-ui-domain-development/**</Path>", "<Path>plus-ui-namewta/.claude/agents/plus-ui-domain-development.md</Path>", "<Path>README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/**</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/apps/admin-web/src/**</Path>", "<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/packages/web-kit/**</Path>", "<Path>plus-ui-namewta/e2e/**</Path>"]
shared_paths: ["<Path>plus-ui-namewta/tooling/architecture/**</Path>", "<Path>plus-ui-namewta/README.md</Path>", "<Path>README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/**</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/tooling/architecture/**</Path> => T-05", "<Path>plus-ui-namewta/README.md</Path> => T-05", "<Path>README.md</Path> => T-05", "<Path>docs/namewta-enhancements.md</Path> => T-05", "<Path>.agents/skills/namewta-fullstack-development/**</Path> => T-05"]
---

# Ticket T-05: 收缩遗留入口并完成全量验收

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/05-contract-legacy-and-validate.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 删除最后的空动态路由入口，以架构测试阻止旧边界回流，并把前端与父仓库长期文档/Skill 更新为最终真实结构。
- **可观察产出：** 工作区只有一条 manifest-only 动态页面路径；旧名称与路径零匹配；开发/生产构建和完整 Admin Playwright 全绿；维护者从 README 与 Skill 能直接找到新 owner。
- **来源：** `US-005`、`US-006`、`US-007`、`AC-009`、`AC-013`、`AC-014`、`AC-015`、`ADR-003`、`USER-DECISION:零兼容基座`。
- **当前事实：** `<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>` 仍导出空 `dynamicRoutes`；父仓库和前端 Skill 仍指向 permission Store、App 私有指令与 `filterAsyncRouter`。
- **Planning Depth 原因：** 这是 destructive contract 阶段和最终集成 Gate，同时跨前端子仓库与父仓库当前知识文件。

## 2. 决策状态

### 已锁定决策

- 删除空 `dynamicRoutes` 及其说明，不保留 deprecated export、alias 或 feature flag。
- 架构规则至少防止 Web Domain 权限宿主回退到 App 私有目录、Platform 引入 Vue/DOM、Admin 恢复本地动态页面 glob 或旧 permission Store 正式入口。
- 文档只描述最终当前真相；迁移历史留在 Speculo/Git，不写入长期 README 或 Skill。
- 前端子仓库代码/文档先形成独立 commit；父仓库再同步长期 Skill/文档和经过验证的 submodule pointer。

### 已采用的低影响假设

- 只更新源码扫描确认失真的长期文档；未提及该能力且仍准确的文件不为形式改写。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 空 dynamicRoutes 删除、架构回流测试、长期 README/Skill 更新、零匹配扫描、全量门禁与父子仓库交付 | T-01 至 T-04 已集成实现、现有架构工具和质量命令 | 不再改变共享 API 或 Admin 行为，不写迁移史，不更新后端源码 |

## 4. 要构建什么

在 Admin 已完全使用新宿主和 navigation Store 后，删除 Router 的空 legacy export，运行精确扫描证明旧 Store、私有权限指令、动态过滤和本地 `views` glob 均不存在。架构测试把这些边界变成可执行 Ratchet；README 与开发 Skill 只展示新的 Web Kit、Platform App Runtime、navigation Store 和 manifest-only 路径。最后在集成父状态运行完整静态、单元、双模式构建和 Playwright。

## 5. 实现契约

- **入口或接缝：** Router 静态配置、架构 CLI/tests、前端/父仓库 README 与权限路由 Skill、根质量命令。
- **输入与输出：** 输入为 T-04 已集成源码；输出为零 legacy 生产入口、可执行架构保护、最新文档和完整验证记录。
- **公共接口变化：** 删除未使用的内部 `dynamicRoutes` export；无兼容层。共享包接口不再变化。
- **不变量：** manifest-only、Platform 终端纯度、App 自有 navigation Store、Client 菜单权威和恢复顺序保持。
- **状态或数据流：** 不改变运行时数据流；只收缩未使用入口并强化静态治理。
- **错误与失败行为：** 任一 legacy 扫描命中、架构违规、构建或 E2E 失败均阻止完成与父分支推进。
- **兼容要求：** 无；删除前由 T-04 和源码扫描证明消费者为零。
- **安全与隐私要求：** 最终 E2E 必须覆盖权限失败关闭；文档不得包含凭据或本机绝对路径。

## 6. 执行路线

1. 在 T-04 集成结果上扫描全部旧名称、路径、glob 和动态过滤调用点，确认仅剩本 Ticket 授权的遗留项。
2. 删除空 `dynamicRoutes`，扩展架构测试以阻止旧 owner、深层导入和终端依赖回流，并执行受控反向验证确认规则会失败。
3. 更新前端 README、前端内置开发 Skill、父仓库增强说明与 canonical 前端 Skill，删除失真说明和历史叙述。
4. 在前端子仓库形成 clean implementation/source commit；Lead 在父状态同步 pointer 与父仓库文档。
5. 运行精确零匹配扫描、architecture、lint、typecheck、test、build:dev、build:prod。
6. 由 Lead 在 direct-parent 或 parent-candidate 运行完整 Playwright，复核父子仓库状态并形成最终 result。

## 7. 路径访问契约

- **预计修改点：** frontmatter 中 Router、架构工具、README、`.codex`/`.claude` 与 canonical Skill 路径。
- **可写范围：** 仅 frontmatter 列出的前端和父仓库路径。
- **只读上下文：** 全部 Admin 源码、共享包和 E2E，用于扫描与验证。
- **共享路径：** 架构工具、根 README、增强说明与 canonical Skill；唯一 owner 为 T-05。
- **保留或不动：** 后端子仓库、领域 API/页面、锁文件、T-01 E2E 和 T-02/T-03 公共实现。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 全工作区 Gate | architecture、lint、typecheck、test、build:dev、build:prod | 所有共享包和 Admin 组合绿色 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-05.md</Path>` |
| 失败路径 | 扫描、架构反向验证、Playwright | 注入受控违规证明规则变红后恢复；运行权限/未知组件 E2E | legacy/越界会被拦截，运行时失败关闭 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-05.md</Path>` |
| 回归 | 完整浏览器与文档审查 | `pnpm test:e2e`；扫描旧路径和内部机器绝对路径 | 全套 Admin 场景通过，长期说明与源码一致 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-05.md</Path>` |

- **Workspace checks：** current 策略在 current-workspace 严格串行；required 策略在 source-worktree 运行非 E2E，Lead 在 parent-candidate 运行全量组合与 E2E。
- **E2E disposition：** required：这是认证、权限、动态菜单迁移完成后的最终收缩和发布 Gate。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；运行完整 `pnpm test:e2e`，source-worktree 不运行 E2E。
- **Integration evidence：** 分别记录前端子仓库 source/implementation commit、父仓库 parent before、适用 candidate、submodule pointer、父仓库 result SHA 与包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 仅在 T-04 绿色并集成后执行；先扫描、再收缩、再文档、最后全量 Gate。
- **兼容窗口：** 无；legacy 使用量为零后立即删除。
- **监控信号：** 零匹配扫描、架构违规数、所有命令退出码、Playwright 场景结果和父子仓库 clean 状态。
- **回滚或前向恢复：** 最终 Gate 失败不推进父分支；通过 Git 回退 T-05 或修复正式入口，不恢复兼容门面。
- **不可逆操作与批准点：** 无生产发布或远端写入；implementation commit、子模块集成、父 pointer 更新均需用户授权。
- **收缩条件：** `dynamicRoutes`、`filterDynamicRoutes`、旧 permission Store、App 私有 permission directive、本地动态 `views` glob 和对应旧文档引用全部零匹配，并有 Evidence。

## 10. 验收标准

- [x] `AC-009`、`AC-013`、`AC-014`、`AC-015` 全部可判定通过。
- [x] 所有 legacy 标识、源码路径和兼容门面零匹配，架构反向验证证明规则有效。
- [x] 前端和父仓库长期文档/Skill 只描述最终真实 owner。
- [x] 全量静态、单元、双模式构建与完整 Playwright 均由 Lead 记录实际退出码。
- [x] 前端子仓库与父仓库 commit/result、submodule pointer 和 clean 状态写入 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-05.md</Path>`。
- [x] 未发生未批准偏差，Ticket、Map 与 Evidence 状态一致。
