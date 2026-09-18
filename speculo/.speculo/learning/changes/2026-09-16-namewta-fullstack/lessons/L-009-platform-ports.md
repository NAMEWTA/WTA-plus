---
lesson_id: L-009
objective_ids: [OBJ-09]
claimed_cells: ["A:platform-http", "A:platform-auth", "A:platform-permission", "A:platform-app-runtime", "A:platform-contracts"]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: five-packages-on-disk
    minutes: 8
  - segment: deep-explanation
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: conflict-and-pause
    minutes: 5
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-007, S-016]
---

# Lesson 009：platform 端口与 App 适配器方向

## 学完你能做什么

你能指着 `frontend/packages/platform/` 说出五个认格子包各自卖什么，并且能画出箭头，而不是只背「ports 在下面」：

1. **端口是形状，适配器是插头，App 是电工。** domain / web-domain 只认形状，不认 axios。
2. **HTTP 形状不在 `platform-http` 里。** `HttpClient` / `HttpRequest` 住在 `platform-contracts`。`platform-http` 卖的是运输事故单（`TransportError`）。浏览器插头 `@namewta/adapter-axios-browser` 同时实现 `HttpClient` 并消费 `platform-http`。
3. **厅堂可以直连若干 platform 包**（`app-runtime` / `auth` / `permission`，admin 另有 `contracts` / `validation`），但 **两个厅堂的 `package.json` 都不声明 `platform-http`**。

本课认格子：`A:platform-http`、`A:platform-auth`、`A:platform-permission`、`A:platform-app-runtime`、`A:platform-contracts`。磁盘上还有第六个包 `validation`——存在，本课只点名，**不发明格子**。adapter / web-kit / `api-contracts` 所有权留给 L-010；`services.ts` 工厂全表留给 L-007；`IdentityAccessService` 打哪些 `/auth/*` 留给 L-014；菜单 host 十条 id 留给 L-020。

## 先把宏观地图放在桌上

L-004 把厅堂和厨房拆开了。本课只盯插座墙。

旧前端像把电线焊死在厅堂墙上：页面直接 `import axios`，拦截器、token、401 弹窗全写在同一个 `src/utils/request.ts`。要开第二扇窗，就再焊一遍。NAMEWTA 把「墙上该有几个孔」写成 platform 包，把「这个浏览器怎么插」写成 adapter，把「今天插哪几根线」留给 App。

```text
frontend/packages/platform/          ← 插座墙（跨领域、跨终端）
├── contracts/     @namewta/platform-contracts      ← 形状纸：HttpClient / SessionStore / CryptoPort …
├── http/          @namewta/platform-http           ← 运输事故单：TransportError
├── auth/          @namewta/platform-auth           ← 过期重登 + SSO 换票流程（注入 ports）
├── permission/    @namewta/platform-permission     ← 快照求值器：createAccessEvaluator
├── app-runtime/   @namewta/platform-app-runtime    ← 组合 runtime：composeAppRuntime / 导航恢复
└── validation/    @namewta/platform-validation     ← 格式/密码校验（存在；无本课格子）

frontend/packages/adapters/          ← 插头（某运行时的实现）
├── axios-browser/     实现 HttpClient，依赖 platform-http + platform-contracts
├── storage-browser/   实现 SessionStore / TokenStorage
├── crypto-browser/    实现 CryptoPort
├── oss-upload-browser 实现 UploadClient
├── taro-request/      README 占位
└── taro-storage/      README 占位
```

方向先记四句，四句一起记：

- **主链：** App → web-domain → domain → platform。
- **一等边：** App → platform（ports / runtime）。厅堂自己调 `composeAppRuntime`、`requestRelogin`、`createSsoAuth`、`createAccessEvaluator`。
- **插头边：** App → adapter；**adapter → platform**。`platform-http` 的消费者是 adapter，不是 App，也不是 domain。
- **禁止：** domain / web-domain / platform **import axios**；platform **依赖 Vue / DOM / 浏览器全局**；adapter 依赖 domain；platform 反向依赖 App。

门禁写在 `frontend/tooling/architecture/src/index.mjs` 的 `runtimeLayerAllowlist`：

| 源层 | 允许指向 |
| --- | --- |
| app | adapter、domain、platform、web-domain、web-kit |
| web-domain | domain、platform、web-kit |
| domain | api-contracts、domain、platform |
| web-kit | platform、web-kit |
| adapter | platform |
| platform | platform |

另有 `terminal-purity`：domain / platform 的运行时依赖和源码都不得出现 `axios`、`vue`、`vue-router`、未遮蔽的 `window` / `localStorage` 等终端物。adapter 才允许抱 axios。

