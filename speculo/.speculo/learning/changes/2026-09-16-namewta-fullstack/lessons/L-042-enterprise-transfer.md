---
lesson_id: L-042
objective_ids: [OBJ-42]
claimed_cells:
  - A:EnterpriseTransferController.send,confirm,unbind
  - B:EnterpriseTransferController.confirm
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: three-posts-on-disk
    minutes: 8
  - segment: confirm-changes-ownership
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 9
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-015, S-L003-01, S-L036-01, S-L040-01, S-L042-01, S-L042-02, S-L042-03, S-L042-04, S-L042-05, S-L042-06, S-L042-07, S-L042-08]
---

# Lesson 042：宏观转移门——EnterpriseTransferController send / confirm / unbind

## 学完你能做什么

打开厨房 `backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/controller/self/EnterpriseTransferController.java`。这份类**正好三个**公开 HTTP 方法，门牌只有一块：`/profile/enterprise/transfer`。你能**口述当前企业负责人怎么把执照钉子交到另一个人手里，以及自己怎么把钉子拔掉**。口试名单就是矩阵这两格，不是管理端档案、不是自助申请、不是材料库、不是前端厨房：

1. **`A:EnterpriseTransferController.send,confirm,unbind`**：门牌 `/profile/enterprise/transfer`。磁盘上正好这三个 `@PostMapping`，外加一只包范围 `@ExceptionHandler`。权限注解三扇共用 `profile:enterprise:apply`。
2. **`B:EnterpriseTransferController.confirm`**：矩阵写「改企业归属」。磁盘上 confirm **当场改 `profile_enterprise_binding`**：旧负责人那一行变成 `UNBOUND`，给目标账号插一根新的 `ACTIVE` 钉子，审计行打成 `CONFIRMED`。失败走 `EnterpriseTransferException` + `@DSTransactional` 回滚，旧钉子保持 `ACTIVE`。

OBJ-42 要你当场说完的那句是：**这是 layered 的自助转移门，不是 archive 柜台，不是 application 申请桌，也不是个人换绑那条河。三扇全是 POST；写窗把操作者从登录票取出，不信请求体里的人。send 先发短信挑战，软失败只回 `NOT_AVAILABLE`；confirm 才换口袋；unbind 只解自己，不删执照本。**

`wta-profile` 在登记表是 **layered**。这条线是 `Controller → EnterpriseTransferUseCaseImpl（每个方法 `@DSTransactional`）→ EnterpriseTransferService → EnterpriseTransferDao → EnterpriseTransferMapper XML`。Controller 字段名叫 `service`，类型却是 `EnterpriseTransferUseCase`。不要把测试夹具 `EnterpriseTransferServiceImpl`（`src/test/...`，同时 `extends EnterpriseTransferService implements EnterpriseTransferUseCase`）说成生产入口。

本课**不宣称**你会拆 `EnterpriseAdminController` 的十二扇档案窗（L-040）、`EnterpriseApplicationController` 的 current/save/submit/probe 与核身匿名回调（L-041）、企业材料 admin/self（L-043），或把 web-domain 怎么消费 `profileService.enterprise.transfer` 再讲一遍（L-044）。今天只认：**已经登录、权限是 apply 的当前负责人，怎么发挑战、怎么用验证码改归属、怎么解自己；以及 confirm 失败时旧口袋里的钉子还在。**

## 先把宏观地图放在桌上

L-003 已经把 `wta-profile` 钉在 layered 列：编排在 UseCase，规则在 Service，SQL 条件在 DAO/XML。L-040 是同一栋楼的**管理端侧门**，门牌 `/profile/enterprise/archive`。L-041 是**本人申请桌**。L-036 是隔壁个人楼的换绑河：那边 confirm **只冻申请单意图**，真正换口袋要等审核 `FINISH`。本课走进企业楼的**转移门**：confirm **自己就是换口袋那一枪**。

2026-09-16 工作树先钉死**包边界**（口试先数包，再数方法）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `EnterpriseAdminController.manageBinding` / `assign` / `revoke` | **另一扇门。** admin 包，权限 override/manage。`revoke` 作废整本执照，不是撤销挑战 |
| `EnterpriseApplicationController` | **自助申请桌。** `/profile/enterprise/application`。转移不走 WAITING，不开工单 |
| `PersonRebindController.confirm` | **隔壁个人河。** 那边 confirm 改申请单 `rebind_intent`，不立刻撕绑定。本课 confirm 立刻撕 |
| `IEnterpriseTransferService` | **生产主链不用这份接口。** 全仓只有自己这一处声明，没有任何 `implements` |
| Controller 字段名叫 `service` | 类型是 **`EnterpriseTransferUseCase`**。不要被字段名骗去找 `EnterpriseTransferService` 当 HTTP 依赖 |
| 绑定状态 `EFFECTIVE` | **没有这个枚举值。** 源负责人必须 `ACTIVE`；目标「已经占着钉子」用 SQL `status in ('ACTIVE','SUSPENDED')` |
| HTTP GET 查询挑战 | **没有。** 合同测试锁死三扇全是 POST：`/send` `/confirm` `/unbind` |
| 目标账号自己点确认 | **没有这扇窗。** `verify` 核对的是 **源** `sourceUserId`。短信打到目标手机，确认键仍在源登录票手里 |
| DDL 注释里的 `EXPIRED` / `FAILED` | **当前 XML 不写这两态。** 过期靠 Redis TTL + 时间戳；失败次数活在 Redis `attempts`，不是 MySQL `failed_attempts` |

同一模块还有 `controller/admin`（档案柜台）和 `controller/anonymous`（`@SaIgnore` 供应商回调）。架构测试锁死：admin/self **不得**写 `@SaIgnore`；anonymous 文件名必须带 `Anonymous`。本课三扇全在 self，权限是 apply，不是 override。

