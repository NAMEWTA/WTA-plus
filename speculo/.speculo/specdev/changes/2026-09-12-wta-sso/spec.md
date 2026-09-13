---
schema_version: 3
artifact: spec
change: 2026-09-12-wta-sso
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:CTO-BRIEF-20260913-t155u
  - USER-DECISION:LOG-032-S-spec-authorized
  - USER-DECISION:D-100-A-FirstPartySsoProvider
  - USER-DECISION:D-101-A-access-scope
  - USER-DECISION:D-102-LOCKED-OpsFlow
  - USER-DECISION:D-103-LOCKED-Code-PKCE
  - USER-DECISION:D-104-A-reuse-F-contracts
  - USER-DECISION:D-110-C-button-then-sso-web
  - USER-DECISION:D-111-B-external-register-depth
  - USER-DECISION:D-112-A-sso-web-password-only
  - USER-DECISION:D-113-A-sys-user-password-not-shared-token
  - USER-DECISION:D-114-SOCIAL_DIR-CONTEXT
  - USER-DECISION:D-115-A-product-hard-AC
  - USER-DECISION:D-116-B-confidential-runtime-deferred
  - USER-DECISION:D-001-through-D-016-engineering
  - ADR-001
  - ADR-002
  - ADR-003
  - ADR-004
  - ADR-005
  - ADR-006
  - CODE:backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java
  - CODE:backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java
  - CODE:backend/wta-admin/src/main/java/org/namewta/web/service/IAuthStrategy.java
  - CODE:backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/SysClient.java
  - CODE:backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java
---

# Spec: WTA SSO（FirstPartySsoProvider · Authorization Code + PKCE）

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

自有前端各 App（admin-web / home-web）与后续外部系统 App 需要**同一套本仓账号密码**完成登录，但不能共享一张业务 Sa-Token。现有链路已按 `clientid` 隔离（见 `<Path>backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java</Path>`、`<Path>backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java</Path>`），跨域种根 Cookie 或复用 Admin-Token 会失败。第三方登录目录已有 Mask / GitHub 等 social（`<Path>backend/wta-admin/src/main/java/org/namewta/web/service/IAuthStrategy.java</Path>` + social 策略），但缺少**自建、默认已接通、排序最前**的 Authorization Code SSO 提供方与统一认人页。

### 目标用户与场景

- **终端用户**：在业务 App 登录页点「WTA SSO」第一按钮，跳转 `sso-web` 用本仓账号密码认人；同一浏览器再进另一业务 App 时不再输入密码；拿到的仍是目标 Client 的业务 Token。
- **应用运营 / 管理员**：按 (a) 创建应用 (b) 配置精确回调 (c) 交付 client/密钥（自有 App 可直接读配置）登记自有或外部 App。
- **业务 App 前端**：`authMode=both|sso|local`；SSO 路径走 platform 合同；本地 `POST /auth/login` 仍可用。
- **外部系统集成方（P0）**：可在管理面被登记为 Client（回调 + client + 密钥字段）；运行时 Code 流以自有 App 打通为先。

### 成功标准

1. **登录归一化**：同一套 `sys_user` 密码可在 admin-web 与 home-web 完成登录；账号归一 ≠ Token 共用。
2. **三门硬验收并列**：
   - `P0-SSO-REUSE`：同浏览器第二次业务 App 授权不得再要本仓密码。
   - `P0-CLIENT-ISOLATION`：admin/home Token `clientid` 不同且互拒。
   - `P0-DEFAULT-PROVIDER-PATH`：默认第三方登录入口不经额外开通即可走通授权码主路径。
3. **FirstPartySsoProvider**：第三方登录目录第一槽位预置启用；登录页第一按钮 → 授权码到 `sso-web`。
4. **协议与模块**：OAuth 2.0 Authorization Code + PKCE S256；`wta-sso` + `sso-web`；扩展 `sys_client`。
5. **本地并存**：保留 `POST /auth/login`；默认 admin/home `authMode=both`。

### 非目标

