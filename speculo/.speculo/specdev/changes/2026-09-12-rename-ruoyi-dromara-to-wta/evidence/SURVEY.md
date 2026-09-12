# Survey: ruoyi / dromara → wta 命名盘点

**Survey date:** 2026-09-12（UTC+8）
**Scope root:** `/workspace/vp-dev/ruoyi-vue-plus-docs`
**Excludes:** `node_modules`, `.git`, `target`, `dist`, `pnpm-lock.yaml`, `.flattened-pom.xml`

本文件只记录调研事实与分类；**不实施代码重命名**。I-implement 在后续 Work 授权后进行。

> **LOG-020 / ADR-002 (rev) naming note:** Owned Maven/Java rename target is **`org.namewta`** (not `org.wta`). Module/artifact prefix remains `ruoyi-X`→`wta-X`. Third-party `org.dromara.*` KEEP unchanged.


## 1. 总览计数（约数）

| 类别 | 约计 | 分类 |
|---|---:|---|
| 后端 `ruoyi-*` 模块/目录名 | 49 | OWNED_RENAME |
| 含 `<artifactId>ruoyi-` 的 pom.xml | 49 | OWNED_RENAME |
| `*/java/org/dromara/**/*.java`（项目自有源码） | 1648 | OWNED_RENAME |
| 内容命中 `org.dromara` 的文件（含 xml/yml/md/imports） | ~1788 | 多数 OWNED；见 KEEP 规则 |
| 内容命中 `ruoyi`（大小写不敏感，排除 lock） | ~277 | 多数 OWNED；部分品牌文案/路径 |
| 前端已用 `@namewta/*` 包名的文件触达 | ~374（namewta/NAMEWTA） | 已部分完成；残留见下 |
| `sms4j` / `org.dromara.sms4j` 文件 | 9 | THIRD_PARTY_KEEP |
| `org.dromara.warm` / warm-flow 文件 | 53 | THIRD_PARTY_KEEP |
| `easy-es` / `org.dromara.easy-es` 文件 | 7 | THIRD_PARTY_KEEP |
| `mica-mqtt` / `org.dromara.mica*` 文件 | 7 | THIRD_PARTY_KEEP |
| Mapper/XML 含 `org.dromara` | 95 | OWNED_RENAME（namespace/type） |
| Spring `AutoConfiguration.imports` 等含 `org.dromara` | ~27（源+编译产物；改源即可） | OWNED_RENAME |

## 2. Maven 坐标

### 2.1 项目自有（OWNED_RENAME）

| 现状 | 建议目标 | 证据路径 |
|---|---|---|
| `groupId=org.dromara` `artifactId=ruoyi-vue-plus` | `org.namewta` / `wta-vue-plus`（或 `wta` 根 artifact，待 tickets 定名） | `ruoyi-vue-plus-namewta/pom.xml` L7–L8 |
| `org.dromara:ruoyi-common-bom` | `org.namewta:wta-common-bom` | 同上 dependencyManagement |
| `org.dromara:ruoyi-profile-bom` | `org.namewta:wta-profile-bom` | 同上 |
| `org.dromara:ruoyi-system` 等业务模块 | `org.namewta:wta-system` 等 | root pom L430+ |
| 模块目录 `ruoyi-admin` / `ruoyi-common-*` / `ruoyi-modules/ruoyi-*` | `wta-admin` / `wta-common-*` / `wta-*` | `ruoyi-vue-plus-namewta/` |

根 POM 元数据仍写上游品牌：

- `<name>RuoYi-Vue-Plus</name>`
- `<url>https://gitee.com/dromara/RuoYi-Vue-Plus</url>`
- `<description>Dromara RuoYi-Vue-Plus后台管理系统</description>`

→ 产品表面 OWNED_RENAME；上游 URL 是否改写见 ADR-005 / AMBIGUOUS。

### 2.2 第三方（THIRD_PARTY_KEEP）

| groupId | artifactId（例） | 说明 |
|---|---|---|
| `org.dromara.sms4j` | `sms4j-spring-boot-starter` | 短信 SDK；坐标与 `import org.dromara.sms4j.*` 保持上游 |
| `org.dromara.warm` | `warm-flow-mybatis-plus-sb4-starter`, `warm-flow-plugin-ui-sb-web` | Warm-Flow 引擎 |
| `org.dromara.mica-mqtt` | `mica-mqtt-client-spring-boot-starter` | MQTT 客户端 |
| `org.dromara.easy-es` | `easy-es-boot-starter` | Easy-Es |

