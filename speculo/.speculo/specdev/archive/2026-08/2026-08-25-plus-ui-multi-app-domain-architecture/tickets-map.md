---
schema_version: 3
artifact: tickets-map
change: 2026-08-25-plus-ui-multi-app-domain-architecture
status: completed
---

# Tickets Map: plus-ui 多 App 领域架构改造

- **Map：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/</Path>`
- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/goal-plan.md</Path>`（Ready；`required + candidate-merge`）

## 1. 目标与拆分策略

本 Map 将 `US-001` 至 `US-012` 和 `ADR-001` 至 `ADR-007` 落成 17 个可验证纵向切片。先用行为基线与占位合同形成安全落点，再建立 workspace、平台端口、浏览器适配器和 demo manifest 曳光弹；随后证明第二 App，迁移认证、工作流及其余能力领域；最后收缩根应用、引入 OpenAPI 生成并固化上游能力映射。

迁移遵循 expand-migrate-contract：`T-03` 至 `T-14` 保留现有 `<Path>plus-ui-namewta/src/**</Path>` 兼容入口，只有 `T-15` 在消费者扫描与双 App 门禁通过后删除旧入口。真实移动端/小程序实现由 Spec 明确排除，本变更只交付占位和可移植边界。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/01-baseline-behavior.md</Path>` | 迁移前认证、路由、请求与构建基线可重复验证 | — | standard | high | yes | `native:/root/t01` | AC-021 | W1/Gate A | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/02-placeholder-contracts.md</Path>` | 完整目标目录以 README 占位且不激活虚假包 | T-01 | lite | low | yes | `native:/root/t01` | AC-004, AC-005, AC-009 | W2/Gate B | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/03-workspace-and-architecture-gate.md</Path>` | pnpm workspace 与依赖边界 Ratchet 激活 | T-02 | deep | high | yes | `native:/root/t01` | AC-001, AC-007, AC-008, AC-025, AC-026, AC-030 | W3/Gate C | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/04-platform-browser-adapters.md</Path>` | 无头平台端口与浏览器适配器保持管理端行为 | T-03 | deep | high | yes | `native:/root/t01` | AC-006, AC-007, AC-008, AC-021 | W4/Gate D | done |
| T-05 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/05-demo-manifest-pilot.md</Path>` | demo 领域通过 manifest 完成首个端到端迁移 | T-04 | deep | high | yes | `native:/root/t01` | AC-010, AC-011, AC-012, AC-022, AC-027 | W5/Gate E | done |
| T-06 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/06-client-web-proof.md</Path>` | client-web 以独立 Client 和壳层组合登录与 demo | T-05 | deep | high | yes | `native:/root/t01` | AC-003, AC-018, AC-019, AC-023 | W6/Gate F | done |
| T-07 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/07-identity-access-migration.md</Path>` | 双 App 共用认证授权核心且动态路由语义不变 | T-06 | deep | critical | yes | `native:/root/t01` | AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-018, AC-021 | W7/Gate G1 | done |
| T-08 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/08-workflow-definition-slice.md</Path>` | 流程分类、定义和表达式成为可组合领域切片 | T-07 | standard | high | yes | `native:/root/t01` | AC-009, AC-010, AC-021 | W8/Gate G2 | done |
| T-09 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/09-workflow-runtime-slice.md</Path>` | 任务、实例、请假与用户选择形成完整运行时切片 | T-08 | deep | high | yes | `native:/root/t01` | AC-006, AC-007, AC-009, AC-010, AC-021 | W8/Gate G3 | done |
| T-10 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/10-system-identity-governance.md</Path>` | Client、用户、角色、菜单和组织治理完成迁移 | T-09 | deep | high | yes | `codex:/root` | AC-009, AC-010, AC-017, AC-018, AC-021 | W8/Gate G4 | done |
| T-11 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/11-system-resource-services.md</Path>` | 字典、配置、通知、OSS、消息与社交能力完成迁移 | T-10 | deep | high | yes | `codex:/root` | AC-009, AC-010, AC-019, AC-021 | W8/Gate G5 | done |
| T-12 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/12-ai-domain.md</Path>` | AI API、模型和页面可由 App 显式组合 | T-09 | standard | medium | yes | `codex:/root` | AC-009, AC-010, AC-019, AC-021 | W8/Gate G5 | done |
| T-13 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/13-devtools-domain.md</Path>` | 代码生成和开发工具通过 system 公开合同组合 | T-11 | standard | high | yes | codex:/root | AC-009, AC-010, AC-019, AC-021 | W8/Gate G6 | done |
| T-14 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/14-operations-domain.md</Path>` | 监控与运维能力完成领域化并保持安全边界 | T-09 | standard | high | yes | `codex:/root` | AC-009, AC-010, AC-019, AC-021 | W8/Gate G6 | done |
| T-15 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/15-admin-web-contract.md</Path>` | admin-web 正式激活且旧根 src 兼容入口清零 | T-11, T-12, T-13, T-14 | deep | critical | yes | codex:/root | AC-002, AC-021, AC-024 | W10/Gate H | done |
| T-16 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/16-openapi-contracts.md</Path>` | api-contracts 可生成、可漂移检查且不污染业务模型 | T-15 | deep | high | yes | codex:/root | AC-027, AC-028 | W11/Gate I | done |
| T-17 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/17-upstream-capability-map.md</Path>` | 上游变更按能力映射到本地边界和验证 | T-16 | standard | medium | yes | codex:/root | AC-029 | W12/Gate J | done |

## 3. 依赖 DAG

```text
T-01 baseline
  -> T-02 placeholders
    -> T-03 workspace + architecture ratchet [shared root owner]
      -> T-04 platform + browser adapters [shared transport owner]
        -> T-05 demo manifest pilot [Gate E]
          -> T-06 client-web proof [Gate F]
            -> T-07 identity-access [shared auth/router owner]
              -> T-08 workflow definition
                -> T-09 workflow runtime + system user seam [fan-out]
                  +-> T-10 system identity -> T-11 system resources -> T-13 devtools --+
                  +-> T-12 AI ----------------------------------------------------+
                  +-> T-14 operations --------------------------------------------+
                                                                                   -> T-15 admin-web contract
                                                                                      -> T-16 OpenAPI
                                                                                        -> T-17 upstream map
