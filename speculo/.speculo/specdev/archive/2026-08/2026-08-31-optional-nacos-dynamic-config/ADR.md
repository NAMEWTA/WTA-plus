# 可选 Nacos 动态配置架构决策

## ADR-001: Nacos 作为可选稀疏覆盖层

**Status:** accepted
**Source:** LOG-001, LOG-002
**Supersedes:** none

### Context
应用需要在运行后按需调整 YAML 配置，但用户明确要求没有 Nacos 或没有远程配置时不影响系统。把 Nacos 设为唯一配置事实源会使本地开发、故障恢复和应用启动强依赖外部服务。

### Decision
Nacos 只承担可选的动态配置覆盖。远程文档只包含需要覆盖的 YAML 键；未出现的键继续读取本地 `application*.yml`。功能关闭、远程无内容或 Nacos 不可达时，本地 YAML 是完整运行基线。服务注册与服务发现不属于本 change。

### Trade-off
完整远程托管能集中管理所有环境配置，但扩大了外部依赖和故障半径；稀疏覆盖保留可靠兜底，但需要明确优先级、删除语义和哪些属性能够安全热更新。

### Consequences
现有本地 YAML 不得被削减为只在 Nacos 存在时才完整的模板。远程配置的优先级、保护键、刷新白名单和失联行为必须在后续节点形成可验证合同。

### Verification / Migration
验收必须覆盖无 Nacos 启动、空远程文档、稀疏覆盖、远程不可达、键删除回退和非法文档保持上一有效版本。

## ADR-002: 薄 common 客户端与外部官方 Nacos Server

**Status:** accepted
**Source:** LOG-003, LOG-004
**Supersedes:** none

### Context
当前仓库以独立 `ruoyi-common-*` artifact 封装可选技术能力，并通过 Docker Compose 交付 MySQL、Redis、MinIO 和 Elasticsearch 等外部基础设施。CDE 同时存在业务内客户端代码和内嵌 Nacos Server，但用户要求本项目采用 common 模块和稳定镜像。

### Decision
新增 `ruoyi-common-nacos` 作为 Nacos Config 客户端与刷新能力的薄封装，并纳入 common reactor 和 BOM。Nacos Server 使用固定版本的官方 `nacos/nacos-server` 镜像加入现有 Docker 基础设施编排；本仓库不内嵌或重打 Nacos Server。

### Trade-off
放入 `ruoyi-system` 可以贴近参考实现和管理接口，但会把应用级配置引导绑定到业务模块；自建 Server 镜像可深度定制，却增加上游安全更新和构建责任。独立 common artifact 与官方镜像边界更清晰，但需要显式装配和版本兼容验证。

### Consequences
组装应用必须按需依赖 `ruoyi-common-nacos`；系统内配置管理面由 ADR-003 约束。服务端端口、凭据和健康合同仍由 D-015 收敛。

### Verification / Migration
Maven reactor、common BOM、目标应用 classpath、Spring Boot 4.1 启动以及 Docker Compose 配置和健康检查都必须有独立验证。

## ADR-003: 在系统管理中完整交付 Nacos 配置管理

**Status:** superseded
**Source:** LOG-006
**Supersedes:** none

### Context
仅交付 Nacos 官方控制台不能融入当前系统的菜单、按钮权限、统一操作日志和使用路径。参考项目已经提供基本读写 API，但用户要求在当前系统内完整复刻并允许必要增强。

### Decision
在 `ruoyi-system` 交付 Nacos 配置查询、校验和发布 API，在 `plus-ui-namewta` 交付系统管理下的 Nacos 配置管理页面，并通过动态菜单和功能权限授权。远程正文继续只存储在 Nacos，系统数据库不复制一份配置事实。

### Trade-off
复用官方控制台实现成本低且功能完整；自有管理面增加前后端、权限和测试成本，但能提供当前系统一致的授权、审计、预检与生效状态。保存第二份数据库副本可做查询，却会制造双事实源，因此不采用。

### Consequences
本 change 同时影响 backend、frontend 和增量 DML。所有发布操作必须使用 POST 和 `@Log`，页面不能获得 Nacos 账号、密码、token 或 identity。跨环境范围和并发合同等待 D-011 决定。

