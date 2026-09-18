---
lesson_id: L-081
objective_ids: [OBJ-81]
claimed_cells:
  - A:createAiService
  - A:createAiWebDomain
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: kitchen-http
    minutes: 8
  - segment: factory-and-session
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-008, S-010, S-L007-01, S-L024-01, S-L081-01, S-L081-02, S-L081-03, S-L081-04, S-L081-05, S-L081-06, S-L081-07, S-L081-08]
---

# Lesson 081：宏观一键聊天厅——`createAiService` 怎样印登记牌，`createAiWebDomain` 怎样把玻璃窗贴到墙上

## 学完你能做什么

打开厨房 `frontend/packages/domains/ai/src/index.ts` 的 `createAiService`，再打开菜单工厂 `frontend/packages/web-domains/ai/src/index.ts` 的 `createAiWebDomain`，你能**口述浏览器这一头怎样把一张登记牌打到 `POST /snail-ai/user/register`，再把一扇同域玻璃贴到管理端墙上**：墙上只有 `ai/chat/index`。不是 Java `SnailAiController.registerCurrentUser` 怎么拿登录人（OBJ-80），不是系统监控那四块屏（L-024），也不是「AI 控制台」那扇运维 iframe（`monitor/snailai/index`）。

口试名单就是矩阵 **(a)** 这两格，符号以**磁盘**为准：

1. **`A:createAiService`**（包 `@namewta/domain-ai`，实现在根 `src/index.ts`，**不是** `transport.ts`）：工厂只认 `HttpClient`，返回 `Object.freeze` 的 `AiService`。封面是 **1 支扁平 HTTP 枪**：`registerCurrentSnailUser()`。这一枪内部 `http.request({ url: '/snail-ai/user/register', method: 'post', headers: { repeatSubmit: false } })`。有 `response.data` 才 `projectAiUserTransport`。**不**自己画 Vue，**不**读 `Admin-Token`，**没有** `chat` / `stream` / `snail-chat` 方法，**没有** `registerCurrentUser` 这个 Java 名。
2. **`A:createAiWebDomain`**（包 `@namewta/web-domain-ai`）：工厂先 `requireAiWebRuntime`，缺托盘或缺四口当场 throw；再冻住 `WebDomainManifest`。id 是 `web-domain-ai`，`domainId` 是 `'ai'`。磁盘上正好 **1** 条 `componentKey`（测试锁死）：`ai/chat/index`。`permissions` 是空数组。它**不** `addRoute`，**不**读 `Admin-Token`，**不**自己拼 `/snail-ai/user/register`。聊天地址是会话对象拼的 `/snail-chat/`，要先 `probeFrame` 成功才写进 iframe。

OBJ-81 还要你能把「墙上这一行」和「厨房那一枪」对上，并说出四处故意错位：

- 厅堂单例导出名是 `aiService`，工厂名仍是 `createAiService`。Java 方法名是 `registerCurrentUser`。不要发明 `createAiChatService`，也不要把厨房方法改名叫 Java 名。
- 厨房只打 `POST /snail-ai/user/register`。iframe 打的是 `{basePath}/snail-chat/?openId=&trustedCredential=`。这两条河都叫 snail，货不一样。
- 「AI控制台」种子是 `monitor/snailai/index`，挂在「系统监控」下，权限字 `monitor:snailai:list`。那是 `admin-external-monitor` 的键，**不是**本课这一键。
- 菜单工厂 `permissions: []`，种子 C 型 `perms` 也是空串。E2E 用 `permissions: []` 仍能打开聊天。不要把「没有权限字」说成「没登录也能进」。登记枪仍走 `Admin-Token`。

2026-09-17 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `createAiChatService` / `createSnailChatService` | **没有。** 一座 `createAiService`，一枪登记 |
| 工厂写在 `transport.ts` | **没有。** 工厂、类型、名片全在 `src/index.ts`。`transport.ts` 只投影 `OpenApiUserVO` → `SnailOpenApiUser` |
| `@namewta/domain-ai/snail-ai` 会得到更小的聊天厨房 | **同一只手。** 子路径再 export 工厂 + `aiSnailResource` 标签。HTTP 仍在根工厂 |
| `aiService.chat.send` / `service.users.register` | **没有嵌套抽屉。** 方法名就是 `registerCurrentSnailUser` |
| 页面 `import { aiService } from '@/application/services'` | **这一张 web-domain 页不。** 厅堂把单例塞进 `runtime.service` |
| `createAiWebDomain(undefined)` 像 notify 那样仍能冻出键 | **会 throw。** `requireAiWebRuntime`：`'AiWebRuntime is required'`。对照 L-054 / L-066；比 L-073 / L-077 **更严**：四口都要是函数 |
| home / sso 对称一份 | **没有。** 两厅搜不到这两个工厂，也搜不到 `aiService` |
| 厨房有 `GET /snail-chat/` | **没有这支枪。** 页走 `createAiChatSession` + `runtime.probeFrame` |
| `ai/model/index` 是第二键 | **没有。** registry 测试锁成 `undefined` |
| README 写「流式 / 下载 / 导航端口」 | **过期。** 磁盘 `AiWebRuntime` 只有 `baseUrl` / `probeFrame` / `service` / `trustedCredential` |
| `@namewta/web-domain-ai` 依赖 axios | **没有。** 探路是宿主 `fetch`；登记是厨房经 `domainHttp` |

本课**不宣称**你会拆 `SnailAiController.registerCurrentUser` 的 LoginHelper / OpenApiUserClient（OBJ-80）、`createMonitorService.externalIntent` 四扇运维玻璃（L-024）、厅堂 `services.ts` 十二个工厂（L-007），或把九个 web-domain 工厂一行标 covered（GP-L-020）。今天只认：**浏览器这一头的 AI 厨房 URL 表（就一枪），一键菜单怎样把厨房喂进页面，会话怎样先登记再探路再贴玻璃，厨房标签上写着、方法表里没有、或墙上另一扇「AI」其实是监控厂的门。**

矩阵 (a) 那一行把九个工厂写在一起。本课只给 **`createAiWebDomain` 这一颗**当菜单证据，**不要**把整行九厂标成 covered。厨房独行只给 **`createAiService`**。

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`export const aiService = createAiService(domainHttp)`，只吃 http。L-008 对照过 home / sso **没有**这座厨房。L-020 讲导航 host 怎样用 `sys_menu.component` 对上 `registration.load`。L-024 是监控室四块屏加两把安全尺；运维 iframe 是厅堂另一份 `admin-external-monitor`。OBJ-80 是后端那一扇登记窗。本课站在**已经登录的管理员浏览器**这一头：口袋里是 `Admin-Token`，司机是 `adminHttp`。

