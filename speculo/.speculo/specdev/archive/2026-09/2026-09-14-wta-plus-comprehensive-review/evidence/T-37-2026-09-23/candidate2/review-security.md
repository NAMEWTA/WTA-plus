# T-37 C2 安全／规格轴固定增量复审

**固定点**：C1 `e2907c41f4b1bef442c68b95a0fc09aa9c3b273a` → C2 `3a87bf71876d92e4afdd227de156045226e52be2`，C2 tree `d60c20e385db3f3b7b064e9a13e0df4474a120ef`。三点 diff：`git diff e2907c41...3a87bf7 -- backend`，仅 3 个产品／测试路径。固定源码由 `git show 3a87bf7:<path>` / diff 读取；未以随后工作树为证据。`git diff --check e2907c41...3a87bf7 -- backend` 退出 0。本复审未运行 Maven、Docker、Redis、MySQL 或供应商调用。

**增量静态结论：PASS，无新增阻断；C2 运行验收尚未判定。** C1 报告 `/tmp/wta-t37-c1/review-security.md` 的 REQUEST CHANGES 保留为历史结果。C1 真实运行 13 tests／2 errors（生产 NameMapper 下 `complete` CAS 失败；SMS fixture 模板映射写反）；C2 增量在源码和断言上分别回应这两项。只有 Lead 在固定 C2 上完成 39 类、真实 13 项与 T36 回归并提供计数／清理／source checkpoint 后，才能判断 AC-037 整体是否通过。本报告不把预计测试当作已通过。

## C1 阻断闭合核对

1. `backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/RedisNotifyIdempotencyStore.java:27-38,82-94,111-133`：`complete` 不再调用 Redisson 4.6.1 有 NameMapper 缺陷的 `RBucket.compareAndSet(CompareAndSetArgs)`，`release` 不再调用同类 `compareAndDelete(Args)`。二者复用已有 `ownerScript`：以 `bucket.getName()` 逻辑名调用 `RedissonScript.eval(String key,...)`，该 API 将路由 key 与 KEYS 映射到物理 key；`getScript(bucket.getCodec())` 按 bucket 的真实值 codec 编码 `expectedValue`／新值。Lua 在同一物理键上先比较完整旧值，再 `PSETEX` 完成态或 `DEL`。Redis `EVAL` 的单键操作原子化；旧 owner nonce 不匹配时不能覆盖、释放新 owner。`markRetryable` 原 `CAS_KEEP_TTL` 路径只抽取共用 helper，保持 `PTTL>0` 和 `SET ... KEEPTTL`。新 `markRetryable` 前置还要求 owner 的 digest/requestId 与 expected JSON 一致，减少内存对象自相矛盾时的状态转换。
2. `CAS_COMPLETE` 的 `%d` 只由 `Duration.toMillis()` 的正 `long` 值填入，并在执行前拒绝 `<=0`。这一数值以 Java 格式化为 Lua 数字字面量；不经 Redisson 的 JSON/Kryo 值编码。`expectedValue`／`completedValue` 仍通过脚本 ARGV 的 bucket codec。成功 `PSETEX` 重新建立完成态窗口，和 C1 `CompareAndSetArgs.timeToLive(acquired.window())` 原语义相符；过期或无有效 TTL 的 owner 不得完成。`release` 不延长 TTL，删除仍只在完整旧值相等时发生。没有发现脚本拼接由外部字符串注入 Lua 的路径：拼入的仅 `long` 数字。
3. `backend/wta-admin/src/test/java/org/namewta/test/notify/idempotency/RedisNotifyIdempotencyStoreIntegrationTest.java:156-186`：原 20 并发重取、旧 owner 完成／转态拒绝、TTL 不重置仍在；新增当前 owner `complete` 后 `Completed` 与完成态 TTL 正例、旧 owner `release` 不删完成态、fresh owner `complete` 正例及 fresh owner `release` 后物理键不存在并可再 acquire。共享 `retryableOwnerScenario(false/true)` 同时覆盖 StringCodec 和生产 CompositeCodec＋KeyPrefixHandler，避免仅用旧 owner 负例出现虚绿。C1 在 156 行失败而未到达的旧四字段／损坏值断言仍在其后，须由 C2 真实运行证实。
4. `backend/wta-admin/src/test/java/org/namewta/test/notify/NotifySmsDispatchIntegrationTest.java:480-482`：fixture 改为逻辑变量 `code`／`expireMinutes` 作键、腾讯位置 `1`／`2` 作值，正好满足 `NotifySendPlanner.preflight:97-119` 对必填变量查 key、连续位置查 value 的合同；这仅修测试输入，未放宽生产验证。C1 的 `validateSubmission` 失败应由新真实运行重新验证是否闭合，不能从静态修复推断两次实际物理发送已通过。

## 规格边界与保留风险

AC-037 要求确认整批未受理后，Outbox 才能按自身预算再次物理调用；已接受／UNKNOWN／混合目标、旧 `COMPLETED FAILED`、不同 digest 与过期 owner不能因泛化错误自动重发。C2 增量未修改 C1 的 Dispatcher、typed SMS 来源、runtime 分类或 Outbox 逻辑；原 C1 对这些未变范围的静态检查沿用，不能替代 C2 当前真实组合运行。完成态 ACK 丢失时，脚本可能已经提交而调用方收到错误；原有保守 UNKNOWN 路径仍应由真实故障注入证明不会盲发。当前 `PSETEX` 窗口来自应用配置；对异常超长 Duration 的 Redis 数值范围，本票没有实际可达证据，不当作阻断。

**待 Lead 验证**：固定 C2 clean source 的 39 类受影响测试、真实 Redis 13 项（尤其 Composite 名称前缀下 fresh `complete/release`、旧值、TTL、四字段、损坏值）、真实 MySQL＋Redis＋SMS/Dispatcher 两次物理调用与 UNKNOWN 安全边界、T36 Atomic/Wake 回归；逐项记录命令、退出码、测试／skip 数、source hash 和资源清理。任何失败都使本增量静态 PASS 失效，须固定下一修订复审。未读取生产 secret 或 private/raw 日志。
