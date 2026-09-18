---
lesson_id: L-041
objective_ids: [OBJ-41]
claimed_cells:
  - A:EnterpriseApplicationController.current,save,submit,probe
  - A:EnterpriseVerificationAnonymousController.callback
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: layered-windows
    minutes: 5
  - segment: current-save-submit-probe
    minutes: 10
  - segment: callback-chain
    minutes: 7
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 2
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L041-01, S-L041-02, S-L041-03, S-L041-04, S-L041-05, S-L041-06, S-L041-07, S-L041-08, S-L041-09, S-L041-10]
---

# Lesson 041：宏观上看，企业自己交的申请条——`current` / `save` / `submit` / `probe` 与供应商 `callback`

## 学完你能做什么

打开 `wta-profile-enterprise` 里**两扇门**，你能**口述** OBJ-41 点名的五枪，并且把分层链说完：

> 已登录的人看自己那张还没办完的企业实名申请，用 `GET /profile/enterprise/application`（Java 名 `current`）。铅笔改字用 `POST` 根路径（Java 名 `save`）。把草稿变成「等老师批」的复印件，用 `POST /profile/enterprise/application/submit`（Java 名 `submit`）。先问「这个统一社会信用代码现在占着吗」，用 `POST /profile/enterprise/application/probe`（Java 名 `probe`）。核身供应商来敲门，用 `POST /profile/enterprise/verification/providers/{providerCode}/callback`（Java 名 `callback`）。前四枪共用权限 `profile:enterprise:apply`，主人只来自登录口袋；`probe` 不读 `userId`。`callback` 挂 `@SaIgnore`，不认登录票。模块是 **layered**：`Controller / Listener → UseCase → Service → DAO → Mapper → Mapper XML`。`current` 找不到进行中申请就 `R.ok(null)`。`submit` 先锁草稿、校验完整、冻材料、写不可变快照、把状态改成 `WAITING`，再喊核身供应商和工作流；工作流起不来，**整段事务回滚**。`callback` 只给核身尝试盖章，**不**发布 `profile_enterprise`。

本课认矩阵 **(a)** 两格，符号以磁盘为准：

1. **`A:EnterpriseApplicationController.current,save,submit,probe`**（`@RequestMapping("/profile/enterprise/application")`，包 `controller.self`）：四扇 HTTP 全在格子里。`current` 是无参 GET；`save` 是 `POST` 根路径；`submit` 是 `POST /submit`，body 只有 `EnterpriseApplicationSubmitBo.expectedVersion`；`probe` 是 `POST /probe`，body 只有 `unifiedCreditCode`。
2. **`A:EnterpriseVerificationAnonymousController.callback`**（`@RequestMapping("/profile/enterprise/verification/providers")`，包 `controller.anonymous`）：一扇匿名 POST `/{providerCode}/callback`。成功枚举是 `ACCEPTED` / `IDEMPOTENT` / `LATE_IGNORED`。失败会改 HTTP 状态码，和自助窗的 Advice 不是同一套。

2026-09-16 工作树先钉死**分层和门牌**（口试先数层，再数方法）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `EnterpriseApplicationController` 直接调 `EnterpriseApplicationService` | **禁止。** 架构测试锁死 Controller 只 import `usecase`，不 import `service` |
| 字段名叫 `service` 所以是 Service | **类型是 `EnterpriseApplicationUseCase`。** 名字骗人，类型才算数 |
| 生产 `EnterpriseApplicationServiceImpl` | **没有。** `src/main` 的类就叫 `EnterpriseApplicationService`。`EnterpriseApplicationServiceImpl` 只在 `src/test`，同时 `implements EnterpriseApplicationUseCase` |
| `IEnterpriseApplicationService` 是生产接线 | **半对。** 接口存在；生产类 **不** `implements` 它。UseCase 注入的是具体类 `EnterpriseApplicationService` |
| `current` 空申请 → 404 | **不是。** `orElse(null)` 再 `R.ok(null)`，HTTP 仍 200 |
| 自助失败 → HTTP 4xx | **不是。** `EnterpriseApplicationExceptionHandler` 返回 `R.fail(category)`：HTTP 200，JSON `code=500`，`msg` 是类别码 |
| 回调失败也 HTTP 200 | **不是。** `EnterpriseVerificationCallbackExceptionHandler`：签名/过期 → HTTP 401；其余 → HTTP 400。`data.category` 才是枚举名 |
| `probe` 看的是「我的申请」 | **不是。** Controller **不**调 `LoginHelper.getUserId()`。它只问信用代码在全库里的占用态 |
| 提交成功立刻有 `profile_enterprise` 档案 | **不能。** submit 只把申请改成 `WAITING` 并启动流程。档案/负责人绑定是 `handleProcess` 收到 `FINISH` 且决定不是 `REJECT` 之后 |
| 供应商回调会发布档案 | **不会。** E2E 在 `ACCEPTED` 之后断言申请仍是 `WAITING`，只改 `profile_verification_attempt` |
| 默认供应商 `manual` 会接回调 | **不会。** `EnterpriseManualVerificationProvider.authenticate` 直接抛 `UNSUPPORTED_CALLBACK` |
| 这扇窗能换负责人 | **不能。** 账号已有有效绑定 → `ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE`。企业已有负责人 → `ENTERPRISE_RESPONSIBLE_ALREADY_BOUND`。转移是 L-042 |

本课**不宣称**你会拆 `EnterpriseAdminController` 审核/绑定/吊销（L-040）、`EnterpriseTransferController` 三枪（L-042）、材料 attach/detach（L-043）、或 `createProfileService` / enterprise 页面（L-044）。今天只认：**自己这扇自助窗的 current/save/submit/probe，以及供应商那扇匿名 callback，它们在 layered 五层上怎么走、失败停在哪。**

## 先把宏观地图放在桌上

L-003 把 `wta-profile` 登记成 layered 试点。L-040 是管理员那张桌子：别人的企业档案、审核、绑定。本课走进**自己的企业申请条**，外加**供应商门铃**。每人同时最多一张「进行中」企业申请；进行中包含 `DRAFT` / `BACK` / `CANCEL` / `WAITING`。其中只有前三个能改字；`WAITING` 能被 `current` 看见，但不能 `save` / `submit`。个人那扇窗（L-035）没有 probe；企业多这一扇。个人的核身回调单独是 L-038；企业的回调被 OBJ-41 钉在本课。

2026-09-16 工作树：自助窗在 `backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/controller/self/EnterpriseApplicationController.java`。匿名回调在同模块 `controller/anonymous/EnterpriseVerificationAnonymousController.java`。申请用例在 `usecase/impl/EnterpriseApplicationUseCaseImpl.java`。核身用例在 `usecase/impl/EnterpriseVerificationUseCaseImpl.java`。申请规矩在 `service/EnterpriseApplicationService.java`。核身协调器在 `service/EnterpriseVerificationAttemptService.java`（实现端口 `EnterpriseVerificationService`）。柜子钥匙只在 `dao/EnterpriseApplicationDao.java` 与 `dao/EnterpriseVerificationAttemptDao.java`。SQL 在 `src/main/resources/mapper/enterprise/EnterpriseApplicationMapper.xml` 与 `EnterpriseVerificationAttemptMapper.xml`。工作流回写不走这两扇 HTTP，走 `listener/EnterpriseApplicationProcessListener.java`。表在基座 `10-cde-base-ddl.sql` 的 `profile_enterprise_application` / `profile_enterprise_submission` / `profile_verification_attempt` / `profile_operation_audit`。配置种子：`profile.enterprise.provider.default=manual`，`profile.enterprise.flowCode=profile_enterprise_verification`。属性默认启用供应商集合是 `{"manual"}`。

