# 档案领域索引

## Scope
`@namewta/domain-profile`，对应后端 `wta-profile`。

## Purpose
提供材料标签、个人档案和企业档案的终端无关合同、服务与安全映射。

## Components
资源位于 `src/material-tags/`、`src/person/`、`src/enterprise/`；根入口聚合兼容 facade。

## Entry Points
公开资源子路径见 [package.json](package.json)，新代码优先使用 `./person/*`、`./enterprise/*`。

当前 OpenAPI 快照已含 `/profile/**`，`current.json` 仅是版本指针。企业转移资源在 service 边界使用 generated transport 并映射自有状态；其他资源按受影响范围核对源码合同。禁止页面直接依赖 generated 文件，禁止手改生成类型。后端与 Web owner 映射见 [Profile 模块索引](../../../../.agents/skills/wta-module-guide/references/modules/profile/index.md)。

## Dependencies
依赖 platform 合同和 api-contracts 生成传输类型；不依赖 Vue、DOM、浏览器存储或具体请求实现，后端负责最终授权。

## Verification
`pnpm --filter @namewta/domain-profile lint`、`typecheck`、`test`。

## Read Next
页面实现读取 [web-domain-profile](../../web-domains/profile/AGENTS.md)。
