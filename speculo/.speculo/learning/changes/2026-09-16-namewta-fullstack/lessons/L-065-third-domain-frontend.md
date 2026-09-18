---
lesson_id: L-065
objective_ids: [OBJ-65]
claimed_cells:
  - A:createThirdService
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: four-families-http
    minutes: 11
  - segment: mapping-gaps
    minutes: 7
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-010, S-014, S-L007-01, S-L065-01, S-L065-02, S-L065-03, S-L065-04, S-L065-05, S-L065-06, S-L065-07, S-L065-08]
---

# Lesson 065：宏观四本表格——`createThirdService` 怎么把 Provider/Endpoint/Credential/Observability 打到 `/third/*`

## 学完你能做什么

打开厨房 `frontend/packages/domains/third/src/index.ts` 的 `createThirdService`，你能**口述浏览器怎么把三方接口的四本表格打到 `/third/*`**：供应商、接口、凭据、观测。不是 `ThirdPartyGateway.execute`（那是 Java 出站，L-064），不是 `createThirdWebDomain` 四键菜单（L-066），也不是把 `POST /third/provider/save` 说成这座厨房已经有的第二支保存枪。

口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

1. **`A:createThirdService`**（包 `@namewta/domain-third`，实现就在根 `src/index.ts`，没有另写 `transport.ts`）：工厂只认 `HttpClient`，返回 `Object.freeze` 的 `ThirdService`。上面是 **15 个扁平方法**，分成四族：Provider 五枪、Endpoint 五枪、Credential 三枪、Observability 两枪。每一枪都是内部 `call` → `http.request({ url, method, params?, data? })`。**不**自己画 Vue，**不**读 `Admin-Token`，**没有** `execute` / `invoke` / `gateway`，**没有**嵌套的 `service.providers.list`。

OBJ-65 还要你能把「哪本表格」和「哪条 URL」对上，并说出厨房**故意少打的两扇后端窗**：

- 厅堂单例 `thirdService = createThirdService(domainHttp)`。
- 四族前缀：`/third/provider`、`/third/endpoint`、`/third/credential`、以及观测的 `/third/invocation` 与 `/third/statistics`（观测控制器门牌是 `/third`，不是 `/third/observability`）。
- `saveProvider` / `saveEndpoint` **永远** `POST` 到新增窗 `/third/provider`、`/third/endpoint`。OpenAPI 快照里的 `POST /third/provider/save`、`POST /third/endpoint/save` **厨房方法表没有**。
- 路径里的 id 走 `encodeURIComponent(String(value))`。测试锁死：`'provider/1'` → `.../provider%2F1/status`。
- 读 GET，写 POST。删除是 `POST .../remove`，启停是 `POST .../status` 且状态在 **query** `params.status`，不在 body。厨房里没有 `method: 'delete'`，也没有 `put`。

2026-09-17 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `createThirdService` 在 `transport.ts`，像 notify | **没有。** 类型、模块名片、工厂、`id()` 全在 `src/index.ts` |
| `@namewta/domain-third/provider` 四个子路径 | **没有。** `package.json` `exports` 只有 `"."` |
| `thirdService.providers.list` / `service.observability.invocations` | **没有嵌套抽屉。** 方法名就是 `listProviders` / `listInvocations` |
| `saveProvider` = `POST /third/provider/save` | **不是。** 磁盘 URL 是 `POST /third/provider`。`/save` 是后端 edit 窗，厨房没接 |
| `createThirdDirectory` / 厅堂另写凭据对象 | **没有。** 凭据三枪就在这座工厂里 |
| 厨房有 `execute(providerCode, endpointCode)` | **没有。** 出站是 Java `ThirdPartyGateway`，浏览器厨房不打电话 |
| 页面自己拼 `/third/provider/list` | **厨房才拼。** Vue 喊 `runtime.service.listProviders(...)` |
| `home-web` / `sso-web` 对称一份 | **没有。** 两厅搜不到 `createThirdService` / `thirdService` |
| `@namewta/domain-third` 依赖 `api-contracts` 所以 URL 是 `keyof paths` | **包依赖表没有 api-contracts。** 15 条 URL 都是字符串字面量 |
| 厨房测试锁死了 15 枪 | **只锁 5 枪。** `index.test.ts` 只断言 status/remove 的 POST 与 `%2F` |

本课**不宣称**你会拆 `ThirdProviderController` 六扇与路径安全（L-060 / 子课 L-001）、Endpoint 如何被网关点名（L-061 / 子课 L-002）、凭据 AES-GCM 与列表不回显（L-062 / 子课 L-003）、invocation 近 7 天 / 最多 200 条（L-063 / 子课 L-004）、`ThirdPartyGateway.execute` → Adapter → SPI（L-064 / 子课 L-005），或把 `createThirdWebDomain` 四键 + 嵌套凭据对话框再讲成菜单课（L-066）。今天只认：**浏览器这一头的三方厨房 URL 表，四族 HTTP 怎么对上四份管理 Controller，以及厨房标签上写着、方法表里没有的那两扇 `/save` 门。**

矩阵 (a) 不要把 `createThirdWebDomain` 一并盖章。本课只给 **`createThirdService` 这一颗**。

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`export const thirdService = createThirdService(domainHttp)`，收尾三个只吃 http 的工厂之一。L-008 对照过 home / sso **没有**这座厨房。L-060…L-063 是后端 `/third/*` 的四份管理窗。L-064 是出站网关，**不是**本课卡车路线。本课站在**已经登录的管理员浏览器**这一头：口袋里是 `Admin-Token`，司机是 `adminHttp`。

四条河都叫 third，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `createThirdService` | 浏览器三方厨房：供应商 / 接口 / 凭据 / 观测查询 | **本课格子** |
| `thirdService` | 厅堂单例，把 `domainHttp` 塞进工厂 | **接线**；格子不标 App 组合点（那是 L-007） |
| `createThirdWebDomain` / `ThirdPage` | 管理端四键 + 一张 Vue 按 `kind` 换脸 | L-066；本课只认它是厨房的消费者 |
| `ThirdProviderController` 等四份 | 后端 layered 窗 | L-060…L-063 |
| `ThirdPartyGateway.execute` | Java 出站打电话 | L-064；**厨房没有这支枪** |
| `thirdDomainModule.capabilities` | 模块名片四项 | **对照**：名片有 `third-credential`，菜单却没有凭据键 |

