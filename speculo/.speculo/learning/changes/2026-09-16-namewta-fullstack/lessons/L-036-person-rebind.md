---
lesson_id: L-036
objective_ids: [OBJ-36]
claimed_cells: [A:PersonRebindController.probe,match,confirm,submit,unbind, B:PersonRebindController.confirm, D:fail-profile-rebind, D:失败路径 profile 换绑]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: five-windows
    minutes: 9
  - segment: confirm-fail-keep-binding
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L036-01, S-L036-02, S-L036-03, S-L036-04, S-L036-05, S-L036-06, S-L036-07, S-L036-08]
---

# Lesson 036：个人换绑的宏观五扇窗——`probe` / `match` / `confirm` / `submit` / `unbind`

## 学完你能做什么

打开 `backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/controller/self/PersonRebindController.java`，你能**口述自助换绑的五扇 JSON 窗**，以及 **match / confirm 失败时原绑定一行都不动**。口试名单就是矩阵这三格（外加矩阵中文行名，方便对表），不是管理端档案、不是普通申请 current/submit、不是核身回调、不是前端厨房：

1. **`A:PersonRebindController.probe,match,confirm,submit,unbind`**：门牌 `/profile/person/rebind`。磁盘上正好这五个 `@PostMapping`，外加一只包范围 `@ExceptionHandler`。权限注解五扇共用 `profile:person:apply`。
2. **`B:PersonRebindController.confirm`**：矩阵写「改绑定；失败保持原绑定」。磁盘上 confirm **改的是申请单上的换绑意图栅栏**（`rebind_intent='Y'` + 冻结的 `target_profile_id` / `expected_binding_id` / `expected_binding_version`），**不是**立刻把旧账号从 `profile_person_binding` 上撕下来。失败走 `PersonRebindException` + `@DSTransactional` 回滚，旧绑定保持 `ACTIVE`。
3. **`D:fail-profile-rebind`**（矩阵行 **`D:失败路径 profile 换绑`**）：`rebind match/confirm 失败不改绑定`。match 失败是软拒绝 `NOT_AVAILABLE`（HTTP 仍 200、零写库）；confirm 失败是硬拒绝（类别码当 `R.fail` 的 msg）。submit 在栅栏变了时 `PERSON_REBIND_BINDING_CHANGED`，也不插提交、不开工单。真正换绑发生在工作流 `FINISH` 之后的 `publishApprovedRebind`；那一枪失败同样保持原绑定。

`wta-profile` 在登记表是 **layered**。这条线是 `Controller / Listener → PersonRebindUseCaseImpl（@DSTransactional）→ PersonRebindService → PersonRebindDao → PersonRebindMapper XML`。Controller 字段名叫 `service`，类型却是 `PersonRebindUseCase`。不要把测试夹具 `PersonRebindServiceImpl`（`src/test/...`，同时 `extends PersonRebindService implements PersonRebindUseCase`）说成生产入口。

本课**不宣称**你会拆 `PersonAdminController` 档案/审核/绑定（L-034）、`PersonApplicationController.current/submit` 普通申请（L-035）、材料 attach/detach 与标签（L-037）、匿名核身回调（L-038）、或 `createProfileService.rebind` 厨房（L-039）。今天只认：**已经登录、权限是 apply 的人，怎么探测证件、核对旧持有人、在申请单上冻栅栏、提交审核、以及解绑自己；失败时旧钱包里的卡还在。**

## 先把宏观地图放在桌上

L-003 已经把 profile 标成 layered 试点。L-034 是管理端档案柜台。L-035 是「我还没有这张实名卡、我自己填申请」。本课站在 **另一条自助河**：证件已经被别人的账号拿着，我要用同一套身份把卡换到我手上。企业换绑是 `EnterpriseTransferController`（L-042），门牌和动词都不同。

2026-09-16 工作树里，五扇窗**共用一块牌子** `/profile/person/rebind`，权限也共用 `profile:person:apply`。HTTP 合同测试锁死：每个声明方法都有 `@PostMapping`、都有 `@Log`、都 **不** 把 `long`/`Long` 放进参数表（操作者编号只从 `LoginHelper.getUserId()` 取，probe 连这个都不用）。

```text
已登录、持 profile:person:apply 的申请人（新账号，必须自己尚未绑定）
        │
        ├─ POST /profile/person/rebind/probe      probe
        │     JSON：documentTypeCode + documentNumber
        │     只回 status：BOUND / IN_PROGRESS / UNBOUND / AVAILABLE
        │     不写库；不看当前 userId
        │
        ├─ POST /profile/person/rebind/match      match
        │     JSON：{ identity: PersonRebindIdentityBo }
        │     成功：REBIND_AVAILABLE + 旧持有人 maskedPhone
        │     失败：NOT_AVAILABLE + maskedPhone=null（软拒绝，零写库）
        │
        ├─ POST /profile/person/rebind/confirm    confirm   ← 矩阵 (b)
        │     JSON：{ identity, expectedVersion }
        │     成功：CONFIRMED + maskedPhone + version+1
        │     只 UPDATE 申请单意图栅栏；旧 binding 仍 ACTIVE
        │     失败：PersonRebindException → R.fail(类别码)；原绑定不动
        │
        ├─ POST /profile/person/rebind/submit     submit
        │     JSON：{ expectedVersion }
        │     冻材料快照、WAITING、开工单+核身；旧 binding 仍 ACTIVE
        │
        └─ POST /profile/person/rebind/unbind     unbind
              无 body；解的是**当前登录人自己的**有效绑定
              不删 profile_person 档案行

工作流 FINISH（不是第六扇 HTTP 窗）
        PersonRebindProcessListener @Order(HIGHEST_PRECEDENCE)
        → UseCase.handleProcess → publishApprovedRebind
        同一事务：旧 UNBOUND 事件 + 新 ACTIVE 绑定 + 申请 FINISH
```

