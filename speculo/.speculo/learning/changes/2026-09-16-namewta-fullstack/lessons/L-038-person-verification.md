---
lesson_id: L-038
objective_ids: [OBJ-38]
claimed_cells:
  - A:PersonVerificationAnonymousController.callback
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: anonymous-window-and-envelope
    minutes: 8
  - segment: authenticate-lock-complete
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L038-01, S-L038-02, S-L038-03, S-L038-04, S-L038-05, S-L038-06, S-L038-07, S-L038-08]
---

# Lesson 038：宏观核身回邮筒——口述 `PersonVerificationAnonymousController.callback`

## 学完你能做什么

打开 `backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/controller/anonymous/PersonVerificationAnonymousController.java`，你能**口述供应商怎么把核身结果塞进这一扇匿名窗**，而不是把「核身」说成申请人 `submit`，也不是把前端页 `PersonVerification` 说成本课门牌。

口试名单就是矩阵这一格，方法名以**磁盘**为准：

1. **`A:PersonVerificationAnonymousController.callback`**：唯一公开 HTTP。门牌 `POST /profile/person/verification/providers/{providerCode}/callback`。方法上 `@SaIgnore`。`@Log` 标题「个人认证供应商回调」，`BusinessType.OTHER`，**不**记请求体、**不**记响应体。返回 `R<PersonVerificationCallbackOutcome>`，成功 data 只可能是 `ACCEPTED` / `IDEMPOTENT` / `LATE_IGNORED`。

2026-09-16 工作树先钉死**三句话**，口试先数门牌，再数方法：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 申请人浏览器打这扇 POST | **供应商机器打。** 包在 `controller.anonymous`。架构测试：匿名面必须带 `Anonymous` 且源码含 `@SaIgnore`；`admin`/`self` **禁止** `@SaIgnore` |
| 回调成功 = 档案发布 / 申请变 `FINISH` | **E2E 明确不发布。** 尝试行变成 `SUCCEEDED`，申请仍是 `WAITING` |
| 失败也像 L-035 那样 HTTP 200 + `msg` 类别码 | **这扇窗改状态码。** 假签/过期 → **401**；其余业务拒收 → **400**；`data.category` 才是枚举名。`LATE_IGNORED` 仍是 **200** |
| 默认供应商 `manual` 也会收回调 | **`authenticate` 直接扔 `UNSUPPORTED_CALLBACK`。** 种子启用集合默认只有 `manual` |
| 前端页 `PersonVerification` 就是回调窗 | **那是自助申请页**（L-039）。厨房不替供应商打这扇 POST |
| 企业回调共用这扇门 | **另一块门牌** `/profile/enterprise/verification/providers/...`，L-041 |

`wta-profile` 在登记表是 **layered**：`Controller -> UseCase -> Service -> DAO -> Mapper -> XML`。核身这条河还多两根电线，仍不是 classic：`PersonVerificationProviderRegistryPort`、`PersonVerificationProvider`。不要口述成 `Controller → ServiceImpl → Mapper`。

本课**不宣称**你会拆个人档案审核/绑定（L-034）、自助申请 current/submit（L-035）、换绑（L-036）、材料 attach/detach（L-037）、厨房 `createProfileService`（L-039），或把企业匿名回调再讲一遍（L-041）。今天只认：**供应商怎么把一封已签名的回执投进个人核身邮筒，邮筒先验签，再按 PENDING 栅栏盖章，盖完不等于毕业。**

## 先把宏观地图放在桌上

L-003 已经把 `wta-profile` 钉在 layered 列。L-035 把申请人 `submit` 讲成：复印材料、把本子改成 `WAITING`、按门铃 `attempts.startAttempt(...)`、再按工作流门铃。**本课站在门铃之后、档案发布之前**：外面那家核身店把结果塞回来。`startAttempt` **没有**本课 HTTP。口试可以说「申请提交时已经写下 PENDING 尝试行」，不要说「回调窗会 start」。

2026-09-16 工作树：个人核身 HTTP 是**一块门牌、一扇窗**：

```text
供应商机器（无登录令牌）
        │
        └─ POST /profile/person/verification/providers/{providerCode}/callback
              PersonVerificationAnonymousController.callback   @SaIgnore
                    │
                    ├─ 校验 CallbackRequest
                    ├─ 把四字段折成 PersonProviderCallbackEnvelope
                    └─ 墙上钟 timeSource.now() 当作 receivedAt
                              │
                              v
              PersonVerificationUseCaseImpl.callback   @DSTransactional
                              │
                              v
              PersonVerificationAttemptService.handleCallback
                    （实现端口 PersonVerificationService）
```

往下走不要跳层：

```text
PersonVerificationAnonymousController
    └─ PersonVerificationUseCase / PersonVerificationUseCaseImpl   @DSTransactional
          └─ PersonVerificationService  ← 生产实现 PersonVerificationAttemptService
                ├─ PersonVerificationProviderRegistryPort
                │      requireEnabled(providerCode)
                │      → PersonVerificationProvider.authenticate(envelope, receivedAt)
                ├─ PersonVerificationAttemptDao -> PersonVerificationAttemptMapper.xml
                │      lockByProviderRequest / lockApplication / completeAttempt / insertSecurityAudit
                │      表 profile_verification_attempt / profile_person_application(+submission) / profile_operation_audit
                └─ PersonVerificationEvidenceCodec
                       入库证据 = schemaVersion + callbackDigest + providerEvidenceJson
```

`PersonVerificationCallbackExceptionHandler` 是 `@RestControllerAdvice(assignableTypes = PersonVerificationAnonymousController.class)`，**只罩这一份类**。不要把它和 `PersonApplicationExceptionHandler` 的「HTTP 仍 200、码写在 msg」混成一句。

