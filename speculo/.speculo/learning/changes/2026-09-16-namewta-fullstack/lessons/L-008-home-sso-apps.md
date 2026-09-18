---
lesson_id: L-008
objective_ids: [OBJ-08]
claimed_cells: [A:home-web.services, A:sso-web.ssoApi, C:home-web, C:sso-web]
estimated_minutes: 36
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: home-composition
    minutes: 9
  - segment: sso-composition
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause-and-transfer
    minutes: 4
  - segment: recap
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-007, S-HOME-01, S-HOME-02, S-HOME-03, S-SSO-01, S-SSO-02, S-ADMIN-01]
---

# Lesson 008：home-web 只接身份+资料，sso-web 是认人厅

## 学完你能做什么

你能把三个前端 App 的**接线板**并排放在桌上，而不是只背「有三个包」：

1. **admin-web**（L-007）：`application/services.ts` 把几乎全部业务 domain 工厂接上同一块 HTTP/会话板。
2. **home-web**（本课）：同一张接线板文件名也叫 `services.ts`，产品侧只接**身份**和**资料**。
3. **sso-web**（本课）：**没有** `application/services.ts`，也没有 domain / web-domain 组合。它是门口的认人厅。HTTP 面是 `ssoApi.ts` 的五件事：`parseAuthorizeQuery`、`apiUrl`、`fetchSession`、`loginWithPassword`、`requestAuthorize`。

本课认格子：`A:home-web.services`、`A:sso-web.ssoApi`、`C:home-web`、`C:sso-web`。

容器所有权（三个包在磁盘上、方向口诀、换票不在认人厅）L-004 已经按 Relational 讲过。本课把这两个容器再往下收到**组合函数**：home 的接线板到底接了谁、sso 的认人面到底有哪些符号。不要把本课当成「再介绍一遍三个 App 存在」。

本课不展开：PKCE 校验与授权码表（L-055 / L-056 / L-058）、`IdentityAccessService` 每个方法的失败关闭（L-014）、profile person/enterprise 切片（L-039 / L-044）、platform 端口目录（L-009）、admin 每个 `create*Service` 的逐项副作用（L-007）。

## 先把宏观地图放在桌上

把三个 App 想成同一条街上的三家店。中央厨房（`packages/domains/*`、`packages/web-domains/*`）只盖一份。每家店自己决定点哪些菜、用哪把收银钥匙。

```text
  人
  ├── 管理操作者 ──► admin-web   接线板：几乎全部业务工厂（L-007）
  ├── 门户用户   ──► home-web    接线板：身份 + 资料（本课）
  └── SSO 客户端 ──► sso-web     无接线板：认人厅 ssoApi.ts（本课）

  home-web 自己的钥匙：Home-Token
  sso-web 自己的钥匙：SSO HttpOnly Cookie（credentials: 'include'）
  业务通行证（Home-Token / Admin-Token）不跟认人厅共用
```

**类比失效处：** 「点菜少」不是「厨房缺两口锅」。home 用的 `createProfileService` 和 admin 是同一个工厂；墙上挂的菜单照片却是 `createProfileSelfWebDomain`，不是 admin 的 `createProfileWebDomain`。认人厅更不是「还没装修完的第三家餐厅」：它故意不进中央厨房，因为它卖的是「你是谁」，不是「你今天点哪道业务菜」。

L-004 已经让你能指路径。本课要你能指**函数**：打开 `home-web/src/application/services.ts` 和 `sso-web/src/ssoApi.ts`，说出各自组合了什么、没组合什么。

## 核心概念与机制

### 直觉讲解

admin-web 的接线板像酒店总台后面那面墙：房务、餐厅、洗衣、监控、通知铃，插头全在。home-web 的接线板看起来文件名一样，墙上却只有两样给客人用的东西：

- **认自己**（身份）：登录、注册、拉「我是谁」、拉「我能进哪些用户中心页」。
- **交自己的档案**（资料）：个人认证、企业认证。不是替管理员审别人的档案。

sso-web 连这面墙都没有。它更像小区门口的保安亭：看你证件、在本亭子里记一笔「这个人今晚进过门」，然后把你送回酒店或用户中心。保安亭不发酒店房卡。房卡是两家店自己去前台用授权码换的。

