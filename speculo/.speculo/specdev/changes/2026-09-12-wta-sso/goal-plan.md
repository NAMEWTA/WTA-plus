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
> 外脑：`external-brain/reply.md` 结论 **CHANGES REQUIRED**；本文件为 G-grill 前 planning precursor。


# Goal Plan: WTA SSO（OAuth2 Authorization Code + PKCE）

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`（尚未创建；待 G-grill → S-spec）
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`（outline only；无正式票）
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/</Path>`
- **Source：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/source.md</Path>`
- **External brain：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/external-brain/</Path>`

> Authority: **CTO 书面决定 > ADR/CONTEXT > Spec > 本 Goal Plan > tickets-map**。  
> `ready_for_execution=false`；文档中的方案要点**不**构成 I-implement 授权。  
> 外脑结果由父进程补写；本文件**不**声称外脑已通过。

## 0. Plan mode and freeze

| 字段 | 值 |
|---|---|
| 模式 | `plan`（仅规划；未授权实现） |
| change_status | `active` |
| current_work | `null`（本轮 P Work 已执行并写入 works_run；语义=planning precursor，非正式 Ready P） |
| 下一 Work | `specdev/G-grill-with-docs` |
| Baseline | CTO：`main @ b06d161`；冻结时 HEAD：`d1ce372`（祖先含 `b06d161`） |
| 禁止 | 改产品代码；push/PR；触碰 `2026-09-10-notify-channel-config`；造假票糊 validate |

## 1. Outcome and Authority

### Outcome

在 WTA-plus 交付**第一方 SSO**：统一登录页认出人，换票后仍签发**现有 Sa-Token**，且 Token extras 绑定**目标业务 Client**（非 SSO 中心 Client）。

P0 可观察结果：

1. 协议：`OAuth 2.0 Authorization Code + PKCE (S256)`；无外置 IdP；禁 Implicit / password grant / SAML；OIDC → P1。
2. 模块：`backend/wta-modules/wta-sso` + `frontend/apps/sso-web`；扩展 `sys_client`（一层应用目录，不另建平行 OAuth 应用表）。
3. 端点：`GET /oauth2/authorize`、`POST /oauth2/token`（仅 `authorization_code` + PKCE S256）、`POST /oauth2/revoke`。
4. 登录模式并存：按 Client 配置 `local` / `sso` / `both`；保留 `POST /auth/login`。
5. **硬验收**：同一浏览器先 SSO 进 `admin-web`，再进 `home-web`；两张 Token 的 `clientid` **必须不同**；互打接口 **必须被拒**。

### Success and False Completion

**Success（规划阶段）：** Intake 冻结；Outcome / 分期 / 模块落地 / 开放问题 / Authorization Matrix 落盘；下一 Work 明确为 G-grill；`ready_for_execution=false`。

**False completion（禁止宣称完成）：**

- 声称 SSO「已上线」或「可执行」但无 CTO 开放问题答复、无 Ready Spec/Tickets、无实现授权
- 用根域 Cookie / 复用 Admin-Token 做伪 SSO（Client 隔离会拒）
- 签发 extras=`sso` 中心 Client 而非目标业务 Client
- 引入 Keycloak/Casdoor/Logto/Hydra 等外置 IdP 作为身份源
- 采用 Implicit / password grant / SAML
- 造假 `ticket/*.md` 糊绿 `validate --stage goal-plan`
- 声称外脑已通过（父进程负责）
- 改 `2026-09-10-notify-channel-config` 或推送/PR

### Non-goals（本 change 规划边界）

- 本期**不**改业务/产品代码、不 push、不 PR
- P0 **不做** OIDC discovery / id_token / userinfo、refresh、SLO、完整同意页（→ P1）
- P0 **不做** 真正外部第三方、独立 SSO 进程（部署形态待 CTO-Q4）、MFA 收敛到 SSO（→ P2）
- 不替换现有 Sa-Token 为纯无状态 JWT；不关闭业务 App Header Bearer 模型

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | CTO 书面决定（含 4 开放问题答复） | 产品取舍与批准 | 更新真正拥有该决策的工件 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/CONTEXT.md</Path>` | 当前 change 架构决定与领域语义 | 返回 G-grill 更新真正 owner |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 已毕业永久决定 | change 替代时在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>` | 外部行为、范围与验收 | 下游不得改写（尚未创建） |
| 5 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/</Path>` | 单 Ticket 契约 | Goal Plan 只编排（尚无正式票） |
| 6 | 冻结 source / 当前代码与运行事实 | 现状与可行性 | 冲突时触发偏差并返回真正 owner |

## 2. Protocol and architecture snapshot（from intake）

### Protocol selection

- **Adopt:** OAuth 2.0 Authorization Code + PKCE S256
- **Reject:** Implicit；把现有密码登录包装成 OAuth password grant；SAML
- **Defer:** OIDC discovery / id_token / userinfo → **P1**
- **IdP:** 不引入外置 IdP；本仓用户、登录域、Client、RBAC、会话清理是真相源
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
| **P0** | authorize + token + PKCE + `sso-web` + Client 管理扩展 + admin/home 可选统一登录 + 本地登录并存 |
| **P1** | OIDC discovery / id_token / userinfo、refresh、SLO、同意页 |
| **P2** | 真正外部第三方、独立进程、MFA 收敛到 SSO |

### P0 hard acceptance

同一浏览器：SSO → `admin-web` → 再 → `home-web`；两 Token `clientid` 不同且互打被拒。

## 3. Execution Graph（planning-only；no formal tickets）

### DAG and Critical Path（Work routing）

```text
[Done-as-precursor] P-goal-plan draft（外脑 CHANGES REQUIRED；正式 Ready P 须 G→S→T 后二次收敛）
  → G-grill-with-docs  （锁定 ADR；消化 CTO 4 问 + 外脑）
  → S-spec             （外部行为与 AC）
  → T-tickets          （正式票 + tickets-map；禁止假票）
  → [Gate] CTO + Lead: implementation_authorized?
        ├─ false: STOP（仅 SpecDev）
        └─ true: I-implement（current / direct-parent 串行）
             → C-code-review / verify
```

### Waves and Ownership（预告；待 T-tickets）

| Wave | 内容预告 | 前置 | Shared owner |
|---|---|---|---|
| W-Grill | 访谈 / ADR proposed→accepted | P-goal-plan | Lead |
| W-Spec | Ready Spec + AC（含硬验收） | G-grill + CTO-Q1..4 足够清晰 | Lead |
| W-Tickets | 正式票拆分 | Ready Spec | Lead |
| W-P0-BE | `wta-sso` + DDL/DML + sys_client 扩展 | 实现授权 | 动态派单（串行） |
| W-P0-FE | `sso-web` + platform/auth + client 管理页 | BE 合同稳定 Gate | 动态派单（串行） |
| W-P0-Apps | admin/home SSO 入口与 authMode | FE SDK | 动态派单（串行） |
| W-P0-E2E | 硬验收场景证据 | Apps | Lead |

### Ticket Quick Reference

| ID | 可观察产出 | 状态 |
|---|---|---|
| （无） | 正式票待 `T-tickets`；本 map 仅 outline，不造假票 | outline |

## 4. Gates and Completion Evidence

### Overall Definition of Done（change 级；非本轮）

1. Ready Spec 覆盖 P0 行为与硬验收 AC
2. 正式 Tickets Ready；Skill 绑定真实项目 Skill
3. CTO 4 开放问题已书面关闭或明确 defer 进 P1/P2
4. `execution_authorization.implementation_commit=authorized` 且 Goal Plan `ready_for_execution=true` 后才实现
5. P0 硬验收 Evidence 可回读；互打拒绝可复现
6. 未引入外置 IdP；未采用禁用 grant

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G-Grill | P-goal-plan draft | ADR/CONTEXT 关键；开放问题记录 | S-spec 不得臆造 CTO 答 | Lead | 回 Grill |
| G-CTO-Q | Grill 草案 | CTO 书面答 Q1–Q4 | 域名/authMode/部署相关票 | CTO | 保持 draft |
| G-Spec | Grill+足够 CTO | Ready Spec | T-tickets | Lead | 回 Spec |
| G-Tickets | Ready Spec | 正式票+map validate | I-implement | Lead | 补票 |
| G-Auth-I | CTO/用户书面 | status 授权字段翻转 | 一切产品写 | CTO/用户 | 停实现 |
| G-P0-Hard | 实现完成 | 同浏览器双 App Token clientid 不同且互拒 Evidence | change complete | Lead | 修或偏差 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 | 状态 |
|---|---|---|
| OAuth2 Code + PKCE S256 | Spec/Tickets 待建 | planned |
| access_token = Sa-Token + 目标 Client extras | Spec/Tickets 待建 | planned |
| local/sso/both 并存 | Spec/Tickets 待建 | planned |
| P0 硬验收（双 App clientid 隔离） | Spec AC 待建 | planned |
| 扩展 sys_client / sso_secret_hash | Spec/Tickets 待建 | planned |

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
| Deploy / migration / production actions | not-authorized | 逐动作、目标和条件；生产域名见 CTO-Q2/Q4 |
| 触碰 `2026-09-10-notify-channel-config` | not-authorized | 永久禁止本派单 |
| 声称外脑已通过 | not-authorized | 父进程负责外脑 |

### Evidence Return

subagent 只返回候选事实与 commit；Lead 独立核对并写 Evidence、状态和最终验收。本轮无实现 Evidence。

## 6. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 无外置 IdP；禁 Implicit / password grant / SAML
- `access_token` 必须是现有 Sa-Token；extras = 目标业务 Client
- 扩展 `sys_client`；不平行再建 OAuth 应用主目录
- `client_secret` 不复用为 OAuth 密钥；用 `sso_secret_hash`
- 回调白名单精确匹配，禁止 `*`
- Cookie 仅 SSO 域；业务 App Header Bearer
- 本轮零产品代码；零 push/PR
- 不伪造 Tickets 糊 validate

### Verification Integrity

- 规划阶段以工件回读 + `validate-specdev` 尝试为准；goal-plan stage 在缺 Spec/正式票时**预期失败**，不得靠假票变绿
- 实现阶段（未来）：判卷接缝含 authorize/token/revoke、client context、双 App 硬验收；禁止删测试或放宽 Client 隔离
- current/direct-parent：严格串行，单一 implementation writer

### Migration or Release Sequence

1. DDL：扩展 `sys_client` + code/consent 等新表 → 种子 SSO Client
2. 后端 `wta-sso` 开关默认安全（`namewta.sso.enabled`）
3. `sso-web` 与 platform auth 辅助
4. admin/home 按 authMode 灰度（both 或 sso，待 CTO-Q3）
5. 生产域名与同进程/独立域决策（CTO-Q2/Q4）后再谈发布窗口

### Risks, Monitoring and Recovery

| 风险 | 缓解 |
|---|---|
| 伪 SSO（共享 Token）破坏 Client 隔离 | 硬验收；SecurityConfig clientid 校验保持 |
| 开放问题未答就开写 | blockers + ready_for_execution=false |
| 外脑延迟 | notes 占位；不阻塞本地 Grill 准备 |
| 与 notify change 并发 | 写集隔离；禁止改 notify change |

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。

## 7. Progress and Decisions

### Current Status

| Item | State |
|---|---|
| Change created | yes · `2026-09-12-wta-sso` |
| Intake frozen | yes · source.md sha256=`839aa056…f281c7` |
| P-goal-plan | **planning precursor / draft for G-grill** · works_run 含 `specdev/P-goal-plan` · 外脑=`CHANGES REQUIRED`（见 `external-brain/reply.md`）· `ready_for_execution=false` |
| G-grill | **next** |
| Spec / Tickets | 未开始 |
| Implementation | not-authorized |
| External brain | 占位；待父进程 |

### Pending Decisions and Blockers（CTO 4 开放问题）

1. **CTO-Q1：** P0 是否就按「授权码 + PKCE + sso-web + 扩展 sys_client + 本地登录并存」来做  
2. **CTO-Q2：** SSO 对外域名和各环境 callback URL  
3. **CTO-Q3：** admin-web / home-web 默认用 `both` 还是部分入口直接 `sso`  
4. **CTO-Q4：** 生产是否先同进程附带，还是一开始就给 `sso-web` 独立域名  

另：外脑待父进程；正式票待 T-tickets。

### Resume Protocol

恢复时读取本 Goal Plan、`.status.json`、`source.md`、ADR/CONTEXT/LOG、最新 Evidence；从下一 Work=`G-grill-with-docs` 继续。不得跳过 Grill/Spec/Tickets 直接实现。

## Assumptions

- HEAD `d1ce372` 相对 CTO 基线 `b06d161` 仅含无关/已收口变更（含 notify），不阻塞 SSO 方案边界。
- 现有 `admin-web` / `home-web` Client 隔离与 Sa-Token extras 行为与 intake 描述一致（Grill/实现前用代码回读复核）。
- 高影响假设（域名、authMode 默认、生产部署形态）**未**当作已锁定 → `ready_for_execution` 必须为 `false`。