| Java 方法 | HTTP | 路径 | 入参 | 成功 VO | 改了什么 |
| --- | --- | --- | --- | --- | --- |
| `probe` | `POST` | `/probe` | `PersonRebindProbeBo` | `PersonRebindProbeVo(status)` | **不改**行；只读 `identity_key` 探测 |
| `match` | `POST` | `/match` | `PersonRebindMatchBo` | `PersonRebindMatchVo(status, maskedPhone)` | **不改**行；失败也 200 + `NOT_AVAILABLE` |
| `confirm` | `POST` | `/confirm` | `PersonRebindConfirmBo` | `PersonRebindConfirmationVo(status, maskedPhone, version)` | 申请单 `rebind_intent='Y'` 与三件套栅栏；**不** `unbind` 旧绑定 |
| `submit` | `POST` | `/submit` | `PersonRebindSubmitBo` | `PersonRebindSubmissionVo(status, snapshotVersion, version)` | 提交行 + 材料 SUBMISSION 快照 + 申请 `WAITING` + 核身 attempt + `workflow.start` |
| `unbind` | `POST` | `/unbind` | 无 body | `PersonRebindUnbindVo("UNBOUND")` | 当前用户自己的 binding → `UNBOUND` + 事件 `SELF_SERVICE` / `PERSON_SELF_UNBOUND` |

`@Log` 标题五句不同。`BusinessType`：confirm 与 unbind 是 `UPDATE`，probe / match / submit 是 `OTHER`。五扇都 `isSaveRequestData = false`、`isSaveResponseData = false`——操作日志不存证件号和脱敏手机。这是隐私门闩，不是漏了 `@Log`。

**图题 / caption：** 个人换绑控制面五扇窗的宏观地图。alt：同一前缀下五枪全是 POST；probe/match/confirm/submit/unbind；真正换绑发生在工作流 FINISH 的 publish，不是 confirm。

**文字等价物：** 票房在 `wta-profile-person` 的 `PersonRebindController`。浏览器先用证件类型+号码问这张身份现在怎样（probe），再用完整身份字段问能不能换（match）。对上了只看见旧持有人的脱敏手机，看不见 profileId / bindingId / 完整手机。点确认时，服务把「我要换的就是这一张、这一版绑定」写进**自己的申请单**，旧账号的绑定行仍是 `ACTIVE`。交出去（submit）只是开工单。等审核流程走到 `FINISH`，另一条监听器才在同一事务里把旧绑定改 `UNBOUND`、给申请人插一条新 `ACTIVE`。自己不想要了，打 unbind，解的是自己口袋里的卡，档案人还在。

类上**没有** `BaseController`。五扇都是 POST + `@Log`，对齐 API-005「变更 POST + `@Log`」；probe/match 虽偏查询，磁盘仍是 POST。口试按磁盘，不要改口成 GET。

## 核心概念与机制

### 直觉讲解

把换绑想成**图书馆借书卡此刻在别人钱包里，你要用同一身份证号把卡换到自己钱包**：

- **probe** 是目录查询：这张身份证现在是「被人拿着 / 正在办手续 / 卡还在但没人拿 / 压根没这张卡」。窗口不看你是谁。
- **match** 是对暗号。姓名、证件、性别、生日、有效期必须和目录上那张卡**逐项相同**，而且卡必须在**另一个人**手里。对上了，窗口递一张纸条，上面只有旧持有人的脱敏手机（`138****8000`）。名字写错？纸条写 `NOT_AVAILABLE`，连星星手机都没有。窗口**不会**把卡从别人钱包抽走。
- **confirm** 是你在**自己的申请表**上盖章：「我认这张卡、这个 binding 版本。」别人的钱包**还没打开**。单元测试的名字就是这句话：`confirmationFreezesTheCurrentBindingWithoutSwitchingIt`。
- **submit** 是把盖了章的表连同材料快照交给审核室，启动核身和工作流。卡仍在旧钱包。
- **unbind** 是另一件事：你把自己钱包里**已经属于你的卡**还回去。不是帮别人换卡。
- 真正换口袋发生在审核室喊 `FINISH` 的那一瞬间：旧卡盖 `UNBOUND` 章，给你缝一张新的 `ACTIVE`。中间任何一步对不上版本，旧卡纹丝不动。

**类比失效处：** 卡不是塑料，是 `profile_person_binding` 行，带 `binding_version` 乐观锁。confirm 写的不是钱包，是 `profile_person_application` 上的 `rebind_intent` 和三件套栅栏。脱敏手机来自跨模块 `UserService.selectPhonenumberById`，短于 8 位直接变成 `****`，不是图书馆员手写。submit 还会冻材料快照、打核身指纹（SHA-256 of `identityKey + snapshotVersion + REBIND`）。工作流监听器必须比普通申请监听器更早（`@Order(HIGHEST_PRECEDENCE)`），否则换绑申请可能被当成普通申请去发布。通知旧账号是事务提交之后的 `DsTxEventListener`：通知失败**不会**把已经换走的绑定换回来。

### 精确定义与 English term