**图题 / caption：** 个人核身匿名窗的宏观分层。alt：供应商 POST 进入 anonymous Controller；UseCase 盖事务章；Service 先验供应商适配器，再锁尝试行与申请行。

**文字等价物：** 调用方是供应商，不是已登录申请人。Controller 只做校验、折信封、看墙上钟、包装 `R`。事务章盖在 UseCase 上。真正认蜡封的是**该 `providerCode` 的适配器**，不是 Controller 里一把共享 secret。认完以后才按 `(providerCode, providerRequestId)` 锁尝试，再锁申请当前提交。成功只改尝试行，不改申请状态，不写档案，不改绑定。

**类比：** 把这扇窗想成学校门口的**匿名回邮筒**。快递公司把「核身做完了」的回执塞进来。邮筒不上学生胸牌锁（`@SaIgnore`），所以第一件事不是改成绩册，而是认这封信是不是这家店的蜡封。蜡封对了，才去柜子里找出那张还盖着「待处理」的回执单，核对是不是这本练习本指定的那家店，然后把回执单改成完成。

**类比失效处：**

1. 默认店是**人工老师**（`manual`）。老师不收回邮筒：`authenticate` 一律 `UNSUPPORTED_CALLBACK`。`startAttempt` 仍会记一张 PENDING 单，只是没有供应商请求号。
2. 时间窗不在 UseCase 里。测试适配器 `PersonDeterministicTestProvider` 才用 **300 秒** `|receivedAt - timestamp|`。换一家店，窗可以不同；不要把 300 秒说成 Controller 硬编码。
3. 迟到回执（申请已是 `FINISH` / `INVALID` / `TERMINATION`）**不是 400**。Service 写审计 `LATE_CALLBACK` 后返回 `LATE_IGNORED`，HTTP 仍 200。
4. 回执盖章 ≠ 毕业证。E2E 方法名就写着 `WithoutPublishing`：申请还是 `WAITING`。发布走 L-034 决定半段 / 工作流 Listener。
5. 通知模块那扇 `POST /notify/callback/{channel}`（OBJ-50）是另一只邮筒：共享 `notify.callback-secret`、HMAC 原文、投递状态单向升级。本课没有那把配置密钥，也不改通知投递行。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **先数层。** 登记表：`wta-modules/wta-profile` = layered。Controller 只持 UseCase + `PersonVerificationTimeSource`。UseCase 只持端口 `PersonVerificationService`。DAO 才持 Mapper。
2. **一扇窗。** 类上 `@RequestMapping("/profile/person/verification/providers")`，方法 `@PostMapping("/{providerCode}/callback")`。没有 GET，没有 DELETE，没有 start 窗。
3. **信封四件套。** `CallbackRequest`：`providerRequestId`（≤128）、`timestampEpochSecond`（`@Positive`）、`payload`（≤1048576）、`signature`（≤1024）。四件都 `@NotBlank`（时间戳除外）。Controller 折成 `PersonProviderCallbackEnvelope`，**原样**交给下游，自己不算 HMAC。
4. **墙上钟，不是信封上的钟。** `receivedAt = timeSource.now()`。生产实现 `SystemPersonVerificationTimeSource` 是 `Instant.now()`。单测把钟钉死，才能复现过期。
5. **权限是「忽略检查」。** 方法 `@SaIgnore`。没有 `@SaCheckPermission`。安全全靠适配器验签 + 申请上钉死的 `provider_code` + PENDING 栅栏。
6. **日志不许抄证件。** `@Log(..., isSaveRequestData=false, isSaveResponseData=false)`。payload 里可能有核身证据。
7. **先认店，再认蜡封，最后才动柜子。** `requireEnabled` → `provider.authenticate` → 失败立刻记审计（申请号常是 null）并抛出。成功才 `lockByProviderRequest`。
8. **三种成功口吻。** 第一次写完成 = `ACCEPTED`。同样证据再投 = `IDEMPOTENT`。申请已终态 = `LATE_IGNORED`（仍成功 HTTP）。
9. **假蜡封和过期是 401。** Handler：`INVALID_SIGNATURE`、`EXPIRED_CALLBACK` → `HttpStatus.UNAUTHORIZED`。别的 `PersonVerificationException` → `BAD_REQUEST`。body 固定英文 `Person verification callback rejected`，类别在 `data.category`。
10. **完成 SQL 自带栅栏。** `completeAttempt` 的 WHERE 含 `status = 'PENDING'` 且 `profile_type = 'PERSON'`。抢不到这一行 → `PROVIDER_FAILURE`（「pending fence」丢了）。
11. **证据入库不含 signature。** Codec 包一层 `{schemaVersion:1, callbackDigest, providerEvidenceJson}`。E2E 断言存库字符串 **不含** `"signature"`。
12. **适配器不许发毕业证。** 端口 javadoc：`authenticate` 只回规范化证据，**不得** publish 档案、**不得**改绑定。HTTP 成功只改 `profile_verification_attempt`。

