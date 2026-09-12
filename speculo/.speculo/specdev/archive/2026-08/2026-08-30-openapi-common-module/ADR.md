# Change Architecture Decisions

## ADR-001: 开放凭据绑定 Client 身份

**Status:** superseded
**Source:** LOG-002
**Supersedes:** none
**Superseded by:** ADR-008

### Context
NAMEWTA 的角色、菜单、登录域和数据权限都受 `sys_client.id` 约束；只绑定 userId 会让开放调用缺少唯一权限解释上下文。

### Decision
每个 AppKey 固定绑定 `userId + clientPk`。调用身份、角色、菜单权限和数据权限均按该 Client 解析，缺失或失效时失败关闭。

### Trade-off
独立服务账号更适合纯机器身份，但会引入第二套账号生命周期；只绑定 userId 更简单，却会破坏 Client 隔离。当前接受一个用户在不同 Client 使用不同凭据的管理成本。

### Consequences
密钥表、缓存键、审计记录、唯一性约束和撤销逻辑都必须携带 clientPk；调用方不能在请求中选择或覆盖 Client。

### Verification / Migration
自动测试覆盖相同 userId 的不同 Client、错误 clientid、缺失 clientPk、角色变化和跨 Client 越权。

## ADR-002: 使用版本化 HMAC 请求签名且不强制 TLS

**Status:** accepted
**Source:** LOG-003
**Supersedes:** none

### Context
来源 MD5 签名未绑定 method、path、query 和 body，无法证明签名针对的是当前请求。部署环境同时需要支持 HTTP 与 HTTPS。

### Decision
定义不兼容来源方案的 NAMEWTA 版本化 HMAC-SHA256 协议，签名覆盖凭据、时间、nonce 和规范化请求语义。服务端不以 HTTPS 作为验签前置条件。

### Trade-off
采用标准 HTTP Message Signatures 可减少自定义协议，但客户端实现复杂度更高；兼容 MD5 更易迁移但继承安全弱点。允许 HTTP 提升内网和开发部署灵活性，但不提供请求与响应机密性，也不能防止被动监听。

### Consequences
文档和示例必须区分认证完整性与传输机密性并推荐生产使用 HTTPS；签名版本进入请求合同，未来升级不得静默改变 canonicalization。

### Verification / Migration
使用固定测试向量验证跨语言签名；对 method、path、query、body、timestamp、nonce 任一篡改均拒绝；HTTP 和 HTTPS 代理场景采用相同 canonical request。

## ADR-003: system 拥有业务并通过类型化端口接入 common 运行时

**Status:** accepted
**Source:** LOG-007
**Supersedes:** none

### Context
`@OpenApi`、签名和拦截属于可复用运行时，但凭据表、用户、Client、权限和管理 API 属于 system。common 直接查询 system 表会反转 Maven 依赖；仓库现有 common SPI 与 system Bean 实现已经提供了清晰的本地分层范式。

### Decision
新增 `ruoyi-common-openapi` 承载方法级注解、协议运行时、开放接口注册表、配置和窄类型化 SPI。`ruoyi-system` 拥有持久化、密钥生命周期、身份权限构建、管理 API 和审计实现；`ruoyi-admin` 只负责依赖装配。common 运行时不持有 system Mapper/domain；内部使用明确的接口和直接应用服务调用，不引入或借鉴 MQ、requestCode、CommandBus 或通用 Command 分发器。

### Trade-off
全部放 system 文件更少但无法形成用户要求的 common 通用模块；将持久化放 common 看似自包含，却把 system 领域泄漏到基础层。类型化端口会增加少量 DTO/SPI，但保留编译期合同与可替换测试实现。

### Consequences
必需 SPI 必须唯一且在启用时可用；system 写用例由明确的应用服务和类型化方法直接处理。开放运行时与数据库实现可独立测试。

### Verification / Migration
增加 Maven 依赖方向检查、缺失/重复 SPI 启动测试、common 纯运行时测试和 system adapter 集成测试。

## ADR-004: 管理端复用 Client-scoped RBAC

**Status:** accepted
**Source:** LOG-010, LOG-016
**Supersedes:** none

### Context
来源方案另设 ALL、ROLES、ADMIN、SUPER_ADMIN 模式，但 NAMEWTA 已有按 Client 隔离的角色、菜单和按钮权限。并存两套准入会产生冲突和不可解释的授权结果。

