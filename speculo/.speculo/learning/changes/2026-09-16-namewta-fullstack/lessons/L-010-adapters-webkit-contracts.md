---
lesson_id: L-010
objective_ids: [OBJ-10]
claimed_cells: [A:adapters, A:web-kit, A:api-contracts]
estimated_minutes: 37
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: three-rooms-on-disk
    minutes: 9
  - segment: deep-explanation
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-007, S-009]
---

# Lesson 010：插头、店徽、运输箱

## 学完你能做什么

你能指着 `frontend/packages/` 说出三间房间各自拥有什么，而不是把它们揉成「前端公共包」：

1. **`adapters/*`**：在浏览器里**实现**平台端口（HTTP、加密、存储、上传）。厅堂买插头，不买插座规格纸。
2. **`web-kit/*`**：已经有真实消费者的**共用壳**——权限指令、上传控件、富文本编辑器。不是业务页面，也不是令牌抽屉。
3. **`api-contracts`**：OpenAPI **生成的运输箱**。立法原件是修订里的 `source.json`；`current.json` 只是指针；`generated/openapi.ts` **禁止手改**。

你能再说三句方向：

- adapter 只依赖 `platform`（axios 那只插头额外依赖 `platform-http`）。
- web-kit 只依赖 `platform`（或另一个 web-kit），不拥有 OSS 票据、不读写 `Admin-Token`。
- transport 的消费者是 **domain**；adapter / platform / 页面都不得把 `generated/` 当自己的账本。

本课认格子：`A:adapters`、`A:web-kit`、`A:api-contracts`。L-009 已经认过端口形状；本课认**谁把形状做成浏览器真东西**、**谁挂店徽**、**谁保管运输箱**。OSS 分片/续传状态机留给 L-029；`services.ts` 工厂全表留给 L-007；表 → HTTP → fetch 的合同顺序留给 L-005。本课要把 L-005 故意留给这里的 CLI 测试讲完。

口诀里若出现「web-kit = 指令 + token helpers」，那是过期简称。2026-09-16 工作树里，令牌读写在 `@namewta/adapter-storage-browser`，钥匙名由 App 传入（`Admin-Token` / `Home-Token`）。web-kit 目录里搜不到 `token`。以字节为准。

## 先把宏观地图放在桌上

L-004 把书包拆开：厅堂（App）可以有三家，中央厨房（domain / web-domain）只留一份，插座规格（platform ports）统一。L-009 认规格纸。本课走进规格纸旁边的三间小作坊。

```text
  App（厅堂）
    │  买插头、挂店徽、起名字（Admin-Token）
    ├──────────────► adapters/*     插头：把端口做成浏览器真动作
    ├──────────────► web-kit/*      店徽：指令、上传壳、富文本壳
    │
    └─► web-domain ─► domain ─► platform（规格纸）
                         │
                         └──► api-contracts（运输箱，生成物）
```

三间作坊不是同一类活：

| 房间 | 卖什么 | 不卖什么 |
| --- | --- | --- |
| adapters | 运行时实现：axios 出门、localStorage 存令牌、RSA/AES 加解密、浏览器直传对象 | 领域模型、页面、菜单、默认 Client、默认会话键 |
| web-kit | 多页共用的 Vue 壳：`v-hasPermi`、FileUpload、RichTextEditor | 业务列表页、令牌语义、OSS 网关、App Store |
| api-contracts | 从活跃 OpenAPI 修订生成的 TypeScript 传输类型 | 领域模型、请求实现、页面、认证策略 |

**类比失效处：** 「作坊」不是商场物业公司。没有外人替你拦错房间。门禁是 `frontend/tooling/architecture` 的 `runtimeLayerAllowlist`：

- `adapter` 只许连 `platform`
- `web-kit` 只许连 `platform` 和 `web-kit`
- `api-contracts` 出边是空集（它不依赖任何人）
- `domain` 可以连 `api-contracts`
- adapter / platform **不许**连 `api-contracts`（架构测试有专门的拒绝用例）

失效点还有三个：

1. 目录在、包未激活。`taro-request` / `taro-storage` / `web-kit/ui-element` 今天都只有 README，但**不是同一条门禁**。架构 `inactivePlaceholders` 只登记 `apps/{client-web,mobile-web,miniapp-taro}` 和 `packages/adapters/taro-{request,storage}`：这几条路径出现 `package.json` 就是 `placeholder-activation`。`ui-element` **不在**名单里；workspace 是 `packages/*/*`，给它加清单会变成活 web-kit 包。真正门槛是 README / `FE-CRUD-006`（两个真实消费者），架构不会替你拦提前开的 Element 库。
2. 厅堂不一定三间作坊都买。admin-web 买四只浏览器插头 + 两只店徽；home-web 买三只插头、**零只** web-kit；sso-web 一只都不买，用 `fetch`。
3. 运输箱已经有某领域的类型，不等于那个 domain 已经从箱子里把货搬进自己的账本。Profile 今天就是这样（L-005 认过映射缺口）。

## 三间房间在磁盘上长什么样

这一节是 deep 的硬证据。2026-09-16 在 `/srv/WTA-plus` 对过工作树。先 `ls`，再背口诀。包 README / `api-contracts/AGENTS.md` 仍可能写「快照不含 `/profile/**`」——那是过期说明书。

### 1. `packages/adapters/`：四只活插头，两只占位

目录里六个名字。有 `package.json` + `src/` 的是四只浏览器实现；另外两只是 README 占位。

