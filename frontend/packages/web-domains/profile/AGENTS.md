# 档案管理 Web 领域索引

## Scope
`@namewta/web-domain-profile`。

## Purpose
提供管理端材料标签、个人档案和企业档案页面，以及用户端档案中心和个人/企业认证页面及 manifest contribution。

## Components
页面与逻辑按 `src/material-tag/`、`src/person/`、`src/enterprise/`、`src/self/` 聚合，runtime 在 `src/runtime.ts` 和 `src/self/runtime.ts`。

## Entry Points
公开 `./material-tag`、`./person`、`./enterprise`、`./self` 和根入口，见 [package.json](package.json)。

`material-tag` 是 `/profile/material-tags` 的页面 owner 别名，组件键保持 `profile/materialTag/index`；无页面的匿名回调不创建 Web 资源。

## Dependencies
依赖 `domain-profile`、platform、Vue 和 Router；App 显式注入服务、权限、反馈、上传和工作流端口。

## Verification
`pnpm --filter @namewta/web-domain-profile lint`、`typecheck`、`test`、`build`。

## Read Next
领域合同读取 [domain-profile](../../domains/profile/AGENTS.md)。
