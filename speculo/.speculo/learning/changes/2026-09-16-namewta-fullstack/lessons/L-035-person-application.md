---
lesson_id: L-035
objective_ids: [OBJ-35]
claimed_cells:
  - A:PersonApplicationController.current, submit
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: layered-windows
    minutes: 8
  - segment: current-submit-chain
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L035-01, S-L035-02, S-L035-03, S-L035-04, S-L035-05, S-L035-06, S-L035-07, S-L035-08, S-L035-09, S-L035-10]
---

# Lesson 035：宏观上看，自己交的申请条——`PersonApplicationController.current` / `submit`

## 学完你能做什么

打开 `wta-profile-person` 里**一扇自助窗**，你能**口述** OBJ-35 点名的两枪，并且把分层链说完：

> 已登录的人看自己那张还没办完的实名申请，用 `GET /profile/person/application`（Java 名 `current`）。要把草稿变成「等老师批」的复印件，用 `POST /profile/person/application/submit`（Java 名 `submit`）。窗口柜员只认登录口袋里的 `userId`，body 里**没有**申请人字段。模块是 **layered**：`Controller / Listener → UseCase → Service → DAO → Mapper → Mapper XML`。`current` 找不到进行中申请就 `R.ok(null)`，不是 404。`submit` 先锁草稿、校验完整、冻材料、写不可变快照、把状态改成 `WAITING`，再喊核身供应商和工作流；工作流起不来，**整段事务回滚**，桌上还是草稿。

本课认矩阵 **(a)** 一格，符号以磁盘为准：

1. **`A:PersonApplicationController.current, submit`**（`@RequestMapping("/profile/person/application")`，包 `controller.self`）：`current` 是无参 GET；`submit` 是 `POST /submit`，body 只有 `PersonApplicationSubmitBo.expectedVersion`。同一扇门上还有邻居 `save`（`POST` 根路径）。矩阵格子只写了 current/submit，课内仍要把 `save` 说清楚——没有草稿就没有提交。

2026-09-16 工作树先钉死**分层和门牌**（口试先数层，再数方法）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `PersonApplicationController` 直接调 `PersonApplicationService` | **禁止。** 架构测试锁死 Controller 只 import `usecase`，不 import `service` |
| 字段名叫 `service` 所以是 Service | **类型是 `PersonApplicationUseCase`。** 名字骗人，类型才算数 |
| 生产 `PersonApplicationServiceImpl` | **没有。** `src/main` 的类就叫 `PersonApplicationService`。`PersonApplicationServiceImpl` 只在 `src/test`，注释写明测试兼容适配器 |
| `IPersonApplicationService` 是生产接线 | **半对。** 接口存在；生产类 **不** `implements` 它。UseCase 注入的是具体类 `PersonApplicationService` |
| `current` 空申请 → 404 | **不是。** `orElse(null)` 再 `R.ok(null)`，HTTP 仍 200、业务码仍成功 |
| `submit` 失败 → HTTP 4xx | **不是。** `PersonApplicationExceptionHandler` 返回 `R.fail(category)`：HTTP 200，JSON `code=500`，`msg` 是类别码 |
| 这扇窗能换绑 | **不能。** `insertApplication` 把 `rebind_intent` 写成 `'N'`。换绑是 L-036 的 `PersonRebindController` |
| 提交成功立刻有 `profile_person` 档案 | **不能。** submit 只把申请改成 `WAITING` 并启动流程。档案/绑定是 `handleProcess` 收到 `FINISH` 且决定不是 `REJECT` 之后，归 L-034 的管理端决定链 |

本课**不宣称**你会拆 `PersonAdminController` 审核/绑定/吊销（L-034）、`PersonRebindController` 五枪（L-036）、材料 attach/detach（L-037）、核身供应商回调（L-038）、或 `createProfileService` 前端工厂（L-039）。今天只认：**自己这扇自助窗的 current/submit，以及它们在 layered 五层上怎么走、失败停在哪。**

## 先把宏观地图放在桌上

L-003 把 `wta-profile` 登记成 layered 试点。L-034 是管理员那张桌子：别人的档案、审核、绑定。本课走进**自己的申请条**：每人同时最多一张「进行中」申请；进行中包含 `DRAFT` / `BACK` / `CANCEL` / `WAITING`。其中只有前三个能改字；`WAITING` 能被 `current` 看见，但不能 `save` / `submit`。

2026-09-16 工作树：Java 窗在 `backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/controller/self/PersonApplicationController.java`。用例在 `usecase/impl/PersonApplicationUseCaseImpl.java`。规矩在 `service/PersonApplicationService.java`。柜子钥匙只在 `dao/PersonApplicationDao.java`。SQL 在 `src/main/resources/mapper/person/PersonApplicationMapper.xml`。工作流回写不走这扇 HTTP，走 `listener/PersonApplicationProcessListener.java`。表在基座 `10-cde-base-ddl.sql` 的 `profile_person_application` / `profile_person_submission`。配置种子：`profile.person.provider.default=manual`，`profile.person.flowCode=profile_person_verification`。

