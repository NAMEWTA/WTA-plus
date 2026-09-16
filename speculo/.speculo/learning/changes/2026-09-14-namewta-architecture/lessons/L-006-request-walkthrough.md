---
lesson_id: L-006
objective_ids: [OBJ-06]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 3
  - segment: deep-explanation
    minutes: 16
  - segment: visuals-and-anti-examples
    minutes: 8
  - segment: transfer-and-pause
    minutes: 4
  - segment: recap
    minutes: 4
expression_level: eli5
coverage_depth: standard
source_ids: [S-004, S-005, S-006, S-008]
---

# Lesson 006：把地图走通：一次 GET 的接力

## 学完你能做什么

你能按层口述：管理端演示列表如何从浏览器走到 Mapper；能指出至少一种错误抄近路；并且能把同一句 GET 在 classic（demo）和 layered（notify 列表形状）里换成正确的层名。本课不宣称你已经会改通知或 SSO 的内部机制。

## 先把宏观地图放在桌上

我们选一条**真实存在、两端都是 GET** 的只读查询当走廊：

- 画面：`packages/web-domains/demo/src/test-demo/DemoPage.vue`（测试单列表）
- 领域服务：`createDemoService` → `listDemo` → `GET /demo/demo/list`
- 接线：`frontend/apps/admin-web/src/application/services.ts` 里 `demoService = createDemoService(domainHttp, ...)`
- 后端：`TestDemoController` 的 `@RequestMapping("/demo/demo")` + `@GetMapping("/list")`
- 模式：`wta-demo` 是 **classic**（登记表）

这条走廊故意选 demo：它是模块地图里的 CRUD 样例，层少、看得见 Mapper。走完再对照 notify 公告列表，看 layered 多出来的 UseCase 与 DAO。

**类比失效处：** “走廊接力”像接力赛。失效点：真实请求里还有鉴权、数据权限、分页包装。本课为了 OBJ-06，把这些标成“走廊墙上的消防栓”，不拆开 Outbox 或 SSO。

## 核心概念与机制

### 直觉讲解

你在管理端点「搜索」。不是页面自己冲到数据库。它像传话：

厅堂（App 已接好的 http）→ 菜单照片（web-domain 页面）→ 中央厨房点菜单（domain `listDemo`）→ 插头（adapter）→ 大楼门卫（Controller，还要看通行证 `demo:demo:list`）→ 这间 classic 厨房的厨师（ServiceImpl）→ 仓库货架（Mapper / XML 或 wrapper SQL）→ MySQL。

如果你让厅堂直接对数据库喊，或者让门卫跳过厨师去开仓库，传话会快一秒，明天别人不知道该改哪一站。

### 精确定义与 English term

| 中文 | English | 精确定义（本课走查） |
| --- | --- | --- |
| 只读查询 | read query | 不改变业务状态；本例 `GET /demo/demo/list` |
| 表现页 | web-domain page | `DemoPage.vue`：筛选与表格，调用 domain 服务 |
| 领域服务 | domain service | `DemoService.listDemo`：发 HTTP GET，并把 transport 映射成 `DemoVO` |
| 入口控制器 | controller | `TestDemoController.list`：校验查询组、查权限、调 Service |
| 服务实现 | ServiceImpl（classic） | `TestDemoServiceImpl.queryPageList`：组 wrapper，调 `demoMapper.selectVoPage` |
| 映射器 | Mapper | `TestDemoMapper extends BaseMapperPlus<TestDemo, TestDemoVo>` |
| 抄近路 | layer skip | 跳过某一层的合法主人，例如页面直写 URL、Controller 注入 Mapper、App 复制 domain 类型 |

**Walkthrough** 不是调试器截图。它是你能用层名讲完、并能指到工作树文件的一条因果链。

### 机制/因果链

按时间顺序：

