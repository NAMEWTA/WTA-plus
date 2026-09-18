---
lesson_id: L-002
objective_ids: [OBJ-02]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 5
  - segment: deep-explanation
    minutes: 15
  - segment: visuals-and-worked-examples
    minutes: 10
  - segment: pause
    minutes: 4
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-001, S-004, S-006, S-010]
claimed_cells: [C:wta-admin, C:wta-api, C:wta-modules, C:wta-common]
---

# Lesson 002：后端不是一锅炖

## 学完你能做什么

你能画出后端 **C4 容器** 的依赖方向，并且能指着工作树核对，而不是凭记忆。本课四个格子是：`C:wta-admin`、`C:wta-api`、`C:wta-modules`、`C:wta-common`。

- `wta-admin` 是可部署进程：**组装**启动，把选中的模块插进同一个 classpath。它**可以**实现 `wta-api` 上的端口（例如 `SsoIdentityService`），也可以放主机接线（启动类、登录 HTTP、基础设施）。那仍是组装，不是「admin 拥有领域规则」。
- 业务模块住在 `wta-modules/*`。跨房间只走 `wta-api` 的公开类型，再按需拿最小的 `wta-common-*`。
- `wta-api` 是**一个 jar**。工作树上的 Java 分包是 `system`、`notify`、`profile`、`sso`、`third`、`workflow`。模块内部的 `service` 接口不上这张菜单。
- `wta-common-*` 是工具间。**common 永不依赖 `wta-modules`。** 库存以 `backend/wta-common/` 目录为准；本 Goal 只把 notify / mybatis / satoken / openapi 四件当主路径枢纽。
- 启动类是 `org.namewta.NamewtaApplication`。`bundle-full` 与 `bundle-core` 只改变插座上插了哪些电器：core **排除** job / ai / demo / workflow，**保留** `wta-profile-person` 与 `wta-profile-enterprise`。箭头方向不变。
- `wta-extend/*` 是另外的可部署进程。本课只点名，不讲内部。

## 先把宏观地图放在桌上

L-001 只说「`backend/` 是服务器房间」。走进去会看到更多门牌。不要把它们想成「全是业务代码」。本课的 C4 粒度是 **Container**：四个要认的盒子是 admin / api / modules / common。extend 画在旁边，不当作第五个本课格子。

工作树里，`backend/pom.xml` 的 reactor 子模块是五块：

```text
backend/                         ← Maven 根：artifactId=wta-vue-plus
├── wta-admin/                    ← 可部署主应用：组装 + 启动（C4 容器）
├── wta-api/                      ← 跨业务公开 Java 合同（一个 jar，C4 容器）
├── wta-common/                   ← 聚合 POM（packaging=pom，不是 jar）
│   └── wta-common-*              ← 真正的工具 jar（C4 容器：工具间）
├── wta-modules/                  ← 业务聚合 POM（packaging=pom，C4 容器：业务房间）
│   ├── wta-ai
│   ├── wta-demo
│   ├── wta-job
│   ├── wta-notify
│   ├── wta-profile/              ← 再聚合 person / enterprise / bom
│   ├── wta-sso
│   ├── wta-system
│   ├── wta-third
│   └── wta-workflow
└── wta-extend/                   ← 另外三个可部署进程（本课范围外）
    ├── wta-monitor-admin
    ├── wta-snailjob-server
    └── wta-snailai-server
```

根 README 的架构树常把 `wta-extend` 省略。以工作树和 `backend/pom.xml` 为准：extend 存在，只是**不是** `wta-admin` 这根电线。

**类比失效处：** 把 admin 想成「插座板」只帮你记组装职责。插座板上其实有接线代码（启动类、登录 HTTP），也**可以**实现 api 端口。失效点是：你不能把**可复用的领域规则**塞进 admin，也不能让别的业务模块去 import `org.namewta.web.*` 来复用登录或领域逻辑。admin 依赖别人；别人不依赖 admin。

## 核心概念与机制

### 直觉讲解

过年聚餐：有人负责摆桌（`wta-admin`），菜单上写「红烧肉怎么对外点」（`wta-api`），公共菜刀砧板在工具间（`wta-common-*`），真正炒菜的是各桌厨师（`wta-modules/*`）。

跨桌只看菜单，不进别人后厨。通知桌要「发给哪些用户」，去菜单上找 `org.namewta.system.api.UserService`，不要去系统桌翻 `SysUserMapper`。

