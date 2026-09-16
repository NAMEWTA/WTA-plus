---
lesson_id: L-004
objective_ids: [OBJ-04]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 16
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-3RD-04, S-3RD-05, S-3RD-06, S-3RD-08]
---

# Lesson 004：监控室只能看回放，不能替你打电话

## 学完你能做什么

你能口述只读观测切片：`ThirdObservabilityController` → `ThirdObservabilityUseCase` / `ThirdObservabilityUseCaseImpl` → `ThirdObservabilityService` → `ThirdInvocationDao` / `ThirdStatisticDao` → 对应 Mapper。你能把 **写入路径** 和这条管理读路径分开：写入发生在 `ThirdInvocationRecorderAdapter`（网关调用 `ThirdInvocationRecorderPort`），读路径从不 insert。你还能说出 invocation 近 7 天 / 最多 200 条、statistic 最多 200 条、以及脱敏后才落库。

## 先把宏观地图放在桌上

前三课都在改配置。这一课是监控室：两面墙，一面是每次逻辑调用的回放（`third_invocation`），一面是按日累加的计数（`third_statistic`）。管理员按可选的 `providerCode` 来看，不能从这里触发出站。

```text
读（管理）：
  GET /third/invocation/list     third:invocation:list
  GET /third/statistics/list     third:statistics:list
       ThirdObservabilityController
            字段 observabilityService : ThirdObservabilityUseCase   ← 名字像 Service，类型是 UseCase
                 -> ThirdObservabilityUseCaseImpl
                      字段 service : ThirdObservabilityService
                           -> ThirdObservabilityService
                                字段 invocationDao : ThirdInvocationDao
                                字段 statisticDao  : ThirdStatisticDao
                                     -> ThirdInvocationMapper.selectRecent
                                     -> ThirdStatisticMapper.selectRecent

写（出站，本课必须能指认，细节与网关咬合在 L-005）：
  ThirdGatewayAdapter.record / recordAttempt
       -> ThirdInvocationRecorderPort
            -> ThirdInvocationRecorderAdapter
                 字段 invocationDao : ThirdInvocationStore   （实现类就是 ThirdInvocationDao）
                 字段 statisticDao  : ThirdStatisticStore    （实现类就是 ThirdStatisticDao）
                 字段 logSink       : ObjectProvider<SysLogEventSink>
                 -> mapper.upsert + 可选日志槽
```

**类比：** 闭路电视控制室。你可以倒带、看计数，不能从监视器里把电话拨出去。

**类比失效处：** 有的监控系统能远程开门。这条切片 **没有 POST**，UseCase 注释写明 *Read-only management contract*。录像写入失败也 **不得** 改变网关已经返回给业务模块的结果——`ThirdGatewayAdapter` 把 recorder 异常吞掉。监控坏了，电话该打完还是打完；健康检查另算（`ThirdInvocationLogHealthIndicator`）。

## 核心概念与机制

### 直觉讲解

Controller 的字段叫 **`observabilityService`**，但它的 Java 类型是 `ThirdObservabilityUseCase`。这是本切片最容易说错的协作方：名字像厨师，实际是管家接口。真正的 `ThirdObservabilityService` 只出现在 UseCaseImpl 的 `service` 字段上。

两个 GET 都可选 `providerCode`。Service 把空白规范化成 `null`（`strip` 后空则 null）。`null` 表示不按供应商过滤，Mapper SQL 就没有 `and provider_code = ...`。

Invocation 查询还带时间下界：`LocalDateTime.now().minusDays(7)`。Mapper：`create_time >= #{from}`，可选供应商，`order by create_time desc limit 200`。Statistic：可选供应商，`order by stat_date desc limit 200`，**没有** 7 天截断。