所以 OBJ-08 的对比句要拆成两刀，不能合成一句「home 和 sso 都比较瘦」：

1. home **仍是厅堂**：有 `services.ts`、有 `composeAppRuntime`、有 `Home-Token`、有动态菜单。瘦在**选了哪些业务**。
2. sso **不是厅堂的缩水版**：没有业务 domain 组合。瘦在**产品角色就是认人**。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 组合 / 接线板 | composition | App 显式 import 工厂、注入 HTTP/会话、选出 domain 与 manifest。home 的权威文件是 `src/application/services.ts`；sso-web **没有**这份文件 |
| 身份服务 | identity access | `createIdentityAccessService` 的产物。home 用它打 `/auth/*`，并用 `systemService.identity` 读 `/system/user/getInfo` 与 `/system/menu/getRouters` |
| 资料服务 | profile service | `createProfileService` 的产物。厨房能力（person / enterprise / materials）两厅堂可共用 |
| 自助档案页 | self-profile web-domain | `createProfileSelfWebDomain`，manifest id `web-domain-profile-self`。用户自己的认证中心 |
| 管理档案页 | profile admin web-domain | `createProfileWebDomain`，manifest id `web-domain-profile`。admin-web 才挂。本课只需记住 **两个工厂不是同一个** |
| 身份配料 | identity port | `createSystemService(...).identity`。home 创建 `systemService` 是为了把 `loadInfo` / `loadMenus` 喂给身份，**不是**把 system 管理 CRUD 组装进门户 |
| 认人厅 | first-party SSO login origin | `sso-web`：独立 Origin、只接受本仓密码、用 Cookie 建 SSO 域会话，不写 `Home-Token` / `Admin-Token` |
| 授权查询 | authorize query | `parseAuthorizeQuery` 从 URL 取出的 `client_id` / `redirect_uri` / `state` / `code_challenge` / `code_challenge_method` / `response_type`。**不含** `sso_secret` |
| 带 Cookie 的 fetch | credentials include | sso-web 三次 HTTP 都 `credentials: 'include'`，让浏览器带上 SSO Cookie |
| 会话命名空间 | session namespace | home：`createBrowserSessionStore({ key: 'Home-Token' })`。sso：后端 Cookie，前端不持 token 钥匙名 |
| 三点组合门 | three composition gates | 业务 App 的 `package.json` 依赖、`services.ts` 工厂、`*ManifestRegistry.ts` 选择。sso-web **不走这三扇门** |

「home 只接身份+资料」是**产品组合**主张，不是「`services.ts` 里只能出现两个 `create*` 单词」。下一节把第三个 `createSystemService` 钉死，避免把配料误读成第三道业务菜。

### 机制/因果链

#### A. home-web 接线板：`A:home-web.services`

打开 `frontend/apps/home-web/src/application/services.ts`（2026-09-16 工作树，整文件 16 行）：

1. 用本厅堂的 `homeHttp` 包一层 `domainHttp.request`。HTTP 实现在 `application/http.ts`：`createAxiosBrowserAdapter`，`baseURL` 是 `VITE_APP_BASE_API`，过期走 `requestRelogin`。这是 **App → adapter**，不是 App 声明 `platform-http`。
2. `createSystemService(domainHttp)` 得到 `systemService`。home 源码里**只有这一处再读取它**：`identity: systemService.identity`。没有 `systemService.users` / `roles` / `clients` / `ssoApps` 的门户页面组合。`identity.loadInfo` 打 `GET /system/user/getInfo`，`identity.loadMenus` 打 `GET /system/menu/getRouters`。
3. `createIdentityAccessService({ client: { clientId: VITE_APP_CLIENT_ID }, encryptLoginRequest, http: domainHttp, identity: systemService.identity, session })` 得到 `identityAccessService`。这是产品侧的**身份插头**。登录写的是本厅堂 `session`（钥匙名 `Home-Token`），HTTP 是 `/auth/login`、`/auth/register`、`/auth/logout`、`/auth/code`、`/auth/client/context`，**不是** `/sso/login`。
4. `createProfileService(domainHttp)` 得到 `profileService`。这是产品侧的**资料插头**。厨房里仍有 person / enterprise / materials / tags；本课不把每个子资源走完。
5. **到此为止，接线板没有** `createNotificationService`、`createWorkflowDefinitionService`、`createThirdService`、`createAiService`、`createDemoService`、`createMonitorService`、`createOpenApiService`、`createOssUploadClient`、`notificationDirectory`。这些是 admin-web 接线板上的东西（L-007）。