**类比补一句：** 把 `providerRequestId` 想成回执单上的**快递单号**。柜子按「哪家店 + 单号」加锁找单，不按申请号从 URL 进来——路径上只有店名。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 匿名核身回调控制器 | anonymous callback controller | `PersonVerificationAnonymousController`；包 `controller.anonymous` |
| 供应商回调 | provider callback | `POST .../{providerCode}/callback`；Java 方法 `callback` |
| 回调信封 | callback envelope | `PersonProviderCallbackEnvelope(providerRequestId, timestampEpochSecond, payload, signature)` |
| 墙上钟 | time source | `PersonVerificationTimeSource.now()`；生产 `SystemPersonVerificationTimeSource` |
| 忽略登录 | `@SaIgnore` | 标在 **方法** 上，不是类上。HTTP 合同测试用反射确认 |
| 核身提供方 | verification provider | 端口 `PersonVerificationProvider`：`providerCode` / `start` / `authenticate` |
| 启用注册表 | provider registry | `PersonVerificationProviderRegistry` 实现 `PersonVerificationProviderRegistryPort` |
| 提供方编码 | provider code | 正则 `[a-z0-9][a-z0-9_-]{0,63}`；路径变量与申请列 `provider_code` 都走它 |
| 规范化回执 | verified callback | `PersonVerifiedCallback`：单号、digest、尝试状态、规范化 JSON、证据 JSON、错误码、完成时刻 |
| 尝试行 | verification attempt | 表 `profile_verification_attempt`；领域 `PersonVerificationAttempt`；实体 `ProfileVerificationAttempt` |
| 尝试状态 | attempt status | `PersonProviderAttemptStatus`：`PENDING` / `SUCCEEDED` / `FAILED` |
| 回调结局 | callback outcome | `PersonVerificationCallbackOutcome`：`ACCEPTED` / `IDEMPOTENT` / `LATE_IGNORED` |
| 失败分类 | failure category | `PersonVerificationFailureCategory` 十五个枚举；HTTP 只把其中一部分映射成 401/400 |
| 申请核身快照 | application verification state | `PersonApplicationVerificationState`；终态集合 `FINISH` `INVALID` `TERMINATION` |
| 相同回执 | same callback | `PersonVerificationAttempt.sameCallback`：digest、status、规范化 JSON、证据 JSON、errorCode 五件都等 |
| 证据信封 | evidence codec | `PersonVerificationEvidenceCodec`；`schemaVersion=1`；旧明文行仍可读 |
| 安全审计 | security audit | `insertSecurityAudit` 写入 `profile_operation_audit`；`operation_type=PROVIDER_CALLBACK`；`operator_user_id=0`；`capability=profile:person:providerCallback` |
| 共享端口 | `PersonVerificationService` | `startAttempt` + `handleCallback`；HTTP 只用后者 |

Controller 这一扇（矩阵只认它）：

| Java 方法 | HTTP | 权限 | `@Log` | 返回 |
| --- | --- | --- | --- | --- |
| `callback` | `POST /profile/person/verification/providers/{providerCode}/callback` | `@SaIgnore`（忽略检查） | OTHER，不记体 | `R<PersonVerificationCallbackOutcome>` |

UseCase 接口**只有** `callback(...)`。`startAttempt` 在端口 `PersonVerificationService` 上，由 L-035 的 `PersonApplicationService.startVerificationAttempt` 在 submit 事务里调用；失败被翻译成 `PERSON_PROVIDER_UNAVAILABLE`，不是本课 HTTP 码。

配置两把钥匙不要混：

| 配置 | 用途 | 谁读 |
| --- | --- | --- |
| `profile.person.provider.default`（系统配置，种子 `manual`） | 申请单上钉哪家店 | L-035 `defaultProvider()` |
| `profile.person.verification.enabled-providers`（默认集合 `{manual}`） | 注册表谁准接电话 | `PersonVerificationProviderProperties` |

店钉在申请上之后，回调路径上的 `{providerCode}` 必须和申请列一致，否则 `PROVIDER_MISMATCH`。注册表**没有**「未知店自动落到 manual」的 fallback。合同测试方法名：`resolvesOnlyTheConfiguredProviderWithoutFallback`。

### 机制/因果链

**1. HTTP 只做四件事，然后放手。**

口述 `callback` 方法体，不要发明第五步：

1. Bean Validation 过 `CallbackRequest`（空串、非正时间戳、超长 payload 进不了 UseCase）
2. `new PersonProviderCallbackEnvelope(request.providerRequestId(), request.timestampEpochSecond(), request.payload(), request.signature())`
3. `useCase.callback(providerCode, envelope, timeSource.now())`
4. `return R.ok(outcome)`

路径上的 `providerCode` **没有** `@Pattern`。非法编码（大写、空格）会在注册表 `validateCode` 变成 `INVALID_PROVIDER_CODE`，再被 Handler 收成 400。

**2. UseCase 只盖事务章。**

`PersonVerificationUseCaseImpl.callback` 一个 `@DSTransactional`，整段 `handleCallback` 同进同退：验签失败时写入的审计、冲突时写入的审计、成功时的 `completeAttempt`，都在这颗章下面。Controller 自己没有事务注解。

**3. 验签发生在锁柜子之前。**

`handleCallback` 开头：

```text
try {
    provider = registry.requireEnabled(providerCode)
    callback = provider.authenticate(envelope, receivedAt)
} catch (PersonVerificationException) {
    recordSecurityAudit(null, category, receivedAt)
    throw
}
```

所以：假签、过期、人工店拒收、未知店、停用店，审计行的 `application_id` 经常是 **null**。合同测试 `authenticatesCallbacksAndAuditsForgedOrExpiredPayloads`：先假签再过期，两次审计，`completeCount=0`。

`requireEnabled` 顺序：校验编码 → 找不到 Bean → `UNKNOWN_PROVIDER`；找到但不在 enabled 集合 → `DISABLED_PROVIDER`。启动期两个同名 Bean → `DUPLICATE_PROVIDER`（构造注册表时炸，不是一次回调）。