四条河都叫 AI，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `createAiService` | 浏览器 AI 厨房：一枪登记当前用户 | **本课格子** |
| `aiService` | 厅堂单例，把 `domainHttp` 塞进工厂 | **接线**；组合点是 L-007 |
| `createAiWebDomain` | 管理端一键菜单 | **本课格子** |
| `createAiChatSession` | 页内会话状态机：校验 → 登记 → 探路 → 贴 iframe | **机制**；不是矩阵格子，口试要会走 |
| `SnailAiController.registerCurrentUser` | 后端 classic 窗 | OBJ-80；本课只对照 URL |
| `monitor/snailai/index` / `VITE_APP_SNAILAI_ADMIN` | 运维控制台 iframe | L-024 邻居；**不是**本课键 |
| snail-ai-server `context-path: /snail-ai` | 独立进程的控制台前缀 | **邻居**：本课 iframe 走 `/snail-chat/` |

2026-09-17 工作树：权威厨房是 `frontend/packages/domains/ai/`（根 facade `src/index.ts` + 投影 `src/transport.ts` + 资源子目录 `src/snail-ai/`）。权威菜单包是 `frontend/packages/web-domains/ai/`。厅堂接线是 `apps/admin-web/src/application/services.ts` 第 90 行与 `apps/admin-web/src/router/adminManifestRegistry.ts` 第 59–76、343、358、365–371 行。`selectedManifestIds` 含 `'web-domain-ai'`。home 的清单是 `web-domain-admin` + `web-domain-profile-self`，**不**选这份 id，也**没有** `ai` domain。

菜单种子在 `50-cde-base-dml.sql`：

- 一级 C 型「AI会话」`1761400000000000008`：`path=aichat`，`component=ai/chat/index`，`perms` 空串。这是本课这一键。
- 「系统监控」下 C 型「AI控制台」`1761400000000000121`：`component=monitor/snailai/index`，`perms=monitor:snailai:list`。这是运维玻璃，不是本课。

```text
已登录的管理员（浏览器，Admin-Token）
        │
        ├─ 一张菜单页（createAiWebDomain）
        │     runtime.service = aiService
        │     runtime.baseUrl / probeFrame / trustedCredential
        │     createAiChatSession 先登记再探路再贴 iframe
        │
        ├─ 运维旁路（不是本工厂的键）
        │     monitor/snailai/index → VITE_APP_SNAILAI_ADMIN
        │
        x  没有 ai/model/index
        x  没有 home / sso 插座
                        │
                        v
              ┌──────────────────────────────────────────────┐
              │  createAiService(domainHttp)                 │  ← 本课厨房
              │    registerCurrentSnailUser  1 枪            │
              │    有 data 才 projectAiUserTransport         │
              └──────────────────────────────────────────────┘
                        │
                        v
              厅堂 domainHttp → adminHttp.request
              （Bearer 登录票；clientid；repeatSubmit 闸关掉）
                        │
        POST /snail-ai/user/register     ← 厨房唯一的枪
        GET  {base}/snail-chat/?openId=&trustedCredential=
                                         ← 会话 + 宿主 probe/iframe
        （厨房不打 /snail-chat/
          厨房不打 /api/snail/chat/**
          厨房不打监控控制台 URL）
```

往下走不要跳层：

```text
Vue 聊天页（只收 runtime）
    └─ web-domain 外包一层 h(page, { runtime })
          └─ createAiChatSession
                ├─ runtime.service.registerCurrentSnailUser
                │     └─ domain 工厂拼 URL / method / 投影
                │           └─ App 的 domainHttp（axios + Admin-Token）
                │                 └─ 后端 SnailAiController
                └─ runtime.probeFrame + iframe src
                      └─ 宿主 fetch / 浏览器加载 /snail-chat/
```

**类比：** 把厨房想成门房手里**唯一一张登记牌**。管理员走到「AI会话」这扇玻璃窗前，服务员不自己写信封。登记牌换来一个 `openId`（外号）。玻璃后面是另一栋叫 snail-chat 的小屋。贴地址之前，先派探子（`probeFrame`）去敲同域的门：必须是成功的 HTML。探子回来，才把带着登录票的地址写到 iframe 上。探子失败，地址不许出现在页面 HTML 里——票还在口袋，不往玻璃上贴。

**类比失效边界：**

1. 「宏观一键」**不**等于「Snail AI 整栋楼只有一扇窗」。后端还有独立 snail-ai-server、gRPC、控制台。墙上没按钮 ≠ 那些进程不存在。
2. 类比**不**等于「home 选了 `web-domain-ai` 就会少一页」——home 根本不选这份 id，连厨房单例都没造。
3. 类比**不**等于「登记牌就是聊天」。聊天发生在 iframe 里面，本课厨房**没有**发消息的枪。
4. 类比更**不**等于「点开聊天就是 `SnailAiController` 的内部实现」——浏览器只保证打到哪扇窗。LoginHelper、OpenApiUserClient、status==1，是 OBJ-80 的后窗。
5. 「AI控制台」**不是**登记牌的第二联。那是监控厂的运维玻璃，权限字都不是空的。
6. README 说的「流式交互端口」**不是**这张托盘上的杯子。2026-09-17 托盘只有四口。

## 核心概念与机制

### 直觉讲解

小孩子版先记十四句：