三点组合门要对着读，数字本来就不是同一个：

| 门 | home 实际选了什么 | 不要误读成 |
| --- | --- | --- |
| `package.json` | `domain-admin`、`domain-system`、`domain-profile`；`web-domain-admin`、`web-domain-profile`；platform：`app-runtime` / `auth` / `permission`；adapter：axios / crypto / storage。**无** web-kit，**无** notify/demo/workflow/third/ai | 「依赖了 domain-system 就是组装了系统管理」 |
| `services.ts` | 产品服务两个：`identityAccessService`、`profileService`。`systemService` 只作身份配料 | 「文件里出现三个 create，所以接了三个业务域」 |
| `homeManifestRegistry.ts` | `selectedDomainIds: ['admin', 'profile']`；`selectedManifestIds: ['web-domain-admin', 'web-domain-profile-self']`；页面工厂 `createAdminWebDomain` + `createProfileSelfWebDomain` | 「admin domain 出现在 home 里，所以 home 是管理端」 |

`createAdminWebDomain` 在 home 里只注册登录页组件键 `identity-access/login/index`。它不是把 admin-web 的系统菜单搬过来。档案页工厂必须说全名：`createProfileSelfWebDomain` ≠ `createProfileWebDomain`。

登录后的容器主路径（组合层，不背菜单 id）：

1. 已有 `Home-Token` 时，`router/index.ts` 调 `restoreProtectedNavigation`。
2. `loadIdentity` → `identityAccessService.getInfo()`（经 `systemService.identity.loadInfo`）。
3. `loadRoutes` → `identityAccessService.getMenus()`，再 `projectServerRoutes` + `resolveHomeWebRegistration`。不在已选 manifest 里的组件键解析失败，不能靠 catch-all 偷偷挂上。
4. 登录成功后的默认去向是 `/profile`，不是管理端工作台。

home 还有第二条进门路，**仍然不把认人厅变成厅堂**：`application/sso.ts` 的 `createSsoAuth` 用 `homeHttp` 打 `POST /sso/oauth2/token`，把 `access_token` 写入 `Home-Token`。`views/SsoCallbackPage.vue` 在 `/sso/callback` 换票。换票发生在 home，不发生在 sso-web。

#### B. sso-web 认人面：`A:sso-web.ssoApi`

磁盘事实：`frontend/apps/sso-web/` 没有 `src/application/`，没有 `services.ts`，`package.json` 运行时依赖只有 `vue` 与 `vue-router`。`src/main.ts` 三条路由 `/`、`/authorize`、`/login` 都指向同一张 `AuthorizePage.vue`。组合面就是 `src/ssoApi.ts`。

五个导出函数各管一截（PKCE 校验与码表写入留给 L-055；本课只认厅堂怎么喊）：

| 符号 | 做什么 | 不做什么 |
| --- | --- | --- |
| `parseAuthorizeQuery(search)` | 从查询串取出 `clientId` / `codeChallenge` / `codeChallengeMethod` / `redirectUri` / `responseType`（缺省 `'code'`）/ `state` | 不读、不存 `sso_secret`。测试断言结果对象 `not.toHaveProperty('sso_secret')` |
| `apiUrl(path)` | `VITE_SSO_API` 去掉尾斜杠再拼 path。开发环境该变量是空串，于是走同源 `/sso/*`，由 Vite 代理到 `VITE_SSO_API_PROXY` | 不拼 `/auth/*`，不拼 `/system/*` |
| `fetchSession()` | `GET apiUrl('/sso/session')`，`credentials: 'include'`。`body.code === 200` 才算已有 SSO 会话 | 不返回用户资料，不写 `Home-Token` |
| `loginWithPassword(username, password)` | `POST apiUrl('/sso/login')`，JSON `{ username, password }`，同样带 Cookie。`code !== 200` 抛错 | 不带 `clientId` / `grantType: 'password'` 那套 `/auth/login` 合同。测试还锁死 `ssoLoginMethods === ['password']`，不含 github / wechat |
| `requestAuthorize(query)` | `GET /sso/oauth2/authorize` 加上 query 里那六项。成功时返回 `{ loginRequired, redirectUri }` | 不自己 `302`。有 `redirectUri` 时由页面 `window.location.assign`。不调用 `/sso/oauth2/token` |

