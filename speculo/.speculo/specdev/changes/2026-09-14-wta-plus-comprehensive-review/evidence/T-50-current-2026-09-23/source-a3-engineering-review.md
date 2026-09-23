# T-50 A3 固定差异工程复审（静态）

固定输入：A2 `1dfdcbca831ad7391d725d7f001acb2b9ba1c88f` → A3 `0b33ff361820249f947a2767674356dabab59774`，A3 tree `f13b1dce91dd61a46115dd6acc3ca246dc87cc3f`；A2 是 A3 祖先。仅两文件 diff（6+/2-），`git diff --check` exit 0。本审查只读 Git 对象，没有运行构建、Maven、HTTP 或服务，没有改仓库；A3 默认/full 和 live capture 当时尚在执行。

**静态结论：A3 最小修复方向正确，无新增静态 blocker；实际 API 合同尚须 A3 full JAR 的 live `/v3/api-docs` 原文证实。** A2 的独立源审查与八类真实 135/0/0/0 只证明 A2，不冒充 A3。

`NotificationCommand.java:3,23,29-42` 仅在现有 primitive `priority` record component 添加 `@JsonProperty(required = true)` 并在 Javadoc 明示 HTTP JSON 必须传 0；record 字段、构造器、策略/模式 null 归一化及公共方法签名不变，未添加 Swagger 依赖或全局 ObjectMapper 配置。当前 `wta-api` 已通过 `wta-common-json` 使用 Jackson 注解；本地 Jackson3 introspector 读取该 required 标记，Swagger `ModelResolver` 的 `propDef.isRequired()` 加入 schema required。注解的默认空 `value` 不改变 JSON 字段名。A2 已实证省略 `priority` 返回 400、显式 0 成功，故此改动将现有绑定事实向 OpenAPI 模型显式化，未设计新的默认或放宽错误路径。

`NotificationModeOpenApiTest.java:19-27` 直接对实际 `NotificationCommand` 使用 `ModelConverters`，新增 `schema.getRequired()` 包含 `priority` 的断言，原 mode enum 仅 ASYNC 的断言和可选证据输出未移除。测试只覆盖模型转换器，**不能单独证明运行中 Springdoc 的最终 HTTP raw**。应以 A3 exact clean 完整 JAR 捕获 `POST /notify/notification` 指向的 `NotificationCommand.required`，确认 priority 已列入、三项 description/enum 与 A2 相同、436 paths/445 schemas 基线无丢失，再由该原文生成/检查前端合同。

待 Lead 实测：A3 默认/full 编译和测试、新模型断言、owned live capture，随后 final B 的八类真实回归、前端/core 门禁。若 A3 live raw 仍未含 required，此审查不算修复通过，须定位 Springdoc 路径而非手改生成快照。A2 raw 缺口和 A1 测试失败证据保持原样。