| 中文说法 | English term | 磁盘定义 |
| --- | --- | --- |
| 个人换绑 | person rebind | 把已存在的 `profile_person` 从旧 `userId` 的有效绑定迁到申请人；HTTP 面是 `PersonRebindController` 五方法 |
| 探测 | probe | `POST /probe`。`identityKey = upper(type) + ":" + upper(number)`，SQL `CASE` 四态 |
| 匹配 | match | 只读精确候选。成功 `REBIND_AVAILABLE`；否则 `NOT_AVAILABLE`。不抛业务「不可用」 |
| 确认意图 | confirm intent | `dao.confirmIntent`：申请单 `rebind_intent='Y'` + 目标档案 + 期望 binding 及版本。**不**调用 `unbindBinding` |
| 提交换绑 | submit rebind | 校验栅栏仍冻着 → 插 submission（拷贝三件套）→ 材料 `snapshotImmutable` → `WAITING` → 核身 + `workflow.start` |
| 自助解绑 | self unbind | 当前登录人自己的 `ACTIVE`/`SUSPENDED` 绑定改 `UNBOUND`，事件 `sourceType=SELF_SERVICE`。不删档案、不删版本 |
| 发布换绑 | publish approved rebind | `publishApprovedRebind`：旧绑定 UNBOUND + 新 ACTIVE + 新 CURRENT 版本 + 申请 FINISH。无 HTTP 映射 |
| 意图栅栏 | frozen binding fence | 申请/提交上的 `targetProfileId` + `expectedBindingId` + `expectedBindingVersion`；submit/publish 都要还能 `lockFrozenCandidate` / `lockExpectedBinding` |
| 可编辑状态 | editable statuses | Service 常量 `DRAFT`,`BACK`,`CANCEL`。`WAITING` 能被 lock 到，但不能 match/confirm/submit |
| 失败保持原绑定 | fail closed on binding | match/confirm/submit 失败以及 publish 版本竞态：`profile_person_binding` 旧行仍 `ACTIVE`，不插申请人新绑定 |
| 脱敏手机 | masked phone | 前 3 + 中间星号 + 后 4；长度 `< 8` → `"****"`。VO 禁止 `phone` / `profileId` / `bindingId` / `userId` |
| 类别码 | failure category | `PersonRebindException` 的 message 就是码（如 `PERSON_REBIND_NOT_AVAILABLE`），handler `R.fail(exception.getMessage())` |

错误出口：`PersonRebindExceptionHandler` 是 `@RestControllerAdvice(basePackageClasses = PersonRebindController.class)`，只拦本控制器抛出的 `PersonRebindException`。别的异常走全局处理。前端靠类别码区分「还不能换」和「版本冲突」，不要只读中文句子——这里根本没有中文句子。

接口遗产：`IPersonRebindService` 在 `service/` 里存在，2026-09-16 工作树**没有**生产实现类去 `implements` 它。生产 UseCase 直接持有具体类 `PersonRebindService`。`PersonRebindUseCase` 上无 `userId` 的 `match/confirm/submit/unbind` 标了 `@Deprecated`，默认抛 `UnsupportedOperationException("请传入 userId")`。Controller 走带 `userId` 的重载。

### 机制/因果链

五扇窗都先过 `@SaCheckPermission("profile:person:apply")`，再进 `PersonRebindUseCaseImpl`。五个业务方法（外加 `publishApproved` / `handleProcess`）都标 `@DSTransactional`。事务边界在 UseCase，不在 Controller，也不在 `PersonRebindProcessListener.handle`（合同测试断言 listener 的 `handle` **没有** `@DSTransactional`，它把事务交给 UseCase）。

身份：match / confirm / submit / unbind 用 `LoginHelper.getUserId()`。probe 的 UseCase 签名没有 userId。HTTP 方法参数表禁止出现 `long`/`Long`，避免调用方把操作者编号写进 JSON。

DAO 是 Mapper 唯一持有者。Service 不得（生产代码也没有）import `PersonRebindMapper`。XML 在 `src/main/resources/mapper/person/PersonRebindMapper.xml`。Mapper 继承 `BaseMapperPlus<ProfilePersonBinding, ProfilePersonBinding>`，换绑 SQL 仍是手写 XML，不是靠 BaseMapper 的 CRUD 自动换绑。

#### 1. `probe`——只问证件，不认人

`POST /profile/person/rebind/probe`。body：`documentTypeCode`（`@NotBlank` ≤64）+ `documentNumber`（`@NotBlank` ≤128）。command 空或规范化后缺字段 → `PERSON_REBIND_PROBE_INVALID`。

`identityKey` 只做 `upper(type) + ":" + upper(number)`。**不**跑证件号正则、不查证件类型表、不锁申请。SQL `selectProbeStatus` 按顺序：

1. 存在 `profile_person` `ACTIVE` 且绑定 `ACTIVE`/`SUSPENDED` → `BOUND`
2. 否则存在申请 `DRAFT|BACK|CANCEL|WAITING` 同一 `identity_key` → `IN_PROGRESS`
3. 否则存在 `ACTIVE` 档案但没有上面那种绑定 → `UNBOUND`
4. 否则 `AVAILABLE`

VO 只有 `status`。E2E 断言 JSON 里 **不存在** `personProfileId`。

#### 2. `match`——软拒绝，零写库（D 的一半）

`POST /match`。body 包一层 `{ identity: PersonRebindIdentityBo }`。前端厨房 `createPersonRebindService.match(identity)` 自己把 identity 再包进 `data: { identity }`。

Service 路径（任一条命中就 `unavailable()` = `PersonRebindMatchVo("NOT_AVAILABLE", null)`，**不抛**）：

- command / identity 规范化或完整性校验失败（`safeIdentity` 吞掉异常变 null）
- 申请人已经有有效档案（`findEffectiveProfileIdByUser != null`）——你自己还占着一张卡，不能再去抢别人的
- 没有开放申请，或状态不在 `EDITABLE_STATUSES`，或申请上的身份字段和这次提交的不完全相同
- 没有精确候选，或候选的 `oldUserId ==` 当前 userId（不能「换绑给自己」）

精确候选 SQL：档案 `ACTIVE` + 绑定 `ACTIVE` + 姓名/证件类型/号码/`identity_key`/性别/生日/`valid_from <=> ` / `valid_until <=> ` 全等。`<=>` 是 MySQL NULL-safe 比较，有效期两边都空也能对上。

成功才调 `UserService.selectPhonenumberById(oldUserId)` 做脱敏。错误姓名那一枪 E2E/单元测试都要求：`maskedPhone` 为 null / JSON 字段不存在，并且 **不会** 为了错误姓名去查手机。

match **没有** UPDATE/INSERT。这就是 D 路径「match 失败不改绑定」的机械原因：失败分支连写库函数都进不去。

#### 3. `confirm`——冻栅栏，不换口袋（矩阵 b）

`POST /confirm`。body：`identity` + `expectedVersion`（`@PositiveOrZero`）。`BusinessType.UPDATE` 指的是申请单版本 +1，不是 binding 换人。