2026-09-17 工作树：权威厨房是 `frontend/packages/domains/third/`（根 facade 一份 `src/index.ts` + `src/index.test.ts`）。权威厅堂接线是 `apps/admin-web/src/application/services.ts` 第 9、91 行。权威菜单包是 `packages/web-domains/third/`（本课不把工厂标 covered）。消费者在 `adminManifestRegistry.ts` 第 245–254 行把 `{ service: thirdService, confirm, success, error }` 交给 `createThirdWebDomain`。菜单种子在 `50-cde-base-dml.sql`：目录 `三方接口管理` 下四张 C 型页 `third/{provider,endpoint,invocation,statistics}/index`；凭据只有挂在供应商页下的 F 型按钮，没有 `third/credential/index`。

```text
已登录的管理员（浏览器，Admin-Token）
        │
        ├─ 四张菜单页（createThirdWebDomain，L-066）
        │     runtime.service = thirdService
        │     kind = providers | endpoints | invocations | statistics
        │     凭据对话框挂在供应商/接口行上，不是第五键
        │
        x  没有厅堂铬另写第三张脸
        x  没有 home / sso 插座
                        │
                        v
              ┌─────────────────────────────────────────┐
              │  createThirdService(domainHttp)         │  ← 本课厨房
              │    list/get/save/status/remove Provider │
              │    list/get/save/status/remove Endpoint │
              │    list/save/remove Credential          │
              │    listInvocations / listStatistics     │
              └─────────────────────────────────────────┘
                        │
                        v
              厅堂 domainHttp → adminHttp.request
              （Bearer 登录票；不是出站 HMAC，也不是供应商回调头）
                        │
        /third/provider/*     /third/endpoint/*
        /third/credential/*   /third/invocation/list
                              /third/statistics/list
        （没有 /third/gateway、没有 /third/execute、
          厨房不打 POST /third/provider/save、
          厨房不打 POST /third/endpoint/save）
```

| 符号 | 磁盘 | 拥有什么 | 不拥有什么 |
| --- | --- | --- | --- |
| `createThirdService` | `domains/third/src/index.ts` | 15 枪 URL / 动词 / 扁平方法名；`Object.freeze`；路径 id 编码 | Vue、权限串、`Admin-Token` 抽屉、出站 execute、`/save` 两窗 |
| `ThirdService` | 同文件 `export interface` | 15 个方法签名；返回 `ApiResponse<T>` | 运行时校验缺方法 |
| `thirdDomainModule` | 同文件 | `id: 'third'`，`backendModules: ['wta-third']`，四项 capabilities | componentKey、HTTP 发送 |
| `id()` | 同文件私有箭头 | `encodeURIComponent(String(value))` | query 参数编码（那是司机的事） |
| `call` | 工厂闭包 | `http.request<ApiResponse<T>>(config)` | fail-closed 投影、剥字段、`no-store` 头 |
| `thirdService` | `apps/admin-web/.../services.ts` | 厅堂单例 | 页面生命周期 |
| `createThirdWebDomain` | `web-domains/third/src/index.ts` | 四键 + 权限目录；凭据权限挂在 `third-provider` 组 | 拼 URL。**本课格子不认它** |
| `ThirdPage.vue` | 同包一张 Vue | 四 `kind` 喊齐 15 枪；凭据嵌套对话框 | 自己写 `/third/*` 字符串 |

**图题 / caption：** 三方前端宏观四本表格。alt：四张菜单页共用一座扁平厨房；凭据不是第五本菜单；厨房不打出站网关，也不打 Provider/Endpoint 的 `/save` 编辑窗。

**文字等价物：** 厅堂只造一个 `thirdService`。要改通讯录就喊 Provider 五枪，要改门牌就喊 Endpoint 五枪，要锁盒子就喊 Credential 三枪，要看回放就喊观测两枪。四族前缀分别是 `/third/provider`、`/third/endpoint`、`/third/credential`、以及挂在 `/third` 门牌下的 `/invocation/list` 与 `/statistics/list`。卡车司机还是 `adminHttp`，口袋里是登录票。业务模块真正打电话走 Java 网关，本课厨房看不见那扇门。后端给供应商/接口各留了两扇保存窗（新增 `/` 与编辑 `/save`），本课打印机只按新增那一扇。

**类比：** 把 `createThirdService` 想成邮局大厅的**四本表格打印机**。柜台上四叠表格：①大楼通讯录（Provider：楼在哪、门牌号、限流）；②房间门牌（Endpoint：敲哪扇门、白名单）；③保险箱编号单（Credential：只印盒号，不印箱里的纸）；④闭路电视回放单（Observability：倒带、看计数）。服务员（Vue）只喊「来一份 list / save / remove」，地址印在厨房。`createThirdWebDomain` 是墙上的点菜单，本课不考点菜单怎么排，只考打印机打哪几张表。

**类比失效处：**

