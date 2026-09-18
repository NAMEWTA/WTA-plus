---
lesson_id: L-077
objective_ids: [OBJ-77]
claimed_cells:
  - A:createDemoService
  - A:createRichTextService
  - A:createDemoWebDomain
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: kitchen-http
    minutes: 10
  - segment: factory-and-pages
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-008, S-010, S-L007-01, S-L029-01, S-L074-01, S-L077-01, S-L077-02, S-L077-03, S-L077-04, S-L077-05, S-L077-06, S-L077-07, S-L077-08]
---

# Lesson 077：宏观三键实验室——`createDemoService` 与 `createRichTextService` 怎样印 URL，`createDemoWebDomain` 怎样把练习卡贴到墙上

## 学完你能做什么

打开厨房 `frontend/packages/domains/demo/src/index.ts` 的 `createDemoService`、同包 `src/rich-text.ts` 的 `createRichTextService`，再打开菜单工厂 `frontend/packages/web-domains/demo/src/index.ts` 的 `createDemoWebDomain`，你能**口述浏览器这一头怎样把三张练习卡打到 `/demo/*`，再贴到管理端墙上**：测试单表、测试树表、富文本演示。不是 Java 八扇单表窗（L-074），不是树表六扇（L-075），不是富文本 Controller 与 OSS 资产解析（L-076），也不是「工厂名叫 Demo 所以厨房没有树、没有富文本」。

口试名单就是矩阵 **(a)** 这三格，符号以**磁盘**为准：

1. **`A:createDemoService`**（包 `@namewta/domain-demo`，实现在根 `src/index.ts`，**不是** `transport.ts`）：工厂认 `HttpClient`，第二参可选 `RichTextService`。返回 `Object.freeze` 的 `DemoService`。封面是 **10 支扁平 HTTP 枪 + 一只嵌套 `richText` 口**：单表 5 枪（list/get/add/update/delete），树表 5 枪（同样五个动词）。单表 list/get 会先 `projectDemoTransport` 再出门。路径 id 走私有 `encodeId` / `encodeIds`：先 `encodeURIComponent(String(id))`，数组再逗号拼接。**不**自己画 Vue，**不**读 `Admin-Token`，**没有** `pageDemo` / `importDemo` / `exportDemo`，**没有**加密、批量、请假那些邻居 Controller。
2. **`A:createRichTextService`**（同包 `src/rich-text.ts`，根 `index.ts` `export *` 再导出）：工厂认 `HttpClient` **和** `RichTextAssetsPort`。返回 `Object.freeze` 的 `RichTextService`。封面是 **5 支 HTTP 枪 + 一只原样挂上的 `assets` 口**：`list` / `get` / `create` / `update` / `remove`。写动词全是 **POST**（`/create`、`/{id}/update`、`/{id}/remove`），和单表/树的 PUT/DELETE **不是同一把棘轮**。`GET /demo/rich-text/assets` **不在**这五支枪里——那是厅堂自己实现的 `assets.resolve`。
3. **`A:createDemoWebDomain`**（包 `@namewta/web-domain-demo`）：工厂先 `requireDemoWebRuntime`，缺托盘当场 throw `'DemoWebRuntime is required'`；再冻住 `WebDomainManifest`。id 是 `web-domain-demo`，`domainId` 是 `'demo'`。磁盘上正好 **3** 条 `componentKey`（测试锁死顺序与名字）：`demo/demo/index`、`demo/tree/index`、`demo/rich-text/index`。它**不** `addRoute`，**不**读 `Admin-Token`，**不**自己拼 `/demo/demo/list` 这类 request URL。导出字符串走宿主 `download`，不进厨房对象。

OBJ-77 还要你能把「墙上哪一行」和「厨房哪一枪」对上，并说出四处故意错位：

- 厅堂单例导出名是 `demoService`。`createRichTextService` **不单独导出**；它被塞进 `demoService.richText`。不要发明厅堂符号 `richTextService`。
- 单表导出：页面走 `runtime.download('demo/demo/export', query, ...)`。厅堂 `downloadWithAxios` 走 **`client.post`**，对得上后端 `POST /demo/demo/export`（L-074）。freeze 对象上搜不到 `exportDemo`。
- 树表导出：后端有 `GET /demo/tree/export`，种子有 F 型 `demo:tree:export`，厨房没有枪，权限组没收，`TreePage` 没有按钮。
- 单表 `GET /page` 与 `POST /importData`：后端窗在，厨房方法表没有，页面没有扳机。

2026-09-17 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `createDemoTableService` / `createTreeService` | **没有。** 一座 `createDemoService` 管单表+树，富文本是第二参 |
| 工厂写在 `transport.ts` | **没有。** 工厂、类型、名片全在 `src/index.ts`。`transport.ts` 只投影 `TestDemoVo` → `DemoVO` |
| `@namewta/domain-demo/rich-text` 子路径会得到更小的富文本厨房 | **没有这个 export。** `package.json` `exports` 只有 `"."` / `./test-demo` / `./test-tree`。富文本类型和工厂从根再 export |
| `demoService.table.list` / `service.richText.documents.list` | **没有嵌套抽屉（表/树）。** 方法名就是 `listDemo` / `listTree`。只有 `richText` 这一只嵌套口 |
| 页面 `import { demoService } from '@/application/services'` | **三张 web-domain 页都不。** 厅堂把单例塞进 `runtime.service` |
| `createDemoWebDomain(undefined)` 像 notify 那样仍能冻出键 | **会 throw。** `requireDemoWebRuntime`：`'DemoWebRuntime is required'`。对照 L-054 / L-066；对齐 L-073 |
| home / sso 对称一份 | **没有。** 两厅搜不到这两个工厂，也搜不到 `demoService` |
| 厨房有 `POST /demo/demo/export` 或 `GET /demo/tree/export` | **没有这两支枪。** 单表导出走 `runtime.download`；树表谁都不接 |
| `createEncryptDemoService` / `demo/encrypt/index` | **没有。** 加密玩具是后端 deferred 窗，本菜单工厂三键不含它 |
| 请假列表示例是 demo 菜单 | **不是。** `workflow/leave/index` 挂在「测试菜单」下，但是 L-073 的流程工厂键 |
| 包 `AGENTS.md` 只写 test-demo / test-tree | **过期。** 磁盘工厂、测试、菜单种子都是**三**页；缺的是富文本。以工作树为准 |
| `@namewta/domain-demo` 依赖 OSS adapter 才能上传图片 | **包依赖表没有 adapter。** 交叉线画在厅堂：`richTextAssets.upload` 转 `ossUploadClient` |

本课**不宣称**你会拆 `TestDemoController` 八扇（L-074）、`TestTreeController` 六扇（L-075）、`TestRichTextController` 与 OSS 资产解析（L-076）、`createOssUploadClient.upload` 直传三层（L-029）、厅堂 `services.ts` 十二个工厂（L-007），或把九个 web-domain 工厂一行标 covered（GP-L-020）。今天只认：**浏览器这一头的 demo 厨房 URL 表（含富文本五枪与资产口），三键菜单怎样把厨房喂进页面，页面实际扣了哪几枪，厨房标签上写着、方法表里没有、或后端有、墙上没按钮的那几扇门。**

