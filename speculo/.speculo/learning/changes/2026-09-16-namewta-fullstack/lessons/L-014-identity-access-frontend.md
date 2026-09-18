---
lesson_id: L-014
objective_ids: [OBJ-14]
claimed_cells: [A:IdentityAccessService.login,prepareLogin,logout,register,getInfo,getMenus, B:session-store]
estimated_minutes: 37
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: method-table-on-disk
    minutes: 10
  - segment: session-namespace
    minutes: 6
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 6
expression_level: eli5
coverage_depth: deep
source_ids: [S-002, S-005, S-006, S-L014-01, S-L014-02, S-L014-03, S-L014-04, S-L014-05, S-L014-06]
---

# Lesson 014：通行证抽屉——IdentityAccessService 怎么打到 `/auth/*`

## 学完你能做什么

打开 `frontend/packages/domains/admin/src/index.ts` 的 `createIdentityAccessService`，你能**口述六枪怎么出门、哪一枪会往抽屉里塞钥匙**。口试名单就是这六枪 + 抽屉，**不是**矩阵 (a) 同行尾巴上的 `social*`：

1. **`prepareLogin`**：先问门开不开，再领验证码。两枪都是 **GET**，都标 `isToken: false`。路径是 **`/auth/client/context`** 然后 **`/auth/code`**。**不写**前端 session。
2. **`login`**（密码交卷）：必须先 `prepared===true`，再 **POST `/auth/login`**。body 带本厅堂的 `clientId` 和 `grantType: 'password'`。`(b)` 闭合：**只有** `parseAccessToken` 读到非空蛇形 **`access_token`** 之后，才对注入的 `SessionStore` 调 `setToken`。没预备、缺字段、不是字符串、全空白 → `invalid-login-response`，抽屉不动。
3. **`register`**：同样要预备。本地先看 `registerEnabled` 和密码公示牌，再 **POST `/auth/register`**。源码里**没有** `session.setToken`。
4. **`logout`**：**POST `/auth/logout`**（这一枪**带**当前票），成功后 `session.clear()`，并把 `prepared` / `context` 清掉。
5. **`getInfo`**：不打 `/auth/*`。交给注入的 **`identity.loadInfo()`**。App 塞进去的是 `systemService.identity`，真实 URL 是 **`GET /system/user/getInfo`**。
6. **`getMenus`**：同样走身份口 **`identity.loadMenus()`**，真实 URL 是 **`GET /system/menu/getRouters`**。OBJ-14 原文没点这两枪；本课认进同一格子，口试要能指 URL，不要推给 L-019。

你还能指着两把**厅堂钥匙名**，而不是把它们说成同一种东西：

- 工作树里 admin-web 的抽屉钥匙是 **`Admin-Token`**（`apps/admin-web/src/application/session.ts`）；home-web 是 **`Home-Token`**（`apps/home-web/src/application/session.ts`）。都由 `createBrowserSessionStore({ key })` 写进 `localStorage`。
- 厨房还导出 **`createClientSessionKey(appId, clientId)`**，拼出 `namewta:<app>:<client>:access-token`。它是**按 App + Client 命名的公式**。2026-09-16 全仓引用只有导出、再导出、测试；两个厅堂的 `session.ts` **没有**拿返回值当 `key`。测试故意断言公式结果 **不是** `Admin-Token`。

预备旗怎么竖起来，**按厅堂对照磁盘**，不要按「登录页 / 注册页」猜：admin 静态 `/login` 与 `/register` 都拆成 `getClientContext` + `getVerification`；home `/login` 与 `/register` 都走一次 `prepareLogin()`。对照表在机制 A。

本课认格子：`A:IdentityAccessService.login,prepareLogin,logout,register,getInfo,getMenus`、`B:session-store`。

同工厂还有 `socialLogin` / `socialCallback` / `social.bindingUrl` / `social.unlock`。它们打回 L-011 那几扇窗；`socialLogin` 用 `ensureClientContext`，**不**看 `prepared`，解析到票也 `setToken`。本课点到 URL 和「callback 看见非空 `access_token` 才会 `setToken`」即可。**社交四方法不是 OBJ-14 口试，也不随六枪标 covered。** sso-web、Sa-Token Redis、验证码 Redis 键、密码策略字段表，分别是 L-008 / L-013 / L-012。

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`createIdentityAccessService({ http, session, client, identity })`。L-011 认过后端六扇窗。本课站在**厨房这一头**，顺着方法走到 URL，再看钥匙写进哪一只抽屉。

```text
浏览器页面 / Pinia
        │  调用 identityAccessService.*
        v
createIdentityAccessService          ← domain-admin（一份厨房）
        │
        ├─ http.request(...)         ← 厅堂的 axios adapter（adminHttp / homeHttp）
        │     GET  /auth/client/context
        │     GET  /auth/code
        │     POST /auth/login
        │     POST /auth/register
        │     POST /auth/logout
        │
        ├─ identity.loadInfo/Menus   ← App 注入的 system 身份口
        │     GET  /system/user/getInfo
        │     GET  /system/menu/getRouters
        │
        └─ session.setToken/clear    ← 厅堂注入的 SessionStore
              admin-web → localStorage['Admin-Token']
              home-web  → localStorage['Home-Token']
```

| 方法 | HTTP | 动词 | 带 token？ | 写前端抽屉？ |
| --- | --- | --- | --- | --- |
| `prepareLogin` | `/auth/client/context` 然后 `/auth/code` | GET + GET | 否（`isToken: false`） | 否 |
| `login` | `/auth/login` | POST | 否 | **是**（`parseAccessToken` 拿到非空 `access_token` **之后**） |
| `register` | `/auth/register` | POST | 否 | **否** |
| `logout` | `/auth/logout` | POST | **是**（默认带头） | **清**（HTTP 成功之后） |
| `getInfo` | `/system/user/getInfo` | GET | 是（走 identity 口，默认带头） | 否 |
| `getMenus` | `/system/menu/getRouters` | GET | 是 | 否 |

