---
lesson_id: L-002
objective_ids: [OBJ-02]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 14
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 4
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: standard
source_ids: [S-001, S-004, S-006, S-010]
---

# Lesson 002：后端不是一锅炖

## 学完你能做什么

你能画出后端依赖方向：`wta-admin` 负责组装启动，业务模块经过 `wta-api` 和自己真正需要的 `wta-common-*` 协作，**common 不反向依赖业务模块**。你也能指出：启动类在 `org.namewta` 包里，名字是 `NamewtaApplication`。

## 先把宏观地图放在桌上

L-001 只说“`backend/` 是服务器房间”。走进去会看到更多门牌。不要把它们想成“全是业务代码”。

```text
backend/
├── wta-admin/          ← 插座板：把模块插上，然后启动
├── wta-api/            ← 房间之间的对讲机（跨业务公开合同）
├── wta-common/         ← 工具间（OSS、Redis、MyBatis 基础……）
├── wta-modules/        ← 真正的业务房间（system / demo / notify / sso …）
└── wta-extend/         ← 另外一些可单独部署的应用（监控、Job 服务器等）
```

**类比失效处：** “插座板”不是说 admin 里完全没有 Java 代码。它有启动类和组装。失效点是：你不能把可复用的业务规则塞进 admin，让别的模块去 import admin。admin 依赖别人，别人不依赖 admin 来复用领域逻辑。

## 核心概念与机制

### 直觉讲解

过年聚餐：有人负责摆桌（admin），有人负责菜单上写“红烧肉怎么对外点”（api），有人负责公共菜刀砧板（common），有人负责真正炒菜（modules）。

如果炒菜的人跑去翻另一桌灶台里的私房酱料（深用别人的 Mapper、内部实体），下一桌换厨师时整桌都会咸。所以跨桌只看菜单（`wta-api`），不进别人后厨。

工具间（common）更不能反过来找炒菜师傅要“业务特例”——否则菜刀厂要懂每一道菜，工具就不再是工具。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 可部署应用 | deployable application / assembly | `wta-admin`：Spring Boot 入口，把 api / common / 选中的 modules 组成一个进程 |
| 公开 API 模块 | public API module | `wta-api`：跨业务模块可依赖的类型与服务合同，不是某个业务的 Mapper |
| 基础能力 | common capabilities | `wta-common-*`：可复用基础设施（Web、MyBatis、Redis、通知契约……） |
| 业务模块 | business module | `wta-modules/*`：某个领域的用例与持久化 |
| 依赖方向 | dependency direction | 箭头只许“组装者 → 业务 → api/common”，不许 common → 业务，不许业务深挖另一业务内部 |
| 组装剖面 | bundle profile | Maven profile：`bundle-full` 默认带上 job/ai/demo/workflow/profile；`bundle-core` 去掉 job/ai/demo/workflow，仍留 profile |

**Dependency direction** 不是社交礼貌，是编译期能检查的箭头。你在业务模块的 `pom.xml` 里加对另一个业务 implementation 的依赖，就是在把箭头画反或画穿。

### 机制/因果链

1. JVM 从 `org.namewta.NamewtaApplication` 启动（S-010）。
2. `wta-admin` 的 POM 声明它依赖哪些模块。平台基础（system、notify、sso、third、api、一批 common）写在常驻依赖里。
3. 默认激活 `bundle-full`，额外插上 `wta-job`、`wta-ai`、`wta-demo`、`wta-workflow` 和 profile 的 person/enterprise。
4. 显式 `-Pbundle-core` 时停掉 full，core 剖面**不**再插 job/ai/demo/workflow，但档案模块仍在。所以“demo 列表示例”在 core 包里可能根本不在 classpath。
5. 业务模块 A 要问业务模块 B 一件事：只依赖 `wta-api` 上的公开类型（例如通知模块用 `org.namewta.system.api.UserService` 解析用户），不 import B 的 Mapper。
6. 业务模块可以使用所需的 `wta-common-*`。common 的 POM 不得再依赖 `wta-modules/*`。

因果：组装在 admin → 合同在 api → 规则在模块 → 工具在 common。把规则塞进 common 或把 Mapper 借给邻居，箭头会环，以后谁都不敢改。

### 图、表或文本图

**图题 / caption：** 后端依赖方向（箭头 = Maven/Java 允许依赖）。

```text
        wta-extend/*（独立进程，本课不展开）

                 wta-admin  （组装 + 启动）
                    |
                    |  依赖（admin → 别人）
                    v
     +--------------+----------------+
     |              |                |
  wta-api     wta-modules/*     wta-common-*
  对讲机         炒菜房间           工具间
     ^              |
     |              |
     +--------------+  业务只依赖 api + 所需 common
                       业务之间不互挖后厨

  禁止：wta-common-*  →  wta-modules/*
  禁止：wta-demo.mapper → 被 wta-system 直接 import
```

