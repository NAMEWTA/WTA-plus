---
lesson_id: L-005
objective_ids: [OBJ-05]
claimed_cells: [C:public-contracts, D:contract-order]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: four-contracts-on-disk
    minutes: 10
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
source_ids: [S-001, S-003, S-004, S-006, S-007, S-019, S-020, S-021, S-022, S-023, S-024, S-025]
---

# Lesson 005：什么算合同，谁先改

## 学完你能做什么

你能把四样东西认成**公共合同**，而不是某个页面的私货：

1. **HTTP/JSON**：路径、方法、字段、错误形状。
2. **SQL 基座**：NAMEWTA 自有表结构与种子数据。
3. **OpenAPI transport**：前端 `packages/api-contracts` 里从活跃修订生成的传输类型。
4. **Java `wta-api`**：模块和模块之间的公开类型，**不是**浏览器合同。

跨端变更时，你能说出顺序：先给出**后端可兼容**的表和 HTTP，再 **fetch 快照 → 翻转指针 → generate → domain 映射 → 页面**。口诀仍是 **表 → HTTP → transport → domain → 页面**；transport 这一步不是「随便改一份 TypeScript」，而是落修订、翻指针、再生成。

本课认格子：`C:public-contracts`、`D:contract-order`。L-002 已经认过 `wta-api` 这间房间；本课认它是一张**条约**。MySQL 六份脚本的升级演练、Tag 差异稿、50 里带变更标识的 DSL 块留给 L-085；本课只钉死「自有产品表权威在 10，自有产品数据权威在 50」。认证 header / `clientid` 留给登录切片。

## 先把宏观地图放在桌上

前面四课认房间。现在认房间之间的**条约**。条约一改，两边都要会签。日记本（某个 Service 私有方法、某个 Vue 按钮颜色）可以换笔，只要条约上的字不变。

四张条约走两条河：

```text
浏览器河：  SQL 基座  →  HTTP/JSON  →  OpenAPI transport  →  domain  →  页面
模块河：    业务模块 A  ←→  Java wta-api  ←→  业务模块 B
            （不经过浏览器，也不经过 api-contracts）
```

**类比失效处：** 「条约」不是法院盖章。没有外部门卫替你拦。失效点有三：

1. 前端 TypeScript `interface` 看起来像合同，但**权威在服务端行为 + 已发布的 JSON / SQL**。只改眼镜，货物标签不会自己改。
2. OpenAPI 生成物是复印件，不是立法机关。立法原件是 `openapi/revisions/<digest>/source.json`。`openapi/current.json` 只是指向那份修订的**指针**。复印件上**已经有** `/profile/**`，不等于 Profile domain 已经从运输箱里把货搬进自己的账本。
3. `wta-api` 的 Java 方法名浏览器看不见。通知模块调 `UserService`，管理端页面调的是 `GET /notify/...`，这是两张纸。

## 四张合同在磁盘上长什么样

这一节是 deep 升级的硬证据。2026-09-16 在 `/srv/WTA-plus` 对过工作树。先认文件，再背口诀。包 README / `api-contracts/AGENTS.md` / `domains/profile/AGENTS.md` 仍可能写「快照不含 `/profile/**`」——那是过期说明书，以这三份字节为准：指针、修订 `source.json`、`generated/openapi.ts`。

### 1. SQL 基座：六份脚本都在，本 Goal 的存储格是 10/50

目录：`release-artifacts/docker/infrastructure/mysql/init/`。磁盘上恰好六份，没有第七份产品基座：

| 文件 | 谁的字 | 本课怎么用 |
| --- | --- | --- |
| `10-cde-base-ddl.sql` | NAMEWTA / 平台业务库结构 | **自有产品表权威**。文件头写「完整最新基座，直接修改本文件」 |
| `20-cde-job.sql` | SnailJob 上游 | 存在；不是新建 NAMEWTA 表的地方 |
| `30-cde-workflow.sql` | Warm-Flow 上游（`flow_*`） | 同上 |
| `40-cde-ai.sql` | Snail AI 上游（`sai_*`） | 同上 |
| `50-cde-base-dml.sql` | NAMEWTA 初始化数据、菜单、回填 | **自有产品数据权威**。文件头写「完整最新基座，直接修改本文件」 |
| `60-cde-nacos.sql` | 独立库 `nacos` 结构快照 | 不得把 Nacos 表建进 `wta-plus` |

本 Goal 的存储格只认 **10 与 50**。六份都要被 Git 跟踪，空环境按 `10 → 20 → 30 → 40 → 50 → 60` 执行；已有环境不重放基座。产品表 `CREATE TABLE` 只进 10；产品数据只进 50。10 里没有 `INSERT`。50 里没有产品 `CREATE TABLE`。

抽查：`10-cde-base-ddl.sql` 有 `CREATE TABLE test_demo`，列是 `test_key`、`value`，以及七个基础字段 `version / create_dept / create_time / create_by / update_time / update_by / del_flag`。`50-cde-base-dml.sql` 有对应的 `INSERT INTO test_demo` 和 `sys_menu` 种子。后端模块里再藏一份 MySQL 方言，不是权威。

