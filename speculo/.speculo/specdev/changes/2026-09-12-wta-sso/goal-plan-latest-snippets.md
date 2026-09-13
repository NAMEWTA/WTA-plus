> **⚠️ 已过期（Round3 / 6 票投影）。** 禁止据此派工或判定完成。Round4 权威见 `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/goal-plan.md</Path>`（正式 7 票、独立 SSO 管理、AC-024、blocked-by-auth）。

> **已被 Ready Spec 取代（2026-09-13）。** 本文件为历史晋升稿；勿当现行 Pending。最新态以 `goal-plan.md` + `spec.md` + `design-tree.json` 为准。无产品 Pending。

## Outcome

在 WTA-plus 的「第三方登录」体系中交付 **FirstPartySsoProvider**（自建、默认已接通、排序最前、默认路径可走通）：自有前端各 App + 外部系统 App 均可接入；同一套本仓账号密码多端登录；P0 只实现 Authorization Code + PKCE；换票后仍签发**目标业务 Client** 的现有 Sa-Token（账号归一 ≠ Token 共用）。

P0 可观察结果：

1. 协议：`OAuth 2.0 Authorization Code + PKCE (S256)`；禁 Implicit / password grant / SAML；OIDC → P1。身份真相源仍在本仓，不引入 Keycloak / Casdoor / Logto / Hydra 替代用户目录。
2. 产品槽位：自建 SSO 作为第三方登录目录第一提供方，默认已接通、默认路径可走通；Mask / GitHub 等同目录其后，不互斥。
3. 模块：`backend/wta-modules/wta-sso` + `frontend/apps/sso-web`；扩展 `sys_client` 为一层应用目录（不另建平行 OAuth 应用表）。
4. 接入流程：(a) 创建应用 (b) 精确回调白名单（禁 `*`） (c) 交付 client/密钥；自有 App 可直接读配置（`/auth/client/context` 或等价面）。
5. 登录模式并存：按 Client 配置 `local` / `sso` / `both`；保留 `POST /auth/login`。
6. 工程不变量：同一浏览器 SSO → `admin-web` → `home-web`；两张 Token 的 `clientid` **必须不同**，互打接口 **必须被拒**；第二次业务 App 授权不得再要本仓密码（SSO-REUSE）。
7. 产品硬验收（D-115=A，与工程两门并列）：默认第三方登录入口不经额外开通即可走完授权码主路径（`P0-DEFAULT-PROVIDER-PATH` / AC-001）。

## Success and False Completion

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

## Non-goals

- 本期不改业务/产品代码、不 push、不 PR；I-implement 未授权
- P0 不做 OIDC discovery / id_token / userinfo、refresh、SLO、完整同意页（→ P1）
- P0 不把用户目录外包给外置 IdP 产品；不替换现有 Sa-Token；不关闭业务 App Header Bearer
- 外部系统 App P0 = 管理面可登记（D-111=B）；confidential token 鉴权运行时后置（D-116=B）。禁止写成「P0 不做外部第三方」，也禁止宣称 P0 已跑通外部 Code 流
- social IdP（Mask / GitHub）已存在，P0 不重做、不拆除；自建 SSO 进入同一目录并排第一
- 独立 SSO 进程、MFA 收敛到 SSO → P2

## Phasing

| Phase | Scope |
|---|---|
| **P0** | Authorization Code + PKCE + `sso-web` + 默认提供方槽位接通 + 应用接入 (a)(b)(c) + Client 管理扩展 + admin/home 可走通 + 本地登录并存 |
| **P1** | OIDC discovery / id_token / userinfo、refresh、SLO、同意页 |
| **P2** | 独立进程、MFA 收敛 |

## Pending Decisions

**无产品 Pending。** Round3 已拍（D-100…116 / D-001…016）。低影响：环境矩阵字面量、Cookie 名/Domain（上线前填真值）。