见 §5 OUT / OOS。含：OIDC / SLO / 独立进程 / Keycloak 类外包用户目录 / 共享业务 Token / confidential 运行时打通 / 改 notify 归档 change / 本期产品代码实现。

## 2. 解决方案与外部行为

### 解决方案摘要

交付 **FirstPartySsoProvider**：自建 SSO 作为第三方登录目录的默认第一提供方（与 Mask/GitHub 同槽、排最前）。人在 `sso-web` 用本仓账号认出，经 Authorization Code + PKCE 换得**目标业务 Client** 的现有 Sa-Token。一层应用目录扩展 `sys_client`；两层会话（SSO 域 HttpOnly Cookie + 业务 App Header Bearer）。外部系统 App 与自有 App 共用登记模型；P0 运行时先打通自有 App，confidential token 鉴权后置。

### 主要流程

**OpsFlow（接入）**

1. (a) 管理面创建应用（`sys_client` 行：自有或外部）。
2. (b) 配置精确回调/返回地址白名单（禁 `*`）。
3. (c) 交付 client / `sso_secret`（明文只一次，存 `sso_secret_hash`）；自有 App 通过扩展后的 `GET /auth/client/context` 读取 `ssoEnabled` / `ssoAuthorizeUrl` / `authMode`，禁止把密钥贴进前端包。

**默认提供方路径（用户）**

1. 业务 App 登录页第三方登录区**第一项**为自建 SSO 按钮（Mask/GitHub 其后）。
2. 点击后走授权码：redirect 到 `sso-web`（独立 Web Origin）。
3. 若无 SSO 会话：`sso-web` 只收本仓账号密码；成功后后端 Set-Cookie 建立 SSO 域会话。
4. authorize 发一次性 code（绑定 client / redirect / PKCE challenge / state）；回调业务 App。
5. 业务 App 用 code + PKCE verifier 换票；`access_token` = 现有 Sa-Token，extras = **目标业务 Client**（禁止 `sso` 中心 Client）。
6. 同浏览器再授权另一业务 App：SSO 会话仍有效 → **不得再要本仓密码**（`P0-SSO-REUSE`）。

**本地登录并存**

- `authMode=both`（默认 admin/home）：本地密码框仍在主路径；第三方区第一项仍为默认已接通的自建 SSO。
- 可配置部分入口直接 `sso`。
- `POST /auth/login` 与现有 `IAuthStrategy`（password/sms/email/social/xcx）保留。

### 边界、失败与稳定错误行为

- PKCE/code 负向：缺 challenge、非 S256、错 verifier、code 复用、过期、client/redirect/challenge 绑定失败、回调非精确白名单、state CSRF、并发至多一成功 → 拒绝发票或拒绝换票；敏感字段不进普通日志。
- Token `clientid` 与请求 header/param 不一致 → 现有 SecurityConfig 拒绝（隔离门）。
- browser 持有 `sso_secret` → 禁止（第一方 SPA=public）。
- revoke 仅撤销提交的那个令牌会话；不等于跨 App SLO。
- 外部 App：P0 可登记；未实现 confidential 运行时时，不得宣称「外部 App 已跑通 Code 流」。

### 状态转换与不变量

| 不变量 | 说明 |
|---|---|
| TokenInvariant | `access_token` = 现有 Sa-Token；extras = 目标业务 Client |
| LoginNormalization | 同一套 `sys_user` 密码多端登录 ≠ 共享 Token |
| ThirdPartySlotFirst | 自建 SSO 目录第一、默认启用、默认路径可走通 |
| TwoLayerSession | SSO Cookie 仅 SSO 域；业务仅 Bearer |
| SingleAppDirectory | P0 扩展 `sys_client`；短寿命对象可新表 |
| NoOutsourcedDirectory | 禁止 Keycloak/Casdoor/Logto/Hydra 替代本仓用户目录 |

## 3. 用户故事