打开 50 还会看见 `create temporary table ..._preflight` 和带变更标识的幂等 DML（例如 `NAMEWTA-PASSWORD-DSL-001`、`NAMEWTA-SSO-DSL-001`、`NAMEWTA-BASE-DSL-*`）。那不是「产品表混进了 50」，也不是另开一个 `migrate/` 的许可证。50 是**完整最新数据基座**：种子、菜单、回填，外加为回填服务的临时表。升级演练、Tag 差异稿、这些 DSL 块怎么幂等执行，全部留给 L-085。本课只要你看见 `CREATE` 时先问：这是产品表还是临时预检表？产品表只该出现在 10。

### 2. HTTP/JSON：路径 + 方法 + 字段 + 错误形状

浏览器和服务器共用这一张。四件都算合同，缺一件就会对不上货。ARCH-004 原文还点了认证 header 与初始化 SQL 顺序；header / `clientid` 本课不展开（登录切片）。权限串 `demo:demo:edit` 会同时出现在 `@SaCheckPermission`、OpenAPI 描述和 `sys_menu.perms`，本格不把它收成第五件，留给菜单 / CRUD 切片。

**路径和方法（Target）。** `API-005` / `DEC-006`：只读查询用 `GET`（`@GetMapping`，参数放 path 或 query，不要 GET body）；产生业务状态变化用 `POST`（`@PostMapping`，打 `@Log`）。CRUD **不用** `PUT` / `PATCH` / `DELETE`。这是 Target，不是 REST 教科书。

工作树对照——同一条演示资源，查询对齐、变更还在棘轮上：

| 动作 | 后端 `TestDemoController`（`@RequestMapping("/demo/demo")`） | 前端 `createDemoService` |
| --- | --- | --- |
| 列表 | `@GetMapping("/list")` → `R<PageResult<TestDemoVo>>` | `listDemo`：`method: 'get'`，`url: '/demo/demo/list'` |
| 详情 | `@GetMapping("/{id}")` | `getDemo`：`method: 'get'` |
| 新增 | `@PostMapping()` + `@Log(... INSERT)` | `addDemo`：`method: 'post'` |
| 修改 | **`@PutMapping()`** + `@Log(... UPDATE)` | **`updateDemo`：`method: 'put'`** |
| 删除 | **`@DeleteMapping("/{ids}")`** + `@Log(... DELETE)` | **`deleteDemo`：`method: 'delete'`** |

`TestTreeController` 同样是改 PUT、删 DELETE。这是 `MIG-CRUD-METHOD-LOG` 的**已知棘轮**，不是新接口样板。

对照 Target 活样本：`NotifyNoticeController`（`/notify/notice`）列表 `GET /list`，保存 `POST /save`，发布 `POST /{noticeId}/publish`，撤回 `POST /{noticeId}/retract`，删除 `POST /remove`，每个 POST 都有 `@Log`。新 CRUD 抄通知，不要抄 demo 的 PUT。

**字段。** JSON 用 Java VO 的驼峰：`testKey`，不是 SQL 列名 `test_key`。`TestDemoVo` 有 `id / deptId / userId / orderNum / testKey / value`。前端领域模型 `DemoVO` 同一组名字。改一边的拼写，另一边编译也许还过，运行会丢列。

**错误形状。** 权威在后端壳 `org.namewta.common.core.domain.R`：`code`（成功常量 `HttpStatus.SUCCESS = 200`）、`msg`、`data`，外加机器可读的 `error`（`ErrorInfo(code, args, field, violations)`）。旧客户端可以继续只看 `code/msg/data`。分页壳是 `rows` + `total`（`PageResult`），不是随便一个 `list`。前端平台包 `@namewta/platform-contracts` 只公开对齐的 `ApiErrorInfo`（同样是 `code / args / field / violations`）和插头 `HttpMethod`；**没有**名为 `ApiResponse` 的平台类型。各 domain 自己复制了一份接近的 `ApiResponse`：`domains/demo` 是 `data?: T`，`domains/profile` 是 `data: T`（必填）。形状近，不是同一副眼镜。兼容故事写在后端 `R` 上，不要假装前端已经有一份公共壳。

平台端口 `HttpMethod` 仍包含 `'put' | 'delete' | 'patch'`，那是插头能力，不是 CRUD 许可证。对象存储直传的浏览器 `PUT` 对象属于非 CRUD 协议（`API-005` 自己划了例外），不要和业务删除接口的 HTTP method 混成一句。

### 3. OpenAPI transport：指针、修订原件、生成物——勿手改生成字节

包：`frontend/packages/api-contracts`，包名 `@namewta/api-contracts`。工具源码把三份路径钉死在 `frontend/tooling/openapi/src/index.mjs` 的 `defaultPaths`：`pointer`、`store`、`output`。

| 路径 | 工具名 | 角色 |
| --- | --- | --- |
| `openapi/current.json` | `pointer` | **修订指针**。当前整份只有 `{"revision":"<64-hex>"}`（约 85 字节）。它可翻转，本身不含 `paths` |
| `openapi/revisions/<digest>/source.json` | store 里的立法原件 | **不可变 OpenAPI 快照**。digest 是 `sha256(source字节 + provenance)` |
| `openapi/revisions/<digest>/provenance.json` | 来源条 | `backendCommit`、`runtimeEndpoint`（默认 `/v3/api-docs`）、`totals.paths` 等 |
| `generated/openapi.ts` | `output` | 从**指针指向的那份** `source.json` 生成的 TypeScript（`paths` / `operations` / `components`） |
| `src/index.ts` | 公开入口 | 再导出类型，并提供 `OpenApiSchema` / `OpenApiOperation` / `OpenApiPath` |