**类比失效处：** 「插座墙」不是说 platform 六个包都是同一类东西。`contracts` 才是六边形里的 **port 接口**；`http` 是运输错误词汇；`auth` 是**用端口编排的流程**；`permission` 是对快照的纯函数求值；`app-runtime` 是组合器。不要把五份 README 都读成「这里定义了 HttpClient」。以 `src/index.ts` 为准。

## 五个认格子包在磁盘上长什么样

这一节是 deep 的硬证据。先 `ls`，再读 `package.json` 和公开入口，不要用 README 句子代替 export。

工作树抽查日期：2026-09-16。六个目录都有 `package.json` + `src/index.ts`；`http` / `auth` / `permission` / `app-runtime` / `contracts` 另有 README 与 AGENTS。`validation` 没有 README，仍是已激活包。

### 谁依赖谁（认格子五包 + 顺手点名 validation）

| 包 | 运行时依赖 | 工作树消费者（`package.json`） | 本课一句话 |
| --- | --- | --- | --- |
| `@namewta/platform-contracts` | **无** | 几乎所有 domain；`platform-http`、`platform-auth`；四个浏览器 adapter；`web-kit-file-upload`；**admin-web**。home-web **不**声明它 | 插座规格纸。`HttpClient` 在这里 |
| `@namewta/platform-http` | `platform-contracts` | **只有** `@namewta/adapter-axios-browser` | 运输事故单。App / domain 都不声明 |
| `@namewta/platform-auth` | `platform-contracts` | admin-web、home-web | 过期重登 + SSO 流程；自己不打 HTTP |
| `@namewta/platform-permission` | **无** | admin-web、home-web、`web-kit-permission` | 前端可见性求值，不是后端授权 |
| `@namewta/platform-app-runtime` | **无**（`@namewta/architecture` 只在 devDependencies） | 两个厅堂；全部业务 domain；全部业务 web-domain | 显式组合 + 菜单投影 + 导航恢复 |
| `@namewta/platform-validation` | **无** | admin-web、`domain-admin` | 格式/密码校验；**本课无格子** |

sso-web 三个都不声明。它用 `ssoApi.ts` 的 `fetch`，不进这棵插座树。

### `A:platform-contracts`：真正的端口接口

公开入口 `frontend/packages/platform/contracts/src/index.ts`。零运行时依赖。

这里才是 domain 每天摸的形状：

| 符号 | English | 干什么 |
| --- | --- | --- |
| `HttpClient` / `HttpRequest` / `HttpMethod` | HTTP port | `request<T>(request): Promise<T>`。domain 工厂吃这个，不吃 axios |
| `SessionStore` / `TokenStorage` / `StoragePort<T>` | session / storage port | `getToken` / `setToken` / `clear`；钥匙名由 App 决定 |
| `CryptoPort` / `EncryptedRequest` | crypto port | 请求加密、响应解密 |
| `ErrorPresenter` / `PresentedError` / `ErrorKind` | error presenter | `present` + `confirmSessionExpired`；UI 由 App 提供 |
| `NavigationPort` / `SessionPort` | navigation / session command | 重登时 `replaceWithLogin`、`logout` |
| `UploadClient` / `UploadItem` / `UploadResult` | upload port | 上传/解析/删除；浏览器实现在 oss adapter |
| `ClientContext` / `requireClientContext` | client context | 空 `clientId` **失败关闭**，不选默认 Client |
| `ApiErrorInfo` | API error info | 结构化字段错误，给 domain 模型用 |

测试只锁一件事：`requireClientContext({ clientId: '   ' })` 抛错，不偷偷找一个 fallback Client。

**README 对齐：** 这份 README 与源码一致——「少量、稳定、跨领域且终端无关的基础类型与端口」。领域模型不要塞进来。

### `A:platform-http`：运输事故单，不是 HttpClient

公开入口 `frontend/packages/platform/http/src/index.ts`。依赖 `platform-contracts` 只为了 `ErrorKind`。

源码导出的是：

- `TransportError` / `createTransportError` / `createHandledError`
- `isTransportError` / `isHandledError`
- `normalizeTransportMessage` / `payloadErrorMessage`
- `TransportErrorKind = ErrorKind | 'encryption' | 'unauthorized'`

它把 cause 洗成 `{ name, message, code }`，`JSON.stringify(error.cause)` 不得带出 `Authorization` 头。测试就锁这件事。

