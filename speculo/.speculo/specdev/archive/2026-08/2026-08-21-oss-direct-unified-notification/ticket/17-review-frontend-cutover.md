---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-17
title: 前端策略化上传与富文本回显修复
status: done
planning_depth: standard
planning_depth_reason: 多组件共享 Hook 与异步回显竞态
ready: true
risk: high
blocked_by: [T-16]
contract_ids: [AC-003, AC-004, AC-014]
owner: codex
expected_changes: ["<Path>plus-ui-namewta/src/components/**</Path>", "<Path>plus-ui-namewta/src/views/system/user/profile/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/src/**</Path>"]
read_only_paths: []
shared_paths: []
shared_path_owners: []
---

# Ticket T-17: 前端策略化上传与富文本回显修复

## 1. 战略与来源
来源 CR-001。所有上传消费者复用策略化工厂，Editor 防止陈旧异步结果覆盖最新内容。

## 2. 决策状态
File=document、Image=image、Avatar=avatar、Editor=image/video；generation 只接纳最新解析。

## 3. 范围边界
IN：现有消费者。REUSE：useDirectOssUpload。OUT：新 UI 框架。

## 4. 要构建什么
组件保持原 modelValue 合同，同时服务端策略与前端用途一致，快速切换内容不被旧请求覆盖。

## 5. 实现契约
- **兼容：** modelValue 仍为 ossId/oss marker；错误完成计数必须收敛。

## 6. 执行路线
1. 策略工厂。2. 迁移组件。3. Editor generation。4. test/lint/build。

## 7. 路径访问契约
只修改 frontend src。

## 8. 验证矩阵
 - **E2E disposition：** not-required；用户已确认不建设 E2E，执行环境为 current-workspace。
| 风险 | 接缝 | 预期 | Evidence |
|---|---|---|---|
| 竞态/策略错误 | Vitest + lint/build | 最新内容获胜，各组件策略正确 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-17.md</Path>` |

## 9. 发布、迁移与恢复
与后端策略配置配对发布。

## 10. 验收标准
- [x] test/lint/build 通过，type 诊断不超过既有 TS1149 基线。
