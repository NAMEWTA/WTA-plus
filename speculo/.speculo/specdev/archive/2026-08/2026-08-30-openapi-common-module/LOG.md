# OpenAPI Common Module Design Log

## Research: 来源实现与 NAMEWTA 适配风险
- Decision / target: 为签名协议、模块边界、身份注入和接口扫描决策提供事实基线；目标工件为 `<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/LOG.md</Path>`。
- Scope / version: 2026-08-30 当前 NAMEWTA 后端与 RuoYi-Plus-UniApp OpenAPI 公开文档。
- Stop condition: 已确认来源方案的认证流程、签名内容、注解粒度和模块组成，并能与本仓库安全不变量逐项比较。

### R-001
- Claim: 来源方案支持方法级和类级 `@OpenApi`，使用 Spring MVC 拦截器、Redis 防重复、AppKey 关联用户权限、密钥管理和自动接口扫描。
- Type: official fact
- Source: <Url>https://ruoyi.plus/backend/common/openapi.html</Url>
- Confidence: high
- Limits: 公开文档展示的是该项目方案，不证明代码能直接兼容本仓库 Spring Boot 4、Sa-Token JWT 与 Client 定制。
- Artifact impact: D-001、D-004、D-009。

### R-002
- Claim: 来源签名是 `MD5(appKey + timestamp + appSecret)`，未覆盖 HTTP method、path、query 或 body；URL 参数也可传认证材料。
- Type: official fact
- Source: <Url>https://ruoyi.plus/backend/common/openapi.html</Url>
- Confidence: high
- Limits: 文档同时声明 Redis 签名去重和 60 秒时间窗，但这不能把签名绑定到原请求语义。
- Artifact impact: D-003、D-011、D-014。

### R-003
- Claim: NAMEWTA 当前全局安全拦截器先要求 Sa-Token 登录，再校验 OAuth `clientid`、Token 内 `clientPk` 对应的访问路径和 IP 白名单；权限只从当前 Token 的 `LoginUser` 快照读取，缺失上下文时返回空权限。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-security/src/main/java/org/dromara/common/security/config/SecurityConfig.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/service/SaPermissionImpl.java</Path>`。
- Confidence: high
- Limits: 尚未选择开放请求使用 request-scoped identity 还是专用机器 Token。
- Artifact impact: D-002、D-007、D-008。

### R-004
- Claim: NAMEWTA 的角色、菜单和登录权限已经按 `sys_client.id` 隔离；`clientId` OAuth 字符串与 `clientPk` Long 主键语义不同，缺少 Client 上下文不得回退到全局权限。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysLoginService.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysPermissionServiceImpl.java</Path>`；`CODE:<Path>docs/upstream/customization-map.md</Path>`。
- Confidence: high
- Limits: OpenAPI credential 的 Client 归属尚待用户决策。
- Artifact impact: D-002、D-005、D-008。

### R-005
- Claim: 当前 `ruoyi-common` 是聚合 POM；已有 common 模块以独立 artifact 和 Spring Boot auto-configuration 组织。密钥持久化与用户/角色业务若放入 common 会导致其反向依赖 system。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-doc/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</Path>`。
- Confidence: high
- Limits: 最终 SPI 数量和落点需要在产品边界确认后决定。
- Artifact impact: D-001、D-006。

### R-006
- Claim: 仓库已有 SpringDoc 3、运行时 JavaDoc 和 Sa-Token 权限元数据解析能力，前端也已有可追溯 OpenAPI 合同生成工具；另写类路径扫描和参数反射会形成第二套接口事实源。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-doc/</Path>`；`CODE:<Path>plus-ui-namewta/tooling/openapi/</Path>`。
- Confidence: high
- Limits: 面向第三方的接口详情 DTO 与客户端示例仍需设计。
- Artifact impact: D-009、D-015。

### Conflicts and Unknowns
- 用户描述同时提到“Controller 类上注解”和“每个方法上注解”，需要明确类级新增方法是否自动暴露。
- 用户请求点名新增 common 模块，但描述覆盖数据库、system 管理 API、个人中心和后台菜单，首期交付边界尚未确定。
- 本仓库没有旧 OpenAPI 平台表或凭据迁移证据；是否存在仓库外存量调用方尚未知。

### Recommendation
- 借鉴来源方案的产品体验，不逐行移植其安全协议与模块耦合。
- 先锁定 D-001 至 D-004，再决定身份上下文、数据模型、管理端、统计和验收细节。

## LOG-001 — 2026-08-30T20:52:10+08:00 — 首期产品边界
- **设计树节点：** D-001
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 首期交付完整平台、后端基础设施还是完整后端。
- **事实与来源：** 用户选择 Q1=C，并明确该能力属于 `ruoyi-system` 的系统能力。
- **选项：** 完整平台；仅 common 基础设施；完整后端、不做前端。
- **推荐：** 完整可用平台按垂直切片交付。
- **结论：** 首期交付完整后端，不交付前端页面；系统能力 owner 为 `ruoyi-system`。
- **原因：** 用户明确选择。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 后端 API 必须独立可验证，并为未来前端保留稳定消费合同。
- **后续：** D-006 决定 common 通用运行时和 system 业务实现的物理边界。
- **替代/被替代：** 无

## LOG-002 — 2026-08-30T20:52:10+08:00 — 调用身份绑定
- **设计树节点：** D-002
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** AppKey 绑定用户全局身份、Client 身份还是独立服务账号。
- **事实与来源：** 用户选择 Q2=A；当前角色、菜单与数据权限使用 `clientPk` 隔离。
- **选项：** userId + clientPk；仅 userId；独立服务账号。
- **推荐：** 固定绑定 userId + clientPk。
- **结论：** 每个 AppKey 固定绑定 `userId + clientPk`，不得由调用方切换 Client。
- **原因：** 保持现有 Client 授权与数据权限不变量。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 缺少、失效或不匹配的 Client 上下文必须失败关闭。
- **后续：** D-005、D-007、D-008 决定所有权、身份注入和持续准入。
- **替代/被替代：** 无

## LOG-003 — 2026-08-30T20:52:10+08:00 — 签名与传输协议
- **设计树节点：** D-003
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 是否兼容来源 MD5 协议，以及是否强制 HTTPS。
- **事实与来源：** 用户不要求兼容来源方案，授权设计更安全的新协议；同时明确请求既可使用 HTTP，也可使用 HTTPS。
- **选项：** 仅新协议；兼容 MD5；双协议迁移。
- **推荐：** HMAC-SHA256 规范请求且仅 HTTPS。
- **结论：** 只设计版本化 HMAC-SHA256 新协议；服务端允许 HTTP 和 HTTPS，TLS 仅推荐、不强制。
- **原因：** 用户明确选择；HMAC 提供身份与请求完整性，协议本身不承担传输机密性。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 文档必须明确 HTTP 会暴露请求和响应内容；secret 不在网络上传输，签名覆盖请求语义并使用 nonce 防重放。
- **后续：** D-010、D-011 固化 secret 和重放/重试细节。
- **替代/被替代：** 无

## LOG-004 — 2026-08-30T20:52:10+08:00 — 注解暴露粒度
- **设计树节点：** D-004
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 是否允许类级注解自动开放 Controller 方法。
- **事实与来源：** 用户选择 Q4=B。
- **选项：** 类级与方法级；仅方法级；类级只分组。
- **推荐：** 类级与方法级并提供排除注解。
- **结论：** `@OpenApi` 只允许方法级使用，每个开放接口必须显式标注。
- **原因：** 避免 Controller 后续新增方法时被隐式对外开放。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 类级出现 `@OpenApi` 应在编译或启动验证中被拒绝，而不是静默忽略。
- **后续：** D-009 使用同一方法注册表生成接口目录。
- **替代/被替代：** 无