判定规则：`groupId` 为 `org.dromara.<product>`（带二级产品段）且 artifact 为独立开源产品 → **KEEP**。  
`groupId` 恰好为 `org.dromara` 且 artifact 为 `ruoyi-*` → **OWNED**。

## 3. Java / Kotlin 包

### 3.1 自有包前缀（OWNED_RENAME → `org.namewta.*`）

调研到的 `org.dromara` 下一级业务段（目录实测）：

`ai`, `common`, `demo`, `job`, `monitor`, `notify`, `profile`, `snailai`, `snailjob`, `system`, `test`, `third`, `web`, `workflow`

示例：

- `org.dromara.system.mapper` → `org.namewta.system.mapper`
- `org.dromara.common.mail.config` → `org.namewta.common.mail.config`
- `org.dromara.notify.service.runtime` → `org.namewta.notify.service.runtime`
- `org.dromara.web.controller` → `org.namewta.web.controller`

配置扫描亦为自有：

- `application.yml`: `mapperPackage: org.dromara.**.mapper`
- `typeAliasesPackage: org.dromara.**.domain`
- springdoc `packages-to-scan: org.dromara.demo|web|system|workflow`

### 3.2 第三方 import（THIRD_PARTY_KEEP）

真实命中示例（不得改写）：

```
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.aliyun.config.AlibabaFactory;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.easyes.core.kernel.BaseEsMapper;
import org.dromara.mica.mqtt.spring.client.MqttClientTemplate;
```

路径示例：

- `ruoyi-common/ruoyi-common-sms/.../Sms4jNotificationProviderResolver.java`
- `ruoyi-modules/ruoyi-notify/.../Sms4jBlendRegistry.java`
- `ruoyi-modules/ruoyi-workflow/.../WorkflowPermissionHandler.java`
- `ruoyi-common/ruoyi-common-elasticsearch/.../EasyEsConfiguration.java`
- `ruoyi-common/ruoyi-common-mqtt/.../MqttAutoConfiguration.java`

注意：Easy-Es 运行时包名为 `org.dromara.easyes`（无连字符），Maven groupId 为 `org.dromara.easy-es`——二者均 KEEP。

### 3.3 extend 包装器（OWNED_RENAME，但依赖上游 KEEP）

- `org.dromara.snailjob` / `org.dromara.snailai` / `org.dromara.monitor.admin`：本仓库包装启动类 → 改 `org.namewta.*`
- 同模块内 `com.aizuda.snailjob.*` / `com.aizuda.snail.ai.*`：上游引入 → **KEEP**

## 4. 前端

| 项 | 现状 | 分类 |
|---|---|---|
| workspace 根 `package.json` `"name"` | `"ruoyi-vue-plus"` | OWNED_RENAME → 建议 `namewta` / `wta-vue` |
| apps / packages | 已 `@namewta/admin-web`、`@namewta/domain-notify` 等 | 基本完成；非本期重点 |
| admin `.env*` `VITE_APP_TITLE` / `VITE_APP_LOGO_TITLE` | `RuoYi-Vue-Plus后台管理系统` / `RuoYi-Vue-Plus` | OWNED_RENAME（品牌文案） |
| home `.env*` 标题 | `NAMEWTA 用户中心` | 已符合；KEEP |
| repository.url | 仍指 `gitee.com/JavaLionLi/plus-ui.git` | AMBIGUOUS（上游溯源 vs 自有 remote） |

## 5. Docker / release-artifacts

