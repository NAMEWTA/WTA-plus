# T-46 Dispatch01B — 共同锁与安全收敛

红灯固定0f8729d：新增unpublishCannotRestoreSourceAfterCleanupHasEnteredProviderDelete收到null（恢复成功）而非拒绝，14旧例通过。red01/manifest保留真实失败，不把内存替身当数据库锁证据。

cors_audit获得唯一产品写锁，仅Ticket10条登记路径；遵守System classic模式，不引入独立模块/schema/分布式状态平台。使用一个migration本地被代理的public DSTransactional服务承载Object→Item事务；现有orchestrator处理外部I/O和逐项批次。全部修改现有service指针的入口（unpublish/rollback/process正常切换/访问验证失败恢复）、工单claim/进度/失败更新及新工单创建均需共享锁和版本/状态守卫，不能以catch fail(item)的陈旧副本覆盖UNKNOWN。status预过滤latest及宽claim(status!=RUNNING)必须修正；工单状态/指针更新非1行即回滚，不报成功。

cleanup先在锁内重读ACTIVE、当前target路由、source/target/key、latest工单、version/status、安全窗口及任何未决栅栏。用已有FAILED/lastErrorStage COMPLETED/固定CLEANUP_OUTCOME_UNKNOWN持久化保守执行权，只在提交已知后发一次有界DELETE，提交不确定则零DELETE；完成后同锁和reservation版本finalize。超时/网络失败/提交ACK丢失保留栅栏。显式cleanup重试仅有界HEAD来源已缺失+目标有效，再锁内重核相同指针/marker/version后完成；来源仍存在、403、timeout或未知都不能重发DELETE或恢复。新批次/旧工单及所有retry/rollback/process不得绕过。

对迁移恢复/清理HEAD和DELETE在OssClient/Abstract增加明确Duration能力；SDK每请求总/attempt timeout配合有界await/cancel，中断保留，资源正确回收，不能宣称取消能撤回远端副作用。迁移属性提供有界预算及校验、运行文档明确UNKNOWN处置；普通OSS操作不做无关替换。copy/内容验证在短事务外，不持整批锁，一个对象超时不能持有其它对象锁。沿用原HTTP/权限/公开副本提示，若必须新增响应/schema/其它路径先报Lead。

测试在现有migration/client测试写集内：将旧“失败后二次DELETE”的期待改为UNKNOWN+零重发/来源缺失后仅finalize，保留旧失败事实。真实IntegrationTest改为真实DynamicLocalTransactionInterceptor的Spring proxy+线程安全mapper/连接资源，至少两物理MySQL连接并实际观测行锁竞争，覆盖两顺序、旧工单/claim/new start栅栏、指针及item CAS0原子回滚、提交确认丢失、晚到DELETE和第二对象进展；真实MinIO验证目标可读与来源状态。使用合法cleanupDelay>=1分钟并推进可注入Clock，不放宽生产校验。测试不存在的接口可按最小服务方法实现，禁止通过mock Spring事务证明真实数据库原子性。

所有生产/测试/运行文档变更一次回交，禁止构建、服务、提交、治理修改；Lead固定后定向、默认/full/core及精确零skip隔离集成。ops仅准备私有runner，legacy只读审查。无需因已获Goal授权再次询问本地实施/commit；远程/生产/归档均不在本派单范围。

## revision215 — 配置身份与红灯证据边界

revision215：T46补充配置物理身份保护，写集10扩14；未结束迁移保护source/target配置，关闭编辑/删除与新工单创建竞态。实现仍由cors_audit独占，尚未完整候选验证；16done/2cancelled/1in_progress/31ready，attempts0，Goal active。

配置引用不能只计算当前sys_oss.service：迁移切换后仍须保护未终结工单的source/target，FAILED（含CLEANUP_OUTCOME_UNKNOWN）继续持有引用，不能改名、删除或改向另一物理存储后把404误判为原来源已删除。已有对象引用保护同步覆盖影响物理寻址的endpoint/isHttps/region及原configKey/bucket/accessPolicy；凭据按同一存储身份轮换仍允许，不能把普通密钥轮换等同身份迁移。域名等字段按实际调用面审查，避免无关冻结。

创建工单须与配置编辑/删除形成真实互斥，配置锁统一稳定顺序且先于Object→Item；其它既有工单事务保持Object→Item，不引入逆序Config锁。配置批删须避免首项普通读建立RR快照后、后续配置锁等待期间新增引用漏检；先取得全部配置锁再读引用，或采用有证据的等价当前读方案。真实MySQL验证创建与编辑/删除竞争、切换后源配置引用、UNKNOWN及正常凭据更新；不以Mapper字符串或mock断言替代竞争证据。

产品写集新增SysOssConfigMapper.java、SysOssConfigServiceImpl.java、oss/config测试目录、System AGENTS.md，共14根；只补本票安全边界与事实，不扩展配置架构。既有common/事务/Java API技能绑定仍适用。

证据精度更正：revision213“恢复来源而后被删”是风险描述，red01 FakeObjects.delete仅计数与等待，没有物理删除字节。该红灯只实证DELETE进入后旧unpublish成功；不能当作来源已不存在证据。真实MinIO HEAD/GET将在最终集成补验，旧记录原字节保留。