`AuthorizePage.vue` 的因果链（组合层）：

1. `onMounted`：`fetchSession()`。
2. 没有 SSO 会话 → 出示密码表。
3. 已有会话或密码提交成功 → `parseAuthorizeQuery(当前 fullPath 的查询串)` → `requestAuthorize`。
4. `loginRequired === true` → 继续留在密码表。
5. 有 `redirectUri` → 离开认人厅，回到业务 App 的 `/sso/callback`。
6. 业务 App 自己换票。认人厅到此结束。

页面文案把产品边界写在卡片上：「业务应用不会共用这张会话。」这不是装饰，是会话命名空间合同。

## 图、表或文本图

**图题 / caption：** 三块接线板。admin 全接；home 只接身份+资料（system 仅作身份配料）；sso 没有接线板，只有认人五函数。换票箭头从厅堂指出，不从认人厅指出。

```text
 admin-web (L-007)              home-web (本课)                 sso-web (本课)
 ┌─────────────────────┐        ┌─────────────────────┐        ┌─────────────────────┐
 │ application/         │        │ application/         │        │ （无 application/    │
 │   services.ts        │        │   services.ts        │        │    无 services.ts）  │
 │ 几乎全部 create*     │        │ systemService ──┐    │        │ ssoApi.ts            │
 │ Service / OSS /      │        │                 │    │        │  parseAuthorizeQuery │
 │ notify directory     │        │ identityAccess  ◄┘   │        │  apiUrl              │
 │ Admin-Token          │        │   (身份)             │        │  fetchSession        │
 │ composeAppRuntime    │        │ profileService       │        │  loginWithPassword   │
 │ 八个 web-domain 包   │        │   (资料)             │        │  requestAuthorize    │
 └──────────┬──────────┘        │ Home-Token           │        │ fetch + cookie       │
            │                   │ composeAppRuntime    │        │ 三路由→AuthorizePage │
            │                   │ admin + profile-self │        └──────────┬──────────┘
            │                   └──────────┬──────────┘                   │
            │                              │                              │
            │  /auth/*  /system/*          │  /auth/*  /system/user|menu  │  /sso/session
            │  /notify  /workflow ...      │  /profile/*                  │  /sso/login
            │                              │                              │  /sso/oauth2/authorize
            │                              │                              │
            │         换票 /sso/oauth2/token 发生在厅堂 application/sso.ts │
            │         不发生在 sso-web ───────────────────────────────────┘
            v                              v
        业务 Sa-Token                  Home-Token
        （管理厅堂）                   （用户中心）
```

**文字等价物：** 左列是 admin-web，接线板在 `application/services.ts`，组合几乎全部业务工厂，会话钥匙 `Admin-Token`。中列是 home-web，接线板文件名相同，但产品只导出身份和资料两根插头；`createSystemService` 的箭头只指向身份工厂的 `identity` 口，不指向系统管理页面。home 的运行时只选 `admin` 与 `profile`，档案 manifest 是 self，会话钥匙 `Home-Token`。右列是 sso-web，没有接线板目录，五个函数画在 `ssoApi.ts` 里，HTTP 是 `fetch` 加 Cookie，三条路由同一张授权页。三条竖线底下的后端路径不同：厅堂走 `/auth/*` 和各自的业务资源；认人厅只走 `/sso/session`、`/sso/login`、`GET /sso/oauth2/authorize`。从认人厅回到厅堂之后，换票那一跳画在两个厅堂自己的 `createSsoAuth` 上，明确标成「不发生在 sso-web」。