摆桌的人可以看见每一桌的锅——因为他是主人。他甚至可以自己当某一道「菜单项」的现场实现（`AdminSsoIdentityService implements SsoIdentityService`，并注入 `SysUserMapper`）。那是主机接线，不是「摆桌的人改行当厨师、把用户规则搬进 admin 当公共库」。邻居厨师仍然不能去翻系统桌的锅。

工具间更不能反过来找炒菜师傅要「这道菜的特例」。否则菜刀厂要懂每一道菜，工具就不再是工具。

「通知」其实是三套门口，不要揉成一个盒子：

| 门口 | 住哪 | 你先记住的用途 | 本课停在门口 |
| --- | --- | --- | --- |
| `org.namewta.notify.api.NotificationApplicationService` | `wta-api` 的 notify 包 | 跨房间提交通知的 Java 合同 | 不讲 Outbox |
| `wta-notify` | `wta-modules` | 通知控制面（公告、收件人、编排） | 不讲渠道回调 |
| `wta-common-notify` | 工具间 | 渠道无关的分发与适配器 SPI | 不讲 `NotifyDispatcher` 内部；它依赖 `wta-common-nacos`，Nacos 本 Goal 已推迟 |

四件主路径工具可以先认门口牌子。其余 common 子目录打开 `backend/wta-common/` 就能看见；本课不当成主路径，也不背一个「一共几个 jar」的数字——那个数字会过期。

| 门口牌子 | 你先记住的用途 | 本课停在门口 |
| --- | --- | --- |
| `wta-common-notify` | 渠道无关的通知契约与分发 | 不讲 Outbox / 供应商回调 / Nacos |
| `wta-common-mybatis` | `BaseEntity` / `BaseMapperPlus` / 分页查询 | 不讲 XML 怎么写 |
| `wta-common-satoken` | `LoginHelper` 登录态 | 不讲 Redis 会话失败细节 |
| `wta-common-openapi` | 机器调用协议、注册表、网关 | 不讲 HMAC 实现 |

**类比失效处：** 聚餐比喻不覆盖 Maven profile、fat jar、独立进程、传递依赖。下一节用精确箭头补上。厨师换桌（layered / classic）是 L-003；菜单上的 HTTP/JSON 是 L-005。`SsoIdentityService` 的认人细节是后课，本课只认「实现可以落在主机」。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 可部署应用 / 组装 | deployable application / composition | `wta-admin`：Spring Boot 入口，把 `wta-api`、所选 `wta-modules/*`、所需 `wta-common-*` 组成**一个 JVM 进程**。入口类 `org.namewta.NamewtaApplication`（`@SpringBootApplication` 默认扫 `org.namewta`）。组装者**可以**实现 api 端口、持有基础设施与被插模块的内部类型；**不可以**把可复用领域规则当成公共库塞进 admin。 |
| 公开 API 模块 | public API module | `wta-api`：跨业务可依赖的 Java 类型与服务合同。自身 POM 只依赖 `wta-common-core`、`wta-common-json` 和 `spring-core`。分包见下表。 |
| 基础能力 | common capabilities | `wta-common-*`：可复用基础设施 jar。父模块 `wta-common` 是聚合 POM，业务 POM 不要依赖这个父 artifact。库存用目录列举，不用死记个数。 |
| 业务模块 | business module | `wta-modules/*`：某个领域的用例与持久化。父模块 `wta-modules` 也是聚合 POM。 |
| 依赖方向 | dependency direction | 编译期允许的 Maven/Java 箭头：组装者 → 业务 / api / common；业务 → api + 所需 common；common **不得** → `wta-modules/*`。 |
| 组装剖面 | bundle profile | `wta-admin/pom.xml` 的 Maven profile。`bundle-full` 默认激活；显式 `-Pbundle-core` 停掉 full。 |
| 主路径枢纽 | happy-path hubs | 本 Goal 只把 notify / mybatis / satoken / openapi 四件 common 当作架构主路径。其余 common 存在，但不在本课格子里。 |
| SPI 合同 | service provider interface | 工具间或 api 声明接口，实现可落在**业务模块**或**主机**。方向仍是实现方依赖合同，不是 common 依赖业务。 |
| 独立扩展进程 | extension process | `wta-extend/*`：另外的 Spring Boot 应用，有自己的 `*Application` 启动类。不是 admin 的子包。若误把 extend 当 jar 插进 admin，默认扫描 `org.namewta.**` 会把另一进程的启动类煮进来。 |

