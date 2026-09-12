---
schema_version: 3
artifact: ticket
change: 2026-08-24-oss-eli5
id: T-01
title: 扩展 OSS 双类型共享配置合同
status: done
planning_depth: deep
planning_depth_reason: 同时扩展 common-oss 访问语义、上传路由配置和私有下载策略，属于安全共享合同与 expand 阶段。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-003, AC-007, AC-013, AC-023]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/enums/AccessPolicy.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/config/AccessControlPolicyConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/config/OssClientConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/config/OssLifecycleProperties.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadProperties.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/config/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadConfigurationUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadPropertiesUnitTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/enums/AccessPolicy.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/config/AccessControlPolicyConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/config/OssClientConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/config/OssLifecycleProperties.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadProperties.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/config/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadConfigurationUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadPropertiesUnitTest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssConfigServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/enums/AccessPolicy.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/config/AccessControlPolicyConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/config/OssClientConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/config/OssLifecycleProperties.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadProperties.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/enums/AccessPolicy.java</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/config/AccessControlPolicyConfig.java</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/config/OssClientConfig.java</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/config/OssLifecycleProperties.java</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadProperties.java</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path> => T-01"
---

# Ticket T-01: 扩展 OSS 双类型共享配置合同

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/01-expand-oss-access-configuration.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 在不激活公共流量的前提下，先建立后续配置治理、readiness、URL 解析和上传路由共同依赖的安全配置合同。
- **可观察产出：** 应用配置只接受 `PRIVATE/PUBLIC_READ`；默认访问类型为 PRIVATE；命名私有访问策略和 uploadPolicy 存储绑定在启动阶段完成类型、引用格式与 TTL 边界校验。
- **来源：** `US-001`、`US-003`、`US-005`、`AC-003`、`AC-007`、`AC-013`、`AC-023`、`ADR-010`、`ADR-011`、`ADR-012`。
- **当前事实：** `AccessPolicy` 仍含 `PUBLIC_READ_WRITE`，默认 ACL 也是公开读写；uploadPolicy 没有 storageConfigKey；下载仅有全局 2 分钟 TTL。
- **Planning Depth 原因：** 这是共享安全合同的 expand prefactor，配置错误会被所有后续切片放大。

## 2. 决策状态

### 已锁定决策

- 运行时访问类型只保留 PRIVATE 与 PUBLIC_READ；空值和默认值保守解析为 PRIVATE。
- PUBLIC_READ 只表达匿名读取，不授权匿名上传、覆盖或删除；不得把 ACL 写入动作作为应用访问语义。
- 每个 uploadPolicy 必须声明 `storageConfigKey` 和预期访问类型；客户端请求合同不增加这两个字段。
- 私有下载默认 2 分钟；服务端命名策略只能在统一安全上下限内定义 TTL，未知、禁用或越界策略失败。
- 既有上传策略全部显式绑定 PRIVATE 默认 configKey，避免升级时改变现有上传落点。

### 已采用的低影响假设

- 命名私有访问策略与开发 Provider URL 回退继续归入 `oss.lifecycle` 类型化属性；具体内部类名按现有 Spring 配置风格确定。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 双类型枚举与保守默认、命名下载策略、uploadPolicy 存储绑定、默认 YAML 和配置测试 | Spring `@ConfigurationProperties`、现有 Policy 校验、现有 2 分钟下载 TTL | 数据库迁移、Provider 探测、实际路由、URL 解析、前端页面 |

## 4. 要构建什么

部署人员配置公共或私有存储用途时，应用启动即可发现非法类型、缺失 storageConfigKey、非法策略名、禁用策略和越界 TTL；已有配置未显式选择公共用途时始终保持 PRIVATE。该 Ticket 只扩展合同，不让公共配置进入可服务状态。

## 5. 实现契约

