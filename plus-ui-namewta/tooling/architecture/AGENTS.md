# 架构检查工具索引

## Scope
`@namewta/architecture` 私有工作区工具。

## Purpose
确定性检查包类型、exports、依赖方向、终端纯度、manifest 和工作区基线。

## Components
CLI 在 `src/cli.mjs`，检查实现和测试在 `src/index.mjs`、`test/`。

## Entry Points
命令 `namewta-architecture`，公开入口见 [package.json](package.json)。

## Dependencies
只服务开发门禁，不进入产品运行时；不改写源码或清单。

## Verification
`pnpm --filter @namewta/architecture lint`、`typecheck`、`test`、`check`。

## Read Next
工作区门禁读取根 [AGENTS.md](../../AGENTS.md)。