1. **先找一座工厂，再找一键。** 厨房 `createAiService(http)`。菜单 `createAiWebDomain(runtime)`。封面 id 永远是 `web-domain-ai`。
2. **工厂名有 Ai，货却只有登记。** freeze 对象根上就一支枪：`registerCurrentSnailUser`。
3. **`transport.ts` 不是工厂。** 它只把 generated `OpenApiUserVO` 投影成 `SnailOpenApiUser`：留下 `openId`（缺的填 `''`）、`externalId` / `nickname` / `created`，丢掉 `avatarUrl`。
4. **登记枪不带 body。** 厨房不传 nickname，不传 userId。后端自己从登录态取人。浏览器这一头只保证 POST 那条路径。
5. **`repeatSubmit: false` 是给 axios 看的闸。** 关掉 500 毫秒「请勿重复提交」。重试按钮才能连点。不是 Controller 的字段。
6. **路径不用 `encodeId`。** 没有路径段 id。不要把 demo / workflow 的编码故事套过来。
7. **菜单工厂先关门再冻一键。** `undefined` runtime 当场 throw。缺 `baseUrl` / `probeFrame` / `service.registerCurrentSnailUser` / `trustedCredential` 也 throw。notify 厂 2026-09-17 不会。
8. **页面只收 runtime。** `AiChatPage.vue` 不 import 厅堂单例。真正扣扳机的是 `createAiChatSession`。
9. **先探路，后贴玻璃。** 探路失败或 15 秒超时，`frameUrl` 保持空，登录票不进 DOM。
10. **权限组是空数组。** 种子 C 型也没有 `ai:chat:list`。藏按钮的指令在这页用不上。最终授权仍是登录态 + 后端窗。
11. **`messages` 只有两句。** `chatTitle: 'Snail AI'`，`retry: '重新加载'`。Vue 把这两句写死在模板里，不读 i18n 块。
12. **capabilities 不是 componentKey。** 名片一项 `embedded-chat` 对得上 registration `id`，决定挂页的仍是 manifest + 菜单种子 + `selectedManifestIds`。
13. **两扇叫 AI 的玻璃。** `ai/chat/index` 是本课。`monitor/snailai/index` 是运维。registry 两行都在，别指错。
14. **厨房抛原错误，会话换成稳定中文。** 测试锁了厨房 `rejects.toBe(failure)`。页面上你看不到那句敏感英文。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| AI 厨房工厂 | `createAiService` | `domains/ai/src/index.ts`；返回 `Object.freeze` |
| AI 厨房封面 | `AiService` | 同文件 `export interface`；1 方法 |
| 登记枪 | `registerCurrentSnailUser` | 无参；`Promise<AiApiResponse<SnailOpenApiUser>>` |
| Java 窗名 | `registerCurrentUser` | `SnailAiController`；**不是**厨房方法名 |
| 用户投影 | `projectAiUserTransport` | `src/transport.ts`；输入 `OpenApiSchema<'OpenApiUserVO'>` |
| 模块名片 | `aiDomainModule` | `id: 'ai'`，`backendModules: ['wta-ai']`，`capabilities: ['embedded-chat']` |
| 资源标签 | resource metadata | `aiSnailResource.controller = 'SnailAiController'`，`basePath: '/snail-ai'`；**不是** HTTP 发送 |
| 管理端 AI 菜单工厂 | `createAiWebDomain` | `web-domains/ai/src/index.ts`；manifest id `web-domain-ai` |
| 菜谱板 | `WebDomainManifest` | `id` / `domainId` / `messages` / `permissions` / `registrations` |
| 组件键 | `componentKey` | `ai/chat/index`；与 `sys_menu.component` 对表 |
| 运行时托盘 | `AiWebRuntime` | `src/runtime.ts`；`baseUrl` / `probeFrame` / `service` / `trustedCredential` |
| 失败关闭（本厂） | fail-closed runtime | `requireAiWebRuntime`；四口缺一当场 throw |
| 嵌入会话 | `createAiChatSession` | `web-domains/ai/src/chatSession.ts`；不是厨房方法 |
| 同域底座 | same-origin base path | `parseSameOriginBasePath`；只收以 `/` 开头、无 `//`、无 query/hash 的路径 |
| 探路 | `probeFrame` | 宿主 `fetch`；成功才允许 iframe 拿到带票 URL |
| 信任票 | `trustedCredential` | 厅堂 `getToken()`；写进 iframe query，**不**进厨房 body |
| 权限中性 | permission-neutral manifest | `permissions: []`；种子 perms 空串 |
| 重复提交闸 | `repeatSubmit: false` | axios 适配器客户端旗标 |

厨房与两条 HTTP 门牌（动词以磁盘为准）：

| 族 | 方法数 | 门牌 | 写动词 | 本课这一页用不用 |
| --- | --- | --- | --- | --- |
| 登记 | 1 | `POST /snail-ai/user/register` | POST，无 body | **用。** 会话 `load()` 扣 |
| 聊天文档 | 0 支厨房 HTTP | `GET {base}/snail-chat/?openId=&trustedCredential=` | 宿主 fetch + iframe | **用。** 不进 freeze 对象 |
| 运维控制台 | 0 | `VITE_APP_SNAILAI_ADMIN` | 监控厂 iframe | **不用本厂。** 另一键 |

`aiDomainModule.capabilities`：`embedded-chat`。这是名片，**不是** componentKey，也不决定 App 挂哪几页。

### 机制/因果链

**1. 一座扁平厨房，一枪，有 data 才投影。**

`createAiService(http)` 立刻 freeze。没有统一的 `call` 助手。这一枪自己写：

```text
http.request({
  url: '/snail-ai/user/register',
  method: 'post',
  headers: { repeatSubmit: false }
})
```

不剥信封、不加 `Cache-Control`、不加 `isEncrypt`。`method` 是小写 `'post'`，和其他 domain 工厂一样。

有 `response.data`：展开整份响应，把 `data` 换成 `projectAiUserTransport(response.data)`。没有 data：把响应**原样**当 `AiApiResponse<SnailOpenApiUser>` 交出去。测试锁的是「有 data 的 happy path」和「request reject 原样抛出」。缺 data 分支按源码口试，不要发明「一定投影」。

投影输出：

| generated `OpenApiUserVO` | `SnailOpenApiUser` |
| --- | --- |
| `openId` | `openId ?? ''` |
| `externalId` / `nickname` / `created` | 原样 |
| `avatarUrl` | **丢掉** |

`transport.test.ts` 用的夹具没有 `avatarUrl`，所以「投影前后相等」过了。口试要能指出：**夹具没这个字段，不等于投影会留下它。** 会话只用 `response.data?.openId`，丢掉头像目前碰不到聊天玻璃。

厨房失败：**不** log，**不**捏一个假用户。`index.test.ts` 第三例标题就是这句话。会话层再把它翻译成「加载 AI 聊天失败，请稍后重试」。

`repeatSubmit: false` 的因果：axios 适配器默认对 POST/PUT 做 500ms 同 URL 同 body 去重。登记枪没有 body，重试会打完全相同的请求。关掉闸，探路失败后立刻「重新加载」才不会被客户端挡成「数据正在处理，请勿重复提交」。这是厨房给会话重试留的门缝，不是后端幂等。

Authorization / `clientid` **不是**厨房写的。厅堂 `adminHttp` 拦截器加 `Bearer ${getToken()}` 和默认头 `clientid`。E2E 锁死：登记请求带 authorization，`clientid` 等于 `e5cd7e4891bf95d1d19206ce24a7b32e`。口试不要说「厨房自己塞了 Admin-Token」。

资源标签 `aiSnailResource`：`controller: 'SnailAiController'`，`basePath: '/snail-ai'`。它**不**发 HTTP。子路径 `@namewta/domain-ai/snail-ai` 把工厂再 export 一次，进口小厨房**不会**少一枪。

**2. 菜单工厂先核四口再冻一键。**

`createAiWebDomain(runtimeInput)`：`requireAiWebRuntime` 按顺序关门：

| 条件 | 抛出 |
| --- | --- |
| `runtime` 假值 | `'AiWebRuntime is required'` |
| `baseUrl` 不是函数 | `'AiWebRuntime.baseUrl is required'` |
| `probeFrame` 不是函数 | `'AiWebRuntime.probeFrame is required'` |
| 没有 `service` 或 `registerCurrentSnailUser` 不是函数 | `'AiWebRuntime.service is required'` |
| `trustedCredential` 不是函数 | `'AiWebRuntime.trustedCredential is required'` |

