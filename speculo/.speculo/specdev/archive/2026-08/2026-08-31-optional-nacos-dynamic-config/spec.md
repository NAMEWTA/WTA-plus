---
schema_version: 3
artifact: spec
change: 2026-08-31-optional-nacos-dynamic-config
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:采用可选 Nacos 稀疏覆盖、官方控制台内嵌、固定官方镜像、独立鉴权与即时生效配置清单
  - ADR-001..ADR-012
  - CODE:CDE Nacos reference implementation and current NAMEWTA backend/frontend/release conventions
  - OFFICIAL:https://nacos.io/docs/v2.5/manual/admin/auth/
  - OFFICIAL:https://github.com/nacos-group/nacos-docker/blob/master/README.md
---

# Spec: 可选 Nacos 动态配置

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

NAMEWTA 当前以本地 `application*.yml` 和部署环境变量提供配置。修改配置通常需要改文件并重启全部实例，而且系统管理中没有统一进入动态配置平台的入口。同时，普通本地开发和不部署 Nacos 的环境不能因此新增强制外部依赖。

### 目标用户与场景

- 应用运维人员按环境发布少量远程 YAML 覆盖，并区分已即时生效与等待重启的配置。
- 系统管理员从系统管理菜单进入官方 Nacos 控制台，并用 Nacos 自身账号完成配置历史、发布和回滚。
- 开发人员在未安装、未启用或暂时无法连接 Nacos 时，仍以完整本地 YAML 启动和开发。
- 安全与发布人员使用固定官方镜像、独立数据库权限、鉴权和本机默认端口暴露部署 Nacos。

### 成功标准

- Nacos 默认关闭；关闭、空配置或不可达均不破坏现有本地启动路径。
- 启用后，每个 ruoyi-admin 实例从所属环境配置单元读取稀疏覆盖，并遵守固定优先级、保护键、原子校验和故障语义。
- 已验证的即时生效配置无需重启即可观察到新行为，其余合法配置清楚报告为等待重启。
- 系统管理菜单能够以内嵌、同源方式打开官方控制台，但不绕过 Nacos 登录或泄露凭据。
- Docker 基础设施可重复启动固定版本 Nacos，验证鉴权、MySQL 持久化、健康门和双实例收敛。

### 非目标

- 不把 Nacos 用作服务注册或发现，不引入 Spring Cloud Bus 或 Spring Cloud Alibaba。
- 不自建 Nacos CRUD API、配置编辑器或 RuoYi 与 Nacos 的 SSO。
- 不承诺全部 YAML 配置即时生效，不在本 change 中实现 ApplicationContext 全量重建。
- 不交付生产多节点 Nacos 高可用集群，也不执行生产部署。

## 2. 解决方案与外部行为

### 解决方案摘要

新增薄 `ruoyi-common-nacos` 客户端能力，把 Nacos Config 作为本地 YAML 之上的可选稀疏覆盖层。命令行参数、JVM system properties 和环境变量保持更高优先级。应用按 active profile 选择 local、dev 或 prod namespace，固定使用 `DEFAULT_GROUP` 与 `ruoyi-namewta.yml`。每个 ruoyi-admin 实例直接订阅并独立报告配置摘要与应用状态。

系统管理仅增加通用 external iframe 菜单入口。开发环境使用显式 Nacos URL，发布环境通过 `/nacos/` 同源反代进入官方控制台；RuoYi 权限只决定入口是否可见，实际配置权限由 Nacos 登录与授权决定。

### 主要流程

1. `nacos.config.enabled=false` 时，应用只加载原有配置，不创建 Nacos 客户端、监听器或连接任务。
2. 显式启用时，应用根据 active profile 选择 namespace，直接请求当前配置并注册监听；无内容或请求失败时继续使用本地配置。
3. 收到非空远程文档后，应用完整解析 YAML，拒绝保护键并校验已知配置类型与约束；只有整份候选版本通过才替换上一有效覆盖。
4. 候选版本中的 `captcha.*`、`notify.idempotency.*` 与 `oss.lifecycle.download-ttl` 进入即时生效路径；其他合法键写入覆盖状态并标记为等待重启。
5. 管理员通过系统管理菜单打开官方控制台，用 Nacos 账号发布、查看历史或回滚；两个应用实例分别接收并报告同一版本摘要。

