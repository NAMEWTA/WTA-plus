---
lesson_id: L-033
objective_ids: [OBJ-33]
claimed_cells:
  - A:createOpenApiService
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: two-scopes-http
    minutes: 8
  - segment: projection-and-secret
    minutes: 7
  - segment: two-shells-permissions
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 2
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-L033-01, S-L033-02, S-L033-03, S-L033-04, S-L033-05, S-L033-06, S-L033-07, S-L033-08]
---

# Lesson 033：宏观两扇柜台——`createOpenApiService` 与资料页 `openApi.vue`

## 学完你能做什么

打开厨房 `frontend/packages/domains/system/src/open-api/service.ts` 的 `createOpenApiService`，再打开厅堂壳 `frontend/apps/admin-web/src/views/system/user/profile/openApi.vue`，你能**口述管理端浏览器怎么管钥匙、怎么看货架**，而不是把「OpenAPI」说成 HMAC 盖章客户端，也不是把资料页壳说成 `systemService` 上的一个字段。

本课认矩阵 **(a)** 一格，符号以磁盘为准：

1. **`A:createOpenApiService`**（包 `@namewta/domain-system`，实现在 `src/open-api/service.ts`）：工厂只认 `HttpClient`，返回冻住的 `OpenApiService`。上面只有两扇柜台：`currentUser`（自己）和 `targetUser`（必须先写出目标 `userId`）。每一枪都是 `http.request({ url, method, params?, data?, headers? })`，再走 `transport.ts` 投影。**不**自己画 Vue，**不**读 `Admin-Token`，**不**写 `X-App-Key` / `X-Signature`。

OBJ-33 还要你能顺着 admin-web 那张**薄壳**把柜台接到人脸上：

- **`openApi.vue`**：资料页「OpenAPI」页签。磁盘上只有一个模板节点：`<open-api-workspace :runtime="runtime" :scope="{ kind: 'current-user' }" />`。它从厅堂单例 `openApiService` 手搓一份 `OpenApiWorkspaceRuntime`。测试锁死：**没有** `userId:`。
- 同包工作区 `OpenApiWorkspace.vue` + `workflow.ts` 是**共用柜员剧本**。超管菜单页 `OpenApiAdminPage.vue`（键 `system/openApi/index`）也用它，但 scope 是 `target-user`。本课要能拆开两扇门，格子仍只认工厂 `createOpenApiService`。

2026-09-16 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `createSystemService.openApi` | **没有这个字段。** 厅堂并列调用 `createOpenApiService(domainHttp)`，导出名 `openApiService` |
| `@namewta/domain-openapi` | **没有这个包。** 厨房在 `@namewta/domain-system` 的 `src/open-api/` |
| 只能走子路径，像监控那样 | **半对。** `package.json` 有 `"./open-api"`；根 `src/index.ts` **也** re-export `createOpenApiService`。监控工厂 `createMonitorService` 才是「根 barrel 找不到、必须 `@namewta/domain-system/monitor`」 |
| `home-web` 对称一份 | **没有。** `apps/home-web` 搜不到 `createOpenApiService` / `openApiService` |
| 页面自己拼 `/system/openApi` | **厨房才拼。** Vue 只喊 `runtime.openApi.currentUser.*` / `targetUser.*` |

本课**不宣称**你会拆 `SysOpenApiCatalogController` 四扇目录窗（L-030）、凭据十三条窗和密文四列（L-031）、网关 HMAC / nonce / 机器会话（L-032）、或把 `createSystemWebDomain` 15 键再讲一遍（L-020）。今天只认：**浏览器这一头的厨房纸条、两扇柜台、两张门脸，以及一次性信封怎么在前端亮一次就丢掉。**

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`import { createOpenApiService, createSystemService } from '@namewta/domain-system'`，下一行 `export const openApiService = createOpenApiService(domainHttp)`。L-019 把这座工厂标成**另一座厨房**，不并进八个抽屉。L-020 只认菜单键 `system/openApi/index` 和 runtime 口 `openApi`，HTTP 全表留给本课。L-030 / L-031 是后端两份 Controller，共用门牌 `/system/openApi`。L-032 是外面的机器人盖章进门。本课站在**已经登录的管理员浏览器**这一头。

三条河都叫 OpenAPI，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| 前端 `openapi/current.json` / `generated/openapi.ts` | 管理端 HTTP 快照复印件（L-005） | 邻居：厨房 URL 会出现在这份快照里 |
| `createOpenApiService` | 浏览器管理厨房：自己的钥匙 + 别人的钥匙 + 货架 | **本课格子** |
| `openApi.vue` / `OpenApiWorkspace` / `OpenApiAdminPage` | 资料页壳、共用柜员、超管选人页 | **本课对照**；矩阵只标工厂 |
| `SysOpenApiCatalogController` / `SysOpenApiCredentialController` | 后端目录窗 / 钥匙柜台 | L-030 / L-031 |
| `OpenApiGatewayFilter` | 机器五颗签名头 | L-032；本课 HTTP **不**走那扇门 |