对照：

- notify / third（L-054 / L-066）：工厂**不**检查 runtime。
- demo / workflow（L-077 / L-073）：只检查对象在不在；`service: {} as never` 仍能冻出键。
- 本厂：四口都要像函数。`service: {}` **过不了**工厂门。

`index.test.ts` 第三例锁了前三句（required / baseUrl / probeFrame）。**没有**断言 service / trustedCredential 那两句。口试按源码四口，不要说「测试已经钉死五句 throw」。

冻住的菜谱板：

| 字段 | 磁盘事实 |
| --- | --- |
| `id` | `web-domain-ai` |
| `domainId` | `'ai'` |
| `messages` | namespace `ai`：`chatTitle` / `retry` |
| `permissions` | `[]`（测试原文：intentionally empty） |
| `registrations` | 一条：`id: 'embedded-chat'`，`componentKey: 'ai/chat/index'`，`componentName: 'AiChatPage'`，`load: () => loadAiChatPage(runtime)` |

`loadAiChatPage` 再 `requireAiWebRuntime` 一次，动态 `import('./snail-ai/AiChatPage.vue')`，外包 `defineComponent({ name: 'AiChatPage', setup: () => () => h(page, { runtime }) })`。页面 props 只有 `runtime`。

`composeAppRuntime` 只有 `selectedManifestIds` 含 `web-domain-ai` 时才把键交给导航。admin 厅堂这份 id 在 registry 第 371 行，`selectedDomainIds` 含 `'ai'`。home 不选。测试第二例：选中时 `componentKeys()` 是 `['ai/chat/index']`；空清单是 `[]`。

包 `exports`：根、`./pages`、`./snail-ai`。`pages.ts` 再 export Vue 默认。工厂**不**走 `pages.ts`，走 `loadAiChatPage`。从 `./pages` 进口组件，**不会**自动注入 runtime。

**3. 会话是这页的大脑：校验 → 登记 → 探路 → 贴玻璃。**

`AiChatPage.vue` 几乎没有业务：`reactive` 一份 `AiChatSnapshot`（`error` / `frameUrl` / `loading`），`createAiChatSession(runtime, snapshot => Object.assign(state, snapshot))`。`onMounted` 调 `load()`，`onBeforeUnmount` 调 `dispose()`。iframe `@load` 把 `src` 交给 `frameLoaded`。按钮「重新加载」再 `load()`。

`createAiChatSession` **再次** `requireAiWebRuntime`。然后 `load()` 按这个梯子走；任何一级失败都把 `frameUrl` 留空：

```text
loading=true, 清掉旧 frameUrl
    │
    ├─ baseUrl() → parseSameOriginBasePath
    │     失败 / 抛错 → 「AI 服务地址不存在，请联系管理员」
    │     （登记枪此时还没扣）
    ├─ trustedCredential()
    │     空串 / null / 抛错 → 「登录凭证不存在，请重新登录后再试」
    │     （登记枪仍没扣）
    ├─ runtime.service.registerCurrentSnailUser()
    │     throw → 「加载 AI 聊天失败，请稍后重试」
    │     没有非空 openId → 「获取 AI 用户身份失败」
    ├─ createFrameUrl(basePath, openId, credential)
    │     `{prefix}/snail-chat/?openId=&trustedCredential=`
    │     prefix 在底座是 `/` 时变成空，否则用去尾斜杠的路径
    ├─ probeFrame({ signal, url }) 与 15s 定时器赛跑
    │     失败 / 超时 → 「AI 聊天连接中断，请重新加载」
    │     （此时仍不写 frameUrl）
    └─ 探路成功才 replaceState({ frameUrl, loading: true })
          iframe @load 且 src 对得上 → loading=false
```

同域底座白名单（测试用这些当反例，登记枪一次都不扣）：

- `https://attacker.example/prod-api`
- `//attacker.example/prod-api`
- `/prod-api?target=other`
- `javascript:alert(1)`

解析器用假 origin `https://app.namewta.invalid`。候选必须 `trim` 后以单个 `/` 开头、不是 `//`、解析后 origin 仍是假 origin、没有 username/password/search/hash。尾斜杠剥掉；剥完变空就当成 `'/'`。开发底座 `/dev-api`、生产 `/prod-api` 都过。绝对 URL 不过。

探路成功之前，`snapshot.frameUrl` 一直是 `''`。E2E 第二例：探路 abort 时，整页 HTML **不含** `Admin-Token` 的字面值。口试句子：**带票的 URL 是探路成功之后才准出现的副作用，不是登记成功的副作用。**

15 秒：`frameLoadTimeoutMs = 15000`。定时器到点会 `probe.abort()`，把 attempt +1，写成连接中断。挂住的 probe 被 abort 后允许立刻重试。过期的登记成功回来，若 attempt 已经变了，**不许**用旧 openId 覆盖新玻璃。测试锁了「stale-user 不能盖住 current-user」。

`dispose()`：`disposed=true`，attempt +1，abort 探路，清 timer，快照回到三空。卸载页必须走这扇门，否则带票 URL 和未完成的 fetch 会漏到下一页。

**4. 厅堂托盘四口；探路是宿主 fetch，不是厨房。**

`adminAiWebRuntime`（registry 第 59–75 行）：

| runtime 口 | 厅堂接到哪 | 谁在用 |
| --- | --- | --- |
| `baseUrl` | `() => import.meta.env.VITE_APP_BASE_API` | 会话拼 `/dev-api/snail-chat/` 或 `/prod-api/snail-chat/` |
| `probeFrame` | `fetch(url, { credentials: 'same-origin', method: 'GET', redirect: 'error', signal })` | 会话；必须 `ok` 且 Content-Type 是 `text/html` 或 `application/xhtml+xml` |
| `service` | `aiService` | 会话 `registerCurrentSnailUser` |
| `trustedCredential` | `() => getToken() ?? null` | 会话；`getToken` 读 `Admin-Token` |

探路失败抛 `'AI chat probe failed'`。JSON 响应、503、非 HTML，都算失败。`redirect: 'error'` 不许跟着 302 走到别的源。未读且未锁的 body 会 `cancel()`，避免漏读的流占着连接；body 已用或 locked 则不 cancel。registry 测试锁了这些分支。

iframe `allow="clipboard-read; clipboard-write"`。这是聊天页自己的权限串，**不是** manifest `permissions`。

`security.excludes` 含 `/snail-chat/**` 与 `/api/snail/chat/**`。那是后端放行静态聊天文档，**不是**厨房第二枪。不要把 excludes 表背成本课方法表。

**5. 权限中性的一页；文案写死；运维玻璃是邻居。**