```text
已登录、持 profile:enterprise:apply 的当前负责人（源账号）
        │
        v
 /profile/enterprise/transfer/*          ← 本课唯一门牌
        │
        ├─ POST /send      send     apply + @Log(OTHER, 双 false)
        │     JSON：fullName + documentLastFour + phone
        │     软失败：NOT_AVAILABLE（HTTP 仍 200、零写库）
        │     成功：SENT + challengeId + expiresInSeconds=300
        │
        ├─ POST /confirm   confirm  apply + @Log(UPDATE, 双 false)  ← 矩阵 (b)
        │     JSON：challengeId + 六位 code
        │     成功：TRANSFERRED；旧 UNBOUND + 新 ACTIVE
        │     失败：类别码当 R.fail 的 msg；事务回滚，旧钉子不动
        │
        └─ POST /unbind    unbind   apply + @Log(UPDATE, 双 false)
              无 body；解的是**当前登录人自己的** ACTIVE 钉子
              不删 profile_enterprise 档案行
                        │
                        v
              EnterpriseTransferUseCaseImpl     三方法都 @DSTransactional
                        │
                        v
              EnterpriseTransferService         规则、挑战、改钉子
                        │
          ┌─────────────┼──────────────────┬─────────────────┬────────────┐
          v             v                  v                 v            v
 EnterpriseTransferDao  Redis 挑战仓     验证码端口      Person 身份查找   UserService
 Mapper XML             ChallengeStore   CodePort        Lookup API       锁账号+手机
          │                  │
          v                  v
 profile_enterprise_binding / _event / _transfer_record
 Redis：challenge / rate / lock 三套键
```

| 符号 | 磁盘 | 拥有什么 | 不拥有什么 |
| --- | --- | --- | --- |
| `EnterpriseTransferController` | `controller/self/EnterpriseTransferController.java` | 三扇映射、权限串、`R` 包装、从 `LoginHelper.getUserId()` 取操作者 | Mapper、事务、状态机、挑战仓 |
| `EnterpriseTransferExceptionHandler` | `controller/advice/` | 只捕 `EnterpriseTransferException` → `R.fail(category)` | HTTP 状态码改写、i18n |
| `EnterpriseTransferUseCase` / `Impl` | `usecase/` | 三个场景 + 每方法 `@DSTransactional`；无操作者的 default 直接 `UnsupportedOperationException("请传入 userId")` | DAO、Mapper、`service.impl` |
| `EnterpriseTransferService` | `service/EnterpriseTransferService.java` | send/confirm/unbind 规则；`@Service` 具体类 | Mapper import、`IService` |
| `EnterpriseTransferDao` | `dao/EnterpriseTransferDao.java` | 锁语义和 Mapper 调用 | 业务 if/状态机 |
| `EnterpriseTransferMapper.xml` | `resources/mapper/enterprise/` | `FOR UPDATE`、CHALLENGED→CONFIRMED、unbind/insertBinding/insertEvent | `profile_person` / `sys_user` 表名 |
| `RedisEnterpriseTransferChallengeStore` | `adapter/store/` | TTL 5 分钟、同源同目标 60 秒限流、错码最多 5 次、consume 一次性 | MySQL 行 |
| `EnterpriseTransferCodeGenerator` | `adapter/security/` | `SecureRandom` 六位数字，走 `EnterpriseTransferCodePort` | 把明文码写入 MySQL |

**图题 / caption：** 企业负责人转移的宏观三扇窗。alt：同一前缀下三枪全是 POST；send 发挑战，confirm 改归属，unbind 解自己；真正换口袋发生在 confirm，不是工作流 FINISH。

**文字等价物：** 工商科侧门挂一块牌子「企业转移」。三扇窗都要出示 apply 通行证。第一扇：当前负责人报出对方姓名、证件后四位、手机号；对得上唯一一个人、那人还没钉着别的执照，窗口才发一条短信验证码，并在 Redis 里放一张 5 分钟的挑战票。第二扇：还是这个负责人把挑战号和六位码递进去；窗口先验票，再把旧钉子拔掉、给对方钉一根新的。第三扇：谁登录就解谁自己的钉子，执照本还躺在柜子里。柜台后面不是办事员直接翻柜子，而是先填一张「用例单」（UseCase），单子进保险柜事务，再由科员（Service）按规矩改本子。本子在 DAO/XML。短信和身份查找要走出这间屋，分别问通知楼和个人档案楼。隔壁还有管理员窗口、申请窗口、材料库窗口，门牌都不是 transfer。

**类比：** 把企业执照想成一把办公室大门钥匙。钥匙此刻挂在你腰上（`profile_enterprise_binding` 的 `ACTIVE`）。你要把钥匙交给同事张三：你报出张三的姓名、证件后四位、手机；系统给张三的手机发验证码；**你**把验证码打进第二扇窗，钥匙当场从你腰上摘下来挂到张三腰上。张三以后不想当负责人了，自己来第三扇窗把钥匙挂回墙上。办公室（`profile_enterprise`）还在，门牌号（统一社会信用代码）也不换。

**类比失效处：**

1. 钥匙不是塑料，是带 `binding_version` 乐观锁的一行。confirm 必须锁到「还是发起时那一根、那个版本」，否则 `ENTERPRISE_TRANSFER_SOURCE_CHANGED`。
2. 短信打到张三手机，确认键却在你的登录票上。张三拿着验证码但没你的会话，打 confirm 会 `CHALLENGE_INVALID`（`verify` 比的是 `sourceUserId`）。
3. 个人换绑不是这把钥匙。L-036 的 confirm 只在申请表上盖「我要换」；企业这边没有申请表、没有工作流、没有 `publishApproved`。
4. 管理员把钥匙挂到别人腰上，走 archive 的 `assign` / `manageBinding`，权限不是 apply。
5. `unbind` 不是把办公室拆了。E2E 解绑后 `profile_enterprise.status` 仍是 `ACTIVE`，只是这本执照暂时没有负责人。
6. 暂停中的钉子（`SUSPENDED`）在这扇门看不见。`selectActiveOwner` / `lockActiveOwner` 都要求绑定 **和** 档案都是 `ACTIVE`。被管理员暂停的人既发不了挑战，也解不了自己。
7. DDL 虽然写了转移记录可以 `EXPIRED`/`FAILED`，当前插入只写 `CHALLENGED`，确认只改 `CONFIRMED`。过期是 Redis 票自己消失。

## 核心概念与机制

### 直觉讲解

先记住三件家具，再背路径：

