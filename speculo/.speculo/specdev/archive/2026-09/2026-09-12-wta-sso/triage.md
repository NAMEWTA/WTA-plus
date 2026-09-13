---
schema_version: 1
artifact: triage
change: 2026-09-12-wta-sso
mode: reconcile
source: <Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/source.md</Path>
classification: feature
risk: high
route: specdev/archive-and-consolidate
ready_for_implementation: true
external_action: not-applicable
updated_at: 2026-09-13T20:18:00+08:00
---

# Triage: WTA 第一方 SSO（Authorization Code + PKCE）

## 当前判定

- **影响：** 独立 SSO 管理双面、`wta-sso` Auth Code/PKCE、`sso-web`、FirstParty 槽位、admin/home 适配与三门 E2E 已落地；本文件仅作归档完成门。
- **紧急度：** scheduled
- **当前证据：** 来源为粘贴 intake（`source_type: pasted`）；无远程 Issue。T-01…T-07 frontmatter `status: done`；Evidence T-01…07 + Round4 验图过门；CR-001 `approve-with-notes`（`reviews/CR-001.md`；fixed `214d601` → head `377a145`；工作树 HEAD 其后仅 docs）。Lead/CTO 已接受 CR-001 NOTES 并派归档。
- **相关代码/工件：** Spec、ADR、CONTEXT、Tickets Map、ticket/01–07、evidence/T-01…07、`reviews/CR-001.md`、`evidence/sso-r4-*.png` / `sso-ac00*.png`（**勿**用 `superseded-r3/` 或历史 `sso-admin-config-*.png` 当完成证据）

### 已裁决残留（不挡归档）

- **CR-001 NOTES：** AC-002/003 PNG 字节相同——隔离门靠 API `code!==200` + Token extras；属截图证据卫生残余，非产品 FAIL。Lead/CTO 已接受。
- **CR-001 note：** SSO 列表「密钥未配置」与「拿配置」并存——对 public/PKCE 合同可接受。
- **外脑：** 归档 dry-run 本轮跳过；**不走 ChatGPT**；**未标通过**。历史 CR 外脑 = 账号 Research Heavy（已吸收），禁止假装 ChatGPT 通过。证据：`external-brain/notes.md`、`reviews/CR-001.md`。

## 未知项

- **可发现事实：** 无（归档所需事实已由 CR-001 / Evidence / Lead 派单固定）
- **需要用户决定：** 无（Lead 已派本 change 归档 dry-run；不 push；等待 confirmed）
- **低影响实现细节：** Tickets Map 表头可能仍写 blocked（文档滞后）；票 frontmatter 与 Evidence 为 done 权威

## 路由

- **下一 Work：** `<Path>{roots.workflows}/specdev/A-archive-and-consolidate/A-archive-and-consolidate.md</Path>`
- **理由：** 本地完成门已满足；来源无远程 Issue；`external_action: not-applicable`；CR-001 NOTES 已接受；外脑未标通过且不阻塞。

## 外部动作

- **远程目标：** 无
- **关闭能力：** not-applicable
- **当前状态：** not-applicable
- **授权记录：** 无
- **尝试与结果：** 无

外部动作只投影最终完成，不替代本地状态、Ticket、Map 或 Evidence。本 change 无远程关闭动作，故 `external_action: not-applicable`。
