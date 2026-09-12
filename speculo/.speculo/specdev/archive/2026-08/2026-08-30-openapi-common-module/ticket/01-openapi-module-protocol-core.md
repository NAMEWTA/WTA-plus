---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-01
title: 建立 OpenAPI common 模块与 NAMEWTA v1 协议内核
status: done
planning_depth: deep
planning_depth_reason: 新增 Maven 公共模块、方法级公开注解和跨语言认证 wire format，直接影响安全与兼容。
ready: true
risk: critical
blocked_by: []
contract_ids: [AC-003, AC-004, AC-007]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/annotation/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/config/properties/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/protocol/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/spi/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/test/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-doc/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path> => T-01"
---

# Ticket T-01: 建立 OpenAPI common 模块与 NAMEWTA v1 协议内核

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/01-openapi-module-protocol-core.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 建立后续凭据、目录、会话和网关共同依赖的稳定 common 合同与可执行签名内核。
- **可观察产出：** reactor 可单独构建新模块；相同固定请求在 Java 与独立向量生成器中产生完全相同的 canonical request 和 HMAC。
- **来源：** `US-001`、`US-004`、`AC-003`、`AC-004`、`AC-007`、`ADR-002`、`ADR-003`、`ADR-017`。
- **当前事实：** common 聚合/BOM 尚无 OpenAPI artifact；`common-satoken` 已依赖标准 `LoginUser`，`common-doc` 与 `common-redis` 可作为最小依赖。
- **Planning Depth 原因：** wire format、公共 SPI、认证算法与 Maven 图错误会影响所有后续切片。

## 2. 决策状态

### 已锁定决策

- `@OpenApi` 只允许方法级；类级使用不得产生公开语义。
- canonical request 严格采用 Spec 的九行 UTF-8/LF 合同；path/query/body 规范化只有一个实现。
- AppSecret 先 Base64URL 无填充解码为 HMAC key；签名使用 HMAC-SHA256 与常量时间比较。
- common 只声明窄类型化凭据解析、授权快照和调用事件 SPI，不引用 system Mapper、entity 或 VO。
- 本 Ticket 一次性拥有全部共享 POM 依赖声明；后续 Ticket 不再修改这些 POM。

### 已采用的低影响假设

- Java 包根使用 `org.dromara.common.openapi`；协议内部类型名可按同模块惯例确定。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| common module/POM、方法注解、配置属性模型、canonicalizer、HMAC、协议 DTO/SPI、固定向量 | JCA、common core/redis/satoken/doc、标准 `LoginUser` | Redis nonce、Session、system 实现、Servlet 网关、自动装配启用 |

## 4. 要构建什么

后端开发者可在方法上显式声明开放元数据；客户端和服务端可基于同一固定向量独立生成签名。非法版本、header 格式、路径/query 编码、时间/nonce 格式或签名长度在进入业务运行时前得到稳定拒绝，不暴露 secret。

## 5. 实现契约

- **入口或接缝：** `@OpenApi`、协议 canonicalizer/verifier、类型安全配置与三个窄 SPI。
- **输入与输出：** 原始 method/path/query/body bytes 与协议 headers -> canonical bytes、签名校验结果或统一认证失败。
- **公共接口变化：** 新增 `org.dromara:ruoyi-common-openapi`；不修改 `LoginUser`。
- **不变量：** 重复 query/空值保留；path 不合并斜杠；百分号十六进制大写；无 body 对零字节 hash。
- **状态或数据流：** 本 Ticket 只做纯协议计算，不访问 Redis、数据库或 Sa-Token 状态。
- **错误与失败行为：** 格式、版本、编码和签名错误统一失败；异常/日志不输出 secret、签名或 canonical 原始敏感 body。
- **兼容要求：** 只发布 NAMEWTA v1；普通 HTTP/Token 合同不变。
- **安全与隐私要求：** 至少 128 bit nonce、256 bit secret；比较必须常量时间。

## 6. 执行路线

1. 先以跨语言固定向量建立会失败的协议测试，覆盖 Unicode、重复/空 query、编码 path 与空/二进制 body。
2. 注册 common artifact、BOM 与消费依赖，保持依赖方向 common -> api/common，不反向依赖 system。
3. 实现方法级注解、配置模型、SPI 和纯 canonical/HMAC 内核。
4. 对每个字段做单变量篡改测试并确认统一失败。
5. 运行新模块测试、依赖方向 review 与受影响 reactor 编译。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 frontmatter 所列新模块子路径与四个共享 POM。
- **只读上下文：** Sa-Token、Redis、SpringDoc 和 `LoginUser` 当前公开实现。
- **共享路径：** 四个 POM 仅 T-01 修改；新模块后续子包按 Ticket 分区。
- **保留或不动：** `LoginUser`、`LoginHelper`、`SaPermissionImpl`、`PlusSaTokenDao` 和普通安全配置。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常向量 | common-openapi unit | `./mvnw -pl ruoyi-common/ruoyi-common-openapi -am test` | canonical 与签名固定值一致 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 篡改/畸形输入 | 参数化 unit | 单字段篡改与非法编码矩阵 | 全部统一拒绝且无敏感输出 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 依赖回归 | reactor compile/package | `./mvnw -pl ruoyi-admin -am package -DskipTests` | 无环且 full/core 可解析 artifact | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |

- **Workspace checks：** 按 Goal Plan 在 current-workspace 或 source-worktree 执行模块测试与 Maven 编译。
- **E2E disposition：** not-required：本切片是纯协议/shared-contract prefactor，外部调用由 T-09/T-12 验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source commit、direct-parent 或 candidate/result SHA、父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先引入 additive artifact 与合同，再由消费者 Ticket 实现；不启用运行时。
- **兼容窗口：** 无旧协议；v1 发布后 canonicalization 不得静默改变。
- **监控信号：** 不适用：本 Ticket 未进入请求链。
- **回滚或前向恢复：** 在消费者合入前可移除 additive module；消费者合入后仅前向修复 v1。
- **不可逆操作与批准点：** 无；implementation commit 与父分支推进仍需执行授权。
- **收缩条件：** 不适用：无旧形式。

## 10. 验收标准

- [ ] `AC-003`、`AC-004`、`AC-007` 固定向量和失败矩阵通过。
- [ ] 新 artifact 在 common 聚合、BOM、system/admin 依赖图中唯一且无环。
- [ ] common SPI 不暴露 system 实现类型，普通认证核心文件未修改。
- [ ] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`。
- [ ] 形成非空 implementation/source commit，经 direct-parent 或 candidate 验证并记录 result SHA。
- [ ] 未发生未批准偏差，Ticket、Map 与 Evidence 一致。
