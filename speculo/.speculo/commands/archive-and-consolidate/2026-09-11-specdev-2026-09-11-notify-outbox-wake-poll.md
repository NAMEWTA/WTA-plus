# Archive and Consolidate Dry-Run Report

> 生成时间：2026-09-11 23:41 (Asia/Shanghai)
> Workflow：specdev
> 模式：archive-single
> 知识策略：generic
> Change：2026-09-11-notify-outbox-wake-poll
> 确认状态：dry-run（本报告生成时未执行移动/知识写入/清理）

## Path Context

| Key | Path |
|-----|------|
| project_root | `/workspace/vp-dev/ruoyi-vue-plus-docs` |
| workflow_root | `speculo/workflows/specdev` |
| state_root | `speculo/.speculo/specdev` |
| changes_root | `speculo/.speculo/specdev/changes` |
| archive_root | `speculo/.speculo/specdev/archive` |
| commands_root | `speculo/commands` |
| knowledge adr | `speculo/.speculo/specdev/adr` (exists) |
| knowledge context | `speculo/.speculo/specdev/context` (exists) |
| knowledge research | `speculo/.speculo/specdev/research` (exists, empty) |

## 完成门 / 预检

| 检查项 | 状态 | 备注 |
|--------|------|------|
| change 名称日期 kebab | pass | `2026-09-11-notify-outbox-wake-poll` |
| `.status.json` 可解析且 `completed` | pass | 完成门前置已写入 `completed_at=2026-09-11T23:41:05+08:00` |
| triage `external_action` | pass | `not-applicable`（对话来源，无远程关闭） |
| 源存在 | pass | `changes/2026-09-11-notify-outbox-wake-poll/` |
| 目标不存在 | pass | `archive/2026-09/...` 尚无 |
| 全局 status：仅在 active、不在 archived | pass | |
| Tickets T-01/02/03 done | pass | Map + frontmatter |
| Evidence Lead accept + commits in HEAD | pass | `2e05d989`/`5936be32`/`0badb07e`/`8a8316b` |
| CR-001 approved | pass | |
| blockers/deviations 空 | pass | |
| 并行 change 不在本计划 | pass | **不归档** `2026-09-10-notify-channel-config` |
| worktree 未合并 | n/a | current/direct-parent；无挂起 worktree |
| Goal Plan frontmatter 仍 `status: ready` | note | 文档滞后；票/Evidence/CR/CTO 批准已闭合完成门，不作为 blocker |

**声明：除完成门前置（triage.md + `.status.json` → completed）外，dry-run 未修改任何文件。此为 dry-run 计划，请确认后执行。**

CTO 已当面确认本 change 归档批准；若本计划无 blocker / 无 needs-confirmation 阻塞项，调用方将直接进入 confirmed。

---

# 阶段一：归档移动 + 知识合并

## Archive Plan

### 预检摘要

| 检查项 | 状态 |
|--------|------|
| changes_root 可访问 | pass |
| archive_root 可访问 | pass |
| status.json 可解析 | pass |
| 候选 change 数量 | 1 |
| 预检通过数 | 1 |
| 预检阻塞数 | 0 |

### 逐项归档计划

| # | Change | 源路径 | 目标路径 | 状态 | 备注 |
|---|--------|--------|---------|------|------|
| 1 | 2026-09-11-notify-outbox-wake-poll | `speculo/.speculo/specdev/changes/2026-09-11-notify-outbox-wake-poll/` | `speculo/.speculo/specdev/archive/2026-09/2026-09-11-notify-outbox-wake-poll/` | ready | 破坏性：目录 mv；更新全局 active→archived；归档 `.status.json` → archived |

### 状态变更（confirmed 时）

- 全局 `status.json`：`active` 移除本 change；`archived` 去重追加名称
- 归档 `.status.json`：`change_status: archived`，`archived: true`，`archive_path: <Path>{roots.state}/specdev/archive/2026-09/2026-09-11-notify-outbox-wake-poll</Path>`，`current_work: null`，`updated_at` 刷新
- **不**触碰 `2026-09-10-notify-channel-config`

### 阻塞项详情

无。

---

## Consolidation Plan

> 扫描 change 数：1
> 知识产物：ADR.md、CONTEXT.md、LOG.md、Evidence、reviews、goal-plan、spec（评估毕业）
> 目标 stores：adr/、context/、research/（research 无提取）

### 提取摘要

| 目标 Store | 新建 | 合并 | 冲突(需确认) | 跳过(Ephemeral) |
|------------|------|------|-------------|----------------|
| adr/ | 2 | 0 | 0 | 1（ADR-002 superseded） |
| context/ | 1 文件（4 术语） | 0 | 0 | 1（「本 change 范围」） |
| research/ | 0 | 0 | 0 | — |

### adr/

