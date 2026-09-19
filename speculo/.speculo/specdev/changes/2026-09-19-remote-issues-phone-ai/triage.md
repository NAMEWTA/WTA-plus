---
schema_version: 1
artifact: triage
change: 2026-09-19-remote-issues-phone-ai
mode: intake
source: <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/source.md</Path>
classification: mixed
risk: high
route: specdev/grill-with-docs
ready_for_implementation: false
external_action: not-applicable
publish_action: not-requested
publish: null
updated_at: 2026-09-19T08:03:45Z
---

# Triage: 手机号必填与独立 AI 服务

## G 后的范围修订（权威为 LOG-003/004/005）

用户确认手机号按写入必填、保留旧用户登录；#2 完整移除 Snail AI，旧数据保留不迁移。#3 明确“先不做，另开一个 change”，转入 <Path>{roots.state}/specdev/changes/2026-09-19-go-python-ai-platform/</Path> 并暂缓。以下摄入记录保留原始分类证据；后续实施范围仅 #1/#2，原 source 不覆盖。

## 当前判定

- **影响：** 账号创建、维护、导入与个人资料合同；Snail AI 的 Java、前端、数据库和交付面；Go/Python 新运行面与 Java 调用边界。
- **紧急度：** normal；来源未声明线上事故或时间承诺。
- **来源：** 当前对话明确要求将获取的 issue 全部集中到一个新 change。远程全分页查询得到 3 条 open、0 条 closed Issue，排除 PR，全部 0 评论。完整来源与内容摘要见 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/issue-index.md</Path>。
- **类别：** #1 bug；#2 refactor；#3 feature。#2 不使用 Snail AI 已明确，#3 为尚未界定首版行为的构想。
- **当前证据：** 工作树仍有 Snail AI server/common/module、前端入口及发布配置；手机号部分入口无必填约束。相关实际入口由 G 静态调查登记；静态调查不等于已运行的复现或验收。
- **相关代码/工件：** <Path>backend/wta-modules/wta-system/</Path>、<Path>backend/wta-modules/wta-ai/</Path>、<Path>backend/wta-common/wta-common-ai/</Path>、<Path>backend/wta-extend/</Path>、<Path>frontend/</Path>、<Path>release-artifacts/</Path>。
- **风险：** 必填切换对存量空手机号的兼容影响；删除第三方运行面时遗漏配置/菜单/表/发布合同；新增 Go/Python 服务的身份、权限、数据和模型凭据边界未锁定。尚无生产迁移或删除真实数据授权。
- **查重与相关历史：** active/archive 来源未命中三条 locator；capture 精确命中三行。旧 comprehensive-review 有相关 AI/注册代码审查，但无同 locator 来源、不是本次功能 intake，保留其独立状态和授权。永久 ADR/context 搜索无相同功能需求的拒绝项。
- **聚合裁决：** 用户最新指令替代每条 capture 各建 change 的默认路由。聚合 source 只冻结本次对话；每条 Issue 各有单一 locator 与独立快照，没有覆盖 capture 原始正文或把多个 locator 写进单份 source frontmatter。

## 未知项

- **可发现事实：** 全部手机号写入入口、唯一性与测试接缝；Snail AI 删除闭包、第三方 schema/发布依赖；现有 OpenAPI 的方向与出站复用能力。已交由两名只读调查者，Lead 负责汇总。
- **需要用户决定：** 手机号存量兼容政策；AI 首版功能与目标用户、管理端归属；模型接入、身份与数据持久化/部署验收边界。后续问题按设计树依赖逐轮出现，不提前锁定答案。
- **低影响实现细节：** 符合现有 wta 命名的目录与局部实现组织、沿用现有手机号格式文案、复用测试接缝，由事实与后续 Ticket 决定。来源使用的 cde 字样不构成全仓重命名要求。

## 路由

- **下一 Work：** <Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>。
- **理由：** 用户明确串联且存在产品、数据兼容与架构高影响决定；G 先查事实并提出完整 frontier，达成共识后依次进入 S / T / P。当前未批准 Direct Spec。
- **后续入口：** <Path>{roots.workflows}/specdev/S-spec/S-spec.md</Path> → <Path>{roots.workflows}/specdev/T-tickets/T-tickets.md</Path> → <Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>。
- **本轮终点：** 按用户指定链完成决策、Ready Spec、Ready Ticket/Map 和 Goal Plan；P 默认 plan。本次没有自动派生提交、推送、部署、远程关闭或归档权限。

## 外部动作

- **聚合 source：** conversation，无单个远程关闭对象，故 frontmatter external_action=not-applicable。
- **远程目标：** 三条原始 Issue 的 URL、关闭能力与逐条 pending-close 状态位于 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/issue-index.md</Path>，不丢失来源关闭义务。
- **关闭能力：** 每条 GitHub Issue supported；聚合对话 not-applicable。日后完成后需逐条准确重读和确认，不能把单 locator reconcile 工具直接当作批量关闭。
- **授权记录：** 当前只有远程读取和本地 intake/规划授权。
- **尝试与结果：** 远程列表与 issue-read 成功；远程写入 0。

## 发布投影

- **publish_action：** not-requested。
- **账本：** 未创建；没有发布投影请求。
- **origin：** 聚合授权 local；三条独立快照 intake。
