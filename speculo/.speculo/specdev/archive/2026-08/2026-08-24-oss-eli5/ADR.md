# Change Architecture Decisions

## ADR-001: OSS 对象具有显式公共与受控读取语义

**Status:** accepted
**Source:** LOG-001, LOG-002, user decision
**Supersedes:** none

### Context
现有 OSS 已完成浏览器直传、短时签名、TEMP 生命周期和业务引用，但新直传对象不保留稳定 URL，读取路径统一签名；门户公开资源因此无法形成正式的稳定匿名访问合同。另一方面，业务附件不能因为引入公共资源而失去现有授权边界。

### Decision
在现有 OSS 基础上增加两种显式读取语义：公共 OSS 对象允许任何持有稳定 URL 的访问者直接预览或下载；受控 OSS 对象保持 Provider 私有，只有业务接口先完成权限和数据权限校验后才能签发短时读取 URL。

公共只允许匿名读取。上传、完成、覆盖、删除、配置管理和元数据查询继续由现有控制面授权；知道 ossId 不等于能够匿名查询公共 URL。业务仍持久化 ossId，不把临时签名 URL写入业务数据。

### Trade-off
全部对象统一签名最简单，但不适合门户长期展示、缓存和 CDN；全部对象公开则破坏业务权限。双语义增加配置、路由和测试矩阵，换取公开内容效率与受控内容安全边界同时成立。

### Consequences
后续必须锁定公共/私有存储拓扑、上传策略路由、跨模块 URL 解析合同、存量迁移和 Provider Policy 验证。现有控制面/数据面分离、UploadTicket、TEMP、引用生命周期与 Business OSS Owner 合同继续复用。

### Verification / Migration
验收至少覆盖：公共对象无登录稳定读取；公共匿名写失败；受控对象原始 URL匿名读取失败；业务越权不签发 URL；签名到期失效；现有对象不会因配置升级被意外公开。

## ADR-002: 公共与受控对象使用独立存储配置和 Bucket

**Status:** accepted
**Source:** LOG-003, user decision
**Supersedes:** none

### Context
同一 Bucket 内混合对象 ACL 会让访问语义依赖逐对象写入是否成功，并使 Bucket Policy、CDN、生命周期和运维审计难以独立验证。现有平台已经支持多个 configKey，sys_oss.service 也记录对象所属配置。

### Decision
公共资源和受控资源分别使用独立 configKey 与独立 Bucket。公共 Bucket 只允许匿名读取；受控 Bucket 禁止无签名读取。单个存储配置不得混合两种访问语义，应用不使用对象级 ACL 在同一 Bucket 内切换公开状态。

### Trade-off
独立 Bucket 增加部署配置、凭据和诊断矩阵；换取访问边界可审查、可隔离，并允许公共 CDN 与私有签名策略独立演进。

### Consequences
命名上传策略需要路由到确定的存储配置；访问 URL 解析必须根据对象所属配置工作。Provider Policy 仍需在部署环境配置和验证。

### Verification / Migration
至少使用一组 public-read Bucket 和一组 private Bucket 验证匿名 GET、匿名 PUT、签名 GET、上传完成与错误路由矩阵。

## ADR-003: 公共 URL 由业务 Owner 发布而非通用匿名查询

**Status:** accepted
**Source:** LOG-004, user decision
**Supersedes:** none

### Context
公共对象允许匿名读取并不意味着 sys_oss 元数据或 ossId 空间应成为匿名资源目录。只有业务 Owner 掌握记录是否发布、撤下和展示。

### Decision
不新增按任意 ossId 匿名查询 URL 的通用 HTTP 接口。门户等业务接口随已获准公开的业务数据返回稳定公共 URL；OSS 管理、对象发现和元数据查询继续要求现有认证授权。

### Trade-off
各公开业务响应需要显式接入 OSS 访问结果，不能依赖一个万能公开端点；换取对象发现边界、业务发布状态和元数据隐私保持清晰。

### Consequences
Business OSS Owner 继续拥有业务可见性。OssService 提供跨模块 URL 解析，但不直接暴露匿名 Controller，也不推导业务记录是否应发布。

### Verification / Migration
匿名访问稳定对象 URL成功；匿名按 ossId 查询平台元数据或 URL 失败；未发布业务记录不能通过门户接口发现 URL。