**类比：** 厨房有一台对讲机（`http`）、一只别人塞进来的档案柜钥匙（`identity`）、一只本厅堂的抽屉（`session`）。对讲机喊门厅（`/auth/*`）办通行证；档案柜钥匙开的是系统室的「我是谁 / 我能进哪些门」（`/system/user/getInfo`、`/system/menu/getRouters`）。抽屉上贴的店名是厅堂自己写的：管理店写 `Admin-Token`，用户中心写 `Home-Token`。两家店共用同一套对讲词，**不能共用同一只抽屉**，否则两个人的通行证会挤在同一个格子里。

**类比失效处：**

1. 对讲机不是 axios。domain 只认 `HttpClient.request`。真出门的插头是 App 买的 `@namewta/adapter-axios-browser`（L-010）。
2. 「抽屉」不是 Redis。服务端通行证在 Sa-Token 的 Redis 柜（L-013）。本课的抽屉是**浏览器 localStorage**。两边都叫 session，必须带前缀：前端 session store / 服务端 token-session。
3. `createClientSessionKey` 不是厅堂正在用的抽屉标签。它是厨房打印标签的**公式**。今天两家店用手写的 `Admin-Token` / `Home-Token`。不要口述成「登录写入 `namewta:admin-web:<clientId>:access-token`」——工作树不是这样。公式未接线 ≠ 公式不存在；口试要能同时指「今天的字节」和「未贴上的标签」。
4. home `/login`（web-domain）用的 TypeScript 接口可以只叫 `IdentityAccessService`（`login` + `prepareLogin`）。工厂实际返回的是更长的 **`IdentityAccessManagementService`**（加上 logout/register/getInfo/getMenus/social）。admin 静态登录页、Pinia 和注册页用的是长接口。不要把两个名字说成两个工厂。

## 核心概念与机制

### 直觉讲解

小孩子版只记七句：

1. **先问门，再交卷。** `login` / `register` 看见 `prepared===false` 就停，**连 POST 都不发**。
2. **问门是两枪。** `prepareLogin` = context + 验证码。验证码坏了，门也算没问成。
3. **交卷成功才把纸条塞进抽屉。** JSON 字段名是蛇形 `access_token`，不是 `accessToken`。`login` 先解析、后 `setToken`；解析失败抽屉不动。
4. **办户口本不发通行证。** `register` 成功只让你去登录页，抽屉空着。
5. **菜单不在门厅。** `getMenus` 打系统室的 `getRouters`。厨房自己不会拼这串 URL，是 App 把 system 的身份口塞进来的。
6. **每家店一把钥匙名。** 管理端 `Admin-Token`，门户 `Home-Token`。公式 `createClientSessionKey` 是备用标签机，今天没贴到抽屉上。
7. **退出先跟门厅说一声，再倒空抽屉。** 门厅那一声失败，厨房这一层**不会** `clear`。管理端 Pinia 也没有 `finally` 倒空；门户 Pinia 有。

再补一句防环：厅堂造服务时把 `http` 包成箭头函数，是为了躲开「HTTP 恢复、路由清单、服务组合」抢着初始化（L-007）。本课不把这句再展开，只要知道 domain 拿到的是**已经接好的** `HttpClient`。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 身份访问服务 | IdentityAccessService | `domain-admin` 里较短的口：`client` + `prepareLogin` + `login`。home 登录 web-domain 的 runtime 只认这一截 |
| 身份访问管理服务 | IdentityAccessManagementService | 工厂真实返回值。在短口上再加 `getClientContext` / `getVerification` / `register` / `logout` / `getInfo` / `getMenus` / `social*` |
| 工厂 | `createIdentityAccessService` | 纯函数。注入 `client`、`http`、`identity`、`session`、可选 `encryptLoginRequest`（默认 `true`）。返回 `Object.freeze` 的管理服务 |
| 预备旗 | `prepared` | 工厂闭包里的布尔。`prepareLogin` 走完验证码解析才变 `true`。`getClientContext` / `logout` / 下一次预备开头会把它打回 `false` |
| 客户端上下文 | client context / `ClientAuthContext` | `GET /auth/client/context` 的前端投影。`clientEnabled` 必须是布尔 `true`，否则抛 `client-context-unavailable`。后端这一枪仍可能是 `R.ok` + 关旗（L-011） |
| 登录预备 | `prepareLogin` | 连续 `loadClientContext` + `loadVerification`，返回 `{ context, verification }` |
| 密码登录 | `login` | `POST /auth/login`，body 含 `username`/`password`/可选 `code`+`uuid`/`clientId`/`grantType:'password'`。`parseAccessToken` 只认非空 `access_token`（`trim` 只当空闸，返回原字符串），**然后** `session.setToken` |
| 公开注册 | `register` | `POST /auth/register`。本地闸：`prepared`、`registerEnabled`、密码策略、可选确认密码。成功**不**写抽屉 |
| 退出 | `logout` | `POST /auth/logout` 成功后再 `session.clear()`，并丢掉闭包里的 context / prepared |
| 身份信息 | `getInfo` / `IdentityInfo` | 调 `identity.loadInfo()`，解析 `user`（必须是对象）以及 `roles`/`permissions` 字符串数组 |
| 服务端菜单 | `getMenus` / `ServerMenuNode` | 调 `identity.loadMenus()`，递归冻结菜单树。`path` 必填且非空白 |
| 身份口 | identity port | 注入对象 `{ loadInfo(): Promise<unknown>; loadMenus(): Promise<unknown> }`。admin/home 都塞 `systemService.identity` |
| 会话仓库 | SessionStore | 平台端口：`getToken()` / `setToken(token)` / `clear()`。**不是** `SessionPort.logout()` |
| 浏览器会话仓库 | `createBrowserSessionStore` | adapter：用一只 `key` 读写 `localStorage`（失败则内存兜底） |
| 厅堂钥匙名 | App session key | 工作树：`Admin-Token`、`Home-Token`。按 **App** 隔离，不按 Client 隔离 |
| 规范会话键 | `createClientSessionKey` | `namewta:${encodeURIComponent(appId)}:${encodeURIComponent(clientId)}:access-token`。空 `appId` 抛错；`clientId` 走 `requireClientContext`（会 trim） |
| 传输加密头 | `isEncrypt` | login/register 请求头。值来自工厂选项，App 用 `VITE_APP_ENCRYPT === 'true'` 对齐后端 `@ApiEncrypt` |
| 跳过令牌头 | `isToken: false` | 告诉 axios adapter **不要**贴 `Authorization`。预备、登录、注册要它；logout / getInfo / getMenus 不要它 |
| 身份访问错误 | `IdentityAccessError` | 带 `code` 与可选 `violations`。本课会碰到的 code 见机制表 |

