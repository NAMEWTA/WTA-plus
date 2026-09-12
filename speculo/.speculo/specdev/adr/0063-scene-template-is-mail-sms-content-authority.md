# ADR-0063: MAIL/SMS 发送内容以场景模板为权威

- **Status:** Accepted
- **Date:** 2026-09-10
- **Source:** `2026-09-10-notify-channel-config` ADR-010（LOG-019）

## Context

若配置页改了邮件正文或短信模板码，却仍发出调用方硬编码字符串，配置页没有意义。

## Decision

MAIL/SMS 发送时，控制面按逻辑场景读取已配置模板，用调用方 `templateParams` 渲染邮件或绑定短信供应商模板。渲染结果写入 intent 快照供审计。调用方 `title`/`content` 不是发送权威。IN_APP 本期仍用代码内文案。

## Consequences

验证码等调用方应只传变量。Dispatch 对 SMS 使用模板绑定，对 MAIL 使用渲染后的主题/正文。
