# Change Architecture Decisions

## ADR-001: 在现有通知数据面之上新增统一通知控制面

**Status:** accepted
**Source:** LOG-001, LOG-002, LOG-004, user decision
**Supersedes:** none

### Context
仓库已经拥有渠道无关的 common-notify、SMS4J 和 SMTP Adapter、Redis 幂等、OSS 附件快照、投递事件、日志表和监控页面。引入 Austin 或在新模块中复制 Dispatcher 会形成第二套请求模型、渠道配置、日志和安全边界，但现有能力又缺少公司级应用、账号、模板、路由、配额、统一 HTTP API 和统计治理。

### Decision
保留现有 common-notify 及 Mail/SMS Adapter 作为通知数据面，在新的业务模块中建设统一通知控制面。控制面拥有通知应用、渠道账号、逻辑模板、供应商模板绑定、路由、配额、对外 API 和统计，并通过稳定端口调用现有数据面。

common-notify 不反向依赖控制面、业务 Mapper、system 实体或用户实现。新模块不 fork Austin，也不创建第二套发送内核和投递日志。

### Trade-off
fork Austin 能获得另一套较完整的消息平台能力，但会带来技术栈整合、双内核迁移和长期同步成本；把管理状态直接放进 common-notify 文件更少，却会破坏薄契约层和依赖方向。控制面/数据面分离增加了端口和模块装配工作，换取对现有调用方兼容及清晰的长期边界。

### Consequences
平台能力必须围绕现有 NotifyClient、Channel SPI 和投递事件演进。动态配置可能需要为 common-sms/common-mail 增加窄运行时 SPI，但数据库实体和管理 Service 仍归控制面。可靠异步、多渠道编排和回执不能假装已由当前同步 Dispatcher 或 best-effort 监控提供。

### Verification / Migration
架构测试应证明 common 模块不依赖 ruoyi-notify 或 ruoyi-system 实现；现有 Captcha、Workflow 和 Demo 调用继续编译运行；新平台发送最终只经过一个 Dispatcher 和一套投递日志。

## ADR-002: 首期采用 ruoyi-notify 模块化单体

**Status:** superseded
**Source:** LOG-003, LOG-006, LOG-007, user decision
**Supersedes:** none

### Context
父工作区包含独立的后端和前端子仓，真正的 Maven 业务模块位于 `ruoyi-vue-plus-namewta/ruoyi-modules`。当前后端由 ruoyi-admin 组装为一个进程，认证、RBAC、数据库、Redis、OSS、OpenAPI 和监控都在该运行时中。立即拆分独立通知服务会引入网络调用、独立部署、数据一致性和运维边界，而当前尚无独立扩缩容或发布隔离证据。

### Decision
首期在 `ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify` 新增业务模块，并由 ruoyi-admin 组装到现有进程。前端对应新增 `packages/domains/notify` 与 `packages/web-domains/notify`，由 admin-web 通过 manifest 显式选择。

公告、站内信、收件箱、投递链路和监控均由 `ruoyi-notify` 统一拥有；System 不再保留通知实现、菜单、接口或通知表。旧数据在初始化时迁移到 Notify 后删除旧表，MQTT 仍属于设备命令域。

### Trade-off
独立服务可以提供进程级故障隔离和独立扩缩容，但现在会重复基础设施并扩大分布式复杂度；继续把所有能力放入 ruoyi-system 变更更少，却会让通知平台缺少自己的业务边界。新的业务模块增加一个 Maven 和前端领域边界，换取清晰所有权与未来可拆分性。

### Consequences
首期通知平台与 ruoyi-admin 同生命周期、共享数据库和 Redis，不能宣称微服务级隔离。模块不得直接依赖 ruoyi-system 内部 Mapper、Entity 或 Service；跨模块能力使用 ruoyi-api 或明确 SPI。只有独立容量、SLA、团队发布节奏或故障域成为真实瓶颈时才重新评估独立部署。