## LOG-005 — 2026-08-30T20:52:10+08:00 — 前端管理范围
- **设计树节点：** D-013
- **轮次与依赖：** round 1 / D-001
- **状态：** rejected
- **问题：** 首期是否交付个人中心和超管开放平台页面。
- **事实与来源：** Q1=C 明确首期只做完整后端。
- **选项：** admin-web 完整交付；多 App 组合；首期不交付。
- **推荐：** admin-web 首期完整交付。
- **结论：** 首期不修改任何前端 App。
- **原因：** 用户明确限定交付边界。
- **影响工件：** Spec / Ticket
- **约束或不变量：** 后端管理 API 与接口目录 API 仍需是稳定公共合同。
- **后续：** 前端实现需要未来独立 change。
- **替代/被替代：** 无

## Research: RocketMQ Command 与本仓库扩展模式
- Decision / target: 决定 common 运行时如何调用 system 的密钥、身份和审计实现；目标工件为 `<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/LOG.md</Path>`。
- Scope / version: Apache RocketMQ develop 分支公开设计与源码；2026-08-30 当前 NAMEWTA common/system 实现。
- Stop condition: 已确认 RocketMQ Command 分派所解决的问题，并找到本仓库可直接沿用的解耦模式。

### R-007
- Claim: RocketMQ 使用 `RemotingCommand.code` 从 processor table 选择 `NettyRequestProcessor` 和业务线程池，该模式服务于自定义网络协议、请求码和并发隔离。
- Type: official fact
- Source: <Url>https://github.com/apache/rocketmq/blob/develop/docs/cn/design.md</Url>；<Url>https://github.com/apache/rocketmq/blob/develop/broker/src/main/java/org/apache/rocketmq/broker/processor/SendMessageProcessor.java</Url>。
- Confidence: high
- Limits: NAMEWTA 的 HTTP 方法已经由 Spring MVC HandlerMapping 分派，没有 RocketMQ 自定义 RPC requestCode。
- Artifact impact: D-006。

### R-008
- Claim: 本仓库已有 common 声明 SPI、system 实现 Bean 的成熟模式，例如 `PermissionService`；统一通知还使用类型化 Adapter、不可变请求结果、注册表重复检测和自动配置装配。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/service/PermissionService.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysPermissionServiceImpl.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/config/NotifyAutoConfiguration.java</Path>`。
- Confidence: high
- Limits: OpenAPI 只有一个 system 凭据实现时不需要多 Adapter 注册表；扩展点应按真实多态需要最小化。
- Artifact impact: D-006、D-007、D-012。

### Recommendation
- common 运行时通过类型化凭据解析、身份解析和调用事件端口调用 system，不直接持有 Mapper，也不使用 `Object` 返回的通用命令总线。
- system 内生成、重置、禁用、删除等写用例可以使用类型化 command record 作为输入，但由明确的应用服务处理；不增加 requestCode dispatcher。
- 开放平台启用但必需 SPI 缺失或重复时启动失败；可选扩展通过条件 Bean 装配并定义明确默认行为。

## LOG-006 — 2026-08-30T21:02:10+08:00 — 密钥所有权与委托
- **设计树节点：** D-005
- **轮次与依赖：** round 2 / D-001, D-002
- **状态：** confirmed
- **问题：** 用户自助与管理员代管的所有权、可见性和撤销权。
- **事实与来源：** 用户选择 Q5=A，并补充不是每个用户都拥有 OpenAPI 页面权限，继续使用既有菜单权限模式。
- **选项：** 用户自助加超管代管；仅超管；仅用户。
- **推荐：** 用户自助加超管代管，secret 只显示一次。
- **结论：** 有相应权限的用户只能管理当前 `userId + clientPk` 的密钥；超级管理员可管理合法目标身份的密钥；保存后的 secret 对所有人不可见。
- **原因：** 复用既有 RBAC，同时支持自助与平台治理。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 管理员代建必须验证目标用户具备目标 Client 登录域；owner 与 creator 分开审计。
- **后续：** D-010 决定 secret 生成、加密和轮换。
- **替代/被替代：** 无

## LOG-007 — 2026-08-30T21:02:10+08:00 — common 与 system 边界
- **设计树节点：** D-006
- **轮次与依赖：** round 2 / D-001, D-002
- **状态：** confirmed
- **问题：** common 通用工具如何优雅调用 system 的数据库和权限实现。
- **事实与来源：** 用户选择 Q6=A，并要求参考 RocketMQ Command 及仓库成熟设计模式；R-007、R-008 完成对比。
- **选项：** common 运行时加 system 实现；全部 system；业务持久化放 common。
- **推荐：** common 定义运行时与类型化 SPI，system 实现业务和持久化。
- **结论：** `ruoyi-common-openapi` 不依赖 system Mapper/domain，通过窄类型化端口调用唯一 system 实现；不引入通用 requestCode CommandBus。
- **原因：** 保持依赖方向和类型安全，同时沿用本仓库 common SPI/Adapter 习惯。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** common 不执行数据库查询；启用功能时必需 system SPI 缺失或重复必须启动失败。
- **后续：** D-007 决定请求身份上下文，D-012 决定审计事件端口。
- **替代/被替代：** 无

## LOG-008 — 2026-08-30T21:02:10+08:00 — 开放接口目录与文档
- **设计树节点：** D-009
- **轮次与依赖：** round 2 / D-001, D-004
- **状态：** confirmed
- **问题：** 无前端首期是否仍提供专用接口目录与详情 API。
- **事实与来源：** 用户选择 Q7=A；仓库已有 Spring MVC、SpringDoc 和运行时 JavaDoc 能力。
- **选项：** 专用目录与详情；仅原始 api-docs；不提供。
- **推荐：** 专用 API 复用实际映射和 SpringDoc。
- **结论：** 后端提供按当前 Client 权限过滤的接口目录与详情，包含参数、示例和 cURL/Java 调用片段。
- **原因：** 保留来源方案的免手写文档体验，同时不建立第二套接口事实源。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 目录可见性与真实可调用性使用同一权限判定和开放接口注册表。
- **后续：** D-015 覆盖映射与 SpringDoc 漂移测试。
- **替代/被替代：** 无

## LOG-009 — 2026-08-30T21:02:10+08:00 — 兼容与迁移
- **设计树节点：** D-014
- **轮次与依赖：** round 2 / D-003
- **状态：** confirmed
- **问题：** 是否存在旧 MD5 调用方、密钥或数据表。
- **事实与来源：** 用户选择 Q8=A；仓库检索也未发现旧开放平台实现。
- **选项：** 全新 v1；兼容或迁移旧实现。
- **推荐：** 只发布 v1。
- **结论：** 无旧协议或数据迁移，MD5 入口不实现。
- **原因：** 用户确认且仓库事实一致。
- **影响工件：** Spec / Ticket
- **约束或不变量：** 不预留旧协议代码。
- **后续：** 无。
- **替代/被替代：** 无

## LOG-010 — 2026-08-30T21:02:10+08:00 — OpenAPI RBAC 准入
- **设计树节点：** D-008
- **轮次与依赖：** round 2 / D-001, D-002, D-005
- **状态：** confirmed
- **问题：** OpenAPI 平台准入使用独立模式配置还是现有菜单权限。
- **事实与来源：** 用户在 Q5 补充 OpenAPI 页面访问继续采用原有菜单权限管理。
- **选项：** 独立 ALL/ROLES/ADMIN/SUPER_ADMIN；现有 Client-scoped 菜单权限。
- **推荐：** 使用现有 RBAC，避免双重准入来源。
- **结论：** 密钥管理和目录 API 使用既有菜单/按钮权限；开放调用使用目标方法原有 `@SaCheckPermission`，不再增加独立 access-control mode。
- **原因：** 与现有角色菜单配置、超管通配符和 Client 隔离保持一致。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 页面隐藏不是授权边界；每个管理 API 都有后端权限注解和 owner 范围校验。
- **后续：** D-015 覆盖权限矩阵。
- **替代/被替代：** 无

## Research: Sa-Token 请求身份桥接
- Decision / target: 为 D-007 选择能复用现有 `@SaCheckPermission`、`LoginHelper` 和数据权限的开放调用身份；目标工件为 `<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/LOG.md</Path>`。
- Scope / version: 本仓库 Sa-Token 1.45.0、JWT simple 模式和 Redis DAO；Sa-Token 官方 dev 文档与源码。
- Stop condition: 已确认无需把内部 Token 返回给第三方也能让当前请求进入现有 Sa-Token 权限链。

### R-009
- Claim: Sa-Token 提供 `createLoginSession` 创建登录会话但不注入上下文，并提供 `setTokenValueToStorage` 将 Token 仅写入当前请求 Storage；后续 `getTokenValue` 优先读取该 Storage。
- Type: official fact
- Source: <Url>https://github.com/dromara/sa-token/blob/dev/sa-token-doc/api/stp-util.md</Url>；<Url>https://github.com/dromara/Sa-Token/blob/dev/sa-token-core/src/main/java/cn/dev33/satoken/stp/StpLogic.java</Url>。
- Confidence: high
- Limits: 官方链接是 dev 分支；实现时必须以 Maven 锁定的 1.45.0 API 编译和集成测试为准。
- Artifact impact: D-007。

### R-010
- Claim: 当前 NAMEWTA 的 `SaPermissionImpl` 只接受当前 Token session 中、且 loginId 匹配的 `LoginUser`；单独的业务 ThreadLocal 或 `switchTo` 不能完整提供现有权限快照。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/service/SaPermissionImpl.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/utils/LoginHelper.java</Path>`。
- Confidence: high
- Limits: 最终内部 Token 的缓存周期、撤销和并发刷新仍由 D-007/D-010 决定。
- Artifact impact: D-007、D-015。

