---
lesson_id: L-007
objective_ids: [OBJ-07]
claimed_cells: [A:admin-web.services, C:admin-web-composition]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: factory-walkthrough
    minutes: 9
  - segment: composition-joints
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-007, S-L007-01, S-L007-02, S-L007-03, S-L007-04, S-L007-05, S-L007-06]
---

# Lesson 007：admin-web 接线板——每个工厂插在哪

## 学完你能做什么

打开磁盘上的 `frontend/apps/admin-web/src/application/services.ts`，你能用手指点出：

1. 每一个 **`create*Service` 工厂**（以及同文件里的 OSS 客户端、通知目录）是谁造的、厅堂导出成什么名字。
2. **OSS** 和 **通知目录** 这两处不是「再 import 一个 domain」，而是 App 把已经造好的插头交叉接上。
3. **App 拥有组合（composition）**。在这份文件里，domain 包并不互相 import 来完成接线。

本课认格子：`A:admin-web.services`、`C:admin-web-composition`。

本课**不讲**每个 domain 的 HTTP 方法清单。`users.list`、`login`、`initUpload`、公告 publish 那些是后面的课（L-014 / L-019 / L-024 / L-029 / L-033 / L-039 / L-053 / L-065 / L-073 / L-077 / L-081）。今天只认：**谁把厨房的插头插进厅堂的插座板**。

核对用的权威文件只有这一份：`frontend/apps/admin-web/src/application/services.ts`。工厂名字以磁盘为准，不要发明 `createWorkflowService` 或 `createNotificationDirectory`。

## 先把宏观地图放在桌上

L-004 已经说过：厅堂（App）选菜，中央厨房（domain）只留一份。本课把厅堂里那块 **接线板** 摊开。

管理端要同时开很多房间：系统、登录、OSS、流程、档案、演示、通知、监控、AI、第三方。如果每个 Vue 页面自己 `new Axios`、自己去找用户、自己去传文件，厨房会互相抄近路，第二个 App 也没法选子集。

NAMEWTA 的规矩是：

```text
package.json          ← 编译期：这个 App 允许依赖哪些 domain / adapter 包
application/http.ts   ← 本厅堂的 HTTP 插头（axios adapter）
application/session.ts← 本厅堂的会话钥匙（Admin-Token）
application/services.ts  ← 本课：用 http/session 调用 create* 工厂，交叉接线
*ManifestRegistry.ts  ← 下一层：把造好的服务塞进 web-domain runtime（L-020 名单）
```

Skill 把这三处叫必须同步的编译期入口：依赖声明、service 创建、manifest 组合。只装包、不接线，或只接线、不注册页面，都是半成品。本课只把中间那一块认全。

**类比：** 把 `services.ts` 想成饭店总台后面的插座板。每间厨房送来自己的插头（`createSystemService` 这类工厂）。总台决定：插哪些、先插哪一个、哪两根线要交叉（OSS 网关、登录身份、富文本资产、通知选人）。厨房不自己把电线伸进隔壁厨房。

**类比失效处：**

- 插座板**不炒菜**。工厂返回的对象里面有 HTTP 方法，那些方法本课不教。
- 「厨房互不接线」**不是**「整个仓库 domain 包永远不能出现在另一个 domain 的 `package.json`」。架构门禁允许 `domain → domain`。工作树上 `domain-workflow` 会从 `@namewta/domain-system/user` 拿用户查询口。那是 workflow **工厂内部**自己再造 `users`，**不是**本文件把 `systemService` 传给 workflow。本课的「这里互不 import」只覆盖 `services.ts` 这一层胶水。
- 插座板也不是空的：App 自己还写了 `notificationDirectory` 和 `richTextAssets`。那是厅堂的接线，不是漏写的 domain。

## 核心概念与机制

### 直觉讲解

小孩子版只记四句：

