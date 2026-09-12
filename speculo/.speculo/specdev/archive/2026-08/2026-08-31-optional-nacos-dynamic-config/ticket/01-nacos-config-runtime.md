---
schema_version: 3
artifact: ticket
change: 2026-08-31-optional-nacos-dynamic-config
id: T-01
title: 建立可选 Nacos 稀疏覆盖运行时
status: done
planning_depth: deep
planning_depth_reason: 新增共享 common artifact、启动期 PropertySource、远程版本原子状态机和脱敏观测合同，影响所有应用配置且具有较高事故半径。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-011, AC-012, AC-013, AC-014, AC-022, AC-023]
owner: unassigned
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-local.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-dev.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-prod.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/nacos/runtime/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-elasticsearch/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 建立可选 Nacos 稀疏覆盖运行时

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/01-nacos-config-runtime.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 建立其他切片共同依赖的可选 Nacos Config 客户端、稀疏覆盖、原子校验、刷新参与者合同和逐实例脱敏状态。
- **可观察产出：** full/core bundle 默认无 Nacos 行为；显式启用后按 profile 读取正确配置单元，合法版本形成覆盖和分类，故障按 Spec 降级。
- **来源：** `US-001`、`US-002`、`US-003`、`US-007`、`AC-001` 至 `AC-014`、`AC-022`、`AC-023`、`ADR-001`、`ADR-002`、`ADR-004`、`ADR-006`、`ADR-009`、`ADR-010`、`ADR-012`。
- **当前事实：** 仓库没有 Nacos 依赖；CDE 的参考实现默认开启、PropertySource 最高优先且使用 Spring Cloud rebinder，均不满足本 Spec。Nacos client 2.5.1 普通 `getConfig` 会读取本地快照，而 `getConfigAndSignListener` 可避免离线启动误用快照。
- **Planning Depth 原因：** 共享启动期基础设施、配置优先级和原子状态错误会影响所有业务模块和 secret。

## 2. 决策状态

### 已锁定决策

- 新 artifact 为 `ruoyi-common-nacos`，使用直接 Nacos Config SDK，不引入 Spring Cloud Alibaba、服务发现或 Bus。
- 客户端版本固定为经 Spring Boot 4.1/Java 21 验证的 Nacos 2.5.x 兼容版本；优先采用参考实现已使用的 `nacos-client:2.5.1`，升级必须另有兼容证据。
- `nacos.config.enabled` 默认 `false`；关闭时自动配置完全退场。
- namespace 由 local/dev/prod profile 映射且可被部署环境覆盖，group/dataId 固定默认 `DEFAULT_GROUP`/`ruoyi-namewta.yml`。
- 启动读取与监听注册使用不回退客户端磁盘 snapshot 的 SDK 接缝；不得通过 JVM 全局 snapshot 开关影响其他客户端。
- 候选 YAML 先完整解析、展平、保护键和已知类型校验，再以不可变快照原子替换；刷新参与者采用 prepare/commit 两阶段，prepare 可失败，commit 不得部分失败。
- 只保护 `nacos.config.*`、`spring.profiles.*` 及实现所需的等价 profile 引导键；不得任意扩大为业务键黑名单。
- 实例状态通过已鉴权的 Actuator info contributor `nacosConfig` 或等价稳定观测接缝提供，只含连接、摘要、结果、时间和分类计数。

### 已采用的低影响假设

- YAML 继续使用 Spring Boot `YamlPropertySourceLoader` 语义，避免自建展平规则。
- 未知键归类为重启生效；只有能从现有配置 metadata/绑定器识别的键才做运行期类型校验。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| common artifact、SDK 生命周期、PropertySource、状态机、分类/刷新 SPI、Actuator 状态、Maven 装配 | Spring Boot YAML loader/Binder、现有 actuator/security、Elasticsearch common 模块组织 | 具体业务刷新适配、Nacos Server、管理 CRUD、服务发现 |

## 4. 要构建什么

应用在默认配置下与当前完全一致。启用后，实例按所属环境配置单元取得当前远程 YAML，并把通过校验的稀疏键放在部署层之下、本地 YAML 之上。监听到新版本时先生成完整候选快照和刷新计划，任何解析、保护键或已知类型错误都拒绝整份版本；成功时一次性更新覆盖、提交已准备的即时刷新并记录等待重启分类。断连保留内存版本，离线重启只用本地基线。

## 5. 实现契约