### 边界、失败与稳定错误行为

- 关闭功能时，Nacos 服务是否存在对应用无影响。
- 启用但启动期不可达、无 dataId、空文档或非法文档时，应用使用完整本地基线，不使用持久化的客户端离线快照。
- 运行中连接中断时保留进程内上一有效覆盖；连接状态可以失败，但不得自动把仍在运行的实例突变回本地值。
- 远程文档被清空或某键被删除时，相应覆盖撤销并回到当前本地值；即时生效键立即回退，重启生效键在下次启动体现。
- 非法 YAML、受保护键或已知键的类型/约束错误会原子拒绝整份新版本并保留上一有效覆盖。未知但语法合法的键允许进入重启生效分类，并由下一次标准应用启动校验。
- Nacos 发布成功、实例收到、实例校验成功与实例即时生效是不同状态，不得合并为一个“成功”。
- 任何日志、状态、健康信息、前端变量或测试证据不得返回远程正文、密码、token、identity 或敏感值。

### 状态转换与不变量

- 实例配置状态按 `本地基线 -> 已收到 -> 已拒绝或已接受 -> 已即时应用和/或等待重启` 演进；连接状态独立记录。
- 上一有效覆盖只在完整候选版本通过后原子替换；拒绝或断连不会产生部分更新。
- 配置优先级恒为：命令行参数/JVM system properties/环境变量 > Nacos 稀疏覆盖 > profile YAML > 基础 YAML。
- `nacos.config.*` 与 `spring.profiles.*` 永远不能由远程文档覆盖。
- 每个实例只订阅自己的环境配置单元；local、dev、prod 不共享 namespace。
- 本地 YAML 始终保持完整，启用 Nacos 不得成为应用启动成功的必要条件。

## 3. 用户故事