2026-09-16 工作树：权威厨房是 `frontend/packages/domains/system/src/open-api/` 五份文件（`service.ts` / `transport.ts` / `types.ts` / `index.ts` / `index.test.ts`）。厅堂接线是 `apps/admin-web/src/application/services.ts` 第 35 行。资料页壳是 `apps/admin-web/src/views/system/user/profile/openApi.vue`。超管页和柜员剧本在 `packages/web-domains/system/src/open-api/`。

```text
已登录的人（浏览器，Admin-Token）          机器调用方（HMAC，L-032）
        │                                         │
        ├─ 资料页签 openApi.vue                    不走本课厨房
        │     scope = current-user                五颗 X-OpenAPI-* 头
        │     runtime.openApi = openApiService
        │
        └─ 菜单键 system/openApi/index
              OpenApiAdminPage 先 listUsers
              再 workspace scope = target-user
                        │
                        v
              createOpenApiService(domainHttp)     OpenApiGatewayFilter
                        │
          currentUser  /self/*                     验签 / nonce / 限流 / 机器口袋
          targetUser   /users/{userId}/*
                        │
                        v
              厅堂 adminHttp.request
              （Bearer 登录票，不是签名头）
                        │
                        ├─ GET  .../credential
                        ├─ POST .../credential/{create|reset|enable|disable|delete}
                        ├─ GET  .../interfaces[/{interfaceId}]
                        └─ GET  /system/openApi/users          （仅 targetUser.listUsers）
```

| 符号 | 磁盘 | 拥有什么 | 不拥有什么 |
| --- | --- | --- | --- |
| `createOpenApiService` | `domains/system/src/open-api/service.ts` | 两扇柜台的 URL / 动词 / 入参校验 / `no-store` 头 | Vue、权限串、剪贴板、`Admin-Token` 抽屉 |
| 投影器 | 同目录 `transport.ts` | Summary / Issued / 目录项冻成不可变；秘密字段闸 | HTTP 发送 |
| `openApiService` | `apps/admin-web/.../services.ts` | 厅堂单例，把 `domainHttp` 塞进工厂 | 页面生命周期 |
| `openApi.vue` | `views/system/user/profile/openApi.vue` | 手搓 runtime + **写死** `current-user` | 选人、`listUsers`、菜单注册 |
| `OpenApiAdminPage.vue` | `web-domains/system/src/open-api/` | 远程搜人；选中后挂 `target-user` | 自己画凭据表单 |
| `OpenApiWorkspace.vue` + `workflow.ts` | 同上 | 加载、创建/重置信封、启停删、目录抽屉、错误四态 | 拼 URL、HMAC |

**图题 / caption：** 管理端 OpenAPI 宏观两扇柜台。alt：资料页走 current-user，超管页先选人再走 target-user；两路汇入同一工厂；工厂只打浏览器登录 HTTP，不打机器签名头。

**文字等价物：** 厅堂只造一个 `openApiService`。自己管钥匙走 `currentUser`，超管替别人管必须先有 `userId` 再走 `targetUser`。资料页壳永远不把 `userId` 写进 scope。菜单页先 `listUsers`，再把选中的人交给同一份工作区。卡车司机是 `adminHttp`，口袋里是登录票。外面的机器人走另一扇门，本课厨房看不见那五颗头。

**类比：** 银行大厅有两扇柜台，共用一台**表格打印机**（`createOpenApiService`）。自助柜台只给「我的柜子」打单。经理柜台必须先在名单上点一个名字，才会给「那个人的柜子」打单。资料页 `openApi.vue` 是自助室的玻璃门，门上写死「当前用户」。超管菜单页是经理室，先搜人再开门。工作区是柜员念的那本剧本：先看名牌、再决定要不要给一次性信封。货架（目录）在大厅另一侧，**没有柜子也能看**「这张证现在能借哪些接口」。

**类比失效处：**

