---
schema_version: 3
artifact: tickets-map
change: 2026-08-27-plus-ui-backend-aligned-domains
status: completed
---

# Tickets Map: 前端领域按后端模块对齐

- **Map：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/evidence/</Path>`
- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/goal-plan.md</Path>`

## 1. 目标与拆分策略

本次是不可保留中间兼容层的宽重构。包名、消费者、manifest 和旧目录必须在同一可构建候选内收缩，因此使用一个 Deep 原子 Ticket，而不是制造无法独立保持绿色的水平切片。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/ticket/01-align-domain-layout.md</Path>` | canonical 模块/资源可定位且双 App 行为不变 | — | deep | critical | yes | codex:/root | AC-001..AC-006 | W1/G1 | done |

## 3. 依赖 DAG

```text
T-01 [DONE] -> G1 [PASSED] -> main@d60b807
```

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01 | workspace/architecture | passed | canonical 包与旧包清零 |
| AC-002 | T-01 | resource manifest/tests | passed | Controller 资源唯一映射 |
| AC-003 | T-01 | Admin unit/build/E2E | passed | 登录菜单权限不回归 |
| AC-004 | T-01 | Client architecture/build/E2E | passed | App 独立性不回归 |
| AC-005 | T-01 | OpenAPI/domain tests | passed | HTTP 合同不变 |
| AC-006 | T-01 | 文档扫描/review | passed | 中文文档当前性 |

## 5. 并行与路径所有权

单 Ticket、单实现 owner、独立 source worktree；Lead 独占 SpecDev 状态、candidate 和前端 main 推进。

## 6. Gate、Wave 与集成点

W1 在 source worktree 完成实现与非 E2E。G1 在最新 main 的 parent-candidate 运行完整非 E2E 与 Playwright；全部通过后才推进 main。

## 7. 横切契约与风险

不改变 HTTP、OpenAPI、Client、Token、权限、component key 或页面行为。旧 workspace 包名无兼容窗口。

## 8. 同步规则

Ticket frontmatter 是状态权威；状态、SHA 和验证事实由 Lead 同步到 Goal Plan、Map 与 Evidence。