1. 打印机**不会**替你给第三方公司打电话。出站是后厨 Java 网关（L-064）。厨房 15 枪全是管理配置与只读回放。
2. 通讯录和门牌在后端各有「新增」和「改正」两扇窗。打印机的保存键**只按新增那扇**。改正时 body 里可以带着旧 id，管家 `useCase.save` 仍会更新，但门口权限字是 `*:add`，日志是 INSERT。不要把「业务上在编辑」说成「厨房打了 `/save`」。
3. 保险箱编号单**不是**第五本点菜单。菜单种子和 web-domain 测试都锁死：没有 `third/credential/index`。盒子挂在通讯录行或门牌行的「凭据」按钮后面。
4. 回放单不能远程开门。观测两枪都是 GET，厨房和后端都没有 POST。
5. `encodeURIComponent` **不等于**生产 id 可以含斜杠。后端路径变量是 `Long`。测试用 `'provider/1'` 只锁 URL 形状，不锁「斜杠 id 能进库」。
6. 打印机填表**不验钞**。没有 OpenAPI 那种投影闸，没有 `no-store`，多出来的 JSON 字段原样塞进 `data`。
7. 名片上有 `third-credential` **不等于**墙上有凭据键。`capabilities` 不是 `componentKey`。
8. 从 `@namewta/domain-third` 进口就是整座厨房。没有「只含观测的小子路径」。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **先找工厂。** `export function createThirdService(http: HttpClient): ThirdService`。参数只有 `http`。返回值 `Object.freeze`。有导出的接口名 `ThirdService`（和 notify 那种只写 `ReturnType` 不一样）。
2. **四族十五枪，扁平，不是一个 `third()`。** Provider 五、Endpoint 五、Credential 三、Observability 二。没有 `service.provider.list`。
3. **读 GET，写 POST。** 启停和删除也是 POST。厨房里搜不到 `'delete'` / `'put'`。
4. **保存键对着新增窗。** `saveProvider` → `POST /third/provider`；`saveEndpoint` → `POST /third/endpoint`；`saveCredential` → `POST /third/credential`。凭据本来就没有 `/save`；供应商和接口有，厨房没接。
5. **路径 id 会编码。** 私有 `id()` 是 `encodeURIComponent`。用在 get / status / remove 三段路径，**不用**在 list 的 query。
6. **启停走 query。** `changeProviderStatus(id, status)` 的 `params: { status }`，不是 `{ data: { status } }`。
7. **没有投影闸。** 内部 `call` 只是 `http.request` 再标成 `ApiResponse<T>`。畸形 JSON 不会在厨房被扔掉。
8. **凭据列表没有 `secretJson`。** 类型 `CredentialSummary` 不带秘密；`CredentialForm` 才带。保存把明文 JSON 交给后端加密（L-062），厨房自己不加密、不解密、不 get-by-id。
9. **观测是只读。** `listInvocations` / `listStatistics` 只有 GET。TypeScript 把 `providerCode` 写成必填 `string`；后端 query 其实可选，空白会被服务端收成「不按供应商过滤」。
10. **一张 Vue 喊齐十五枪。** 和 notify「厨房有、UI 不用」相反：`ThirdPage` 按 `kind` 把 15 个方法都扣过扳机。少的是后端 `/save` 两窗，不是厨房备用枪。
11. **子路径不存在。** 不要口述成「从 `./observability` 进口只含两枪」。包只有根 barrel。
12. **home 没有这台打印机。** 产品差在 App 插座板，不在工厂里 `delete` 方法。

**类比补一句：** 把 `index.ts` 想成一台只会填表头的打印机，不会验钞，也不会猜你是「新增」还是「改正」——保存键焊死在新增窗口上。填错格子（比如把数字 id 写成带斜杠的字符串）它仍会把 `%2F` 打出去；进不进得了 Java `Long`，是后窗的事。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 三方领域工厂 | `createThirdService` | `frontend/packages/domains/third/src/index.ts`。入参 `HttpClient`，返回冻住的 `ThirdService` |
| 三方领域模块名片 | `thirdDomainModule` | 同文件：`id: 'third'`，`backendModules: ['wta-third']`，`capabilities: ['third-provider', 'third-endpoint', 'third-credential', 'third-observability']` |
| 厅堂三方单例 | `thirdService` | `apps/admin-web/src/application/services.ts` 第 91 行 |
| 供应商 | Provider | 大楼：`baseUrl`、编码、超时、限流、共享头。类型 `Provider` / `ProviderForm` |
| 接口 / 调用点 | Endpoint | 门牌：相对路径、方法、schema JSON、幂等、可选 `adapterCode`。类型 `Endpoint` / `EndpointForm` |
| 凭据摘要 | `CredentialSummary` | 盒号视图：id、范围、类型、`kekVersion`、到期、`enabled: string`。**无** `secretJson` |
| 凭据表单 | `CredentialForm` | 写入：`secretJson: string`，`enabled: boolean`，可选 `credentialId` |
| 调用回放 | Invocation | 观测行：requestId、逻辑状态、脱敏 JSON、耗时 |
| 按日计数 | Statistic | 观测行：attempt/success/failure/timeout/rejected/quota。类型**没有** statisticId |
| 包装信封 | `ApiResponse<T>` | 本包自写：`code?` / `data?` / `msg?` / `error?`。不是 axios 类型 |
| 路径段编码 | `id()` | `encodeURIComponent(String(value))`。只用于路径 id |
| 失败关闭投影 | fail-closed projection | **本课厨房没有。** 对照 L-033 的 OpenAPI `transport.ts` |
| 出站网关 | `ThirdPartyGateway.execute` | Java SPI；**本课厨房不打** |
| 管理端菜单工厂 | `createThirdWebDomain` | L-066；runtime 口 `service: ThirdService` |
| 权限串形状 | `third:<resource>:<action>` | 包 `AGENTS.md` 约定；厨房源码**不**出现这些字符串 |

`capabilities` 是模块名片，**不是** componentKey，也不决定 App 挂哪几页。菜单键在 web-domain manifest 与 `50-cde-base-dml.sql`。

厨房产方法与 Controller 门牌：

| 族 | 后端窗（父课） | 厨房有没有对应方法 | 本课页面用不用（对照，格子仍只认工厂） |
| --- | --- | --- | --- |
| Provider | `ThirdProviderController` `/third/provider` 六扇 | **五枪。** 缺 `POST /save` | `ThirdPage` kind=`providers` 用五枪 |
| Endpoint | `ThirdEndpointController` `/third/endpoint` 六扇 | **五枪。** 缺 `POST /save` | kind=`endpoints` 用五枪 |
| Credential | `ThirdCredentialController` `/third/credential` 三扇 | **三枪。** 后端本来就没有 get / status / `/save` | 嵌套对话框用三枪；**无**凭据菜单键 |
| Observability | `ThirdObservabilityController` `@RequestMapping("/third")` 两扇 GET | **两枪。** `listInvocations` / `listStatistics` | kind=`invocations` / `statistics` |
| Gateway | 无管理 Controller | **无方法** | 无 Vue |

### 机制/因果链

#### 1. 厅堂怎么把钥匙塞进这座厨房

`frontend/apps/admin-web/src/application/services.ts`：先做懒电线 `domainHttp = { request: config => adminHttp.request(config) }`（L-007 的初始化环），再：

```ts
import { createThirdService } from '@namewta/domain-third';
// ...
export const thirdService = createThirdService(domainHttp);
```

`adminHttp` 来自 `application/http.ts` 的 axios adapter，带 `getToken`（`Admin-Token`）和 `clientId`。厨房函数签名里没有 session。换 App 只换司机；home-web / sso-web **根本不雇**这座厨房。

`adminManifestRegistry.ts`：