种子「AI会话」：一级菜单，C 型，可见，`perms` 空。manifest `permissions: []`。E2E 的 `getInfo` 给 `permissions: []` 仍加载 iframe。口试：**没有 `v-hasPermi`，没有 `ai:chat:*`。** 没登录仍过不了——缺票时会话停在「登录凭证不存在」，登记枪要 Bearer。

Vue 标题 `Snail AI`、按钮 `重新加载`、空态「正在加载 Snail AI」写在模板里。manifest messages 同文案，页**不**读它。不要把 messages 数成「i18n 已接线」。

「AI控制台」：父菜单「系统监控」，`component=monitor/snailai/index`，`monitor:snailai:list`。registry 把它编进 `admin-external-monitor`，`domainId: 'system'`，页是同一张 `views/monitor/external/index.vue`，`target: 'snail-ai'`，URL 来自 `VITE_APP_SNAILAI_ADMIN`，经 `monitorService.externalIntent`。要权限，要安全尺。**不要**说成 `createAiWebDomain` 的第二键。

`resolveAdminWebRegistration('ai/chat/index', 'ai')` → `AiChatPage`。`('ai/model/index', 'ai')` → `undefined`。`('monitor/snailai/index', 'system')` → `SnailAi`。三行都要能指。

**6. 厨房有枪没扳机；更外面还有根本没写进厨房的窗。**

2026-09-17 **有 Vue 扣**的厨房枪：就 `registerCurrentSnailUser`，扳机在会话 `load()`，不在模板字符串里。

厨房对象上**根本没有**：`chat`、`stream`、`send`、`listModels`、`snail-chat`、`registerCurrentUser`。

后端仍在、本课页面不经厨房打：

| 后端 / 静态窗 | 谁接 |
| --- | --- |
| `POST /snail-ai/user/register` | **厨房一枪** |
| `GET /snail-chat/**` | **会话 + probeFrame + iframe** |
| snail-ai-server 控制台 | **监控厂 iframe** |
| `/api/snail/chat/**` | 安全排除名单；本厂无方法 |

Controller 还挂着 `@ConditionalOnProperty(prefix = "snail-ai.open-api", name = "enabled", havingValue = "true")`。dev yml 里 `snail-ai.enabled` 是 **false**（客户端模式），`open-api.enabled` 是 **true**（这扇窗活着）。不要把「enabled: false」说成「登记窗关了」。那是 OBJ-80 的电闸细节，本课只认：浏览器打的是 open-api 那扇 HTTP 窗。

### 图、表或文本图

**图 1：宏观一键怎样吃到厨房**

```text
 sys_menu.component（管理端；种子在 50-cde-base-dml.sql）
   ai/chat/index                 ← 一级「AI会话」；perms 空
   monitor/snailai/index         ← 「系统监控 / AI控制台」；不是本厂
        │
        v
 GET /system/menu/getRouters
        │
        v
 App composeAppRuntime
   selectedManifestIds 含 web-domain-ai
        │
        ├─ 键在已选 manifest → registration.load
        │     h(AiChatPage, { runtime })
        └─ 键不在已选清单 → 解析失败关闭
                │
                v
         Page props.runtime
                │
                ├─ runtime.service = createAiService(domainHttp)
                ├─ runtime.baseUrl / trustedCredential / probeFrame
                └─ 页面只喊会话，不拼 request URL
                     （iframe 字符串是会话拼的，不是厨房）
```

**图题 / caption：** 宏观同一厨房、管理端一键、托盘把厨房和探路口一起塞进页面。alt：菜单键来自 50-cde-base-dml.sql；admin 选 web-domain-ai；页面只收到 runtime；登记字符串在 domain 工厂；聊天文档字符串在会话。

**文字等价物：** 人先碰到动态菜单里的 component 字符串。App 用已选 manifest 把字符串换成带 runtime 的 Vue 页。聊天页面向会话喊 `load()`；会话再向 `runtime.service` 喊登记，向宿主喊探路。厨房把方法换成 POST `/snail-ai/user/register`。home 即使以后有人把厨房单例造出来，墙上没有这一键，导航也不会挂页。不要按「名字里有 AI」，就把控制台键算进 `createAiWebDomain`。

**图 2：厨房有枪 / 本课页面扣扳机**

| 厨房方法 / 窗口 | HTTP（动词以磁盘为准） | 2026-09-17 谁扣扳机 |
| --- | --- | --- |
| `registerCurrentSnailUser` | `POST /snail-ai/user/register`，headers `repeatSubmit: false`，无 body | **会话 `load()`**；先过底座和票 |
| 聊天文档 | `GET {base}/snail-chat/?openId=&trustedCredential=` | **probeFrame + iframe**；厨房无方法 |
| `GET /page` 式的模型列表 | 无 | **厨房无、页无、registry `ai/model/index` undefined** |
| 运维控制台 | `VITE_APP_SNAILAI_ADMIN` | **`monitor/snailai/index`**；监控厂 |
| Java `registerCurrentUser` | 同一条 POST | 后窗 OBJ-80；浏览器不喊这个名字 |

**图题 / caption：** 工厂注册了一键 ≠ 所有 snail 路径都进了厨房 ≠ 名字带 AI 的菜单都是本厂。alt：登记走厨房；聊天文档走会话；控制台走监控厂；模型页不存在。

**文字等价物：** 管理员这一页消耗厨房一枪。玻璃上的聊天不是厨房第二枪。顶栏没有 AI 铃铛。不要把「工厂注册了一键」说成「所有 `/snail-*` HTTP 都有按钮」。

**图的边界：** 不画 Java LoginHelper / OpenApiUserClient / status==1（OBJ-80）。不画 snail-ai-server gRPC。不画监控四块屏的 cache/online（L-024）。不保证以后产品会补模型页或流式端口。不把 capabilities 名片画成菜单。不把 `messages` 画成已接线的 i18n。

## 正例、反例与边界

**正例 1 — 管理员打开 AI 会话。** 已登录。导航命中 `ai/chat/index`。会话 `load()`：底座 `/prod-api`（E2E）或 `/dev-api`（开发），票非空，登记枪 POST，回 `openId: 'browser-proof-user'`。探路 GET HTML 200。iframe `src` 路径 `/prod-api/snail-chat/`，query 有 `openId` 和 `trustedCredential`。loading 在 `@load` 后消失。

**正例 2 — 登记枪不带业务 body。** 厨房请求快照就是 `{ url, method: 'post', headers: { repeatSubmit: false } }`。nickname / externalId 不在浏览器信封里。

**正例 3 — 缺 runtime 当场关门。** `createAiWebDomain(undefined)` throw `'AiWebRuntime is required'`。缺 `baseUrl` 函数 throw `'AiWebRuntime.baseUrl is required'`。notify 工厂 2026-09-17 不会。