### Verification / Migration
后端全量和 core bundle 必须验证模块组装；前端架构检查必须验证 Domain/Web Domain 依赖和 manifest 注册；通知 URL、权限和前端路由统一切换到 `/notify/*`，旧入口不保留兼容。

## ADR-003: 统一发送 API 复用 NAMEWTA OpenAPI 机器认证边界

**Status:** accepted
**Source:** LOG-005, user decision
**Supersedes:** none

### Context
公司其他系统需要通过稳定 HTTP API 调用通知平台。仓库已经提供显式接口暴露、HMAC 请求签名、时间窗、nonce 防重放、机器会话、权限匹配和 Redis 分布式限流。再建设一套通知专用 appKey、secret 和签名协议会重复高风险安全代码；直接使用浏览器登录 Token 又不适合服务间调用。

### Decision
统一通知 API 使用现有 NAMEWTA OpenAPI 作为机器认证和接口暴露边界。请求认证后，ruoyi-notify 再根据通知应用执行允许模板、允许渠道、配额、频控和数据可见性授权。

OpenAPI credential 只证明机器身份，不自动等于通知应用、OAuth Client、Platform Client 或 Provider Account；这些概念不得通过同名字段隐式混用。

### Trade-off
专用凭据协议可以完全围绕通知应用设计，但会重复密钥存储、轮换、签名、防重放和限流；复用现有 OpenAPI 降低实现和审计成本，代价是必须设计 credential 与 notify_app 的显式绑定，并遵守现有权限会话语义。

### Consequences
发送接口必须显式标注为 OpenAPI 操作并具有最小权限。通知应用停用、模板授权或配额拒绝发生在 Provider 调用前。现有 OpenAPI 的全局限流不能替代通知领域的应用配额和目标频控。

### Verification / Migration
验收应覆盖签名错误、过期时间戳、nonce 重放、无接口权限、未绑定或停用通知应用、模板越权、渠道越权和配额耗尽，且所有拒绝均不得调用 Provider。

## ADR-004: 平台提供同步快速发送与可靠异步发送双模式

**Status:** accepted
**Source:** LOG-009, user decision
**Supersedes:** none

### Context
现有 NotifyDispatcher 同步调用 Provider，适合验证码等需要立即确认是否被 Provider 接受的场景，但调用线程承担 Provider 时延。普通业务通知还需要削峰、失败恢复和可查询的持久任务状态。现有异步 Listener 仅用于 best-effort 监控，应用宕机窗口允许丢失，不能作为可靠发送队列。

### Decision
统一通知平台提供两种显式执行模式：同步快速发送在请求线程完成一次路由和 Provider 尝试并返回结果；可靠异步发送先持久化受理任务，再由 Outbox 或 durable queue 驱动投递、恢复和受控重试。

异步受理成功不表示 Provider 已接受或最终送达。Spring `@Async` 通知监控事件不得承担可靠任务职责，两种模式最终复用同一数据面 Dispatcher 和 attempt 记录语义。

### Trade-off
只保留同步模式实现最简单，但无法可靠削峰和恢复；全部异步可以统一后台执行，却增加验证码等低延迟流程的轮询和状态复杂度。双模式增加 API、状态机和测试矩阵，换取对实时与可靠业务的明确支持。

### Consequences
后续必须分别定义同步响应、异步受理、状态查询、幂等和超时语义。可靠任务需要持久状态、领取租约、重试策略和停机恢复；HTTP 超时不能被调用方直接解释为“未发送”。

### Verification / Migration
验收必须证明：同步调用返回真实 attempt 结果；异步受理提交与任务持久化一致；进程在领取前后宕机能够恢复；重复消费不产生不受控重复发送；best-effort 监控失败不改变任务事实。

## ADR-005: notify_app 是通知领域的一等调用身份

**Status:** accepted
**Source:** LOG-010, user decision
**Supersedes:** none

### Context
OpenAPI credential 负责机器认证并映射到现有授权身份，但不表达某个业务系统可以发送哪些模板、使用哪些渠道或消费多少配额。OAuth Client 和 Platform Client 服务登录域与权限求值，Provider Account 则代表供应商配置；复用任一概念都会混淆认证、通知授权和供应商路由。

