# Change Context

- 工作区：/srv/WTA-plus；2026-09-18 HEAD e285d0800c530fcb6b90790a8aab5dcc3162d3bd。
- 授权：全面复核并直接重构本change文档；禁止子代理，一个并发。产品实现、提交、部署和运行数据操作不在范围内。
- 基座无需旧版兼容；仓内调用、测试与生成物应同步切换。保留Client隔离、权限、事务、资源与真实供应商协议。
- 当前清单：50个backend POM、247个backend src/test/java Java源文件、3个App package、49个后端AGENTS。它们不是运行通过数。
- SQL owner：release-artifacts/docker/infrastructure/mysql/init；10-cde-base-ddl.sql结构，50-cde-base-dml.sql数据，共六份完整基座。
- Notify已有submit/query和Outbox，Redis负责wake/lease及挑战缓存；不能把Redis写入与MySQL写入称为同事务。
- 单票frontmatter为状态/依赖/写集权威，plan-data与Map同步投影；当前29票计划Ready、T-03/T-23 blocked；Spec总体draft、Goal执行关闭，规划完成不等于实现完成。
- 详细复核、真实命令及未验证项分别见reviews/re-review.md、reviews/re-review-command-results.json、verification.md。

2026-09-19用户新指示：T-03无需样本或目标容量，先用JSON/机器各2MiB独立可配置默认；T-23自主预置腾讯云/阿里云短信与QQ/163/腾讯企业邮箱SMTP，填写所需凭据后启用。默认禁用且不放真实凭据，按官方协议区分受理与送达；原等待资料已结束，后续串行实施，全部提交仍暂缓。

Revision120：T-23开始，610上游hash已核对。用户要求的五种禁用账号预设、必要凭据/签名校验和SMS4J消息ID保留先行；已登记common-sms、DML、通知配置页面、精确父规范与供应商说明写集。官方原生回执均不同于现有HMAC；阿里重试文档互相矛盾、腾讯仅说明再试2次，保留期不猜测。回执安全接入/身份仍在内部研究，不等待用户、不标review。29review/1in_progress/1ready/0Done，全部提交暂缓。