| 目录 | 包名 | 实现的端口 | `package.json` 依赖 | 公开出口 |
| --- | --- | --- | --- | --- |
| `axios-browser/` | `@namewta/adapter-axios-browser` | `HttpClient`（经 axios） | `platform-contracts`、`platform-http`、`axios` | `createAxiosBrowserAdapter`；另出口 `downloadWithAxios`、`extractAxiosErrorMessage` |
| `crypto-browser/` | `@namewta/adapter-crypto-browser` | `CryptoPort` | `platform-contracts`、`crypto-js`、`jsencrypt` | `createBrowserCryptoAdapter` |
| `storage-browser/` | `@namewta/adapter-storage-browser` | `TokenStorage` / `SessionStore` | 只有 `platform-contracts` | `createBrowserTokenStorage`、`createBrowserSessionStore` |
| `oss-upload-browser/` | `@namewta/adapter-oss-upload-browser` | `UploadClient` | 只有 `platform-contracts` | `createOssUploadClient` |
| `taro-request/` | （无包名） | 未来小程序 HTTP | **无** `package.json` | 无 |
| `taro-storage/` | （无包名） | 未来小程序存储 | **无** `package.json` | 无 |

**axios 插头。** 公开出口不止工厂：`createAxiosBrowserAdapter`、`downloadWithAxios`、`extractAxiosErrorMessage`（另再导出 `isHandledError`）。`createAxiosBrowserAdapter(options)` 造一只 axios 实例。App 必选字段比「六项注入」多：`baseURL`、`client`、`encryptionEnabled`、`errorPresenter`、`getLanguage`、`getToken`、`onUnauthorized`、`repeatSubmissions`、`resolveErrorCode`、`serializeParams`、`successCode`；`crypto` 只在开加密时必填，`timeout` / `now` 可选。请求拦截器写 `clientid`、可选 `Authorization: Bearer …`、可选加密头 `encrypt-key`；响应里 `code === 401` 走 `onUnauthorized`，其它非成功码交给 `errorPresenter`。防重柜由 App 提供：admin 用 session 缓存；home 写成 `{ get: () => null, set: () => undefined }` 空柜（500ms 窗口细节不展开）。`downloadWithAxios` 走同一只 client 的 `post` + blob；admin `http.ts` 的 `download()` 实调用它，home **不** import。它**不**选择 Client，**不**拥有领域 URL 表。`platform-http` 的工作区依赖**只出现在这一只插头**的 `package.json` 里——两个厅堂都不声明 `@namewta/platform-http`。

**存储插头 = 令牌 helper 的真正主人。** `createBrowserSessionStore({ key })` 把平台的 `SessionStore` 接到 `localStorage`（可注入假存储）。钥匙字符串必须由 App 传入：admin 是 `Admin-Token`，home 是 `Home-Token`。测试覆盖「同一块 storage、两把钥匙互不串读」。包 README 写死：不定义令牌语义、默认会话键或跨 App 共享策略。

**加密插头。** `createBrowserCryptoAdapter({ publicKey, privateKey })` 实现 `encryptRequest` / `decryptResponse`。缺密钥直接抛错，不能静默改明文。谁开加密、用哪对钥匙，是 App 的 `VITE_APP_ENCRYPT` 和 RSA 环境变量，不是 adapter 的策略。

**上传插头。** `createOssUploadClient({ clientId, gateway, getToken, transfer? })` 返回 `UploadClient`：`upload` / `resolve` / `remove`。网关（init/sign/complete/abort）由 App 从 `systemService.resources.oss` 注入；直传字节走 `transferToOss`（开发环境可换成代理）。本课只认所有权：它住在 adapter，实现 `platform-contracts` 的 `UploadClient`，**不**依赖 `platform-http`，**不**依赖 web-kit。分片窗口、续传 IndexedDB、abort 回滚是 L-029 的格子。

**占位。** `taro-request/README.md` 与 `taro-storage/README.md` 第一行就是 `placeholder`。架构源码 `inactivePlaceholders` 把这两条路径和未激活 App 写在一起：不得出现 `package.json`。激活小程序规格之前，不要假装已经有 Taro 插头。

厅堂怎么买插头（抽查，不是 L-007 工厂表）：

| App | axios | crypto | storage | oss-upload | 会话钥匙 |
| --- | --- | --- | --- | --- | --- |
| admin-web | 有 | 有 | 有 | 有 | `Admin-Token` |
| home-web | 有 | 有 | 有 | **无** | `Home-Token` |
| sso-web | 无 | 无 | 无 | 无 | SSO cookie；`ssoApi.ts` 用 `fetch` |

### 2. `packages/web-kit/`：三只活店徽，一只占位

| 目录 | 包名 | 当前消费者 | 依赖 | 职责 |
| --- | --- | --- | --- | --- |
| `permission/` | `@namewta/web-kit-permission` | **admin-web** `directive/index.ts` | `platform-permission`、`vue` | `installWebPermissionHost` 注册 `v-hasPermi` / `v-hasRoles` |
| `file-upload/` | `@namewta/web-kit-file-upload` | **admin-web** `components/FileUpload`、`ImageUpload` | `platform-contracts`、`element-plus`、`vue` | Vue 上传壳；`props.client: UploadClient` |
| `rich-text/` | `@namewta/web-kit-rich-text` | **web-domain-demo** `RichTextPage.vue` | wangeditor、dompurify、vue（**无** platform） | 编辑器 / 查看器 / 内容规范化 |
| `ui-element/` | （无包名） | 无 | 无 `package.json` | README 占位：Element 控件仍归 `apps/admin-web/src/components`。**不在** `inactivePlaceholders` |

**指令。** `installWebPermissionHost(app, provider)` 不读 Store、不读 Router、不碰会话。`provider()` 必须给出 `AccessEvaluator`；没有 evaluator 或绑定值不是非空字符串数组，就**拆掉 DOM 节点再抛错**（失败关闭）。admin-web 的 `createAdminAccessEvaluator` 是厅堂自己的 provider。架构还专门禁止 admin 在 `src/directive/permission/` 私藏一套指令，或 `.directive('hasPermi'|'hasRoles')` 自己注册——必须走 web-kit 公开入口。home-web **不**依赖这个包：它把 `hasPermission` 当函数传进 self-profile runtime。

