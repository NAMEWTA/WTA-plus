# ADR-0010: 业务模块拥有 OSS 引用生命周期

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-010

## Context

持久化 ossId 的业务模块才掌握授权、业务事务、旧新附件和删除恢复语义；中央协调器无法在不理解业务表的前提下正确拥有这些责任。

## Decision

每个持久化 ossId 的业务写入 module 是 Business OSS Owner。业务记录与引用转换在同一 `@DSTransactional` 内 fail-closed；共享能力只做集合差分和引用转换，不读取、回调或反射业务表，也不推断 ACL。逻辑删除是否解绑由真实恢复合同决定，不从 `@TableLogic` 推断。

基座系统不做历史回填或兼容窗口。所有 Owner 必须登记显式 manifest，并以 insert/update/delete/restore（适用时）合同测试提供 ratchet；manifest 不是运行时注册中心，不采用注解扫描或动态业务回调。

## Consequences

业务模块保留少量显式 ownership 代码，换取授权、事务和恢复语义的 locality。新增持久化 OSS carrier 未登记或缺合同测试时必须在交付门禁失败；主动清理启用仍需独立批准。