读出来的 JSON 是已经脱敏过的 `sanitizedRequestJson` / `sanitizedResponseJson`。管理端不会再解密，因为写入时 `ThirdLogSanitizerAdapter` 已经打码并截断到 16KiB。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 观测控制器 | `ThirdObservabilityController` | `@RequestMapping("/third")`；协作方 `private final ThirdObservabilityUseCase observabilityService` |
| 用例 | `ThirdObservabilityUseCase` | `invocations(providerCode)`、`statistics(providerCode)` |
| 用例实现 | `ThirdObservabilityUseCaseImpl` | 协作方 `private final ThirdObservabilityService service`；**无** `@DSTransactional` |
| 读服务 | `ThirdObservabilityService` | 协作方 `ThirdInvocationDao invocationDao`、`ThirdStatisticDao statisticDao` |
| 调用行 | `ThirdInvocation` | 一次逻辑调用的回放：requestId、尝试次数、逻辑状态、失败分类、脱敏 JSON |
| 统计行 | `ThirdStatistic` | 按日 upsert 的计数：attempt/success/failure/timeout/rejected/quota |
| 读模型 | `ThirdInvocationVo` / `ThirdStatisticVo` | record；statistic Vo **没有** statisticId |
| 调用存储端口 | `ThirdInvocationStore` | `upsert(ThirdInvocation)`；DAO 实现 |
| 统计存储端口 | `ThirdStatisticStore` | `upsert(ThirdStatistic)`；DAO 实现 |
| 记录器端口 | `ThirdInvocationRecorderPort` | `record(...)` 与 `recordAttempt(...)` |
| 记录器适配器 | `ThirdInvocationRecorderAdapter` | 写库 + 写 `SysLogEventSink`；失败只打 warn |
| 脱敏 | `ThirdLogSanitizerAdapter` | 静态工具：屏蔽常见密钥头/字段名，截断字节 |
| 出站尝试 | `ThirdOutboundAttempt` | 一次物理 HTTP 的可脱敏细节；与逻辑调用行分开 |
| 健康指示 | `ThirdInvocationLogHealthIndicator` | Bean 名 `thirdOutboundLog`；看 `logSinkFailureCount` |

### 机制/因果链

#### 管理读：两条映射

1. **invocations**  
   `GET /third/invocation/list`，权限 `third:invocation:list`，无 `@Log`。  
   `observabilityService.invocations` → UseCaseImpl → `service.invocations`：  
   `invocationDao.findRecent(code, now-7d)` → `mapper.selectRecent`。  
   每行 `new ThirdInvocationVo(invocationId, requestId, providerCode, endpointCode, attemptCount, logicalStatus, failureCategory, httpStatus, durationMs, sanitizedRequestJson, sanitizedResponseJson, createTime)`。  
   **不映射** `providerErrorCode`（实体有该字段，Vo 没有）。

2. **statistics**  
   `GET /third/statistics/list`，权限 `third:statistics:list`。  
   `statisticDao.findRecent` → `selectRecent`。  
   Vo：`providerCode, endpointCode, statDate, attemptCount, successCount, failureCount, timeoutCount, rejectedCount, quotaValue`。  
   记录器写入时会 **upsert 两行**：一行带 endpointCode，一行 endpointCode=null 的供应商汇总。所以列表里同一天可能看到门级和供应商级两行。

Mapper SQL 用 `@SelectProvider` 拼字符串：供应商参数为 null 时不加 where（invocation 仍有时间下界；statistic 则全表最近 200）。这是当前实现，不是分页 API。

#### 写入路径（管理切片的上游）

`ThirdInvocationRecorderAdapter.record`：

1. 新 `ThirdInvocation`，id 用 `IdGeneratorUtil.nextLongId()`，`requestId` 来自 `ThirdPartyResponse.requestId()`。  
2. `logicalStatus` = `response.isSuccess() ? "SUCCESS" : "FAILURE"`。成功定义在 `ThirdPartyResponse.isSuccess()`：`category == NONE` 且 HTTP 2xx。  
3. `failureCategory` 存枚举 **name()**。  
4. body 经 `ThirdLogSanitizerAdapter.jsonValue(..., additionalSensitiveFields)` 再入库。  
5. `invocationDao.upsert`：按 `request_id` 等做 `insert ... on duplicate key update`（见 Mapper `@Insert`）。同一次逻辑调用多次 record 会覆盖尝试次数与状态，而不是无限插入。  
6. 再 `statisticDao.upsert` 两次（门级 + 供应商级）。SQL 把 attempt/success/failure/timeout/rejected **累加**，`quota_value` 取新值。当前 `quotaValue` 恒写 `0L`。  
7. timeout 计数：仅当 category 是 `TIMEOUT`。rejected 计数：`RATE_LIMITED` 或 `REJECTED`。  
8. 写库失败 catch 后 warn，不抛给网关。  
9. 另组一份 `THIRD_HTTP_INVOCATION` 事件：query/headers/body 都经过 sanitizer，再 `logSink.ifAvailable(sink -> sink.write(event))`。槽失败则 `logSinkFailureCount` +1。