**上传壳 ≠ 上传插头。** `FileUpload.vue` 的 `http-request` 调 `createUploadRequest(props.client, …)`，而 `client` 的类型是平台 `UploadClient`。web-kit **没有** `@namewta/adapter-oss-upload-browser` 依赖。厅堂包装器 `apps/admin-web/src/components/FileUpload/index.vue` 才把 `ossUploadClient` 和 feedback 塞进去，再由 `adminManifestRegistry.ts` 以 `fileUpload: WorkflowFileUpload` **注入** workflow runtime。`web-domain-workflow` 的 `package.json` 不声明 web-kit-file-upload。注入不是反向依赖。

**富文本。** 这是「web-domain 可以直接依赖 web-kit」的活样本：allowlist 允许 `web-domain → web-kit`。demo 页面 import 编辑器，资产上传仍走 App 组合出来的 `ossUploadClient`（L-007 / L-029），编辑器自己不认 MinIO。

**提取门槛。** `FE-CRUD-006` / `ui-element` README：只有至少两个真实消费者形成稳定边界才提取。admin 自己的 Element 零件先留在厅堂，不要提前开一个假的共享组件库。给 `ui-element` 加 `package.json` **不会**走 `placeholder-activation`——那条规则只管 Taro 插头和未激活 App。

**home-web 与 sso-web 都不声明 `@namewta/web-kit-*`。** 店徽不是每个厅堂的入场券。

### 3. `packages/api-contracts/`：指针、保险柜、复印件

包名 `@namewta/api-contracts`。`exports` 只有 `".": "./src/index.ts"`。`src/index.ts` 从 `../generated/openapi` 再导出 `paths` / `operations` / `components`，并提供 `OpenApiSchema` / `OpenApiOperation` / `OpenApiPath`。页面和 adapter 都不应 import `generated/` 内部文件。

工具把三份路径钉死在 `frontend/tooling/openapi/src/index.mjs` 的 `defaultPaths`：

| 路径 | 工具名 | 角色 | 2026-09-16 抽查 |
| --- | --- | --- | --- |
| `openapi/current.json` | `pointer` | 可翻转的修订指针。整份只有 `{"revision":"<64-hex>"}` | **85 字节**，指向 `adab7988…d321`（邻居 `a1f65734…b50f` 不是指针） |
| `openapi/revisions/<digest>/source.json` | 立法原件 | 不可变 OpenAPI 快照。digest = `sha256(source字节 + provenance)` | 活跃修订 **399** paths，其中 **50** 条 `/profile/**` |
| `openapi/revisions/<digest>/provenance.json` | 来源条 | `backendCommit`、`runtimeEndpoint`、`totals`、`generator` | `paths: 399`，`/v3/api-docs`，`openapi-typescript@7.13.0` |
| `generated/openapi.ts` | `output` | 从**指针指向的那份** `source.json` 生成的 TypeScript | 约 854 KB；已有 `PersonProfileSummaryVo` |
| `src/index.ts` | 公开入口 | 再导出类型别名 | domain 只应从这里 import |

磁盘上 `revisions/` 现在有 **8** 份历史修订。只有指针指向的那一份是活跃合同。旁边多放一份孤儿修订，**不能**改变 last-known-good——工具测试就叫这个名字。

生成工具住在 `frontend/tooling/openapi`（`@namewta/tooling-openapi`），**不是**运行时包。前端根 `package.json` **没有** `openapi:*` 脚本。命令在工具包上：

```text
pnpm --filter @namewta/tooling-openapi openapi:fetch -- --source <url-or-file> --backend-commit <40-hex>
pnpm --filter @namewta/tooling-openapi openapi:generate
pnpm --filter @namewta/tooling-openapi openapi:check
pnpm --filter @namewta/tooling-openapi test
```

三条命令的职责（实现名 `fetchSnapshot` / `generateContracts` / `checkContracts`）：

1. **fetch**：读 `--source`（URL 或文件，默认 runtime 是 `/v3/api-docs`），校验 OpenAPI 3.0/3.1，要求 `--backend-commit` 为 40 位 sha，把不可变修订写入 `revisions/<digest>/`，再**原子翻转**指针。来源无效或不可达时，**保持**上一份活跃修订。
2. **generate**：离线读活跃修订，写入 `generated/openapi.ts`。
3. **check**：在内存里重生成，对比已提交的 `generated/openapi.ts`，**不写盘**。手改生成文件会报 `contract drift detected`。

消费规则：domain 在边界映射。2026-09-16 声明了 `@namewta/api-contracts` 的 domain：`admin`、`system`、`demo`、`notify`、`ai`、`workflow`。**没有** `profile`、**没有** `third`。Profile 的 generated 类型已经在箱子里，service 仍手写 `/profile/**` URL——映射缺口，不是快照缺口。过期 README 不能当证据。

`ARCH-006`：`packages/api-contracts` 生成结果是生成物，应通过 OpenAPI 工具修复，不直接手改。覆盖矩阵把「generated 快照字节」标成 `deferred(generated)`——本课认所有权和工序，不把 854 KB 生成文件当教材正文。

## 核心概念与机制

### 直觉讲解

把前端想成一条商业街。

插座墙上印着规格（platform ports）：「这里要 220 伏 HTTP」「这里要一把带钥匙的抽屉」。电工铺（adapters）按规格做插头：浏览器这一头用 axios 和 `localStorage`；将来若开小程序店，再做 Taro 插头，规格纸不用撕掉。厅堂自己决定插哪一只、抽屉刻什么字（`Admin-Token`）。电工铺不卖菜谱，也不决定今天用哪把钥匙。

