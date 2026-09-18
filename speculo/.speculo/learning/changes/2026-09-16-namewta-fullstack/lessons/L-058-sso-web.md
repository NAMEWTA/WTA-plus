---
lesson_id: L-058
objective_ids: [OBJ-58]
claimed_cells:
  - A:parseAuthorizeQuery
  - A:fetchSession
  - A:loginWithPassword
  - A:requestAuthorize
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: four-guns-and-pkce
    minutes: 10
  - segment: page-causal-chain
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-012, S-SSO-01, S-SSO-02, S-L058-01, S-L058-02, S-L058-03, S-L058-04, S-L058-05, S-L058-06]
---

# Lesson 058：宏观认人厅四步——`parseAuthorizeQuery` / `fetchSession` / `loginWithPassword` / `requestAuthorize`

## 学完你能做什么

打开 `frontend/apps/sso-web/src/ssoApi.ts` 和同一包里唯一的业务页 `src/views/AuthorizePage.vue`，你能**口述认人厅怎样把人认完再送回厅堂**：查询串抄成六字段、用布尔问有没有 SSO 手环、用本仓密码换 HttpOnly Cookie、再拿同一份查询串去要授权码门口的 JSON。不是厅堂 `POST /auth/login`，不是 `createSsoAuth` 换票，也不是后端自己 `302`。

口试名单就是矩阵 **(a)** 这一行四格，符号以**磁盘**为准：

1. **`A:parseAuthorizeQuery`**：从查询串取出 `clientId` / `codeChallenge` / `codeChallengeMethod` / `redirectUri` / `responseType`（缺省 `'code'`）/ `state`。蛇形键进、驼峰字段出。**不**验 PKCE，**不**算 SHA-256，**不**持有 `code_verifier`，结果对象**没有** `sso_secret`。
2. **`A:fetchSession`**：`GET apiUrl('/sso/session')`，`credentials: 'include'`。只看 `body.code === 200`，返回 `boolean`。未登录是 `false`，**不**抛错。**不**把 `userId` / `username` 交给页面。
3. **`A:loginWithPassword`**：`POST apiUrl('/sso/login')`，JSON `{ username, password }`，同样带 Cookie。`body.code !== 200` 才 `throw`。函数自己**不** `setToken`，**不**读 Cookie；浏览器因 `Set-Cookie` 收下 SSO 手环。
4. **`A:requestAuthorize`**：`GET /sso/oauth2/authorize` 加上刚才那六项。成功 JSON 收成 `{ loginRequired, redirectUri }`。`loginRequired` 必须 `=== true` 才算要密码。**不**自己 `302`，**不**打 `/sso/oauth2/token`。

OBJ-58 还要你能把这四步接到 **PKCE 复印件**：厅堂 `createSsoAuth.startSsoLogin` 把 `code_challenge` / `S256` / `state` 写进认人厅 URL；认人厅原样抄去授权门口；`code_verifier` 留在厅堂 `sessionStorage`。认人厅**看不见**那句秘密。

2026-09-17 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `@namewta/domain-sso` / `createSsoService` | **没有。** `frontend/packages/domains/` 没有 sso 包 |
| `@namewta/web-domain-sso` / 授权菜单工厂 | **没有。** 认人厅自己就是 App，不是 web-domain |
| `src/application/services.ts` | **没有。** 组合面就是 `ssoApi.ts` |
| axios adapter / `Admin-Token` / `Home-Token` | **没有。** 三次 HTTP 都是 `fetch` + `credentials: 'include'` |
| 页面 `import { createSsoAuth }` | **没有。** 那是厅堂 `application/sso.ts` 的换票工厂 |
| `ssoApi` 导出 `logout` / `exchangeToken` | **没有。** 后端有 `POST /sso/logout` 与 `POST /sso/oauth2/token`；本包零命中 |
| `ssoLoginMethods` 在 `loginWithPassword` 里分支 | **没有。** 它是模块常量 `['password']`，`AuthorizePage.vue` **不 import** |
| `parseAuthorizeQuery` 会校验 `S256` | **没有。** 缺字段变 `''`；`response_type` 缺了才默认 `'code'` |
| 成功授权由服务器 `Location: 302` | **没有。** JSON 里给 `redirectUri`，页面 `window.location.assign` |

本课**不宣称**你会拆 `SsoOAuthController.authorize` 的 Client 白名单 / `PkceS256.requireS256` / 码表写入（L-055）、`POST /sso/oauth2/token` 与 revoke（L-056）、Redis `sso:session:` 与 Cookie 构造细节（L-057）、`SsoClientCatalog`（L-059），或把 home/admin 接线板再对照一遍（L-008）。L-008 已经覆盖五函数的**组合职责**（谁喊谁、哪条 URL、带不带 Cookie、写不写业务 Token）。今天只认：**这四支枪在认人厅里的因果链，以及 PKCE 查询串如何作为不验的复印件穿过它们。** `apiUrl` 是水管，不是本课格子。

## 先把宏观地图放在桌上

L-004 把 `sso-web` 钉成独立 Origin 的认人厅容器。L-008 把它和 home 并排放：厅堂有 `services.ts`，认人厅没有。L-011 的 `AuthController.clientContext` 只给厅堂一块路牌 `ssoAuthorizeUrl = webOrigin + "/authorize"`（默认 `http://127.0.0.1:4176/authorize`）。人点厅堂「WTA SSO」之后，真正走路的是本课这张页。

把认人厅想成小区门口的**保安亭**。亭子里只有一张桌子、四张纸条：