1. 经理也拿不到第二次明文钥匙。信封只在 create / reset 成功那一次出现；关掉对话框就 `dismissIssuedSecret`。
2. 打印机打的是登录票卡车，不是机器人盖章。厨房源码里搜不到 `X-App-Key`。
3. 货架不靠柜子存在。工作区文案写明：目录按当前身份的实时权限生成，不依赖凭据是否已创建。凭据 404 且目录成功 → 空柜子，不是整页报错。
4. 表格抽屉里有一张备用单 `getInterface`。柜员剧本**从不**调用它；抽屉详情用的是列表里已经拿到的那一行。
5. `systemDomainModule.capabilities` **没有** `openApi` 这一项。能力清单缺席，不等于厨房不存在。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **先找工厂，再找页面。** `export function createOpenApiService(http: HttpClient)`。参数只有 `http`。返回值 `Object.freeze`。
2. **两扇柜台，不是一个 `openApi()`。** `currentUser` 八枪，URL 都带 `/self/`。`targetUser` 九枪，除 `listUsers` 外每一枪都要把合法 `userId` 编进路径。
3. **厅堂并列插插头。** `openApiService` 和 `systemService` 是邻居，不是父子。
4. **读 GET，写 POST。** 删除也是 POST `.../credential/delete`，没有 `method: 'delete'`。
5. **只有开柜和换锁带禁缓存头。** create / reset 的 headers 是冻住的 `{ 'Cache-Control': 'no-store', repeatSubmit: false }`。GET / 启停 / 删除不加这组头。
6. **名牌盒和信封盒不是同一个类型。** GET / 启停走 `OpenApiCredentialSummary`（**禁止**任何键名带 `secret`）。create / reset 走 `OpenApiCredentialIssued`（必须有非空 `appSecret`，而且秘密键只能叫 `appSecret`）。
7. **路径段要盖章。** `userSegment` 只放 `/^[1-9]\d*$/`；`interfaceSegment` 拒绝空白。测试用 `'order/query'`，路上变成 `order%2Fquery`。
8. **创建请求会先换衣服。** `createInput` 只留下 trim 后的 `appName`（1–100 字）和可选的 `expiresAt` / `remark`（备注 ≤500）。测试故意塞 `ownerUserId` / `appSecret`，出门时这两样**不在** `data` 里。
9. **资料页是薄壳。** `openApi.vue` 不调 `listUsers`，不写 `userId`，不注册菜单键。它只把厅堂单例和 `kind: 'current-user'` 交给工作区。
10. **超管页先闸再搜。** 没有 `system:openApi:list` 就显示「当前账号没有目标用户查询权限」，**不打** `/users`。有权限才 `listUsers({ keyword, limit: 50 })`。
11. **权限串按 scope 换字典。** 自助室四个动作全是 `system:openApi:self`。经理室：看=`query`，建=`add`，改（重置/启停）=`edit`，删=`remove`。只有 list 权限的人，工作区 create 会在进厨房前失败关闭成 `forbidden`。
12. **信封不进 localStorage。** web-domain 测试用 `not.toMatch(/localStorage|sessionStorage/)` 锁死工作区源码。关闭对话框的两条路径（`@update:model-value` 和 `@closed`）都要 `dismissIssuedSecret`。

**类比补一句：** 把 `transport.ts` 想成银行的验钞灯。灯一灭就抛 `OpenAPI 响应不可用`，不会把「差不多对」的 JSON 交给页面。这盏灯坏了不是去 HMAC 门口找原因。

### 精确定义与 English term

| 中文说法 | English term | 磁盘定义 |
| --- | --- | --- |
| OpenAPI 领域工厂 | `createOpenApiService` | `frontend/packages/domains/system/src/open-api/service.ts`。入参 `HttpClient`，返回冻住的 `OpenApiService` |
| OpenAPI 领域服务 | `OpenApiService` | `types.ts`：只含 `currentUser` + `targetUser` 两个只读口 |
| 当前用户柜台 | `CurrentUserOpenApiService` | 八方法；路径前缀 `/system/openApi/self`；编译期 **没有** `userId` 参数 |
| 目标用户柜台 | `TargetUserOpenApiService` | 九方法；`listUsers` 走 `/users`，其余走 `/users/{userId}/...`；`userId` 类型 `string \| number` |
| 安全摘要 | `OpenApiCredentialSummary` | 可重复运输的名牌。无 `appSecret`。GET / 启停 / 用户列表嵌套凭据 |
| 一次性签发 | `OpenApiCredentialIssued` | Summary + `appSecret`。类型注释：*A command-only value. Never retain this object in a store or cache.* |
| 创建输入 | `OpenApiCredentialCreateInput` | `appName` 必填；`expiresAt` / `remark` 可选。工厂侧会 trim / 截类型 |
| 用户查询 | `OpenApiUserQuery` | `keyword?` + `limit?`（整数 1–100） |
| 目录项 | `OpenApiCatalogItem` | `interfaceId` / `summary` / `method` / `path` / `accessRule` / `parameters` / 两个 schema / curl / java 示例 |
| 传输投影 | `projectOpenApi*` | `transport.ts`。形状不对就 `unavailable()`，文案固定 `OpenAPI 响应不可用` |
| 信封响应包装 | `projectOpenApiResponse` | 要求 `code` 是整数，再投影 `data`；`msg` 可缺省 |
| 凭据状态 | `OpenApiCredentialState` | `resolveOpenApiCredentialState`：过期时间已到 → `expired`；否则 `status==='0'` → `enabled`，否则 `disabled` |
| 目录分组 | `groupOpenApiCatalog` | 按 `path` 第一段分组。厨房导出，工作区表格**没用**它 |
| 工作区 scope | `OpenApiWorkspaceScope` | 判别联合：`{ kind: 'current-user' }` 或 `{ kind: 'target-user', userId, userLabel }` |
| 工作区运行时 | `OpenApiWorkspaceRuntime` | `openApi` + `confirm` / `success` / `error` / `hasPermission` / `copyText`。比 `SystemWebRuntime` 窄 |
| 工作区错误 | `OpenApiWorkspaceError` | `'conflict' \| 'disabled' \| 'forbidden' \| 'unavailable'` |
| 资料页壳 | `openApi.vue` | admin-web 个人设置页签。静态插入，不经 `createSystemWebDomain` 的 `runtimeView` |
| 超管页 | `OpenApiAdminPage` | manifest 登记：id `system-open-api`，键 `system/openApi/index`，组件名 `OpenApi` |
| 禁缓存头 | `NO_STORE` | `Object.freeze({ 'Cache-Control': 'no-store', repeatSubmit: false })` |
| 失败关闭投影 | fail-closed projection | 多一个 `secret*` 键、缺 `appSecret`、非法 method `CONNECT`、空 permissions 列表 → 整份扔掉 |

