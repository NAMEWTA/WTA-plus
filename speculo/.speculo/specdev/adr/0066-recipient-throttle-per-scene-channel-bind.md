# ADR-0066: 收件人拦截按场景渠道绑定配置

- **Status:** Accepted
- **Date:** 2026-09-10
- **Source:** `2026-09-10-notify-channel-config` ADR-014（LOG-025 / LOG-028）

## Context

验证码与公告不能共用一套「每号每分钟 1 条」。YAML 全局 restricted / minute-max / account-max 无法表达差异。

## Decision

每个逻辑场景的 MAIL 与 SMS 渠道绑定各自拥有 `restricted`、`minute-max`、`account-max`。SMS 按手机号计数，MAIL 按邮箱计数。YAML 删除这些运行时项。账号吞吐限额（ADR-0065）仍然存在，与收件人拦截同时生效。

## Consequences

控制面在供应商调用前同时检查：账号每分钟吞吐、模板每分钟吞吐、该收件人在该模板下的分钟/日限额。
