---
lesson_id: L-003
objective_ids: [OBJ-03]
estimated_minutes: 40
time_budget:
  - segment: orientation-and-map
    minutes: 6
  - segment: deep-explanation
    minutes: 15
  - segment: visuals-and-worked-examples
    minutes: 11
  - segment: pause
    minutes: 4
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-005, S-006, S-007, S-012, S-013, S-014, S-015, S-016, S-017, S-018]
claimed_cells: [C:layered-classic]
---

# Lesson 003：两种后端写法，为什么不能混

## 学完你能做什么

你能打开**唯一**的模块模式登记表，背出当前两列，并且能把「表上的名字」和「磁盘上的房间」分开：

- 表上的 layered 只有三行：`profile`、`notify`、`sso`。
- 表上的 classic 只有五行：`system`、`workflow`、`job`、`demo`、`ai`。
- 未登记的**新**业务模块默认五层；新模块不能把 classic 当抄近路。
- 同一模块禁止混用两种模式。
- `wta-profile` 这一行是聚合 POM；能跑五层检查的房间是 `wta-profile-person` 和 `wta-profile-enterprise`。
- `wta-third` 磁盘上已有 UseCase/DAO，但 **`03-backend-module-modes.md` 没有这一行**。形状 ≠ 已发表的登记。
- classic 不是永远等于 Controller→ServiceImpl→Mapper：`wta-job` 和 `wta-ai` 仍是 classic 行，家具不一样。
- `wta-admin` / `wta-api` / `wta-common-*` 不在这张业务表上。

## 先把宏观地图放在桌上

L-002 把业务都放进 `wta-modules/*` 这层楼。进到一个具体房间，家具摆法有两种，而且**以登记表为唯一名单**，不是「看哪个目录比较顺手」，不是「邻居怎么写我就怎么写」，也不是「脚本退出码 0 才算登记」。

权威入口只有一份：`.agents/skills/engineering-standards/references/project/03-backend-module-modes.md`。Skill 摘要、模块地图、模块 `AGENTS.md`、demo 示例，都不能另写一份模式名单。

```text
登记表说 layered 的调用链（新写法 / Target）：
  门卫 Controller / Listener / API Adapter（adapter/api）
       → 管家 UseCase（可以是接口 + usecase/impl）
            → 厨师 Service（规则，不碰 Mapper）
                 → 仓库管理员 DAO
                      → Mapper → XML

登记表说 classic 的**典型**调用链（存量兼容，不是每一间 classic 都长这样）：
  门卫 Controller
       → 厨师+仓库 Service / ServiceImpl（可以持有 Mapper）
            → Mapper → XML
```

**类比失效处：** 「管家 / 厨师 / 仓库」只帮你记谁不能进仓库。失效点有五：

1. UseCase 不是「再写一遍 Service 的别名」；`*UseCaseImpl` 也不是 classic 的 `ServiceImpl`。
2. DAO 不是「Mapper 换个名字」。
3. classic 的 ServiceImpl 不是「新模块可以偷懒的样板」。
4. 登记名 `wta-profile` 不是一间已经摆好五层家具的房间，它是楼号（聚合 POM）。
5. 登记 classic 不是「必须能画出 Mapper」：`wta-job` 是定时器房间，`wta-ai` 是对外客户端，仍然写在 classic 列。

每一层少一样禁止事项。把 `service/impl` 留在 layered 房间，等于请厨师同时管仓库钥匙。

不在这张**业务五层表**上的房间，不要硬套五层目录：`wta-admin`（插座板）、`wta-api`（对讲机）、`wta-common-*`（工具间）、`wta-extend`（另一些可部署进程）、以及其它聚合 POM。它们按各自职责走，不是第三种业务模式。若要把其中某个改造成业务实现，必须先补表。

## 核心概念与机制

### 直觉讲解

旧房子（classic）厨房和仓库门常常是通的：`ServiceImpl` 可以直接抱着 Mapper 写 SQL 条件。房子已经住人了（system、demo、workflow，以及家具不同的 job、ai），为了不把全楼拆光，登记表允许它们暂时保持**各自既有**摆法。但**不允许为了新功能把通道再开宽**，也不允许新房子先按旧房子盖、「以后再拆」。

