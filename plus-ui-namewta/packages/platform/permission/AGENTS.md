# 权限平台合同索引

## Scope
`@namewta/platform-permission`。

## Purpose
提供角色、权限标识和访问评估合同，只控制前端可见性和可操作性。

## Components
合同与边界测试位于 `src/index.ts`、`src/index.test.ts`。

## Entry Points
仅公开包根入口，见 [package.json](package.json)。

## Dependencies
不依赖 Vue 指令、App Store、后端实现或浏览器全局；后端仍是最终授权者。

## Verification
`pnpm --filter @namewta/platform-permission lint`、`typecheck`、`test`。

## Read Next
Vue 指令宿主读取 [web-kit-permission](../../web-kit/permission/AGENTS.md)。
