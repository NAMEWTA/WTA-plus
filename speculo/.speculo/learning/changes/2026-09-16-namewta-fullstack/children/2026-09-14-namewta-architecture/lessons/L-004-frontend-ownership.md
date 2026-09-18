---
lesson_id: L-004
objective_ids: [OBJ-04]
claimed_cells: [C:admin-web, C:home-web, C:sso-web, C:frontend-direction]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: three-apps-on-disk
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
source_ids: [S-001, S-002, S-003, S-004, S-006, S-008, S-009]
---

# Lesson 004：前端所有权与三个 App

## 学完你能做什么

你能顺着依赖方向说两句，而不是只背一句口号：

1. **主链：** App → web-domain → domain → platform。
2. **一等边：** **App → platform（ports）**。厅堂可以直接拿运行时、认证、权限这些插头形状，不必先绕一圈 domain。

再补两句实现：

- **adapter 实现 ports。** 浏览器 HTTP 插头是 `@namewta/adapter-axios-browser`；它才依赖 `@namewta/platform-http`。`platform-http` 的消费者是 adapter，不是 App。
- 厅堂还可以 **App → adapter / web-kit**（插插头、挂店徽）。App 之间没有箭头。web-domain 收厅堂塞进来的组件，不是 web-domain import App。

你能指着工作树说出三个真实 App 包：

- `admin-web`：组装业务 web-domain 的管理厅堂。
- `home-web`：身份 + 资料**子集**；档案页工厂是 `createProfileSelfWebDomain`，**不是** admin 的 `createProfileWebDomain`。
- `sso-web`：认人厅，HTTP 在 `ssoApi.ts`，不是管理后台。

你能把「磁盘上几个可构建包」和「文档把谁写成产品 App / 发布面」分成两句，不把冲突吞掉。

本课认格子：`C:admin-web`、`C:home-web`、`C:sso-web`、`C:frontend-direction`。工厂全表、manifest 十条 id 留给 L-007 / L-020；OpenAPI 字段留给 L-005；SSO 函数签名与 PKCE 留给后续 SSO 课。

## 先把宏观地图放在桌上

旧前端像一个大书包：页面、接口、类型全塞进同一个 App 的 `src/api`。要开第二个窗口，就再复印一个书包。NAMEWTA 把书包拆开，让厅堂（App）变多，中央厨房（domain / web-domain）只留一份。

```text
frontend/
├── apps/
│   ├── admin-web/     ← 管理端厅堂（src + build 都在）
│   ├── home-web/      ← 用户门户厅堂（src + build 都在）
│   └── sso-web/       ← 第一方 SSO 认人厅（src + build 都在）
└── packages/
    ├── web-domains/   ← Vue 页面、manifest（有界面）
    ├── domains/       ← 模型与服务（无 DOM）
    ├── platform/      ← 端口：HTTP、权限、运行时、认证……
    ├── adapters/      ← 端口的浏览器实现
    ├── web-kit/       ← 多页面共用的壳、指令
    └── api-contracts/ ← OpenAPI 生成的 transport（细节见 L-005）
```

方向先记口诀，四条一起记：

- **App → web-domain → domain → platform**（主链：厅堂选菜单照片，照片调厨房，厨房认插头形状）。
- **App → platform（ports）**（一等边：厅堂自己接 `composeAppRuntime`、`requestRelogin`、`createSsoAuth`）。
- **adapter 实现 ports；`platform-http` 的消费者是 adapter。** App 拿 axios adapter，不声明 `platform-http`。
- **App → adapter / web-kit**（厅堂插插头、挂店徽）。**App 禁止 → 另一个 App。**

工作树门禁 `runtimeLayerAllowlist`（`frontend/tooling/architecture/src/index.mjs`）允许 App 直连 `adapter`、`domain`、`platform`、`web-domain`、`web-kit`；adapter 只连 platform。Skill 模块地图仍常把公开口号写成「App → web-domain → domain → platform，以及 App → adapter/web-kit」，**漏写 App → platform**。以门禁和工作树 `package.json` 为准，把漏边记成来源问题，不要改口诀去迎合过期摘要。

