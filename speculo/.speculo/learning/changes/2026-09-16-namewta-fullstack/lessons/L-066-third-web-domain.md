---
lesson_id: L-066
objective_ids: [OBJ-66]
claimed_cells:
  - A:createThirdWebDomain
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: factory-and-runtime
    minutes: 8
  - segment: pages-consume-service
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-010, S-014, S-L066-01, S-L066-02, S-L066-03, S-L066-04, S-L066-05, S-L066-06, S-L066-07, S-L066-08]
---

# Lesson 066：宏观四键三方面板——`createThirdWebDomain` 如何消费 `thirdService`

## 学完你能做什么

打开厅堂菜单工厂 `frontend/packages/web-domains/third/src/index.ts` 的 `createThirdWebDomain`，再打开同一张 `ThirdPage.vue` 和 runtime 托盘，你能**口述浏览器怎么把三方配置纸条贴到墙上**：服务员（Vue）只对托盘喊 `runtime.service.listProviders` / `saveEndpoint` / `listCredentials` / `listInvocations` 这类方法名，地址写在厨房 `createThirdService`。不是 OpenAPI 入站目录（L-033），不是 `ThirdPartyGateway.execute` 出站大门（L-064），也不是「厨房被裁过所以没有凭据页」。

口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

1. **`A:createThirdWebDomain`**（包 `@namewta/web-domain-third`）：工厂返回冻住的 `WebDomainManifest`。id 是 `web-domain-third`，`domainId` 是 `'third'`。磁盘上正好 **4** 条 `componentKey`（测试锁死顺序）：`third/provider/index`、`third/endpoint/index`、`third/invocation/index`、`third/statistics/index`。它**不** `addRoute`，**不**读 `Admin-Token`，**不**自己拼 `/third/*` 字符串。四键外包的是**同一张** `ThirdPage`，用 `kind` 分成四种门脸。

OBJ-66 还要你顺着四张门脸，把厨房方法喊到人脸上：

- **供应商** `kind === 'providers'`：`listProviders` / `getProvider` / `saveProvider` / `changeProviderStatus` / `deleteProvider`；凭据抽屉叠在行上。
- **接口** `kind === 'endpoints'`：`listEndpoints` / `getEndpoint` / `saveEndpoint` / `changeEndpointStatus` / `deleteEndpoint`；凭据抽屉同样叠在行上，只是会带上 `endpointCode`。
- **调用明细** `kind === 'invocations'`：只喊 `listInvocations(providerCode)`。只读，没有新增/编辑/删除。
- **调用统计** `kind === 'statistics'`：只喊 `listStatistics(providerCode)`。只读。
- **凭据（嵌在供应商/接口行里，不是第五键）**：`listCredentials` / `saveCredential` / `deleteCredential`。列表 VO 没有明文；替换时 `secretJson` 先清空。

2026-09-17 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `@namewta/web-domain-third-provider` 四个包 | **没有。** 一个包 `@namewta/web-domain-third`，`package.json` `exports` 只有 `"."` |
| `third/credential/index` 第五键 | **没有。** 测试锁死 `componentKey` 不含 `credential`；权限串 `third:credential:*` 挂在 `third-provider` 那一组 |
| `createSystemWebDomain` 的三方键 | **没有三方键。** OpenAPI 是 `system/openApi/index`（L-033） |
| `runtime.service.execute` / `invoke` | **没有。** `ThirdService` 封面没有出站方法；大门是 Java `ThirdPartyGateway`（L-064） |
| 页面 `import { thirdService } from '@/application/services'` | **这张 web-domain 页不。** 厅堂把单例塞进 `runtime.service` |
| home 对称一份 `createThirdWebDomain` | **没有。** `apps/home-web` 搜不到这个工厂，也搜不到 `thirdService` |
| `createThirdWebDomain(undefined)` 会像档案楼那样当场关门 | **2026-09-17 没有 `requireThirdWebRuntime`。** 测试把 `service: {} as never` 传进去也能冻出四键 |
| 四张 Vue 文件 | **没有。** 只有 `ThirdPage.vue`；工厂用 `view(name, kind, runtime)` 包四次 |

本课**不宣称**你会拆 `createThirdService` 的 URL 全表与 `encodeURIComponent`（L-065）、五扇 Java 窗（父课 L-060…L-064 / 子课 third L-001…L-005）、厅堂 `services.ts` 十二个工厂（L-007）、或把九个 web-domain 工厂一行标 covered（GP-L-020）。今天只认：**管理端这份三方菜单工厂怎样把 `thirdService` 喂进四张门脸，门脸实际扣了哪几枪，墙上没挂、厨房也没印的出站大门在哪。**

矩阵 (a) 那一行把九个工厂写在一起。本课只给 **`createThirdWebDomain` 这一颗**当证据，**不要**把整行九厂标成 covered。

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`export const thirdService = createThirdService(domainHttp)`。L-020 讲导航 host 怎样用 `sys_menu.component` 对上 `registration.load`。L-033 是**另一栋楼**的入站 OpenAPI 工作区。子课 `children/2026-09-14-wta-third/` 把后端五切片走完；本 Goal 把它们升级成 L-060…L-064，厨房是 L-065，菜单是本课。本课站在**已经登录的管理员浏览器**这一头。

三条河都叫 third，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `createOpenApiService` / `system/openApi/index` | 入站 OpenAPI 目录与凭据（L-033） | 邻居：不要走错楼 |
| `createThirdService` | 浏览器三方厨房：供应商 / 接口 / 凭据 / 观测两枪 | **厨房是 L-065**；本课认页面怎么喊它 |
| `ThirdPartyGateway.execute` | 业务模块出站大门；写 invocation / statistic | **L-064**；本工厂页面**不能**打电话 |
| `createThirdWebDomain` | 管理端四键菜单 | **本课格子** |
| `ThirdProviderController` 等五扇 | 后端 layered | L-060…L-064 |
| `thirdDomainModule.capabilities` | 名片四字：`third-provider` / `third-endpoint` / `third-credential` / `third-observability` | 名片，**不是** componentKey |

