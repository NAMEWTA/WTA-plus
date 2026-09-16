# 总体背景：wta-third

## 为什么这个主题重要

业务模块不许自己拼 RestTemplate 打第三方。统一走 `ThirdPartyGateway`：供应商、Endpoint、凭证、限流、观测集中在 `wta-third`。抄近路会把密钥和 URL 散落到各个业务包。

## 宏观地图

```text
管理端 CRUD
  Third*Controller → Third*UseCaseImpl → Third*Service → Third*Dao → Mapper

出站
  业务模块 → ThirdPartyGateway (wta-api)
           → ThirdGatewayAdapter
           → snapshot/cache, crypto, resilience, SPI adapter, HTTP
           → ThirdInvocationRecorderAdapter
```

## 核心概念与关系

| 中文 | English |
| --- | --- |
| 供应商 | Provider |
| 调用点 | Endpoint |
| 凭证 | Credential（落库加密，接口不回显明文） |
| 适配器 SPI | `ThirdProviderAdapter` |
| 观测 | Invocation / Statistic |

## 先决知识与缺口

需要 layered 五层（架构 L-003）。不要求已掌握各供应商 API。

## 术语表

`ThirdConfigSnapshot` 是运行时配置快照。`ThirdLimitLease` 是限流租约。

## 来源与不确定性

以 `wta-third` 与 `wta-api` 的 `ThirdPartyGateway` 源码为准。
