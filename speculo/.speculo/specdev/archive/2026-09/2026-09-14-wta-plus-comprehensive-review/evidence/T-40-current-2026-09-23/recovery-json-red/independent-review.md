# T40 R1 公告版本 JSON 大整数栅栏：窄修设计审查

固定源码 `c860480914b5fc563dd119e760e37d5eb99064e3`；读取 `/tmp/wta-t40-r1-publish-diagnosis.md` 的安全诊断，不读取运行中的 dirty 测试。**设计结论：可按拟定的 canonical 正 int64 字符串兼容实现，静态无须替换全局 mapper 或改授权；但 R1 的 publish R500 与此链吻合，原始运行尚无异常/回滚证据，必须先用真实 Jackson 模块红灯证伪，再以同源 HTTP 重验闭合。** 本审查未修改仓库、构建或启动服务。

## 源码链与边界

- `JacksonConfig.java:34–40` 给 `Long`/`long` 注册 `BigNumberSerializer`；该类 `:21,43–50` 在绝对值超过 JS 安全整数 `9007199254740991` 时将数值 `toString()` 输出为 JSON 字符串。`NotifyNoticePublisherService.java:69–77` 的 `initial` 元数据随后被 `requirePublishedIdentity` 读取；真实 Snowflake `noticeId/snapshotId` 超过阈值时形成可重现类型差异。
- `NotifyNoticeVersionFence.java:109–127` 内层 JSON 的 `noticeId/snapshotId/version` 共用 `integer(Object)`；现仅接受 `Integer/Long/BigInteger`。在完整 Spring mapper 下，两个大 ID 字符串被拒绝，`strictVersion` 进而判无效。R1 安全结果仅证明 `publish` HTTP200/R500，具体异常类别和事务回滚仍是待验证推论；独立 18 例的手工 mapper 装配不能替代此真实 JSON 合同。
- 接受**仅在 `integer`** 扩展 `String`：完整匹配 ASCII `[1-9][0-9]*`，限定长度最多 19 位（避免无界 `BigInteger` 分配），再 `longValueExact()`/等价有界解析；溢出返回 null。旧 `Integer/Long/BigInteger` 分支原样保留。null、Boolean、其他 Number（如 Double/BigDecimal）、空白、`+/-`、前导零、`0`、小数、指数、Unicode 数字、超 `Long.MAX_VALUE` 均返回 null。正数要求与 `parseVersion:118–120` 的三字段正数及 version `<= Integer.MAX_VALUE` 双重校验一致。
- 此更改只将同一**数学整数**的 canonical JSON 字符串编码识别为数值，不改变 `metadata.audit`、`retracted` Boolean、`noticeLike`、`identityMatches` 中 `appId/scene/template/bizType/bizId/idempotencyKey` 的精确校验。`requirePublishedIdentity` 的 snapshotId 精确比较、`retractedMetadata` 的 Notice/Intent 锁后旧元数据校验及 `state` 对不明来源的 `UNVERIFIED` 都应保持。它不能把缺 marker、错 notice/version/snapshot、旧 owner 或未经授权的撤回转成有效。小版本号即使被人为写成 canonical `"2"` 也只在其他同一性条件全满足时解析为 2；该表达形式扩展是方案的明确边界，不是放宽身份或撤回栅栏。
- 仅改 Notify 模块私有 Fence 及已登记的 FenceTest；**不**修改 `BigNumberSerializer`、`JacksonConfig`、`JsonUtils.JSON_MAPPER`、通知 DTO、Controller、权限或全局 JSON 绑定。Worker/result 已经基于 `state` 阻断 `UNVERIFIED/RETRACTED`，解析修复须让真实 ACTIVE/RETRACTED 可辨，不能改阻断分支绕过。

## 红绿和失败判别

1. 在现有 `NotifyNoticeVersionFenceTest` 用实际 `JacksonConfig.registerJavaTimeModule()` 构造本地 mapper，并通过有界的 `mockStatic(JsonUtils, CALLS_REAL_METHODS)` 只绑定 `getJsonMapper()`；不可让全局缓存/另一个测试先初始化决定结果。用 `noticeId` 与 `snapshotId` 均大于阈值且为不同正 int64 的真实 `initial`，先确认内层 JSON 两 ID 实际为 quoted string、version 仍为数值，再走 `requirePublishedIdentity → state ACTIVE → retractedMetadata → state RETRACTED`。修复前应在严格发布回执处红，而不是让其它夹具错误冒充红灯。
2. 表驱动逐字段负例：错 app/scene/bizType/bizId/idempotencyKey、错 snapshot/version、缺 audit/marker、`retracted` 非 Boolean；字符串 `"0"`、`"01"`、带符号/空白、decimal/exponent、非 ASCII 数字、越界；数值旧 `Integer/Long/BigInteger` 正例及超界/非正拒绝。对非法 notice-like Intent 验证 `state=UNVERIFIED` 且 `retractedMetadata` 抛业务异常；非公告场景仍 `NOT_NOTICE`。不得只测 `integer` 私有函数或人工手写大 ID 字符串而跳过生产 mapper。
3. 固定产品候选后重跑该 test 与完整 Spring owned HTTP 的 save→publish→Worker→retract/本人详情，留 `source/JAR` 前后、fresh XML、服务清理和失败阶段。若真实配置红灯不落在版本回执，或修后仍 HTTP200/R500，则继续按诊断文档的安全类别/状态计数取证；不能把 R1 的高置信源码推论当作已观测根因或发布事务回滚证明。