硬前置（失败即 `PersonRebindException`，事务回滚）：

1. `userId <= 0` → `PERSON_USER_INVALID`
2. command 空、身份不完整/证件规则不过 → `PERSON_REBIND_NOT_AVAILABLE`（`requireIdentity` 把内部 `PERSON_REBIND_IDENTITY_*` 收成这一码）
3. 没有开放申请 → `PERSON_REBIND_APPLICATION_REQUIRED`（`lockOpenApplication` `FOR UPDATE`，状态集合含 `WAITING`，但下一步立刻用 `EDITABLE_STATUSES` 挡掉 WAITING）
4. 申请状态不可编辑、`version != expectedVersion`、申请字段与 identity 不一致 → `PERSON_REBIND_NOT_AVAILABLE`
5. 申请人已有有效绑定 → `PERSON_REBIND_APPLICANT_ALREADY_BOUND`
6. 精确候选缺失或 `oldUserId == userId` → `PERSON_REBIND_NOT_AVAILABLE`
7. `lockCandidate`：`lockFrozenCandidate(profileId, bindingId, bindingVersion) FOR UPDATE`，锁到的行必须仍 `ACTIVE` 且身份全等，否则 `PERSON_REBIND_NOT_AVAILABLE`
8. `dao.confirmIntent(...)` 必须恰好改 1 行，否则 `PERSON_REBIND_VERSION_CONFLICT`

`confirmIntent` XML 只碰 `profile_person_application`：

```sql
update profile_person_application
   set target_profile_id = ?, rebind_intent = 'Y',
       expected_binding_id = ?, expected_binding_version = ?,
       version = version + 1, update_time = current_timestamp, update_by = ?
 where person_application_id = ? and applicant_user_id = ?
   and status in ('DRAFT','BACK','CANCEL') and version = ? and del_flag = '0'
```

没有 `update profile_person_binding`。单元测试 `verify(service, never()).unbindBinding(eq(202L), any())`。MySQL E2E 在 confirm 成功、`session.commit()` 之后立刻读旧 `person_binding_id`，状态仍是 `ACTIVE`。

成功 VO：`("CONFIRMED", maskedPhone, expectedVersion + 1)`。合同测试锁死 record 组件名正好是 `status, maskedPhone, version`，**不含** `profileId, bindingId, userId, phone`。

失败保持原绑定的因果：异常在 UseCase 事务里抛出 → 回滚申请单那一行（如果 confirmIntent 还没跑或跑了也一并回滚）→ binding 表本就没被这条路径写过。这就是 **(b) confirm 改绑定失败保持原绑定** 在磁盘上的意思：矩阵把「改绑定」说成这扇窗的职责；实现把「改」限制在申请意图，把「绑定行」留给以后的 publish；无论哪一层失败，旧 `ACTIVE` 行还在。

#### 4. `submit`——开工单，仍不换口袋

`POST /submit`。body 只有 `expectedVersion`。身份不再传一遍，以锁到的申请为准。

链路：

1. `lockOpenByUserId`（申请 DAO，`FOR UPDATE`，状态含 WAITING）
2. 申请人必须是自己、状态可编辑、`version == expectedVersion`，否则 `PERSON_REBIND_VERSION_CONFLICT`
3. `validateComplete`（姓名/证件/性别集合 `MALE|FEMALE|UNKNOWN`/生日不晚于今天/证件类型正则/需要有效期则起止都在且未过期）
4. `providers.requireEnabled` 失败改写成 `PERSON_PROVIDER_UNAVAILABLE`
5. `requireApplicantUnbound`
6. `requireFrozenCandidate`：申请必须已经 `rebindIntent` 且三件套非空，否则 `PERSON_REBIND_CONFIRMATION_REQUIRED`；再 `lockFrozenCandidate`，对不上 → `PERSON_REBIND_BINDING_CHANGED`
7. 材料 `validateRequired(WORKING, documentType, {ALWAYS})`
8. 插 submission，把 `rebindIntent/target/expectedBinding*` 拷进快照；`DuplicateKeyException` → `PERSON_SUBMISSION_CONFLICT`
9. 材料 WORKING → SUBMISSION `snapshotImmutable`
10. `markWaiting`；失败 `PERSON_APPLICATION_VERSION_CONFLICT`
11. `attempts.startAttempt`（指纹含 `"REBIND"`）；失败 `PERSON_PROVIDER_UNAVAILABLE`
12. `workflow.start(applicationId, submissionId, snapshotVersion)`

返回 `WAITING` + `submissionSeq` + 新 version。单元测试 `submitKeepsTheOldBindingAndStartsReviewFromImmutableSnapshot`：`never().unbindBinding`。栅栏变了：`rejectsSubmitWhenTheFrozenBindingFenceChanged` —— 不插 submission、不 `workflow.start`。E2E 在 submit 之后旧 binding 仍 `ACTIVE`，申请才是 `WAITING`。

#### 5. `unbind`——解自己，不删人

`POST /unbind`。无 body。`unbindBinding`：`lockEffectiveBindingByUser` 找不到 → `PERSON_BINDING_NOT_FOUND`；`dao.unbind` 必须改 1 行否则 `PERSON_BINDING_VERSION_CONFLICT`；再插事件 `UNBOUND` / `SELF_SERVICE` / `PERSON_SELF_UNBOUND`。XML 把 binding `status='UNBOUND'`、`binding_version+1`、写下 `unbound_time`。持久化测试：`never().updateProfile`。E2E 自助解绑后：该档案下 `ACTIVE` 绑定数为 0，`profile_person.status` 仍 `ACTIVE`，版本行数不变。

这扇窗**不是**换绑成功路径的最后一步。换绑成功路径的最后一步没有 HTTP。

#### 6. 真正换口袋：`publishApprovedRebind`（对照，不进 (a) 五法）

