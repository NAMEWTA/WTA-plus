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

## 2026-09-26 裁决与预检更新

用户明确回复“同意采用本轮补充验收”；此前的历史证据阻断按该授权变更接纳方式，历史clean缺失事实仍原样保留。T43单条claim专项真实smoke通过，Linux前后端各两次真实启动及doctor通过，存储10项通过，前后端质量门禁/release/包级自检通过。无永久知识动作。

完成治理提交前 complete 仅剩预期的 Git dirty 错误（1 error/0 warnings）；非空实现区间、串行父链、30条集成记录、30 done/20 cancelled、Skill记录和交付数量均已过校验。归档前先提交并推送治理记录，再要求 complete=0 和clean，未通过则不移动。

路径预检：唯一目标；目标不存在；本change仅在active；无父实现change；无triage源Issue或publish请求；源无逃逸符号链接。归档计划获用户当前指令授权，执行阶段保持 mechanical-only。原历史deviations已保存在change证据中，仅移除过期活动投影，不删除原事实。

## 已执行归档

归档前治理提交 `8853ca3349195812d011c65977cfed5d939f1b3b` 已push；其clean complete：0 errors/0 warnings。用户授权按当前补验完成并归档。已按计划原子移动全部 10836 文件，其中原Git跟踪 9827 文件；逐文件SHA-256核对仅 `.status.json` 的已列明归档元数据变化，其他内容原样保留，ignored本地证据也随目录移动、未强行新增追踪。完整清单见 `2026-09-26-specdev-2026-09-14-wta-plus-comprehensive-review-manifest.json`。全局active只移除本change，OIDC条目保持；无永久知识或其他清理。

源不存在、目标完整、全局active/archived无重叠、目标schema字段已回读确认。最后归档commit后再次运行complete与包级自检，随后push。

## 归档后验证补遗

2026-09-26T23:58:57.247252+00:00：归档提交 `3219bb78ae06f6dc9b6885d4038c1c04d926b793` 后工作树clean；`node speculo/workflows/specdev/common/tools/validate-specdev.mjs --stage complete --repo /srv/WTA-plus speculo/.speculo/specdev/archive/2026-09/2026-09-14-wta-plus-comprehensive-review` 与 `--self-check` 均 exit 0、0 errors/0 warnings。再次回读源/目标/全局索引/归档状态一致。移动暂存阶段的Git状态读取失败未被视为通过，最终以此clean提交后的真实校验为准。

归档目录从此只读。本补遗只更新commands报告；提交补遗后重复complete，再执行用户授权的最终push。无部署、永久知识提升或额外删除。