**adapter 怎么用：** `createAxiosBrowserAdapter` 从 `platform-contracts` **import type** `HttpClient`，从 `platform-http` **import 值** `createTransportError`。401 变成 `kind: 'unauthorized'`；加密失败变成 `kind: 'encryption'`；网络失败变成 `kind: 'network'`。domain 测试里可以自己造一个 `{ request: async () => ... }` 当 `HttpClient`，根本不必碰 `TransportError`。

**文档冲突（本课张开，不抹平）：** `platform-http` 的 README / AGENTS 仍写「定义请求、响应、错误、取消和拦截端口，供 domain 依赖并由具体 adapter 实现」。工作树否决后半句：

1. 请求形状 `HttpClient` 在 `platform-contracts`。
2. domain 的 `package.json` **没有** `@namewta/platform-http`。
3. 唯一运行时消费者是 `adapter-axios-browser`。

以 `src/index.ts` 和依赖图为准。把过期摘要记成来源问题，不要改口诀去迎合 README。

### `A:platform-auth`：流程，不是会话仓库

公开入口 `frontend/packages/platform/auth/src/index.ts`。只依赖 `platform-contracts`。

两块公开能力：

1. **`requestRelogin({ navigation, presenter, session, state })`。** 用 `ReloginState.show` 当单飞锁：确认过期 → `session.logout()` → `navigation.replaceWithLogin(当前路径)`。取消或 UI 不可用都在 `finally` 里把锁放开。App 把 Element Plus 的 `ElMessageBox`、本厅堂 logout、本厅堂 router 塞进 ports。
2. **`createSsoAuth(ports)`。** 端口是 `SsoAuthPorts`：`exchangeToken`、`navigate`、`randomBytes`、`sha256`、`storage`。包自己不算 PKCE 之外的密码学、不 `fetch`、不读 `sessionStorage`。admin 的 `application/sso.ts` / home 的对应文件把这些孔插上，用本厅堂 HTTP 打 `/sso/oauth2/token`。

`ClientContext`、`SessionStore`、`TokenStorage` **不在这个包**。README 写「认证会话、ClientContext、令牌端口」时，那些符号在 `contracts`。auth 包是「拿到别人的插头之后怎么走完重登 / SSO」。PKCE 向量与 RFC 测试在包内；函数级换票副作用留给 SSO 课。

### `A:platform-permission`：快照求值，失败关闭

公开入口 `frontend/packages/platform/permission/src/index.ts`。零依赖。

`createAccessEvaluator(snapshot)` 吃 `permissions` / `roles` 两个未知形状，不是字符串数组就当成空。求值器是冻结对象：

- 权限：命中具体串，或命中 `*:*:*`（`ALL_PERMISSIONS`）。
- 角色：命中具体串，或角色里有 `superadmin` / 遗留别名 `admin`（`SUPERADMIN_ROLE` / `LEGACY_ADMIN_ROLE`）。
- 空数组、空串、非数组：**拒绝**（`hasAny*` / `hasAll*` 都是 false）。

这是前端「藏按钮 / 拆 DOM」的尺子，**不是**后端授权。后端仍是最终授权者。Vue 指令不进这个包：`web-kit-permission` 的 `installWebPermissionHost` 拿 `() => AccessEvaluator`，在 `mounted` 里 `hasAnyPermission` / `hasAnyRole`，不允许就从 DOM 拿掉。admin 在 `application/access.ts` 用当前用户 Store 造求值器；home 把 `hasPermission` 函数传进 self-profile runtime，不装指令。

### `A:platform-app-runtime`：组合器 + 导航恢复

公开入口 `frontend/packages/platform/app-runtime/src/index.ts`，再导出 `routeAssembler`、`navigationRecovery`。无运行时业务依赖。

三件厅堂会摸到的工具：

1. **`composeAppRuntime({ appId, domainModules, manifests, selectedDomainIds, selectedManifestIds })`。** 只收下**已选** domain 与 manifest。缺模块、manifest 所属 domain 未选、重复 `componentKey` / registration id / message namespace / permission contribution id，一律抛 `AppRuntimeError`。结果冻结。`resolve({ componentKey, domainId })` 找不到键也失败关闭。
2. **`restoreProtectedNavigation`。** 顺序写死：`loadIdentity()` → `loadRoutes()` → 非外链 `addRoute` → `createReplacement()`。admin 的 `src/permission.ts` 把 `getInfo`、导航 Store、`router.addRoute` 填进这些孔。
3. **`projectServerRoutes` / `assembleServerRoutes` / `findDuplicateRouteNames`。** 把服务端菜单树投影成厅堂路由。是否展平 `ParentView`、缺组件时给什么诊断组件，由调用方注入。空 children 不保留隐式 redirect。

