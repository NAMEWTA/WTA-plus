# 平台基础合同

`@namewta/platform-contracts` 保存少量、稳定、跨领域且终端无关的基础类型与端口。

这里不是公共业务垃圾桶：领域模型应留在所属 domain，浏览器实现应放入 adapters，Vue 类型应留在 Web 层。新增合同前必须证明至少存在多个真实消费者且所有权无法归入单一领域。

本包不得依赖 App、domain、web-domain、具体适配器或 DOM；公共 API 变化必须由类型和消费者测试共同验证。
