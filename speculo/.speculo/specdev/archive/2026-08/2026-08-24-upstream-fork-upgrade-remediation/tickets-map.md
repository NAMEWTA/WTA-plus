---
schema_version: 3
artifact: tickets-map
change: 2026-08-24-upstream-fork-upgrade-remediation
status: completed
---

# Tickets Map: 上游 Fork 升级整改

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/</Path>`

## 1. 目标与拆分策略

四个 Ticket 按前端红门禁、后端构建合同、跨仓 CI/E2E、数据与父快照收口串行推进。T-03 复用前两个 Ticket 暴露的稳定 scripts；T-04 是唯一数据迁移和父仓 integration owner。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Status |
|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/ticket/01-frontend-quality-and-placeholder-retirement.md</Path>` | 前端门禁全绿且占位组件退役 | — | standard | medium | yes | codex | AC-001, AC-004 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/ticket/02-backend-build-upstream-and-support-contract.md</Path>` | 后端默认测试、双 bundle、上游补丁和 MySQL 合同 | — | standard | medium | yes | codex | AC-002, AC-005 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/ticket/03-ci-and-real-service-acceptance.md</Path>` | CI 自动执行两端和真实服务验收 | T-01, T-02 | standard | high | yes | codex | AC-003 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/ticket/04-menu-migration-knowledge-and-parent-snapshot.md</Path>` | 菜单下线、知识准确、父快照候选正确 | T-01, T-02, T-03 | deep | high | yes | codex | AC-004, AC-006 | done |

## 3. 依赖 DAG

```text
T-01 ─┐
      ├─→ T-03 ─→ T-04
T-02 ─┘
```

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 状态 |
|---|---|---|
| AC-001 | T-01 | covered |
| AC-002 | T-02 | covered |
| AC-003 | T-03 | covered |
| AC-004 | T-01, T-04 | covered |
| AC-005 | T-02 | covered |
| AC-006 | T-04 | covered |

## 5. 并行与路径所有权

T-01/T-02 路径不相交，但当前 workspace 策略要求严格串行。T-03 独占 CI 和新增 MinIO test；T-04 独占 SQL、长期知识和父仓 gitlink。Lead 是唯一 SpecDev/Evidence writer。

## 6. 横切契约与风险

- 不移动上游基线标签，不向镜像分支写业务提交。
- 不泄露 CI 临时 secret，不用 UI 隐藏替代服务端权限。
- implementation commit/push 已由用户授权；四个 Ticket 均以可复现父仓结果和远程 CI 证据闭合。
