---
lesson_id: L-005
objective_ids: [OBJ-05]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 12
  - segment: visuals-and-worked-examples
    minutes: 10
  - segment: pause
    minutes: 4
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: standard
source_ids: [S-001, S-003, S-004, S-006, S-007]
---

# Lesson 005：什么算合同，谁先改

## 学完你能做什么

你能把“字段名、HTTP 路径与方法、SQL 基座、OpenAPI transport”认成**公共合同**，把“某个 Vue 按钮颜色、某个 Service 私有方法”认成实现细节。跨端变更时，你能说出顺序：先给出后端可兼容的合同，再改前端消费者。

## 先把宏观地图放在桌上

前面四课认房间。现在认房间之间的**条约**。条约一改，两边都要会签；日记本（实现）可以偷偷换笔，只要条约上的字不变。

本课四张条约：

1. **HTTP/JSON**：路径、方法、字段、错误形状。浏览器和服务器都看见。
2. **SQL 基座**：空环境怎么建库建表。权威在 `release-artifacts/docker/infrastructure/mysql/init/`。
3. **OpenAPI transport**：前端 `packages/api-contracts` 里生成出来的传输类型，不是 domain 自己的业务模型。
4. **Java 跨模块 API**：L-002 的 `wta-api`。本课只提醒它也是合同，不展开每个接口。

**类比失效处：** “条约”不是法律课。没有外部门章。失效点：前端 TypeScript 类型看起来像合同，但**权威在服务端行为 + 已发布的 JSON/SQL**。只改前端 interface、不改后端，对方系统不会跟着变。

## 核心概念与机制

### 直觉讲解

两家商店约定：货物标签写 `testKey`，用门口左边的窗口取货（`GET /demo/demo/list`）。如果一家把标签改成 `test_key`，另一家窗口还在等 `testKey`，货就对不上。

SQL 更硬：表一旦在真实环境长出来，删列像拆承重墙。所以 NAMEWTA 自有表结构只并进 `10-cde-base-ddl.sql`，初始化数据和菜单并进 `50-cde-base-dml.sql`。它们是**完整可重建基座**，不是“每天往日志末尾追加一笔”的无限迁移条。

OpenAPI 像一份盖了章的送货单复印件。生成物在 `frontend/packages/api-contracts/generated/` 和快照 `openapi/current.json`。domain 再把 transport **映射**成自己的模型。手改生成物，下次生成会把你的字盖掉。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 公共合同 | public contract | 跨边界必须保持兼容的形状：HTTP 路径/方法/字段、SQL schema、OpenAPI transport、跨模块 Java API |
| 传输类型 | transport | `api-contracts` 生成的、贴近 HTTP JSON 的类型 |
| 领域模型 | domain model | 各 `packages/domains/*` 自有模型；在边界从 transport 映射过来 |
| 兼容先行 | backend-first compatibility | 跨端变更先形成向后兼容或同步可交付的后端合同，再更新前端 |
| 查询 GET / 变更 POST | API-005 | Target：只读查询用 GET，产生业务状态变化用 POST，POST 打 `@Log`；CRUD 不用 PUT/PATCH/DELETE |
| 棘轮 | Ratchet | 存量仍可能混用 PUT/DELETE；新代码与触及的合同按 Target 收紧 |

**Public contract** 的检验：如果另一边（另一个模块、另一个 App、下一班值班的人）会因为你改了这个字而编译失败或行为错误，它就是合同。

### 机制/因果链

1. 你想给列表加一个字段 `color`。
2. 先问：JSON 里要出现它吗？表里要存它吗？若是，这是合同，不是 CSS。
3. 后端：表结构进 `10-cde-base-ddl.sql`（及对应 entity）；HTTP VO/BO 增加字段；查询仍走 GET。
4. 前端：更新 OpenAPI 快照/生成 transport（工具链），domain 映射新字段，web-domain 再展示。
5. 不要先在 Vue 里加 `color` 再回头求后端“顺便加点”。ARCH-004：破坏性变更必须有迁移和回滚。
6. 方法名也是合同。Target 说列表 GET、新增 POST。工作树里 `TestDemoController`：`GET /list`、`POST` 新增，但修改仍是 `@PutMapping`，前端 `updateDemo` 仍是 `put`。这是已知棘轮（MIG-CRUD-METHOD-LOG），不是“PUT 才是正规 REST，所以新接口也该 PUT”。

因果：合同先稳定 → 两边才能独立发布。先改消费者再改生产者，中间会出现“页面能点、服务器不认”的窗口。

### 图、表或文本图

**图题 / caption：** 四张条约与改动顺序（先左后右）。

```text
  [SQL 基座]  50-ddl / 60-dml / 10-base / 20-job / 30-workflow / 40-ai
       |
       v
  [HTTP/JSON]  路径 + 方法 + 字段 + 错误形状
       |
       v
  [OpenAPI transport]  packages/api-contracts  生成物，勿手改
       |
       v
  [domain 模型]  映射后的业务形状
       |
       v
  [web-domain / App]  画面

  Java wta-api 是另一条平行条约（模块↔模块），不经过浏览器。
```