- **US-001**：作为终端用户，我希望在业务 App 登录页点第一颗「WTA SSO」按钮跳到 `sso-web` 用本仓账号登录，以便走默认第三方登录路径而无需额外开通。
- **US-002**：作为终端用户，我希望同一浏览器登录过 SSO 后再进另一业务 App 时不再输入密码，以便体验真·SSO 复用（`P0-SSO-REUSE`）。
- **US-003**：作为终端用户 / 安全干系人，我希望 admin 与 home 的业务 Token `clientid` 不同且互打被拒，以便 Client 隔离不被伪 SSO 破坏（`P0-CLIENT-ISOLATION`）。
- **US-004**：作为终端用户，我希望同一套本仓账号密码能在 admin-web 与 home-web 都完成登录，以便登录归一化（产品硬门之一）。
- **US-005**：作为应用运营，我希望按 (a)(b)(c) 登记应用（含外部 App），并为自有 App 提供可读的 client context，以便统一接入。
- **US-006**：作为业务 App 用户，我希望在 `authMode=both` 时仍可用本地密码框，同时第三方区第一项是自建 SSO，以便迁移期并存。
- **US-007**：作为安全干系人，我希望 PKCE/code 负向合同全部拒绝，以便授权码流不可被绕过。
- **US-008**：作为集成方，我希望第一方 SPA 从不持有 `sso_secret`，confidential 字段可建但运行时后置，以便 P0 不假装外部 confidential 已通。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| **AC-001**（**P0-DEFAULT-PROVIDER-PATH**） | 默认环境；FirstPartySsoProvider 预置启用；admin/home 可读 context | 用户打开业务 App 登录页，点第三方登录区**第一按钮** | 不经额外开通即可发起授权码主路径并到达 `sso-web`；同一套本仓账号可在 admin 与 home 完成登录 | E2E：登录页槽位顺序 + 授权码主路径；账号双端登录 |
| **AC-002**（**P0-SSO-REUSE**） | 同浏览器已在 SSO 域建立会话并完成第一业务 App 换票 | 用户再对第二业务 App 发起授权 | **不得**再要求输入本仓密码；仍能换得第二 Client 的 Token | E2E：同浏览器 SSO→admin→home |
| **AC-003**（**P0-CLIENT-ISOLATION**） | 已分别取得 admin 与 home 的业务 Token | 用 A 的 Token + B 的 `clientid`（或互打对方 API） | 两 Token `clientid` **不同**；互打 **被拒**（对齐 SecurityConfig） | API/集成：Token extras + 拒绝码；参照 `<Path>backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java</Path>` |
| **AC-004** | public SPA 发起 authorize | 缺 `code_challenge` / method≠S256 | authorize **拒绝** | API：authorize 负向 |
| **AC-005** | 已发 code | 错 `code_verifier` 或非绑定 verifier | token **拒绝** | API：token 负向 |
| **AC-006** | 已成功换票一次的 code | 再次使用同一 code | **拒绝**（单次使用） | API：code 复用 |
| **AC-007** | code 已过 TTL | 换票 | **拒绝** | API：TTL |
| **AC-008** | code 存在 | redirect_uri / client_id / challenge 与签发时不一致 | **拒绝** | API：绑定校验 |
| **AC-009** | Client 回调白名单已配 | 使用未登记或不精确匹配（含 `*`）的 redirect | authorize **拒绝** | API：白名单 |
| **AC-010** | 授权含 state | 回调 state 缺失或篡改 | 业务 App **拒绝**完成登录 | E2E/API：CSRF state |
| **AC-011** | 同一 code 并发换票 | 并行 token 请求 | **至多一次**成功 | API：并发 |
| **AC-012** | 任意失败/成功路径 | 查普通应用日志 | code / verifier / secret **不**出现在普通日志 | 日志抽查 |
| **AC-013** | `wta-sso` 模块边界 | 静态/架构检查 | 仅经 wta-api / 明确 Port/SPI 取用户、Client、登录准入；禁止依赖 system Mapper/`ISys*` / admin 组装例外 | 模块边界检查 |
| **AC-014** | Client 配 `authMode=both`（默认 admin/home） | 打开登录页 | 本地密码框可用；`POST /auth/login` 仍通；第三方区第一项为自建 SSO | E2E + API login |
| **AC-015** | 部分入口配置为 `sso` | 访问该入口 | 走 SSO 授权码路径（可不展示本地框） | E2E 入口覆盖 |
| **AC-016** | SSO 登录成功 | 观察 Set-Cookie / 存储 | SSO 域 HttpOnly Cookie 由**后端** Set-Cookie；`sso-web` 不以 JS storage 冒充 SSO 会话；业务 App 仅 Header Bearer + 自有存储键 | 浏览器网络面板 + Cookie 域 |
| **AC-017** | 管理员执行 OpsFlow | (a) 建应用 (b) 配精确回调 (c) 交付或自有读配置 | 管理面可完成；自有 App context 含 `ssoEnabled`/`ssoAuthorizeUrl`/`authMode`；密钥不进前端包 | 管理 API + context 契约 |
| **AC-018**（D-111=B） | 管理面 | 登记外部系统 App（回调 + client + 密钥字段） | **可登记**成功；不要求 P0 跑通外部 confidential 运行时 | 管理 API |
| **AC-019**（D-116=B） | 管理面建 confidential 字段 | 保存 Client | 字段可建、可存 `sso_secret_hash`；P0 **不**验收 confidential token 鉴权跑通 | 管理 API；运行时后置明示 |
| **AC-020** | `sso-web` 登录页 | 查看可登录方式 | **只**本仓账号密码；不出现 Mask/GitHub；social 仅在业务 App 登录页且排在自建 SSO 之后 | UI E2E |
| **AC-021** | 换票成功 | 解码/检视 Token extras | extras.`clientid` = 目标业务 Client，**不是** `sso` | Token 断言 |
| **AC-022** | 调用 revoke | `POST` 撤销某业务 Token | 仅该令牌会话失效；**不**等于他 App SLO | API revoke |
| **AC-023** | 第三方登录目录 | 读与 social 同目录的提供方配置 | FirstPartySsoProvider 预置、排序第一、默认启用；自有 App 经 context 免配可读 | 配置面 + context |

