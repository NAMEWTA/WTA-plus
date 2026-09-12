---
schema_version: 1
artifact: source
change: 2026-09-12-richtext-list-automapper
source_type: conversation
canonical_locator: null
captured_at: 2026-09-12T00:35:08+08:00
content_sha256: 55abde801e240d8e62219fb5822db35ac2af0a0de8e05e2f4922531264f6972b
remote_state: not-applicable
close_capability: not-applicable
---

# Source: 富文本列表 MapStruct 转换失败 follow-up

## Capture Metadata

- **Capture method:** conversation + e2e
- **Author:** user / Lead
- **Created / updated:** 2026-09-12T00:35:08+08:00
- **Labels or classification supplied by source:** follow-up of archived `2026-09-08-richtext-third-oss`; activate diagnose then tickets/implement
- **Attachments:** `temp/team/lead/e2e/richtext-41-backend-error.txt`, screenshots richtext-40/41
- **Redactions:** none

## Original Content

富文本演示页今日保存操作报错（错误编号 85384381）。E2E 复现：POST /demo/rich-text/create 成功写入；随后 GET /demo/rich-text/list 返回应用码 500。根因：ConvertException cannot find converter from TestRichText to TestRichTextSummaryVo；TestRichTextSummaryVo 缺少 @AutoMapper(target=TestRichText.class)。

本 change 为已归档 2026-09-08-richtext-third-oss 的 follow-up（supersedes 链），不 reopen 归档。请激活 D-diagnose-bugs 固化诊断，再切片修复 list VO 映射与回归测。

## Source Comments

- Supersedes / related archive: `speculo/.speculo/specdev/archive/2026-09/2026-09-08-richtext-third-oss/`
- Parallel active `2026-09-10-notify-channel-config` unchanged.
- Lead decision: do not unarchive prior change (A-archive contract).