```text
已登录的人（浏览器，Admin-Token / 登录票）
        │  权限串 profile:person:apply
        │
        ├─ GET  /profile/person/application              current
        ├─ POST /profile/person/application              save（同门邻居）
        └─ POST /profile/person/application/submit       submit
                        │
                        v
        PersonApplicationController
          LoginHelper.getUserId()     body 里没有 userId
          GET 无 @Log
          两扇 POST 有 @Log，且不存请求/响应体
                        │  只注入 PersonApplicationUseCase
                        v
        PersonApplicationUseCaseImpl     每枪 @DSTransactional
                        │  字段只有 PersonApplicationService
                        v
        PersonApplicationService         规则书；不 import Mapper
          current : 查进行中 → VO 或 null
          save    : 规范化字段 → 写/改 DRAFT
          submit  : 锁 → 校验 → 冻材料 → 快照 → WAITING → 核身 → 工作流
                        │  只注入 PersonApplicationDao + 端口
                        v
        PersonApplicationDao             唯一持有 Mapper
                        │
                        v
        PersonApplicationMapper.xml
          selectOpenByUserId / lockOpenByUserId
          insertApplication / updateDraft
          insertSubmission / markWaiting
                        │
                        v
        表 profile_person_application
          生成列 open_user_id / open_identity_key
          进行中每人一行、每个身份键一行
        表 profile_person_submission
          不可变复印件；(application_id, submission_seq) 唯一
                        │
                        ├─ submit 成功后（仍在同一事务）
                        │     materials.snapshotImmutable(WORKING → SUBMISSION)
                        │     attempts.startAttempt(...)
                        │     workflow.start(...)          失败 → PERSON_WORKFLOW_*
                        │
                        └─ 以后（不是本窗 HTTP）
                              ProcessEvent → PersonApplicationProcessListener
                              → useCase.handleProcess → 可能 publishApproved
                              （档案/绑定 = L-034 的决定半段）
```

| 符号 | 磁盘 | 拥有什么 | 不拥有什么 |
| --- | --- | --- | --- |
| `PersonApplicationController` | `controller/self/` | 三扇 HTTP、权限、校验、包装 `R` | Service、DAO、Mapper、`userId` 入参 |
| `PersonApplicationExceptionHandler` | 同包 `@RestControllerAdvice` | 把类别码写成 `R.fail(msg)` | HTTP 状态码改写；它不抛 404/409 |
| `PersonApplicationUseCaseImpl` | `usecase/impl/` | `@DSTransactional` 事务边界 | DAO / Mapper import |
| `PersonApplicationService` | `service/` | 字段规则、乐观锁、提交顺序、发布 | MyBatis 类型 |
| `PersonApplicationDao` | `dao/` | 全部 Mapper 调用 | UseCase / Service 反向依赖 |
| `PersonApplicationProcessListener` | `listener/` | 把 `ProcessEvent` 转成命令 | 自己写库 |

**图题 / caption：** 个人自助申请的宏观分层窗。alt：已登录用户的 current/save/submit 三扇 HTTP 汇入同一 UseCase；UseCase 把事务章盖在 Service 上；只有 DAO 碰 Mapper XML；submit 之后的档案发布走 Listener，不走这三扇窗。

**文字等价物：** 自己办实名，先 GET 看桌上有没有未完成的本子。有字要改就 POST 根路径保存草稿。满意了把 `expectedVersion` 交给 POST `/submit`。柜员从登录口袋拿出你的编号，从不相信纸条上写的「我是谁」。办事员给整段操作盖事务章。规则书写完才让档案柜员改表。提交成功只表示本子进入等待，不等于档案已经盖章。老师批完的回信从另一扇监听门进来。

**类比：** 把这扇窗想成学校门口的**作业窗口**。`current` 是「把我那本还没交完的练习本拿出来看看」——没有练习本就空着手回来，不会贴一张「404 找不到本子」。`save` 是铅笔改字。`submit` 是把这一页**复印**成不能再涂的快照，贴上材料，送到老师办公室排队。窗口只认你胸牌上的学号。办公室的章（`@DSTransactional`）保证：复印机卡纸或老师办公室门锁着，桌上那页铅笔字还在，不会留下半张复印件。

**类比失效处：**

1. 练习本不是一人一辈子一本。终态（`FINISH` / `INVALID` / `TERMINATION`）会把生成列 `open_user_id` 变成 NULL，名额腾出来，同一人可以再开一本新的。进行中的四态才占唯一键。
2. `WAITING` 仍算「进行中」，`current` 看得到，但窗口不再借铅笔（不可 `save`/`submit`）。
3. 老师批完（`publishApproved`）不是这扇窗的返回值。`submit` 的 VO 状态是 `WAITING`。E2E 在 submit 之后还要另调 `handleProcessEvent("finish")` 才会出现 `profile_person`。
4. 换绑不是把 `rebind_intent` 偷偷改成 Y。这扇窗的 INSERT 写死 `'N'`。别人已经绑了这张身份证，submit 会喊 `PERSON_REBIND_CONFIRMATION_REQUIRED`，让你去 L-036。
5. 分层比喻里的「柜员」字段名叫 `service`，实际类型是 UseCase。不要被变量名带去 classic。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **先数层。** 登记表：`wta-modules/wta-profile` = layered。强制链是 `Controller/Listener → UseCase → Service → DAO → Mapper → XML`。架构测试 `enforcesTheFiveLayerDependencyDirection` 会拆掉越层 import。
2. **三扇窗，两格名字。** GET 根路径 = `current`。POST 根路径 = `save`。POST `/submit` = `submit`。没有 PUT，没有 DELETE，没有路径上的 `{id}`。
3. **主人只来自登录口袋。** `LoginHelper.getUserId()`。`PersonApplicationSaveBo` 的 record 组件名测试锁死**不含** `userId` / `applicantUserId`。
4. **权限就一把钥匙。** 三个方法都是 `@SaCheckPermission("profile:person:apply")`。`self` 包里的 Controller **禁止** `@SaIgnore`。
5. **读 GET，写 POST。** `current` 无 `@Log`。`save` 的 `@Log` 标题「保存个人实名认证申请」、`BusinessType.UPDATE`。`submit` 标题「提交个人实名认证申请」、`BusinessType.OTHER`。两扇写窗 `isSaveRequestData=false` 且 `isSaveResponseData=false`——证件号码不许进操作日志。
6. **事务章盖在 UseCase 上。** `current` / `save` / `submit` / `handleProcess` 都有 `@DSTransactional`。连只读的 `current` 也盖了章。Service 方法本身**没有**这颗注解。
7. **进行中 ≠ 可编辑。** 开着的状态：`DRAFT` `BACK` `CANCEL` `WAITING`。可编辑：前三个。`PersonApplication.editable()` 和 Service 的 `EDITABLE_STATUSES` 是同一组。
8. **`current` 是张望。** `requireUserId` → `selectOpenByUserId`（**不加** `for update`）→ 有就 `PersonApplicationVo.from`，没有就 `null`。
9. **`submit` 是复印+排队。** 必须先有打开的可编辑本子。版本对不上就冲突。字段必须齐全。材料 `ALWAYS` 标签必须齐。然后才写 `profile_person_submission`，把工作副本改成 `WAITING`。
10. **身份键是证件拼出来的。** `PersonIdentityFields.normalize`：类型和号码 `strip` 再 `Locale.ROOT` 大写，拼成 `documentType + ":" + documentNumber`。VO **不**把 `identityKey` 送出窗口。
11. **失败用类别码说话。** 异常消息就是 `PERSON_APPLICATION_VERSION_CONFLICT` 这种码。Advice 原样放进 `R.msg`。E2E 断言 HTTP `isOk()` 且 `$.msg` 为码。
12. **工作流挂了，草稿还在。** `SpringPersonWorkflowGateway.start`：没有 `WorkflowService` 或没有 `profile.person.flowCode` → `PERSON_WORKFLOW_UNAVAILABLE`；`startCompleteTask` 失败 → `PERSON_WORKFLOW_START_FAILED`。E2E 在失败后 `session.rollback()`，断言申请仍是 `DRAFT`、submission 0 行、核身尝试 0 行、SUBMISSION 材料 0 行。