**Login ≠ register。** 一个往抽屉塞钥匙，一个只办户口。

**prepareLogin ≠ getClientContext。** 后者只问门，并把 `prepared` 打回 false；前者问完门还要领验证码，才把 `prepared` 置 true。

**SessionStore ≠ SessionPort。** 一个管抽屉里的字符串；一个是 401 时「请你去登出」的端口。axios 的 `onUnauthorized` 用的是 `SessionPort`。

**`accessToken`（TS）≠ `access_token`（JSON）。** 解析只认蛇形。写错字段名会被当成 `invalid-login-response`，抽屉不动。

### 机制/因果链

工厂一造好就做两件事：`requireClientContext(clientInput)`（空白 `clientId` 当场抛 `ClientContext.clientId is required`，**零 HTTP**），然后把 trim 后的 Client 冻住。闭包里 `prepared=false`，`context=undefined`。

#### A. `prepareLogin`：两枪，不写抽屉

1. `prepared = false`。
2. `loadClientContext`：再把 `prepared`/`context` 清掉，然后 `GET /auth/client/context`，头 `{ isToken: false }`。`parseClientAuthContext`：必须是对象；`clientEnabled`/`registerEnabled` 必须是布尔；**`clientEnabled` 不是 true 就抛 `client-context-unavailable`**。后端关旗仍是 `R.ok`（L-011）；前端把关旗当成错误，后面的 login/register 发不出去。
3. `loadVerification`：先 `prepared=false`；没有 `context` 直接抛同一错误；然后 `GET /auth/code`，头 `{ isToken: false }`，`timeout: 20000`。`captchaEnabled===false` 可以没有图。`true` 时 `img`/`uuid` 必须是 trim 后非空字符串，否则 `invalid-verification-response`，**`prepared` 保持 false**。
4. 两枪都过：`prepared=true`，返回冻住的 `{ context, verification }`。

因果：验证码坏了，等于没预备成。测试写明随后的 `login()` 只会再抛 `client-context-unavailable`，请求数组里**没有** `/auth/login`。

竖旗有两条磁盘路径，都能把 `prepared` 置 true——因为 `getVerification` / `loadVerification` 成功结尾写 `prepared=true`。只调用 `getClientContext` 就去 `login`/`register`，会撞上 `!prepared`。**哪一页走哪条，按厅堂，不要按「登录 vs 注册」：**

| 厅堂 | 路由 | 磁盘文件 | 怎么竖旗 | 提交 |
| --- | --- | --- | --- | --- |
| admin-web | 静态 `/login` | `apps/admin-web/src/views/login.vue`（`router/index.ts` 白名单直挂这页） | `getClientContext()` 再 `getVerification()` | Pinia `useUserStore().login` → domain `login` |
| admin-web | `/register` | `apps/admin-web/src/views/register.vue` | 同样拆枪；`clientEnabled` 或 `registerEnabled` 为假则 `router.push('/login')` | domain `register` |
| home-web | `/login` | web-domain `LoginPage.vue` ← `createIdentityLoginState.prepare()` | 一次 `runtime.service.prepareLogin()` | `runtime.service.login` |
| home-web | `/register` | `apps/home-web/src/views/RegisterPage.vue` | `onMounted` 一次 `prepareLogin()`；**不**读 `registerEnabled` | domain `register`（关旗在提交时撞 `registration-disabled`） |

admin 的 `createAdminWebDomain` 也登记了 `identity-access/login/index`，但静态白名单登录页**不是**它。home 的 `router/index.ts` 才 `resolveHomeWebRegistration('identity-access/login/index', 'identity-access')`。拆枪能竖旗仍对；「登录页 = `prepareLogin`、注册页 = 拆枪」与磁盘相反。

#### B. `login`：交卷并写抽屉

1. `if (!prepared) throw clientContextError()` —— **此时零 HTTP、零 `setToken`。**
2. `requireCredentials`：`username.trim()` 与 `password` 缺一不可，否则 `invalid-credentials`（「请输入用户名和密码」）。
3. `POST /auth/login`，头 `{ isToken: false, isEncrypt: encryptLoginRequest, repeatSubmit: false }`。body：用户名密码、有则带 `code`/`uuid`、**冻住的** `clientId`、`grantType: 'password'`。测试证明注入 `' client-proof '` 会变成 `'client-proof'`。
4. `parseAccessToken(response.data)` 只读 **`access_token`**。缺、不是字符串、或 `!token.trim()` → `invalid-login-response`，**不** `setToken`。闸用 `trim` 判空，**返回值不 trim**：带空格的票会原样进抽屉。
5. **然后** `session.setToken(accessToken)`，返回 `{ accessToken }`。HTTP 200 但没有可用票，抽屉仍空。

因果：前端抽屉写在**解析成功之后**，不是 POST 一出门就写。后端 Redis 柜写在策略里（L-013）。缺 Client 的后端红章到不了 `setToken`；前端没预备则连红章都碰不到。同工厂的 `socialLogin` / 条件 `socialCallback` 也是写手，**不是本格口试**。

`login` **不**跑 `validatePassword`。密码策略挡的是注册（以及改密，L-012 / L-015），不是这条交卷枪。

