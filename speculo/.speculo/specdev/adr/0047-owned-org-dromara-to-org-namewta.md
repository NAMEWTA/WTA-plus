# ADR-0047: 自有 org.dromara → org.namewta

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-002（LOG-020；废止 `org.wta`）

## Context

自有 Maven group 与 Java 基包曾用 `org.dromara`；短暂采纳 `org.wta` 与 `@namewta`/`namewta-*` 分裂。

## Decision

自有 Maven `groupId` 与 Java 基包统一为 **`org.namewta`**。不采用 `com.wta`；**不再采用** `org.wta`。第三方 `org.dromara.sms4j|warm|easyes|mica.mqtt` **MUST NOT** 变为 `org.namewta.*`。

## Consequences

`mapperPackage` / `typeAliasesPackage` / springdoc / `AutoConfiguration.imports` 同步到 `org.namewta.*`；artifactId 仍 `ruoyi-X`→`wta-X`（ADR-0050）。禁止「替换所有 org.dromara」。