### Verification / Migration
验收覆盖菜单可见性、查询与发布权限、未授权访问、非法 YAML、并发修改、凭据不泄漏和真实 Nacos 发布。

## ADR-004: 部署层优先且保护 Nacos 引导键

**Status:** accepted
**Source:** LOG-007
**Supersedes:** none

### Context
参考实现把 Nacos PropertySource 放到最高优先级，因此远程内容可以覆盖容器环境变量。当前发布工件依靠环境变量注入数据库和监控等部署值；若 Nacos 还能改写自身连接与 profile，可能绕过部署约束或造成递归失联。

### Decision
配置优先级固定为命令行参数和环境变量高于 Nacos 稀疏覆盖，Nacos 高于 profile YAML 和基础 YAML。`nacos.config.*`、`spring.profiles.*` 以及规格中列明的其他引导键禁止出现在远程覆盖层。

### Trade-off
Nacos 绝对最高优先级更接近参考代码，也能远程覆盖任何部署值；部署层优先牺牲这部分自由度，换取容器配置、secret 注入和 Nacos 自身引导的稳定边界。

### Consequences
PropertySource 插入位置不能直接照搬参考实现的 `addFirst`。管理 API、启动加载和监听更新必须共用同一保护键校验器。

### Verification / Migration
测试必须证明环境变量不被 Nacos 覆盖、普通文件键可以被覆盖、保护键被拒绝且拒绝不会改变上一有效运行配置。

## ADR-005: 固定 Nacos 2.5.4 单机持久化发布基线

**Status:** accepted
**Source:** LOG-009
**Supersedes:** none

### Context
当前 Docker 发布是单网络、单 MySQL 的基础设施编排。需求需要一个稳定、可复现的 Nacos 服务，但没有要求在本 change 内交付生产多节点高可用。

### Decision
基础设施固定使用官方 `nacos/nacos-server:v2.5.4`，以 standalone 运行，复用现有 MySQL 服务中的独立数据库和账号，开启 Nacos 鉴权并默认只绑定本机。生产多节点集群另立部署 change。

### Trade-off
Nacos 3.x 提供更新能力，多节点集群提供更高可用性；2.5.4 单机基线与参考客户端更接近、资源更小、部署简单，但单节点故障时远程配置不可用。

### Consequences
应用必须保持 ADR-001 的本地兜底，不能因 Nacos 单节点故障停止启动。Docker 需要 Nacos schema 初始化、独立数据库用户、必填鉴权变量、健康检查和后端连接配置。

### Verification / Migration
真实 Compose 验证镜像固定、鉴权、MySQL 持久化、容器重启、健康状态和 Nacos 不可用时应用本地启动。

## ADR-006: 区分即时生效配置与重启生效配置

**Status:** accepted
**Source:** LOG-010
**Supersedes:** none

### Context
Nacos 可以在运行中推送任意 YAML 文本，但 Spring Environment 发生变化不意味着已经创建的端口监听器、数据源、条件 Bean 或普通字段都会被安全重建。把“内容已推送”描述成“所有配置已热更新”会产生不可验证的运行承诺。

### Decision
远程覆盖分为两类：只有规格明确列出并经测试的配置前缀进入即时生效配置清单；其余合法配置允许保存，但标记为重启生效。配置发布、运行日志和状态信息必须区分这两种结果。

### Trade-off
强制重建整个 ApplicationContext 可覆盖更多属性，但中断风险接近应用重启；宣称全部即时生效实现简单，却不符合 Spring 对象生命周期。显式分级增加维护清单的成本，换取清晰且可测试的行为合同。

### Consequences
`ruoyi-common-nacos` 需要独立判断远程文档是否合法、哪些变更可即时应用、哪些只记录为待重启；不能因存在重启生效键而拒绝整份合法文档，也不能对它们报告即时成功。

### Verification / Migration
测试至少覆盖可重绑定属性即时变化、`server.port` 等启动期配置保持当前值且重启后生效、混合文档的分类结果，以及分类状态不泄露敏感值。