**类比补一句：** 把 `expectedVersion` 想成练习本封面上的**页码章**。你改字时要把看到的那一页页码交回去。别人先交了，页码已经跳了，你还拿旧页码来，窗口就说 `PERSON_APPLICATION_VERSION_CONFLICT`。这不是「再试一次 GET 就会自动提交」。

### 精确定义与 English term

| 中文说法 | English term | 磁盘定义 |
| --- | --- | --- |
| 个人自助申请控制器 | `PersonApplicationController` | `controller.self`。前缀 `/profile/person/application`。三方法：`current` / `save` / `submit` |
| 查询当前进行中申请 | `current` | `GET` 根路径。`R<PersonApplicationVo>`。无 body。`LoginHelper.getUserId()` |
| 提交申请 | `submit` | `POST /submit`。body `PersonApplicationSubmitBo(expectedVersion)`。只把 int 传给 UseCase |
| 保存草稿 | `save` | 同前缀 `POST`。body `PersonApplicationSaveBo`。矩阵格子没写它，同门必须能口述 |
| 分层模块 | layered module | 登记表：`wta-modules/wta-profile`。Controller 不得持有 Service；Service 不得 import Mapper；DAO 才 `@Repository` 持有 Mapper |
| 应用用例 | `PersonApplicationUseCase` | 接口。无参 `current()`/`save`/`submit` 标 `@Deprecated`，默认抛「请传入 userId」。生产实现是 `PersonApplicationUseCaseImpl` |
| 领域服务 | `PersonApplicationService` | 具体类，实现 `PersonApplicationPublicationPort`。不实现 UseCase 接口 |
| 进行中申请 | open application | SQL：`status in ('DRAFT','BACK','CANCEL','WAITING') and del_flag='0'` |
| 可编辑申请 | editable application | `DRAFT` / `BACK` / `CANCEL`。`WAITING` 只读 |
| 乐观锁版本 | `version` / `expectedVersion` | 表列 `version`；HTTP 入参 `expectedVersion`；VO 出参也叫 `version` |
| 提交序号 / 快照版本 | `submissionSeq` / `snapshotVersion` | 表列 `submission_seq`；VO 字段名 `snapshotVersion`；submit 时 `submissionSeq + 1` |
| 身份键 | `identityKey` | `TYPE:NUMBER` 大写拼接。表上有列；VO 没有这个字段 |
| 工作副本表 | `profile_person_application` | 可改的申请本。生成列保证进行中每人/每身份唯一 |
| 不可变提交快照 | `profile_person_submission` | 每次 submit 一行。`uk_profile_person_submission_seq (person_application_id, submission_seq)` |
| 统一响应 | `R<T>` | 成功 `code=200`；`R.fail` 的 `code=500`。Advice 不改 HTTP 状态 |
| 业务类别码 | `PersonApplicationException` | `getMessage()` 就是类别字符串，例如 `PERSON_APPLICATION_NOT_EDITABLE` |

### 机制/因果链

#### 1. 窗口柜员：三扇窗怎么接线

`PersonApplicationController` 不 `extends BaseController`。构造注入一个字段：

```java
private final PersonApplicationUseCase service;
```

口试时先说类型，再说字段名。三个方法共用权限 `profile:person:apply`。HTTP 合同测试还断言：每个声明方法都有这把权限；凡是 `PostMapping` 都必须有 `@Log` 且两头都不存包体。

| Java 方法 | HTTP | Body | 传给 UseCase 的参数 |
| --- | --- | --- | --- |
| `current()` | `GET /profile/person/application` | 无 | `service.current(LoginHelper.getUserId())` |
| `save(PersonApplicationSaveBo)` | `POST /profile/person/application` | 姓名/证件/性别/日期/`expectedVersion` | `service.save(userId, command)` |
| `submit(PersonApplicationSubmitBo)` | `POST /profile/person/application/submit` | `{ expectedVersion }` | `service.submit(userId, command.expectedVersion())` |

`PersonApplicationSubmitBo` 是单字段 record，`@PositiveOrZero int expectedVersion`。Controller **不**把整个 BO 往下传，只拆出 int。

前端厨房（格子归 L-039，本课只作旁证）`createPersonApplicationService` 三枪 URL 与上表一致：`current` GET 根、`save` POST 根、`submit` POST `/submit` 且 `data: { expectedVersion }`。`profilePersonApplicationResource.controller` 字符串就是 `'PersonApplicationController'`。

