---
schema_version: 3
artifact: tickets-map
change: 2026-08-20-namewta-client-rbac-review
status: completed
---

# Tickets Map: Client RBAC Review 整改

- **Map：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/</Path>`
- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/goal-plan.md</Path>`

## 1. 目标与拆分策略

按上游基线、后端合同、前端消费者、集成交付四个垂直 Gate 串行推进。公共合同由后端先行，父文档和 gitlink 由最终集成 Ticket 唯一拥有。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/ticket/01-sync-backend-upstream.md</Path>` | 当前 upstream 后端基线 | — | standard | medium | yes | codex-root | AC-006 | W1/G1 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/ticket/02-fix-backend-contracts.md</Path>` | 后端严格 Client/RBAC 合同 | T-01 | deep | critical | yes | codex-root | AC-001,003,004,005 | W2/G2 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/ticket/03-fix-frontend-contracts.md</Path>` | 前端 scoped 角色和 fail-closed 认证 | T-02 | deep | high | yes | codex-root | AC-002,003 | W3/G3 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/ticket/04-integrate-and-verify.md</Path>` | 文档、双端门禁和父快照 | T-02,T-03 | deep | high | yes | codex-root | AC-001..006 | W4/G4 | done |

## 3. 依赖 DAG

```text
T-01 [DONE] -> T-02 [DONE] -> T-03 [DONE] -> T-04 [DONE]
```

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-02,T-04 | backend negative matrix | covered | fail-closed RBAC |
| AC-002 | T-03,T-04 | user UI/API | covered | scoped summaries |
| AC-003 | T-02,T-03,T-04 | token/auth pages | covered | no fallback |
| AC-004 | T-02,T-04 | register transaction | covered | optional identities |
| AC-005 | T-02,T-04 | session state | covered | all-domain invalidation |
| AC-006 | T-01,T-04 | build/SQL/git tree | covered | reproducible delivery |

## 5. 并行与路径所有权

current 模式严格串行，不启用 implementation subagent。T-04 是两份父文档和 gitlink 的唯一 owner；Speculo 状态始终由 Lead 写入。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-02 | T-03 | 无 | 是 | 后端合同先行 |
| T-03 | T-04 | gitlink 间接依赖 | 是 | child commit 后更新 parent |

## 6. Gate、Wave 与集成点

G1 当前 upstream 可构建；G2 后端合同与测试通过；G3 前端消费者与静态/构建通过；G4 双端门禁、CR-002 代码/规范轴和父快照通过。Disposable SQL、HTTP、Token 和浏览器 runtime E2E 由用户明确设为 `not-required`，不计为 Gate 失败。

## 7. 横切契约与风险

不保留兼容 fallback；不新增 schema/依赖/测试源码；所有 Client PK 使用 Long；未执行的 runtime E2E 必须保留为 `not-required`，不得写成通过。

## 8. 同步规则

Ticket 状态以 frontmatter 为权威；Goal Plan 编排串行 direct-parent；每个状态变化后运行 validator 并同步本 Map。