**文字等价物：** 从上到下是依赖顺序，也是跨端改动的建议顺序。最上面是数据库基座脚本：没有列，HTTP 字段就是谎言。然后是浏览器和服务器共享的 HTTP/JSON。再然后是前端根据 OpenAPI 生成的 transport 类型。domain 把 transport 转成自己的模型，页面只消费 domain。模块之间还有一条不经过浏览器的 Java `wta-api` 条约。箭头表示“左边/上边先定，右边/下边跟着改”，不是运行时函数调用。

**图的边界：** 本图不保证 OpenAPI 快照已覆盖每一个 Controller。生成物与手写 domain 之间若漂移，以漂移检查工具和当前工作树为准，不要假设 100% 同步。本课不展开 Nacos、OSS 内部状态。

### 正例、反例与边界

**正例 1：** 演示列表。后端 `GET /demo/demo/list` 返回 `TestDemoVo`；前端 `listDemo` 使用 `method: 'get'`、`url: '/demo/demo/list'`。查询合同对齐。

**正例 2：** NAMEWTA 自己的新表只写入 `10-cde-base-ddl.sql`，种子数据写入 `50-cde-base-dml.sql`，而不是在模块里再复制一份 MySQL 方言。

**正例 3：** 跨模块要用户 ID 列表，走 `wta-api` 的 `UserService`，不复制一张用户表到通知模块。

**反例 1：** 只在 `DemoPage.vue` 增加字段展示，JSON 里没有这个字段。画面在撒谎。

**反例 2：** 手改 `packages/api-contracts/generated/openapi.ts` 而不改来源快照。下一次生成会覆盖。

**反例 3：** 新 CRUD 删除接口写成 `DELETE /...`，因为“REST 教科书这样写”。项目 Target 是变更走 POST，并打 `@Log`。存量 demo 的 PUT/DELETE 是待收紧的棘轮，不是样板。

**边界：** `docs/fm/**` 是静态 CRUD 模板，给人对照实现，不是运行时代码生成器（生成器已删除）。模板也必须服从 GET/POST 合同；不能把模板里的旧 PUT 当成新模块许可证。

## 变式与迁移

- **变式 A：只改文案。** 不是合同。可以只动 web-domain 或 App 语言包。
- **变式 B：改字段含义但名字不变。** 这是静默破坏。要当合同变更：版本、兼容窗口或双读。
- **变式 C：存量 demo 修改仍是 PUT。** 触及这条合同时，前后端、`@Log`、测试一起迁到 POST；不要在旁边再新增一条 PUT。
- **迁移口诀：** 表 → HTTP → transport → domain → 页面。跳步就会出现“一边编译过、一边运行挂”。

## 常见误区

1. **“TypeScript interface 就是合同。”** 它是消费者的眼镜。眼镜改了，货物标签不会自己改。
2. **“SQL 在后端模块里放一份更方便。”** 空环境重建的权威在父仓六份脚本。
3. **“先改前端，后端跟不上再补，用户能先看效果。”** 效果是假的，联调窗口会变成生产事故。
4. **“OpenAPI 生成失败就手写一份 transport。”** 可以暂时挡编译，但会与快照永久分叉。

## 非评分暂停

选一个你最近改过的“小字段”。问三句：JSON 有它吗？表有它吗？第二个 App 会看见它吗？三句里有一句“会”，它就可能是合同，不该只改一个 Vue 文件。

再看一眼 `updateDemo` 的 `put`：这是现状，还是 Target？用棘轮两个字回答，不要用“REST 更正宗”。

## 总结、词汇表与下一步

- 合同：HTTP/JSON、SQL 基座、OpenAPI transport、Java `wta-api`。
- 顺序：后端兼容合同先行，再更新前端。
- Target：查询 GET、变更 POST；存量 PUT/DELETE 按触及范围收紧。
- 生成物不要手改。

下一步：L-006 把地图走通——从管理端列表页的一次 GET，一层一层走到 Mapper。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-006 | `architecture-and-boundaries.md` | ARCH-004 合同与跨端顺序 | ARCH-004 | 2026-09-14 |
| S-007 | `02-decisions-and-exceptions.md` | DEC-006 GET/POST；MIG-CRUD-METHOD-LOG | DEC-006、当前状态表 | 2026-09-14 |
| S-003 | `00-project-profile.md` | 六份 MySQL 基座；50/60 分工；api-contracts 生成物 | 排除与冻结、事实来源 | 2026-09-14 |
| S-004 | `01-module-map.md` | 前端消费 HTTP/JSON，不深耦合 Java 类型 | 依赖方向 | 2026-09-14 |
| S-001 | `README.md` | 前后端经 HTTP/JSON 协作并独立构建 | 架构一览后段落 | 2026-09-14 |