OpenAPI 快照 `openapi.ts` 把同一前缀拆成 GET=`current`、POST=`save_3`、`/submit` POST=`submit_1`。没有 PUT/DELETE。

#### 2. 办事员：事务章和废弃入口

`PersonApplicationUseCase` 接口留着无 `userId` 的 default 方法，全部 `@Deprecated`，调用就抛 `UnsupportedOperationException("请传入 userId")`。带 `userId` 的 default 会回落到无参版本——所以**新入口必须覆盖带 userId 的方法**。`PersonApplicationUseCaseImpl` 覆盖了四枪：`current(long)` / `save(long, BO)` / `submit(long, int)` / `handleProcess`，每一枪 `@DSTransactional`。

`handleProcess` 不是 HTTP。Listener 收到 WarmFlow 的 `ProcessEvent` 后构造 `PersonApplicationProcessCommand`（实例号、业务号、flowCode、status、`profileDecision`、`snapshotVersion`、时间），再交给这一枪。flowCode 对不上或 businessId 不是正数，Service 直接 return，不炸。

测试适配器 `PersonApplicationServiceImpl`（仅 test）同时实现 UseCase 和 `IPersonApplicationService`，无参方法内部再调 `LoginHelper.getUserId()`。生产路径不走这个类。架构测试规定 production service **不得**以 `ServiceImpl` 结尾。

#### 3. `current`：张望进行中的本子

```text
requireUserId(userId)          userId <= 0 → PERSON_USER_INVALID
findOpenByUserId
  dao.selectOpenByUserId       无 for update
  映射成 PersonApplication
map(PersonApplicationVo::from) 或 null
Controller: R.ok(voOrNull)
```

SQL 选出的状态集合包含 `WAITING`。所以排队中的申请，刷新页面还在。终态本子这句 SQL 看不见——`current` 不会把已办结的档案申请再递出来。

VO 字段：`personApplicationId` / `status` / 身份展示字段（姓名、证件类型号码、性别、生日、有效期）/ `providerCode` / `snapshotVersion` / `version` / `submittedTime` / `finishedTime`。**没有** `applicantUserId`、`identityKey`、`targetProfileId`、`rebindIntent`。窗口给人看的是表单，不是内部锁。

#### 4. `save`：铅笔，且会把 BACK/CANCEL 写回 DRAFT

`save` 不是矩阵格子名，但是 submit 的前置。因果链：

1. `requireUserId`。
2. `PersonIdentityFields.normalize(command)`：null command → `PERSON_DRAFT_REQUIRED`；空白当 null；类型/号码/性别大写；身份键在类型和号码都在时才拼。
3. `validateDraft`：姓名 ≤100、号码 ≤128；性别只许 `MALE`/`FEMALE`/`UNKNOWN`；若填了证件类型，必须能在 `profile_document_type`（`status='0'`）查到规则，号码要匹配 `numberPattern`；生日不能晚于 `Clock` 的今天；有效期起不能晚于止。草稿**不**要求字段齐全，也**不**要求证件在有效期内。
4. 已有进行中且 `!editable()`（也就是 `WAITING`）→ `PERSON_APPLICATION_READ_ONLY`。
5. 供应商：已有本子沿用它的 `providerCode`；新建则读配置 `profile.person.provider.default`，空则 `PERSON_PROVIDER_NOT_CONFIGURED`。`providers.requireEnabled` 失败被收成 `PERSON_PROVIDER_UNAVAILABLE`。
6. 目标档案：账号已有有效绑定则盯着那份档案；否则若身份键已有 ACTIVE 档案就盯那份。单测 `keepsTheCurrentAccountsProfileAsTheReauthenticationTargetWhenIdentityChanges`：人换了身份证号码，`targetProfileId` 仍是账号原来那份档案。
7. `saveDraft`：`lockOpenByUserId`。没有行时 `expectedVersion` 必须是 0，否则冲突；`insertApplication` 写死 `status='DRAFT'`、`submission_seq=0`、`rebind_intent='N'`、`version=0`。有行时状态必须可编辑且版本相等，然后 `updateDraft`：`status='DRAFT'`，`version = version + 1`，WHERE 带旧 `version`。撞唯一键 → `PERSON_APPLICATION_IDENTITY_CONFLICT`。`changed != 1` → 创建冲突或版本冲突。

`updateDraft` 把 `BACK`/`CANCEL` 拉回 `DRAFT`。E2E：流程 `back` 之后再 POST 保存，`$.data.version` 变成 3。

#### 5. `submit`：复印、贴材料、改 WAITING、再按门铃

这是 OBJ-35 要能顺着说完的主链。Service.submit 的顺序以磁盘为准，不要把「先开流程再写库」说成事实：

1. `requireUserId`。
2. `lockOpenByUserId`（`for update`）。没有行 → `PERSON_APPLICATION_NOT_FOUND`。
3. 申请人必须是当前用户，且状态 ∈ `DRAFT/BACK/CANCEL`，否则 `PERSON_APPLICATION_NOT_EDITABLE`。（`WAITING` 走到这里也会被挡。）
4. `application.version() != expectedVersion` → `PERSON_APPLICATION_VERSION_CONFLICT`。
5. `validateComplete`：草稿规则 + 姓名/类型/号码/性别/生日都必填 + 证件规则的号码再验一次 + 若 `validityRequired` 则有效期起止必填 + 完整模式下有效期必须盖住今天，否则 `PERSON_DOCUMENT_EXPIRED`。
6. `requireProviderEnabled`。
7. `requireSubmissionAllowed(userId, targetProfileId, identityKey)`：
   - 身份键空 → `PERSON_IDENTITY_REQUIRED`。
   - 账号已有 `SUSPENDED` 绑定 → `PERSON_BINDING_SUSPENDED`。
   - 账号已绑，但目标档案对不上 → `PERSON_ACCOUNT_ALREADY_BOUND`。
   - 账号已绑，身份键指向另一份 ACTIVE 档案 → `PERSON_IDENTITY_CONFLICT`。
   - 账号未绑，但这份身份已被别人有效绑定 → `PERSON_REBIND_CONFIRMATION_REQUIRED`（持久化单测锁死这条，不是去改别人的绑定）。