**Dependency direction** 不是礼貌。它是 `pom.xml` 和 import 能检查的箭头。你在业务模块 POM 里加对另一个业务 implementation 的依赖，就是在把箭头画穿。

有一条容易看错的细线：若干 common 会依赖 `wta-api` 上的稳定类型，例如 `org.namewta.system.api.model.LoginUser`。工作树上 **POM 显式依赖 `wta-api` 的 common** 是：`wta-common-satoken`、`wta-common-mybatis`、`wta-common-translation`、`wta-common-log`、`wta-common-push`。这是 **common → 公开合同**，不是 **common → 业务模块**。禁止的是 `import org.namewta.demo...` 或依赖 `wta-system` 这个 artifact。`wta-common-notify` **不**依赖 `wta-api`。

另一条细线：`org.namewta.system.api.*` 是公开合同；`org.namewta.system.service.ISysClientService` 是 system 模块内部服务接口，**不在** `wta-api`。主机可以注入它；邻居模块不行。

`wta-api` 工作树分包（`backend/wta-api/src/main/java/org/namewta/`，2026-09-16 核对）：

| Java 包 | 合同角色（本课只需认包，不背每个类型） | 代表类型 |
| --- | --- | --- |
| `org.namewta.system.api` | 用户、部门、角色、登录 DTO | `UserService`、`LoginUser` |
| `org.namewta.notify.api` | 跨房间提交/查询通知 | `NotificationApplicationService` |
| `org.namewta.profile.api` | 档案投影与材料端口 | `ProfileService`、`CompositeProfileService` |
| `org.namewta.sso.api` | 第一方 SSO 认人端口 | `SsoIdentityService` |
| `org.namewta.third.api` | 第三方 HTTP 网关合同 | `ThirdPartyGateway` |
| `org.namewta.workflow.api` | 工作流启动/完成合同 | `WorkflowService` |

实现这些接口的类**不一定**住在对应 `wta-modules/*`。全仓 `SsoIdentityService` 的实现目前只在 `wta-admin` 的 `AdminSsoIdentityService`。`UserService` 的实现是 `wta-system` 的 `SysUserServiceImpl`。

### 机制/因果链

按启动和编译实际发生的顺序走：