矩阵 (a) 那一行把九个工厂写在一起。本课只给 **`createDemoWebDomain` 这一颗**当菜单证据，**不要**把整行九厂标成 covered。厨房独行只给 **`createDemoService` + `createRichTextService`**。

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`export const demoService = createDemoService(domainHttp, createRichTextService(domainHttp, richTextAssets))`。先做 `richTextAssets`，再造富文本厨房，再塞进 demo 工厂第二参。L-008 对照过 home / sso **没有**这座厨房。L-020 讲导航 host 怎样用 `sys_menu.component` 对上 `registration.load`。L-029 是 OSS 直传客户端；本课只认厅堂怎样把它接到 `assets.upload`。L-074…L-076 是后端 `/demo/*` 的三份 classic 窗。本课站在**已经登录的管理员浏览器**这一头：口袋里是 `Admin-Token`，司机是 `adminHttp`。

四条河都叫 demo，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `createDemoService` | 浏览器演示厨房：单表五枪 / 树表五枪 + 嵌套富文本口 | **本课格子** |
| `createRichTextService` | 同包第二座小厨房：富文本五枪 + `assets` 端口 | **本课格子**；厅堂不单独导出 |
| `demoService` | 厅堂单例，把 `domainHttp` 和富文本厨房塞进工厂 | **接线**；组合点是 L-007 |
| `createDemoWebDomain` | 管理端三键菜单 | **本课格子** |
| `TestDemoController` / `TestTreeController` / `TestRichTextController` | 后端 classic 窗 | L-074…L-076 |
| `createOssUploadClient.upload` | 浏览器直传 MinIO | L-029；本课只认 policy 字符串从哪来 |
| `@namewta/web-kit-rich-text` | 编辑器 / 预览器 | **邻居**：页把 `runtime.service.richText.assets` 塞进去 |
| Redis/MQTT/加密/批量那些 Controller | 走廊玩具柜 | 矩阵 deferred；**不是**三键 |

2026-09-17 工作树：权威厨房是 `frontend/packages/domains/demo/`（根 facade `src/index.ts` + 富文本 `src/rich-text.ts` + 单表投影 `src/transport.ts` + 两个资源子目录）。权威菜单包是 `frontend/packages/web-domains/demo/`。厅堂接线是 `apps/admin-web/src/application/services.ts` 第 54–69 行与 `apps/admin-web/src/router/adminManifestRegistry.ts` 第 78–85、355、368 行。`selectedManifestIds` 含 `'web-domain-demo'`。home **不**选这份 id，也**没有** `demo` domain。

菜单种子在 `50-cde-base-dml.sql`：三张 C 型页都挂在「测试菜单」`1761400000000000005` 下面——`demo/demo/index`、`demo/tree/index`、`demo/rich-text/index`（富文本块 `NAMEWTA-RICHTEXT-DML-001`）。请假列表示例也挂在同一棵测试菜单下，但那是流程工厂的键，不是本课三键。

```text
已登录的管理员（浏览器，Admin-Token）
        │
        ├─ 三张菜单页（createDemoWebDomain）
        │     runtime.service = demoService
        │     runtime.confirm / success / download
        │
        x  没有加密页、没有批量页、没有请假页
        x  没有 home / sso 插座
                        │
                        v
              ┌──────────────────────────────────────────────┐
              │  createDemoService(domainHttp, richText)     │  ← 本课厨房
              │    单表 5（list 投影）/ 树表 5                │
              │    richText = createRichTextService(...)     │  ← 本课第二座
              │      list/get/create/update/remove           │
              │      assets = 厅堂 richTextAssets            │
              └──────────────────────────────────────────────┘
                        │
                        v
              厅堂 domainHttp → adminHttp.request
              （Bearer 登录票；上传字节另走 ossUploadClient）
                        │
        /demo/demo/*          /demo/tree/*
        /demo/rich-text/*     厅堂 GET /demo/rich-text/assets
        （厨房不打 /demo/demo/page、
          厨房不打 /demo/demo/importData、
          厨房不打 /demo/demo/export、
          厨房不打 /demo/tree/export、
          厨房不打 /demo/encrypt、/demo/batch）
```

往下走不要跳层：

```text
Vue 演示页（只收 runtime）
    └─ web-domain 外包一层 h(page, { runtime })
          └─ domain 工厂拼 URL / method / 单表投影
                └─ App 的 domainHttp（axios + Admin-Token）
                      └─ 后端 TestDemo / TestTree / TestRichText Controller
```

富文本插图**不**走 `domainHttp` 的 POST 文件窗：

```text
RichTextEditor 选了一张图
    └─ runtime.service.richText.assets.upload(file, 'image', { signal, onProgress })
          └─ 厅堂 policy = 'richtext-image'
                └─ ossUploadClient.upload（L-029）
                      └─ 只把 { ossId, fileName } 交回编辑器
预览要 URL
    └─ assets.resolve(ossIds, { signal, richTextId })
          └─ 厅堂 GET /demo/rich-text/assets?ossIds=&richTextId=
```

**类比：** 把厨房想成实验室柜台上的**两本出餐单钉在一起**：左边五道「没父子的练习卡」（单表），右边五道「有爸爸的练习卡」（树表）；夹子上再别着一本「作文本」（富文本五枪）。`createDemoWebDomain` 是墙上的**三行点菜单**。服务员（Vue）只对托盘喊「来一份 listDemo / listTree / richText.create」。地址印在出餐单上。作文本要贴图画时，厨房自己没有颜料仓库——厅堂雇了一个仓库门卫（`richTextAssets`），门卫去 OSS 拿票送货（L-029），再去富文本窗问「这张图还能看吗」（`GET /assets`）。印 Excel 走大厅另一根**下载管**（`runtime.download`），不盖厨房的章。

**类比失效边界：**

1. 「宏观三键」**不**等于「厨房里只有三道菜」。厨房还有投影、缺省关闭的资产口、树表 delete 能吃数组。墙上没按钮 ≠ 刀被收了。
2. 类比**不**等于「home 选了 `web-domain-demo` 就会少一页」——home 根本不选这份 id，连厨房单例都没造。
3. 类比**不**等于「单表、树、富文本三族 HTTP 动词一样」。表/树是存量 PUT/DELETE 棘轮；富文本写路径全是 POST。一本出餐单，两套填法。
4. 类比更**不**等于「点保存就是 `TestDemoServiceImpl` 的内部实现」——浏览器只保证打到哪扇窗。乐观锁 version、数据权限、XML 自定义分页，是 L-074…L-076 的后窗。
5. 仓库门卫**不是**厨房漏装的抽屉。`@namewta/domain-demo` 的 `package.json` 依赖没有 OSS adapter。把直传写进工厂，就会逼演示包 import 仓储楼。
6. 「测试菜单」底下还有请假列表示例。那张卡是流程厅的菜，不要因为父菜单一样就背进本课三键。

## 核心概念与机制

### 直觉讲解

小孩子版先记十四句：