8. `materials.validateRequired(WORKING, documentTypeCode, Set.of("ALWAYS"))`。缺材料在这里停，还没写 submission。
9. `snapshotVersion = submissionSeq + 1`。时钟 `clock.instant()`（生产 UTC）。
10. `insertSubmission`：字段快照 JSON + 列拷贝。唯一键冲突 → `PERSON_SUBMISSION_CONFLICT`。
11. `materials.snapshotImmutable(WORKING → SUBMISSION)`。工作区那两张证件照片复印到提交所有者上。E2E 在 WAITING 时断言 SUBMISSION 材料 2 行，此时 `profile_person` 仍是 0 行。
12. `markWaiting`：SQL 要求 `submission_seq = 新序号 - 1`、状态仍可编辑、`version = expectedVersion`。成功后 `version + 1`，`status='WAITING'`，清掉旧决定字段。对不上 → 版本冲突。
13. `startVerificationAttempt`：指纹是 `SHA-256(identityKey + "\n" + snapshotVersion)`。供应商异常收成 `PERSON_PROVIDER_UNAVAILABLE`。
14. `workflow.start(applicationId, submissionId, snapshotVersion)`。网关把变量 `profileType=PERSON`、`snapshotVersion`、`submissionId` 塞进 `StartProcessDTO`，`businessId` 是申请主键字符串，`flowCode` 来自配置。
15. 返回 `PersonApplicationVo.from(waiting)`，状态 `WAITING`，`snapshotVersion` 已是新序号。

单测 `submitsImmutableFieldAndMaterialSnapshotBeforeWorkflow` 按上面 8/11/14 的顺序 verify。`workflowFailureDoesNotPublishAnything`：`workflow.start` 抛 `PERSON_WORKFLOW_UNAVAILABLE` 时 **never** `publishApproved`。

因为 UseCase 整段在一个 `@DSTransactional` 里，13/14 抛错会把 10/11/12 一起撤掉。E2E `returnsThenResubmitsAndRollsBackEverySubmitWriteWhenWorkflowFails` 是这条因果链的铁证：HTTP 仍 200，`$.msg=PERSON_WORKFLOW_UNAVAILABLE`，回滚后草稿还在。

#### 6. 提交之后（本课边界，只为把 WAITING 说完）

`handleProcess` 不是 current/submit 的返回路径，但口试常被问「提交完档案怎么出来的」。最短正确说法：

- 事件 `flowCode` 必须等于配置 `profile.person.flowCode`，否则忽略。
- 申请必须仍是 `WAITING` 且 `submissionSeq == snapshotVersion`，否则忽略（含迟到的 finish）。
- `FINISH` 且决定 `REJECT` → 把申请打成 `INVALID`，不发布。
- `FINISH` 且非拒绝 → `publishApproved`：写版本、更新档案、可能插绑定；若 `rebindIntent=Y` 则 `PERSON_REBIND_NOT_SUPPORTED_BY_THIS_COMMAND`。
- `BACK`/`CANCEL`/`INVALID`/`TERMINATION` → 只改申请状态，不发布。`BACK`/`CANCEL` 的 `finished_time` 置空，本子重新可编辑。

管理员在 L-034 的 `decide` 是另一张桌子；流程引擎打回来的才走 Listener。本课 submit 只负责把本子推进 `WAITING` 并按门铃。

#### 7. 数据库怎么保证「进行中只有一本」

表 `profile_person_application` 有两列生成列（实体上 `FieldStrategy.NEVER`，Java 不写）：

- `open_user_id` = 进行中四态且未删时的 `applicant_user_id`，否则 NULL。唯一键 `uk_profile_person_application_open_user`。
- `open_identity_key` = 同样条件下的 `identity_key`。唯一键 `uk_profile_person_application_open_identity`。

E2E `permitsOnlyOneConcurrentOpenSubmissionPerIdentityAndPerAccount`：两个会话同时插入 WAITING，同一身份或同一账号，只能活一行，另一行唯一冲突；此时还没有 `profile_person` / binding。这是表自己当裁判，不是 Service 先 SELECT 再碰运气。

`markWaiting` 不释放这两列——`WAITING` 仍占坑。所以排队时别人不能用同一身份证再开一本，你自己也不能再开第二本。

### 图、表或文本图

**图 1. 宏观五层与三扇窗**

```text
 HTTP self 窗                         不是本窗
 GET  /application          current    ProcessEvent ──► Listener
 POST /application          save              │
 POST /application/submit   submit            v
        │                              UseCase.handleProcess
        v
 PersonApplicationController ──► UseCaseImpl (@DSTransactional)
        │
        v
 PersonApplicationService
        │  不持有 Mapper
        v
 PersonApplicationDao ──► PersonApplicationMapper.xml
        │
        ├─ profile_person_application   工作副本
        └─ profile_person_submission    不可变复印件
```

- **alt：** current/save/submit 从 Controller 进入 UseCase 事务，再进 Service 和 DAO；工作流事件从 Listener 进同一 UseCase。
- **caption：** 图 1——OBJ-35 的分层空间。矩阵 A 是 current 与 submit；save 同门。
- **文字等价物：** 自助 HTTP 只有这一份 Controller。它不碰表。事务在 UseCase。规则在 Service。SQL 在 XML。Listener 和 Controller 是同一层的两种入口。
- **图的边界：** 不画 `PersonAdminController`、不画 `PersonRebindController`、不画材料 attach 的另一份 Controller。不把 `IPersonApplicationService` 画进生产箭头。