```text
已登录的人（浏览器，Admin-Token / 登录票）
        │  权限串 profile:enterprise:apply
        │
        ├─ GET  /profile/enterprise/application              current
        ├─ POST /profile/enterprise/application              save
        ├─ POST /profile/enterprise/application/submit       submit
        └─ POST /profile/enterprise/application/probe        probe（不读 userId）
                        │
                        v
        EnterpriseApplicationController
          current/save/submit : LoginHelper.getUserId()
          probe               : 只把 UnifiedCreditCode 往下传
          GET 无 @Log
          三扇 POST 有 @Log，且不存请求/响应体
                        │  只注入 EnterpriseApplicationUseCase
                        v
        EnterpriseApplicationUseCaseImpl     每枪 @DSTransactional
                        │  字段只有 EnterpriseApplicationService
                        v
        EnterpriseApplicationService         规则书；不 import Mapper
          current : 查进行中 → VO 或 null
          save    : 规范化字段 → 写/改 DRAFT
          submit  : 锁 → 校验 → 冻材料 → 快照 → WAITING → 核身 → 工作流
          probe   : 信用代码占用态（不写库）
                        │  只注入 EnterpriseApplicationDao + 端口
                        v
        EnterpriseApplicationDao             唯一持有 Mapper
                        │
                        v
        EnterpriseApplicationMapper.xml
          selectOpenByUserId / lockOpenByUserId
          insertApplication / updateDraft
          insertSubmission / markWaiting
          selectProbeStatus
                        │
                        v
        表 profile_enterprise_application
          生成列 open_user_id / open_identity_key
          进行中每人一行、每个信用代码一行
        表 profile_enterprise_submission
          不可变复印件；(application_id, submission_seq) 唯一

核身供应商（没有登录票）
        │  @SaIgnore
        │
        └─ POST /profile/enterprise/verification/providers/{providerCode}/callback
                        │
                        v
        EnterpriseVerificationAnonymousController
          timeSource.now() 当 receivedAt
          body → EnterpriseProviderCallbackEnvelope
                        │  只注入 EnterpriseVerificationUseCase
                        v
        EnterpriseVerificationUseCaseImpl    @DSTransactional
                        │  字段名叫 coordinator，类型是端口
                        v
        EnterpriseVerificationAttemptService
          provider.authenticate → 锁尝试 → ACCEPTED / IDEMPOTENT / LATE_IGNORED
                        │
                        v
        表 profile_verification_attempt   profile_type='ENTERPRISE'
        表 profile_operation_audit        失败/迟到才记

submit 成功后（仍在同一事务，不是 callback）
        materials.snapshotImmutable(WORKING → SUBMISSION)
        attempts.startAttempt(...)
        workflow.start(...)          失败 → ENTERPRISE_WORKFLOW_*

以后（不是本窗 HTTP）
        ProcessEvent → EnterpriseApplicationProcessListener
        → useCase.handleProcess → 可能 publishApproved
        （档案/负责人绑定 = L-040 的决定半段）
```

| 符号 | 磁盘 | 拥有什么 | 不拥有什么 |
| --- | --- | --- | --- |
| `EnterpriseApplicationController` | `controller/self/` | 四扇 HTTP、权限、校验、包装 `R` | Service、DAO、Mapper、`userId` 入参 |
| `EnterpriseApplicationExceptionHandler` | 同包 `@RestControllerAdvice` | 把类别码写成 `R.fail(msg)` | HTTP 状态码改写；它不抛 404/409 |
| `EnterpriseVerificationAnonymousController` | `controller/anonymous/` | 一扇 `@SaIgnore` 回调、时间源 | 登录口袋；发布档案 |
| `EnterpriseVerificationCallbackExceptionHandler` | `assignableTypes` 钉死匿名窗 | HTTP 401/400 + `data.category` | 自助窗的 200/`msg=码` 合同 |
| `EnterpriseApplicationUseCaseImpl` | `usecase/impl/` | `@DSTransactional` 事务边界（含 probe） | DAO / Mapper import |
| `EnterpriseVerificationUseCaseImpl` | 同目录 | 回调事务章 | 自己写库 |
| `EnterpriseApplicationService` | `service/` | 字段规则、乐观锁、提交顺序、发布 | MyBatis 类型 |
| `EnterpriseVerificationAttemptService` | `service/` | 核身尝试与回调幂等 | 改申请状态、插绑定 |
| `EnterpriseApplicationDao` | `dao/` | 申请侧全部 Mapper 调用 | UseCase / Service 反向依赖 |
| `EnterpriseApplicationProcessListener` | `listener/` | 把 `ProcessEvent` 转成命令 | 自己写库 |

**图题 / caption：** 企业自助申请与供应商回调的宏观分层窗。alt：已登录用户的 current/save/submit/probe 四扇 HTTP 汇入同一 UseCase；供应商 callback 从 anonymous 包进入另一份 UseCase；两份 UseCase 都盖事务章；只有 DAO 碰 Mapper XML；submit 之后的档案发布走 Listener，不走这五扇窗。

**文字等价物：** 自己办企业实名，先 GET 看桌上有没有未完成的本子。有字要改就 POST 根路径保存草稿。拿不准这个信用代码有没有被人占着，就 POST `/probe`，窗口只回四个字里的一个状态，不给你看别人的表单。满意了把 `expectedVersion` 交给 POST `/submit`。柜员从登录口袋拿出你的编号，从不相信纸条上写的「我是谁」。probe 连口袋都不看，只看信用代码。供应商来敲门走另一扇没有胸牌的门。办事员给整段操作盖事务章。规则书写完才让档案柜员改表。提交成功只表示本子进入等待，不等于企业档案已经盖章。供应商点头只表示核身尝试完成，更不等于档案诞生。老师批完的回信从另一扇监听门进来。

**类比：** 把自助窗想成工商窗口前的**企业作业本**。`current` 是「把我那本还没交完的本子拿出来看看」——没有本子就空着手回来，不会贴一张「404」。`save` 是铅笔改字。`submit` 是把这一页**复印**成不能再涂的快照，贴上材料，送到审批办公室排队。`probe` 是窗口墙上的**占用灯**：这个统一社会信用代码正在办、已经有负责人、档案还在但没人认领、还是空号。供应商 `callback` 是快递站的**门铃**：快递员按门铃交回执，不进工商窗口，也不替局长盖公章。

**类比失效处：**

1. 练习本不是一人一辈子一本。终态（`FINISH` / `INVALID` / `TERMINATION`）会把生成列 `open_user_id` 变成 NULL，名额腾出来，同一人可以再开一本新的。进行中的四态才占唯一键。
2. `WAITING` 仍算「进行中」，`current` 看得到，但窗口不再借铅笔（不可 `save`/`submit`）。
3. `probe` 的灯不是「我的本子」。别人正在用同一个信用代码办，你也会看到 `IN_PROGRESS`。它要登录和 `profile:enterprise:apply`，不是匿名查询。
4. 老师批完（`publishApproved`）不是 submit 的返回值，更不是 callback 的返回值。submit 的 VO 状态是 `WAITING`。E2E 在 submit 之后还要另调 `handleProcessEvent("finish")` 才会出现 `profile_enterprise`。
5. 默认供应商 `manual` 是人工复核。门铃按了，`manual` 直接说「本站不收快递」→ `UNSUPPORTED_CALLBACK`。测试里的 `test-provider` 才验 HMAC。
6. 分层比喻里的「柜员」字段名叫 `service`，实际类型是 UseCase。核身用例的字段名叫 `coordinator`，类型是端口 `EnterpriseVerificationService`。不要被变量名带去 classic。
7. 个人那扇窗没有 probe；企业多这一扇。不要把 L-035 的三枪表直接复读成本课四枪。

## 核心概念与机制

### 直觉讲解

小孩子版只记十四句：

1. **先数层。** 登记表：`wta-modules/wta-profile` = layered。能跑五层检查的房间是 `wta-profile-enterprise`。强制链是 `Controller/Listener → UseCase → Service → DAO → Mapper → XML`。架构测试 `enforcesTheFiveLayerDependencyDirection` 会拆掉越层 import。
2. **四扇自助窗，一扇门铃。** GET 根路径 = `current`。POST 根路径 = `save`。POST `/submit` = `submit`。POST `/probe` = `probe`。门铃是另一块牌子 `/profile/enterprise/verification/providers/{providerCode}/callback`。没有 PUT，没有 DELETE，自助窗没有路径上的 `{id}`。
3. **主人只来自登录口袋——probe 除外。** `current` / `save` / `submit` 调 `LoginHelper.getUserId()`。`EnterpriseApplicationSaveBo` 的 record 组件名测试锁死**不含** `userId` / `applicantUserId`。`probe` 连口袋都不掏。
4. **自助权限就一把钥匙。** 四个方法都是 `@SaCheckPermission("profile:enterprise:apply")`。`self` 包里的 Controller **禁止** `@SaIgnore`。`@SaIgnore` 只允许出现在包名以 `.anonymous` 结尾、类名带 `Anonymous` 的控制器上。
5. **读 GET，写 POST。** `current` 无 `@Log`。`save` 的 `@Log` 标题「保存企业实名认证申请」、`BusinessType.UPDATE`。`submit` 标题「提交企业实名认证申请」、`BusinessType.OTHER`。`probe` 标题「探测企业认证状态」、`BusinessType.OTHER`。三扇写窗以及门铃 `isSaveRequestData=false` 且 `isSaveResponseData=false`——信用代码、法人证件、供应商签名不许进操作日志。
6. **事务章盖在 UseCase 上。** `current` / `save` / `submit` / `probe` / `handleProcess` 以及核身 `callback` 都有 `@DSTransactional`。连只读的 `current` 和只读的 `probe` 也盖了章。Service 方法本身**没有**这颗注解。HTTP 合同测试只锁死 save/submit/handleProcess 三颗章；磁盘上 current/probe 同样有。
7. **进行中 ≠ 可编辑。** 开着的状态：`DRAFT` `BACK` `CANCEL` `WAITING`。可编辑：前三个。`EnterpriseApplication.editable()` 和 Service 的 `EDITABLE_STATUSES` 是同一组。
8. **`current` 是张望。** `requireUserId` → `selectOpenByUserId`（**不加** `for update`）→ 有就 `EnterpriseApplicationVo.from`，没有就 `null`。
9. **`probe` 是占用灯。** 规范化信用代码 → 校验统一社会信用代码格式 → `selectProbeStatus` → 只回 `{ status }`。四个值：`IN_PROGRESS` / `BOUND` / `UNBOUND` / `AVAILABLE`。
10. **`submit` 是复印+排队。** 必须先有打开的可编辑本子。版本对不上就冲突。字段必须齐全。材料 `ALWAYS` 标签必须齐；办理人不是法定代表人时还要 `HANDLER_NOT_LEGAL_REPRESENTATIVE`。然后才写 `profile_enterprise_submission`，把工作副本改成 `WAITING`。
11. **身份键就是大写信用代码。** `EnterpriseIdentityFields.normalize`：信用代码 `strip` 再 `Locale.ROOT` 大写，同时当作 `identityKey`。邮箱改小写。VO **不**把 `identityKey`、`applicantUserId`、`targetProfileId` 送出窗口。
12. **自助失败用类别码说话。** 异常消息就是 `ENTERPRISE_APPLICATION_VERSION_CONFLICT` 这种码。Advice 原样放进 `R.msg`。E2E 断言 HTTP `isOk()` 且 `$.msg` 为码。
13. **门铃失败改 HTTP 状态。** 签名错、太晚 → 401；其它类别 → 400。JSON `msg` 是英文句子 `Enterprise verification callback rejected`，类别在 `data.category`。
14. **工作流挂了，草稿还在。** `SpringEnterpriseWorkflowGateway.start`：没有 `WorkflowService` 或没有 `profile.enterprise.flowCode` → `ENTERPRISE_WORKFLOW_UNAVAILABLE`；`startCompleteTask` 失败 → `ENTERPRISE_WORKFLOW_START_FAILED`。E2E 在失败后 `session.rollback()`，断言申请仍是 `DRAFT`、submission 0 行、核身尝试 0 行。