## ADR-004: OssService 统一解析访问 URL 并保留显式签名方法

**Status:** accepted
**Source:** LOG-005, user decision
**Supersedes:** none

### Context
业务模块不应依赖 ruoyi-system 内部配置、Mapper 或 common-oss 工厂来判断对象属于公共还是受控存储。仅提供两套低层方法会把分类判断分散到调用方。

### Decision
在 ruoyi-api OssService 增加 resolveAccessUrl(ossId) 结构化合同，结果包含 accessType、url、可空 expiresAt 和 fileName。公共对象返回稳定 URL且 expiresAt 为空；受控对象返回短时签名 URL和明确过期时间。保留 presignDownload(ossId)，供必须强制受控下载的调用方使用。

### Trade-off
统一解析减少重复判断，但 OssService 合同会承担访问类型分派；显式签名方法并存会形成两个入口，需要精确 JavaDoc 和合同测试防止误用。

### Consequences
调用方仍先完成业务授权，OssService 不读取业务表或推导 ACL。后续需决定 selectUrlByIds/selectByIds 的兼容语义和 TTL 配置范围。

### Verification / Migration
公共、受控、对象不存在、配置不存在、待删除对象均需要跨模块合同测试；公共结果不得伪造 expiresAt，受控结果必须包含有效过期时间。

## ADR-005: 存量 OSS 对象默认保持受控并显式迁移公共资源

**Status:** accepted
**Source:** LOG-006, user decision
**Supersedes:** none

### Context
旧 accessPolicy 在数据库注释、管理 UI 和代码枚举中的语义不一致，且现有新直传读取路径统一签名。依据旧字段自动公开可能使历史业务附件越权暴露。

### Decision
升级后所有现有 sys_oss 对象继续按受控资源访问。只有经过业务清点、目标确认和显式迁移的门户资源才复制或移动到公共 Bucket；不得按旧 accessPolicy 自动公开，也不得原地把含混合历史对象的 Bucket 改为 public-read。

### Trade-off
迁移需要业务清单和额外操作，不能一键公开全部旧图片；换取默认安全、可审计和可回滚的公开范围。

### Consequences
新公共上传走新的命名策略。存量迁移需要独立验证来源对象、目标对象、业务引用切换和失败恢复；未迁移对象不改变现有 URL 行为。

### Verification / Migration
迁移前后分别验证业务引用、对象内容、公开 URL、旧私有对象匿名拒绝和失败回滚；迁移清单之外的 ossId 不得改变访问类型。

## ADR-006: 存储配置是对象访问类型的唯一权威来源

**Status:** accepted
**Source:** LOG-007, user decision
**Supersedes:** none

### Context
独立 configKey 和独立 Bucket 已将公共与受控对象物理隔离。sys_oss.service 已记录对象所属配置；再增加对象级访问类型会形成数据库声明、存储配置和 Provider Policy 三份可能漂移的状态。

### Decision
每个 OSS 存储配置必须声明唯一的 PUBLIC 或 PRIVATE 访问类型，单个配置不得混用。对象通过冻结的 sys_oss.service 归属获得访问类型，不在 sys_oss 增加对象级访问类型快照。配置存在存量对象时，不允许普通更新直接改变其访问类型。

### Trade-off
运行时解析 URL 需要读取对象所属配置，不能只看对象行；换取更少重复状态，并让访问类型与实际 Bucket 边界保持一致。

### Consequences
命名上传策略必须选择确定的 storageConfigKey。配置管理需要区分可安全轮换字段与改变对象边界的字段，后者必须进入显式迁移流程。

### Verification / Migration
覆盖 PUBLIC、PRIVATE、未知配置、配置被禁用和试图改变有对象配置类型的测试；迁移前后的对象 service 归属和真实读取能力必须一致。

## ADR-007: 应用诊断但不主动修改 Provider Bucket Policy

**Status:** accepted
**Source:** LOG-008, user decision
**Supersedes:** none

### Context
Provider Policy 属于部署和云资源治理边界。让应用凭据修改 Policy 会增加权限并可能在启动或运行中意外公开数据；只记录告警又会让错误配置继续承载流量。

### Decision
应用不创建、修改或放宽 Provider Bucket Policy。应用对配置声明和实际匿名读写能力执行启动与健康诊断：public-read 必须允许匿名读取且拒绝匿名写，private 必须拒绝匿名读取。诊断不一致时停用对应配置并使 readiness 失败，由部署配置负责修正。