1. **先找两座工厂，再找三键。** 厨房 `createDemoService(http, richText?)`。富文本 `createRichTextService(http, assets)`。菜单 `createDemoWebDomain(runtime)`。封面 id 永远是 `web-domain-demo`。
2. **工厂名有 Demo，货却有表、树、作文本。** 十支扁平枪在 freeze 对象根上；作文本挂在 `service.richText`。
3. **`transport.ts` 不是工厂。** 它只把 OpenAPI 的 `TestDemoVo` 投影成 `DemoVO`：留下 id / deptId / userId / orderNum / testKey / value，缺的填 `''` 或 `0`，丢掉 `createTime` / `createBy*` / `update*` / **`version`**。
4. **树表不投影。** `listTree` / `getTree` 原样过。不要说「整个厨房都 fail-closed 投影」。
5. **表/树：读 GET，新增 POST，改 PUT，删 DELETE。** 这是 L-005 那把存量棘轮。厨房里搜得到 `'put'` 和 `'delete'`。
6. **富文本：读 GET，写全 POST。** `create` 打 `/demo/rich-text/create`，不是 `POST /demo/rich-text`。`update` / `remove` 走 `/{id}/update`、`/{id}/remove`。
7. **路径 id 会编码。** 私有 `encodeId` 是 `encodeURIComponent`。`encodeIds` 把数组先编码再逗号拼。用在 get / delete 的路径段，**不用**在 list 的 query。
8. **缺第二参，仓库门锁死。** `createDemoService(http)` 不传富文本时，内部仍会 `createRichTextService`，但 `assets.upload` 直接 throw `'Rich-text assets port is not configured'`，`resolve` 回 `[]`。admin-web **没有**走这扇缺省门。
9. **菜单工厂先关门再冻三键。** `undefined` runtime 当场 throw。传一个缺方法的 stub 仍能冻出三键——缺枪要等页面点击才爆。
10. **页面只收 runtime。** 三张 Vue 都不 import 厅堂单例。单表/树用 `runtime.confirm` / `runtime.success`；富文本页**没接**这两口，自己喊 `ElMessage` / `ElMessageBox`。
11. **导出不进厨房。** 单表按钮走 `download`。树表种子有导出字，页上没按钮。
12. **权限组不是种子全集。** 单表组没有 `query` / `import`；树表组没有 `query` / `export`；富文本组有 `query` 和 `common:richtext:upload`。藏按钮 ≠ 授权。
13. **`messages` 只有两句标题。** `tableTitle` / `treeTitle`。富文本页标题写在 Vue 模板里，不进 i18n 块。不要把 messages 数成三。
14. **capabilities 不是 componentKey。** 名片三项 `demo-table` / `demo-tree` / `demo-rich-text` 对得上三组 permissions 的 id，决定挂页的仍是 manifest + 菜单种子 + `selectedManifestIds`。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 演示厨房工厂 | `createDemoService` | `domains/demo/src/index.ts`；返回 `Object.freeze` |
| 演示厨房封面 | `DemoService` | 同文件 `export interface`；10 方法 + `richText` |
| 富文本厨房工厂 | `createRichTextService` | `domains/demo/src/rich-text.ts` |
| 富文本厨房封面 | `RichTextService` | 同文件；5 方法 + `assets` |
| 单表投影 | `projectDemoTransport` | `src/transport.ts`；输入 `OpenApiSchema<'TestDemoVo'>` |
| 路径段编码 | `encodeId` / `encodeIds` | 私有箭头；数组 → 逗号拼接 |
| 资产端口 | `RichTextAssetsPort` | `upload` / `resolve`；厅堂实现，厨房只原样挂上 |
| 模块名片 | `demoDomainModule` | `id: 'demo'`，`backendModules: ['wta-demo']`，三项 capabilities |
| 资源标签 | resource metadata | `demoTestDemoResource.controller = 'TestDemoController'`，`basePath: '/demo/demo'`；树表同理。**不是** HTTP 发送。富文本**没有**资源对象文件 |
| 管理端演示菜单工厂 | `createDemoWebDomain` | `web-domains/demo/src/index.ts`；manifest id `web-domain-demo` |
| 菜谱板 | `WebDomainManifest` | `id` / `domainId` / `messages` / `permissions` / `registrations` |
| 组件键 | `componentKey` | 与 `sys_menu.component` 对表 |
| 运行时托盘 | `DemoWebRuntime` | `src/runtime.ts`；`service` / `confirm` / `success` / `download`。**没有** `error` / `dicts` / `treePanel` |
| 失败关闭（本厂） | fail-closed runtime | `requireDemoWebRuntime`；缺 runtime 当场 throw |
| 浏览器拼树 | `buildTree` | `web-domains/demo/src/composables.ts`；扁平表按 `parentId` 长成树 |
| 宿主下载管 | host `download` | 厅堂 `application/http.download`；单表导出走它 |
| 缺省资产口 | default assets port | upload throw；resolve `[]` |

厨房三族与 HTTP 门牌（动词以磁盘为准）：

| 族 | 方法数 | 门牌前缀 | 写动词 | 本课三页用不用 |
| --- | --- | --- | --- | --- |
| 单表 | 5 | `/demo/demo` | POST 增、**PUT** 改、**DELETE** 删 | **用。** 管理页 list/get/add/update/delete。**无** page / import。导出走 download |
| 树表 | 5 | `/demo/tree` | 同上 PUT/DELETE | **用。** 列表+下拉都打 `listTree`，浏览器 `buildTree`。**无** export |
| 富文本 HTTP | 5 | `/demo/rich-text` | **POST** create/update/remove | **用。** `RichTextPage` 五枪 |
| `assets` | 0 支厨房 HTTP | 厅堂 `GET /demo/rich-text/assets`；上传走 OSS | 端口 | **用。** 编辑器 / 预览器 |

`demoDomainModule.capabilities`：`demo-table` / `demo-tree` / `demo-rich-text`。这是名片，**不是** componentKey，也不决定 App 挂哪几页。

### 机制/因果链

**1. 一座扁平厨房，两把棘轮，只有单表投影。**

`createDemoService(http, richText?)` 立刻 freeze。内部没有统一的 `call` 助手：每一枪自己写 `http.request({ url, method, params?, data? })`。不剥信封、不加 `no-store`。

单表 / 树表写动词按后端存量：**GET 读，POST 新增，PUT 改，DELETE 删**。不要把 third / notify 的「变更一律 POST」套到这两族。也不要把这两族的 PUT 套到富文本。

`encodeId('demo/7')` → `demo%2F7`。`deleteDemo(['demo/7', 'demo 8', 'demo,9'])` → `/demo/demo/demo%2F7,demo%208,demo%2C9`。测试锁死这条。单表后端 `@PathVariable Long[] ids`、树表 `Long[] ids` **绑不上**带斜杠的测试 id——厨房测试保的是浏览器拼法，不是后端一定吃得下。

`listDemo`：先 GET `/demo/demo/list`，若 `response.data.rows` 是数组，才 `rows.map(projectDemoTransport)`；否则把响应**原样**当 `PageResult<DemoVO>` 交出去（测试没锁这条缺 rows 分支，口试按源码）。`getDemo`：有 `data` 就投影，没有 data 就原样返回 metadata。

投影输出的 `DemoVO` **没有** `version`、没有审计字段。`DemoForm` 继承 `AuditFields`，初始表单却只有 id/deptId/userId/orderNum/testKey/value。编辑 `Object.assign(form, res.data)` 也拿不回 version。`updateDemo` 原样 PUT 这份表单——L-074 的 `@Version` 在浏览器这一头常常是空枪。口试要能说出「投影把锁扔掉了」，不要把后窗乐观锁讲成前端已经带上。