`OpenApiCredentialStatus` 仍是 `'0'` / `'1'`（启用 / 停用），**不是** OSS 的 `Y`/`N`。过期是前端用 `Date.parse(expiresAt) <= now` 另算的第三态，不是库里的第三种 status。

### 机制/因果链

#### 1. 厅堂怎么把钥匙塞进这座厨房

`frontend/apps/admin-web/src/application/services.ts`：先做懒电线 `domainHttp = { request: config => adminHttp.request(config) }`（L-007 的初始化环），再：

```ts
export const systemService = createSystemService(domainHttp);
export const openApiService = createOpenApiService(domainHttp);
```

`adminHttp` 来自 `application/http.ts` 的 axios adapter，带 `getToken`（`Admin-Token`）和 `clientId`。厨房函数签名里没有 session。换 App 只换司机；home-web **根本不雇**这座厨房。

`adminManifestRegistry.ts` 把同一单例塞进 `adminSystemWebRuntime.openApi`。`createSystemWebDomain(adminSystemWebRuntime)` 用 `runtimeView` 把**整份** `SystemWebRuntime` 注入 `OpenApiAdminPage`。资料页不走这条注册：`profile/index.vue` 的页签直接 `<open-api />`，壳文件自己 import `@/application/services`。

两扇门脸因此**不是**同一个 runtime 对象：超管页拿到的是大厅全套（含 `service` / 字典 / 关页）；资料页只手搓工作区那六个字段。结构上够用，因为工作区只读 `OpenApiWorkspaceRuntime`。

#### 2. 两扇柜台的 HTTP 形状

`BASE_PATH = '/system/openApi'`。内部 `request` 先 `http.request`，再 `projectOpenApiResponse(response, project)`。运输错误原样拒绝：测试里 `kind: 'business', code: 403` 的对象 `rejects.toBe(forbidden)`，工厂不包一层。

**`currentUser`（八枪）**

| 方法 | HTTP | 投影 | 特殊头 |
| --- | --- | --- | --- |
| `getCredential()` | GET `/self/credential` | Summary | 无 |
| `createCredential(input)` | POST `/self/credential/create` body=`createInput(input)` | Issued | `NO_STORE` |
| `resetCredential()` | POST `/self/credential/reset` | Issued | `NO_STORE` |
| `enableCredential()` | POST `/self/credential/enable` | Summary | 无 |
| `disableCredential()` | POST `/self/credential/disable` | Summary | 无 |
| `deleteCredential()` | POST `/self/credential/delete` | `projectOpenApiEmpty` → `null` | 无 |
| `listInterfaces()` | GET `/self/interfaces` | Catalog 数组 | 无 |
| `getInterface(id)` | GET `/self/interfaces/{encodeURIComponent(id)}` | Catalog 一项 | 无 |

**`targetUser`（九枪）**

| 方法 | HTTP | 投影 |
| --- | --- | --- |
| `listUsers(query?)` | GET `/users` `params=userQuery(query)` | 用户摘要数组（可嵌套 Summary 或 `credential: null`） |
| `getCredential(userId)` | GET `/users/{id}/credential` | Summary |
| `createCredential(userId, input)` | POST `/users/{id}/credential/create` | Issued + `NO_STORE` |
| `resetCredential(userId)` | POST `/users/{id}/credential/reset` | Issued + `NO_STORE` |
| `enableCredential(userId)` | POST `/users/{id}/credential/enable` | Summary |
| `disableCredential(userId)` | POST `/users/{id}/credential/disable` | Summary |
| `deleteCredential(userId)` | POST `/users/{id}/credential/delete` | empty |
| `listInterfaces(userId)` | GET `/users/{id}/interfaces` | Catalog 数组 |
| `getInterface(userId, id)` | GET `/users/{id}/interfaces/{encode(id)}` | Catalog 一项 |