2026-09-16 抽查：指针指向 `adab7988fa8b134f865c16e7290639ffefbcc2078ec04088a3f13d59019cd321`。该修订 `provenance.json` 写 `paths: 399`、`runtimeEndpoint: "/v3/api-docs"`。`source.json` 有 399 条 path，其中 **50** 条以 `/profile/` 开头（另有 2 条 `/system/user/profile*`，那是账号资料页，不是 profile 模块）。`generated/openapi.ts` 同样列出这 50 条 `/profile/**`，并已有 `PersonProfileSummaryVo` / `EnterpriseProfileSummaryVo`。因此：**不能再说「快照不含 `/profile/**`，所以 generated 没有 Profile 传输类型」。** 类型已经在运输箱里。

生成工具在 `frontend/tooling/openapi`（`@namewta/tooling-openapi`）：

1. `openapi:fetch`（实现名 `fetchSnapshot`）：读 `--source`（URL 或文件，默认打 `/v3/api-docs`），校验 OpenAPI 3.0/3.1，要求 `--backend-commit` 为 40 位 sha，把不可变修订写入 `revisions/<digest>/`，再**原子翻转**指针。
2. `openapi:generate`：离线读活跃修订，写入 `generated/openapi.ts`。
3. `openapi:check`：在内存里重生成再对比已提交的 `generated/openapi.ts`，不写文件。

手改 `generated/openapi.ts`，下一次 generate 会盖掉，check 会报漂移。消费规则：只从包公开入口 import；页面不得直接依赖 `generated/` 内部文件。domain 在边界把 transport **映射**成自己的模型。演示的活样本：

```text
OpenApiSchema<'TestDemoVo'>  ──projectDemoTransport──►  DemoVO
（api-contracts 生成的运输箱）                         （domain 自己的货）
```

文件：`frontend/packages/domains/demo/src/transport.ts`。`listDemo` 先按 HTTP GET 拿到运输箱，再 `rows.map(projectDemoTransport)`。

**Profile 活反例（映射缺口，不是快照缺口）。** HTTP 已有 `/profile/person/application` 等路径；活跃 `source.json` 与 `generated/openapi.ts` 已纳入。`packages/domains/profile` **不** `import` `@namewta/api-contracts` / `OpenApiSchema`。`src/transport.ts` 从本地 `unknown` 投影；各资源 service 仍写死 URL（例如 `url: '/profile/person/application'`）。工序停在 generate 之后、domain 映射之前。包 README 若仍写「generated 没有 Profile 类型」，不要跟着那句走。手改 generated 字节仍然禁止；缺口在映射，不在复印件缺页。

生成物会忠实地复印棘轮：`generated/openapi.ts` 里 `"/demo/demo"` 仍有 `put: operations["edit_15"]`。复印件有 PUT，不代表新接口该 PUT。

CLI 测试、无效来源保持最后修订、漂移断言留给 L-010。本课只要你能指着三份文件说出谁是指针、谁是立法原件、谁是运输箱。

### 4. Java `wta-api`：模块 ↔ 模块，不是浏览器

模块：`backend/wta-api/`。POM 只依赖 `wta-common-core`、`wta-common-json`、`spring-core`。公开类型按包分，例如：

- `org.namewta.system.api.UserService` / `UserDTO`
- `org.namewta.notify.api.NotificationApplicationService`
- `org.namewta.sso.api.SsoIdentityService`
- `org.namewta.third.api.ThirdPartyGateway`
- `org.namewta.workflow.api.WorkflowService`
- `org.namewta.profile.api.ProfileService`

浏览器 **import 不到** 这些 `.java`。管理端列表走 HTTP；通知发布走 Java。活样本：`NotifyNoticePublisherService` 注入 `UserService` 解析收件人，再调 `NotificationApplicationService.submit`。实现类住在 `wta-system` / `wta-notify`，调用方 POM 只依赖 `wta-api`，不依赖邻居的 Mapper。

改 `wta-api` 的签名、字段、null/异常语义，是 Java 源码/二进制兼容问题（`java-api-compatibility`）。它**不会**自动改 `GET /demo/demo/list` 的 JSON。两张河，两张纸。兼容演进在磁盘上至少有两种活样本，不要只背一句注解：

- **弃用委托：** `OssService.selectUrlByIds` / `selectByIds` 标 `@Deprecated(since = "6.0.0", forRemoval = false)`，文档指向新方法 `resolveAccessUrl`。旧调用方还能编过。
- **default 新能力：** `UserService.searchActiveUsers` 默认返回空列表；`lockActiveById` 默认抛 `UnsupportedOperationException`。旧抽象方法没有弃用。旧实现可以继续加载，只是没有新能力。

## 核心概念与机制

### 直觉讲解