**类比补一句：** 把 `expectedVersion` 想成练习本封面上的**页码章**。你改字时要把看到的那一页页码交回去。别人先交了，页码已经跳了，你还拿旧页码来，窗口就说 `ENTERPRISE_APPLICATION_VERSION_CONFLICT`。这不是「再试一次 GET 就会自动提交」。

### 精确定义与 English term

| 中文说法 | English term | 磁盘定义 |
| --- | --- | --- |
| 企业自助申请控制器 | `EnterpriseApplicationController` | `controller.self`。前缀 `/profile/enterprise/application`。四方法：`current` / `save` / `submit` / `probe` |
| 查询当前进行中申请 | `current` | `GET` 根路径。`R<EnterpriseApplicationVo>`。无 body。`LoginHelper.getUserId()` |
| 保存草稿 | `save` | 同前缀 `POST`。body `EnterpriseApplicationSaveBo`。矩阵格子写了它 |
| 提交申请 | `submit` | `POST /submit`。body `EnterpriseApplicationSubmitBo(expectedVersion)`。只把 int 传给 UseCase |
| 探测占用态 | `probe` | `POST /probe`。body `EnterpriseApplicationProbeBo(unifiedCreditCode)`。**不**传 userId。返回 `EnterpriseApplicationProbeVo(status)` |
| 企业核身匿名控制器 | `EnterpriseVerificationAnonymousController` | `controller.anonymous`。前缀 `/profile/enterprise/verification/providers`。一方法 `callback` |
| 供应商回调 | `callback` | `POST /{providerCode}/callback`。`@SaIgnore`。body 四字段信封。返回 `EnterpriseVerificationCallbackOutcome` |
| 分层模块 | layered module | 登记表：`wta-modules/wta-profile`。Controller 不得持有 Service；Service 不得 import Mapper；DAO 才 `@Repository` 持有 Mapper |
| 申请用例 | `EnterpriseApplicationUseCase` | 接口。无参 `current()`/`save`/`submit` 标 `@Deprecated`，默认抛「请传入 userId」。`probe` 是唯一非 default 方法。生产实现是 `EnterpriseApplicationUseCaseImpl` |
| 核身用例 | `EnterpriseVerificationUseCase` | 只有 `callback(providerCode, envelope, receivedAt)` |
| 领域服务 | `EnterpriseApplicationService` | 具体类，实现 `EnterpriseApplicationPublicationPort`。不实现 UseCase，也不实现 `IEnterpriseApplicationService` |
| 核身协调器 | `EnterpriseVerificationAttemptService` | 实现端口 `EnterpriseVerificationService`。UseCase 字段名叫 `coordinator` |
| 进行中申请 | open application | SQL：`status in ('DRAFT','BACK','CANCEL','WAITING') and del_flag='0'` |
| 可编辑申请 | editable application | `DRAFT` / `BACK` / `CANCEL`。`WAITING` 只读 |
| 乐观锁版本 | `version` / `expectedVersion` | 表列 `version`；HTTP 入参 `expectedVersion`；VO 出参也叫 `version` |
| 提交序号 / 快照版本 | `submissionSeq` / `snapshotVersion` | 表列 `submission_seq`；VO 字段名 `snapshotVersion`；submit 时 `submissionSeq + 1` |
| 身份键 | `identityKey` | 规范化后的统一社会信用代码。表上有列；VO 没有这个字段 |
| 占用灯状态 | probe status | `IN_PROGRESS` / `BOUND` / `UNBOUND` / `AVAILABLE` |
| 工作副本表 | `profile_enterprise_application` | 可改的申请本。生成列保证进行中每人/每身份唯一 |
| 不可变提交快照 | `profile_enterprise_submission` | 每次 submit 一行 |
| 核身尝试表 | `profile_verification_attempt` | 与个人共用表，本模块写入 `profile_type='ENTERPRISE'` |
| 回调结果 | `EnterpriseVerificationCallbackOutcome` | 枚举：`ACCEPTED`、`IDEMPOTENT`、`LATE_IGNORED` |
| 回调失败类别 | `EnterpriseVerificationFailureCategory` | 例如 `INVALID_SIGNATURE`、`EXPIRED_CALLBACK`、`CONFLICTING_CALLBACK` |
| 统一响应 | `R<T>` | 成功 `code=200`；`R.fail` 的 `code=500`。自助 Advice 不改 HTTP 状态；门铃 Advice 改 |
| 业务类别码 | `EnterpriseApplicationException` | `getMessage()` 就是类别字符串，例如 `ENTERPRISE_APPLICATION_NOT_EDITABLE` |

### 机制/因果链

#### 1. 窗口柜员：四扇自助窗怎么接线

`EnterpriseApplicationController` 不 `extends BaseController`。构造注入一个字段：

```java
private final EnterpriseApplicationUseCase service;
```

口试时先说类型，再说字段名。四个方法共用权限 `profile:enterprise:apply`。HTTP 合同测试还断言：每个声明方法都有这把权限；凡是 `PostMapping` 都必须有 `@Log` 且两头都不存包体；`SaveBo` 组件名不含 `userId` / `applicantUserId`；`ProbeVo` 组件名恰好只有 `status`。

| Java 方法 | HTTP | Body | 传给 UseCase 的参数 |
| --- | --- | --- | --- |
| `current()` | `GET /profile/enterprise/application` | 无 | `service.current(LoginHelper.getUserId())` |
| `save(EnterpriseApplicationSaveBo)` | `POST /profile/enterprise/application` | 企业字段 + `expectedVersion` | `service.save(userId, command)` |
| `submit(EnterpriseApplicationSubmitBo)` | `POST /profile/enterprise/application/submit` | `{ expectedVersion }` | `service.submit(userId, command.expectedVersion())` |
| `probe(EnterpriseApplicationProbeBo)` | `POST /profile/enterprise/application/probe` | `{ unifiedCreditCode }` | `service.probe(command)` —— **没有** userId |

`EnterpriseApplicationSubmitBo` 是单字段 record，`@PositiveOrZero int expectedVersion`。Controller **不**把整个 BO 往下传，只拆出 int。`EnterpriseApplicationProbeBo` 是 `@NotBlank @Size(max = 64) String unifiedCreditCode`。

前端厨房（格子归 L-044，本课只作旁证）`createEnterpriseApplicationService` 四枪 URL 与上表一致：`current` GET 根、`save` POST 根、`submit` POST `/submit` 且 `data: { expectedVersion }`、`probe` POST `/probe` 且 `data: { unifiedCreditCode }`。`profileEnterpriseApplicationResource.controller` 字符串就是 `'EnterpriseApplicationController'`。

OpenAPI 快照 `openapi.ts` 把同一前缀拆成 GET=`current_1`、POST=`save_4`、`/submit` POST=`submit_2`、`/probe` POST=`probe_1`。没有 PUT/DELETE。

