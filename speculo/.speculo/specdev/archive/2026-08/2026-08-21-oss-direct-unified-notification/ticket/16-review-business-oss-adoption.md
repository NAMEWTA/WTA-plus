---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-16
title: 业务 OSS 引用与下载授权接入
status: done
planning_depth: deep
planning_depth_reason: 用户头像、公告和通知跨事务生命周期
ready: true
risk: high
blocked_by: [T-15]
contract_ids: [AC-009, AC-010, AC-013, AC-028]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>", "<Path>plus-ui-namewta/src/views/system/notice/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>", "<Path>plus-ui-namewta/src/**</Path>"]
read_only_paths: []
shared_paths: []
shared_path_owners: []
---

# Ticket T-16: 业务 OSS 引用与下载授权接入

## 1. 战略与来源
来源 CR-001 与 ADR-002/005。目标是让已有业务真正使用统一引用和业务授权短链。

## 2. 决策状态
头像绑定 `sys_user`；富文本 oss marker 绑定 `sys_notice`；公告/通知端点校验业务归属后 presign。

## 3. 范围边界
IN：用户、公告、通知。REUSE：OssService。OUT：所有未来业务表自动扫描。

## 4. 要构建什么
业务写入成功后绑定新对象并解除旧引用；业务详情无需全局 OSS 权限即可加载自身附件。

## 5. 实现契约
- **事务：** 业务行与 bind/unbind 在同一动态数据源事务。
- **安全：** 业务端点只签发内容中实际引用的 ossId。

## 6. 执行路线
1. 头像 reconcile。2. 公告 marker reconcile。3. 业务下载端点。4. 前端 resolver。

## 7. 路径访问契约
common 不依赖 system；前端复用 Editor resolver 接缝。

## 8. 验证矩阵
 - **E2E disposition：** not-required；用户已确认不建设 E2E，执行环境为 current-workspace。
| 风险 | 接缝 | 预期 | Evidence |
|---|---|---|---|
| 孤儿/越权下载 | service/controller | 新旧引用收敛，只解析业务实际附件 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-16.md</Path>` |

## 9. 发布、迁移与恢复
无 schema 迁移；旧数据未含 marker 时不绑定，保持兼容。

## 10. 验收标准
- [x] 头像/公告引用与通知/公告业务短链闭环。