1. **App 组合已经发生过**（启动时，不是每次点击）。`admin-web` 依赖 `@namewta/domain-demo` 与 `@namewta/web-domain-demo`，`services.ts` 创建 `demoService`，manifest 注册演示页面。没有这一步，菜单键解析不到组件。
2. **用户在 DemoPage 点搜索。** 页面属于 web-domain。它应当调用注入进来的 demo 服务，而不是自己 `axios.get`。
3. **domain `listDemo`。** 工作树：`url: '/demo/demo/list'`，`method: 'get'`，`params: query`。若有 `rows`，用 `projectDemoTransport` 映射成领域 `DemoVO`。HTTP 合同在这里被消费，Java 类型进不来。
4. **adapter / platform HTTP** 把请求送到后端。细节属平台，本课只要求：出门的是 GET，路径是 `/demo/demo/list`。
5. **`TestDemoController.list`。** `@SaCheckPermission("demo:demo:list")`，参数是 `TestDemoBo` + `PageQuery`，返回 `R<PageResult<TestDemoVo>>`。Controller 只调 `testDemoService.queryPageList`。
6. **classic 分叉：** `TestDemoServiceImpl` **直接持有** `TestDemoMapper`，`selectVoPage`。没有 UseCase，没有 DAO。这在登记表上合法，因为 demo 是 classic。
7. **Mapper / SQL** 打到 MySQL。列表还带数据权限注解能力（Mapper 上的 `@DataPermission` 用于自定义分页路径）。本课点到为止：权限是走廊上的闸机，不是另一栋楼。
8. **JSON 回家。** 前端 domain 映射 → 页面表格。字段名仍是 L-005 的合同。

对照（不是第二条要背完的业务）：公告列表 `GET /notify/notice/list`。`NotifyNoticeController` 只持有 `NotifyNoticeUseCase`；UseCase.list 调 `NotifyNoticeService.page`；Service 只通过 `NotifyPersistenceDao` 碰库。这是 layered 的合法形状。不要把 demo 的 ServiceImpl 抄进 notify。

### 图、表或文本图

**图题 / caption：** `GET /demo/demo/list` 层间接力（classic），虚线框是错误抄近路。

```text
[浏览器 DemoPage.vue]  web-domain
        |
        |  调用 demoService.listDemo
        v
[domain-demo listDemo]  GET /demo/demo/list
        |
        |  App 注入的 domainHttp（adapter）
        v
[HTTP GET]  /demo/demo/list   权限稍后在服务端再查
        |
        v
[TestDemoController.list]  classic 入口
        |
        v
[ITestDemoService / TestDemoServiceImpl]
        |
        v
[TestDemoMapper] --> SQL --> MySQL

虚线抄近路（不要画实线）：
  DemoPage ──x──> axios('/demo/demo/list')     跳过 domain
  Controller ──x──> TestDemoMapper            跳过 Service
  新模块 ──x──> 复制 ServiceImpl 持 Mapper      跳过登记表（应用 layered）
  只改 DemoVO 前端字段 ──x──> 不改后端          跳过合同顺序
```

**文字等价物：** 实线从演示列表页面出发，进入无界面的 domain 服务 `listDemo`，由它发出 GET `/demo/demo/list`。请求经过 App 配好的 HTTP 适配器到达后端 `TestDemoController` 的 list 方法，再进入 classic 的 `TestDemoServiceImpl`，最后由 `TestDemoMapper` 访问数据库。四条虚线是禁止的捷径：页面自己发 URL；控制器直接拿 Mapper；新业务模块照抄 ServiceImpl；只改前端字段。闸机（`demo:demo:list`）在控制器上，不在 Vue 的 `v-hasPermi`——按钮隐藏不是授权。

**图的边界：** 这条链在 `bundle-core` 里可能不存在，因为 core 剖面不组装 `wta-demo`。本图也不包含修改接口：工作树里修改仍是 PUT，那是棘轮，不是本课 GET 走廊。

### 正例、反例与边界

**正例 1：** 列表查询全程 GET，前后端路径都是 `/demo/demo/list`。只读走查询合同。