生产启用默认只有 `manual`。`PersonManualVerificationProvider.providerCode()` 返回 `"manual"`；`start` 回 `PersonProviderStartResult.pending()`（`providerRequestId=null`，`completedAt=null`）；`authenticate` **永远**扔 `UNSUPPORTED_CALLBACK`。

测试店 `PersonDeterministicTestProvider`（只存在 test 源码，code=`test-provider`）才演示「真收回执」：

- 规范串：`providerRequestId + "." + timestampEpochSecond + "." + payload`
- HMAC-SHA256，密钥是构造时传入的 secret；比较用 `MessageDigest.isEqual`（避免短电路计时）
- 签名不是 hex 或对不上 → `INVALID_SIGNATURE`
- `|receivedAt.epoch - timestamp| > 300`（或减法溢出）→ `EXPIRED_CALLBACK`
- 通过后：**无论 payload 是 `approved` 还是 `rejected`，尝试状态都写成 `SUCCEEDED`**。payload 原样进 `normalizedResultJson`；digest 是 payload 的 SHA-256 hex；证据 JSON 是 `{"provider":"test-provider"}`，**没有** signature 字段

口试不要把测试店的 300 秒 / HMAC 规范串说成生产 `manual` 的行为。生产默认店根本不验这四字段，直接拒收。

**4. 找到那张 PENDING 单，再锁申请。**

蜡封通过后：

1. `dao.lockByProviderRequest(providerCode, callback.providerRequestId())` → XML `FOR UPDATE`，且 `profile_type='PERSON'`、`del_flag='0'`。找不到 → `ATTEMPT_NOT_FOUND`
2. `dao.lockApplication(attempt.applicationId())` → 申请 **join** 当前 `submission_seq` 对应的提交行，也是 `FOR UPDATE`。找不到 → `APPLICATION_NOT_FOUND`（这条在 start 路径更常见；回调时尝试行已指向申请）
3. 申请上的 `providerCode` 必须等于路径店名，否则 `PROVIDER_MISMATCH`（测试会把申请改成 `other-provider` 来演示；`completeCount` 仍 0）
4. `application.terminal()` → 记审计 `LATE_CALLBACK`，**返回** `LATE_IGNORED`，不 complete
5. 尝试已不是 `PENDING`：
   - `attempt.sameCallback(callback)` → `IDEMPOTENT`，不写审计，不 complete
   - 否则 `CONFLICTING_CALLBACK`（合同测试：先 `approved` ACCEPTED，再同信封 IDEMPOTENT，再 `rejected` 冲突；全程 `completeCount=1`）
6. 仍是 PENDING → `complete(...)` → `ACCEPTED`

`sameCallback` 比的是**规范化证据**，不是 HTTP 原文是否字节相等。测试店把不同 payload 都标 `SUCCEEDED`，所以第二次投 `rejected` 会因 digest / normalized JSON 不同而冲突，**不是**因为状态变成 `FAILED`。

**5. complete 是栅栏更新，不是盲目 overwrite。**

`complete`：

1. `evidenceCodec.encode(digest, providerEvidenceJson)` 得到入库字符串
2. `dao.completeAttempt(id, callback.status().name(), normalizedResultJson, storedEvidence, errorCode, completedAt)`
3. XML：`SET status, normalized_result_json, provider_evidence_json, error_code, completed_time, version=version+1`，WHERE `verification_attempt_id` **且** `profile_type='PERSON'` **且** `status='PENDING'` **且** `del_flag='0'`
4. 更新行数不是 1 → `PROVIDER_FAILURE`（「completion lost its pending fence」）

并发两封同样的信：事务 + `FOR UPDATE` 让第二封看到已完成行，走 IDEMPOTENT。两封内容不同：第二封走 CONFLICTING，并追加审计。

**6. 审计行长什么样。**

`recordSecurityAudit`：

- `LATE_CALLBACK` → `result=IGNORED`；其余失败 → `result=FAILED`
- `insertSecurityAudit`：`profile_type='PERSON'`，`operation_type='PROVIDER_CALLBACK'`，`operator_user_id=0`（匿名，没有登录者），`capability='profile:person:providerCallback'`
- 插入失败 → `PROVIDER_FAILURE`

E2E 在冲突之后数：`profile_operation_audit` 里该 `application_id` 且 `failure_category='CONFLICTING_CALLBACK'` 的行数 = 1。

生产三参构造 `@Autowired` **不**接收测试里的 `PersonVerificationSecurityAuditRecorder`。四参构造把第四个参数标成 `ignoredAuditRecorder`，注释写明「不依赖测试审计记录器占位参数」。审计就在 Service 自己的 `dao.insertSecurityAudit`。不要口述成「生产还装配了一份 Recorder Bean」——那份类只在 **test** 源码。

**7. 尝试行怎么出现在柜子里（本课 HTTP 碰不到）。**

`startAttempt`（submit 调用）：锁申请 → 提交号必须仍是当前 `submissionId` 否则 `STALE_SUBMISSION`（**零** insertAttempt，测试 `retryRejectsAStaleSubmissionBeforeCallingAnyProvider`）→ 申请终态 → `APPLICATION_TERMINAL`（注意：回调终态不抛这个码，而是 `LATE_IGNORED`）→ `requireEnabled(申请上的店)` → `nextAttemptNo` → `provider.start` → `insertAttempt`，`profile_type` 写死 `'PERSON'`。单号冲突（唯一键 `uk_profile_verification_provider_request (provider_code, provider_request_id)`）→ `PROVIDER_FAILURE`。显式重试会追加 `attempt_no` 2、3…，店仍是申请上钉死的那家（`explicitRetryAppendsAnAttemptUsingTheProviderFixedOnTheApplication`）。

