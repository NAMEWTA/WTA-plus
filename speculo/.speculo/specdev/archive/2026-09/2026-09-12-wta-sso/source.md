---
schema_version: 1
artifact: source
change: 2026-09-12-wta-sso
source_type: pasted
canonical_locator: null
captured_at: 2026-09-12T17:26:04+08:00
content_sha256: 839aa05666d49789ba013bcf1f53f5dbef04b598e172e006b734def497f281c7
remote_state: not-applicable
close_capability: not-applicable
---

# Source: WTA SSO 技术方案（CTO intake）

## Capture Metadata

- **Capture method:** pasted（CTO t159u 粘贴全文；权威 intake MD）
- **Author:** CTO
- **Created / updated:** 2026-09-12（intake）；冻结于 2026-09-12T17:26:04+08:00
- **Labels or classification supplied by source:** 本期只出方案 / SpecDev 工件；不改业务代码、不推送
- **Attachments:** `temp/team/lead/intake/2026-09-12-wta-sso-proposal.md`（与 `temp/team/goal-plan/INTAKE-20260912-wta-sso.md` 字节相同）
- **Redactions:** none
- **content_sha256 of intake file:** `839aa05666d49789ba013bcf1f53f5dbef04b598e172e006b734def497f281c7`
- **Baseline note:** CTO cited `main @ b06d161`；冻结时 HEAD=`d1ce372`（祖先含 `b06d161`）

## Original Content

# WTA SSO 技术方案（CTO intake · 2026-09-12）

> 来源：CTO t159u 粘贴全文。原路径 `/home/workdir/artifacts/WTA-SSO-技术方案.docx` 在 Grok Bot 电脑未找到文件；以本 MD 为权威 intake。
> 对照声明：文中写 `main @ b06d161`；派单时本地 `HEAD=d1ce372`（含 notify-channel-config 收口，祖先含 `b06d161`）。

## 范围

本期只出方案 / SpecDev 工件（goal-plan → grill → …），**不改业务代码、不推送**。实现另开 I-implement 授权后才做。

## 现状判断

WTA-plus 已经有「多 App + Client 隔离 + 统一登录策略」，但还不是 SSO。

当前链路：每个前端自己的登录页 → `POST /auth/login`（`clientId + grantType`）→ `IAuthStrategy`（password / sms / email / social / xcx）→ `LoginHelper.login` → 签发 **该 Client 的 Sa-Token**。

Token 不是纯无状态 JWT，而是 **JWT Simple + Redis 会话**：

- `loginId = userType:userId`
- extra 里同时有 `clientid`（字符串）和 `clientPk`（`sys_client.id`）
- `SecurityConfig` 要求请求里的 `clientid` 必须等于 Token extra
- RBAC / 菜单 / 会话清理按 `userId + clientPk` 计算
- Cookie 默认关闭，业务 App 只走 Header Bearer
- 已激活终端只有 `admin-web`、`home-web`，会话键分别是 `Admin-Token` / `Home-Token`

所以今天跨域名无法「登一次到处用」。把 Admin-Token 种到根域 Cookie 做伪 SSO 也走不通，因为不同 App 的 ClientId 不同，拦截器会直接拒。

另外：`sys_client.client_secret` 现在只是生成 `clientId = MD5(clientKey + clientSecret)` 的原料，`/auth/login` **并不校验 secret**。它还不是 OAuth 客户端密钥。

## 协议选型

**OAuth 2.0 Authorization Code + PKCE（S256）**

不采用 Implicit（已废弃）、不把现有密码登录包装成 OAuth password grant、不上 SAML。OIDC 的 discovery / id_token / userinfo 放 P1。

只借鉴 Keycloak / Casdoor / Logto / Hydra / Spring Authorization Server 的协议，**不引入它们当身份源**。本仓用户、登录域、Client、RBAC、会话清理是真相源。

Sa-Token SSO ticket 偏内部同 Redis；`sa-token-oauth2` 可参考，但不能让它拥有 Client / Token / 用户。

## 推荐架构

第一方模块 **「WTA 单点登录 SSO」**：统一登录页认出人，换票后仍走现有 Token。