### Trade-off
部署需要预先准备正确 Policy，应用无法一键自愈；换取最小权限、变更可审计和错误配置 fail-closed。

### Consequences
需要定义 Provider 可移植的诊断合同及无法探测时的明确失败行为。诊断不得创建公开测试对象或改变业务对象状态。

### Verification / Migration
分别验证正确策略、匿名读误开、匿名写误开、探测超时和凭据不足；错误配置不得被 UploadTicket 或 URL 解析选中，readiness 必须呈现原因。

## ADR-008: 生产公共 URL 必须使用受管域名

**Status:** accepted
**Source:** LOG-009, user decision
**Supersedes:** none

### Context
Provider Bucket URL 可用于本地验证，但生产门户需要稳定、可缓存、可迁移且能够统一治理的地址。OSS 配置已经具有 domainUrl 能力。

### Decision
生产环境的 PUBLIC 存储配置必须提供有效 domainUrl 或 CDN 域名，缺失时不得启用。开发环境可以通过显式开关回退到 Provider Bucket URL。公共 URL 使用配置域名和编码后的对象 key 结构化生成；PRIVATE 对象始终走预签名读取。

### Trade-off
生产部署增加域名和 CDN 准备要求；换取 Provider 解耦、缓存治理与后续域名迁移能力。

### Consequences
配置校验需要感知运行环境和显式开发回退。URL 生成必须统一处理尾斜杠、路径编码和 endpoint/path-style 差异。

### Verification / Migration
覆盖生产缺少 domainUrl、开发回退开关、空格和非 ASCII key、尾斜杠以及 PRIVATE 对象不会生成公共 URL 的测试。

## ADR-009: 旧 OssService URL 方法兼容委托统一访问解析

**Status:** accepted
**Source:** LOG-010, user decision
**Supersedes:** none

### Context
selectUrlByIds 和 selectByIds 已被跨模块调用。直接删除会扩大迁移面；保留旧的统一签名行为又会让公共对象无法获得稳定 URL，并形成两套分类实现。

### Decision
保留现有方法签名，将 URL 生成委托给 resolveAccessUrl。公共对象返回稳定 URL，受控对象返回短时签名 URL。调用方继续在调用前完成业务授权；OSS 管理列表不因兼容改造而自动携带 URL，也不新增匿名 Controller。

### Trade-off
旧方法的返回 URL 生命周期会因对象类型而不同，调用方需读取或理解访问类型；换取二进制和源码迁移范围可控、分类逻辑集中。

### Consequences
JavaDoc 和结果合同必须说明 PUBLIC 的 expiresAt 为空、PRIVATE 的 expiresAt 非空。批量方法应避免重复读取配置或无界生成签名。

### Verification / Migration
合同测试覆盖混合 PUBLIC/PRIVATE ids、空集合、不存在对象和禁用配置；扫描调用点确认业务授权仍在，并验证管理列表不生成 URL。

## ADR-010: 私有下载 TTL 由服务端命名访问策略约束

**Status:** accepted
**Source:** LOG-011, user decision
**Supersedes:** none

### Context
全局默认 2 分钟适合多数读取，但预览、导出等业务可能需要不同窗口。允许客户端直接指定 TTL 会把 bearer URL 的风险窗口交给不可信输入。

### Decision
保留全局默认下载 TTL 2 分钟。服务端可定义命名访问策略，在统一配置的安全最小值和最大值内覆盖 TTL；调用方只选择被允许的策略名，外部客户端不得直接提交 TTL。未知策略、越界配置和非法持续时间 fail-fast。

### Trade-off
新增策略配置和校验复杂度；换取业务可调性，同时保持有效期由服务端安全边界控制。

### Consequences
resolveAccessUrl 默认使用全局策略，明确业务可以选择服务端暴露的命名策略。返回 expiresAt 必须反映实际签发时间，签名 URL 不能写入业务持久数据或缓存超过其有效期。

### Verification / Migration
覆盖默认 2 分钟、合法覆盖、未知策略、低于下限、高于上限、客户端 TTL 注入和时钟边界测试；现有调用方不传策略时保持默认行为。

## ADR-011: 命名上传策略固定绑定存储配置

**Status:** accepted
**Source:** LOG-012, user decision
**Supersedes:** none

