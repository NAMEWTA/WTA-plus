---
schema_version: 1
artifact: triage
change: 2026-08-24-upstream-fork-upgrade-remediation
mode: intake
source: <Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/source.md</Path>
classification: implementation
risk: high
route: specdev/tickets
ready_for_implementation: true
external_action: not-applicable
updated_at: 2026-08-24T01:25:28+08:00
---

# Triage: 上游 Fork 评估问题整改

## 当前判定

- **影响：** 父仓治理、前后端质量门禁、后端构建配置、真实 Redis/MySQL/S3 验证、业务菜单数据和长期知识。
- **紧急度：** high；先修复不可复现快照与红色类型门禁，再扩展功能。
- **实施依据：** 两份固定点代码审查及其完整评估报告。

## 已锁定决策

- NAMEWTA 产品数据库合同明确为 MySQL-only，不在本 change 扩展其他方言。
- 四个无业务合同的占位页面和菜单入口下线，不臆造业务实现。
- 本地 6.X 镜像只做 fast-forward；产品 main 吸收上游非重叠补丁。
- CI 执行前后端现有门禁，并激活已有真实 Redis/MySQL 测试；S3 增加真实 MinIO 验证。
- 初始范围不提交、不推送、不部署；用户于 2026-08-24 后续明确授权执行计划中的 commit 与 push，部署仍未授权。

## 未知项

- **可发现事实：** CI 服务参数、MinIO client 构造、Maven profile 实际 reactor、占位菜单固定 ID 和工程知识计数。
- **需要用户决定：** 无；MySQL-only、无历史兼容负担和整改授权均已明确。
- **低影响实现细节：** workflow job 拆分、测试文件命名和文档章节位置。

## 路由

- **下一 Work：** `<Path>{roots.workflows}/specdev/T-tickets/T-tickets.md</Path>`
- **理由：** 跨父仓与两个子仓，包含数据迁移和多类独立验收，不能作为 Direct Spec 执行。

## 外部动作

- **远程目标：** 无
- **关闭能力：** not-applicable
- **当前状态：** not-applicable
- **授权记录：** 产品代码整改、commit 与 push 已由用户明确授权；部署、生产迁移与清理未授权。