```text
厅堂（admin-web / home-web）
  createSsoAuth.startSsoLogin
    心里秘密 code_verifier  → sessionStorage（厅堂 Origin）
    搅成 code_challenge S256 → 写进认人厅 URL
    state / client_id / redirect_uri 一起写上
        │
        │  window.location 跳到
        │  {webOrigin}/authorize?response_type=code&client_id=…&…
        v
┌──────────────────────────────────────────────┐
│  sso-web  宏观认人厅（本课）                    │
│  三条路由 /  /authorize  /login                │
│  同一张 AuthorizePage.vue                      │
│                                                │
│  ① parseAuthorizeQuery   抄查询串，不验密      │
│  ② fetchSession          问手环在不在          │
│  ③ loginWithPassword     本仓密码 → Set-Cookie │
│  ④ requestAuthorize      把复印件交给授权门口  │
│                                                │
│  成功：window.location.assign(redirectUri)     │
│  redirectUri 上带着 code + state               │
└──────────────────────────────────────────────┘
        │
        v
厅堂 /sso/callback
  createSsoAuth.handleCallback
    对 state，拿出 verifier
    POST /sso/oauth2/token   ← 不在 sso-web
    写入 Admin-Token / Home-Token
```

**类比：** `parseAuthorizeQuery` 是保安把访客胸牌上的字**抄到登记本**，字抄错了下一关才会骂，这一关不验公章。`fetchSession` 是抬头看你手腕有没有本亭发的塑料环。`loginWithPassword` 是验本小区门卡（本仓用户名密码），验完由后门的打印机把环套上（后端 `Set-Cookie`）。`requestAuthorize` 是拿着登记本去授权窗口要一张**一次性纸条**；窗口若说「先戴环」，JSON 里写 `loginRequired=true`，不是把你拽去另一栋楼。

**类比失效处：**

1. 保安亭**不发**酒店房卡。房卡是厅堂用纸条去换的 `Admin-Token` / `Home-Token`。
2. 塑料环是 **HttpOnly** 的 `Sso-Token`（默认名，L-057）。页面脚本读不到，不能 `localStorage.setItem('Sso-Token', …)` 冒充。
3. 「未戴环」有**两扇窗**：`GET /sso/session` 用 `code !== 200` 说话（`fetchSession` → `false`）；`GET /sso/oauth2/authorize` 在 Client/PKCE 过关后仍用 `code === 200` 加 `loginRequired=true` 说话。不要合成一句「未登录就抛错」。
4. 胸牌上的 `code_challenge` **不是**秘密本身。秘密留在厅堂。认人厅把挑战串当普通字符串抄来抄去。
5. 授权成功**不是** HTTP 302。`SsoOAuthController.authorize` 返回 `R.ok(SsoAuthorizeVo)`。跳走的是浏览器 `assign`。
6. 三条路由不是三张脸。`main.ts` 里 `/`、`/authorize`、`/login` 都挂 `AuthorizePage.vue`。
7. 亭子里**没有**注销按钮。后端 `POST /sso/logout` 存在；`frontend/apps/sso-web` 全树搜不到 `logout`。

2026-09-17 工作树：权威认人面是 `frontend/apps/sso-web/src/ssoApi.ts`。权威门脸是 `src/views/AuthorizePage.vue`。权威路由是 `src/main.ts`。运行时依赖只有 `vue` 与 `vue-router`。开发/生产 `.env*` 的 `VITE_SSO_API` 都是空串：三次 HTTP 走同源 `/sso/*`，开发由 Vite 代理到 `VITE_SSO_API_PROXY`（默认 `http://127.0.0.1:18080`），生产由 Nginx 反代。PKCE 的**生成与换票**在 `frontend/packages/platform/auth/src/index.ts` 的 `createSsoAuth`，厅堂 `apps/{admin,home}-web/src/application/sso.ts` 把它接到各自的 HTTP 与 `sessionStorage` 键。

**图题 / caption：** 宏观认人厅四步。alt：厅堂生成 PKCE 并跳到 sso-web；认人厅抄查询串、问会话、可选密码登录、再 GET 授权；回调与换票回到厅堂。

**文字等价物：** 人从管理端或门户带着六项查询串走进认人厅。页面先 `fetchSession`。没有 SSO 会话就出示密码表，`loginWithPassword` 只 POST 用户名密码，成功体里没有 sessionId，手环靠浏览器收下 `Set-Cookie`。有会话之后 `parseAuthorizeQuery` 从当前 `fullPath` 抄查询串，`requestAuthorize` 原样打到 `GET /sso/oauth2/authorize`。若 JSON 说还要登录，密码表再出现；若给出 `redirectUri`，页面把整页交给那个地址。换票、写业务 Token、校验 `code_verifier`，全部发生在厅堂，不发生在 `sso-web`。

**图的边界：** 不保证三个 App 已在生产部署。不画 Redis 键和授权码表列（L-057 / L-055）。不把 `apiUrl` 画成本课格子。`webOrigin` 默认 `127.0.0.1:4176` 是后端配置，不是 sso-web 自己算出来的。e2e 复用会话「第二次不用再输密码」依赖 Cookie 还在，本图不把跨 Origin Cookie 策略讲成已经关闭的生产事实。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **认人厅是独立小店。** 包名 `@namewta/sso-web`。没有厨房包，没有菜单工厂，没有 `application/`。
2. **一张页，三条门牌。** `/`、`/authorize`、`/login` 都进 `AuthorizePage.vue`。
3. **先问手环，再抄胸牌。** `onMounted` 先 `fetchSession`，有环才 `continueAuthorize`。
4. **胸牌六个格子。** `client_id`、`redirect_uri`、`state`、`code_challenge`、`code_challenge_method`、`response_type`。最后一个缺了当 `'code'`，别的缺了当 `''`。
5. **挑战串是复印件。** 认人厅不算 SHA-256，不碰 `code_verifier`。
6. **密码只走 `/sso/login`。** 身体是 `{ username, password }`。没有 `clientId`，没有 `grantType: 'password'`，没有验证码，没有 `@ApiEncrypt`。
7. **手环不是 JS 写的。** `loginWithPassword` 成功只看 `code === 200`。Cookie 是后端 `Set-Cookie`。
8. **授权是 GET。** `requestAuthorize` 把六个驼峰字段变回蛇形查询串。
9. **要登录是 JSON，不是抛错。** `loginRequired === true` 时页面再出示密码表。
10. **有回调地址才离开。** `window.location.assign(result.redirectUri)`。没有地址就停在「授权未返回回调地址」。
11. **换票在厅堂。** sso-web 源码搜不到 `oauth2/token`。
12. **没有注销导出。** 不要把五函数表读成 SSO HTTP 全集。

