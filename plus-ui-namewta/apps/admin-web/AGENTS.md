# Admin Web 索引

## Scope
当前已激活的后台管理浏览器 App。

## Purpose
拥有 ClientContext、启动流程、布局、品牌、路由、Store、宿主适配和部署配置。

## Components
`src/application/` 组合服务与宿主；`src/router/` 动态清单；`src/store/` App 状态；`src/views/` 自有页面。

## Entry Points
入口 [src/main.ts](src/main.ts)；服务组合 [src/application/services.ts](src/application/services.ts)；清单 [src/router/adminManifestRegistry.ts](src/router/adminManifestRegistry.ts)；导航恢复 [src/permission.ts](src/permission.ts)。

## Dependencies
显式选择各 domain/web-domain 和浏览器 adapters；不向其他 App、包内部路径或 Taro 导入。

## Verification
`pnpm --filter @namewta/admin-web lint`、`typecheck`、`test`、`build`。

## Read Next
页面能力读取 `packages/web-domains/*/AGENTS.md`，领域服务读取 `packages/domains/*/AGENTS.md`。
