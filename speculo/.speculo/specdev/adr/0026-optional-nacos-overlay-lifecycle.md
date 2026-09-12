# ADR-0026: 可选 Nacos 稀疏覆盖生命周期

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>` ADR-001、ADR-009、ADR-012

## Context

应用需要按需接收远程配置，同时必须在未部署 Nacos、远程文档为空或 Nacos 故障时依靠本地 `application*.yml` 独立启动。若远程配置成为完整事实源，或离线重启读取持久快照，运行结果会隐含依赖不可用或已过期的远程状态。

## Decision

`nacos.config.enabled` 默认关闭，并由部署显式启用。Nacos 只提供本地完整 YAML 之上的稀疏覆盖：远程未出现或被删除的键回退本地值。运行中失联保留进程内上一有效覆盖；失联后重启不读取远程持久快照，只使用本地基线。启动期非法文档被忽略，监听期非法版本整份原子拒绝并保留上一有效覆盖。

## Consequences

本地 YAML 必须始终完整，关闭 Nacos 时不得创建客户端、监听器或连接告警。网络抖动不会立即改变运行配置，但失联期间重启实例与未重启实例可能暂时不同；运维必须修正或回滚非法远程版本后再观察收敛。