1. **先有厅堂的电线，再有厨房的插头。** `domainHttp` 包着 `adminHttp.request`；`session` 是 `Admin-Token`。
2. **先插系统，再插要借用系统的东西。** OSS 网关来自 `systemService.resources.oss`；登录身份来自 `systemService.identity`；通知目录的用户类型来自 `systemService.userTypes.options()`。
3. **交叉线由厅堂来接。** demo 不认识 OSS adapter；notify 不认识 system 包；admin 登录包不认识 system 包。文件里把它们接上。
4. **造出来的服务是本厅堂的单例出口。** 页面和 registry 从 `@/application/services` 拿，不在 domain 里 `export const systemService = ...`。

再补一句防环：文件顶部把 `domainHttp.request` 写成箭头函数，注释写明要躲开「HTTP 恢复逻辑、Router 清单与服务组合」的初始化环。模块加载时只保存函数，**不立刻打电话**。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 组合 | composition | App 显式 import 工厂、注入 http/session/端口、导出本厅堂服务单例。主人是 `apps/admin-web`，不是 domain |
| 工厂 | factory / `create*Service` | domain 或 adapter 导出的创建函数。调用一次得到冻结的服务对象。本文件里的名字以磁盘为准 |
| 厅堂 HTTP | `domainHttp` | 对 `adminHttp.request` 的延迟包装，类型是 `Parameters<typeof adminHttp.request>[0]`。不导出 |
| 网关 | OSS upload gateway | `systemService.resources.oss`：票据/分片/完成/中止等 **system 资源口**。adapter 认这张口，不认 Vue |
| 直传客户端 | `createOssUploadClient` | `@namewta/adapter-oss-upload-browser` 的浏览器实现。**不是** `create*Service`，但是本接线板的一等成员 |
| 通知目录 | `notificationDirectory` | App 自己写的选人端口：`searchUsers` / `usersByIds` / `userTypes`。不是 notify 工厂的返回值 |
| 资产端口 | `RichTextAssetsPort` | demo 富文本要的 `upload` / `resolve`。由 App 用 OSS 客户端和 `/demo/rich-text/assets` 实现 |
| 身份口 | `identity` port | `systemService.identity` 的 `{ loadInfo, loadMenus }`，喂给 `createIdentityAccessService` |
| 组合点 | composition joint | 一处跨包胶水：OSS、身份、富文本资产、通知目录。不是 domain 内部的 CRUD 方法 |
| 初始化环 | initialization cycle | `http.ts` 恢复登录时碰到 router；registry 又 import services；services 再 import http。延迟调用打断环 |

### 机制/因果链

按文件从上到下读。顺序不是审美，是依赖。

1. **import 工厂，不要 import 邻居的实现。** domain 来自 `@namewta/domain-*`；OSS 客户端来自 `@namewta/adapter-oss-upload-browser`；`adminHttp` / `session` 来自本 App。`createMonitorService` 走子路径 `@namewta/domain-system/monitor`，和 `createSystemService` 不是同一个函数。
2. **做一根懒电线 `domainHttp`。** 对象字面量里的 `request` 等到真正发请求才碰 `adminHttp`。这样模块求值阶段不会跟 `http.ts` 里的 `requestRelogin` → `router`、以及 registry 再 import 本文件打成死结。
3. **先 `createSystemService(domainHttp)`、`createOpenApiService(domainHttp)`。** 系统服务是后面 OSS / 身份 / 通知用户类型的上游。OpenAPI 是同包的**另一个工厂**，不是 `systemService.openApi`。
4. **`createOssUploadClient`。** 注入 `clientId: import.meta.env.VITE_APP_CLIENT_ID`、`gateway: systemService.resources.oss`、`getToken: session.getToken`、以及可选的开发代理 `transfer`。adapter 拿票据、往对象存储传字节；票据口在 system，不在 adapter 里写死 URL。
5. **`createIdentityAccessService`。** 注入本厅堂 Client、是否加密登录体、`http`、`identity: systemService.identity`、`session`。`@namewta/domain-admin` 的 `package.json` **没有** `domain-system`；身份口是 App 塞进去的。
6. **只吃 http 的工厂：`createWorkflowDefinitionService`、`createProfileService`。** 导出名分别是 `workflowService`、`profileService`。不要把导出名误当成工厂名。
7. **厅堂先做 `richTextAssets`，再 `createDemoService(domainHttp, createRichTextService(domainHttp, richTextAssets))`。** 上传走 `ossUploadClient.upload`；解析走 `GET /demo/rich-text/assets`。demo 包不依赖 OSS adapter。
8. **`createNotificationService(domainHttp)` 只管通知业务 HTTP。选人是旁边的 `notificationDirectory`：** `searchUsers`、`usersByIds` 打 `/notify/recipients/*`；空关键字短路；`userTypes` 转调 `systemService.userTypes.options()`。notify 包不依赖 system 包。
9. **收尾三个只吃 http 的工厂：`createMonitorService`、`createAiService`、`createThirdService`。**
10. **消费者在厅堂别处。** `adminManifestRegistry.ts` 把这些单例放进 web-domain runtime（通知页还要 `directory: notificationDirectory`）。`FileUpload` / `ImageUpload` / `Editor` / `useDirectOssUpload` 拿 `ossUploadClient`。登录页拿 `identityAccessService`。胶水已经接完，页面不再跨 domain import。