管理端 Pinia `useUserStore().login` 只是 `identityAccessService.login` 再把返回值抄进 `token` ref。真写 localStorage 的是 domain 里那一次 `setToken`。门户同样。

#### C. `register`：本地闸 + 办证，不写抽屉

1. `!prepared || !context` → `client-context-unavailable`，零 HTTP。
2. `context.registerEnabled` 为假 → `registration-disabled`，请求停在 context+code。
3. `requirePasswordPolicy(context)` 失败 → `password-policy-unavailable`。
4. `validatePassword` 有违例 → `password-policy-violation`（带 `violations`），**不发** POST。
5. 若传了 `confirmPassword` 且不等于 `password` → `invalid-credentials`（「两次输入的密码不一致」）。没传确认密码则跳过这一闸。
6. `POST /auth/register`，头与 login 相同（无 token、可加密、`repeatSubmit: false`）。body：**没有** `grantType`（后端 `RegisterBody` 构造器自己填 `"password"`，L-011），有 `clientId`。
7. 函数结束。全函数搜不到 `session.setToken`。综合测试「owns registration…」**没有** `expect(session.setToken).not.toHaveBeenCalled()`；负证据在源码，不在那则 expect。

因果：弱密码在浏览器就被拦住，门厅的 `SysRegisterService` 根本看不见。办成了也要再走 B。admin 注册页成功后 `router.push('/login')`；home `RegisterPage` 成功后跳 `${VITE_APP_CONTEXT_PATH}login`。都是「办证不发通行证」的 UI 翻译。

#### D. `logout`：先通知门厅，再倒抽屉

`POST /auth/logout`，**没有** `isToken: false`，所以 adapter 会贴 `Authorization: Bearer <当前票>`（有票的话）。await 成功后：`session.clear()`；`prepared=false`；`context=undefined`。

因果：HTTP 抛错则这两行清理**不跑**。管理端 `user.ts` 在 `await identityAccessService.logout()` **之后**才 `removeToken()`，没有 `try/finally`——门厅失败时 `Admin-Token` 可能还在。门户 `user.ts` 用 `try/finally` 再 `removeToken()`，抽屉会被厅堂自己倒空。本课认 domain 这一层的合同：**成功才 clear**；Pinia 的 finally 是厅堂加的第二道扫帚。

#### E. `getInfo` / `getMenus`：系统室，不是门厅

```ts
getInfo: async () => parseIdentityInfo(await identity.loadInfo()),
getMenus: async () => parseMenus(await identity.loadMenus()),
```

`createSystemService` 的身份口（admin/home 都这样塞）：

- `loadInfo` → `GET /system/user/getInfo`，取 `.data`
- `loadMenus` → `GET /system/menu/getRouters`，取 `.data`

解析闸：`user` 必须是非数组对象，否则 `invalid-identity-response`。`roles`/`permissions` 不是「全是非空字符串的数组」时，**降成空数组**，不抛。菜单：根必须是数组；每个节点 `path` 必填且 trim 后非空；`children` 若出现必须是数组；`meta.noCache` 若出现必须是布尔。坏菜单 → `invalid-menu-response`，消息带 JSON 路径，例如 `menus[0].children[0].path`。好菜单整棵树 `Object.freeze`。`null` 的可选字段会被丢掉，不当错误（测试认过后端 Java 的 nullable 运输）。

因果：这两枪默认**带 token**。没登录就去 `getRouters`，过的是后端登录闸，不是本工厂的 `prepared` 旗。e2e 顺序仍是 login → getInfo → getRouters（L-006）。导航店 `navigation.ts` 的 `generateRoutes` 只调 `getMenus()`，不自己拼 URL。

#### F. `B:session-store`：谁拥有钥匙名

平台端口（`@namewta/platform-contracts`）：

```ts
interface SessionStore {
  clear(): void;
  getToken(): string | null;
  setToken(token: string): void;
}
```

浏览器实现：`createBrowserSessionStore({ key, storage? })` 内部就是 `createBrowserTokenStorage`。默认 `storage` 是 `localStorage`；读/写/删 **catch 全部异常** 后改走内存 `fallbackValue`，调用方看不到异常。测试用 `SecurityError` / `QuotaExceededError` 当例子，**不是**类型过滤。没有 `localStorage` 的运行时（测试里的 SSR 形状）也走内存。

厅堂接线：

| App | 文件 | `key` |
| --- | --- | --- |
| admin-web | `frontend/apps/admin-web/src/application/session.ts` | `'Admin-Token'` |
| home-web | `frontend/apps/home-web/src/application/session.ts` | `'Home-Token'` |

两份文件都 `export const session = createBrowserSessionStore({ key })`，再包 `getToken` / `removeToken`（admin 还包 `setToken`）。`services.ts` 把**同一只** `session` 交给 `createIdentityAccessService`。OSS 客户端另外借 `session.getToken`（L-007），不另造抽屉。

厨房公式：

```ts
createClientSessionKey(appId, clientId)
// namewta:${encodeURIComponent(appId.trim())}:${encodeURIComponent(client.clientId)}:access-token
```

测试：`'fixture-web' + ' fixture-proof '` → `namewta:fixture-web:fixture-proof:access-token`；斜杠和空格会进百分号编码；空 `appId` 抛 `App id is required`；结果 **≠** `'Admin-Token'`。

2026-09-16 全仓 `createClientSessionKey` 的引用：导出、再导出、测试。**没有** App 把它的返回值传给 `createBrowserSessionStore`。所以：

- **按 App 隔离**——今天成立，靠手写 `Admin-Token` / `Home-Token`。
- **按 Client 隔离**——公式支持，厅堂未接。同一浏览器里两个 Client 共用一个管理端，会抢同一只 `Admin-Token`。
- **规范键字符串**——存在，是教学上的「应该能叫出的公式」，不是当前 localStorage 里的字节。
- adapter 隔离测试里第二把钥匙是 `Client-Token`；厅堂真实第二把是 home `session.ts` 的 **`Home-Token`**。不要把测试夹具名说成门户钥匙。

