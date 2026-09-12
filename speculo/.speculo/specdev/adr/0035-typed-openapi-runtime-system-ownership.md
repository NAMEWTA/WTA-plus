# ADR-0035: 类型化 OpenAPI 运行时与 system 所有权

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-30-openapi-common-module/ADR.md</Path>` ADR-003

## Context

方法注解、签名、拦截和映射注册是可复用运行时能力；凭据、用户、角色、Client、权限和管理 API 则属于 system 领域。common 直接访问 system Mapper 会反转 Maven 依赖，把全部能力放入 system 又无法形成可复用模块。

## Decision

`ruoyi-common-openapi` 拥有方法级注解、协议运行时、开放接口注册表、配置和窄类型化 SPI。`ruoyi-system` 拥有凭据持久化、密钥生命周期、用户授权快照、管理 API 和调用事件实现；`ruoyi-admin` 只负责依赖装配。common 不持有 system Mapper 或业务实体，内部协作使用明确接口和直接应用服务调用，不引入通用 CommandBus、requestCode 或 MQ 分发层。

## Consequences

启用时所需 SPI 必须唯一且可用，缺失或重复实现应使装配失败。运行时、system adapter 和管理用例可以按层独立测试，新增跨层能力必须先扩展类型化合同而非泄漏持久化模型。