domain 依赖这个包，主要是为了导出 `DomainModule`（`id` / `backendModules` / `capabilities`）。web-domain 依赖它，是为了 `WebDomainManifest` / `WebRegistration`。页面组件本身仍在 web-domain，不在 platform。

### `validation`（点名，不认格子）

`@namewta/platform-validation` 提供 `validateFormat`、`validatePassword`、`toValidator`。`domain-admin` 用它挡密码策略；admin-web 也声明了它。它是跨领域的纯校验，不是 HTTP 端口，也不是 adapter。L-012 再讲密码策略怎么挡住注册。本课记住：第六个目录存在，矩阵里没有 `A:platform-validation`。

## 核心概念与机制

### 直觉讲解

把厨房（domain）想成只认「圆孔 220V」的电器。它不管墙上实际插的是德标还是国标转接头，更不管电线是铜还是铝。

- **规格纸**贴在 `platform-contracts`：圆孔长什么样（`HttpClient.request`）。
- **事故单**印在 `platform-http`：短路时怎么分类（网络 / 业务 / 未授权 / 加密），且不准把 Bearer token 抄到事故单背面。
- **德标插头**是 `adapter-axios-browser`：里面才允许出现 axios。
- **厅堂电工**是 App：买插头、写下 ClientId、把 token 钥匙命名为 `Admin-Token` 或 `Home-Token`，再把圆孔那一头递给 `createSystemService(http)`。
- **权限尺**是 `platform-permission`：对着这张餐牌上印的权限串量一量按钮显不显示。后厨（后端）仍可以拒绝真的炒菜。
- **排菜表**是 `composeAppRuntime`：今晚菜单上没点的菜，厨房里即使有也不许上桌。

第二家餐厅（home-web）再买一套插头、再请一次电工，厨房图纸不用复印。认人亭（sso-web）根本不进这面墙：它自己用 `fetch` 问「你是谁」。

**类比失效处：** 电工（App）**可以**直接读规格纸和排菜表，不必先点一道菜。这就是 **App → platform**。失效的另一边：你不能因为厅堂 `package.json` 里还有 `"axios": "catalog:"`，就说 domain 也可以 import axios。那是厅堂壳的存量边（admin 甚至有 `application/host/download.ts` 直接 import axios），不是厨房的许可证。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 端口 | port | 跨领域、跨终端的接口或最小合同。本课最硬的 HTTP 端口是 `platform-contracts` 的 `HttpClient`，不是 axios 实例 |
| 适配器 | adapter | 某运行时对端口的实现。浏览器 HTTP 适配器是 `@namewta/adapter-axios-browser` |
| 组合根 | composition root | App：创建 adapter、注入 domain 工厂、`composeAppRuntime`、把 UI ports 塞进 `requestRelogin` |
| 运输错误 | `TransportError` | `platform-http` 的错误类型；adapter 把运行时异常映射到 `kind` / `code` / `isHandled` |
| 已处理错误 | handled error | `isHandled === true`：UI 已经展示过，调用方不要再弹一次 |
| 访问求值器 | `AccessEvaluator` | 对当前会话权限/角色快照的纯查询；失败关闭；不替代后端 |
| 应用运行时 | `AppRuntime` | `composeAppRuntime` 的冻结结果：已选 manifest 的组件键、文案、权限贡献、`resolve` |
| 运行时错误 | `AppRuntimeError` | 组合期失败：重复键、未选 domain、缺模块、缺组件键 |
| 导航恢复 | `restoreProtectedNavigation` | `getInfo → getRouters → addRoute → replace` 的端口化顺序 |
| 终端纯度 | terminal purity | domain / platform 不得依赖 axios/vue 等终端库，也不得碰未遮蔽浏览器全局 |
| 显式 Client | `ClientContext` | 每个请求链必须有非空 `clientId`；缺了就抛，不默选 |

### 机制/因果链

一次管理端登录后拉菜单，插座怎么通电：