- **执照本 `profile_enterprise`。** 转移不改它的 `status`，不改信用代码。归属换的是**谁腰上挂着钥匙**，不是换一家公司。
- **负责人钉子 `profile_enterprise_binding`。** 一本活档案最多一根有效钉子，一个账号最多钉一本活企业档案。生成列 `effective_user_id` / `effective_profile_id` 在 `ACTIVE` **和** `SUSPENDED` 时都占位子。
- **挑战票。** 明文六位码只出现在短信模板参数和 Redis 里的 BCrypt 哈希。MySQL 的 `profile_enterprise_transfer_record` 只记挑战号、源/目标、期望版本、状态，**不存验证码**。

再记住三扇窗的脾气：

- **send** 像对暗号。三要素对不上、对上的是自己、对方已经挂着钥匙、你自己根本不是当前负责人——窗口都只说 `NOT_AVAILABLE`。不抛业务异常，也不告诉你是哪一条没对上。这是防枚举，不是「成功态」。
- **confirm** 才是换口袋。矩阵 (b) 的「改企业归属」就是这一枪。验票失败、目标中途不合格、源钉子被人动过、Redis 消费失败——一律硬拒绝，事务回滚。
- **unbind** 是另一件事：解**自己**口袋里已经属于你的钥匙。不是帮别人换，也不是管理员覆盖。

再记住五层楼梯，不要并成「Controller 调 Service」：

Controller 只做 `@Valid`、权限、`LoginHelper`、`R.ok`。事务在 UseCase。规则、短信、Redis、跨模块查找在 Service。SQL 和 `FOR UPDATE` 在 DAO/XML。架构测试锁死：Controller 不得 import `service.`；UseCase 不得 import `dao.` / `mapper.`；Service 生产代码不得 import `mapper.`。

### 精确定义与 English term

| 中文说法 | English term | 磁盘定义 |
| --- | --- | --- |
| 企业转移 | enterprise transfer | 把已存在的 `profile_enterprise` 从源账号的 `ACTIVE` 绑定迁到目标账号；HTTP 面是 `EnterpriseTransferController` 三方法 |
| 发送挑战 | send challenge | `POST /send`。精确命中唯一合格目标后，Redis `PENDING_DELIVERY` → 同步短信 → MySQL `CHALLENGED` → Redis `ACTIVE`。成功 VO `SENT` |
| 确认归属 | confirm ownership | `POST /confirm`。验票后立刻 `unbindSource` + `insertBinding`。成功 VO `TRANSFERRED`。这就是矩阵 (b) |
| 自助解绑 | self unbind | `POST /unbind`。当前登录人自己的 `ACTIVE` 绑定改 `UNBOUND`，事件 `SELF_UNBIND` / `RESPONSIBLE_SELF_UNBIND`。不删档案 |
| 挑战票 | transfer challenge | `EnterpriseTransferChallenge`：挑战号、源/目标 userId、档案/绑定/版本、个人档案 id、姓名、证件后四位、手机、BCrypt 码、状态、attempts、过期毫秒 |
| 挑战状态 | challenge state | 只有 `PENDING_DELIVERY` 与 `ACTIVE`。未激活不能 verify |
| 有效钉子 | effective binding | SQL `status in ('ACTIVE','SUSPENDED') and del_flag='0'`。用来挡「目标已经有企业」；源操作仍要求纯 `ACTIVE` |
| 软不可用 | soft not-available | send 失败回 `EnterpriseTransferVo.status("NOT_AVAILABLE")`，HTTP 200，不写 Redis/MySQL |
| 类别码 | failure category | `EnterpriseTransferException` 的 message 就是码；handler `R.fail(exception.getMessage())`。E2E 里 HTTP 仍 200，读的是 `$.msg` |
| 一次性消费 | consume-once | Redis `storageToken`；`consume` 对不上或并发第二枪返回 false；UseCase 事务把已经写下的归属改动滚回去 |

错误出口：`EnterpriseTransferExceptionHandler` 是 `@RestControllerAdvice(assignableTypes = EnterpriseTransferController.class)`，只拦本控制器抛出的 `EnterpriseTransferException`。Bean 校验失败走框架 `@Valid`，不进这个 Handler。前端靠类别码区分「挑战作废」和「目标不合格」，不要只读中文句子——这里根本没有中文句子。

接口遗产：`IEnterpriseTransferService` 在 `service/` 里存在，2026-09-16 工作树**没有**生产实现类去 `implements` 它。生产 UseCase 直接持有具体类 `EnterpriseTransferService`。`EnterpriseTransferUseCase` 上无 `userId` 的 `send/confirm/unbind` 标了 `@Deprecated`，默认抛 `UnsupportedOperationException("请传入 userId")`。Controller 走带 `userId` 的重载。

跨模块合同：目标是谁，问 `PersonIdentityLookupService.findActiveExactMatches(fullName, documentLastFour)`，只拿回 `userId` + `personProfileId`，不拿姓名明文。手机号和账号是否启用，问 `UserService`。短信问 `NotificationApplicationService.submit`。EnterpriseTransferMapper XML **禁止**出现 `profile_person`、`profile_person_binding`、`sys_user`——架构测试按文本内容锁死。

### 机制/因果链

三扇窗都先过 `@SaCheckPermission("profile:enterprise:apply")`，再进 `EnterpriseTransferUseCaseImpl`。三个业务方法都标 `@DSTransactional`。事务边界在 UseCase，不在 Controller，也不在 Redis adapter。

身份：三扇都用 `LoginHelper.getUserId()`。HTTP 方法参数表没有 `long`/`Long` 操作者字段；unbind 连 body 都没有。`@Log` 三句标题不同。`BusinessType`：send 是 `OTHER`，confirm 与 unbind 是 `UPDATE`。三扇都 `isSaveRequestData = false`、`isSaveResponseData = false`——操作日志不存姓名、证件后四位、手机、验证码。这是隐私门闩，不是漏了 `@Log`。

Bean 校验在进 Service 之前：

- `EnterpriseTransferSendBo`：`fullName` `@NotBlank` ≤100；`documentLastFour` 必须 `[0-9A-Za-z]{4}`；`phone` `@NotBlank` ≤32。空格姓名、三位数后四位，合同测试直接判 invalid。
- `EnterpriseTransferConfirmBo`：`challengeId` `@NotBlank` ≤64；`code` 必须 `\\d{6}`。五位数字过不了门。