admin `login.vue` 的「记住我」另外写 `username` / `rememberMe` 进 localStorage，**不是** SessionStore 钥匙，不要口述成 `Admin-Token`。

axios 读票：`getToken` 来自同一只 store。`headers.isToken !== false` 时贴 `Authorization`。HTTP 401 走 `SessionPort.logout`（管理端会调 Pinia `logout()`，门户直接 `session.clear()`），那是过期扫帚，不是本课的 `IdentityAccessService.logout`。

同工厂社交（**非口试**，点到为止）：`socialLogin` → `POST /auth/login` + `grantType:'social'`，解析到票就 `setToken`（用 `ensureClientContext`，**不**要求 `prepared`）。`socialCallback` → `POST /auth/social/callback`；当前后端不发新票（L-011）；前端若看见非空 `access_token` 仍会 `setToken`（兼容分支，测试覆盖）。`social.bindingUrl` → `GET /auth/binding/{source}`；`social.unlock` → `DELETE /auth/unlock/{id}`。这四只方法不进六枪口试，也不把矩阵 (a) 的 `social*` 尾巴标成本课 covered。

## 图、表或文本图

**图题 / caption：** 六枪出门。alt：prepareLogin 两枪无 token；login 写抽屉；register 不写；logout 先 POST 再 clear；getInfo/getMenus 走 system。

```text
                    createIdentityAccessService 闭包
                    prepared / context / 冻住的 clientId
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
        v                          v                          v
  prepareLogin               login / register              getInfo / getMenus
  GET /auth/client/context   须 prepared===true            identity.load*
  GET /auth/code             login : POST /auth/login      GET /system/user/getInfo
  isToken:false              register: POST /auth/register GET /system/menu/getRouters
  不碰 session               isToken:false, isEncrypt?     默认带 Bearer
        │                    login 解析 access_token            │
        │                    → session.setToken                │
        │                    register 绝不 setToken            │
        v                          │                          │
  prepared=true                    v                          v
                           logout: POST /auth/logout     解析冻结后交给 Pinia / 导航
                           （带 token）
                           成功 → session.clear()
                                 prepared=false
```

**文字等价物：** 图中央是工厂闭包里的预备旗和 Client。左边 `prepareLogin` 连续打门厅两枪公开接口，都不带旧票，也不改抽屉；两枪解析成功才把预备旗竖起来。中间 login/register 先看旗。login 把蛇形 `access_token` 塞进注入的 `SessionStore`；register 即使 HTTP 成功也不碰抽屉。右边 getInfo/getMenus 不经过 `/auth`，它们打电话给 App 塞进来的 system 身份口。logout 单独一列：先带着旧票通知门厅，成功才倒空抽屉并放倒预备旗。

**图的边界：** 不画 Captcha Redis 键、BCrypt、`LoginHelper.login`（L-012 / L-013）。不画 sso-web 的 `/sso/login`。不把 `createClientSessionKey` 画成 localStorage 的实际 key。不画社交四方法的完整分流（非口试；细节 L-011 / L-022）。不保证每个 App 的 Pinia 在 HTTP 失败时都会 `clear`。不把 admin 静态 `/login` 画成 `prepareLogin()`。

**图题 / caption：** 两只抽屉与一张未贴上的标签。alt：Admin-Token 与 Home-Token 隔离；公式键未被 session.ts 使用。

```text
localStorage
 ┌─────────────────────┐     ┌─────────────────────┐
 │ Admin-Token         │     │ Home-Token          │
 │  (admin-web)        │     │  (home-web)         │
 │  createBrowser-     │     │  同一 adapter，     │
 │  SessionStore       │     │  另一把 key         │
 └─────────▲───────────┘     └─────────▲───────────┘
           │                           │
           │  session.setToken         │
           │  / clear / getToken       │
           │                           │
  identityAccessService (domain-admin，一份厨房，两家厅堂各造一次)
           │
           └── 另有公式（未接到上面两只抽屉）
               createClientSessionKey('admin-web','pc-client')
               → namewta:admin-web:pc-client:access-token
               测试：该字符串 !== 'Admin-Token'
```

**文字等价物：** 浏览器里有两只互相看不见的格子，格子名是厅堂写死的。管理端登录把票放进 `Admin-Token`；门户放进 `Home-Token`。同一份 `createIdentityAccessService` 源码被两家各调用一次，各自注入自己的 store，所以不会在源码里写死钥匙名。厨房另外打印了一种「店名+门牌」标签，今天没有贴到 localStorage 上。把公式结果当成当前字节，就是把标签机说明书当成已经贴好的标签。

**图的边界：** 不讨论 Cookie / HttpOnly。adapter 在 storage 抛错时改内存，图上仍画 localStorage 作为主路径。sso-web 没有这只抽屉（L-008）。Pinia 的 `token` ref 是格子内容的复印件，刷新后要再 `getToken()` 才能对齐——门户 getInfo 开头有 `token.value = getToken()`，管理端 login 后靠复印件。

## 正例、反例与边界

**正例 1：** 口述预备两枪。打开 `index.ts` 的 `prepareLogin`：先 `loadClientContext`（`GET /auth/client/context`），再 `loadVerification`（`GET /auth/code`，timeout 20000）。两枪头都是 `isToken: false`。对照 `index.test.ts`：`requests` 恰好这两项。

**正例 2：** 没预备就登录。测试「fails closed when client context is invalid」：`prepareLogin` 拒绝后 `login` 再拒绝，`requests` 仍只有 context，没有 `/auth/login`，`setToken` 零次。

**正例 3：** 交卷写抽屉。测试「uses the validated clientId in login payload and writes only the injected session」：先 `prepareLogin`，再 POST `/auth/login`，body `grantType: 'password'`，**然后** `setToken('client-token')`。断言打在解析成功之后；没有 `access_token` 的路径不会走到这则 `toHaveBeenCalledWith`。