因果：谁拥有交叉线，谁就能换厅堂。home-web 可以再调一次 `createSystemService` + `createIdentityAccessService` + `createProfileService`，不必把 admin 的插座板整块搬走。那是 L-008 的对照，本课只需要知道：**组合可以复制，单例不能跨 App 偷。**

## 磁盘上的工厂全表

下面这张表就是 `A:admin-web.services` 的函数目的。路径一律相对仓库根。工厂名必须能在 `services.ts` 里搜到。

| 磁盘工厂 | 来源包 | 本文件导出 | 注入了什么 | 本课只认到这里 |
| --- | --- | --- | --- | --- |
| `createSystemService` | `@namewta/domain-system` | `systemService` | `domainHttp` | 系统房间总插头；后面三处要借它的 `resources.oss` / `identity` / `userTypes` |
| `createOpenApiService` | `@namewta/domain-system` | `openApiService` | `domainHttp` | 同包第二工厂，管 OpenAPI 凭据/目录，不挂在 `systemService` 下面 |
| `createOssUploadClient` | `@namewta/adapter-oss-upload-browser` | `ossUploadClient` | `clientId`、**`gateway: systemService.resources.oss`**、`getToken`、可选 `transfer` | 浏览器直传客户端。网关是 system 资源口 |
| `createIdentityAccessService` | `@namewta/domain-admin` | `identityAccessService` | `client`、`encryptLoginRequest`、`http`、**`identity: systemService.identity`**、`session` | 登录/注册/菜单恢复的身份服务。身份口来自 system |
| `createWorkflowDefinitionService` | `@namewta/domain-workflow` | `workflowService` | `domainHttp` | 流程定义/任务等。本文件**没有**把 `systemService` 传进去 |
| `createProfileService` | `@namewta/domain-profile` | `profileService` | `domainHttp` | 个人/企业档案。管理端页面工厂是 L-039 / L-044 |
| `createRichTextService` | `@namewta/domain-demo` | （不单独导出） | `domainHttp` + **`richTextAssets`** | 富文本 CRUD + 资产端口 |
| `createDemoService` | `@namewta/domain-demo` | `demoService` | `domainHttp` + 上一项的返回值 | 演示表/树；富文本作为第二参 |
| `createNotificationService` | `@namewta/domain-notify` | `notificationService` | `domainHttp` | 公告/收件箱/配置/监控等通知 HTTP |
| （无工厂函数） | App 自己写 | `notificationDirectory` | `domainHttp` + **`systemService.userTypes.options`** | 组合点：`searchUsers` / `usersByIds` / `userTypes` |
| `createMonitorService` | `@namewta/domain-system/monitor` | `monitorService` | `domainHttp` | 缓存/登录日志/在线/操作日志。与 `createSystemService` 分开 |
| `createAiService` | `@namewta/domain-ai` | `aiService` | `domainHttp` | 当前用户注册到 SnailAI 的 domain 口 |
| `createThirdService` | `@namewta/domain-third` | `thirdService` | `domainHttp` | 供应商/端点/凭据/观测 |

