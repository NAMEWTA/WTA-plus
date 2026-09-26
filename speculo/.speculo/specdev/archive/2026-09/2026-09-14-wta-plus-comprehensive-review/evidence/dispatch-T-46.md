# T-46 Dispatch01 — 恢复与来源清理共同互斥

## revision213 — T-46启动与共同互斥合同

revision213：T45已验收完成（产品09be6db、治理4a8fea8c）；T46激活，以恢复/清理竞争红灯起步。16done/2cancelled/1in_progress/31ready，Goal active，未完成或归档。

基线`4a8fea8c19392972ee5cfef4263962ff6b0e3bfb`，main/current/direct-parent。实现仍复用sys_oss对象锁及现有工单，不新增schema、队列/分布式状态机。所有unpublish/rollback/process切指针及cleanup统一Object→Item锁序，在被Spring代理的public DSTransactional边界重读对象/工单，核对ACTIVE、当前service、source/target/key、最新工单、版本/状态和安全窗口；指针与工单条件更新均须恰1行且同事务，不能吞CAS失败。批次逐对象处理，不持整批锁。

严格超时不能证明供应商DELETE立即取消。为满足“清理获得合法执行权后恢复拒绝”和“删除已成功但DB提交失败可安全重试”，先在对象锁事务中用既有FAILED + lastErrorStage=COMPLETED + 固定CLEANUP_OUTCOME_UNKNOWN持久化执行/未知栅栏，提交已知后才发有界DELETE；不确定提交不发DELETE。完成再按Object→Item与同版本收敛COMPLETED。超时、响应/提交确认丢失保留栅栏，所有恢复/切指针路径拒绝绕过；后续显式cleanup可用有界HEAD确认来源缺失、目标有效后仅finalize，来源仍在或读取未知不盲重发DELETE或恢复来源。这是已有工单字段的保守执行权，不引入新的分布式状态平台；SDK cancel不等于撤销远端副作用。

来源存在与核对HEAD、DELETE均采用明确Duration预算，复用OssClient/Abstract最小重载及迁移ObjectStore端口；普通OSS调用语义保持。读写I/O不跨整批持锁，副作用前后只锁单对象；copy/verify不借此扩张重写。写集从5扩到10：两common API/实现、client测试目录、backend运行文档和system事实。java-api-compatibility用于新增有界操作及仓内消费者核对，不新建旧API兼容桥。HTTP形状/权限/公开副本提示原则上保持；实际合同变化须先报Lead。

Dispatch01A仅修改既有OssStorageMigrationServiceUnitTest.java，使用可阻塞对象删除和两个调用者固定旧时序，证明旧cleanup已进入删除时unpublish仍能恢复来源而后被删；锁获胜顺序和条件更新失败后续用真实MySQL双连接/真实DSTransactional代理及MinIO补验。当前生产代码不动，不宣称内存替身能证明DB锁。Lead固定红灯后派01B完整实现。cors_audit唯一writer；legacy_audit只读合同审查；ops_audit仅私有隔离驱动准备。Lead独占治理、提交、构建、真实服务。

必需门禁：受影响OSS单元/合同、默认后端、full/core；真实MySQL两物理连接证明共同锁、两竞争顺序、指针/工单CAS0与回滚、旧工单/未知栅栏、超时及晚到DELETE、提交确认丢失和安全finalize；第二对象仍能推进，MinIO目标字节可读。只有fresh XML零skip/ownedcleanup及双轴审查完成才勾AC；最多3次完整候选失败先四项复盘。当前attempts0。

## revision214 — 红灯与Dispatch01B

revision214：T46红灯在0f8729d复现，15例/1预期failure/0error/0skip，源码前后clean；DELETE已进入后旧unpublish返回成功。Dispatch01B开始共同锁、持久UNKNOWN和有界I/O实现。16done/2cancelled/1in_progress/31ready，完整候选attempts0，Goal active。

权威实施包为 `evidence/dispatch-T-46-implementation.md`；红灯和写入口清单见 `evidence/T-46-current-2026-09-23/red01/manifest.json`。没有真实服务或完整候选通过记录，AC未勾。