这张表对齐 L-031 的十三扇凭据窗 + L-030 的四扇目录窗。工厂**没有** export，没有 PUT。`listUsers` 的 `limit` 在工厂侧卡 1–100；超管页调用时写死 `limit: 50`，和后端默认值碰巧相同，但卡子在厨房。

非法 `userId`（`0`、小数、空串、带字母）在发车前抛 `TypeError('OpenAPI target userId is invalid')`，不会把垃圾拼进 URL。空白 `interfaceId` 同样先抛。

#### 3. 投影闸：名牌不许夹带秘密

`projectCredential(value, includeSecret)` 先把对象上所有键名小写后含 `secret` 的键找出来：

- Summary（`includeSecret=false`）：**任何一个**这样的键 → `unavailable()`。所以 GET 响应里哪怕多写一个 `appSecret: null` 也会整份扔掉。
- Issued（`includeSecret=true`）：允许且只允许键名恰好是 `appSecret`；值为空串也不行（`requiredString`）。

目录项同样严：HTTP method 枚举不含 `CONNECT`；`permissions.values` 不允许空数组；`parameters.location` 只认 `header|path|query`；`responseSchema` 必须是非空字符串，不能是对象。

`groupOpenApiCatalog` 用 `item.path.split('/').find(Boolean) ?? 'root'` 当组键，**不改** item 身份（测试 `groups[0].items[0]` 是同一个引用）。工作区表格的 `:data="[...state.catalog]"` 只是拆 freeze 给 Element Plus，没有分组 UI。

`resolveOpenApiCredentialState` 给工作区徽章用：过期优先于启停。过期时「启用/停用」按钮不渲染（`credentialState !== 'expired'`），重置和删除仍在。

#### 4. 资料页壳怎么把柜台接到脸上

`profile/index.vue` 在个人设置卡片里加页签 `name="openApi"`，标签「OpenAPI」，里面是 `<open-api />`。页签是静态的：没有 `v-hasPermi`，没有按权限卸掉页签。真正的闸在工作区 `load()` 的 `requireAction('view')` → `system:openApi:self`。没有这串权限，工作区停在 `forbidden` 警告，不发 HTTP。

`openApi.vue` 全文很短，职责只有三件：

1. import `OpenApiWorkspace`（子路径 `@namewta/web-domain-system/open-api`，不是厨房包）。
2. import 厅堂 `openApiService`。
3. 组 runtime：`openApi` / `confirm` / `success` / `error` / `hasPermission` / `copyText`。`copyText` 没有 Clipboard API 就抛 `Clipboard API unavailable`，工作区会把失败写成「复制失败，请手动复制」。

scope **字面量** `{ kind: 'current-user' }`。composition 测试同时锁 `index.vue` 含 `name="openApi"` 和壳文件不含 `userId:`。

watch 到 scope 就重置 state 并 `load()`。资料页 scope 永不切换，所以这是「进页签立刻拉自己的名牌和货架」。

#### 5. 超管页与共用柜员剧本

`OpenApiAdminPage` 自己管选人。`onMounted` 调 `searchUsers()`。远程下拉每次输入都再打 `listUsers`。没选人时只显示空状态「请选择要管理的目标用户」，**不**挂工作区。选中后：

```vue
<open-api-workspace
  :key="String(selectedUser.userId)"
  :runtime="runtime"
  :scope="{ kind: 'target-user', userId: selectedUser.userId, userLabel: userLabel(selectedUser) }"
/>
```

`:key` 换人就拆掉旧工作区，避免上一个人的信封留在对话框里。`userLabel` 是 `` `${userName} / ${nickName || userId}` ``。

`createOpenApiWorkspaceController` 按 scope 绑死函数指针。current-user 测试：`create` / `load` 只碰 `service.currentUser.*`，`targetUser.getCredential` 一次都不会被叫。target-user 测试：`load` / `reset` / `disable` / `delete` 全部带 `'41'`。

加载用 `Promise.allSettled([getCredential(), listInterfaces()])`，再用 `generation` 丢弃过期响应。判定空柜子的规则要背精确：

| 凭据结果 | 目录结果 | 页面态 |
| --- | --- | --- |
| 成功 | 成功 | 有名牌 + 货架 |
| 404 | 成功 | `credential=null`，`error=null`（尚未创建） |
| 404 | 404 | `error='disabled'`（文案：OpenAPI 当前未启用） |
| 其它失败 | 任意 | 先看目录失败；目录成功则看凭据失败 |

`classifyOpenApiError` 读 `status` / `code` / `response.status` / `response.data.code`：403→forbidden，404→disabled，409→conflict，其余 unavailable。这是前端恢复 UI 的四态，不是 L-032 网关对外的 401/429/503。

create / reset 走 `applyIssued`：`state.issued = value`，同时 `safeSummary` 用解构丢掉 `appSecret` 再写入 `state.credential`。名牌区只渲染 Summary 字段（appName / appKey / 到期 / 更新 / 备注），**没有**密钥行。密钥只活在「一次性 AppSecret」对话框里，`close-on-click-modal=false`。