本文件**没有** `createGenService`。`gen` 已删除，不得在接线板复活。

未导出、但属于组合机制的符号：`domainHttp`、`createDevelopmentOssTransfer`、`richTextAssets`。它们是电线，不是给页面用的插座。

## 四条交叉线（OSS / 身份 / 富文本 / 通知目录）

### 交叉 1：OSS 客户端的网关

```ts
export const ossUploadClient = createOssUploadClient({
  clientId: import.meta.env.VITE_APP_CLIENT_ID,
  gateway: systemService.resources.oss,
  getToken: session.getToken,
  transfer: createDevelopmentOssTransfer()
});
```

- **gateway** 必须是已经造好的 `systemService.resources.oss`。adapter 的 `OssUploadGateway` 要 `initUpload` / `signParts` / `completeUpload` / `abortUpload` 这些口；system 资源服务正好长这样。本课不把每个 OSS HTTP 走一遍（L-026 / L-027 / L-029）。
- **getToken** 读本厅堂 `Admin-Token`，不读 home / SSO 的钥匙。
- **transfer** 默认可以是 `undefined`。只有同时满足：`import.meta.env.DEV`、配置了非空 `VITE_APP_OSS_PROXY_PREFIX`、浏览器里有 `window`，才会把预签名 URL 改写到同源代理再 `transferToOss`。生产路径不走这根代理线。
- **失败边界：** `clientId` 空字符串时，adapter 在**构造期**抛 `OSS upload clientId is required`。这是接线失败，不是上传到一半失败。上传中止、complete 失败要 abort，是 L-027 / L-029 的数据流。

### 交叉 2：登录服务的身份口

```ts
export const identityAccessService = createIdentityAccessService({
  client: { clientId: import.meta.env.VITE_APP_CLIENT_ID },
  encryptLoginRequest: import.meta.env.VITE_APP_ENCRYPT === 'true',
  http: domainHttp,
  identity: systemService.identity,
  session
});
```

`domain-admin` 只声明：我需要一个能 `loadInfo()` / `loadMenus()` 的口。App 把 `systemService.identity` 塞进去。登录怎么打 `/auth/login`、会话怎么写入，留给 L-014。本课只认：**身份口的主人是接线板，不是 admin domain 去 import system。**

### 交叉 3：demo 富文本资产

`richTextAssets.upload`：按 kind 选 policy（附件用 `richtext-file`，其余 `richtext-${kind}`），调用 **已经造好的** `ossUploadClient.upload`，返回 `{ ossId, fileName }`。

`richTextAssets.resolve`：厅堂自己用 `domainHttp` 打 `GET /demo/rich-text/assets`；`response.data` 不是数组就当 `[]`。

然后：

```ts
export const demoService = createDemoService(domainHttp, createRichTextService(domainHttp, richTextAssets));
```

domain-demo 的工厂第二参是可选的。若厅堂不传，demo 内部会给一个「upload 直接抛 `Rich-text assets port is not configured`」的缺省口。admin-web **没有**走缺省口，它把 OSS 接上了。这是组合的正例：缺省关闭，厅堂显式打开。

### 交叉 4：通知目录

```ts
export const notificationDirectory = {
  searchUsers: (keyword, page = 1, pageSize = 20) =>
    keyword.trim()
      ? domainHttp.request({ url: '/notify/recipients/search', method: 'get', params: { ... } })
      : Promise.resolve({ data: { rows: [], total: 0 } }),
  usersByIds: (ids) =>
    domainHttp.request({ url: '/notify/recipients/by-ids', method: 'get', params: { userIds: ids.join(',') } }),
  userTypes: () => systemService.userTypes.options()
};
```