2026-09-17 工作树：权威厨房是 `frontend/packages/domains/third/src/index.ts` 的 `createThirdService`。权威菜单包是 `frontend/packages/web-domains/third/`。厅堂接线是 `apps/admin-web/src/application/services.ts` 第 91 行与 `apps/admin-web/src/router/adminManifestRegistry.ts` 第 245–254 行。`selectedManifestIds` 含 `'web-domain-third'`。home **不**选这份 id，也**没有** `third` domain。

菜单种子在 `50-cde-base-dml.sql` 块 `NAMEWTA-THIRD-MENU-DML-001`：目录 `三方接口管理`（`menu_id` `2100700000000000001`）下四张 C 型页，外加一批 F 型按钮。凭据三串 F 型挂在**供应商**页下面，**没有** C 型 `third/credential/index`。`menu_id` 从 `…0012`（接口）跳到 `…0014`（调用明细），磁盘上没有 `…0013` 那张凭据页。

```text
已登录的管理员（Admin-Token）
        │
        ├─ 厅堂厨房
        │     thirdService = createThirdService(domainHttp)
        │
        └─ 菜单工厂
              createThirdWebDomain({
                service: thirdService,     ← 本课消费点
                confirm, success, error    ← 可选宿主口；页面用 ?.
              })
                    id = web-domain-third
                    键：provider / endpoint / invocation / statistics
                    凭据：嵌在 provider/endpoint 行上，不是第五键
```

往下走不要跳层：

```text
Vue ThirdPage（只收 runtime + kind）
    └─ web-domain 外包一层 h(ThirdPage, { runtime, kind })
          └─ domain 工厂拼 URL / method
                └─ App 的 domainHttp（axios + Admin-Token）
                      └─ 后端 Third*Controller（L-060…L-063）
```

出站打电话**不**走这条河：

```text
业务模块
    └─ ThirdPartyGateway.execute   （L-064；厨房对象上没有）
          └─ 记录 invocation / statistic
                └─ 管理端只读页事后刷 listInvocations / listStatistics
```

**类比失效边界：** 「宏观四键菜单」**不**等于「厨房里只有这四道菜」。厨房还有凭据三枪，墙上不单开一页。类比也**不**等于「home 选了 `web-domain-third` 就会少两道菜」——home 根本不选这份 id，连厨房单例都没造。类比还不等于「capabilities 有 `third-credential` 所以一定有凭据菜单键」——名片有能力，工厂可以不挂第五键。今天就是这样。类比更**不**等于「点保存就是打出站电话」——保存只改配置；真正出门的是网关。

## 核心概念与机制

### 直觉讲解

把 `createThirdWebDomain` 想成**电话总机墙上的点菜单**，不是后厨本身，更不是去别家公司敲门的那扇大门。

- **点菜单（`createThirdWebDomain`）**：四行：供应商名册、接口名册、通话回放、按日计数。每一行写死 `componentKey`，load 时用 `h(ThirdPage, { runtime, kind })` 把托盘和门脸种类一起塞进**同一张**页。页面自己**不** import 厅堂单例。
- **托盘（`ThirdWebRuntime`）**：必带 `service`（厨房）。可选三口：`confirm` / `success` / `error`。没有 `hasPermission`、没有 `dicts`、没有 `directory`、没有 `navigate`。
- **厨房（`runtime.service`）**：服务员只喊「来一份 list / save / delete」。地址印在 `createThirdService`：`/third/provider`、`/third/endpoint`、`/third/credential`、`/third/invocation|statistics`。
- **抽屉里的保险箱（凭据对话框）**：不是第五行点菜单。供应商行和接口行都可以拉开。柜台只展示盒号；`secretJson` 保存时加密，列表不回显。

小孩子版只记十二句：

1. **先找工厂，再找四键。** `export function createThirdWebDomain(runtime: ThirdWebRuntime)`。封面 id 永远是 `web-domain-third`。
2. **四键不是随便排。** 测试锁死：provider → endpoint → invocation → statistics。
3. **四键一张 Vue。** `kind` 才把门脸分开；不要去找 `ProviderPage.vue`。
4. **厨房从托盘进门，不从 `@/application/services` 进门。** 页只认 `props.runtime`。
5. **凭据不是第五键。** 测试断言 `componentKey` 不含 `credential`，同时又锁 `third:credential:add` 在 provider 权限组里。
6. **新增和编辑在页上是两颗按钮，厨房却是同一枪 `saveProvider` / `saveEndpoint`。**
7. **观测两页不能打电话。** 只有 list；写入发生在网关，不在这本点菜单。
8. **按钮藏起来靠 `v-hasPermi`，不是 `runtime.hasPermission`。** 托盘上根本没有这口。
9. **工厂今天不检查托盘齐不齐。** 缺方法会在 `onMounted` 时炸，不会在 `createThirdWebDomain` 当场 throw。
10. **`confirm` 是可选口。** 写成 `runtime.confirm?.(...)`；厅堂没塞时，删除不会先弹确认框。
11. **OpenAPI 凭据不是三方凭据。** 入站目录在 system 楼。
12. **出站 `execute` 不在这本点菜单上。** 种子里也没有「试调用」按钮。