`recordAttempt` **只写日志事件**（`THIRD_HTTP_ATTEMPT_START` / `THIRD_HTTP_ATTEMPT_FINISH`），不写 invocation 表。所以管理列表看到的是逻辑调用，不是每一次物理重试行。重试次数落在 `attemptCount` 字段里。

`ThirdInvocationLogHealthIndicator.health`：失败计数为 0 则 `Health.up()`，否则 `down()`，细节键 `sinkFailures`。它不读取 DAO。

Skill 把 observability 写进 service。Java 里读是 `ThirdObservabilityService`，写是 `adapter/observability/ThirdInvocationRecorderAdapter`。按 Java 教。

### 图、表或文本图

**图题 / caption：** 读写分叉：同一张表，两个入口。

```text
ThirdGatewayAdapter (L-005)
    | recordAttempt  -->  日志事件 only
    | record         -->  ThirdInvocationRecorderAdapter
    |                       |  sanitizer
    |                       +--> invocationDao.upsert   --> third_invocation
    |                       +--> statisticDao.upsert x2 --> third_statistic
    |                       \--> SysLogEventSink (best effort)
    |
    |                                          另一扇门（只读）
    v                                          v
业务模块拿到 ThirdPartyResponse     GET /third/invocation|statistics/list
                                              ThirdObservabilityController.observabilityService
                                              ThirdObservabilityUseCaseImpl.service
                                              ThirdObservabilityService
                                              findRecent --> selectRecent limit 200
```

**文字等价物：** 出站网关在每次逻辑结果（以及每次物理尝试）上调用记录器端口。适配器把脱敏后的调用行 upsert 进 `third_invocation`，并把当日计数累加进 `third_statistic`（门级一行、供应商级一行）。尝试级细节只进日志槽。管理端两个 GET 从同一对 DAO 做 `selectRecent`：调用回放限制近 7 天且 200 行，统计限制 200 行。Controller 注入的字段名叫 `observabilityService`，类型却是 UseCase。记录失败不能修改已经分类好的网关响应；日志槽失败另由健康指示器暴露。

**图的边界：** 本图不教失败分类枚举的每一种如何产生（L-005）。不保证 200 行覆盖全部历史。不把 `sanitized_*` 当成可还原密文。`ThirdStatisticMapper.upsert` / `ThirdInvocationMapper.upsert` 的 SQL 写在注解上，不在 `ThirdMapper.xml`（那个 XML 只给 `ThirdProviderMapper.selectByProviderCode`）。

### 正例、反例与边界

**正例 1：** `GET /third/invocation/list?providerCode=AcmePay`。看到近 7 天该供应商最多 200 条，按时间倒序。字段里有 `FAILURE` + `TIMEOUT` 之类分类名，body 已被 `***` 或截断。

**正例 2：** 同一天同一门成功 1 次、超时 1 次。统计 upsert 累加后，该门 `attemptCount` 增加的是两次 record 各自带上的 attempts 总和，`successCount=1`，`timeoutCount=1`，`failureCount=1`。另有 endpoint_code 为空的供应商汇总行。

**正例 3：** 网关 recorder 抛异常。`ThirdGatewayAdapter.record` 的 catch 注释写明 *must not change the synchronous gateway result*。业务仍拿到原来的 `ThirdPartyResponse`。

**反例 1：** 给观测 Controller 加 POST“重放这次调用”。工作树没有。重放等于新的 `ThirdPartyGateway.execute`，应走业务模块 + 网关，并再写新回放。

**反例 2：** 以为 UseCaseImpl 还要开事务。读路径没有 `@DSTransactional`。写入在记录器里各自 upsert，也不经过 Observability UseCase。