- `searchUsers` / `usersByIds` 是本课必须能指着说出的两个目录方法。它们打的是 notify 的收件人搜索，**不是** `createNotificationService` 返回对象上的方法。
- 关键字全空白：**不发 HTTP**，本地返回空页。这是组合层的短路，不是后端「查了零个人」。
- `userTypes` 是第三根交叉线：通知选人要用户类型选项时，厅堂去问 system，而不是让 `@namewta/domain-notify` 依赖 `@namewta/domain-system`。
- registry 把 `{ service: notificationService, directory: notificationDirectory }` 一起交给 `createNotifyWebDomain`。页面要选人找 directory，要发公告找 service。

## 图、表或文本图

**图题 / caption：** admin-web `services.ts` 组合。厅堂拥有插座板；虚线是本文件里的交叉线；实线是「只注入 http」。

```text
  admin-web application/
  ┌──────── session.ts (Admin-Token)
  │
  ├──────── http.ts  (adminHttp = axios adapter)
  │              ▲
  │              │ 延迟 request（防初始化环）
  │              │
  ▼              │
  services.ts  domainHttp ─────────────────────────────────┐
       │                                                   │
       │ 1. createSystemService ──► systemService          │
       │       ├─ resources.oss ──┐                        │
       │       ├─ identity ───────┼─┐                      │
       │       └─ userTypes ──────┼─┼─┐                    │
       │                          │ │ │                    │
       │ 2. createOpenApiService ─► openApiService         │
       │                          │ │ │                    │
       │ 3. createOssUploadClient │ │ │                    │
       │       gateway ◄──────────┘ │ │                    │
       │       getToken ◄── session │ │                    │
       │       transfer? DEV 代理   │ │                    │
       │              │             │ │                    │
       │              ▼             │ │                    │
       │         ossUploadClient    │ │                    │
       │              │             │ │                    │
       │ 4. createIdentityAccessService                    │
       │       identity ◄───────────┘ │                    │
       │       session / http / clientId                   │
       │              │               │                    │
       │              ▼               │                    │
       │       identityAccessService  │                    │
       │                              │                    │
       │ 5. createWorkflowDefinitionService ► workflowService
       │ 6. createProfileService ► profileService          │
       │                              │                    │
       │    richTextAssets.upload ────┘ (用 ossUploadClient)
       │    richTextAssets.resolve ── GET /demo/rich-text/assets
       │ 7. createDemoService(http, createRichTextService(http, assets))
       │       ► demoService                               │
       │                              │                    │
       │ 8. createNotificationService ► notificationService│
       │    notificationDirectory.searchUsers / usersByIds │
       │    notificationDirectory.userTypes ◄── userTypes ─┘
       │
       │ 9. createMonitorService ► monitorService
       │10. createAiService ► aiService
       │11. createThirdService ► thirdService
       │
       ▼
  本厅堂单例出口（页面 / FileUpload / ManifestRegistry 来取）
```

**文字等价物：** 图的上方是厅堂自己的会话和 HTTP。中间整块是 `services.ts`。先用懒包装 `domainHttp` 接到 `adminHttp`，避免模块加载时跟登录恢复、路由清单互相抢初始化。第一排造 `systemService` 和 `openApiService`。从 `systemService` 拉出三根虚线：OSS 网关进 `createOssUploadClient`，身份口进 `createIdentityAccessService`，用户类型进 `notificationDirectory.userTypes`。OSS 客户端再被富文本资产的 upload 使用。workflow / profile / monitor / ai / third 只接到 `domainHttp`，图上没有虚线。notify 分成两块：工厂产出 `notificationService`；App 另写 `notificationDirectory`，其中搜索和按 ID 取人走 notify HTTP，用户类型走 system。所有出口都停在 App 单例，不画 domain 互相 import。