**类比失效边界：** 点菜单类比**不**覆盖「密钥字节怎么进 KEK」。保存仍走厨房 `saveCredential`，后端再进 `ThirdCredentialCryptoPort`（L-062）。类比也**不**等于「墙上没挂 execute 厨房就把刀收了」——厨房本来就没有这把刀。类比还不等于「`ThirdPage.vue` 是网关柜台」——那张 Vue 只改配置、刷回放。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 管理端三方菜单工厂 | `createThirdWebDomain` | `web-domains/third/src/index.ts`；manifest id `web-domain-third` |
| 菜谱板 | `WebDomainManifest` | `id` / `domainId` / `permissions` / `registrations` / `messages` |
| 组件键 | `componentKey` | 与 `sys_menu.component` 对表：`third/provider/index` 等 |
| 运行时托盘 | `ThirdWebRuntime` | `src/runtime.ts`；`service` 类型是 `ThirdService` |
| 三方厨房 | `thirdService` | 厅堂导出名；工厂 `createThirdService` 在 `domains/third/src/index.ts` |
| 门脸种类 | `kind` | `'providers' \| 'endpoints' \| 'invocations' \| 'statistics'` |
| 外包视图 | `view(name, kind, runtime)` | `defineComponent({ name, setup: () => () => h(ThirdPage, { runtime, kind }) })` |
| 权限目录 | permissions catalog | 工厂四组 id：`third-provider` / `third-endpoint` / `third-invocation` / `third-statistics` |
| 凭据摘要 | `CredentialSummary` | 列表：类型、范围、KEK 版本、过期；**没有** secret |
| 宿主端口 | host port | 可选 `confirm` / `success` / `error` |
| 指令闸 | `v-hasPermi` | 厅堂 `installWebPermissionHost`；**不是** runtime 字段 |
| 失败关闭 | fail-closed | 档案楼 `requireProfileWebRuntime` 那套；**本工厂 2026-09-17 没有** |
| 出站大门 | `ThirdPartyGateway.execute` | `wta-api`；厨房与 Vue **都没有** |

厨房对象形状（L-065 的封面，本课用来对扳机）：

| 厨房字段 | HTTP 门牌（动词以 transport 为准） | 本课门脸用不用 |
| --- | --- | --- |
| `listProviders` / `getProvider` / `saveProvider` / `changeProviderStatus` / `deleteProvider` | `/third/provider/*` | **用。** `kind=providers` |
| `listEndpoints` / `getEndpoint` / `saveEndpoint` / `changeEndpointStatus` / `deleteEndpoint` | `/third/endpoint/*` | **用。** `kind=endpoints` |
| `listCredentials` / `saveCredential` / `deleteCredential` | `/third/credential/*` | **用。** 嵌套对话框 |
| `listInvocations` | `GET /third/invocation/list` | **用。** `kind=invocations` |
| `listStatistics` | `GET /third/statistics/list` | **用。** `kind=statistics` |
| `getCredential` | — | **厨房无方法** |
| Controller `POST /third/provider/save`、`POST /third/endpoint/save` | 编辑权限口 | **厨房无方法**；页上编辑仍喊 `saveProvider` / `saveEndpoint` |
| `ThirdPartyGateway.execute` | 出站，非 `/third/*` 管理窗 | **厨房无方法；无按钮** |

`thirdDomainModule.capabilities` 有 `'third-credential'`。这是名片，**不是** componentKey，也不决定 App 挂哪几页。

### 机制/因果链

**1. 工厂把四键冻进同一本菜单，一张 Vue 演四角，但不检查托盘。**

`createThirdWebDomain(runtime)` 立刻 `Object.freeze` 一份 manifest。`registrations[].load` 闭包住这份 `runtime` 和 `kind`，再 `defineComponent({ name, setup: () => () => h(ThirdPage, { runtime, kind }) })`。测试锁死四键顺序，并断言**没有任何** `componentKey` 含 `credential`，同时 `third-provider` 那组权限**含** `third:credential:add`。把 `service: {} as never` 传进去**也能**得到四键——缺方法要等页面 `onMounted` 才爆。对照 L-044：档案楼会 `requireProfileWebRuntime`，缺十一口 archive 当场 throw。口试不要把两座工厂的关门策略背成一句。

权限目录四组（字符串以工厂为准）：

| id | 冻进去的串 | 页上按钮实际核对 |
| --- | --- | --- |
| `third-provider` | list / query / add / edit / remove，外加 `third:credential:list|add|remove` | 新增、编辑、启停（edit）、删除；凭据三串 |
| `third-endpoint` | list / query / add / edit / remove | 同上，**不含** credential 串 |
| `third-invocation` | `third:invocation:list` | 只读；无新增按钮 |
| `third-statistics` | `third:statistics:list` | 只读；无新增按钮 |

`composeAppRuntime` 只有 `selectedManifestIds` 含 `web-domain-third` 时才把键交给导航。admin 厅堂这份 id 在 `adminManifestRegistry.ts` 第 375 行；`selectedDomainIds` 含 `'third'`。home **不**选它。registry 测试另锁：`third/provider/index` + `domainId: 'third'` 的 `componentName` 是 `ThirdProvider`。

消息贡献只有 `namespace: 'thirdAdmin'`、`title: '三方接口管理'`。页面标题不读它，写死在 `ThirdPage` 的 `config` 里。

**2. 厅堂托盘把厨房和弹窗从页面 import 里拆出去；授权指令不走 runtime。**

`adminManifestRegistry.ts` 第 245–254 行：

| runtime 口 | 厅堂接到哪 | 门脸怎么用 |
| --- | --- | --- |
| `service` | `thirdService` | 所有 HTTP 方法 |
| `confirm` | 动态 import `@/application/host/feedback` 的 `modal.confirm` | 删除配置 / 删除凭据 |
| `success` | `modal.msgSuccess` | 保存配置 / 保存凭据 |
| `error` | `modal.msgError` | 保存失败、启停失败、打开凭据失败 |

口试不要说「页面直接 `import thirdService`」。也不要说「`runtime.hasPermission('third:provider:add')`」——接口上没有这口。按钮用全局指令 `v-hasPermi="[permission + ':add']"`，尺来自厅堂 `createAdminAccessEvaluator` + `installWebPermissionHost`（L-020）。权限目录冻在 manifest 里，给 `permissionContributions()` 当元数据，**不是** `v-hasPermi` 的运行时数据源；运行时数据源是 `getInfo` 带回的权限串。

