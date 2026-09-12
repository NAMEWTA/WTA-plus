# ADR-0004: 静态命名上传策略

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-004

## Context

直传文件字节不经过 Controller，前端扩展名和大小检查不能构成安全边界。

## Decision

服务端以类型化配置加载并在启动时校验命名 `uploadPolicy`。策略冻结允许类型、最大大小、对象前缀、SINGLE/MULTIPART 模式、阈值、权限和可选 Client 准入；Client 只限制入口调用资格。Complete 后执行 HEAD、策略与适用 magic bytes 校验，失败时删除对象和 Ticket 且不生成 `ossId`。

## Consequences

前端只提交策略键，不能自报安全限制。策略修改需要配置发布；有限文件头校验不等同于杀毒或隔离扫描。