**类比失效处：** 「书包拆开」不是说 App 变空。App 仍拥有 Client、布局、会话名、环境变量、选哪些 domain，并且**直接**依赖若干 platform 包。失效点：你不能把可复用列表页写回 `apps/admin-web/src/views/system/...` 当长期主人；也不能因为厅堂 `views/` 里还有工作台、redirect、iframe 运维页，就说「CRUD 还没搬完」。那些是厅堂壳，不是领域页。

## 三个 App 在磁盘上长什么样

这一节是 deep 升级的硬证据。先认目录，再认依赖，不要用「文档说几个」代替 `ls`。

三个包都有 `src/` 和 `"build"` 脚本。`frontend/pnpm-workspace.yaml` 收 `apps/*` 与 `packages/*/*`。根 `frontend/package.json` 的 `build:prod` 走 `pnpm build:workspace`（`pnpm architecture:check && pnpm -r --if-present build`）。只要声明了 `build`，就会被递归构建点到。`build:dev` 还显式 `--filter @namewta/admin-web` 与 `--filter @namewta/home-web`。

| 包 | 产品角色 | 本课「全部 / 子集」怎么数 | 会话 |
| --- | --- | --- | --- |
| `@namewta/admin-web` | 管理厅堂 | `package.json` 声明全部八个业务 `@namewta/web-domain-*`；runtime 再选多个 domain id 与多个 manifest id（条数不必等于八） | `Admin-Token` |
| `@namewta/home-web` | 用户门户 | 只声明 `web-domain-admin` + `web-domain-profile`；档案用 **self** 工厂 | `Home-Token` |
| `@namewta/sso-web` | 认人厅 | 不声明任何 domain / web-domain / platform / adapter | SSO cookie |

这三套可数对象**不是同一个数字**：包依赖、`selectedDomainIds`、`selectedManifestIds` 可以不一致。本课不把它们合成一句假和平。admin 选了多个 domain/manifest id——完整名单是 L-007 / L-020，本课不背十条 id，也不背每一个 `create*Service`。

### `frontend/apps/admin-web`：组装业务 web-domain

入口 `src/main.ts`。脚本有 `dev`、`build` / `build:prod`、`test`、`typecheck`。

它声明八个业务 domain 包和八个业务 web-domain 包，并直接依赖 platform：`platform-app-runtime`、`platform-auth`、`platform-permission`、`platform-contracts`、`platform-validation`。浏览器 adapter（axios / storage / crypto / oss-upload）和 web-kit（permission / file-upload）也是厅堂自己的依赖。**没有** `@namewta/platform-http`：HTTP 端口形状由 axios adapter 去实现。

容器主路径（名字要认得，工厂表不要背）：

1. `createBrowserSessionStore({ key: 'Admin-Token' })`（`application/session.ts`）。
2. `createAxiosBrowserAdapter`（`application/http.ts`）——App → adapter；adapter → `platform-http`。
3. `application/services.ts` 用本厅堂的 http / session 创建 domain 服务（接线板存在即可）。
4. `adminManifestRegistry.ts` 里 `composeAppRuntime`，再 `resolveAdminWebRegistration` 把菜单组件键解析成已选 manifest 里的页面。
5. 登录后 `permission.ts` 调 `restoreProtectedNavigation`（拉身份、装动态路由）。
6. `directive/index.ts` 调 `installWebPermissionHost`，把 `v-hasPermi` 接到本会话的 evaluator。这是 **App → web-kit**，不是 web-domain 自己拥有指令全局单例。

厅堂 `src/views/` **不是空的**，也不等于「还没搬完的 CRUD」。它拥有：登录/注册、401/404、SSO 回调、账号资料，以及常量工作台 `index.vue`、`redirect/`、iframe 运维 `monitor/external/`（挂在 App 自有的 external-monitor manifest 上）。可复用业务页住在 `packages/web-domains/`。

host 包装器：`src/components/FileUpload/index.vue` 包一层 `@namewta/web-kit-file-upload`，再由 registry **注入** workflow runtime 的 `fileUpload`。箭头仍是 App → web-kit / App 传入 web-domain。`runtimeLayerAllowlist` 禁止 web-domain → app。注入 ≠ 反向依赖。

