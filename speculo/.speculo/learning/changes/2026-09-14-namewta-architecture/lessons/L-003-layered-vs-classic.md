---
lesson_id: L-003
objective_ids: [OBJ-03]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 14
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 5
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: standard
source_ids: [S-004, S-005, S-006, S-007]
---

# Lesson 003：两种后端写法，为什么不能混

## 学完你能做什么

你能打开模块模式登记表，指出哪些业务模块是 `layered`、哪些是 `classic`；能说出**未登记的新业务模块默认五层**；并且能拒绝在同一个模块里把两种模式混着写。

## 先把宏观地图放在桌上

L-002 把业务都放进 `wta-modules/*` 这层楼。进到一个具体房间，家具摆法有两种，而且**以登记表为唯一名单**，不是“看哪个目录比较顺手”。

```text
登记表说 layered 的房间（新写法）：
  门卫 Controller / Listener / API Adapter
       → 管家 UseCase
            → 厨师 Service（规则，不碰 Mapper）
                 → 仓库管理员 DAO
                      → Mapper → XML

登记表说 classic 的房间（存量）：
  门卫 Controller
       → 厨师+仓库 Service / ServiceImpl（可以持有 Mapper）
            → Mapper → XML
```

**类比失效处：** “管家 / 厨师 / 仓库”帮你记谁不能进仓库。失效点：UseCase 不是“再写一遍 Service 的别名”，DAO 也不是“Mapper 换个名字”。每一层少一样禁止事项。把 ServiceImpl 留在 layered 房间，等于请厨师同时管仓库钥匙。

## 核心概念与机制

### 直觉讲解

旧房子（classic）厨房和仓库门是通的：`ServiceImpl` 可以直接抱着 Mapper 写 SQL 条件。房子已经住人了（system、demo、workflow、job、ai），为了不把全楼拆光，登记表允许它们暂时保持这种摆法。但**不允许为了新功能把通道再开宽**。

新房子（layered：profile、notify、sso，以及以后未登记的新模块）规定：门口只认识管家（UseCase）；管家只吩咐厨师（Service）；厨师只叫自己的仓库管理员（DAO）；只有 DAO 能碰 Mapper 和 MyBatis 类型。

小孩子会问：为什么新房子这么麻烦？因为旧房子里，炒菜、记账、进货、打电话给隔壁，最后全堆在一个 `ServiceImpl` 里。一改 SQL，事务、权限、外部调用一起抖。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 五层模块 | layered module | 调用链固定为 Controller/Listener/API Adapter → UseCase → Service → DAO → Mapper → XML |
| 存量模块 | classic module | Controller → Service → ServiceImpl → Mapper → XML；仅保护登记表上的存量 |
| 登记表 | module-mode registry | `03-backend-module-modes.md`：唯一决定某模块哪种模式的入口 |
| 用例层 | UseCase | 入口适配器之后的编排；可开事务；不得直连 DAO/Mapper |
| 领域服务 | Service（layered 含义） | 业务规则；不得导入 Mapper、`IService`、`ServiceImpl`，不得互调另一个 Service |
| 数据访问对象 | DAO | 业务里唯一持有 Mapper / MyBatis 类型的层 |
| 棘轮 | Ratchet | 新代码按 Target；classic 不借机全仓改造成 layered |

**Layered** 在本仓库不是“多几个文件夹好看”，而是 import 方向和验证脚本（`validate-module-mode.mjs`）会检查的合同。

### 机制/因果链

1. 你要改或新增后端业务。先打开登记表，不要先复制邻居的 `service/impl`。
2. 若模块已是 classic：沿用 Controller → Service → ServiceImpl → Mapper。可以持有本模块 Mapper。不要在这个模块里突然再加一套 UseCase/DAO 当“新旧并存的日常写法”。
3. 若模块已是 layered，或这是未登记的新业务模块：默认 layered。Controller 只注入 UseCase。例如公告列表：`NotifyNoticeController` 调 `NotifyNoticeUseCase.list`，UseCase 再调 `NotifyNoticeService.page`，Service 只通过 `NotifyPersistenceDao` 碰库。
4. 跨模块仍然走 `wta-api`（L-002）。layered 的 Service 可以依赖公开 API，例如通知 Service 用 `UserService`，那是 Port，不是去拿 system 的 Mapper。
5. classic 改 layered 不是改个目录名。必须先映射旧路径、公开合同、调用方、SQL、测试和回滚。本课不授权你把 `wta-system` 拆了。

因果：登记表 → 选择调用链 → 每一层的禁止事项 → 验证脚本能检查。混用 = 两把仓库钥匙，检查脚本和人脑都会漏。

### 图、表或文本图

**图题 / caption：** 当前登记（2026-09-14 工作树 / S-005），以及两条链哪里分叉。