**类比失效边界：** 「保安亭」**不**覆盖「密码怎么进 `SsoIdentityService`」。那是 L-057 / L-059。类比也**不**等于「`ssoLoginMethods` 会在运行时挡住 github」——常量只给测试锁「本仓只接受 password」，`loginWithPassword` 函数体内没有 `if`。类比还不等于「空查询串会在抄写这一关失败关闭」——空串会抄出一堆空字段再出门，失败发生在授权门口或更后面。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 认人厅 | first-party SSO login origin | `frontend/apps/sso-web`；`AGENTS.md`：独立 Origin，只接受本仓密码 |
| 授权查询 | authorize query | `SsoAuthorizeQuery`；六字段全是 `string` |
| 抄查询串 | `parseAuthorizeQuery` | `ssoApi.ts`；`URLSearchParams`；蛇形 → 驼峰 |
| 会话探测 | `fetchSession` | `GET /sso/session`；返回 `boolean` |
| 本仓密码登录 | `loginWithPassword` | `POST /sso/login`；`Promise<void>` |
| 请求授权 | `requestAuthorize` | `GET /sso/oauth2/authorize`；返回 `{ loginRequired, redirectUri? }` |
| 同源水管 | `apiUrl` | `VITE_SSO_API` 去尾斜杠再拼 path；dev/prod 默认空串 |
| 带 Cookie 的 fetch | credentials include | 三次 HTTP 都写了；让浏览器带上 SSO Cookie |
| 证明密钥交换（复印件） | PKCE S256 challenge copy | 认人厅只抄 `code_challenge` / `code_challenge_method`；验算在 L-055，verifier 在厅堂 |
| 登录必要 | login required | `body.data.loginRequired === true`；严格相等，`undefined` 当 false |
| 回调地址 | redirect URI | 授权成功 JSON 的 `data.redirectUri`；应带 `code` 与 `state`（由后端 `appendQuery`，L-055） |
| SSO 域会话 Cookie | SSO session cookie | 默认名 `Sso-Token`；HttpOnly、`path=/`、`SameSite=Lax`；本课只认「谁写下、谁读不到」 |
| 业务通行证 | business token | `Admin-Token` / `Home-Token`；认人厅不写 |
| 登录方法常量 | `ssoLoginMethods` | `['password'] as const`；测试锁死不含 github / wechat；**不是**运行时闸 |
| 待办 PKCE | pending PKCE | 厅堂 `sessionStorage`：`namewta-admin-sso-pending` / `namewta-home-sso-pending`；含 `verifier` |

四支枪对照（本课格子，2026-09-17）：

| 符号 | HTTP | 成功长什么样 | 失败长什么样 | 不做什么 |
| --- | --- | --- | --- | --- |
| `parseAuthorizeQuery(search)` | 无 | 六个字符串 | 不会 throw；缺键给 `''` 或 `'code'` | 不读 hash、不读 `sso_secret`、不验 S256 |
| `fetchSession()` | `GET /sso/session` | `true`（`code === 200`） | `false`；网络/JSON 炸才 throw | 不展示用户、不写 Token |
| `loginWithPassword(u, p)` | `POST /sso/login` | `undefined`（`code === 200`） | `throw Error(msg \|\| '登录失败')` | 不带 clientId、不 `setToken` |
| `requestAuthorize(query)` | `GET /sso/oauth2/authorize?...` | `{ loginRequired, redirectUri }` | `throw Error(msg \|\| '授权失败')` | 不 302、不 POST token、不读 verifier |

`readJson` 是文件内私有助手：`response.json()` 完事。三次 HTTP **都不**看 `response.ok`。业务成败看 JSON 的 `code`。

### 机制/因果链

**1. `parseAuthorizeQuery` 只抄不验。**

```text
search  →  去掉可选前导 '?'  →  URLSearchParams.get
client_id                → clientId                  ?? ''
code_challenge           → codeChallenge             ?? ''
code_challenge_method    → codeChallengeMethod       ?? ''
redirect_uri             → redirectUri               ?? ''
response_type            → responseType              ?? 'code'
state                    → state                     ?? ''
```

页面这样取查询串：

```text
route.fullPath 含 '?'
  ? slice(indexOf('?'))   →  "?client_id=…"（带问号）
  : ''
```

`fullPath` 是路径加查询，不是 `route.query` 对象。hash 模式不在本 App：`createWebHistory()`。人从厅堂来时，路牌是 `{webOrigin}/authorize`，查询由 `buildSsoAuthorizeUrl` 写死六键，其中 `code_challenge_method` 恒为 `'S256'`。认人厅**不依赖**这个恒等式，它只是再抄一遍。

测试锁的是负空间：结果对象 `not.toHaveProperty('sso_secret')`。Client 的 SSO 密钥（L-018 信封）不属于授权查询串。

**2. `fetchSession` 把「未登录」收成 false。**

`GET apiUrl('/sso/session')` + Cookie。后端无会话时 `R.fail("未登录")`，业务码是 `500`（`HttpStatus.ERROR`），不是 `200`。于是 `body.code === 200` 为假，函数返回 `false`。页面**不会**走进 `catch`。

`catch` 只接：断网、响应不是 JSON、`json()` 炸掉。那时页面同样出示密码表，文案仍是「请输入本仓账号密码」，把探测失败和未登录合成同一张脸。这是门脸策略，不是 `fetchSession` 把 500 当成异常。

成功时后端其实塞了 `SsoAuthenticatedUser(userId, username)`。本函数的返回类型是 `Promise<boolean>`，页面标题也不会变成「你好，WTA」。认人厅不靠这张卡片做欢迎语。

**3. `loginWithPassword` 只换手环，不换房卡。**

