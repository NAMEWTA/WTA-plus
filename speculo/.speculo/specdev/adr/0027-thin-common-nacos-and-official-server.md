# ADR-0027: 薄 common Nacos 客户端与官方服务端

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>` ADR-002

## Context

动态配置需要跨应用复用客户端装配和安全刷新能力，但把它放入业务模块会使应用级配置引导依赖具体领域；在仓库内嵌或重打 Nacos Server 又会扩大服务端构建和安全维护责任。

## Decision

`ruoyi-common-nacos` 是按需依赖的薄 common artifact，只封装 Nacos Config 客户端、稀疏覆盖、校验和刷新能力，并纳入 common reactor 与 BOM。Nacos Server 使用固定版本的官方 `nacos/nacos-server` 镜像作为外部基础设施；本仓库不内嵌、不重打服务端，也不把服务注册发现纳入该模块。

## Consequences

组装应用必须显式依赖并启用客户端，业务模块不拥有配置引导。服务端版本、鉴权、持久化和健康合同由发布资产管理，客户端与服务端兼容性需要作为独立门禁持续验证。
