# ADR-0065: 渠道账号与短信模板配置每分钟发送配额

- **Status:** Accepted
- **Date:** 2026-09-10
- **Source:** `2026-09-10-notify-channel-config` ADR-012（LOG-023；取代 change ADR-009「频控不进配置页」）

## Context

YAML `minute-max` 是单手机号防刷，不是厂商账号吞吐。多账号不能共用一个全局计数器。

## Decision

每个邮件/短信渠道账号有每分钟发送上限；短信逻辑场景绑定可设置更细的每分钟条数，且不得超过所属账号上限。YAML 仍删除账号段；SMS4J 线程池仍可留在 YAML。

## Consequences

控制面在调用供应商前检查配额；超限不得改选其他账号。Skill 须区分账号吞吐与号码防刷。收件人拦截见 ADR-0066。
