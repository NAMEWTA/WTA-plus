---
schema_version: 3
plan_contract_version: 1
plan_revision: 1
requested_deliverables: []
deliverable_policy: "本轮仅 P-goal-plan draft；无正式 Ticket 文件。禁止为糊 validate 造假票。正式拆票待 T-tickets（上游：G-grill → S-spec）。"
artifact: tickets-map
change: 2026-09-12-wta-sso
status: draft
---

# Tickets Map — 2026-09-12-wta-sso（outline only）

- **Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`（尚未创建）
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/</Path>`（空；无 Markdown 票）
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/</Path>`
- **可选 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/goal-plan.md</Path>`

> **OUTLINE ONLY — 待 T-tickets。** 下列内容为规划预告，**不是**正式票；`ticket/` 不得放置伪造票文件。`validate --stage goal-plan` 因无票失败是预期。

## 1. 目标与拆分策略

将 CTO SSO 方案拆为：Grill 锁定 → Spec AC → 正式票 →（授权后）P0 后端/前端/双 App 接入/硬验收。`ticket_workspace_policy=current` + `integration_gate=direct-parent` 严格串行。

### 总体实施背景

- 权威：CTO > ADR/CONTEXT > Spec > Goal Plan > 本 map > tickets。
- 协议：OAuth2 Authorization Code + PKCE S256；无外置 IdP；禁 Implicit/password/SAML；OIDC→P1。
- 模块：`backend/wta-modules/wta-sso` + `frontend/apps/sso-web`；扩展 `sys_client`；access_token=Sa-Token 且 extras=目标业务 Client。
- P0：authorize/token/revoke + local/sso/both；硬验收双 App clientid 隔离。
- 写面：本轮仅 SpecDev；`implementation_commit=not-authorized`。
- 开放：CTO-Q1…Q4（见 Goal Plan blockers）。
- 隔离：禁止触碰 `2026-09-10-notify-channel-config`。

### 项目 Skill 读取矩阵

预告最低必读集合（T-tickets 时按真实票 ID 再绑定；本轮无票可覆盖）。

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | 架构、权限、API、质量门禁 | Map 后、Ticket 前 | 硬约束与模块模式 |
| ALL | `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>` | 跨层 SSO/前后端合同 | Map 后、Ticket 前 | 垂直切片 |
| ALL | `<Path>.agents/skills/wta-module-guide/SKILL.md</Path>` | 新模块 `wta-sso` 落地 | Map 后、Ticket 前 | 模块边界 |
| ALL | `<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>` | common/SPI 与安全工具入口 | 进入后端票前 | 禁止误接第三方 IdP SDK |
| ALL | `<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>` | 域名/回调/同进程 vs 独立域 | 发布相关票前 | 环境与回调清单 |

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| — | （无正式票） | outline only；见 Goal Plan Waves | G-grill → S-spec → T-tickets | — | — | no | — | — | — | draft-outline |

正式 `T-01…` 编号、路径与 frontmatter **仅**在 T-tickets 产生。禁止为变绿 validate 在此表伪造 Ready 行并落盘 `ticket/*.md`。

## 3. 依赖 DAG

```text
[Done] P-goal-plan
  → G-grill-with-docs
  → S-spec
  → T-tickets   （此处才出现 T-xx 节点）
  → [Gate] implementation_authorized?
        └─→ P0 tickets (BE → FE → Apps → Hard-E2E)  # current 串行
```

无正式票边；上图为 Work/预告依赖，不充当 Ticket DAG 权威。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-PROTO-PKCE | 待 T-tickets | authorize/token PKCE S256 | uncovered | Spec 未建；非 deferred 糊弄 |
| AC-TOKEN-CLIENT-EXTRAS | 待 T-tickets | Token extras=目标业务 Client | uncovered | 硬不变量 |
| AC-AUTHMODE | 待 T-tickets | local/sso/both + /auth/login | uncovered | 待 CTO-Q3 |
| AC-HARD-DUAL-APP | 待 T-tickets | 同浏览器双 App clientid 互拒 | uncovered | P0 硬验收 |
| AC-SYS-CLIENT-EXT | 待 T-tickets | sys_client SSO 字段 + secret hash | uncovered | 一层应用目录 |

## 5. 并行与路径所有权

- implementation subagent 上限来自 config（Goal Plan 快照为 2）；Lead 不计。
- current 模式：**禁止**并行写项目路径。
- 共享路径预告：`sys_client`、`/auth/client/context`、`packages/platform/auth` → 正式票指定唯一 shared owner。
- 与 notify change 路径隔离。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| （无） | （无） | — | — | outline；待 T-tickets |

## 6. Gate、Wave 与集成点

见 `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/goal-plan.md</Path>`：G-Grill → G-CTO-Q → G-Spec → G-Tickets → G-Auth-I → G-P0-Hard。本 Map 不另造 Gate 权威。

## 7. 横切契约与风险

- Token/Client 隔离不可放宽；禁伪 SSO Cookie。
- 无外置 IdP；禁 Implicit/password/SAML。
- 域名/回调/部署形态未决 → 相关实现票不得 Ready。
- 外脑由父进程补；不因占位宣称审查通过。

## 8. 同步规则

- 正式票出现后同步执行清单与 DAG；以 Ticket frontmatter 为权威。
- Skill 矩阵随票 ID 收紧 Applies To；重新 validate。
- Goal Plan 存在时 Wave/Gate/owner 以 Goal Plan 为编排权威。
- 禁止相对 Markdown 链接；使用 Path 标签。

## 9. 总控与恢复

从本 Map 进入 `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>` 的 plan/run/resume/replan/verify。当前仅 **plan draft** 完成；下一 Work=`specdev/G-grill-with-docs`。

- `requested_deliverables=[]`；无额外命名交付物数量要求。
- `ready_for_execution=false`；缺 Spec/正式票/CTO 答复/实现授权时不得 run。
- 恢复：读 Goal Plan、`.status.json`、source、ADR/CONTEXT/LOG、external-brain notes；从 Grill 继续。
- 全部票 done 仍不等于 Goal 完成；须过 G-P0-Hard。