工作区**不调用** `getInterface`。点「查看调用合同」把列表行塞进 `selectedInterface`，抽屉展示 `curlExample` / `javaExample`。厨房仍保留详情枪，给合同对齐和测试，不是这张 UI 的数据源。

### 图、表或文本图

```text
profile/openApi.vue                 OpenApiAdminPage.vue
  scope=current-user                  闸 system:openApi:list
  无 userId                           listUsers(keyword, 50)
        \                            /  选中才挂 workspace
         \                          /   scope=target-user + userId
          v                        v
           OpenApiWorkspace.vue
           createOpenApiWorkspaceController
                  │  can(action) 先查权限
                  │  current-user → 全 self
                  │  target-user  → query/add/edit/remove
                  v
           openApiService = createOpenApiService(domainHttp)
                  │
                  ├─ create/reset → Issued 投影 + no-store
                  ├─ GET/enable/disable → Summary 投影（拒 secret 键）
                  ├─ delete → data 必须是 null/undefined
                  └─ listInterfaces → 冻住的目录数组
```

**图题 / caption：** 两张门脸共用一份柜员剧本和一座厨房。alt：资料页无 userId；超管页先 list 再 target-user；控制器按动作换权限串；工厂按方法换投影。

**文字等价物：** 人先碰到的是壳。壳决定 scope。剧本决定喊 `currentUser` 还是 `targetUser`、先查哪串权限。厨房决定 URL、动词、禁缓存头和投影闸。信封只在剧本的 `state.issued` 里活一会儿，名牌区永远是 Summary。

### 正例、反例与边界

**正例 1 — 资料页开柜。** 有 `system:openApi:self` 的登录人打开个人设置 → OpenAPI。`load` 并行 GET `/self/credential` 与 `/self/interfaces`。若凭据 404、目录 200，空状态出现「创建凭据」。填应用名「订单同步服务」，可选到期 `YYYY-MM-DDTHH:mm:ss`，备注空则送 `null`。厨房 POST `/self/credential/create`，headers 带 `no-store`，body 只有合法字段。投影出发票 `Issued`。对话框展示 AppKey 与 AppSecret；名牌区用丢掉秘密后的 Summary。点关闭 → `issued=null`。再 GET 也只有名牌。

**正例 2 — 超管给 41 换锁。** 有 `system:openApi:list` 的人打开菜单 `system/openApi/index`，搜 `demo`，`listUsers` 打出 `/system/openApi/users?keyword=demo&limit=50`。选中 41 后工作区 `load` 打 `/users/41/credential` 与 `/users/41/interfaces`。点重置前先 `confirm`。成功后再次弹出信封。`appKey` 是否变化不是前端决定的（L-031：reset 保持编号）；前端只把本次 `Issued` 亮出来。

**正例 3 — 斜杠接口号。** 测试 `getInterface('order/query')` 的 URL 是 `/system/openApi/self/interfaces/order%2Fquery`。不要把斜杠当路径层级。

**反例 1 — 把 HMAC 头塞进厨房。** `open-api/` 目录没有签名头字符串。那是 L-032 机器门。本课卡车只有登录票。

**反例 2 — 资料页传 `target-user`。** 壳测试禁止 `userId:`。自己的柜子走 `/self/`，owner 由后端 `LoginHelper.getUserId()` 决定（L-031），前端不得在自助室填别人。

**反例 3 — Summary 里偷看 `appSecret`。** 投影直接关闸。页面即使想绑这个字段，厨房也不会把非法 GET 响应递进来。

**反例 4 — 只有 list 权限就替别人 create。** 工作区 `requireAction('create')` 要 `system:openApi:add`。测试：`hasPermission` 只对 list 为真时，`targetUser.createCredential` **零次**调用，`error='forbidden'`。

**边界 1 — 404 的两种读法。** 单凭据 404 ≠ 功能关闭。目录也 404 才显示「OpenAPI 当前未启用」（Bean 没装配或入口 404，对应 L-031/L-032 的 `openapi.enabled`）。

**边界 2 — `getInterface` 存在但 UI 不用。** 口试要能指厨房有这枪，并说工作区详情抽屉吃的是列表行。

**边界 3 — `groupOpenApiCatalog` 存在但表格不分组。** 不要口述成页面按 path 第一段分了手风琴。

**边界 4 — capabilities 缺席。** `systemDomainModule.capabilities` 列到 oss/social/monitor-*，没有 openApi。菜单键和权限目录在 web-domain manifest 里，不在这份能力数组。

**边界 5 — 过期徽章。** `expiresAt` 已到则按钮组隐藏启停，不是把 status 改成第三种码。

## 变式与迁移