招牌铺（web-kit）做已经有两家店都要用的门口铃和菜单灯箱：权限指令、上传按钮、富文本框。它不是后厨，也不保管收银抽屉。谁能点哪个菜，由厅堂请来的保安（`AccessEvaluator`）告诉铃铛；铃铛自己不查花名册。上传灯箱只规定「把文件交给一只叫 `UploadClient` 的手」；那只手是电工铺做的，厅堂在包装纸里塞进去。

复印室（api-contracts）不发明货物。仓库先备货（表 + HTTP），快递员去窗口抄一份送货单锁进保险柜（`revisions/<digest>/source.json`），把柜门标签换成新号码（`current.json`），再复印给会计（`generated/openapi.ts`）。会计（domain）誊进自己的账本。你在复印件上用铅笔改一个字段名，下次复印机会把铅笔印盖掉。柜门标签只有一串 64 位十六进制，搜 `/profile` 会空手而归——那不证明保险柜是空的。

三家店买货不一样：管理厅堂插头、店徽都要；门户只要插头，自己用函数判权限；认人亭连插头都不买，门口用一张 `fetch` 纸条。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 适配器 | adapter | 某运行时对平台端口的实现。当前激活的是浏览器四包；Taro 两包是占位 |
| 平台端口 | port | `packages/platform/*` 里的形状：`HttpClient`、`CryptoPort`、`SessionStore`、`UploadClient`、`AccessEvaluator`。App 可直连若干端口包；HTTP 形状的运行时消费者是 axios adapter |
| 共用壳 | web-kit / shared chrome | 已被真实消费者证明稳定的 Vue 机制（指令、上传/富文本控件）。不是业务 web-domain 页面 |
| 宿主注入 | host injection | App 把包装好的组件或函数传入 web-domain runtime；不是 web-domain import App |
| 令牌存储 | token storage | adapter-storage-browser 实现的 `TokenStorage`/`SessionStore`；钥匙名是 App 的，不是 web-kit 的 |
| 占位 | placeholder | 只有 README、无 `package.json` 的目录；架构禁止提前激活 |
| 修订指针 | revision pointer | `openapi/current.json`：只含活跃 `revision` digest，可原子翻转 |
| 不可变快照 | immutable snapshot | `revisions/<digest>/source.json` + `provenance.json`；digest 绑定字节与来源条 |
| 生成运输箱 | generated transport | `generated/openapi.ts`：从活跃快照生成的 HTTP 形状；禁止手改 |
| 最后可用修订 | last-known-good | fetch 失败时保持指针与保险柜不动 |
| 合同漂移 | contract drift | check 发现已提交 `generated/openapi.ts` 与内存重生成不一致 |
| 来源漂移 | provenance drift | 活跃修订的 `source.json`/`provenance.json` 对不上 digest |
| 依赖允许表 | allowlist | `runtimeLayerAllowlist`：谁可以 import 谁 |

**Adapter ≠ port。** 端口是规格；适配器是实现。厅堂依赖 adapter 包，不把 axios 类型泄漏进 domain（`FE-CRUD-002`）。

**Web-kit ≠ domain 页面。** 通知列表、用户表格住在 `web-domains/`。web-kit 没有资源路径，没有 Controller 对齐。

**Web-kit ≠ token helper。** 令牌 helper 的英文名字在磁盘上叫 `createBrowserTokenStorage` / `createBrowserSessionStore`。App 的 `application/session.ts` 只是给钥匙起名。

**Transport ≠ domain model。** `OpenApiSchema<'TestDemoVo'>` 是箱子；`DemoVO` 是账本。页面状态用账本。

**Pointer ≠ snapshot。** 在 `current.json` 里找不到 path。

### 机制/因果链

先走「浏览器发出一条业务 GET」时三间房间怎么排队，再走「运输箱更新」和「挂一只新店徽」。

**A. 一次管理端 GET 出门（插头）。**

1. App 启动时 `createBrowserSessionStore({ key: 'Admin-Token' })` 造抽屉。
2. App `createAxiosBrowserAdapter({ getToken, client, repeatSubmissions, … })` 造插头。需要加密时再 `createBrowserCryptoAdapter`。blob 下载不走这条 GET：admin 另调公开函数 `downloadWithAxios`。
3. domain 服务拿到的是平台 `HttpClient.request`，URL 写在 domain，**不是**写在 adapter。
4. 插头补 `clientid`、Bearer、语言；axios 真正出网。
5. 响应 `code` 不是成功：插头变成 `TransportError`，该提示的提示，401 交给 App 注入的 `onUnauthorized`（`requestRelogin` 是 platform-auth，L-009）。
6. domain 若声明了 api-contracts，把 JSON 投影成自己的模型。Profile 今天跳过第 6 步，仍手写 URL。

因果：换插头（将来 Taro）不必改 domain URL 表；换钥匙名不必改存储实现。把 URL 写进 adapter，第二个 App 就会被绑死。

**B. 一次直传（插头 + 店徽，所有权不混）。**

1. adapter `createOssUploadClient` 实现 `UploadClient.upload`。
2. web-kit FileUpload 只认识 `props.client.upload`。
3. App 包装器把第 1 步的实例塞进第 2 步，再注入 workflow。
4. web-domain 页面渲染注入来的组件，自己不 import adapter。

因果：壳可以给第二个页面复用；票据协议仍只有一份插头。把直传协议写进 Vue 组件，第二个终端（或测试）就要复制 MinIO 方言。

**C. 更新运输箱（复印室工序，本课的 CLI 深证据）。**