`confirm` / `success` / `error` 都是可选。删除写成 `await props.runtime.confirm?.('确认删除当前配置？')`：厅堂没塞时，`?.` 得到 `undefined`，删除**照样往下走**。保存失败才 `error?.`；`load()` **没有** `catch`——列表失败会变成未捕获拒绝，`finally` 仍会关掉 loading。

**3. 一张页按 `kind` 分支，筛选框名字会骗人。**

`ThirdPage` 一个 `config` 表：

| kind | 标题 | permission 前缀 | editable | 列 |
| --- | --- | --- | --- | --- |
| providers | 三方供应商 | `third:provider` | true | 编码 / 名称 / Base URL |
| endpoints | 三方接口 | `third:endpoint` | true | 供应商 / 接口编码 / 方法 / 路径 |
| invocations | 调用明细 | `third:invocation` | false | 请求 ID / 接口 / 状态 / 失败分类 / 耗时 |
| statistics | 调用统计 | `third:statistics` | false | 接口 / 日期 / 次数 / 成功 / 失败 / 限流拒绝 |

查询表单四个门脸都有「供应商」输入，绑定的却是 `providerCode`。真正进厨房时：

- providers：`listProviders(providerCode)` → 厨房当 `keyword`（后端对编码**或**名称 like）。
- endpoints：`listEndpoints(providerId || undefined, providerCode || undefined)`；多一个「供应商 ID」框。
- invocations / statistics：`listInvocations(providerCode)` / `listStatistics(providerCode)`。空字符串也会传进去。

没有分页组件。厨房返回 `response.data ?? []`，后端是 `List`，不是表格分页对象。HTTP 方法、请求/响应模式写死在页里：`GET|POST|PUT|PATCH|DELETE`，`JSON|QUERY|FORM`，`JSON|TEXT|BYTES`。没有 `runtime.dicts`。

**4. 供应商/接口：编辑先 get，保存一律 save*，启停走 status，删除走 remove。**

`editable === true` 时才有新增、编辑、删除、启停开关。

- 列表：见上。
- 新增：`resetForm()` 后打开对话框。供应商编码/接口编码在编辑时 disabled。
- 编辑：先 `getProvider(providerId)` 或 `getEndpoint(endpointId)`，再 `Object.assign(form, value.data ?? row)`。这枪对应后端 `third:provider:query` / `third:endpoint:query`。按钮只核 `:edit`。
- 保存：`kind === 'providers'` → `saveProvider(form)`，否则 `saveEndpoint(form)`。对话框底部：编辑中核 `:edit`，新增核 `:add`，**两颗按钮喊的是同一枪厨房方法**。厨房把这一枪打到 `POST /third/provider` 与 `POST /third/endpoint`（Controller 的 **add** 口，权限 `*:add`）。Controller 另有 `POST .../save`（权限 `*:edit`），`ThirdService` **没有**对应方法。口试不要把「页上写着保存、按钮核 edit」说成「HTTP 已经打到 `/save`」。URL 全表留给 L-065；本课只认：**页面消费的是 `saveProvider` / `saveEndpoint` 这一对名字。**
- 启停：开关 `model-value="scope.row.status === '0'"`（`'0'` 启用，`'1'` 停用）。`changeProviderStatus(id, status)` / `changeEndpointStatus`。失败会 `error` 再 `load()`。
- 删除：可选确认后 `deleteProvider` / `deleteEndpoint`。**没有**成功 toast，**没有** `try/catch`。

`resetForm` 里还有 `pathSchemaJson` / `responseSchemaJson` / `adapterCode` / `credentialId`。供应商/接口对话框**没有**这些输入。新增会把空串一并 POST；编辑会把 get 回来的值原样送回。

**5. 凭据嵌在行上：有摘要、无第五键、无 get-by-id、无明文回显。**

供应商行和接口行都有「凭据」按钮，核 `third:credential:list`。打开时：

- `providerCode` 抄自当前行；
- 从接口行进来才填 `endpointCode`，从供应商行进来留空（供应商范围）；
- `secretJson` 清空；`enabled` 默认 `true`；
- `listCredentials(providerCode, endpointCode || undefined)`。

表格只展示 `credentialType` / `scopeType` / `kekVersion` / `expiresAt`。`scopeType` **没有**表单控件，是后端填的。替换按钮核 `third:credential:add`：抄 `credentialId` / 类型 / `enabled === '0'`，**再次清空** `secretJson`。没有 `getCredential`——厨房也没有这支枪。保存 `saveCredential(credentialForm)`，成功后再清 `secretJson`。删除核 `third:credential:remove`。

种子把凭据三串 F 型挂在供应商菜单（`parent_id` `…0011`）下，不挂接口菜单。工厂也把这三串冻进 `third-provider` 组，**不**冻进 `third-endpoint` 组。页面却在接口行同样画「凭据」按钮——有 `third:credential:list` 的人从接口页也能拉开抽屉。不要把「权限目录组名」说成「只有供应商页才有 DOM」。

e2e `frontend/e2e/third-party-management.spec.ts` 锁：打开凭据对话框时 `凭据 JSON` 值为空；全文搜不到 `raw-secret`；list-only 权限下新增/编辑/删除/开关消失，凭据仍可打开但保存/替换/删除/JSON 框都没有。

**6. 观测两页只刷 list，文件名像监控，扳机却打不出去。**

`kind` 为 invocations / statistics 时 `editable === false`：没有新增、没有操作列、没有启停、没有凭据。`load()` 只喊 `listInvocations` / `listStatistics`。没有日期范围、没有 200 条上限的输入框——那是后端 L-063 的查询窗；前端把厨房给的数组直接铺表。

不要把这两页说成「试调用柜台」。出站写入在 `ThirdInvocationRecorderAdapter`（L-064）。管理读路径从不 insert。页上也没有 `execute` 按钮。

**7. 厨房方法这张页几乎扣完；更外面还有根本没写进厨房的窗。**