`POST` JSON `{ username, password }`，头是 `Content-Type: application/json`，`credentials: 'include'`。测试把输入锁成 `WTA` / `admin123`，并断言 URL 含 `/sso/login`、method POST、credentials include。

成功：`R.ok()`，无 `data`。浏览器把 `Set-Cookie` 记在**认人厅这次 fetch 的 Origin**上（同源 `/sso` 合同下，就是 sso-web 自己的 Origin，经代理/反代打到后端）。JS 读不到 HttpOnly 值。

失败：`throw new Error(body.msg || '登录失败')`。页面 `submit` 的 `catch` 把 `error.message` 写进 `status`。注意：`submit` 在调用登录**之前**没有改 `needPassword`；失败时表还在。成功时先 `needPassword = false`，再喊 `continueAuthorize`。

对照厅堂密码登录（L-014）：`/auth/login` 要 `clientId` + `grantType: 'password'`，写的是业务 Token。认人厅这一枪**没有**那些字段。不要把两张登录表合成「都是用户名密码所以是同一个 HTTP」。

**4. `requestAuthorize` 把复印件交给授权门口。**

六个驼峰字段变回蛇形，`URLSearchParams` 会编码 `redirect_uri`。请求是 **GET**，Cookie 照带。`code !== 200` 抛「授权失败」。`200` 时：

- `loginRequired` 只有后端明确 `true` 才为真。`false` / 缺字段 / `data` 为空，都是假。
- `redirectUri` 原样取出，可能 `undefined`。

后端邻居（L-055，本课不拆五层）：无 SSO 用户时 `needsLogin()`，**不写授权码表**，控制器仍 `R.ok`。有用户才插码，并把 `code`、`state` 拼进回调。所以「要登录」走成功码，「PKCE/Client 不过关」才走失败码。认人厅必须分清这两枝，不能一律 `throw`。

**5. `AuthorizePage` 把四支枪编成一条因果链。**

初始：`needPassword=false`，`status='正在检查 SSO 会话…'`，用户名输入框默认 `'WTA'`，密码空，`busy=false`。表单 `v-if="needPassword"`，挂载瞬间看不见。

```text
onMounted
  fetchSession
    true  → status「已有 SSO 会话，正在授权…」→ continueAuthorize
    false → needPassword=true，status「请输入本仓账号密码」
    throw → 与 false 同一张脸（catch 不读 error.message）

continueAuthorize
  parseAuthorizeQuery(当前 fullPath 的查询串或 '')
  requestAuthorize
    loginRequired  → needPassword=true，status「请输入本仓账号密码」；return
    redirectUri    → window.location.assign；认人厅结束
    两者都没有     → status「授权未返回回调地址」（表不一定回来）

submit（busy 闸只包这一段）
  loginWithPassword
    成功 → 藏表，status「登录成功，正在授权…」→ continueAuthorize
    失败 → status=error.message，表还在
```

两处调用 `continueAuthorize` 的错误处理**不对称**：挂载时授权失败被外层 `catch` 变成密码表；提交后授权失败走进 `submit` 的 `catch`，此时表已经被藏起来，人只看到红色/灰色 `status`。刷新才能回到探测。口试要能指出这个门脸缺口，不要发明「登录成功后授权失败会自动再出示表」。

`busy` **不**包 `onMounted`。探测期间按钮还不存在，所以没有双重点击问题；探测慢时也没有禁用态。

**6. PKCE 在认人厅是复印件，在厅堂才是秘密。**

厅堂 `startSsoLogin`：

1. `randomVerifier`（32 字节再 BASE64URL）→ `verifier`
2. SHA-256 + BASE64URL → `challenge`
3. 另随机 `state`
4. `sessionStorage` 存 `{ clientId, redirectUri, state, verifier }`
5. 跳到 `authorizeUrl`，查询含 `response_type=code`、`client_id`、`redirect_uri`、`state`、`code_challenge`、`code_challenge_method=S256`

认人厅：抄挑战，不抄 verifier（URL 上本来就没有）。授权门口把挑战锁进授权码行（L-055）。人回到厅堂 `/sso/callback?code=&state=`。`handleCallback` 对 `state`，用 pending 里的 `verifier` 去 `POST /sso/oauth2/token`。state 对不上就清存储并抛「SSO 回调 state 无效」。

所以 OBJ-58 的 PKCE 深度是：**认人厅如何把厅堂写在 URL 上的挑战原样送到 `GET /sso/oauth2/authorize`，以及为什么它不能、也不该持有 verifier。** 不是把 `PkceS256.verify` 再讲一遍。

**7. `apiUrl` 是空前缀合同，不是漏配。**

`String(import.meta.env.VITE_SSO_API ?? '').replace(/\/$/, '') + path`。`.env.development` 与 `.env.production` 都是 `VITE_SSO_API=`。开发另有 `VITE_SSO_API_PROXY=http://127.0.0.1:18080`，只进 Vite `server.proxy['/sso']`。把空串改成绝对 API Origin，Cookie 就会落到别的站，还要另开 CORS+credentials。本课不把这种改法当正例。

**8. 厅堂路牌与认人厅路径要对上，但认人厅自己不读 `webOrigin`。**

`AuthController.clientContext` 在 Client 启用 SSO 且 `namewta.sso.webOrigin` 非空时，设置 `ssoAuthorizeUrl = origin + "/authorize"`。厅堂登录页没有这块路牌就不点 SSO。人跳进来之后，sso-web **不再**读这个配置；它只看自己地址栏上的查询串。改路牌去 `/login` 也能进同一张页，但要自己保证查询串还在。

### 图、表或文本图

**图 1：宏观四步状态**