新房子（layered：notify、sso，以及 profile 楼里可住人的 person / enterprise）规定：门口只认识管家（UseCase）；管家只吩咐厨师（Service）；厨师只叫自己的仓库管理员（DAO）；只有 DAO 能碰 Mapper 和 MyBatis 类型。Listener、匿名回调、`adapter/api` 里的 API Adapter 也是门口，同样不能跳过管家去抓仓库钥匙。别的 `adapter/*`（例如 notify 的 `adapter/worker`）不是门口。

小孩子会问：为什么新房子这么麻烦？因为旧房子里，炒菜、记账、进货、打电话给隔壁，最后全堆在一个 `ServiceImpl` 里。一改 SQL，事务、权限、外部调用一起抖。五层是把三件事拆开：

1. **编排和事务边界**在 UseCase（什么时候开事务、先改状态还是先通知）。
2. **业务规则**在 Service（能不能编辑、生命周期、跨模块 Port）。
3. **怎么碰库**在 DAO（Wrapper、加锁 SQL、分页）。

脚本 `validate-module-mode.mjs` 会按目录和 import 检查 **layered**。`--mode classic` **几乎不看调用链**：它主要确认 `src/main/java` 和 `src/main/resources` 在不在。混用 = 两把仓库钥匙，检查脚本和人脑都会漏。

工作树里还有一间已经摆好五层家具、但门牌没写进登记表的房间：`wta-third`。它不是第三种模式。本课后面单独讲「形状 vs 未发表的登记」。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 五层模块 | layered module | 调用链固定为 Controller/Listener/API Adapter → UseCase → Service → DAO → Mapper → Mapper XML |
| 存量模块 | classic module | 登记表保护的存量兼容窗口；**典型**链是 Controller → Service → ServiceImpl → Mapper → Mapper XML，但 job / ai 的既有形状不是这条 CRUD 链 |
| 登记表 | module-mode registry | `03-backend-module-modes.md`：唯一决定某模块哪种模式的入口；2026-09-16 只有 3 条 layered + 5 条 classic |
| 聚合 POM | aggregator POM | 只有 `<modules>`、没有 `src/main/java` 的 Maven 父工程。`wta-profile` 是表上的 layered 行，本身不是可跑五层的房间 |
| 入口适配器 | entry adapter | Controller、Listener、以及路径在 `adapter/api`（或 `api/adapter`）的 API Adapter；只能依赖 UseCase，不得直连 DAO/Mapper/Service |
| 用例层 | UseCase | 入口之后的编排；可开 `@DSTransactional`；允许 `usecase` 接口 + `usecase/impl`；不得直连 DAO、Mapper、Gateway、Store、Provider 或外部 API |
| 领域服务 | Service（layered 含义） | 业务规则；只调本模块 DAO、已声明的外部 Port、无状态 policy/codec/converter；不得导入 Mapper、`IService`、`ServiceImpl`、`PageQuery`、MyBatis，不得互调另一个 Service |
| 数据访问对象 | DAO | 业务里唯一持有 Mapper / MyBatis 类型的层；不得调其他 DAO、UseCase、Service、Gateway |
| 读模型 | Row / Projection | `domain/model/read` 下给 Mapper 用的读结果；**不是第六层**，也不得当 Controller 返回值 |
| 棘轮 | Ratchet | 新代码按 Target；classic 不借机全仓改造成 layered，也不得借新功能继续扩大越层 |
| 模式验证 | module-mode validation | `validate-module-mode.mjs <module-path> --mode layered` 查五层目录和 import；`--mode classic` 几乎只确认 java/resources 目录存在 |

**Layered** 在本仓库不是「多几个文件夹好看」，而是 import 方向和验证脚本会检查的合同。目录叫 `usecase` 但 Service 仍 `import XxxMapper`，灵魂仍是 classic。

### 机制/因果链

动手前先走这根因果链，不要先复制邻居的 `service/impl`。

1. **查表，不要猜。** 打开登记表。当前业务五层登记（对照 2026-09-16 工作树 / S-005）**只有**这两列，没有第三列：
   - layered：`wta-profile`、`wta-notify`、`wta-sso`
   - classic：`wta-system`、`wta-workflow`、`wta-job`、`wta-demo`、`wta-ai`