#### [NEW] 0044-outbox-write-wake-and-slow-poll.md
- **来源 change**：2026-09-11-notify-outbox-wake-poll
- **决策标题**：保留 Outbox；写入唤醒 + 慢速兜底；跨进程唤醒一等
- **毕业判定**：stable-mechanism + must-know
- **内容摘要**：保留 Transactional Outbox；调度改为写入唤醒 + 默认 60s 兜底；跨进程一等；不扩配置页/MQ Relay/common-notify。
- **Supersedes**：none（相对永久库）
- **来源 change ADR**：ADR-001

#### [NEW] 0045-redis-cross-process-outbox-wake-reuses-claim.md
- **来源 change**：2026-09-11-notify-outbox-wake-poll
- **决策标题**：Redis 为主的跨进程唤醒；唤醒后复用 claim
- **毕业判定**：stable-mechanism + must-know
- **内容摘要**：Redis pub/sub（或 list）为主唤醒；同 JVM afterCommit 仅辅；信号 afterCommit；唤醒只走既有 claim+dispatch；`next_attempt_at` 只靠慢速兜底。
- **Supersedes**：none（相对永久库；change 内 ADR-002 已撤回，不提升）
- **来源 change ADR**：ADR-003
- **与 ADR-0006/0007 关系**：互补不冲突——继续薄 common-notify；在 ruoyi-notify 实现 Outbox 可靠投递调度

### context/

#### [ADD 文件] `notify-outbox-wake-terms.md`（4 术语）
- **Transactional Outbox（本模块）**：毕业 stable-mechanism / must-know
- **Claim / Lease**：毕业 stable-mechanism / must-know
- **跨进程写入唤醒**：毕业 stable-mechanism / must-know
- **慢速兜底扫描**：毕业 stable-mechanism / must-know
- **冲突**：无（永久 context 尚无同名 Outbox 唤醒术语）

### Ephemeral（不提取）

| Change | 知识项 | 跳过原因 |
|--------|--------|---------|
| 本 change | ADR-002（已撤回） | superseded / 反毕业 |
| 本 change | CONTEXT「本 change 范围」 | 单次 change 边界说明，非跨 change 术语 |
| 本 change | LOG.md 访谈细节 / design-tree / Evidence 行级实现 / CR-001 | 反毕业：实现细节或过程记录；结论已由 ADR/CONTEXT 捕获 |
| 本 change | Goal Plan / Spec 全文 | 合同工件随归档保留，不提升 |

---

# 阶段二：清理候选

## Cleanup Candidates

> 扫描 store：adr/、context/、research/
> 本 change 相关：无既有 Outbox 唤醒 ADR 可删；新建 ADR 将 keep

### 分类摘要

| 分类 | 数量 |
|------|------|
| delete | 0 |
| merge | 0 |
| rewrite | 0 |
| keep | 全部既有 adr/context（仍被引用或现役） |
| needs-confirmation | 0 |

### Delete / Merge / Rewrite / Needs-Confirmation

无（对本批归档无破坏性清理动作）。

### Keep（摘录）

| # | 文件/条目 | 保留原因 |
|---|----------|---------|
| 1 | adr/0006、0007 | 现役通知契约；与本提升互补 |
| 2 | context/oss-direct-notification-terms.md | 现役同步通知术语 |
| 3 | 其余 adr/context | 无本批删除证据 |

### 反模式标记

无新增。

---

## 摘要

| 项 | 值 |
|----|-----|
| 待归档 change | 1 |
| 待合并知识 | 2 ADR + 1 context 文件（4 术语） |
| 待清理候选（动作） | 0 |
| needs-confirmation | 0 |
| blocker | 0 |

## 破坏性动作清单（confirmed 时）

1. `mv` change 目录 → `archive/2026-09/...`
2. 改写全局 `status.json` active/archived
3. 改写归档 `.status.json` 终态字段
4. 新建 `adr/0044-*.md`、`adr/0045-*.md`
5. 新建 `context/notify-outbox-wake-terms.md`

**不执行：** git push、删除整个库、改业务源码、归档并行 change。

---

# 执行后验证补遗（confirmed）

> 执行时间：2026-09-11 23:42 (Asia/Shanghai)
> mode：executed / confirmed（CTO 已批；dry-run 无 blocker / 无 needs-confirmation）

| 检查 | 结果 |
|------|------|
| 源 `changes/2026-09-11-notify-outbox-wake-poll/` 不存在 | pass |
| 目标 `archive/2026-09/2026-09-11-notify-outbox-wake-poll/` 完整 | pass |
| 全局 status：active 仅并行 change；archived 含本 change；无重叠 | pass |
| 归档 `.status.json`：`change_status=archived`，`archived=true`，`archive_path` 正确，`current_work=null` | pass |
| 知识：`adr/0044-*.md`、`adr/0045-*.md`、`context/notify-outbox-wake-terms.md` 存在 | pass |
| 并行 `2026-09-10-notify-channel-config` 仍在 changes/ 且 active | pass |
| 未 git push；未改业务源码；未删库 | pass |

**verdict:** verified