1. 后端表和 HTTP 已经可交付（L-005 的顺序；本课不重讲 SQL）。
2. `openapi:fetch --source … --backend-commit <40-hex>`：校验 → 写不可变修订 → 原子写指针。`--backend-commit` 不是 40 位 sha 会直接失败，指针不动。
3. 同一份 `source.json`、不同 `backendCommit`，会激活**另一份** digest（测试：identical snapshots with different provenance）。保险柜按来源条分柜，不只按正文。
4. 故意放一份未激活的 `revisions/<orphan>/`，`check` / `generate` 仍只看指针。孤儿不能改合同。
5. `openapi:generate` 从活跃修订写出 `generated/openapi.ts`。
6. `openapi:check` 重生成后字节对比。手改 output 加一行注释 → `contract drift detected`，磁盘上的生成文件在 check 过程中**不被工具改回去**（check 不写盘；人要跑 generate 才盖掉）。
7. 改活跃 `source.json` 却不重算 digest → `provenance drift detected`；generate 和 check 都拒绝。
8. 无效来源 / 缺失文件 / OpenAPI 2.0：抛错，指针和上一份修订字节保持原样（last-known-good）。
9. HTTP 源带 `user:secret` 或 query token：报错信息只保留 `https://host/path`，诊断里不能出现 secret / token。

因果：生成物永远能从活跃修订重放。手改复印件，下一次 generate 或 CI check 会揭穿。手改指针去「假装」纳入一条 path，digest 对不上 provenance，同样揭穿。

**D. 提取一只新店徽。**

局部逻辑先留在 web-domain 或 App。出现第二个真实消费者、合同稳定，再搬进 web-kit。`ui-element` 还没跨过这道门槛。提前给它加 `package.json`，架构**不会**当占位激活拦住你——拦住你的是 README / `FE-CRUD-006`，不是 `inactivePlaceholders`。

## 图、表或文本图

**图题 / caption：** 三间作坊与允许的箭头。alt：App 连 adapter 与 web-kit；adapter 只连 platform；domain 连 api-contracts；生成物由工具写入。

```text
                    admin-web                         home-web              sso-web
                    买 4 插头 + 2 店徽                 买 3 插头              不买
                    FileUpload 注入 workflow           无 web-kit             fetch
                           |                               |                    |
                           +---------------+---------------+                    |
                                           v                                    |
                         ┌─────────────────────────────────────┐                |
                         │  adapters（浏览器实现）              │                |
                         │  axios ──► platform-http            │                |
                         │  crypto / storage / oss-upload      │                |
                         │      ──► platform-contracts         │                |
                         │  taro-* = README 占位               │                |
                         └─────────────────────────────────────┘                |
                                           ^                                    |
                         ┌─────────────────┴───────────────┐                    |
                         │  web-kit（共用壳）               │                    |
                         │  permission 指令 ← evaluator     │                    |
                         │  file-upload ← UploadClient 注入 │                    |
                         │  rich-text ← web-domain-demo     │                    |
                         │  ui-element = README 占位        │                    |
                         │  （不在 inactivePlaceholders）   │                    |
                         └─────────────────────────────────┘                    |
                                           |                                    |
                         domain ──import──► api-contracts                       |
                                      src/index.ts                              |
                                           ^                                    |
                     tooling/openapi  fetch/generate/check                      |
                           |                                                    |
                           +-- current.json  (pointer, 85B, …d321)              |
                           +-- revisions/<digest>/source.json  (立法原件)         |
                           +-- generated/openapi.ts  (复印件, 勿手改)             |
                                                                                |
                     allowlist 禁止：adapter→api-contracts；platform→api-contracts
                     allowlist 允许：domain→api-contracts；web-domain→web-kit
                     sso-web ──────────────────────────────────fetch──► /sso/*
```

**文字等价物：** 顶上一排放三个厅堂。左边管理厅堂同时买插头和店徽，并把上传壳注入工作流 runtime。中间门户只买 axios / crypto / storage 三只插头，权限用函数而不是 `v-hasPermi`。右边认人亭不进这三间作坊，HTTP 走自己的 `fetch`。中间一层是 adapters：axios 指向 `platform-http`，其它浏览器插头指向 `platform-contracts`；Taro 两包画成虚线占位，且在架构 `inactivePlaceholders`。旁边一层是 web-kit：指令向厅堂要 evaluator，上传向厅堂要 `UploadClient`，富文本被 demo 的 web-domain 直接引用；`ui-element` 今天只有 README，**不在**架构占位名单。再往下 domain 只从 api-contracts 的公开入口取运输箱。箱子由 `tooling/openapi` 写成三份文件：指针（活跃 digest 以 `…d321` 结尾）、修订原件、生成 TypeScript。图上的禁止箭头写明 adapter 和 platform 不得依赖运输箱。sso-web 的箭头单独指向后端 `/sso/*`。

**图的边界：** 本图不保证三个 App 已生产部署。本图不展开 OSS 分片算法、Sa-Token 会话写入、菜单动态路由。本图不把 8 份历史修订都画成活跃合同，只认指针那一份。包 README 过期不改这张图。`generated/openapi.ts` 的 PUT 复印件不是新接口样板（L-005）。

**图题 / caption：** 令牌抽屉和店徽不是同一间屋。alt：App 命名空间隔离的 session store 与 web-kit 指令分流。

```text
  admin-web/application/session.ts
        key: 'Admin-Token'
        createBrowserSessionStore  ──►  adapter-storage-browser
                                              │
                                              │  同一 localStorage
                                              v
  home-web/application/session.ts             互不串读（测试写过）
        key: 'Home-Token'

  admin-web/directive/index.ts
        installWebPermissionHost(app, createAdminAccessEvaluator)
              ──►  web-kit-permission   （不 getToken）

  home-web
        无 @namewta/web-kit-*
        hasPermission 函数传入 self-profile runtime
```

**文字等价物：** 两把钥匙都由存储适配器实现，差别只在 App 传入的 `key`。权限指令是另一条线：admin 用 web-kit 挂指令，指令向 evaluator 问路，不打开抽屉。home 不装指令。把「token helpers」写进 web-kit，会在这张图上找不到文件。

