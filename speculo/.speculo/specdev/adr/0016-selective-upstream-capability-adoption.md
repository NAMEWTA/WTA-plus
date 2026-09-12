# ADR-0016: 上游前端按能力选择性吸收

- **Status:** Accepted
- **Date:** 2026-08-28
- **Source:** `2026-08-25-plus-ui-multi-app-domain-architecture` ADR-007

## Context

本地前端已经形成多 App、跨终端和模块化 Domain 架构。继续以目录同构和低差异为第一目标会让上游单 App 结构反向支配本地产品边界；完全放弃上游又会失去安全修复和新能力来源。

## Decision

产品 `main` 以本地架构为权威。上游镜像用于发现修复、优化和新增能力；候选变化先映射到本地 `platform`、Domain、Web Domain、`web-kit` 或 App，再在对应边界增量实现，不要求整包合并或文件一一对应。

## Consequences

上游同步转为能力评估优先，直接合并便利性降低且需要持续维护映射。任何吸收动作必须记录来源能力、目标本地边界、保留的 Client/认证/权限/菜单不变量和验证证据。