### Decision
密钥管理 API 使用当前前端 Client 的现有菜单/按钮权限，owner 范围校验始终在 service 层执行。接口目录和开放调用不继承当前前端 Client，而使用 ADR-008 定义的 OpenAPI 全局权限快照，并继续校验目标方法原有 `@SaCheckPermission`。

### Trade-off
配置模式可快速全局开放，但绕开菜单治理。复用 RBAC 需要初始化菜单和角色关系，却保持单一权限来源、审计路径和管理习惯。

### Consequences
需要为个人密钥与平台管理设计清晰的权限码和 owner 范围；普通管理请求仍受当前 Client 边界约束，OpenAPI 调用权限按全局身份单独解释。

### Verification / Migration
测试无菜单权限、有自助权限、管理员权限、超管、跨 owner、跨 Client、目标接口权限缺失和权限撤销场景。

## ADR-005: 使用请求内机器身份桥接现有权限链

**Status:** accepted
**Source:** LOG-011, LOG-016, LOG-017
**Supersedes:** none

### Context
开放调用必须复用现有 `@SaCheckPermission`、`LoginHelper` 和数据权限，但第三方不应获取或维护浏览器式 Sa-Token。

### Decision
普通 Sa-Token 请求保持单 Client 行为不变。签名请求先验证开放凭据，再创建或取得标记为 OpenAPI 通道的内部机器 Token，仅写入当前请求 Storage；该 Token 不进入响应、文档或日志。全局 `LoginUser` 快照保存于该 TokenSession 并通过现有 Sa-Token Redis DAO 复用；缓存不存在或版本过期时重建，请求结束只清理请求 Storage，不删除仍有效的机器 Session。

### Trade-off
独立改造全部权限组件支持第二种 ThreadLocal 身份会扩大公共安全面的修改范围；内部请求 Token 增加少量会话管理成本，但能复用现有权限与数据权限链。

### Consequences
凭据禁用、过期、删除或 owner 失效必须在内部 Token 注入前拒绝；内部 Token 不能绕过每次验签，也不能返回给第三方。权限缓存 miss 只能触发从 system 权威数据重建，不能把当前请求所需权限自动写入授权集合。

### Verification / Migration
测试普通 Token、仅签名请求、双认证冲突、权限即时撤销、上下文清理和内部 Token 不泄漏。

## ADR-006: Secret 单版本加密保存并立即切换

**Status:** accepted
**Source:** LOG-012
**Supersedes:** none

### Context
AppSecret 需要在验签时可用，但数据库泄露时不能直接暴露明文；每个用户只保留一个有效开放凭据。

### Decision
使用 CSPRNG 生成 secret，以外部注入、带版本的 KEK 执行 AES-256-GCM 加密保存。secret 仅创建或重置成功时显示一次；一条凭据同一时刻只有一个有效 secret，重置后旧 secret 立即失效。

### Trade-off
立即失效不支持无停机双 secret 轮换，但状态和撤销语义简单，符合每用户单凭据约束。

### Consequences
KEK 缺失或无效时启用 OpenAPI 必须启动失败；密文记录携带 key version、nonce 和认证标签，任何接口与日志不得回显 secret。

### Verification / Migration
测试一次性展示、密文篡改、错误 KEK、KEK 版本读取、并发重置和旧 secret 立即拒绝。

## ADR-007: 每个用户最多一个有效开放凭据

**Status:** accepted
**Source:** LOG-015
**Supersedes:** ADR-001 中“一个用户可在不同 Client 使用不同凭据”的管理结论

### Context
开放凭据代表用户的机器调用入口，而不是每个应用或每个 Client 的独立集成实例。用户要求一个用户只能创建一个类似 API key/token 的凭据。

### Decision
每个用户最多拥有一条有效开放凭据，由一个公开 AppKey 和一个仅展示一次的 AppSecret 组成。重复创建返回现有凭据状态并提示重置，不生成第二条；超级管理员也只能代建、重置、禁用或删除这条凭据。

### Trade-off
单凭据无法为多个第三方集成独立撤销或分摊限流，也不支持双 secret 平滑轮换，但所有权和使用体验更简单。

### Consequences
数据库必须对 owner userId 建立有效凭据唯一约束；凭据只绑定 userId，不携带 clientPk，不能用新增第二条凭据绕过。

### Verification / Migration
测试用户与管理员并发创建、删除后重建、禁用后行为、重置立即切换及唯一约束冲突处理。

## ADR-008: OpenAPI 使用跨前端 Client 的全局授权身份

**Status:** accepted
**Source:** LOG-016
**Supersedes:** ADR-001