树表**不**走 `projectDemoTransport`。`listTree` 的返回类型是 `ApiResponse<TreeVO[]>`，不是分页信封。后端 `R<List<TestTreeVo>>` 对得上：一棵树一次拿全表，页面自己长树。

**2. 富文本是第二座工厂，资产口是厅堂的门卫。**

`createRichTextService(http, assets)` 自己的 `encodeId` 只吃 `string`。五枪：

| 方法 | HTTP |
| --- | --- |
| `list(query)` | `GET /demo/rich-text/list`，`params` 是 `PageQuery` |
| `get(id)` | `GET /demo/rich-text/${encodeId(id)}` |
| `create(data)` | `POST /demo/rich-text/create`，body `RichTextForm`（title / html，可选 version） |
| `update(id, data)` | `POST /demo/rich-text/${encodeId(id)}/update`，body 必须带 `version: number` |
| `remove(id, version)` | `POST /demo/rich-text/${encodeId(id)}/remove`，body `{ version }` |

`assets` **原样**出现在 freeze 对象上，不是再包一层。厨房测试 `index.test.ts` **只锁表/树 10 枪与投影**，**不锁**这五支 URL。口试不要说「测试已经钉死富文本路径」——2026-09-17 没有那份合同。

厅堂 `richTextAssets`（`services.ts` 第 54–68 行，**未导出**）：

- `upload`：`kind === 'attachment'` 时 policy 是 `richtext-file`，否则 `richtext-${kind}`（image / audio / video）。然后 `ossUploadClient.upload`，只把 `{ ossId: String(result.id), fileName: result.name }` 交回。字节不进 `/demo/rich-text/*`。
- `resolve`：厅堂自己 `domainHttp.request`，`GET /demo/rich-text/assets`，`params: { ossIds: ossIds.join(','), richTextId }`。`data` 不是数组就当 `[]`。这一枪的权限字是后端的 `demo:richtext:query`。

缺省口（工厂第二参省略）：upload throw `'Rich-text assets port is not configured'`；resolve 回 `[]`。admin-web 显式传入，**不走**缺省。第三份 App 若只抄 `createDemoService(http)` 就去贴图，会在点击上传时炸掉——这是组合的正例，不是厨房漏实现。

**3. 菜单工厂先关门再冻三键。**

`createDemoWebDomain(runtimeInput)`：`requireDemoWebRuntime` 看到 `undefined` 就 throw。测试传入的是 stub runtime，**不是** `undefined`，所以仍能冻出三键。把 `service: {} as never` 传进去也能得到键——缺方法要等页面点击才爆。对照 notify/third：工厂根本不检查。对照档案楼：缺十一口当场 throw。本厂只检查「托盘在不在」。

`registrations[].load` 闭包住这份 runtime，再 `runtimeView` → `defineComponent({ setup: () => () => h(page, { runtime }) })`。三张 Vue，不是一张页三个 `kind`。

权限目录三组（字符串以工厂为准）：

| id | 冻进去的串 | 页上实际核对 |
| --- | --- | --- |
| `demo-table` | list / add / edit / remove / **export** | 工具栏增删改导出。**没有** query、**没有** import。详情枪仍打 `getDemo`（后端要 `demo:demo:query`） |
| `demo-tree` | list / add / edit / remove | 增、行上改/增子/删。**没有** export，也**没有** query |
| `demo-rich-text` | list / **query** / add / edit / remove / `common:richtext:upload` | **页上没有 `v-hasPermi`。** 按钮始终画出来；后端仍按字检查 |

`messages` 只冻两句：`测试单列表` / `测试树列表`。两张页模板里也把这两句写死了。富文本页标题是「富文本通用组件演示」，不进 messages。

`composeAppRuntime` 只有 `selectedManifestIds` 含 `web-domain-demo` 时才把键交给导航。admin 厅堂这份 id 在 registry 第 368 行。home 的清单是 `web-domain-admin` + `web-domain-profile-self`。重复 `componentKey` 会 `AppRuntimeError`。

包 `exports`：根、`./test-demo`、`./test-tree`。**没有** `./test-rich-text`。`test-demo/index.ts` 再 export `DemoTablePage`；`test-tree/index.ts` 再 export `DemoTreePage`。富文本页只能走根工厂的 `loadRichTextPage`。

**4. 厅堂托盘四口；富文本页只用了 `service`。**

`demoRuntime`（registry 第 78–85 行）：

| runtime 口 | 厅堂接到哪 | 谁在用 |
| --- | --- | --- |
| `service` | `demoService` | 三页都喊 |
| `confirm` | host/feedback `modal.confirm` | **DemoPage / TreePage** 删除前 |
| `success` | `modal.msgSuccess` | **DemoPage / TreePage** 保存与删除后 |
| `download` | `application/http.download` | **只有 DemoPage** 导出 |

没有 `error`、没有 `dicts`、没有 `treePanel`、没有 `fileUpload`。树的下拉不是厅堂 TreePanel，是页内 `el-tree-select` + 再打一次 `listTree`。

口试不要说「页面直接 `import demoService`」。也不要说「三页都走 confirm/success」——富文本页绕开托盘，直接 `ElMessage` / `ElMessageBox`。

**5. 单表页五枪 + 一根下载管；成功文案写死「修改成功」。**

`DemoPage`：`listDemo(queryParams)` 填表；搜索把 `pageNum` 置 1。新增 `openDialog('添加测试单')`。编辑先 `getDemo(row.id || ids[0])`，再 `Object.assign`。提交：有 `form.id` 走 `updateDemo`，否则 `addDemo`。然后**永远** `runtime.success('修改成功')`——连新增也说「修改」。这不是后端文案。

删除：`confirm` 后再 `deleteDemo(demoIds)`，ids 可以是一行或勾选数组。导出：`runtime.download('demo/demo/export', { ...queryParams }, \`demo_${timestamp}.xlsx\`)`。字符串**没有**前导 `/`，也**不是**厨房方法。权限按钮核 `demo:demo:export`。

表单初始没有 version。规则里有一条 `id` 必填，模板却没有 id 的 `el-form-item`——新增不靠这条规则拦，靠 `form.id` 空不空分流。

**6. 树表页用 list + 浏览器拼树；下拉再打一枪；导出是空子弹。**

`TreePage`：`listTree(queryParams)` 拿扁平表，`buildTree(data, 'id', 'parentId')` 在浏览器长成树。没有分页组件。展开/折叠是 `useTreeTableExpand` 本地 toggle，不打 HTTP。

下拉 `getTreeselect`：**再** `listTree()`（不带当前筛选），前面塞一个前端造的 `{ id: 0, treeName: '顶级节点' }`。id `0` 不是厨房返回的。工具栏新增把 `parentId` 置 0；行上「新增」把 `parentId` 置成当前行 id。

编辑：先 `getTreeselect`，源码里会短暂把 `form.parentId = row.id`（写成了「自己是自己的爸爸」），紧接着 `getTree(row.id)` + `Object.assign`，用详情把父 id 盖回去。口试不要把这行中间赋值教成合同；真正出门的 `updateTree` body 以详情为准。