### `frontend/apps/home-web`：身份 + 资料子集

描述是 user portal。同样有 `src/` 与 `build`。

`package.json` 只选了 `domain-admin`、`domain-system`、`domain-profile`，以及 `web-domain-admin`、`web-domain-profile`。没有 notify / demo / workflow / third / ai 的 web-domain，也**没有** `web-domain-system`，也**没有** `@namewta/web-kit-*`。platform 直连 `platform-app-runtime` / `platform-auth` / `platform-permission`。同样不声明 `platform-http`。

子集**不是**「同一份管理菜单少点两道菜」：

- 两个厅堂都可以 `createProfileService`。
- admin 的页面工厂是 `createProfileWebDomain`（档案管理）。
- home 的页面工厂是 `createProfileSelfWebDomain`（manifest id `web-domain-profile-self`，用户自己的档案）。
- **`createProfileSelfWebDomain` ≠ `createProfileWebDomain`。**

`domain-system` 在、`web-domain-system` 不在，不是漏接：`createIdentityAccessService({ identity: systemService.identity, ... })` 只要系统身份能力喂给登录，不要系统管理页面。home 的 `hasPermission` 是 App 函数，传进 self-profile runtime，不装 admin 那套 `v-hasPermi` 指令。

接线板仍叫 `application/services.ts`。`homeManifestRegistry.ts` 用 `composeAppRuntime`，`selectedDomainIds` 只有身份和资料。动态路由在 `src/router/index.ts` 调 `restoreProtectedNavigation`。会话钥匙 `Home-Token`。`src/views/`：`PortalPage.vue`、`RegisterPage.vue`、`SsoCallbackPage.vue`。home-web **没有** import admin-web 的任何文件。

### `frontend/apps/sso-web`：认人厅，不是管理后台

描述是 first-party SSO login origin。有 `src/` 与 `build`（`vite build --mode production`），`preview` 端口 `4176`。

依赖极瘦：只有 `vue` 和 `vue-router`。没有 domain、web-domain、platform、adapter。不要因此说它「不是 App」——它仍是 `apps/` 下的终端包。组合多少厨房，是产品选择，不是 App 资格考试。

`src/main.ts` 三条路由 `/`、`/authorize`、`/login` 都指向同一张 `AuthorizePage.vue`。`src/ssoApi.ts` 用 `fetch` + `credentials: 'include'` 打 `/sso/session`、`POST /sso/login`、`GET /sso/oauth2/authorize`。这是认人厅：问「你是谁」，发授权码，把浏览器送回业务 App。

**换票不在认人厅。** admin / home 各自的 `application/sso.ts` 调 `createSsoAuth`（platform-auth），用本厅堂 HTTP 打 `/sso/oauth2/token`，把 access token 写入 `Admin-Token` / `Home-Token`。SSO cookie 不跟管理端菜单、按钮权限、动态路由共用。本课不展开 PKCE，不背 `parseAuthorizeQuery`。

### 共享包目录确实存在

`frontend/packages/README.md` 按所有权而不是按文件类型分房间。工作树抽查（2026-09-16）：

| 目录 | 磁盘上有什么 | 本课怎么用 |
| --- | --- | --- |
| `packages/domains/` | admin、ai、demo、notify、profile、system、third、workflow | 无 DOM 的模型/服务 |
| `packages/web-domains/` | 同上八个名字 | Vue 页面与 manifest |
| `packages/platform/` | app-runtime、auth、contracts、http、permission、validation | 插头形状（ports） |
| `packages/adapters/` | axios-browser、crypto-browser、oss-upload-browser、storage-browser 有源码；`taro-request` / `taro-storage` 是 README 占位 | 实现 ports；axios-browser 依赖 `platform-http` |
| `packages/web-kit/` | permission、file-upload、rich-text 有源码；`ui-element` 仍是占位 | 多页共用壳；当前消费者主要是 admin-web |
| `packages/api-contracts/` | `generated/openapi.ts` + `openapi/current.json` | 生成的 transport；手改生成结果是错的（L-005） |

