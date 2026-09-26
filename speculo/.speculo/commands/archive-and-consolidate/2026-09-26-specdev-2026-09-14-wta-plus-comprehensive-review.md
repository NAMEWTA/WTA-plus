# Comprehensive review 归档准备计划

范围 archive-single；知识策略 mechanical-only。本次无永久知识写入、合并、删除或清理。

路径上下文：project_root=/srv/WTA-plus；workflow_root=speculo/workflows/specdev；state_root=speculo/.speculo/specdev；changes_root=speculo/.speculo/specdev/changes；archive_root=speculo/.speculo/specdev/archive；commands_root=speculo/.speculo/commands（不是 speculo/commands）。

已获用户授权：先 commit/push，然后归档，再 commit/push。第一批产品修复已由 94091cda、c671b8cf 提交并推送 origin/main。

计划动作：

1. 整体移动 `speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review` 至 `speculo/.speculo/specdev/archive/2026-09/2026-09-14-wta-plus-comprehensive-review`。
2. 仅更新本 change 状态（archived=true、change_status=archived、archive_path 与日期）、全局 active/archived 索引。OIDC active 条目保持原样。
3. 在移动前保留完整文件摘要；移动后逐项比对，除已列明的状态元数据外内容不变。
4. 运行 complete 与包级 self-check，确认 clean Git、源不存在、目标完整、active/archived 无重叠，再 commit/push。

前置阻断：若用户未批准采用本轮 clean 提交的补充验收替代缺失的历史 clean 时点证明，则不标记 completed、不执行移动。所有历史失败、未执行项与 Windows 豁免保持原事实。T43 专项探针同步与真实回归进行中。

尚未移动、未改永久知识；此为准备计划。归档动作本身已经获授权，待裁决的是历史验收方式。原始证据格式已经准备规范化，并与当前补验明确分开。
