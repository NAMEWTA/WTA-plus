# 工作记录

## Goal

完成用户指定的 WTA-plus 全仓架构、代码、目录、文档、交付及 UI/UX 审查，输出可供本人或其他 AI 接续的详实改造计划。仅写本 change；不实现、不提交、不发布。

## Current status

已激活 R-review-architecture；root 与三个审查代理并行取证。Spec/ADR/Tickets 均为待用户审核草案，尚未 Ready。

## Decisions

- 使用现有 roots.workflows=speculo/workflows、roots.state=speculo/.speculo。
- 用户只允许写 change，故不登记全局 status/config、不修改永久 ADR/context。
- 不兼容升级是建议目标；受影响的硬约束单列 ADR 决策提案，未默认撤销。
- 历史归档、上游许可证、第三方 schema、生成物不能因旧名称而自动删除。

## Files changed

只新增本 change 文件；初始工作树及逐文件 SHA-256 位于 Evidence。

## Remaining work

1. 核查每个主轴及业务模块，保存覆盖矩阵与真实源码证据。
2. 复核代理发现，区分已确认、待复现、保留设计与未审范围。
3. 编写总报告、改造方案、spec、ADR、ticket、tickets-map、清理清单和交接说明。
4. 校验路径、引用、依赖图、状态、需求追踪和只写 change 边界。

## Verification / 完成标准

- 每项进入计划的发现均有现状证据、风险、目标、修改/删除范围和验收场景。
- 所有 Ticket 可追溯 finding 与 AC，依赖无环，均 draft/ready=false。
- 不把静态推断当浏览器/数据库/部署验证；记录实际命令及退出码。
- 完成文件回读、文档校验及工作树 SHA-256 对比，解释任何外部并发变化。
- 用户审核入口明确；实施授权与评审交付完成分开。
