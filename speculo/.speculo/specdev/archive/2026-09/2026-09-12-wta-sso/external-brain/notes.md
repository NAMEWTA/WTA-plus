# External brain notes — 2026-09-12-wta-sso

**Updated:** 2026-09-13T09:34:04+08:00

## 权威

- CTO BRIEF 2026-09-13 t155u
- Research / Grok Heavy：`reports/20260913-wta-sso-grill-report.md`（部分符合）；文档收束 Review：`reports/20260913-wta-sso-doc-review.md`
- 可晋升：`runs/20260913-wta-sso-grill/`（含 `open-questions-grill.md`）；收束稿：`runs/20260913-wta-sso-doc-review/02-replace-patches.md`
- **旧 ChatGPT `reply.md` 不作产品定位权威**

## 已晋升（最新态）

| 目标 | 来源 |
|---|---|
| `CONTEXT.md` | CONTEXT-latest.md + 2026-09-13 文档收束（F-1/F-2） |
| `goal-plan.md` | Ready Spec 对齐最新态（不再 Grill precursor） |
| `ADR.md` | FirstPartySsoProvider / Round3 已收口 |
| 开放题 | P0-1…P0-7 → design-tree D-110…116；BRIEF 确认题 D-100/D-101（已答附录） |
| `external-brain/open-questions-grill.md` | 同 runs 片段；文首已收束 |

## 决策状态（最新）

- D-100=A；D-101=A；D-102/103 LOCKED_FROM_BRIEF；D-104=A
- D-110=C；D-111=B；D-112=A；D-113=A；D-114=SOCIAL_DIR+CONTEXT；D-115=A；D-116=B
- frontier 空；design-tree `consensus` round=3
- S-spec **已授权并定稿**（LOG-032）；T-tickets 已落盘（LOG-033）
- 门禁：`implementation_commit=not-authorized`；不实现；不碰已归档 notify

## C-code-review CR-001（2026-09-13T20:03+08:00）

- **现行外脑 = 账号 Research**，不是 ChatGPT。
- 本轮 C **已吸收** Research Heavy 前审报告：
  - `/workspace/share-research/reports/20260913-wta-sso-r4-heavy-review.md`
  - 该报告结论 PASS_WITH_NOTES（开 I 前 Spec/票审）；本 CR 用其 8 条硬核对点对照 Round4 I 实现产物。
- **禁止**将旧 `reply.md` / ChatGPT pack 标为外脑通过。
- 权威 CR：`reviews/CR-001.md`（approve-with-notes）。

## 归档 dry-run（2026-09-13T20:18:00+08:00）

- 归档 dry-run 外脑跳过；不走 ChatGPT；未标通过。
- 本轮优先跳过 Research；不假装任何外脑通道通过。
- CR-001 NOTES（AC-002/003 同图证据卫生等）已由 Lead/CTO 接受并派归档。
