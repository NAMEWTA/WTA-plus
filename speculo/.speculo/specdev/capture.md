---
schema_version: 1
artifact: capture-index
mode: capture
repo: NAMEWTA/WTA-plus
updated_at: 2026-09-17T11:29:56Z
---

# Capture

## 捕获计划

- **目标 repo：** `NAMEWTA/WTA-plus`
- **origin：** local
- **排除 id：** 无
- **确认记录：** 用户要求把对话中的两条记事项发布到本仓库 GitHub Issue；随后更正 #2：不用 snail-ai，完整移除，仅保留 cde-ai / cde-common-ai 占位。本次：用户要求把「在 backend/wta-extend 新起 Go 网关 + Python 智能体独立服务，由 cde-ai / cde-common-ai 经 OpenAPI 引用」记为仍 open 的 inbox Issue；具体方案与管理端归属以后再说

capture 不创建 Change，不写 `current_work`，不关闭 Issue。GitHub 是 inbox，不是开发权威。

## 账本

| id | kind | title | labels | number | url | marker | sha256 | state |
|---|---|---|---|---|---|---|---|---|
| 2026-09-17-phone-number-required | bug | bug: 手机号应为必填项，当前仍为选填 | bug, specdev:captured, origin:local | 1 | https://github.com/NAMEWTA/WTA-plus/issues/1 | specdev:capture:2026-09-17-phone-number-required | a849e2c2ece87cdd58a6563fedd2006118260cbaacedd428cedef434b8d7f000 | open |
| 2026-09-17-ai-module-placeholder-cleanup | refactor | refactor: 完整移除 snail-ai，仅保留 cde-ai 与 cde-common-ai 占位 | enhancement, specdev:captured, origin:local | 2 | https://github.com/NAMEWTA/WTA-plus/issues/2 | specdev:capture:2026-09-17-ai-module-placeholder-cleanup | 4e61bd75492fd8d1ea9529b128ca7e99a1e2bcafcb2875b09e0efd61ee67800b | open |
| 2026-09-17-wta-extend-go-python-ai | feature | feature: 在 backend/wta-extend 新起 Go 网关 + Python 智能体独立服务，由 cde-ai / cde-common-ai 经 OpenAPI 引用 | enhancement, specdev:captured, origin:local | 3 | https://github.com/NAMEWTA/WTA-plus/issues/3 | specdev:capture:2026-09-17-wta-extend-go-python-ai | 51fc33f5de239086db6c64e99ff787f4405899e1e65ae60cbc55f25e189f3599 | open |

`state`：`planned | open | skipped:duplicate | intaken | waived | failed`。

`sha256` 覆盖实际发出的 Issue 正文。漂移只警告，不自动重发。

## 计数

- **inbox_open：** 3
- **inbox_intaken：** 0
- **inbox_waived：** 0
- **skipped：** 0
- **failed：** 0

计数权威是本文件，不是 GitHub 搜索。`inbox_open` 不计 `published_issues`。

## 重试

无
