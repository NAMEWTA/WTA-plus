---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-15
title: 上传策略与安全续传修复
status: done
planning_depth: deep
planning_depth_reason: 跨 Redis 会话、签名与浏览器恢复状态
ready: true
risk: high
blocked_by: [T-14]
contract_ids: [AC-001, AC-004, AC-005, AC-014, AC-032]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**/upload/**</Path>", "<Path>plus-ui-namewta/src/hooks/oss/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/**</Path>", "<Path>plus-ui-namewta/src/**</Path>"]
read_only_paths: []
shared_paths: []
shared_path_owners: []
---

# Ticket T-15: 上传策略与安全续传修复

## 1. 战略与来源
来源 CR-001。目标是按身份和策略安全恢复 SINGLE/Multipart，上传成功不依赖管理下载权限。

## 2. 决策状态
Ticket 同时绑定 userId/clientPk；SINGLE resume 返回新签名；本地 key 哈希包含 Client、Token、policy、fingerprint。

## 3. 范围边界
IN：会话、命名策略、Hook。REUSE：现有 ticket store/transport。OUT：跨设备续传。

## 4. 要构建什么
同一登录上下文可恢复；其他 Client 拒绝；过期 SINGLE 可重签；各组件显式选择 policy。

## 5. 实现契约
- **失败行为：** 已知 stale session 清除本地记录；下载 URL 获取失败仍返回上传成功。

## 6. 执行路线
1. 服务端恢复测试。2. 策略配置。3. Hook 隔离键与容错。4. 前端测试。

## 7. 路径访问契约
不触碰 backend 既有 dirty `ruoyi-admin/pom.xml`。

## 8. 验证矩阵
 - **E2E disposition：** not-required；用户已确认不建设 E2E，执行环境为 current-workspace。
| 风险 | 接缝 | 预期 | Evidence |
|---|---|---|---|
| 会话劫持/签名过期 | admin + Vitest | owner 校验、SINGLE 重签、哈希隔离 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-15.md</Path>` |

## 9. 发布、迁移与恢复
前后端配对发布；无数据库迁移。

## 10. 验收标准
- [x] admin upload tests 与 Hook tests 通过。