```ts
const adminThirdWebRuntime: ThirdWebRuntime = {
  service: thirdService,
  confirm: async message => { /* modal.confirm */ },
  success: message => { /* modal.msgSuccess */ },
  error: message => { /* modal.msgError */ }
};
const thirdManifest = createThirdWebDomain(adminThirdWebRuntime);
```

`selectedManifestIds` 含 `'web-domain-third'`。四键的页面剧本留给 L-066；本课只要能指着：**runtime 只有 `service` 这一根 HTTP 线**，没有 notify 那种并列 `directory`。凭据不另写厅堂对象。

`composeAppRuntime` 还登记了 `thirdDomainModule`。名片和菜单工厂是两份冻住的东西：缺名片不会让 15 枪消失；有名片也不会多出 `/save`。

#### 2. 工厂内部长什么样

整座厨房 59 行。没有子目录。`call` **不**做 fail-closed 投影。运输错误原样拒绝。路径 id 用 `id()` 编码后再插模板字符串。

```ts
const id = (value: string | number) => encodeURIComponent(String(value));
export function createThirdService(http: HttpClient): ThirdService {
  const call = <T>(config: Parameters<HttpClient['request']>[0]) => http.request<ApiResponse<T>>(config);
  return Object.freeze({ /* 15 个方法 */ });
}
```

对照邻居，不要背成「所有 domain 工厂都一样严」：

| | `createThirdService` | `createNotificationService`（L-053） | `createOpenApiService`（L-033） |
| --- | --- | --- | --- |
| 文件 | 根 `index.ts` | `transport.ts` | `open-api/service.ts` + 投影器 |
| 形状 | 15 个扁平函数 | 五块嵌套 | 两扇柜台 |
| URL | 字符串字面量 | 监控两枪 `keyof paths`，其余字面量 | 字面量 + 路径段函数 |
| id | `encodeURIComponent` | **直插**，不编码 | `interfaceSegment` 编码 |
| 投影 | 无 | 无 | fail-closed，秘密闸 |
| 测试锁 | 5 枪 status/remove | 三类目标 save + 独立 publish 等 | self/target 全表 + 403 |

#### 3. 十五枪对十七扇窗

OpenAPI 快照 `generated/openapi.ts` 里 `/third/*` 一共 **17** 条 path。厨房方法 **15**。差的就是两扇编辑保存窗。

**Provider（五枪厨房 / 六扇后端）**

| 方法 | HTTP | query / body | 后端窗 | 权限字（后门，厨房不写） |
| --- | --- | --- | --- | --- |
| `listProviders(keyword?)` | GET `/third/provider/list` | `params: { keyword }`，即使 `undefined` 也出现在对象里 | `list` | `third:provider:list` |
| `getProvider(id)` | GET `/third/provider/${id(id)}` | 路径 | `get` | `third:provider:query` |
| `saveProvider(data)` | POST `/third/provider` | `data: ProviderForm` | **`add`**，不是 `save` | `third:provider:add` |
| （厨房无方法） | POST `/third/provider/save` | body 同样是 Bo | `save` / edit | `third:provider:edit` |
| `changeProviderStatus(id, status)` | POST `/third/provider/${id(id)}/status` | `params: { status }` | `status` | `third:provider:edit` |
| `deleteProvider(id)` | POST `/third/provider/${id(id)}/remove` | 无 body | `remove` | `third:provider:remove` |

`ThirdPage` 编辑供应商会先 `getProvider`，再 `saveProvider(form)`。表单带着 `providerId`。后端 `add` 与 `save` **都**进 `useCase.save`：有 id 就更新。所以「改正」在业务上能成功，前提是调用方持有 **add** 权限。页面保存按钮在 `editing` 时核 `third:provider:edit`——UI 闸和 HTTP 窗**不是同一把锁**。口试要能同时说出这两句，不要发明「编辑走 `/save`」。

**Endpoint（五枪厨房 / 六扇后端）**

和 Provider 一个模子，前缀换成 `/third/endpoint`，路径变量是 `endpointId`：

| 方法 | HTTP |
| --- | --- |
| `listEndpoints(providerId?, keyword?)` | GET `/third/endpoint/list`，`params: { providerId, keyword }` |
| `getEndpoint(id)` | GET `/third/endpoint/${id(id)}` |
| `saveEndpoint(data)` | POST `/third/endpoint`（**add 窗**） |
| （厨房无方法） | POST `/third/endpoint/save` |
| `changeEndpointStatus(id, status)` | POST `/third/endpoint/${id(id)}/status`，`params: { status }` |
| `deleteEndpoint(id)` | POST `/third/endpoint/${id(id)}/remove` |

列表多一个可选 `providerId`。页面把「供应商 ID」输入框和「供应商」关键字拆开：`listEndpoints(providerId.value || undefined, providerCode.value || undefined)`。空串在页面先收成 `undefined`，厨房仍会把键放进 `params` 对象。

**Credential（三枪厨房 / 三扇后端，对齐）**

后端本来就没有 get-by-id、没有 status、没有 `/save`、没有 `third:credential:edit`。更新也走新增窗，权限永远是 `third:credential:add`。这一族厨房**没有**少接的窗。

| 方法 | HTTP | 备注 |
| --- | --- | --- |
| `listCredentials(providerCode, endpointCode?)` | GET `/third/credential/list` | `params: { providerCode, endpointCode }`。`providerCode` 在类型上必填 |
| `saveCredential(data)` | POST `/third/credential` | body 含 `secretJson` 与布尔 `enabled`；有 `credentialId` 就是替换 |
| `deleteCredential(id)` | POST `/third/credential/${id(id)}/remove` | 测试锁死数字 `3` → `/third/credential/3/remove` |

列表类型是 `CredentialSummary[]`：有 `scopeType` / `kekVersion` / `enabled: string`，**没有**秘密字段。页面文案写「保存时加密，不会回显」——加密发生在后端（L-062），厨房只是把本次输入的 `secretJson` 放进 POST body。替换时页面先把 `secretJson` 写成 `''`，再等用户重新填；空白会被后端拒绝（`secretJson` `@NotBlank`）。不要口述成「替换可以不带秘密」。

`enabled` 在两份类型里不是同一种东西：摘要上是 **string**（后端 Vo 填的是 `delFlag`）；表单上是 **boolean**（Bo 的 `Boolean enabled`）。页面 `replaceCredential` 用 `credential.enabled === '0'` 还原开关。不要把摘要上的 `'0'` 说成 Provider 的 status 码——碰巧都是 `'0'` 表示在用，字段名却会骗人。