## 核心概念与机制

### 直觉讲解

三家餐厅共用中央厨房。菜单照片（web-domain）可以各挂各的墙；菜怎么从后厨点出来（domain）只有一份；插座规格（platform ports）统一；插头（adapter）按浏览器来做；托盘和店徽（web-kit）给已经有店在用的东西。

新开一家餐厅（新 App）时，不要把中央厨房再盖一遍。选你要的菜，再配自己的厅堂和收银。厅堂还要自己认插座规格：运行时怎么拼、过期怎么重新登录、SSO 怎么换票——这些是 **App → platform**，不是「先点一道菜才允许碰插座」。

HTTP 这只插头更严：规格纸（`platform-http`）放在平台柜里，真正插进墙上的是 axios adapter。厅堂买的是插头，不是规格纸。

`admin-web` 点了整本业务菜单（八个 web-domain 包），runtime 里还会多挂几张厅堂自己的菜单纸。`home-web` 只点登录和**自己的**档案，不是把管理端档案页缩成手机版。`sso-web` 甚至不进中央厨房：它是门口的认人亭，认完人把你送回厅堂；厅堂自己去换自己的餐牌（Token）。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 终端应用 | App | `apps/*`：Client、入口、布局、品牌、会话命名空间、领域选择、部署。三个真实包是 `admin-web`、`home-web`、`sso-web` |
| 表现域 | web-domain | Vue 页面、领域组件、hooks、语言包、动态路由 manifest；不拥有 App 全局单例 |
| 领域包 | domain | 无 Vue/DOM 的类型、查询/命令服务、transport 映射；对齐后端模块名 |
| 平台端口 | platform port | 跨领域抽象：HTTP、权限、auth、app-runtime、contracts。App **可以**直连其中若干包；HTTP 端口例外见 adapter |
| 适配器 | adapter | 端口在某运行时的实现。`@namewta/adapter-axios-browser` 实现并消费 `platform-http` |
| 共用壳 | web-kit | 已被多个真实消费者证明稳定的 Web 机制（指令、上传控件） |
| 组合 | composition | App 显式 import 并创建服务、把 web-domain 注册进 `composeAppRuntime` |
| 工作区包 | workspace package | pnpm 包；`apps/*` 与 `packages/*/*` 都是包，App 之间禁止互相 import |
| 传输合同 | api-contracts / transport | OpenAPI 生成的线上 JSON 形状；不是 domain 模型，细节见 L-005 |
| 注入 | host injection | App 把包装好的组件/函数传入 web-domain runtime；不是 web-domain → App |

门禁允许的 App 出边：adapter、domain、platform、web-domain、web-kit。反向（domain import Vue 页面、web-domain 拥有登录 token 全局单例、web-domain import App、App import 另一个 App）是 `ARCH-003` 和 `pnpm architecture:check` 要抓的。

### 机制/因果链

1. 浏览器打开某一个 App 的入口（`admin-web/src/main.ts`、`home-web/src/main.ts`、`sso-web/src/main.ts`）。三个入口互不引用。
2. 业务 App 创建自己的 session 与 HTTP：`createBrowserSessionStore` 钥匙名不同（`Admin-Token` / `Home-Token`）；HTTP 走 `createAxiosBrowserAdapter`。需要重新登录时 App 直连 `platform-auth` 的 `requestRelogin`。sso-web 没有这套接线，它用 `ssoApi.ts` 的 `fetch`。
3. 业务 App 在 `application/services.ts` 用这些端口创建 domain 服务。home 用 `systemService.identity` 喂登录，仍不装 system 页面。
4. App **直连** `platform-app-runtime`：`composeAppRuntime` 收下已选 domain 与 manifest。admin 选多个业务 domain/manifest id（含厅堂自有 iframe 运维）；home 只选身份 + self-profile。`resolve*Registration` 是给菜单组件键用的查询口。
5. 登录后 `restoreProtectedNavigation` 按后端菜单把组件键解析成真正的页面。页面（web-domain）只调 domain 服务。admin 另用 `installWebPermissionHost` 挂指令。
6. HTTP 真正出门走 adapter。JSON 形状可以对齐 `api-contracts` 生成物。前端不 import Java 类型。
7. 若走 SSO：浏览器先到 sso-web 认人；授权码回到厅堂 `/sso/callback`；厅堂 `createSsoAuth` 换票。认人厅不写 `Admin-Token`。