1. **JVM 入口。** `NamewtaApplication.main` 构造 `SpringApplication`，`@SpringBootApplication` 默认扫描 `org.namewta` 包。外部 Tomcat 走同包的 `NamewtaServletInitializer`，仍指向这个启动类。
2. **常驻插座。** `wta-admin/pom.xml` 的非 profile 依赖始终插入：`wta-api`、`wta-system`、`wta-third`、`wta-notify`、`wta-sso`，以及 admin 直接声明的若干 common（doc / social / mail / notify / nacos / mcp / openapi）。MySQL 驱动也在这里。
3. **默认 `bundle-full`。** `activeByDefault=true`。额外插入 `wta-job`、`wta-ai`、`wta-demo`、`wta-workflow`，以及档案 `wta-profile-person`、`wta-profile-enterprise`。
4. **显式 `-Pbundle-core`。** 停掉 full。core 剖面**不再**插 job / ai / demo / workflow，但仍插两份档案。所以 core 包里可能根本没有 `/demo/demo/list` 对应的 Controller，但档案查询还在。
5. **Maven profile → 运行时 classpath → Spring 扫描 → 同一 JVM 接上实现。** bundle 决定 admin 这个可部署 jar **带上谁**。带上之后，接口（api）和实现（模块或主机）落在同一个进程里，Spring 才能注入。`NotifyNoticePublisherService` 编译期只看见 `UserService`；运行期接到的是 `SysUserServiceImpl`。`wta-modules/pom.xml` 仍列出全部业务子模块，`./mvnw test` 仍会编译它们。reactor ≠ fat jar。
6. **直接 POM ≠ Java import 集合。** admin POM **没有**声明 `wta-common-satoken` / `wta-common-encrypt`，但 `AuthController` 仍能使用 `LoginHelper`、`@ApiEncrypt`——它们经被插模块（如 `wta-system`）传递进编译 classpath。四件枢纽里，admin 直接声明的是 notify 与 openapi；mybatis / satoken 不在 admin POM 里，仍是本课主路径门口。只背 admin `<dependencies>` 会看漏传递边。
7. **跨业务只走合同。** 通知发布时，`NotifyNoticePublisherService` 注入 `org.namewta.system.api.UserService` 解析收件人，再调用 `org.namewta.notify.api.NotificationApplicationService.submit`。实现类 `SysUserServiceImpl` 住在 `wta-system`，通知模块 POM 只依赖 `wta-api`，不依赖 `wta-system`。
8. **业务按需拿 common。** 例如 `wta-notify` 显式依赖 api + core/json/mybatis/redis/notify/mail/sms/log/security/web/satoken/push。`wta-job` 更瘦：只有 `wta-common-json` 和 `wta-common-job`，连 `wta-api` 都没有。最小原则是「用到再声明」，不是「把 `backend/wta-common/` 下看见的目录一次性写进 POM」。
9. **common 内部可以分层，但不能指回业务。** `wta-common-core` 不依赖其他 `wta-common-*`。satoken 依赖 core + redis + **api 合同**。mybatis 依赖 core + satoken + **api 合同**。openapi 依赖 core + redis + satoken + doc。notify 枢纽额外依赖 `wta-common-nacos`（本 Goal 不展开 Nacos）。全仓 `wta-common-*/pom.xml` 没有任何 `wta-system` / `wta-demo` / 其他 `wta-modules` artifact。
10. **SPI 的实现落点有两种，不要揉成一句。** （a）接口在 common、实现在业务：`DictService` 在 `wta-common-core`，`SysDictTypeServiceImpl` 在 `wta-system` 里 `implements DictService`。OpenAPI 凭据/授权 SPI 在 `wta-common-openapi`，实现在 system。（b）common 自己的类调用 api：`UserNameTranslationImpl` 住在 `wta-common-translation`，POM 依赖 `wta-api`，注入 `UserService`。这是 common → 合同，不是「业务填 SPI」。（c）接口在 api、实现在主机：`SsoIdentityService` 在 `wta-api`，`AdminSsoIdentityService` 在 `wta-admin`。
11. **组装者可以看见实现，邻居不行。** `wta-admin` 的 `AuthController` 可以注入 `ISysClientService`、`NotificationApplicationService`；`AdminSsoIdentityService` 可以注入 `SysUserMapper`。因为 admin 是进程主机。`wta-notify` 不可以注入 `SysUserMapper`。主机接线 ≠ 房间互挖后厨 ≠ admin 拥有用户领域。

因果：组装在 admin → 合同在 api → 规则在模块（偶尔端口实现落在主机）→ 工具在 common。把规则塞进 common，或把 Mapper 借给邻居，箭头会环，以后谁都不敢改。

## 图、表或文本图

**图题 / caption：** C4 容器与后端依赖方向。实线箭头 = Maven/Java 允许依赖。虚线框 = 本课点名但不展开。四个实线容器是本课格子。

```text
  （范围外，独立 C4 容器 / 另一 JVM）
  wta-extend/
    MonitorAdminApplication
    SnailJobServerApplication
    SnailAiServerApplication

                 C:wta-admin
                 NamewtaApplication
                 组装 + 可实现 api 端口
                 可深用被插模块内部类型
                    |
                    |  允许：admin → 别人
                    v
     +--------------+------------------+
     |              |                  |
  C:wta-api   C:wta-modules/*    C:wta-common-*
  公开合同      业务房间            工具 jar
  六包见下      规则住这里          永不 → modules
     ^              |                  ^
     |              |                  |
     +--------------+                  |
     业务 → api + 所需 common ---------+
     若干 common → api 稳定类型（LoginUser 等）
     主机也可 implements api 端口（SsoIdentityService）

  常驻插上（两个 bundle 都有）：
    wta-system, wta-notify, wta-sso, wta-third, wta-api

  bundle-full 另插：
    wta-job, wta-ai, wta-demo, wta-workflow,
    wta-profile-person, wta-profile-enterprise

  bundle-core 另插：
    wta-profile-person, wta-profile-enterprise
    （不插 job / ai / demo / workflow）

  禁止：wta-common-*  →  wta-modules/*
  禁止：业务 A 的 Mapper / 内部实体 被业务 B 直接 import
  禁止：业务模块依赖 wta-admin 来复用登录或领域代码
  允许：主机 import 被插模块 Mapper / 内部 Service（组装，不是领域所有权）
```