1. **和 `createMonitorService` 对照。** 监控必须走子路径，根 `index.ts` 不导出。OpenAPI **两处都能 import**：根 barrel 与 `./open-api`。厅堂实际走根：`from '@namewta/domain-system'`。不要把「system 包三厂」说成三种同样的导出姿势。
2. **和 `createSystemService` 对照。** 八抽屉是 CRUD 资源口，方法名 `list/get/add`。OpenAPI 按**人的身份**切柜台，方法名是 `getCredential` / `listInterfaces`。不要在 `systemService` 上找 `openApi`。
3. **和 L-031 对照。** 后端十三条窗的权限、超管闸、密文四列、create 不作废会话，本课不重讲。前端只保证：自助室不传 owner、create/reset 带 `no-store`、Summary 拒秘密、Issued 只活在内存对话框。
4. **和 L-030 对照。** 目录四扇窗的过滤规则在服务端。前端 `listInterfaces` 把数组原样投影；工作区文案「不依赖凭据是否已创建」对应「货架和柜子不是同一张表」。
5. **换 App。** 今天只有 admin-web 接线。若以后某个 App 只想给用户看自己的柜子，应只暴露 `currentUser` 用法，仍须经过 `createOpenApiService`，不要在页面里手写 `/system/openApi/self`。
6. **详情要不要再打一枪。** 当前 UI 信任列表行。若以后目录项变大、列表改成摘要，才需要把抽屉改成 `getInterface`。现在改等于重复请求。

## 常见误区

