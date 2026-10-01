# 身份与访问领域索引

## Scope
`@namewta/domain-admin`，对应后端 `wta-admin`。

## Purpose
提供 ClientContext、验证码、登录、注册、社交登录和会话相关的终端无关能力。

## Components
`src/auth/`、`src/captcha/`、密码策略和传输映射位于 `src/`。

## Entry Points
资源公开为 `./auth`、`./captcha` 和根入口，见 [package.json](package.json)。

## Dependencies
依赖 API/platform 合同，不依赖 Vue、DOM、Router、具体 Axios/Storage 或 App。

## Verification
`pnpm --filter @namewta/domain-admin lint`、`typecheck`、`test`。

## Read Next
页面实现读取 [web-domain-admin](../../web-domains/admin/AGENTS.md)。

`src/social.ts` 维护外部身份源、登录事务、补资料、当前用户绑定和全局退出 API；响应从 unknown 投影，补资料与绑定冲突不产生业务 token。token 由 App 确认当前会话后写入。