因果：可复用规则留在 domain → 第二个 App 才能选同一份规则、换自己的厅堂。复印 `src/api` 会让 Client 隔离和类型漂移立刻发生。sso-web 刻意不复用那份厨房，是因为它卖的是「认人」而不是「菜单」。App 直连 platform，是因为厅堂要自己拼运行时；HTTP 端口却留给 adapter，是为了换运行时（浏览器 / 未来终端）时只换插头。

## 图、表或文本图

**图题 / caption：** 前端所有权与依赖方向。三个 App 包是工作树事实；App → platform 是一等边；`platform-http` 挂在 adapter 下。

```text
  admin-web                 home-web                    sso-web
  厅堂：业务 web-domain      厅堂：身份+self 档案         认人厅
  composeAppRuntime          composeAppRuntime           ssoApi.ts (fetch)
  restoreProtectedNavigation restoreProtectedNavigation  AuthorizePage ×3
  Admin-Token                Home-Token                  SSO cookie
  FileUpload 注入 runtime    无 web-kit                  无 domain 树
     |                          |                           |
     |  8× web-domain 包        |  2× web-domain 包          |  vue + vue-router
     |  + 多个 manifest id      |  profile-self ≠ profile    |
     |                          |  domain-system 无页面      |
     +------------+-------------+                           |
                  |  选择并组合                              |
                  v                                         |
           web-domains/*     web-kit（admin 注入，不是反向） |
                  |                                         |
                  v                                         |
            domains/*  ----→  api-contracts                 |
                  |           （生成 transport）              |
                  v                                         |
             platform/*  ← 插头形状（ports）                 |
                  ^                                         |
                  |  adapter 实现 ports                      |
                  |  axios-browser → platform-http           |
             adapters/*                                     |
                                                            |
     App ──→ platform（一等边）                              |
     App ──→ adapters / web-kit                             |
     App ──→ web-domain ──→ domain ──→ platform             |
     App 禁止 ──→ 另一个 App                                |
     web-domain 禁止 ──→ App                                |
     sso-web ──fetch──→ /sso/*                              |
     厅堂 createSsoAuth ──→ /sso/oauth2/token ──→ Token ────+
```

**文字等价物：** 顶上一排放三个终端，竖线互不相连。左边 admin-web 是管理厅堂：会话 `Admin-Token`，声明全部八个业务 web-domain 包，并在 `composeAppRuntime` 里选多个 domain/manifest id；登录后用 `restoreProtectedNavigation` 解析组件键；上传控件由厅堂包装 web-kit 再注入 runtime。中间 home-web 也是厅堂，会话 `Home-Token`，只组合身份和资料；档案工厂是 `createProfileSelfWebDomain`，不是 admin 的 `createProfileWebDomain`；它依赖 `domain-system` 只为登录身份，不装 system 页面，也不依赖 web-kit。右边 sso-web 是认人亭：三条路由同一张授权页，用 `ssoApi.ts` 打 SSO HTTP，不进 domain 树；换票发生在两个厅堂的 `createSsoAuth`，不在认人亭。从 admin/home 往下，主链是 web-domain → domain → platform；另有一等边 App 直连 platform。adapter 画在 platform 旁边并指回 platform：它实现端口。`platform-http` 只出现在 adapter 那一跳，不出现在两个厅堂的 `package.json`。sso-web 的箭头走向后端 `/sso/*`，不走进这棵包树。

**图的边界：** 不保证三个 App 都已在生产环境部署。本机未验证部署。图不列出 admin 的十条 manifest id，也不列出 `services.ts` 里每一个工厂。图不表示 sso-web 组合了和 admin-web 一样多的 domain。图也不把 `taro-*`、`web-kit/ui-element` 画成已激活包。`api-contracts` 只标明「生成的运输箱」，字段级合同是 L-005。Skill / README 对「当前激活几个 App」的说法与这张图冲突——冲突在下一节张开，不在图里抹平。