**alt：** 自上而下：独立 extend 进程旁置；admin 容器指向 api、业务模块、common 三个容器；业务指回 api 与所需 common；部分 common 可指向 api 合同；主机可实现 api 端口；禁止 common 指向业务，禁止业务互挖 Mapper。

### 文字等价物

图的最上可部署进程是 `wta-admin` 这个 C4 容器。它指向另外三个本课容器：跨模块合同 `wta-api`、各业务模块 `wta-modules`、各 common 工具。业务模块可以指向 `wta-api` 和自己真正用到的 common。部分基础设施 common 可以指向 `wta-api` 里的稳定 DTO/SPI（例如登录用户），但这仍不是指向 `wta-modules`。common 不得指向任何业务模块；一个业务也不得直接使用另一个业务的 Mapper、内部实体或 ServiceImpl。admin 作为主机可以深用被插模块的内部类型，也可以成为某个 api 端口的运行时实现；这不把领域规则的所有权搬进 admin。`wta-extend` 画在旁边，表示还有别的可部署进程，本课不把它们和 admin 混成一个箭头。`bundle-full` 与 `bundle-core` 改变的是 admin **插了哪些业务模块**，不改变箭头方向。core 排除 job / ai / demo / workflow，档案 person / enterprise 两个剖面都在。

**图的边界：** 这张图不区分 layered / classic（L-003），不画 HTTP 路径（L-005），不展开通知 Outbox、SSO PKCE、OpenAPI 签名、登录方法性状（后课）。common 子模块打开 `backend/wta-common/` 列举，本课主路径只认 notify / mybatis / satoken / openapi 四个门口。`AuthController` 只证明主机可接线，不在本课走登录链。

业务房间对照表（`wta-modules/pom.xml` 的 `<modules>` 顺序；`wta-profile` 是子聚合，不是第三间独立业务房）：

| artifact | 房间一句话 | 本课要你记住的依赖姿态 |
| --- | --- | --- |
| `wta-demo` | 示例 CRUD / 树 / 富文本 | 依赖 api + 一批 common；只在 full 进 admin |
| `wta-job` | 业务侧 Job 执行器 | 只依赖 json + `wta-common-job`；只在 full 进 admin |
| `wta-system` | 用户、Client、角色、菜单 | 常驻；实现 `UserService` 等 api 合同 |
| `wta-workflow` | WarmFlow 工作流 | 依赖 api + common；只在 full 进 admin |
| `wta-ai` | AI 业务薄封装 | 依赖 api + `wta-common-ai` 等；只在 full 进 admin |
| `wta-profile` | 聚合 POM：`person` + `enterprise` 两个业务 jar，外加 `wta-profile-bom`（只锁版本，不是第三间房） | 两个子 jar 在 full **和** core 都进 admin |
| `wta-third` | 第三方 HTTP 网关 | 常驻；依赖 api + 所需 common |
| `wta-notify` | 通知控制面 | 常驻；用 api 的 `UserService` 解析用户 |
| `wta-sso` | 第一方 SSO | 常驻；依赖 api + satoken 等；认人端口的实现目前在 admin，不在本模块 |

### 正例、反例与边界

**正例 1（组装）：** `wta-admin` 依赖 `wta-notify`、`wta-sso`、`wta-system`、`wta-third`、`wta-api`。这是插座插电器。启动类在 `backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java`。

**正例 2（跨业务走 api）：** `NotifyRecipientDirectoryService` 只持有 `UserService`，把 `searchActiveUsers` / `selectNotificationUsers` 的结果转成通知 VO。通知 POM 没有 `wta-system`。实现仍由 `SysUserServiceImpl implements ISysUserService, UserService` 提供，运行时由 Spring 在 admin 进程里接上。

**正例 3（最小 common）：** 新建 JSON 工具放到已有 `wta-common-json`（或按 common 规则新增 common 模块），不要丢进 `wta-admin`。`wta-job` 不需要用户合同，就不声明 `wta-api`。需要哪些工具，打开 `backend/wta-common/` 按目录挑选，不要按过期数字全家桶拷贝。

**正例 4a（业务实现 common SPI）：** 字典 SPI `DictService` 住在 `wta-common-core`，`SysDictTypeServiceImpl` 住在 system 并 `implements DictService`。工具声明合同，业务填实现。