- **US-001**：作为应用运维人员，我希望按环境发布稀疏 YAML 覆盖，以便无需复制或替换整份本地配置。
- **US-002**：作为开发人员，我希望 Nacos 默认关闭且失败可降级，以便无 Nacos 环境保持现有启动体验。
- **US-003**：作为应用运维人员，我希望明确知道哪些配置已经即时生效、哪些等待重启，以便正确安排变更窗口。
- **US-004**：作为系统管理员，我希望从系统管理菜单进入官方 Nacos 控制台，以便使用官方历史、回滚和 namespace 能力。
- **US-005**：作为安全管理员，我希望控制台使用独立登录且凭据不进入 RuoYi 前端或应用输出，以便维持清晰的权限与 secret 边界。
- **US-006**：作为发布人员，我希望用固定官方镜像和现有 MySQL 部署可选 Nacos，以便获得可重复、可持久化且默认不对外暴露的基础设施。
- **US-007**：作为值班人员，我希望逐实例观察配置版本与失败状态，以便发现双实例未收敛或单实例拒绝。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 未配置 `nacos.config.enabled` 或显式为 `false` | 启动任一 ruoyi-admin bundle | 应用沿用现有 YAML；没有 Nacos 客户端、监听线程或连接告警 | Spring 启动集成测试、线程/Bean 断言 |
| AC-002 | Nacos 已启用且环境配置单元包含部分普通键 | 启动应用 | 远程只覆盖出现的键，未出现键仍取本地值 | `ruoyi-common-nacos` 集成测试 |
| AC-003 | 同一键分别存在于命令行或 system property、环境变量、Nacos、profile YAML、基础 YAML | 启动应用 | 取值符合固定优先级；部署层值不被远程覆盖 | PropertySource 优先级测试 |
| AC-004 | 远程文档包含 `nacos.config.*` 或 `spring.profiles.*` | 启动或监听更新 | 整份版本被拒绝，上一有效覆盖不变，并记录脱敏拒绝原因 | 校验器单元测试、监听集成测试 |
| AC-005 | active profile 分别为 local、dev、prod | 启用客户端 | 每个 profile 使用各自可由环境变量设置的 namespace，并固定 group/dataId | 配置绑定与订阅测试 |
| AC-006 | 远程配置缺失、空白或启动时 Nacos 不可达 | 启动应用 | 应用使用完整本地 YAML 正常启动 | 无服务/空 dataId 启动测试 |
| AC-007 | 本机存在旧 Nacos 客户端快照且服务不可达 | 重启应用 | 不读取旧持久快照，仅使用本地 YAML | 隔离临时目录的启动集成测试 |
| AC-008 | 应用已接受远程覆盖 | 运行中断开 Nacos | 当前进程保留上一有效覆盖，连接状态单独显示异常 | 真实 Nacos 故障注入测试 |
| AC-009 | 上一有效覆盖含某键 | 发布删除该键或清空文档的新版本 | 覆盖撤销并回到本地值；状态摘要更新 | 监听集成测试 |
| AC-010 | 应用已运行 | 发布 `captcha.*`、`notify.idempotency.*` 或 `oss.lifecycle.download-ttl` 的合法变化 | 后续业务调用无需重启即可观察到新配置行为 | 对应模块行为集成测试 |
| AC-011 | 应用已运行 | 发布 `server.port`、数据源、条件 Bean 或其他不在即时生效清单的合法变化 | 当前运行对象不被误重建，实例将这些键报告为等待重启 | 分类器测试、应用集成测试 |
| AC-012 | 远程文档同时含即时生效键和重启生效键 | 发布合法版本 | 整份覆盖被接受；两类键分别报告，只有清单内行为立即变化 | 混合文档集成测试 |
| AC-013 | 已存在上一有效覆盖 | 发布非法 YAML 或已知配置键的类型/约束错误 | 整份候选版本被原子拒绝，运行值与上一摘要保持不变 | 解析/绑定/原子更新测试 |
| AC-014 | 两个 ruoyi-admin 实例已启用同一环境配置单元 | 发布一个合法版本 | 两实例独立订阅并最终报告相同摘要；任一实例失败可单独定位 | 双实例 Docker E2E |
| AC-015 | 用户无 `system:nacos:console` 权限 | 请求动态菜单或直接访问 external route | 菜单不可见且前端路由守卫拒绝进入 | 菜单 DML 合同测试、前端权限测试 |
| AC-016 | 用户有入口权限 | 打开 Nacos 配置管理菜单 | 页面通过安全 URL 规则加载官方控制台，并要求 Nacos 自身登录；不存在 SSO 或密码注入 | 前端组件/路由测试、浏览器 E2E |
| AC-017 | 发布环境 Nginx 与 Nacos 运行 | 访问 `/nacos/` | 同源代理可加载完整控制台资源，且未放宽为任意跨域嵌入 | Nginx 配置检查、浏览器 E2E |
| AC-018 | 设置全部必填部署 secret | 启动基础设施 | 使用 `nacos/nacos-server:v2.5.4` standalone、独立 Nacos 数据库/用户、鉴权、持久目录和健康检查；8848/9848 默认绑定本机 | Compose 静态检查、真实容器验收 |
| AC-019 | 未提供必填 Nacos 鉴权或数据库 secret | 启动 Nacos-enabled 编排 | 编排明确失败，不使用仓库内固定默认 secret | Compose 配置负向测试、secret 扫描 |
| AC-020 | 分别使用基础编排与 Nacos-enabled 组合编排 | 启动服务 | 基础后端仍可独立运行且客户端关闭；组合编排为两实例启用客户端并以 Nacos 健康状态作为启动门 | Compose config、双场景 E2E |
| AC-021 | Nacos 已写入配置后重启容器 | 再次登录和读取配置 | 配置历史与当前内容仍存在，应用可重新读取 | MySQL 持久化 E2E |
| AC-022 | 日志、实例状态、健康响应、前端构建产物可检查 | 使用含假 secret 的测试配置执行正常和失败路径 | 任何接缝均不出现配置正文或假 secret | 自动化不回显断言、产物扫描 |
| AC-023 | Maven 以 full 与 core bundle 构建并启动 | 分别保持默认关闭或显式开启 | 两个 bundle 均包含可选客户端能力且默认行为兼容；关闭时无额外运行依赖 | Maven reactor 与 bundle 启动验证 |
| AC-024 | 全新数据库或已存在业务数据库 | 执行增量初始化脚本一次或重复执行 | 系统管理菜单和 Nacos 独立 schema 可创建且脚本幂等，不修改上游基线 SQL | SQL 合同测试、真实 MySQL 初始化测试 |