DAO 是 Mapper 唯一持有者。Service 生产代码不得（也没有）import `EnterpriseTransferMapper`。测试夹具为了单测直接把 Mapper 塞进 `EnterpriseTransferDao`。Mapper 继承 `BaseMapperPlus<ProfileEnterpriseTransferRecord, ProfileEnterpriseTransferRecord>`，转移 SQL 仍是手写 XML，不是靠 BaseMapper 的 CRUD 自动换绑。

#### 1. `send`——发挑战，不换口袋

`POST /profile/enterprise/transfer/send`。body：姓名 + 证件后四位 + 手机。Service 先 `trim`，后四位再 `toUpperCase`。

因果顺序：

1. `dao.selectActiveOwner(sourceUserId)`：绑定 `ACTIVE` **且** 档案 `ACTIVE`。没有这行 → 后面会掉进同一条软失败。
2. `personIdentities.findActiveExactMatches(fullName, documentLastFour)`，再用 `UserService.selectListByIds` 过滤 `status=="0"` 且手机号**全等**。必须恰好一人，且那人不是源自己。
3. `hasEffectiveEnterpriseBinding(target)`：目标已有 `ACTIVE`/`SUSPENDED` 钉子 → 同样软失败。
4. 以上任一条不满足：直接 `EnterpriseTransferVo.status("NOT_AVAILABLE")`。单元测试 `returnsTheSameMinimalResultWhenTheThreeTargetFactorsDoNotMatch`：`challengeId=null`，且 **不会** 碰 Redis、验证码生成器、用户批量查询、通知。
5. 过关才 `UUID` 挑战号 + `codes.generate()` 六位码 + `BCrypt.hashpw` + 过期 `now+5min`。挑战状态先是 `PENDING_DELIVERY`。
6. `challenges.stage`：同源同目标的 Redis 限流键 `profile:enterprise:transfer:rate:{source}:{target}`，`setIfAbsent` 60 秒。第二枪 `RATE_LIMITED` → 抛 `ENTERPRISE_TRANSFER_RATE_LIMITED`。这是硬拒绝，因为窗口已经知道目标合法，只是不让刷短信。
7. `notify.submit(...)`：应用 `profile`，场景码 `ENTERPRISE_TRANSFER`，模板 `enterprise-transfer`，渠道只有 `SMS`，模式 `SYNC`，幂等键 `profile:enterprise:transfer:{challengeId}`，元数据 `audit=REDACT_SENSITIVE`，模板参数只有 `code`。种子库里虽然还有一封 MAIL 模板，**这条 HTTP 不发邮件**。回执必须是 `ACCEPTED` 或 `DELIVERED`，否则 `revoke` Redis 再抛 `ENTERPRISE_TRANSFER_DELIVERY_FAILED`。单元测试名字就是这句话：`failedSmsNeverLeavesAConfirmableChallenge`——没有 `activate`。
8. 短信成功才 `insertTransferRecord`：状态 `'CHALLENGED'`，`failed_attempts=0`，`challenge_id` 唯一键。冲突 → `ENTERPRISE_TRANSFER_RECORD_CONFLICT`。
9. `challenges.activate`：只有 `PENDING_DELIVERY` 能变成 `ACTIVE`。失败抛 `ENTERPRISE_TRANSFER_CHALLENGE_STATE_FAILURE`，并 `revoke` Redis。MySQL 那一行还在同一场 UseCase 事务里，异常会滚回去。

成功 VO：`EnterpriseTransferVo.sent(challengeId)` → `status=SENT`，`expiresInSeconds=300`。注意：300 是写死的展示值，不是去读 Redis 剩余 TTL。

Redis E2E：`PENDING_DELIVERY` 时拿正确验证码去 `verify`，结果仍是 `INVALID`。票还没激活，不能确认。

#### 2. `confirm`——当场改企业归属（矩阵 b）

`POST /confirm`。body：`challengeId` + 六位 `code`。`BusinessType.UPDATE` 指的就是钉子换人，不是申请单 version+1。

这是和 L-036 必须对照着说的那一句：**个人 confirm 冻栅栏；企业 confirm 换口袋。**

硬前置（失败即 `EnterpriseTransferException`，事务回滚，binding 表要么没写、要么一起滚）：

1. `challenges.verify(challengeId, sourceUserId, code)` 必须 `VERIFIED`。下列任一条件都是 `INVALID`，再被收成 `ENTERPRISE_TRANSFER_CHALLENGE_INVALID`：票不存在、状态不是 `ACTIVE`、登录人不是源、已过期、BCrypt 对不上。错码会 `failedAttempt()`；满 5 次 Redis 直接 `delete`，第六次就算密码对了也没有票。`invalidOrReplayedCodeNeverTouchesBindings`：`verifyNoInteractions(mapper, personIdentities, users, notify)`。
2. 用挑战票上冻着的 `targetUserId / personProfileId / fullName / documentLastFour` 去 `lockActiveExactMatch`。锁不到、账号不是启用、手机变了、目标已经有有效企业钉子 → `ENTERPRISE_TRANSFER_TARGET_INELIGIBLE`。口试要说：send 时查过一次不够，confirm 必须在同一场事务里再锁一次。
3. `transferBindings`（下一小节的 SQL 顺序）。源变了 → `ENTERPRISE_TRANSFER_SOURCE_CHANGED`；目标锁到有效钉子 → 同样 `TARGET_INELIGIBLE`；插入撞生成列唯一键 → `ENTERPRISE_TRANSFER_BINDING_CONFLICT`。
4. `challenges.consume(verified)` 必须成功。失败仍抛 `CHALLENGE_INVALID`。`consumeRaceRollsBackInsteadOfReportingASecondTransfer` 断言：`confirmTransferRecord` **已经调用过**，但方法抛错——靠 UseCase 事务把归属改动撤回去，绝不能对调用方说第二次 `TRANSFERRED`。

成功 VO：`EnterpriseTransferVo.status("TRANSFERRED")`。没有 maskedPhone，没有 profileId，没有 bindingId。挑战号也不回显。

E2E `currentResponsibleTransfersBySmsOnceThenTargetCanSelfUnbind` 在 `session.commit()` 之后立刻读库：