`PersonRebindProcessListener` `@EventListener` + `@Order(Ordered.HIGHEST_PRECEDENCE)`，把 `ProcessEvent` 收成 `PersonRebindProcessCommand`（`profileDecision`、`snapshotVersion`）。UseCase `handleProcess`：flowCode 必须等于配置 `profile.person.flowCode`，状态规范化后是 `FINISH`，决定不是 `REJECT`。缺 applicationId / snapshotVersion 就静默 return。

`publishApprovedRebind`：

- 申请不是 `WAITING`、snapshot 对不上、`rebind_intent` 不是 `Y` → `Optional.empty()`（普通申请到这里会空手离开，把舞台让给 L-035 的监听器）
- 提交行 `rebind_intent` 不是 `Y`，或申请与提交身份/三件套不一致 → `PERSON_REBIND_SNAPSHOT_INVALID`
- `lockExpectedBinding` 锁不到（别人已经改了 binding_version 或不再 ACTIVE）或档案字段对不上 → `PERSON_REBIND_BINDING_CHANGED`；持久化测试断言：**在任何** `supersedeVersion` / `unbind` / `insertBinding` / `finishApplication` **之前**就停
- 申请人此时若已有有效绑定 → `PERSON_REBIND_APPLICANT_ALREADY_BOUND`
- 成功则同一事务：旧版本 SUPERSEDED、插新 CURRENT 版本、更新档案字段、旧 binding UNBOUND + 事件、插新 ACTIVE binding + 事件、申请 `FINISH` / `APPROVED` / `WORKFLOW`
- `DuplicateKeyException` → `PERSON_REBIND_PUBLICATION_CONFLICT`

E2E `bindingVersionRaceRollsBackWithNoSwitchOrNotification`：人工把 `binding_version` 改成 2 再 publish，抛 `PERSON_REBIND_BINDING_CHANGED`，rollback 后申请仍 `WAITING`、旧 binding 仍 `ACTIVE`、申请人名下 0 条绑定、版本仍 1、通知审计 0 行。这是 D 路径在「审核后换口袋」这一层的同构：失败保持原绑定。

成功后 UseCase 还做：材料 SUBMISSION → VERSION 快照；`notifications.stage(event)`；`events.publishEvent(PersonReboundEvent(profileId, applicationId, oldUserId))`。`PersonRebindNotificationListener` `@DsTxEventListener` 在提交后再 `notifyOldAccount`。E2E 里通知渠道抛 `offline`：绑定已经换完，审计记 `FAILED`（两行），**不会**回滚口袋。不要把「通知失败」说成 D 格子的「不改绑定」。

### 图、表或文本图

```text
旧账号 202 口袋                     申请人 101 的申请单              档案 9201
 binding 9301 ACTIVE                 DRAFT / version=3               ACTIVE
 binding_version=2                   身份字段 = 档案字段              identity_key 对齐
        │                                    │
        │  match 失败（错名）                  │
        │  ── 零写；两列都不变 ──              │
        │                                    │
        │  confirm 失败（版本/未绑栅栏/已绑定） │
        │  ── 回滚；9301 仍 ACTIVE ──          │
        │                                    ▼
        │                          confirm 成功：rebind_intent=Y
        │                          冻 9201 / 9301 / v2 ；version=4
        │                          9301 仍 ACTIVE  ← 关键
        │                                    │
        │                          submit 成功：WAITING + 快照
        │                          9301 仍 ACTIVE
        │                                    │
        │                          publish 失败（栅栏版本变了）
        │                          ── 回滚；9301 仍 ACTIVE；101 无新绑定
        │                                    │
        ▼                                    ▼
 publish 成功（同一事务）
 9301 UNBOUND v3 + 事件                     新 binding ACTIVE v1 给 101
 申请 FINISH                                新 CURRENT 版本
 事务后再通知 202（失败也不撤回上面四行）
```

**图题 / caption：** 失败路径 profile 换绑（`D:fail-profile-rebind`）与 confirm 栅栏的时间线。alt：match/confirm/submit/publish 失败时旧 binding 保持 ACTIVE；confirm 成功也只改申请单；真正 UNBOUND+新 ACTIVE 只在 publish 成功事务里。

**文字等价物：** 左边是旧持有人的有效绑定，右边是申请人的草稿。match 写错姓名时两边的表都不 UPDATE。confirm 失败时 UseCase 事务回滚，左边仍 `ACTIVE`。confirm 成功只在右边申请单写下「我要的是档案 9201、绑定 9301、版本 2」，左边仍 `ACTIVE`。submit 把右边改成等待审核，左边还是 `ACTIVE`。只有工作流 FINISH 且三件套还能锁上，才在同一事务把左边改 `UNBOUND`、给申请人插新 `ACTIVE`。任何一枪在锁栅栏时发现版本变了，就抛 `PERSON_REBIND_BINDING_CHANGED`（或 confirm 阶段的 `NOT_AVAILABLE`），旧行保持原状。通知在事务之后，失败只记审计。

**图的边界：** 这张图不画材料 WORKING/SUBMISSION/VERSION 三层 owner，不画核身供应商回调（L-038），不画管理端 `manageBinding`（L-034）。unbind 是申请人（或任何人）解**自己**的有效绑定，可以从图上任意 `ACTIVE` 口袋出发，不经过 confirm。probe 的四态是另一张只读表，不出现在这条时间线上。

### 正例、反例与边界

**正例 1：** HTTP 合同：`@RequestMapping` 正好 `"/profile/person/rebind"`；每个声明方法都有 `@PostMapping`、`@SaCheckPermission("profile:person:apply")`、`@Log` 且不存请求/响应体；参数类型不含 `long`/`Long`。

**正例 2：** 隐私 VO：probe 只有 `status`；match 只有 `status, maskedPhone`；confirm 只有 `status, maskedPhone, version`。E2E probe/match JSON 没有 `personProfileId`。

**正例 3：** 单元测试精确匹配才脱敏：`张三` + 完整证件 → `REBIND_AVAILABLE` / `138****8000`；`错误姓名` → `NOT_AVAILABLE` / `maskedPhone=null`。短号 `1234567` → `****`。

