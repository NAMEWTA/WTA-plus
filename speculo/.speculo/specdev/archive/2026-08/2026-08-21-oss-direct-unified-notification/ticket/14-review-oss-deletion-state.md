---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-14
title: OSS 生命周期幂等与可恢复删除
status: done
planning_depth: deep
planning_depth_reason: 外部 Provider 副作用与数据库事务一致性
ready: true
risk: critical
blocked_by: [T-13]
contract_ids: [AC-007, AC-008, AC-010, AC-011]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**/oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/**</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"]
read_only_paths: []
shared_paths: []
shared_path_owners: []
---

# Ticket T-14: OSS 生命周期幂等与可恢复删除

## 1. 战略与来源
修复重复 unbind 重置宽限期和 Provider 删除不可补偿。来源 CR-001、ADR-005。

## 2. 决策状态
使用 ACTIVE/PENDING 两阶段删除；bind 可取消 PENDING；重复 unbind 不改变生命周期。

## 3. 范围边界
IN：状态、行锁、清理。REUSE：现有 TEMP/ref。OUT：消息队列。

## 4. 要构建什么
删除请求先变成可观察 PENDING；后续任务幂等删除 Provider 与元数据，失败后继续重试。

## 5. 实现契约
- **状态流：** ACTIVE -> PENDING -> metadata removed；PENDING -> ACTIVE 仅由有效 bind。

## 6. 执行路线
1. 状态机测试。2. Mapper/DDL。3. 两阶段清理。4. 失败回归。

## 7. 路径访问契约
DDL 只追加；不修改冻结基线 SQL。

## 8. 验证矩阵
 - **E2E disposition：** not-required；用户已确认不建设 E2E，执行环境为 current-workspace。
| 风险 | 接缝 | 预期 | Evidence |
|---|---|---|---|
| DB/Provider 失配 | 生命周期单测 | Provider 只在已持久化 PENDING 后调用 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-14.md</Path>` |

## 9. 发布、迁移与恢复
先加 delete_state 默认 ACTIVE；旧应用忽略新列。清理保持 dry-run 默认。

## 10. 验收标准
- [x] 两阶段、失败保留、bind 取消和重复 unbind 测试通过。