### Recommendation
- 保持普通 Sa-Token 请求路径不变；仅当开放方法没有有效普通登录而提交签名头时，完成 OpenAPI 验签并把内部机器 Token 写入当前请求 Storage。
- 内部 Token 不进入响应、日志或第三方文档；每次调用在执行目标方法前刷新绑定 `userId + clientPk` 的 `LoginUser` 快照，调用结束清理请求级状态。
- AppKey 禁用、删除、过期或 owner/Client 失效时，验签入口先拒绝；内部 Token 即使尚未物理过期也不能绕过凭据验证。

## LOG-011 — 2026-08-30T21:33:04+08:00 — 请求身份注入
- **设计树节点：** D-007
- **轮次与依赖：** round 3 / D-002, D-003, D-006
- **状态：** confirmed
- **问题：** 验签后如何进入现有权限与数据权限链。
- **事实与来源：** 用户选择 Q9=A；R-009、R-010 已确认 Sa-Token 请求 Storage 桥接能力和当前 `LoginUser` 读取约束。
- **选项：** 请求内机器 Token；持久浏览器式 Token；改造为独立 ThreadLocal 身份。
- **推荐：** 请求内机器 Token，并逐请求刷新权限快照。
- **结论：** 普通 Sa-Token 请求不变；签名请求验签后把内部机器 Token 仅写入当前请求上下文，绝不返回调用方，每次请求重新构建用户权限快照。
- **原因：** 复用现有 `@SaCheckPermission`、`LoginHelper` 和数据权限，避免第三方维护登录 Token。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 内部 Token 不得绕过每次凭据校验；请求结束必须清理上下文。
- **后续：** D-016 确认构建快照所使用的唯一 Client。
- **替代/被替代：** 无

## LOG-012 — 2026-08-30T21:33:04+08:00 — Secret 生成、保存与重置
- **设计树节点：** D-010
- **轮次与依赖：** round 3 / D-002, D-003, D-005
- **状态：** confirmed
- **问题：** AppSecret 如何生成、保存和轮换。
- **事实与来源：** 用户选择 Q10=A，并要求每用户仅一个有效凭据。
- **选项：** 单 secret 立即切换；双 secret 过渡；不可逆摘要。
- **推荐：** CSPRNG、AES-256-GCM、版本化外部 KEK、一次展示、重置立即失效。
- **结论：** 每条凭据只有一个有效 secret；密文可认证加密保存，重置后旧 secret 立即失效。
- **原因：** 验签需要服务端取得 secret，同时数据库不能保存明文。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** KEK 不入库、不进仓库；secret 不回显、不记录。
- **后续：** D-011 确认重放、幂等和限流。
- **替代/被替代：** 无

## LOG-013 — 2026-08-30T21:33:04+08:00 — 调用审计内容目标
- **设计树节点：** D-012
- **轮次与依赖：** round 3 / D-001, D-006
- **状态：** confirmed-with-follow-up
- **问题：** 首期调用记录只保存元数据、摘要还是完整请求与响应。
- **事实与来源：** 用户选择 Q11=C，表达希望保存完整请求与响应正文。
- **选项：** 元数据；元数据加摘要；完整正文。
- **推荐：** 原推荐为不含敏感正文的审计事件；用户选择完整正文。
- **结论：** 以可审计完整正文为产品目标，但不能据此让 secret、Token、密码、二进制和无界流量直接落日志。
- **原因：** 完整正文便于排障和计费核对，但与仓库日志安全不变量及流式接口技术边界存在冲突。
- **影响工件：** ADR / Spec / Ticket
- **约束或不变量：** 安全排除、脱敏、体积上限和流式内容规则必须先由 D-017 决定。
- **后续：** D-017。
- **替代/被替代：** 无

## LOG-014 — 2026-08-30T21:33:04+08:00 — 明确排除 MQ/Command 思路
- **设计树节点：** D-006
- **轮次与依赖：** round 4 / D-001, D-002
- **状态：** superseding
- **问题：** common 与 system 的内部协作是否参考 MQ/Command 分发思想。
- **事实与来源：** 用户明确要求内部不使用该思想，并指出仓库其他同类能力也未采用。
- **选项：** 本地类型化 SPI 与直接服务；CommandBus；MQ/requestCode 分发。
- **推荐：** 只沿用仓库现有 common SPI、system Bean 实现、直接应用服务和 Spring 装配。
- **结论：** 不引入或借鉴 MQ、RocketMQ processor、requestCode、CommandBus 或通用 Command 分发器；common 只通过窄类型化接口调用 system 实现。
- **原因：** 本能力由 Spring MVC 完成路由且只有明确的本地 system 实现，额外分发模型没有真实多态收益。
- **影响工件：** ADR / Spec / Ticket
- **约束或不变量：** 接口按业务能力命名并使用明确 DTO；禁止 `Object` 入参出参和操作码分派。
- **后续：** 无。
- **替代/被替代：** 替代 LOG-007 中“参考 RocketMQ”的研究前提，保留 LOG-007 的 common/system 分层结论。

## LOG-015 — 2026-08-30T21:33:04+08:00 — 每用户单一开放凭据
- **设计树节点：** D-005
- **轮次与依赖：** round 4 / D-001, D-002
- **状态：** superseding
- **问题：** 一个用户可创建多少开放凭据。
- **事实与来源：** 用户明确提出一个用户只能创建一个类似 API key/token 的东西。
- **选项：** 每用户一个；每用户每 Client 一个；每用户多个应用凭据。
- **推荐：** 每用户一个有效 AppKey/AppSecret 凭据。
- **结论：** 用户与超级管理员都不能为同一 owner 创建第二条有效凭据；可重置、禁用、删除，删除后可重新创建。
- **原因：** 凭据代表用户机器调用入口，用户不需要管理多个客户端集成身份。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 数据库唯一约束必须处理并发创建；它不是 Bearer Token，调用仍使用 HMAC 请求签名。
- **后续：** D-016 决定该唯一凭据内部绑定哪个 Client 权限上下文。
- **替代/被替代：** 替代 LOG-006 中按 `userId + clientPk` 可分别持有凭据的数量语义；保留其管理权限与 secret 可见性结论。