**正例 4b（common 调用 api 合同）：** `UserNameTranslationImpl` 住在 `wta-common-translation`（common 自己的实现），POM 依赖 `wta-api`，调用 `UserService`。这不是「业务填 SPI」。

**正例 4c（主机实现 api 端口）：** `AdminSsoIdentityService` 在 `org.namewta.web.sso`，`implements SsoIdentityService`，字段注入 `SysUserMapper`。这是组装层接线，不是「admin 拥有用户领域」。SSO 认人细节后课再讲。

**正例 5（主路径枢纽用法）：** 业务 Web CRUD 通常显式要 `wta-common-mybatis` + `wta-common-web` + `wta-common-security`（security 会带 satoken）。发统一通知时，业务注入 `org.namewta.notify.api.NotificationApplicationService`，不要在业务房间直接 new 渠道 SDK，也不要去调 `wta-common-notify` 的 `NotifyDispatcher` 当跨房间 API。机器调用协议走 `wta-common-openapi` 的 `@OpenApi`；凭据实现仍在 system。

**反例 1：** 在 `wta-common-core` 里 `import org.namewta.demo...` 或 POM 依赖 `wta-demo`。工具间开始懂演示业务，箭头反了。

**反例 2：** `wta-system` 的某个 ServiceImpl 直接注入 `TestDemoMapper`。后厨打通：demo 一改 SQL，系统模块跟着炸。这条禁的是**邻居业务**，不是主机：admin 注入 `SysUserMapper` 不落这条反例。

**反例 3：** 业务 POM 写 `<artifactId>wta-common</artifactId>`（父聚合）或 `<artifactId>wta-modules</artifactId>`。这两个 packaging 都是 `pom`，不是给你当 jar 用的。

**反例 4：** 因为看见旧组织名或别的产品名，就在新代码里改包名。启动类是 `NamewtaApplication`，公开包名合同是 `org.namewta`。

**反例 5：** 把可复用用户规则写进 `org.namewta.web.service`，再让 `wta-profile` 去依赖 `wta-admin`。admin 是主机，不是共享库。实现 `SsoIdentityService` 不等于把用户模块搬进 admin。

**反例 6：** 把 `ISysClientService`（`org.namewta.system.service`）当成 `wta-api` 合同，让 `wta-notify` 去依赖它。主机可以碰模块内部接口；邻居只能碰 `org.namewta.system.api.*`。

**边界：**

- `wta-api` 不是「把所有 VO 都倒进去的筐」。只有**跨业务真正需要**的合同才上去。模块内部的 BO/VO 留在模块里。
- common 不是业务垃圾桶。某条「只有档案才懂」的规则不属于 mybatis。
- admin 里的 `AuthController` / `SysLoginService` 是主机接线，后续切片课再讲；本课只要求：别把它们当成可被业务模块 import 的公共库，也别把「主机可深用 Mapper」推广成「全仓谁都可以碰 `SysUserMapper`」。
- `wta-common-doc` 的 SpringDoc 配置 ≠ `wta-api` 的 Java 合同 ≠ 浏览器看到的 HTTP/JSON。后两张合同是 L-005。
- 本课不教 `wta-extend` 的监控 UI、SnailJob Server 还是 SnailAi Server 怎么配。记住它们有自己的启动类就够了。
- 本课不背 common 子模块个数。要清单就列 `backend/wta-common/` 目录。

## 变式与迁移