#### 2. 办事员：事务章和废弃入口

`EnterpriseApplicationUseCase` 接口留着无 `userId` 的 default 方法，全部 `@Deprecated`，调用就抛 `UnsupportedOperationException("请传入 userId")`。带 `userId` 的 default 会回落到无参版本——所以**新入口必须覆盖带 userId 的方法**。`probe` 在接口上不是 default，生产必须实现。`EnterpriseApplicationUseCaseImpl` 覆盖了五枪：`current(long)` / `save(long, BO)` / `submit(long, int)` / `probe(BO)` / `handleProcess`，每一枪 `@DSTransactional`。

`handleProcess` 不是 HTTP。Listener 收到 WarmFlow 的 `ProcessEvent` 后构造 `EnterpriseApplicationProcessCommand`（实例号、业务号、flowCode、status、`profileDecision`、`snapshotVersion`、时间），再交给这一枪。flowCode 对不上或 businessId 不是正数，Service 直接 return，不炸。

测试适配器 `EnterpriseApplicationServiceImpl`（仅 test）同时实现 UseCase 和 `IEnterpriseApplicationService`，无参方法内部再调 `LoginHelper.getUserId()`。E2E 把这个适配器直接塞进 Controller，那是测试捷径。生产路径不走这个类。架构测试规定 production service **不得**以 `ServiceImpl` 结尾。

#### 3. `current`：张望进行中的本子

```text
requireUserId(userId)          userId <= 0 → ENTERPRISE_USER_INVALID
findOpenByUserId
  dao.selectOpenByUserId       无 for update
  映射成 EnterpriseApplication
map(EnterpriseApplicationVo::from) 或 null
Controller: R.ok(voOrNull)
```

SQL 选出的状态集合包含 `WAITING`。所以排队中的申请，刷新页面还在。终态本子这句 SQL 看不见——`current` 不会把已办结的档案申请再递出来。

VO 字段：`enterpriseApplicationId` / `status` / 企业展示字段（名称、信用代码、类型、法人、证件、办理人是否法人、成立日、营业期限、地址、范围、联系人、电话、邮箱、注册资本、行业、网站）/ `providerCode` / `snapshotVersion` / `version` / `submittedTime` / `finishedTime`。**没有** `applicantUserId`、`identityKey`、`targetProfileId`。窗口给人看的是表单，不是内部锁。

#### 4. `save`：铅笔，且会把 BACK/CANCEL 写回 DRAFT

因果链：

1. `requireUserId`。
2. `EnterpriseIdentityFields.normalize(command)`：null command → `ENTERPRISE_DRAFT_REQUIRED`；空白当 null；信用代码/企业类型/法人证件类型号码/行业编码大写；邮箱小写；身份键就是大写信用代码。
3. `validateDraft`：长度上限；若填了信用代码必须通过 `ValidationFormat.UNIFIED_SOCIAL_CREDIT_CODE`；若填了法人证件类型，号码要匹配 `profile_document_type` 的 `numberPattern`；邮箱格式；注册资本不能为负；成立日不能晚于 `Clock` 的今天；营业期限起不能晚于止。草稿**不**要求字段齐全，也**不**要求营业期限盖住今天。
4. 已有进行中且 `!editable()`（也就是 `WAITING`）→ `ENTERPRISE_APPLICATION_READ_ONLY`。
5. 供应商：已有本子沿用它的 `providerCode`；新建则读配置 `profile.enterprise.provider.default`，空则 `ENTERPRISE_PROVIDER_NOT_CONFIGURED`。`providers.requireEnabled` 失败被收成 `ENTERPRISE_PROVIDER_UNAVAILABLE`。
6. 目标档案：账号已有有效绑定（`ACTIVE`/`SUSPENDED`）则盯着那份档案；否则若身份键已有 ACTIVE 档案就盯那份。单测 `savesCompleteEnterpriseDraftWithoutPrefillingAnExistingProfile`：账号未绑、信用代码已有档案 9201 时，`targetProfileId` 钉在 9201。
7. `saveDraft`：`lockOpenByUserId`。没有行时 `expectedVersion` 必须是 0，否则冲突；`insertApplication` 写死 `status='DRAFT'`、`submission_seq=0`、`version=0`。有行时状态必须可编辑且版本相等，否则 `ENTERPRISE_APPLICATION_VERSION_CONFLICT`；然后 `updateDraft`：`status='DRAFT'`，`version = version + 1`，WHERE 带旧 `version` 且 `applicant_user_id`。撞唯一键 → `ENTERPRISE_APPLICATION_IDENTITY_CONFLICT`。`changed != 1` → 创建冲突或版本冲突。

`updateDraft` 把 `BACK`/`CANCEL` 拉回 `DRAFT`。WAITING 走不到这里，前面已经被 `READ_ONLY` 拦住。

#### 5. `probe`：占用灯，不写库，不认口袋

这是企业相对个人申请窗多出来的那一扇。因果链：

1. command 为 null → `ENTERPRISE_PROBE_REQUIRED`。
2. `normalizeCredit`：strip + 大写；空则后面当无效。
3. `ValidationUtils.isValid(..., UNIFIED_SOCIAL_CREDIT_CODE)` 失败 → `ENTERPRISE_CREDIT_CODE_INVALID`。
4. `dao.selectProbeStatus(identityKey)`，CASE 按这个顺序：
   - 存在进行中申请（四态、未删、`identity_key` 相等）→ `'IN_PROGRESS'`
   - 存在 ACTIVE 企业档案，且该档案有 `ACTIVE`/`SUSPENDED` 绑定 → `'BOUND'`
   - 存在 ACTIVE 企业档案（没有上面那种绑定）→ `'UNBOUND'`
   - 否则 `'AVAILABLE'`
5. 返回 `new EnterpriseApplicationProbeVo(status)`。单测 `probeReturnsOnlyTheMinimalStatus` 还断言入参前后空白会被剥掉、大小写不敏感。

probe **不**锁行、**不**看当前用户、**不**返回企业名称或申请人。墙上只有一盏灯。权限仍要 `profile:enterprise:apply`，所以这不是给路人的公开查询。

#### 6. `submit`：复印、贴材料、改 WAITING、再按门铃

这是 OBJ-41 要能顺着说完的主链。Service.submit 的顺序以磁盘为准，不要把「先开流程再写库」说成事实：

1. `requireUserId`。
2. `lockOpenByUserId`（`for update`）。没有行 → `ENTERPRISE_APPLICATION_NOT_FOUND`。
3. 申请人必须是当前用户，且状态 ∈ `DRAFT/BACK/CANCEL`，否则 `ENTERPRISE_APPLICATION_NOT_EDITABLE`。（`WAITING` 走到这里也会被挡。）
4. `application.version() != expectedVersion` → `ENTERPRISE_APPLICATION_VERSION_CONFLICT`。
5. `validateComplete`：草稿规则 + 企业名称/信用代码/类型/法人姓名/法人证件类型号码/成立日/注册地址/经营范围都必填 + 证件规则的号码再验一次 + 完整模式下若填了营业期限，必须盖住今天，否则 `ENTERPRISE_BUSINESS_TERM_EXPIRED`。
6. `requireProviderEnabled`。
7. `requireSubmissionAllowed(userId, targetProfileId, identityKey)`：
   - 身份键空 → `ENTERPRISE_IDENTITY_REQUIRED`。
   - 账号已有 `ACTIVE` 或 `SUSPENDED` 绑定 → `ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE`。（持久化单测锁死这条：普通申请不能替换任何有效负责人账号。）
   - 该信用代码的 ACTIVE 档案已有有效绑定（含 `SUSPENDED`）→ `ENTERPRISE_RESPONSIBLE_ALREADY_BOUND`。
   - 档案在，但目标档案对不上 → `ENTERPRISE_IDENTITY_CONFLICT`。
8. 材料闸：所有者是 `WORKING` + 申请主键。文档类型码传 `"*"`。标签集合至少 `ALWAYS`；`handlerIsLegalRepresentative == false` 时再加 `HANDLER_NOT_LEGAL_REPRESENTATIVE`。单测 `requiresAuthorizationLetterOnlyForANonLegalRepresentativeHandler` 锁死这组差集。
9. `snapshotVersion = submissionSeq + 1`。时钟 `clock.instant()`（生产 UTC）。
10. `insertSubmission`：字段快照 JSON + 列拷贝。唯一键冲突 → `ENTERPRISE_SUBMISSION_CONFLICT`。
11. `materials.snapshotImmutable(WORKING → SUBMISSION)`。
12. `markWaiting`：SQL 要求 `submission_seq = 新序号 - 1`、状态仍可编辑、`version = expectedVersion`。成功后 `version + 1`，`status='WAITING'`，清掉旧决定字段。对不上 → 版本冲突。
13. `startVerificationAttempt`：指纹是 `SHA-256(identityKey + "\n" + snapshotVersion)`。供应商异常收成 `ENTERPRISE_PROVIDER_UNAVAILABLE`。
14. `workflow.start(applicationId, submissionId, snapshotVersion)`。网关把变量 `profileType=ENTERPRISE`、`snapshotVersion`、`submissionId` 塞进 `StartProcessDTO`，`businessId` 是申请主键字符串，`flowCode` 来自配置。
15. 返回 `EnterpriseApplicationVo.from(waiting)`，状态 `WAITING`，`snapshotVersion` 已是新序号。