**正例 4：** confirm 成功后 `version=4`，`never unbindBinding(202)`。E2E confirm 后 `select status from profile_person_binding where person_binding_id=970000000201` = `ACTIVE`。

**正例 5：** submit 后申请 `WAITING`、`snapshotVersion=1`，旧 binding 仍 `ACTIVE`；`workflow.start(9001, 9101, 1)` 被调用。栅栏变了则 `PERSON_REBIND_BINDING_CHANGED`，不插提交、不开工单。

**正例 6：** 工作流 FINISH 后旧 binding `UNBOUND`、申请人名下 1 条 `ACTIVE`、UNBOUND 事件 `binding_version=2`、ACTIVE 事件给新 user、版本行变成 2、申请 `FINISH`。

**正例 7：** 绑定版本竞态：publish 抛 `PERSON_REBIND_BINDING_CHANGED`，rollback 后申请 `WAITING`、旧 `ACTIVE`、申请人 0 绑定、无通知审计。

**正例 8：** 自助 unbind 后档案仍 `ACTIVE`、版本行数不变、有效绑定清零。事件 `sourceType=SELF_SERVICE`。

**正例 9：** 通知渠道 offline：换绑事务已提交，审计 `FAILED` 两行，绑定保持已切换状态。通知失败 ≠ D 格子。

**正例 10：** 前端对照（格子仍归后端）：`createPersonRebindService` 五枪 URL 与 method 全是 `post`，confirm 带 `{ identity, expectedVersion }`，unbind 无 data。权限常量 `profilePermissions.person.apply = 'profile:person:apply'`。本课不认前端工厂格子（L-039）。

**反例 1：** 「confirm 成功 = 旧账号已经解绑。」测试名和 E2E 都说还没有。

**反例 2：** 「match 失败会抛 `PERSON_REBIND_NOT_AVAILABLE`。」match 走 `unavailable()` 软拒绝。那条码是 confirm 的硬拒绝。

**反例 3：** 「probe 用当前登录人的档案。」probe 不读 `userId`，只读证件拼出的 `identity_key`。

**反例 4：** 「五扇窗有 GET。」合同测试要求每个方法都有 `@PostMapping`。unbind 也是 POST 空 body，不是 DELETE。

**反例 5：** 「Controller 注入 `IPersonRebindService`。」注入的是 `PersonRebindUseCase`。`IPersonRebindService` 生产未接线。

**反例 6：** 「这是 classic，Service 直接持 Mapper。」登记表 layered；生产 Service 持 DAO。测试夹具 `PersonRebindServiceImpl` 才在 test 源码里 new Dao(mapper)。

**反例 7：** 「unbind 把 `profile_person` 删掉或改成无效。」只改 binding；档案 `ACTIVE` 还在。

**反例 8：** 「submit 会把旧绑定换过来，因为已经 CONFIRMED。」submit 明确 `never unbindBinding`。

**反例 9：** 「换绑发布走普通 `PersonApplicationProcessListener`。」rebind 监听器 `HIGHEST_PRECEDENCE`；`rebind_intent != Y` 时 publish 直接 empty，把普通申请让出去。

**反例 10：** 「失败码是中文。」handler 把 exception message（类别码）原样放进 `R.fail`。

**反例 11：** 「confirm 的 `BusinessType.UPDATE` 表示更新了 binding。」它更新申请单 version 与意图字段。

**反例 12：** 「申请人已经绑定别的档案也能换。」`requireApplicantUnbound` / match 里 `findEffectiveProfileIdByUser != null` 直接不可用。

**反例 13：** 「候选人是自己（oldUserId == userId）也能 match。」`unavailable()`。

**反例 14：** 「这五扇窗会回 `profileId` 给前端接着调用管理端 binding。」VO 合同禁止内部编号。

**反例 15：** 「`handleProcess` 是第六个 HTTP 方法。」它是 listener → UseCase，无 `@RequestMapping`。

**反例 16：** 「企业 `EnterpriseTransferController.confirm` 和本课 confirm 同一套。」那是 L-042，门牌 `/profile/enterprise/transfer`。

**反例 17：** 「D 格子包含通知失败回滚绑定。」E2E 证明通知失败时绑定已经切完。

**反例 18：** 「`PersonRebindServiceImpl` 在 main。」它在 `src/test/java/.../service/impl/`。

**边界：**

- `lockOpenApplication` SQL 含 `WAITING`，但 confirm/match/submit 的可编辑集合不含它。WAITING 申请再 confirm → `PERSON_REBIND_NOT_AVAILABLE`（或 submit 的 `VERSION_CONFLICT` 路径，取决于哪道闸先打）。
- confirmIntent 的 WHERE 也只允许 `DRAFT|BACK|CANCEL`。即便 Service 漏检，0 行会变成 `PERSON_REBIND_VERSION_CONFLICT`。
- `valid_from <=> #{validFrom}`：两边都 NULL 算相等；一边 NULL 一边有值不相等。
- probe 的 `BOUND` 把 `SUSPENDED` 绑定也算「被人拿着」；精确候选 / 冻栅栏只认 `ACTIVE` 绑定。暂停中的绑定能被 probe 看见，不能被 match 选中。
- 证件类型规则只在 match/confirm/submit 的 `validateComplete` 出现；probe 不校验正则。乱填号码的 probe 仍可能得到 `AVAILABLE`。
- `PersonRebindUseCase` 无 userId 的默认方法会抛「请传入 userId」。不要在新入口走这四个 deprecated 方法。
- 异常若不是 `PersonRebindException`，不会走本类 handler。
- E2E 要 `PROFILE_MYSQL_E2E_URL` 才跑；口试主证据仍是 HTTP 合同测试 + Service 单元/持久化测试。本课不把 E2E 环境当作学习者机器上必须跑绿。
- 核身供应商、材料标签、管理端 `manageBinding` 都可能改变同一张 binding；那些入口的失败路径不在本课 D 格子里。本课 D 只认 **rebind 这条河上的 match/confirm（及同构的 submit/publish 栅栏失败）**。

