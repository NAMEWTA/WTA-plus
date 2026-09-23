# 当前架构复核与取舍

2026-09-23；已确认计划的架构复核，不重新激活独立R/C流程。来源<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-review.md</Path>。

已有App/web-domain/domain/platform、Notify五层、System classic和统一NotificationApplicationService边界可承载修复，无须重写。保留发布快照/接收者快照、数据库唯一约束、短事务、Outbox租约fence、HMAC持久回执、OSS用户owner与真实对象访问授权。

| 待收敛职责 | 当前问题 | 目标及责任 |
|---|---|---|
| 持久收件箱/实时提示 | 一个开关关闭两者 | T-34/41，REST独立、实时只刷新 |
| 本地持久化/外部发送 | IN_APP失败等待不存在回执 | T-36，消息与结果同本地事务；外部I/O仍在外 |
| Outbox/common幂等 | 双层完成态吞重试 | T-37，明确未发送可重试，接受/未知保留防重 |
| 命令声明/真实实现 | priority/SYNC/ESCALATION未兑现 | T-50，拒绝未支持并同批改调用方 |
| OSS业务安全/诊断 | canary与陈旧快照阻断合法对象 | T-44/45，保留本地校验，诊断报告范围与未知 |
| 对象指针/来源清理 | 不同互斥下删当前来源 | T-46，所有相关入口共用锁序和有界单对象事务 |
| 通知附件/存储归属 | 生产SPI缺失、旧owner退役 | T-42，生产装配和真实持久引用不可省略 |
| 启动/repair | 普通路径承担深度诊断 | T-48/49，框架配置权威、显式repair |

高影响推荐在<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>标proposed，不当已接受决定。尤其System AGENTS硬门禁替代和附件owner要先锁定。保留现有测试端口/Clock，禁止为简化移除权限或安全预算；N-10先测量，不新增MQ/增量计数平台。

相邻OIDC仍是独立change；本轮认证回归保护现行行为，不实现内部密码换票或私有直登旁路，也不把目标OIDC设计写成已实现。