```text
admin-web / home-web / 未来 App
        \      |      /
         Authorization Code + PKCE
                ↓
        frontend/apps/sso-web
        登录 / 注册 / 同意 / 注销
                ↓
        backend/wta-modules/wta-sso
        /oauth2/authorize  /token  /revoke
                ↓
        现有 LoginHelper + sys_user + sys_client + Sa-Token
```

关键不变量：

1. **不替换 Token**。`access_token` 就是现有 WTA Sa-Token。
2. **签发 extras 必须是目标业务 Client**，不能是 `sso` 中心 Client。
3. **两层会话**：SSO 域名才允许 HttpOnly Cookie；业务 App 继续 Header Bearer + 自己的存储命名空间。
4. **一层应用目录**：P0 扩展 `sys_client`，不另建平行「OAuth 应用表」。新表只放一次性 code / refresh / consent。
5. **现有 `client_secret` ≠ OAuth 密钥**。新增 `sso_secret_hash`，明文只显示一次，可轮换。
6. **本地登录并存**。`POST /auth/login` 保留，按 Client 配置 `local / sso / both`。

## 模块落地

| 位置 | 作用 |
|---|---|
| `backend/wta-modules/wta-sso` | 新模块，Controller → UseCase → Service → DAO |
| `wta-admin` 默认组装 | `namewta.sso.enabled` 开关 |
| `frontend/apps/sso-web` | ClientId=`sso`，会话键=`Sso-Token` |
| `packages/platform/auth` | `startSsoLogin()` / `handleCallback()` |
| `packages/web-domains/system/src/client` | 客户端管理页加 SSO 分组 |
| `50-namewta-ddl.sql` / `60-namewta-dml.sql` | 基座改表和种子 |

P0 接口：

- `GET /oauth2/authorize`
- `POST /oauth2/token`（只 `authorization_code` + PKCE S256）
- `POST /oauth2/revoke`

`GET /auth/client/context` 扩展：`ssoEnabled` / `ssoAuthorizeUrl` / `authMode`。

## 管理端

现有「客户端管理」配：应用创建与启停、精确回调白名单（禁止 `*`）、public/confidential、SSO 开关、生成/重置 SSO 密钥、PKCE、自动同意、scope。

## 分期

- **P0**：authorize + token + PKCE + `sso-web` + Client 管理扩展 + admin/home 可选统一登录 + 本地登录并存
- **P1**：OIDC discovery / id_token / userinfo、refresh、SLO、同意页
- **P2**：真正外部第三方、独立进程、MFA 收敛到 SSO

P0 硬验收：同一浏览器先 SSO 进 `admin-web`，再进 `home-web`，两张 Token 的 `clientid` 必须不同，互打接口必须被拒。

## CTO 待确认（4 项）

1. P0 是否就按「授权码 + PKCE + sso-web + 扩展 sys_client + 本地登录并存」来做
2. SSO 对外域名和各环境 callback URL
3. admin-web / home-web 默认用 `both` 还是部分入口直接 `sso`
4. 生产是否先同进程附带，还是一开始就给 `sso-web` 独立域名

## Source Comments

- Lead 派单：`temp/team/lead/dispatch-20260912-wta-sso-goal-plan.md` → RVP·规划执行 P-goal-plan。
- 同仓活跃 change `2026-09-10-notify-channel-config` 主题无关；**禁止触碰**。
- 本轮仅 SpecDev 规划工件；下一 Work=`specdev/G-grill-with-docs`；I-implement 未授权。
- 外脑由父进程补跑；本 change 不得声称外脑已通过。
- **Baseline 注记（2026-09-13）：** 冻结时 HEAD=`d1ce372`。Lead 现引 `677d9a9`，声明可能新于冻结点。最新代码锚以仓内实际 HEAD 为准；分期/产品口径以 CONTEXT + spec 为准，不以本 intake 的「P2=真正外部第三方」为现行分期。
- **阶段注记（2026-09-13）：** 上条「下一 Work=`specdev/G-grill-with-docs`」为冻结时历史句；现态 G/S/T 已跑完，待命 I-implement（`implementation_commit=not-authorized`）。以 CONTEXT / goal-plan / `.status.json` 为准。
