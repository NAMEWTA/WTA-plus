# 课程设计：wta-third 第三方 HTTP 切片

## 目标与期望效果

学完后能口述 Provider / Endpoint / Credential / Observability 四条管理切片，以及出站 `ThirdPartyGateway` / SPI 适配器路径上每个函数做什么。字段密钥不得在 Lesson 里编造真实 secret。

## 学习者与表达/深度配置

| 字段 | 值 |
| --- | --- |
| 交互语言 | `zh-CN` |
| expression_level | `eli5` |
| coverage_depth | `deep` |
| Lesson 时长 | `35` 分钟 |

## 目标合同

| 类型 | 路径 |
| --- | --- |
| `ThirdProviderController` + `ThirdProviderUseCase`/`Impl` | admin `/third/provider` |
| `ThirdEndpointController` + `ThirdEndpointUseCase`/`Impl` | admin `/third/endpoint` |
| `ThirdCredentialController` + `ThirdCredentialUseCase`/`Impl` | admin `/third/credential` |
| `ThirdObservabilityController` + `ThirdObservabilityUseCase`/`Impl` | admin `/third/invocation|statistics` |
| 出站网关（同模块路径，非 Controller） | `ThirdGatewayAdapter` implements `ThirdPartyGateway` |

| ID | 可观察目标 | 关键性 | 前置 | Lesson |
| --- | --- | --- | --- | --- |
| OBJ-01 | 口述 Provider 列表/详情/保存/状态/删除：Controller → UseCaseImpl → `ThirdProviderService` → DAO/Mapper，以及路径安全校验 | 是 | none | L-001 |
| OBJ-02 | 口述 Endpoint 切片同样五层，并说明 endpoint 如何被网关选中 | 是 | OBJ-01 | L-002 |
| OBJ-03 | 口述 Credential 切片：加密适配器、列表不回显明文、删除 | 是 | OBJ-01 | L-003 |
| OBJ-04 | 口述 invocation/statistics 查询：`ThirdObservabilityUseCaseImpl` → `ThirdObservabilityService` → invocation/statistic DAO | 是 | OBJ-02 | L-004 |
| OBJ-05 | 口述一次出站调用：`ThirdPartyGateway` → `ThirdGatewayAdapter` → 限流/凭证/HTTP/SPI `ThirdProviderAdapter` → 记录 | 是 | OBJ-02, OBJ-03 | L-005 |

## 课程地图

L-001 Provider → L-002 Endpoint → L-003 Credential → L-004 Observability → L-005 Gateway/SPI。

## 成功证据与范围外

成功：能指出真实类名与方法名。范围外：具体供应商 HTTP 报文私有协议、Nacos、OSS。

## Revision 记录

| 时间 | 变化 | 原因 |
| --- | --- | --- |
| 2026-09-14T07:42:38.672Z | 初版五切片 | 从架构课拆出 third 深课 |
