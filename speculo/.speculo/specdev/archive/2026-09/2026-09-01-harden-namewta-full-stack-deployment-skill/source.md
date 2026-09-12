---
schema_version: 1
artifact: source
change: 2026-09-01-harden-namewta-full-stack-deployment-skill
source_type: conversation
canonical_locator: null
captured_at: 2026-09-01T18:06:51+08:00
content_sha256: 89668d179234cda7773b7d74090c76866a5c11162e175262d1a6acd1f9f77fb7
remote_state: not-applicable
close_capability: not-applicable
---

# Source: 强化 NAMEWTA 全栈部署 Skill

## Capture Metadata

- **Capture method:** conversation
- **Author:** user
- **Created / updated:** 2026-09-01
- **Labels or classification supplied by source:** deployment, skill, retrospective
- **Attachments:** 当前会话中已完成部署的 Speculo Evidence
- **Redactions:** SSH、数据库、OpenAPI 和登录凭据均未持久化

## Original Content

优化 `deploy-namewta-environment`：将本次 NAMEWTA 前后端完整部署的经验、故障与恢复方式固化为可执行 Skill 合同；按已确认的四 Ticket 计划执行，不重新连接或修改部署服务器。

已确认范围包括 Compose 身份、候选一致性、逐实例滚动、业务语义、前端上下文、稳定窗口、数据库备份例外、报告和自动化测试。

## Source Comments

用户先要求只读 Plan Mode 复盘，随后明确回复“执行”。