**Observability（两枪厨房 / 两扇后端，对齐）**

| 方法 | HTTP | 后端 |
| --- | --- | --- |
| `listInvocations(providerCode)` | GET `/third/invocation/list`，`params: { providerCode }` | `ThirdObservabilityController.invocations` |
| `listStatistics(providerCode)` | GET `/third/statistics/list`，`params: { providerCode }` | `statistics` |

注意拼写：**statistics** 全程复数。控制器类名是 Observability，URL **不**出现 `observability` 这个词。两枪都无 `@Log`，都是 GET。TypeScript 要求 `providerCode: string`；页面空着也会传 `''`。后端 Service 把空白收成 `null`，表示不按供应商过滤（L-063：invocation 另截近 7 天、最多 200 条；statistic 最多 200 条、不截 7 天）。这些截断是后窗合同，厨房只保证 URL 与动词。

页面表格列不展示 `sanitizedRequestJson` / `sanitizedResponseJson`。类型里有这两字段，因为列表 JSON 会带回来。口试不要说「厨房剥掉了脱敏字段」——厨房没有投影器。

#### 4. 谁在喊：一张 Vue，四种 kind

```text
ThirdPage kind=providers
  listProviders / getProvider / saveProvider
  changeProviderStatus / deleteProvider
  行上「凭据」→ listCredentials(providerCode)   （不传 endpointCode）
                 saveCredential / deleteCredential

ThirdPage kind=endpoints
  listEndpoints / getEndpoint / saveEndpoint
  changeEndpointStatus / deleteEndpoint
  行上「凭据」→ listCredentials(providerCode, endpointCode)

ThirdPage kind=invocations
  listInvocations(providerCode)

ThirdPage kind=statistics
  listStatistics(providerCode)
```

web-domain 页 **不** import `@/application/services`。厅堂没有铃铛第二张脸。口试可以说：三方只有 runtime 这一张脸。

`createThirdWebDomain({ service: {} as never })` 仍能冻出四键（web-domain 测试就是这么写的）。缺方法会在 `onMounted` 的 `load()` 炸，不会在厨房工厂或菜单工厂当场 throw。

#### 5. 测试实际锁了什么

`src/index.test.ts` 只有一个 `it`：`uses the POST status and remove routes exposed by wta-third`。假 `HttpClient` 记下 `HttpRequest`，然后：

1. `changeProviderStatus('provider/1', '1')` → `{ url: '/third/provider/provider%2F1/status', method: 'post', params: { status: '1' } }`
2. `deleteProvider('provider/1')` → `{ url: '/third/provider/provider%2F1/remove', method: 'post' }`
3. `changeEndpointStatus(2, '0')` → `/third/endpoint/2/status`，`params.status = '0'`
4. `deleteEndpoint(2)` → `/third/endpoint/2/remove`
5. `deleteCredential(3)` → `/third/credential/3/remove`

口试要点：

- 斜杠被编码，数字不被改写。
- 断言对象**没有** `data`。启停不走 body。
- **没有**断言 `saveProvider` 的 URL。所以「保存打 `/save`」这种口误，现有测试抓不住。要以 `index.ts` 第 44、49 行为准，不要以「想当然的 REST 编辑窗」为准。
- 观测两枪、三份 list、两份 get、三份 save **都不在**这个 `it` 里。不是「没有这些方法」，是「测试没锁」。

### 图、表或文本图

```text
ThirdPage (providers|endpoints|invocations|statistics)
        runtime.service
                 │
                 v
          createThirdService          无 directory，无 execute
                 │
     ┌───────────┼───────────┬────────────┐
     v           v           v            v
 /third/provider /third/endpoint /third/credential  /third/invocation/list
     │               │              │               /third/statistics/list
     x 不打 /save    x 不打 /save    （本来就无 /save）     无 POST
     │
     x 不打 /third/gateway
     x 不打 /third/execute
```

**图题 / caption：** 四族前缀、两扇没接的 `/save`、没有出站窗。alt：扁平工厂把 15 枪打到四份 Controller；Provider/Endpoint 的 edit 保存窗空着；网关不在浏览图表上。

**文字等价物：** 人先碰到四张菜单或行上的凭据按钮。服务员对托盘喊扁平方法名。厨房决定 `/third/provider|endpoint|credential|invocation|statistics` 这些 URL 和 GET/POST。供应商和接口的「改正」在后端另有 `/save`，打印机没做那只键。真正给第三方打电话的网关不从这张浏览图表出发。

**图 2：十五枪 / 十七窗对照**

| 厨房方法 | 厨房 URL | OpenAPI / Controller 是否另有窗 | 2026-09-17 谁扣扳机 |
| --- | --- | --- | --- |
| `listProviders` | GET `/third/provider/list` | 对齐 | `ThirdPage` providers |
| `getProvider` | GET `/third/provider/{id}` | 对齐 | 编辑行 |
| `saveProvider` | POST `/third/provider` | **另有** POST `/third/provider/save` | 新增与编辑都喊它 |
| `changeProviderStatus` | POST `.../status` | 对齐 | 表格开关 |
| `deleteProvider` | POST `.../remove` | 对齐 | 删除 |
| `listEndpoints` … `deleteEndpoint` | `/third/endpoint/*` | **另有** POST `/third/endpoint/save` | endpoints kind |
| `listCredentials` / `saveCredential` / `deleteCredential` | `/third/credential/*` | 对齐（无 get、无 `/save`） | 嵌套对话框 |
| `listInvocations` / `listStatistics` | GET `.../list` | 对齐 | invocations / statistics kind |
| （无） | — | Java `ThirdPartyGateway.execute` | **无 Vue** |

**图题 / caption：** 工厂方法表以 `index.ts` 为准，不以 REST 想象为准。alt：15 个厨房方法全部有 Vue 调用；少的是两扇 `/save` 和整条出站链。

**文字等价物：** 和通知厨房相反：三方厨房里没有「墙上没挂的备用枪」；十五支都有人扣。缺口在另一侧——后端多出来的编辑保存窗，以及根本不属于管理厨房的出站网关。

**图的边界：** 不画 Java 五层、不画 Redis 快照 `evict`、不画 AES-GCM 字节、不保证以后产品会把 `saveProvider` 拆成 add/edit 两枪。不把 `capabilities` 画成菜单。不把 `ThirdPage` 四 `kind` 说成本课格子。脱敏 JSON 如何打码是 L-063 写入路径，本图画的是管理 GET。