**图的边界：** 不画出每个工厂内部的 HTTP 路径。不画出 `composeAppRuntime` 的 manifest id 名单（L-020）。不画出 home-web 的插座板（L-008 只有三枚插头）。不保证 `VITE_APP_OSS_PROXY_PREFIX` 在每台开发机都有值——没值时 `transfer` 就是 `undefined`。图不表示 `domain-workflow` 的 `package.json` 没有 `domain-system`；那条包依赖发生在 workflow 工厂内部的 `createUserQueryPort`，不画进本接线板的虚线。

## 正例、反例与边界

**正例 1：** 文件顶部按包 import `createSystemService`、`createOpenApiService`、`createOssUploadClient`、`createIdentityAccessService`、`createWorkflowDefinitionService`、`createProfileService`、`createDemoService`、`createRichTextService`、`createNotificationService`、`createMonitorService`、`createAiService`、`createThirdService`。这些名字都能在磁盘上找到。导出时 `workflowService = createWorkflowDefinitionService(...)`，工厂名和变量名允许不一样，但工厂名不许改写。

**正例 2：** OSS 组合点写的是 `gateway: systemService.resources.oss`，不是 `gateway: systemService`，也不是在 demo 里 import adapter。

**正例 3：** 空搜索关键字走本地空页。这是目录组合自己的失败/空输入边界，不假装后端返回了零行查询。

**正例 4：** `adminManifestRegistry.ts` import 本文件的单例，把 `notificationDirectory` 交给 notify web-domain。胶水仍在 App。

**正例 5：** `FileUpload/index.vue` 从 `@/application/services` 拿 `ossUploadClient`，再注入 workflow runtime 的 `fileUpload`。这是厅堂包装 web-kit，不是 workflow domain 去找 OSS。

**反例 1：** 在 `@namewta/domain-notify` 里 `import { createSystemService } from '@namewta/domain-system'`，让通知自己查用户类型。那是把交叉线藏进厨房。本仓库 notify 包没有这条依赖。

**反例 2：** 在 `@namewta/domain-demo` 里直接调 `createOssUploadClient`。demo 只该认 `RichTextAssetsPort`。

**反例 3：** 在 `@namewta/domain-admin` 里 import `domain-system` 去拉菜单。登录包要的是身份口，由 App 注入。

**反例 4：** 发明 `createWorkflowService`、`createOssService`（指本文件）、`createNotificationDirectory`。磁盘上没有这些工厂。

**反例 5：** 把 `createOpenApiService` 画成 `systemService.openApi`，或把 `createMonitorService` 画成 `systemService.monitor`。它们是并列工厂，monitor 还走子路径导出。

**反例 6：** 在 `admin-web/src/api/*.ts` 再包一层 axios。L-004 已经禁止把中央厨房搬回厅堂；本课再加一条：新接线只进 `services.ts`。

**反例 7：** home-web `import { systemService } from '../../admin-web/src/application/services'`。App 互引会串 `Admin-Token`。home 必须自己再 `create*` 一次（L-008）。

**边界：**

- 本文件可以出现 **App 自己的小对象**（`notificationDirectory`、`richTextAssets`）。那仍是组合，不是「业务规则漏在 App」。规则（怎么发公告、怎么校验档案）仍在 domain。
- `domainHttp` 不是新的 HTTP 栈，只是延迟调用。真出发仍是 `adminHttp`。
- workflow 包依赖 `domain-system/user` 不推翻「本文件互不 import」。两句话要并排，不要合成假和平。
- 架构 allowlist 里 `domain → domain` 是合法层边；Skill 原文是「领域之间默认不直接依赖」。默认 ≠ 门禁禁止。本课以 `services.ts` 的胶水为准，把 workflow 的包依赖记成例外，不在本课改门禁。

## 变式与迁移

- **变式 A：只吃 http 的插头。** `createProfileService(domainHttp)` 这类一行工厂，看起来像「没有组合」。它们仍然是组合：http 是厅堂的，服务单例是厅堂的。交叉线为零，不等于可以挪到 domain 包根去 `export const profileService`。

