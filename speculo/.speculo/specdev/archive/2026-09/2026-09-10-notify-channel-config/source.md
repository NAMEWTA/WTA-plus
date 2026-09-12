---
schema_version: 1
artifact: source
change: 2026-09-10-notify-channel-config
source_type: conversation
canonical_locator: null
captured_at: 2026-09-10T14:49:31+08:00
content_sha256: 7eb75f11621b5dd7c9d4586592e6b2d25ec877a93d86b6b77cc2700f41506986
remote_state: not-applicable
close_capability: not-applicable
---

# Source: 通知中心邮件/短信配置管理

## Capture Metadata

- **Capture method:** conversation
- **Author:** user
- **Created / updated:** 2026-09-10T14:49:31+08:00
- **Labels or classification supplied by source:** 激活 `specdev/G-grill-with-docs`；要求先深度调研再交流形成 ADR / LOG / CONTEXT
- **Attachments:** 用户引用 `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>`
- **Redactions:** none

## Original Content

我发现现在前后端还有需要可以优化的地方。当前的短信/邮件通知，发件人或者配置的供应商都是写死状态，另外SMS模板代码也是写死在代码里的，这个是十分不科学的，应该类似OSS配置那样子，在通知中心下增加针对通知的配置管理，然后里面有不同的TAB页可以切换。目前先是邮件配置 / 短信配置 两个配置页面，整体可以学习OSS配置这样。去配置/启动/关闭。针对邮件，则可以调整发送的内容。{当然，针对变量，这种则是不应该能够改变的}。先激活 G-grill-with-docs 你先进行深度的调研，结合本项目系统以及外部成熟的设计模式。再进行交流形成ADR LOG CONTEXT

## Source Comments

用户明确激活设计访谈。同会话另有活跃 change `2026-09-08-richtext-third-oss`（当前 work 为 Tickets），主题无关，本请求新建独立 change，不接管该 change。