- 源那一行 `enterprise_binding_id` 变成 `UNBOUND`
- 同一本档案、目标 userId、`status='ACTIVE'` 的行数 = 1
- 事件表各有一条 `UNBOUND/SELF_TRANSFER` 和 `ACTIVE/SELF_TRANSFER`
- `profile_enterprise_transfer_record.status='CONFIRMED'`
- 再用同一挑战号打第二枪 confirm：HTTP 200，`$.msg=ENTERPRISE_TRANSFER_CHALLENGE_INVALID`

这就是 **(b) confirm 改企业归属** 在磁盘上的意思：矩阵把「改归属」说成这扇窗的职责；实现把「改」落在 binding 行上，而不是申请单；无论验票失败还是消费失败，旧 `ACTIVE` 行要么没被碰，要么随事务回来。

#### 3. `transferBindings`——换口袋的 SQL 顺序

口试要能按手指头数这几步，顺序被 `EnterpriseTransferPersistenceTest.recordsChallengeAndSwitchesBindingsWithAppendOnlyEvents` 的 `InOrder` 锁死：

1. `lockActiveOwner(sourceUserId) FOR UPDATE`。行必须还在，且 `bindingId / profileId / bindingVersion` 与挑战票上冻的三件套全等。
2. `lockEffectiveBindingId(targetUserId) FOR UPDATE`。锁到任何 `ACTIVE`/`SUSPENDED` → 停，**先于** `unbindSource`。`staleSourceOrOccupiedTargetStopsBeforeMutation`。
3. `unbindSource`：`status='UNBOUND'`，`binding_version+1`，写下 `unbound_time`。WHERE 含原版本且 `status='ACTIVE'`。必须恰好 1 行。
4. `insertEvent` 旧钉子：`event_type='UNBOUND'`，`binding_version` 是旧版本+1，`source_type='SELF_TRANSFER'`，`reason='TRANSFER_CHALLENGE:{id}'`。
5. `insertBinding` 新钉子：新 id，同一 `profileId`，目标 userId，`status='ACTIVE'`，`binding_version=1`，`source_type='SELF_TRANSFER'`，`source_id` 指向旧 bindingId。
6. `insertEvent` 新钉子：`event_type='ACTIVE'`，`binding_version=1`，同一来源与 reason。
7. `confirmTransferRecord`：只更新仍是 `CHALLENGED` 且三件套仍对得上的那一行，改成 `CONFIRMED` 并写 `confirmed_time`。必须恰好 1 行，否则 `ENTERPRISE_TRANSFER_RECORD_CHANGED`。

事件是追加的，不改历史行。档案行、版本页、申请桌，这条路径一眼都不看。

#### 4. `unbind`——解自己，不删本，不发短信

`POST /unbind`。无 body。不碰 Redis，不碰通知，不碰个人身份查找。单元测试 `currentResponsibleCanUnbindWithoutWorkflowOrDeletion`：`verifyNoInteractions(challenges, codes, personIdentities, users, notify)`。

`unbindBinding`：`lockActiveOwner` 找不到 → `ENTERPRISE_TRANSFER_SOURCE_NOT_ACTIVE`；`unbindSource` 必须改 1 行否则 `SOURCE_CHANGED`；再插事件 `UNBOUND` / `SELF_UNBIND` / `RESPONSIBLE_SELF_UNBIND`。持久化测试：只改 binding + 下一版本事件。E2E 在目标接过钉子之后，把登录票换成目标再打 unbind：档案上 `ACTIVE` 绑定数为 0，`profile_enterprise` 仍 `ACTIVE` 且 `del_flag='0'`。

这扇窗**不是**转移成功路径的最后一步。转移成功路径的最后一步就是 confirm。unbind 是负责人自己挂钥匙的侧门，管理员那扇叫 `manageBinding` 的 `UNBIND`。

### 图、表或文本图

```text
send 成功后的票
  Redis  challenge:{id}  state=ACTIVE  codeHash=BCrypt  ttl≤300s
  Redis  rate:{src}:{tgt}             ttl=60s
  MySQL  transfer_record              status=CHALLENGED  不存 code

confirm 成功（同一场 @DSTransactional）
  verify ──► lock 个人身份 ──► lock 源钉子 ──► lock 目标有效钉子
                         │
                         ▼
              旧 binding  ACTIVE ──update──► UNBOUND  (version N+1)
              旧 event    UNBOUND / SELF_TRANSFER / TRANSFER_CHALLENGE:{id}
              新 binding  insert ACTIVE version=1 / SELF_TRANSFER
              新 event    ACTIVE  / SELF_TRANSFER / TRANSFER_CHALLENGE:{id}
              transfer_record  CHALLENGED ──► CONFIRMED
                         │
                         ▼
              Redis consume（storageToken 一次性）
              失败 → 抛 CHALLENGE_INVALID → 上面的 SQL 全部回滚

unbind（另一扇窗，无票）
  lock 自己的 ACTIVE 钉子 ──► UNBOUND + event SELF_UNBIND
  执照本仍 ACTIVE
```

| Java 方法 | HTTP | 路径 | 入参 | 成功 VO | 改了什么 |
| --- | --- | --- | --- | --- | --- |
| `send` | `POST` | `/send` | `EnterpriseTransferSendBo` | `SENT` + challengeId + 300 | Redis 票 + 一行 `CHALLENGED`；**不**改 binding |
| `confirm` | `POST` | `/confirm` | `EnterpriseTransferConfirmBo` | `TRANSFERRED` | **改归属**：旧 UNBOUND + 新 ACTIVE + 记录 CONFIRMED + 烧掉 Redis 票 |
| `unbind` | `POST` | `/unbind` | 无 | `UNBOUND` | 当前用户自己的 binding → UNBOUND；不删档案、不写 transfer_record |

**图题 / caption：** confirm 改企业归属的因果链。alt：验票、再锁身份、再锁两根钉子、先拔后钉、最后一次性消费 Redis；消费失败整单回滚。

**文字等价物：** 第二扇窗不是「再确认一次意向」。它先问 Redis：这张票是不是还活着、是不是这个源、六位码对不对。对了，再问个人档案楼：张三是不是还是那本身份证。再问用户楼：账号是不是启用、手机是不是还是那个号。再在企业绑定表上锁源、锁目标。源必须还是发起时那一版钥匙；目标腰上不能已经挂着别的企业钥匙。然后才 UPDATE 旧行、INSERT 新行、把审计行打成 CONFIRMED，最后把 Redis 票撕掉。票撕失败等于整场搬家没发生。