因为 UseCase 整段在一个 `@DSTransactional` 里，13/14 抛错会把 10/11/12 一起撤掉。E2E `rollsBackTheSubmissionSnapshotWhenWorkflowCannotStart` 是这条因果链的铁证：HTTP 仍 200，`$.msg=ENTERPRISE_WORKFLOW_UNAVAILABLE`，回滚后草稿还在、submission 0、attempt 0。

#### 7. 提交之后（本课边界，只为把 WAITING 说完）

`handleProcess` 不是 current/save/submit/probe 的返回路径，但口试常被问「提交完档案怎么出来的」。最短正确说法：

- 事件 `flowCode` 必须等于配置 `profile.enterprise.flowCode`，否则忽略。
- 申请必须仍是 `WAITING` 且 `submissionSeq == snapshotVersion`，否则忽略（含迟到的 finish）。单测 `finishesOnlyTheCurrentSnapshotAndPublishesVersionMaterials`：当前快照是 3 时，迟到的 finish(2) 不会 `publishApproved`。
- `FINISH` 且决定 `REJECT` → 把申请打成 `INVALID`，不发布。
- `FINISH` 且非拒绝 → `publishApproved`：写版本、更新档案、**插入恰好一份**负责人绑定和绑定事件（原因 `ENTERPRISE_VERIFICATION_APPROVED`）；材料从 SUBMISSION 再冻到 VERSION。
- `BACK`/`CANCEL`/`INVALID`/`TERMINATION` → 只改申请状态，不发布。

管理员在 L-040 的 `decide` 是另一张桌子；流程引擎打回来的才走 Listener。本课 submit 只负责把本子推进 `WAITING` 并按门铃。

`publishApproved` 里如果申请人此时已经有有效绑定，仍抛 `ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE`。档案已有负责人同理。没有 ACTIVE 档案时可以新建；若有已吊销档案，新档案的 `previous_profile_id` 指向它（successor）。E2E `publishesExactlyOneResponsibleThenAllowsUnboundReauthenticationAndRevokedSuccessor` 把这条人生走完：先占坑 → 第二人撞 `ENTERPRISE_RESPONSIBLE_ALREADY_BOUND` → 解绑后再认证仍是同一档案新版本 → 吊销后再办是新档案。

#### 8. 数据库怎么保证「进行中只有一本」

表 `profile_enterprise_application` 有两列生成列（实体上 `FieldStrategy.NEVER`，Java 不写）：

- `open_user_id` = 进行中四态且未删时的 `applicant_user_id`，否则 NULL。唯一键 `uk_profile_enterprise_application_open_user`。
- `open_identity_key` = 同样条件下的 `identity_key`。唯一键 `uk_profile_enterprise_application_open_identity`。

E2E `databaseAllowsOnlyOneOpenEnterpriseApplicationPerIdentityAndAccount`：两个会话同时插入进行中申请，同一信用代码或同一账号，只能活一行，另一行唯一冲突。这是表自己当裁判，不是 Service 先 SELECT 再碰运气。

`markWaiting` 不释放这两列——`WAITING` 仍占坑。所以排队时别人不能用同一信用代码再开一本，你自己也不能再开第二本。probe 的 `IN_PROGRESS` 就是看见这个坑还占着。

#### 9. 门铃柜员：`callback` 怎么接线

`EnterpriseVerificationAnonymousController` **不是** `@RequiredArgsConstructor`。它显式接收两份依赖：`EnterpriseVerificationUseCase` 和 `EnterpriseVerificationTimeSource`。生产时间源是 `SystemEnterpriseVerificationTimeSource`，`now()` 等于 `Instant.now()`。测试把 `() -> NOW` 塞进去，好让过期窗口可重复。

```text
POST /profile/enterprise/verification/providers/{providerCode}/callback
  @SaIgnore
  @Log 标题「企业认证供应商回调」，不存包体
  @Valid CallbackRequest
      providerRequestId   @NotBlank @Size(max=128)
      timestampEpochSecond @Positive
      payload             @NotBlank @Size(max=1048576)
      signature           @NotBlank @Size(max=1024)
        │
        v
  envelope = (requestId, timestamp, payload, signature)
  receivedAt = timeSource.now()     不是登录时间，不是 body 里的钟
  useCase.callback(providerCode, envelope, receivedAt)
        │  @DSTransactional
        v
  coordinator.handleCallback(...)
```

`providerCode` 是路径上的字符串，BeanValidation 合同只给数值型 `@PathVariable` 加 `@Positive`；编码合法性由注册表用正则 `[a-z0-9][a-z0-9_-]{0,63}` 裁决。未知 → `UNKNOWN_PROVIDER`；未启用 → `DISABLED_PROVIDER`；格式差 → `INVALID_PROVIDER_CODE`。

#### 10. `handleCallback`：验签、幂等、迟到、冲突

```text
providerRegistry.requireEnabled(providerCode)
provider.authenticate(envelope, receivedAt)
  失败（含签名/过期/不支持）→ 记安全审计（applicationId 可能仍是 null）→ 原样抛
lockByProviderRequest(providerCode, providerRequestId)
  没有行 → ATTEMPT_NOT_FOUND（审计后抛）
lockApplication(attempt.applicationId)
  申请上的 providerCode 对不上路径 → PROVIDER_MISMATCH
  申请已终态 FINISH/INVALID/TERMINATION
        → 审计 LATE_CALLBACK，返回 LATE_IGNORED（HTTP 200，不 complete）
  尝试已不是 PENDING
        同一份证据 → IDEMPOTENT（HTTP 200）
        不同证据 → CONFLICTING_CALLBACK（审计后抛，HTTP 400）
  仍是 PENDING → completeAttempt（WHERE status='PENDING'）
        → ACCEPTED（HTTP 200）
```

测试供应商 `EnterpriseDeterministicTestProvider` 把规范串定为 `providerRequestId + "." + timestamp + "." + payload`，HMAC-SHA256 对比签名；时钟偏差超过 300 秒 → `EXPIRED_CALLBACK`。生产默认 `manual` 的 `authenticate` **直接拒绝**，类别 `UNSUPPORTED_CALLBACK`（走 400 分支）。

`complete` 用 `EnterpriseVerificationEvidenceCodec` 把 digest 和证据 JSON 编进去。E2E 断言落库的 `provider_evidence_json` **不含** `"signature"`。回调**不**改 `profile_enterprise_application.status`，**不**插绑定。合同单测名字就叫 `authenticatesAndHandlesIdempotentConflictingAndLateCallbacksWithoutPublishing`。

失败 Advice：

```text
INVALID_SIGNATURE, EXPIRED_CALLBACK  → HTTP 401 UNAUTHORIZED
其它类别                             → HTTP 400 BAD_REQUEST
body: R.fail("Enterprise verification callback rejected", { category })
```

安全审计写入 `profile_operation_audit`：`profile_type='ENTERPRISE'`，`operation_type='PROVIDER_CALLBACK'`，`operator_user_id=0`，`capability='profile:enterprise:providerCallback'`。`LATE_CALLBACK` 的 result 是 `IGNORED`，其它失败是 `FAILED`。

### 图、表或文本图

**图 1. 宏观五层、四扇自助窗、一扇门铃**

```text
 HTTP self 窗                                      不是本窗
 GET  /application          current                 ProcessEvent ──► Listener
 POST /application          save                           │
 POST /application/submit   submit                         v
 POST /application/probe    probe                   UseCase.handleProcess
        │
        v
 EnterpriseApplicationController ──► UseCaseImpl (@DSTransactional)
        │
        v
 EnterpriseApplicationService
        │  不持有 Mapper
        v
 EnterpriseApplicationDao ──► EnterpriseApplicationMapper.xml
        │
        ├─ profile_enterprise_application   工作副本
        └─ profile_enterprise_submission    不可变复印件

 HTTP anonymous 门铃
 POST /verification/providers/{code}/callback   callback
        │  @SaIgnore
        v
 EnterpriseVerificationAnonymousController ──► VerificationUseCaseImpl
        │
        v
 EnterpriseVerificationAttemptService
        │
        ├─ profile_verification_attempt     核身尝试
        └─ profile_operation_audit          门铃失败/迟到
```