2. **把登记名解析成可跑的 Maven 路径。** `wta-profile` 的 POM 是 `<packaging>pom</packaging>`，子模块是 `wta-profile-bom`、`wta-profile-person`、`wta-profile-enterprise`。表上的「试点」写的是 person/enterprise 使用五层。对父路径跑脚本必然失败（没有 `src/main/java`），**失败不证明 profile 不是 layered**，只证明脚本要对准房间而不是楼号。
3. **未登记的新业务模块默认 layered。** 没有证据不得以 classic 当临时默认。新模块不能选 classic 当捷径。若确有存量兼容理由要留 classic，必须登记模块、owner、例外理由、补偿验证和删除条件。
4. **工作树形状 ≠ 已发表的登记。** `wta-third` 磁盘上已有 `controller/admin`、`usecase/impl`、`service`、`dao`、`mapper`；模块 `AGENTS.md` 自述五层；登记表 **没有** `wta-third` 这一行。不要假装 Skill 表已经列出 third。写代码按「未登记默认 layered」；同时必须回头补表。不得发明第三种模式，也不得拿「表上没有」当 classic 借口。
5. **若模块已是 classic：** 保持**该模块既有**结构，按 Ratchet 收紧触及的文件。不要在这个模块里再加一套 UseCase/DAO 当「新旧并存的日常写法」。`wta-job` 若长出**独立新业务能力**，应另行登记为 layered 模块，而不是把 classic 房间再开一间阁楼。
6. **classic 列不是一种家具。** `wta-demo` / `wta-system` / `wta-workflow` 接近课图上的 ServiceImpl→Mapper。`wta-job` 是 `snailjob/*` 执行器（例如 `TestClassJobExecutor`），没有业务 Controller、没有 Mapper、没有 `src/main/resources`。`wta-ai` 是 `SnailAiController` + `OpenApiUserClient`，没有 ServiceImpl、没有 Mapper。它们**仍然是登记过的 classic**。不要在这两间房里寻找课图上的 Mapper 链，也不要把「没有 Mapper」理解成「可以不登记就非五层」。
7. **若模块已是 layered，或这是未登记的新业务模块：** 默认五层。入口只注入 UseCase。UseCase 只编排 Service。Service 只通过 DAO 碰库。DAO 才持有 Mapper。辅助包 `support` / `policy` / `codec` / `converter` / listener / event 可以存在，但**不能绕过 UseCase，也不能自己持有数据库或远程基础设施**。`adapter/api` 是入口；其它 `adapter/*` 不是入口，也不得反向依赖 UseCase/Service/DAO。
8. **跨模块仍然走 `wta-api`（L-002）。** layered 的 Service 可以依赖公开 API，例如通知 Service 用 `org.namewta.system.api.UserService`，那是 Port，不是去拿 system 的 Mapper。
9. **classic 改 layered 不是改个目录名。** 必须先映射旧路径、公开合同、调用方、SQL、测试和回滚。「仅移动」和「行为变化」分开验证。本课不授权把 `wta-system` 拆了。
10. **模式变化是架构合同变化。** 要同步更新模块 `AGENTS.md`、登记表、模块地图、对应 Skill 和验证脚本。

登记列、源码形状、脚本退出码三者打架时：**先信登记表**（它是唯一名单），再用工作树形状决定怎么写，最后把脚本当快速反馈而不是判决书。脚本 `classic` 分支不会检查「突然长出五层目录」——那要人看。脚本 `forbiddenByLayer` 仍写 `org.dromara.system.api` 等旧前缀；入口若误 import `org.namewta.system.api.UserService`，dromara 那条规则可能漏检。

每一层的禁止事项叠在一起，才叫同一模式：

```text
layered 入口     不得 import dao / mapper / service
layered UseCase  不得 import dao / mapper / gateway / store / provider
layered Service  不得 import mapper / 另一个 Service / IService / ServiceImpl / PageQuery / MyBatis
layered DAO      不得 import usecase / service / 其他 dao / 跨模块 API
classic ServiceImpl  允许持有本模块 Mapper；不得借新功能继续扩大越层依赖
classic 入口     不得再抱 Mapper（那是扩大越层，不是存量允许的形状）
```

因果：登记表 → 解析可跑路径 → 选择该模块既有调用链 → 每一层的禁止事项 → 脚本给人快速反馈。同一模块混用，脚本看到两套目录，人下次不知道改哪条链。