### Context
现有直传已经以 uploadPolicy 约束 MIME、大小和数量，但初始化默认使用全局 OssClient。公共与受控 Bucket 分离后，客户端若能提交 configKey 就能自行改变对象访问类型。

### Decision
每个服务端命名 uploadPolicy 增加并固定绑定 storageConfigKey。公共门户策略只能绑定 PUBLIC 配置，受控附件策略只能绑定 PRIVATE 配置；客户端请求只携带允许的策略名，不得提交或覆盖 configKey。应用启动时校验配置存在、启用且访问类型符合策略声明。

### Trade-off
新增上传用途时必须先配置策略与存储路由，不能临时选择任意 Bucket；换取访问分类由可信配置决定，并完整复用现有 UploadTicket、直传和完成协议。

### Consequences
UploadTicket 必须冻结策略解析出的实际 configKey，初始化、完成、取消和回收均使用该配置，不能在后续阶段重新读取默认 OssClient。策略或配置变更不得改变已签发票据的目标。

### Verification / Migration
覆盖公共与受控策略正确路由、未知策略、客户端 configKey 注入、配置禁用、类型不匹配和策略变更后的在途 Ticket；确认对象 sys_oss.service 与目标配置一致。

## ADR-012: 应用访问策略只保留 PRIVATE 与 PUBLIC_READ

**Status:** accepted
**Source:** LOG-013, user decision
**Supersedes:** none

### Context
当前 accessPolicy 在枚举、SQL 注释和管理 UI 中分别出现 private、public、custom、PUBLIC_READ_WRITE 和 PUBLIC_READ 等不一致解释。目标能力只需要受控读取与公共匿名读取，匿名写没有业务需求且会扩大攻击面。

### Decision
应用支持的存储读取类型收敛为 PRIVATE 与 PUBLIC_READ。管理界面、数据库字典与增量脚本、Java 枚举、配置校验和测试使用相同名称与语义。PUBLIC_READ 只开放匿名读取，任何上传、覆盖和删除仍使用鉴权控制面或预签名请求；不支持 PUBLIC_READ_WRITE/custom。

### Trade-off
旧配置值需要一次显式迁移，可能阻止含义不明的配置自动升级；换取跨层语义一致和更小权限面。

### Consequences
升级脚本必须对已知旧值给出明确映射，对不能证明含义的记录 fail-fast 并要求人工确认。Provider Policy 诊断按两种类型验证，不再为匿名写提供分支。

### Verification / Migration
检查数据库、后端和前端无遗留歧义标签；验证 PUBLIC_READ 匿名 GET 成功但匿名 PUT/DELETE 失败，PRIVATE 匿名 GET 失败；不确定旧值不得被自动公开。

## ADR-013: 有存量对象的配置边界变更必须走显式迁移

**Status:** accepted
**Source:** LOG-014, user decision
**Supersedes:** none

### Context
对象通过 sys_oss.service 绑定 storage configKey，并由该配置唯一决定 Bucket 和访问类型。若普通配置编辑直接改变 Bucket 或 PRIVATE/PUBLIC_READ，历史元数据会立即指向不同位置或获得不同安全语义，而对象本身尚未迁移。

### Decision
configKey 存在存量对象时，普通配置更新不得改变 Bucket 或访问类型。此类变化只能通过显式、可审计、可验证且可回滚的存储边界迁移完成。迁移依次验证目标配置、复制对象、验证内容、原子切换 sys_oss.service、验证业务读取，并在安全窗口后清理源对象。凭据、endpoint、domainUrl 等不改变对象归属与访问类型的字段仍可通过常规配置流程轮换。

### Trade-off
Bucket 重组和公开转换需要迁移任务，不能通过一次配置编辑即时完成；换取历史对象不会被误公开、失联或静默改变访问合同，同时保留正常运维能力。

### Consequences
配置更新服务必须检查引用对象并区分边界字段与可轮换字段。迁移需要幂等状态、批次审计、失败恢复和源对象延迟清理；业务仍只保存 ossId，迁移通过 service 归属切换保持引用稳定。

### Verification / Migration
覆盖无对象配置允许改边界、有对象配置拒绝普通修改、复制失败、校验失败、切换失败、重复执行和回滚；迁移前后业务 ossId 不变，公开/受控读取行为与目标配置一致，源对象只在验证完成后清理。