- **变式 A（剖面）：** 你在 core 包里找不到 `/demo/demo/list`。先看是不是 `-Pbundle-core` 根本没插 `wta-demo`，不是「Controller 被删了」。
- **变式 B（三套通知）：** 两个业务都要发邮件。不要互相调用对方的 Service，也不要把 `wta-common-notify` 当成 `wta-api` 的 notify 包。走 `NotificationApplicationService`（api）。渠道适配器才是 `wta-common-notify`。控制面编排才是 `wta-notify`。本课不展开 Outbox。
- **变式 C（档案仍在 core）：** core 不是「只剩 system」，也不是「只剩 api + system + admin 直接 common」。person / enterprise 仍在 classpath。缺的是 job / ai / demo / workflow。
- **变式 D（Job 执行器）：** `wta-job` 进的是 **admin 进程**，作为执行器客户端。`wta-snailjob-server` 是 **另一个进程**。不要把「模块没进 bundle」和「Job 服务器没启动」当成同一件事。后者是 extend，本课不展开。
- **变式 E（OpenAPI 关掉）：** admin 直接依赖 `wta-common-openapi`。机器调用默认开启，可用 `OPENAPI_ENABLED=false` 关掉。关的是协议网关，不是把 `wta-api` 从依赖图里摘掉。
- **变式 F（传递 classpath）：** 看见 `AuthController` 使用 `LoginHelper`，不要先去 admin POM 找 `wta-common-satoken`。先问：是不是被插模块把 satoken 传进来了。登录方法本身后课再走。
- **迁移：** 接到「加一个后端功能」，先问三句：这是新业务房间、旧房间的新用例，还是一段可复用工具？再决定放 `wta-modules`、`wta-api` 还是某个 `wta-common-*`。只有跨房间需要的类型才进对应 api 分包（system / notify / profile / sso / third / workflow）。主机接线（含实现某个 api 端口）可以留在 admin，但可复用规则不要从模块搬进 `org.namewta.web`。新房间内部层次见 L-003。跨端字段见 L-005。需要新 common 子模块时，按最小能力声明，对照 `backend/wta-common/` 目录按需添加，不要按记忆中的个数一次性写进 POM。

## 常见误区

1. **「admin 很大，所以业务就写在 admin。」** admin 大是因为组装和主机接线，不是因为领域规则属于它。能实现 `SsoIdentityService` ≠ 拥有用户领域。
2. **「都在 `org.namewta` 下，互相 import 没关系。」** 包名前缀相同不等于依赖方向合法。看 POM 和「是不是对方的内部包」。
3. **「common 更公共，所以把业务也放进去更共享。」** 共享的是工具和 SPI，不是某条业务规则。
4. **「api 模块等于 OpenAPI 文档。」** `wta-api` 是 Java 跨模块合同；`wta-common-openapi` 是机器调用运行时；浏览器 HTTP/JSON 是另一张合同。
5. **「依赖了 `wta-common-bom` 就有全部工具。」** BOM 只锁版本。业务仍要按需声明子 artifact。`wta-profile-bom` 同样只锁档案子模块版本。
6. **「common 依赖了 `wta-api`，所以 common 已经依赖业务了。」** `wta-api` 不是业务模块。业务模块是 `wta-modules/*`。看错这一格就会把合法的 `LoginUser` 引用当成违规。
7. **「reactor 里有 demo，运行时就一定有 demo。」** 编译图和 fat jar 剖面是两件事。
8. **「`wta-extend` 也是模块，应该插进 admin。」** extend 是独立进程。硬插进去会把监控服务器和主站煮成一锅。
9. **「common 一共 N 个 jar。」** 个数来自过期摘要。以 `backend/wta-common/` 目录为准；本课主路径只有四件门口。
10. **「禁止 Mapper 是全仓禁令。」** 邻居禁、主机不禁。`wta-notify` 碰 `SysUserMapper` 违规；`AdminSsoIdentityService` 碰它是组装。
11. **「只有 satoken / mybatis 可以依赖 api。」** 至少还有 translation / log / push。名单以各 common 的 `pom.xml` 为准。
12. **「`UserNameTranslationImpl` 证明业务填了 common SPI。」** 它是 common 内实现在调用 api。真正「业务 implements common SPI」的样例是 `DictService`。

## 非评分暂停

用手指在空气中画四个 C4 盒子：admin、api、modules、common。然后只画允许的箭头。

如果画出了 common → demo，或者 notify → `SysUserMapper`，停下来。那条线就是本课要拆掉的电线。

如果画出了 satoken → `LoginUser`（在 `wta-api` 里），那条可以留着：它指向合同，不是指向 `wta-system` 源码。

如果画出了 admin → `SsoIdentityService` 实现，以及 admin → `SysUserMapper`，那两条也可以留着：主机接线，不是邻居互挖。

再对照一次：`bundle-full` 和 `bundle-core` 改变的是箭头方向，还是插座上插了几件电器。core 排除的是 job / ai / demo / workflow，不是档案。

## 总结、词汇表与下一步

