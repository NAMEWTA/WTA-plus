# WTA SSO — 领域上下文

> **Status:** Grill-confirmed（含 Round4 t297u）/ **S-spec Round4 重出 ready**（对齐 BRIEF t155u + D-100…116 + D-200…203）。  
> **Authority:** CTO 书面决定（含 2026-09-13 t155u / t297u）> 本 CONTEXT > Spec > Goal Plan。  
> 阶段：Round4 后须重出 Spec/票 — **权威 Spec 已按 Round4 重写**；旧 `ticket/*.md` 相对过时，由 Lead 派 T 作废/重出。本派单不实现产品代码；不翻转 execution_authorization。  
> 冲突以 CTO 最新口径为准。旧词 `NoExternalIdP` /「仅第一方」已废止，由 FirstPartySsoProvider 取代。旧「创建应用=客户端管理」已废止（D-203 / ADR-007）。

## Glossary

| Term | Meaning |
|---|---|
| **第三方登录体系** | 登录页上可插拔的登录提供方目录。已有 Mask、GitHub 等 social；本期把**自建 SSO**放进同一目录，且排第一。 |
| **FirstPartySsoProvider** | 自建、自己实现的登录提供方：默认已接通、排序最前、默认路径可走通。对接入方等同「第三方登录」；身份真相源仍在本仓。取代旧词 `NoExternalIdP` /「仅第一方」。 |
| **登录归一化** | 同一套本仓账号密码，多端（自有前端各 App + 外部系统 App）登录。账号归一 ≠ Token 共用。 |
| **SSO（本期）** | 自建 SSO 服务：人在 `sso-web` 用本仓账号认出，经 Authorization Code + PKCE 换得**目标业务 Client** 的现有 Sa-Token。 |
| **接入方 App** | 自有前端各 App（admin-web / home-web / 后续自有 App）以及外部系统 App；均可按同一套流程接入。 |
| **Sa-Token（access_token）** | 现有 JWT Simple + Redis 会话；`loginId=userType:userId`；extra 含 `clientid` 与 `clientPk`。 |
| **业务 Client** | `sys_client` 行（admin / home / 外部登记应用）；Token extras 必须指向它，而非 SSO 中心 Client。 |
| **SSO Client** | ClientId=`sso`；会话键=`Sso-Token`；仅 SSO 域会话，**不**作为业务 API Token extras。 |
| **authMode** | 每 Client：`local` / `sso` / `both`；默认 admin/home=`both`；允许部分入口直接 `sso`；保留 `POST /auth/login`。与「SSO 排在第三方登录最前」同时成立：`both` 时本地密码框仍在主路径，第三方区第一项为自建 SSO（D-110=C：第一按钮 → 授权码到 `sso-web`）。 |
| **PKCE S256** | Authorization Code 流强制；禁 Implicit / password grant。OIDC → 后期。 |
| **sso_secret_hash** | OAuth 客户端密钥哈希；**不等于**现有 `client_secret`。自有 App 直接读配置；外部 App 由管理面交付 client/密钥。 |
| **两层会话** | SSO 域可 HttpOnly Cookie（后端 Set-Cookie）；业务 App 仅 Header Bearer + 自有存储键。 |

## 概念卡

**LoginNormalization：** 同一套本仓账号密码，多端登录（自有 App + 外部系统 App）。账号归一 ≠ Token 共用。

**ThirdPartySlotFirst：** 自建 SSO 是第三方登录目录的第一槽位：默认已接通、排序最前、默认路径可走通；Mask / GitHub 等仍在其后。不是「取消第三方登录」，也不是「再引入外置用户目录」。

**FirstPartySsoProvider：** 产品面 = 第三方登录提供方；身份面 = 本仓用户与 RBAC 仍是真相源。禁止把用户目录外包给 Keycloak / Casdoor / Logto / Hydra；禁止用「无外置 IdP」否定外部 App 接入。

**AccessScope：** 自有前端各 App + 外部系统 App 均可接入。外部系统 App = RP/Client，不是外置身份源。

**OpsFlow：** (a) 创建应用 (b) 配置回调/返回地址 (c) 把 client/密钥交给应用；自有 App 也可直接读配置。

**WhyNotPseudoSso：** 跨域种根 Cookie 或复用 Admin-Token 会因 `SecurityConfig` 要求请求 `clientid` == Token extra 而失败。真正 SSO 是「统一认人 + 按目标 Client 换票」。

**TokenInvariant：** `access_token` **就是**现有 Sa-Token；换票不得签发「SSO 中心 Client」的业务票。

**SsoAdminDualSurface：** 系统管理下独立「SSO 管理」负责创建应用与拿配置（自有+外部注册）；「客户端管理」仅配置**自有 App** 的 SSO 接入；外部平台自配置。应用目录数据**仍扩展 `sys_client`**（D-202=A）；**UI 主路径不得塞进客户端管理**。

**Phasing：** P0 = Authorization Code + sso-web + 应用接入（a 创建应用 / b 回调 / c 交付或读取 client 配置）+ 默认提供方槽位走通 + 本地登录并存；P1 = OIDC / refresh / SLO / 同意；P2 = 独立进程 / MFA 收敛。不把「外部第三方」单列为本期禁区。

## 产品定位

本期是「第三方登录」体系的一部分：除已接通的 Mask / GitHub 等外接 IdP，再提供自建第一方 SSO，并把它当作默认、排序最前、路径已通的第三方登录提供方。接入范围 = 自有前端各 App + 外部系统 App。实现目标 = 登录归一化。模块 = `wta-sso`（先只做 Authorization Code 类）。操作流程：(a) 创建应用 (b) 配置回调/返回地址 (c) 把 client/密钥交给应用；自有 App 也可直接读配置。

## 现状锚点（intake）

- 已激活终端：`admin-web` / `home-web`；外部系统 App 为明确接入对象。P0 深度 = **管理面可登记**（D-111=B）；confidential token 鉴权运行时后置（D-116=B）；运行时 Code 流先打通自有 App。不得宣称「外部 App 已跑通 Code 流」。
- 已有 social：`IAuthStrategy` 含 password / sms / email / social / xcx（Mask、GitHub 等）
- Cookie 默认关闭；业务走 Bearer
- `/auth/login` **不**校验 `client_secret` 作为 OAuth secret
- Baseline：CTO intake 引用 `main @ b06d161`；规划冻结时 HEAD `d1ce372`（祖先含 `b06d161`）。**Lead 现声明仓 HEAD 可能为 `677d9a9`，冻结声明可能过时；实现前以仓内实际 HEAD 为准，不以过时 hash 当代码锚。本 change 不改产品代码。**

## Round4 已确认（CTO t297u · grill consensus）

- **D-200=A：** 独立「SSO 管理」模块/菜单（创建应用、拿配置）。
- **D-201=A：** SSO 管理 vs 客户端管理双面分工（外部自配置）。
- **D-202=A：** 数据仍扩展 `sys_client`；UI 主路径必须独立 SSO 管理；自有 App 接入配置在客户端管理。
- **D-203=A：** 废止「创建应用/拿配置=客户端管理」旧 DEC。

## 开放语义

无（Round4 frontier 空）。**可以进新 S-spec**（Lead 派规格重出）；访谈不实现。