## 5. 范围

### IN

- FirstPartySsoProvider 产品槽位与默认路径 UX（登录页第一按钮 → `sso-web` 授权码）
- `backend/wta-modules/wta-sso` + `frontend/apps/sso-web`（ClientId=`sso`，会话键=`Sso-Token`）
- OAuth 2.0 authorize / token（authorization_code+PKCE S256）/ revoke
- 扩展 `sys_client`（回调白名单、public/confidential 字段、SSO 开关、PKCE、自动同意、scope、密钥轮换元数据）；短寿命 code/refresh/consent 表可新建
- `GET /auth/client/context` 扩展 `ssoEnabled` / `ssoAuthorizeUrl` / `authMode`
- `authMode` = `local` \| `sso` \| `both`；默认 admin/home = `both`；保留 `POST /auth/login`
- platform/auth 纯合同 + adapters 浏览器侧；admin-web / home-web 接入
- 外部 App：**管理面登记深度**（D-111=B）
- 环境级 Origin/callback 矩阵（可占位）
- 同进程组装 + `sso-web` 独立 Web Origin（A1+B2）
- 三门硬验收 + PKCE 负向 AC

### REUSE

- 现有 Sa-Token / `LoginHelper` extras（`clientid` / `clientPk`）
- `SecurityConfig` clientid 一致性校验
- `IAuthStrategy` 与 password/sms/email/social/xcx 策略；social（Mask/GitHub）同目录后续槽位
- `sys_client` 现有管理与查询（`<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/SysClient.java</Path>`）
- 现有 `GET /auth/client/context`（`<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>`）扩展而非另起平行配置面
- 现有 `client_secret` 语义不变；OAuth 密钥用 `sso_secret_hash`

### OUT