**图 2. submit 成功才把本子改成 WAITING；失败停在哪一层**

```text
submit(expectedVersion):
  无进行中行                    ──► PERSON_APPLICATION_NOT_FOUND
  不是本人或状态不是可编辑      ──► PERSON_APPLICATION_NOT_EDITABLE
  version 对不上                ──► PERSON_APPLICATION_VERSION_CONFLICT
  字段不齐 / 证件规则失败       ──► PERSON_FIELDS_INCOMPLETE 等
  供应商不可用                  ──► PERSON_PROVIDER_UNAVAILABLE
  账号已绑别人的档案 / 身份被占 ──► PERSON_ACCOUNT_ALREADY_BOUND
                                   / PERSON_REBIND_CONFIRMATION_REQUIRED
  ALWAYS 材料不齐               ──► 材料端口异常（本课不拆）
  insertSubmission 撞序号       ──► PERSON_SUBMISSION_CONFLICT
  markWaiting 条件不中          ──► PERSON_APPLICATION_VERSION_CONFLICT
  startAttempt 失败             ──► PERSON_PROVIDER_UNAVAILABLE  （事务回滚）
  workflow 缺 Bean/缺 flowCode  ──► PERSON_WORKFLOW_UNAVAILABLE （事务回滚）
  startCompleteTask 失败        ──► PERSON_WORKFLOW_START_FAILED（事务回滚）
  全绿                          ──► VO.status=WAITING, snapshotVersion=旧seq+1
                                   此时还没有 profile_person 新档案
```

- **alt：** submit 在写快照和改 WAITING 之后才启动核身与工作流；后两步失败会回滚前面的写。
- **caption：** 图 2——submit 的失败停点。current 不在这张图上，因为它不写库。
- **文字等价物：** 先锁、再验、再复印、再贴「等待」。门铃按不响，复印作废。不要说「流程失败了但快照留下当审计」。
- **图的边界：** 不保证 WarmFlow 内部任务长什么样。不保证核身供应商 HTTP。不保证管理员 `decide` 的按钮文案。

**图 3. current 看见什么状态**

```text
status
 DRAFT ----save----► DRAFT          current 看得到，可编辑
 BACK  ----save----► DRAFT          current 看得到；save 会拉回 DRAFT
 CANCEL----save----► DRAFT          同上
 WAITING  (submit 出口)             current 看得到，不可 save/submit
 FINISH / INVALID / TERMINATION     current 的 SQL 看不见（生成列释放）
```

- **alt：** 进行中四态能被 current 查出；终态不能；WAITING 只读。
- **caption：** 图 3——current 的可见性与可编辑性不是同一件事。
- **文字等价物：** 「进行中」是给唯一键和 GET 用的集合。「可编辑」少了 WAITING。「终态」离开 current 的眼睛，也离开唯一键。
- **图的边界：** 不解释管理员如何把 WAITING 审成 FINISH。那是 L-034 + `handleProcess`。

### 正例、反例与边界

**正例 1：** 用户 101 从未开过申请。`GET /profile/person/application` → `R.ok(null)`，`data` 为空。然后 `POST` 根路径保存身份证草稿 `expectedVersion=0`，供应商配置为 `manual` 且已启用。INSERT 后 VO：`status=DRAFT`，`version=0`，`snapshotVersion=0`，`providerCode=manual`。再挂上 ALWAYS 材料，`POST /submit` body `{"expectedVersion":0}`。VO：`status=WAITING`，`snapshotVersion=1`。表上多了一行 submission。`profile_person` 仍 0 行。

**正例 2：** 流程把同一本打回 `BACK`。`current` 仍能看见。再 `save` 必须带上新的 `expectedVersion`（E2E 里是 2→3）。再 `submit` `{"expectedVersion":3}` 得到第二次 WAITING，`submission` 变成 2 行。第一次复印件还在，序号唯一键按 `(application_id, seq)` 分开。

**正例 3：** 账号已经 ACTIVE 绑定档案 9201。用户改了身份证号再 save。`targetProfileId` 仍是 9201。以后 `publishApproved` 走「换身份键、不换档案、不换绑定」——这是提交之后的发布规则，submit 当时只是把目标钉在账号现有档案上。

**正例 4：** 合同测试：三方法权限都是 `profile:person:apply`；SaveBo 没有 userId 字段；UseCaseImpl 的 `save`/`submit`/`handleProcess` 带 `@DSTransactional`。

**正例 5：** 两个人同时用同一身份证抢 WAITING 坑，或同一账号同时用两个号码抢坑：生成列唯一键只让一行活着。档案表仍空。

**反例 1：** 把 `current` 说成「没有申请就 404」。磁盘是 `orElse(null)` + `R.ok`。空是合法答案：桌上没有本子。

**反例 2：** 对 `WAITING` 再 POST `/submit`。状态不在可编辑集合 → `PERSON_APPLICATION_NOT_EDITABLE`。不要说「幂等提交会返回同一 VO」。

**反例 3：** body 里塞 `userId` 想替别人交。Controller 不读这个字段；SaveBo 根本没有它。权限过了也只动登录者自己的行。`updateDraft` 的 WHERE 还带 `applicant_user_id`。

**反例 4：** 工作流 Bean 缺失时，以为 submission 会留下。E2E 明确：回滚后 submission 0、attempt 0、SUBMISSION 材料 0、申请仍 DRAFT。口试若说「至少留快照」，与事务边界相反。

**反例 5：** 把这扇窗的 submit 说成换绑。身份已被别人绑定时，这里停在 `PERSON_REBIND_CONFIRMATION_REQUIRED`，不会改 `profile_person_binding`。换绑五枪是 L-036。

**反例 6：** 把 Controller 字段 `service` 画成 classic：`Controller → Service → Mapper`。架构测试会在 Controller 源码里搜到 `import ...service.` 就失败。生产 Service 源码里搜到 `import ...mapper.` 也会失败。

