---
schema_version: 3
plan_contract_version: 1
plan_revision: 2
requested_deliverables: []
deliverable_policy: "用户未要求额外命名交付物数量；requested_deliverables 为空。本轮仅 SpecDev T-tickets Round4 定稿；不授权 I-implement；不翻 execution_authorization。"
artifact: tickets-map
change: 2026-09-12-wta-sso
status: blocked
---

# Tickets Map — 2026-09-12-wta-sso（Round4；正式 7 票；未授权实施）

- **Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`（Round4 `ready` / `ready_for_tickets=true`）
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/</Path>`
- **可选 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/goal-plan.md</Path>`
- **作废目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/superseded-r3/</Path>`（旧 6 票 + 旧 Evidence；不当完成）

> Round4（plan_revision=2）：正式 **7** 票已落盘；全部 `status: blocked` / `ready: false`（**blocked-by-auth**）。目标：双面管理 + 协议脊柱 + 三门 + AC-024。`ready_for_execution=false`。禁止造假票/假 Evidence 糊绿。旧 Round3 6 票已移 `superseded-r3/`，旧 done/Evidence 不当完成依据。

## 1. 目标与拆分策略

交付 **FirstPartySsoProvider** + Authorization Code/PKCE + `wta-sso`/`sso-web` + **独立 SSO 管理 / 客户端管理双面** + 双 App 接入，并以三门硬验收 + **AC-024 完成态门禁**收口。垂直切片：SSO 管理创建配置 → 客户端管理接入成功 → OAuth 模块 → sso-web 会话 → Provider/context → App 调用方 → 三门+AC-024。`ticket_workspace_policy=current` + `integration_gate=direct-parent` 串行。

### 总体实施背景

- 权威：CTO > ADR/CONTEXT > Spec > Goal Plan > 本 map > tickets。
- 产品冻结：FirstPartySsoProvider；三门并列 AC-001/002/003；D-200/201/202/203=A（DEC-200…203）；D-111=B 外部仅 SSO 管理登记+配置交付；D-116=B confidential 运行时后置；Code+PKCE S256；Token extras=目标业务 Client；authMode both|sso|local；禁 OIDC/SLO/独立进程/Keycloak·Casdoor·Logto·Hydra/共享 Token/Implicit/password/SAML；禁动 notify；禁把创建主路径塞回客户端管理；禁红色「没有接入」当完成。
- 本轮仅 T 定稿；**不翻** `execution_authorization`；**不授权** I；须 Lead Research 后另派 I。
- `ready_for_execution=false`。
- 写面仅本 change SpecDev；不碰已删 `ruoyi-vue-plus-docs`。

### 项目 Skill 读取矩阵

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | 架构、权限、API、质量门禁；任意产品/测试改动前 | Map 后、Ticket 前 | 硬约束与模块模式 |
| ALL | `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>` | 跨层 SSO/前后端合同垂直切片 | Map 后、Ticket 前 | 垂直切片与接缝 |
| T-03 | `<Path>.agents/skills/wta-module-guide/SKILL.md</Path>` | 新模块 `wta-sso` 落地与跨模块接入 | 进入 T-03 前 | 模块骨架与边界 |
| T-03 | `<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>` | common/SPI/安全工具入口选择 | 进入 T-03 后端票前 | 禁止误接外置 IdP SDK |
| T-06 T-07 | `<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>` | Origin/callback 矩阵消费与 E2E 运行时审计 | 进入 T-06/T-07 前 | 环境与回调可达 |