**正例 2：** Controller 依赖 Service 接口，不依赖 Mapper。即使 classic 允许 ServiceImpl 持 Mapper，入口仍然不越层。

**正例 3：** 若你走 notify 列表，Controller 只依赖 UseCase。换层名，不换“入口不得抓仓库钥匙”这条规矩。

**反例 1（抄近路）：** 在 `DemoPage.vue` 里写死 `fetch('/demo/demo/list')`。第二个终端无法复用映射和错误处理；URL 成为页面私产。

**反例 2（抄近路）：** 给 `TestDemoController` 加上 `TestDemoMapper` 字段，“列表很简单不用 Service”。闸机后面没有厨师，校验和包装会散落。

**反例 3（抄近路）：** 新建业务模块时，把 `TestDemoServiceImpl` 连同直接持 Mapper 一起粘贴。demo 是 classic 样例，**不是**新模块模板。新模块默认 layered（L-003）。

**反例 4（抄近路）：** 前端先把列加上 `color`，后端列表还没有该字段（L-005 顺序反了）。走查会在 JSON 这一站断掉。

**边界：** `v-hasPermi="['demo:demo:list']"` 只藏按钮。没有权限的人仍可能打 GET；服务端 `@SaCheckPermission` 才是闸机。UI 隐藏不能当授权合同。

## 变式与迁移

- **变式 A：同一资源的详情。** `GET /demo/demo/{id}` → `getDemo` / `getInfo`。层相同，只是路径多主键。
- **变式 B：换到 layered GET。** 把口述里的 ServiceImpl 换成 UseCase + Service + DAO。其余（App 组合、domain HTTP、权限注解）仍在。
- **变式 C：core 包。** 走查失败时先问 classpath 有没有 `wta-demo`，再问代码写没写。
- **迁移到改列表：** 改列 = 可能改 VO + SQL + transport 映射 + 页面。从 L-005 的顺序走，再回到本课检查有没有跳层。

## 常见误区

1. **“页面能看到按钮 = 有权限。”** 按钮是化妆品；Controller 注解才是门禁。
2. **“classic 已经持有 Mapper，Controller 再拿一次也没关系。”** 入口越层会让校验和事务失去唯一位置。
3. **“demo 好抄，所以新模块先 classic。”** 登记表禁止把未登记新模块默认成 classic。
4. **“走查等于已经掌握。”** 本课只训练口述和指文件。没有作业、没有延迟复习，能力仍未验证。

## 非评分暂停

把这八个词按点击列表的顺序排一下（可在心里排，不要写成答卷）：DemoPage、listDemo、GET、Controller、ServiceImpl、Mapper、MySQL、admin 组合。少了“组合”的人，通常会在“为什么菜单打开是白页”那里迷路。

再把其中一站改成虚线抄近路，说一句会坏什么。一句就够。

## 总结、词汇表与下一步

- 主走廊：web-domain → domain GET `/demo/demo/list` → Controller → classic ServiceImpl → Mapper → SQL。
- layered 对照：Controller → UseCase → Service → DAO → Mapper。
- 至少记住一种抄近路：页面直打 URL，或新模块照抄 ServiceImpl。
- 本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

标准路径 L-001～L-006 到这里走完。下一步由你选：`H-homework` 出题，或针对 Notify/SSO 另开 Change。本课不自动串联。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `01-module-map.md` | demo 为前端/后端 CRUD 基线；App 显式组合 | 实现基线表 | 2026-09-14 |
| S-005 | `03-backend-module-modes.md` | demo=classic；notify=layered | 当前登记 | 2026-09-14 |
| S-006 | `architecture-and-boundaries.md` | 前端边界；五层禁止入口直连 Mapper | ARCH-003、ARCH-002A | 2026-09-14 |
| S-008 | `admin-web` 组合与三个 App 包 | `services.ts` 接线；App 真实存在 | 工作树 | 2026-09-14 |