## 正例、反例与边界

**正例 1 — 按关键字列供应商。** 有 `third:provider:list` 的人打开 `third/provider/index`。工具栏「供应商」输入 `pay`，点查询。`listProviders('pay')` → `GET /third/provider/list?keyword=pay`。厨房不 trim、不短路；空字符串也会出门。后端 list 才对 code/name like（L-060）。

**正例 2 — 新建大楼。** 点新增，填编码、名称、`https://api.example.com`。`saveProvider(form)` 且 `providerId` 为空 → `POST /third/provider`。后端 `add` 窗、`BusinessType.INSERT`、`useCase.save` 走新建分支。

**正例 3 — 改正仍打新增窗。** 编辑行先 `getProvider(id)` → `GET /third/provider/{id}`。改备注后保存仍 `saveProvider(form)`，body 带着原来的 `providerId`，URL **还是** `POST /third/provider`。不是 `/save`。若调用方只有 edit 没有 add，UI 可能仍显示保存键，HTTP 会在 add 窗 403。这是映射事实，不是口试用「应该」抹掉的。

**正例 4 — 斜杠编码。** 测试 `changeProviderStatus('provider/1', '1')` 的 URL 是 `/third/provider/provider%2F1/status`。生产雪花数字不会走这条分支；口试认的是 `id()` 这把尺子。

**正例 5 — 开关走 query。** 表格开关打开 → `changeProviderStatus(id, '0')`（启用）；关掉 → `'1'`（停用）。POST，`params.status`。后端把一切非 `"1"` 的字符串写成启用（L-060）；厨房原样传递。

**正例 6 — 供应商级盒子。** 供应商行点「凭据」。`listCredentials('acme')`（第二参 `undefined`）→ `GET /third/credential/list?providerCode=acme`。后端只列 `endpoint_id IS NULL` 的供应商级盒子，不会顺便倒出各扇门的盒子（L-062）。

**正例 7 — 门牌级盒子与替换。** 接口行点「凭据」。`listCredentials(code, endpointCode)`。点替换：页面写入 `credentialId`，清空 `secretJson`，用户重新贴一段 JSON，再 `saveCredential`。仍是 `POST /third/credential`，权限仍是 add。没有 `third:credential:edit` 这串种子。

**正例 8 — 回放。** 打开 `third/invocation/index`，供应商框留空。`listInvocations('')` → `GET /third/invocation/list?providerCode=`。后端空白当不过滤。厨房不会自己加 `from` / `limit`；7 天和 200 条是 Mapper 的事。

**反例 1 — 「`saveProvider` 打 `/third/provider/save`。」** 打开第 44 行。URL 是 `'/third/provider'`。`/save` 只活在 Java 与 `openapi.ts`。

**反例 2 — 「`thirdService.execute` / `thirdService.gateway`。」** freeze 对象没有这些键。出站在 L-064。

**反例 3 — 「`service.providers.list`。」** 没有嵌套。方法名是 `listProviders`。

**反例 4 — 「从 `@namewta/domain-third/observability` 进口小厨房。」** `exports` 只有 `"."`。

**反例 5 — 「页面手写 `/third/endpoint/list`。」** 违反 L-004 方向：`App → web-domain → domain → platform`。URL 只许出现在 `domains/third/src/index.ts`。

**反例 6 — 「删除用 DELETE，启停用 PUT。」** POST `.../remove`、POST `.../status`。

**反例 7 — 「`status` 放进 body。」** 磁盘是 `params: { status }`。测试对象里没有 `data`。

**反例 8 — 「凭据有 get-by-id，编辑时再拉一次明文。」** 没有这枪。摘要永不带 `secretJson`。替换必须重新提交秘密。

**反例 9 — 「在 home-web 的 `services.ts` 加 `createThirdService`，却不改 package.json 与 registry。」** 三点组合门会裂开（L-008）。今天 home 没有三方产品差。

**反例 10 — 「本课覆盖 `createThirdWebDomain`。」** 矩阵下一格是 OBJ-66 / L-066。不要把四键顺序、凭据嵌套测试标进本课 covered。

**反例 11 — 「观测 URL 是 `/third/observability/invocation/list`。」** 控制器 `@RequestMapping("/third")` + `@GetMapping("/invocation/list")`。

**反例 12 — 「`listStatistics` 的 path 是 `/third/statistic/list`。」** 磁盘是 **statistics** 复数。

**边界 1 — 厨房 15 枪都有 Vue；缺口在后端多窗。** 不要把 notify 的「备用枪」故事原样套过来。

**边界 2 — UI 权限串 ≠ HTTP 窗权限串。** 编辑保存核 `*:edit`，卡车却开向 `*:add`。凭据族没有这个裂缝，因为根本没有 edit 窗。

**边界 3 — `params` 对象里的 `undefined`。** 工厂总是把键写上。司机（axios）是否省略 undefined query，是 adapter 的事；口试以工厂字面量为准，不要把「省略」教成厨房短路。

**边界 4 — `listInvocations` 类型必填 vs 后端可选。** 空串会出门。不要说「没填供应商厨房就不打观测」。

**边界 5 — 测试只锁 5 枪。** 其余 10 枪以 `index.ts` 为准。补测试是以后的工程，不是本课作业。

**边界 6 — 路径编码 vs 查询编码。** `id()` 只管路径段。`providerCode` 在 query 里，不走 `id()`。

**边界 7 — 名片有 credential，菜单无 credential 键。** web-domain 测试：`componentKey.includes('credential') === false`；`third-provider` 权限组含 `third:credential:add`。

**边界 8 — 软删除与停用。** 厨房 `delete*` 只打 remove URL，不先替你打 status。后端 Provider/Endpoint 删除前必须已停用（L-060 / L-061）；失败文案从后窗回。厨房不预检。

**边界 9 — `CredentialForm.enabled` 是 boolean，摘要 `enabled` 是 string。** 保存时页面传布尔，对齐 `ThirdCredentialBo.enabled`。不要把摘要的 `'0'` 直接塞回 Bo。

**边界 10 — 厨房不读 `third.crypto.masterKey`。** 主密钥在服务器。Lesson 正文不编造任何 secret 样例。

## 变式与迁移