### Context
`sys_client` 用于区分同一后端的不同前端登录入口、动态路由和请求策略。OpenAPI 直接调用后端方法，不需要前端路由，也不应要求同一用户为不同前端维护多套凭据。

### Decision
OpenAPI 是独立认证通道，不创建或伪装一条专用 `sys_client`。每个 AppKey 只绑定 userId；每次调用从所有该用户具有持久合法身份的 Client 中聚合当前有效角色、权限字符和数据权限，构建 `OpenApiPrincipal`/内部 `LoginUser` 快照。调用方不发送 `clientid`，快照不加载动态路由树，也不设置可被业务误认为某个前端的单一 clientPk。

### Trade-off
绑定单 Client 能直接复用现有登录快照，但会迫使同一用户管理多套 key 或重复授权。全局身份符合统一后端调用体验，却需要专用聚合查询、安全拦截分支，并要求明确处理不同 Client 数据范围的合并及依赖单一 clientPk 的业务接口。

### Consequences
普通前端 Token、菜单树、动态路由、Client 请求头一致性、访问路径和 IP 规则完全保持原样。只有完成 OpenAPI 签名验证且目标方法显式标注 `@OpenApi` 的请求进入全局身份通道。接口目录与实际调用共享同一全局权限判定。

### Verification / Migration
测试 A 有/B 无、A 无/B 有、A+B 均有、全部撤销、禁用 Client/角色/菜单、重复权限字符、数据范围合并、无 `clientid`、伪造普通 Token 和 Client 相关下游能力。

## ADR-009: 用 Sa-Token TokenSession 缓存 OpenAPI LoginUser

**Status:** accepted
**Source:** LOG-017
**Supersedes:** none

### Context
现有 `LoginHelper`、`SaPermissionImpl`、`@SaCheckPermission`、`@SaCheckRole`、数据权限和敏感字段判断都依赖 TokenSession 中的 `LoginUser`。另建一套 Principal 和权限注解会重复现有安全链。

### Decision
每个有效 OpenAPI 凭据关联一个仅服务端可见的 Sa-Token 机器 Token。首次有效签名请求或 TokenSession 缺失时，common 通过类型化 SPI 请求 system 组装全局 `LoginUser`，写入现有 `LoginHelper.LOGIN_USER_KEY`；`PlusSaTokenDao` 负责 Redis 持久化。后续每次请求仍先验签，再取得内部 Token，将其仅写入当前 Sa-Token request Storage，使现有权限、角色、数据权限和脱敏链无需分叉即可执行。

### Trade-off
逐请求重建权限最及时但数据库成本高；独立权限缓存更容易定制却形成第二套事实源。复用 TokenSession 减少代码和行为分叉，但需要专门处理机器 Token 生命周期、并发首次创建、权限变更失效和多节点本地缓存窗口。

### Consequences
缓存 miss 表示重建已有授权快照，不是授予权限；目标权限不在 system 权威结果中时返回 403。内部 Token 与 AppKey/AppSecret 完全不同，不作为调用协议字段。机器 Session 创建必须关闭响应头写 Token，并在分布式锁内双重检查，避免 `is-share=false` 下并发生成多个 Session。

### Verification / Migration
测试首次创建、Redis 命中、Session 丢失重建、并发首调、内部 Token 不泄漏、普通 Token 不受影响、无权限不写入、权限撤销、凭据重置和多节点缓存失效。

## ADR-010: 以薄会话桥接层复用现有安全主链

**Status:** accepted
**Source:** LOG-019
**Supersedes:** ADR-008 中新建独立 `OpenApiPrincipal` 的可选表述

### Context
OpenAPI 需要组织不同于普通登录的内部 Token、Session ID 和认证通道信息，但权限、角色、数据权限及脱敏判断已经统一依赖 TokenSession 中的标准 `LoginUser`。直接重写登录主链或创建第二套 Principal 会扩大与上游的冲突面，并产生双重安全语义。

### Decision
在 `ruoyi-common-openapi` 内增加最薄的 OpenAPI 会话桥接层，使用 Sa-Token 1.45.0 已有公共 API 创建/取得机器 TokenSession、写入 `LoginHelper.LOGIN_USER_KEY` 并仅向当前 request Storage 注入内部 Token。common 通过窄类型化授权快照端口请求 system 返回标准 `LoginUser`；不得新增平行 Principal、权限注解或独立 Redis 权限缓存。`credentialId`、授权 revision、认证通道等 OpenAPI 专属信息优先保存为 Token extra 或 TokenSession sidecar，不污染 `LoginUser` 领域字段。

