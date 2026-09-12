# Change Architecture Decisions — 2026-09-12-wta-sso

> **Status legend:** Grill consensus 后下列条目为 **accepted**（change-local；非永久 ADR 毕业）。  
> ChatGPT 外脑仅为候选；正式拍板来源为 CTO via Lead + design-tree/LOG。

## ADR-001: 协议选型 — Authorization Code + PKCE S256

**Status:** accepted
**Source:** LOG-004 / LOG-008  
**Source:** CTO intake 2026-09-12 / Goal Plan

### Context

需要跨 App 统一登录，同时保留每 Client 的 Sa-Token 隔离。Implicit 已废弃；把现有密码登录包装成 password grant 会混淆本地登录与 OAuth 客户端凭证；SAML 超出本期栈。

### Decision（proposed）

采用 **OAuth 2.0 Authorization Code + PKCE（S256）**。禁止 Implicit、OAuth password grant、SAML。OIDC discovery / id_token / userinfo **延期 P1**。不引入外置 IdP。

### Consequences

P0 只实现 authorize / token（authorization_code+PKCE）/ revoke；客户端必须支持 PKCE；第一方 SPA=public（D-015）；PKCE/code 负向矩阵为 P0 硬 AC（D-010）；revoke≠SLO（D-016）。

---

## ADR-002: access_token = 现有 Sa-Token；extras = 目标业务 Client

**Status:** accepted
**Source:** LOG-004 / LOG-009 / intake 不变量  
**Source:** CTO intake 不变量 1–2

### Context

业务安全模型已按 `clientid` / `clientPk` 隔离 RBAC 与会话。替换 Token 形态或签发 SSO 中心 Client 票会破坏隔离或迫使伪 SSO。

### Decision（proposed）

`access_token` **就是**现有 WTA Sa-Token。换票时 extras **必须**写入**目标业务 Client**，禁止写入 `sso` 中心 Client。

### Consequences

硬验收：admin 与 home 两 Token `clientid` 不同且互打被拒。SSO Cookie 不得被业务 App 当作 API Token。

---

## ADR-003: 模块边界 — wta-sso + sso-web；扩展 sys_client；API/Port 与前端分层

**Status:** accepted
**Source:** LOG-004 / LOG-010 / LOG-012  
**Source:** CTO intake 模块落地

### Context

需要清晰的第一方 SSO 表面，同时避免平行「OAuth 应用表」与现有客户端管理分叉。

### Decision（proposed）

- 后端新模块 `backend/wta-modules/wta-sso`
- 前端新应用 `frontend/apps/sso-web`（ClientId=`sso`，会话键=`Sso-Token`）
- P0 **扩展** `sys_client`；新表仅 code / refresh / consent 等短寿命对象
- 现有 `client_secret` **不是** OAuth 密钥；新增 `sso_secret_hash`
- `wta-admin` 以 `namewta.sso.enabled` 组装开关
- `packages/platform/auth` 提供 `startSsoLogin()` / `handleCallback()`

### Consequences

客户端管理页增加 SSO 分组；回调白名单精确匹配（禁 `*`）；DDL/DML 基座变更进入 P0 票。

---

## ADR-004: 本地登录并存 — authMode = local | sso | both

**Status:** accepted
**Source:** LOG-005  
**Source:** CTO intake 不变量 6；开放问题 Q3

### Context

迁移期不能切断现有 `POST /auth/login`。各入口是否强制 SSO 需产品决定。

### Decision（proposed）

保留本地登录；按 Client 配置 `local` / `sso` / `both`。默认 admin/home 为 `both`；允许部分入口直接 `sso`（CTO-Q3/D-003）。入口级覆盖细节进 Spec。

### Consequences

`GET /auth/client/context` 扩展 `ssoEnabled` / `ssoAuthorizeUrl` / `authMode`。生产默认按 D-003。

---

## ADR-005: 分期与部署形态；独立 Origin + 同进程；SSO Cookie owner

**Status:** accepted
**Source:** LOG-006 / LOG-007 / LOG-011  
**Source:** CTO intake 分期；开放问题 Q2/Q4

### Context

协议核心可与 OIDC/SLO/独立进程解耦。域名与是否独立部署影响 cookie 域与回调配置。

### Decision（proposed）

- **P0：** authorize + token + PKCE + sso-web + Client 扩展 + 可选统一登录 + 本地并存  
- **P1：** OIDC / refresh / SLO / 同意页  
- **P2：** 外部第三方 / 独立进程 / MFA 收敛  

对外域名/callback：环境级矩阵（D-002=A，可占位）。生产：**同进程** + **`sso-web` 独立 Web Origin**（D-004=A1+B2）。独立后端进程维持 P2。

### Consequences

实现与发布票必须引用环境矩阵；占位行须在上线前替换真实 Origin/callback。SSO HttpOnly Cookie 由后端 Set-Cookie（D-013）；反代须维持独立 Web Origin。

---

## ADR-006: 规划门禁 — 无实现授权

**Status:** accepted（流程）
**Source:** LOG-015 / Lead dispatch  
**Source:** Lead dispatch 2026-09-12

### Decision（proposed）

本 change 在 G→S→T 完成且 CTO/用户书面授权前：`implementation_commit=not-authorized`，`ready_for_execution=false`。禁止 push/PR；禁止触碰 `2026-09-10-notify-channel-config`；禁止假票糊 validate。**2026-09-12：** CTO 否决此时进入 S-spec（LOG-016）；即使 Grill 决策 consensus，亦不得催派 RVP·规格，直至 CTO 另行开放。