### 图、表或文本图

**图题 / caption：** 登记名、可跑路径、源码形状、2026-09-16 脚本退出码对照。alt：左列登记表唯一名单；中列 Maven 路径与家具；右列脚本不是判决书；下方两条 CRUD 链只覆盖接近课图的模块。

```text
唯一名单（03-backend-module-modes.md，2026-09-16）
  layered 三行：  wta-profile | wta-notify | wta-sso
  classic 五行：  wta-system | wta-workflow | wta-job | wta-demo | wta-ai
  未登记的新业务：默认 layered（规则，不是表上多出来的第四行 layered）

登记名 / 可跑路径 / 形状 / 脚本（工作目录 /srv/WTA-plus，S-014 实跑）：
  wta-profile              聚合 POM，无 src          表上 layered 行
                           --mode layered → 退出码 1（没有五层目录；对的是楼号）
  wta-profile-person       子模块，168 个 Java       五层家具；表上没有单独一行
                           --mode layered → 退出码 0
  wta-profile-enterprise   子模块                    同样是可跑的五层房间
  wta-notify / wta-sso     表上 layered              五层家具
                           --mode layered → 退出码 1（Javadoc 误报 / support 依赖 Spring）
                           退出码 1 ≠ 「所以它们不是 layered」
  wta-third                表上没有这一行            磁盘已有 UseCase/DAO 五层
                           --mode layered → 退出码 0
                           形状 ≠ 已发表登记；不要假装表已经列出它
  wta-demo                 表上 classic              ServiceImpl 持 Mapper
                           --mode classic → 退出码 0（只确认 java/resources 存在）
  wta-job                  表上 classic              snailjob 执行器；无 resources
                           --mode classic → 退出码 1（缺 src/main/resources）
  wta-ai                   表上 classic              Controller + OpenApiUserClient
                           --mode classic → 退出码 1（缺 src/main/resources）

不在业务五层表上：
  wta-admin / wta-api / wta-common-* / wta-extend / 其它聚合 POM
```

```text
HTTP / Listener / adapter/api
   |
   v
Controller ----+---- UseCase ---- Service ---- DAO ---- Mapper ---- XML     layered
               |     （或 UseCaseImpl）
               |
               +---- Service / ServiceImpl -------------- Mapper ---- XML     classic 的 CRUD 形态
                                                                            （demo / system / workflow）

job：  snailjob 执行器（无这条 Mapper 链，仍是 classic 行）
ai：   SnailAiController → OpenApiUserClient（无 ServiceImpl/Mapper，仍是 classic 行）
```

**真实 layered 列表链（公告，S-012）：**

```text
GET /notify/notice/list  +  notify:notice:list
  NotifyNoticeController.list(query, pageQuery)
    唯一字段：NotifyNoticeUseCase noticeUseCase
    把 pageQuery 拆成 pageNum / pageSize 再往下传
        → NotifyNoticeUseCase.list
              字段：NotifyNoticeService + NotifyNoticePublisherService
              列表只调 noticeService.page（不碰 DAO）
                  → NotifyNoticeService.page
                        字段：NotifyPersistenceDao + UserService(wta-api)
                        dao.page 之后 toVo
                            → NotifyPersistenceDao.page
                                  字段：NotifyNoticeMapper + NotifyNoticeSnapshotMapper
                                  LambdaQueryWrapper + noticeMapper.selectPage
                                      → mapper/notify/NotifyNoticeMapper.xml
```

**真实 classic 列表链（测试单表，S-013）：**

```text
GET /demo/demo/list  +  demo:demo:list
  TestDemoController.list(bo, pageQuery)
    唯一业务字段：ITestDemoService testDemoService
        → TestDemoServiceImpl.queryPageList
              字段：TestDemoMapper demoMapper
              buildQueryWrapper(bo) 后 demoMapper.selectVoPage(...)
                  → TestDemoMapper / mapper/demo/TestDemoMapper.xml
```