1. **和 `createNotificationService` 对照。** 通知厨房嵌套五块，另有厅堂 directory，且有备用枪（`snapshot` / `seen` / `config.account`）。三方厨房扁平十五枪、无 directory、无备用枪，缺口是后端 `/save`。不要把「domain 工厂」说成同一种抽屉形状。
2. **和 `createOpenApiService` 对照。** OpenAPI 有两扇柜台、`NO_STORE`、fail-closed 投影、路径段正则。三方厨房是扁平填表：无投影、无禁缓存头、保存不剥多余字段。凭据明文只在 POST body 活一次，但闸在后端不回显，不在前端投影器。
3. **和 `createSystemService` 对照。** system 八抽屉仍有 PUT/DELETE 存量。三方厨房从第一天就遵守「变更 POST」（API-005 方向）。不要把 `users.remove` 的 DELETE 习惯套到 `deleteProvider`。
4. **和 L-007 对照。** 组合点课只要求你能指着「`createThirdService` 一行，注入 `domainHttp`」。本课要求能把 15 枪 URL 背到 method，并指出两扇没接的窗。不要把 L-007 的接线板图当成已经 covered 的方法表。
5. **和 L-060…L-063 对照。** 后端空关键字 like、删前停用、凭据加密、观测 7 天/200 条，本课不重讲。前端保证：四族前缀、POST remove/status、保存键焊在 add 窗、路径 id 编码、摘要无秘密。
6. **和 L-064 对照。** 网关 `execute` 要 `providerCode` + `endpointCode` 点名快照。管理端改完配置靠后端 `evict`，不是厨房再打一枪「刷新网关」。不要在 `ThirdService` 上找 `reloadCache`。
7. **和 L-066 对照。** 菜单四键、一张 `ThirdPage`、凭据嵌套、工厂不校验 runtime，是下一课。本课若被问「谁在喊」，指 `runtime.service.*` 即可，不要把四键顺序标进本格。
8. **换 App。** 今天只有 admin-web 接线。以后若门户只要看回放：复制的是 `createThirdService(该厅堂 http)` 再只暴露观测两枪的用法，不是 import admin 的 `services.ts`（会串 `Admin-Token`）。工厂本身不会因为换 App 变瘦。
9. **要让「只有 edit、没有 add」的人能改正供应商。** 必须让厨房长出打 `POST /third/provider/save` 的方法（或让 `saveProvider` 按有无 id 选窗），再让页面编辑走那一枪。现在改 Vue 权限串解决不了 403——卡车开错窗。
10. **要加一条新的 `/third/*`。** 先改 `src/index.ts` 的 `ThirdService` 与工厂，再补 `index.test.ts`（最好覆盖 URL，不要只锁 remove）。若这条是出站 execute，不要做浏览器厨房方法。若这条需要嵌套抽屉，那是新形状，不要假装现有扁平接口已经有。
11. **观测要不要分页参数。** 当前厨房只传 `providerCode`。后端 list 不是真分页。不要在 Vue 里手写 `pageNum` 去「补」厨房。
12. **迁移口诀：** 先数包只有根 barrel → 再数 15 个扁平名字 → 再数四族前缀（观测没有 `observability` 这个词）→ 再数两扇没接的 `/save` → 再数测试只锁 5 枪 status/remove 与 `%2F` → 最后数厨房没有 execute。跳步会出现「把 save 说成 `/save`」「把网关画进厨房」「把凭据说成第五键」。

## 常见误区

