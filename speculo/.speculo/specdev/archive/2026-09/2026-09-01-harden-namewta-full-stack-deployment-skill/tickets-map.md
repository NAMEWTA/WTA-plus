---
schema_version: 3
artifact: tickets-map
change: 2026-09-01-harden-namewta-full-stack-deployment-skill
status: completed
---

# Tickets Map: 强化 NAMEWTA 全栈部署 Skill

- **Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/</Path>`
- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/goal-plan.md</Path>`

## 1. 目标与拆分策略

四个纵向切片依次稳定 profile、候选验证、Agent 运行流程和私密交接输出。每个 Ticket 都有独立测试或静态接缝，当前 workspace 严格串行，避免共享脚本并行写入。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/01-version-deployment-profile-contract.md</Path>` | v2 profile 严格且 v1 可读 | — | standard | medium | yes | codex:/root | AC-001, AC-002 | W1/G1 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/02-verify-release-candidate.md</Path>` | 已知发布故障稳定红灯 | T-01 | standard | medium | yes | codex:/root | AC-003..007 | W2/G2 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/03-document-rolling-release-runbook.md</Path>` | 完整发布运行手册可发现 | T-02 | standard | medium | yes | codex:/root | AC-009 | W3/G3 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/04-render-complete-release-evidence.md</Path>` | 私密配置和完整报告可生成 | T-03 | standard | medium | yes | codex:/root | AC-008, AC-010 | W4/G4 | done |

## 3. 依赖 DAG

```text
T-01 [READY]
  -> T-02
       -> T-03
            -> T-04
```

关键路径为 `T-01 -> T-02 -> T-03 -> T-04`；每个 Gate 关闭后才开始下一 Ticket。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001, AC-002 | T-01 | profile unit | covered | schema 与兼容 |
| AC-003..007 | T-02 | candidate/frontend unit + CLI | covered | 发布风险矩阵 |
| AC-009 | T-03 | Skill/reference static | covered | Agent 操作合同 |
| AC-008, AC-010 | T-04 | render/report integration | covered | 交接与全回归 |

## 5. 并行与路径所有权

- implementation subagent 上限来自 `<Path>{roots.state}/specdev/config.json</Path>`，但本计划不派单。
- Lead 是唯一项目、SpecDev 状态和父分支 owner。
- `ticket_workspace_policy: current`，所有 Ticket 严格串行。
- Ticket 可写路径无并行交集；后续 Ticket 只读取前序稳定合同。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 是 | 串行 |
| T-02 | T-03 | 无 | 是 | 串行 |
| T-03 | T-04 | 无 | 是 | 串行 |

## 6. Gate、Wave 与集成点

- G1：profile v2 正负向与 v1 回归通过。
- G2：候选、语义、前端、稳定窗口和 waiver 矩阵通过。
- G3：Skill 与一级 references 同步且安全边界完整。
- G4：私密渲染、报告、全测试、SpecDev 和 diff 门禁通过。

## 7. 横切契约与风险

- secret 只进入 `0600` 本地文件；state、stdout 和 Speculo 工件不含值。
- 默认备份硬门保持；dev waiver 不扩展到生产。
- 本 change 不访问外部环境，E2E 均 not-required 并由本地 CLI 集成替代。

## 8. 同步规则

- Ticket 状态变化后同步本 Map 与 Goal Plan。
- Ticket frontmatter 为单 Ticket 权威，Goal Plan 为 Gate/顺序权威。
- 每个 Gate 运行 SpecDev validator；最终运行 implement stage。