| 项 | 现状 | 分类 |
|---|---|---|
| compose 服务名 | `namewta-server1`, `namewta-nginx-lb`, network `namewta` | 已 namewta；KEEP |
| `SPRING_APPLICATION_NAME` | `namewta-admin` | KEEP |
| 镜像名 | `namewta/namewta-admin:6.0.0` | KEEP |
| build context 目录 | `./backend/images/ruoyi-admin` 等 | OWNED_RENAME（目录/ Dockerfile 路径） |
| 容器内 WORKDIR / LOG_PATH | `/ruoyi/server`, `/ruoyi/monitor`, … | OWNED_RENAME → `/wta/...`（需同步 alloy 路径） |
| alloy `__path__` | `/var/log/ruoyi/...` | OWNED_RENAME |
| MySQL init | `10-ruoyi-base.sql`；种子 OSS bucket 例 `ruoyi-1240000000` | 文件名 OWNED；bucket 字面量 AMBIGUOUS |
| SQL 注释 | 「由 ruoyi-notify 拥有」 | OWNED_RENAME（文档性） |

## 6. 文档 / Skills / 仓库路径

> **SUPERSEDED (LOG-011 / ADR-008·010·011)：** 下列「聚合 KEEP / 去掉前缀 AMBIGUOUS」为调研当时快照，**已废止**。现行：子模块前缀 **换** `ruoyi-`→`wta-`；原三仓冻结；前端+后端+副仓合并进全新 public monorepo 并 orphan 重置历史。勿按本表旧 AMBIGUOUS 实施。

| 项 | 现状 | 分类 |
|---|---|---|
| 聚合仓目录名 `ruoyi-vue-plus-docs` | clone path / `.gitmodules` 子模块名 | ~~建议本期 KEEP~~ → **SUPERSEDED**：见 ADR-010/011（新 public monorepo；旧仓冻结） |
| 子模块目录 `ruoyi-vue-plus-namewta` / `frontend` | 已带 namewta 后缀 | ~~去掉 ruoyi- 前缀 AMBIGUOUS~~ → **SUPERSEDED**：ADR-008 **换前缀** `ruoyi-`→`wta-`（在新 monorepo 内） |
| Skills `ruoyi-module-guide` / `ruoyi-common-modules-guide` | 工程 Skill 名 | OWNED_RENAME（与代码波次同步） |
| README 标题 | `NAMEWTA RuoYi-Vue-Plus` | 品牌文案 OWNED；「基于上游」叙述 KEEP 溯源语义 |
| fm 模板 `import org.dromara.common...` | `docs/fm/java/**` | OWNED_RENAME |

## 7. 分类汇总表（按命中类）

| 分类 | 约文件触达 | 代表 |
|---|---:|---|
| OWNED_RENAME | ~1700+（java）+ ~49 pom 模块 + docker 路径 + 品牌 env + fm/skills | `org.dromara.system.*`, `ruoyi-admin`, `/ruoyi/server` |
| THIRD_PARTY_KEEP | sms4j~9 + warm~53 + easy-es~7 + mica~7 + aizuda 引入 | `org.dromara.sms4j.*`, `org.dromara.warm.*` |
| AMBIGUOUS_NEEDS_CONFIRM | 少量但高影响 | 见下节 |

## 8. AMBIGUOUS（需 CTO 确认）

> **部分已关闭（LOG-010/011）：** 项 1→ADR-008/010/011；项 2→ADR-006；项 3→ADR-007；项 4→ADR-009；项 6 方向→ADR-010（新 public orphan；旧仓不 rewrite）。下列保留作历史快照；实施以 ADR 为准。

1. ~~**聚合仓 / 子模块物理路径**是否在本期改名~~ → **CLOSED**：换前缀 + 新 monorepo（ADR-008/010/011）。
2. ~~**Nacos `data-id`**~~ → **CLOSED**：改+兼容期（ADR-006）；时长/退出指标仍待 S-spec 量化。
3. ~~**默认用户名 `ruoyi`**~~ → **CLOSED**：→`wta`（ADR-007）；旧库迁移策略仍待定。
4. ~~**种子字面量**~~ → **CLOSED**：一并改（ADR-009）。
5. **上游 URL**（pom `<url>`、frontend `repository.url`、LICENSE 作者行）保留溯源还是改为 NAMEWTA。
6. ~~**Git remote / 历史**~~ → **CLOSED 方向**：旧仓冻结；新仓 orphan public（ADR-010）；精确 slug 仍待 CTO。
7. **注释中同时出现**「dromara sms4j」类句子：改写叙述时不得改 import —— 实现时按行分类。

## 9. 抽样命令（可复现）

见同 change `source.md`「Survey commands」一节。
