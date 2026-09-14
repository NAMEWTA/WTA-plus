# Archive and Consolidate Dry-Run Report

> 生成时间：2026-09-13 07:23 (Asia/Shanghai)
> Workflow：specdev
> 模式：archive-single
> 知识策略：generic
> Change：2026-09-10-notify-channel-config
> 确认状态：dry-run（本报告生成时未执行移动/知识写入/清理）

## Path Context

| Key | Path |
|-----|------|
| project_root | `/workspace/vp-dev/WTA-plus` |
| workflow_root | `speculo/workflows/specdev` |
| state_root | `speculo/.speculo/specdev` |
| changes_root | `speculo/.speculo/specdev/changes` |
| archive_root | `speculo/.speculo/specdev/archive` |
| commands_root | `speculo/commands` |
| knowledge adr | `speculo/.speculo/specdev/adr` (exists; next=0056) |
| knowledge context | `speculo/.speculo/specdev/context` (exists) |
| knowledge research | `speculo/.speculo/specdev/research` (exists, empty; 无提取) |

## 完成门 / 预检

| 检查项 | 状态 | 备注 |
|--------|------|------|
| change 名称日期 kebab | pass | `2026-09-10-notify-channel-config` |
| `.status.json` 可解析且 `completed` | pass | `completed_at=2026-09-13T07:22:30+08:00` |
| triage `external_action` | pass | `not-applicable`（对话来源，无远程关闭） |
| 源存在 | pass | `changes/2026-09-10-notify-channel-config/` |
| 目标不存在 | pass | `archive/2026-09/2026-09-10-notify-channel-config/` 尚无 |
| 全局 status：本 change 在 active、不在 archived | pass | 全局 active 目前仅本 change；并行 `2026-09-12-wta-sso` 目录存在且 change_status=active，**未**入全局 active（归档后补入，不改其目录内容） |
| Tickets T-01–T-06 done | pass | Map + frontmatter |
| worktrees integrated + result_sha ancestor of HEAD | pass | result_sha `3a86dfe…` ⊂ HEAD `98e0c8b` |
| CR-003 approved（本地双轴） | pass | `external_brain_gate: suspended-by-cto-lead` |
| blockers | pass | 已清空；原两项记入 deviations waived（CR-003） |
| 并行 change 不在本计划 | pass | **不归档 / 不改内容** `2026-09-12-wta-sso` |
| worktree 未合并 | n/a | current/direct-parent |
| Goal Plan frontmatter 仍 `status: ready` | note | 文档滞后；票/Evidence/CR/Lead 批准已闭合完成门，不作为 blocker |
| `--stage complete` 无 `--repo` | pass | 0 error |
| `--stage complete --repo` | note | 6× dirty-while-integrated：仅 speculo 过程件 + 未跟踪的 `wta-sso/`；**无业务源码脏**。不 commit、不 push |

**声明：除完成门前置（`external-brain/notes.md` 归档时外脑跳过声明 + `triage.md` + `.status.json` → completed）外，dry-run 未修改任何文件。此为 dry-run 计划，请确认后执行。**

Lead PRIORITY 已授权本 change 归档；本计划无 blocker / 无 needs-confirmation → 调用方将直接进入 confirmed。**不 push。**

### 外脑（归档时）

- ChatGPT/GPT：CTO t148u 作废；**未**再走、**未**假装通过
- 历史 CR-003：仍 **BLOCKED**（CF Just a moment）/ 门禁 **SUSPEND**
- 本轮外脑：**跳过**（可改走 Research，本轮不阻塞归档）

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
| 1 | 2026-09-10-notify-channel-config | `speculo/.speculo/specdev/changes/2026-09-10-notify-channel-config/` | `speculo/.speculo/specdev/archive/2026-09/2026-09-10-notify-channel-config/` | ready | 破坏性：目录 mv；全局 active→archived；归档 `.status.json` → archived |

### 状态变更（confirmed 时）

- 全局 `status.json`：`active` 移除本 change；**补入**仍 active 的 `2026-09-12-wta-sso`（仅索引，不改其目录）；`archived` 去重追加本 change 名称
- 归档 `.status.json`：`change_status: archived`，`archived: true`，`archive_path: <Path>{roots.state}/specdev/archive/2026-09/2026-09-10-notify-channel-config</Path>`，`current_work: null`，`updated_at` 刷新
- **不**触碰 `changes/2026-09-12-wta-sso/` 内容

