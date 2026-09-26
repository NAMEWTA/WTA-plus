# T40 R2 JSON 公告版本栅栏固定候选复核

固定差异：`5db09b9f..8a387192e5a7787d1c51731dfc982506207c5482`，R2 tree `f3fe463cff773c0178e2b99e692c71464cc12caa`，四文件：Fence Java/测试及两份真实浏览器 runner Python。**静态结论：PASS，无发现需阻断的授权或数值兼容缺陷；Lead 的目标 Fence 5 项及全量/真实浏览器验收仍在运行，不能以本报告代称通过。** 仅固定 `git show/diff` 只读，未运行测试/服务或改仓库。

## 生产 Fence 数值合同

- `NotifyNoticeVersionFence.java:123–138` 旧 `Integer/Long/BigInteger` 分支保留；新 `String` 分支逐字符限定首位 ASCII `1..9`、余位 ASCII `0..9`、长度 1–19，再由 `BigInteger.longValueExact()` 限定 `Long.MAX_VALUE`。`Long.MAX_VALUE` 正好可达；`9223372036854775808`、任何更长字符串、空白/换行、`+/-`、`0`/前导零、小数、指数、全角或其他 Unicode 数字均拒绝。非 Number/String 的 Boolean、Double/BigDecimal 仍拒绝。字符检查使 `BigInteger` 对字符串不会遇到未捕获的格式异常；溢出由 `ArithmeticException` 捕获并转 null。
- `parseVersion:109–120` 的 `retracted` 必须 Boolean、三个 ID/版本必须正数且 version `<= Integer.MAX_VALUE` 未变。`metadata.audit`、`noticeLike`、`identityMatches` 的 app/scene/template/bizType/bizId/idempotencyKey 精确比较，`requirePublishedIdentity` 的 snapshotId 等式与 `retractedMetadata` 的旧 marker 精确等式也未改。故仅拓展同一数学整数的 JSON 表示，不将错 Notice、错快照、错版本或非法撤回识别为已授权。缺 marker 的历史 audit-only 行仍按原有精确身份条件仅供撤回升级，`state` 仍为 `UNVERIFIED`，本次没有扩大该例外。数值形式 `-1/0` 虽保留原类型入口，仍在 parseVersion 正数门禁拒绝。
- 该私有方法的表示兼容会把人工写入的小 canonical 字符串版本如 `"2"` 也解析为 2；这是本轮明示的正 int64 String 方案，并仍须完整身份/快照/版本匹配。没有更改全局 Jackson/BigNumberSerializer、权限或 Worker 对 `UNVERIFIED/RETRACTED` 的拒绝。

## 测试与真实 runner

- `NotifyNoticeVersionFenceTest.java:23–55` 用实际 `JacksonConfig.registerJavaTimeModule()` 建 `JsonMapper`，`mockStatic(JsonUtils, CALLS_REAL_METHODS)` 仅在 try 范围替换 `getJsonMapper()`；`initial → requirePublishedIdentity → ACTIVE → retractedMetadata → RETRACTED` 用两枚不同的 >2^53 ID，并检查内层 JSON 确实为 quoted string。新增 `Long.MAX_VALUE`/`MAX-1` 边界。没有写 `JsonUtils.JSON_MAPPER` 全局缓存或留下 static mock，能与其他测试隔离。
- `:59–70` 新表涵盖空白、符号、前导零、小数/指数、overflow、Unicode、Boolean、Double，并要求 `state=UNVERIFIED`、严格发布回执抛 `ServiceException`；原数值、小公告、audit-only 升级、无关场景等测试保留。它没有逐一矩阵化 snapshot/version 的所有非法表示，但生产三字段共用同一 `integer`，Python runner 新负例另覆盖 snapshot 和 version；此处不是阻断。
- `run-notice-retraction-real.py` 新 `positive_int64` 对 Python `type(value) is int`（明确排除 `bool`）或 ASCII `[1-9][0-9]{0,18}` 字符串做 `1..Long.MAX_VALUE` 范围校验；`notice_version_fact` 分别核 DB snapshot、marker notice/snapshot/version 和 `retracted` 真 Boolean，保留原精确等式语义。大 ID 由 JSON 读成字符串时不再被 runner 错判失败。它只用于 owned 证据核对，不改变产品授权。
- `test_run_notice_retraction_real.py` 新合成正例覆盖 quoted 大 ID，错 snapshot/非 canonical/溢出/bool/float 负例；旧正常和不匹配断言未删。Python `re.fullmatch` 拒绝尾随换行，`type(...) is int` 排除 True/False；与 Java 字符串分支范围一致。

## 待验收事实

Lead 当前目标 Fence 5 项仍在运行；需核固定 R2 源码的真实退出、fresh XML、零 skip，再完成串行全量及完整 Spring owned HTTP save→publish→撤回/Worker/本人详情、Chrome 与资源清理。R1 的 HTTP200/R500 历史不能改写为已证异常类别或回滚，本报告也不声明 R2 已通过这些出口。