- **变式 B：开发 OSS 代理。** `createDevelopmentOssTransfer` 只改写 URL 的 origin/pathname，把预签名请求送到 Vite 代理前缀。失效处：它不是鉴权网关，也不是生产 CDN。缺前缀、非 DEV、无 `window` → 返回 `undefined` → adapter 走默认 `transferToOss`。不要把这根线教成「所有上传都进代理」。

- **变式 C：home-web 子集（指向 L-008，不在本课展开）。** `frontend/apps/home-web/src/application/services.ts` 只有 `createSystemService`、`createIdentityAccessService`、`createProfileService`。没有 OSS 客户端，没有 `notificationDirectory`，没有 demo/ai/third/workflow/monitor/openApi。同一套工厂，另一块插座板，另一把 `Home-Token`。

- **变式 D：缺省富文本口。** `createDemoService(http)` 不传第二参时，upload 会抛「端口未配置」。admin-web 显式传入 `createRichTextService`。迁移新终端时：要么接 OSS，要么接受缺省关闭，不要静默 no-op。

- **迁移：新增一个业务 domain 到管理端。** 顺序是硬的：① 后端模块存在；② `packages/domains/<name>` 导出 `createXService(http)`（需要跨域口就做成端口参数，不要 import 邻居）；③ `admin-web/package.json` 声明依赖；④ 在 **本文件** 调用工厂并 `export`；若要借用 system/OSS/选人，在本文件交叉，不在 domain 里偷；⑤ registry 把服务放进 web-domain runtime。三处缺一处就是半接线。不要先在 `views/` 写可复用 CRUD。

- **迁移：不要把本课的方法表提前背完。** 你今天能指着工厂说出组合点，就已经满足 OBJ-07。下一步 L-008 对照另外两个 App；L-009 才把 platform 端口目录摊开。

## 常见误区

1. **「`services.ts` 就是一份 API 列表。」** 它是接线板。方法清单在各 domain 课。
2. **「`workflowService` 所以工厂叫 `createWorkflowService`。」** 磁盘工厂是 `createWorkflowDefinitionService`。
3. **「OpenAPI / 监控是 systemService 的字段。」** 它们是 `createOpenApiService`、`createMonitorService`。
4. **「OSS 上传属于 demo，因为富文本在用。」** 客户端在 adapter；网关在 system；demo 只收资产端口。
5. **「`notificationDirectory` 是 `createNotificationService` 返回的。」** 返回值是 `notificationService`。目录是 App 对象。
6. **「空关键字还是会打 `/notify/recipients/search`。」** `keyword.trim()` 为空时本地空页。
7. **「domain 之间这里也会互相 import，反正门禁允许 domain→domain。」** **这里**（本文件）没有。允许 ≠ 本接线板在用。
8. **「把 `adminHttp` 直接传进工厂更短，wrapper 是多余的。」** 注释写明要防初始化环。wrapper 的价值是延迟读取，不是换一套 HTTP。
9. **「组合完成 = 页面能用。」** 还要 registry / 菜单组件键（L-020）。本课只保证服务单例存在。
10. **「在 domain 里 `export const xxxService = createXxxService(defaultHttp)` 省掉 App。」** 那会把 Client、会话、OSS 代理、加密开关冻死在厨房。第二个 App 无法选子集。
11. **「`createOssUploadClient` 也是 `create*Service`。」** 命名像，分层不是。它是 adapter 工厂，gateway 仍来自 system。
12. **「Skill 写未激活终端只留 README，所以不必给每个 App 一块插座板。」** 工作树里 home-web 已有自己的 `services.ts`。文档冲突以工作树为准（L-004 的 C-001），本课不把 home 的接线板当成占位。

## 非评分暂停

打开磁盘，不要凭记忆默写 HTTP。不要改文件。

