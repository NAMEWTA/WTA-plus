# 工作流领域索引

## Scope
`@namewta/domain-workflow`，对应后端 `wta-workflow`。

## Purpose
提供流程分类、定义、任务、实例、审批、参与人、请假和附件的终端无关合同与状态规则。

## Components
资源位于 `src/category/`、`src/definition/`、`src/task/`、`src/instance/`、`src/leave/` 等目录。

## Entry Points
公开资源子路径见 [package.json](package.json)，根入口只聚合稳定模块。

## Dependencies
依赖 API、system domain 和 platform 合同；不拥有 Vue、设计器宿主、路由或上传副作用。

## Verification
`pnpm --filter @namewta/domain-workflow lint`、`typecheck`、`test`。

## Read Next
页面实现读取 [web-domain-workflow](../../web-domains/workflow/AGENTS.md)。