```text
[进入 /authorize?…]
        │
        v
 fetchSession ──────────── throw/网络炸
   │                         │
   ├─ false                  │
   │    │                    │
   │    v                    v
   │  出示密码表 ◄────────────┘
   │    │
   │    │ submit
   │    v
   │  loginWithPassword
   │    ├─ throw → 表还在，status=msg
   │    └─ 200 → 藏表
   │              │
   └─ true ───────┤
                  v
           parseAuthorizeQuery
                  │
                  v
           requestAuthorize
            ├─ throw
            │    挂载路径 → 又变密码表
            │    提交路径 → 表已藏，只改 status
            ├─ loginRequired true → 出示密码表
            ├─ redirectUri → assign 离开
            └─ 都没有 → 「授权未返回回调地址」
```

**图题 / caption：** 宏观认人厅门脸状态。alt：先探测会话；假或异常出示密码；真则授权；登录后再授权；loginRequired 把人送回密码表。

**文字等价物：** 页面不是一上来就打授权。它先问 `/sso/session`。没有手环才出现用户名密码。登录成功并不直接跳回厅堂，必须再打一次授权 GET。授权若仍说要登录，密码表回来。授权若给了回调，整页交给那个 URL。授权若既不要登录也没有回调，人停在认人厅里看一句失败提示。

**图 2：PKCE 跨 Origin 分工**

```text
admin-web Origin :4174          sso-web Origin :4176           后端 /sso
sessionStorage                  地址栏查询串                    Cookie + 码表
verifier  ──────────┐
state     ──────────┤
clientId  ──────────┤   parseAuthorizeQuery 抄前五+response_type
redirectUri ────────┤          │
challenge ──────────┼─────────►│
                    │          v
                    │   requestAuthorize GET
                    │          ├─ 无会话 → JSON loginRequired
                    │          └─ 有会话 → 写码，redirectUri 带 code+state
                    │                      │
handleCallback ◄────┴──────────────────────┘
  对 state，POST /token(code, verifier)     （这一枪从厅堂发出）
```

**图题 / caption：** 宏观 PKCE 复印件。alt：verifier 永不进 sso-web；challenge 经查询串进授权 GET；换票回厅堂。

**文字等价物：** 厅堂生成两样东西：一句很长的秘密，和它搅出来的挑战。秘密进自己的 `sessionStorage`。挑战、state、client、回调写进认人厅 URL。认人厅把这些字抄进授权 GET。授权门口认不认挑战、写不写码，是后端的事。人回到厅堂时带上 code 和 state；厅堂用留下的秘密去换业务 Token。把 verifier 写进 `parseAuthorizeQuery` 或 `loginWithPassword` 的 body，等于把秘密交给保安亭——测试连 `sso_secret` 都不许出现在查询对象上。

**图 3：三次 HTTP 与两把钥匙**

| 谁 | 枪 | Cookie | 写了什么钥匙 |
| --- | --- | --- | --- |
| sso-web | `GET /sso/session` | 带 | 不写 |
| sso-web | `POST /sso/login` | 带（让浏览器收 `Set-Cookie`） | SSO 域会话（HttpOnly） |
| sso-web | `GET /sso/oauth2/authorize` | 带 | 不写业务 Token；后端可能写授权码行 |
| 厅堂 | `POST /sso/oauth2/token` | 不走认人厅 fetch | `Admin-Token` 或 `Home-Token` |
| 厅堂 | `POST /auth/login` | 无 SSO Cookie 合同 | 另一条进门，本课不走 |

**图题 / caption：** 认人厅三枪只碰 SSO Cookie。alt：token 与 /auth/login 都不在 ssoApi.ts。

**文字等价物：** 认人厅三次出门都带 `credentials: 'include'`，为的是同一只 SSO Cookie。登录这一枪让后端写下它。会话探测和授权读取它。业务通行证在厅堂换票之后才出现。`/auth/login` 是厅堂自己的密码门，和保安亭不是同一把锁。

**图的边界：** 图不展示 `SsoSessionCookie.create` 不设 `Secure` / `Domain` 的生产含义（L-057）。图不保证代理把 `Set-Cookie` 原样转回浏览器——那是部署合同；源码侧认人厅只声明 `credentials: 'include'` 和同源空前缀。图不把 e2e 的 admin/home Client 十六进制 id 写成 `parseAuthorizeQuery` 的默认值；测试样例用过 `client_id=admin`，那是单元夹具，不是种子 Client。

## 正例、反例与边界

**正例 1 — 抄查询串，不含密钥。** `parseAuthorizeQuery('?response_type=code&client_id=admin&redirect_uri=http://127.0.0.1:4174/sso/callback&state=abc&code_challenge=xyz&code_challenge_method=S256')` 得到 `clientId='admin'`、`responseType='code'`、挑战与 method 原样。对象上没有 `sso_secret`。带不带前导 `?` 结果一样。

**正例 2 — 缺 `response_type` 仍当 code。** 查询只有 `client_id` 时，`responseType` 是 `'code'`，其余空串。这是抄写层的缺省，不是授权门口已经放行。

**正例 3 — 第一次进门：无环 → 密码 → 授权 → 离开。** `fetchSession` 得 `false`，表出现。人提交 `WTA` / 本仓密码。`loginWithPassword` 200 后藏表，`requestAuthorize` 带上地址栏那六项。JSON 给 `redirectUri`，`assign` 到厅堂 `/sso/callback`。

**正例 4 — 第二次进门：有环，不再输密码。** e2e「三闸」里 admin 先走认人厅；同一浏览器上下文再从 home 点 SSO。home 侧断言密码框计数为 0。这是 `fetchSession === true` 然后直接 `continueAuthorize` 的产品脸。前提是 SSO Cookie 还在，不是 home 复用了 `Admin-Token`。

**正例 5 — 授权仍要登录。** 探测以为有环，或登录刚成功，但授权 GET 回来 `data.loginRequired === true`。页面把表再次打开。后端这一枝不写码表（L-055）。认人厅把「要登录」当分支，不当 throw。

**正例 6 — 未登录探测不抛。** 后端 `R.fail("未登录")`，`code=500`。`fetchSession` 返回 `false`。`AuthorizePage` 的 `onMounted` 走 `else`，不是 `catch`。