1. 打开 `frontend/apps/admin-web/src/application/services.ts`。用手指按表点完全部磁盘工厂：`createSystemService`、`createOpenApiService`、`createOssUploadClient`、`createIdentityAccessService`、`createWorkflowDefinitionService`、`createProfileService`、`createDemoService`、`createRichTextService`、`createNotificationService`、`createMonitorService`、`createAiService`、`createThirdService`。
2. 点 OSS 组合点：`gateway: systemService.resources.oss`。再点 `createDevelopmentOssTransfer` 的三个门闩：`DEV`、代理前缀、`window`。
3. 点身份组合点：`identity: systemService.identity` 和 `session`。
4. 点通知目录：`searchUsers`、`usersByIds`、空关键字短路、`userTypes` 转调 system。
5. 点 `demoService` 那一行，确认第二参是 `createRichTextService(domainHttp, richTextAssets)`，而 `richTextAssets.upload` 调用的是 `ossUploadClient`。
6. 看文件有没有从 `@namewta/domain-notify` import `createSystemService`，有没有从 `@namewta/domain-demo` import OSS adapter。应该没有。

## 总结、词汇表与下一步

- App 拥有组合。权威接线板是 `admin-web/src/application/services.ts`。
- 磁盘工厂要能叫出全名；导出名可以不同（`workflowService` ← `createWorkflowDefinitionService`）。
- 四条交叉线：OSS 网关、登录身份、富文本资产、通知目录（`searchUsers` / `usersByIds`，外加 `userTypes` 借 system）。
- 在这份文件里 domain 不互相 import 来完成接线。workflow 对 `domain-system/user` 的包依赖是另一层事实，不要吞掉，也不要画进本板虚线。
- 不在本课背各 domain HTTP。下一步 L-008 对照 home-web / sso-web；L-009 看 platform 端口方向。

词汇表：composition / factory / gateway / notificationDirectory / RichTextAssetsPort / identity port / initialization cycle / App-owned singleton。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/admin-web`（父 Change 来源表） | admin-web 组装全部业务 domain；本课接线板位于 App | `application/services.ts` | 2026-09-16 |
| S-006 | `frontend/packages/{domains,adapters}` | 工厂来自 domain / OSS adapter，不来自 web-domain | 各包 `create*` 导出 | 2026-09-16 |
| S-007 | `.agents/skills/engineering-standards/references/project/01-module-map.md` 与 frontend architecture | App 显式组合；领域默认不直连；三处编译期入口 | 「依赖方向」「App 显式组合」 | 2026-09-16 |
| S-L007-01 | `frontend/apps/admin-web/src/application/services.ts` | 全部工厂调用、OSS gateway、identity、richTextAssets、notificationDirectory、domainHttp 延迟包装 | 整文件 | 2026-09-16 |
| S-L007-02 | `frontend/apps/admin-web/src/application/{http,session}.ts` | `adminHttp`、`Admin-Token`、http 引用 router 构成环的一端 | `export const adminHttp`；`createBrowserSessionStore({ key: 'Admin-Token' })` | 2026-09-16 |
| S-L007-03 | `frontend/apps/admin-web/src/router/adminManifestRegistry.ts` | 消费本课单例；`directory: notificationDirectory` | import 列表；`createNotifyWebDomain({ service, directory })` | 2026-09-16 |
| S-L007-04 | `frontend/packages/domains/{admin,demo,notify,system,workflow}/package.json` 与 admin `IdentityAccessServiceOptions` | admin/demo/notify 不依赖邻居 domain；workflow 依赖 `domain-system`；身份口是注入端口 | 各 `dependencies`；`createIdentityAccessService` 参数 | 2026-09-16 |
| S-L007-05 | `frontend/packages/adapters/oss-upload-browser/src/{client,types}.ts`；`domains/system/src/resource-service.ts` | `createOssUploadClient` 要 `gateway: OssUploadGateway`；`systemService.resources.oss` 提供同名口；空 clientId 构造失败 | `OssUploadGateway`；`createOssService` | 2026-09-16 |
| S-L007-06 | `frontend/apps/home-web/src/application/services.ts`；`frontend/tooling/architecture/src/index.mjs` | home 子集接线；allowlist 允许 `domain → domain` | home 三个工厂；`runtimeLayerAllowlist.domain` | 2026-09-16 |