**图的边界：** 图不列出 admin 接线板上每一个工厂名字（那是 L-007）。图不保证三个 App 已在生产部署。图不把 `createSystemService` 画成 home 的第三块业务域。图不把 PKCE 校验、授权码表、Sa-Token 写入画进 sso-web。`requestAuthorize` 的成功 JSON 里出现 `redirectUri` 不等于浏览器已经被服务器 302。home 的 `createProfileService` 厨房能力与 admin 同构，不表示 home 挂了管理端档案页。

对照表（组合函数层，2026-09-16 工作树）：

| 观察点 | admin-web | home-web | sso-web |
| --- | --- | --- | --- |
| 接线板文件 | `src/application/services.ts` | 同名，16 行 | **不存在** |
| 产品 `create*` | 身份 + system/OpenAPI/OSS + workflow/profile/demo/notify/monitor/ai/third | `createIdentityAccessService` + `createProfileService`（另 `createSystemService` 只喂 identity） | 无 domain 工厂 |
| 页面工厂 | 含 `createProfileWebDomain` | `createAdminWebDomain`（登录）+ `createProfileSelfWebDomain` | 无 web-domain |
| HTTP 出口 | axios adapter | axios adapter | `fetch` + `credentials: 'include'` |
| 会话 | `Admin-Token` | `Home-Token` | SSO Cookie |
| 登录 HTTP | `/auth/login` | `/auth/login`（厅堂密码）或回调换票 | `POST /sso/login` |
| 动态菜单 | 有 | 有（`getMenus` + 已选 manifest） | 无 |
| 是否管理端 | 是 | 否 | 否，连厅堂都不是 |

## 正例、反例与边界

**正例 1（home 接线板）。** `services.ts` 把 `identityAccessService` 和 `profileService` 交给 registry 与 store。`store/user.ts` 的 `login` / `getInfo` / `logout` 只打身份服务。`RegisterPage.vue` 走 `identityAccessService.prepareLogin` 与 `register`。`homeManifestRegistry.ts` 把 `profileService` 注入 `createProfileSelfWebDomain`。这就是「只接身份+资料」在源码里的操作定义。

**正例 2（身份配料不是第三道菜）。** `systemService` 被 export，但 `frontend/apps/home-web` 内除 `services.ts` 那一次 `identity:` 赋值外，没有第二个文件读取它。`package.json` 有 `domain-system`、没有 `web-domain-system`。`selectedDomainIds` 没有 `'system'`。有 domain、无 web-domain、无 runtime 选择，这三件事同时成立，才叫配料。

**正例 3（home 仍是厅堂）。** 登录后 `restoreProtectedNavigation` 会拉菜单。门户壳 `HomeShell.vue`、公开页 `PortalPage.vue`、注册页、SSO 回调页留在 App `views/`。这些是终端私有壳，不是漏搬的 CRUD，也不证明 home 组装了 notify/workflow。

**正例 4（认人厅五函数）。** `AuthorizePage.vue` 只从 `../ssoApi` 引入 `fetchSession`、`loginWithPassword`、`parseAuthorizeQuery`、`requestAuthorize`。`ssoApi.test.ts` 锁了三件事：只接受仓库密码登录、POST `/sso/login` 必须 `credentials: 'include'`、authorize 查询不含 secret。

**正例 5（换票在厅堂）。** home 的 `application/sso.ts` 才 POST `/sso/oauth2/token`，并把 access token 交给 `session.setToken`。sso-web 源码里搜不到 `oauth2/token`。认人成功 ≠ 用户中心已登录。

**反例 1.** 因为要在 home 做「通知铃」或「流程待办」，把 `createNotificationService` / `createWorkflowDefinitionService` 写进 home 的 `services.ts`，却不改 `package.json` 与 registry。三点组合门会裂开；更关键的是：你把门户变成了第二管理端，OBJ-08 的产品差被你亲手拆掉。

**反例 2.** 在 sso-web 新建 `src/application/services.ts`，`createIdentityAccessService` 打 `/auth/login`，再 `composeAppRuntime` 挂 profile。认人厅一旦开始组合业务域，Cookie 会话和 `Home-Token` 会拧到同一根绳子上。那是把保安亭改成酒店后厨。