- **OOS-001**：OIDC discovery / id_token / userinfo、refresh token 完整语义、同意页、SLO（→ P1）
- **OOS-002**：`wta-sso` 独立后端进程、MFA 收敛到 SSO（→ P2）
- **OOS-003**：把本仓用户目录外包/替换为 Keycloak、Casdoor、Logto、Hydra 等
- **OOS-004**：共享一张业务 Sa-Token / 根域 Cookie 伪 SSO / extras=`sso` 中心 Client 业务票
- **OOS-005**：Implicit、OAuth password grant、SAML
- **OOS-006**：P0 confidential client **运行时** token 鉴权打通（管理字段可建；D-116=B）
- **OOS-007**：在 `sso-web` 展示 Mask/GitHub 等 social
- **OOS-008**：本期归一 sms/email/xcx/仅 social 无密码用户的多端策略（本期 = password + 自建 SSO）
- **OOS-009**：改动已归档 notify change、push/PR、未授权 I-implement、伪造 `ticket/*.md`
- **OOS-010**：ChatGPT 外脑旧产品句作权威；本轮不假装外脑通过

## 6. 已锁定实现约束

- **DEC-100**：产品定位 = **FirstPartySsoProvider**（第三方登录目录默认第一；废止 NoExternalIdP /「仅第一方」口号）。来源：D-100=A / BRIEF / ADR-001。
- **DEC-101**：接入范围 = 自有前端各 App + 外部系统 App。来源：D-101=A。
- **DEC-102**：OpsFlow = (a) 创建应用 (b) 配置回调 (c) 交付 client/密钥；自有 App 可直接读配置。来源：D-102 LOCKED_FROM_BRIEF。
- **DEC-103**：P0 技术包 = Authorization Code + PKCE S256 + `sso-web` + 扩展 `sys_client` + 本地并存；禁 Implicit/password/SAML；OIDC→P1。来源：D-103 LOCKED_FROM_BRIEF / D-001。
- **DEC-104**：工程 F 合同 D-001…016 / D-010…016 **续用**。来源：D-104=A。
- **DEC-110**：UX = 登录页第一按钮 + 点下去走授权码到 `sso-web`。来源：D-110=C。
- **DEC-111**：外部 App P0 = **管理面可登记**；运行时先打通自有 App。来源：D-111=B。
- **DEC-112**：`sso-web` 只收本仓账号密码；social 在业务 App 登录页且排在自建 SSO 之后。来源：D-112=A。
- **DEC-113**：登录归一 = 现有 `sys_user` 密码；**≠ 共享 Token**。来源：D-113=A。
- **DEC-114**：默认接通落在与 social **同目录**第一槽 + 自有 App 读 `/auth/client/context`。来源：D-114=SOCIAL_DIR+CONTEXT。
- **DEC-115**：产品硬验收升格，与工程两门并列（AC-001/002/003）。来源：D-115=A。
- **DEC-116**：confidential 管理字段可建，**运行时后置**。来源：D-116=B。
- **DEC-001**：Code+PKCE S256 为唯一 P0 授权登录。来源：D-001 / ADR-001。
- **DEC-002**：环境级 Origin/callback 矩阵（可占位，上线前填真值）；精确匹配禁 `*`。来源：D-002。
- **DEC-003**：默认 admin/home `authMode=both`；允许部分入口直接 `sso`。来源：D-003 / ADR-004。
- **DEC-004**：生产同进程组装 + `sso-web` 独立 Web Origin。来源：D-004 / ADR-005。
- **DEC-010**：PKCE/code 负向全进 P0 AC（AC-004…012）。来源：D-010。
- **DEC-011**：硬验收含 `P0-SSO-REUSE` + `P0-CLIENT-ISOLATION`（+ 产品门 AC-001）。来源：D-011。
- **DEC-012**：`wta-sso` 仅 API/Port。来源：D-012 / ADR-003。
- **DEC-013**：SSO HttpOnly Cookie 由后端 Set-Cookie。来源：D-013。
- **DEC-014**：`packages/platform/auth` 无浏览器依赖；browser 侧在 `packages/adapters/*`。来源：D-014。
- **DEC-015**：第一方 SPA = public，强制 PKCE；禁止 browser 持有 secret。来源：D-015。
- **DEC-016**：revoke ≠ SLO。来源：D-016。
- **DEC-020**：`access_token` = 现有 Sa-Token；extras = 目标业务 Client。来源：ADR-002。
- **DEC-021**：现有 `client_secret` ≠ OAuth 密钥；用 `sso_secret_hash`。来源：ADR-003。
- **DEC-022**：`implementation_commit=not-authorized` 直至另行授权；本 Spec 不授权实现。来源：ADR-006。

