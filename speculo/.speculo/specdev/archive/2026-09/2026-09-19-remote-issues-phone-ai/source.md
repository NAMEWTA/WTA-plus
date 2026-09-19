---
schema_version: 1
artifact: source
change: 2026-09-19-remote-issues-phone-ai
source_type: conversation
canonical_locator: null
captured_at: 2026-09-19T08:03:45.271401Z
content_sha256: 739b5c8031c41a11549a51d17847f2978a73339c94fda76ccdbe902954cb4ce0
remote_state: not-applicable
close_capability: not-applicable
---

# Source: 集中摄入 remote issue，并串联 Triage / G / S / T / P

## Capture Metadata

- **Capture method:** conversation
- **Author:** current user
- **Created / updated:** current session
- **Labels or classification supplied by source:** none
- **Attachments:** none
- **Redactions:** 将用户原文中的机器绝对路径替换为已解析的 Work Path 引用；其余文字不改写。
- **Scope provenance:** 当前对话为聚合授权来源；三条 GitHub 来源分别冻结，见 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/issue-index.md</Path>。
- **Digest contract:** SHA-256 of UTF-8 text from the literal `## Original Content` heading through EOF, including final newline.

## Original Content

激活 <Path>{roots.workflows}/specdev/T-triage/T-triage.md</Path> 获取当前仓库的 remote 的 issue有哪些，这个获取下来之后，新建又给change，都在这个change里进行解决。先激活 <Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path> 如有问题则根据该work进行提问。完成后紧接着就是 <Path>{roots.workflows}/specdev/S-spec/S-spec.md</Path> <Path>{roots.workflows}/specdev/T-tickets/T-tickets.md</Path> <Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>

## Source Comments

无