### 阻塞项详情

无。

---

## Consolidation Plan

> 扫描 change 数：1
> 知识产物：ADR.md（001–016）、CONTEXT.md、LOG.md、Evidence、reviews、goal-plan、spec
> 目标 stores：adr/、context/、research/（research 无提取）

### 提取摘要

| 目标 Store | 新建 | 合并 | 冲突(需确认) | 跳过(Ephemeral) |
|------------|------|------|-------------|----------------|
| adr/ | 13 | 0 | 0 | 3（ADR-001 本期范围；ADR-009 superseded；ADR-013 一次清扫） |
| context/ | 1 文件（14 术语） | 0 | 0 | 「首期场景清单」并入术语；LOG/过程不提取 |
| research/ | 0 | 0 | 0 | — |

不改写永久 ADR-0006/0007 或 `oss-direct-notification-terms.md`（数据面仍有效；控制面新 ADR 互补，避免 needs-confirmation）。

### adr/

序号从现库最大 0055 起。

#### [NEW] 0056-notify-module-owns-config-control-plane.md
- 来源 ADR-002；毕业：stable-mechanism + must-know
- notify 模块拥有配置表/管理 API/菜单/页；common-mail/sms 仅运行时 SPI；common-notify 不建模板中心

#### [NEW] 0057-database-sole-runtime-authority-mail-sms-accounts.md
- 来源 ADR-003；毕业：stable-mechanism + must-know
- MAIL/SMS 账号运行时权威是数据库；YAML 不留发件人/供应商并行权威

#### [NEW] 0058-independent-account-enable-and-explicit-route.md
- 来源 ADR-004；毕业：stable-mechanism + must-know
- 账号独立启停、可同时启用多个；发送必须按路由选明确账号，禁止隐式默认

#### [NEW] 0059-code-seeded-scenes-and-locked-variables.md
- 来源 ADR-005；毕业：stable-mechanism + must-know
- 逻辑场景代码播种；管理员不可增删场景/改变量名

#### [NEW] 0060-scene-channel-unique-account-binding.md
- 来源 ADR-006；毕业：stable-mechanism + must-know
- 场景×渠道唯一账号绑定；普通调用方不传 providerKey；绑定缺失/停用则该渠道失败关闭

#### [NEW] 0061-channel-secrets-plaintext-no-echo.md
- 来源 ADR-007；毕业：stable-mechanism + must-know
- SMTP/短信密钥库内明文（跟 OSS）；管理接口不回显；空白保持原值

#### [NEW] 0062-account-and-template-test-send.md
- 来源 ADR-008；毕业：stable-mechanism + must-know
- 账号级 + 模板级试发；独立权限；不得绕过停用/绑定/变量契约

#### [NEW] 0063-scene-template-is-mail-sms-content-authority.md
- 来源 ADR-010；毕业：stable-mechanism + must-know
- MAIL/SMS 发送内容以场景模板+变量为准；调用方 title/content 不是发送权威

#### [NEW] 0064-captcha-template-code-sync-not-sync-send.md
- 来源 ADR-011；毕业：must-know
- 验证码补稳定 templateCode；SYNC 仍只入队，不是请求线程同步发送

#### [NEW] 0065-account-and-scene-send-quotas.md
- 来源 ADR-012；毕业：stable-mechanism + must-know
- 账号每分钟上限 + 短信场景更细上限（不得超过账号上限）

#### [NEW] 0066-recipient-throttle-per-scene-channel-bind.md
- 来源 ADR-014；毕业：stable-mechanism + must-know
- 收件人拦截按场景×渠道绑定配置，不是 YAML 全局

#### [NEW] 0067-quota-exhaustion-fail-closed.md
- 来源 ADR-015；毕业：stable-mechanism + must-know
- 任一配额用尽立即失败关闭该渠道，不改选、不延期

#### [NEW] 0068-notice-workflow-wrapper-templates.md
- 来源 ADR-016；毕业：stable-mechanism + must-know
- notice-published / workflow-task 使用包装模板，必须保留已声明变量

### context/

#### [ADD 文件] `notify-channel-config-terms.md`
术语：通知配置、渠道账号、发送路由、配置权威、逻辑场景、变量契约、渠道绑定、测试发送、发送内容权威、变量占用位置、收件人拦截、账号发送配额、包装模板、首期逻辑场景。
- 与 `oss-direct-notification-terms.md` 的 Notify Channel/Provider（数据面）分层互补，不覆盖。