**正例 4：** 注册不写抽屉。打开 `index.ts` 的 `register`：POST `/auth/register` 之后函数结束，全文无 `session.setToken`。综合测试「owns registration…」能证明请求列表有 `/auth/register`、随后 `getInfo`/`getMenus` 走假 identity 口（`identityCalls` 为 `['info','menus']`，HTTP 无 `/system/*`）、`logout` 后 `clear` 一次；**该 `it` 没有** `expect(session.setToken).not.toHaveBeenCalled()`。负证据在源码。同一测试里 `socialCallback` 的 data 为空，也不会写——不要把这则 `it` 说成已经断言 register 不写 session。

**正例 5：** 指系统室 URL。打开 `createSystemService` 的 `identity.loadInfo` / `loadMenus`：`/system/user/getInfo`、`/system/menu/getRouters`。再打开 admin `navigation.ts`：`identityAccessService.getMenus()`，没有手写 axios。

**正例 6：** 厅堂钥匙。`admin-web/.../session.ts` 一行 `key: 'Admin-Token'`；`home-web/.../session.ts` 一行 `key: 'Home-Token'`。home 的 `session.test.ts` 标题就是 `home session namespace`。

**正例 7：** 公式不是 `Admin-Token`。`createClientSessionKey('fixture-web','fixture-proof')` 的断言写在 `index.test.ts`。导出路径：`domains/admin/src/index.ts` 与 `auth/index.ts` 再导出。

**正例 8：** 关旗在前端是错误。`parseClientAuthContext` 在 `clientEnabled` 不是 `true` 时抛 `client-context-unavailable`。这与 L-011「后端 context 仍 `R.ok`」并排成立，不要吞掉任何一边。

**正例 9：** 弱密码不发注册。测试「returns stable policy violations without sending a weak registration request」：只有 context+code 两枪。

**正例 10：** 退出清抽屉。同一综合测试末尾 `logout` 后 `session.clear` 恰好一次。请求最后一项是 `/auth/logout`。

**正例 11：** 预备调用面按厅堂。admin `views/login.vue`：`loadClientAuthContext` → `getClientContext`，`getCode` → `getVerification`，提交 `userStore.login`。admin `views/register.vue`：同样拆枪，关旗 `router.push('/login')`。home `router/index.ts` 把 `/login` 接到 web-domain `identity-access/login/index`，`createIdentityLoginState.prepare()` → `prepareLogin()`。home `RegisterPage.vue`：`onMounted(prepare)` 一次 `prepareLogin()`，本地不看 `registerEnabled`。

**正例 12：** 登录 web-domain 只认短口。`IdentityAccessWebRuntime.service` 的类型是 `IdentityAccessService`。`createIdentityLoginState` 只调 `prepareLogin` 与 `login`。**home `/login` 才走这条短口**；admin 静态 `/login` 走 `views/login.vue` + Pinia。长方法由 App 的 Pinia / 注册页 / 导航店调用。

**反例 1：** 「`getMenus` 打 `/auth/menus` 或 `/system/menu/list`。」工作树是 **`/system/menu/getRouters`**。`/list` 是菜单管理 CRUD（L-016 / L-019）。

**反例 2：** 「`getInfo` 打 `/auth/info`。」工作树是 **`/system/user/getInfo`**。

**反例 3：** 「登录写入 `namewta:admin-web:<client>:access-token`。」厅堂 key 是 `Admin-Token` / `Home-Token`。`createClientSessionKey` 存在但未接到任何 `session.ts`。

**反例 4：** 「admin 和 home 共用 `Admin-Token`，反正都是 NAMEWTA。」两把钥匙。混用会让管理端登录把门户踢掉，或反过来。

**反例 5：** 「`register` 成功会 `setToken`，因为已经有账号。」源码无 `setToken`。UI 跳回登录页。

**反例 6：** 「`login` 会先跑密码策略。」`login` 只 `requireCredentials` + POST。`validatePassword` 在 `register`。

**反例 7：** 「domain 自己 `import { session } from 'admin-web'`。」工厂吃注入的 `SessionStore`。厨房测试用 `vi.fn()` 就能造。

**反例 8：** 「`prepareLogin` 只打 context。」它必须再打 `/auth/code`。只打 context 的是 `getClientContext`，而且会把 `prepared` 打回 false。

**反例 9：** 「logout 不需要票，因为门厅 `@SaIgnore`。」前端这一枪**不**设 `isToken: false`，有票就会带。门厅未登录也回成功句（L-011）；前端仍尽量带票，好让服务端撕对那一格 Redis。

**反例 10：** 「`IdentityAccessService` 四个字母接口就是工厂返回值。」工厂返回 `IdentityAccessManagementService`。短接口给登录页。

**反例 11：** 「`SessionStore` 就是 `SessionPort`。」后者只有 `logout()`。401 扫帚用它。

**反例 12：** 「社交 callback 一定会写抽屉。」只有 data 里出现非空 `access_token` 才写。当前 Java 这扇窗不发新票。

**反例 13：** 「sso-web 也 `createIdentityAccessService`。」认人厅没有这份工厂（L-008）。不要把 `/sso/login` 说成本课六枪。

**反例 14：** 「`createClientSessionKey` 会写 localStorage。」它只返回字符串。写字节的是 `session.setToken`，key 来自厅堂手写的 `Admin-Token` / `Home-Token`。

**反例 15：** 「登录页 = `prepareLogin`，注册页 = 拆枪。」磁盘是 **admin 两页都拆枪，home 两页都 `prepareLogin`**。打开 admin `views/login.vue` 找不到 `prepareLogin`；打开 home `RegisterPage.vue` 找不到 `getClientContext`。

**反例 16：** 「口试要把 `socialLogin` / `socialCallback` / `bindingUrl` / `unlock` 都背出来，因为矩阵 (a) 同行挂着 `social*`。」口试是六枪。社交写手不是本格；(a) 行名写宽了应备注 `not-in-L-014-oral`，不要扩课硬考。

**边界：**