**正例 4 — 脏底座不扣登记枪。** `javascript:alert(1)`、协议相对、带 query 的路径，会话直接「AI 服务地址不存在」。`registerCurrentSnailUser` 调用次数 0，`probeFrame` 次数 0。

**正例 5 — 没票不登记。** `trustedCredential: () => null` → 「登录凭证不存在，请重新登录后再试」。登记枪 0 次。

**正例 6 — 登记失败换成稳定中文。** 厨房 reject 带敏感英文的 Error。页面快照是「加载 AI 聊天失败，请稍后重试」，`frameUrl` 空。再 `load()` 若只回 `{}`，变成「获取 AI 用户身份失败」。

**正例 7 — 探路失败票不进 DOM。** E2E 先 abort 探路。可见「AI 聊天连接中断，请重新加载」，iframe 数量 0，HTML 不含 token 字面值。点「重新加载」会再登记、再探路。

**正例 8 — 过期登记盖不住新玻璃。** 第一次登记慢，第二次已经用 `current-user` 贴上。第一次才回来 `stale-user`。快照里的 openId 仍是 `current-user`。attempt 计数就是这把锁。

**正例 9 — 权限空数组仍能挂页。** 测试：`manifest.permissions` 等于 `[]`。E2E：`getInfo.permissions` 等于 `[]`，聊天仍加载。种子 perms 空串对得上。

**正例 10 — 厅堂只吃 http。** `aiService = createAiService(domainHttp)`。没有第二参，没有 assets 口，没有 directory。

**反例 1 — 「这张 Vue `import { aiService }`。」** 全页搜不到 `@/application/services`。直接消费单例的是厅堂 registry。

**反例 2 — 「`createAiWebDomain` 等于九个 web-domain 工厂都 covered。」** 矩阵 (a) 把九个名字写在同一行。GP-L-020 已经挖过：不要整行盖章。本课只给 AI 这一颗。

**反例 3 — 「工厂在 `transport.ts`。」** 打开 `src/index.ts` 的 `createAiService`。`transport.ts` 只有 `projectAiUserTransport`。

**反例 4 — 「厨房方法叫 `registerCurrentUser`。」** 那是 Java。厨房是 `registerCurrentSnailUser`。厅堂导出是 `aiService`。

**反例 5 — 「`service.users.register` / `service.chat.send`。」** 扁平一枪，没有抽屉，没有 send。

**反例 6 — 「从 `@namewta/domain-ai/snail-ai` 进口小厨房。」** 子路径再 export 同一工厂，外加资源标签。HTTP 仍在根。

**反例 7 — 「页面手写 `/snail-ai/user/register`。」** 违反 L-004 方向。登记走 `runtime.service.registerCurrentSnailUser`。`/snail-chat/` 是会话拼的，也不是 Vue 模板字面量里的业务 API。

**反例 8 — 「iframe 就是厨房第二枪。」** freeze 对象上搜不到 `snail-chat`。探路是宿主 `fetch`。

**反例 9 — 「`ai/chat/index` 和 `monitor/snailai/index` 是同一厂的两键。」** 前者 `web-domain-ai` / `domainId: 'ai'`。后者 `admin-external-monitor` / `domainId: 'system'`。

**反例 10 — 「OBJ-81 包含 `SnailAiController`。」** 那是 OBJ-80。本课只对照那一扇窗被浏览器扣了没有。

**反例 11 — 「home 也能打开 AI 会话。」** 工作树没有这份工厂接线。

**反例 12 — 「包依赖了 `api-contracts` 所以 URL 是 `keyof paths`。」** 只有投影吃 `OpenApiSchema<'OpenApiUserVO'>`。登记 URL 是字符串字面量。

**反例 13 — 「README 写了流式端口所以 runtime 有 download/navigate。」** 四口以 `runtime.ts` 为准。

**反例 14 — 「`createAiWebDomain({ service: {} })` 能冻出键，缺方法等点击才爆。」** 本厂在工厂门就要求 `registerCurrentSnailUser` 是函数。那是 demo 的故事，不是 AI 的。

**反例 15 — 「投影会留下 avatarUrl。」** 丢掉。

**反例 16 — 「权限中性等于匿名可进。」** 没票停在凭证文案；登记仍要 Bearer。

**边界 1 — 工厂校验四口 ≠ 校验探路一定成功。** 函数在，底座仍可能是脏字符串。脏底座在会话层关门，不在工厂 throw。

**边界 2 — 厨房抛原错误；页面不展示原错误。** 口试两层都要说。不要把会话的中文说成厨房返回值。

**边界 3 — `openId ?? ''` 会把缺字段变成空串。** 会话把空串当成「获取 AI 用户身份失败」。投影「成功」和会话「失败」可以叠在同一响应上。

**边界 4 — 探路 Content-Type 只认两种 HTML。** `application/json` 即使 200 也失败。不要说「能 GET 到就行」。

**边界 5 — iframe `@load` 要 src 对得上当前 `frameUrl`。** 乱序的 load 事件会被 `matchesFrame` 丢掉。

**边界 6 — `messages` 与模板同文案 ≠ i18n 已接。** 改 manifest 不会改按钮字。

**边界 7 — 开发底座是 `/dev-api`，E2E / 生产测试夹具是 `/prod-api`。** 会话不写死 prod。口试说「`VITE_APP_BASE_API` + `/snail-chat/`」。

**边界 8 — `snail-ai.enabled: false` 关的是客户端模式，不是这扇 open-api 窗。** 浏览器打的窗看 `snail-ai.open-api.enabled`。

**边界 9 — 资源标签 `basePath: '/snail-ai'` 对得上登记，对不上 iframe。** iframe 前缀是宿主 baseUrl，路径是 `/snail-chat/`。

**边界 10 — `createAiChatSession` 不是 claimed cell。** 不把它写进矩阵 (a)。口试仍要会走，因为它是这一键怎么喊厨房的唯一路径。

## 变式与迁移