`PersonProviderStartResult.pending()` 的 `providerRequestId` 为 null。MySQL 唯一键对 NULL 不互斥，所以人工店可以多次 start。回调按单号找行：人工店本来就没有单号可对。

### 图、表或文本图

**图 1 标题 / caption：** 宏观回邮筒。供应商一封 POST 如何变成三种成功口吻或 401/400。

**alt：** 左列 HTTP 信封，中列 UseCase/Service/适配器，右列三张表。

```text
供应商
  POST /profile/person/verification/providers/{code}/callback
  body { providerRequestId, timestampEpochSecond, payload, signature }
        │  @SaIgnore   @Log 不记体
        v
  Controller.callback
        │  envelope + timeSource.now()
        v
  UseCase @DSTransactional
        v
  requireEnabled(code) ──未知/停用/非法码──► audit(app=null) ──► 400
        │
        v
  provider.authenticate ──假签/过期──► audit(app=null) ──► 401
                        ──manual 拒收──► audit(app=null) ──► 400 UNSUPPORTED_CALLBACK
        │
        v
  lock attempt by (code, requestId) FOR UPDATE
        │  找不到 ──► ATTEMPT_NOT_FOUND 400
        v
  lock application+current submission FOR UPDATE
        │  店名 ≠ 申请.provider_code ──► PROVIDER_MISMATCH 400
        │  申请 FINISH/INVALID/TERMINATION ──► audit LATE_CALLBACK, return LATE_IGNORED (200)
        │  尝试非 PENDING 且 sameCallback ──► IDEMPOTENT (200, 不 complete)
        │  尝试非 PENDING 且不同证据 ──► CONFLICTING_CALLBACK 400
        v
  completeAttempt WHERE status='PENDING'  ──► ACCEPTED (200)
        │
        ├─ profile_verification_attempt.status = SUCCEEDED（测试店）
        ├─ provider_evidence_json = codec 信封（无 signature）
        └─ profile_person_application.status 仍 WAITING     ← 不发布
```

**文字等价物：** 供应商把四字段 JSON 打到带店名的路径上。登录检查被忽略。Controller 折信封并盖墙上钟，交给带事务的 UseCase。Service 先问注册表这家店开不开门，再让店自己验签。验签失败时往往还没有申请号，但审计仍要写。验签成功才按店名+单号锁尝试行，再锁申请当前提交。申请已经终态就忽略回执并回 `LATE_IGNORED`。尝试已经完成且证据相同就幂等。证据不同就冲突。仍是 PENDING 才把状态写成适配器给出的终态，并把证据封进 codec。申请行保持等待。本图不画 submit 的 startAttempt，不画工作流 Listener，不画企业邮筒。

**图的边界：** 不保证 `openapi/current.json` 与生成文件同步——2026-09-16 `frontend/packages/api-contracts/generated/openapi.ts` **有**这条 POST，枚举 data 为 `ACCEPTED|IDEMPOTENT|LATE_IGNORED`；同目录 `openapi/current.json` **搜不到** `/profile/person/verification`。不要把快照说成本课 HTTP 的权威源。不保证生产环境存在 `test-provider`：那是测试夹具。不保证业务失败是 `R.fail(类别码)` 且 HTTP 200——那是申请窗；本窗失败是 401/400 + 英文 msg + `data.category`。

**图 2 标题 / caption：** 失败分类进哪扇门。左边是回调 HTTP 看得到的；右边是 startAttempt 才用、本课窗没有的。

```text
回调 HTTP 映射（Handler）
  401 UNAUTHORIZED   INVALID_SIGNATURE, EXPIRED_CALLBACK
  400 BAD_REQUEST    其余 PersonVerificationException
  200 R.ok           ACCEPTED, IDEMPOTENT, LATE_IGNORED

回调路径常见类别
  UNKNOWN_PROVIDER / DISABLED_PROVIDER / INVALID_PROVIDER_CODE
  UNSUPPORTED_CALLBACK          ← manual.authenticate
  INVALID_SIGNATURE / EXPIRED_CALLBACK
  ATTEMPT_NOT_FOUND / PROVIDER_MISMATCH / CONFLICTING_CALLBACK
  LATE_CALLBACK                 ← 只审计，结局是 LATE_IGNORED，不是抛给 400
  PROVIDER_FAILURE              ← complete 栅栏丢失 / 审计插不进 / 单号唯一键

startAttempt 才抛、本课窗不走
  STALE_SUBMISSION / APPLICATION_TERMINAL / APPLICATION_NOT_FOUND
  DUPLICATE_PROVIDER            ← 注册表构造期
```

**文字等价物：** Handler 只用两个 HTTP 失败码：假签和过期当未授权，别的当坏请求。`LATE_CALLBACK` 故意不当失败 HTTP。申请终态在 start 路径是 `APPLICATION_TERMINAL` 异常，在回调路径是忽略。口试把这两条说反，就是把 L-035 的门铃和本课邮筒接错线。

### 正例、反例与边界

**正例 1：** 申请 `WAITING`，尝试 `PENDING`，店 `test-provider` 已启用。供应商 POST 正确 HMAC。MockMvc `status().isOk()`，`$.data` = `ACCEPTED`。尝试行 complete 一次。

**正例 2：** 同一 body 再 POST 一次。仍 200，`$.data` = `IDEMPOTENT`。`completeCount` 仍是 1。E2E 里尝试行数仍是 1。

**正例 3：** 申请已被改成 `FINISH`。合法蜡封回来。200，`LATE_IGNORED`。零 complete。审计类别 `LATE_CALLBACK`，`result=IGNORED`。