- `encryptLoginRequest` 默认 true；测试里登录那条为了断言明文 body 才传 `false`。App 跟 `VITE_APP_ENCRYPT`。
- `confirmPassword` 可选。页面传了就要相等；API 调用方不传则跳过。
- `roles`/`permissions` 坏数据变空数组，**不**当 `invalid-identity-response`。缺 `user` 对象才抛。
- 菜单 `name: null` 等 Java nullable 运输被丢掉，不当格式错误。`path: null` / `path: '   '` / `children: {}` 才关。
- `getClientContext` 会撤销已经竖起的预备旗。测试「revokes a previous prepared state」：第二次 context 关旗后，login 不再发 POST。
- 本课不认格子的社交方法仍在同一 `Object.freeze` 返回值里。矩阵 A 行若挂着 `social*`，那是行名写宽了，**不是**口试扩成四只社交方法。口试主项仍是六枪 + 抽屉。

## 变式与迁移

- **变式 A：新厅堂要登录。** 复制的是工厂调用，不是 admin-web 的 `session.ts`。自己选一把 **不会和现有 App 撞名** 的 `key`，再 `createIdentityAccessService({ http, session, client, identity })`。需要按 Client 隔离时，把 `createClientSessionKey(appId, clientId)` 的返回值**第一次**传进 `createBrowserSessionStore({ key })`。今天两家店都还没这么做，迁移时要同时改测试，不要只改公式。

- **变式 B：只要登录页，不要注册/菜单。** 登录 web-domain 的类型已经是短口。仍建议注入完整工厂，因为同一单例还要给 Pinia 的 logout/getInfo。不要造第二个只含 `login` 的对象，否则抽屉和预备旗会分裂。

- **变式 C：admin 继续拆枪。** admin `login.vue` / `register.vue` 可以 `getClientContext` + `getVerification` 代替一次 `prepareLogin`，但顺序不能反，也不能省掉 verification——否则 `prepared` 仍是 false。刷新验证码应再调 `getVerification`（它会先把 `prepared` 放倒，成功再竖起）。home `RegisterPage.vue` **已经**走一次 `prepareLogin()`，不要改成拆枪「才算对」。

- **变式 D：关掉传输加密。** App 的 `VITE_APP_ENCRYPT` 与工厂 `encryptLoginRequest` 一起关。只关前端头、后端仍 `@ApiEncrypt`，过滤器会解不开 body（L-011 变式 F）。

- **变式 E：门户要吃 SSO 回跳写入的 `Home-Token`。** 门户 `getInfo` 开头 `token.value = getToken()`，就是从抽屉复印到 Pinia。不要在 getInfo 之前假设 Pinia 的初始 ref 仍有效。管理端登录路径靠 login 返回值赋值，刷新后初始 `ref(getToken())` 也能对上。

- **变式 F：logout HTTP 失败。** domain 不清抽屉。若产品要求「门厅已经 401 也要倒空」，在**厅堂**加 `finally { session.clear() }`（门户已有类似 finally），不要改成「HTTP 前先 clear」——那样门厅可能撕不到对应 Redis 格。

- **变式 G：菜单解析失败。** `invalid-menu-response` 发生在前端，后端可能已经 200。迁移时修的是运输形状或解析器，不是再发一次 `/auth/login`。

- **迁移口诀：** 先预备（context + code；admin 拆、home 一次 `prepareLogin`）→ 交卷解析到 `access_token` 才写抽屉 → 办证不写抽屉 → 菜单走 system 身份口 → 钥匙名归厅堂（`Admin-Token` / `Home-Token`），公式归厨房且今天未贴上 → 两家店两把钥匙。跳步会出现「前端以为已登录、localStorage 空」或「管理端票写进了门户格子」。

## 常见误区

1. **「身份服务的所有方法都打 `/auth/*`。」** `getInfo`/`getMenus` 打 system。预备的第二枪是 `/auth/code`，不是 login。
2. **「`prepareLogin` 可省，页面已经有验证码图。」** 没有 `prepared`，login/register 会抛 `client-context-unavailable`。
3. **「`client-context-unavailable` 等于后端 `R.fail`。」** 常常是后端 `R.ok` 关旗，前端自己停。
4. **「JSON 的 token 字段是 `accessToken`。」** 解析认 `access_token`。`login` 先解析、后 `setToken`。
5. **「注册成功就是已登录。」** 源码无 `setToken`，无 Pinia token，下一枪 `getRouters` 没有票。不要用综合测试那则 `it` 代替源码。
6. **「session key 叫 `createClientSessionKey` 的结果。」** 工作树是 `Admin-Token` / `Home-Token`。公式未接到 `session.ts`。
7. **「两家 App 可以 import 对方的 `session`。」** L-007 反例：会串抽屉。
8. **「domain-admin 依赖 domain-system，所以自己知道 getRouters。」** `package.json` 没有这条依赖。身份口是 App 注入的。
9. **「`login` 会校验密码策略。」** 不会。策略在 `register`（以及改密路径）。
10. **「logout 一定清掉 localStorage。」** domain 层要求 HTTP 成功。管理端 Pinia 无 finally。
11. **「`getVerification` 可以在没有 context 时单独打 `/auth/code`。」** 没有 `context` 直接抛，不发 code。
12. **「短接口 `IdentityAccessService` 没有 logout，所以工厂也不能 logout。」** 长接口才是返回值。
13. **「把 Pinia 的 `token` ref 当成 SessionStore。」** ref 是复印件。OSS、axios、`getToken()` 读的是 adapter 那只抽屉。
14. **「社交登录走 `/auth/social/login`。」** `socialLogin` 仍是 **`POST /auth/login`** + `grantType:'social'`。社交四方法不是六枪口试。
15. **「登录页一定 `prepareLogin`，注册页一定拆枪。」** admin 两页都拆；home 两页都 `prepareLogin`。

## 非评分暂停

打开磁盘，不要凭记忆默写 URL。不要改文件。没有标准答案栏。

