# ADR-0057: 数据库是邮件短信账号的唯一运行时权威

- **Status:** Accepted
- **Date:** 2026-09-10
- **Source:** `2026-09-10-notify-channel-config` ADR-003（LOG-004 / LOG-009 / LOG-010）

## Context

`mail.*` 与 `sms.blends` 曾写在 YAML。完成后不得再把 YAML 当发件人/供应商并行权威。

## Decision

邮件与短信账号的运行时权威是数据库，经缓存或热刷新生效。YAML 删除发件人与供应商账号内容。占位密钥不得自动启用。

## Consequences

Adapter 必须能按控制面解析出的账号发送。SMS4J 不以 yaml blends 为账号源。非账号基础设施可留 YAML。Skill 与注释须与代码一致。
