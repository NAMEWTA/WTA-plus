# ADR-0005: OSS TEMP 与业务引用

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-005

## Context

上传完成与业务保存之间存在孤儿窗口，同一 OSS 对象也可能被多条业务记录共享。

## Decision

新对象完成校验后进入可索引 TEMP 状态，默认保留 24 小时。`sys_oss_ref` 以唯一 `oss_id + ref_type + ref_id` 保存轻量反向关系，其中 `ref_type` 是真实物理表名，`ref_id` 是真实主键。最后引用解除后重新进入 TEMP。引用只用于生命周期和定位，不做 ACL、动态外键、反射查表或业务回调。

无引用对象先持久化 `delete_state=PENDING`，再幂等删除 Provider 和元数据；失败保留 PENDING 供重试，新引用可在行锁内取消 PENDING。

## Consequences

业务 Service 必须先完成授权再维护引用。通用删除拒绝有引用对象；多态引用不能由数据库外键验证，表名变更需要同步迁移引用值。主动清理默认 disabled/dry-run，启用需独立批准。