1. **App 创建会话插头。** `createBrowserSessionStore({ key: 'Admin-Token' })`（home 是 `Home-Token`）。实现位于 `adapter-storage-browser`，形状是 `SessionStore`。
2. **App 创建 HTTP 插头。** `createAxiosBrowserAdapter({ baseURL, client: { clientId }, getToken, errorPresenter, onUnauthorized, crypto, ... })`。`onUnauthorized` 里调用 `requestRelogin`，把 presenter / session.logout / router.replace 填进 `platform-auth`。axios 只活在这一跳。
3. **App 把细的 `HttpClient` 递给 domain。** admin 的 `application/services.ts` 故意包一层 `domainHttp.request = adminHttp.request`，切断初始化环，再 `createSystemService(domainHttp)`、`createIdentityAccessService({ http: domainHttp, session, client, identity })`。domain 源码写 `import type { HttpClient } from '@namewta/platform-contracts'`。
4. **domain 只认 `http.request({ url, method, params, data })`。** 例如 `createSystemService` 里 `GET /system/client/list`。测试里用手工对象冒充 `HttpClient`，不启动 axios。web-domain 页面调的是这些 service，不是 axios。
5. **App 直连 `composeAppRuntime`。** admin 选多个 domain/manifest id；home 选 `admin` + `profile`，manifest 是 `web-domain-admin` 与 `web-domain-profile-self`。未选的 registration 根本不进 runtime。`resolve` 失败关闭，不用 click-time 注册掩盖漏接。
6. **登录后 `restoreProtectedNavigation`。** 身份没有 roles 时：拉 `getInfo`，再拉菜单投影，非外链 `addRoute`，最后 `replace` 回目标。任一步抛错就 logout，不把用户送进未注册页。
7. **权限尺接上。** admin：`createAccessEvaluator` → `installWebPermissionHost`（**App → web-kit → platform-permission**）。home：同一把尺，函数传入 self-profile，不挂全局指令。
8. **真正出门。** adapter 填 `Authorization` / `clientid` / 语言，必要时走 `CryptoPort`，把后端 `code` 映射成 `TransportError`。JSON 形状可以对齐 `api-contracts`（L-010 / L-005），前端仍不 import Java 类型。

因果：厨房只认圆孔 → 换插头（浏览器 axios / 未来 taro）不必改 `createSystemService`。厅堂自己拼 runtime → 第二个 App 才能选子集。事故单与规格纸分开 → domain 测试不必依赖 axios 错误形状。

## 图、表或文本图

**图题 / caption：** platform 端口、adapter 实现、App 接线。五个认格子包按真实 import 画箭头；`validation` 虚线点名；`platform-http` 只挂在 axios adapter 下。

```text
  admin-web / home-web                         sso-web
  厅堂电工                                      认人亭（不进这面墙）
  application/{session,http,services,sso}.ts    ssoApi.ts fetch
  *ManifestRegistry.ts  composeAppRuntime
  permission.ts         restoreProtectedNavigation
           |
           |  App → adapter
           v
  adapters/axios-browser  ----implements---->  HttpClient          (contracts)
                          ----consumes------->  TransportError      (http)
  adapters/storage-browser ----implements---->  SessionStore        (contracts)
  adapters/crypto-browser  ----implements---->  CryptoPort          (contracts)
  adapters/oss-upload-…    ----implements---->  UploadClient        (contracts)
           |
           |  App 把 HttpClient / SessionStore 注入 domain 工厂
           v
  domains/*  -------------------------------->  platform-contracts
             -------------------------------->  platform-app-runtime   (DomainModule)
             - - - - - - - - - - - - - - - ->  platform-validation    (仅 domain-admin；无格子)
  web-domains/* ----------------------------->  platform-app-runtime   (manifest 类型)
                ----→ domain ----→ contracts
           ^
           |  App → platform（一等边，不必绕 domain）
           |
  platform-app-runtime   composeAppRuntime / restoreProtectedNavigation / projectServerRoutes
  platform-auth          requestRelogin / createSsoAuth   (注入 contracts ports)
  platform-permission    createAccessEvaluator
           |
           |  web-kit-permission → platform-permission
           v
  指令宿主只在 App 安装；web-domain 不拥有 v-hasPermi 全局单例

  禁止：domain/web-domain/platform → axios
  禁止：App → platform-http
  禁止：platform → domain / App / Vue
  允许：platform → platform（http/auth → contracts）
```

**文字等价物：** 图顶上是两个业务厅堂，它们当电工：各自创建 session / HTTP / SSO 插头，调用 `composeAppRuntime` 和 `restoreProtectedNavigation`。右边 sso-web 单独走开，用 `fetch` 打 SSO，不进入 platform 包树。厅堂向下先到 adapters：axios 适配器实现 `HttpClient`（形状来自 contracts），并消费 `platform-http` 的 `TransportError`；storage / crypto / oss 适配器分别实现 contracts 里的会话、加密、上传端口。厅堂把细的 `HttpClient` 交给 domain 工厂。domain 只依赖 contracts（以及 app-runtime 的 `DomainModule`）；web-domain 只为 manifest 类型依赖 app-runtime，页面调用仍走 domain。厅堂还有一条不经过 domain 的直连：app-runtime、auth、permission。permission 再被 web-kit 的指令宿主消费，指令由 App 安装。图用虚线标出 `platform-validation`，并写明本课不认这格。图底下列出禁止边：厨房和插座墙不得 import axios；厅堂不得声明 `platform-http`；platform 不得回头依赖 domain 或 App。