**文字等价物：** 登记表是唯一名单：layered 只有 profile、notify、sso 三行，classic 只有 system、workflow、job、demo、ai 五行。未登记的新业务模块按规则默认走五层，这不是表上多出来的一行。`wta-profile` 这一行是聚合 POM（楼号）；真正能跑五层、有 `controller/usecase/service/dao/mapper` 的房间是 `wta-profile-person` 和 `wta-profile-enterprise`。对父路径跑验证脚本失败，只说明对准了楼号。`wta-third` 在磁盘上已经长成五层（UseCase/DAO 都在），但登记表没有这一行：形状和未发表的登记冲突时，写代码按默认 layered，同时必须补表，不得假装表已经登记 third，不得发明第三种模式，也不得拿缺行当 classic 借口。右列五个 classic 模块里，demo / system / workflow 才接近「控制器交给 ServiceImpl、实现类直接抱 Mapper」；job 是定时执行器，ai 是对外客户端，家具不同，登记仍是 classic。两条 CRUD 链在 Controller 之后分叉，不能在同一模块里各写一半。`wta-admin`、`wta-api`、`wta-common-*`、`wta-extend` 不在这张业务五层登记表里。公告列表这条链里，Controller 可以看见 `PageQuery`（HTTP 绑定），但 UseCase 只收两个整数，Service 不 import Mapper；demo 列表这条链里，`PageQuery` 一直传到 `TestDemoServiceImpl`，由它直接抱着 `TestDemoMapper` 分页。脚本退出码不是模式判决书：notify/sso 作为 layered 目标，本课用公告链证明 import 方向，不以「整模块脚本现在为 0」当证据。

**图的边界：** 本图不教通知 Outbox、SSO 授权码、第三方 HTTP 适配器内部、job 执行器内部、ai starter 细节。Listener / `adapter/api` 与 Controller 同属入口，也不得跳过 UseCase 去抓 DAO。其它 `adapter/*` 不是入口。`domain/vo` 是 HTTP 输出；Mapper 读模型不是第六层。

## 正例、反例与边界

**正例 1（layered 入口形状）。** `NotifyNoticeController` 只有一个业务字段 `NotifyNoticeUseCase noticeUseCase`。`list` 把分页参数交给 `noticeUseCase.list`。匿名回调 `ProviderCallbackController` 同样只注入 `ProviderCallbackUseCase`。这是入口只认识管家。

**正例 2（layered 编排与持久化分离）。** `NotifyNoticeUseCase` 的字段是 `NotifyNoticeService` 和 `NotifyNoticePublisherService`，没有 Mapper。只读 `list` / `get` 直接转发 Service；写操作 `save` / `publish` / `retract` / `remove` 带 `@DSTransactional`。`publish` 先 `noticeService.publish(id)`，只有返回非 null 才 `publisher.publish(notice)`。两个 Service **互不调用**；管家负责先后顺序。`NotifyNoticeService` 的持久化字段只有 `NotifyPersistenceDao dao`，跨模块用户目录走 `UserService`，不 import `NotifyNoticeMapper`。`NotifyPersistenceDao` 才持有 `NotifyNoticeMapper` 和 `NotifyNoticeSnapshotMapper`；加锁语句 `selectByIdForUpdate` 写在 `NotifyNoticeMapper.xml`。

**正例 3（classic 允许持有 Mapper）。** `TestDemoController` 注入 `ITestDemoService`。`TestDemoServiceImpl` 实现该接口，字段是 `private final TestDemoMapper demoMapper`。`queryPageList` 在 ServiceImpl 里 `buildQueryWrapper`，再 `demoMapper.selectVoPage`。这是登记过的 classic **存量形状**，允许。它**不是**新模块样板。

**正例 4（新模块默认五层）。** 你要新增 `wta-modules/wta-billing`。没有登记为 classic 的例外，就按 layered 建 `controller` / `usecase` / `service` / `dao` / `mapper`，并先补登记表。变更说明还要同时给出：唯一业务 owner、artifactId、Java package、数据库 owner；layered 目录主轴和入口访问面；UseCase / Service / DAO / Mapper 的依赖方向和完整 SQL 验证路径；用到的 `wta-api`、`wta-common-*` 和框架入口；测试、bundle、配置、50/60 MySQL 基座和前端合同影响。缺这些证据时，不得写「先 classic 以后再升」。新模块不能把 classic 当抄近路。