2026-09-17 `ThirdService` 封面 16 支枪，`ThirdPage` **都有扳机**。本课和 L-054 相反：剩的不是「厨房有、页面无」，而是「后端还有、厨房没收」。

厨房对象上**根本没有**：`POST /third/provider/save`、`POST /third/endpoint/save`、凭据 get-by-id、`ThirdPartyGateway.execute`。不要把「Controller 有 `/save`」说成「Vue 编辑走 `/save`」。

**8. 四颗 C 型种子，凭据只做 F 型。**

`50-cde-base-dml.sql`：

- 目录 `三方接口管理` `2100700000000000001`，path `third`，component 空（M 型）。
- 四张 C 型：`third/provider/index`、`third/endpoint/index`、`third/invocation/index`、`third/statistics/index`，perms 分别是对应的 `:list`。
- F 型：供应商 query/add/edit/remove；接口 query/add/edit/remove；凭据 list/add/remove（父菜单是供应商页）。观测两页**没有** F 型 query。

两颗种子都能让 `getRouters` 吐出同一 `componentKey`，registry 对上同一份外包组件。不要发明第二张 Vue，也不要把 F 型「凭据查询」说成已经挂了 `third/credential/index`。

### 图、表或文本图

**图 1：宏观四键怎样吃到厨房**

```text
 sys_menu.component（管理端 Client）
   third/provider/index
   third/endpoint/index
   third/invocation/index
   third/statistics/index
   （F 型 third:credential:* 没有 component，不上图）
        │
        v
 GET /system/menu/getRouters        已按人 + Client 裁剪（L-020）
        │
        v
 App composeAppRuntime
   selectedManifestIds 含 web-domain-third
        │
        ├─ 键在已选 manifest → registration.load
        │     defineComponent({ name, setup: () => () => h(ThirdPage, { runtime, kind }) })
        └─ 键不在已选清单 → 解析失败关闭
           third/credential/index → 没有 registration（测试锁死不含 credential）
                │
                v
         ThirdPage props.runtime + props.kind
                │
                ├─ runtime.service = createThirdService(...)   厨房
                ├─ confirm / success / error                   可选弹窗
                └─ 页面只喊方法名，不拼 URL；按钮走 v-hasPermi
```

**图题 / caption：** 宏观同一厨房、管理端四键、一张 Vue 四个 kind、凭据嵌在行上。alt：菜单键来自 sys_menu；admin 选 `web-domain-third`；页面只收到 runtime 和 kind；HTTP 字符串在 domain 工厂。

**文字等价物：** 人先碰到动态菜单里的 component 字符串。App 用已选 manifest 把字符串换成带 runtime 的 Vue 页。四张三方门脸面向 `runtime.service` 喊方法；凭据不另开路由。厨房把方法换成 GET/POST 和 `/third/...`。home 即使以后有人把厨房单例造出来，墙上没有这四键，导航也不会挂页。`third/credential/index` 这种把凭据当成第五张 C 型页的键，工厂明确没有。

**图 2：厨房有枪 / 本课页面扣扳机**

| 厨房方法 | HTTP（动词以 transport 为准） | 2026-09-17 谁扣扳机 |
| --- | --- | --- |
| `listProviders` / `getProvider` / `saveProvider` / `changeProviderStatus` / `deleteProvider` | GET list 与 `/{id}`；POST 集合根 / `/{id}/status` / `/{id}/remove` | **ThirdPage** `kind=providers` |
| `listEndpoints` / `getEndpoint` / `saveEndpoint` / `changeEndpointStatus` / `deleteEndpoint` | 同上，前缀 `/third/endpoint` | **ThirdPage** `kind=endpoints` |
| `listCredentials` / `saveCredential` / `deleteCredential` | GET list；POST 集合根；POST `/{id}/remove` | **凭据对话框**（供应商行或接口行） |
| `listInvocations` | GET `/third/invocation/list` | **ThirdPage** `kind=invocations` |
| `listStatistics` | GET `/third/statistics/list` | **ThirdPage** `kind=statistics` |
| Controller `POST /save`（provider/endpoint 编辑口） | `/third/provider/save`、`/third/endpoint/save` | **厨房无方法，无 Vue 直打** |
| 凭据 get-by-id / 明文回显 | — | **厨房无方法** |
| `ThirdPartyGateway.execute` | 出站 SPI | **厨房无方法，无 Vue** |

**图题 / caption：** 工厂注册了四键 ≠ 后端每一扇窗都有独立菜单页。alt：配置与观测的厨房方法都有扳机；凭据嵌套；`/save` 编辑口和出站 execute 不在厨房封面。

**文字等价物：** 管理员四键消耗供应商五枪、接口五枪、凭据三枪、观测两枪。凭据对话框叠在前两键的行上。Controller 为编辑另开的 `/save` 口，厨房没收，页面编辑仍喊 `save*`。出站 execute 连厨房封面都没印。不要把「工厂注册了四键」说成「所有 `/third/*` HTTP 都有独立页」，也不要把「厨房 16 支枪都有扳机」说成「后端每一扇窗都已从浏览器打到正确权限口」。

**图的边界：** 不画 Java 五层（L-060…L-064）。不画 KEK 信封字节。不保证以后产品会补凭据 C 型页或试调用按钮。不把 `capabilities` 名片画成菜单。不把 OpenAPI HMAC 会话当本课凭据源。不把 e2e 夹具里的示例供应商名当成生产密钥。

### 正例、反例与边界

**正例 1 — 管理员翻供应商名册。** 有 `third:provider:list` 的人打开 `third/provider/index`。页 `listProviders(providerCode)`。厨房 `GET /third/provider/list?keyword=...`。

**正例 2 — 编辑先拉详情。** 持 `third:provider:edit` 点「编辑」。页先 `getProvider(providerId)` → `GET /third/provider/{id}`（后端还要 `third:provider:query`）。供应商编码输入框 disabled。点保存喊 `saveProvider(form)`，不是第二个厨房方法。