**文字等价物：** 图的最上是可部署的 `wta-admin`，它指向三类东西：跨模块合同 `wta-api`、各业务模块、各 common 工具。业务模块可以指回 `wta-api` 和自己需要的 common，但不能让 common 指回业务，也不能让一个业务直接使用另一个业务的 Mapper 或内部实体。`wta-extend` 画在旁边，表示还有别的可部署进程，本课不把它们和 admin 混成一个箭头。

**图的边界：** 这张图不区分 layered / classic（那是 L-003），也不画 HTTP 路径（L-005）。`bundle-full` 与 `bundle-core` 改变的是 admin **插了哪些业务模块**，不改变箭头方向。

### 正例、反例与边界

**正例 1：** `wta-admin` 依赖 `wta-notify`、`wta-sso`、`wta-system`、`wta-api`。这是插座插电器。

**正例 2：** 通知业务要解析“发给哪些用户”时，依赖 `wta-api` 的 `UserService`，而不是打开 `wta-system` 的用户 Mapper。

**正例 3：** 新建一个工具类处理 JSON。放进已有的 `wta-common-json`（或按 common 规则新增 common 模块），不要为了图省事丢进 `wta-admin`。

**反例 1：** 在 `wta-common-core` 里 import `org.namewta.demo...`。工具间开始懂演示业务，箭头反了。

**反例 2：** `wta-system` 的某个 ServiceImpl 直接注入 `TestDemoMapper`。后厨打通，demo 一改 SQL，系统模块跟着炸。

**反例 3：** 因为看见旧组织名，就在新代码里使用旧组织包名。启动类是 `NamewtaApplication`，包名合同是 `org.namewta`。

**边界：** `wta-api` 不是“把所有 VO 都倒进去的筐”。只有**跨业务真正需要**的合同才上去。模块内部的 BO/VO 留在模块里。common 也不是业务垃圾桶。

## 变式与迁移

- **变式 A：** 你在 core 包里找不到 `/demo/demo/list`。先看是不是 `bundle-core` 根本没插 `wta-demo`，不是“接口写错了”。
- **变式 B：** 两个业务都要发邮件。不要互相调用对方的 Service；走 common 的通知/邮件 SPI，或走已有的通知合同。
- **迁移：** 接到“加一个后端功能”，先问：这是新业务房间、旧房间的新用例，还是一段可复用工具？再决定放 modules、api 还是 common。新业务房间的内部层次见 L-003。

## 常见误区

1. **“admin 很大，所以业务就写在 admin。”** admin 大是因为组装，不是因为领域规则属于它。
2. **“都在 org.namewta 下，互相 import 没关系。”** 包名相同不等于依赖方向合法。
3. **“common 更公共，所以把业务也放进去更共享。”** 共享的是工具，不是某条业务规则。
4. **“api 模块等于 OpenAPI 文档。”** `wta-api` 是 Java 跨模块合同；浏览器看到的 HTTP/JSON 是另一张合同，L-005 再拆。

## 非评分暂停

用手指在空气中画四个盒子：admin、api、modules、common。然后只画允许的箭头。如果你画出了 common → demo，停下来，那条箭头就是本课要拆掉的那根电线。

再问自己：`bundle-full` 和 `bundle-core` 改变的是箭头方向，还是插座上插了几件电器？

## 总结、词汇表与下一步

- admin = 组装启动；api = 跨业务对讲；modules = 业务；common = 工具。
- 箭头：组装者指向别人；业务指向 api 与所需 common；common 不指向业务。
- 启动类是 `NamewtaApplication`，包名是 `org.namewta`。
- `bundle-*` 只改变 classpath 里有哪些业务模块。

下一步：L-003 走进业务房间内部，看为什么有的模块是五层，有的仍是 classic，以及为什么同一模块不能两种写法混用。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-001 | `README.md` | admin 只组装；跨模块走 api 或 common SPI | 架构一览段落后说明 | 2026-09-14 |
| S-004 | `01-module-map.md` | 模块职责、依赖方向、bundle-full/core | 「后端 Maven 模块」「依赖方向」 | 2026-09-14 |
| S-006 | `architecture-and-boundaries.md` | ARCH-002 后端依赖方向 | ARCH-002 | 2026-09-14 |
| S-010 | `NamewtaApplication.java` | 启动入口类名与包名 | `org.namewta.NamewtaApplication` | 2026-09-14 |
