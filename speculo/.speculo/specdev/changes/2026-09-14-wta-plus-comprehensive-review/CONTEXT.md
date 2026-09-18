# Change Context

- 工作区：/srv/WTA-plus；2026-09-18 HEAD e285d0800c530fcb6b90790a8aab5dcc3162d3bd。
- 授权：全面复核并直接重构本change文档；禁止子代理，一个并发。产品实现、提交、部署和运行数据操作不在范围内。
- 基座无需旧版兼容；仓内调用、测试与生成物应同步切换。保留Client隔离、权限、事务、资源与真实供应商协议。
- 当前清单：50个backend POM、247个backend src/test/java Java源文件、3个App package、49个后端AGENTS。它们不是运行通过数。
- SQL owner：release-artifacts/docker/infrastructure/mysql/init；10-cde-base-ddl.sql结构，50-cde-base-dml.sql数据，共六份完整基座。
- Notify已有submit/query和Outbox，Redis负责wake/lease及挑战缓存；不能把Redis写入与MySQL写入称为同事务。
- 单票frontmatter为状态/依赖/写集权威，plan-data与Map同步投影；当前29票计划Ready、T-03/T-23 blocked；Spec总体draft、Goal执行关闭，规划完成不等于实现完成。
- 详细复核、真实命令及未验证项分别见reviews/re-review.md、reviews/re-review-command-results.json、verification.md。