- **入口或接缝：** `AccessPolicy`、`OssClientConfig`、`OssLifecycleProperties`、`OssUploadProperties` 和 `application.yml`。
- **输入与输出：** 服务端配置 -> 经过校验的 PRIVATE/PUBLIC_READ、命名 TTL 与 storageConfigKey；非法配置 -> 启动失败。
- **公共接口变化：** 无 HTTP/Java 跨模块方法变化；只扩展服务器配置合同。
- **不变量：** 默认 PRIVATE；客户端无 configKey/Bucket/accessType/TTL 字段；同一策略的预期类型固定。
- **状态或数据流：** YAML 绑定 -> 静态格式/范围校验 -> 后续 Ticket 消费；不访问数据库或 Provider。
- **错误与失败行为：** 空 storageConfigKey、非法访问类型、未知命名策略或越界 TTL fail-fast；不回退公开读写。
- **兼容要求：** 现有策略键、HTTP 请求、Ticket 序列化和 2 分钟默认下载行为不变。
- **安全与隐私要求：** 配置异常不得输出 AccessKey、SecretKey 或完整签名 URL。

## 6. 执行路线

1. 先扩展属性单元测试，使 PUBLIC_READ_WRITE、缺失 storageConfigKey、非法 TTL 和客户端注入场景失败。
2. 收敛 common-oss 双类型语义和 PRIVATE 默认值，移除公开读写运行分支。
3. 扩展下载与上传类型化配置，给既有策略绑定 PRIVATE configKey。
4. 验证配置绑定、未知策略、边界 TTL、既有策略兼容和请求模型字段白名单。
5. 运行 common-oss/system/admin 定向测试与 reactor 编译。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 frontmatter 所列共享配置、YAML 与定向测试。
- **只读上下文：** 配置 Service、上传 Service 和冻结上游 SQL。
- **共享路径：** `AccessPolicy`、配置模型、两份 properties 与 `application.yml` 全部由 T-01 唯一修改；后续 Ticket 只读消费。
- **保留或不动：** `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`、Controller 和 Provider Client 方法。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 合法 PRIVATE/PUBLIC_READ 配置 | properties unit | `./mvnw -pl ruoyi-admin -am -Dtest='Oss*PropertiesUnitTest' -Dsurefire.failIfNoSpecifiedTests=false test` | 合法配置绑定且默认 PRIVATE | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 非法类型/TTL/存储绑定 | 参数化 unit | 同命令运行失败矩阵 | 启动校验拒绝且不回退 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 既有上传与请求合同 | HTTP/config regression | 运行 `OssUploadHttpContractUnitTest` 与配置字段扫描 | 客户端模型无 configKey/accessType/TTL，既有策略为 PRIVATE | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |

- **Workspace checks：** 按 Goal Plan 在 current workspace 或 source worktree 运行定向单元测试和 `./mvnw -pl ruoyi-admin -am -DskipTests package`。
- **E2E disposition：** not-required：本 Ticket 是 default-off 的共享配置 expand；真实双 Bucket 行为由 T-03/T-08 验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** 非空 implementation/source commit、parent before、适用 candidate/result SHA 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先以兼容字段扩展服务器配置，再由 T-02 迁移数据库语义，最后由消费者启用行为。
- **兼容窗口：** 既有 HTTP 与 Ticket 格式不变；服务器配置必须在进入 T-03 前完成 storageConfigKey 补齐。
- **监控信号：** 启动配置绑定失败及非法策略名；此 Ticket 不产生运行期健康状态。
- **回滚或前向恢复：** 公共流量尚未启用，可回滚配置扩展；一旦后续消费者合入，只允许前向修复双类型合同。
- **不可逆操作与批准点：** 无；implementation commit 与父分支推进仍需单独执行授权。
- **收缩条件：** 仓库扫描不存在 `PUBLIC_READ_WRITE/custom` 运行语义，且全部启用 uploadPolicy 均声明 storageConfigKey。

## 10. 验收标准

- [x] `AC-003`、`AC-007`、`AC-013`、`AC-023` 的配置与注入矩阵通过。
- [x] 默认值为 PRIVATE，现有策略未被隐式路由到公共存储。
- [x] 客户端请求合同没有 configKey、Bucket、访问类型或 TTL。
- [x] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`。
- [x] 实际修改未超出修正后的 writable_paths，共享路径只由 T-01 修改。
- [x] 形成非空 implementation/source commit，经 candidate 验证并记录 result SHA。
- [x] 测试路径修正已审计，Ticket、Map 与 Evidence 一致。