### Decision
新增一等 `notify_app` 作为通知领域调用身份。它显式绑定一个或多个经验证的 OpenAPI credential，并拥有启停状态、允许模板、允许渠道、配额和审计标签。OpenAPI 完成机器认证后，平台必须解析唯一有效的通知应用再执行领域授权。

notify_app 不是 tenant，不自动形成其他业务数据隔离，也不替代 OpenAPI credential、OAuth Client、Platform Client 或 Provider Account。

### Trade-off
直接使用 credential 或用户可以少一张表和绑定流程，但会把通知授权生命周期绑到人或密钥；独立应用模型增加管理和一致性约束，换取稳定业务身份、密钥轮换解耦和准确配额审计。

### Consequences
必须锁定 credential 绑定基数、应用停用行为、密钥轮换、管理员可见范围和历史审计。所有发送日志和统计需要记录稳定 notify_app 标识，而不只记录当前 `client_pk`。

### Verification / Migration
验收覆盖未绑定、重复绑定、应用停用、credential 轮换、模板/渠道越权及配额隔离；任何应用解析或授权失败均不得调用 Provider。

## ADR-006: 公共发送合同采用逻辑模板并由平台选择供应商

**Status:** accepted
**Source:** LOG-011, LOG-012, user decision
**Supersedes:** none

### Context
如果普通业务系统直接提交阿里云/腾讯云模板编号、任意正文或 providerKey，它仍然了解供应商，平台无法透明迁移、统一审批、准确统计或自动故障切换。完全禁止原始正文又会阻断少量受控内部通知场景。

### Decision
普通统一发送合同只接受稳定逻辑模板编码、目标和已声明参数，由平台解析渠道内容版本、供应商模板绑定和渠道账号。普通调用方不能指定 providerKey。

原始内容发送和定向 providerKey 是独立高权限运维或内部能力，不属于普通应用默认权限，并且不能绕过配额、频控、敏感审计和投递记录。

### Trade-off
透传 Provider 参数最灵活且改造调用方较少，却持续制造供应商耦合；模板优先需要模板生命周期和参数 schema，换取集中治理、供应商迁移与可验证合同。保留受控原文能力降低特殊场景阻力，但扩大权限审计面。

### Consequences
平台需要逻辑模板、渠道版本、供应商绑定和发布快照。底层 NotifyRequest 的 providerKey 接缝可以保留给路由结果和运维测试，但不得直接映射为普通外部 API 字段。

### Verification / Migration
验收覆盖未知模板、参数缺失/多余、模板未发布、供应商切换而调用请求不变化、普通应用提交原文或 providerKey 被拒绝，以及高权限调用仍完整记录治理信息。

## ADR-007: 生产渠道配置加密持久化并原子热发布

**Status:** accepted
**Source:** LOG-013, user decision
**Supersedes:** none

### Context
当前 SMS4J YAML 可以配置多个供应商账号，但修改需要配置发布和应用重启，不满足平台集中迁移目标。数据库直接保存明文会扩大泄露面；发送线程逐字段读取正在编辑的数据则可能观察到半发布状态。

### Decision
生产渠道账号在数据库集中管理，秘密字段使用项目批准的 KEK/AES-GCM 模式加密，管理端创建后不回显明文。编辑数据只有通过完整校验和发布动作后才形成不可变运行时配置快照；多实例原子切换到新版本，任何校验或传播失败都继续使用上一可用版本。

YAML 只用于本地开发或引导配置，不是生产账号的并行权威来源。

### Trade-off
YAML 加重启简单可靠但无法提供在线集中治理；数据库热发布提升运维效率，却需要版本、缓存传播、并发控制、密钥轮换和失败回退。选择原子快照增加实现复杂度，以避免发送线程使用半更新凭据。

### Consequences
后续必须决定草稿/发布版本、审批权限、多实例传播确认、密钥轮换和运行时 Adapter 刷新 SPI。HTTP/业务日志、异常和管理响应不得包含 secret。