1. **「`createThirdService` 在 `transport.ts`。」** 在 `src/index.ts`。
2. **「OBJ-65 包含 `createThirdWebDomain`。」** 那是 OBJ-66。
3. **「OBJ-65 包含 `ThirdPartyGateway.execute`。」** 那是 OBJ-64。
4. **「`saveProvider` 等于后端 `save` 方法那扇 `/save` 窗。」** 厨房打的是 `add` 那扇 `/third/provider`。
5. **「编辑走 PUT。」** POST 到新增窗，body 带 id。
6. **「删除用 HTTP DELETE。」** POST `/{id}/remove`。
7. **「`changeProviderStatus` 的 status 在 JSON body。」** 在 query `params`。
8. **「`service.provider.list` / `service.monitor.invocations`。」** 扁平：`listProviders`、`listInvocations`。
9. **「观测门牌是 `/third/observability`。」** 是 `/third` + `/invocation|statistics/list`。
10. **「厨房会加密 `secretJson`。」** 不会。只运输。加密在 L-062。
11. **「列表能再看一次明文。」** `CredentialSummary` 没有该字段；没有 get。
12. **「home-web 也有三方厨房。」** 本课核对：无工厂、无单例。
13. **「`capabilities` 有 `third-credential` 所以有凭据页。」** 名片不是菜单。键不存在。
14. **「页面自己拼 URL。」** 厨房才拼。
15. **「本课覆盖四 `kind` 的按钮剧本。」** 那是 L-066。本课认 URL 与谁注入谁。
16. **「厨房测试锁死了全部 15 枪。」** 只锁 status/remove 五枪。
17. **「`id()` 证明生产主键可以含 `/`。」** 后端是 `Long`。测试只锁编码。
18. **「`listInvocations` 不填供应商就不发车。」** 会发，`providerCode` 可能是空串。
19. **「启用状态 `'0'` 和凭据摘要 `enabled: '0'` 是同一个字段。」** 前者是 Provider/Endpoint 的 `status`；后者是 Vo 上的 `delFlag` 别名。
20. **「看见 OpenAPI 有 `/third/provider/save` 厨房就有。」** 标签不是方法表。方法表以 `index.ts` 的 freeze 对象为准。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/domains/third/src/index.ts`。圈 `export interface ThirdService` 的 15 个方法名、`thirdDomainModule.capabilities` 四项、`id()` 的 `encodeURIComponent`、`Object.freeze`、内部 `call` 只有 `http.request`。顺着 Provider / Endpoint / Credential / Observability 把 URL 和 method 点完。圈 `saveProvider` / `saveEndpoint` 的 `'/third/provider'`、`'/third/endpoint'`——确认**没有** `'/save'`。确认文件里**没有** `execute`、**没有** `'delete'`、**没有** Vue import。
2. 打开同包 `package.json`。圈 `name` `@namewta/domain-third`、`exports` 只有 `"."`、`dependencies` 只有 `platform-app-runtime` 与 `platform-contracts`。打开 `AGENTS.md`，圈「does not import Vue」和权限形状 `third:<resource>:<action>`。
3. 打开 `src/index.test.ts`。圈五个期望 URL，尤其 `provider%2F1`。数一数：这个 `it` 没有碰到 list / get / save / 观测。
4. 打开 `apps/admin-web/src/application/services.ts` 第 9、91 行。圈工厂从 `@namewta/domain-third` 进，导出 `thirdService`，只注入 `domainHttp`。在 `frontend/apps/home-web` 与 `frontend/apps/sso-web` 搜 `createThirdService`，确认无匹配。
5. 打开 `adminManifestRegistry.ts` 第 245–254 行与 `selectedManifestIds` 里的 `'web-domain-third'`。圈 runtime 只有 `service` / `confirm` / `success` / `error`，**没有** directory。打开 `web-domains/third/src/index.test.ts`，圈四键顺序和 `credential` 键为 false。
6. 打开 `ThirdPage.vue` 的 `load` / `save` / `toggleStatus` / `openCredentials`。圈编辑保存仍喊 `saveProvider` / `saveEndpoint`；凭据 `listCredentials` 在供应商行不传 endpointCode、在接口行传；`replaceCredential` 清空 `secretJson`。打开四个后端 Controller 与 `openapi.ts` 的 17 条 `/third/*` path，把厨房没有的两扇 `/save` 圈出来。

## 总结、词汇表与下一步

- **宏观四本表格：** 一座扁平厨房 `createThirdService` 打供应商、接口、凭据、观测。不是嵌套抽屉，不是第五本凭据菜单，不是出站电话。
- **(a) `createThirdService`：** 只认 `HttpClient`，冻住 15 枪。前缀 `/third/provider|endpoint|credential`，观测两枪在 `/third/invocation|statistics/list`。读 GET，写 POST。路径 id 编码。无 Vue、无投影闸、无 execute、无 `/save` 方法。
- **十五对十七：** OpenAPI 多两扇 `POST .../save`。厨房保存键焊在 add 窗。凭据族三对三。观测两对两。
- **一张脸：** 菜单页经 `runtime.service`。没有铃铛旁路。home / sso 无插座。
- **测试窄、源码宽：** 单元测试只锁 status/remove 与 `%2F`。口试以 `index.ts` 方法表为准。
- **不是网关，不是菜单工厂。**

词汇表：`createThirdService` / `ThirdService` / `thirdService` / `thirdDomainModule` / Provider / Endpoint / `CredentialSummary` / `CredentialForm` / Invocation / Statistic / `ApiResponse` / `id()` / `call` / `web-domain-third` / `ThirdPage` kind / `Admin-Token` / `ThirdPartyGateway`。

下一步：供应商六扇与路径安全是 OBJ-60（child L-001）。Endpoint 点名与 schema 安检是 OBJ-61。凭据加密、列表不回显、删除是 OBJ-62。invocation/statistics 只读查询是 OBJ-63。出站网关是 OBJ-64。四键菜单与嵌套凭据对话框如何消费 `runtime.service` 是 OBJ-66。厅堂插头是 OBJ-07。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web,sso-web}` | 工厂单例只在 admin-web `services.ts`；home/sso 无匹配 | `application/services.ts` 第 9、91 行；home/sso 无 `createThirdService` | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}/third` | 厨房一份 index；web-domain 四键与 runtime 口 `service` | 各包 `package.json` 与 `src` | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 菜单键 `third/{provider,endpoint,invocation,statistics}/index`；凭据只有 F 型挂在供应商页下 | `NAMEWTA-THIRD-MENU-DML-001` 段 | 2026-09-17 |
| S-014 | child `2026-09-14-wta-third` course / INDEX | 原料课五切片把前端划出范围；本 Goal 用 L-065/L-066 补厨房与菜单 | child `course.md`「范围外」；父 `goal-plan.md` / `chain.md` | 2026-09-17 |
| S-L007-01 | `apps/admin-web/src/application/services.ts` | 工厂调用、只注入 `domainHttp`、排在 monitor/ai 之后 | 第 9、91 行 | 2026-09-17 |
| S-L065-01 | `packages/domains/third/src/index.ts` | 15 枪 URL/动词/扁平形状；freeze；`id()`；无 execute；save 不打 `/save`；名片 capabilities | `createThirdService` 全文 | 2026-09-17 |
| S-L065-02 | `src/index.test.ts`；`package.json`；`AGENTS.md` | 五枪 status/remove 与 `%2F`；exports 只有 `.`；无 Vue、无 api-contracts | 一个 `it`；`exports`；AGENTS 首段 | 2026-09-17 |
| S-L065-03 | `web-domains/third/src/{index.ts,index.test.ts,runtime.ts,ThirdPage.vue}` | runtime 只要 `ThirdService`；四键无 credential；15 枪都有 Vue 调用；编辑仍 `saveProvider` | 测试第一例；`load`/`save`/`openCredentials` | 2026-09-17 |
| S-L065-04 | `apps/admin-web/src/router/adminManifestRegistry.ts` | `{ service: thirdService }` 注入；`selectedManifestIds` 含 `web-domain-third` | 第 245–254、375 行 | 2026-09-17 |
| S-L065-05 | `ThirdProviderController.java`；`ThirdEndpointController.java` | 各六扇；add=`POST /`，save=`POST /save`；status 用 `@RequestParam`；remove 为 POST | 四个 `@RequestMapping` 方法 | 2026-09-17 |
| S-L065-06 | `ThirdCredentialController.java`；`ThirdCredentialBo.java`；`ThirdCredentialVo` 形状（子课 L-003） | 三扇；更新也走 POST `/` + add 权限；Bo `secretJson` 必填、`enabled` 布尔；列表无秘密 | Controller 三方法；Bo 字段 | 2026-09-17 |
| S-L065-07 | `ThirdObservabilityController.java`；子课 L-004 | `@RequestMapping("/third")`；两 GET；`providerCode` 可选；无 POST | `invocations` / `statistics` | 2026-09-17 |
| S-L065-08 | `frontend/packages/api-contracts/generated/openapi.ts` 的 `/third/*` | 17 条 path，含厨房未接的 `/third/provider/save` 与 `/third/endpoint/save`；无 gateway path | path 键 1134–1287 与 3924–4026 段 | 2026-09-17 |