- **alt：** current/save/submit/probe 从 self Controller 进入申请 UseCase 事务；callback 从 anonymous Controller 进入核身 UseCase；工作流事件从 Listener 进同一申请 UseCase。
- **caption：** 图 1——OBJ-41 的分层空间。矩阵 A 是四枪自助加一枪回调。
- **文字等价物：** 自助 HTTP 只有这一份 Controller。门铃是另一份。它们都不碰表。事务在 UseCase。规则在 Service。SQL 在 XML。Listener 和 Controller 是同一层的两种入口。
- **图的边界：** 不画 `EnterpriseAdminController`、不画 `EnterpriseTransferController`、不画材料 attach 的另一份 Controller。不把 `IEnterpriseApplicationService` 画进生产箭头。不保证 WarmFlow 内部任务长什么样。

**图 2. submit 成功才把本子改成 WAITING；失败停在哪一层**

```text
submit(expectedVersion):
  无进行中行                    ──► ENTERPRISE_APPLICATION_NOT_FOUND
  不是本人或状态不是可编辑      ──► ENTERPRISE_APPLICATION_NOT_EDITABLE
  version 对不上                ──► ENTERPRISE_APPLICATION_VERSION_CONFLICT
  字段不齐 / 证件规则失败       ──► ENTERPRISE_FIELDS_INCOMPLETE 等
  供应商不可用                  ──► ENTERPRISE_PROVIDER_UNAVAILABLE
  账号已是某企业负责人          ──► ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE
  该企业已有负责人（含暂停）    ──► ENTERPRISE_RESPONSIBLE_ALREADY_BOUND
  ALWAYS / 授权书材料不齐       ──► 材料端口异常（本课不拆）
  insertSubmission 撞序号       ──► ENTERPRISE_SUBMISSION_CONFLICT
  markWaiting 条件不中          ──► ENTERPRISE_APPLICATION_VERSION_CONFLICT
  startAttempt 失败             ──► ENTERPRISE_PROVIDER_UNAVAILABLE  （事务回滚）
  workflow 缺 Bean/缺 flowCode  ──► ENTERPRISE_WORKFLOW_UNAVAILABLE （事务回滚）
  startCompleteTask 失败        ──► ENTERPRISE_WORKFLOW_START_FAILED（事务回滚）
  全绿                          ──► VO.status=WAITING, snapshotVersion=旧seq+1
                                   此时还没有 profile_enterprise 新档案
```

- **alt：** submit 在写快照和改 WAITING 之后才启动核身与工作流；后两步失败会回滚前面的写。
- **caption：** 图 2——submit 的失败停点。current / probe 不在这张图上，因为它们不写库。
- **文字等价物：** 先锁、再验、再复印、再贴「等待」。门铃按不响，复印作废。不要说「流程失败了但快照留下当审计」。
- **图的边界：** 不保证核身供应商 HTTP。不保证管理员 `decide` 的按钮文案。材料缺件的具体类别码归 L-043。

**图 3. current 看见什么状态；probe 看见什么灯**

```text
status（自己的本子）
 DRAFT ----save----► DRAFT          current 看得到，可编辑
 BACK  ----save----► DRAFT          current 看得到；save 会拉回 DRAFT
 CANCEL----save----► DRAFT          同上
 WAITING  (submit 出口)             current 看得到，不可 save/submit
 FINISH / INVALID / TERMINATION     current 的 SQL 看不见（生成列释放）

probe 灯（任意信用代码，不看 userId）
 进行中申请存在          → IN_PROGRESS
 ACTIVE 档案 + 有效绑定  → BOUND
 ACTIVE 档案、无人认领   → UNBOUND
 都没有                  → AVAILABLE
```

- **alt：** 进行中四态能被 current 查出；终态不能；WAITING 只读。probe 按信用代码问全库占用，不返回表单。
- **caption：** 图 3——current 的可见性、可编辑性、probe 的占用灯是三件不同的事。
- **文字等价物：** 「进行中」是给唯一键和 GET 用的集合。「可编辑」少了 WAITING。「终态」离开 current 的眼睛，也离开唯一键。probe 可以看见别人的进行中，也可以看见别人已经绑上的企业，但它只回一个单词。
- **图的边界：** 不解释管理员如何把 WAITING 审成 FINISH。那是 L-040 + `handleProcess`。不解释解绑后灯如何从 BOUND 变成 UNBOUND——解绑是 L-042。

**图 4. 门铃三态与失败 HTTP**

```text
callback:
  验签失败 / 时钟偏差>300s（测试供应商） ──► 401  data.category=INVALID_SIGNATURE|EXPIRED_CALLBACK
  manual.authenticate                     ──► 400  UNSUPPORTED_CALLBACK
  找不到尝试                              ──► 400  ATTEMPT_NOT_FOUND
  路径供应商 ≠ 申请供应商                 ──► 400  PROVIDER_MISMATCH
  申请已终态                              ──► 200  data=LATE_IGNORED   （审计 IGNORED）
  已完成且证据相同                        ──► 200  data=IDEMPOTENT
  已完成且证据不同                        ──► 400  CONFLICTING_CALLBACK
  PENDING 且验签通过                      ──► 200  data=ACCEPTED
                                           申请仍 WAITING，不 publish
```

- **alt：** 成功三态走 HTTP 200 且 data 是枚举名；签名类失败走 401；业务冲突走 400。
- **caption：** 图 4——匿名回调的出口。不要把它画成自助窗那种「HTTP 200 + msg=类别码」。
- **文字等价物：** 门铃只给尝试盖章。第一次盖章是 ACCEPTED，再按同一封信是 IDEMPOTENT，换一封信是冲突，局长已经结案是迟到忽略。假签名不给进门。
- **图的边界：** 300 秒窗口是测试供应商的数字，不是 `manual` 的行为。`manual` 连信封都不拆。

### 正例、反例与边界

**正例 1：** 用户 101 从未开过企业申请。`GET /profile/enterprise/application` → `R.ok(null)`，`data` 为空。`POST /probe` body `{"unifiedCreditCode":"91310000ABCDEF123Y"}` → `{ "status": "AVAILABLE" }`。然后 `POST` 根路径保存草稿 `expectedVersion=0`，供应商配置为 `manual` 且已启用。INSERT 后 VO：`status=DRAFT`，`version=0`，`snapshotVersion=0`，`providerCode=manual`，邮箱被存成小写。再挂上 ALWAYS 材料（办理人是法人，不必授权书），`POST /submit` body `{"expectedVersion":0}`。VO：`status=WAITING`，`snapshotVersion=1`。表上多了一行 submission。`profile_enterprise` 仍 0 行。

**正例 2：** 办理人不是法定代表人。submit 调 `materials.validateRequired(..., "*", {ALWAYS, HANDLER_NOT_LEGAL_REPRESENTATIVE})`。法人自己办则集合只有 `ALWAYS`。

**正例 3：** 合同测试：四方法权限都是 `profile:enterprise:apply`；SaveBo 没有 userId 字段；ProbeVo 只有 status；UseCaseImpl 的 `save`/`submit`/`handleProcess` 带 `@DSTransactional`。匿名回调方法带 `@SaIgnore`。

**正例 4：** 两个人同时用同一信用代码抢进行中坑，或同一账号同时用两个号码抢坑：生成列唯一键只让一行活着。档案表仍空。

**正例 5：** 测试供应商回调：同一 body 第一次 `$.data=ACCEPTED`，第二次 `IDEMPOTENT`；改 payload 再签 → HTTP 400、`$.data.category=CONFLICTING_CALLBACK`；伪造签名 `00` → HTTP 401、`INVALID_SIGNATURE`。申请行仍是 `WAITING`。证据 JSON 里没有 signature。

**正例 6：** E2E 人生：第一人办结后成为唯一 ACTIVE 负责人。第二人 save 成功（草稿不检查负责人闸），submit 停在 `ENTERPRISE_RESPONSIBLE_ALREADY_BOUND`。把绑定改成 `UNBOUND` 后，第二人可以再认证，档案主键不变、版本变成 2。再吊销档案后，第三人办结得到新档案，`previous_profile_id` 指向旧档案。

**反例 1：** 把 `current` 说成「没有申请就 404」。磁盘是 `orElse(null)` + `R.ok`。空是合法答案：桌上没有本子。

**反例 2：** 对 `WAITING` 再 POST `/submit`。状态不在可编辑集合 → `ENTERPRISE_APPLICATION_NOT_EDITABLE`。不要说「幂等提交会返回同一 VO」。

**反例 3：** body 里塞 `userId` 想替别人交。Controller 不读这个字段；SaveBo 根本没有它。权限过了也只动登录者自己的行。`updateDraft` 的 WHERE 还带 `applicant_user_id`。

**反例 4：** 工作流 Bean 缺失时，以为 submission 会留下。E2E 明确：回滚后 submission 0、attempt 0、申请仍 DRAFT。口试若说「至少留快照」，与事务边界相反。

**反例 5：** 把这扇窗的 submit 说成换负责人 / 企业转移。账号已有有效绑定，这里停在 `ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE`，不会改 `profile_enterprise_binding`。转移三枪是 L-042。