## Research: 用户权限与无 Client 调用语义
- Decision / target: 判断“OpenAPI 请求不传 Client，因此按用户全部权限并集授权”是否符合当前系统；目标工件为 `<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/LOG.md</Path>`。
- Scope / version: 2026-08-30 当前 NAMEWTA 登录、角色、菜单、数据权限与定制不变量。
- Stop condition: 已确认调用方 Client 感知与服务端权限解释上下文是两个独立问题，并能给出不破坏隔离的方案。

### R-011
- Claim: 当前角色和菜单查询都要求 `clientPk`，缺少 Client 参数时返回空集合；查询同时约束角色与菜单所属 Client，现有权限链没有“按 userId 汇总全部 Client 权限”的合法回退。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysRoleMapper.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysMenuMapper.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysPermissionServiceImpl.java</Path>`。
- Confidence: high
- Limits: 尚未决定使用专用 OpenAPI Client 还是凭据生成时的业务 Client。
- Artifact impact: D-016、ADR-001、ADR-004。

### R-012
- Claim: 项目定制不变量要求登录、角色、菜单、动态路由与 Token 始终绑定当前 Client，缺少上下文时失败关闭，禁止跨 Client 回退；数据权限的角色快照同样在单一 Client 下构建。
- Type: project rule
- Source: `CODE:<Path>docs/upstream/customization-map.md</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysLoginService.java</Path>`。
- Confidence: high
- Limits: OpenAPI 调用方可以不发送 `clientid`，只要服务端从可信配置或凭据记录确定唯一 `clientPk`。
- Artifact impact: D-016、D-015。

### Recommendation
- `@OpenApi` 只定义接口可被外部认证入口命中，接口原有权限注解继续定义用户能否调用；这部分与用户理解一致。
- 调用方不登录、不传 `clientid`，但服务端仍应从可信配置确定一个专用 OpenAPI `clientPk`，并只查询该 Client 下用户角色、菜单和数据权限。
- 禁止按 userId 汇总全部 Client 权限。否则用户在管理端 Client 的高权限可能泄漏给原本低权限的业务 Client，且数据权限无法形成唯一解释。

### R-013
- Claim: 现有 `clientid` 不仅决定动态路由；普通受保护请求还必须让请求头或参数中的 `clientid` 与 Token extra 一致，并继续执行 Client 访问路径和 IP 白名单规则。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-security/src/main/java/org/dromara/common/security/config/SecurityConfig.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/utils/LoginHelper.java</Path>`。
- Confidence: high
- Limits: 这是普通 Sa-Token 通道的现状，不阻止显式设计一个先验签、再进入的 OpenAPI 独立认证通道。
- Artifact impact: D-007、D-016、D-015。

## LOG-016 — 2026-08-30T22:01:44+08:00 — OpenAPI 全局授权身份
- **设计树节点：** D-002, D-016
- **轮次与依赖：** round 4 / D-002, D-005, D-007, D-008
- **状态：** superseding
- **问题：** 同一后端的多个前端 Client 是否要求 OpenAPI 凭据和权限按 Client 分开。
- **事实与来源：** 用户明确区分前端动态路由场景和直接后端接口调用场景，并提出把 OpenAPI 视为独特客户端，获得该用户所有合法 Client 角色和权限并集；R-011 至 R-013 明确当前实现边界。
- **选项：** 每凭据绑定一个前端 Client；专用 sys_client 重复授权；独立 OpenAPI 全局认证通道。
- **推荐：** 独立 OpenAPI 全局认证通道，不复用或伪装 `sys_client`。
- **结论：** AppKey 只绑定 userId。OpenAPI 请求不发送 `clientid`，逐请求聚合用户在所有持久合法 Client 下的有效角色、权限字符和数据权限；不加载或返回动态路由树。普通 Sa-Token 请求继续使用单 Client 身份和全部既有后端校验。
- **原因：** 多个前端共享同一后端且 Client 主要表达前端登录、路由和请求策略；直接 API 调用不应迫使同一用户维护多套 key 或复制角色配置。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 只有验签成功且目标方法显式标注 `@OpenApi` 的请求可进入全局通道；不能把所有启用 Client 的权限无条件赋给用户，也不能全局关闭普通 Client 校验。
- **后续：** D-018 明确合法 Client、默认角色、状态过滤和数据范围合并；D-019 明确依赖单一 clientPk 的业务接口。
- **替代/被替代：** 替代 LOG-002 的 `userId + clientPk` 凭据绑定结论和本节旧 Recommendation 的专用 Client 建议；保留现有普通登录必须按 Client 失败关闭的事实。

## Research: 复用 LoginUser、Sa-Token Session 与 Redis
- Decision / target: 判断 OpenAPI 是否能复用现有 `LoginUser`、Sa-Token 注解和 Redis TokenSession，并识别缓存授权的失效风险；目标工件为 `<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/LOG.md</Path>`。
- Scope / version: 本仓库 Sa-Token 1.45.0、JWT simple、`LoginHelper`、`PlusSaTokenDao`、角色菜单会话清理实现；Sa-Token v1.45.0 官方源码。
- Stop condition: 已确认机器 Session 的创建/请求注入路径、Redis 持久化方式和现有失效机制覆盖缺口。

### R-014
- Claim: `LoginHelper.login` 将用户/部门/登录域写入 Token extra，并把完整 `LoginUser` 写入当前 TokenSession 的 `loginUser` 键；`SaPermissionImpl` 的权限和角色读取只消费该 Session 中的 `LoginUser`，因此只要 OpenAPI 注入正确内部 Token，现有注解链无需增加第二套权限接口。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/utils/LoginHelper.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/service/SaPermissionImpl.java</Path>`。
- Confidence: high
- Limits: OpenAPI 全局 `LoginUser` 的聚合字段和 Client 相关业务边界仍由 D-018、D-019 决定。
- Artifact impact: D-007、D-020。

### R-015
- Claim: `PlusSaTokenDao` 通过 `RedisUtils` 持久化 Sa-Token 字符串和对象，并使用写后 5 秒过期的 Caffeine 本地缓存；TokenSession 中的 `LoginUser` 因此可跨请求从 Redis 复用，但单纯删除 Redis Session 在多节点上仍可能有至多约 5 秒的本地旧值窗口。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path>`。
- Confidence: high
- Limits: 未运行多节点集成测试；准确可见窗口还受请求落点和 Sa-Token key 读取顺序影响。
- Artifact impact: D-020、D-021、D-015。

### R-016
- Claim: Sa-Token 1.45.0 的 `createLoginSession` 创建会话并返回 Token，但不负责把 Token 注入客户端；`setTokenValueToStorage` 可只写当前请求 Storage。当前项目全局 `is-share=false`，因此并发 cache miss 若无锁会生成多个内部 Token。
- Type: official fact + code fact
- Source: <Url>https://github.com/dromara/Sa-Token/blob/v1.45.0/sa-token-core/src/main/java/cn/dev33/satoken/stp/StpLogic.java</Url>；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>`。
- Confidence: high
- Limits: 实现时必须使用 1.45.0 已编译 API 并通过测试验证 `rightNowCreateTokenSession`、response header 和 JWT simple 行为。
- Artifact impact: D-020、D-015。

