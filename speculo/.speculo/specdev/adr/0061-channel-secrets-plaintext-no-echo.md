# ADR-0061: 渠道密钥跟随 OSS 的明文存储与不回显

- **Status:** Accepted
- **Date:** 2026-09-10
- **Source:** `2026-09-10-notify-channel-config` ADR-007（LOG-013）

## Context

YAML 不再保存账号密钥。本期范围是运维面，不是密钥平台。

## Decision

SMTP 密码与短信 AK/SK 库内明文存储。管理接口不回显 secret；编辑时空白表示保持原值；写操作不把请求体写入操作日志。HTTP、业务日志和异常不得包含 secret。

## Consequences

前端不得要求用户每次编辑都重填密钥。后续若做 KEK，另开 change，不在本期假装已加密。