只有经实现验证确认现有公共扩展点不足时，才允许在 `common-satoken` 或 `common-security` 增加一个最小、通用、隔离的认证通道扩展；该变化必须附 WHY 注释、回归普通登录，并登记到上游定制映射。不得重写 `LoginHelper`、`SaPermissionImpl`、`PlusSaTokenDao` 或普通登录流程。

### Trade-off
直接修改现有工具类看似代码更少，却会把 OpenAPI 特例散布到上游高频变更文件。薄桥接层增加少量适配代码，但把定制集中在新模块，保持现有安全链和升级路径稳定。

### Consequences
实现应优先组合而非继承或复制现有工具；任何扩展都必须证明现有 API 无法满足。普通前端 Token 的 Client 约束完全不变，OpenAPI 例外只能从显式验签入口触发。

### Verification / Migration
建立普通登录全量回归、OpenAPI Session 创建/恢复、request Storage 清理、Token 不写响应、序列化兼容和上游差异审查；代码评审禁止重复实现权限读取或 Redis Session DAO。

## ADR-011: 分离应用开放管理与开放接口调用

**Status:** accepted
**Source:** LOG-020
**Supersedes:** none

### Context
凭据创建与管理发生在已登录的系统管理场景；开放接口调用发生在第三方已经取得凭据之后。两者若混成同一权限模型，会把菜单权限错误地当作接口权限，或让调用权限绕过管理入口。

### Decision
`ruoyi-system` 提供“应用开放管理”菜单对应的管理 API。它继续使用当前前端 Client 的既有菜单/按钮权限和 service owner 校验；超级管理员可管理所有用户的唯一凭据，并可代建、重新创建/重置、禁用、删除和查看目标用户的可调用接口。

开放调用运行时只接受已经创建且有效的凭据：每次先校验 AppKey、HMAC、时间/重放和凭据状态，再按 ADR-009/ADR-010 从 Redis 复用或从 system 权威关系装载 `LoginUser`，最后由目标方法原有权限注解决定是否允许。管理权限与调用权限互不推导。

### Trade-off
共享一个入口会减少 Controller 数量，却混淆浏览器身份与机器身份。分离两条入口增加清晰的 API 边界，同时仍共享凭据领域服务、授权快照解析器和开放接口注册表。

### Consequences
管理 API 不接受 AppKey 作为管理身份；开放调用不读取动态路由，也不要求第三方登录。凭据创建完成并不授予任何接口权限，接口权限始终来自用户当前权威角色、菜单和数据权限关系。

### Verification / Migration
测试无管理菜单权限、用户自助、跨 owner、超级管理员代管、凭据未创建、凭据禁用/重置，以及“有管理权限但无目标接口权限”和“有目标接口权限但无管理权限”两条负向路径。

## ADR-012: 目录预览与真实调用共享授权解析器

**Status:** accepted
**Source:** LOG-021
**Supersedes:** none

### Context
超级管理员需要点开任意用户查看其能调用的 OpenAPI 接口。若管理页面使用管理员自己的会话，或另写一套角色菜单过滤 SQL，显示结果会与目标用户真实调用不一致。

### Decision
定义一个以 `targetUserId` 为输入的窄类型化 OpenAPI 授权快照解析器，由 system 基于权威关系实现；真实调用的 Session cache miss、用户自助目录和超级管理员目标用户预览均复用该解析器。目录过滤和运行时暴露判断共享同一开放接口注册表及权限匹配函数。

预览是只读计算：它不切换为目标用户登录态、不继承查看者的接口权限、不创建机器 TokenSession，也不向 Redis 写入调用期缓存。管理 Controller 只负责校验查看权限和目标用户范围。

### Trade-off
直接复用机器 Session 可以命中缓存，但会让只读管理操作产生登录副作用。显式目标用户解析可能增加一次查询，却保证语义清晰且可单测；必要时可复用授权 revision 对应的只读缓存，但不能改变事实源。

### Consequences
开放接口列表中的 method、path、说明、原始权限要求和可调用结果都来自同一注册表/解析器组合。新增 `@OpenApi` 方法后，无需维护第二份管理目录规则。

### Verification / Migration
对同一 targetUserId 比较管理员预览、用户自助目录和真实签名调用的允许/拒绝矩阵；覆盖管理员自身权限更高/更低、目标用户无凭据、权限变更及禁用接口。