**图的边界：** 不讲 Redis 里的服务端会话（L-084）。不讲 SSO cookie 怎么换 `Admin-Token`（L-058）。只钉前端浏览器抽屉的所有权。

## 正例、反例与边界

**正例 1：** admin-web / home-web 的 HTTP 经 `createAxiosBrowserAdapter` 出门。两份 `package.json` 都依赖 `@namewta/adapter-axios-browser`，都不依赖 `@namewta/platform-http`。admin `http.ts` 的 `download()` 再调公开函数 `downloadWithAxios`；home 只 import 工厂和 `extractAxiosErrorMessage`，不买下载出口。

**正例 2：** 会话钥匙不同。`admin-web/src/application/session.ts` 与 `home-web/src/application/session.ts` 都从 `@namewta/adapter-storage-browser` 调用 `createBrowserSessionStore`，分别传入 `Admin-Token` 和 `Home-Token`。adapter 测试用同一块假 storage 证明两把钥匙互不读取。

**正例 3：** admin 权限指令走公开入口：`directive/index.ts` 只有 `installWebPermissionHost` 和一枚厅堂私有的 `copyText`。没有 `src/directive/permission/` 私货。

**正例 4：** 上传壳与上传插头分离。web-kit FileUpload 声明 `client: UploadClient`；厅堂包装器注入 `ossUploadClient`；registry 再注入 workflow。三跳都是 App 朝外指，没有 web-domain → adapter。

**正例 5：** demo 列表映射走公开入口：`domains/demo/src/transport.ts` `import type { OpenApiSchema } from '@namewta/api-contracts'`，再 `projectDemoTransport`。不 import `generated/openapi.ts`。

**正例 6：** 工具测试覆盖「手改 generated → check 报 drift 且不写盘」「无效来源保持 last-known-good」「孤儿修订不影响活跃合同」「HTTP 报错脱敏」。这是 `A:api-contracts` 的函数级证据，不是 L-005 的 overview。

**反例 1：** 在 `admin-web/src/api/http.ts` 再包一层 axios。中央厨房的插头被搬回厅堂。架构把 App 内 `src/api/*` 当违规信号。

**反例 2：** domain 的 service `import axios from 'axios'` 或 import `@namewta/adapter-axios-browser`。domain 必须只看见 `HttpClient`。浏览器实现换掉时，领域包不该一起重写。

**反例 3：** web-kit FileUpload 里直接 `createOssUploadClient` 或写 MinIO URL。壳抢走了插头的工作，第二个消费者无法换网关。

**反例 4：** 把 `createBrowserSessionStore` 放进 web-kit-permission，「反正都是登录相关」。指令包会被迫依赖存储，home 那种不装指令的厅堂也会被绑住。磁盘上它们不是一个包。

**反例 5：** 给 `taro-request` 加一份 `package.json`「先占坑」。`placeholder-activation` 规则会判失败。占位只许 README。把同一条规则套到 `ui-element` 是错的：架构名单没有它；加清单会经 `packages/*/*` 变成活 web-kit 包，拦住你的是 `FE-CRUD-006`。

**反例 6：** 手改 `generated/openapi.ts` 加一个 Profile 字段，或手改 `current.json` 把 revision 改成邻居 digest 来「切换合同」。前者 check 报 drift；后者若修订不存在或 provenance 对不上，generate/check 失败。活跃指针是 `adab7988…d321`；`…b50f` 是邻居 `a1f65734…`，不是柜门标签。正确工序是 fetch（落修订 + 翻指针）→ generate。

**反例 7：** 页面 `import type { paths } from '@namewta/api-contracts/generated/openapi'` 或把 `OpenApiSchema` 当 Vue 表单状态。`FE-CRUD-001`：页面不得直接把生成类型当领域状态。

**反例 8：** adapter 或 `platform-http` 声明 `@namewta/api-contracts`。架构测试标题就是 `rejects adapter to api-contracts` / `rejects platform to api-contracts`。

**反例 9：** 看见 README 写「`current.json` 不含 `/profile/**`」，就去手写平行 transport。打开指针指向的 `source.json` 和 `generated/openapi.ts`：50 条 `/profile/**` 和 `PersonProfileSummaryVo` 已经在。缺口在 `domains/profile` 的映射。

**反例 10：** 因为 workflow 页面出现了上传按钮，就让 `web-domain-workflow` 依赖 `@namewta/web-kit-file-upload` 并自己 new 一个 OSS client。正确方向是 host 注入。

**边界：**

- sso-web 不用这三间作坊，仍是 App。认人厅的 HTTP 所有权在 `ssoApi.ts`（L-008 / L-058），不是本课的反例。
- home 无 web-kit 合法。不要为了「对称」给门户硬装 `v-hasPermi`。
- `createOssUploadClient.upload` 的成功/abort 路径是 L-029 的格子；本课只认它住在 adapter。
- Vite auto-import `d.ts` 与 generated 快照字节在矩阵里 `deferred(generated)`。本课不把生成文件当手改许可证。
- `docs/fm/**` 静态模板不是这三间作坊的主人。

## 变式与迁移