### R-017
- Claim: 当前菜单修改通过 `kickoutClient` 清除该 Client 登录会话，角色权限修改也按角色所属 Client 清理，用户角色变化按 userId + clientPk 清理；没有 clientPk 的 OpenAPI 全局 Session 不会被这些 matcher 自动命中。角色 ID 扫描清理可以命中含该角色的快照，但不是所有写路径都使用该方式。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientSessionService.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMenuServiceImpl.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysRoleController.java</Path>`。
- Confidence: high
- Limits: 新的 OpenAPI 失效机制尚待 D-021 定案。
- Artifact impact: D-021、D-015。

### Recommendation
- 不另建 Principal/权限注解体系，复用标准 `LoginUser`、TokenSession、`LoginHelper`、`SaPermissionImpl`、数据权限和脱敏链。
- 首次有效验签或 Session 缺失时，在分布式锁内从 system 权威数据组装全局 `LoginUser`；后续验签后从 Redis Session 复用并只注入当前 request Storage。不要直接用一份平行 `RedisUtils.setCacheObject(LoginUser)` 缓存。
- 缓存 miss 只允许重建已有授权；若目标权限不存在则 403，禁止根据被请求的接口自动把权限补入 Session 或数据库。
- 凭据状态与签名每次校验；权限变更采用事件驱动 Session 注销加 Redis revision 校验，避免多节点本地缓存短暂继续授权。

## LOG-017 — 2026-08-30T22:29:13+08:00 — 复用 Sa-Token 机器会话
- **设计树节点：** D-007, D-020
- **轮次与依赖：** round 4 / D-007, D-016
- **状态：** confirmed-with-follow-up
- **问题：** OpenAPI 是否直接复用现有 LoginUser、权限注解、TokenSession 和 Redis 缓存，以及首次缺失权限时写入什么。
- **事实与来源：** 用户要求尽可能复用现有类型和注解，并设想首次调用写入 Redis、后续通过不同 Session ID 读取；R-014 至 R-017 已确认可行路径和失效缺口。
- **选项：** 独立 Principal/注解；逐请求重建；内部 Sa-Token TokenSession 缓存标准 LoginUser。
- **推荐：** 内部 TokenSession 缓存标准 `LoginUser`。
- **结论：** 每个有效凭据关联一个只在服务端使用的 Sa-Token 机器 Token。首次有效签名请求或 Session 缺失时由 system 组装 OpenAPI 全局 `LoginUser` 并写入现有 TokenSession；后续请求先验签，再从 Redis 复用 Session 并把内部 Token 仅注入当前请求 Storage。缓存缺失触发重建，真实权限缺失仍返回 403，绝不自动授权。
- **原因：** 最大化复用现有 `@SaCheckPermission`、`@SaCheckRole`、`LoginHelper`、数据权限、脱敏、Sa-Token DAO 和 Redis 序列化路径，同时保持数据库授权为唯一事实源。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** AppKey/AppSecret 与内部 Token 分离；内部 Token 不出现在响应、日志或文档；凭据必须每请求验证；并发首调需锁与双重检查。
- **后续：** D-021 确认权限 revision、事件注销和多节点缓存窗口；D-018/D-019 确认聚合与 Client 相关接口边界。
- **替代/被替代：** 细化 LOG-011 中“逐请求刷新 LoginUser”的结论为“逐请求验证凭据，授权快照按 Session 缓存并在 miss/stale 时重建”。

## LOG-018 — 2026-08-30T22:40:36+08:00 — 澄清自动获取权限仅指缓存装载
- **设计树节点：** D-020
- **轮次与依赖：** round 5 / D-007, D-016
- **状态：** superseding
- **问题：** “自动获取权限”是否意味着根据被请求接口自动授予用户权限。
- **事实与来源：** 用户明确指出讨论前提是凭据已经创建，所述自动获取仅指 Redis 缓存机制；用户从未要求自动授予权限。
- **选项：** 缓存未命中后从权威关系装载；按请求接口补写权限；每次都查库且不缓存。
- **推荐：** 标准 cache-aside：命中即复用，miss/stale 才从 system 权威关系装载并回写现有 TokenSession。
- **结论：** Redis 有有效 `LoginUser` 时直接使用；没有或失效时获取该用户已有的角色、菜单权限字符和数据权限，组装标准 `LoginUser` 并写回 Redis。后续始终走同一权限注解链；权威数据不含目标权限时返回 403，绝不申请、创建、补写或授予权限。
- **原因：** Redis 是性能层而非授权事实源；这同时满足复用现有链路和权限变化后的可重建需求。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 只有 system 权威授权关系能改变用户权限；接口路径和 `@OpenApi` 注解不能成为授予依据。
- **后续：** D-021 决定 revision、注销事件和多节点缓存失效窗口。
- **替代/被替代：** 替代 LOG-017 中可能被误读为“首次调用自动写入权限”的措辞，不改变其 TokenSession 复用结论。

## LOG-019 — 2026-08-30T22:40:36+08:00 — 复用优先与上游友好边界
- **设计树节点：** D-022
- **轮次与依赖：** round 5 / D-006, D-020
- **状态：** confirmed
- **问题：** OpenAPI 特殊 Token/Session 组织应通过重写现有登录组件还是增量适配实现。
- **事实与来源：** 用户强调复用性并要求避免深度改造，以降低持续 fork 上游时的冲突；现有 R-014 至 R-017 已证明标准 `LoginUser`、TokenSession、Sa-Token DAO 和 request Storage 可复用。
- **选项：** 重写 LoginHelper/安全主链；独立 Principal 与缓存；新模块薄桥接现有公共 API。
- **推荐：** `ruoyi-common-openapi` 薄会话桥接器 + 窄类型化 system 授权快照端口。
- **结论：** 不重写 `LoginHelper`、`LoginUser`、`SaPermissionImpl`、`PlusSaTokenDao` 和普通登录主链，不另建 Principal、权限注解或平行 Redis 缓存。OpenAPI 专属元数据优先使用 Token extra/TokenSession sidecar；只有公共扩展点不足时才对 satoken/security 做最小通用扩展并登记上游定制映射。
- **原因：** 定制集中在新增模块可以保持安全语义单一，并显著减少上游合并冲突。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 优先组合现有 API；例外扩展需有代码证据、WHY 注释、普通登录回归和定制映射记录；不使用 MQ/CommandBus。
- **后续：** 实施前以 Sa-Token 1.45.0 编译 API 验证确切桥接方法。
- **替代/被替代：** 细化 ADR-008 的 `OpenApiPrincipal` 表述：只使用标准 `LoginUser`，不创建独立 Principal。

## LOG-020 — 2026-08-30T22:40:36+08:00 — 分离 OpenAPI 管理面与调用面
- **设计树节点：** D-005, D-023
- **轮次与依赖：** round 5 / D-005, D-008, D-020
- **状态：** confirmed
- **问题：** 凭据管理和已经创建凭据后的接口调用权限应如何区分。
- **事实与来源：** 用户明确说明当前讨论的权限缓存属于“已经创建 Token 之后”的调用期；管理能力属于 System 中类似“应用开放管理”的菜单。
- **选项：** 单一混合入口；管理面和调用面完全独立重复实现；入口分离但共享领域服务与授权解析器。
- **推荐：** 入口分离、核心能力复用。
- **结论：** `ruoyi-system` 的“应用开放管理”继续使用当前前端 Client 的菜单/按钮权限；超级管理员可管理全部用户凭据并代建、重新创建/重置。开放调用只处理有效凭据，验签后恢复调用用户的 `LoginUser`，再执行目标方法原有权限校验。两类权限互不转换。
- **原因：** 管理动作依赖浏览器会话与菜单治理，第三方调用依赖机器签名与用户接口权限，身份入口不同但事实源相同。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 创建凭据不授予接口权限；有管理权限不代表能调用目标接口；开放凭据不能用于调用管理 API。
- **后续：** 前端暂不在首期范围，但后端合同必须支持未来菜单接入。
- **替代/被替代：** 补充 LOG-015 的凭据所有权结论，并为 LOG-018 限定调用期前提。

## LOG-021 — 2026-08-30T22:40:36+08:00 — 超管按目标用户预览可调用接口
- **设计树节点：** D-009, D-024
- **轮次与依赖：** round 5 / D-009, D-018, D-023
- **状态：** confirmed-with-follow-up
- **问题：** 超级管理员点开一个用户时如何准确展示该用户可调用的 OpenAPI 接口。
- **事实与来源：** 用户要求超级管理员可查看目标用户拥有的 OpenAPI 接口权限；D-009 已决定目录基于真实映射和 SpringDoc。
- **选项：** 按管理员会话过滤；管理端单独查询；显式 targetUserId 复用运行时解析器与注册表。
- **推荐：** 共享解析器与注册表的只读目标用户预览。
- **结论：** 管理 API 校验查看权限后，以 `targetUserId` 调用运行时同一授权快照解析器，再用同一开放接口注册表和权限匹配函数计算目录；不使用管理员自身接口权限，不伪造目标用户登录，也不创建机器 TokenSession。
- **原因：** 只有一个解析与过滤口径，才能保证页面所见和第三方真实调用结果一致，并避免管理查询产生登录副作用。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 预览是只读操作；目标用户是否已有凭据不改变其理论可调用目录，凭据状态只影响能否实际发起调用。
- **后续：** D-018 仍需明确跨 Client 聚合与数据范围合并规则。
- **替代/被替代：** 扩展 LOG-008 的当前用户目录语义，增加超级管理员按目标用户预览。

## LOG-022 — 2026-08-31T00:19:25+08:00 — 管理页与个人开放应用 Tab
- **设计树节点：** D-013, D-023, D-025
- **轮次与依赖：** round 6 / D-005, D-009, D-013, D-023, D-024
- **状态：** confirmed
- **问题：** 管理员全量管理和用户本人创建密钥、查看授权接口与文档分别位于哪个前端入口。
- **事实与来源：** 用户明确指定管理配置页面位于“系统管理”菜单下，个人能力位于用户个人信息中的“开放应用”Tab。当前 admin-web 个人信息源码已经使用同级 Tabs 组织基本资料、修改密码、第三方应用和在线设备。
- **选项：** 全部放系统管理；全部放个人中心；系统管理管理员页 + 个人信息“开放应用”Tab。
- **推荐：** 双入口、单领域能力、显式 owner scope。
- **结论：** “系统管理 > 应用开放管理”用于超级管理员管理所有用户；“个人中心/个人信息 > 开放应用”Tab 用于当前用户管理自己的唯一凭据，并查看自己的可调用接口、详情和 OpenAPI 调用文档。个人入口是现有个人信息页同级 Tab，不新增动态菜单路由。
- **原因：** 管理员与本人自助的 owner scope 不同，但凭据状态、接口目录和文档能力相同；复用同一 domain/web-domain 可避免两套前端实现和授权口径。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** Tab 与命令受既有菜单/按钮权限控制，前端隐藏不是安全边界；后端独立校验 current user/target user scope。首期仍不交付前端页面。
- **后续：** 未来独立前端 change 决定组件拆分和交互细节，本 change 的后端 API 应支持这两个稳定消费场景。
- **替代/被替代：** 补充 LOG-005 的“首期不做前端”，不是推翻；细化 LOG-020 的管理面为两个浏览器入口。

## Research: 2026-08-31 最新前后端实现对齐
- Decision / target: 在生成 Spec 前，以已同步的父仓、前端和后端 HEAD 复核模块边界、安全缓存、日志与未来 UI 落位；目标工件为本 change 的 `spec.md`、`ADR.md`、`CONTEXT.md` 和设计树。
- Scope / version: 父仓 `6de6884`、`plus-ui-namewta` `381918e`、`ruoyi-vue-plus-namewta` `e5cef5a61`。
- Stop condition: 已识别会改变原设计事实、复用约束或验收接缝的最新实现，并保留仍需用户决策的高影响问题。

### R-018
- Claim: 标准 `LoginUser` 已位于 `ruoyi-api`，`common-satoken` 已直接依赖 `ruoyi-api`；`LoginHelper` 和 `SaPermissionImpl` 都消费这一类型，且权限读取只信任当前 TokenSession，不存在按 userId 的全局回退。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/model/LoginUser.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/pom.xml</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/utils/LoginHelper.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/service/SaPermissionImpl.java</Path>`。
- Confidence: high
- Limits: `SysLoginService.buildLoginUser` 仍位于 `ruoyi-admin` 且是单 Client 组装器，不能被 system/common 反向依赖，也不能直接表达全局并集。
- Artifact impact: D-006、D-020、D-022、ADR-014、Spec REUSE。