1. **「`systemService.openApi`。」** 没有。厅堂单例叫 `openApiService`。
2. **「资料页就是菜单页 `OpenApi`。」** 菜单组件名 `OpenApi` 对应 `OpenApiAdminPage.vue`。资料页文件是小写 `openApi.vue`，scope 永远 current-user。
3. **「前端会 HMAC。」** 不会。管理页走登录票。
4. **「明文不回显 = create 也不返回密钥。」** 和 L-031 同一句话：签发盒必须带 `appSecret`；不回显指 GET / 列表 / 启停 / 关掉对话框之后。
5. **「删除用 DELETE。」** POST `.../delete`。
6. **「工作区会把密钥写入 sessionStorage 方便刷新后再看。」** 测试禁止这两类 API。刷新只能再 GET 名牌。
7. **「点接口详情等于 `getInterface`。」** 不等于。
8. **「`openApi.vue` 会搜用户。」** 不会。`listUsers` 只出现在 `OpenApiAdminPage` 和 `targetUser`。
9. **「权限全是 `system:openApi:self`。」** 只对 current-user。target-user 四动作拆四串，外加选人用的 list。
10. **「404 就是没权限。」** 403 才是 forbidden。凭据 404 可能只是还没开柜。
11. **「home-web 也有资料页 OpenAPI。」** 本课核对：home-web 无工厂、无单例。
12. **「`capabilities` 有 openApi 所以厨房在领域模块清单里。」** 清单没有这一项；厨房仍在 `src/open-api/`。
13. **「`repeatSubmit: false` 出现在每一枪。」** 只在 create/reset 的 issued headers。
14. **「本课覆盖机器网关失败关闭。」** 那是 L-032。本课失败关闭是**投影拒收畸形 JSON** 和**缺权限不发命令**。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/domains/system/src/open-api/service.ts`。圈 `BASE_PATH`、`NO_STORE`、`userSegment`、`interfaceSegment`、`createInput`。顺着 `currentUser` / `targetUser` 把 URL 和 method 点完。圈 create/reset 独有的 `headers: issuedHeaders`。
2. 打开同目录 `transport.ts`。圈 `secretKeys` 过滤、`projectOpenApiCredentialIssued` 只在 `includeSecret` 为真时要 `appSecret`、`projectOpenApiEmpty`、`resolveOpenApiCredentialState`、`groupOpenApiCatalog` 的第一段规则。
3. 打开 `index.test.ts`。圈 self 那组期望 URL（含 `order%2Fquery`）、create 的 `data` 被剥掉 `ownerUserId`/`appSecret`、target 每枪都带 `41`、403 对象 `rejects.toBe`。
4. 打开 `apps/admin-web/src/application/services.ts` 第 7 行和第 35 行。圈工厂从 `@namewta/domain-system` 进，导出 `openApiService`，**不是** `systemService` 的字段。
5. 打开 `apps/admin-web/src/views/system/user/profile/openApi.vue` 与 `index.vue` 的 OpenAPI 页签。圈 `kind: 'current-user'` 和 `openApi: openApiService`。打开同目录 `openApi.test.ts`，确认断言不含 `userId:`。
6. 打开 `packages/web-domains/system/src/open-api/workflow.ts`。圈权限字典、`Promise.allSettled`、404+目录成功才把凭据留空、`safeSummary` 丢 `appSecret`、`classifyOpenApiError` 四态。打开 `OpenApiAdminPage.vue` 圈 `system:openApi:list` 与 `kind: 'target-user'`。打开 `OpenApiWorkspace.vue` 圈一次性对话框的两条关闭路径，以及目录抽屉没有调用 `getInterface`。

## 总结、词汇表与下一步

- **宏观两扇柜台：** `currentUser` 管自己，`targetUser` 必须先有人。资料页 `openApi.vue` 只开自助室；菜单 `OpenApiAdminPage` 先选人再开经理室。
- **(a) `createOpenApiService`：** 独立工厂，冻住两口，拼 `/system/openApi`，读 GET 写 POST，create/reset 带 `no-store`，投影失败关闭。
- **薄壳：** admin-web `openApi.vue` 手搓 runtime，写死 `current-user`，不搜人，不进 manifest 15 键那条 `runtimeView` 链。
- **共用剧本：** `OpenApiWorkspace` 按 scope 换权限串和柜台；信封只活在 `state.issued`；目录与凭据并行拉。
- **不是机器门：** 卡车是 `Admin-Token`。HMAC、nonce、机器口袋留在 L-032。
- **备用枪不是 UI 枪：** `getInterface` / `groupOpenApiCatalog` 在厨房里，工作区表格和抽屉没用它们。

词汇表：`createOpenApiService` / `OpenApiService` / `currentUser` / `targetUser` / `OpenApiCredentialSummary` / `OpenApiCredentialIssued` / `NO_STORE` / `projectOpenApiResponse` / `resolveOpenApiCredentialState` / `openApi.vue` / `OpenApiWorkspace` / `OpenApiAdminPage` / `OpenApiWorkspaceScope` / `OpenApiWorkspaceRuntime` / `system:openApi:self` / fail-closed projection。

下一步：目录过滤与 `interfaceId` 烙印是 OBJ-30；密文四列、明文只在签发盒、超管闸是 OBJ-31；HMAC 与失败关闭网关是 OBJ-32。厅堂插头是 OBJ-07，菜单键挂牌是 OBJ-20。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web,sso-web}` | 工厂单例只在 admin-web `services.ts`；home-web 无 `createOpenApiService`；资料页壳与页签 | `application/services.ts`；`views/system/user/profile/openApi.vue`；home-web 无匹配 | 2026-09-16 |
| S-006 | `frontend/packages/{domains,web-domains,platform}` | 厨房在 domain-system `open-api/`；工作区在 web-domain-system；根 barrel 与 `./open-api` 双导出 | 各包 `package.json` 与 `src` | 2026-09-16 |
| S-L033-01 | `packages/domains/system/src/open-api/service.ts` | 工厂、两扇柜台、`NO_STORE`、`userSegment`/`interfaceSegment`/`createInput`/`userQuery` | `createOpenApiService` 全文 | 2026-09-16 |
| S-L033-02 | `src/open-api/transport.ts`；`types.ts` | Summary 拒 secret 键；Issued 只要 `appSecret`；空删除；状态三值；分组函数 | `projectCredential`；`resolveOpenApiCredentialState`；`OpenApiCredentialIssued` 注释 | 2026-09-16 |
| S-L033-03 | `src/open-api/index.ts`；`src/open-api/index.test.ts`；`domains/system/src/index.ts`；`package.json` `exports` | 子路径与根 re-export；self/target URL 表；create 剥多余字段；403 原样拒绝 | 导出列表；两个 `describe` | 2026-09-16 |
| S-L033-04 | `apps/admin-web/src/application/services.ts`；`application/http.ts` | 并列 `createOpenApiService(domainHttp)`；卡车是带 `getToken` 的 axios，无 OpenAPI 签名头 | 第 7、35 行；`createAxiosBrowserAdapter` | 2026-09-16 |
| S-L033-05 | `apps/admin-web/src/views/system/user/profile/openApi.vue`；`index.vue`；`openApi.test.ts` | 薄壳 current-user；页签名 `openApi`；断言无 `userId` | 模板与 composition 测试 | 2026-09-16 |
| S-L033-06 | `web-domains/system/src/open-api/workflow.ts`；`workflow.test.ts` | 权限字典；allSettled 空柜子规则；信封丢弃；缺 add 不发 create；409/403 可分 | controller；六个 `it` | 2026-09-16 |
| S-L033-07 | `OpenApiWorkspace.vue`；`OpenApiAdminPage.vue`；`open-api/index.ts` | 一次性对话框双关闭路径；目录抽屉用列表行；选人闸 list；target-user + `:key` | 模板与 script | 2026-09-16 |
| S-L033-08 | `web-domains/system/src/index.ts`；`runtime.ts`；`index.test.ts`；`adminManifestRegistry.ts`；`domains/system/src/index.ts` `systemDomainModule` | 键 `system/openApi/index` 名 `OpenApi`；runtime 口 `openApi`；权限目录含 self/list/remove；capabilities 无 openApi；厅堂注入 `openApi: openApiService` | registrations 行；permissions `openApi`；capabilities 数组；registry `adminSystemWebRuntime` | 2026-09-16 |
