# ADR-0032: 接受 Nacos 敏感配置明文静态存储

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>` ADR-008

## Context

部分数据库密码和第三方密钥需要由 Nacos 覆盖。Nacos AES 配置加密插件会引入服务端插件、客户端依赖和 dataId 约定，当前产品明确不承担这套复杂度。

## Decision

普通 `ruoyi-namewta.yml` 可以包含敏感配置，不引入 Nacos 配置加密插件。这些值可能以明文存在于 Nacos 的 MySQL 持久化、备份以及获授权用户可访问的控制台中。风险通过可信网络、Nacos 鉴权、最小权限、独立数据库和严格禁止日志或状态回显来降低。

## Consequences

Nacos 数据库、备份和控制台账号必须按 secret 系统管理，不能宣称敏感值已经静态加密。应用日志、异常、健康状态、RuoYi 页面、测试证据和仓库工件均不得包含真实 secret 或远程配置正文。