保存成功文案是 **`操作成功`**，不是单表那句「修改成功」。删除只走当前行 id，没有多选列。厨房 `deleteTree` 仍能吃数组（测试锁了），页没用。

种子 F 型 `demo:tree:export` 在。`TestTreeController` 的导出是 **`GET /export`**（和单表 POST 不一样）。厨房没有枪，权限组没收，页没有按钮。不要发明树表导出。

**7. 富文本页五枪齐，权限指令一枪没有，version 从列表里找。**

`RichTextPage`：`list({ pageNum: 1, pageSize: 50 })` 写死这一页。编辑 `get(id)` 只把 title / html 填进本地 ref，**不**把 `version` 存进编辑态。保存：有 `editingId` 就 `update(id, { title, html, version: rows.find(...)?.version ?? 0 })`，否则 `create`。也就是说乐观锁吃的是**列表行上的 version**，不是详情响应。列表过期时，后窗 L-076 的 version 对不上，保存会失败——这是前端拼法，不是厨房替你读锁。

删除：`ElMessageBox.confirm`（不是 `runtime.confirm`），再 `richText.remove(id, version)`。编辑器 `profile="full"`，预览 `profile="article"`，两边都吃 `runtime.service.richText.assets` 和 `resource-id=editingId`。`editorState.valid === false`（上传未完成或失败）时拒绝保存。

页上**没有** `v-hasPermi`。工具栏「新建并保存 / 保存修改 / 新建 / 编辑 / 删除」对任何登录用户都画出来。真正关门的是后端 `@SaCheckPermission`。不要把「工厂权限组含 `demo:richtext:query`」说成「页上核过 query」。

页顶有一个 `AbortController`，`onBeforeUnmount` 会 abort；list/get/create **没有**把 `signal` 传进厨房。厨房五枪的签名也不收 signal。能取消的是编辑器内部上传（assets.upload 的 options.signal），不是列表请求。

**8. 厨房有枪没扳机；更外面还有根本没写进厨房的窗。**

2026-09-17 **无 Vue** 喊：没有。表/树/富文本封面里的 15 支 HTTP 枪，三页都有人扣（树的 delete 只扣单 id；富文本 list 写死 50 条）。

厨房对象上**根本没有**：`pageDemo`、`importDemo`、`exportDemo`、`exportTree`、`listRichTextAssets`、任何 `/demo/encrypt`、任何 `/demo/batch`、任何 `/demo/excel`。

后端仍在、本课页面不打：

| 后端窗 | 谁接 |
| --- | --- |
| `GET /demo/demo/page` | 厨房无；页无 |
| `POST /demo/demo/importData` | 厨房无；种子无 import F；页无 |
| `POST /demo/demo/export` | **download 口** |
| `GET /demo/tree/export` | 谁都不接 |
| `GET /demo/rich-text/assets` | **厅堂 `richTextAssets.resolve`**，不是厨房方法名 |

### 图、表或文本图

**图 1：宏观三键怎样吃到厨房**

```text
 sys_menu.component（管理端；种子在 50-cde-base-dml.sql）
   demo/demo/index            ← 父菜单「测试菜单」
   demo/tree/index            ← 同一父
   demo/rich-text/index       ← 同一父；块 NAMEWTA-RICHTEXT-DML-001
        │
        v
 GET /system/menu/getRouters
        │
        v
 App composeAppRuntime
   selectedManifestIds 含 web-domain-demo
        │
        ├─ 键在已选 manifest → registration.load
        │     h(Page, { runtime })
        └─ 键不在已选清单 → 解析失败关闭
                │
                v
         Page props.runtime
                │
                ├─ runtime.service = createDemoService(..., createRichTextService(...))
                ├─ runtime.download  ← 仅单表导出字符串
                └─ 页面只喊方法名，不拼 request URL
                     （导出除外：download 吃 'demo/demo/export'）
```

**图题 / caption：** 宏观同一厨房、管理端三键、托盘把厨房和下载管一起塞进页面。alt：菜单键来自 50-cde-base-dml.sql；admin 选 web-domain-demo；页面只收到 runtime；HTTP 字符串在 domain 工厂，单表导出字符串在 download；富文本资产口在厅堂。

**文字等价物：** 人先碰到动态菜单里的 component 字符串。App 用已选 manifest 把字符串换成带 runtime 的 Vue 页。三张演示页面向 `runtime.service` 喊方法；单表导出走宿主 download；贴图走 `service.richText.assets`。厨房把方法换成 GET/POST/PUT/DELETE 和 `/demo/...`。home 即使以后有人把厨房单例造出来，墙上没有这三键，导航也不会挂页。不要按「测试菜单」底下还有请假示例，就把 leave 键算进 `createDemoWebDomain`。

**图 2：厨房有枪 / 本课页面扣扳机**

| 厨房方法 | HTTP（动词以磁盘为准） | 2026-09-17 谁扣扳机 |
| --- | --- | --- |
| `listDemo` / `getDemo` / `addDemo` / `updateDemo` / `deleteDemo` | GET `/demo/demo/list`；GET/DELETE `/{id}`；POST/PUT `/demo/demo` | **DemoPage**；list/get 先投影 |
| 单表导出 | `POST /demo/demo/export` | **download 口**，不是厨房方法 |
| `GET /demo/demo/page` / `POST /importData` | 后端有 | **厨房无方法，无 Vue** |
| `listTree` / `getTree` / `addTree` / `updateTree` / `deleteTree` | GET `/demo/tree/list`；GET/DELETE `/{id}`；POST/PUT `/demo/tree` | **TreePage**；列表与下拉都 `listTree` |
| 树表导出 | `GET /demo/tree/export` | **厨房无、页无、权限组无**；种子有 F |
| `richText.list/get/create/update/remove` | GET list；GET `/{id}`；POST `/create` `/{id}/update` `/{id}/remove` | **RichTextPage** |
| `richText.assets.upload` | 不打 `/demo/*`；OSS 直传 | **RichTextEditor**；厅堂 policy `richtext-*` |
| `richText.assets.resolve` | 厅堂 `GET /demo/rich-text/assets` | **Editor / Viewer** |
| `/demo/encrypt`、`/demo/batch` | 后端玩具柜 | **本工厂无键，无 Vue** |

**图题 / caption：** 工厂注册了三键 ≠ 后端每扇窗都进了厨房 ≠ 种子每一颗 F 都有按钮。alt：单表导出走 download；树表导出谁都不接；page/import 没进厨房；资产解析是厅堂口；加密/批量不在三键。

**文字等价物：** 管理员三页消耗单表五枪、树表五枪、富文本五枪。贴图走资产端口。顶栏没有演示铃铛。自定义分页和导入目前只有后端窗。单表导出有按钮但不是厨房方法。树表导出连按钮都没有。不要把「工厂注册了三键」说成「所有 `/demo/*` HTTP 都有按钮」。

**图的边界：** 不画 Java classic 链、XML `customPageList`、数据权限 join、`@Version` 后窗（L-074…L-076）。不画 MinIO 分片与 abort（L-029）。不保证以后产品会补树表导出按钮或单表导入页。不把 capabilities 名片画成菜单。不把 `web-kit-rich-text` 的 HTML 白名单画进本课格子。不把请假示例画进 demo 三键。

## 正例、反例与边界

