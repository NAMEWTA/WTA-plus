---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-13
title: 通知敏感审计与物理清理修复
status: done
planning_depth: deep
planning_depth_reason: CR-001 涉及安全、数据索引与删除语义
ready: true
risk: critical
blocked_by: [T-12]
contract_ids: [AC-020, AC-025, AC-028, AC-029]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**/notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/**</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"]
read_only_paths: []
shared_paths: []
shared_path_owners: []
---

# Ticket T-13: 通知敏感审计与物理清理修复

## 1. 战略与来源
目标是让 credential-like 通知最小化审计、重复请求可持久化、清空有界且真正回收数据。来源 CR-001 与用户接受决定。

## 2. 决策状态
新增 FULL/REDACT_SENSITIVE；验证码强制 REDACT；requestId 非唯一；删除与清空物理删除。

## 3. 范围边界
IN：通知审计、索引、清理。REUSE：NotifyClient、Mapper XML。OUT：自动保留期策略。

## 4. 要构建什么
验证码内容不落库；重复幂等日志可并存；管理员清空按 500 条批次解除附件并删除父子日志。

## 5. 实现契约
- **不变量：** Provider Secret/签名 URL 不入库；附件先解绑。
- **迁移：** DDL 只在末尾追加索引调整。

## 6. 执行路线
1. 审计策略红测。2. Dispatcher/Listener 透传。3. 物理删除 Mapper。4. DDL 追加。5. 回归。

## 7. 路径访问契约
仅写 frontmatter 路径；DDL 只能追加。

## 8. 验证矩阵
 - **E2E disposition：** not-required；用户已确认不建设 E2E，执行环境为 current-workspace。
| 风险 | 接缝 | 预期 | Evidence |
|---|---|---|---|
| OTP 泄漏/重复/清理 | admin unit + DDL review | 内容空、目标脱敏、重复可插入、物理分批删除 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-13.md</Path>` |

## 9. 发布、迁移与恢复
先执行 NAMEWTA-OSS-NOTIFY-DDL-002；应用前向兼容新增普通索引。

## 10. 验收标准
- [x] 审计、重复和清理测试通过，DDL 保持只追加。