1. 打开 `frontend/packages/domains/admin/src/index.ts`。用手指点 `prepareLogin`、`login`、`register`、`logout`、`getInfo`、`getMenus` 六处 `url` 或 `identity.load*`。把 `/auth/*` 和 `/system/*` 分成两堆。不要把 `social*` 算进这六处。
2. 在 `login` 里圈出 `parseAccessToken` 和它后面的 `session.setToken`。确认 `setToken` 在解析之后。在 `register` 里确认没有它。在 `logout` 里确认 `clear` 在 `await http.request` **之后**。
3. 打开 `frontend/apps/admin-web/src/application/session.ts` 与 `home-web` 的同名文件。把两把 `key` 写在纸上（`Admin-Token` / `Home-Token`）。再搜 `createClientSessionKey` 有没有出现在这两份文件里。
4. 对照四页调用面：admin `views/login.vue`、admin `views/register.vue`、home web-domain `LoginPage` / `loginState.ts`、home `views/RegisterPage.vue`。记下谁拆枪、谁 `prepareLogin`。
5. 打开 `frontend/packages/domains/system/src/service.ts` 的 `identity` 对象，核对 `getInfo`/`getMenus` 的真实 URL。
6. 打开 admin `store/modules/user.ts` 的 `logout` 和 home `store/user.ts` 的 `logout`，比较有没有 `finally`。想一句：门厅失败时，谁的抽屉可能还留着票。

## 总结、词汇表与下一步

- **工厂在 `frontend/packages/domains/admin/src/index.ts`。** 返回冻住的 `IdentityAccessManagementService`。home 登录页可以只看见短口；admin 静态登录页走 `views/login.vue` + Pinia。
- **口试是六枪，不是 `social*`。** `prepareLogin` = `GET /auth/client/context` + `GET /auth/code`；`login` = `POST /auth/login` 且**解析到非空 `access_token` 之后**写抽屉；`register` = `POST /auth/register` 且源码不写抽屉；`logout` = `POST /auth/logout` 成功后倒抽屉；`getInfo`/`getMenus` = system 身份口，`/system/user/getInfo` 与 `/system/menu/getRouters`。
- **预备旗是硬闸。** 没竖起来，login/register 零 POST。`getClientContext` 会放倒旗。竖旗调用面：admin 两页拆枪，home 两页 `prepareLogin`。
- **抽屉是厅堂的。** `Admin-Token` / `Home-Token` 经 `createBrowserSessionStore`。`createClientSessionKey` 是按 App+Client 拼标签的公式，今天未接到 `session.ts`。
- **前端 session ≠ Redis 会话。** 一边是浏览器格子，一边是 Sa-Token（L-013）。

词汇表：IdentityAccessService / IdentityAccessManagementService / `createIdentityAccessService` / prepared / client context / `prepareLogin` / `login` / `register` / `logout` / identity port / SessionStore / `createBrowserSessionStore` / App session key / `createClientSessionKey` / `access_token` / `isToken` / `IdentityAccessError`。

下一步：system 资源课从 L-015 起。L-019 把 `createSystemService` 整张方法表摊开（本课只借用了 `identity` 这一只口）。L-020 看 `getMenus` 之后的 manifest 导航。L-058 才进 sso-web。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-002 | `backend/wta-admin/.../AuthController.java` | 前端六枪打回的门厅窗；register 不发 token；logout 未登录也成功句 | `/auth` 映射 | 2026-09-16 |
| S-005 | `frontend/apps/{admin-web,home-web}` | 两厅堂各自 `services.ts` 注入同一工厂；钥匙名不同；sso-web 无此工厂 | `application/services.ts`、`application/session.ts` | 2026-09-16 |
| S-006 | `frontend/packages/{domains,adapters,platform}` | domain 工厂、SessionStore 端口、browser session adapter | `domain-admin`、`adapter-storage-browser`、`platform-contracts` | 2026-09-16 |
| S-L014-01 | `frontend/packages/domains/admin/src/index.ts` | 六枪→URL；`prepared` 闸；`parseAccessToken` 之后才 `setToken`；`register` 源码无 `setToken`；`createClientSessionKey` 公式未接线；接口长短；`social*` 非口试 | `createIdentityAccessService`；`createClientSessionKey`；`parseAccessToken` | 2026-09-16 |
| S-L014-02 | `frontend/packages/domains/admin/src/index.test.ts` | 两枪预备；关旗不发 login；clientId trim；login 写注入 session；综合测试无 register 的 `setToken` 负断言；logout clear；公式 ≠ `Admin-Token`；菜单冻结与失败关闭 | 各 `it(...)` | 2026-09-16 |
| S-L014-03 | `frontend/apps/admin-web/src/application/session.ts`；`home-web/.../session.ts` 与 `session.test.ts` | `Admin-Token` / `Home-Token`；home 命名空间测试 | `createBrowserSessionStore({ key })` | 2026-09-16 |
| S-L014-04 | `frontend/packages/domains/system/src/service.ts`；admin/home `application/services.ts` | 身份口 URL；App 把 `systemService.identity` 与 `session` 注入工厂 | `identity.loadInfo`/`loadMenus`；`createIdentityAccessService({...})` | 2026-09-16 |
| S-L014-05 | `frontend/packages/adapters/storage-browser/src/index.ts` 与 `index.test.ts`；`platform/contracts` 的 `SessionStore` | adapter 实现端口；key 隔离；storage 抛错走内存 | `createBrowserSessionStore`；`preserves the configured key` | 2026-09-16 |
| S-L014-06 | admin `views/login.vue`、`views/register.vue`、`router/index.ts`、`store/modules/user.ts`、`navigation.ts`；home `views/RegisterPage.vue`、`router/index.ts`、`store/user.ts`；`web-domains/admin` 的 `loginState.ts` / `LoginPage.vue` / `runtime.ts`；axios-browser `isToken` | 预备调用面按厅堂（admin 拆枪、home `prepareLogin`）；Pinia 委托；短口只服务 home `/login`；logout 带票；管理端/门户 finally 差 | 对应方法与头处理 | 2026-09-16 |