两家商店约定：货架标签写 `testKey`，用门口左边窗口取货（`GET /demo/demo/list`）。一家把标签改成 `test_key`，另一家窗口还在等 `testKey`，货就对不上。仓库货架上的列名本来就是 `test_key`——那是 SQL 的方言；窗口上的标签是 JSON 的方言。两种方言都是合同，只是翻译层要诚实。

SQL 更硬：表一旦在真实环境长出来，删列像拆承重墙。所以 NAMEWTA 自有产品表只并进 `10-cde-base-ddl.sql`，产品数据只并进 `50-cde-base-dml.sql`。它们是**完整可重建基座**，不是每天往日志末尾追加一笔的无限迁移条。

OpenAPI 像盖了章的送货单。仓库先把货备好（表 + HTTP），快递员去窗口抄一份新送货单（`openapi:fetch`），把这份单子锁进保险柜（`revisions/<digest>/source.json`），再把柜门上的标签换成新号码（翻转 `current.json`），最后复印给会计（`openapi:generate` → `generated/openapi.ts`）。会计再把格子誊进自己的账本（domain 映射）。手改复印件上的字，下次复印机会把你的字盖掉。柜门标签不是送货单本身：打开 `current.json` 看不到 `/profile/person/application`。

Java `wta-api` 是后厨之间的点菜单，食客（浏览器）不进后厨。食客只看窗口上的 HTTP。

谁先改？先让仓库和窗口的标签同时对得上（后端兼容合同），再抄送货单、翻柜门标签、复印、誊账本，最后才换橱窗海报（页面）。先换海报再求仓库「顺便加一列」，橱窗在撒谎。送货单已经抄好、账本还用手写旧格子，也是撒谎——这就是今天的 Profile。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 公共合同 | public contract | 跨边界必须保持兼容的形状：HTTP 路径/方法/字段/错误形状、SQL schema 与种子、OpenAPI transport、跨模块 Java API |
| 修订指针 | revision pointer | `openapi/current.json`：只含活跃 `revision` digest，可原子翻转；不是 path 清单 |
| 不可变快照 | immutable snapshot / `source.json` | `openapi/revisions/<digest>/source.json`：立法原件；旁路 `provenance.json` 记录来源 |
| 传输类型 | transport | 从活跃快照生成的、贴近 HTTP JSON 的类型（`generated/openapi.ts`）；不是 domain 模型 |
| 领域模型 | domain model | 各 `packages/domains/*` 自有模型；在边界从 transport 映射过来 |
| 映射缺口 | mapping gap | HTTP 与 generated 类型已在，domain 仍手写 URL / 不 `import` `OpenApiSchema` |
| 兼容先行 | backend-first compatibility | 跨端变更先形成向后兼容或同步可交付的后端合同，再更新前端消费者（`ARCH-004`，格子 `D:contract-order`） |
| 查询 GET / 变更 POST | API-005 / DEC-006 | Target：只读查询用 GET，产生业务状态变化用 POST 并打 `@Log`；CRUD 不用 PUT/PATCH/DELETE |
| 棘轮 | Ratchet | 存量仍可能混用 PUT/DELETE；新代码与**触及的**合同按 Target 收紧，不借机全仓重写 |
| 错误形状 | error envelope | 权威是后端 `R` 的 `code`+`msg`+`data`+可选 `error`；平台对齐 `ApiErrorInfo`；分页另有 `rows`/`total` |
| 自有表基座 | owned-table baseline | NAMEWTA 自己的产品表结构只进 `10-cde-base-ddl.sql`，产品数据只进 `50-cde-base-dml.sql` |
| 跨模块 Java API | wta-api | `backend/wta-api` 的公开接口与 DTO；模块 ↔ 模块，不经过浏览器 |

**Public contract** 的检验：如果另一边（另一个模块、另一个 App、下一班值班的人、下一台空库）会因为你改了这个字而编译失败或行为错误，它就是合同。

**Transport ≠ domain。** 运输箱可以已经有某个领域的类型（Profile 现在就是），领域模型仍必须有人拥有，并且从运输箱映射过来。把生成类型直接当页面状态，是把复印件钉在橱窗上。手写 URL 而放着生成类型不用，是账本和送货单分叉。

**wta-api ≠ HTTP。** 改 Java 方法名救不了 JSON 字段；改 JSON 字段也救不了邻居模块的编译。

**Pointer ≠ snapshot。** 在 `current.json` 里搜 `/profile` 会空手而归。那不证明快照缺页。

### 机制/因果链

按「加一个列表字段 `color`」的真实顺序走（格子 `D:contract-order`）。FE-CRUD-001 从「生成 transport（需要时）」起，假定 HTTP 已存在；overview 必须把表和 HTTP 放在 fetch 前面。

