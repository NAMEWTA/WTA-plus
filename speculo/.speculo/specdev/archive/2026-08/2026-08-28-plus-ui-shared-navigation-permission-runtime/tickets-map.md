---
schema_version: 3
artifact: tickets-map
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
status: completed
---

# Tickets Map: Plus UI 共享导航与权限运行时收口

- **Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/</Path>`
- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/goal-plan.md</Path>`（completed：T-01 至 T-08 与 CR-002 全部关闭）

## 1. 目标与拆分策略

八个 Ticket 共同交付 `US-001` 至 `US-007` 和 `AC-001` 至 `AC-015`。T-01 至 T-05 保留第一次实现的历史完成事实；`CR-001` 的 `request-changes` 重新打开 change，并将整改拆为三个独立边界：T-06 修复权限指令异常失败关闭，T-07 建立递归菜单领域模型与显式 Router 适配，T-08 稳定并发 E2E 并重新执行最终 Gate。

本 change 不采用 expand-contract：用户已确认当前为基座，不需要兼容窗口。阶段 checkpoint 只用于保持绿色和 Git 恢复，不构成双轨生产入口。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/01-freeze-permission-navigation-behavior.md</Path>` | Admin 浏览器行为基线不依赖 legacy fallback | — | deep | high | yes | codex:leadership-epoch-1 | AC-005/006/008/009/011/012/014 | Wave 0 / Baseline | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/02-build-web-permission-host.md</Path>` | 任意 Vue Web App 可注入 evaluator 安装权限指令 | T-01 | deep | high | yes | codex:leadership-epoch-1 | AC-001/002/003/004/012/014 | Wave 1 / Shared contract | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/03-extract-server-menu-runtime.md</Path>` | App Runtime 提供确定菜单投影与诊断 | T-01 | deep | high | yes | codex:leadership-epoch-1 | AC-006/007/010/012/014 | Wave 1 / Shared contract | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/04-migrate-admin-navigation-composition.md</Path>` | Admin 完成新宿主、navigation Store 和 manifest-only 迁移 | T-02, T-03 | deep | critical | yes | codex:leadership-epoch-1 | AC-001..012/014 | Wave 2 / Admin migration | done |
| T-05 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/05-contract-legacy-and-validate.md</Path>` | legacy 零匹配、架构 Ratchet、文档同步、全量验收 | T-04 | deep | high | yes | codex:leadership-epoch-1 | AC-009/013/014/015 | Wave 3 / Contract + Final | done |
| T-06 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/06-fail-close-permission-directive-errors.md</Path>` | 异常时先移除受限 DOM，再抛出稳定错误 | T-05 | deep | high | yes | codex:leadership-epoch-1 | AC-002/003/004/012/014 | Wave 4 / Review repair | done |
| T-07 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/07-validate-server-menu-boundary.md</Path>` | 递归菜单校验与显式 Admin Router adapter | T-05 | deep | high | yes | codex:leadership-epoch-1 | AC-006/007/009/010/012/014 | Wave 4 / Review repair | done |
| T-08 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/08-stabilize-e2e-and-final-validation.md</Path>` | 两条 locator 稳定、并发重复验证、最终全量 Gate | T-06, T-07 | deep | high | yes | codex:leadership-epoch-1 | AC-002/005/006/008/009/011/012/014/015 | Wave 5 / Final revalidation | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影。

## 3. 依赖 DAG

```text
T-01 [DONE: behavior-freeze prefactor]
  ├─→ T-02 [DONE: Web permission shared contract]
  └─→ T-03 [DONE: server-menu shared contract]
        ╲
T-02 ────╲
           → T-04 [DONE: Admin migration convergence]
                    → T-05 [DONE: zero-compat contract + final gate]
                         → CR-001 [REQUEST-CHANGES]
                              ├─→ T-06 [DONE: permission failure-close]
                              └─→ T-07 [DONE: recursive menu boundary]
                                      ╲
                              T-06 ─────╲
                                         → T-08 [DONE: stable E2E + final gate]
                                              → CR-002 [APPROVED]
```

- 根 Ticket：T-01。
- 历史并行扇出：T-02 与 T-03 已按 required worktree 实现并由 Lead 串行集成。
- 汇合点：T-04 必须消费两个已集成公共合同。
- 收缩点：T-05 只有在 T-04 证明正式消费者已迁移后才能删除 legacy。
- 评审整改扇出：T-06 与 T-07 在获得新授权后可从 T-05 result 独立实现；candidate 仍由 Lead 串行集成。
- 最终汇合：T-08 必须基于 T-06/T-07 都已进入前端 main 的结果；归档还要求独立 `CR-002=approved`。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-02, T-04 | Web Kit Vue test + Admin composition | covered | 指令宿主与真实 App 组合 |
| AC-002 | T-02, T-04, T-06, T-08 | 真实 Vue lifecycle + Admin E2E | passed | T-06 证明异常先移除 DOM，T-08 完整回归 |
| AC-003 | T-02, T-04 | evaluator 组合 test | covered | 指令与命令式语义一致 |
| AC-004 | T-02 | dependency/architecture test | covered | Platform 维持非 Web 纯度 |
| AC-005 | T-01, T-04 | navigation recovery test + Playwright | covered | 固定恢复顺序和请求次数 |
| AC-006 | T-01, T-03, T-04, T-07, T-08 | Domain parser、菜单纯函数、Store、Client E2E | passed | 后端菜单权威；Domain runtime boundary 已闭合 |
| AC-007 | T-03, T-04, T-07 | Domain/App Runtime 表驱动 test | passed | 特殊组件、ParentView、children、外链与递归 narrowing |
| AC-008 | T-01, T-04 | manifest registry + Playwright | covered | 已选 manifest 与静态页面 |
| AC-009 | T-01, T-04, T-05, T-07, T-08 | 未知键/畸形输入、零匹配扫描、E2E | passed | manifest-only 与递归输入失败关闭 |
| AC-010 | T-03, T-04, T-07 | 结构化诊断 + Admin 呈现 test | passed | 重复 route name 无 UI 反依赖，输入先缩窄 |
| AC-011 | T-01, T-04 | navigation Store、布局、Playwright | covered | 导航投影行为不变 |
| AC-012 | T-01, T-02, T-03, T-04, T-06, T-07, T-08 | 生命周期、递归 parser、各层失败测试 + E2E | passed | CR-001 的 DOM 异常与菜单边界缺口已关闭 |
| AC-013 | T-05 | legacy 扫描 + architecture tests | covered | 零兼容且阻止回流 |
| AC-014 | T-01..T-08 | 阶段 Evidence 与 checkpoint | passed | 历史与整改 Evidence、source/candidate/result 完整 |
| AC-015 | T-05, T-08 | 根静态/单元/双构建/重复并发 Playwright | passed | 5-worker/0-retry 连续三轮与标准 suite 全部通过 |

