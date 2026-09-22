---
schema_version: 3
plan_contract_version: 1
plan_revision: 1
requested_deliverables: [{"name": "保留原始审计报告", "count": 1}, {"name": "实施完成报告", "count": 1}, {"name": "最终代码PR", "count": 1}]
deliverable_policy: "用户明确要求审计与实现不丢失、完成报告、一个大PR并验证后合并；六票为施工拆分不是额外交付数量。"
artifact: "tickets-map"
change: "2026-09-22-audit-foundation-hardening"
status: "ready"
---

# Tickets Map：基座审计整改

## 1. 目标与拆分策略

### 总体实施背景

原审计是输入，真实源码和测试决定具体修复。以同版消费者切换实施，不做兼容层。通知结果、OSS操作、安全配置共享源码采用单Lead串行；现有SSO/旧review change均不接管。永久ADR只在A阶段沉淀。

### 项目 Skill 读取矩阵

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | <Path>.agents/skills/engineering-standards/SKILL.md</Path> | 现有代码、测试与交付 | Map后Ticket前，按scope引用 | apply-project-contracts-and-verify-scope |
| ALL | <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path> | 现有代码、测试与交付 | Map后Ticket前，按scope引用 | apply-project-contracts-and-verify-scope |
| ALL | <Path>.agents/skills/wta-module-guide/SKILL.md</Path> | 现有代码、测试与交付 | Map后Ticket前，按scope引用 | apply-project-contracts-and-verify-scope |
| ALL | <Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path> | 现有代码、测试与交付 | Map后Ticket前，按scope引用 | apply-project-contracts-and-verify-scope |

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|
| T-01 | <Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/ticket/01-safe-defaults-inbox.md</Path> | 安全默认值和独立消息读取 | — | deep | high | true | audit-lead | S-01, S-02, N-01 | 1 | in_progress |
| T-02 | <Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/ticket/02-notify-delivery-reliability.md</Path> | 通知本地原子性与外部重试语义 | T-01 | deep | high | true | audit-lead | N-02, N-03, N-04 | 2 | ready |
| T-03 | <Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/ticket/03-notify-contract-lifecycle.md</Path> | 通知重试截止撤回与附件合同 | T-02 | deep | high | true | audit-lead | N-05, N-06, N-07, N-09 | 3 | ready |
| T-04 | <Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/ticket/04-inbox-bounded-read.md</Path> | 有界收件箱读取与批量投递成本 | T-03 | deep | high | true | audit-lead | N-08, N-10 | 4 | ready |
| T-05 | <Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/ticket/05-oss-runtime-boundary.md</Path> | OSS按需访问与迁移互斥 | T-04 | deep | high | true | audit-lead | O-01, O-02, O-03, O-04 | 5 | ready |
| T-06 | <Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/ticket/06-simple-start-consolidation.md</Path> | 启动配置收敛与整体验收 | T-05 | deep | high | true | audit-lead | D-01, D-02 | 6 | ready |

## 3. 依赖 DAG

T-01 → T-02 → T-03 → T-04 → T-05 → T-06。序列反映共享合约与最终集成，单写工作区不并行施工。

## 4. 合同覆盖矩阵

S-01, S-02, N-01 由 T-01 的验证矩阵覆盖，未执行前不宣称通过。

N-02, N-03, N-04 由 T-02 的验证矩阵覆盖，未执行前不宣称通过。

N-05, N-06, N-07, N-09 由 T-03 的验证矩阵覆盖，未执行前不宣称通过。

N-08, N-10 由 T-04 的验证矩阵覆盖，未执行前不宣称通过。

O-01, O-02, O-03, O-04 由 T-05 的验证矩阵覆盖，未执行前不宣称通过。

D-01, D-02 由 T-06 的验证矩阵覆盖，未执行前不宣称通过。

## 5. 并行与路径所有权

current workspace，唯一Lead audit-lead；未装配子代理。共享源码按上述顺序交接，Lead唯一持有specdev/父分支，其他active change只读。

## 6. Gate、Wave 与集成点

安全与消息读取 → 发送可靠性 → 应用合同 → 有界读取 → OSS → 启动与全套验收。任何失败都写Evidence，整体验收不以票状态代替。

## 7. 横切契约与风险

无真实供应商发送、无生产数据、无历史强推；SQL唯一基座与Client授权保留；未知不等于失败。

## 8. 同步规则

Ticket frontmatter为单票权威，Map仅投影。Lead更新绑定摘要/计划revision时说明原因，不覆盖旧证据。原报告原样保存。

## 9. 总控与恢复

从各Evidence恢复真实源/命令/未闭合动作。验证失败暂停合并；全部实际验收后写完成报告，A dry-run显示本change归档与知识计划，用户已授权本change归档但不清理其他change。