**边界 1：** `current` 虽只读，UseCase 仍 `@DSTransactional`。不要发明「读方法无事务」的分层教条；以这份实现为准。

**边界 2：** 异常 Advice 把类别码放进 `msg`，HTTP 状态仍 200，JSON `code` 才是 500。前端若只看 HTTP 状态会误判成功。L-039 才讲厨房如何读 `R`。

**边界 3：** `PersonApplicationService` 构造函数有个未使用的 `Object jsonMapper` 参数；真正序列化走 `JsonUtils.toJsonString`。不要口误成「Service 持有 Jackson Mapper」。

**边界 4：** `selectOpenByUserId` 没有 `LIMIT 1`。唯一键保证进行中每人一行；若有脏数据多行，MyBatis 映射行为不在本课保证范围。以 DDL 唯一键为设计意图。

**边界 5：** 核身默认供应商种子是 `manual`（人工复核）。`startAttempt` 仍会记尝试。供应商回调窗是 L-038，不是 submit 的 HTTP。

## 变式与迁移

- **同一人再认证。** 绑定还在，换证件号：save 盯着原档案；submit 仍走这扇窗，不是 rebind 窗。发布时改 identity guard，档案主键不变。
- **别人占用身份。** 未绑账号去提交已被占用的身份键：submit 在 `requireSubmissionAllowed` 停住，提示需要换绑确认。迁移到 L-036 的 probe/match/confirm，不要在 ApplicationController 上发明第四扇窗。
- **打回再交。** `BACK` 仍占进行中唯一键。current 看得到。必须先 save（拉回 DRAFT 并 +version）再 submit。直接拿打回前的旧 version 会冲突。
- **企业对照。** L-041 的 `EnterpriseApplicationController` 格子写成 `current,save,submit,probe`——企业多一扇 probe。个人这扇**没有** probe。不要把企业的 save 写进个人矩阵格子名；个人只是同门邻居。
- **前端对照。** `createPersonApplicationService` 三枪 URL 与本课 HTTP 一一对应。本课不认前端格子。迁移时只借它证明：GET 根、POST 根、POST `/submit`，没有 DELETE。
- **classic 对照。** L-031 那种 `Controller → Service → @Select Mapper` 不适用于 profile。这里 XML 语句 id 必须与 Mapper 方法一一对应，架构测试 `mapperMethodsAreBackedByMatchingXmlWithoutAnnotationSql` 禁止注解 SQL。
- **发布对照。** submit 成功 ≠ 档案存在。把「主路径 PersonApplication.submit → admin decide」说完整，需要 L-034 的决定半段。本课只保证 submit 把本子送进 WAITING 并按门铃。

## 常见误区

1. **把 `current` 当成详情 GET `/application/{id}`。** 没有 id。只能看「我的进行中那一本」。办结了就变 null。
2. **把 `WAITING` 排除出 current。** SQL 包含它。排队中刷新页面，本子还在，只是铅笔被收走。
3. **以为 submit 的 body 还要再交一遍姓名证件。** SubmitBo 只有版本号。完整字段必须已经在工作副本上，来自上次 save。
4. **以为分层里 UseCase 只是空转发，所以可以删掉。** 事务注解在 UseCase。删掉它，submit 的回滚契约就没了盖章位置。Service 无 `@DSTransactional`。
5. **把测试类 `PersonApplicationServiceImpl` 当成生产分层。** 它在 test 源码，还同时实现 UseCase——这是为了老测试。生产 Controller 注入的是接口，实现类是 `PersonApplicationUseCaseImpl`。
6. **把 `R.fail` 说成 HTTP 500 页面。** E2E 用 MockMvc `status().isOk()`。业务失败在 JSON 里。
7. **把材料校验当成 L-035 的 HTTP。** submit 会喊 `ProfileMaterialPort.validateRequired`，但挂材料的窗是 `PersonMaterialSelfController`（L-037）。没有 ALWAYS 材料，submit 到不了 WAITING。
8. **把 `identityKey` 画进 VO。** 规则内部用它做唯一和冲突；窗口 JSON 没有这一列。
9. **把 Listener 画进 current/submit 时序图的同步返回。** 浏览器在 submit 返回 WAITING 时还没拿到 FINISH。回写是另一条时间线。
10. **用 classic 的 `IService` / `ServiceImpl` / `QueryWrapper` 来读这张表。** 架构测试在 service 包禁止这些词，DAO 才碰 Mapper。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 打开 `PersonApplicationController`。把三对 mapping 抄成一张表：路径、动词、Java 名、权限串、有没有 `@Log`、Log 是否存包体。圈出：GET 是 `current`；POST 根是 `save`；POST `/submit` 才是矩阵点名的 submit。圈字段类型 `PersonApplicationUseCase`。
2. 用手指划 `current` → `UseCaseImpl.current`（`@DSTransactional`）→ `Service.current` → `dao.selectOpenByUserId` → XML 的状态列表。在 `orElse(null)` 旁边写：空是成功。
3. 再划 `submit` → `command.expectedVersion()` → `lockOpenByUserId` → `validateComplete` → `requireSubmissionAllowed` → `validateRequired` → `insertSubmission` → `snapshotImmutable` → `markWaiting` → `startAttempt` → `workflow.start`。对照单测方法名 `submitsImmutableFieldAndMaterialSnapshotBeforeWorkflow`。
4. 打开 DDL 里 `profile_person_application` 的两列生成列和两个 unique key。对照实体上 `openUserId` / `openIdentityKey` 的 `FieldStrategy.NEVER`。打开 E2E `permitsOnlyOneConcurrentOpenSubmissionPerIdentityAndPerAccount`。
5. 打开 `PersonApplicationExceptionHandler` 和 E2E 里工作流失败那一段。抄下：HTTP `isOk`，`$.msg` 为 `PERSON_WORKFLOW_UNAVAILABLE`，回滚后 DRAFT 还在。把「失败=HTTP 4xx」划掉。