### Verification / Migration
验收覆盖正确发布、多实例收敛、并发发布冲突、无效凭据校验失败、发布中断、旧快照回退、密钥轮换和日志扫描；发布失败期间现有发送能力必须保持可用。

## ADR-008: 路由使用版本快照且仅对可重试错误故障切换

**Status:** accepted
**Source:** LOG-014, user decision
**Supersedes:** none

### Context
平台需要在多个阿里云、腾讯云等渠道账号之间迁移、灰度和容灾。如果每次 attempt 都读取最新规则，同一通知的执行无法解释；如果所有失败都切换供应商，模板错误、签名拒绝或非法号码会造成重复费用和重复通知。

### Decision
平台按通知应用、逻辑模板、渠道和目标区域匹配有版本的路由规则，并在通知开始执行时固定路由快照。每个 Provider 调用形成独立 attempt；只有错误分类明确为可重试时才允许进入快照中的备用渠道账号，未知或确定性业务错误默认不切换。

### Trade-off
总是使用默认账号最简单但不支持灰度和容灾；无条件轮询供应商提高表面成功率却扩大重复风险。版本快照和错误分类增加状态与适配工作，换取可审计决策和受控故障切换。

### Consequences
路由配置、健康状态和错误分类需要版本化；日志必须保留命中规则、候选顺序和所有 attempt。配置发布不会改变已经受理通知的选择解释。

### Verification / Migration
验收覆盖规则优先级、灰度稳定性、配置切换中的在途请求、可重试错误切换、确定性错误不切换、最大 attempt 以及每次尝试完整留痕。

## ADR-009: 模板使用不可变发布版本和供应商绑定

**Status:** accepted
**Source:** LOG-017, user decision
**Supersedes:** none

### Context
逻辑业务场景需要跨短信、邮件以及不同供应商保持稳定，但渠道内容、参数和供应商模板编号会独立变化。就地修改已发布模板会让历史投递无法证明发送时使用的内容，也无法安全回滚。

### Decision
逻辑模板使用稳定业务编码。每个渠道内容以不可变版本经历草稿、发布和停用生命周期；发布版本包含参数 schema，并通过供应商模板绑定映射渠道账号下的 Provider 模板编码、签名和参数。发送只读取已发布快照。

### Trade-off
就地编辑记录和直接暴露 Provider 编号实现更少，但牺牲历史审计、审批和供应商迁移。不可变版本增加表和发布流程，换取确定性发送与回滚能力。

### Consequences
模板修改产生新版本，停用不改写历史。路由与投递日志必须记录实际模板版本和绑定快照；参数在 Provider 调用前严格校验。

### Verification / Migration
验收覆盖草稿不可发送、发布后不可原位修改、版本回滚、参数 schema、供应商迁移、停用行为及历史内容快照稳定性。

## ADR-010: 最终投递状态只接受可信供应商回执

**Status:** accepted
**Source:** LOG-018, user decision
**Supersedes:** none

### Context
同步 Provider API 通常只表示请求已被接受，现有表也明确 ACCEPTED 不等于 DELIVERED。把同步成功计为送达会污染报表；未经验证或重复、乱序的回调又可能篡改状态。

### Decision
每个供应商通过独立回调适配器验证签名、来源、时间窗和关联标识，先幂等记录回调摘要，再更新对应 Provider Attempt。平台区分 ACCEPTED、DELIVERED、UNDELIVERABLE 和 UNKNOWN；没有可信回执时不得宣称最终送达。

### Trade-off
只记录 ACCEPTED 最简单但无法衡量真实送达；轮询或默认成功会制造错误事实。回调适配器增加公网入口、安全验证和乱序状态机，换取可信统计。

### Consequences
需要回调去重、迟到和乱序规则、供应商状态映射、未知消息处理及有限原始报文保留。最终状态统计必须以回执状态为准。

