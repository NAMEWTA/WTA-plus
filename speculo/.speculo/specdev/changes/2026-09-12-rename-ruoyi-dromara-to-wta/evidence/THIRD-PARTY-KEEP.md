# Third-Party Keep List

> **Owned rename target (LOG-020 / ADR-002):** first-party `org.dromara` → **`org.namewta`** (SUPERSEDES prior `org.wta`). This KEEP list is unchanged: third-party coordinates must **never** become `org.namewta.*`.


本清单为硬约束：I-implement 与任何机械重命名脚本**不得**改写下列 Maven 坐标、Java/Kotlin 包名、import、文档中的上游产品专名（作为依赖名引用时）。

## 1. 明确 KEEP（已在本仓库实测）

| 产品 | Maven groupId | 典型 artifactId | 运行时包（import 前缀） | 证据 |
|---|---|---|---|---|
| SMS4J | `org.dromara.sms4j` | `sms4j-spring-boot-starter` | `org.dromara.sms4j.**` | root `pom.xml`；`Sms4jBlendRegistry.java`；`Sms4jNotificationProviderResolver.java` |
| Warm-Flow | `org.dromara.warm` | `warm-flow-mybatis-plus-sb4-starter`, `warm-flow-plugin-ui-sb-web` | `org.dromara.warm.**` | root pom；`ruoyi-workflow` 多处 handler/controller |
| Easy-Es | `org.dromara.easy-es` | `easy-es-boot-starter` | `org.dromara.easyes.**` | root pom；`EasyEsConfiguration.java`；demo `DocumentMapper` |
| mica-mqtt | `org.dromara.mica-mqtt` | `mica-mqtt-client-spring-boot-starter` | `org.dromara.mica.mqtt.**` | root pom；`ruoyi-common-mqtt`；demo `MqttController` |

## 2. 同属「引入但不改坐标」的非 dromara 上游（对照，防误伤）

下列本就不在「owned dromara→namewta / ruoyi→wta」范围内，列出以免脚本按「第三方」误伤：

- `com.aizuda:snail-job-*` / `com.aizuda:snail-ai-*`（extend 内 `com.aizuda.**` 包 KEEP）
- `com.baomidou.*`, `cn.hutool`, `cn.dev33` (sa-token), `org.redisson`, `com.yomahub:liteflow-*`, AWS SDK, Nacos, Elasticsearch 客户端等

## 3. 检测规则（给实现 / 审阅用）

**KEEP 当且仅当满足任一：**

1. Maven `groupId` 匹配 `org.dromara.<segment>` 且 `<segment>` ∈ `{sms4j, warm, mica-mqtt, easy-es}`（可扩展：未来再引入的 dromara 生态独立产品同理）。
2. Java import / FQCN 匹配：
   - `org.dromara.sms4j.`
   - `org.dromara.warm.`
   - `org.dromara.easyes.`（注意无连字符）
   - `org.dromara.mica.`
3. 文档/注释中作为**上游产品名**引用（例如「SMS4J 官方支持 interface 配置源」）——可保留产品名；不得把产品名改成 wta。

**RENAME 当：**

1. `groupId` 恰好为 `org.dromara` 且 `artifactId` 以 `ruoyi-` 开头（本仓模块）。
2. 源码目录 / `package` 为 `org.dromara.{common,system,web,notify,workflow,profile,demo,job,ai,third,test,monitor,snailjob,snailai}.**`（自有业务与包装器）。
3. 模块目录名、Docker 镜像构建上下文中的 `ruoyi-*` 自有产物名、容器内自有 `/ruoyi/` 路径、品牌文案 RuoYi。

## 4. 易混淆对照

| 字符串 | 处置 |
|---|---|
| `org.dromara.common.sms`（本仓模块包） | RENAME → `org.namewta.common.sms` |
| `org.dromara.sms4j.api.SmsBlend` | KEEP |
| 类名 `Sms4jNotificationProviderResolver` 位于 `org.dromara.common.sms` | **类所在包 RENAME**；其 **import 的 sms4j API KEEP** |
| `org.dromara.workflow`（本仓） | RENAME |
| `org.dromara.warm.flow` | KEEP |
| 注释：「对接 dromara sms4j」 | AMBIGUOUS：叙述可改为「对接上游 SMS4J」；不得改 import |

## 5. 版本钉扎（调研时快照，非升级任务）

- `sms4j.version` = 3.3.5
- `warm-flow.version` = 1.8.9
- `mica-mqtt.version` = 2.6.8
- `easy-es.version` = 3.0.2

本期 rename **不升级**这些版本。