## 总结、词汇表与下一步

- **宏观一扇自助门。** 前缀 `/profile/person/application`。GET 看进行中；POST 根保存草稿；POST `/submit` 交复印件。权限一把 `profile:person:apply`。主人只来自 `LoginHelper.getUserId()`。
- **分层硬链。** Controller/Listener → UseCase（事务）→ Service（规则）→ DAO（唯一 Mapper 持有者）→ XML。不要把字段名 `service` 说成 classic。
- **current。** 无锁查询进行中四态；没有就 `R.ok(null)`；VO 不带身份键和申请人 id。
- **submit。** 锁可编辑行 → 对版本 → 完整校验 → 绑定/身份冲突闸 → ALWAYS 材料 → 不可变 submission → WAITING → 核身尝试 → 工作流门铃。后两步失败，整段回滚。
- **进行中唯一。** 生成列 `open_user_id` / `open_identity_key` 在数据库层保证每人/每身份一本开着的申请。WAITING 仍占坑。
- **submit ≠ 档案诞生。** 主路径的后半段是流程回写 / 管理员决定（L-034）。换绑是另一扇门（L-036）。
- **格子按磁盘：** `A:PersonApplicationController.current, submit`。课内必须能指认同门 `save`，不要把它说成矩阵里的第四个符号。

词汇表：`PersonApplicationController` / layered / `PersonApplicationUseCase` / `@DSTransactional` / `PersonApplicationService` / `PersonApplicationDao` / `PersonApplicationVo` / `PersonApplicationSubmitBo` / `expectedVersion` / `identityKey` / `DRAFT` / `BACK` / `CANCEL` / `WAITING` / `profile_person_application` / `profile_person_submission` / `open_user_id` / `markWaiting` / `PersonApplicationException` / `profile:person:apply` / `LoginHelper.getUserId()` / `PERSON_WORKFLOW_UNAVAILABLE`。

下一步：L-036 走进换绑五枪——本课 `PERSON_REBIND_CONFIRMATION_REQUIRED` 把人推向那扇门。L-037 讲 ALWAYS 材料如何挂到 WORKING 所有者上，那是 submit 能通过材料闸的前提。L-038 讲供应商回调。L-034 讲管理员决定如何让 WAITING 变成档案和绑定。L-039 才把 `createProfileService` 接到这三扇 HTTP。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | 工作树 `backend/wta-modules/**` Controller / UseCase | profile-person 公开入口存在 | `sources.md` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-modules/wta-profile` = layered | 当前登记表 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 申请表生成列/唯一键；`profile.person.provider.default=manual`；`profile.person.flowCode=profile_person_verification`；权限种子 `profile:person:apply` | DDL `profile_person_application`；DML 配置/菜单 | 2026-09-16 |
| S-L035-01 | `PersonApplicationController.java`、`PersonApplicationExceptionHandler.java` | 三扇窗、权限、Log 不存包体、`LoginHelper.getUserId()`、Advice → `R.fail(msg)` | `controller/self` | 2026-09-16 |
| S-L035-02 | `PersonApplicationUseCase.java`、`PersonApplicationUseCaseImpl.java` | 废弃无 userId 入口；实现四枪均 `@DSTransactional`；注入具体 `PersonApplicationService` | `usecase/` | 2026-09-16 |
| S-L035-03 | `PersonApplicationService.java` | `current`/`save`/`submit`/`handleProcess` 顺序、可编辑集合、版本冲突、提交闸、指纹、失败类别码 | `service/PersonApplicationService.java` | 2026-09-16 |
| S-L035-04 | `PersonApplicationDao.java`、`PersonApplicationMapper.java`、`PersonApplicationMapper.xml` | DAO 唯一持有 Mapper；`selectOpenByUserId`/`lockOpenByUserId`/`insertApplication`/`updateDraft`/`insertSubmission`/`markWaiting` | `dao/`、`mapper/`、`resources/mapper/person/` | 2026-09-16 |
| S-L035-05 | `PersonApplication.java`、`PersonIdentityFields.java`、`PersonApplicationVo.java`、`PersonApplicationSaveBo.java`、`PersonApplicationSubmitBo.java` | editable/terminal；normalize 拼身份键；VO 无 identityKey；SaveBo/SubmitBo 形状 | `domain/application`、`domain/vo`、`domain/bo` | 2026-09-16 |
| S-L035-06 | `PersonApplicationHttpContractTest.java`、`PersonModuleArchitectureTest.java` | 路径/权限/Log/DSTransactional；五层依赖方向；Controller 在 `self` 且无 `@SaIgnore` | `src/test/java/.../controller/self`、`architecture/` | 2026-09-16 |
| S-L035-07 | `PersonApplicationServiceTest.java`、`PersonApplicationServicePersistenceTest.java`、`PersonApplicationMySqlE2ETest.java` | 提交先冻材料再开流程；工作流失败不发布；身份被占要换绑确认；HTTP 回滚；并发唯一键 | 同模块测试 | 2026-09-16 |
| S-L035-08 | `PersonApplicationProcessListener.java`、`SpringPersonWorkflowGateway.java` | 事件转命令；缺 Bean/缺 flowCode → `PERSON_WORKFLOW_UNAVAILABLE` | `listener/`、`adapter/gateway/` | 2026-09-16 |
| S-L035-09 | `frontend/packages/domains/profile/src/person/application/service.ts`、`index.ts`（旁证，不认 L-039 格子） | 浏览器三枪 URL 与动词对齐 | `createPersonApplicationService` | 2026-09-16 |
| S-L035-10 | `ProfilePersonApplication.java`、`PersonApplicationServiceImpl.java`（test） | 生成列 `NEVER`；测试适配器不是生产 ServiceImpl | 实体；`src/test/.../service/impl` | 2026-09-16 |