1. **和 L-007 对照。** 组合点课只要求你能指着「收尾三个只吃 http 的工厂之一：`createAiService` → `aiService`」。本课要求能把那一枪 URL 背到 method，并指出 iframe 不进厨房、控制台不是本厂。不要把 L-007 的接线板图当成已经 covered 的方法表。
2. **和 L-053 / L-065 / L-073 / L-077 对照。** 通知厨房嵌套抽屉。三方扁平十五枪。流程扁平六族。demo 表/树 PUT + 富文本 POST。AI 厨房：**一枪、无抽屉、无路径编码、无导出管。** 不要把「domain 工厂」说成同一种抽屉形状。
3. **和 L-054 / L-066 / L-073 / L-077 对照。** 都是「页面经 runtime 消费厨房」。差别：AI 厂 **会** `requireAiWebRuntime` 且核四口；notify/third 2026-09-17 不会；demo/workflow 只核对象在不在。AI 是一键一张 Vue；third 是四键一张页四个 kind。AI **有** iframe 第三条河，但拼法在会话，不在 `designUrl`。
4. **和 L-073 的设计器 iframe 对照。** 流程设计页地址来自宿主 `designUrl`（warm-flow-ui）。AI 聊天地址由会话用 `baseUrl + openId + credential` 拼，还多一步 HTML 探路。不要说「都是 iframe 所以都是宿主给死 URL」。
5. **和 L-024 / 运维玻璃对照。** 监控厂 `externalIntent` 要权限、要安全尺、URL 来自 `VITE_APP_SNAILAI_ADMIN`。本课玻璃要登录票、要登记、要同域 `/snail-chat/`。两扇都叫 Snail AI，钥匙不是一把。
6. **和 OBJ-80 对照。** 后端 XML 没有。Controller 无 `@SaCheckPermission`，从 LoginHelper 取 userId/nickname，调 OpenApiUserClient。本课不重讲。前端保证：哪一枪打哪条 URL、无 body、失败不捏用户、页面怎样把错误换成中文。
7. **和 L-020 对照。** 导航 host 仍是 `getRouters` → 解析 componentKey → `addRoute`。AI 不另写一套路由恢复。
8. **以后若要模型页。** 先让厨房长出方法，再让 `createAiWebDomain` 多冻一键，再补种子 component。只往 Vue 写 `ai/model/index` 会撞上今天 registry 的 `undefined`。不要先改种子。
9. **以后若要流式对话不走 iframe。** 那是新枪 + 新 runtime 口（今天没有 download/navigate/stream）。不要把 README 的愿望当成已经存在的端口。
10. **以后若要给聊天加 `ai:chat:list`。** 种子 perms、manifest `permissions`、页上 `v-hasPermi` 三处要一起改。只改一处会出现「工厂以为中性、种子以为要权」的裂缝。今天三处都空，是对齐的。
11. **以后若要瘦掉探路。** 会把带票 URL 在 HTML 探路之前就写进 iframe。E2E 第二例会红。不要为了「快 200ms」拆这道门。
12. **换 App。** 第三份 App 若只要聊天，仍须 `createAiService(该厅堂 http)` + 一份含 `ai/chat/index` 的 manifest + 能给 `baseUrl` / `probeFrame` / `trustedCredential` 的 runtime。不要偷 admin 的 `aiService` 单例。组合可以复制，单例不能跨 App 偷（L-007）。
13. **要加一条新的 `/snail-ai/*`。** 先改封面与工厂，再补 `index.test.ts`（像现有登记那样锁 URL）。若这条是控制台或 gRPC，不要做产品菜单键。
14. **迁移口诀：** 先数一座工厂名 ≠ Java 方法名 ≠ 厅堂单例名 → 再数一枪 POST 无 body、有 data 才投影、丢掉 avatarUrl、repeatSubmit 闸关掉 → 再数一键 `ai/chat/index`、空 permissions、`requireAiWebRuntime` 四口 → 再数会话先票后登记再探路、15 秒、过期 attempt、票不进失败 HTML → 再数控制台是监控厂、模型页不存在、九厂一行不要盖章。跳步会出现「把 Ai 说成聊天厨房」「把控制台说成本课第二键」「把九厂一行盖章」「把 Java 名说成厨房名」。

## 常见误区

