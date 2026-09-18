# 来源：wta-third

访问日期：2026-09-14。

| Source ID | 定位 |
| --- | --- |
| S-3RD-01 | `controller/admin/ThirdProviderController.java` |
| S-3RD-02 | `controller/admin/ThirdEndpointController.java` |
| S-3RD-03 | `controller/admin/ThirdCredentialController.java` |
| S-3RD-04 | `controller/admin/ThirdObservabilityController.java` |
| S-3RD-05 | `usecase/impl/Third*UseCaseImpl.java` 与对应接口 |
| S-3RD-06 | `service/Third*Service.java`、`dao/`、`mapper/` |
| S-3RD-07 | `adapter/gateway/ThirdGatewayAdapter.java`、`wta-api/.../ThirdPartyGateway.java` |
| S-3RD-08 | `spi/ThirdProviderAdapter*.java`、`adapter/security|resilience|observability|store/` |
| S-3RD-09 | `http/ThirdHttpClientFactory.java`、`support/ThirdEndpointSecurity.java` |
| S-3RD-10 | 登记表：wta-third 模块地图（layered 未在 03 表逐行点名时以源码五层为准） |

冲突：若 Skill 说 third 是 classic，以工作树 Controller→UseCase→Service→DAO 为准。