1. **先分类。** `color` 要进 JSON 吗？要进表吗？第二个 App 会看见吗？三句里有一句「会」，这就不是 CSS。
2. **表。** NAMEWTA 自有列并进 `10-cde-base-ddl.sql`（及对应 entity）。种子/菜单需要的话并进 `50-cde-base-dml.sql`。不要写进 `20/30/40/60`，也不要在 `wta-demo` 旁边再放一份方言 SQL。已有环境不重放整份基座；破坏性变更要有迁移和回滚（`ARCH-004`）。不要因为 50 里有临时预检表，就把产品 `CREATE TABLE` 写进 50。
3. **HTTP/JSON。** VO/BO 增加 `color`；查询仍走 GET；变更若是新接口，走 POST + `@Log`。错误壳不要另起炉灶：继续用 `R`。路径、方法、字段、错误形状四件一起改，测试和权限标识一起对齐。
4. **后端可兼容。** 旧客户端暂时可以不认识新字段（加列/加 JSON 字段通常可兼容）。改名、删列、改 method、改 `code` 含义是破坏性变更，必须给窗口期，不能只靠前端先改。表和 HTTP 先可交付，再碰 transport。
5. **fetch 快照并翻转指针。** `openapi:fetch -- --source <url-or-file> --backend-commit <40-hex>`。工具把不可变修订写入 `revisions/<digest>/source.json` + `provenance.json`，再原子改写 `current.json` 的 `revision`。不要手改指针去「假装」纳入一条路径，也不要在 `current.json` 里粘贴 OpenAPI 正文。
6. **generate。** `openapi:generate` 从活跃修订离线写出 `generated/openapi.ts`。`openapi:check` 只对比、不写盘。不要手改生成字节。生成物里已经有的 schema，不要再当「快照缺口」。
7. **domain 映射。** 映射函数（像 `projectDemoTransport`）把新字段搬进领域模型；请求 URL/方法应对齐生成的 path，而不是另写一份永久分叉。页面仍然只看见 domain。**Profile 今天卡在这一步。**
8. **web-domain / App。** 表格列、表单、文案最后动。admin-web 与 home-web 若都要展示，改的是共享 domain/web-domain，不是只改一个 App 的 `views/`。
9. **平行的模块河。** 若邻居 Java 模块也要这个值，走 `wta-api`（新方法、`default` 新能力，或可兼容字段），不要让通知模块去 import `TestDemo` 实体。Java 合同的兼容规则与 HTTP 合同分开执行。

因果：合同先稳定 → 两边才能独立发布。先改消费者再改生产者，中间会出现「页面能点、服务器不认」的窗口。那不是敏捷，那是事故预告。HTTP 和运输箱已经对齐、domain 还在手写旧 URL，窗口同样会撒谎，只是编译器未必喊疼。

方法名也是合同。Target 说列表 GET、新增 POST。工作树 demo 修改仍是 PUT——触及这条合同时，前后端、`@Log`、测试、OpenAPI 快照一起迁到 POST；不要在旁边再新增一条 PUT。`docs/fm` 静态模板的校验脚本已经禁止 `@PutMapping` / `method: 'put'`；模板不是棘轮许可证，demo 源码才是棘轮现场。

## 图、表或文本图

**图题 / caption：** 四张条约与改动顺序（先上后下）。alt：SQL 到页面的合同依赖，外加一条不经过浏览器的 Java API；transport 拆成指针、修订、生成物。

```text
  [SQL 基座]  本 Goal 存储格：10-cde-base-ddl.sql / 50-cde-base-dml.sql
              产品表结构只进 10；产品数据只进 50
              六份脚本还在：20 job / 30 workflow / 40 ai / 60 nacos
                    |
                    v
  [HTTP/JSON]  路径 + 方法 + 字段 + 错误形状 R{code,msg,data,error}
               Target：查询 GET，变更 POST + @Log
               棘轮现场：demo 的 PUT/DELETE（不是样板）
                    |
                    v
  [OpenAPI]    fetch ──► revisions/<digest>/source.json   （立法原件）
                    └──► current.json 翻转 revision         （指针）
               generate ──► generated/openapi.ts           （运输箱）
               公开入口 src/index.ts；勿手改生成字节
                    |
                    v
  [domain 模型]  例如 projectDemoTransport(TestDemoVo) → DemoVO
                 Profile 今日：生成类型已在，service 仍手写 URL
                    |
                    v
  [web-domain / App]  画面

  平行条约（不经过浏览器、也不经过 api-contracts）：
  业务模块 A  ←→  Java wta-api  ←→  业务模块 B
```

**文字等价物：** 从上到下是依赖顺序，也是跨端改动的建议顺序。最上面是数据库基座：没有列，HTTP 字段就是谎言。六份初始化脚本都在那个目录里，但 NAMEWTA 自己的产品表只认 10、产品数据只认 50；另外四份是任务、工作流、AI、Nacos 的上游或配套库。然后是浏览器和服务器共享的 HTTP/JSON，包括路径、方法、字段名和 `R` 错误壳。再然后才是 OpenAPI：先把后端文档落成不可变修订并翻转指针，再生成 TypeScript 运输箱；domain 把 transport 转成自己的模型，页面只消费 domain。模块之间还有一条不经过浏览器的 Java `wta-api` 条约。箭头表示「上边先定，下边跟着改」，不是运行时函数调用，也不是 Maven 依赖箭头。

**图的边界：** 本图不保证每一个 domain 已经消费生成物。Profile 的 generated 类型已在，映射未完成。包 README 过期不改这张图。本图不展开 Nacos 内部表、OSS 直传失败路径、HMAC 机器调用、`openapi:fetch` 的 CLI 测试、50 里 DSL 块的升级语义。`wta-api` 画成平行条约，是因为它不进入 `api-contracts` 的生成循环。认证 header 故意不画进来。