## ADR-013: 管理员页面与个人开放应用 Tab 复用同一能力

**Status:** accepted
**Source:** LOG-022
**Supersedes:** none

### Context
管理员需要管理全部用户的开放凭据，普通用户只需要管理自己的凭据并阅读自己可调用接口的文档。当前 admin-web 的个人信息页已经以同级 Tabs 组织基本资料、修改密码、第三方应用和在线设备；若为个人 OpenAPI 再增加动态菜单或复制管理员页面，会造成入口含义和实现重复。

### Decision
管理员入口固定为“系统管理 > 应用开放管理”，提供按用户检索、目标用户详情、代建、重新创建/重置、禁用、删除以及可调用接口预览。个人入口固定为“个人中心/个人信息 > 开放应用”Tab，与现有个人设置 Tabs 同级；它只允许当前登录用户创建和管理自己的唯一凭据，并查看自己的可调用接口、接口详情和调用文档，不允许选择或传入其他 owner。

两个入口在前端复用同一 OpenAPI domain、web-domain 组件与状态能力，由宿主入口显式提供 `current-user` 或 `target-user` scope；不得复制 transport、领域服务、权限过滤或文档渲染。个人 Tab 是否可见及其中命令是否可用继续使用当前 Client 的既有菜单/按钮权限，所有后端接口仍独立鉴权。按 ADR-020 的范围修正，本 change 首期实现上述两个入口。

### Trade-off
两个独立页面可以快速按角色定制，但会产生两套密钥状态和接口目录逻辑。共享领域/Web 能力需要显式 scope 合同，却能保持 owner 边界、交互和授权结果一致，并符合多 App 显式组合方向。

### Consequences
个人信息静态路由不因该 Tab 变成动态菜单；无权限用户不能仅因可访问个人信息页而获得 OpenAPI 管理能力。系统管理页面不得复用管理员自身目录替代目标用户目录；个人 Tab 不暴露全局用户搜索或管理员命令。

### Verification / Migration
本 change 的前端验证必须覆盖：无权限 Tab 隐藏和直接 API 拒绝、本人创建/重置/查看、不能篡改 owner、超级管理员目标用户操作、两入口接口目录一致性，以及个人信息现有 Tabs 和路由回归。按 ADR-021 的轻量门禁运行适用的 architecture、lint、typecheck、test 和 build。

## ADR-014: 以当前 ruoyi-api 身份模型和 system 前端域作为扩展锚点

**Status:** accepted
**Source:** LOG-023
**Supersedes:** ADR-008 中独立 `OpenApiPrincipal` 的残余表述

### Context
最新后端把标准 `LoginUser` 放在 `ruoyi-api`，`common-satoken` 已依赖该模块；最新前端由 admin App 显式组合 system domain/web-domain，个人信息仍是 App 自有静态页，运行时代码生成域已经退出当前组合。

### Decision
OpenAPI 授权快照端口直接返回 `ruoyi-api` 的标准 `LoginUser`，`ruoyi-common-openapi` 组合 `common-satoken` 公共 API 与 system 窄 SPI，不复制身份 DTO，也不反向依赖 `ruoyi-admin` 的单 Client `SysLoginService`。机器身份使用与浏览器会话不碰撞的内部登录命名空间，`clientPk/clientKey` 不伪装任一前端 Client。

前端实现归 system domain/web-domain：动态的“应用开放管理”页由 system manifest 暴露；App 自有个人信息页只新增“开放应用”Tab 并组合 system web-domain 组件。不得恢复 `gen` 或在 App 内复制 system transport、状态和授权过滤。

### Trade-off
复用 admin 登录组装器文件更少，但会反转模块边界且仍只能构建单 Client 身份。新建独立 Principal 更自由，却会分叉权限链。当前方案需要 system 新增全局聚合查询/组装，但跨层类型和前端能力仍只有一份。

### Consequences
OpenAPI `LoginUser` 的序列化、`LoginHelper`、`SaPermissionImpl`、数据权限和敏感判断与普通登录共享；OpenAPI 专属字段只进入 Token extra/Session sidecar。后端 owner-scope 合同与本 change 的管理员页和个人 Tab 共用。

### Verification / Migration
验证 Maven 依赖无环、标准 `LoginUser` 序列化兼容、普通登录无变化、机器 loginId 不碰撞；本 change 的前端变更运行 domain/web-domain 架构门禁并确认个人页不新增动态路由。

## ADR-015: 正文审计复用统一有界采集并强制脱敏

