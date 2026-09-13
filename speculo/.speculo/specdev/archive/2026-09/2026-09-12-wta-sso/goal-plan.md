---
schema_version: 6
artifact: goal-plan
change: 2026-09-12-wta-sso
status: blocked
modes: [high-assurance]
orchestration: lead-directed
lead: rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00
implementation_agent_limit: 2
integration_attempt_limit: 3
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: WTA SSO（FirstPartySsoProvider · Round4 双面管理）

> **Authority:** CTO BRIEF t155u + CTO t297u 产品纠偏 + Round4 已拍（D-200…203 / D-100…116 / D-001…016）。  
> **Spec：** Round4 `ready` / `ready_for_tickets=true`。**正式 7 票**已落盘（全部 `blocked` / `ready=false`，**blocked-by-auth**）。  
> 本文件为 **replan（Round4）**：废止 Round3「正式 6 票」与「客户端管理页加 SSO 分组」作为创建主路径的施工投影。  
> `ready_for_execution=false`；文档要点**不**构成 I-implement 授权。旧 ChatGPT 外脑不作产品权威。本文件**不翻** `.status.json` 的 `execution_authorization`。

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`（Round4 Ready）
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`（正式 **7** 票；plan_revision=2；全部 blocked-by-auth）
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/</Path>`
- **作废目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/superseded-r3/</Path>`（旧 6 票 + 旧 Evidence；**不当完成**）
- **Source：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/source.md</Path>`（冻结 intake；不当现行合同）
- **Heavy 审阅：** `/workspace/share-research/reports/20260913-wta-sso-r4-heavy-review.md`（§2 P0：本回写）

> 权威链：**CTO 书面决定 > ADR/CONTEXT > Spec（ready）> 本 Goal Plan（编排）> tickets-map / tickets**。  
> Wave/Gate/owner 以本 Goal Plan 为编排权威；Ticket frontmatter 仍为单票状态/依赖/路径权威。

## 0. Plan mode and freeze

| 字段 | 值 |
|---|---|
| 模式 | `replan` → 落盘后仍为 `plan` 语义（仅规划；未授权实现） |
| change_status | `active` |
| current_work | `null`（P / G / S / T 已写入 works_run；本轮回写 Goal Plan） |
| Round | **4**（CTO t297u 产品枢轴后） |
| Tickets Map | 正式 **7** 票；全部 blocked-by-auth |
| `ready_for_execution` | **false**（schema：`status=blocked` ⇒ 必须 false） |
| 下一 Work | **等 Lead 书面收窄授权范围 = Round4 T-01…07 后再派 I**；**禁止**自启实现；**不得**继承 CTO-t275u 旧授权到新票集 |
| 禁止 | 改产品代码；push/PR；触碰 `2026-09-10-notify-channel-config`；造假票 / 假 Evidence；翻 `execution_authorization`；按旧 6 票或「客户端管理创建主路径」施工 |

## 1. Outcome and Authority

### Outcome

在 WTA-plus 的「第三方登录」体系中交付 **FirstPartySsoProvider**（自建、默认已接通、排序最前、默认路径可走通）：自有前端各 App + 外部系统 App 均可接入；同一套本仓账号密码多端登录；P0 只实现 Authorization Code + PKCE；换票后仍签发**目标业务 Client** 的现有 Sa-Token（账号归一 ≠ Token 共用）。

**Round4 产品枢轴（相对 Round3 必须遵守）：**

1. **独立「SSO 管理」**（DEC-200 / AC-017 / AC-018）：系统管理下创建应用 + 配置精确回调 + 获取/交付 client/密钥（自有与外部同一登记模型）。
2. **客户端管理仅自有 App 接入成功态**（DEC-201 / AC-019）：开关 / authMode / 可读 context；呈现可区分于红色「没有接入」的成功态。
3. **数据仍扩展 `sys_client`**（DEC-202=A / D-202=A），但 **UI 不得把创建/拿配置主路径塞回客户端管理**（DEC-203）。
4. **AC-024**：仅 Token 隔离、或管理面仍红色「没有接入」、或缺少 SSO 管理成功路径与接入成功态 → **不合格**，不得标完成。
5. 协议脊柱与三门硬验收（AC-001/002/003 + PKCE AC-004…012）**保留并列**。

P0 可观察结果（摘要）：

1. 协议：`OAuth 2.0 Authorization Code + PKCE (S256)`；禁 Implicit / password grant / SAML；OIDC → P1。身份真相源仍在本仓；禁 Keycloak / Casdoor / Logto / Hydra 外包用户目录。
2. 产品槽位：自建 SSO = 第三方登录目录第一提供方；Mask / GitHub 同目录其后。
3. 模块：`backend/wta-modules/wta-sso` + `frontend/apps/sso-web`；扩展 `sys_client`；短寿命 code/consent 可新表。
4. 管理面：独立 SSO 管理 (a)(b)(c) + 客户端管理自有接入成功；外部 = 登记 + 配置交付 + 对方自配（D-111=B）；confidential 运行时后置（D-116=B）。
5. 登录模式并存：`local` / `sso` / `both`；保留 `POST /auth/login`；默认 admin/home = `both`。
6. 工程不变量：同浏览器 SSO → admin-web → home-web；两 Token `clientid` 不同且互拒；第二次业务 App 授权不得再要本仓密码。

### Success and False Completion

**Success（本编排阶段）：** Round4 Ready Spec + 正式 7 票 + map 与本 Goal Plan 对齐；`ready_for_execution=false`；未改产品代码、未翻授权、未造假 Evidence；旧 Round3 6 票已移 `superseded-r3/` 且不当完成。

**False completion（禁止宣称完成 / 禁止按此施工）：**

- 按 **旧正式 6 票**（Round3）DAG / Wave 派工或宣称 done
- 把 **创建应用 / 拿配置** 继续塞进「客户端管理」或「客户端管理页加 SSO 分组」当**创建主路径**
- 把 `superseded-r3/` 旧 `status: done` / 旧 Evidence / 历史截图当 Round4 完成证据
- 管理面仍红色「**没有接入**」却标完成（违反 AC-019 / AC-024）
- **只验 Token 隔离**（AC-003）却宣称 SSO / 三门 / change 完成（缺 AC-001/002 + 双面管理成功态）
- 声称可执行 / 已上线 / 三门已通（无 Lead 对 Round4 T-01…07 的书面授权 + 无真 Evidence）
- 用根域 Cookie / 复用 Admin-Token 做伪 SSO；签发 extras=`sso` 中心 Client
- 把本仓用户目录外包给 Keycloak / Casdoor / Logto / Hydra
- 把本期写成「仅第一方 / NoExternalIdP」或「P0 禁止外部系统 App 接入」或「P0 已跑通外部 confidential」
- 采用 Implicit / password grant / SAML
- 造假 `ticket/*.md` 或 `evidence/T-*.md` 糊绿 validate
- 改 `2026-09-10-notify-channel-config` 或 push / PR
- **继承** `.status.json` 里 CTO-t275u 的 `implementation_commit=authorized` 到 Round4 新票集并开 I（字段残留 ≠ 本票集授权）

### Non-goals（本 change 规划边界）

- 本期不改业务/产品代码、不 push、不 PR；Round4 票集 **未获** Lead 书面收窄后的 I 授权
- P0 不做 OIDC discovery / id_token / userinfo、refresh、SLO、完整同意页（→ P1）
- P0 不把用户目录外包；不替换现有 Sa-Token；不关闭业务 App Header Bearer
- 外部系统 App P0 = SSO 管理可登记 + 配置交付（D-111=B）；confidential 运行时后置（D-116=B）
- social（Mask / GitHub）不重做、不拆除
- 独立 SSO 进程、MFA 收敛 → P2
- 不翻 `execution_authorization`（本文件与本派单）

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | CTO 书面决定（t155u / t297u / Round4 已拍） | 产品取舍与批准 | 更新真正拥有该决策的工件 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/CONTEXT.md</Path>` | 架构决定与领域语义（含 ADR-007 双面管理） | 返回 G-grill 更新真正 owner |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 已毕业永久决定 | change 替代时在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>` | 外部行为、范围与验收（Round4 Ready） | 下游不得改写 |
| 5 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/</Path>` + `tickets-map.md` | 单票契约与投影（正式 **7** 票） | Goal Plan 只编排；不以旧 6 票为准 |
| 6 | 冻结 source / 当前代码与运行事实 / Heavy 审阅 | 现状与可行性 | 冲突时触发偏差；工作树残留按 Round4 合同重验 |

**废止（编排面）：** Round3「正式 6 票」施工计划；「客户端管理页加 SSO 分组」作为创建主路径；`goal-plan-latest-snippets.md` 中 6 票投影（见过期横幅）；`superseded-r3/` 旧 done/Evidence。

## 2. Protocol and architecture snapshot

### Protocol selection

- **Adopt:** OAuth 2.0 Authorization Code + PKCE S256
- **Reject:** Implicit；password grant；SAML；伪 SSO（共享业务 Token）
- **Defer:** OIDC discovery / id_token / userinfo → **P1**；confidential 运行时 → D-116=B
- **IdP / 身份源：** 禁止外包用户目录；FirstPartySsoProvider 排第一；Mask/GitHub 同目录后续
- **Sa-Token：** `access_token` = 现有 Sa-Token；extras = **目标业务 Client**（禁止 `sso` 中心 Client）

### Key invariants

1. TokenInvariant / LoginNormalization / ThirdPartySlotFirst / TwoLayerSession
2. SingleAppDirectory：扩展 `sys_client`；**UI 主路径 = 独立 SSO 管理**
3. SsoAdminDualSurface：SSO 管理 = 创建/拿配置；客户端管理 = 自有接入；外部自配置
4. NoFakeDoneOnNoAccess：「没有接入」红色态 ≠ 完成（AC-024）
5. `client_secret` ≠ OAuth 密钥；用 `sso_secret_hash`；明文只一次
6. 本地登录并存：`POST /auth/login`；`local` / `sso` / `both`

### Module landing（对齐 Round4 Spec；禁止旧主路径表述）

| 位置 | 作用 |
|---|---|
| 系统管理 · **独立「SSO 管理」** UI/API | **创建主路径**：(a) 创建应用 (b) 精确回调 (c) 拿配置；覆盖自有+外部登记（AC-017/018；T-01） |
| `frontend/packages/web-domains/system/src/client`（ClientPage 等） | **仅**自有 App SSO **接入**控件直至接入成功态（AC-019；T-02）；**禁止**再当创建/拿配置主路径 |
| `backend/wta-modules/wta-system` · `SysClient` + DDL/DML | 扩展 `sys_client` SSO 字段；T-01 owner |
| `backend/wta-modules/wta-sso` | authorize / token / revoke + PKCE 负向 + 模块边界（T-03） |
| `frontend/apps/sso-web` | ClientId=`sso`；只本仓密码；后端 Set-Cookie（T-04） |
| AuthController / LoginPage / Provider 种子 | FirstParty 槽位 + context + authMode（T-05） |
| `packages/platform/auth` + adapters + admin-web / home-web | 纯合同 + 浏览器 adapters + 双 App 接入（T-06） |
| `frontend/e2e/` + Evidence | 三门 + AC-024 完成态门禁（T-07） |

`GET /auth/client/context` 扩展：`ssoEnabled` / `ssoAuthorizeUrl` / `authMode`。

### Phasing

| Phase | Scope |
|---|---|
| **P0** | 独立 SSO 管理 + 自有接入成功 + Code/PKCE + sso-web + Provider/context + admin/home + 三门 + AC-024 |
| **P1** | OIDC / refresh / SLO / 同意页 |
| **P2** | 独立进程、MFA 收敛 |

### P0 hard acceptance

- **三门并列（AC-001/002/003）** + **双面管理成功态（AC-017/018/019）** + **完成态门禁（AC-024）**；缺一不可宣称 P0 / change 完成。
- 外部 App：P0 只验收 SSO 管理登记+配置交付；不宣称 confidential Code 流已通。

## 3. Execution Graph（Round4 正式 7 票；全部 blocked-by-auth）

### DAG and Critical Path（权威）

```text
T-01 → T-02
T-01 → T-03 → T-04
T-01 + T-03 → T-05
T-02 + T-04 + T-05 → T-06 → T-07

# 语义：
# T-01 独立 SSO 管理创建配置（AC-017/018）
# T-02 客户端自有接入成功（AC-019）
# T-03 wta-sso OAuth/PKCE（AC-004…013,021,022）
# T-04 sso-web Cookie（AC-016,020）
# T-05 Provider/context/authMode（AC-001,014,015,023）
# T-06 admin/home + platform/auth adapters（AC-001,014）
# T-07 三门 + AC-024（AC-001,002,003,024）

[Done] P → G (r3+r4) → S-spec Round4 → T-tickets Round4（7 票）
[Gate] G-Auth-I: Lead Research 后另派 I / 书面授权范围 = Round4 T-01…07？
  └─ false: STOP（当前态；不可开 I；不继承 CTO-t275u）
```

**Work routing（历史）：** P-goal-plan → G-grill（含 Round4 D-200…203）→ S-spec Round4 → T-tickets Round4 → **本 Goal Plan replan** →（待书面授权）I-implement（current / direct-parent 串行）。

### Waves and Ownership（由上述 DAG 投影；Wave ≠ 并发写授权）

| Wave | Ticket | 前置条件 | 项目写路径（摘要） | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W-P0-Admin | T-01 | G-Auth-I 书面授权本票集 | 独立 SSO 管理 UI/API；SysClient+DDL/DML | T-01 | G-Auth-I → 串行 #1 |
| W-P0-Admin | T-02 | T-01 | ClientPage 接入控件（非创建主路径） | T-02 | 串行 #2 |
| W-P0-BE | T-03 | T-01 | `wta-sso` 模块；可追加 code 表（DDL owner 仍 T-01） | T-03 | 串行 #3 |
| W-P0-FE | T-04 | T-03 | `sso-web` + SSO 域会话 Cookie | T-04 | 串行 #4 |
| W-P0-FE | T-05 | T-01 + T-03 | Provider 槽位；AuthController/LoginPage；context/authMode | T-05（LoginPage owner） | 串行 #5 |
| W-P0-Apps | T-06 | T-02 + T-04 + T-05 | platform/auth + admin/home adapters | T-06 | 串行 #6 |
| W-P0-E2E | T-07 | T-06 | e2e + Evidence（三门+AC-024） | Lead / T-07 | G-P0-Hard |

> `ticket_workspace_policy=current`：**严格串行**，单一 implementation writer。Wave 标签仅便于汇报；**不等于**可并发写。全部票另被 **blocked-by-auth** 全局阻塞，直至 Lead 书面收窄授权。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 独立 SSO 管理 (a)(b)(c)；扩展 sys_client；外部可登记 | —（+ auth） | `current` | Lead / dynamic（现 unassigned） | not-required: 管理 UI/API；完成态由 T-07 汇合 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-01.md</Path>` |
| T-02 | 客户端管理自有接入成功态（禁「没有接入」伪绿） | T-01 | `current` | Lead / dynamic | not-required: 接入态 UI；T-07 复核 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-02.md</Path>` |
| T-03 | authorize/token/revoke + PKCE 负向 + 模块边界 | T-01 | `current` | Lead / dynamic | not-required: API/集成；T-07 消费 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-03.md</Path>` |
| T-04 | sso-web 只密码 + 后端 Set-Cookie | T-03 | `current` | Lead / dynamic | not-required: Cookie/UI；T-07 消费 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-04.md</Path>` |
| T-05 | FirstParty 槽位 + context + authMode | T-01, T-03 | `current` | Lead / dynamic | not-required: 槽位/both；T-07 汇合 AC-001 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-05.md</Path>` |
| T-06 | admin/home 经 platform/auth 接入 | T-02, T-04, T-05 | `current` | Lead / dynamic | not-required: 调用方接通；T-07 硬验 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-06.md</Path>` |
| T-07 | 三门硬 E2E + AC-024 完成态门禁 | T-06 | `current` | Lead / dynamic | **required**：同浏览器三门 + SSO 管理成功 + 接入成功；仅隔离或红「没有接入」→ 不得 done | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-07.md</Path>` |

权威依赖/状态投影见 `tickets-map.md` §2–§3；全部 `status: blocked` / `ready: false` / `owner: unassigned`。

## 4. Gates and Completion Evidence

### Overall Definition of Done（change 级）

1. Round4 Ready Spec 覆盖双面管理 + 三门 + AC-024（**已满足**）
2. 正式 7 票落盘；Skill 绑定真实；map validate 绿（**已满足；仍 blocked-by-auth**）
3. 本 Goal Plan 与 Spec/map/7 票 DAG 一致（**本回写**）
4. Lead **另派**书面授权且范围 = **Round4 T-01…07** 后，Goal Plan 方可考虑 `ready_for_execution=true`（须同步 status 规则）；**不得**把 CTO-t275u 字段残留当本票集授权
5. T-07 Evidence 可回读：SSO 管理 (a)(b)(c) 成功 + 自有接入成功 + 三门同场；禁止降级
6. 未把用户目录外包；未采用禁用 grant；未动 notify

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G-Grill | P-goal-plan | ADR/CONTEXT；Round4 D-200…203 | S 不得臆造 | Lead | 回 Grill · **done** |
| G-Spec | Grill+CTO | Round4 Ready Spec | T-tickets | Lead | 回 Spec · **done** |
| G-Tickets | Ready Spec | 正式 7 票+map validate | I-implement | Lead | 补票 · **done（blocked-by-auth）** |
| G-Goal-Replan | Round4 票落盘 + Heavy §2 P0 | 本 Goal Plan 与 7 票 DAG 一致 | 以本文件为 Wave 权威前 | Lead / RVP·规划 | 继续回写 · **closing** |
| G-Auth-I | Lead Research 后**书面**声明授权范围=Round4 T-01…07 | 书面派单 + 票 ready；**不**自动继承 CTO-t275u | 一切产品写 / 开 I | Lead（+ CTO 若要求） | 停实现 · **open** |
| G-P0-Hard | 实现完成且已授权 | T-07：三门 + AC-024 Evidence | change complete | Lead | 修或偏差；禁伪绿 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-017/018 独立 SSO 管理 + 外部登记 | T-01 | 管理 UI/API | T-01 → T-07 汇合 | covered |
| AC-019 自有接入成功态 | T-02 | 客户端管理 UI | T-02 → T-07 | covered |
| AC-004…013,021,022 PKCE/边界/extras/revoke | T-03 | API/集成/边界 | T-03 | covered |
| AC-016/020 Cookie + sso-web 只密码 | T-04 | 浏览器/UI | T-04 | covered |
| AC-001/014/015/023 Provider+context+authMode | T-05（+T-06） | 槽位/login | T-05/T-06 | covered |
| AC-001/014 双 App 接入 | T-06 | 调用方 | T-06 | covered |
| AC-001/002/003 三门 + AC-024 完成态 | T-07 | E2E + Evidence 评审 | T-07 | covered（**不得 deferred**） |

## 5. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00` | `.status.json` leadership.current |
| Implementation subagents | `2`（≤ config `max_implementation_agents=3`） | 自 config 快照；可降不可升；current 实际串行=1 |
| Integration attempts | `3` | 自 config `max_integration_attempts=3` 快照 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation |
| Dispatch | execution-time dynamic | 按 Ticket 事实选择 |
| Workspace | `current` + `direct-parent` | 严格串行；不建 source/candidate worktree |

### Ticket Workspace and Integration

当 `ticket_workspace_policy: current` 时：每次只允许一个 implementation owner 写入；完成非 E2E 检查并形成 commit 后，Lead 在同一父分支运行适用集成检查和 E2E（T-07 required），再开始下一票。不得创建 source/candidate worktree。

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01…T-07 | main / current | current（串行） | 票级 | 每票必需（授权后） | T-07 required E2E；其余按票 | result_sha=implementation commit |

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| SpecDev 规划工件（本 change） | allowed | 仅 `2026-09-12-wta-sso`；含本 Goal Plan Round4 回写 |
| Current workspace Ticket changes | **not-authorized** | **I 须 Lead 另派书面授权，范围 = Round4 T-01…07**；**不得继承** `.status.json` 里 CTO-t275u 旧 `implementation_commit=authorized` 到新票集 |
| Ticket worktree local changes | not-authorized | 本计划为 current 模式 |
| Implementation commit | **not-authorized**（对本 Round4 票集） | 字段字面或有旧 authorized 残留；**操作权威** = blockers + 7 票 blocked-by-auth + DEC-022 + 本 Matrix。本文件**不翻**授权字段 |
| Local direct-parent verification and parent update | not-authorized（对本票集） | 同上；须书面收窄后再谈 |
| Local candidate integration and parent update | not-authorized | 本计划不用 candidate-merge |
| Push / PR / remote merge | not-authorized | 不从本计划本地授权继承 |
| Branch/worktree cleanup | not-authorized | 成功集成不自动继承 |
| Deploy / migration / production actions | not-authorized | 逐动作；环境矩阵见 D-002 |
| 触碰 `2026-09-10-notify-channel-config` | not-authorized | 永久禁止本派单 |
| 声称外脑已通过 / 按旧 6 票施工 | not-authorized | Round3 废止；Heavy §2 P0 |

**授权范围声明（原文口径，供 Lead 派 I 时照抄收窄）：**

> **I 须 Lead 另派书面授权，范围 = Round4 T-01…07；不得继承 `.status.json` 里 CTO-t275u 旧 `implementation_commit=authorized` 到新票集；本文件不翻 `execution_authorization`。**

### Evidence Return

subagent 只返回候选事实与 commit；Lead 独立核对并写 Evidence、状态和最终验收。`superseded-r3/` 与历史 `evidence/*.png`（客户端管理错位截图）**不当** Round4 完成证据。本轮无新实现 Evidence。

## 6. Constraints, Risk and Recovery

### Non-negotiable Constraints

- Round4 双面管理：独立 SSO 管理创建；客户端管理仅自有接入；禁创建主路径回落
- 禁止外包用户目录；禁 Implicit / password grant / SAML
- `access_token` = 现有 Sa-Token；extras = 目标业务 Client
- 扩展 `sys_client`；不平行再建 OAuth 应用主目录；UI 仍独立
- 回调白名单精确匹配，禁止 `*`
- Cookie 仅 SSO 域；业务 App Header Bearer
- 本轮零产品代码；零 push/PR；不翻 execution_authorization
- 不伪造 Tickets / Evidence；不按旧 6 票施工

### Verification Integrity

- 规划阶段：工件回读 + `validate-specdev` / `ticket-control --map`；不得靠假 Evidence 变绿
- 实现阶段（未来）：判卷含 SSO 管理成功路径、接入成功态、authorize/token/revoke、三门；禁止删测试或放宽 Client 隔离
- current/direct-parent：严格串行
- AC-024：仅隔离绿或红「没有接入」= 不合格

### Migration or Release Sequence

1. DDL：扩展 `sys_client` + code/consent 等（T-01 owner；T-03 可追加）
2. 独立 SSO 管理菜单/API（T-01）→ 自有接入成功态（T-02）
3. `wta-sso`（T-03）→ `sso-web`（T-04）→ Provider/context（T-05）→ admin/home（T-06）
4. T-07 三门 + AC-024；发布窗口仅在 G-Auth-I 书面授权且 Evidence 可回读后谈

### Risks, Monitoring and Recovery

| 风险 | 缓解 |
|---|---|
| 按过期 Goal Plan 6 票 DAG 施工 | 本 Round4 回写；map §8 以本文件为编排权威 |
| 继承 CTO-t275u 旧授权开 I | Authorization Matrix + blockers；须书面收窄 |
| 伪 SSO / 只验隔离 / 「没有接入」伪绿 | AC-024 + T-07；superseded-r3 切断 |
| 工作树残留（ClientPage SSO 分组、半成品 wta-sso/sso-web） | 授权后按 Round4 合同重验，禁止把旧实现当 done |
| 与 notify change 并发 | 写集隔离；禁止改 notify |

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。

## 7. Progress and Decisions

### Current Status

| Item | State |
|---|---|
| Change | `2026-09-12-wta-sso` · active |
| G-grill | **done** · design-tree consensus（含 Round4） |
| S-spec | **ready** Round4（LOG-039） |
| T-tickets | **formal 7** · 全部 blocked-by-auth（LOG-040）；旧 6 票在 `superseded-r3/` |
| Goal Plan | **Round4 回写完成**（本文件）；`status=blocked`；`ready_for_execution=false` |
| Implementation（Round4 票集） | **not authorized**（须 Lead 另派书面范围=T-01…07）；字段残留不继承 |
| Heavy 审阅 | PASS_WITH_NOTES；§2 P0 本项关闭中 |
| Plan Quality Review | 背景/边界、控制图、验收、权限、恢复均可判定；执行授权轴 **blocked（预期）** |

### Pending Decisions and Blockers

- 产品 Pending：**无**（Round4 D-200…203 已锁）
- **Blocker：** blocked-by-auth — 等 Lead Research（Heavy 已出）后**另派 I**，书面收窄授权到 Round4 T-01…07
- **不**自启 I；**不**把 `ready_for_execution` 写 true；**不**翻 `execution_authorization`

### Resume Protocol

恢复时读取：本 Goal Plan、`.status.json`（只读授权字段）、`spec.md`、`tickets-map.md`、LOG-039/040/本 LOG、`superseded-r3/README.md`、Heavy 报告。确认仍 blocked-by-auth。**不要**从 G-grill 重开。未获书面 Round4 范围授权不得把票标 ready / in_progress，不得写产品树，不得造假 Evidence，不得按旧 6 票施工。

## Assumptions

- config 快照：`max_implementation_agents=3` → 本计划 `implementation_agent_limit=2`；`max_integration_attempts=3`。
- 环境矩阵字面量可占位（D-002）；authMode 默认与生产形态已锁（D-003 / D-004）。
- 现有 admin/home Client 隔离与 Sa-Token extras 行为与 Spec 引用代码一致（实现前代码回读复核）。
- `ready_for_execution=false` 因 **Round4 票集未获书面收窄授权**（及 schema status=blocked），不是因为高影响产品假设未拍。
- `.status.json` 中 CTO-t275u `authorized` 为枢轴前残留；与 blockers / DEC-022 / 本 Matrix 并存时，**以「未授权本票集」操作为准**。
