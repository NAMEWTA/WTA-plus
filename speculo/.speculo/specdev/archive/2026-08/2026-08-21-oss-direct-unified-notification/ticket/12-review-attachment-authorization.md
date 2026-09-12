---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-12
title: 收紧通知附件入口授权
status: done
planning_depth: standard
planning_depth_reason: CR-001 critical 权限边界修复
ready: true
risk: high
blocked_by: []
contract_ids: [AC-013, AC-023, AC-024]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/**/MailSendController.java</Path>", "<Path>plus-ui-namewta/src/views/monitor/notify/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/**</Path>", "<Path>plus-ui-namewta/src/api/monitor/notify/**</Path>", "<Path>plus-ui-namewta/src/views/monitor/notify/**</Path>"]
read_only_paths: []
shared_paths: []
shared_path_owners: []
---

# Ticket T-12: 收紧通知附件入口授权

## 1. 战略与来源
- **目标：** 阻止仅凭 ossId 通过 Demo 附件发送入口读取任意对象。
- **来源：** CR-001、AC-013、ADR-002。

## 2. 决策状态
附件 HTTP 入口要求 `system:oss:download`；内部业务仍在自身授权后调用快照 SPI。

## 3. 范围边界
IN：附件 Demo 入口权限。REUSE：Sa-Token 权限。OUT：通用 OSS ACL 引擎。

## 4. 要构建什么
无权限用户不能调用附件发送入口；内部 Job/Service 不被全局 OSS 权限错误阻断。

## 5. 实现契约
- **安全要求：** HTTP 边界显式鉴权；内部调用方承担业务授权。

## 6. 执行路线
1. 增加入口权限。2. 复核快照服务无 ThreadLocal 权限耦合。3. 运行附件测试。

## 7. 路径访问契约
修改仅限 frontmatter writable_paths；保留既有用户 dirty POM。

## 8. 验证矩阵
 - **E2E disposition：** not-required；用户已确认不建设 E2E，执行环境为 current-workspace。
| 风险 | 接缝 | 预期 | Evidence |
|---|---|---|---|
| 越权附件 | Controller/单测 | 无权限拒绝，快照失败不调用 Provider | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-12.md</Path>` |

## 9. 发布、迁移与恢复
无数据迁移；回退入口注解可恢复旧行为但不建议。

## 10. 验收标准
- [x] 附件入口显式鉴权且附件快照回归通过。