**图题 / caption：** 同一条演示字段怎么换方言。alt：test_demo.test_key 到页面列的映射链。

```text
  表 test_demo.test_key          （10-cde-base-ddl.sql）
           |
           |  MyBatis / Jackson
           v
  JSON  "testKey"                （TestDemoVo，GET /demo/demo/list）
           |
           |  fetch 落修订 + 翻转指针 + generate
           v
  OpenApiSchema<'TestDemoVo'>    （api-contracts / generated/openapi.ts）
           |
           |  projectDemoTransport
           v
  DemoVO.testKey                 （packages/domains/demo）
           |
           v
  DemoPage 表格列                （web-domain-demo）
```

**文字等价物：** 仓库列叫 `test_key`（下划线）。窗口上的 JSON 叫 `testKey`（小驼峰）。生成的运输箱沿用 JSON 名字，前提是指针指向的那份 `source.json` 已经包含这条 path。domain 的 `DemoVO` 也用 `testKey`，由 `projectDemoTransport` 从运输箱搬过来，缺省时写成空字符串。页面只读 `DemoVO`。有人把页面改成 `test_key` 却不改后端，货就丢了。有人只改 domain 类型、不改表和 VO，橱窗仍然在撒谎。有人表和 HTTP 都改了、也 generate 了，却让 domain 继续手写旧 URL，账本和送货单会悄悄分叉。

**图的边界：** 这是演示列表这一条资源的字段链，不是全仓字典。翻译注解、数据权限、分页包装会再包一层，本课不拆。Profile 字段链同构，只是最后两步还没接上 `OpenApiSchema`。

## 正例、反例与边界

**正例 1：** 演示列表查询合同对齐。后端 `GET /demo/demo/list` 返回 `R<PageResult<TestDemoVo>>`；前端 `listDemo` 使用 `method: 'get'`、`url: '/demo/demo/list'`，并用 `OpenApiSchema<'TestDemoVo'>` 经 `projectDemoTransport` 映射成 `DemoVO`。

**正例 2：** NAMEWTA 自己的新产品表只写入 `10-cde-base-ddl.sql`，产品数据写入 `50-cde-base-dml.sql`。`test_demo` 的 CREATE 在 10，INSERT 在 50。

**正例 3：** 跨模块要用户 ID 列表，走 `wta-api` 的 `UserService`（通知发布就是这么做的），不复制一张用户表到通知模块，也不让浏览器去 import `UserService.java`。新能力用 `default` 方法或 `@Deprecated` 旧方法，而不是改旧参数含义。

**正例 4：** 新的变更接口抄 `NotifyNoticeController`：删除是 `POST /notify/notice/remove` 并打 `@Log`，不是 `DELETE`。

**正例 5：** 更新 transport 走 `tooling/openapi` 的 fetch（落修订 + 翻转指针）/ generate / check，只从 `@namewta/api-contracts` 的公开入口消费。生成物里已经有的 path，domain 用映射接上，而不是再手写一份平行 URL 表。

**反例 1：** 只在 `DemoPage.vue` 增加字段展示，JSON 里没有这个字段。画面在撒谎。

**反例 2：** 手改 `packages/api-contracts/generated/openapi.ts` 而不改活跃 `source.json`（也不走 fetch）。下一次生成会覆盖；`openapi:check` 会报漂移。

**反例 3：** 新 CRUD 删除接口写成 `DELETE /...`，因为「REST 教科书这样写」。项目 Target 是变更走 POST，并打 `@Log`。存量 demo 的 PUT/DELETE 是待收紧的棘轮，不是样板。生成物里的 `put: operations["edit_15"]` 只是复印件，同样不是样板。

**反例 4：** 前端先把列加上 `color`，再回头求后端「顺便加点」。`ARCH-004` 的顺序反了，格子 `D:contract-order` 记的就是这件事。

**反例 5：** 在 `20-cde-job.sql` 或某个模块 `script.sql` 里新建 NAMEWTA 业务表。自有产品表基座只有 10；自有产品数据只有 50。

**反例 6：** 把 `wta-api` 的 DTO 改名，以为前端 OpenAPI 会跟着变。浏览器从不编译那份 Java。

**反例 7：** 看见 generated 已有 `PersonProfileSummaryVo`，仍说「快照没有 `/profile/**`」，并继续把 Profile service 的手写 URL 当成永久合同。这是映射缺口，不是快照缺口。过期 README 不能当证据。

**反例 8：** 打开 `current.json` 搜不到路径，就去手写一份 `generated/openapi.ts`。指针本来就没有 path。该打开的是指针指向的 `source.json`。

**边界：** `docs/fm/**` 是静态 CRUD 模板，给人对照实现，不是运行时代码生成器（生成器已删除）。模板校验已经禁止 PUT/DELETE；不能把**存量 demo 源码**里的 PUT 当成新模块许可证。认证 header、Client 标识也是 `ARCH-004` 点名的合同，本课不展开登录；改它们仍然要后端兼容先行。50 里的临时预检表和 DSL 块是数据基座的一部分，不是产品 DDL；升级步骤见 L-085。OpenAPI CLI 函数级测试见 L-010。

## 变式与迁移

