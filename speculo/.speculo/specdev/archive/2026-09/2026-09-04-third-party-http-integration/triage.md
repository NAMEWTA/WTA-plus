---
schema_version: 1
artifact: triage
change: 2026-09-04-third-party-http-integration
mode: intake
source: <Path>{roots.state}/specdev/changes/2026-09-04-third-party-http-integration/source.md</Path>
classification: investigation
risk: high
route: specdev/grill-with-docs
ready_for_implementation: false
external_action: not-applicable
updated_at: 2026-09-04T00:00:00+08:00
---

# Triage: 统一第三方 HTTP 供应商与 URI 接入能力

## 当前判定

- **影响：** 新增跨模块公共外部调用能力，涉及供应商和接口生命周期、凭据/加密、安全审计、数据库模型、HTTP 公共 API、容错与观测；会影响 `ruoyi-admin` 装配和未来多个业务模块。
- **紧急度：** normal
- **当前证据：** `cde-third-universal` 只有单表 `third_api_config`、天眼查特化管理入口和 `@ThirdPartyApiUsage` 异步计数；没有供应商实体、URI 实体、HTTP 执行器或认证加密端口。工程后端为 Spring Boot 4.1.0 / Java 21。
- **相关代码/工件：** `D:/Document/code/cde-standard/cde-base/cde-modules/cde-third`；`<Path>ruoyi-vue-plus-namewta/pom.xml</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>`。

## 未知项

- **可发现事实：** Spring Boot 4.1 与 Spring Framework 7 的 HTTP Service Clients、`RestClient`、Spring Cloud/OpenFeign 和 Resilience4j 的当前兼容性；现有 POM、配置和模块装配边界。
- **需要用户决定：** 目标仓库/模块 owner；声明式 `@HttpExchange` 与数据库驱动动态 URI 的边界；配置和密钥存储；同步/异步及重试/熔断默认合同；首期是否直接实现企查查。
- **低影响实现细节：** DTO 的具体命名、Mapper 查询写法、缓存 key、HTTP 底层 request factory 和测试替身，由后续规格/Ticket 按已确认合同决定。

## 路由

- **下一 Work：** `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>`
- **理由：** 这是高影响架构与公共调用合同问题，必须先完成设计访谈；当前不具备实现授权。

## 外部动作

- **远程目标：** 无
- **关闭能力：** not-applicable
- **当前状态：** not-applicable
- **授权记录：** 无
- **尝试与结果：** 无