### Verification / Migration
验收覆盖伪造签名、重复回调、乱序回调、未知消息 ID、迟到回执、Provider 状态映射和无回执 UNKNOWN。

## ADR-011: 内外调用共享控制面但同 JVM 不绕行 HTTP

**Status:** accepted
**Source:** LOG-019, user decision
**Supersedes:** none

### Context
Captcha、Workflow 等当前在同一 JVM 中直接使用 NotifyClient。继续永久直调会绕过新平台的模板、应用、路由和配额；强制它们调用本机 HTTP 又引入序列化、机器认证和 loopback 网络故障。

### Decision
保留 NotifyClient 作为通知数据面端口，新增 ruoyi-notify 应用服务作为产品治理入口。同 JVM 产品调用方逐步调用应用服务，外部业务系统通过 OpenAPI HTTP Adapter 调用同一应用服务；内部调用不绕行 HTTP。

### Trade-off
全部 HTTP 可以统一传输边界，却在模块化单体中增加无价值网络复杂度；永久直调数据面最简单但无法统一治理。共享应用服务并保留两种 Adapter 增加少量装配，换取一致业务语义和合理运行成本。

### Consequences
应用服务必须与 Servlet 无关，外部 Controller 只做协议适配。现有调用方需要按业务场景建立 notify_app 或受控内部身份，并逐步迁移模板合同。

### Verification / Migration
Captcha、Workflow、Demo 的契约测试应证明它们进入应用服务且不发起本机 HTTP；外部 API 与内部调用对模板、路由、配额和日志产生一致结果。

## ADR-012: 渠道秘密不回显且高风险操作分权审计

**Status:** accepted
**Source:** LOG-020, user decision
**Supersedes:** none

### Context
渠道账号包含可直接消费短信或邮件额度的秘密。数据库明文、管理端回显、单一超大权限或异常日志原样记录都会扩大泄露和滥用风险；生产发布和密钥轮换还需要可追责。

### Decision
渠道 secret 复用项目批准的 KEK/AES-GCM 模式加密，创建或替换后不再回显。查看元数据、编辑草稿、发布、停用和测试发送使用独立权限；生产发布与密钥轮换记录不可篡改审计。运行日志、HTTP 日志、异常和管理响应不得包含 secret。

### Trade-off
允许回显和单一权限便于运维，却难以控制凭据扩散。分权、不回显和审计增加重新录入及权限管理成本，换取最小暴露面和事件追溯能力。

### Consequences
必须设计安全测试连接、密钥重新录入、KEK 版本轮换和脱敏异常映射。普通账号详情只能展示密钥存在状态或末次更新时间，不能展示可恢复内容。

### Verification / Migration
验收覆盖数据库密文、API 不回显、权限矩阵、日志扫描、Provider 敏感异常、KEK 轮换、生产发布审计和未授权测试发送。

## ADR-013: 平台幂等按通知应用和渠道分区

**Status:** accepted
**Source:** LOG-021, user decision
**Supersedes:** none

### Context
现有 common-notify 的幂等作用域是 Channel 与业务 Key，适合同一应用内调用，但公司级平台允许多个独立业务系统接入。不同应用可能合法使用相同业务键；把 Platform Client 放入作用域又会混淆登录授权与通知应用。

### Decision
统一通知 API 的平台幂等作用域为 `notifyAppId + channel + idempotencyKey`。默认窗口 24 小时，通知应用可以在 5 分钟至 7 天的服务端安全范围内配置。

同一作用域和同一确定性请求摘要复用原受理 ID、结果或当前状态；同键不同摘要返回冲突；进行中请求返回原任务的稳定状态。所有判断在路由和 Provider 调用前完成。

### Trade-off
全局 Key 最简单却会造成应用间冲突；无限期保留提供最强去重但持续占用状态并阻止业务键复用。应用分区和有限可配窗口增加摘要、存储与清理逻辑，换取隔离且可预测的重复语义。

### Consequences
外层平台幂等需要与 common-notify 内部幂等协调，避免双层 Key 产生不同结果。请求摘要字段、状态投影和 TTL 更新必须稳定；该能力仍不是 Provider exactly-once。