## ADR-007: 以内嵌官方控制台替代自建配置管理面

**Status:** accepted
**Source:** LOG-013
**Supersedes:** ADR-003

### Context
当前管理端已经使用通用 external iframe、动态菜单权限、环境 URL 和 Nginx 同源反代接入 SnailJob、SnailAI。自建 Nacos CRUD 能提供 RuoYi 风格接口，但会重复维护官方控制台已有的历史、回滚、namespace 和权限能力。

### Decision
系统管理下新增 Nacos 配置管理菜单，复用通用 external iframe，并在生产通过 `/nacos/` 同源反代打开 Nacos 官方控制台。用户在 iframe 中使用 Nacos 自身账号登录；本 change 不实现 SSO，不创建 `/system/nacos` CRUD API，也不创建自有配置编辑器。

### Trade-off
自建页面可以统一 RuoYi 操作日志和发布前校验，但增加前后端合同并长期追赶官方能力。内嵌控制台降低重复实现，并保留完整官方功能，代价是菜单权限与配置权限分属两个系统，应用无法阻止非法内容先被保存到 Nacos。

### Consequences
RuoYi 只负责菜单入口和 URL 安全，不能向浏览器传递 Nacos 密码、token 或 identity。`ruoyi-common-nacos` 必须在消费端独立校验每次远程版本；Nacos 自身承担账号、配置历史和回滚。

### Verification / Migration
删除 ADR-003 规划出的自建 API、页面和按钮权限工件；验收动态菜单权限、iframe URL 安全、开发与生产地址、`/nacos/` 反代、Nacos 独立登录及凭据不注入前端。

## ADR-008: 接受 Nacos 中敏感配置的明文静态存储

**Status:** accepted
**Source:** LOG-011, LOG-014
**Supersedes:** none

### Context
需求允许数据库密码和第三方密钥进入 Nacos。Nacos AES 配置加密插件需要额外的 Server 插件、客户端兼容依赖和 dataId 约定，用户明确选择不承担这项复杂度。

### Decision
继续使用普通 `ruoyi-namewta.yml`，不引入配置加密插件。敏感值允许成为普通 Nacos 配置内容，并可能以明文存在于 Nacos 的 MySQL 持久化以及面向获授权用户的控制台中。

### Trade-off
静态加密可以降低数据库备份或存储泄露时的暴露，但增加插件供应、版本锁定和客户端解密风险。当前选择部署简单性，接受存储层不加密的风险，并依赖鉴权、可信网络和最小权限降低可访问面。

### Consequences
Nacos 数据库、备份和控制台账号都应按 secret 系统管理。应用日志、健康信息、异常、RuoYi 页面和测试证据不得包含配置正文或敏感值。该决定不应被描述为“secret 已加密”。

### Verification / Migration
测试使用假凭据验证覆盖行为，并对日志、状态接口、错误响应和前端构建产物执行不回显检查；真实 secret 不得写入仓库或 SpecDev 工件。

## ADR-009: 以本地基线和上一有效覆盖处理远程故障

**Status:** accepted
**Source:** LOG-015, LOG-016
**Supersedes:** none

### Context
可选 Nacos 必须同时处理远程删除、网络失联和非法版本。失联即回退会让运行配置因网络抖动突变，持久快照又会让重启实例使用运维当前不可见的陈旧配置；局部接受非法文档则无法证明实例对应哪个版本。

### Decision
远程清空或键删除使覆盖层回退本地值。运行中失联时保留进程内上一有效覆盖；失联后重启不使用远程持久快照，只使用本地 YAML。启动期非法文档被忽略，监听期非法文档整份原子拒绝并保留上一有效覆盖。

### Trade-off
保持上一有效覆盖提高运行连续性，但失联期间无法立即撤回远程值；离线重启回退本地提高可解释性，却可能令重启实例与尚未重启实例暂时不同。整份拒绝牺牲合法子集的更新，换取清晰版本边界。

### Consequences
连接和应用状态必须分别记录。即时生效键可在删除后回退，重启生效键只保证下次启动读取回退结果。非法版本需要运维在官方控制台修正或回滚。