**反例 3：** 把 Controller 字段 `observabilityService` 说成注入了 `ThirdObservabilityService`。类型是 UseCase；若真注入 Service 就跳过了 UseCase 层，违反 layered。

**边界：**

- `providerCode` 空白与缺省都会变成 SQL 不加供应商条件（Controller 里 `required = false`）。  
- invocation Vo 不含 `providerErrorCode`；实体列存在，管理读模型丢掉它。  
- `quotaValue` 目前记录器恒 0，列表里看见 0 不代表对接了供应商配额 API。  
- 脱敏 BLOCKED 集合包含 authorization、cookie、api-key、token、password 等子串；Endpoint 的 `sensitiveFieldsJson` 作为 **额外精确字段名** 传入。管理保存敏感字段名的地方在 L-002。  
- 健康 down 只表示日志槽失败计数非 0，不表示表没写上。

## 变式与迁移

- **变式 A：** 不传 providerCode，看到的是全局最近 200 条。多供应商环境里这会混在一起，不是分页。  
- **变式 B：** 一次调用网关重试了 3 次物理发送。管理 invocation 仍是 **一行**，`attemptCount` 反映逻辑次数；三次 START/FINISH 只在日志槽。  
- **变式 C：** 只关心统计墙。可以只打 `/third/statistics/list`。它不依赖 7 天窗口，但仍然 limit 200，旧日行可能被挤出结果集。  
- **迁移：** 观测不能替代告警系统。健康指示器只覆盖 sinkFailures。配置错误仍要回 L-001/L-002 修元数据。

## 常见误区

1. **“ObservabilityController 的 observabilityService 就是 ThirdObservabilityService。”** 看类型，不要看字段名。  
2. **“findRecent 是分页。”** 是 `limit 200` 的截断列表。  
3. **“统计表按次覆盖。”** upsert 对计数列是 `+ values(...)` 累加。  
4. **“管理读会再脱敏一次。”** Service 只 `new Vo(...)`。脱敏发生在写入。  
5. **“记录器失败应让调用失败，否则丢审计。”** 当前合同相反：出站分类稳定优先。审计尽力而为。

## 非评分暂停

打开 `ThirdObservabilityController`，确认只有两个 GET，字段类型是 UseCase。打开 `ThirdObservabilityUseCaseImpl`，确认唯一字段 `service`。打开 `ThirdObservabilityService`，确认两个 DAO。然后再打开 `ThirdInvocationRecorderAdapter` 的构造器三个参数，说清谁写谁读。

不要打分。能把读写画成两条箭头，OBJ-04 就站得住。

## 总结、词汇表与下一步

- 读协作方：Controller.`observabilityService`（UseCase）；UseCaseImpl.`service`；Service.`invocationDao` + `statisticDao`。  
- 写协作方：Recorder.`invocationDao`（Store）+ `statisticDao`（Store）+ `logSink`。  
- 两条 GET；invocation 7 天/200；statistic 200；upsert 累加；尝试事件不进管理表。  
- 脱敏后落库；记录失败不影响网关响应。

下一步：L-005 把出站整条管道走一遍——快照、加密、限流、SPI、HTTP 工厂、记录器。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-3RD-04 | `ThirdObservabilityController.java` | 两个 GET、权限、协作方 `observabilityService` | `controller/admin/ThirdObservabilityController.java` | 2026-09-14 |
| S-3RD-05 | `ThirdObservabilityUseCase` / `Impl` | 只读合同、协作方 `service` | `usecase/` | 2026-09-14 |
| S-3RD-06 | `ThirdObservabilityService` / invocation&statistic DAO&Mapper / Vo | findRecent、7 天、limit 200、Vo 字段 | `service/` `dao/` `mapper/` `domain/vo/` | 2026-09-14 |
| S-3RD-08 | `ThirdInvocationRecorderAdapter` / `ThirdLogSanitizerAdapter` / `ThirdInvocationLogHealthIndicator` / ports | 写入、脱敏、健康、best effort | `adapter/observability/` `adapter/log/` `port/` | 2026-09-14 |