**Status:** accepted
**Source:** LOG-024
**Supersedes:** permanent ADR-0017 within this change, because backend HEAD `e5cef5a61` and the later ready redaction change have already replaced its raw-credential runtime behavior

### Context
用户希望调用审计保留请求与响应正文，但当前已交付的统一 HTTP 日志明确禁止密码、Token、secret、API key、Cookie 和 Session 标识原样落日志，并提供有界采集与 JSON 失败关闭脱敏。

### Decision
OpenAPI 正文可观测性复用 `common-web` 统一 HTTP 日志，不建立第二份原始正文存储。正文“完整”指在配置上限内可安全记录的 JSON：默认每方向 1 MiB；敏感头和 JSON 字段递归替换为 `[REDACTED]`；非法或截断 JSON 整段隐藏。AppKey、AppSecret、签名、普通 Token 和内部机器 Token 永不记录原值。非 JSON 文本、multipart、二进制、文件、音视频、SSE 与其他流式正文只记录媒体类型、长度、状态、耗时等元数据。

OpenAPI 调用事件只承载凭据标识的安全引用、owner、接口、结果、耗时和可扩展计量信息，不复制正文。新增协议头必须纳入统一敏感头策略。

### Trade-off
原始正文最利于重现请求，但会不可接受地暴露凭据与个人数据。完全不记正文更安全，却损失普通 JSON 调试价值。复用统一有界采集保持安全策略单一，并允许对非敏感字段排障。

### Consequences
审计/日志失败不得改变业务响应；调用示例和错误日志也不能输出 secret 或签名。该 change 不采用任何要求原样记录凭据的旧约定。

### Verification / Migration
扩展 `SysLogFilterTest` 覆盖全部 OpenAPI 协议头、嵌套 JSON、非法/截断 JSON、二进制/流式媒体；验证业务 request/response 未被日志副本修改。

## ADR-016: 复用 Sa-Token 集群确认注销 OpenAPI 授权快照

**Status:** accepted
**Source:** LOG-025
**Supersedes:** ADR-009 中“多节点本地缓存窗口”风险和 ADR-010 中未定的授权 revision 侧写

### Context
当前 `PlusSaTokenDao` 已把写入与删除接入 `ClusterCacheInvalidationCoordinator`，能够在 Redis 变更时广播 key 指纹、等待订阅节点确认并精确失效本地 Caffeine。旧设计假设的固定 5 秒旧值窗口已不存在；真正缺口是现有 `ClientSessionService` 不会自动匹配不带 `clientPk` 的 OpenAPI 全局机器 Session。

### Decision
不增加平行的授权 revision 缓存。为 OpenAPI 机器通道提供按 userId 精确注销的窄会话失效能力，并复用现有活动 Token 搜索、`LoginUser` 匹配、`logoutByTokenValue`、失败重试集合和 Sa-Token DAO 集群确认。凭据禁用/删除/重置以及所有会改变全局快照的 system 授权写路径，在写事务内完成权威变更后、提交成功响应前注销受影响机器 Session；失败必须传播而不是静默成功。下一次有效验签在 Session miss 时按 ADR-009 重建。

### Trade-off
每请求校验 revision 可以兜底漏接写路径，却引入新的状态源和 Redis 读。统一注销更符合当前代码，但要求维护一张完整的授权变化到用户集合映射，并对批量变化验证成本上限。

### Consequences
不得直接删除 Redis key或绕过 Sa-Token DAO。OpenAPI Session 必须可由机器通道和 userId 稳定识别；普通 Client 会话的现有失效语义不变。D-018 最终聚合规则必须同步定义失效矩阵。

### Verification / Migration
自动测试覆盖单用户、角色、菜单、登录域、Client、用户状态、数据范围和凭据生命周期变化；多进程测试证明写操作返回成功前所有节点旧 Session 已失效，故障时写操作显式失败且可重试。

## ADR-017: nonce 只负责防重放且首期不提供通用业务幂等

**Status:** accepted
**Source:** LOG-027
**Supersedes:** none

### Context
签名 nonce 能证明一份签名材料没有被再次使用，但无法判断一次写请求是否已经在服务端提交。当前 `@RepeatSubmit` 又依赖普通 Sa-Token 请求头和方法参数摘要，不是第三方可依赖的稳定幂等协议。通用缓存响应还会遇到事务提交不确定、文件、流式及超大响应边界。

