# ADR-0068: 公告与工作流使用包装模板

- **Status:** Accepted
- **Date:** 2026-09-10
- **Source:** `2026-09-10-notify-channel-config` ADR-016（LOG-029）

## Context

公告标题正文来自公告编辑器，工作流 subject/message 来自流程运行时。若绕过模板直发会留下第二套正文权威。

## Decision

`notice-published` 与 `workflow-task` 的 MAIL/SMS 使用包装模板。配置页编辑外壳文案，必须保留代码声明的变量（至少 `${title}`、`${content}`，以及已声明的 `${path}` 等）。运行时由公告快照或流程参数填入变量值。调用方只传变量。

## Consequences

播种这两类场景时必须带包装模板与必填变量。保存配置时若删掉必填 token 则拒绝。短信侧把 title/content 映射到供应商模板参数。