**正例 5（API Adapter 才是门口，且只认管家）。** `PersonIdentityLookupServiceImpl` 放在 `wta-profile-person` 的 `adapter/api`，实现 `wta-api` 的 `PersonIdentityLookupService`，唯一字段是 `PersonProfileApiUseCase`。这是脚本认的入口适配器。验证 person 房间用：`node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-profile/wta-profile-person --mode layered`，不要拿父路径 `wta-profile` 的失败当证据。

**正例 6（UseCaseImpl 不是 ServiceImpl）。** `SsoOAuthUseCaseImpl` 实现 `SsoOAuthUseCase`，字段只有 `SsoAuthorizationService`，没有 Mapper。`wta-sso` / `wta-profile-person` / `wta-third` 常见「usecase 接口 + usecase/impl」。layered **允许** `usecase/impl`。禁止的是 `service/impl` 持有 Mapper。

**反例 1（layered 房间塞 classic 灵魂）。** 在 `wta-notify` 里新建 `service/impl` 并直接注入 Mapper，「先跑起来再分层」。这是同一模块混模式。`validate-module-mode.mjs` 会在 Service 层看到 Mapper import。

**反例 2（classic 房间再开一条五层）。** 在 `wta-demo` 里加 UseCase + DAO，但 Controller 仍有另一半方法直接打 `ITestDemoService` / Mapper。两条链并存，下次谁都不知道该改哪条。classic 保护的是存量结构，不是「可以再新开一条 layered 日常写法」。同一模块一种模式。

**反例 3（新模块抄 demo）。** 因为 `TestDemoServiceImpl` 好复制，新模块先抄它，登记表却还没写例外。登记表写明：demo 不得当 layered 的反面教材。新模块不能把 classic 当抄近路。

**反例 4（按方法挑选模式）。** 同一个 `wta-sso` 里，令牌发放走 UseCase，会话查询却让 Controller 注入 Mapper。同一模块一种模式。

**反例 5（入口再抱 Mapper，不是存量允许）。** `TestBatchController` 注释写「为了便于测试 直接引入mapper」，字段 `TestDemoMapper testDemoMapper`。存量允许的是 **ServiceImpl → 本模块 Mapper**。Controller 直持 Mapper 已经是 classic 棘轮拧松的越层，不是测试方便许可证，更不是新模块模板。

**反例 6（把任意 adapter 当成门口，或把缺登记当成第三种模式）。** `NotifyOutboxWorker` 在 `adapter/worker`，注入的是 Port，不是 UseCase；脚本不把它当 entry。`wta-third` 的 `ThirdProviderController` 只注入 `ThirdProviderUseCase`，`ThirdProviderUseCaseImpl` 调 Service，`ThirdProviderDao` 持 Mapper——形状已经是五层，但表上没有 third 行。不要说「表已经登记 third」，也不要说「没登记就可以按 classic 写」。

**边界：**

- common 模块不按业务五层硬套目录。把 Redis 工具改成 UseCase 是套错衣服。`wta-admin` / `wta-api` / `wta-common-*` / `wta-extend` 也不在这张表上；把它们改造成业务实现前，必须先补登记表和迁移说明。
- classic 保护的是存量结构，不是「可以再新开一个 ServiceImpl 去碰别人的表」，也不是「job / ai 必须补一套 Mapper 才算 classic」。
- Row / Projection 给 Mapper 读结果用，不是可以让 Controller 返回的第六层。
- 本课用公告列表链证明五层 import 方向，不以 2026-09-16 notify/sso 整模块脚本退出码 1 否定它们的登记。Outbox / `service/runtime`、SSO Cookie、job 执行器内部、ai starter 不在本课展开。
- 脚本 `--mode classic` 不检查「有没有突然长出强制五层目录」；那句话是给人看的验证步骤，不是脚本行为。

## 变式与迁移