**图的边界：** 不保证 taro adapter 已实现——磁盘上仍是 README 占位。不保证 admin-web 已清掉对 axios 的直接依赖（`host/download.ts` 仍 import axios）。不画出每一个 `create*Service`。不展开 PKCE 字节、菜单十条 `componentKey`、OpenAPI 字段。home-web 不声明 `platform-contracts` 仍合法：它经 adapter 间接用到形状，自己的源码没 import 该包。本机未验证生产部署。

## 正例、反例与边界

**正例 1（`A:platform-contracts` + domain 不认 axios）：** `createSystemService(http: HttpClient)`、`createIdentityAccessService({ http, session, client, identity })`、`createThirdService(http)` 都只 import `platform-contracts` 的类型。`packages/domains/**/package.json` 与 `packages/web-domains/**/package.json` 都没有 `axios`。源码 `from 'axios'` 在 domain / web-domain / platform 中为零。domain 测试构造 `{ request: vi.fn() }` 即可。

**正例 2（`A:platform-http` 的真实消费者）：** `frontend/packages/adapters/axios-browser/package.json` 同时声明 `platform-contracts` 与 `platform-http`。admin-web / home-web 的 `package.json` **没有** `@namewta/platform-http`。这是允许的 **App → adapter → platform-http**，不是 App 漏写依赖。

**正例 3（`A:platform-auth` 注入，不拥有 UI）：** admin 与 home 的 `application/http.ts` 都把 `ElMessageBox.confirm`、本厅堂 logout、`router.replace({ path: '/login' })` 塞进 `requestRelogin`。auth 包测试用假 presenter / 假 navigation，证明单飞锁：连续三次调用只确认一次。SSO：`createSsoAuth` 的 `exchangeToken` 由厅堂用**已经接好的** HTTP 去打 `/sso/oauth2/token`。

**正例 4（`A:platform-permission` 失败关闭）：** 快照里 `permissions` 不是数组、是空串、或 `hasAllPermissions([])`，求值器都返回 false。超管角色 `superadmin` 或遗留 `admin` 才对任意角色询问放行。admin 用它驱动 `v-hasPermi`；home 用同一函数做 self-profile 按钮，不装 web-kit。

**正例 5（`A:platform-app-runtime` 显式选择）：** home 的 `composeAppRuntime` 只选 `['admin', 'profile']` 与 `['web-domain-admin', 'web-domain-profile-self']`。即使仓库里还有 system web-domain，home 的 runtime `resolve` 也解析不到那些组件键。admin 的 `restoreProtectedNavigation` 把 `getInfo` / `generateRoutes` / `router.addRoute` 填进端口，失败则 logout。

**反例 1：** 在 `packages/domains/system/src/user/service.ts` 写 `import axios from 'axios'`。门禁 `terminal-purity` 会抓。第二个终端将被迫拖着浏览器 axios。

**反例 2：** 看见厅堂用了 HTTP，就在 `admin-web/package.json` 加上 `@namewta/platform-http`。消费者是 adapter。厅堂要的是插头。

**反例 3：** 把 `HttpClient` 接口搬进 `platform-http`，再让所有 domain 改 import。这是把规格纸和事故单订成一本，domain 测试开始依赖运输错误类型。工作树没有这样做。

**反例 4：** web-domain 自己 `axios.create` 当「页面专用请求器」，或在 web-domain 里读 `localStorage` 的 token。会话属于 App 命名空间；web-domain 只通过 runtime / domain 服务说话。

**反例 5：** 把 `createAccessEvaluator` 当成授权。前端藏了按钮，后端 `SysMenuController` / 数据权限仍必须拒绝。缺会话时求值器为空，指令必须拆掉 DOM（web-kit 已这样失败关闭）。

**反例 6：** `composeAppRuntime` 把未选 domain 的 manifest 悄悄收进来，或用 `import.meta.glob('/src/views/**')` 在 admin 里本地扫页面。门禁 `admin-navigation-boundary` 要求动态页走已选 manifest；runtime 对 `unselected-domain` / `missing-component-key` 抛错。

**边界：**