- **变式 A：新开一个浏览器能力（例如新的下载方式）。** 先问：这是端口形状还是一次厅堂私货？形状进 `platform-contracts`（或已有 port）；浏览器实现进 `adapters/*`；App 显式构造。不要塞进 web-kit，也不要写进 domain。今天文件下载已经有 `downloadWithAxios`（admin `download()` 在用），不是新房间。
- **变式 B：第二个页面也要同一套上传按钮。** 壳已经在 web-kit。新页面继续吃注入的 `UploadClient`，不要复制 `FileUpload.vue` 到 web-domain。
- **变式 C：home 以后要按钮级权限指令。** 可以开始依赖 `@namewta/web-kit-permission`，自己提供 evaluator。不要复用 admin 的 `createAdminAccessEvaluator` 文件（App 互引禁止）。这会让 permission 包真正拥有「两个厅堂消费者」。
- **变式 D：小程序规格激活。** 按 README 实现 Taro 插头，补 `package.json`、合同测试、真机证据，并从 `inactivePlaceholders` 拿掉路径。不得让 Taro 包依赖 axios-browser 或 Web DOM。
- **变式 E：HTTP 加了一个字段。** 不改 adapter，不改 web-kit。走 L-005：表 → HTTP → fetch 落修订 → 翻转指针 → generate → domain 映射 → 页面。本课只强调 generate 的输入是指针指向的 `source.json`，输出是 `generated/openapi.ts`。
- **变式 F：Profile domain 接上运输箱。** 让 `domains/profile` 声明 `@namewta/api-contracts`，用 `OpenApiSchema` 替换手写 URL/类型。不要先手改 generated。README 里的缺口段落在映射完成后才能当历史。
- **变式 G：发现 generated 与后端在线文档不一致。** 先确认后端合同可交付，再 fetch（带 40 位 sha），不要在 `generated/openapi.ts` 里补类型「顶一下编译」。
- **迁移口诀：** 规格（port）→ 插头（adapter）→ 厅堂注入 → 壳（web-kit）显示；运输箱单独一条：fetch → 指针 → generate → domain 映射。跳步就会出现「页面能点、插头不知道 Client」或「复印件有货、账本手写旧地址」。

## 常见误区

1. **「adapters / web-kit / api-contracts 都是公共组件库。」** 第一间是运行时实现，第二间是壳，第三间是生成运输箱。依赖方向都不一样。
2. **「token helpers 在 web-kit。」** 磁盘上在 `adapter-storage-browser`。web-kit 零处 `token`。App 的 `getToken` 只是给钥匙起名。
3. **「厅堂用了 HTTP，所以 App 该依赖 `platform-http`。」** 消费者是 axios adapter。L-004 / L-009 已说；本课用 `package.json` 再钉一次。
4. **「上传组件自然拥有直传协议。」** 壳拥有按钮和文件列表；协议拥有 adapter；票据网关由 domain/App 注入。
5. **「每个 App 都必须装 web-kit。」** home 没有；sso 没有。缺的是产品选择，不是漏接。
6. **「占位目录可以先加 package.json 方便以后。」** 对 Taro 插头和未激活 App：架构 `inactivePlaceholders` 把这当成激活。对 `ui-element`：架构不拦清单；拦住你的是「还没有两个真实消费者」（`FE-CRUD-006`）。两条门禁不要揉成一条。
7. **「`current.json` 就是快照。」** 它是 85 字节指针，活跃 digest 以 `…d321` 结尾。立法原件在 `revisions/<digest>/source.json`。邻居修订 `a1f65734…b50f` 不是指针。
8. **「手改 generated 比较快，check 再改回来就行。」** check 不写盘，只报警。CI 红了你还得 generate。铅笔印不是合同。
9. **「fetch 失败了合同会空掉。」** 测试保证无效/缺失来源保持 last-known-good。不要为了「修 fetch」去删指针。
10. **「adapter 也可以 import OpenApiSchema，反正都是 HTTP。」** allowlist 禁止。插头处理信封和运行时，箱子里的字段表给 domain。
11. **「包 README 写缺口，所以 generated 没有 Profile 类型。」** 说明书过期。以指针、`source.json`、`generated/openapi.ts` 三份字节为准。
12. **「web-domain 用了 FileUpload，所以可以 import `@/components/FileUpload`。」** 那是 App 路径。要注入，不要反向依赖。
13. **「富文本在 web-kit，所以 demo 页面不该 import 它。」** 相反：allowlist 允许 `web-domain → web-kit`。这正是壳的合法消费者。
14. **「根目录敲 `pnpm openapi:check`。」** 前端根脚本没有这一条。命令在 `@namewta/tooling-openapi`。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把地图钉住，没有标准答案栏。

1. 列出 `frontend/packages/adapters/` 六个名字。圈出哪四个有 `src/` 和 `package.json`，哪两个只有 README。打开 axios 与 admin-web / home-web 的 `package.json`，确认 `platform-http` 出现在哪一侧。再打开 axios `src/index.ts`：除了 `createAxiosBrowserAdapter`，公开函数还有 `downloadWithAxios` 和 `extractAxiosErrorMessage`。对照 admin `http.ts` 的 `download()` 与 home 是否 import 下载出口。
2. 打开 `adapter-storage-browser/src/index.ts` 的 `createBrowserSessionStore`，再打开两个厅堂的 `application/session.ts`。写下两把钥匙名。再到 `packages/web-kit/` 里搜 `token`：预期是空。
3. 打开 `web-kit-permission/src/index.ts` 的 `installWebPermissionHost`，对照 `admin-web/src/directive/index.ts`。再确认 home-web 的 `package.json` 有没有 `@namewta/web-kit-*`。打开架构源码搜 `inactivePlaceholders`：名单里有没有 `web-kit/ui-element`。
4. 打开 `web-kit-file-upload/src/types.ts` 的 `client: UploadClient`，对照厅堂 `components/FileUpload/index.vue` 如何注入 `ossUploadClient`，对照 `adminManifestRegistry.ts` 的 `fileUpload`。再打开 `web-domain-workflow/package.json`：它依不依赖 web-kit-file-upload。
5. 打开 `openapi/current.json`（应只有 `revision`，后缀 `…d321`，不是邻居 `…b50f`）。用 digest 打开对应 `revisions/<digest>/source.json` 与 `generated/openapi.ts`，确认 `/profile/person/application` 和 `PersonProfileSummaryVo`。再打开 `tooling/openapi/test/openapi.test.mjs`，找到「manual generated-file edits」和「last-known-good」两则测试的名字。想清楚：手改复印件之后，check 写不写盘。