### Ephemeral（不提取）

| Change | 知识项 | 跳过原因 |
|--------|--------|---------|
| 本 change | ADR-001 本期只做运维面 | 单次 change 范围/过程编排 |
| 本 change | ADR-009 YAML 频控不进页 | superseded by ADR-012 |
| 本 change | ADR-013 清除硬编码正文 | 一次实现清扫；耐久规则在 ADR-010/0063 |
| 本 change | LOG / design-tree / Evidence 行级 / CR 过程 P-01–P-08 | 反毕业：过程记录 |
| 本 change | Goal Plan / Spec 全文 | 合同工件随归档保留 |
| 本 change | 外脑 notes / CR-003 SUSPEND | 过程门禁记录，随归档保留，不提升 |

---

# 阶段二：清理候选

## Cleanup Candidates

> 扫描 store：adr/、context/、research/
> 无 delete/merge/rewrite/needs-confirmation

### 分类摘要

| 分类 | 数量 |
|------|------|
| delete | 0 |
| merge | 0 |
| rewrite | 0 |
| keep | 全部既有 adr/context |
| needs-confirmation | 0 |

### Keep（摘录）

| # | 文件/条目 | 保留原因 |
|---|----------|---------|
| 1 | adr/0006、0007、0008、0009 | 现役数据面通知契约；与本提升互补 |
| 2 | adr/0044、0045 | Outbox 调度；不冲突 |
| 3 | context/oss-direct-notification-terms.md、notify-outbox-wake-terms.md | 现役；不改写 |
| 4 | 其余 adr/context | 无本批删除证据；<30 天或仍现役 |

### 反模式标记

无新增。

---

## 摘要

| 项 | 值 |
|----|-----|
| 待归档 change | 1 |
| 待合并知识 | 13 ADR + 1 context 文件 |
| 待清理候选（动作） | 0 |
| needs-confirmation | 0 |
| blocker | 0 |

## 破坏性动作清单（confirmed 时）

1. `mv` change 目录 → `archive/2026-09/2026-09-10-notify-channel-config/`
2. 改写全局 `status.json` active/archived（active 仅留 `2026-09-12-wta-sso`）
3. 改写归档 `.status.json` 终态字段
4. 新建 `adr/0056`–`adr/0068`
5. 新建 `context/notify-channel-config-terms.md`

**不执行：** git push、删除整个库、改业务源码、改/删 `2026-09-12-wta-sso` 目录内容。


---

# 执行后验证补遗（confirmed）

> 执行时间：2026-09-13 07:24 (Asia/Shanghai)
> mode：executed / confirmed（Lead PRIORITY 已批；dry-run 无 blocker / 无 needs-confirmation）
> 未 git push；未改业务源码；未改 `2026-09-12-wta-sso` 目录内容

| 检查 | 结果 |
|------|------|
| 源 `changes/2026-09-10-notify-channel-config/` 不存在 | pass |
| 目标 `archive/2026-09/2026-09-10-notify-channel-config/` 完整（32 文件 digest 与移动前一致 + 归档后 `.status.json` 终态） | pass |
| 全局 status：active 仅 `2026-09-12-wta-sso`；archived 含本 change；无重叠 | pass |
| 归档 `.status.json`：`change_status=archived`，`archived=true`，`archive_path` 正确，`current_work=null`，`completed_at=2026-09-13T07:22:30+08:00` | pass |
| 知识：`adr/0056`–`0068`、`context/notify-channel-config-terms.md` 存在；未改 ADR-0006/0007、`oss-direct-notification-terms.md` | pass |
| 并行 `changes/2026-09-12-wta-sso/` 仍在且 change_status=active（12 文件 digest 未变） | pass |
| `--stage complete`（移动前、无 `--repo`） | pass 0 error |
| `--stage complete --repo` | note：dirty-while-integrated 仅 speculo 过程件 + 未跟踪 wta-sso；无业务源码脏 |
| 移动后对归档目录跑 `--stage complete` | note：`change_status=archived` 故 complete 阶段报「requires completed」——属终态，不回写 |
| 未 git push；HEAD 仍 `98e0c8b`；`main` ahead origin/main 1 | pass |
| 外脑 notes 保留 SUSPEND/BLOCKED；本轮跳过；未标通过 | pass |

**verdict:** verified
