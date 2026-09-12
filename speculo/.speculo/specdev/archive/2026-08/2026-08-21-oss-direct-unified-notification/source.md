---
schema_version: 1
artifact: source
change: 2026-08-21-oss-direct-unified-notification
source_type: conversation
canonical_locator: null
captured_at: 2026-08-23T13:22:35+08:00
content_sha256: e9cbcf42e146eadfc47472a54ff143d470ec4bf0b4812054c4e900d02b139b5b
remote_state: not-applicable
close_capability: not-applicable
---

# Source: OSS 浏览器直传与统一对外通知

## Capture Metadata

- **Capture method:** conversation，归档前补录本地来源链
- **Author:** user
- **Created / updated:** 2026-08-21 / 2026-08-23T13:22:35+08:00
- **Labels or classification supplied by source:** AR-001、基座系统、无历史兼容负担
- **Attachments:** 项目内 Speculo workflow/change 路径；无外部 URL
- **Redactions:** 将机器绝对路径规范化为 Speculo 路径标签；未省略产品决定或授权边界

## Original Content

1. 指定 `<Path>{roots.workflows}/specdev/R-review-architecture/R-review-architecture.md</Path>` 审查 `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification</Path>`，选择 AR-001。
2. Q1 A，Q2 A，并确认“当前是基座系统，没有任何的历史负担，所以无需考虑兼容问题”；Q3 A，Q4 A，Q5 A；随后确认共识。
3. 指定 `<Path>{roots.workflows}/specdev/T-tickets/T-tickets.md</Path>` 为该 change 创建并确认新 Tickets。
4. 指定 `<Path>{roots.workflows}/specdev/I-implement/I-implement.md</Path>` 完成新 Tickets 与整体实现。
5. 指定 `<Path>{roots.workflows}/specdev/A-archive-and-consolidate/A-archive-and-consolidate.md</Path>` 归档该 change，并确认执行；该 change 无远程 Issue/PR 需要关闭。

## Source Comments

原始 change 在未建立 source/triage 工件时直接由本地对话启动。本文件只补录可验证的来源、设计确认和执行授权；完整逐轮决定保留在 change `LOG.md`，不补造远程来源。