### R-019
- Claim: `PlusSaTokenDao` 的写入和删除已统一经过 `ClusterCacheInvalidationCoordinator`；协调器用 key 指纹广播、等待全部订阅节点确认并失效各节点 Caffeine，因此旧 R-015 所述“只删 Redis 后可能继续读 5 秒本地旧值”已不符合当前 HEAD。
- Type: code fact
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/cache/ClusterCacheInvalidationCoordinator.java</Path>`。
- Confidence: high
- Limits: 现有 `ClientSessionService` 只按 userType/client/user+client 匹配；OpenAPI 全局 Session 仍需一个按用户或机器通道的窄失效入口，并由所有相关授权写路径调用。
- Artifact impact: D-021、ADR-009、ADR-016、Spec AC/NFR。

### R-020
- Claim: 当前 `common-web` HTTP 日志对请求和响应每方向默认最多采集 1 MiB，敏感头和 JSON 字段递归替换为 `[REDACTED]`；非法或截断 JSON 整体隐藏，业务正文不被修改。最新后端提交已明确禁止原始密码、Token、secret、API key、Cookie 和 Session 标识进入 HTTP 日志。
- Type: code fact + accepted current behavior
- Source: `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/config/SysLogProperties.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/SysLogFilter.java</Path>`；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/SysLogBodySanitizer.java</Path>`；`CHANGE:<Path>speculo/.speculo/specdev/changes/2026-08-29-login-password-policy-runtime-and-http-log-redaction/spec.md</Path>`。
- Confidence: high
- Limits: OpenAPI 新增的 AppKey/签名头必须纳入敏感头集合或在 OpenAPI 接缝先行隐藏；multipart、二进制和流式正文不适合正文持久化。
- Artifact impact: D-012、D-017、ADR-015、Spec NFR-001/NFR-004。

### R-021
- Claim: 当前前端 admin App 显式组合 `system` domain/web-domain，`gen` 已退出选择集合；系统动态页面由 system web-domain manifest 承接，而个人信息页是 App 自有静态页并已使用同级 `el-tabs` 组织个人能力。
- Type: code fact + architecture rule
- Source: `CODE:<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>`；`CODE:<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/index.vue</Path>`；`CODE:<Path>plus-ui-namewta/packages/domains/system/README.md</Path>`；`CODE:<Path>plus-ui-namewta/packages/web-domains/system/README.md</Path>`；`ADR:<Path>speculo/.speculo/specdev/adr/0013-cross-terminal-domain-and-web-domain-separation.md</Path>`；`ADR:<Path>speculo/.speculo/specdev/adr/0021-manifest-only-dynamic-page-resolution.md</Path>`。
- Confidence: high
- Limits: 前端实现仍不属于本 change 首期范围；这里只约束后端合同的两个未来消费者和未来落位。
- Artifact impact: D-013、D-025、ADR-013、ADR-014、Spec IN/OUT。