**反例 6：** 把 `probe` 说成 GET `/application/{creditCode}` 或匿名查询。它是 POST、要 `profile:enterprise:apply`、返回值只有 status。

**反例 7：** 把 Controller 字段 `service` 画成 classic：`Controller → Service → Mapper`。架构测试会在 Controller 源码里搜到 `import ...service.` 就失败。生产 Service 源码里搜到 `import ...mapper.` 也会失败。

**反例 8：** 把供应商回调说成「HTTP 200 + msg=类别码」，或说成会发布档案。门铃 Advice 改的是 HTTP 状态；成功枚举在 `data`；E2E 锁死不 publish。

**反例 9：** 对生产默认 `manual` 走回调，期望 `ACCEPTED`。`authenticate` 抛 `UNSUPPORTED_CALLBACK`。人工复核没有快递回执这条路。

**边界 1：** `current` 与 `probe` 虽只读，UseCase 仍 `@DSTransactional`。不要发明「读方法无事务」的分层教条；以这份实现为准。HTTP 合同测试没点名这两颗章，不等于磁盘上没有。

**边界 2：** 自助异常 Advice 把类别码放进 `msg`，HTTP 状态仍 200，JSON `code` 才是 500。门铃 Advice 相反：HTTP 401/400，`msg` 是英文句子。前端若只看 HTTP 状态，自助失败会误判成功，门铃失败会当成「真的未授权」。L-044 才讲厨房如何读 `R`。

**边界 3：** `EnterpriseApplicationService` 构造函数有个未使用的 `Object jsonMapper` 参数；真正序列化走 `JsonUtils.toJsonString`。不要口误成「Service 持有 Jackson Mapper」。

**边界 4：** `selectOpenByUserId` 没有 `LIMIT 1`。唯一键保证进行中每人一行；若有脏数据多行，MyBatis 映射行为不在本课保证范围。以 DDL 唯一键为设计意图。

**边界 5：** 核身默认供应商种子是 `manual`（人工复核）。`startAttempt` 仍会记尝试。`manual` 的 `start` 返回 `pending()`，`providerRequestId` 可以为 null。回调窗不是 submit 的 HTTP。

**边界 6：** probe 的 `IN_PROGRESS` 用申请表的 `identity_key` 等值比较；`BOUND`/`UNBOUND` 用档案表 `upper(trim(unified_credit_code))`。两边规范化策略略有差别。口试以 XML 为准，不要发明「一定都 trim」。

**边界 7：** 材料 `validateRequired` 的文档类型参数是 `"*"`，不是法人证件类型码。不要把个人那套「按证件类型要 ALWAYS」原样搬过来。

**边界 8：** `SUSPENDED` 绑定对 submit 仍算「有效负责人」。probe 的 `BOUND` 同样包含 `SUSPENDED`。暂停不是空位。

## 变式与迁移

- **打回再交。** `BACK` 仍占进行中唯一键。current 看得到。必须先 save（拉回 DRAFT 并 +version）再 submit。直接拿打回前的旧 version 会冲突。
- **已有档案、无人认领。** probe 亮 `UNBOUND`。save 会把 `targetProfileId` 钉在那份 ACTIVE 档案上。submit 在档案没有有效绑定时可以通过负责人闸。发布时更新同一档案、插入恰好一份绑定。
- **档案已被占用。** probe 亮 `BOUND`。第二人仍可 save 自己的草稿（草稿不跑负责人闸），submit 才停在 `ENTERPRISE_RESPONSIBLE_ALREADY_BOUND`。不要在 ApplicationController 上发明第四扇「抢绑」窗。迁移到 L-042 的 send/confirm/unbind。
- **自己已经是某企业负责人。** submit 在 `requireSubmissionAllowed` 第一道闸就停：`ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE`。再认证/转移不是本窗。
- **吊销后继任。** 旧档案 `REVOKED` 后，新申请发布会新建档案并把 `previous_profile_id` 指回去。这是 `publishApproved` 的故事，submit 当时只是再走一遍 WAITING。
- **个人对照。** L-035 的格子写成 `current, submit`——个人没有 probe，save 只是同门邻居。个人身份键是 `TYPE:NUMBER`；企业身份键是大写信用代码。个人被占用时喊 `PERSON_REBIND_CONFIRMATION_REQUIRED` 去 L-036；企业被占用时喊 `ENTERPRISE_RESPONSIBLE_ALREADY_BOUND` 去 L-042。个人回调是 L-038；企业回调钉在本课。
- **门铃对照。** 门牌从 `/profile/person/verification/providers` 换成 `/profile/enterprise/verification/providers`。信封字段同形：requestId / timestamp / payload / signature。尝试表用 `profile_type` 列分开，不是两张物理表。
- **前端对照。** `createEnterpriseApplicationService` 四枪 URL 与本课 HTTP 一一对应。probe 走 `raw` 再 `projectStatusProbeResponse`。本课不认前端格子。
- **classic 对照。** L-031 那种 `Controller → Service → @Select Mapper` 不适用于 profile。这里 XML 语句 id 必须与 Mapper 方法一一对应，架构测试禁止注解 SQL。
- **发布对照。** submit 成功 ≠ 档案存在。callback 成功 ≠ 档案存在。把「主路径 EnterpriseApplication.submit → admin decide」说完整，需要 L-040 的决定半段。本课只保证 submit 把本子送进 WAITING 并按门铃，callback 只给尝试盖章。

## 常见误区

1. **把 `current` 当成详情 GET `/application/{id}`。** 没有 id。只能看「我的进行中那一本」。办结了就变 null。
2. **把 `WAITING` 排除出 current。** SQL 包含它。排队中刷新页面，本子还在，只是铅笔被收走。
3. **以为 submit 的 body 还要再交一遍企业字段。** SubmitBo 只有版本号。完整字段必须已经在工作副本上，来自上次 save。
4. **以为 `probe` 是 current 的过滤参数。** 两扇窗。一个看自己的本子，一个看信用代码占用灯。probe 不返回表单。
5. **以为分层里 UseCase 只是空转发，所以可以删掉。** 事务注解在 UseCase。删掉它，submit 的回滚契约和 callback 的幂等事务就没了盖章位置。Service 无 `@DSTransactional`。
6. **把测试类 `EnterpriseApplicationServiceImpl` 当成生产分层。** 它在 test 源码，还同时实现 UseCase——这是为了老测试和 E2E 接线。生产 Controller 注入的是接口，实现类是 `EnterpriseApplicationUseCaseImpl`。
7. **把自助 `R.fail` 说成 HTTP 500 页面，或把门铃失败说成 HTTP 200。** 自助 E2E 用 MockMvc `status().isOk()`。门铃伪造签名用 `isUnauthorized()`，冲突用 `isBadRequest()`。
8. **把材料校验当成 L-041 的 HTTP。** submit 会喊 `ProfileMaterialPort.validateRequired`，但挂材料的窗是 `EnterpriseMaterialSelfController`（L-043）。没有 ALWAYS 材料，submit 到不了 WAITING。
9. **把 `identityKey` 画进 VO。** 规则内部用它做唯一和冲突；窗口 JSON 没有这一列。
10. **把 Listener 或 callback 画进 submit 时序图的同步返回。** 浏览器在 submit 返回 WAITING 时还没拿到 FINISH，也未必已经收到供应商回执。回写是另一条时间线。
11. **用 classic 的 `IService` / `ServiceImpl` / `QueryWrapper` 来读这张表。** 架构测试在 service 包禁止这些词，DAO 才碰 Mapper。
12. **把企业 probe 和个人 rebind probe 说成同一扇门。** 个人 probe 在 `/profile/person/rebind/probe`（L-036）。企业申请 probe 在 `/profile/enterprise/application/probe`。返回的四态单词碰巧同形（`BOUND`/`IN_PROGRESS`/`UNBOUND`/`AVAILABLE`），门牌和规则不是同一份 SQL。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 打开 `EnterpriseApplicationController`。把四对 mapping 抄成一张表：路径、动词、Java 名、权限串、有没有 `@Log`、Log 是否存包体、是否调用 `LoginHelper.getUserId()`。圈出：GET 是 `current`；POST 根是 `save`；POST `/submit` 是 submit；POST `/probe` **没有** userId。圈字段类型 `EnterpriseApplicationUseCase`。
2. 用手指划 `current` → `UseCaseImpl.current`（`@DSTransactional`）→ `Service.current` → `dao.selectOpenByUserId` → XML 的状态列表。在 `orElse(null)` 旁边写：空是成功。
3. 再划 `submit` → `command.expectedVersion()` → `lockOpenByUserId` → `validateComplete` → `requireSubmissionAllowed` → `validateRequired`（`ALWAYS` ± 授权书）→ `insertSubmission` → `snapshotImmutable` → `markWaiting` → `startAttempt` → `workflow.start`。对照 E2E 方法名 `rollsBackTheSubmissionSnapshotWhenWorkflowCannotStart`。
4. 打开 `selectProbeStatus` 的 CASE。按顺序在纸上写下四盏灯。打开 `EnterpriseApplicationProbeVo`，确认组件名只有 `status`。
5. 打开 `EnterpriseVerificationAnonymousController` 和两份 Advice。抄下：自助失败 HTTP 200/`msg=码`；门铃失败 HTTP 401 或 400/`data.category`。打开 `EnterpriseManualVerificationProvider.authenticate`，在「默认供应商会接回调」旁边划掉。
6. 打开 DDL 里 `profile_enterprise_application` 的两列生成列和两个 unique key。对照实体上 `openUserId` / `openIdentityKey` 的 `FieldStrategy.NEVER`。打开 E2E `databaseAllowsOnlyOneOpenEnterpriseApplicationPerIdentityAndAccount`。

