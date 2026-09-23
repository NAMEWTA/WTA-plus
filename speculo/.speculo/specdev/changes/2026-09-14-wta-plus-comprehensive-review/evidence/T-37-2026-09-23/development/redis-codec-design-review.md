# T-37 Redis codec／脚本边界只读预审

**输入**：固定产品 base `38032d24335c52cafea855b19d51fb36295162ef`；Redisson **4.6.1** 本地 `redisson-4.6.1-sources.jar` 与项目 `RedisConfig`／`KeyPrefixHandler`。没有修改仓库、读取真实 Redis 值或运行 Maven/Docker/服务。下列是源码 API 判定与未来隔离测试建议，不是运行通过证据。

## 正确的同 key codec 路径

生产 `RedisConfig.java:66-75` 装配 `CompositeCodec(StringCodec.INSTANCE, TypedJsonJackson3Codec(Object.class,jsonMapper), 同 value codec)` 和 `KeyPrefixHandler`；`application-{local,dev,prod}.yml` 当前 keyPrefix 均为 `WTA`。旧 `RedisNotifyIdempotencyStore` 使用 `redissonClient.getBucket(storageKey)`，即默认 **Composite value codec**。它把 `JsonMapper.writeValueAsString(StoredState)` 得到的 Java String 再经过 TypedJsonJackson3Codec 存进 Redis。不能把新实现只换成 `getBucket(storageKey, StringCodec.INSTANCE)`：这会把旧 Redis 字节直接当 plain JSON 字符串读，旧四字段 `StoredState` 无法按原语义解码；新写入的 plain JSON 又会与旧 bucket codec 混存。具体 raw 字节形状由当前 Jackson 配置决定，设计不应在代码里手工去一层引号或猜字符串转义。

**最小安全用法**：继续用默认 `RBucket<String> bucket = redissonClient.getBucket(logicalStorageKey)` 完成旧值读取、首个 `setIfAbsent`、既有 `complete`／`release`；转态脚本用 `RScript script = redissonClient.getScript()`，它与默认 bucket 使用同一客户端 codec。调用 `script.eval(RScript.Mode.READ_WRITE, LUA, RScript.ReturnType.LONG, List.of(logicalStorageKey), expectedJavaString, nextJavaString)`。RedissonScript 4.6.1 `:39-49,80-94,201-206,240-255` 证明：默认脚本取客户端 codec；第一个 KEYS 元素用来定位节点；字符串 KEYS 由 NameMapper.map，ARGV 由 `commandExecutor.encode(codec,value)` 转成 Redis 字节。`CommandAsyncService:950-963` 用 `codec.getValueEncoder()`，与 bucket 的 `encode(value)` 对齐。不要给 ARGV 传已经过 codec 编码的 ByteBuf、外层再包 JSON 的字符串，或把 `JsonMapper.writeValueAsString` 的结果自己二次编码；它应只是旧 Store 原本传给 bucket 的**同一个 Java String**。如果显式使用 `getScript(codec)`，必须传与旧默认 bucket 实际完全相同的 Composite codec；直接 `getScript()` 更不易漂移。`StringCodec.INSTANCE` 只适合当前独立 StringCodec 测试客户端，不能替代生产默认 value codec。

## 单键 PTTL／KEEPTTL 与 NameMapper

`RBucket.compareAndSet(CompareAndSetArgs)` 不设 TTL 的实现先 `SET` 再只在有 TTL 参数时 PEXPIRE，因此会清旧 TTL（`RedissonBucket.java:457-490`）；普通 `compareAndSet(expect,update)` 也只 `SET`（`:64-90`）。`setAndKeepTTL` 虽可保 TTL，却不是 owner CAS。安全脚本须在同一 Redis 原子执行中：`GET KEYS[1]` 精确等于编码后的 `ARGV[1]`；`PTTL KEYS[1] > 0`；然后 `SET KEYS[1] ARGV[2] KEEPTTL` 并返回成功整数。缺 key、无 TTL（PTTL=-1）、已到期或旧值不等时不写，向调用方返回明确失败／不可用；不要先 Java 读 TTL 后另发 SET。Redis 6+ 的 KEEPTTL 语义由目标隔离 Redis 验证。对 `IN_PROGRESS→RETRYABLE` 和 `RETRYABLE→IN_PROGRESS(new nonce)` 两处都用这条单键脚本或等价原子操作，才能保留原剩余窗口。正常 `complete` 现有 `.timeToLive(acquired.window())` 是成功完成态重设窗口的既有合同，与这两处不同。即使脚本已成功但 ACK 丢失，调用方仍需 fail closed，不可向 DB 报可重试已确认。

始终给脚本传 Coordinator 生成的**逻辑** `notify:idempotency:v1:<hash>` key，不要传 `bucket.getRawName()`、手工加 `WTA:`，更不要把 key 作为 ARGV 供 Lua 自行拼前缀。RedissonObject `:183-185` 在 bucket 创建时 map 名称，`getName()` 为 unmap 后逻辑名、`getRawName()` 为已映射名（`:170-176`）；RedissonScript `:240-251` 对 routing key 与 KEYS 中每个 String 各 map 一次。使用 `eval(mode,...,List.of(logicalKey),...)` 可让其自动按同一逻辑 key 路由，避免另传显式 routing key 时混淆逻辑／物理名。当前 `KeyPrefixHandler.map` 对已带 `WTA:` 的值碰巧幂等，但应以 API 一次映射为合同，不依赖该容错。

## 旧四字段、nonce 与生产等价测试

新 `StoredState` 若增 owner nonce，旧 `{state,digest,requestId,result}` 四字段必须仍能由**默认 Composite bucket** 解码；缺 nonce 只说明 legacy 状态，不得把旧 `COMPLETED FAILED` 或 `PROVIDER_REJECTED` 升级为 RETRYABLE。旧 IN_PROGRESS 原 nonce 缺失时仍按活跃占位处理，过期后新 claim 才写含独立 nonce 的状态；旧 owner 的 expected bytes 与新值必须不等。旧已完成受理与失败结果都保持原 key 的 digest 冲突／重复处理。反序列化失败、未知 state、无 TTL 均失败关闭，不降级成空 key。不要更换 key prefix 或清缓存以绕过兼容。

未来真实隔离测试至少用**两个 Redisson 客户端配置**覆盖同一 Store：一套与 `RedisConfig` 相同的 CompositeCodec＋`KeyPrefixHandler("owned")`，以旧 `getBucket(logicalKey)` 写入四字段值并设 TTL，再由新 Store 读取 old completed accepted/failed、old in-progress、冲突、nonce 新值、转态、同 digest 再领取、旧 owner CAS；另一套现有 StringCodec 无前缀 fixture 验证通用实现。前者要核对单逻辑 key 对应的物理 key，既无 `owned:owned:` 双前缀，也没有编码错位导致 CAS=0；PTTL 在连续转态中单调不增，旧字节在到期前可读。隔离客户端只使用新建 owned key，不输出 target/digest/raw JSON，结束只删其 owned key。现有 `RedisNotifyIdempotencyStoreIntegrationTest` 只显式 `StringCodec`，不能代替生产 Composite 验证。所有上述测试必须由产品 writer/Lead 在固定候选按任务门禁执行；本次只核源码。
