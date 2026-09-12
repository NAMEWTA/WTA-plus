# ADR-0028: 部署优先级与 Nacos 引导键保护

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>` ADR-004

## Context

发布环境通过命令行和环境变量注入数据库、监控及 secret 等部署值。若 Nacos 位于最高优先级，远程文档可以绕过容器约束，甚至改写自身连接或 active profile，造成递归失联和跨环境配置漂移。

## Decision

配置优先级固定为命令行参数和环境变量高于 Nacos 稀疏覆盖，Nacos 高于 profile YAML 和基础 YAML。`nacos.config.*`、`spring.profiles.*` 及规格列明的其他引导键禁止出现在远程覆盖层；启动加载、监听更新和管理入口必须复用同一保护键校验合同。

## Consequences

远程配置不能覆盖部署层强制值或改变自身配置单元。普通文件键仍可动态覆盖；包含保护键的版本必须被拒绝，且拒绝不得替换上一有效运行配置。
