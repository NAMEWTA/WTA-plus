---
schema_version: 1
artifact: source
change: 2026-08-21-module-knowledge-skills
source_type: conversation
canonical_locator: null
captured_at: 2026-08-21T09:50:00+08:00
content_sha256: 71a2f8dec7f2e6b3c6744d57345363c7bd5e78a356647c29d47cecf2ce0a1610
remote_state: not-applicable
close_capability: not-applicable
---

# Source: 模块研究与 Skill 创建

## Capture Metadata

- **Capture method:** conversation
- **Author:** user
- **Created / updated:** 2026-08-21T09:23:00+08:00 / 2026-08-21T09:50:00+08:00
- **Labels or classification supplied by source:** none
- **Attachments:** conversation attached skill-creator; body not persisted (tooling attachment, URL references only: none)
- **Redactions:** omitted inlined third-party skill-creator SKILL.md dump (type: attached-skill-body); no secrets, tokens, or personal data removed from the user request

## Original Content

请你创建一个 change 激活 speculo/workflows/specdev/common/skills/research/SKILL.md 为我深度的搜索当前仓库里以下内容的情况。随后激活 /skill-creator 去创建多个 skill 以满足我的要求。该 SKILL 都创建到 .agents/skills/* 下。
具体需要探索的如下：
1、plus-ui-namewta 关于前端的代码编码规范、注释规范、动态权限路由规范等
2、ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system 这个公共服务类的整体使用方法，提供了哪些能力供其他子模块调用使用
3、ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow 这个公共服务类的整体使用方法，提供了哪些能力供其他子模块调用使用
4、ruoyi-vue-plus-namewta/ruoyi-common 这里包含了哪些模块，分别都是什么作用？有哪些工具类？

注意，这些 SKILL 都需要强调一点，针对每个模块的具体描述，如不明确的，可直接根据引用读取代码仓库的对应代码。

## Source Comments

无。