### 正例、反例与边界

**正例 A（口述三扇窗）。** 当前负责人 101 钉着档案 9201、钉子 9101、版本 7。他 POST `/send`，body 是张三 / `3001` / `13800138000`。系统只找到一个启用账号 202，202 没有企业钉子。返回 `SENT` 和一个挑战号，短信模板参数里有六位码，审计元数据是 `REDACT_SENSITIVE`。101 再 POST `/confirm`，带挑战号和码。返回 `TRANSFERRED`。9101 变成 `UNBOUND`，202 多了一根 `ACTIVE`，记录 `CONFIRMED`。202 再 POST `/unbind`，返回 `UNBOUND`，档案 9201 仍活着。这就是合同测试 + 单元测试 + MySQL/Redis E2E 串起来的主路径。

**正例 B（矩阵 b 的成功形态）。** confirm 的 `InOrder` 是：先 `lockActiveExactMatch`，再 `lockActiveById`，再 `countEffectiveBinding`，再 `lockActiveOwner`。口试不要说成「先改库再验人」。归属改动发生在身份锁和源锁都拿到之后。

**反例 1：send 三要素对不上。** 姓名/后四位/手机不能唯一命中 → `NOT_AVAILABLE`，零写。窗口不会为了错姓名去生成验证码。

**反例 2：短信失败。** `notify.submit` 抛错或回执不是 ACCEPTED/DELIVERED → Redis `revoke`，永不 `activate`，MySQL 不留可确认行。

**反例 3：未激活就 confirm。** Redis E2E：`PENDING_DELIVERY` 的票，正确码也是 INVALID。

**反例 4：错码五次。** 第五次删票；再提交正确码仍 INVALID。MySQL `failed_attempts` 列不会跟着加——那一列当前插入恒为 0，真正的次数在 Redis JSON 的 `attempts`。

**反例 5：重放 confirm。** 票已 consume。E2E 第二枪 `CHALLENGE_INVALID`，绑定不再被第二枪改写。

**反例 6：源钉子被人动过。** confirm 时 `lockActiveOwner` 对不上冻版本 → `SOURCE_CHANGED`，停在任何 UPDATE 之前。

**反例 7：目标中途挂上了企业。** `lockEffectiveBindingId` 不是 null → `TARGET_INELIGIBLE`，旧钉子仍 ACTIVE。

**反例 8：consume 并发。** Redis 两枪只有一枪 `true`；失败那枪即便 SQL 已经跑过，也因异常回滚，调用方看不到第二次 `TRANSFERRED`。

**反例 9：暂停中的负责人来 send/unbind。** `selectActiveOwner`/`lockActiveOwner` 只要 `ACTIVE`，SUSPENDED 当「不是当前负责人」。他要回去找管理员的 manage 窗。

**反例 10：目标自己拿码来 confirm。** `verify` 比源 userId，目标会话对不上票。

边界：

- 限流键按 **源+目标** 配对，不是按源一个人。换一个目标，60 秒内仍可能再发。
- 挑战 TTL 5 分钟，限流 60 秒。60 秒后可以再 stage 一张新票；旧票只要还 ACTIVE 也能被 confirm。先成功的那张会把源版本抬走，后到的那张走 `SOURCE_CHANGED`。
- `expiresInSeconds=300` 与 Redis `remainTimeToLive` 不是同一个数。激活时 adapter 用剩余 TTL 重写，不会把 5 分钟续上。
- 通知渠道磁盘写死 SMS。MAIL 模板是种子，不是这条链的通道。
- `delivered()` 把 `ACCEPTED` 也当成功——同步提交被通知楼收下即可，不等手机回执。
- 生成列唯一键会把「目标其实已经占坑」从 DuplicateKey 收成 `BINDING_CONFLICT`。
- HTTP 合同：恰好三扇 POST，权限恰好 apply，`@Log` 双 false。没有 GET，没有 DELETE，没有 admin 前缀。

## 变式与迁移

### 变式

- **软失败 vs 硬失败。** send 的「找不到/不唯一/是自己/目标已占用/自己不是负责人」并成 `NOT_AVAILABLE`。限流、短信、状态机、验票、目标锁、源版本、消费失败走类别码。口试不要把两类出口说成一种。
- **Handler 形态。** 类别码当 `R.fail` 的 msg。E2E 断言 HTTP 200 + `$.msg`。校验失败不进这个 Handler。
- **测试夹具。** E2E 甚至 `new EnterpriseTransferController(service)`，`service` 是测试 `EnterpriseTransferServiceImpl`。生产 Bean 仍是 UseCaseImpl。夹具还实现了无 userId 的 UseCase 方法去叫 `LoginHelper`；生产 default 直接抛。
- **前端厨房只作篱笆。** `createEnterpriseTransferService` 三枪 URL 与本课门牌相同；`createProfileService` 还把 `sendTransfer/confirmTransfer/unbind` 挂到 `enterprise.application` 上，和 `enterprise.transfer` 是同一组函数引用。格子仍归 L-044，本课只要求听得懂门牌。
- **个人对照。** L-036 confirm 改申请单；本课 confirm 改 binding。两边 unbind 都是解自己、都不删档案。不要把 `rebind_intent`、核身、工作流 listener 抄进企业转移。
- **管理端对照。** 管理员指定/暂停/解绑/注销走 archive。本课没有 override，没有 `ADMIN_OVERRIDE` 事件。转移事件来源是 `SELF_TRANSFER` / `SELF_UNBIND`。
- **documentLastFour。** Bean 允许大小写字母；Service 与 `ActiveIdentityQuery` 都 upper。长度不是 4，API record 构造会炸——但 HTTP 进 Service 之前 Bean 已经挡过。

### 迁移

当前是 Target 形状，不是 classic 遗留房间。口试仍要能指出**还放在抽屉里、不能当主链讲**的几件旧家具：