## 变式与迁移

- **变式 A：证件尚无人占用。** probe=`AVAILABLE`。match 找不到 `ACTIVE` 候选 → `NOT_AVAILABLE`。应去 L-035 普通申请，不要对空空气 confirm。
- **变式 B：档案在、绑定不在。** probe=`UNBOUND`。精确候选要求 `b.status='ACTIVE'`，match 仍不可用。这是「卡在馆里没人借」，不是换绑。
- **变式 C：同身份已有 WAITING 申请。** probe 可能 `IN_PROGRESS`（申请闸在 BOUND 之后）。持有人那边若仍 ACTIVE，probe 其实先返回 `BOUND`。口试要按 SQL 顺序说，不要发明第五态。
- **变式 D：confirm 时 expectedVersion 过期。** 申请 version 对不上 → `PERSON_REBIND_NOT_AVAILABLE`；即便撞上 confirmIntent 的 version 谓词，也是 0 行 → `PERSON_REBIND_VERSION_CONFLICT`。旧绑定不动。调用方应重新 GET 申请（L-035 `current`）拿到新 version，不要把旧 version 重放当幂等成功。
- **变式 E：confirm 后、submit 前旧持有人自己 unbind 或管理端改了 binding。** submit 的 `lockFrozenCandidate` 锁空 → `PERSON_REBIND_BINDING_CHANGED`。申请可能仍 DRAFT 且意图还在，但不会进入 WAITING。
- **变式 F：submit 之后、FINISH 之前 binding_version 被改。** 就是 E2E 竞态：publish 失败、申请留在 WAITING、原绑定 ACTIVE。重放 FINISH 仍会失败，直到栅栏与真实 binding 再对齐（通常意味着这单已经作废，要另开流程）。
- **变式 G：工作流 REJECT 或状态不是 FINISH。** `handleProcess` 直接 return，不碰绑定。
- **变式 H：flowCode 配错。** 配置键 `profile.person.flowCode` 对不上事件 → 同样 return。空配置时 `expectedFlowCode()` 是空串。
- **变式 I：申请人在 WAITING 期间又给自己绑上了别的档案。** publish 抛 `PERSON_REBIND_APPLICANT_ALREADY_BOUND`。旧持有人那一行仍按「未成功 publish」保持原状。
- **变式 J：自己解绑后再申请别人的身份。** unbind 只清自己的口袋。随后的 probe/match/confirm 才是换绑河。不要把 unbind 说成换绑的 commit。
- **迁移口诀：** 先数五扇窗三格矩阵 → 分清软拒绝 match 与硬拒绝 confirm → confirm 只冻申请栅栏 → 旧 binding 直到 publish 成功才 UNBOUND → 任何一枪失败（含栅栏版本竞态）左边口袋不动 → unbind 是解自己不是换绑收尾。跳步会出现「把 confirm 说成已经换人」「把 match 失败当成异常码」「把通知失败说成回滚绑定」「把测试夹具 ServiceImpl 说成生产分层」。

## 常见误区

1. **「OBJ-36 包含 `createPersonRebindService`。」** 那是 OBJ-39。本课认 Controller 五方法 + confirm 性状 + D 失败路径。厨房 URL 只做对照。
2. **「confirm = 改 `profile_person_binding`。」** XML 只 UPDATE 申请单。测试禁止 `unbindBinding`。
3. **「矩阵 (b) 写『改绑定』所以成功时旧账号已解绑。」** 磁盘把「改」实现成意图栅栏；真正改口袋在 publish。口试要主动拆开这两层，不要和矩阵口号对着硬刚，也不要假装 confirm 已经切绑定。
4. **「match 失败会回滚。」** match 无写，无事务可回。HTTP 200 + `NOT_AVAILABLE`。
5. **「probe 需要 identity 全套字段。」** 只要类型和号码。
6. **「unbind 是换绑成功的第六枪。」** 它是自助解除**当前用户**有效绑定；换绑成功没有申请人 HTTP 收尾枪。
7. **「这是 classic / 走 `IPersonRebindService`。」** layered + UseCase 具体 Service。
8. **「Listener 自己开事务。」** `handle` 无 `@DSTransactional`；UseCase 有。
9. **「失败码是中文 msg。」** 类别码原样进 `R.fail`。
10. **「VO 会带回 oldUserId 方便通知。」** 通知用领域事件，不走 HTTP VO。
11. **「SUSPENDED 也能被 match 选中。」** probe 的 BOUND 含 SUSPENDED；候选 SQL 只要 ACTIVE。
12. **「五扇窗权限不同。」** 都是 `profile:person:apply`。菜单种子「个人认证申请」也是这一串。
13. **「`PersonRebindProcessListener` 要算进矩阵 (a) 第六法。」** 不算。口试可以提它是换口袋的触发器。
14. **「D 格子覆盖管理端 `manageBinding` 失败。」** 那是 L-034。本课 D 是 rebind match/confirm（及同河 submit/publish 栅栏失败）。
15. **「通知失败会保持原绑定。」** 通知在事务后；失败只写审计。
16. **「deprecated 无 userId 方法还能用，会自己从 LoginHelper 取。」** 生产默认方法直接抛。自己取 LoginHelper 的是 **测试** `PersonRebindServiceImpl`。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `PersonRebindController.java`。圈 `@RequestMapping("/profile/person/rebind")`。数五个 `@PostMapping`：`/probe` `/match` `/confirm` `/submit` `/unbind`。圈每扇上的 `profile:person:apply` 和 `@Log` 的 `isSaveRequestData/isSaveResponseData`。圈 `LoginHelper.getUserId()` 出现在哪四扇、probe 没有它。圈字段类型 `PersonRebindUseCase`。
2. 打开 `PersonRebindHttpContractTest`。顺着「只有当前账号命令 + 一把权限」「VO 不暴露内部编号」「listener 在普通监听器之前且 handle 无 `@DSTransactional`」三条断言看。
3. 打开 `PersonRebindUseCaseImpl`。圈每个方法上的 `@DSTransactional`。再打开 `PersonRebindUseCase` 里四个 `@Deprecated` 默认方法的 `UnsupportedOperationException`。
4. 打开 `PersonRebindMapper.xml` 的 `selectProbeStatus`、`selectExactCandidate`、`confirmIntent`、`unbind`。圈 confirmIntent **没有** binding 表。圈候选和冻栅栏的 `b.status = 'ACTIVE'`。圈 probe 把 `SUSPENDED` 算进 BOUND。
5. 打开 `PersonRebindService.confirm` 与 `confirmationFreezesTheCurrentBindingWithoutSwitchingIt`。圈 `never().unbindBinding`。对照 `submitKeepsTheOldBinding...` 与 `rejectsSubmitWhenTheFrozenBindingFenceChanged`。
6. 打开 `PersonRebindServicePersistenceTest.bindingVersionRaceStopsBeforeAnyProfileOrBindingMutation` 与 E2E `bindingVersionRaceRollsBackWithNoSwitchOrNotification`。圈失败后仍 `ACTIVE`、无新绑定、无通知。再对照 E2E 主路径：confirm/submit 之后 binding 仍 `ACTIVE`，listener `FINISH` 之后才 `UNBOUND`。