**正例 1 — 管理员翻测试单表。** 有 `demo:demo:list` 的人打开 `demo/demo/index`。`listDemo({ pageNum, pageSize, testKey, value, ... })`。厨房 `GET /demo/demo/list`。rows 被投影成 `DemoVO`，缺的 deptId 变成 `''`。

**正例 2 — 新增也提示「修改成功」。** 点新增，填五个框，提交。`addDemo(form)` → `POST /demo/demo`。成功后 `runtime.success('修改成功')`。不要去后端日志里找这四个字。

**正例 3 — 导出不喊厨房方法。** 点导出。`runtime.download('demo/demo/export', query, 'demo_….xlsx')`。freeze 对象上搜不到 `exportDemo`。后端窗是 POST（L-074）。

**正例 4 — 删两行拼进路径。** 勾两行，确认后 `deleteDemo([id1, id2])` → `DELETE /demo/demo/{encodeId(id1)},{encodeId(id2)}`。测试用带斜杠和空格的假 id 锁编码，不是锁「斜杠主键能进库」。

**正例 5 — 树表在浏览器长成树。** `listTree` 回来扁平数组。`buildTree` 按 `parentId` 挂 `children`。没有父的进根。下拉再打一次 `listTree()`，根上摆「顶级节点」0。

**正例 6 — 行上新增子节点。** 点某行 Plus。`parentId = row.id`，`addTree` → `POST /demo/tree`。成功文案「操作成功」。

**正例 7 — 缺 runtime 当场关门。** `createDemoWebDomain(undefined)` throw `'DemoWebRuntime is required'`。notify 工厂 2026-09-17 不会。

**正例 8 — 厅堂接上仓库门卫。** `createDemoService(domainHttp, createRichTextService(domainHttp, richTextAssets))`。上传一张 png：policy `richtext-image`，`ossUploadClient.upload` 回 ossId。预览 `GET /demo/rich-text/assets?ossIds=…`。

**正例 9 — 缺省口关上传。** 测试里 `createDemoService(client)` 不传第二参。这时若有人喊 `service.richText.assets.upload(...)`，会 throw `'Rich-text assets port is not configured'`。admin-web 没有这条路径。

**正例 10 — 富文本保存带 version。** 列表行 `version: 3`。点编辑再保存。`POST /demo/rich-text/{id}/update`，body 含 `version: 3`（从 `rows.find` 来，不是从 `get()` 来）。删除 `POST .../remove`，body `{ version }`。

**反例 1 — 「三张 Vue `import { demoService }`。」** 全包搜不到 `@/application/services`。直接消费单例的是厅堂 registry。

**反例 2 — 「`createDemoWebDomain` 等于九个 web-domain 工厂都 covered。」** 矩阵 (a) 把九个名字写在同一行。GP-L-020 已经挖过：不要整行盖章。本课只给演示这一颗。

**反例 3 — 「工厂在 `transport.ts`。」** 打开 `src/index.ts` 的 `createDemoService`。`transport.ts` 只有 `projectDemoTransport`。富文本工厂在 `rich-text.ts`。

**反例 4 — 「厅堂导出了 `richTextService`。」** L-007 表写明 `createRichTextService` 不单独导出。页面喊 `runtime.service.richText.*`。

**反例 5 — 「`service.demos.list` / `service.tree.list`。」** 扁平：`listDemo`、`listTree`。

**反例 6 — 「从 `@namewta/domain-demo/test-demo` 进口小厨房。」** 子路径只再 export 类型，外加 `demoTestDemoResource = { controller: 'TestDemoController', basePath: '/demo/demo' }`。HTTP 仍在根工厂。

**反例 7 — 「页面手写 `/demo/demo/list`。」** 违反 L-004 方向。列表走 `runtime.service.listDemo`。导出是**唯一**单表页里出现 `demo/demo/export` 字符串的地方，而且走 download。

**反例 8 — 「树表导出按钮走 download，和单表一样。」** 树表页没有导出按钮。后端还是 GET，不是 POST。

**反例 9 — 「`listTree` 是分页信封。」** 类型是 `TreeVO[]`。不要拿 `rows/total` 去接。

**反例 10 — 「富文本 `update` 是 PUT `/demo/rich-text`。」** 是 `POST /demo/rich-text/{id}/update`。

**反例 11 — 「`GET /demo/rich-text/assets` 是 `createRichTextService` 的第六枪。」** 厨房方法表没有。厅堂 `richTextAssets.resolve` 才打。

**反例 12 — 「富文本页核了 `demo:richtext:*`。」** 模板没有 `v-hasPermi`。权限组冻着给导航/指令用，这张页没用指令。

**反例 13 — 「单表编辑会带 version。」** `DemoVO` 没有这个字段；投影丢掉 generated `version`。

**反例 14 — 「home 也能打开测试单表。」** 工作树没有这份工厂接线。

**反例 15 — 「OBJ-77 包含 `TestDemoController`。」** 那是 OBJ-74。本课只对照八扇里哪几扇被浏览器扣了扳机。

**反例 16 — 「请假列表示例是 `createDemoWebDomain` 第四键。」** 键在 `web-domain-workflow`，父菜单碰巧也是测试菜单。

**反例 17 — 「包依赖了 `api-contracts` 所以 URL 是 `keyof paths`。」** 只有单表投影吃 `OpenApiSchema<'TestDemoVo'>`。15 条 URL 都是字符串字面量。

**反例 18 — 「`AGENTS.md` 写两页所以只有两键。」** 工厂、测试、种子都是三键。文档过期。

**边界 1 — 工厂校验 runtime ≠ 校验 service 方法齐。** 缺 `listDemo` 仍能通过 `requireDemoWebRuntime`，只要对象不是 `undefined`。页面一喊才会在运行时失败。

**边界 2 — `hasPermission` 藏按钮；HTTP 仍可能 403。** 单表编辑按钮核 `edit`，详情枪要后端 `query`。只有 edit、没有 query 的人，点修改会在 `getDemo` 上 403。厨房把错误原样抛出。

**边界 3 — 路径编码只管路径段。** query 里的 `testKey` 不走 `encodeId`。资产 `ossIds` 是 `join(',')` 放进 params，编码是司机的事。

**边界 4 — 投影只发生在单表 list/get。** 树、富文本、add/update 的请求体都不投影。不要说「整个厨房都 fail-closed 投影」。

**边界 5 — 富文本 version 从列表取。** `get()` 的 `RichTextRecord` 类型上有 version（继承 Summary），页没用。并发编辑以列表快照为准。

**边界 6 — 单表成功文案与树表不同。** 单表永远「修改成功」；树表保存「操作成功」；富文本「保存成功」。三页三套字，都不是后端 `R.msg`。

**边界 7 — 树表 `parentId = 0` 是前端根。** 不要把它说成数据库一定有 id=0 的行。

**边界 8 — 附件 kind 会改名。** 编辑器 kind `attachment` → policy `richtext-file`。image/audio/video 保持 `richtext-${kind}`。yml 里四份 policy 对得上。不要发明 `richtext-attachment`。

**边界 9 — 下载管字符串没有前导斜杠。** 磁盘是 `'demo/demo/export'`。对照流程页常写 `'/workflow/...'`。以各页源码为准，不要统一「必须有 /」。

