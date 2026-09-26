# T46 Dispatch03 — 预约事务提交ACK失联夹具

仅授权 cors_audit 编辑既有 OssStorageMigrationIntegrationTest.java；生产、其它测试与治理均不动。MybatisOssMigrationStore.updateItem override 在 super.updateItem成功且 item.errorMessage == CLEANUP_OUTCOME_UNKNOWN 后捕获非空当前XID、单次arm。DelegatingDataSource包装连接时保存所属XID，物理commit仅对同一armed XID（及必要连接身份）执行delegate.commit后抛固定SQLRecoverableException；普通预读commit不能消费。增加arm/commit-hit精确一次和异常cause链断言，持久UNKNOWN、provider DELETE=0与后续CAS/并发/缺桶场景保留。任何RuntimeException不再足以证明目标故障。

旧运行仅证明UNKNOWN为null，预读提前提交是源码机制支持的强解释，不能倒填旧捕获栈。未改变业务合同，不放宽失败断言；不执行构建/服务/提交。完整回交后Lead固定新候选，精确两方法owned runner与其余门禁继续。完整候选attempts=1，下一失败照实累计，上限3次先复盘。