## LOG-023 — 2026-08-31T00:35:01+08:00 — 以最新源码重新锚定复用边界
- **设计树节点：** D-006, D-020, D-022, D-025
- **轮次与依赖：** round 7 / D-006, D-013, D-020, D-022, D-025
- **状态：** confirmed
- **问题：** 远程同步后，原设计中的身份类型、模块依赖与未来前端路径是否仍准确。
- **事实与来源：** R-018、R-021。
- **结论：** OpenAPI 全局身份必须返回并缓存 `ruoyi-api` 已有 `LoginUser`；common-openapi 组合 common-satoken 与窄 system SPI，不复制身份模型，也不反向调用 admin 的单 Client 登录组装器。未来系统管理动态页归 system web-domain，个人“开放应用”Tab 由 App 静态个人信息壳组合同一 system web-domain 能力；不得恢复或依赖 gen。
- **原因：** 这些边界与当前 Maven/前端组合完全一致，能最大限度复用现有安全链并降低 fork 上游冲突。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** `LoginUser.getLoginId()` 需要非空 userType；机器身份必须使用与浏览器会话不碰撞的内部登录命名空间，且不得伪装某个前端 Client。

## LOG-024 — 2026-08-31T00:35:01+08:00 — OpenAPI 正文审计服从最新脱敏边界
- **设计树节点：** D-012, D-017
- **轮次与依赖：** round 7 / D-012
- **状态：** confirmed
- **问题：** “记录完整请求与响应正文”是否允许原始凭据、无限正文或流式内容落日志。
- **事实与来源：** R-020；最新已交付安全行为已明确禁止凭据原值日志。
- **结论：** 完整正文解释为复用现有 HTTP 日志的有界、可安全记录 JSON：默认每方向 1 MiB，JSON 递归脱敏，非法或截断 JSON 失败关闭；密码、Token、Cookie、AppKey、AppSecret、签名和内部机器 Token 永不记录原值；非 JSON 文本、multipart、二进制、文件、音视频、SSE 和其他流式正文只记元数据。OpenAPI 调用事件只记录计量/审计元数据，不复制原始正文。
- **原因：** 保留排障价值的同时遵守当前源码已实现的凭据保密边界，避免两套正文采集与脱敏规则漂移。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 日志或审计写入失败不得改变业务响应；新增签名头必须进入统一敏感头策略。
- **替代/被替代：** 关闭 D-017；对本 change 而言，取代任何可被理解为“原样记录凭据正文”的旧文档表述。

## LOG-025 — 2026-08-31T00:35:01+08:00 — 复用现有集群确认完成机器会话失效
- **设计树节点：** D-021
- **轮次与依赖：** round 7 / D-018, D-020
- **状态：** confirmed
- **问题：** 是否还需要额外的 Redis 授权 revision 来弥补多节点本地缓存窗口。
- **事实与来源：** R-019；当前 Sa-Token DAO 删除已具备集群确认和本地缓存精确失效。
- **结论：** 不新增平行授权 revision 缓存。复用现有会话注销与 Sa-Token DAO 集群失效：OpenAPI 为机器通道提供按 userId 精确注销的窄入口，凭据状态或 system 权威授权变化的既有写路径在写事务内完成权威变更后、提交成功响应前调用它；注销失败向写操作传播并可重试。下一次有效验签遇到 Session miss 后从权威关系重建。
- **原因：** 当前基础设施已解决旧的本地缓存窗口，额外 revision 会形成第二套状态和每请求额外读；真正缺口只是全局机器 Session 的匹配与调用覆盖。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 禁止直接删 Redis key；禁用、删除、重置凭据先使旧机器 Session 失效；所有能改变全局快照的写路径必须纳入失效矩阵。
- **替代/被替代：** 替代 R-015、R-017 Recommendation 中基于旧 HEAD 的 5 秒窗口与 revision 兜底建议；不改变 cache-aside 语义。

## LOG-026 — 2026-08-31T00:35:01+08:00 — 生成同步当前实现的 Spec 草案
- **设计树节点：** D-015
- **轮次与依赖：** round 7 / all answered and open nodes
- **状态：** draft
- **问题：** 当前决策是否足以形成可核验规格，以及是否已经能安全拆 Ticket。
- **结论：** 已生成覆盖后端首期范围、外部行为、复用边界、双未来 UI 消费者、数据与安全合同的 `spec.md`；因 D-011、D-018、D-019 仍会改变公开协议或授权语义，规格保持 `status: draft`、`ready_for_tickets: false`。
- **影响工件：** Spec / `.status.json` / global status
- **后续：** 返回 Grill 只确认三项高影响问题，再更新验收矩阵并进入 Ticket。

## LOG-027 — 2026-08-31T09:20:55+08:00 — 防重放、重试与两级限流
- **设计树节点：** D-011
- **轮次与依赖：** round 8 / D-003, D-010
- **状态：** confirmed
- **问题：** OpenAPI v1 是否提供通用业务幂等与响应重放，以及如何限制单凭据和单接口流量。
- **事实与来源：** 用户接受推荐 A。现有 `@RepeatSubmit` 使用普通 Sa-Token 请求头和方法参数摘要构造 key，不适合直接成为 OpenAPI 协议；现有 `RateLimiterAspect` 已提供 Redis/Redisson 令牌能力。
- **选项：** nonce + 两级限流且业务自有幂等；再增加通用 Idempotency-Key/JSON 响应重放；只做 nonce。
- **推荐：** nonce + 两级限流，首期不做通用响应重放。
- **结论：** Redis 原子登记 `AppKey + nonce`，接受时间窗口和 nonce TTL 默认 60 秒；重试必须生成新的 timestamp、nonce 和签名。首期不实现框架级 `Idempotency-Key` 或响应重放，变更接口的幂等由接口自身显式定义。限流同时执行每 AppKey 1000 次/分钟和每 AppKey + 开放接口 100 次/分钟，阈值可配置；Redis 不可用时失败关闭。
- **原因：** nonce 解决认证重放，不应冒充业务幂等；通用响应缓存无法安全覆盖任意事务、文件、流式和超大响应。两级限流可直接复用现有 Redis 能力并同时约束凭据总量与热点接口。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** GET 等天然幂等请求可用新签名重试；写请求在未知执行结果后是否可重试由目标接口文档决定。nonce key 和日志不得包含原始 secret 或签名。
- **后续：** Spec 固定 canonicalization、限流错误行为和测试向量；Ticket 复用 Redis 能力而不复用普通 Token 型 `@RepeatSubmit` key。
- **替代/被替代：** 无。

## LOG-028 — 2026-08-31T09:20:55+08:00 — 合法 Client 与全局权限聚合
- **设计树节点：** D-018
- **轮次与依赖：** round 8 / D-016
- **状态：** confirmed
- **问题：** 哪些 Client、默认/显式角色、菜单权限和数据权限进入 OpenAPI 全局 `LoginUser`。
- **事实与来源：** 用户接受推荐 A。当前 `ClientUserTypeAccessService` 以活动 Client、活动登录域和用户登录域关系判定登录资格；`SysRoleServiceImpl` 会把 Client 默认角色隐式并入登录角色；`getDataScopeRoleMap` 按权限字符汇总实际角色 ID。
- **选项：** 完整复用活动登录资格并聚合有效默认/显式授权；忽略登录域关系；只聚合显式 `sys_user_role`。
- **推荐：** 只聚合用户确实可以合法登录的活动 Client，并包含其有效默认角色。
- **结论：** 合法 Client 必须正常、已配置正常登录域且用户持有该登录域关系。每个合法 Client 纳入正常默认角色和用户显式分配的正常角色，只纳入正常菜单权限；角色字符和权限字符去重。`dataScopeRoleMap` 按权限汇总所有实际生效角色 ID，由现有数据权限链联合计算。超管继续使用 `superadmin` 与 `*:*:*`，在线 Session 不作为授权事实。
- **原因：** 这与用户在每个前端 Client 中真正拥有的当前授权一致，既不会因“Client 已启用”就无条件授予默认权限，也不会漏掉登录时自动生效的默认角色。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 禁用用户、Client、登录域、角色或菜单不进入新快照；相关状态变化必须触发 ADR-016 的机器 Session 注销。不能复用当前缺少菜单状态过滤的查询而静默扩大 OpenAPI 权限。
- **后续：** Spec 增加完整聚合矩阵和数据范围联合验证。
- **替代/被替代：** 关闭 LOG-016、LOG-021 中保留的聚合细节后续项。