不存在 uncovered、deferred 或 open repair 合同；全部合同已有通过的 Evidence，`CR-002` 为 `approved`。

## 5. 并行与路径所有权

- implementation subagent 上限来自 `<Path>{roots.state}/specdev/config.json</Path>`，当前为 3；Goal Plan 可降低且不含 Lead。
- T-02 唯一拥有 Admin package manifest 与 lockfile；T-03 唯一拥有 Platform App Runtime。
- T-04 唯一拥有 Admin 迁移源码，但明确不写 Router 静态表；T-05 唯一拥有 Router legacy 收缩、架构工具和长期文档/Skill。
- T-06 唯一拥有 Web Kit permission 修复；T-07 唯一拥有 Domain/App Runtime/Router adapter 修复；T-08 仅拥有两条 E2E spec。
- Lead 始终拥有 SpecDev 状态、Evidence、父分支集成和父仓库 submodule pointer。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-02 | T-03 | 无 | 否 | required 可并行；current 串行 |
| T-01 | T-02/T-03 | 无 | 是 | 先完成行为冻结 |
| T-02/T-03 | T-04 | 无，但公共 exports 被消费 | 是 | 两者均集成后开始 T-04 |
| T-04 | T-05 | 无；T-05 只读 T-04 生产源码 | 是 | 先迁移消费者，再 contract |
| T-06 | T-07 | 无 | 否 | 获得授权后可并行实现；Lead 串行 candidate integration |
| T-06/T-07 | T-08 | 无；T-08 只读产品结果 | 是 | 两个修复均进入 parent 后创建 T-08 |

## 6. Gate、Wave 与集成点

- **Wave 0 / Baseline Gate：** T-01 E2E 绿色。
- **Wave 1 / Shared Contract Gate：** T-02、T-03 包级与架构验证分别绿色，并在 T-04 前都已进入父状态。
- **Wave 2 / Admin Migration Gate：** T-04 定向测试、静态门禁和 required E2E 绿色。
- **Wave 3 / Contract Gate：** T-05 legacy 零匹配、架构反向验证、文档同步和全量 Gate 绿色。
- **Wave 4 / Review Repair Gate：** T-06 真实生命周期失败关闭绿色；T-07 递归 parser、显式 adapter、架构与定向 E2E 绿色。
- **Wave 5 / Final Revalidation Gate：** T-08 两条 locator 定向通过，5 workers/0 retries 完整套件连续至少 3 次通过，根全量 Gate 通过。
- **Review Gate：** Lead 以 T-06 至 T-08 result 为 fixed input 形成 `CR-002`；只有 `approved` 才可恢复 completed 并进入归档 dry-run。

Goal Plan 使用 `required + candidate-merge`。T-01 至 T-05 的历史结果保持不变；T-06 至 T-08 已形成 source commit、通过 candidate 并进入前端 `main@07962c7cad9ca4db168b3c423b9e3675f312a874`，`CR-002` 已批准。source cleanup 未授权，因此三个 source worktree/branch 保留。

## 7. 横切契约与风险

- 后端按 Client 裁剪的身份、权限与菜单始终是权威；前端不重新授权。
- 恢复顺序固定为 `getInfo -> getRouters -> addRoute -> replace`，任一步失败不得继续。
- 当前为零兼容基座：不引入 alias、转发、功能开关、dual path 或弃用期。
- source-worktree 不运行或声明 E2E；required E2E 只能由 Lead 在 parent-candidate 执行。
- 前端子仓库先完成源码 commit 与验证，父仓库后同步 submodule pointer 和长期文档；后端子仓库不在范围内。
- 每个阶段失败都停止推进并按 Git checkpoint 恢复，不通过放宽门禁制造绿色。
- 本次用户已授权 T-06 至 T-08 的实现 worktree、implementation commit 与 candidate integration；source cleanup、push、远端与生产动作仍不得发生。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；Ticket frontmatter 是权威。
- Goal Plan 建立后，Wave、Gate、workspace、owner 和集成顺序以 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/goal-plan.md</Path>` 为编排权威。
- 依赖、合同或路径所有权变化后重新运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`。
- 任一 Ticket 发现需修改其他 owner 路径时必须停止并按偏差流程修订，不能先改后报。