**反例 3.** home 用相对路径 `../../admin-web/src/application/services.ts` 复用 admin 接线板。App 互引被 `ARCH-003` 禁止；`Admin-Token` 与 `Home-Token`、两套 `VITE_APP_CLIENT_ID` 会串。home 的 Client 行与 admin 不是同一个。

**反例 4.** 看见 `createSystemService` 就说「home 也组装了 system」。那是把配料口当成业务域。admin 的 `systemService` 还要喂用户/角色/菜单/Client 管理页；home 的同名变量没有那些消费者。

**反例 5.** 把 `createProfileSelfWebDomain` 说成「admin 档案页少几列」。self manifest 只有三个组件键：`profile/center/index`、`profile/person/application`、`profile/enterprise/application`。admin 的 `createProfileWebDomain` 另有档案管理、材料标签等贡献。工厂不同，产品不同。

**反例 6.** 因为 sso-web 依赖少，就说它不是 App，或把它的 HTTP 改成 axios adapter + `Home-Token`。认人厅的会话合同是 Cookie；改插头等于改产品。

**反例 7.** 在 `parseAuthorizeQuery` 里顺手收下并保存 `sso_secret`。测试已经禁止结果对象持有这个字段。secret 不属于认人厅查询串。

**边界.**

- home 可以（也确实）直连 platform：`composeAppRuntime`、`requestRelogin`、`createSsoAuth`、`createAccessEvaluator`。这是 L-004 的一等边，不是「又组装了一个业务域」。
- home 的 `hasPermission` 是 App 函数，传入 self-profile runtime；不装 admin 那套 `v-hasPermi` web-kit。无 web-kit ≠ 无权限判断。
- sso-web 可以没有 domain 仍是 `apps/` 下的终端。组合多少厨房是产品选择，不是 App 资格考试。
- 本课覆盖 sso 五函数的**组合职责**（谁喊谁、打哪条 URL、带不带 Cookie、写不写业务 Token）。授权码怎么写入、PKCE 怎么验、换票失败怎么关，是 L-055 / L-056 / L-058 的格子。
- 容器格子 `C:home-web` / `C:sso-web` 在 L-004 已按 Relational 覆盖「身份+资料子集 / 认人厅」。本课把同一容器收到组合函数，不重新发明第三个 App，也不把 Relational 故事再讲一遍冒充新覆盖。

## 变式与迁移

- **变式 A：对照 L-007。** 打开 admin 的 `services.ts` 和 home 的 `services.ts`。admin 从第 34 行起到文件末尾连续 export 多个业务服务，并在中段组装 OSS 直传与通知收件人目录。home 在身份、资料两行之后结束。迁移动作：新增可复用业务页时，先问「这是管理厅堂的菜还是用户自己的档案」。管理菜接到 admin 接线板（L-007 的门）；用户自己的档案接到 home 的 self manifest。不要因为「都是 Vue App」就把工厂两边各接一份。

- **变式 B：home 要加一张用户可见页。** 合法路径仍是三点同步：`package.json` 增加对应 domain/web-domain → `services.ts` 增加对应 `create*Service`（若尚无）→ registry 增加 manifest 并写入 `selectedDomainIds` / `selectedManifestIds`。若新页其实是管理员审核别人的档案，目标 App 是 admin-web，工厂是 `createProfileWebDomain`，不是把 admin 页塞进 home。

- **变式 C：第二条进门（SSO）改坏了命名空间。** 合法：认人厅只建 SSO Cookie；home 回调用 `createSsoAuth` 换 `Home-Token`；admin 回调换 `Admin-Token`。非法：让 `loginWithPassword` 直接 `session.setToken` 写 `Home-Token`，或让 home 的 `identityAccessService.login` 改打 `/sso/login`。两条登录 HTTP 合同不同：厅堂密码登录带 `clientId` + `grantType: 'password'` 进 `/auth/login`；认人厅只 POST `{ username, password }` 进 `/sso/login`。