### Decision
OpenAPI v1 使用 Redis 原子登记 `AppKey + nonce`，接受时间窗口与 nonce TTL 默认 60 秒；每次网络重试必须使用新的 timestamp、nonce 和签名。首期不实现框架级 `Idempotency-Key` 或响应重放，写接口必须在自身合同中声明能否重试，并按需复用或实现业务级幂等。

运行时复用现有 Redis 限流能力，同时执行每 AppKey 1000 次/分钟和每 AppKey + 开放接口 100 次/分钟两级限制；阈值均可通过 OpenAPI 配置覆盖，Redis 不可用时认证入口失败关闭。

### Trade-off
通用响应重放能改善超时重试体验，但无法可靠包住任意 Controller 的事务和响应类型。只做 nonce 最简单，却缺少滥用保护。当前方案把认证安全、流量治理和业务幂等分离，接受写接口调用方必须阅读其独立重试合同。

### Consequences
nonce 不得被文档描述为业务幂等键。GET 等天然幂等请求可以重新签名重试；写请求在未知结果后重试可能重复执行，除非目标接口另有明确幂等保证。限流和 nonce 的 Redis key 不包含原始 secret 或签名。

### Verification / Migration
固定并发测试证明同一 nonce 只有一次成功；窗口外、Redis 故障和重复 nonce 均失败关闭。分别验证全局 key 限额与 key + endpoint 限额、配置覆盖和普通 `@RateLimiter` 行为不变。

## ADR-018: 全局授权只聚合用户可合法登录的有效 Client 权限

**Status:** accepted
**Source:** LOG-028
**Supersedes:** none

### Context
“用户所有 Client 权限”不能解释为所有启用 Client 的权限。当前登录资格由 Client、登录域和用户登录域关系共同决定；Client 默认角色虽不写入 `sys_user_role`，却会在普通登录中自动生效。状态过滤和数据范围合并若不明确，会产生权限放大或与真实登录不一致。

### Decision
OpenAPI 全局快照只纳入满足全部条件的 Client：Client 状态正常、已配置正常登录域、用户持有该登录域关系。每个合法 Client 纳入其正常默认角色和用户显式分配的正常角色，只纳入状态正常的菜单权限。角色字符和权限字符集合去重；`dataScopeRoleMap` 按权限字符汇总所有实际生效角色 ID，交由现有数据权限链联合计算。超级管理员继续使用 `superadmin` 与 `*:*:*`。

授权事实只来自数据库当前权威关系，不从用户当前或历史在线 Session 推断。用户、Client、登录域、角色或菜单停用后不得进入重建快照。

### Trade-off
只聚合显式角色查询更简单，却会漏掉真实登录自动拥有的默认角色；聚合所有活动 Client 默认角色会给无登录资格用户授权。完整资格判断需要专用批量查询和状态矩阵，但授权语义可解释且与登录模型一致。

### Consequences
全局解析器不能机械循环调用缺少完整状态过滤的旧查询并拼接结果；它应复用领域规则并形成安全的专用聚合接缝。任何影响上述集合的写路径都必须按 ADR-016 注销相关机器 Session。

### Verification / Migration
矩阵覆盖 Client/登录域/关系/默认角色/显式角色/菜单的正常与停用组合、重复权限字符、超管和无合法 Client；对每个权限比较目录预览、真实调用和 `dataScopeRoleMap` 角色集合。

## ADR-019: OpenAPI 首期只允许 Client 无关接口

**Status:** accepted
**Source:** LOG-029
**Supersedes:** none

### Context
OpenAPI 全局身份刻意不绑定某个前端 Client，但现有 OSS 上传票据、Client 级访问策略和部分管理能力把 `clientPk` 当作授权输入。调用方选择 Client 会重新引入多 Client 协议，服务端根据权限命中推断又会在重复权限字符下产生歧义。

### Decision
首期只有不依赖单一 Client 上下文的方法可以使用 `@OpenApi`。OpenAPI 全局 `LoginUser.clientPk/clientKey` 保持为空；调用协议不接受 Client 字段，服务端不选择、不推断也不填充默认 Client。任何必须读取单一 Client 的下游能力失败关闭，并且不得作为开放接口发布。

OSS 上传、Client 级路径/IP/业务策略等能力必须先通过后续 change 形成明确的 Client 无关公共合同，才可再考虑开放。

### Trade-off
签名携带 Client 可以覆盖更多现有接口，却破坏一个用户一条全局凭据和全局权限身份的简洁语义。服务端推断改动较少，但无法稳定解释多个 Client 同时拥有同一权限。禁止开放会缩小首期接口集合，但边界清晰且不会静默越权。