| Current | Target / 口试说法 | 证据 |
| --- | --- | --- |
| `IEnterpriseTransferService` 仍在 `service/` | 生产 Controller/UseCase **不得**注入它 | 全仓只有自己这一处引用 |
| `src/test/.../EnterpriseTransferServiceImpl` 同时当 UseCase 和 Mapper 持有者 | 夹具允许；E2E 为了同一条 SQL/Redis 会话才直接 new Controller | 不要画进模块地图主箭头 |
| UseCase 无 userId 的 default | 全部 `throw new UnsupportedOperationException("请传入 userId")` | 新入口必须显式操作者 |
| Service 兼容构造仍接具体 `EnterpriseTransferCodeGenerator` | 生产主构造接 `EnterpriseTransferCodePort` | `@Deprecated` 构造 |
| DDL 注释 `CHALLENGED/CONFIRMED/EXPIRED/FAILED` 与 `failed_attempts` | 当前写入只有 CHALLENGED→CONFIRMED；错码次数在 Redis | XML insert/update；Redis `MAX_ATTEMPTS=5` |
| 种子 MAIL 模板 `enterprise-transfer` | HTTP send 只提交 SMS 渠道 | `List.of(NotificationChannel.SMS)` |
| `ChallengeStore.revoke` 的 Javadoc 写「撤销转移挑战或档案绑定」 | 磁盘只删 Redis 票，不改 binding | `bucket.delete()` |
| 读方法也没有，三扇全是写 | 对齐 API-005：变更 POST + `@Log` | 合同测试 |

Ratchet：新窗继续 layered；不要为了「看起来像个人换绑」给企业转移加 probe/match/submit/工作流；不要把 Mapper 送回 Service；不要在 Controller 开事务。不要把 confirm 改成「只冻意图」，那会让矩阵 (b) 变成假话。

## 常见误区

1. **「这是管理端指定负责人。」** 指定在 archive 的 `assign`，权限 override。本课是当前负责人自助移交。
2. **「confirm 跟个人换绑一样，先冻意图再等审核。」** 企业没有这张申请单。confirm 当场改归属。
3. **「目标收到短信，所以是目标点确认。」** `verify` 要源 userId。目标只能事后自己 unbind，不能替源 confirm。
4. **「send 失败会抛 ENTERPRISE_TRANSFER_NOT_AVAILABLE。」** 没有这颗码。软失败是 VO 状态字 `NOT_AVAILABLE`。
5. **「layered 所以 Controller 调 Service。」** Controller 调 UseCase。字段名叫 `service` 是陷阱。
6. **「UseCase 只包写操作。」** 本课三扇本来就都是写；不要借这句话去猜申请桌的 GET。
7. **「生效=状态字 EFFECTIVE。」** 有效钉子是 ACTIVE 或 SUSPENDED 的生成列。源操作还更严，只要 ACTIVE。
8. **「unbind 会把执照本逻辑删除。」** `del_flag` 仍是 `'0'`，`status` 仍是 `ACTIVE`。只拔钉子。
9. **「`POST /unbind` 等于管理员 `revoke`。」** revoke 把档案打成 `REVOKED`。完全另一扇门。
10. **「验证码存在 transfer_record 里。」** 表里没有 code 列。哈希在 Redis，明文只进短信参数，且 `@Log` 不存报文。
11. **「IEnterpriseTransferService 的方法注释才是合同。」** 以 Controller + Service 实现 + XML + Redis adapter 为准。
12. **「XML 自己 join sys_user 查手机。」** 架构测试禁止表名；手机走 `UserService`。
13. **「失败次数看 MySQL failed_attempts。」** 当前写入恒 0。五次烧票在 Redis。
14. **「把测试夹具说成生产分层。」** 生产是 UseCaseImpl 注入 Service；夹具为了单测持 Mapper。
15. **「前端 `enterprise.application.unbind` 是申请桌的方法。」** 那是 `createProfileService` 把 transfer.unbind 挂到 application 对象上的别名。HTTP 仍打 `/profile/enterprise/transfer/unbind`。格子归 L-044。
16. **「转移会换统一社会信用代码。」** 归属是负责人绑定。执照身份键不动。
17. **「SUSPENDED 的负责人也能移交。」** 这扇门找不到他。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 打开 `EnterpriseTransferController`。把三对 mapping 抄成一张表：路径、动词、Java 名、权限串、`BusinessType`、返回类型、有没有 body、`@Log` 是否双 false。圈出：全是 POST；字段类型是 `EnterpriseTransferUseCase`；操作者只来自 `LoginHelper.getUserId()`。
2. 用手指划 `confirm` → `LoginHelper.getUserId()` → `UseCaseImpl.confirm`（`@DSTransactional`）→ `EnterpriseTransferService.confirm` → `challenges.verify` → `lockActiveExactMatch` → `transferBindings` → `challenges.consume`。在 consume 旁边写：失败时归属不该已经提交。
3. 打开 `EnterpriseTransferMapper.xml` 的 `selectActiveOwner`、`lockActiveOwner`、`countEffectiveBinding`、`unbindSource`、`insertBinding`、`confirmTransferRecord`。抄下 status 集合。对照：源操作只要 `ACTIVE`，占坑检查含 `SUSPENDED`。再看 XML 文本里有没有 `profile_person` / `sys_user`。
4. 打开 `EnterpriseTransferHttpContractTest`。确认恰好 `/send` `/confirm` `/unbind`，权限都是 apply，没有 GET。再打开 `EnterpriseBeanValidationContractTest` 里 SendBo / ConfirmBo 那两行 invalid 字段。
5. 打开 `EnterpriseTransferServiceTest` 的 `invalidOrReplayedCodeNeverTouchesBindings` 与 `consumeRaceRollsBackInsteadOfReportingASecondTransfer`，对照 E2E 主路径 commit 之后的四条 SELECT（旧 UNBOUND、新 ACTIVE、事件、记录 CONFIRMED）。把「矩阵 (b) 改的是 binding，不是申请单」写在名册旁边。

## 总结、词汇表与下一步