**正例 7 — 密码错误抛给门脸。** `loginWithPassword` 看到 `code !== 200`，`throw new Error(body.msg || '登录失败')`。`submit` 把这句话写进 `status`，`busy` 在 `finally` 松开，表还在。

**正例 8 — 换票不在本包。** 在 `frontend/apps/sso-web` 搜 `oauth2/token`、`code_verifier`、`exchangeToken`：没有。它们在厅堂 `application/sso.ts` 与 `platform/auth`。

**反例 1 — 「`parseAuthorizeQuery` 会拒绝非 S256。」** 它不会。method 填 `plain` 也会抄进去。拒绝发生在授权服务（L-055）。认人厅可能先把人送到门口再挨骂。

**反例 2 — 「`fetchSession` 返回用户对象。」** 返回 `boolean`。页面不读 `data.username`。

**反例 3 — 「`fetchSession` 失败会 throw，所以未登录等于异常。」** 未登录是 `false`。throw 是运输层事故。

**反例 4 — 「`loginWithPassword` 往 `localStorage` 写 `Sso-Token`。」** 函数返回 `void`。钥匙是后端 Cookie。

**反例 5 — 「`loginWithPassword` 要带 `clientId`。」** body 只有用户名密码。Client 在查询串里，等 `requestAuthorize` 再出门。

**反例 6 — 「`requestAuthorize` 就是换票。」** 换票是厅堂 `POST /sso/oauth2/token`。本枪只问能不能发码、要不要先登录。

**反例 7 — 「授权成功靠 302。」** 靠 JSON `redirectUri` + `window.location.assign`。

**反例 8 — 「`loginRequired` 缺省为 true，安全。」** 实现是 `=== true`。缺字段当 false，然后看有没有 `redirectUri`；都没有就停在「授权未返回回调地址」。

**反例 9 — 「`ssoLoginMethods` 在提交时拦住社交登录。」** 页面不 import 它。没有 github 按钮。常量是测试钉，不是运行时 `switch`。

**反例 10 — 「三条路由三张页。」** 一个组件。

**反例 11 — 「在 sso-web 里 `createSsoAuth` 更干净。」** 那会把 verifier 和换票拖进认人厅，Cookie 与业务 Token 拧到一根绳上。L-008 的反例在函数层仍然成立。

**反例 12 — 「空 `VITE_SSO_API` 是漏配。」** 空串表示同源 `/sso`。dev/prod 都这样。

**反例 13 — 「认人厅也该有 logout 按钮才算完整。」** 产品面今天就是四支枪加一张密码表。注销窗口在后端，调用方不在本 App。

**反例 14 — 「`AuthorizePage` 会校验 redirect 白名单。」** 白名单在 `SsoAuthorizationService.requireExactRedirect`（L-055）。页面有地址就 `assign`。

**边界 1 — 空查询串仍会授权出门。** `fullPath` 无 `?` 时传入 `''`，六个字段五个空、一个 `'code'`。`requestAuthorize` 仍 GET。失败在后端必填/Client/PKCE，不在抄写函数。

**边界 2 — 登录成功后授权 throw，表已被藏。** `submit` 先 `needPassword=false` 再 `continueAuthorize`。口试不要说「任何失败都会回到密码表」。

**边界 3 — `busy` 只管提交。** 挂载探测可以重叠；没有第二次提交按钮，直到表出现。

**边界 4 — 三次 HTTP 不看 `response.ok`。** HTML 404 会在 `json()` 炸掉，被门脸收成密码表或「登录失败」。

**边界 5 — Cookie 写点是浏览器，不是 JS。** 口试问「谁写入 SSO 会话」：后端 `Set-Cookie` + 浏览器存储；`loginWithPassword` 只负责把凭证 POST 出去并检查 `code`。

**边界 6 — 默认用户名 `'WTA'` 是 Vue 初值，不是 API 默认。** `ssoApi.ts` 不填用户名。

**边界 7 — 测试未覆盖 `fetchSession` / `requestAuthorize` / `apiUrl`。** `ssoApi.test.ts` 三例：方法常量、密码 POST、查询无 secret。这两支枪的证据在源码与 `AuthorizePage.vue`，不要假装有单测锁死它们的 URL。

**边界 8 — 同源代理是开发合同。** `changeOrigin: true` 把 `/sso` 转到 `18080`。生产靠 Nginx。本课不关闭「Cookie 在反代后是否带 Secure」——源码 `SsoSessionCookie.create` 此刻不设 `Secure`（L-057）。

**边界 9 — 厅堂两个 pending 键互不相通。** admin 与 home 各写各的 `sessionStorage`。认人厅 Cookie 却是同一 Origin 上的同一只手环。所以可以复用认人、仍换两张不同的业务票（e2e AC-002 / AC-003）。认人成功 ≠ 两厅堂共享 Token。

**边界 10 — `requestAuthorize` 总是带六个查询键。** 空字符串也会出现在 URL 上（`state=`、`code_challenge=`）。不要说「空字段会被省略」。

## 变式与迁移