**正例 3 — 启停开关。** 行 `status === '0'` 时开关亮。关掉 → `changeProviderStatus(id, '1')` → `POST /third/provider/{id}/status?status=1`。e2e 锁这一枪。失败会 toast 再刷新。

**正例 4 — 凭据嵌在供应商行。** 点「凭据」→ `listCredentials('qichacha')`。对话框标题「凭据管理」。JSON 框为空。保存走 `saveCredential`，成功后再把 JSON 清空。列表列没有 secret 字段。

**正例 5 — 从接口行开凭据。** 接口行把 `endpointCode` 填进表单。`listCredentials(providerCode, endpointCode)`。同一套对话框，不是第二张页。

**正例 6 — list-only 藏按钮。** 只有 `third:provider:list` 与 `third:credential:list` 时：新增/编辑/删除/开关消失；凭据仍可打开，但保存/替换/删除/JSON 框都没有。e2e 第二例锁死。

**正例 7 — 未授权路由失败关闭。** 菜单投影里没有接口页时，直接打开 `/system/third/endpoint` 看不到「三方接口」标题，也不会多打 `GET /third/endpoint/list`。这是导航 host（L-020），不是工厂自己 `addRoute`。

**正例 8 — 观测只读。** `listInvocations(providerCode)` → `GET /third/invocation/list`。页上没有「重放」或「试调用」。脱敏后的 JSON 若在行数据里，表格列当前**没挂** `sanitizedRequestJson`；e2e 仍断言页面文本不含 `raw-secret`。

**正例 9 — 四键外包组件名。** `third/provider/index` 的 `componentName` 是 `ThirdProvider`。另外三键是 `ThirdEndpoint` / `ThirdInvocation` / `ThirdStatistics`。它们都是 `view(...)` 包出来的壳，里面仍是 `ThirdPage`。

**反例 1 — 「四张 Vue `import { thirdService }`。」** 全包搜不到 `@/application/services`。

**反例 2 — 「`createThirdWebDomain` 等于九个 web-domain 工厂都 covered。」** 矩阵 (a) 把九个名字写在同一行。GP-L-020 / L-044 / L-054 已经挖过：不要整行盖章。本课只给三方这一颗。

**反例 3 — 「`ThirdPage.vue` 会 `execute` 出站。」** 那张页只改配置、刷回放。execute 是 L-064 的 Java 大门；厨房 facade 2026-09-17 没收这支枪。

**反例 4 — 「系统 OpenAPI 页就是三方接口页。」** OpenAPI 键是 `system/openApi/index` + `system`。真三方键是 `third/endpoint/index` + `third`。

**反例 5 — 「有凭据按钮就有 `third/credential/index`。」** 测试：`componentKey.includes('credential')` 为 false。

**反例 6 — 「替换凭据会先 get 再回显明文。」** 页把 `secretJson` 置空。厨房没有 get-by-id。后端列表 VO 本来就不含 secret（L-062）。

**反例 7 — 「`runtime.hasPermission`。」** 托盘没有这口。对照档案楼不要背错。

**反例 8 — 「`createThirdWebDomain(undefined)` 会给出空菜单。」** 今天不会在工厂里 throw。缺 `service.listProviders` 的失败发生在 `onMounted`。

**反例 9 — 「观测页能选日期、翻页、重放。」** 只有一个供应商编码框和一张表。

**反例 10 — 「home 的厨房没有 invocation。」** home **没有**这座厨房，也没有这份菜单工厂。不要用 L-039「同一厨房两份菜单」那句话套到 third——档案楼那句成立，是因为 home 真的调了 `createProfileService`。

**反例 11 — 「`capabilities` 有 third-credential 所以一定有凭据菜单键。」** 名片有能力，工厂可以不挂。今天就是这样。

**反例 12 — 「编辑保存打 `POST /third/provider/save`。」** 页面消费的方法名是 `saveProvider`；厨房这一枪的 URL 是集合根 POST。`/save` 是 Controller 另一扇窗，本课厨房封面没有。

**边界 1 — 工厂不校验 runtime ≠ 厨房方法不存在。** 缺 `listProviders` 仍能 `createThirdWebDomain`。页面一喊才会在运行时失败。

**边界 2 — `v-hasPermi` 藏按钮；HTTP 仍可能 403。** 编辑按钮只核 `:edit`，详情枪却要 `:query`。保存按钮在编辑态核 `:edit`，厨房却打 add 口。授权以后端 `@SaCheckPermission` 为准。

**边界 3 — `confirm?.` 缺席时删除不拦截。** 厅堂今天塞了 confirm；测试用的 `{ service: {} as never }` 没有。口试不要把「可选口」说成「工厂保证先确认」。

**边界 4 — `load()` 无 catch。** loading 会关；错误不是页内 toast。对照保存/启停那两条有 `error?.`。

**边界 5 — 厨房 id 插进 URL 前会 `encodeURIComponent`。** 这是 L-065 的枪。页面只传 `providerId`。口试不要发明「Vue 自己 `%2F`」，也不要说「三方厨房直插不编码」——transport 测试锁的是 `provider%2F1`。

**边界 6 — 状态码 `'0'`/`'1'` 与凭据 `enabled` 布尔不是同一套。** 供应商/接口行用字符串；凭据表单用 boolean（`ThirdCredentialBo.enabled` 也是 `Boolean`）。替换时 `enabled === '0'` 才勾上开关。

**边界 7 — 筛选框写「供应商」，providers 列表实际当 keyword。** 后端 like 编码或名称。不要把占位符「供应商编码」说成唯一匹配字段。

**边界 8 — 接口页能开凭据，权限目录却把 credential 串放在 provider 组。** DOM 和 catalog 分组不是同一张表。

**边界 9 — 匿名出站无页面。** 不要为 `ThirdPartyGateway` 找 `src/gateway/` 下的 Vue。web-domain 包只有 `ThirdPage.vue`。

