# ADR-0064: 验证码补 templateCode；SYNC 不是同步发送

- **Status:** Accepted
- **Date:** 2026-09-10
- **Source:** `2026-09-10-notify-channel-config` ADR-011（LOG-022）

## Context

Captcha 提交空 `templateCode` 会被控制面拒绝。`NotificationMode.SYNC` 只入队，请求线程不调用供应商。

## Decision

给验证码补上稳定 `templateCode`（与 `auth-captcha` 场景一致），使其走模板与路由。不把 SYNC 改成请求线程内调用供应商；真正同步发送另开 change。

## Consequences

验证码接口在 submit 成功后仍可能尚未真正送达供应商。文档不得把 SYNC 字段解释为已同步发送。