- **变式 D：文档与工作树。** home 的 `AGENTS.md` 写「组合 admin/profile domain 和 profile self web-domain」，没点名 `domain-system`。Skill 架构文写业务 App 三点集合必须一致。工作树里 `domain-system` 在依赖和 `createSystemService` 里出现，却不在 `selectedDomainIds`。本课裁决：以工作树为准，把它解释成身份配料，并把「三点数字可以不是同一个」记下来，不要为了迎合摘要去删 `createSystemService`，也不要为了迎合 `createSystemService` 去给 home 加 `web-domain-system`。

- **变式 E：有人把 sso-web 当成「登录组件库」拷进 admin。** 认人厅是独立 Origin 的终端包。厅堂登录页来自 `createAdminWebDomain` 的 `identity-access/login/index`。两张登录表看起来都有用户名密码，所有权不同、HTTP 不同、会话不同。拷贝 `AuthorizePage.vue` 进 admin `views/` 会把认人厅和厅堂焊死。

- **迁移口诀：** 改 home 组合 → 先改 `services.ts` 的身份/资料插头，再改 registry 的 self manifest，最后才碰 `views/` 壳。改认人厅 → 只改 `ssoApi.ts` 与 `AuthorizePage.vue` 对这五个函数的调用；需要换票或菜单时，回到厅堂，不要在 sso-web 开接线板。

## 常见误区

1. **「home 和 sso 都瘦，所以是一类东西。」** home 瘦的是业务选择；sso 瘦的是没有业务组合。一个是小厅堂，一个是保安亭。
2. **「`services.ts` 出现了 `createSystemService`，OBJ-08 说『只接身份+资料』就错了。」** 配料不是产品域。操作定义看：有没有 system 页面工厂、有没有 `selectedDomainIds` 含 `system`、有没有第二个文件消费 `systemService` 的管理口。
3. **「home 引用了 `createAdminWebDomain`，所以 home 是管理端。」** 那个工厂在 home 里只挂登录页。管理端是 admin-web 那块全业务接线板。
4. **「self-profile 就是把管理档案页 CSS 改小。」** 两个 web-domain 工厂、两套组件键、一份可能共用的 `createProfileService`。页面所有权以工厂为准。
5. **「sso-web 没有 `services.ts` 是没做完。」** 2026-09-16 的目录就是完整认人厅。补接线板才是做完了错误产品。
6. **「`fetchSession` 成功了，用户就已经进入用户中心。」** 那只证明 SSO Cookie 还在。用户中心认的是 `Home-Token`。中间还差授权码和厅堂换票。
7. **「`requestAuthorize` 就是换票。」** 它只问认人厅「能不能发码/要不要先登录」。换票是厅堂的 `POST /sso/oauth2/token`。
8. **「认人厅也该走 axios adapter，好和 home 共用 HTTP。」** 共用插头容易共用会话。认人厅刻意用 `fetch` + Cookie。
9. **「把 admin 的通知/流程接到 home，用户会更方便。」** 方便的代价是第二个管理端。本课的产品差就是挡住这件事。
10. **「L-004 已经讲过 home/sso，这课可以只背口号。」** L-004 覆盖容器关系；本课覆盖组合函数。背口号过不了 `A:home-web.services` 和 `A:sso-web.ssoApi`。
11. **「`apiUrl` 空前缀是配置漏了。」** 开发环境 `VITE_SSO_API=` 表示同源 `/sso`，代理去 `127.0.0.1:18080`。空串是合同，不是缺口。
12. **「三点组合门必须三个数字相等。」** 依赖包、工厂、selected id 可以不一致。home 的 system 就是合法的不一致。非法的是「只改其中一扇门」。

## 非评分暂停

打开磁盘，不要凭记忆。不要在本课改代码。不要写成问答。

1. 打开 `frontend/apps/home-web/src/application/services.ts`：数 `create*` 调用；圈出 `identity: systemService.identity`；确认没有 notify / workflow / third / ai / demo / oss 工厂。
2. 打开 `homeManifestRegistry.ts`：读 `selectedDomainIds` 与 `selectedManifestIds`；确认档案工厂函数名带 `Self`。
3. 在 `frontend/apps/sso-web` 下确认没有 `src/application/services.ts`。打开 `ssoApi.ts`，按表格指五个导出函数各自的 URL。
4. 打开 `AuthorizePage.vue` 的 `onMounted` / `submit` / `continueAuthorize`，用手指把「会话 → 密码 → 授权 → 离开」走一遍。
5. 打开 home 的 `application/sso.ts`，确认 `oauth2/token` 写在厅堂。回到 sso-web 搜 `token`，确认认人厅不换票。

