# 工作流 Web 领域索引

## Scope
`@namewta/web-domain-workflow`。

## Purpose
提供流程分类、定义、设计、任务、实例、审批、请假、参与人和附件页面。

## Components
页面资源位于 `src/category/`、`src/definition/`、`src/task/`、`src/instance/`、`src/leave/`；共享组件位于 `src/components/`。

## Entry Points
公开资源子路径和根入口见 [package.json](package.json)。

## Dependencies
依赖 `domain-workflow`、platform、Vue、Router；宿主注入设计器、上传下载、导航和反馈能力。

## Verification
`pnpm --filter @namewta/web-domain-workflow lint`、`typecheck`、`test`、`build`。

## Read Next
领域合同读取 [domain-workflow](../../domains/workflow/AGENTS.md)。