**边界 10 — `createRichTextService` 的 assets 是同一对象引用。** 厅堂改 port 实现，freeze 对象上的 `assets` 也跟着变。厨房没有再包一层。

## 变式与迁移

1. **和 L-007 对照。** 组合点课只要求你能指着「先 `richTextAssets`，再两座工厂套在一起，导出名 `demoService`」。本课要求能把 15 枪 URL 背到 method，并指出 page/import 鬼窗、两扇导出、资产解析在厅堂。不要把 L-007 的接线板图当成已经 covered 的方法表。
2. **和 L-053 / L-065 / L-073 对照。** 通知厨房嵌套抽屉 + 厅堂 directory。三方厨房扁平十五枪、变更 POST。流程厨房扁平六族、动词混用、选人口嵌在工厂里。demo 厨房：表/树扁平 PUT/DELETE，富文本嵌套且 POST，资产口在厅堂。不要把「domain 工厂」说成同一种抽屉形状。
3. **和 L-054 / L-066 / L-073 对照。** 都是「页面经 runtime 消费厨房」。差别：demo 厂 **会** `requireDemoWebRuntime`；notify/third 2026-09-17 不会。demo 是三键三张 Vue；third 是四键一张页四个 kind。demo 没有 iframe 第三条河。
4. **和 L-074…L-076 对照。** 后端 XML 分页、导入监听、树 GET 导出、富文本 OSS 解析与 version 冲突，本课不重讲。前端保证：哪一枪打哪条 URL、哪一页扣扳机、哪扇窗厨房没收。
5. **和 L-029 对照。** 直传、进度、abort 是 OSS 课。本课只认 policy 字符串怎么从 kind 算出来，以及 resolve 不走 adapter、走 `/demo/rich-text/assets`。
6. **和 L-020 对照。** 导航 host 仍是 `getRouters` → 解析 componentKey → `addRoute`。demo 不另写一套路由恢复。
7. **以后若要单表导入。** 先让厨房长出打 `POST /demo/demo/importData` 的方法（或统一走 upload 口），再补种子 F 与权限组，最后才画按钮。只改 Vue 违反 L-004。
8. **以后若要树表导出。** 不要抄单表的 POST download 字符串——后端是 GET。要么厨房长枪，要么 download 口对准 `demo/tree/export` 并确认厅堂 download 的动词。同时把 `demo:tree:export` 补进权限组。只改种子解决不了——今天连枪和按钮都没有。
9. **以后若要单表带 version。** 先让 `DemoVO` / 投影留下 `version`，表单提交再带上。只在 Vue 里塞一个 `version: 0` 会把乐观锁打成「永远从 0 改」。
10. **以后若要富文本页藏按钮。** 给模板补 `v-hasPermi`，权限组已经有 query/add/edit/remove/upload。不要以为冻进 manifest 就会自动藏。
11. **以后若只要「测试单表」瘦菜单。** 今天的工厂**会**把三键一起注册。不能靠「只用 DemoPage」让工厂少返回两键。要瘦，得改工厂或让 App 不选那些菜单种子。
12. **换 App。** 第三份 App 若只要单表，仍须 `createDemoService(该厅堂 http)` + 一份含 `demo/demo/index` 的 manifest + 能给 `download` 的 runtime（即便暂时用不到导出）。若还要贴图，必须再接 `RichTextAssetsPort`，不要偷 admin 的 `demoService` 单例。组合可以复制，单例不能跨 App 偷（L-007）。
13. **要加一条新的 `/demo/*`。** 先改封面与工厂，再补 `index.test.ts`（最好像现有表/树那样锁 URL）。若这条是加密玩具或批量反例，不要做产品菜单键。若这条需要嵌套抽屉，那是新形状。
14. **迁移口诀：** 先数两座工厂名 ≠ 厅堂只导出一个单例 → 再数表/树 10 枪 PUT/DELETE + 富文本 5 枪 POST → 再数投影只在单表、资产口在厅堂 → 再数三键顺序与 `requireDemoWebRuntime` → 再数单表导出走 download、树表导出空子弹、page/import 没进厨房、富文本页没 hasPermi → 最后数九厂一行不要盖章。跳步会出现「把 Demo 说成只管单表」「把 assets 说成厨房第六枪」「把九厂一行盖章」「把请假示例说成本课第四键」。

## 常见误区