1. **和 L-008 对照。** 那课认「五函数是认人面、没有接线板」。本课认四支枪各自的返回值、失败通道、以及 PKCE 复印件。不要在本课把 home `services.ts` 三只 `create*` 再背一遍。`apiUrl` 仍是水管：dev/prod 空串同源。
2. **和 L-055 对照。** 授权门口验 Client、精确回调、强制 S256、无用户早退、有用户写码。本课只保证查询串被 GET 送过去，且 `loginRequired` / `redirectUri` 被读对。口试若被问「plain 为什么失败」，指服务层，不要指 `parseAuthorizeQuery`。
3. **和 L-056 对照。** 换票、revoke、verifier 比对在厅堂 HTTP。本课页面 `assign` 之后就结束。
4. **和 L-057 对照。** 手环怎么进 Redis、Cookie 名/TTL/HttpOnly 怎么构造，是会话课。本课认「登录枪不写 JS Token、探测枪把非 200 当 false」。
5. **和 L-014 对照。** 厅堂 `IdentityAccessService.login` 打 `/auth/*`，写前端 session store。认人厅 `loginWithPassword` 打 `/sso/login`，写 Cookie。两张表长得像，合同不是同一张。
6. **和 L-011 对照。** `ssoAuthorizeUrl` 是门卫 context 里的路牌。本课不读它。路牌缺了，人根本走不到认人厅。
7. **和 L-018 对照。** `client_id` 是 `sys_client` 那一行。认人厅不旋转密钥、不 bind。查询对象禁止出现 `sso_secret`。
8. **以后若要社交登录进认人厅。** 先改产品：`ssoLoginMethods` 今天锁死 `['password']`。再改的是后端身份口与门脸，不是在 `parseAuthorizeQuery` 里加 `provider=` 就完。
9. **以后若要注销。** 后端已有 `POST /sso/logout`。应新增一支带 `credentials: 'include'` 的枪，并处理 Cookie 过期。不要借用厅堂 `identityAccessService.logout`（那删的是业务 Token）。
10. **以后若 `VITE_SSO_API` 指向独立 API Origin。** 先设计 Cookie Domain / CORS credentials，再改水管。空串合同一破，三次 `fetch` 的「带 Cookie」可能全部落空。
11. **把认人厅做成组件库拷进 admin。** 非法。独立 Origin 是会话隔离的一部分。拷 `AuthorizePage.vue` 进厅堂 `views/` 会把保安亭焊进酒店大堂。
12. **迁移口诀：** 先数四支枪的 URL 与返回值 → 再数页面 `onMounted` / `submit` / `continueAuthorize` 三枝 → 再数 PKCE 秘密在哪一侧 → 再数哪些后端窗根本没有前端枪（token / logout / revoke）。跳步会出现「把 requestAuthorize 说成换票」「把 fetchSession 说成 getInfo」「把空 VITE_SSO_API 说成漏配」。

## 常见误区