已扫描并排除：`java-api-compatibility`（本期不单独交付公开 Java API 兼容演进）、`project-customization-delivery`（用户未显式激活）。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/01-sso-admin-create-and-config.md</Path>` | 独立 SSO 管理 (a)(b)(c)；扩展 sys_client | auth | deep | high | no | unassigned | AC-017,018 | W-P0-Admin / G-Auth-I | blocked |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/02-client-admin-own-app-access-success.md</Path>` | 客户端管理自有接入成功态 | T-01 / auth | standard | high | no | unassigned | AC-019 | W-P0-Admin / G-Auth-I | blocked |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/03-wta-sso-oauth-pkce-module.md</Path>` | authorize/token/revoke+PKCE 负向+边界 | T-01 / auth | deep | critical | no | unassigned | AC-004…013,021,022 | W-P0-BE / G-Auth-I | blocked |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/04-sso-web-password-session-cookie.md</Path>` | sso-web 只密码+Set-Cookie | T-03 / auth | standard | high | no | unassigned | AC-016,020 | W-P0-FE / G-Auth-I | blocked |
| T-05 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/05-first-party-provider-context-authmode.md</Path>` | FirstParty 槽位+context+authMode | T-01,T-03 / auth | standard | high | no | unassigned | AC-001,014,015,023 | W-P0-FE / G-Auth-I | blocked |
| T-06 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/06-admin-home-platform-auth-adapters.md</Path>` | admin/home+platform/auth 接入 | T-02,T-04,T-05 / auth | standard | medium | no | unassigned | AC-001,014 | W-P0-Apps / G-Auth-I | blocked |
| T-07 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/07-three-gate-and-completion-gate.md</Path>` | 三门硬 E2E + AC-024 完成态 | T-06 / auth | deep | critical | no | unassigned | AC-001,002,003,024 | W-P0-E2E / G-P0-Hard | blocked |

Ticket frontmatter 为状态/依赖/路径权威；本表为投影。全部票另被 **blocked-by-auth**（本轮不翻授权、不派 I）全局阻塞。

## 3. 依赖 DAG

```text
T-01 [BLOCKED-by-auth]
  ├─→ T-02 [BLOCKED-by-auth]
  ├─→ T-03 [BLOCKED-by-auth]
  │     └─→ T-04 [BLOCKED-by-auth]
  └─→ T-05 [BLOCKED-by-auth]   (also waits T-03)
        └─→ (join T-02, T-04) → T-06 [BLOCKED-by-auth]
              └─→ T-07 [BLOCKED-by-auth]  # 三门 + AC-024
[Gate] G-Auth-I: Lead Research 后另派 I / 书面授权本 Round4 票集？
  └─ false: STOP（仅 SpecDev；不可开 I）