```text
layered（登记）：                 classic（登记）：
  wta-profile                     wta-system
  wta-notify                      wta-workflow
  wta-sso                         wta-job
  （未登记的新业务模块也走这里）      wta-demo
                                  wta-ai

HTTP 进来
   |
   v
Controller ----+---- UseCase ---- Service ---- DAO ---- Mapper ---- XML     layered
               |
               +---- Service / ServiceImpl -------------- Mapper ---- XML     classic
```

**文字等价物：** 左列三个模块（profile、notify、sso）以及将来新建、尚未登记的业务模块，必须走五层：控制器把请求交给 UseCase，UseCase 编排 Service，Service 只通过 DAO 访问数据库，DAO 再调用 Mapper 和 XML。右列五个模块（system、workflow、job、demo、ai）维持 classic：控制器交给 Service 接口，实现类 ServiceImpl 可以直接持有 Mapper。两条链在 Controller 之后分叉，不能在同一模块里各写一半。`wta-admin`、`wta-api`、`wta-common-*` 不在这张业务五层登记表里。

**图的边界：** 本图不教通知 Outbox、SSO 授权码细节。只教“进房间后家具怎么摆”。Listener / API Adapter 与 Controller 同属入口，也不得跳过 UseCase 去抓 DAO。

### 正例、反例与边界

**正例 1：** 读公告列表。`NotifyNoticeController` 只有 `NotifyNoticeUseCase` 字段；`list` 方法把分页参数交给 UseCase。这是 layered 入口。

**正例 2：** 读测试单表列表。`TestDemoController` 注入 `ITestDemoService`；`TestDemoServiceImpl` 字段是 `TestDemoMapper`。这是登记过的 classic，允许。

**正例 3：** 你要新增 `wta-modules/wta-billing`。没有登记为 classic 的例外，就按 layered 建目录，并先补登记表。

**反例 1：** 在 `wta-notify` 里新建 `service/impl` 并直接注入 Mapper，“先跑起来再分层”。这是同一模块混模式。

**反例 2：** 在 `wta-demo` 里加 UseCase + DAO，但 Controller 仍有另一半方法直接打 Mapper。两条链并存，下次谁都不知道该改哪条。

**反例 3：** 因为 demo 的 ServiceImpl 好复制，新模块先抄它。登记表写明：demo 不得当 layered 的反面教材。

**边界：** common 模块不按业务五层硬套目录。把 Redis 工具改成 UseCase 是套错衣服。classic 保护的是存量结构，不是“可以再新开一个 ServiceImpl 去碰别人的表”。

## 变式与迁移

- **变式 A：** 改 `wta-system` 用户列表。保持 classic 形状，按 Ratchet 收紧触及的文件，不借机全模块搬家。
- **变式 B：** 改 `wta-sso` 令牌发放。保持五层；只经 `wta-api` 读用户与 Client。
- **迁移 classic → layered：** 先列旧 URL、权限、Mapper XML、测试。搬家和改行为分开验证。没有这份地图，不准说“只是重构”。
- **与 L-006 的衔接：** 同一条 GET，在 demo 是 Controller→ServiceImpl→Mapper；在 notify 是 Controller→UseCase→Service→DAO→Mapper。口述时先报模块模式，再报层名。

## 常见误区

1. **“多几个包就是 layered。”** 若 Service 仍 import Mapper，目录再漂亮也是 classic 灵魂。
2. **“UseCase 可以顺便查库，少一层更快。”** 更快的是把钥匙再塞回厨师口袋，检查项会失效。
3. **“未登记就先 classic，以后再升。”** 登记表：没有证据不得以 classic 当临时默认。
4. **“两种模式可以在同一模块里按方法挑选。”** 同一模块一种模式。混用会让验证脚本和人同时迷路。

## 非评分暂停

打开登记表（或回想本课那两列）。随便点一个你最近改过的后端文件，问：它落在哪一列？如果你准备新建一个类去持有 Mapper，它在 layered 列合法吗？

不要在本课给自己打分。只要能感到“先查表，再动手”这根筋，就够进入下一课。

## 总结、词汇表与下一步

- 查表，不要猜：layered = profile / notify / sso + 新业务；classic = system / workflow / job / demo / ai。
- 新模块默认五层；同一模块禁止混用。
- classic 是兼容窗口，不是新功能的样板。
- 五层里只有 DAO 碰 Mapper。

下一步：L-004 离开 Java 房间，看前端为什么不能再复制一份 `src/api`。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `03-backend-module-modes.md` | 模式定义、当前登记、未登记默认 layered | 全文登记表 | 2026-09-14 |
| S-006 | `architecture-and-boundaries.md` | ARCH-002A 五层调用链 | ARCH-002A | 2026-09-14 |
| S-007 | `02-decisions-and-exceptions.md` | DEC-009 新模块默认五层；Ratchet | DEC-009、DEC-002 | 2026-09-14 |
| S-004 | `01-module-map.md` | 各模块角色；demo 为 classic 样例 | 模块表与实现基线 | 2026-09-14 |