### Verification / Migration
覆盖远程文档清空、单键删除、连接中断、离线重启、非法 YAML、保护键、类型错误和恢复后新版本收敛。

## ADR-010: 每个应用实例直接订阅并独立报告配置状态

**Status:** accepted
**Source:** LOG-017
**Supersedes:** none

### Context
Docker 发布包含两个 ruoyi-admin 实例。单凭 Nacos 发布成功不能证明每个实例都已解析并应用同一版本，引入 Spring Cloud Bus 或实例转发又会增加并非需求所需的消息基础设施和单点路径。

### Decision
每个 ruoyi-admin 实例直接订阅自身 profile 对应的 namespace、`DEFAULT_GROUP` 和 `ruoyi-namewta.yml`。不引入 Spring Cloud Bus；每个实例独立记录版本摘要、应用结果、最后成功时间及即时生效或等待重启分类。

### Trade-off
直接订阅简单且无转发单点，但运维需要按实例观察收敛状态。集中总线能统一传播事件，却不能消除实例级解析与应用失败，且扩大部署范围。

### Consequences
健康或管理状态不得返回配置正文与 secret。规格必须把“发布成功”“实例收到”“实例校验成功”和“实例即时生效”定义为不同状态。

### Verification / Migration
以两个真实应用实例发布同一版本，证明两者最终报告相同摘要；同时模拟单实例拒绝版本，证明失败可以单独定位。

## ADR-011: Nacos 基础设施采用本机暴露、独立存储权限和健康门

**Status:** accepted
**Source:** LOG-018
**Supersedes:** none

### Context
官方 Nacos 2.5.4 standalone 需要 HTTP 与 gRPC 端口、鉴权身份、MySQL 持久化和启动健康合同。直接公网暴露、复用业务数据库高权限用户或把固定 secret 提交到仓库都会放大 ADR-008 已接受的明文风险。

### Decision
8848/9848 默认只绑定 `NAMEWTA_BIND_HOST=127.0.0.1`，鉴权 token、identity 等部署参数由必填环境变量提供。Nacos 使用现有 MySQL 服务中的独立数据库和最小权限用户，交付所需持久目录、健康检查和后端健康依赖门；Web 控制台统一经 `/nacos/` 同源反代访问。

### Trade-off
直接暴露端口和复用账号更省配置，但扩大攻击面与数据权限。当前合同增加初始化变量、schema 和健康编排成本，换取明确隔离和故障可见性。

### Consequences
仓库只能提供示例变量，不能包含真实 token、identity、密码或业务 secret。standalone 是可复现基础设施基线，不代表生产高可用；高可用集群必须另立 change。

### Verification / Migration
真实 Compose 验证端口绑定、未授权拒绝、授权登录、独立 schema 权限、MySQL 持久化、健康门、`/nacos/` 代理和 Nacos 不可达时应用本地启动。

## ADR-012: Nacos 客户端默认关闭并由部署显式启用

**Status:** accepted
**Source:** LOG-020
**Supersedes:** none

### Context
Nacos 是可选覆盖层，但普通本地开发和部分部署并不运行 Nacos。客户端默认尝试连接会增加启动等待、错误日志和隐含外部依赖；始终关闭又会让标准 Docker 发布无法直接获得所需动态配置能力。

### Decision
`nacos.config.enabled` 默认值为 `false`，不迁移或删除现有 `application*.yml` 配置。本地开发显式选择是否开启；包含 Nacos 基础设施的 Docker 发布通过环境变量为每个 ruoyi-admin 实例启用客户端。

### Trade-off
默认开启减少一个部署参数，但会影响无 Nacos 环境；显式开启要求维护环境合同，却使连接行为可预测，并保留完全独立的本地启动路径。

### Consequences
关闭时不得创建 Nacos 客户端、后台监听器或连接告警。开启但远程不可达时仍按本地基线启动。Docker 两个实例必须使用相同环境配置单元参数，但分别建立订阅和报告状态。

### Verification / Migration
分别验证默认关闭、显式开启、开启但不可达、Docker 双实例开启以及现有本地 YAML 未被删除或变为残缺模板。