```

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-05, T-06, T-07 | 槽位/调用方/E2E | covered | P0-DEFAULT-PROVIDER-PATH |
| AC-002 | T-07 | 同浏览器复用 | covered | P0-SSO-REUSE |
| AC-003 | T-07 | clientid 互拒 | covered | P0-CLIENT-ISOLATION |
| AC-004 | T-03 | authorize 负向 | covered | PKCE challenge |
| AC-005 | T-03 | token 负向 | covered | verifier |
| AC-006 | T-03 | code 复用 | covered | 单次 |
| AC-007 | T-03 | TTL | covered | 过期 |
| AC-008 | T-03 | 绑定校验 | covered | client/redirect/challenge |
| AC-009 | T-03 | 白名单 | covered | 禁 `*` |
| AC-010 | T-03 | state CSRF | covered | |
| AC-011 | T-03 | 并发 | covered | 至多一成功 |
| AC-012 | T-03 | 日志 | covered | 无敏感字段 |
| AC-013 | T-03 | 模块边界 | covered | API/Port |
| AC-014 | T-05, T-06 | both+本地 login | covered | |
| AC-015 | T-05 | authMode=sso | covered | |
| AC-016 | T-04 | Set-Cookie | covered | |
| AC-017 | T-01 | 独立 SSO 管理 OpsFlow | covered | DEC-200 |
| AC-018 | T-01 | 外部登记 D-111=B | covered | SSO 管理 |
| AC-019 | T-02 | 自有接入成功态 | covered | 禁「没有接入」伪绿 |
| AC-020 | T-04 | sso-web 只密码 | covered | |
| AC-021 | T-03 | Token extras | covered | 非 sso |
| AC-022 | T-03 | revoke≠SLO | covered | |
| AC-023 | T-05 | Provider 预置/context | covered | |
| AC-024 | T-07 | Evidence 完成态门禁 | covered | 管理成功+接入成功+三门；**不得 deferred** |

无 uncovered；无 deferred。AC-001…024 **全 covered**。

## 5. 并行与路径所有权

- implementation subagent 上限：Goal Plan=2；**current 模式实际串行=1**。
- 共享路径：DDL/DML→T-01（T-03 可追加 code 表）；pom/application.yml→T-03；AuthController/LoginPage→T-05；platform/auth→T-06。
- **T-01 vs T-02 管理面分离**：T-01=独立 SSO 管理创建/配置；T-02=客户端管理接入成功态（非创建主路径）。
- **T-05 vs T-06**：AuthController/LoginPage owner=T-05；调用方/home 归 T-06。
- 与 notify change 路径隔离。
- 因全部 blocked/ready=false，无并发 Ready 写冲突；授权后仍按 DAG 串行。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-03 | DDL/DML | 是（T-03→T-01） | 串行；owner=T-01 |
| T-01 | T-02 | 无（管理面分离） | 是（T-02→T-01） | 串行 |
| T-03 | T-04 | wta-sso 会话包可能交叠 | 是（T-04→T-03） | 串行 |
| T-05 | T-06 | admin LoginPage 可能交叠 | 是（T-06→T-05） | 串行；LoginPage owner=T-05 |
| T-01 | T-05 | DML 种子 | 是（T-05→T-01） | 种子写尊重 T-01 owner |

## 6. Gate、Wave 与集成点

| Gate/Wave | 含义 |
|---|---|
| G-Tickets | 本轮：Round4 正式 7 票+map validate |
| G-Auth-I | Lead Research 后另派 I / 书面授权本票集前 **不可开 I** |
| G-P0-Hard | T-07 三门 + AC-024 Evidence |
| W-P0-Admin | T-01→T-02 |
| W-P0-BE | T-01→T-03 |
| W-P0-FE | T-04∥规划上接 T-05 |
| W-P0-Apps | T-06 |
| W-P0-E2E | T-07 |

编排权威仍见 Goal Plan；**Goal Plan 已回写 Round4（正式 7 票 / blocked）**，本 Map 为其票状态投影。

## 7. 横切契约与风险

- Token/Client 隔离不可放宽；禁伪 SSO。
- D-111=B 文档必须明示：外部登记≠ confidential 运行时已通。
- 环境矩阵占位未替换不得上线。
- 外脑旧 ChatGPT 句不作权威；本轮禁用 ChatGPT 外脑。
- 旧 Round3 done/Evidence / 红色「没有接入」截图不当完成。

## 8. 同步规则

- 以 Ticket frontmatter 为权威同步本表。
- Skill 矩阵变更后重新 validate。
- **Goal Plan 已回写 Round4**；Wave/Gate/owner 以 Goal Plan 为编排权威。
- 禁止相对 Markdown 链接；使用 Path 标签。

## 9. 总控与恢复

从本 Map 进入 `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>` 的 plan/run/resume/replan/verify。先运行 `<Path>{roots.workflows}/specdev/common/tools/ticket-control.mjs</Path>` 的 `--map` 只读检查。

- `requested_deliverables=[]`；无额外命名交付物。
- `ready_for_execution=false`；**不可开 I-implement**，直至 Lead Research 后另派 I 且票 ready。
- 恢复：读 Goal Plan、`.status.json`、Spec、本 Map、LOG、`superseded-r3/README.md`；确认仍 blocked-by-auth。
- 全部票 done 仍须过 G-P0-Hard（T-07，含 AC-024）。