## 正例、反例与边界

**正例 1：** `admin-web` 依赖全部业务 `@namewta/web-domain-*`，在 registry 里 `composeAppRuntime`。演示列表页在 `packages/web-domains/demo/...`，不在 admin-web 的 `views/` 里当可复用资源主人。`views/index.vue` 与 `views/monitor/external/` 留在厅堂，是壳和 iframe 运维，不是漏搬的 CRUD。

**正例 2：** `home-web` 自己再组合一次它需要的 domain。`createProfileSelfWebDomain` 与 admin 的 `createProfileWebDomain` 是两个工厂。它不 import `apps/admin-web/**`。

**正例 3：** `sso-web` 把 `/`、`/authorize`、`/login` 都交给 `AuthorizePage.vue`，用 `ssoApi.ts` 打 `/sso/oauth2/authorize`。它不注册 system 菜单，也不装 `v-hasPermi`。换票在厅堂。

**正例 4：** admin-web / home-web 的 HTTP 经 `@namewta/adapter-axios-browser` 出门；session 经 `@namewta/adapter-storage-browser`。两厅堂的 `package.json` 都不声明 `platform-http`。这是允许的 **App → adapter**，加上 **adapter → platform-http**。

**正例 5：** admin-web 直连 `platform-app-runtime` / `platform-auth` / `platform-permission`。这是允许的 **App → platform**，不是漏画的旁路。

**反例 1：** 在 `admin-web/src/api/demo.ts` 再写一套 axios 封装。这是把中央厨房搬回厅堂。架构检查把 App 内 `src/api/*` 当成违规信号。

**反例 2：** `home-web` 用相对路径 `../../admin-web/src/...` 偷组件。App 互引，Client 会话会串。`ARCH-003` 禁止。

**反例 3：** domain 里 `import { ref } from 'vue'` 并操作 DOM。domain 不再能给第二个终端或未来非 Vue 终端用。

**反例 4：** 看见 Skill 写「唯一可构建 App 是 admin-web」，就把 `home-web/`、`sso-web/` 当成 README 占位删掉或忽略。工作树里这两个目录有 `src` 和 `build`，不是占位。

**反例 5：** 把 home 的档案页说成「admin 档案页的瘦客户端」。工厂不同，产品不同。

**反例 6：** 因为 web-domain 用了 `FileUpload`，就让 `packages/web-domains/workflow` import `@/components/FileUpload`。正确方向是 App 注入。

**边界：** App 私有的壳（登录品牌、布局、404、SSO 回调、工作台、iframe 运维入口、host 包装器）可以留在 App。判断标准：有没有第二个终端要复用同一套业务页。会复用的，下沉到 web-domain / web-kit。sso-web 可以不依赖 domain，是因为它的产品范围就是认人，不是「第三个管理后台」。有 domain 无 web-domain（home 的 system）也合法。

## 变式与迁移

- **变式 A：冲突 C-001（Skill / 文档 vs 工作树）。** 不要合成一句假和平。并排记下这些说法，让它们继续打架：
  1. 工作树（S-008）：三个包存在，且能被 workspace 递归构建。
  2. `.agents/skills/engineering-standards/references/project/00-project-profile.md`（S-003）：`frontend/apps/admin-web/src/main.ts` 是「当前唯一可构建和部署的浏览器 App；其他未激活终端仅保留 README 占位」。
  3. `01-module-map.md`（S-004）：`plus-ui` 公开入口仍写 `apps/admin-web/src/main.ts`；方向口号漏 **App → platform**。
  4. `frontend/README.md`（S-008）：「当前激活 `admin-web` 管理端和 `home-web` 应用用户端」。
  5. `frontend/apps/README.md`（S-009）：只点名 admin-web 与 home-web「可构建和部署」。
  6. `docs/namewta-enhancements.md`（S-002）：「当前发布面包含 admin-web 和 home-web；未激活终端不参与工作区构建」。
  7. 根 `README.md`（S-001）：`admin-web` / `home-web` / `sso-web` 都标「已激活」。
  本课只锁定两句产品差：**(1) 三个包存在，且能被 workspace 递归构建；(2) 只有 admin-web 组装了全部业务 web-domain 包。** 不要把 (2) 偷换成「所以另外两个不是 App」，也不要把 (1) 偷换成「所以三个都已对外发布」。`sso-web` 源码与 build **存在**。它等不等于「产品发布面」，本课保持 unresolved。本机未验证生产部署。