## 总结、词汇表与下一步

- **宏观五扇窗：** `/profile/person/rebind` 上的 probe / match / confirm / submit / unbind。矩阵 (a) 与磁盘一致。全 POST、全 `profile:person:apply`、全 `@Log` 且不存报文。
- **(b) confirm：** 成功冻申请意图栅栏并 version+1，返回 `CONFIRMED` + 脱敏手机；**不**切换 `profile_person_binding`。失败抛类别码，`@DSTransactional` 回滚，原绑定保持。
- **D `fail-profile-rebind`：** match 软拒绝零写；confirm 硬拒绝回滚；submit/publish 栅栏失败同样不切口袋。矩阵中文行是「失败路径 profile 换绑」。
- **真正换口袋：** 工作流 `FINISH` → 最高优先级 listener → `publishApprovedRebind` 同一事务 UNBOUND+ACTIVE。通知在事务后，失败不撤回。
- **unbind 是解自己。** 不删档案。不是换绑提交的收尾 HTTP。
- **分层：** layered；Controller 调 UseCase；事务在 UseCase；DAO 持 Mapper。测试夹具 `PersonRebindServiceImpl` 不是生产入口。

词汇表：`PersonRebindController` / `PersonRebindUseCase` / `PersonRebindService` / `PersonRebindDao` / `confirmIntent` / `rebind_intent` / `expectedBindingVersion` / `lockFrozenCandidate` / `REBIND_AVAILABLE` / `NOT_AVAILABLE` / `CONFIRMED` / `WAITING` / `publishApprovedRebind` / `PersonReboundEvent` / `PERSON_REBIND_BINDING_CHANGED` / `PERSON_REBIND_NOT_AVAILABLE` / `profile:person:apply` / fail-profile-rebind / maskedPhone。

下一步：普通申请 current/submit 是 OBJ-35；管理端档案与 `manageBinding` 是 OBJ-34；核身回调是 OBJ-38；材料是 OBJ-37；浏览器厨房 `createProfileService` / web-domain 是 OBJ-39；企业转移 confirm 是 OBJ-42。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / UseCase / `wta-api` | profile-person 换绑公开 HTTP 与分层入口 | `PersonRebindController`；`PersonRebindUseCaseImpl` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-modules/wta-profile` 登记为 layered | 登记表 profile 行 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 菜单权限串 `profile:person:apply` | 个人认证 / 个人认证申请种子 | 2026-09-16 |
| S-L036-01 | `controller/self/PersonRebindController.java`；`PersonRebindExceptionHandler.java` | 五扇 POST、共用权限与 `@Log`、probe 无 userId、handler `R.fail(message)` | 类注解与五个映射；`handle` | 2026-09-16 |
| S-L036-02 | `usecase/PersonRebindUseCase.java`；`usecase/impl/PersonRebindUseCaseImpl.java` | deprecated 无 userId 抛错；生产方法全 `@DSTransactional`；注入具体 `PersonRebindService` | 默认方法；五个 override | 2026-09-16 |
| S-L036-03 | `service/PersonRebindService.java`；`service/IPersonRebindService.java` | probe/match/confirm/submit/unbind/publish 因果；`IPersonRebindService` 生产未接线 | `match` 软拒绝；`confirm` 不 unbind；`requireFrozenCandidate` | 2026-09-16 |
| S-L036-04 | `mapper/person/PersonRebindMapper.xml`；`dao/PersonRebindDao.java` | probe 四态；候选只认 ACTIVE；confirmIntent 只改申请单；unbind 改 binding | `selectProbeStatus`；`confirmIntent`；`unbind` | 2026-09-16 |
| S-L036-05 | `controller/self/PersonRebindHttpContractTest.java` | 五 POST、一把权限、参数无 long、VO 组件名、listener Order 与无事务注解 | 三个 `@Test` | 2026-09-16 |
| S-L036-06 | `service/impl/PersonRebindServiceTest.java`；`PersonRebindServicePersistenceTest.java` | 脱敏；confirm/submit 不切绑定；栅栏变化拒 submit；publish 竞态在写库前停；self unbind 不改档案 | 具名测试方法 | 2026-09-16 |
| S-L036-07 | `service/impl/PersonRebindMySqlE2ETest.java` | confirm/submit 后 binding 仍 ACTIVE；FINISH 后才切换；竞态回滚无通知；通知失败不撤回 | 两个 `@Test` | 2026-09-16 |
| S-L036-08 | `listener/PersonRebindProcessListener.java`；`PersonRebindNotificationListener.java`；`frontend/packages/domains/profile/src/person/rebind/service.ts` | FINISH 入口；事务后通知；前端五枪 URL 对照（格子仍归后端） | `handle`；`@DsTxEventListener`；`createPersonRebindService` | 2026-09-16 |