## 总结、词汇表与下一步

- **宏观两扇门。** 自助前缀 `/profile/enterprise/application`：GET 看进行中；POST 根保存草稿；POST `/submit` 交复印件；POST `/probe` 问占用灯。门铃前缀 `/profile/enterprise/verification/providers/{providerCode}/callback`。自助权限一把 `profile:enterprise:apply`。门铃 `@SaIgnore`。主人只来自 `LoginHelper.getUserId()`——probe 除外。
- **分层硬链。** Controller/Listener → UseCase（事务）→ Service（规则）→ DAO（唯一 Mapper 持有者）→ XML。不要把字段名 `service` / `coordinator` 说成 classic。
- **current。** 无锁查询进行中四态；没有就 `R.ok(null)`；VO 不带身份键和申请人 id。
- **save。** 规范化信用代码与邮箱；WAITING 只读；BACK/CANCEL 拉回 DRAFT；新建必须 `expectedVersion=0`。
- **probe。** 不写库、不认口袋；四态灯；格式错是 `ENTERPRISE_CREDIT_CODE_INVALID`。
- **submit。** 锁可编辑行 → 对版本 → 完整校验 → 负责人/身份冲突闸 → ALWAYS（±授权书）材料 → 不可变 submission → WAITING → 核身尝试 → 工作流门铃。后两步失败，整段回滚。
- **callback。** 验签 → 锁尝试 → ACCEPTED / IDEMPOTENT / LATE_IGNORED。不发布档案。`manual` 拒收。失败改 HTTP 状态。
- **进行中唯一。** 生成列 `open_user_id` / `open_identity_key` 在数据库层保证每人/每信用代码一本开着的申请。WAITING 仍占坑。
- **submit ≠ 档案诞生；callback ≠ 档案诞生。** 主路径的后半段是流程回写 / 管理员决定（L-040）。转移是另一扇门（L-042）。
- **格子按磁盘：** `A:EnterpriseApplicationController.current,save,submit,probe` 与 `A:EnterpriseVerificationAnonymousController.callback`。

词汇表：`EnterpriseApplicationController` / `EnterpriseVerificationAnonymousController` / layered / `EnterpriseApplicationUseCase` / `EnterpriseVerificationUseCase` / `@DSTransactional` / `EnterpriseApplicationService` / `EnterpriseVerificationAttemptService` / `EnterpriseApplicationDao` / `EnterpriseApplicationVo` / `EnterpriseApplicationProbeVo` / `EnterpriseApplicationSubmitBo` / `expectedVersion` / `unifiedCreditCode` / `identityKey` / `DRAFT` / `BACK` / `CANCEL` / `WAITING` / `IN_PROGRESS` / `BOUND` / `UNBOUND` / `AVAILABLE` / `ACCEPTED` / `IDEMPOTENT` / `LATE_IGNORED` / `profile_enterprise_application` / `profile_enterprise_submission` / `profile_verification_attempt` / `open_user_id` / `markWaiting` / `EnterpriseApplicationException` / `EnterpriseVerificationFailureCategory` / `profile:enterprise:apply` / `@SaIgnore` / `LoginHelper.getUserId()` / `ENTERPRISE_WORKFLOW_UNAVAILABLE` / `ENTERPRISE_RESPONSIBLE_ALREADY_BOUND` / `UNSUPPORTED_CALLBACK`。

下一步：L-042 走进企业转移 send/confirm/unbind——本课 `ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE` / `ENTERPRISE_RESPONSIBLE_ALREADY_BOUND` 把人推向那扇门。L-043 讲 ALWAYS 与授权书材料如何挂到 WORKING 所有者上，那是 submit 能通过材料闸的前提。L-040 讲管理员决定如何让 WAITING 变成档案和唯一负责人。L-044 才把 `createProfileService` / enterprise 页面接到这五扇 HTTP。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | 工作树 `backend/wta-modules/**` Controller / UseCase | profile-enterprise 公开入口存在 | `sources.md` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-modules/wta-profile` = layered | 当前登记表 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 申请表生成列/唯一键；`profile.enterprise.provider.default=manual`；`profile.enterprise.flowCode=profile_enterprise_verification`；权限种子 `profile:enterprise:apply` | DDL `profile_enterprise_application`；DML 配置/菜单 | 2026-09-16 |
| S-L041-01 | `EnterpriseApplicationController.java`、`EnterpriseApplicationExceptionHandler.java` | 四扇窗、权限、Log 不存包体、`LoginHelper.getUserId()`（probe 除外）、Advice → `R.fail(msg)` | `controller/self`、`controller/advice` | 2026-09-16 |
| S-L041-02 | `EnterpriseVerificationAnonymousController.java`、`EnterpriseVerificationCallbackExceptionHandler.java` | `@SaIgnore` 回调、信封四字段、HTTP 401/400、`data.category` | `controller/anonymous`、`controller/advice` | 2026-09-16 |
| S-L041-03 | `EnterpriseApplicationUseCase.java`、`EnterpriseApplicationUseCaseImpl.java`、`EnterpriseVerificationUseCaseImpl.java` | 废弃无 userId 入口；实现五枪均 `@DSTransactional`；核身 callback 也盖章 | `usecase/` | 2026-09-16 |
| S-L041-04 | `EnterpriseApplicationService.java` | `current`/`save`/`submit`/`probe`/`handleProcess` 顺序、可编辑集合、版本冲突、提交闸、指纹、失败类别码 | `service/EnterpriseApplicationService.java` | 2026-09-16 |
| S-L041-05 | `EnterpriseVerificationAttemptService.java`、`EnterpriseManualVerificationProvider.java`、`EnterpriseVerificationProviderRegistry.java` | 回调 ACCEPTED/IDEMPOTENT/LATE_IGNORED；manual 拒收；注册表启用集合默认 `manual` | `service/`、`adapter/provider/` | 2026-09-16 |
| S-L041-06 | `EnterpriseApplicationDao.java`、`EnterpriseApplicationMapper.xml`、`EnterpriseVerificationAttemptMapper.xml` | DAO 唯一持有 Mapper；open/lock/insert/update/markWaiting/selectProbeStatus；attempt/audit SQL | `dao/`、`resources/mapper/enterprise/` | 2026-09-16 |
| S-L041-07 | `EnterpriseApplication.java`、`EnterpriseIdentityFields.java`、VO/BO 五件套、`EnterpriseVerificationCallbackOutcome.java` | editable/terminal；normalize 把信用代码当身份键；VO 无 identityKey；ProbeVo 只有 status | `domain/application`、`domain/vo`、`domain/bo`、`domain/verification` | 2026-09-16 |
| S-L041-08 | `EnterpriseApplicationHttpContractTest.java`、`EnterpriseModuleArchitectureTest.java`、`EnterpriseVerificationAnonymousControllerTest.java` | 路径/权限/Log/DSTransactional；五层依赖方向；SaIgnore 只在 anonymous；门铃幂等与伪造签名 | `src/test/java/.../controller`、`architecture/` | 2026-09-16 |
| S-L041-09 | `EnterpriseApplicationServiceTest.java`、`EnterpriseApplicationPersistenceTest.java`、`EnterpriseApplicationMySqlE2ETest.java`、`EnterpriseVerificationCallbackContractTest.java`、`EnterpriseVerificationMySqlE2ETest.java` | 授权书材料差集；负责人闸；工作流失败回滚；并发唯一键；回调不发布 | 同模块测试 | 2026-09-16 |
| S-L041-10 | `EnterpriseApplicationProcessListener.java`、`SpringEnterpriseWorkflowGateway.java`、`ProfileEnterpriseApplication.java`、`EnterpriseApplicationServiceImpl.java`（test）、`frontend/packages/domains/profile/src/enterprise/application/service.ts`（旁证，不认 L-044 格子） | 事件转命令；缺 Bean/缺 flowCode → `ENTERPRISE_WORKFLOW_UNAVAILABLE`；生成列 `NEVER`；测试适配器不是生产 ServiceImpl；浏览器四枪 URL 对齐 | `listener/`、`adapter/gateway/`、实体、test、前端 service | 2026-09-16 |
