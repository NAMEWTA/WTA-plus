---
schema_version: 6
artifact: goal-plan
change: 2026-09-12-wta-sso
status: draft
modes: [high-assurance]
orchestration: lead-directed
lead: rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00
implementation_agent_limit: 2
integration_attempt_limit: 3
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: WTA SSO（OAuth2 Authorization Code + PKCE）

> **Authority:** CTO BRIEF 2026-09-13 t155u + Round3 已拍（D-100…116 / D-001…016）。**S-spec 已定稿**（`spec.md` status=`ready`）；**正式 6 票已落盘**（全部 `blocked` / `ready=false`，blocked-by-auth）。仍 `ready_for_execution=false`；`implementation_commit=not-authorized`。
> 文档中的方案要点**不**构成 I-implement 授权。旧 ChatGPT `external-brain/reply.md` 不作产品权威。

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`（S-spec 已定稿；`ready_for_tickets=true`）
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`（正式 6 票；全部 blocked-by-auth）
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/</Path>`
- **Source：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/source.md</Path>`
- **External brain：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/external-brain/</Path>`

> Authority: **CTO 书面决定 > ADR/CONTEXT > Spec（ready）> 本 Goal Plan（编排）> tickets-map / tickets**。  
> `ready_for_execution=false`；文档中的方案要点**不**构成 I-implement 授权。  
> 旧 ChatGPT 外脑 reply 不作产品权威；本轮外脑 = Research / Grok Heavy。

## 0. Plan mode and freeze

| 字段 | 值 |
|---|---|
| 模式 | `plan`（仅规划；未授权实现） |
| change_status | `active` |
| current_work | `null`（P / G / S / T 已写入 works_run） |
| 下一 Work | 待命 I-implement（须 CTO 书面翻转 `implementation_commit`）；**禁止**自启实现 |
| Tickets Map | 正式 6 票已落盘；全部 blocked-by-auth |
| Baseline | CTO intake `b06d161`；冻结时 HEAD `d1ce372`；Lead 现引 `677d9a9`（冻结声明可能过时，以仓内实际 HEAD 为准） |
| 禁止 | 改产品代码；push/PR；触碰 `2026-09-10-notify-channel-config`；造假票 / 假 Evidence |

## 1. Outcome

在 WTA-plus 的「第三方登录」体系中交付 **FirstPartySsoProvider**（自建、默认已接通、排序最前、默认路径可走通）：自有前端各 App + 外部系统 App 均可接入；同一套本仓账号密码多端登录；P0 只实现 Authorization Code + PKCE；换票后仍签发**目标业务 Client** 的现有 Sa-Token（账号归一 ≠ Token 共用）。

P0 可观察结果：

1. 协议：`OAuth 2.0 Authorization Code + PKCE (S256)`；禁 Implicit / password grant / SAML；OIDC → P1。身份真相源仍在本仓，不引入 Keycloak / Casdoor / Logto / Hydra 替代用户目录。
2. 产品槽位：自建 SSO 作为第三方登录目录第一提供方，默认已接通、默认路径可走通；Mask / GitHub 等同目录其后，不互斥。
3. 模块：`backend/wta-modules/wta-sso` + `frontend/apps/sso-web`；扩展 `sys_client` 为一层应用目录（不另建平行 OAuth 应用表）。
4. 接入流程：(a) 创建应用 (b) 精确回调白名单（禁 `*`） (c) 交付 client/密钥；自有 App 可直接读配置（`/auth/client/context` 或等价面）。
5. 登录模式并存：按 Client 配置 `local` / `sso` / `both`；保留 `POST /auth/login`。
6. 工程不变量：同一浏览器 SSO → `admin-web` → `home-web`；两张 Token 的 `clientid` **必须不同**，互打接口 **必须被拒**；第二次业务 App 授权不得再要本仓密码（SSO-REUSE）。
7. 产品硬验收（D-115=A，与工程两门并列）：默认第三方登录入口不经额外开通即可走完授权码主路径（`P0-DEFAULT-PROVIDER-PATH` / AC-001）。


### Success and False Completion

**Success（本阶段）：** Ready Spec 覆盖 P0 行为与三门硬 AC；正式票 T-01…06 + map 已落盘且全部 blocked-by-auth；`ready_for_execution=false`；未改产品代码、未造假 Evidence。

**False completion（禁止宣称完成）：**

- 声称可执行 / 已上线 / 三门已通（无授权实施 + 无真 Evidence）
- 用根域 Cookie / 复用 Admin-Token 做伪 SSO
- 签发 extras=`sso` 中心 Client 而非目标业务 Client
- 把本仓用户目录外包给 Keycloak / Casdoor / Logto / Hydra
- 把「禁止外包身份源」写成「系统不得存在任何第三方登录」
- 把本期写成「仅第一方 / NoExternalIdP」或「P0 禁止外部系统 App 接入」或「P0 已跑通外部 confidential」
- 采用 Implicit / password grant / SAML
- 造假 `ticket/*.md` 或 `evidence/T-*.md` 糊绿 validate
- 改 `2026-09-10-notify-channel-config` 或推送 / PR


### Non-goals（本 change 规划边界）

- 本期不改业务/产品代码、不 push、不 PR；I-implement 未授权
- P0 不做 OIDC discovery / id_token / userinfo、refresh、SLO、完整同意页（→ P1）
- P0 不把用户目录外包给外置 IdP 产品；不替换现有 Sa-Token；不关闭业务 App Header Bearer
- 外部系统 App P0 = 管理面可登记（D-111=B）；confidential token 鉴权运行时后置（D-116=B）。禁止写成「P0 不做外部第三方」，也禁止宣称 P0 已跑通外部 Code 流
- social IdP（Mask / GitHub）已存在，P0 不重做、不拆除；自建 SSO 进入同一目录并排第一
- 独立 SSO 进程、MFA 收敛到 SSO → P2


### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | CTO 书面决定（含 Round3 已拍） | 产品取舍与批准 | 更新真正拥有该决策的工件 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/CONTEXT.md</Path>` | 当前 change 架构决定与领域语义 | 返回 G-grill 更新真正 owner（仅当高影响产品句冲突） |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 已毕业永久决定 | change 替代时在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>` | 外部行为、范围与验收 | 下游不得改写（权威 Ready Spec） |
| 5 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/</Path>` | 单 Ticket 契约 | Goal Plan 只编排；正式票已落盘（blocked-by-auth） |
| 6 | 冻结 source / 当前代码与运行事实 | 现状与可行性 | 冲突时触发偏差并返回真正 owner |

## 2. Protocol and architecture snapshot（from intake）

### Protocol selection

- **Adopt:** OAuth 2.0 Authorization Code + PKCE S256
- **Reject:** Implicit；把现有密码登录包装成 OAuth password grant；SAML
- **Defer:** OIDC discovery / id_token / userinfo → **P1**
- **IdP / 身份源：** 禁止把本仓用户目录外包/替换为 Keycloak / Casdoor / Logto / Hydra。已有 social（Mask / GitHub）同目录后续槽位，不拆除、不互斥。自建 SSO = FirstPartySsoProvider，排第一。
- **Sa-Token：** 可参考 `sa-token-oauth2`，但不可让其拥有 Client / Token / 用户

### Key invariants

1. **不替换 Token**：`access_token` = 现有 WTA Sa-Token
2. **签发 extras 必须是目标业务 Client**，不能是 `sso` 中心 Client
3. **两层会话**：SSO 域名才允许 HttpOnly Cookie；业务 App 继续 Header Bearer + 自己的存储命名空间
4. **一层应用目录**：P0 扩展 `sys_client`；新表只放一次性 code / refresh / consent
5. **现有 `client_secret` ≠ OAuth 密钥**：新增 `sso_secret_hash`，明文只显示一次，可轮换
6. **本地登录并存**：`POST /auth/login` 保留；`local` / `sso` / `both`

### Module landing

| 位置 | 作用 |
|---|---|
| `backend/wta-modules/wta-sso` | 新模块，Controller → UseCase → Service → DAO |
| `wta-admin` 默认组装 | `namewta.sso.enabled` 开关 |
| `frontend/apps/sso-web` | ClientId=`sso`，会话键=`Sso-Token` |
| `packages/platform/auth` | `startSsoLogin()` / `handleCallback()` |
| `packages/web-domains/system/src/client` | 客户端管理页加 SSO 分组 |
| `50-namewta-ddl.sql` / `60-namewta-dml.sql` | 基座改表和种子 |

`GET /auth/client/context` 扩展：`ssoEnabled` / `ssoAuthorizeUrl` / `authMode`。

### Phasing

| Phase | Scope |
|---|---|
| **P0** | Authorization Code + PKCE + `sso-web` + 默认提供方槽位接通 + 应用接入 (a)(b)(c) + Client 管理扩展 + admin/home 可走通 + 本地登录并存 |
| **P1** | OIDC discovery / id_token / userinfo、refresh、SLO、同意页 |
| **P2** | 独立进程、MFA 收敛 |


### P0 hard acceptance

- **三门并列（缺一不可宣称 P0 完成）：**
  - `P0-DEFAULT-PROVIDER-PATH`（AC-001）：登录页第三方区第一按钮 → `sso-web` 授权码主路径可走通；同一套本仓账号可在 admin-web 与 home-web 完成登录。
  - `P0-SSO-REUSE`（AC-002）：同浏览器第二次业务 App 授权不得再要本仓密码。
  - `P0-CLIENT-ISOLATION`（AC-003）：admin/home Token `clientid` 不同且互拒。
- **目标句：** 登录归一化——同一套本仓账号密码，多端登录；账号归一 ≠ Token 共用。
- **外部 App：** P0 只验收管理面登记（D-111=B / AC-018）；confidential 运行时后置（D-116=B / AC-019）。


## 3. Execution Graph（P/G/S/T 已跑完；待命 I）

### DAG and Critical Path（Work routing）

```text
[Done] P-goal-plan
  → [Done] G-grill-with-docs  （design-tree consensus round=3）
  → [Done] S-spec             （spec.md ready / ready_for_tickets=true；LOG-032）
  → [Done] T-tickets          （正式 6 票 + map；全部 blocked-by-auth；LOG-033）
  → [Gate] CTO + Lead: implementation_authorized?
        ├─ false: STOP（仅 SpecDev；当前态）
        └─ true: I-implement（current / direct-parent 串行）
             → C-code-review / verify
```

### Waves and Ownership（正式票投影）

| Wave | 内容 | 前置 | Shared owner |
|---|---|---|---|
| W-Grill | 访谈 / ADR accepted | P-goal-plan | Lead · **done** |
| W-Spec | Ready Spec + AC（含三门硬验收） | G-grill + Round3 | Lead · **done** |
| W-Tickets | 正式票拆分 T-01…06 | Ready Spec | Lead · **done**（blocked-by-auth） |
| W-P0-BE | `wta-sso` + DDL/DML + sys_client 扩展（T-01→T-02） | 实现授权 | 动态派单（串行） |
| W-P0-FE | `sso-web` + platform/auth + client 管理页（T-03/T-04） | BE 合同稳定 Gate | 动态派单（串行） |
| W-P0-Apps | admin/home SSO 入口与 authMode（T-05） | FE SDK | 动态派单（串行） |
| W-P0-E2E | 三门硬验收场景证据（T-06） | Apps | Lead |

### Ticket Quick Reference

| ID | 可观察产出 | 状态 |
|---|---|---|
| T-01 | OpsFlow+外部登记+confidential 字段可建 | blocked / ready=false |
| T-02 | authorize/token/revoke+PKCE 负向+边界 | blocked / ready=false |
| T-03 | sso-web 只密码+Set-Cookie | blocked / ready=false |
| T-04 | FirstParty 槽位+context+authMode | blocked / ready=false |
| T-05 | admin/home+platform/auth 接入 | blocked / ready=false |
| T-06 | 三门硬 E2E 证据 | blocked / ready=false |

权威投影见 `tickets-map.md` §2；全部另被 `implementation_commit=not-authorized` 全局阻塞。

## 4. Gates and Completion Evidence

### Overall Definition of Done（change 级；非本轮）

1. Ready Spec 覆盖 P0 行为与三门硬验收 AC（**已满足**）
2. 正式 Tickets 落盘；Skill 绑定真实项目 Skill（**已落盘；仍 blocked-by-auth**）
3. Round3 高影响决策已书面关闭；低影响字面量进环境矩阵（**已满足**）
4. `execution_authorization.implementation_commit=authorized` 且 Goal Plan `ready_for_execution=true` 后才实现
5. P0 三门同场 Evidence 可回读（AC-001/002/003）；禁止降级为「只验隔离」
6. 未把用户目录外包给外置 IdP 产品；未采用禁用 grant

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G-Grill | P-goal-plan draft | ADR/CONTEXT 对齐；开放问题记录 | S-spec 不得臆造 CTO 答 | Lead | 回 Grill · **done** |
| G-CTO-Q | Grill 草案 | CTO 书面答 Round3 | 域名/authMode/部署相关票 | CTO | 保持 draft · **done** |
| G-Spec | Grill+足够 CTO | Ready Spec | T-tickets | Lead | 回 Spec · **done** |
| G-Tickets | Ready Spec | 正式票+map validate | I-implement | Lead | 补票 · **done（blocked-by-auth）** |
| G-Auth-I | CTO/用户书面 | status 授权字段翻转 | 一切产品写 | CTO/用户 | 停实现 · **open** |
| G-P0-Hard | 实现完成且已授权 | 同浏览器三门同场 Evidence：AC-001 默认路径 + AC-002 第二次不要密码 + AC-003 clientid 互拒 | change complete | Lead | 修或偏差；禁止降级为「只验隔离」 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 | 状态 |
|---|---|---|
| OAuth2 Code + PKCE S256 | T-02 + spec AC-004…012 | covered |
| access_token = Sa-Token + 目标 Client extras | T-02 + AC-021 | covered |
| local/sso/both 并存 | T-04/T-05 + AC-014/015 | covered |
| P0 三门硬验收（DEFAULT-PROVIDER-PATH / SSO-REUSE / CLIENT-ISOLATION） | T-06 + spec AC-001/002/003 | covered |
| 扩展 sys_client / sso_secret_hash | T-01 + AC-017/018/019 | covered |

## 5. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00` | 唯一 SpecDev 状态、Evidence 与父分支 owner |
| Implementation subagents | `2`（≤ config `max_implementation_agents=3`） | Goal Plan 快照；可降不可升超 config |
| Integration attempts | `3` | 自 config `max_integration_attempts` 快照 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation，不写状态 |
| Dispatch | execution-time dynamic | provider/模型/派单按 Ticket 事实选择 |
| Workspace | `current` + `direct-parent` | 严格串行；不建 source/candidate worktree |

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| SpecDev 规划工件（本 change） | allowed | 仅 `2026-09-12-wta-sso` 命名空间 |
| Current workspace Ticket changes | not-authorized | 待授权 + Ready Tickets |
| Ticket worktree local changes | not-authorized | 本计划为 current 模式；不启用 required |
| Implementation commit | not-authorized | 缺失则 Plan blocked（预期） |
| Local direct-parent verification and parent update | not-authorized | 待授权 |
| Local candidate integration and parent update | not-authorized | 本计划不用 candidate-merge |
| Push / PR / remote merge | not-authorized | 不从本计划本地授权继承 |
| Branch/worktree cleanup | not-authorized | 成功集成不自动继承 |
| Deploy / migration / production actions | not-authorized | 逐动作、目标和条件；生产域名见环境矩阵（D-002/D-004） |
| 触碰 `2026-09-10-notify-channel-config` | not-authorized | 永久禁止本派单 |
| 声称外脑已通过 | not-authorized | 旧 ChatGPT reply 不作权威 |

### Evidence Return

subagent 只返回候选事实与 commit；Lead 独立核对并写 Evidence、状态和最终验收。本轮无实现 Evidence。

## 6. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 禁止把本仓用户目录外包给外置 IdP 产品；禁 Implicit / password grant / SAML
- `access_token` 必须是现有 Sa-Token；extras = 目标业务 Client
- 扩展 `sys_client`；不平行再建 OAuth 应用主目录
- `client_secret` 不复用为 OAuth 密钥；用 `sso_secret_hash`
- 回调白名单精确匹配，禁止 `*`
- Cookie 仅 SSO 域；业务 App Header Bearer
- 本轮零产品代码；零 push/PR
- 不伪造 Tickets / Evidence 糊 validate

### Verification Integrity

- 规划阶段以工件回读 + `validate-specdev` 为准；正式票已落盘但仍 blocked-by-auth，不得靠假 Evidence 变绿
- 实现阶段（未来）：判卷接缝含 authorize/token/revoke、client context、三门硬验收；禁止删测试或放宽 Client 隔离
- current/direct-parent：严格串行，单一 implementation writer

### Migration or Release Sequence

1. DDL：扩展 `sys_client` + code/consent 等新表 → 种子 SSO Client
2. 后端 `wta-sso` 开关默认安全（`namewta.sso.enabled`）
3. `sso-web` 与 platform auth 辅助
4. 默认 admin/home `authMode=both`（D-003）；生产同进程 + `sso-web` 独立 Origin（D-004）；环境矩阵可占位，上线前填真值（D-002）
5. 发布窗口仅在 G-Auth-I 授权且三门 Evidence 可回读后谈

### Risks, Monitoring and Recovery

| 风险 | 缓解 |
|---|---|
| 伪 SSO（共享 Token）破坏 Client 隔离 | 三门硬验收；SecurityConfig clientid 校验保持 |
| 未授权就开写 | blockers + ready_for_execution=false |
| 规划面过期语义误导实施者 | 2026-09-13 文档收束：只写最新态 |
| 与 notify change 并发 | 写集隔离；禁止改 notify change |

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。

## 7. Progress and Decisions

### Current Status

| Item | State |
|---|---|
| Change created | yes · `2026-09-12-wta-sso` |
| Intake frozen | yes · source.md |
| P-goal-plan | 已跑（works_run）；本文须回写为与 Ready Spec 一致的最新态，不再当 Grill precursor |
| G-grill | **done** · design-tree `consensus` round=3 |
| S-spec | **ready** / `ready_for_tickets=true`（LOG-032） |
| T-tickets | **formal** · T-01…06 全部 blocked-by-auth（LOG-033） |
| Implementation | `not-authorized` |
| Pending Decisions | 无产品 Pending。低影响：环境矩阵字面量、Cookie 名/Domain |

### Pending Decisions and Blockers

**Round3 决策已收口**（D-100…116）。无产品 Pending。

**门禁：** S-spec / T-tickets **已完成**；不可进 I-implement（除非另令）。`ready_for_execution=false`；不实现。


### Resume Protocol

恢复时读取 Goal Plan、`.status.json`、`spec.md`、`tickets-map.md`、LOG-032/033。确认仍 `implementation_commit=not-authorized`。**不要**从 `G-grill-with-docs` 重开。未授权不得把票标 ready / in_progress，不得写产品树，不得造假 Evidence。

## Assumptions

- 冻结时 HEAD `d1ce372` 相对 CTO 基线 `b06d161` 仅含无关/已收口变更（含 notify）；Lead 现引 `677d9a9`，冻结声明可能过时，**以仓内实际 HEAD 为准**。
- 现有 `admin-web` / `home-web` Client 隔离与 Sa-Token extras 行为与 intake 描述一致（实现前用代码回读复核）。
- 域名矩阵字面量可占位（D-002）；authMode 默认与生产形态已锁（D-003 / D-004）。`ready_for_execution=false` 仅因 **I 未授权**，不是因为高影响产品假设未拍。
