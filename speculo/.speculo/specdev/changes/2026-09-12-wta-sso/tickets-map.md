---
schema_version: 3
plan_contract_version: 1
plan_revision: 1
requested_deliverables: []
deliverable_policy: "用户未要求额外命名交付物数量；requested_deliverables 为空。本轮仅 SpecDev 正式票（T-tickets）；implementation_commit=not-authorized，禁止产品代码/push/假 Evidence。"
artifact: tickets-map
change: 2026-09-12-wta-sso
status: blocked
---

# Tickets Map — 2026-09-12-wta-sso（正式票；未授权实施）

- **Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`（`ready` / `ready_for_tickets=true`）
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/</Path>`
- **可选 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/goal-plan.md</Path>`

> 正式 6 票已落盘；全部 `status: blocked` / `ready: false`（**blocked-by-auth**）。三门硬验收未授权实施。禁止造假票/假 Evidence 糊绿。

## 1. 目标与拆分策略

交付 **FirstPartySsoProvider**（默认第一第三方登录提供方）+ Authorization Code/PKCE + `wta-sso`/`sso-web` + 双 App 接入，并以三门硬验收收口。垂直切片按稳定契约扇出：Client 目录 → OAuth 模块 → sso-web 会话 → Provider/context → App 调用方 → 三门 E2E。`ticket_workspace_policy=current` + `integration_gate=direct-parent` 串行。

### 总体实施背景

- 权威：CTO > ADR/CONTEXT > Spec > Goal Plan > 本 map > tickets。
- 产品冻结：FirstPartySsoProvider；三门并列 AC-001/002/003；D-111=B 外部仅管理面登记；D-116=B confidential 运行时后置；Code+PKCE S256；Token extras=目标业务 Client；authMode both|sso|local；禁 OIDC/SLO/独立进程/Keycloak/共享 Token/Implicit/password/SAML；禁动 notify 归档。
- `implementation_commit=not-authorized`；`ready_for_execution=false`。
- 写面仅本 change SpecDev；不碰已删 `ruoyi-vue-plus-docs`。

### 项目 Skill 读取矩阵

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | 架构、权限、API、质量门禁；任意产品/测试改动前 | Map 后、Ticket 前 | 硬约束与模块模式 |
| ALL | `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>` | 跨层 SSO/前后端合同垂直切片 | Map 后、Ticket 前 | 垂直切片与接缝 |
| T-02 | `<Path>.agents/skills/wta-module-guide/SKILL.md</Path>` | 新模块 `wta-sso` 落地与跨模块接入 | 进入 T-02 前 | 模块骨架与边界 |
| T-02 | `<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>` | common/SPI/安全工具入口选择 | 进入 T-02 后端票前 | 禁止误接外置 IdP SDK |
| T-05 T-06 | `<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>` | Origin/callback 矩阵消费与 E2E 运行时审计 | 进入 T-05/T-06 前 | 环境与回调可达 |

已扫描并排除：`java-api-compatibility`（本期不单独交付公开 Java API 兼容演进）、`project-customization-delivery`（用户未显式激活）。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/01-sys-client-opsflow-external-register.md</Path>` | OpsFlow+外部登记+confidential 字段可建 | — / auth | deep | high | no | unassigned | AC-017,018,019,023 | W-P0-BE / G-Auth-I | blocked |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/02-wta-sso-oauth-pkce-module.md</Path>` | authorize/token/revoke+PKCE 负向+边界 | T-01 / auth | deep | critical | no | unassigned | AC-004…013,021,022 | W-P0-BE / G-Auth-I | blocked |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/03-sso-web-password-session-cookie.md</Path>` | sso-web 只密码+Set-Cookie | T-02 / auth | standard | high | no | unassigned | AC-016,020 | W-P0-FE / G-Auth-I | blocked |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/04-first-party-provider-context-authmode.md</Path>` | FirstParty 槽位+context+authMode | T-01,T-02 / auth | standard | high | no | unassigned | AC-001,014,015,023 | W-P0-FE / G-Auth-I | blocked |
| T-05 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/05-admin-home-platform-auth-adapters.md</Path>` | admin/home+platform/auth 接入 | T-03,T-04 / auth | standard | medium | no | unassigned | AC-001,014 | W-P0-Apps / G-Auth-I | blocked |
| T-06 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/06-three-gate-hard-e2e.md</Path>` | 三门硬 E2E 证据 | T-05 / auth | deep | critical | no | unassigned | AC-001,002,003 | W-P0-E2E / G-P0-Hard | blocked |

Ticket frontmatter 为状态/依赖/路径权威；本表为投影。全部票另被 **implementation_commit=not-authorized** 全局阻塞。

## 3. 依赖 DAG

```text
T-01 [BLOCKED-by-auth]
  ├─→ T-02 [BLOCKED-by-auth]
  │     └─→ T-03 [BLOCKED-by-auth]
  └─→ T-04 [BLOCKED-by-auth]   (also waits T-02)
        └─→ (join T-03) → T-05 [BLOCKED-by-auth]
              └─→ T-06 [BLOCKED-by-auth]  # 三门：AC-001/002/003