1. **「OBJ-58 包含 `SsoOAuthController.authorize` 五层。」** 那是 OBJ-55。本课认浏览器怎么喊。
2. **「OBJ-58 包含 `createSsoAuth`。」** 工厂在 platform-auth，组合在厅堂。本课只认它写下的查询串如何被抄走。
3. **「`parseAuthorizeQuery` 解析 `route.query`。」** 磁盘用 `route.fullPath` 切片。
4. **「`fetchSession` 成功表示已经进入用户中心。」** 只表示 SSO Cookie 还被这次 GET 认到。厅堂还没换票。
5. **「`loginWithPassword` 返回 token。」** 返回 `void`。
6. **「`requestAuthorize` 用 POST，因为登录是 POST。」** 授权是 GET。
7. **「`credentials: 'include'` 让 JS 读到 Cookie。」** 正好相反：它让浏览器**带上** HttpOnly Cookie；脚本仍读不到。
8. **「`ssoLoginMethods` 是第四支枪。」** 不是函数，不是本课格子。
9. **「`apiUrl` 是 OBJ-58 声称格子。」** chain 的四格不含它。L-008 已把它当作五函数认人面的水管讲过。
10. **「页面会 `encodeURIComponent` 后再手工拼 URL。」** `URLSearchParams` 在 `requestAuthorize` 里做。抄写层不做第二次编码。
11. **「默认用户名 WTA 证明只许管理员登录。」** 那是输入框初值。后端仍走本仓密码校验。
12. **「有 SSO 会话就会 302。」** 有会话仍先 JSON，再由页面 `assign`。
13. **「把 `loginRequired` 当成 HTTP 401。」** 它是 `200` 里的字段。
14. **「认人厅要挂 captcha / `@ApiEncrypt`。」** 2026-09-17 这两样都不在 `ssoApi.ts`。
15. **「home 与 admin 共用认人 Cookie 就是共用业务 Token。」** e2e 锁的是 Client 隔离：两张业务票的 `client_id` 不同，交叉 `getInfo` 不是 200。
16. **「L-008 已经讲过这四个名字，本课可以只背口号。」** L-008 覆盖组合职责。本课覆盖返回值、两扇「未登录」窗、PKCE 复印件、门脸三枝不对称的 catch。背口号过不了四格 (a)。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/apps/sso-web/src/ssoApi.ts`。圈六个 `SsoAuthorizeQuery` 字段、`parseAuthorizeQuery` 的 `?? ''` 与 `?? 'code'`、三次 `fetch` 的 `credentials: 'include'`、`fetchSession` 的 `code === 200`、`loginWithPassword` 的 JSON body、`requestAuthorize` 的蛇形键和 `loginRequired === true`。确认没有 `token`、没有 `logout`、没有 `code_verifier`。
2. 打开 `src/ssoApi.test.ts`。圈 `ssoLoginMethods` 只含 `password`、POST `/sso/login` 的 credentials、查询对象没有 `sso_secret`。确认没有 `describe` 覆盖 `fetchSession` / `requestAuthorize`。
3. 打开 `src/views/AuthorizePage.vue`。顺着 `onMounted` → `submit` → `continueAuthorize` 用手指走三枝。圈 `needPassword` 何时变 true/false，圈 `assign(result.redirectUri)`，圈提交路径里授权失败时表已经被藏。
4. 打开 `src/main.ts`。圈三条路由都指向 `AuthorizePage`。打开 `package.json`，圈运行时依赖只有 `vue` 与 `vue-router`。确认没有 `src/application/`。
5. 打开 `.env.development` 与 `.env.production`。圈 `VITE_SSO_API=`。打开 `vite.config.ts`，圈 `/sso` 代理目标。打开厅堂 `apps/admin-web/src/application/sso.ts` 与 `packages/platform/auth/src/index.ts` 的 `buildSsoAuthorizeUrl` / `startSsoLogin`，圈 `code_verifier` 只出现在厅堂存储与换票 body。回到 sso-web 搜 `verifier`，确认无匹配。

## 总结、词汇表与下一步

- **宏观认人厅四步：** 抄查询串 → 问手环 → 可选本仓密码换 Cookie → GET 授权要 JSON。四支枪在 `ssoApi.ts`，门脸在 `AuthorizePage.vue`，三条路由同一张页。
- **(a) `parseAuthorizeQuery`：** 蛇形抄驼峰；`response_type` 缺省 `'code'`；不验 PKCE；不持有 secret / verifier。
- **(a) `fetchSession`：** `GET /sso/session`；`code === 200` 才是 true；未登录是 false 不是 throw。
- **(a) `loginWithPassword`：** `POST /sso/login` `{ username, password }`；失败 throw；成功不写 JS Token，Cookie 由浏览器收。
- **(a) `requestAuthorize`：** `GET /sso/oauth2/authorize` 带回六个字段；`loginRequired === true` 留在亭里；有 `redirectUri` 才 `assign`；不换票、不 302。
- **PKCE 复印件：** 挑战在 URL 上穿过认人厅；秘密留在厅堂 `sessionStorage`。认人成功 ≠ 业务已登录。
- **不是厅堂，不是缩小的 home。** 没有 domain 工厂，没有 `services.ts`，没有 logout 导出。

词汇表：`parseAuthorizeQuery` / `SsoAuthorizeQuery` / `fetchSession` / `loginWithPassword` / `requestAuthorize` / `apiUrl` / `ssoLoginMethods` / credentials include / login required / redirect URI / PKCE S256 challenge copy / `code_verifier` / SSO session cookie / `Sso-Token` / first-party SSO login origin / `window.location.assign`。

下一步：授权门口 PKCE/Client/码表是 OBJ-55。换票与 revoke 是 OBJ-56。SSO 域会话三条窗与 Cookie 构造是 OBJ-57。`SsoClientCatalog` / `SsoIdentityService` 只经 `wta-api` 读用户与 Client 是 OBJ-59。厅堂组合对照是 OBJ-08。Client 管理窗是 OBJ-18。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 文字等价物

- **口令：** 抄胸牌、问手环、换手环、要纸条。秘密不进亭。
- **口诀：** 先 session，后 authorize；密码只打 `/sso/login`；挑战原样 GET 出去；跳走靠 `assign` 不靠 302。
- **对照：**

| 说法 | 换成磁盘事实 |
| --- | --- |
| 认人厅登录 | `loginWithPassword` → `/sso/login` + Cookie |
| 厅堂密码登录 | `/auth/login` + 业务 Token（L-014） |
| 认人厅授权 | `requestAuthorize` → GET authorize JSON |
| 厅堂换票 | `POST /sso/oauth2/token` + verifier |
| 未登录（探测） | `fetchSession` 的 `false` |
| 未登录（授权） | `loginRequired === true` 且 HTTP 仍 200 |

- **禁用说法：** 「`requestAuthorize` 换票」「`fetchSession` 返回用户」「`parseAuthorizeQuery` 校验 S256」「服务器 302 出认人厅」「空 `VITE_SSO_API` 漏配」「`ssoLoginMethods` 运行时闸」「认人 Cookie 等于 Admin-Token」「在 sso-web 写 `services.ts` 才算做完」。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web,sso-web}` | 三 App 存在；sso-web 无 `application/`、无 domain 依赖 | 各 App 根与 `package.json` | 2026-09-17 |
| S-006 | `frontend/packages/platform/auth/src/index.ts` | `buildSsoAuthorizeUrl` / `startSsoLogin` / `handleCallback`；verifier 留在厅堂 | `computeS256Challenge`；`createSsoAuth` | 2026-09-17 |
| S-012 | child `2026-09-14-wta-sso` course / L-001…L-003 | 后端 authorize / token / session 切片；本课只对照不复述五层 | `children/2026-09-14-wta-sso/` | 2026-09-17 |
| S-SSO-01 | `frontend/apps/sso-web/src/ssoApi.ts`、`ssoApi.test.ts` | 四支枪 + `apiUrl` + `ssoLoginMethods`；Cookie fetch；查询无 secret | 整文件 | 2026-09-17 |
| S-SSO-02 | `src/main.ts`、`AuthorizePage.vue`、`AGENTS.md`、`.env.development` / `.env.production`、`vite.config.ts` | 三路由同一页；门脸因果链；空前缀同源；`/sso` 代理 | 工作树 | 2026-09-17 |
| S-L058-01 | `ssoApi.ts` `parseAuthorizeQuery` / `SsoAuthorizeQuery` | 蛇形抄驼峰；缺省 `'code'`；无 verifier / secret | 第 3–22 行 | 2026-09-17 |
| S-L058-02 | `ssoApi.ts` `fetchSession` / `loginWithPassword` / `requestAuthorize` / `readJson` | 三 URL；`code === 200`；`loginRequired === true`；不看 `response.ok` | 第 29–68 行 | 2026-09-17 |
| S-L058-03 | `AuthorizePage.vue` | `onMounted` / `submit` / `continueAuthorize`；默认用户名 WTA；`assign` | script 第 22–77 行 | 2026-09-17 |
| S-L058-04 | `apps/{admin,home}-web/src/application/sso.ts` | 厅堂换票 `POST /sso/oauth2/token`；pending 键；`/sso/callback` | 各文件全文 | 2026-09-17 |
| S-L058-05 | `SsoOAuthController.authorize`；`SsoAuthorizeVo`；`SsoSessionController.login/session` | JSON `loginRequired`/`redirectUri`；登录 `R.ok()` + Set-Cookie；session `R.fail("未登录")` | 两控制器与 VO | 2026-09-17 |
| S-L058-06 | `AuthController.clientContext`；`SsoProperties.webOrigin`；`e2e/sso-three-gates.spec.ts` | 路牌 `{origin}/authorize`；复用会话不再出密码表；Client 隔离 | context 第 205–211 行；e2e AC-002/003 | 2026-09-17 |

*生成依据：L-contract / OBJ-58 / 仓库源码核实。*
