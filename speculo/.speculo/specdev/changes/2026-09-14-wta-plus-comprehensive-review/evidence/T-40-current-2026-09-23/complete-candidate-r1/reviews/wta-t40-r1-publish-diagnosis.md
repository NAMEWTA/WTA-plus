# T40 R1 公告发布失败只读诊断（2026-09-23）

固定输入：`/tmp/wta-t40/browser-runs/0c8f89704398d48a/result.json`，source `c860480914b5fc563dd119e760e37d5eb99064e3`，full JAR SHA-256 `59dda7ff6e9b8e94f903310bc71bbbab439b6ff50f087d825b2e23d4f2d56978`。只读源码与安全结果；未读取已删 raw log，未启动服务、测试或构建，也未改仓库。

## 已由本次运行证明

- fresh 六 SQL 初始化成功，业务表 103、Outbox 0、外部投递 0；后端探针 HTTP 200。控制账号、A、B 三次**无 User-Agent** 真登录都拿到令牌，并分别通过本人在线 DTO 与 `info_id` 基线后的异步成功审计。固定 Chrome UA 控制登录的在线 DTO/审计也通过。
- `POST /notify/notice/save` 返回成功，运行器随后以精确标题在 owned DB 找到唯一 `notice_id`。紧接着 `POST /notify/notice/{id}/publish` 返回 **HTTP 200 / R.code 500**，失败阶段 `notice_publish`，源码行 839；未到 Worker 送达、撤回或两例 Chrome。
- source/JAR 前后一致；进程组、owned 容器和卷、loopback 端口均已清理。安全结果没有 `R.msg`、异常类别、栈或失败时 DB 生命周期，因此尚不能把任何单一源头称为本次实测根因。
- 同候选另一条独立真实 18 例已全通过，但它以手工代理、mock `UserService` 和独立事件容器装配 Notice/Runtime，不能证明完整 Spring Boot 的 JSON 配置及 HTTP 调用链。

## 高置信、可证伪的源码因果链

1. `NotifyNoticePublisherService.publish` 生成 Snowflake `snapshotId`，调用 `NotifyNoticeVersionFence.initial(noticeId,snapshotId,version)`，经 `JsonUtils.toJsonString(version(...))` 写入 `NotificationCommand.metadata.noticeVersion`。随后 `notificationService.submit` 持久化 Intent，发布者锁定该 Intent 并调用 `NotifyNoticeVersionFence.requirePublishedIdentity`。这在完整路径上发生于 HTTP 返回成功之前。
2. `JacksonConfig.registerJavaTimeModule` 为 `Long` 注册 `BigNumberSerializer`；其阈值为 JavaScript 安全整数 `9007199254740991`，更大的雪花 ID 被写成 JSON **字符串**。完整 Spring Boot 自动配置注册此模块，`JsonUtils.getJsonMapper()` 优先使用 Spring `JsonMapper`。本次公告 ID、快照 ID 处于 18/19 位雪花区间，超过阈值。
3. `NotifyNoticeVersionFence.parseVersion` 从内层 JSON 读取 `noticeId`、`snapshotId`，调用 `integer(Object)`；该方法只允许 `Integer`、`Long`、`BigInteger`，对上述规范化十进制字符串返回 `null`。`strictVersion` 因而抛 `ServiceException("公告发布版本事实不一致")`。`GlobalExceptionHandler.handleServiceException` 会将无自定义 code 的业务异常封装为默认 `R.code=500`、HTTP 200；这与安全结果完全吻合。`@DSTransactional` 还应使该发布事务回滚，但 R1 没有保留失败时 DB 读数，不能声称已证回滚。
4. 真实 18 例借用 `NotifyAtomicResultIntegrationTest.open()` 的手工容器，只登记事务事件相关 Bean，未登记 `JsonMapper`；`JsonUtils` 在无法从 Spring 获取 mapper 时回退到裸 `FALLBACK_MAPPER`，其 Long 保留 JSON 数值，解释了该测试全绿而 full HTTP 失败的差异。此项是装配源码推论，不是 R1 原始异常观测。

这也是撤回/Worker 来源核验的潜在缺口：`retractedMetadata` 与 `state` 同样调用 `parseVersion`，即使绕过发布核验，大 ID 字符串也会被视作 `UNVERIFIED` 或拒绝撤回。不能通过放宽发布回执检查解决。

## 建议最小红绿验证与修复边界

- 先在现有 `backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/support/NotifyNoticeVersionFenceTest.java` 增一个使用实际 `JacksonConfig.registerJavaTimeModule()` 的 mapper、ID `>9007199254740991` 的 roundtrip：`initial → requirePublishedIdentity → state(ACTIVE) → retractedMetadata → state(RETRACTED)`。可用 scoped `mockStatic(JsonUtils, CALLS_REAL_METHODS)` 仅替换 `getJsonMapper`，不要污染全局 `JsonUtils.JSON_MAPPER`。旧 numeric JSON 正例仍保留。此项应先在未修产品时红灯，证明配置差异，而非用人工字符串断言冒充生产 mapper。
- 仅在 `NotifyNoticeVersionFence.integer` 额外接受规范 ASCII 正十进制字符串 `[1-9][0-9]*`，用 `BigInteger.longValueExact()` 限定 signed-64；原数值类型继续支持。负例明确拒绝空白、正负号、前导零、小数、指数、布尔值、越界值。`noticeId/snapshotId/version` 与 Intent 精确同一性检查、撤回栅栏和 Worker fail-closed 不变。需要预登记上述 FenceTest 精确路径；生产 Fence 路径已属 T40 原写集，但仍由 Lead 按治理确认。
- 固定源码重跑该单测及完整 Spring owned HTTP 发布→Worker→撤回/Chrome，保留失败时源/JAR/资源清理证明。若红灯没有落在 `requirePublishedIdentity`，或修后 HTTP 仍为 R500，应在下一 runner 候选增加**安全诊断**：仅保留 `GlobalExceptionHandler` 已输出的异常类 allowlist 和当前 Notice 的生命周期/快照/Intent/Outbox 计数，或对固定已知业务消息做精确类别映射；不保留 `R.msg`、原始响应、SQL 行、异常文本、令牌或正文。借助失败前后 DB 状态区分事务内失败与提交后路径。R1 现有证据不足以判断其他异常。

## 已静态排除或降权的方向

- 不支持的渠道或缺短信账号：控制 payload 仅 `IN_APP`；`validateSubmission` 对非 SMS 直接返回，外部 DML 账号默认停用不参与本次投递。
- 无目标：A 真实登录、owned 查询证实启用；`save` 与 `publish` 均调用 `selectNotificationUsers`，且 WTA 的固定 ID 是 `SUPER_ADMIN_USER_ID`。不过 save 忽略返回集合，不能仅凭它证明 publish 集合非空，仍应由后续真实取证闭合。
- 发布后 Redis wake 故障：`NotifyOutboxWakePublisher.publishAfterCommit` 对发布异常与超时作 catch 并保留慢轮询，不是当前最有力候选；完整上下文的其他提交后异常仍需保守保留。