**边界 10 — Client 作用域在厅堂 HTTP，不在工厂。** e2e 断言 `/third/*` 请求带管理端 `clientid`。工厂不读 Client。

## 变式与迁移

1. **和 L-065 对照。** 厨房认 `createThirdService` 的 URL、`encodeURIComponent`、16 支枪封面。本课认菜单工厂怎样把 `service` 放进 runtime，以及四张门脸扣了哪些扳机。不要把 URL 全表在本课再抄一遍；口试若被问「list 的 path」，指回 kitchen，不要指 Vue。
2. **和 L-007 对照。** 厅堂才 `export const thirdService = createThirdService(domainHttp)`。web-domain 包的 `package.json` **没有** axios，也没有 `@namewta/domain-system`。以后换厅堂，只要再实现 `ThirdService` 这 16 口，不必让三方页面 import App。
3. **和 L-020 对照。** 导航 host 仍是 `getRouters` → `resolveAdminWebRegistration` → `addRoute`。third 不另写一套路由恢复。键找不到就是诊断，不是 glob 扫 `views/third/`。
4. **和 L-033 对照。** OpenAPI 是入站目录/HMAC 凭据，manifest 走 system 工厂。三方是出站供应商/接口/凭据/观测，manifest id `web-domain-third`。两套「凭据」不要画成一个抽屉。
5. **和 L-044 / L-054 对照。** 都是「页面经 runtime 消费厨房」。差别：档案楼 fail-closed 校验十一口；通知楼工厂不校验、四张 Vue；三方楼工厂不校验、**一张 Vue 四个 kind**，托盘更瘦（没有 directory / dicts / hasPermission）。通知楼厨房有枪页面没扣；三方楼厨房封面的枪页面几乎扣完，缺的是厨房没收的 `/save` 与 execute。
6. **和子课 L-001 / 父课 L-060 对照。** 后端 `add` 与 `save` 是两扇 HTTP 窗、两种权限字，UseCase 都进 `save`。前端只有 `saveProvider` 一枪。不要把「对话框写保存」说成已经打到 `POST /save`。
7. **和子课 L-003 / 父课 L-062 对照。** 后端列表不回显明文、没有 get-by-id。前端替换先清空 JSON，和这条契约对齐。
8. **和子课 L-004 / 父课 L-063 对照。** 后端 invocation 近 7 天 / 最多 200 条。前端不把这些数字画成筛选器。
9. **和子课 L-005 / 父课 L-064 对照。** 出站只准走大门。本课四键是改通讯录和看回放，不是大门本身。
10. **以后若要凭据独立页。** 应再冻第五键并让 `kind` 多一个值，或拆组件；不要让页面直接打 `/third/credential`。种子今天没有 C 型 component 可对。
11. **以后若要试调用柜台。** 先让 L-065 的厨房长出 execute（或显式声明「浏览器不准出站」），再在 web-domain 加按钮。不要让 Vue 自己 new `fetch` 打 `baseUrl`。
12. **以后若要「只要观测」的瘦菜单。** 今天的工厂**会**把四键一起注册。不能靠「只用 statistics kind」让工厂少返回三键。要瘦，得改工厂或让 App 不选那些菜单种子。
13. **换 App。** 第三份 App 若只要供应商名册，仍须 `createThirdService` + 一份含 provider 键的 manifest + 能弹确认框的 runtime。把 admin 的 `thirdService` 单例跨 App 偷走，口试先数 L-007「组合可以复制，单例不能跨 App 偷」。
14. **迁移口诀：** 先数工厂四键顺序与「一张 Vue」→ 再数 `runtime.service` 真正喊出的方法 → 再数嵌套凭据三枪（没有第五键）→ 再数托盘没有 hasPermission / dicts → 最后数厨房没收的 `/save` 与 execute。跳步会出现「把 ThirdPage 说成网关」「把九厂一行盖章」「把凭据说成独立路由」。

## 常见误区