**正例 4：** E2E `authenticatesHttpCallbackAndPersistsIdempotentEvidenceWithoutPublishing`：先 `startAttempt` 落库，再 HTTP ACCEPTED、再 IDEMPOTENT、再 conflicting → 400 `CONFLICTING_CALLBACK`。然后 SQL：尝试 `SUCCEEDED`，申请 `WAITING`，尝试行数 1，冲突审计 1 行，证据 JSON 不含 `signature`。

**正例 5：** 架构测试 `controllersUseOnlyExplicitAccessSurfaces`：这份类必须躺在 `controller/anonymous`，文件名含 `Anonymous`，源码含 `@SaIgnore`。

**反例 1：** 「回调成功会把申请改成 FINISH 并插入 `profile_person`。」E2E 断言申请仍 `WAITING`。发布是另一条河。

**反例 2：** 「这扇窗也要 `profile:person:apply`。」方法是 `@SaIgnore`。self 包禁止这个注解。

**反例 3：** 「失败像申请窗：HTTP 200，`msg=INVALID_SIGNATURE`。」Handler 设 401，msg 是固定英文，类别在 `data.category`。

**反例 4：** 「`manual` 默认店会按 HMAC 收 `approved`。」`authenticate` 直接 `UNSUPPORTED_CALLBACK`，400。

**反例 5：** 「未知店会落到 manual。」`requireEnabled("missing")` → `UNKNOWN_PROVIDER`。无 fallback。

**反例 6：** 「前端 `PersonVerification` 页提交就是这扇 POST。」那页走 L-035 的 `/profile/person/application`。组件名带 Verification，门牌不是 verification/providers。

**反例 7：** 「企业也可以打 `/profile/person/verification/providers/manual/callback` 改企业尝试。」XML 写死 `profile_type='PERSON'`。企业是 L-041 另一份 Controller。

**反例 8：** 「Controller 自己算 HMAC，密钥在 `application.yml`。」Controller 不算。测试店密钥在测试构造函数；生产 manual 无回调密钥。不要把 notify 的 `notify.callback-secret` 搬过来。

**反例 9：** 「第二次不同 payload 会把尝试改成 FAILED。」测试店两次都是 `SUCCEEDED` 意图；第二次因 `sameCallback` 失败而 **拒绝写入**，第一次的 SUCCEEDED 留着。

**反例 10：** 「`LATE_IGNORED` 是 400。」它是 `R.ok` 的三种 data 之一。

**反例 11：** 「审计 operator 是当前登录用户。」匿名，`operator_user_id=0`。

**反例 12：** 「UseCase 有 start 和 callback 两扇。」UseCase 接口只有 callback。start 在 Service 端口上。

**反例 13：** 「证据列是 DDL 注释说的纯明文 payload。」列注释写「明文」，Java 另包 schema 信封；旧行 decode 失败才整段当证据。以 codec 为准，把注释冲突说出来。

**反例 14：** 「生产装配了 `PersonVerificationSecurityAuditRecorder`。」该类只在 test；生产 Service 自己 insert。

**反例 15：** 「OpenAPI current.json 就是权威。」生成文件有路径，快照缺 `/profile/person/verification`。

**边界：**

- `timestampEpochSecond` 必须 `@Positive`。0 进不了 UseCase，不会被当成 1970-01-01 去比 300 秒窗。
- payload 上限 1 MiB。再大是校验失败，不是核身类别。
- 终态集合只有三个字符串，大小写敏感。`WAITING` 不是终态，所以审核中的申请仍收 ACCEPTED。
- `lockApplication` 必须 join 到当前 `submission_seq`。申请还在但提交行对不上，start 会 `APPLICATION_NOT_FOUND`；这不是回调窗的主路径。
- 测试店对 hex 解析失败也归 `INVALID_SIGNATURE`，不是单独的格式码。
- `PersonVerificationAttempt` compact：`applicationId/submissionId/attemptNo` 必须为正；PENDING 的 start 结果才允许 `completedAt==null`。
- layered 硬约束仍在：架构测试禁止 Controller import `service.`，禁止 Service import Mapper。测试夹具把 Mapper mock 塞进 DAO，不得回流生产 Controller。

## 变式与迁移

- **变式 A：默认人工店。** submit 钉 `manual` 并 `startAttempt` 记 PENDING。供应商若仍 POST `/manual/callback`，400 `UNSUPPORTED_CALLBACK`。老师在管理端审核（L-034），不走这只邮筒。
- **变式 B：以后接入真供应商。** 新类实现 `PersonVerificationProvider`，code 小写正则，放进 Spring 列表，并把 code 加入 `enabled-providers`。`authenticate` 只许回 `PersonVerifiedCallback`，不许碰档案表。回调 URL 就是本课这一扇，店名换路径变量。
- **变式 C：重复投递。** 快递公司至少一次。同样证据 → IDEMPOTENT。改口（先过后再拒绝）→ CONFLICTING，第一次证据不动。
- **变式 D：申请已经结案还回执。** 200 `LATE_IGNORED` + 审计 IGNORED。不要当成「补发布」钩子。
- **变式 E：对照通知回调。** OBJ-50：共享 secret、HMAC 原文、300 秒在 UseCase、投递状态单向升级。本课：每店自己的 `authenticate`、钟在适配器、三种结局枚举、不改通知行。两只邮筒不要画成一个 Handler。
- **变式 F：对照申请窗失败合同。** L-035 Advice 把类别码放进 `msg`，HTTP 200。本课 Handler 改 401/400，msg 英文固定句。厨房若只看 HTTP 200 会把 `LATE_IGNORED` 当普通成功——它确实是成功枚举，但语义是忽略。
- **变式 G：企业对称。** `EnterpriseVerificationAnonymousController` 门牌 `/profile/enterprise/...`，格子在 OBJ-41，不在本课。口试说「企业也打 person 路径」即越界。
- **变式 H：前端对照。** `generated/openapi.ts` 已有路径与 `RPersonVerificationCallbackOutcome`。自助页 `PersonVerificationPage.vue` 文案是「提交后进入审核」，按钮走 application save/submit。本课不认 OBJ-39 格子。
- **迁移口诀：** 先数一块门牌一扇 POST → `@SaIgnore` 所以先验店再验签 → 三种 200 口吻与 401/400 分家 → complete 只动尝试行、申请仍 WAITING → manual 不收邮筒。跳步会出现「把申请页当回调窗」「把 ACCEPTED 说成已建档」「把假签说成 200+msg」。