## 总结、词汇表与下一步

- **home-web** 是用户中心厅堂。接线板 `application/services.ts` 的产品插头是 `createIdentityAccessService` 与 `createProfileService`。`createSystemService` 只把 `identity` 喂给登录后的 getInfo/getMenus，不组装系统管理。页面是登录 web-domain + **self** 档案 web-domain。会话是 `Home-Token`。
- **sso-web** 是认人厅，不是管理端，也不是缩小的 home。没有 `services.ts`，没有业务 domain 组合。组合面是 `ssoApi.ts`：`parseAuthorizeQuery`、`apiUrl`、`fetchSession`、`loginWithPassword`、`requestAuthorize`。HTTP 是 `fetch` + Cookie。三条路由同一张授权页。换票在厅堂。
- **对照 L-007：** admin 接线板组装全部业务服务（含 OSS/通知目录）。home 不接那些插头。把它们拷进 home 或拷进 sso，都是拆掉本课的产品差。
- 格子：`A:home-web.services` = 上述接线板操作定义；`A:sso-web.ssoApi` = 五函数认人面；`C:home-web` / `C:sso-web` = 同一容器在组合函数层的差（Relational 故事见 L-004）。

词汇表：composition、identity access、identity port、self-profile、first-party SSO login origin、authorize query、credentials include、session namespace、three composition gates。

下一步：L-009 看 platform 端口形状（HTTP/auth/permission/app-runtime/contracts）以及 App 适配器方向。不要在那一课把 home/sso 的接线板再背一遍。身份服务每个方法的副作用见 L-014；认人厅函数进 PKCE/码表见 L-055 与 L-058。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web,sso-web}` | 三个 App 源码与 build 存在；home/sso 产品角色 | 工作树 App 根 | 2026-09-16 |
| S-006 | `frontend/packages/{domains,web-domains,platform,adapters}` | domain / web-domain / 端口 / 适配器被 home 选择；sso 不声明它们 | 工作树 packages | 2026-09-16 |
| S-007 | `.agents/skills/engineering-standards/references/project/01-module-map.md` 与 fullstack `frontend/architecture.md` | App 显式组合；三点门；方向口诀。home 的 system 配料以工作树为准 | 「App 显式组合」；依赖方向 | 2026-09-16 |
| S-HOME-01 | `frontend/apps/home-web/src/application/services.ts` | 仅 `createSystemService` + `createIdentityAccessService` + `createProfileService`；identity 配料；无其它业务工厂 | 整文件 | 2026-09-16 |
| S-HOME-02 | `frontend/apps/home-web/src/router/homeManifestRegistry.ts` | `selectedDomainIds` admin+profile；`web-domain-profile-self`；`createProfileSelfWebDomain` | 第 12–40 行 | 2026-09-16 |
| S-HOME-03 | home `package.json`、`application/{http,session,sso,access}.ts`、`router/index.ts`、`store/{user,navigation}.ts`、`views/SsoCallbackPage.vue` | `Home-Token`；axios adapter；厅堂换票；`restoreProtectedNavigation`；身份 store | 工作树 | 2026-09-16 |
| S-SSO-01 | `frontend/apps/sso-web/src/ssoApi.ts`、`ssoApi.test.ts` | 五函数、Cookie fetch、只接受 password、查询不含 secret | 整文件 | 2026-09-16 |
| S-SSO-02 | sso `package.json`、`src/main.ts`、`src/views/AuthorizePage.vue`、`AGENTS.md`、`.env.development` | 无 domain 依赖；三路由同一页；认人链；`VITE_SSO_API` 空串走同源 | 工作树 | 2026-09-16 |
| S-ADMIN-01 | `frontend/apps/admin-web/src/application/services.ts` | L-007 对照：全业务工厂 + OSS + 通知目录 | 整文件 | 2026-09-16 |