- admin = 组装启动（可实现 api 端口、可持基础设施）；api = 跨业务对讲；modules = 业务；common = 工具 jar。四个都是本课 C4 容器。
- 箭头：组装者指向别人；业务指向 api 与所需 common；common 不指向 `wta-modules/*`。
- 启动类是 `NamewtaApplication`，包名是 `org.namewta`。
- `wta-api` 六包：system、notify、profile、sso、third、workflow。`system.api` ≠ `system.service`。
- 常驻业务：system / notify / sso / third。full 另加 job / ai / demo / workflow；档案 person / enterprise 在两个剖面都在。
- 主路径 common 门口：notify、mybatis、satoken、openapi。其余打开 `backend/wta-common/` 列举，不背个数。
- `bundle-*` 只改变 admin classpath 里有哪些业务模块。
- 三套通知：api 合同 / `wta-notify` 控制面 / `wta-common-notify` 渠道工具。
- `wta-extend` 旁置，本 Goal 不教内部。

下一步：L-003 走进业务房间内部，看为什么有的模块是五层，有的仍是 classic，以及为什么同一模块不能两种写法混用。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-001 | `README.md` | admin 只组装；跨模块走 api 或 common SPI；README 树未画出 extend | 「架构一览」段落后说明 | 2026-09-16 |
| S-004 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | 模块职责、依赖方向、bundle-full/core（core 保留 profile、排除 job/ai/demo/workflow）、extend 三个启动类；admin 负责组装、不承载可复用领域实现 | 「后端 Maven 模块」「依赖方向」 | 2026-09-16 |
| S-006 | `.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md` | ARCH-002：组装者组装；业务走 api 与最小 common；common 不反向依赖业务 | ARCH-002 | 2026-09-16 |
| S-010 | `backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java` | 启动入口类名与包名；`@SpringBootApplication` 无显式 `scanBasePackages` | `org.namewta.NamewtaApplication` | 2026-09-16 |
| WT-admin-pom | `backend/wta-admin/pom.xml` | 常驻依赖；`bundle-full` 默认；`bundle-core` 保留 person/enterprise、排除 job/ai/demo/workflow；admin 直接 common 不含 satoken/mybatis/encrypt | `<dependencies>`、`<profiles>` | 2026-09-16 |
| WT-api-pom | `backend/wta-api/pom.xml` | api 只依赖 common-core / common-json / spring-core | `<dependencies>` | 2026-09-16 |
| WT-api-pkgs | `backend/wta-api/src/main/java/org/namewta/{system,notify,profile,sso,third,workflow}/api` | 六个 Java 分包存在；`SsoIdentityService` 在 sso.api | 目录与接口文件 | 2026-09-16 |
| WT-modules | `backend/wta-modules/pom.xml` 及子模块 POM | 九个业务房间名单；profile 为聚合 POM + person/enterprise + bom；各模块依赖 api + 所需 common；job 不依赖 api | `<modules>` 与各 `<dependencies>` | 2026-09-16 |
| WT-common | `backend/wta-common/pom.xml` 及 notify/mybatis/satoken/openapi 子 POM | 父模块 packaging=pom；四件枢纽不依赖 `wta-modules/*`；notify 依赖 nacos 但不依赖 api；mybatis/satoken 可依赖 api 合同 | 各 `pom.xml`；库存用目录列举 | 2026-09-16 |
| WT-common-api | `wta-common-{satoken,mybatis,translation,log,push}/pom.xml` | 这五件 POM 显式依赖 `wta-api` | `<artifactId>wta-api</artifactId>` | 2026-09-16 |
| WT-extend | `backend/wta-extend/pom.xml` 与三个 `*Application` | extend 是独立可部署进程 | monitor / snailjob / snailai 启动类 | 2026-09-16 |
| WT-cross | `NotifyRecipientDirectoryService`、`NotifyNoticePublisherService`、`SysUserServiceImpl` | 通知经 `UserService` 解析用户；实现位于 system | `wta-notify` / `wta-system` 源码 | 2026-09-16 |
| WT-host-spi | `backend/wta-admin/src/main/java/org/namewta/web/sso/AdminSsoIdentityService.java` | 主机实现 `SsoIdentityService` 并注入 `SysUserMapper`；全仓仅此一处实现 | 类声明与字段 | 2026-09-16 |
| WT-spi-split | `DictService` / `SysDictTypeServiceImpl`；`UserNameTranslationImpl` | 业务填 common SPI vs common 调用 api | `wta-common-core` / `wta-system` / `wta-common-translation` | 2026-09-16 |
| WT-auth-import | `backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java` | 主机注入 `ISysClientService`（非 api）与 `NotificationApplicationService`；使用传递进来的 `LoginHelper` / `@ApiEncrypt` | import 与字段 | 2026-09-16 |