- **三扇窗。** 前缀 `/profile/enterprise/transfer`。POST 三扇：send 发挑战、confirm 改归属、unbind 解自己。layered：Controller → UseCase → Service → DAO → XML；挑战票在 Redis。
- **(b) confirm 改企业归属。** 成功：旧钉子 `UNBOUND` + 新钉子 `ACTIVE` + 记录 `CONFIRMED` + 烧掉票，VO `TRANSFERRED`。失败：类别码，事务回滚，旧 `ACTIVE` 还在。不走 WAITING，不开工作流。
- **send 软失败。** `NOT_AVAILABLE` 是状态字，不是异常。短信失败不留可确认票。未激活不能验。
- **unbind 是解自己。** 不删档案。不是转移的收尾 HTTP——收尾就是 confirm。
- **合格目标。** 姓名+证件后四位精确命中唯一有效个人身份，账号启用，手机全等，没有企业有效钉子，不能是源自己。confirm 还要再锁一次。
- **格子按磁盘全表：** 不要把 archive / application / 材料 / 个人换绑 / 前端工厂补进来。不要把 `IEnterpriseTransferService` 说成 HTTP 依赖。不要把 EFFECTIVE 说成状态字。不要说目标点确认。

词汇表：EnterpriseTransferController / layered / EnterpriseTransferUseCaseImpl / `@DSTransactional` / EnterpriseTransferDao / `profile_enterprise_binding` / `profile_enterprise_transfer_record` / CHALLENGED / CONFIRMED / PENDING_DELIVERY / ACTIVE / `binding_version` / effective binding / SELF_TRANSFER / SELF_UNBIND / `NOT_AVAILABLE` / `TRANSFERRED` / `UNBOUND` / `ENTERPRISE_TRANSFER_CHALLENGE_INVALID` / `ENTERPRISE_TRANSFER_SOURCE_CHANGED` / `ENTERPRISE_TRANSFER_TARGET_INELIGIBLE` / `profile:enterprise:apply` / `LoginHelper.getUserId()` / Redis challenge store / consume-once / `PersonIdentityLookupService`。

下一步：L-043 走进材料总库的 admin/self 切片。L-044 才把 web-domain enterprise 接到这三扇窗和 archive/application。对照时带着 L-036 的宏观口吻，换表名、换权限前缀、换「confirm 当场换口袋」这道篱笆，不要复制个人柜台的 `rebind_intent`。管理端指定与暂停仍在 L-040。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / UseCase / `wta-api` | profile-enterprise 转移公开 HTTP 与分层入口 | `EnterpriseTransferController`；`EnterpriseTransferUseCaseImpl` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-modules/wta-profile` 登记为 layered | 登记表 profile 行 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 表 `profile_enterprise_transfer_record` 状态注释；菜单 `profile:enterprise:apply`；模板 `enterprise-transfer` | DDL 转移表；DML 申请菜单 / 210063 模板 | 2026-09-16 |
| S-015 | `backend/wta-api/.../profile/api/person/PersonIdentityLookupService.java` 与 `system`/`notify` API | 跨模块只回 userId+personProfileId；手机与通知走公开服务 | `findActiveExactMatches` / `lockActiveExactMatch` | 2026-09-16 |
| S-L003-01 | `children/2026-09-14-namewta-architecture/lessons/L-003-layered-vs-classic.md` | 五层楼梯与登记表；profile 是试点房间 | OBJ-03 前驱 | 2026-09-16 |
| S-L036-01 | `lessons/L-036-person-rebind.md` | 个人 confirm 冻意图、不立刻换绑；本课对照「当场改归属」 | OBJ-36 (b) | 2026-09-16 |
| S-L040-01 | `lessons/L-040-enterprise-admin.md` | 管理端 archive 十二扇与 revoke/manageBinding 篱笆；转移不在那扇门 | OBJ-40 | 2026-09-16 |
| S-L042-01 | `controller/self/EnterpriseTransferController.java`；`controller/advice/EnterpriseTransferExceptionHandler.java` | 三扇 POST、共用 apply 与双 false `@Log`、`LoginHelper`、handler `R.fail(message)` | 类注解与三个映射；`handle` | 2026-09-16 |
| S-L042-02 | `usecase/EnterpriseTransferUseCase.java`；`usecase/impl/EnterpriseTransferUseCaseImpl.java` | deprecated 无 userId 抛错；生产三方法全 `@DSTransactional`；注入具体 `EnterpriseTransferService` | 默认方法；三个 override | 2026-09-16 |
| S-L042-03 | `service/EnterpriseTransferService.java`；`service/IEnterpriseTransferService.java` | send 软失败；confirm 验票后 `transferBindings`；unbind 不解档案；接口生产未接线 | `send` / `confirm` / `transferBindings` / `unbindBinding` | 2026-09-16 |
| S-L042-04 | `dao/EnterpriseTransferDao.java`；`mapper/EnterpriseTransferMapper.java`；`mapper/enterprise/EnterpriseTransferMapper.xml` | 源只要 ACTIVE；占坑含 SUSPENDED；CHALLENGED→CONFIRMED；事件追加 | XML 各 id | 2026-09-16 |
| S-L042-05 | `adapter/store/RedisEnterpriseTransferChallengeStore.java`；`domain/transfer/EnterpriseTransferChallenge.java`；`adapter/security/EnterpriseTransferCodeGenerator.java` | 5 分钟 TTL、60 秒限流、5 次烧票、PENDING 不能验、consume 一次性、六位 SecureRandom | `stage`/`activate`/`verify`/`consume` | 2026-09-16 |
| S-L042-06 | `EnterpriseTransferHttpContractTest.java`；`EnterpriseModuleArchitectureTest.java`；`EnterpriseBeanValidationContractTest.java` | 三扇权限与安全 `@Log`；五层 import 方向；XML 禁 person/sys_user；命令 Bean 校验 | 测试方法名 | 2026-09-16 |
| S-L042-07 | `EnterpriseTransferServiceTest.java`；`EnterpriseTransferPersistenceTest.java`；`EnterpriseTransferServiceImpl.java`（test） | 短信失败不留票；错码不碰库；consume 竞态回滚；SQL InOrder；夹具不是生产主链 | 具名测试方法 | 2026-09-16 |
| S-L042-08 | `EnterpriseTransferMySqlRedisE2ETest.java`；`RedisEnterpriseTransferChallengeStoreE2ETest.java`；`frontend/packages/domains/profile/src/enterprise/transfer/service.ts` | 主路径改归属后目标可自解；重放无效；五次烧票；前端三枪 URL 对照（格子仍归后端） | `currentResponsibleTransfersBySmsOnceThenTargetCanSelfUnbind`；Redis 三测 | 2026-09-16 |