- **入口或接缝：** Spring EnvironmentPostProcessor/启动监听、Nacos listener、刷新参与者 SPI、Actuator info contributor。
- **输入与输出：** `nacos.config.*` 与 active profile + Nacos YAML -> 稀疏 PropertySource、不可逆摘要、应用结果和分类。
- **公共接口变化：** 新增配置属性合同和 common 刷新参与者/状态观测合同；无业务 HTTP API。
- **不变量：** 部署层优先；本地 YAML 完整；候选版本原子接受或拒绝；状态不含键值；每实例独立客户端。
- **状态或数据流：** local -> fetched -> validated -> prepared -> committed/rejected；connection status 与 applied status 分离。
- **错误与失败行为：** 启动超时/空/非法用本地；监听非法保留上一有效；删除撤销覆盖；shutdown 注销 listener 并关闭 ConfigService。
- **兼容要求：** full/core 都直接包含 artifact；默认关闭不产生连接、线程、日志噪声或额外服务要求。
- **安全与隐私要求：** 用户名/密码/token、YAML 正文、键值和异常原文不得进入 info/log；摘要使用单向 digest。

## 6. 执行路线

1. 先建立默认关闭、优先级、保护键、snapshot 禁用语义和原子状态机失败测试。
2. 注册 Maven 版本、reactor、BOM 和 ruoyi-admin 直接依赖，形成 full/core 可编译的空自动配置。
3. 实现属性绑定、profile 配置单元解析、启动 fetch/listen 和稀疏 PropertySource 正确插入。
4. 实现 YAML/已知类型校验、两阶段刷新合同、删除/断连/恢复状态机与脱敏摘要。
5. 增加 Actuator 实例状态和确定性资源关闭，验证默认关闭没有客户端 Bean。
6. 执行模块、ruoyi-admin 启动和 full/core 构建回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 frontmatter 所列 POM、新 common artifact、四份应用配置和 Nacos 测试。
- **只读上下文：** Elasticsearch artifact 组织、三组目标配置及 system 行为。
- **共享路径：** 无跨 Ticket 共同写入；T-01 是新 common 合同与 POM 的唯一 writer，T-02 只读其公共合同。
- **保留或不动：** 现有 YAML 业务值、Spring Cloud 依赖、`ruoyi-system` HTTP API。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | common/boot 集成测试 | `./mvnw -pl ruoyi-admin -am -Dtest='*Nacos*Test' -Dsurefire.failIfNoSpecifiedTests=false test` | 稀疏覆盖、优先级、profile 和状态成立 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 失败 | fake ConfigService/临时 snapshot 目录 | 空、不可达、非法、删除、断连与离线重启矩阵 | 使用本地或上一有效版本，无部分应用 | 同上 |
| 回归 | Maven bundle | 分别 package full/core，并以默认关闭启动上下文 | 两 bundle 构建，默认无客户端行为 | 同上 |

- **Workspace checks：** current workspace 串行运行 formatter、定向 JUnit、reactor 和 full/core package；不得夹带当前脏工作树的用户改动。
- **E2E disposition：** not-required：本 Ticket 用可控 SDK 接缝证明状态机；真实 Nacos/MySQL/双实例由 T-03 与 T-06 的 parent/current 集成场景验证。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit、direct-parent before/result SHA、父分支包含关系与测试报告。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先 additive artifact/POM，再默认关闭装配，最后才允许部署显式启用。
- **兼容窗口：** 默认关闭期间现有环境不感知；T-02 完成前即时生效清单没有参与者，只能报告等待重启。
- **监控信号：** 客户端启用、连接状态、digest、last result/time、分类计数和拒绝类别。
- **回滚或前向恢复：** 保持配置键与 artifact 后回退启用变量；已发布远程内容不会成为本地启动依赖。
- **不可逆操作与批准点：** 无生产操作；实现 commit 与父分支推进需执行授权。
- **收缩条件：** 不适用：新增能力，无旧协议移除。

## 10. 验收标准

- [x] 所列 AC 的运行时、失败、状态与 bundle 合同通过自动化验证。
- [x] `ruoyi-common-nacos` 不包含 Server、服务发现、Spring Cloud 或自建管理 API。
- [x] 输出扫描不含假 secret、正文或键值。
- [x] 验证、implementation commit 和 direct-parent 结果写入 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`。
- [x] 修改未超出可写路径，无未批准偏差。