## 7. 数据、接口与兼容

- **公共接口变化：** 新增 OAuth2 authorize/token/revoke（`wta-sso`）；扩展 `GET /auth/client/context` 字段；管理面 Client SSO 分组字段；前端 `startSsoLogin`/`handleCallback` 合同。保留 `POST /auth/login`。
- **数据模型与持久化：** 扩展 `sys_client`（SSO 相关列）；可选短寿命 code/refresh/consent 表；不新建平行「OAuth 应用表」替代 `sys_client`。
- **兼容要求：** 现有本地登录、social 登录、Bearer 业务调用、`clientid` 隔离行为保持。
- **迁移要求：** DDL/DML 基座变更进 P0 票；Provider 预置数据默认启用；环境矩阵占位行上线前替换。
- **发布或运维影响：** `namewta.sso.enabled` 组装开关；反代维持 `sso-web` 独立 Web Origin；Cookie Domain 随矩阵。

## 8. 非功能要求

- **NFR-001 安全与隐私：** PKCE S256 强制；密钥哈希存储、明文只一次；敏感值不进普通日志；SSO Cookie HttpOnly；SPA 不持 secret；Client 隔离不放宽。
- **NFR-002 性能与容量：** 不适用具体新阈值；code 为短寿命对象，须有 TTL 与单次使用。
- **NFR-003 可用性与可靠性：** `authMode=both` 下本地登录可作为迁移期回退；SSO 组装开关可关。
- **NFR-004 可观测性与运营：** 授权失败原因可区分（PKCE/白名单/过期/绑定）且不泄露 secret；管理面可完成 OpsFlow。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 同浏览器 SSO→admin→home | E2E | AC-001, AC-002, AC-003, AC-020 | `<Path>frontend/e2e/client-auth-context.spec.ts</Path>` 等扩展 | e2e-log |
| authorize/token/revoke API | API/集成 | AC-004…012, AC-021, AC-022 | 后端安全测试风格 | test-report |
| SecurityConfig clientid 互拒 | 集成/单测 | AC-003 | `<Path>backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java</Path>` | test-report |
| client context 扩展 | API | AC-017, AC-023 | `<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>` | test-report |
| 模块边界 | 架构/静态 | AC-013 | module map / 依赖规则 | review-note |
| 管理面登记外部 App | API/UI | AC-018, AC-019 | SysClient 管理 | test-report |
| Cookie owner | 浏览器 | AC-016 | 网络面板人工/E2E | e2e-log |

## 10. 风险、假设与未决问题

### 风险

- 误把「登录归一化」做成共享 Token → 用 AC-002+AC-003 双门防呆。
- 环境 Origin/callback 占位未替换导致上线白名单失败 → 发布票强制填矩阵。
- 外部集成方误以为 P0 已跑通 confidential → AC-018/019 与文档明示登记深度。

### 已采用的低影响假设

- 环境矩阵中 SSO Web Origin / Auth Origin / admin callback / home callback 本轮可用占位符，上线前替换真实值（D-002）。
- Cookie 具体名/Domain 字符串随独立 Origin 矩阵在实现票填写，语义锁定为后端 Set-Cookie + SSO 域限定（D-013）。
- Provider 同目录的物理表可以是现有 social 配置表扩展或等价目录，只要与 Mask/GitHub 同槽且自建第一（D-114）。

### 未决问题

无。