### Consequences
开放接口评审和测试必须检查 Controller 及关键下游没有读取 `clientPk/clientKey`。运行时不提供 Client fallback；错误标注的接口在 Client 依赖处失败，而不是借用浏览器或默认 Client 继续执行。普通前端请求保持现状。

### Verification / Migration
至少选择一个 Client 无关接口证明可调用，并对 OSS 上传或专用测试接口证明空 Client 失败关闭；扫描首批 `@OpenApi` 方法的已知依赖并纳入代码评审清单。

## ADR-020: 首期同步交付双入口前端并复用 system Web 领域

**Status:** accepted
**Source:** LOG-030, LOG-031, LOG-032
**Supersedes:** ADR-013 与 ADR-014 中“首期仅交付后端、前端留待未来 change”的范围限定；其双入口与复用设计继续有效

### Context
此前首期范围被记录为只交付后端，但用户明确纠正：`admin-web` 本次就需要“系统管理 > 应用开放管理”和个人信息“开放应用”Tab。当前前端已经以 App 显式组合 system domain/web-domain，且个人信息是 App 自有静态页面。

### Decision
本 change 首期同步交付前后端。OpenAPI transport、领域模型、owner-scope 服务进入 `@namewta/domain-system`；共享 Vue 组件和动态管理页进入 `@namewta/web-domain-system`；system manifest 注册“应用开放管理”动态页面。`admin-web` 只完成显式组合，并在现有个人信息静态页增加“开放应用”Tab，向同一共享能力传入 current-user scope；管理页使用 target-user scope。

两个入口继续共享凭据生命周期、接口目录、详情/文档渲染和权限结果。前端只控制当前 Client 中的可见性和交互，后端仍对每项操作独立鉴权。

### Trade-off
把页面留到后续 change 可以缩小本次规模，但无法交付用户要求的可用平台。把所有实现直接写进 App 更快，却会复制 system 领域合同并增加上游合并冲突。按现有 domain/web-domain/App 边界落位需要显式 runtime 和 exports，但复用性与架构一致性更好。

### Consequences
Spec 和 Ticket 必须包含前端代码、菜单 manifest、个人 Tab、权限状态和文档查看体验。个人信息页仍是静态路由；管理页由服务端菜单和 system manifest 解析。不得恢复 `gen`、建立 App 私有 API 副本或复制两套 OpenAPI 状态。

### Verification / Migration
验证 system domain/web-domain 公开入口、manifest 组件键、admin 显式组合、个人 Tab current-user 边界、管理页 target-user 边界、无权限隐藏与直接 API 拒绝，以及现有个人信息 Tabs 回归。

## ADR-021: 首期采用含前端门禁的轻量模块级验证

**Status:** accepted
**Source:** LOG-033
**Supersedes:** none

### Context
严格的真实 MySQL、Redis 和多进程矩阵能提供更高安全置信度，但用户选择较轻量的 B 级门禁。同时本 change 已确认包含前端，不能继续按“无前端变更”排除前端验证。

### Decision
后端发布门禁以 common-openapi、system 和 admin 装配的单元/模块测试为主，基础设施允许使用测试替身；不把真实 MySQL、真实 Redis、多进程集群和全量 E2E 设为首期强制条件。测试仍必须覆盖固定签名向量、权限聚合、nonce 原子语义、两级限流、凭据生命周期、接口目录一致性、失败关闭和普通登录回归。

前端强制执行架构检查，以及受影响的 system domain、system web-domain 和 admin App 的 lint、test、typecheck、build；聚焦组件/集成测试覆盖两个入口、owner scope、菜单/按钮权限、接口目录与个人信息页回归。

### Trade-off
该选择缩短交付反馈周期，但无法证明真实数据库唯一约束竞争、真实 Redis 原子性和多节点 Session 失效。测试替身适合冻结合同，不等同于生产基础设施验证。

### Consequences
Spec/Ticket 必须把真实基础设施和多节点行为列为残余风险，不得把模拟测试结果描述为集群验证。任何测试替身都必须保留失败关闭、并发冲突和拒绝语义。

### Verification / Migration
记录实际执行的 Maven 模块测试和前端目标包命令。前端至少运行 `pnpm architecture:check`，并对 `@namewta/domain-system`、`@namewta/web-domain-system`、`@namewta/admin-web` 执行 lint、test、typecheck、build；后端测试报告须明确哪些 MySQL/Redis/多节点场景未以真实基础设施执行。
