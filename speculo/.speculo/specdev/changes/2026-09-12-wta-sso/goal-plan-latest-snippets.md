## Outcome

在 WTA-plus 的「第三方登录」体系中交付 **FirstPartySsoProvider**（自建、默认已接通、排序最前、默认路径可走通）：自有前端各 App + 外部系统 App 均可接入；同一套本仓账号密码多端登录；P0 只实现 Authorization Code + PKCE；换票后仍签发**目标业务 Client** 的现有 Sa-Token（账号归一 ≠ Token 共用）。

P0 可观察结果：

1. 协议：`OAuth 2.0 Authorization Code + PKCE (S256)`；禁 Implicit / password grant / SAML；OIDC → P1。身份真相源仍在本仓，不引入 Keycloak / Casdoor / Logto / Hydra 替代用户目录。
2. 产品槽位：自建 SSO 作为第三方登录目录第一提供方，默认已接通、默认路径可走通；Mask / GitHub 等同目录其后，不互斥。
3. 模块：`backend/wta-modules/wta-sso` + `frontend/apps/sso-web`；扩展 `sys_client` 为一层应用目录（不另建平行 OAuth 应用表）。
4. 接入流程：(a) 创建应用 (b) 精确回调白名单（禁 `*`） (c) 交付 client/密钥；自有 App 可直接读配置（`/auth/client/context` 或等价面）。
5. 登录模式并存：按 Client 配置 `local` / `sso` / `both`；保留 `POST /auth/login`。
6. 工程不变量：同一浏览器 SSO → `admin-web` → `home-web`；两张 Token 的 `clientid` **必须不同**，互打接口 **必须被拒**；第二次业务 App 授权不得再要本仓密码（SSO-REUSE）。
7. 产品验收（待 Grill 确认是否升格为硬门）：默认第三方登录入口不经额外开通即可走完授权码主路径。

## Success and False Completion

**Success（本阶段）：** 最新口径写入 CONTEXT / Goal Plan；Grill 开放问题（提供方 UX、外部 App P0 深度、social 同槽、账号边界、配置面、验收升格、confidential）有书面答或明确 defer；下一 Work 仍为 G-grill 收口或 CTO 另行开放 S-spec；`ready_for_execution=false`。

**False completion（禁止宣称完成）：**

- 声称可进 S-spec / 可执行 / 已上线（Grill 收口 ≠ S 授权）
- 用根域 Cookie / 复用 Admin-Token 做伪 SSO
- 签发 extras=`sso` 中心 Client 而非目标业务 Client
- 把本仓用户目录外包给 Keycloak / Casdoor / Logto / Hydra
- 把「禁止外包身份源」写成「系统不得存在任何第三方登录」
- 把本期写成「仅第一方 / NoExternalIdP / 与第三方登录无关」或「P0 禁止外部系统 App 接入」
- 采用 Implicit / password grant / SAML
- 造假 `ticket/*.md` 糊绿 validate
- 改 `2026-09-10-notify-channel-config` 或推送 / PR

## Non-goals

- 本期不改业务/产品代码、不 push、不 PR；不催派 RVP·规格
- P0 不做 OIDC discovery / id_token / userinfo、refresh、SLO、完整同意页（→ P1）
- P0 不把用户目录外包给外置 IdP 产品；不替换现有 Sa-Token；不关闭业务 App Header Bearer
- 外部系统 App 的 **运行时深度**（confidential token 鉴权是否 P0 必须跑通）待 Grill，不在本文写成「P0 不做外部第三方」
- social IdP（Mask / GitHub）已存在，P0 不重做、不拆除；只要求自建 SSO 进入同一目录并排第一
- 独立 SSO 进程、MFA 收敛到 SSO → P2

## Phasing

| Phase | Scope |
|---|---|
| **P0** | Authorization Code + PKCE + `sso-web` + 默认提供方槽位接通 + 应用接入 (a)(b)(c) + Client 管理扩展 + admin/home 可走通 + 本地登录并存 |
| **P1** | OIDC discovery / id_token / userinfo、refresh、SLO、同意页 |
| **P2** | 独立进程、MFA 收敛 |

## Pending Decisions

旧 CTO-Q1…Q4 已答，不再列入。本轮 Grill：

1. 默认第一提供方 UX：登录页第一按钮 / 整页跳 `sso-web` / 两者都要？「默认接通」的最小含义？
2. 外部系统 App：P0 只登记，还是必须跑通 Authorization Code（含 confidential）？
3. `sso-web` 与 Mask / GitHub：只本仓密码 / SSO 页也挂 social / 平级不同页？
4. 「同一套账号密码」= 现有 `sys_user` 密码？无密码/仅 social 用户如何归一？书面确认账号归一 ≠ 共享 Token。
5. 「默认已接通、排第一」落在 social 目录、`sys_client` 开关，还是新建 provider 表？`authMode=both` 时本地密码框与「默认路径最前」如何并存？
6. 是否把「多端同一账号可登 + 默认第三方入口走通」升为 P0 产品硬验收？
7. confidential 运行时是否服务外部系统 App（第一方 SPA 维持 public）？
