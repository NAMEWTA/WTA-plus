# UA 真实消费者红灯

诊断源码 `6eb5783cee5d29dc6f7ffa9d61d92774143dcc73`，clean；10 tests / 6 failures / 0 errors / 0 skipped。三处真实消费者在无/空白 User-Agent 时各有一次 NPE，Chrome 三项和已有字段保留一项通过。此前 `a9ebea30` 的首次运行在测试夹具构造器重载处编译失败，单独保留，不算业务红灯。

这些是 Dispatch02A 事前声明的诊断，不是恢复批完整候选。独立审查另发现 RedisUtils.CLIENT / MapstructUtils.CONVERTER static final 的共享 JVM 污染风险；Dispatch02B 须隔离测试 JVM，不能只恢复 SpringUtils。正式绿灯和默认测试待修复后执行。

依赖探针等价性和私有产物迁移记录一并保存；迁移未改原始 JAR 字节，原路径保留可读链接。