- **变式 B：口诀缺边。** 只背「App → web-domain → domain → platform」会把 `composeAppRuntime` 画成必须经过 domain。工作树允许 App 直连 platform。只把 adapter 画在 platform「下面」、却不说谁 import 谁，会让人以为 App 该依赖 `platform-http`。

- **变式 C：瘦 App 仍是 App。** sso-web 依赖只有 Vue。home-web 只有身份 + self 档案。它们不是「没做完的 admin-web」。

- **变式 D：注入不是反向。** 看到 web-domain 渲染了 FileUpload / Editor，先找 App 传入的 runtime 字段，再怀疑依赖方向。

- **迁移：** 新增可复用页面：先找对应后端模块名 → `packages/domains/<同名>` → `packages/web-domains/<同名>` → 在**目标 App** 的 `application/services.ts` 与 manifest registry 里接线。admin-web 要接业务房间时走它的接线板，并接受「包数 / domain 选择 / manifest 选择」可以不是同一个数字；home-web 只接线它选中的子集，档案用 self 工厂；不要先在 App `views/` 长出可复用 CRUD。新增终端时复制的是「厅堂 + 选择 + 自己的 platform/adapter 接线」，不是 `admin-web/src/api`。

## 常见误区

1. **「工作区里能 build 的 App = 已经对外发布。」** 构建脚本存在只证明包是真的，不证明生产部署，也不自动统一文档里的「发布面」一词。
2. **「sso-web 依赖少，所以不是 App。」** 它仍是 `apps/` 下的终端。认人厅不是管理控制台，瘦是故意的。
3. **「web-domain 可以保存登录 token 全局单例。」** 会话属于 App（`Admin-Token` / `Home-Token` / SSO cookie）。web-domain 不拥有 App 全局单例。
4. **「domain 可以对齐后端模块，所以可以 import Java 名字。」** 对齐的是模块名与 HTTP 资源，传输的是 JSON。Java 类型过不了这条河。
5. **「三个 App 既然都在，就可以互相 import 省事。」** App 互引会把 Client 和会话拧成一根绳子。
6. **「Skill 写唯一 admin-web，我就当另外两个是占位。」** 这是把 C-001 吞掉。先看磁盘，再把过期摘要记成来源问题。
7. **「`api-contracts` 就是 domain 模型。」** 那是生成的运输箱。手改 `generated/openapi.ts` 会在下次生成时被盖掉（L-005）。
8. **「platform 只出现在 domain 下面，App 不能碰。」** App → platform 是一等边。漏的是口号，不是门禁。
9. **「厅堂用了 HTTP，所以 App 该依赖 `platform-http`。」** 消费者是 adapter。
10. **「home 的 profile 就是 admin 档案页少几列。」** 工厂不同：`createProfileSelfWebDomain` ≠ `createProfileWebDomain`。
11. **「`views/monitor/external` 还在 App 里，说明架构没做完。」** iframe 运维入口是厅堂所有权，不是待搬 CRUD。
12. **「组装全部 = 正好八个 manifest。」** 包依赖、domain id、manifest id 是三套计数。本课不把它们捏成一个数字。

## 非评分暂停

打开磁盘，不要凭记忆。不要改数字去对齐文档。