[Gate] G-Auth-I: implementation_commit authorized?
  └─ false: STOP（仅 SpecDev；不可开 I）
```

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-04, T-05, T-06 | 槽位/调用方/E2E | covered | P0-DEFAULT-PROVIDER-PATH |
| AC-002 | T-06 | 同浏览器复用 | covered | P0-SSO-REUSE |
| AC-003 | T-06 | clientid 互拒 | covered | P0-CLIENT-ISOLATION |
| AC-004 | T-02 | authorize 负向 | covered | PKCE challenge |
| AC-005 | T-02 | token 负向 | covered | verifier |
| AC-006 | T-02 | code 复用 | covered | 单次 |
| AC-007 | T-02 | TTL | covered | 过期 |
| AC-008 | T-02 | 绑定校验 | covered | client/redirect/challenge |
| AC-009 | T-02 | 白名单 | covered | 禁 `*` |
| AC-010 | T-02 | state CSRF | covered | |
| AC-011 | T-02 | 并发 | covered | 至多一成功 |
| AC-012 | T-02 | 日志 | covered | 无敏感字段 |
| AC-013 | T-02 | 模块边界 | covered | API/Port |
| AC-014 | T-04, T-05 | both+本地 login | covered | |
| AC-015 | T-04 | authMode=sso | covered | |
| AC-016 | T-03 | Set-Cookie | covered | |
| AC-017 | T-01 | OpsFlow | covered | |
| AC-018 | T-01 | 外部登记 D-111=B | covered | 无运行时要求 |
| AC-019 | T-01 | confidential 字段 D-116=B | covered | 运行时后置 |
| AC-020 | T-03 | sso-web 只密码 | covered | |
| AC-021 | T-02 | Token extras | covered | 非 sso |
| AC-022 | T-02 | revoke≠SLO | covered | |
| AC-023 | T-01, T-04 | Provider 预置/context | covered | |

无 uncovered；无 deferred。

## 5. 并行与路径所有权

- implementation subagent 上限：Goal Plan=2；**current 模式实际串行=1**。
- 共享路径：DDL/DML→T-01；pom/application.yml→T-02；AuthController/context→T-04；platform/auth→T-05。
- 与 notify change 路径隔离。
- 因全部 blocked/ready=false，无并发 Ready 写冲突；授权后仍按 DAG 串行。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | DDL/DML | 是（T-02→T-01） | 串行；owner=T-01 |
| T-02 | T-03 | wta-sso 会话包可能交叠 | 是（T-03→T-02） | 串行 |
| T-04 | T-05 | `frontend/packages/{web-,}domains/admin` | 是（T-05→T-04） | 串行。T-04 只稳定 context 契约 + 槽位种子 + admin 登录页第一按钮原型；调用方/home 接线后 **owner 迁到 T-05**。授权前无写冲突（全部 blocked）。 |
| T-01 | T-04 | DML 种子 | 是（T-04→T-01） | 种子写尊重 T-01 owner |

## 6. Gate、Wave 与集成点

| Gate/Wave | 含义 |
|---|---|
| G-Tickets | 本轮：正式票+map validate（进行中） |
| G-Auth-I | CTO/用户书面翻转 `implementation_commit` 前 **不可开 I** |
| G-P0-Hard | T-06 三门 Evidence |
| W-P0-BE | T-01→T-02 |
| W-P0-FE | T-03∥规划上接 T-04 |
| W-P0-Apps | T-05 |
| W-P0-E2E | T-06 |

编排权威仍见 Goal Plan；本 Map 投影。

## 7. 横切契约与风险

- Token/Client 隔离不可放宽；禁伪 SSO。
- D-111=B 文档必须明示：外部登记≠ confidential 运行时已通。
- 环境矩阵占位未替换不得上线。
- 外脑旧 ChatGPT 句不作权威。

## 8. 同步规则

- 以 Ticket frontmatter 为权威同步本表。
- Skill 矩阵变更后重新 validate。
- Goal Plan 存在时 Wave/Gate/owner 以 Goal Plan 为编排权威。
- 禁止相对 Markdown 链接；使用 Path 标签。

## 9. 总控与恢复

从本 Map 进入 `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>` 的 plan/run/resume/replan/verify。先运行 `<Path>{roots.workflows}/specdev/common/tools/ticket-control.mjs</Path>` 的 `--map` 只读检查。

- `requested_deliverables=[]`；无额外命名交付物。
- `ready_for_execution=false`；**不可开 I-implement**，直至 `implementation_commit` 授权且票 ready。
- 恢复：读 Goal Plan、`.status.json`、Spec、本 Map、LOG；确认仍 blocked-by-auth。
- 全部票 done 仍须过 G-P0-Hard（T-06）。