## 5. 范围

### IN

- 可选 `ruoyi-common-nacos` Config 客户端、稀疏覆盖、校验、监听、分类与实例状态。
- common reactor/BOM 和 ruoyi-admin full/core 的显式装配。
- local、dev、prod 环境配置单元、保护键、优先级、删除、断连和非法版本语义。
- 三组即时生效配置及其真实业务行为测试；其他配置的重启生效分类。
- 系统管理动态菜单、external iframe target、权限和环境 URL。
- `/nacos/` 同源反代、固定 Nacos 2.5.4 standalone、MySQL 持久化、鉴权、健康检查和可选组合编排。
- 新装与升级所需的追加式、幂等 SQL 和运行手册。

### REUSE

- 复用 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-elasticsearch</Path>` 的可选 common artifact 组织方式，但不复制其业务行为。
- 复用 `<Path>plus-ui-namewta/apps/admin-web/src/views/monitor/external</Path>`、外部监控 target、动态菜单权限和 URL 安全机制。
- 复用现有 Docker MySQL、network、本机端口绑定和 Nginx 外部服务反代约定。
- CDE Nacos 实现只作为客户端生命周期与 YAML 展平参考；优先级、默认开启、Spring Cloud rebinder 和快照行为以本 Spec 为准。

### OUT

- **OOS-001**：Nacos 服务注册、服务发现和微服务改造；需求只涉及配置。
- **OOS-002**：RuoYi 自建 Nacos CRUD API、页面和操作日志；官方控制台已承担管理面。
- **OOS-003**：RuoYi 到 Nacos 的 SSO 或自动登录；两个系统保持独立权限边界。
- **OOS-004**：AES 配置加密插件；已明确接受 Nacos MySQL 与授权控制台中的明文静态存储风险。
- **OOS-005**：所有 YAML 配置即时刷新；只承诺清单内且有行为测试的配置。
- **OOS-006**：Nacos 多节点生产高可用与公网暴露；另立部署 change。
- **OOS-007**：修改 `ry_vue.sql` 等上游基线或删除现有本地 YAML；迁移只追加到 NAMEWTA 管理文件。

## 6. 已锁定实现约束

- **DEC-001**：Nacos 是可选稀疏覆盖层，本地 YAML 是完整基线。来源：`ADR-001`。
- **DEC-002**：客户端由独立 `ruoyi-common-nacos` 薄 artifact 提供，Server 使用固定官方镜像。来源：`ADR-002`、`ADR-005`。
- **DEC-003**：部署层优先于 Nacos，远程禁止覆盖 Nacos 引导与 profile 选择。来源：`ADR-004`。
- **DEC-004**：只有 `captcha.*`、`notify.idempotency.*`、`oss.lifecycle.download-ttl` 属于本 change 的即时生效配置清单。来源：`ADR-006` 与当前源码运行期读取接缝。
- **DEC-005**：系统管理内嵌官方控制台，不实现自建管理 API、页面或 SSO。来源：`ADR-007`。
- **DEC-006**：允许普通 Nacos 配置保存敏感值，但不引入 AES 插件，所有输出必须脱敏。来源：`ADR-008`。
- **DEC-007**：运行时失联保留上一有效覆盖，离线重启只用本地基线，非法候选整份拒绝。来源：`ADR-009`。
- **DEC-008**：每个 ruoyi-admin 实例直接订阅并独立报告，不引入配置事件总线。来源：`ADR-010`。
- **DEC-009**：Nacos 使用独立数据库最小权限、必填外部 secret、本机默认端口和健康门。来源：`ADR-011`。
- **DEC-010**：客户端默认关闭；只有明确选择 Nacos 的环境才启用。来源：`ADR-012`。

## 7. 数据、接口与兼容

- **公共接口变化：** 新增 `nacos.config.*` 应用配置合同、Nacos external target 与 `system:nacos:console` 菜单权限。只提供脱敏的实例配置状态观测接缝，不新增业务 CRUD API。
- **数据模型与持久化：** Nacos 在现有 MySQL 服务中使用独立数据库和最小权限用户；远程正文只存于 Nacos，不复制进 RuoYi 业务表。系统库只追加动态菜单数据。
- **兼容要求：** 现有配置文件和非 Nacos 部署保持有效；full/core 两种 ruoyi-admin bundle 均可装配；现有 SnailJob/SnailAI external target 行为不回归。
- **迁移要求：** 只向 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` 及发布镜像追加幂等菜单 DML；Nacos schema 同时覆盖全新初始化与已存在数据卷的升级路径。不得修改上游基线 SQL。
- **发布或运维影响：** Nacos-enabled 组合编排需要显式 secret、namespace ID 和客户端启用变量；首次使用按官方流程设置强管理员密码。基础后端编排不要求 Nacos。