1. **「OBJ-66 包含 `createThirdService`。」** 那是 OBJ-65。本课认菜单工厂与门脸如何消费它。
2. **「OBJ-66 包含 `ThirdProviderController`。」** 那是 OBJ-60 / 子课 L-001。
3. **「把九个 web-domain 工厂一行标 covered。」** 本课只给 `A:createThirdWebDomain`。
4. **「四张 `.vue` 文件。」** 只有 `ThirdPage.vue`。
5. **「`service.providers.list`。」** 厨房是扁平函数名 `listProviders`，不是嵌套抽屉。
6. **「页面自己拼 `/third/provider/list`。」** 厨房才拼。
7. **「凭据走 `createOpenApiService`。」** 走 third 的 credential HTTP。
8. **「观测页也能改供应商。」** `editable === false`。
9. **「`v-hasPermi` 是厨房方法。」** 是厅堂装的指令。
10. **「工厂缺 runtime 会 throw。」** 2026-09-17 不会。
11. **「home 也能打开三方接口管理。」** 工作树没有这份工厂接线。
12. **「capabilities 决定挂哪几页。」** 决定挂页的是 manifest + 菜单种子 + `selectedManifestIds`。
13. **「前端授权。」** 藏按钮 ≠ 授权。后端仍是最终授权者。
14. **「编辑保存已经用 edit 权限口。」** 按钮核 edit；厨房方法仍是 `save*` 集合根 POST。
15. **「`AGENTS.md` 写了 credential 就是第五键。」** 它写的是「credential summary」管理，和测试一致：摘要嵌在页里。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/web-domains/third/src/index.ts`。圈 id `web-domain-third`、四键顺序、四组 permissions、`h(ThirdPage, { runtime, kind })`。打开 `index.test.ts`，核对 `service: {} as never` 仍能冻出四键，以及 `componentKey` 不含 `credential`、provider 组含 `third:credential:add`。
2. 打开 `src/runtime.ts`。圈 `service: ThirdService` 与三个可选口。确认**没有** `requireThirdWebRuntime`，**没有** `hasPermission`。
3. 打开 `ThirdPage.vue`。圈 `kind` 四路 `load()`、`save()` 只喊 `saveProvider`/`saveEndpoint`、凭据三枪、`v-hasPermi`、`confirm?.`。确认没有 `execute`、没有 `@/application/services`。
4. 打开 `apps/admin-web/src/router/adminManifestRegistry.ts` 第 245 行附近。圈 `service: thirdService`、`selectedManifestIds` 里的 `'web-domain-third'`。打开 `adminManifestRegistry.test.ts` 第 107 行，核对 `third/provider/index` 的 `componentName`。打开 `apps/home-web/src/router/homeManifestRegistry.ts` 第 39 行，确认没有这份 id。
5. 打开 `apps/admin-web/src/application/services.ts` 第 91 行。圈 `thirdService = createThirdService(domainHttp)`。这是厅堂插头，不是本工厂。
6. 打开 `domains/third/src/index.ts`。圈 16 支枪与**没有** execute。对照 Controller 的 `POST /save`（L-060/L-061）确认厨房封面没收。打开 `50-cde-base-dml.sql` 三方菜单块，圈四张 C 型与凭据 F 型的父菜单。

## 总结、词汇表与下一步

- **宏观四键三方面板：** `createThirdWebDomain` 把四张门脸订进 `web-domain-third`。墙上是供应商 / 接口 / 调用明细 / 调用统计。一张 `ThirdPage` 用 `kind` 演戏。凭据是抽屉，不是第五行。厨房方法全集 ≈ 墙上已扣的扳机；后端还有厨房没收的 `/save` 与出站大门。
- **(a) `createThirdWebDomain`：** 工厂冻住四键与四组 `third:*` 权限串，用 `h(ThirdPage, { runtime, kind })` 把厅堂的 `thirdService` 喂进页面。本课**不**把矩阵那一行九个工厂一起闭合。
- **页面实际调用：** 供应商/接口走 list/get/save/status/remove；凭据走 list/save/delete（嵌套，无明文）；观测只 list。`execute`、凭据 get-by-id、Controller `/save` 目前没有厨房字段或没有页面直打。
- **托盘很瘦。** 只有 `service` 加三个可选弹窗口。授权靠 `v-hasPermi`。不是 system OpenAPI，不是 home 菜单。

词汇表：`createThirdWebDomain` / `ThirdWebRuntime` / `thirdService` / `ThirdService` / `kind` / `componentKey` / `web-domain-third` / `CredentialSummary` / `v-hasPermi` / permissions catalog / host port / `ThirdPartyGateway.execute`。

下一步：厨房 URL 与 16 支枪封面是 OBJ-65。供应商/接口/凭据/观测/网关的 Java 五层是 OBJ-60…OBJ-64（子课 L-001…L-005）。入站 OpenAPI 是 OBJ-33。厅堂插头是 OBJ-07。导航 host 是 OBJ-20。本课结束不发作业、不打分；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/admin-web`；`frontend/apps/home-web` | 厅堂 `thirdService`；registry 注入 runtime；home 无此工厂 | `application/services.ts` 第 91 行；`router/adminManifestRegistry.ts` 第 245–254、375 行；`homeManifestRegistry.ts` 第 39 行 | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}/third` | 厨房 facade、web-domain 四键、runtime 类型 | 各包 `package.json` `exports` 与 `src` | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 三方目录四键 + F 型按钮；凭据挂在供应商下；无 credential C 型 | `NAMEWTA-THIRD-MENU-DML-001`；`210070…` | 2026-09-17 |
| S-014 | child `2026-09-14-wta-third` course / L-001…L-005 | 后端五切片与网关出站；本课只对照不复述五层 | `children/2026-09-14-wta-third/` | 2026-09-17 |
| S-L066-01 | `web-domains/third/src/index.ts`；`index.test.ts` | 四键顺序；permissions 四组；`h(ThirdPage, { runtime, kind })`；凭据非第五键；`service: {} as never` | `createThirdWebDomain`；一个 `it` | 2026-09-17 |
| S-L066-02 | `web-domains/third/src/runtime.ts` | `ThirdWebRuntime`；`service` 类型来自 `ThirdService`；三口可选 | 接口字段 | 2026-09-17 |
| S-L066-03 | `ThirdPage.vue`；`pages.ts` | 四 kind；list/get/save/status/remove；凭据三枪；`v-hasPermi`；无 execute | 模板按钮与 script `load` / `save` / `openCredentials` | 2026-09-17 |
| S-L066-04 | `adminManifestRegistry.ts`；`.test.ts`；`homeManifestRegistry.ts` | `adminThirdWebRuntime`；`web-domain-third`；`ThirdProvider` 组件名；home 未选 | registry 第 245–254、366–376 行；测试第 107 行 | 2026-09-17 |
| S-L066-05 | `domains/third/src/index.ts`；`index.test.ts` | 16 支枪封面；无 execute；集合根 POST；id 编码 | `ThirdService`；`createThirdService`；transport 测试 | 2026-09-17 |
| S-L066-06 | `ThirdProviderController` / `ThirdEndpointController` / `ThirdCredentialController` / `ThirdObservabilityController` | add 与 `/save` 分窗；凭据无 get；观测只读 | `backend/wta-modules/wta-third/.../controller/admin/` | 2026-09-17 |
| S-L066-07 | `frontend/e2e/third-party-management.spec.ts` | 四页投影；凭据嵌套且 JSON 为空；list-only 藏突变；未授权路由失败关闭；Client 头 | 两个 `test` | 2026-09-17 |
| S-L066-08 | `web-domains/third/AGENTS.md`；`package.json` | HTTP 留在 domain-third；页面经 runtime；exports 只有 `"."` | AGENTS 全文；exports 字段 | 2026-09-17 |