1. 数 `frontend/apps/` 下有几个带 `package.json` 的目录，并确认每个都有 `src/` 和 `"build"` 脚本。
2. 打开 `admin-web/package.json` 与 `home-web/package.json`：数 `@namewta/web-domain-*`；确认有没有 `@namewta/platform-http`；home 有没有 `@namewta/web-kit-*`。
3. 打开两个 `*ManifestRegistry.ts`：认出 `composeAppRuntime`；认出 home 的 `createProfileSelfWebDomain` 对 admin 的 `createProfileWebDomain`。不要抄十条 id 当本课作业。
4. 打开 `sso-web/src/main.ts` 与 `ssoApi.ts`：三条路由是否都进 `AuthorizePage`；HTTP 是否是 `fetch` + `credentials: 'include'`。
5. 并排打开 `00-project-profile.md`「唯一可构建」、`01-module-map.md` 的 plus-ui 入口、`frontend/README.md`、`frontend/apps/README.md`、根 README 终端表。把说法留下来：工作树包数 / Skill 唯一论 / 前端 README 激活数 / 根 README 激活数。若对不上，留下冲突。

## 总结、词汇表与下一步

- 方向：主链 App → web-domain → domain → platform；**一等边 App → platform（ports）**；adapter 实现 ports，`platform-http` 的消费者是 adapter；App 也可依赖 adapter / web-kit。App 互引禁止。web-domain → App 禁止（注入除外）。
- 工作树三个包：`admin-web` 组装业务 web-domain（并选多个 domain/manifest id）；`home-web` 身份 + self 档案，`createProfileSelfWebDomain` ≠ `createProfileWebDomain`；`sso-web` 认人厅，`ssoApi.ts`，换票在厅堂。
- 冲突 C-001 保持张开：三个包可被 workspace 构建 ≠ 文档已统一发布面 ≠ 只有 admin-web 才算 App。唯一能确定的产品差是：只有 admin-web 组装了全部业务 web-domain 包。
- 不要复印 `src/api`。可复用规则下沉；终端壳、host 包装器留在 App。

词汇表：App / web-domain / domain / platform port / adapter / web-kit / composition / transport / host injection。下一步 L-005 看跨过前后端那条河时，什么东西算合同（字段、路径、SQL、OpenAPI），谁先改。L-007 再打开 admin-web 接线板逐个工厂；L-020 再面对 manifest 名单。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-001 | `README.md` | 三终端表标为已激活；apps + packages 分层；不复制 `src/api` | 「终端」「架构一览」 | 2026-09-16 |
| S-002 | `docs/namewta-enhancements.md` | 分层职责；发布面写 admin-web + home-web；未激活终端不参与构建 | 「多 App 领域化」 | 2026-09-16 |
| S-003 | `.agents/skills/engineering-standards/references/project/00-project-profile.md` | 写「唯一可构建和部署的浏览器 App 是 admin-web；其他仅 README 占位」（C-001） | 事实来源列表 | 2026-09-16 |
| S-004 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | 口号 App → web-domain → domain → platform 与 App → adapter/web-kit；漏 App → platform；plus-ui 公开入口仍写 `apps/admin-web/src/main.ts` | 「依赖方向」；顶层 Scope `plus-ui` | 2026-09-16 |
| S-006 | `.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md` | ARCH-003：App 拥有 Client/布局/部署；domain 无界面；adapter 实现平台端口；禁止 App 互引 | ARCH-003 | 2026-09-16 |
| S-008 | `frontend/package.json`、`pnpm-workspace.yaml`、`frontend/README.md`、三个 App 的 `package.json` / `src` / `application/{session,http,services,sso}.ts` / `*ManifestRegistry.ts` / `ssoApi.ts`、`packages/adapters/axios-browser/package.json`、`frontend/tooling/architecture/src/index.mjs` | 三个可构建包；admin 八个 web-domain 包 + 直连 platform、无 `platform-http`；home 子集 + self 工厂 + `domain-system` 无页面 + 无 web-kit；sso 认人厅；`build:prod` 递归 build；allowlist 含 App → platform；adapter 消费 `platform-http`；前端 README 只写激活 admin+home | 工作树 | 2026-09-16 |
| S-009 | `frontend/apps/README.md` | 只点名 admin-web 与 home-web 可构建部署 | 「当前状态」 | 2026-09-16 |