- **变式 A：** 改 `wta-system` 用户列表。保持 classic 形状，按 Ratchet 收紧触及的文件，不借机全模块搬家。
- **变式 B：** 改 `wta-sso` 令牌发放。保持五层；入口继续只注入 UseCase；只经 `wta-api` 读用户与 Client。看到 `SsoOAuthUseCaseImpl` 不要改成 `ServiceImpl` 去持 Mapper。
- **变式 C：** 在已登记 layered 模块加新 HTTP。新 Controller 方法仍然只调已有或新建 UseCase，不「图快」在 Controller 里 new 一个 Mapper。
- **变式 D：** `wta-job` 需要一块独立新业务。不要在 classic 模块里再堆一套越层依赖，也不要先抄 demo 的 ServiceImpl；按登记条件另开 layered 模块并补表。
- **变式 E：** 改 `wta-third` 的 Provider。按磁盘上已有的五层写（Controller → UseCase → Service → DAO → Mapper），同时把缺行当作当前表缺陷：变更说明里补登记，不要口头改写 Skill 表。
- **变式 F：** 改个人资料。对准 `wta-profile-person`，不要在聚合 POM 目录下新建 Java。跨模块查询走 `adapter/api` 的 `PersonIdentityLookupServiceImpl`，不要让别的模块去 import person 的 DAO。
- **迁移 classic → layered：** 先列旧 URL、权限、Mapper XML、调用方、测试和回滚。搬家和改行为分开验证。没有这份地图，不准说「只是重构」。本课不授权无关全仓改造。
- **与 L-006 的衔接：** 同一条 GET，在 demo 是 Controller→ServiceImpl→Mapper；在 notify 是 Controller→UseCase→Service→DAO→Mapper。口述时先报模块模式，再报层名。job / ai 不要硬套这两条 CRUD 口述。

## 常见误区

1. **「多几个包就是 layered。」** 若 Service 仍 import Mapper，目录再漂亮也是 classic 灵魂。
2. **「UseCase 可以顺便查库，少一层更快。」** 更快的是把钥匙再塞回厨师口袋，检查项会失效。
3. **「未登记就先 classic，以后再升。」** 登记表：没有证据不得以 classic 当临时默认。新模块不能把 classic 当抄近路。
4. **「两种模式可以在同一模块里按方法挑选。」** 同一模块一种模式。混用会让验证脚本和人同时迷路。
5. **「demo / system 能跑，所以新功能照抄 ServiceImpl。」** 那是存量兼容窗口，不是 Target。对照样例前先查表。
6. **「先混着写，算渐进迁移。」** 没有旧路径映射、测试和回滚计划的混写，只是两把钥匙。真正的迁移是一次有地图的搬家，不是日常两种写法并存。
7. **「表上写了 wta-profile，所以对父路径跑脚本就能证明五层。」** 父工程是聚合 POM。房间是 person / enterprise。
8. **「wta-third 已经有 UseCase，所以登记表肯定有它。」** 没有。2026-09-16 的 Skill 表未列 third。形状不能冒充已发表登记。
9. **「job / ai 没有 Mapper，所以它们其实不是 classic / 可以不登记。」** 它们是登记过的 classic，只是家具不是 CRUD 链。
10. **「UseCaseImpl 看起来像 ServiceImpl。」** 管家的实现类仍不得持有 Mapper。
11. **「TestBatchController 写了便于测试，所以入口持 Mapper 合法。」** 那是棘轮反例，不是模板。
12. **「脚本退出码 0 才算 layered。」** 先信登记表。脚本是快速反馈，classic 分支几乎不查调用链。

## 非评分暂停

打开登记表（或回想本课那两列：layered 三行、classic 五行）。随便点一个你最近改过的后端文件，沿 import 往下走一层：它落在哪一列？它的 Maven 路径是登记名本身，还是登记名下面的子模块？如果你准备新建一个类去持有 Mapper，它在 layered 列合法吗？如果这是 `wta-third`，你能不能同时说出「磁盘已是五层」和「表上还没有这一行」？如果这是 `wta-job` 或 `wta-ai`，你还在找 ServiceImpl 吗？如果这是一个还没出现在表上的新 `wta-modules/*`，默认应该长成哪条链？

不要在本课给自己打分。只要能感到「先查表，再核路径，再看形状」这根筋，就够进入下一课。

## 总结、词汇表与下一步