- App **可以**依赖 axios（两个厅堂目前都声明了 catalog；admin 的 `host/download.ts` 仍直接 import）。这是厅堂壳存量，不是「厨房也可以」。新的领域请求必须走注入的 `HttpClient`。
- home-web 不声明 `platform-contracts`、不声明 web-kit，仍是合法厅堂。
- sso-web 不声明任何 platform / adapter，仍是 App。
- `platform-http` 可以依赖 `platform-contracts`（platform → platform）。反向不行。
- `taro-*` 只是 README：不要把它们画进当前组合。
- `validation` 存在且被 `domain-admin` 使用；不要为它编一个本课格子。

## 变式与迁移

- **变式 A：README 把 http 包说成「请求端口」。** 并排记下冲突，不要合成假和平：README/AGENTS 说 domain 依赖 http 端口；`src/index.ts` 只有 `TransportError`；domain 依赖 `HttpClient`（contracts）；唯一消费者是 axios adapter。口诀保持：「规格纸在 contracts，事故单在 http，插头在 adapter，电工在 App。」

- **变式 B：口诀缺边。** 只背「App → web-domain → domain → platform」会把 `composeAppRuntime` 画成必须先经过一道菜。厅堂直接 import app-runtime。只把 adapter 画在 platform「下面」却不写谁 import 谁，会让人给 App 加上 `platform-http`。Skill `01-module-map.md` 与 `frontend/AGENTS.md` 仍常漏 **App → platform** 和 **adapter → platform-http**。以 allowlist 与 `package.json` 为准。

- **变式 C：纯函数端口不需要浏览器插头。** `createAccessEvaluator` 和 `validateFormat` 没有 axios 版 adapter。它们的「适配」是：App 提供快照或把函数传入 web-domain。不要为它们发明 `adapter-permission-browser`。

- **变式 D：流程包依赖端口，不拥有端口。** `requestRelogin` / `createSsoAuth` 要 `ErrorPresenter`、`NavigationPort`、`SsoAuthPorts`。换一套 UI（不一定 Element Plus）只改 App 填孔，不改 auth 包。

- **迁移：新增终端。** 先实现该运行时的 adapter（实现 `HttpClient` / `SessionStore` / 需要时 `CryptoPort`），在**新 App** 里接线，把同一批 domain 工厂再调一次。不要复制 `admin-web/src/utils/request.ts` 进 domain。`taro-request` 从 README 变成真包时，它应依赖 `platform-http` + `platform-contracts`，而不是让 domain 依赖 Taro。

- **迁移：新增领域请求。** domain 工厂增加 `http.request({ url, method })`；App 的 `services.ts` 已经注入的那根 `HttpClient` 足够。不要在 web-domain 里新开 axios。菜单键还要进目标 App 的 `composeAppRuntime` 选择集（L-007 / L-020）。

- **迁移：清厅堂 axios 直依赖。** 目标是领域请求全部经 adapter。`host/download.ts` 这类厅堂壳应收敛到 `downloadWithAxios`（adapter 已导出）或继续留在 App，但不得下沉进 domain。本课不发动全仓删除 axios 声明。

## 常见误区

1. **「platform-http 就是 HttpClient。」** 工作树里 `HttpClient` 在 contracts。http 包是 `TransportError`。
2. **「domain 依赖 platform-http。」** 依赖图否决。domain 依赖 contracts（以及 app-runtime 的模块描述）。
3. **「厅堂用了 HTTP，所以 App 该声明 platform-http。」** 消费者是 adapter。
4. **「ports 只能出现在 domain 下面，App 不能碰 platform。」** App → platform 是一等边。漏的是口号。
5. **「adapter 画在 platform 下面，所以 platform 实现 axios。」** 方向反了：adapter 实现 ports，platform 禁止 axios。
6. **「createAccessEvaluator 放行了，后端就该放行。」** 前端尺子只管显隐。授权在后端。
7. **「composeAppRuntime 会自动发现 packages/web-domains 里所有页面。」** 只收 `selectedManifestIds`。未选等于不存在。
8. **「requestRelogin 会自己找 Element Plus / Vue Router。」** 它只调注入的 ports。找不到 UI 就放弃并解锁。
9. **「sso-web 没接 platform，所以端口设计失败。」** 认人厅产品范围就是认人；换票发生在两个厅堂的 `createSsoAuth`。
10. **「validation 没出现在 OBJ 里，所以目录是垃圾。」** 目录真实存在，只是本课不认格子。
11. **「home 没声明 platform-contracts，所以它的 domain 不走端口。」** home 声明了 axios adapter 和若干 domain；domain 自己声明 contracts。App 清单不必重复每一层。
12. **「在 platform 里读 localStorage 比较方便。」** `terminal-purity` 禁止 platform / domain 碰未遮蔽浏览器全局。钥匙名属于 App。