## 总结、词汇表与下一步

- **adapters**：浏览器里实现端口。活的四只是 axios / crypto / storage / oss-upload。axios 公开面是 `createAxiosBrowserAdapter` + `downloadWithAxios` + `extractAxiosErrorMessage`；防重柜由 App 注入，home 可空。storage 才是令牌 helper。Taro 两只是 README 占位，且在架构 `inactivePlaceholders`。adapter 只依赖 platform；axios 是 `platform-http` 的唯一工作区消费者。
- **web-kit**：共用壳，不是业务页，不是抽屉。活的三只是 permission 指令、file-upload 壳、rich-text 壳。`ui-element` 今天只有 README，**不在** `inactivePlaceholders`；提取门槛是两个消费者，不是架构占位名单。home / sso 可以不买。壳要的 `UploadClient` / `AccessEvaluator` 由厅堂注入或提供。
- **api-contracts**：生成运输箱。`current.json` 指针（85 字节，`adab7988…d321`）→ `revisions/<digest>/source.json` 立法原件 → `generated/openapi.ts` 复印件。只从 `src/index.ts` 消费。禁止手改生成字节。工序在 `@namewta/tooling-openapi`：fetch 保持 last-known-good，check 不写盘，漂移要 generate 才盖掉。
- 方向：App → adapter / web-kit；adapter → platform；domain → api-contracts；adapter/platform ↛ api-contracts。
- 过期说明书（README 说快照没有 `/profile/**`；口诀把 token helpers 算进 web-kit）以工作树为准，记下冲突，不改字节去迎合摘要。

词汇表：adapter / port / web-kit / host injection / token storage / placeholder / revision pointer / immutable snapshot / generated transport / last-known-good / contract drift / allowlist。

下一步：认证切片 L-011 从 `AuthController` 的公开 HTTP 入口往下走。插头已经认过，登录请求怎么写会话是下一间房。OSS 直传状态机见 L-029；admin 接线板工厂全表见 L-007。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web,sso-web}` 的 `package.json`、`application/{http,session,services}.ts`、`directive/index.ts`、`components/FileUpload/index.vue`、`router/adminManifestRegistry.ts` | 三厅堂买插头/店徽的差；钥匙名；axios 组合；OSS client 组合；指令安装；FileUpload 包装与注入 | 工作树 | 2026-09-16 |
| S-006 | `frontend/packages/{adapters,web-kit,api-contracts,platform,domains,web-domains}` | 三间作坊目录、活包与占位、domain 谁声明 api-contracts | 工作树；`frontend/packages/README.md` | 2026-09-16 |
| S-007 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | OpenAPI transport 在 api-contracts；domain 映射；App → adapter/web-kit | 「依赖方向」 | 2026-09-16 |
| S-009 | `.agents/skills/engineering-standards/references/project/00-project-profile.md`；`architecture-and-boundaries.md` ARCH-003/006；`typescript/code-organization-and-comments.md` TS-ORG-001/003；`crud-api-and-pages.md` FE-CRUD-001/002/006 | 生成物不手改；adapter 实现端口；按所有权落位；页面不把生成类型当状态；多消费者才提取 web-kit | 对应规则正文 | 2026-09-16 |
| S-L010-01 | `frontend/packages/adapters/*/package.json`、`AGENTS.md`、`src/index.ts`、`oss-upload-browser/src/client.ts`、`taro-*/README.md`；`apps/admin-web/src/application/http.ts` | 四只活插头的依赖与工厂；axios 公开面 `createAxiosBrowserAdapter` / `downloadWithAxios` / `extractAxiosErrorMessage`；`AxiosBrowserOptions` 必选含 `repeatSubmissions`；`createOssUploadClient` 返回 `UploadClient`；Taro 占位无清单 | 包入口、`DownloadOptions`、admin `download()`；README 首行 `placeholder` | 2026-09-16 |
| S-L010-02 | `frontend/packages/web-kit/{permission,file-upload,rich-text,ui-element}` | 指令失败关闭；FileUpload 的 `UploadClient`；rich-text 被 web-domain-demo 消费；ui-element 只有 README、**不在** `inactivePlaceholders` | `src/index.ts`、`types.ts`、`RichTextPage.vue`、README | 2026-09-16 |
| S-L010-03 | `frontend/packages/api-contracts/openapi/current.json`、`revisions/adab7988…d321/provenance.json`、`generated/openapi.ts`、`src/index.ts`、包 README/AGENTS.md | 指针 85 字节、`revision`=`adab7988…d321`；邻居 `a1f65734…b50f` 不是指针；399 paths / 50 条 `/profile/**`；`PersonProfileSummaryVo`；公开入口；README 过期缺口句 | 指针 `revision`；provenance `totals`；generated schema 名 | 2026-09-16 |
| S-L010-04 | `frontend/tooling/openapi/src/index.mjs`、`cli.mjs`、`README.md`、`test/openapi.test.mjs`、`package.json` | `defaultPaths`；fetch/generate/check；40 位 sha；last-known-good；孤儿修订；provenance drift；手改 drift 且 check 不写盘；URL 脱敏；根 workspace 无 `openapi:*` | `fetchSnapshot` / `checkContracts`；七则 `node:test` | 2026-09-16 |
| S-L010-05 | `frontend/tooling/architecture/src/index.mjs`、`test/architecture.test.mjs`；`frontend/pnpm-workspace.yaml` | allowlist；`inactivePlaceholders` 只有未激活 App 与 Taro 两包、**不含** `web-kit/ui-element`；`packages/*/*` 会收新清单；禁止 adapter/platform → api-contracts；admin 指令必须来自 web-kit | `runtimeLayerAllowlist`；`placeholder-activation`；`rejects adapter to api-contracts` | 2026-09-16 |