- 查表，不要猜：唯一名单是 layered = profile / notify / sso，classic = system / workflow / job / demo / ai。
- 未登记的新业务默认五层；同一模块禁止混用；新模块不能选 classic 当捷径。
- `wta-profile` 是聚合 POM；可跑的五层房间是 person 与 enterprise。
- `wta-third` 磁盘有 UseCase/DAO，登记表没有这一行：形状 vs 未发表登记，补表，不假装已登记。
- classic 是兼容窗口，不是新功能的样板，也不是统一的 CRUD 家具。job 执行器、ai 客户端仍是 classic 行。
- `wta-admin` / `wta-api` / `wta-common-*` 不在这张业务表上。
- 五层里只有 DAO 碰 Mapper；UseCase 编排，Service 管规则，入口只认 UseCase。`usecase/impl` 允许；入口抱 Mapper 不允许。
- 词汇：layered、classic、registry、aggregator POM、UseCase、UseCaseImpl、DAO、Ratchet、entry adapter、`adapter/api`。

下一步：L-004 离开 Java 房间，看前端为什么不能再复制一份 `src/api`。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | 唯一登记入口；layered 三行 profile/notify/sso；classic 五行 system/workflow/job/demo/ai；未登记默认 layered；新模块不得以 classic 为临时默认；禁止混用与扩大 classic；admin/api/extend/聚合 POM/common 不在业务五层表；profile 行说明 person/enterprise 使用五层 | 全文登记表 | 2026-09-16 |
| S-006 | `.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md` | ARCH-002A 五层调用链、各层禁止事项、Listener/API Adapter、Row 不是第六层 | ARCH-002A | 2026-09-16 |
| S-007 | `.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md` | DEC-009 新模块默认五层；DEC-002 Ratchet；DEC-003 目录主轴 | DEC-009、DEC-002、DEC-003 | 2026-09-16 |
| S-004 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | `wta-profile` 为无源码聚合 POM；person/enterprise 才有 `src/main/java`；`wta-third` 在模块地图中是独立业务模块；demo 为 classic CRUD 样例 | 模块表与实现基线 | 2026-09-16 |
| S-012 | 工作树 `backend/wta-modules/wta-notify`：`NotifyNoticeController`、`NotifyNoticeUseCase`、`NotifyNoticeService`、`NotifyPersistenceDao`、`NotifyNoticeMapper`、`mapper/notify/NotifyNoticeMapper.xml`；`adapter/worker/NotifyOutboxWorker` | layered 公告列表/发布的真实调用链与字段；worker 不是入口适配器 | 公告入口到 XML | 2026-09-16 |
| S-013 | 工作树 `backend/wta-modules/wta-demo`：`TestDemoController`、`ITestDemoService`、`TestDemoServiceImpl`、`TestDemoMapper` | classic ServiceImpl 持有 Mapper 的真实调用链 | 测试单表列表 | 2026-09-16 |
| S-014 | `.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs` | layered 目录存在性与按层禁止 import；classic 分支只检查 java/resources；`forbiddenByLayer` 仍含 `org.dromara.*`；`adapter/api` 才当 entry | 脚本 `classic` 分支 / `forbiddenByLayer` / `--mode` | 2026-09-16 |
| S-015 | 工作树 `backend/wta-modules/wta-third`：`ThirdProviderController`、`ThirdProviderUseCase` / `ThirdProviderUseCaseImpl`、`ThirdProviderDao`、`wta-third/AGENTS.md` | 磁盘已有五层；登记表无 third 行 | Provider 管理链与模块自述 | 2026-09-16 |
| S-016 | 工作树 `backend/wta-modules/wta-job/src/main/java/org/namewta/job/snailjob/TestClassJobExecutor.java`；`backend/wta-modules/wta-ai/.../SnailAiController.java` | job 为执行器、无 Controller/Mapper/resources；ai 为 Controller + `OpenApiUserClient`，无 ServiceImpl/Mapper；二者仍登记 classic | job/ai 既有形状 | 2026-09-16 |
| S-017 | 工作树 `backend/wta-modules/wta-demo/.../TestBatchController.java` | Controller 直持 `TestDemoMapper`；注释写便于测试；棘轮反例而非新模块模板 | 批量测试入口 | 2026-09-16 |
| S-018 | 工作树 `wta-profile-person/.../adapter/api/PersonIdentityLookupServiceImpl.java`；`wta-sso/.../usecase/impl/SsoOAuthUseCaseImpl.java`；`wta-profile/pom.xml` | API Adapter 只注入 UseCase；UseCaseImpl 不是 ServiceImpl；profile 父 POM 为聚合 | person 入口适配器 / SSO 管家实现 / 聚合 POM | 2026-09-16 |