1. **「OBJ-77 只包含 `createDemoService`。」** 原文三颗：两座厨房 + 菜单。
2. **「OBJ-77 包含 `TestDemoController`。」** 那是 OBJ-74。
3. **「把九个 web-domain 工厂一行标 covered。」** 本课只给 `A:createDemoWebDomain`。
4. **「厅堂符号叫 `richTextService`。」** 磁盘没有这份导出。
5. **「工厂在 `transport.ts`。」** 表/树在 `index.ts`，富文本在 `rich-text.ts`。
6. **「页面自己拼 `/demo/demo/list`。」** 厨房才拼。导出字符串是例外，走 download。
7. **「`service.table.list`。」** 方法名是 `listDemo`。
8. **「三族写动词都是 POST。」** 表/树改删是 PUT/DELETE。
9. **「树表 list 有 rows/total。」** 是数组。
10. **「树表导出和单表一样 POST。」** 后端 GET；页上没按钮。
11. **「`GET /page` 就是 `listDemo`。」** `listDemo` 打 `/list`。`/page` 没进厨房。
12. **「投影会留下 version。」** 丢掉。
13. **「富文本 update 走 PUT。」** POST `/{id}/update`。
14. **「assets 是厨房 HTTP 第六枪。」** 是端口；resolve 在厅堂。
15. **「富文本页有 hasPermi。」** 没有。
16. **「新增成功文案是『新增成功』。」** 单表写死「修改成功」。
17. **「home 也能打开测试单表。」** 工作树没有。
18. **「capabilities 决定挂哪几页。」** 决定挂页的是 manifest + 菜单种子 + `selectedManifestIds`。
19. **「前端授权。」** 藏按钮 ≠ 授权。后端仍是最终授权者。
20. **「`requireDemoWebRuntime` 会检查 10 支枪。」** 只检查 runtime 对象在不在。
21. **「包 README/AGENTS 写两页。」** 以工厂三键为准。
22. **「加密演示是第四键。」** 本工厂没有。
23. **「请假示例是 demo 厨房的枪。」** 是流程厨房。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/domains/demo/src/index.ts`。圈 `createDemoService`、`encodeId` / `encodeIds`、`listDemo` 的投影、`updateDemo` 的 `'put'`、`deleteDemo` 的 `'delete'`、缺省 `richText` 那行 throw。打开 `rich-text.ts`，圈五支 POST/GET URL 和 `assets`。打开 `transport.ts`，确认没有工厂。
2. 打开 `src/index.test.ts`。圈表/树 10 枪快照、路径 `%2F` 与逗号、投影把缺字段填成 `''` / `0`。在 freeze 对象上搜 `export` / `import` / `page`，确认没有。确认测试**没有**断言 `/demo/rich-text/*`。
3. 打开 `frontend/packages/web-domains/demo/src/index.ts` 与 `index.test.ts`。圈 id `web-domain-demo`、三键顺序、三组 permissions、`requireDemoWebRuntime`。核对 `componentKey` 不含 encrypt，也不含 leave。
4. 打开 `DemoPage.vue`、`TreePage.vue`、`RichTextPage.vue`。圈各自真正喊出的 `runtime.service.*` 与 `runtime.download`。确认单表成功文案「修改成功」、树表「操作成功」、富文本没有 `v-hasPermi`、富文本 update 的 version 来自 `rows.find`。
5. 打开 `apps/admin-web/src/application/services.ts` 第 54–69 行与 `router/adminManifestRegistry.ts` 第 78–85、355、368 行。圈 `richTextAssets`、两座工厂套在一起、`demoRuntime`、`selectedManifestIds` 里的 `'web-domain-demo'`。打开 home-web，确认搜不到这两个工厂。
6. 打开 `50-cde-base-dml.sql` 测试单表/树表块和 `NAMEWTA-RICHTEXT-DML-001`。圈三张 C 型 component、单表没有 import F、树表有 `demo:tree:export`、富文本有 `common:richtext:upload`。再打开三份 Controller：确认 `/page` 与 `/importData` 在单表、树导出是 GET、富文本 `/assets` 与 `/{id}` 都要 `demo:richtext:query`。

## 总结、词汇表与下一步

- **宏观三键实验室：** `createDemoService` 印表/树 10 枪并夹住一座富文本小厨房；`createRichTextService` 印 5 支 POST/GET 并把资产口挂上；`createDemoWebDomain` 把三张页订进 `web-domain-demo`。墙上的菜 ≠ 厨房方法全集 ≠ 后端窗全集。
- **(a) `createDemoService`：** 根 `index.ts` freeze；路径 `encodeId`；只给单表投影；缺省资产口关闭。本课**不**把 Java 三份 Controller 再标一遍 covered。
- **(a) `createRichTextService`：** `rich-text.ts` freeze；写路径全 POST；`assets` 是厅堂门卫。厅堂不单独导出这份工厂。
- **(a) `createDemoWebDomain`：** 先 fail-closed 要 runtime，再冻三键与三组权限串，用 `h(page, { runtime })` 把厅堂的 `demoService` 喂进页面。本课**不**把矩阵那一行九个工厂一起闭合。
- **页面实际调用：** 单表五枪 + download 导出；树表五枪 + 浏览器拼树；富文本五枪 + 资产口。page/import 没有按钮。树表导出没有按钮。富文本页不走 hasPermi，也不走 runtime.confirm/success。

词汇表：`createDemoService` / `DemoService` / `createRichTextService` / `RichTextService` / `projectDemoTransport` / `encodeId` / `RichTextAssetsPort` / `demoDomainModule` / `createDemoWebDomain` / `DemoWebRuntime` / `requireDemoWebRuntime` / `componentKey` / `web-domain-demo` / `buildTree` / resource metadata / host `download` / default assets port。

下一步：单表八扇是 OBJ-74。树表六扇是 OBJ-75。富文本与 OSS 资产解析是 OBJ-76。OSS 直传三层是 OBJ-29。厅堂插头是 OBJ-07。导航 host 是 OBJ-20。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/admin-web` | 厅堂 `richTextAssets` + 两座工厂；registry 注入 runtime；`selectedManifestIds` 含 `web-domain-demo`；home 无此工厂 | `application/services.ts` 第 54–69 行；`router/adminManifestRegistry.ts` 第 78–85、355、368 行；`home-web` `selectedManifestIds` | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}/demo` | 厨房 facade、投影、富文本工厂、web-domain 三键、runtime、子路径 export | 各包 `package.json` `exports` 与 `src` | 2026-09-17 |
| S-008 | 登记表 classic | `wta-demo` 保持 classic；本课只对照不改层次 | engineering-standards 模块模式 | 2026-09-17 |
| S-010 | `50-cde-base-dml.sql` | 测试菜单父 id `1761400000000000005`；三张 C 型 component；单表无 import F；树表有 export F；富文本块 `NAMEWTA-RICHTEXT-DML-001` | 第 158–169、1709–1719 行附近 | 2026-09-17 |
| S-L007-01 | L-007 厅堂组合 | 先资产口再两座工厂；`createRichTextService` 不单独导出；缺省口关闭 | `lessons/L-007-admin-web-composition.md` | 2026-09-17 |
| S-L029-01 | L-029 OSS 直传 | `ossUploadClient.upload` 不是本课格子；本课只认 policy 与交叉线 | `lessons/L-029-oss-frontend.md` | 2026-09-17 |
| S-L074-01 | L-074 单表八扇 | `/list` vs `/page`；import 无前端；export 是 POST；厨房五枪对照 | `lessons/L-074-test-demo.md` | 2026-09-17 |
| S-L077-01 | `domains/demo/src/index.ts`；`index.test.ts` | 10 枪 URL/动词；`encodeIds`；缺省 richText；无 page/import/export | `createDemoService`；两个合同 `it` | 2026-09-17 |
| S-L077-02 | `domains/demo/src/transport.ts`；`transport.test.ts` | 单表投影去 generated 字段（含 version） | `projectDemoTransport` | 2026-09-17 |
| S-L077-03 | `domains/demo/src/rich-text.ts` | 五枪 POST/GET；`assets` 原样挂上；路径 `encodeId` | `createRichTextService` | 2026-09-17 |
| S-L077-04 | `web-domains/demo/src/index.ts`；`index.test.ts`；`runtime.ts` | 三键顺序；三组权限；`requireDemoWebRuntime` | `createDemoWebDomain`；两个 `it` | 2026-09-17 |
| S-L077-05 | `DemoPage.vue`；`TreePage.vue`；`composables.ts` | 修改成功；download 导出；buildTree；操作成功；无树导出 | 模板按钮与 script | 2026-09-17 |
| S-L077-06 | `RichTextPage.vue` | 五枪；version 从 rows；无 hasPermi；ElMessage 不走 runtime | `save` / `remove` / `load` | 2026-09-17 |
| S-L077-07 | `TestDemoController.java`；`TestTreeController.java`；`TestRichTextController.java` | page/import/export 窗；树 GET 导出；assets 与 GET `/{id}` 都要 query | `@RequestMapping` 与映射方法 | 2026-09-17 |
| S-L077-08 | `web-domains/demo/AGENTS.md`；`domains/demo/AGENTS.md`；`package.json` exports | 文档仍写两页；export 无 `./rich-text` / `./test-rich-text` | 包名片与过期索引 | 2026-09-17 |

## 文字等价物

本课所有 ASCII 图与对照表都可以用这段话代替：已经登录的管理员在 admin-web 打开测试菜单下的三张演示页。导航用 `sys_menu.component` 去对 `createDemoWebDomain` 冻住的三个 `componentKey`。页面只拿到 `DemoWebRuntime`。真正的 `/demo/demo/*` 与 `/demo/tree/*` 字符串写在 `createDemoService` 里，共十枪，单表 list/get 会投影并丢掉 version；富文本五枪写在 `createRichTextService` 里，写路径全是 POST，再由厅堂把这座小厨房塞进 `demoService.richText`。贴图不走这十五枪，走厅堂资产口：上传转 OSS 直传，解析打 `GET /demo/rich-text/assets`。单表导出走宿主 download；树表导出、自定义分页、导入没有前端扳机。home 与 sso 没有这座厅。九个 web-domain 工厂那一行不要因为本课盖章。