## LOG-029 — 2026-08-31T09:20:55+08:00 — 仅开放 Client 无关方法
- **设计树节点：** D-019
- **轮次与依赖：** round 8 / D-004, D-016
- **状态：** confirmed
- **问题：** OpenAPI 全局身份遇到必须读取单一 `clientPk` 的下游能力时是否允许调用方选择或服务端推断 Client。
- **事实与来源：** 用户接受推荐 A。当前 OSS 上传身份、上传票据和策略把 `clientPk` 作为授权组成，其他普通管理路径也直接读取当前 Client；全局 OpenAPI 身份没有唯一 Client 可安全填充。
- **选项：** 首期禁止 Client 相关方法；签名携带目标 Client；服务端按权限命中推断 Client。
- **推荐：** 首期只开放 Client 无关方法。
- **结论：** OpenAPI `LoginUser.clientPk/clientKey` 保持为空；协议不接受 Client，服务端也不推断。必须读取单一 Client 的下游能力失败关闭且不得使用 `@OpenApi`。OSS 上传、Client 级路径/IP/业务策略等能力只有先在后续 change 形成明确 Client 无关合同，才可开放。
- **原因：** 选择或推断 Client 都会重新引入已拒绝的 Client 绑定，且同一权限存在于多个 Client 时没有确定、安全的推断结果。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 不修改普通 Client 请求行为；OpenAPI 入口不得以默认值伪装某个 Client。代码评审与接口级测试必须证明首批标注方法不读取 Client 上下文。
- **后续：** Spec 将 Client 相关能力列为稳定失败边界，并为首批开放接口建立负向测试。
- **替代/被替代：** 关闭 ADR-008 中保留的 Client 相关接口后续项。

### R-022
- Claim: 当前 `admin-web` 已显式组合 system domain/web-domain；系统动态页面由 system manifest 注册，个人信息页则是 App 自有静态页并已有同级 `el-tabs`。因此 OpenAPI 管理页应新增 system domain/web-domain 资源，个人“开放应用”入口应由静态个人信息壳组合该 Web 领域的共享组件。
- Type: code fact + architecture rule
- Source: `CODE:<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>`；`CODE:<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/index.vue</Path>`；`CODE:<Path>plus-ui-namewta/packages/domains/system/README.md</Path>`；`CODE:<Path>plus-ui-namewta/packages/web-domains/system/README.md</Path>`；`SKILL:<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`。
- Confidence: high
- Limits: App 静态壳只负责入口组合，不得拥有或复制 OpenAPI transport、领域状态、owner-scope 规则和权限过滤。
- Artifact impact: D-001、D-013、D-015、D-025、ADR-020、ADR-021、Spec IN/OUT 与 Ticket。

## LOG-030 — 2026-08-31T09:46:39+08:00 — 首期范围修正为完整前后端平台
- **设计树节点：** D-001
- **轮次与依赖：** round 9 / 无
- **状态：** superseded-and-confirmed
- **问题：** 首期是否只交付后端，还是同时交付已经明确的管理端和个人入口。
- **事实与来源：** 用户纠正此前范围，明确 `plus-ui-namewta/apps/admin-web` 需要增加个人部分和管理部分；R-022 确认当前组合与落位。
- **结论：** 首期改为交付完整前后端开放平台：后端 common/system/admin 装配与前端两个入口均在本 change 范围内。
- **原因：** 管理页和个人 Tab 不是未来占位，而是本次功能完整性的组成部分。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 前端加入范围不改变后端最终授权、每用户唯一凭据、全局机器身份和复用现有权限链等已确认合同。
- **替代/被替代：** 替代 LOG-001 中“首期不交付前端”的结论；LOG-001 保留为讨论历史。

## LOG-031 — 2026-08-31T09:46:39+08:00 — 首期交付两个前端入口
- **设计树节点：** D-013
- **轮次与依赖：** round 9 / D-001, D-005, D-009
- **状态：** superseded-and-confirmed
- **问题：** 本 change 中前端管理能力实际落在哪些入口和模块。
- **事实与来源：** 用户明确两个前端部分都需要实现；R-022。
- **结论：** system domain/web-domain 新增 OpenAPI 领域合同、服务、共享组件和动态管理页；`admin-web` 通过 system manifest 组合“系统管理 > 应用开放管理”，并在 App 自有个人信息静态页增加“开放应用”Tab 来组合共享组件。
- **原因：** 这种落位遵守现有多 App 依赖方向，同时让 target-user 与 current-user 两种 owner scope 共享一份业务实现。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 不在 `apps/admin-web/src/api` 复制接口，不让 App 拥有 OpenAPI 领域状态，不把个人 Tab 改成动态菜单路由。
- **替代/被替代：** 替代 LOG-005 中“首期不交付任何前端页面”的结论。

## LOG-032 — 2026-08-31T09:46:39+08:00 — 双入口从未来消费者改为首期交付
- **设计树节点：** D-025
- **轮次与依赖：** round 9 / D-005, D-009, D-013, D-023, D-024
- **状态：** superseded-and-confirmed
- **问题：** 已确认的双入口信息架构是否仍只约束未来 change。
- **事实与来源：** 用户明确本 change 需要实现这两个前端部分；R-022。
- **结论：** “系统管理 > 应用开放管理”和个人信息“开放应用”Tab 均为首期交付项；原有双入口、单能力、显式 owner scope、既有菜单/按钮权限和后端独立鉴权合同不变。
- **原因：** 只改变交付时点，不改变经过确认的信息架构和复用边界。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 管理页不得以管理员权限替代目标用户目录；个人入口不得选择其他用户；`gen` 不参与。
- **替代/被替代：** 替代 LOG-022、LOG-023 中把前端描述为“未来实现”的范围限定，不替代其双入口设计。

## LOG-033 — 2026-08-31T09:46:39+08:00 — 采用含前端验证的轻量模块级门禁
- **设计树节点：** D-015
- **轮次与依赖：** round 9 / all behavioral and architecture nodes
- **状态：** confirmed
- **问题：** 首期上线候选采用严格真实基础设施矩阵，还是轻量模块级门禁；前端纳入范围后如何验证。
- **事实与来源：** 用户选择 B，并明确本 change 包含 `admin-web` 的管理页和个人 Tab。
- **选项：** 严格真实 MySQL/Redis/多进程后端矩阵；轻量单元/模块门禁；无前端改动时仍执行全前端门禁。
- **推荐：** 原推荐为严格后端门禁；因范围修正，前端至少应执行受影响包门禁。
- **结论：** 后端采用单元/模块测试和测试替身，不把真实 MySQL、真实 Redis、多进程集群或全量 E2E 作为首期强制发布条件；仍覆盖签名、授权聚合、nonce/限流、凭据生命周期、接口目录、失败关闭和普通登录回归。前端强制执行 `pnpm architecture:check`，并对 `@namewta/domain-system`、`@namewta/web-domain-system`、`@namewta/admin-web` 执行 lint、test、typecheck 和 build，以聚焦组件/集成测试覆盖双入口、owner scope、权限失败关闭和个人信息页回归。
- **原因：** 尊重用户选择的较低发布成本，同时避免前端已进入范围却完全没有自动化门禁。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 测试替身不能改变 Redis 故障失败关闭、唯一凭据并发收敛和权限拒绝的合同；真实基础设施与多节点风险必须在 Spec/Ticket 中显式记录为残余风险，而不能宣称已被验证。
- **后续：** S-spec 将验收矩阵和 IN/OUT 范围同步到最新结论，再进入 Ticket 拆分。