## 常见误区

1. **「OBJ-38 还包含 `startAttempt` HTTP。」** start 没有窗。格子只有 `callback`。
2. **「`PersonVerification` 组件 = 本课 Controller。」** 组件是申请页；Controller 在 anonymous 包。
3. **「匿名所以谁都能改档案。」** 适配器契约禁止 publish/binding；E2E 钉死申请不升级。
4. **「验签在 UseCase 里用同一把 secret。」** 验签在 Provider；UseCase 只转发。
5. **「401 表示没登录。」** 这里 401 表示假签或过期。本来就不要求登录。
6. **「IDEMPOTENT 会再 complete 一次。」** 非 PENDING 且 sameCallback 直接 return。
7. **「冲突会覆盖成最新 payload。」** 拒绝写入，留第一次证据，另记审计。
8. **「终态申请抛 `APPLICATION_TERMINAL`。」** 那是 start。回调是 `LATE_IGNORED`。
9. **「enabled-providers 空时所有店都开。」** 默认集合是 `{manual}`；设成空则谁都 `DISABLED`（找不到的仍 UNKNOWN）。
10. **「回调会 start 工作流。」** 工作流门铃在 submit。回调不调 `PersonWorkflowGateway`。
11. **「审计能当操作者追溯到供应商 IP。」** 行上操作者是 0，能力串是 `profile:person:providerCallback`。
12. **「DDL 唯一键 `uk_profile_verification_attempt_no` 能当回调幂等。」** 回调幂等靠单号锁 + `sameCallback`；attempt_no 唯一是防同一申请重复序号。
13. **「layered 所以 Controller 可以碰 Mapper。」** 生产 Controller 只持 UseCase。测试才把 Mapper 塞进 DAO 夹具。
14. **「企业/个人共用一张尝试表就可以共用一个 Controller。」** 表按 `profile_type` 混住，但 XML 与门牌按模块切开。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `PersonVerificationAnonymousController.java`。圈 `@RequestMapping`、唯一的 `@PostMapping`、方法上的 `@SaIgnore`、`@Log` 的 `isSaveRequestData = false`。顺着方法体把四字段折成 `PersonProviderCallbackEnvelope`、把 `timeSource.now()` 传进 UseCase、把 `R.ok(outcome)` 圈出来。
2. 打开 `CallbackRequest` record。圈 `@Positive` 和三段 `@Size`。对照 Handler：只有 `INVALID_SIGNATURE` 与 `EXPIRED_CALLBACK` 走 401。
3. 打开 `PersonVerificationUseCase.java` 与 `PersonVerificationUseCaseImpl.java`。确认接口只有 `callback`；实现上 `@DSTransactional` 且字段类型是 `PersonVerificationService`。
4. 打开 `PersonVerificationAttemptService.handleCallback`。按源码顺序标：`requireEnabled` → `authenticate` 的 try/catch 审计 → `lockByProviderRequest` → `lockApplication` → mismatch → terminal `LATE_IGNORED` → 非 PENDING 的幂等/冲突 → `complete`。对照 `PersonVerificationCallbackContractTest` 四个 `@Test` 方法名。
5. 打开 `PersonManualVerificationProvider.java` 与测试夹具 `PersonDeterministicTestProvider.java`。圈 manual 拒收；圈测试店 300 秒、HMAC 规范串、`isEqual`、始终 `SUCCEEDED`。
6. 打开 `PersonVerificationAttemptMapper.xml`。圈 `lockByProviderRequest` 的 `FOR UPDATE` 与 `profile_type='PERSON'`；圈 `completeAttempt` 的 `status='PENDING'` 栅栏；圈 `insertSecurityAudit` 的 `operator_user_id=0` 与 `capability`。对照 `10-cde-base-ddl.sql` 的 `profile_verification_attempt` 两个唯一键。
7. 打开 `PersonVerificationMySqlE2ETest.authenticatesHttpCallbackAndPersistsIdempotentEvidenceWithoutPublishing`。圈 ACCEPTED → IDEMPOTENT → CONFLICTING 的 HTTP 状态；圈申请仍 `WAITING`；圈证据不含 `signature`。
8. 打开 `PersonModuleArchitectureTest.controllersUseOnlyExplicitAccessSurfaces`。把 anonymous 必须 `@SaIgnore`、self/admin 禁止 `@SaIgnore` 标成对照。打开前端 `PersonVerificationPage.vue` 只确认它走申请文案，不当本课入口。

## 总结、词汇表与下一步

