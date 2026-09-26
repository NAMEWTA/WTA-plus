# T-50 live OpenAPI 描述与必填性核对方案（未执行）

只读取 **A2 或后续同源完整 JAR 的 owned live capture 原始** `/v3/api-docs` `source.json`，先验证捕获驱动的 exact source/JAR、HTTP 200 原字节 SHA、完整 paths/schemas、资源清理；不要从现有已提交快照拼接或把 Java Javadoc 当生成结果。生成、更新 `frontend/packages/api-contracts` 仍由产品 writer 在捕获后进行。

现有已提交 live 快照 `5f2afc58f2a31e4c172fbf12076d43f1aba8fbfbba6e71079701bc335c976302/source.json` 显示 `POST /notify/notification` 的 JSON request body `$ref` 为 `#/components/schemas/NotificationCommand`。旧 schema 中 strategy/mode/priority 的 description 分别是“编排策略”/“执行模式”/“优先级”，与当时 `NotificationCommand` 的 `@param` Javadoc 一致。`backend/pom.xml:183-187,517-522` 有 Therapi runtime Javadoc 与 annotation processor，表明本项目存在把 Javadoc 导入运行时的机制；**但是否把本次新增文字导出，必须以新 live source 逐字段核实**，不能只看源码或依赖推断。

对新 raw `source.json` 做最小只读断言：

1. 找到 `paths['/notify/notification'].post.requestBody.content['application/json'].schema.$ref`，解析到真实组件，不能仅凭猜测 schema 名称；组件字段 strategy、mode、priority 均存在。
2. `strategy.enum` 保留 `ALL`、`ORDERED_FALLBACK`、`ESCALATION` 供历史模型读取，而 `strategy.description` 明确新提交仅支持 ALL；`mode.enum` 只有 ASYNC，description 明确只支持 ASYNC；`priority.type` 为 integer，description 明确仅支持 0/不提供排序能力。若新增 Javadoc 文句未出现在 live schema，记录实际 raw 字段（只截取这三个非敏感描述）与后端/JAR 来源，按需要在本票写集内显式增加 `@Schema` 文档并重新 full capture；**不能声称 Javadoc 已同步到 OpenAPI**。
3. A2 的真实 HTTP 已证明 **省略 priority 返回 400**，而旧快照的 `NotificationCommand` 没有 `required` 列表。新 raw schema 应明确 `priority` 必填（或实现真正可省略并默认 0，且新增正负 HTTP 测试）；仅在 description 写“默认 0”却仍允许生成客户端省略会与运行时冲突。检查 `required`、`default` 与生成 TypeScript 的可选性，不能只比 enum。strategy/mode 在 A2 可省并归一为 ALL/ASYNC，可选性应反映此事实。
4. 独立保留新 raw 的全量 path/schema 数及 SHA；若存在无关新增路径或 schema，按 live 原样审查，缺失则失败，不用旧快照补齐。`openapi:fetch`/`generate` 后检查 revision/provenance 记录真实后端 commit，`openapi:check` 再从不可变快照核生成物。

若 live raw 证实 `priority` 未列入 `NotificationCommand.required`，最小候选是给 `NotificationCommand` record component 增加现有 `com.fasterxml.jackson.annotation.JsonProperty(required = true)`，不引入 Swagger 注解到 wta-api。固定项目依赖为 `wta-api → wta-common-json → spring-boot-starter-jackson`；本地 Jackson annotations 2.21 `JsonProperty` 允许 FIELD/METHOD/PARAMETER，Jackson databind 3.1.4 `JacksonAnnotationIntrospector.hasRequiredMarker` 识别 `required()`；swagger-core-jakarta 2.2.47 的 `ModelResolver` 对 `propDef.isRequired()` 调用 `addRequiredItem`。Jackson3 现有 `FAIL_ON_NULL_FOR_PRIMITIVES(true)` 已使缺失 int 返回 400，预计该注解只将已观察到的必填事实写进 schema。**这是源码/JAR推断，不是已经验证的实际 Springdoc 输出。** 需在既有 `NotificationModeOpenApiTest` 断言 `schema.getRequired()` 含 `priority`，继续保留真实 HTTP 显式 0 成功／省略字段 400，再以新完整 JAR 重捕 live raw 确认。不要只改已生成快照或为此新增 wta-api Swagger 依赖。

这是一个**待捕获核对方案**。当前未启动服务、未读取未来 raw，也未断言新增 Javadoc 一定进入 schema。若新 raw 仍显示 priority 非必填，属于真实 HTTP 与描述/生成合同不一致，应在 Source B 生成前裁决，不能以八类后端通过掩盖。