- **变式 A：只改文案。** 不是合同。可以只动 web-domain 或 App 语言包。
- **变式 B：改字段含义但名字不变。** 这是静默破坏。要当合同变更：版本、兼容窗口或双读。`code: 200` 若有一天表示失败，所有客户端都会把灾难当成成功。
- **变式 C：存量 demo 修改仍是 PUT。** 触及这条合同时，Controller、`createDemoService`、测试、OpenAPI 快照、`@Log` 一起迁到 POST；不要在旁边再新增一条 PUT。树表 `TestTreeController` 是同一把棘轮。
- **变式 D：只改 Java `wta-api`。** 走兼容演进：新方法先出现（可 `default`），旧方法委托或标 `@Deprecated(since = "6.0.0", forRemoval = false)`；不要改参数含义。这解决不了浏览器 JSON。若 HTTP 也要变，仍按表 → HTTP → fetch → 翻转指针 → generate → domain → 页面。
- **变式 E：生成类型已在、domain 尚未映射。** 像 2026-09-16 的 Profile：不要再发明「快照缺口」故事。下一步是让 domain `import` `OpenApiSchema`、删掉与生成物重复的手写 URL，页面仍不直接引用 `generated/` 内部文件。若将来真有一条 HTTP 还没进活跃 `source.json`，先把后端合同做可交付，再 fetch / 翻转指针 / generate，而不是手改 generated 字节。
- **迁移口诀：** 表 → HTTP → fetch 落修订 → 翻转指针 → generate → domain 映射 → 页面。跳步就会出现「一边编译过、一边运行挂」，或「运输箱有货、账本还用手写」。破坏性变更必须有迁移和回滚，不能只靠前端开关隐藏按钮。

## 常见误区

1. **「TypeScript interface 就是合同。」** 它是消费者的眼镜。眼镜改了，货物标签不会自己改。
2. **「SQL 在后端模块里放一份更方便。」** 空环境重建的权威在父仓六份脚本；NAMEWTA 自有产品表只认 10，产品数据只认 50。
3. **「先改前端，后端跟不上再补，用户能先看效果。」** 效果是假的，联调窗口会变成生产事故。这是把 `D:contract-order` 画反。
4. **「OpenAPI 生成失败就手写一份 transport / 手改 generated。」** 可以暂时挡编译，但会与快照永久分叉。Profile 今天缺的是映射，不是 generated 缺页。
5. **「`wta-api` 给浏览器用。」** 给 Java 模块用。浏览器只看见 HTTP/JSON。
6. **「生成物里有 PUT，所以 PUT 是正规 REST。」** 生成物复印现状。Target 在 `API-005`，棘轮在 `MIG-CRUD-METHOD-LOG`。
7. **「六份 SQL 都可以往里面加 NAMEWTA 新表。」** 错。20/30/40/60 不是自有表登记处。
8. **「平台 HttpMethod 含 put，所以业务可以 put。」** 插头能发出去，不等于合同允许。OSS 直传 PUT 对象是另一份非 CRUD 协议。
9. **「`current.json` 就是快照；里面没有 `/profile` 就是没纳入。」** `current.json` 是指针。立法原件在 `revisions/<digest>/source.json`。2026-09-16 活跃修订已有 50 条 `/profile/**`。
10. **「包 README 写缺口，所以 generated 没有类型。」** 说明书会过期。以指针、`source.json`、`generated/openapi.ts` 三份字节为准。
11. **「前端 `ApiResponse` 住在 platform/contracts。」** 该包导出 `ApiErrorInfo`，不导出 `ApiResponse`。错误形状权威在后端 `R`。
12. **「50 里看见 `create temporary table` 就说明规则破了，可以建业务表或另开 migrate/。」** 产品表仍只在 10。临时表是数据基座的预检工具。细节留给 L-085。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷：

1. 列出 `release-artifacts/docker/infrastructure/mysql/init/` 的六个文件名。圈出本 Goal 的存储格：哪两个是 NAMEWTA 自有产品表 / 产品数据。顺手在 50 里找到一处 `create temporary table`，标成「数据基座预检，不是产品 DDL」。
2. 打开 `TestDemoController` 的修改/删除映射，再打开 `createDemoService` 的 `updateDemo` / `deleteDemo`。用「棘轮」或「Target」给它们贴标签，不要用「REST 更正宗」。
3. 打开 `NotifyNoticeController` 的删除映射，对照上一张标签。
4. 打开 `openapi/current.json`（应只有 `revision`）。用那个 digest 打开对应 `revisions/<digest>/source.json` 与 `generated/openapi.ts`，确认 `/profile/person/application` 和 `PersonProfileSummaryVo` 在不在。再打开 `packages/domains/profile` 的某个 service：URL 仍是手写字符串。想清楚：缺的是快照还是映射。
5. 打开 `generated/openapi.ts` 的 `/demo/demo`。复印件上的 `put` 是谁的字。再打开 `platform/contracts`：有没有 `ApiResponse`。

## 总结、词汇表与下一步