### Verification / Migration
验收覆盖跨应用同键、跨渠道同键、同摘要重复、处理中重复、同键异摘要冲突、TTL 边界、Redis/状态存储故障和同步/异步一致性。

## ADR-014: ruoyi-notify 使用新监控合同且不保留历史兼容

**Status:** accepted
**Source:** LOG-022, latest user decision
**Supersedes:** ADR-002

### Context
ADR-002 选择 ruoyi-notify 模块化单体，同时要求保留现有 `/monitor/notify`、权限字符和历史日志兼容。用户随后明确决定无需历史兼容。旧 system/notify 是 best-effort 监控结构，不完整表达可靠异步任务、路由快照、多次 Provider Attempt、可信回执和应用统计。

### Decision
继续采用 `ruoyi-modules/ruoyi-notify` 模块化单体以及对应 notify Domain/Web Domain，但由新模块直接建立规范监控合同。不保留旧 `/monitor/notify` 路径、`system:notify:*` 权限、旧表数据迁移、Controller 别名或双写兼容层。

新合同统一拥有可靠任务、逻辑通知、Provider Attempt、供应商回执和统计。完整正文与模板参数保留 30 天，脱敏投递明细保留 180 天，供应商回调摘要保留 90 天，按日脱敏聚合保留 24 个月；credential-like 通知始终使用 `REDACT_SENSITIVE`。

### Trade-off
兼容迁移可以保留旧审计和调用入口，但会让新模型背负旧 best-effort 语义及迁移复杂度。直接替换允许建立干净合同，代价是部署升级具有破坏性，旧监控数据与书签不可恢复。

### Consequences
旧后端、菜单、权限、前端页面和 schema 必须完整删除或替换，不能留下两套入口。新路径和权限在 Spec 中作为唯一权威定义；数据清理和聚合任务必须按保留期自动执行。

### Verification / Migration
发布前验证旧路径不可访问、旧权限不再生成、旧菜单和前端注册消失、旧表无运行时引用、新表状态机完整、分级清理正确且敏感通知从未保存正文或可恢复目标。

## ADR-015: 异步受理采用持久化 at-least-once 边界

**Status:** accepted
**Source:** LOG-023, user decision
**Supersedes:** none

### Context
可靠异步通知需要在平台确认受理后经受进程重启和消费者重复执行。跨平台无法原子提交本地任务与第三方 Provider 请求，因此宣称 Provider exactly-once 不真实；只依赖内存队列又会丢失已返回成功的任务。

### Decision
异步 API 只有在任务已经持久化并进入可恢复状态后才返回受理成功。平台内部采用 at-least-once 执行、租约领取、幂等状态转换和受控重试；成功受理的任务不得因进程重启丢失。平台不承诺 Provider exactly-once，并通过平台幂等、Provider message ID、路由 attempt 和回执降低及识别重复风险。

第一版上线门槛包括：模拟 Provider 持续 200 请求/秒和 1000 目标/秒运行 10 分钟无任务丢失；异步受理 P95 不超过 300ms、P99 不超过 800ms，正常负载投递启动 P95 不超过 5 秒；同步平台开销 P95 不超过 100ms、Provider 超时 5 秒；重启后 60 秒内恢复；月度平台可用性目标 99.9%。

### Trade-off
at-most-once 可以避免平台重复却允许任务丢失；所谓 exactly-once 无法跨未知 Provider 可靠实现。at-least-once 增加幂等、状态机和重复观测复杂度，换取已受理任务不丢失和可恢复性。

### Consequences
调用方必须使用幂等键并正确解释受理、Provider 接受和最终送达三个阶段。指标需要分别统计平台可用性、Provider 故障、积压、重复消费、投递启动延迟和最终回执。

### Verification / Migration
验收覆盖持久化前失败不返回成功、受理后立即宕机恢复、消费者重复领取、Provider 超时后的未知结果、路由切换、回执去重以及上述容量、延迟和恢复门槛。