```

`T-15` 是 contract 汇合点；它必须等待所有存量能力迁移完毕。依赖边只表达可执行前置产物，不表达人员交接。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-03 | workspace list/lockfile | covered | 只激活真实包 |
| AC-002 | T-15 | admin build + Playwright | covered | 管理端正式入口 |
| AC-003 | T-06 | client build + Playwright | covered | 第二 App 证明 |
| AC-004 | T-02 | 占位目录扫描 | covered | 不含 package.json |
| AC-005 | T-02 | README 合同检查 | covered | 八项边界齐全 |
| AC-006 | T-04, T-09 | import graph | covered | 无头依赖边界 |
| AC-007 | T-03, T-04, T-09 | exports/typecheck | covered | 禁止 deep import |
| AC-008 | T-03, T-04 | dependency graph | covered | 无环且方向正确 |
| AC-009 | T-02, T-08, T-09, T-10, T-11, T-12, T-13, T-14 | README/manifest tests | covered | 七领域可追踪 |
| AC-010 | T-05, T-08, T-09, T-10, T-11, T-12, T-13, T-14 | manifest tests | covered | App 显式选择 |
| AC-011 | T-05 | registry conflict unit | covered | 重复 key fail-fast |
| AC-012 | T-05, T-07 | registry/adapter unit + staged E2E | covered | T-05 证明 registry 与显式诊断 harness；T-07 以无物理 facade 的 typo key 关闭全局 fail-visible |
| AC-013 | T-07 | router/store + E2E | covered | 恢复顺序保持 |
| AC-014 | T-07 | permission store | covered | 不做二次 Client 过滤 |
| AC-015 | T-07 | Client auth E2E | covered | 登录注册 fail-close |
| AC-016 | T-07 | request/session + E2E | covered | 401 语义保持 |
| AC-017 | T-07, T-10 | evaluator/auth matrix | covered | superadmin 规范化 |
| AC-018 | T-06, T-07, T-10 | 多 Client E2E | covered | 会话上下文隔离 |
| AC-019 | T-06, T-11, T-12, T-13, T-14 | import graph + 双 App build | covered | 表现层可独立定制 |
| AC-020 | 后续独立变更 | 新终端 architecture check + build | deferred | 用户批准本期仅建 mobile-web、miniapp-taro 与 Taro adapter README 占位；真实终端在规格激活后另立 change |
| AC-021 | T-01, T-04, T-07-T-15 | 每波 Gate Evidence | covered | 失败不收缩兼容入口 |
| AC-022 | T-05 | Goal Plan Gate E | covered | demo 失败阻断认证迁移 |
| AC-023 | T-06 | Goal Plan Gate F | covered | 第二 App 失败阻断根入口迁移 |
| AC-024 | T-15 | rg + builds + E2E | covered | 旧入口清零后收缩 |
| AC-025 | T-03 | manifests/lockfile | covered | workspace/catalog 合同 |
| AC-026 | T-03 | config diff | covered | 不引入重编排平台 |
| AC-027 | T-05, T-16 | API tests + generation gate | covered | 前序不等待生成 |
| AC-028 | T-16 | generation drift + typecheck | covered | transport 与业务模型分离 |
| AC-029 | T-17 | customization map review | covered | 按能力吸收上游 |
| AC-030 | T-03 | baseline report + ratchet | covered | 新边界立即严格 |

## 5. 并行与路径所有权

- `T-03` 唯一拥有根 `<Path>plus-ui-namewta/package.json</Path>`、workspace、lockfile 策略、根 TypeScript/Vite/Oxlint 配置；其他 Ticket 只读，变更必须由 Goal Plan 形成显式 ownership deviation。`DEV-T04-001` 与 `DEV-T05-001` 只把各自 root facade 实际消费的内部 `workspace:*` 声明及匹配 lock specifier/importers 委托给对应 Ticket，不转移策略所有权；`DEV-T05-001` 同时把占位 README 明确约定在 demo pilot 激活的 `<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>` 转交 T-05。`DEV-T06-001` 只允许 T-06 写其目录内新 package manifests 机械生成的 lock importers，不允许 root manifest 或 lock 策略漂移。
- `T-04` 唯一拥有兼容期 `<Path>plus-ui-namewta/src/utils/request.ts</Path>` 与 `<Path>plus-ui-namewta/src/utils/auth.ts</Path>`；`T-07` 消费其端口并拥有认证 store/router/guard。
- `T-07` 唯一拥有兼容期认证、权限和动态路由共享路径；业务域 Ticket 只注册 manifest，不写全局路由表。
- `T-09` 先建立 system user 公共接缝，`T-10` 复用并扩展 system-admin 内部实现，不回写 workflow 私有路径。
- `T-10`、`T-12`、`T-14` 在 `T-09` 后可并行；`T-13` 必须等待 `T-11` 的字典/菜单公开合同。
- `T-15` 是 `<Path>plus-ui-namewta/src/**</Path>` 收缩 owner；它在汇合前不得开始删除。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-10 | T-12 | 无 | 否 | 可并行 |
| T-10 | T-14 | 无 | 否 | 可并行 |
| T-12 | T-14 | 无 | 否 | 可并行 |
| T-11 | T-13 | system 公开合同 | 是 | T-13 blocked_by T-11 |
| T-12 | T-15 | AI 包及旧入口 | 是 | T-15 只在汇合后收缩 |

## 6. Gate、Wave 与集成点

本变更已由 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/goal-plan.md</Path>` 锁定为 `lead-directed`、`required` source worktree、`candidate-merge`。主要实现仓库 `<Path>plus-ui-namewta/</Path>` 每 Ticket 使用独立 worktree，最多 3 个 implementation subagent；Lead 串行集成 candidate、运行 required E2E 并推进前端 `main`。docs 聚合父仓库不创建 worktree，只由 Lead 在当前 workspace 写 SpecDev 工件和 T-17 scoped docs commit。

Map 中 W1-W12/Gate A-J 是产品迁移投影；Goal Plan 将 workflow/system 扇出细化为 W08A/W08B/W09A-W09C，并明确 T-10 优先集成以解锁 T-11。实际 base/source/candidate/result SHA、worktree locator 与 Gate 关闭证据在执行期写入 change status 和 Ticket Evidence。

## 7. 横切契约与风险

- 后端按 Client 裁剪菜单仍是授权权威；前端只映射 component key 并 `addRoute`。
- Domain 不依赖 Vue Router、Element Plus、DOM、浏览器存储、App 或 concrete adapter；跨包只走 exports。
- ClientContext 在认证请求前必须完整且类型正确；失败时 fail-close，不能回退默认 Client。
- manifest component key 冲突与缺失都必须稳定失败并提供 App/domain/key 诊断。
- 每波保持 admin-web 可运行；旧入口只有在替代路径、双 App build、E2E 与调用点扫描均通过后收缩。
- generated transport schema 不能替代 domain 业务模型；上游更新不能以路径同构为目标。

## 8. 同步规则

- Ticket frontmatter 是状态、依赖、深度和路径契约权威，Map 仅作同步投影。
- Goal Plan 建立后，Wave、Gate、owner、workspace 和 integration 顺序以 Goal Plan 为权威并回写本 Map。
- Ticket 状态、依赖、合同覆盖或路径所有权变化后，重新运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`。
- 每个完成 Ticket 必须产生 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-NN.md</Path>`，记录命令、结果和 SHA；当前只创建 Evidence 目录，不预写完成证据。
