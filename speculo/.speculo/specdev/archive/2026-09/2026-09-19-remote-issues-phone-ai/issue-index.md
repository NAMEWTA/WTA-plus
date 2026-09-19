# Remote Issue Intake

## 当前开发归属

- #1 / #2 继续属于本 change。
- #3 按用户 G Q2 的“这个先不做，另开一个change，等我想清楚”转入 <Path>{roots.state}/specdev/changes/2026-09-19-go-python-ai-platform/</Path>，当前暂缓。下面 #3 快照仅保留原始摄入审计，其待关闭责任随新 change 转移，不属于本 change 的 Done/关闭范围。
- 原始捕获表不覆盖；最新范围来源为 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/LOG.md</Path> LOG-004。

- Repository: <Url>https://github.com/NAMEWTA/WTA-plus</Url>
- Captured at: 2026-09-19T08:03:45.271401Z
- REST full pagination: 3 open issues, 0 closed issues; PR excluded. Each issue has 0 comments.
- 本次冻结范围是捕获时的全部 3 条 open Issue；后续新建或修改的 Issue 不自动改变本地合同。
- 用户明确要求单一 change；当前对话拥有聚合授权，每条远程来源拥有独立 locator、正文与 digest，未把多个 locator 放入一个 source frontmatter。
- 查重：active/archive 来源快照没有命中这些 locator；capture 三行均命中并在完成冻结后消费。已存在 comprehensive-review 的状态与授权保持独立。
- 关闭状态逐条记录；pending-close 只表示将来完成后的待处理事项，不授予远程写入权。聚合 conversation source 本身无可关闭对象。

| Issue | 原始标题 | Locator | 冻结来源 | Content SHA-256 | External action |
|---|---|---|---|---|---|
| 1 | bug: 手机号应为必填项，当前仍为选填 | <Url>https://github.com/NAMEWTA/WTA-plus/issues/1</Url> | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/sources/issue-1.md</Path> | 26a9c457fab27c0985192d162488451c2d05fd9f855731d8773413f45d5d9c28 | closed |
| 2 | refactor: 完整移除 snail-ai，仅保留 cde-ai 与 cde-common-ai 占位 | <Url>https://github.com/NAMEWTA/WTA-plus/issues/2</Url> | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/sources/issue-2.md</Path> | 325836639794829b4d9164556cb6520b52a4bda9f9381f53b03d4bfeedbc19dd | closed |
| 3 | feature: 在 backend/wta-extend 新起 Go 网关 + Python 智能体独立服务，由 cde-ai / cde-common-ai 经 OpenAPI 引用 | <Url>https://github.com/NAMEWTA/WTA-plus/issues/3</Url> | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/sources/issue-3.md</Path> | d9eaf812bc332c676aeaeb367f69fa43a91800727892ea6d757a94823f01e059 | pending-close |

## Reconcile — 2026-09-19T14:25:47.627545+00:00

#1/#2已按本轮明确授权评论并以completed原因关闭，远程重读确认CLOSED且各仅1条本change完成marker。#3仍OPEN、正文/评论/updatedAt未变，责任属于独立暂缓change。完整远程回执见 evidence/reconcile.json。聚合conversation source保持不变，逐条GitHub来源冻结hash未修改。