## 非评分暂停

打开磁盘，不要凭 README 背。不要改依赖去对齐过期摘要。

1. 列出 `frontend/packages/platform/` 下六个目录，对照五个认格子包，把 `validation` 单独放在旁边。
2. 打开五个认格子包的 `src/index.ts`：在 contracts 里找到 `HttpClient`；在 http 里确认没有 `HttpClient`、只有 `TransportError`；在 auth 里找到 `requestRelogin` 与 `createSsoAuth`；在 permission 里找到 `createAccessEvaluator`；在 app-runtime 里找到 `composeAppRuntime` 与 `restoreProtectedNavigation`。
3. 打开 `adapter-axios-browser/package.json`、`admin-web/package.json`、`home-web/package.json`、任意一个 `packages/domains/*/package.json`：谁声明了 `platform-http`，谁声明了 `axios`，谁只声明了 `platform-contracts`。
4. 打开 `admin-web/src/application/http.ts` 与 `services.ts`：认出 `createAxiosBrowserAdapter`、`requestRelogin`、以及 `createSystemService(domainHttp)` 这三跳。
5. 打开 `frontend/tooling/architecture/src/index.mjs` 的 `runtimeLayerAllowlist` 与 `terminalPackage`：核对 App 可指向 platform，adapter 只指向 platform，domain/platform 不得依赖 axios。

## 总结、词汇表与下一步

- 磁盘：`platform/{contracts,http,auth,permission,app-runtime,validation}`。本课认前五个格子。validation 存在，不编第六格。
- 形状：`HttpClient` / `SessionStore` / `CryptoPort` / `UploadClient` / `ErrorPresenter` / `NavigationPort` 在 **contracts**。
- 事故单：`TransportError` 在 **http**；唯一运行时消费者是 **axios adapter**。
- 流程：auth 用注入的 ports 做重登与 SSO；permission 对快照求值；app-runtime 显式组合并失败关闭。
- 方向：主链 App → web-domain → domain → platform；一等边 App → platform；插头边 App → adapter → platform。domain / web-domain / platform 不依赖 axios。App 不声明 `platform-http`。
- 文档冲突保持张开：http/auth 的 README 句子大于源码时，以 `src/index.ts` 和 `package.json` 为准。

词汇表：port / adapter / composition root / `HttpClient` / `TransportError` / `AccessEvaluator` / `AppRuntime` / `restoreProtectedNavigation` / terminal purity / `ClientContext`。

下一步 L-010：adapters、web-kit、`api-contracts` 各自所有权，以及「生成的 transport 不可手改」。L-014 再顺着 `IdentityAccessService` 看 `/auth/*`。L-020 再看 system web-domain 怎样消费这套导航 host。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web,sso-web}` 的 `package.json` 与 `src/application/{http,session,services,sso,access}.ts`、`permission.ts`、`*ManifestRegistry.ts` | 两厅堂直连 app-runtime / auth / permission；不声明 platform-http；HTTP 经 axios adapter；session 经 storage adapter；`requestRelogin` / `createSsoAuth` / `composeAppRuntime` / `restoreProtectedNavigation` / `createAccessEvaluator` 的接线；sso-web 不进 platform 树 | 工作树 App | 2026-09-16 |
| S-006 | `frontend/packages/platform/{http,auth,permission,app-runtime,contracts,validation}` 的 `src/index.ts`、测试、`package.json`、README/AGENTS；`packages/adapters/{axios,storage,crypto,oss-upload}-browser`；`packages/domains/**/package.json`；`packages/web-domains/**/package.json`；`packages/web-kit/permission` | 五包真实 export；HttpClient 在 contracts；http 仅 TransportError 且唯一消费者为 axios adapter；auth/permission/app-runtime 公开符号；validation 存在；domain/web-domain 无 axios；adapter 实现 contracts 端口 | 工作树 packages | 2026-09-16 |
| S-007 | `.agents/skills/engineering-standards/references/project/01-module-map.md`；`namewta-fullstack-development/references/frontend/architecture.md`；`frontend/AGENTS.md`；`frontend/tooling/architecture/src/index.mjs` | 公开口号常写成 App → web-domain → domain → platform 与 App → adapter/web-kit，漏 App → platform；ARCH-003；allowlist 与 terminal-purity 以工作树门禁为准 | 模块地图 / ARCH-003 / allowlist | 2026-09-16 |
| S-016 | `{roots.state}/learning/learner-profile.md` | 本课 `zh-CN`、`eli5`、deep | 学习者偏好 | 2026-09-16 |