- **宏观一扇窗：** `PersonVerificationAnonymousController.callback` 是个人核身唯一 HTTP。匿名、POST、不记体。
- **先验店再验签再锁单：** 注册表没有 fallback；验签在 Provider；锁在 DAO XML 的 `FOR UPDATE`。
- **三种 200：** `ACCEPTED` 写完成；`IDEMPOTENT` 同样证据；`LATE_IGNORED` 申请已终态。假签/过期 401，其它拒收 400。
- **盖章不是毕业：** 只改 `profile_verification_attempt`；申请仍 `WAITING`；不 publish、不改绑定、不 start 工作流。
- **默认店不收邮筒：** `manual.authenticate` → `UNSUPPORTED_CALLBACK`。真 HMAC 故事只在测试店。
- **失败合同与申请窗不同：** 类别在 `data.category`，msg 是英文固定句，HTTP 会变。
- **layered 五层 + 两根电线：** UseCase 事务；Service 规则；DAO/XML 锁与栅栏；Registry/Provider 是端口，不是第二套 Controller。

词汇表：`PersonVerificationAnonymousController` / `callback` / `@SaIgnore` / `PersonProviderCallbackEnvelope` / `PersonVerificationTimeSource` / `PersonVerificationUseCase` / `PersonVerificationService` / `PersonVerificationAttemptService` / `PersonVerificationProvider` / `PersonVerificationProviderRegistry` / `PersonVerifiedCallback` / `PersonVerificationCallbackOutcome` / `PersonVerificationFailureCategory` / `PersonProviderAttemptStatus` / `sameCallback` / `completeAttempt` / `PersonVerificationEvidenceCodec` / `profile_verification_attempt` / `profile_operation_audit` / `PROVIDER_CALLBACK` / `profile:person:providerCallback` / `manual` / layered。

下一步：申请 current/submit 何时 `startAttempt` 是 OBJ-35。管理端决定如何让 `WAITING` 变成档案是 OBJ-34。厨房与 web-domain 接到申请页是 OBJ-39。企业匿名回调是 OBJ-41。通知供应商回调是 OBJ-50，不要和本课邮筒合并。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / UseCase / `wta-api` | 个人核身公开入口在 person 模块 anonymous 包 | `wta-profile-person` `controller/anonymous` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-profile` = layered；按五层口述 | 登记表 layered 行 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql` | 尝试表、操作审计表、唯一键、`profile_type` | DDL `profile_verification_attempt` 约 1189–1214 行；`profile_operation_audit` 约 1241–1266 行 | 2026-09-16 |
| S-L038-01 | `PersonVerificationAnonymousController.java`；`PersonVerificationCallbackExceptionHandler.java` | 一扇 POST、`@SaIgnore`、信封校验、墙上钟、401/400 映射、英文 msg、`data.category` | 类与 `callback`；Handler `switch` | 2026-09-16 |
| S-L038-02 | `PersonVerificationUseCase.java`；`PersonVerificationUseCaseImpl.java`；`PersonVerificationAttemptService.java` | UseCase 只有 callback；`@DSTransactional`；handleCallback 顺序与三种结局 | UseCase 接口与实现；Service 约 82–125 行 | 2026-09-16 |
| S-L038-03 | `PersonVerificationProvider.java`；`PersonVerificationService.java`；`PersonVerificationProviderRegistry.java`；`PersonManualVerificationProvider.java`；`PersonVerificationProviderProperties.java` | 适配器不得 publish/改绑定；端口含 start+callback；无 fallback；默认 `{manual}`；人工店拒收 | Provider javadoc；Service 端口两方法；`requireEnabled`；`authenticate` throw | 2026-09-16 |
| S-L038-04 | `PersonVerificationAttemptMapper.xml`；`PersonVerificationAttemptDao.java`；`PersonVerificationEvidenceCodec.java` | PERSON 写死、FOR UPDATE、PENDING 栅栏、审计列、codec 信封 | XML `lockByProviderRequest`/`completeAttempt`/`insertSecurityAudit`；codec `SCHEMA_VERSION=1` | 2026-09-16 |
| S-L038-05 | `PersonVerificationCallbackContractTest.java`；`PersonVerificationAnonymousControllerTest.java`；`PersonDeterministicTestProvider.java` | 假签/过期审计且不 complete；ACCEPTED→IDEMPOTENT→冲突；HTTP 200/401；300 秒 HMAC | 四个合同 `@Test`；Controller 测试方法名；测试店 `MAX_SKEW_SECONDS` | 2026-09-16 |
| S-L038-06 | `PersonVerificationMySqlE2ETest.java`；`PersonVerificationAttemptServiceTest.java`；`PersonVerificationProviderRegistryTest.java` | 不发布、证据无 signature、重试沿用申请店、过期提交零 insert、无 fallback | E2E 方法名 `WithoutPublishing`；coordinator 两测；registry 两测 | 2026-09-16 |
| S-L038-07 | `PersonModuleArchitectureTest.java`；`PersonApplicationService.java`（仅边界） | anonymous 必须 `@SaIgnore`；submit 才 `startAttempt` 并译成 `PERSON_PROVIDER_UNAVAILABLE` | `controllersUseOnlyExplicitAccessSurfaces`；`startVerificationAttempt` 约 782–788 行 | 2026-09-16 |
| S-L038-08 | `EnterpriseVerificationAnonymousController.java`（仅边界）；`PersonVerificationPage.vue`（仅边界）；`frontend/packages/api-contracts/generated/openapi.ts` | 企业另一门牌；申请页不是回调窗；生成 OpenAPI 有路径与三态 data | enterprise `@RequestMapping`；Vue 文案「提交后进入审核」；openapi 约 1972–1985、9655–9667 行 | 2026-09-16 |