1. **「OBJ-81 只包含 `createAiService`。」** 原文两颗：厨房 + 菜单。
2. **「OBJ-81 包含 `SnailAiController`。」** 那是 OBJ-80。
3. **「把九个 web-domain 工厂一行标 covered。」** 本课只给 `A:createAiWebDomain`。
4. **「厅堂符号叫 `createAiChatService`。」** 磁盘没有这份工厂。
5. **「工厂在 `transport.ts`。」** 在 `index.ts`。
6. **「页面自己拼 `/snail-ai/user/register`。」** 厨房才拼。
7. **「厨房方法叫 `registerCurrentUser`。」** 厨房是 `registerCurrentSnailUser`。
8. **「`service.chat.send`。」** 没有。
9. **「iframe `/snail-chat/` 是厨房第二枪。」** 不是。
10. **「`monitor/snailai/index` 是本厂第二键。」** 是监控厂。
11. **「`ai/model/index` 已经注册。」** registry 锁成 undefined。
12. **「投影会留下 avatarUrl。」** 丢掉。
13. **「README 的流式端口已接线。」** 没有。
14. **「`requireAiWebRuntime` 只检查对象在不在。」** 核四口。
15. **「缺 service 仍能冻出键，等点击才爆。」** 工厂门就爆。
16. **「权限中性等于匿名。」** 要票，要登记。
17. **「home 也能打开 AI 会话。」** 工作树没有。
18. **「capabilities 决定挂哪几页。」** 决定挂页的是 manifest + 菜单种子 + `selectedManifestIds`。
19. **「前端授权。」** 本页连藏按钮都没有。后端登录态仍是门。
20. **「厨房自己塞了 Authorization。」** 厅堂拦截器塞。
21. **「登记 body 带 nickname。」** 不带。后端 LoginHelper。
22. **「`snail-ai.enabled: false` 所以这页是死的。」** 看 `open-api.enabled`。
23. **「messages 已做 i18n。」** 模板写死。
24. **「把 `createAiChatSession` 标进矩阵 (a)。」** 不要。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/domains/ai/src/index.ts`。圈 `createAiService`、唯一的 `'post'`、`'/snail-ai/user/register'`、`repeatSubmit: false`、有 data 才投影。打开 `transport.ts`，确认没有工厂，确认没有 `avatarUrl`。打开 `snail-ai/index.ts`，圈资源标签不是 request。
2. 打开 `src/index.test.ts`。圈请求快照、无 body、失败 `rejects.toBe(failure)`。在 freeze 对象上搜 `chat` / `snail-chat` / `registerCurrentUser`，确认没有。
3. 打开 `frontend/packages/web-domains/ai/src/index.ts` 与 `index.test.ts` 与 `runtime.ts`。圈 id `web-domain-ai`、一键、空 permissions、四口 throw。核对测试**没有**断言 service / trustedCredential 那两句。核对 `componentKey` 不含 `monitor/snailai`，也不含 `ai/model`。
4. 打开 `chatSession.ts` 与 `chatSession.test.ts`。圈五句稳定中文、15 秒、脏底座不登记、探路成功才有 `frameUrl`、过期 openId 不能覆盖。打开 `AiChatPage.vue`。圈不 import 厅堂单例、按钮「重新加载」、iframe `allow` 剪贴板。
5. 打开 `apps/admin-web/src/application/services.ts` 第 90 行与 `router/adminManifestRegistry.ts` 第 59–76、343、358、371 行。圈 `aiService`、`adminAiWebRuntime`、`selectedManifestIds` 里的 `'web-domain-ai'`。打开 home-web，确认搜不到这两个工厂。打开 registry 测试，圈 `ai/chat/index` → `AiChatPage`，`ai/model/index` → undefined，`monitor/snailai/index` → `SnailAi`，以及 probeFrame 的 HTML / JSON / 503 分支。
6. 打开 `50-cde-base-dml.sql` 第 42 行与第 68–69 行。圈「AI会话」空 perms 对 `ai/chat/index`，「AI控制台」`monitor:snailai:list` 对 `monitor/snailai/index`。再打开 `SnailAiController.java`：确认方法名 `registerCurrentUser`、无 `@SaCheckPermission`、无 request body、条件注解在 `snail-ai.open-api`。

## 总结、词汇表与下一步

- **宏观一键聊天厅：** `createAiService` 印一枪登记牌；`createAiWebDomain` 把一张玻璃订进 `web-domain-ai`。墙上的菜 ≠ 厨房方法全集 ≠ 名字带 AI 的菜单全集。
- **(a) `createAiService`：** 根 `index.ts` freeze；POST `/snail-ai/user/register`；无 body；有 data 才投影并丢掉 `avatarUrl`；失败原样抛。本课**不**把 Java Controller 再标一遍 covered。
- **(a) `createAiWebDomain`：** 先 fail-closed 要四口，再冻一键与空权限数组，用 `h(page, { runtime })` 把厅堂的 `aiService` 喂进页面。本课**不**把矩阵那一行九个工厂一起闭合。
- **页面实际调用：** 会话扣那一枪，再探路，再贴 `/snail-chat/`。探路失败票不进 DOM。控制台玻璃不是本厂。模型页不存在。页上没有 hasPermi。

词汇表：`createAiService` / `AiService` / `registerCurrentSnailUser` / `projectAiUserTransport` / `aiDomainModule` / `aiSnailResource` / `createAiWebDomain` / `AiWebRuntime` / `requireAiWebRuntime` / `createAiChatSession` / `probeFrame` / `trustedCredential` / `componentKey` / `web-domain-ai` / permission-neutral / same-origin base path / resource metadata / host port / `registerCurrentUser`（Java，非本厂）。

下一步：登记窗 Java 链是 OBJ-80。监控室四块屏是 OBJ-24。厅堂插头是 OBJ-07。导航 host 是 OBJ-20。home / sso 没有这座厅是 OBJ-08。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/admin-web` | 厅堂 `aiService`；`adminAiWebRuntime` 四口；`selectedManifestIds` 含 `web-domain-ai`；home 无此工厂 | `application/services.ts` 第 90 行；`router/adminManifestRegistry.ts` 第 59–76、343、358、365–371 行；`home-web` `selectedManifestIds` | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}/ai` | 厨房 facade、投影、web-domain 一键、runtime、会话、子路径 export | 各包 `package.json` `exports` 与 `src` | 2026-09-17 |
| S-008 | 登记表 classic | `wta-ai` 保持 classic；本课只对照不改层次 | engineering-standards `03-backend-module-modes.md` | 2026-09-17 |
| S-010 | `50-cde-base-dml.sql` | 一级「AI会话」`ai/chat/index` 空 perms；「AI控制台」`monitor/snailai/index` | 第 42、68–69 行 | 2026-09-17 |
| S-L007-01 | L-007 厅堂组合 | 只吃 `domainHttp`；导出名 `aiService`；收尾三厂之一 | `lessons/L-007-admin-web-composition.md` | 2026-09-17 |
| S-L024-01 | L-024 监控室 | 运维 iframe 不是监控四屏，也不是本课一键 | `lessons/L-024-monitor-frontend.md`；registry `admin-external-monitor` | 2026-09-17 |
| S-L081-01 | `domains/ai/src/index.ts`；`index.test.ts` | 一枪 URL/动词；`repeatSubmit: false`；无 body；失败原样抛 | `createAiService`；三个 `it` | 2026-09-17 |
| S-L081-02 | `domains/ai/src/transport.ts`；`transport.test.ts` | 投影去 `avatarUrl`；`openId ?? ''` | `projectAiUserTransport` | 2026-09-17 |
| S-L081-03 | `web-domains/ai/src/index.ts`；`index.test.ts`；`runtime.ts` | 一键；空 permissions；四口 fail-closed | `createAiWebDomain`；三个 `it` | 2026-09-17 |
| S-L081-04 | `chatSession.ts`；`chatSession.test.ts` | 五句中文；脏底座；15s；过期 attempt；探路成功才有 URL | `createAiChatSession` | 2026-09-17 |
| S-L081-05 | `AiChatPage.vue` | 只收 runtime；iframe allow 剪贴板；按钮写死「重新加载」 | 模板与 script | 2026-09-17 |
| S-L081-06 | `adminManifestRegistry.test.ts`；`e2e/ai-domain.spec.ts` | 键解析；probe HTML/JSON/503；E2E 空权限仍加载；失败 HTML 不含 token | `ai/chat/index` / `probeFrame` / 两个 `test` | 2026-09-17 |
| S-L081-07 | `SnailAiController.java`；`application-dev.yml`；`application.yml` excludes | Java 名 `registerCurrentUser`；无 Sa 权限字；open-api 电闸；`/snail-chat/**` 排除 | `@PostMapping("/user/register")`；`snail-ai.open-api.enabled` | 2026-09-17 |
| S-L081-08 | `web-domains/ai/README.md`；`domains/ai/README.md`；`package.json` exports | README 流式/下载口过期；export 有 `./snail-ai` / `./pages` | 包名片与磁盘 runtime 对照 | 2026-09-17 |

## 文字等价物

本课所有 ASCII 图与对照表都可以用这段话代替：已经登录的管理员在 admin-web 打开一级菜单「AI会话」。导航用 `sys_menu.component` 去对 `createAiWebDomain` 冻住的一个 `componentKey`：`ai/chat/index`。页面只拿到 `AiWebRuntime`。真正的 `POST /snail-ai/user/register` 写在 `createAiService` 里，就一枪，无 body，有 data 才投影并丢掉头像。聊天玻璃的地址不是这支枪：会话先核同域底座和 `Admin-Token`，再喊登记换 `openId`，再让宿主 `probeFrame` 确认 `/snail-chat/` 是 HTML，成功才把带 `trustedCredential` 的 URL 写进 iframe。探路失败时票不准出现在页面 HTML。`permissions` 是空数组，种子 perms 也是空的，但没票仍进不去。名字很像的「AI控制台」挂在系统监控下，是另一份 manifest。home 与 sso 没有这座厅。九个 web-domain 工厂那一行不要因为本课盖章。