- 合同四张：HTTP/JSON（路径/方法/字段/错误形状）、SQL 基座（本 Goal 存储格 10/50；六份脚本存在）、OpenAPI transport（指针 + 不可变 `source.json` + 生成物，勿手改生成字节）、Java `wta-api`（模块 ↔ 模块，不是浏览器）。
- 顺序（`D:contract-order`）：后端兼容的表和 HTTP 先行，再 fetch 落修订、翻转指针、generate、domain 映射、页面。跨端口诀：表 → HTTP → transport → domain → 页面；transport 内部是 fetch → 指针 → generate。
- Profile 例子：generated 可以已经有类型，domain 仍手写 URL。那是映射缺口，不是「快照缺失」。
- Target：查询 GET、变更 POST + `@Log`。存量 demo 的 PUT/DELETE 按触及范围收紧，不是新代码样板。
- 生成物不要手改。错误形状权威在 `R` + 平台 `ApiErrorInfo`。
- 改名/删列/改 method/改错误码含义是破坏性变更，要迁移和回滚。50 的 DSL / 升级演练见 L-085。

词汇表：public contract / revision pointer / immutable snapshot / transport / mapping gap / domain model / Ratchet / owned-table baseline / error envelope / backend-first compatibility。

下一步：L-006 把地图走通——从管理端列表页的一次 GET，一层一层走到 Mapper，并指出至少一种抄近路。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-001 | `README.md` | 前后端经 HTTP/JSON 协作并独立构建；跨模块走 `wta-api` | 「架构一览」后段落 | 2026-09-16 |
| S-003 | `.agents/skills/engineering-standards/references/project/00-project-profile.md` | 六份 MySQL 基座唯一事实源；自有表进 10、种子进 50；`api-contracts` 生成物不可手改 | 「事实来源」「排除与冻结」 | 2026-09-16 |
| S-004 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | 前端消费 HTTP/JSON，不深耦合 Java；OpenAPI transport 在 `api-contracts`；`wta-api` 是跨业务合同面；跨端以后端兼容合同先行 | 「依赖方向」 | 2026-09-16 |
| S-006 | `.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md` | ARCH-004 合同与跨端顺序；ARCH-006 生成物不手改 | ARCH-004、ARCH-006 | 2026-09-16 |
| S-007 | `.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md` | DEC-006 GET/POST；MIG-CRUD-METHOD-LOG 棘轮 | DEC-006、当前状态表 | 2026-09-16 |
| S-019 | `release-artifacts/docker/infrastructure/mysql/init/` 六份 SQL；`10-cde-base-ddl.sql` 的 `test_demo`；`50-cde-base-dml.sql` 的 INSERT 与临时预检表 | 六脚本存在；10/50 为自有产品表/数据权威；50 含 `create temporary table` 预检，产品 CREATE 仍只在 10 | 目录 listing；`CREATE TABLE test_demo`；`INSERT INTO test_demo`；`namewta_password_dsl_001_preflight` | 2026-09-16 |
| S-020 | `TestDemoController.java`、`TestTreeController.java`、`NotifyNoticeController.java`、`TestDemoVo.java` | demo 查询 GET、变更 PUT/DELETE 棘轮；通知变更 POST；VO 字段 `testKey` | `@RequestMapping` / mapping 注解 | 2026-09-16 |
| S-021 | `frontend/packages/domains/demo/src/index.ts`、`transport.ts` | `listDemo` GET；`updateDemo` put；`projectDemoTransport` | `createDemoService` | 2026-09-16 |
| S-022 | `frontend/packages/api-contracts/**`、`frontend/tooling/openapi/src/index.mjs`、`cli.mjs`、`README.md` | `current.json` 为 pointer；立法原件 `revisions/<digest>/source.json`；活跃修订 399 paths / 50 条 `/profile/**`；`generated/openapi.ts` 已有 Profile schema 与 demo `put`；fetch 翻转指针、generate 写 output、check 不写盘；勿手改生成字节。包 README 仍写缺口，以字节为准 | `defaultPaths`；指针 85 字节；`provenance.json` `paths: 399`；`PersonProfileSummaryVo` | 2026-09-16 |
| S-023 | `R.java`、`HttpStatus.java`、`PageResult.java`、`frontend/packages/platform/contracts/src/index.ts`、各 domain `ApiResponse` | 错误壳权威 `code/msg/data/error`；成功码 200；分页 `rows/total`；平台仅 `ApiErrorInfo` / `HttpMethod`；`ApiResponse` 多包复制且 `data` 可选性不一致 | 类字段与接口 | 2026-09-16 |
| S-024 | `backend/wta-api/pom.xml`、`UserService.java`、`OssService.java`、`NotificationApplicationService.java`、`NotifyNoticePublisherService.java` | `wta-api` 只依赖稳定 common；模块河走 Java 接口；`@Deprecated` 与 `default` 两种兼容活样本 | POM dependencies；注入点；`selectUrlByIds`；`searchActiveUsers` / `lockActiveById` | 2026-09-16 |
| S-025 | `api-errors-resources.md` API-005；`persistence-transactions-and-ddl.md` PERSIST-006；`crud-api-and-pages.md` FE-CRUD-001/002；`docs/fm/scripts/validate.mjs`；`java-api-compatibility/SKILL.md`；`domains/profile/src/**` | GET/POST Target；六文件基座职责；表/HTTP 先于 generate；transport→domain→页面；模板禁止 PUT；Java API 兼容演进；Profile domain 不 import `api-contracts`、手写 `/profile/**` URL | 对应规则正文与 profile service | 2026-09-16 |
