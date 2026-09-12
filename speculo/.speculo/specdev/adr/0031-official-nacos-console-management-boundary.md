# ADR-0031: 官方 Nacos 控制台管理边界

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>` ADR-007
- **Source:** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-01-admin-runtime-capability-reconciliation/ADR.md</Path>` ADR-003

## Context

Nacos 官方控制台已经提供 namespace、历史、回滚和权限能力。再建设 RuoYi CRUD 会形成第二套长期维护面和双重授权语义，而现有管理端已经具备 external iframe 与同源反代模式。

## Decision

当前入口固定为“系统监控 > Nacos配置中心”，通过通用 external iframe 和生产 `/nacos/` 同源反代打开官方控制台。RuoYi 动态菜单权限只控制入口可见性；用户在 iframe 中使用 Nacos 自身账号认证。本项目不实现 `/system/nacos` CRUD、自有编辑器或 Nacos SSO。

## Consequences

菜单权限与配置权限由两个系统分别负责，RuoYi 不得向浏览器注入 Nacos 密码、token 或 identity。远程内容可能先被官方控制台接受，因此应用消费端仍必须独立执行保护键、类型和跨字段校验。