## 8. 非功能要求

- **NFR-001 安全与隐私：** Nacos 只面向可信网络，鉴权开启，数据库账号最小权限，端口默认只绑定 `127.0.0.1`；RuoYi 前端与运行输出不携带 Nacos 凭据、正文或敏感值。明文静态存储是已接受且必须在运维文档中显式说明的风险。
- **NFR-002 性能与容量：** 配置监听不得在每次业务请求中访问 Nacos；连接与读取超时可配置。除现有运行先例外，本 change 不虚构吞吐或延迟阈值。
- **NFR-003 可用性与可靠性：** Nacos 的关闭、缺失、空内容、启动不可达和运行断连均有确定降级行为；候选版本原子接受或拒绝，实例关闭时释放监听与客户端资源。
- **NFR-004 可观测性与运营：** 每个实例能观察连接状态、版本摘要、最后结果、最后成功时间、即时生效与等待重启分类，但不显示键值或正文；Docker 提供 Nacos 健康检查和可诊断的初始化失败。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| Nacos 配置解析、优先级、保护键、分类与生命周期 | 单元/应用集成 | AC-001..AC-013、AC-022、AC-023 | Maven reactor 的模块测试与 Spring Boot 启动测试 | 测试报告、启动日志脱敏断言 |
| Captcha、通知幂等与 OSS 下载 TTL 的后续业务调用 | 模块行为集成 | AC-009..AC-012 | 对应 common/system 模块现有 JUnit 测试约定 | 行为断言 |
| 动态菜单、external target、URL 安全与权限 | SQL 合同/Vitest | AC-015、AC-016、AC-024 | 前端 monitor domain 测试、manifest 测试、NAMEWTA DML 合同 | 测试报告、SQL 断言 |
| Nginx `/nacos/` 与官方登录页 | 配置/浏览器 E2E | AC-016、AC-017、AC-022 | 前端 Playwright 与发布 Nginx 配置检查 | 截图、网络与控制台证据 |
| MySQL + Nacos + 两个 ruoyi-admin 实例 | 真实 Docker 集成 | AC-006..AC-009、AC-014、AC-018..AC-024 | `docker compose config`、固定镜像的真实容器场景 | 健康状态、API/行为断言、持久化证据 |
| full/core bundle 与默认关闭兼容 | 构建/冒烟 | AC-001、AC-020、AC-023 | 项目 Maven bundle profile 与应用启动命令 | 构建日志、class path/Bean 断言 |
| 仓库 secret 与输出不回显 | 静态/运行扫描 | AC-019、AC-022 | 项目 secret 扫描和测试日志检查 | 扫描报告 |

## 10. 风险、假设与未决问题

### 风险

- Nacos 2.5.4 简单鉴权只适用于可信内网，不能被表述为互联网级安全边界。
- 允许敏感配置进入普通 dataId 意味着数据库、备份和获授权控制台用户可以看到明文；部署必须按 secret 系统治理。
- Nacos 单节点故障时，运行实例与离线重启实例可能暂时使用不同配置；逐实例状态用于识别该差异。
- 未知配置键无法在运行期证明业务类型，只能作为重启生效候选并由标准应用启动完成最终校验。

### 已采用的低影响假设

- external component 继续使用现有通用视图，Nacos 的稳定 target 名为 `nacos`；由前端合同测试验证，不影响服务端协议。
- 发布环境 URL 使用根相对 `/nacos/`，开发环境可用显式 `VITE_APP_NACOS_ADMIN`；由 URL 安全与浏览器测试验证。
- Nacos-enabled 通过独立 Compose 组合/override 启用两实例及健康依赖，基础 backend compose 保持可单独校验和运行；由两套 `docker compose config` 验证。

### 未决问题

无。
