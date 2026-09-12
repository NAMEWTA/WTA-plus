# 用户密码策略与临时凭据设计日志

## LOG-001 — 2026-08-28T10:09:42+0800 — 用户密码优化范围
- **设计树节点：** D-001
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 本 change 需要覆盖哪些用户密码流程。
- **事实与来源：** 用户指定 admin-web 用户管理和对应后端，要求临时密码、随机重置默认密码及注册强度校验；`USER-DECISION:2026-08-28`。
- **选项：** 只改页面；只改重置；覆盖管理、注册和认证纵切片。
- **推荐：** 覆盖完整纵切片，否则前端校验可被直接 HTTP 请求绕过，临时密码也无法进入认证策略。
- **结论：** 本 change 覆盖用户管理临时密码、管理员重置密码和公开注册，具体相邻密码入口覆盖面由 D-009 决定。
- **原因：** 三项需求共享密码策略并跨越管理与认证边界。
- **影响工件：** CONTEXT / Spec / Ticket / Goal Plan
- **约束或不变量：** 当前仅授权创建规划工件，不授权产品实现。
- **后续：** 完成设计树后进入 Spec 与 Tickets。
- **替代/被替代：** 无

## LOG-002 — 2026-08-28T10:09:42+0800 — 最低密码强度
- **设计树节点：** D-002
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 密码必须满足的最低强度。
- **事实与来源：** 用户明确指定大小写字母、数字、特殊字符且至少 8 位；仓库已有未启用的 `RegexConstants.PASSWORD` 表达相同基线；`USER-DECISION:2026-08-28`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/constant/RegexConstants.java</Path>`。
- **选项：** 沿用 5/6 位旧校验；固定正则；配置驱动且最低不弱于该基线。
- **推荐：** 服务端统一策略作为权威，前端只做一致的即时反馈。
- **结论：** 新写入密码至少 8 位，并至少各含一个大写字母、小写字母、数字和特殊字符。
- **原因：** 这是用户明确要求，也是代码中已有但尚未启用的目标强度。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 不在登录读取路径拒绝存量弱密码，避免无迁移地锁死现有账号。
- **后续：** D-006 决定配置载体，D-009 决定写入入口覆盖面。
- **替代/被替代：** 无

## LOG-003 — 2026-08-28T10:09:42+0800 — 一分钟临时密码目标
- **设计树节点：** D-003
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 临时密码的有效窗口与用途。
- **事实与来源：** 用户要求管理员生成符合规则、在一分钟内可登录的临时密码；当前 `PasswordAuthStrategy` 只校验 `sys_user.password` 的 BCrypt 哈希，登录后按 Client 签发普通 Token；`USER-DECISION:2026-08-28`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/impl/PasswordAuthStrategy.java</Path>`。
- **选项：** 临时覆盖永久密码；独立短期凭据；仅生成但不接认证。
- **推荐：** 独立短期凭据，不修改永久密码。
- **结论：** 临时密码自签发起有效 60 秒，并可进入密码认证流程；消费和会话语义尚由 D-007、D-008 决定。
- **原因：** 覆盖数据库永久密码会把短期恢复能力变成破坏性重置。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 临时凭据不得绕过用户状态、Client、登录域或验证码校验。
- **后续：** 决定一次性消费、Client 作用域与会话限制。
- **替代/被替代：** 无

## LOG-004 — 2026-08-28T10:09:42+0800 — 重置默认密码不再使用 123456
- **设计树节点：** D-004
- **轮次与依赖：** round 1 / D-002
- **状态：** confirmed
- **问题：** 管理员重置密码的默认值来源。
- **事实与来源：** 用户要求随机且合规，不再使用 123456；当前 SQL 的 `sys.user.initPassword` 是 123456，前端重置仍要求管理员手填 5–20 位密码；`USER-DECISION:2026-08-28`，`CODE:<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`，`CODE:<Path>plus-ui-namewta/packages/web-domains/system/src/user/UserPage.vue</Path>`。
- **选项：** 固定 123456；前端随机；服务端按策略产生。
- **推荐：** 服务端按策略产生并只在响应中返回一次明文。
- **结论：** 重置默认值不能再是 123456，random 模式必须生成符合 D-002 的密码。
- **原因：** 服务端生成才能同时控制规则、随机源和不可绕过校验。
- **影响工件：** CONTEXT / Spec / Ticket / SQL migration
- **约束或不变量：** 不修改上游 `ry_vue.sql`；NAMEWTA 配置迁移只能追加到 `script/sql/namewta/DML.sql`。
- **后续：** D-006、D-010 决定配置与明文交付交互。
- **替代/被替代：** 无

## LOG-005 — 2026-08-28T10:09:42+0800 — 注册服务端强校验
- **设计树节点：** D-005
- **轮次与依赖：** round 1 / D-002
- **状态：** confirmed
- **问题：** 公开注册是否只做前端校验。
- **事实与来源：** 用户要求注册执行同样规则；当前前端只校验 5–20 位及禁用字符，`RegisterBody` 只校验 5–30 位，强度正则被注释；`USER-DECISION:2026-08-28`，`CODE:<Path>plus-ui-namewta/apps/admin-web/src/views/register.vue</Path>`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/model/RegisterBody.java</Path>`。
- **选项：** 仅前端；仅 Bean Validation 固定正则；前端反馈加服务端动态策略。
- **推荐：** 前端即时反馈加服务端策略权威校验。
- **结论：** 注册持久化前必须在服务端拒绝不符合 D-002 的密码，前端呈现相同规则与错误提示。
- **原因：** 客户端校验不是安全边界，动态策略也不能只固化在注解正则中。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 保持 Client `registerEnabled`、登录域授权和验证码现有失败关闭行为。
- **后续：** D-006 确定策略读取方式，D-009 确定其他入口。
- **替代/被替代：** 无

## LOG-006 — 2026-08-28T10:09:42+0800 — 当前代码与配置事实
- **设计树节点：** 不适用
- **轮次与依赖：** discovery / 无
- **状态：** confirmed
- **问题：** 仓库中默认密码、强度、重置和认证实际位于何处。
- **事实与来源：** `sys.user.initPassword` 是 `sys_config` 参数而非 `sys_dict`；新增用户页面和 Excel 导入读取该值，管理员重置要求手填，注册与个人改密规则彼此不一致，后端多个写入口未执行统一强度策略；临时登录必须修改 `ruoyi-admin` 认证策略，注册模型位于 `ruoyi-api`。来源为本轮源码、SQL、测试和项目规范检索。
- **选项：** 按目录名猜测；仅改用户管理 SFC/Controller；建立跨模块统一策略。
- **推荐：** 以 `ruoyi-system` 拥有策略/临时凭据服务，`ruoyi-admin` 在认证和注册组装层调用，前端 domain/web-domain 消费明确 HTTP 合同。
- **结论：** 最终计划必须跨 `plus-ui-namewta`、`ruoyi-system`、`ruoyi-admin`、`ruoyi-api`、必要的 common 常量及 NAMEWTA DML；仅修改用户点名的两个目录无法完成需求。
- **原因：** 真实调用链跨越管理、认证、公共请求模型和运行时缓存边界。
- **影响工件：** Spec / Ticket / Goal Plan
- **约束或不变量：** 前端不生成安全密码；后端不记录密码到 `@Log` 数据库审计参数/响应；专用 HTTP 原样日志仍遵循永久 ADR-0017。
- **后续：** 关闭 frontier 后把路径所有权和依赖顺序写入 Tickets Map/Goal Plan。
- **替代/被替代：** 无

## LOG-007 — 2026-08-28T10:22:53+0800 — 继续使用 sys_config
- **设计树节点：** D-006
- **轮次与依赖：** round 1 / D-002
- **状态：** confirmed
- **问题：** 密码策略继续使用 `sys_config`，还是迁移到真正的 `sys_dict`。
- **事实与来源：** 用户明确选择继续使用 `sys_config`；当前 `sys.user.initPassword` 也属于参数配置；`USER-DECISION:2026-08-28`。
- **选项：** `sys_config` 类型化 JSON；`sys_dict` 多项枚举；新建专用表。
- **推荐：** `sys_config` 类型化 JSON，保持当前配置所有权并校验跨字段不变量。
- **结论：** 统一密码策略继续由 `sys_config` 承载，不迁移到 `sys_dict`。
- **原因：** 结构化策略与生成规则是运行配置，不是用于翻译的枚举集合。
- **影响工件：** ADR / CONTEXT / Spec / Ticket / SQL migration
- **约束或不变量：** fixed 模式密码也必须通过强度校验；畸形配置不能回退到 123456。
- **后续：** D-010、D-013、D-016 决定交付、失败与公开投影。
- **替代/被替代：** 无

## LOG-008 — 2026-08-28T10:22:53+0800 — 临时密码单次原子消费
- **设计树节点：** D-007
- **轮次与依赖：** round 1 / D-003
- **状态：** confirmed
- **问题：** 临时密码是否可重复使用以及新签发如何处理旧值。
- **事实与来源：** 用户完整接受单次使用、成功后原子消费、新签发覆盖旧值且永久密码不受影响；`USER-DECISION:2026-08-28`。
- **选项：** 60 秒内重复使用；单次使用；覆盖永久密码。
- **推荐：** Redis 中只保存不可逆校验值，以 60 秒 TTL 和原子校验删除实现一次性消费。
- **结论：** 临时密码只允许一次成功认证；新签发作废上一枚；不修改用户永久密码。
- **原因：** 兼顾恢复能力、集群一致性和最小重放窗口。
- **影响工件：** ADR / CONTEXT / Spec / Ticket
- **约束或不变量：** 错误密码不得消费；并发正确密码只能有一个请求消费成功。
- **后续：** D-008、D-015 决定会话与 Client 作用域。
- **替代/被替代：** 无

## LOG-009 — 2026-08-28T10:22:53+0800 — 所有密码写入口统一校验
- **设计树节点：** D-009
- **轮次与依赖：** round 1 / D-001, D-002, D-005
- **状态：** confirmed
- **问题：** 强度规则是否只覆盖注册与管理员重置。
- **事实与来源：** 用户选择全部覆盖，并要求存量弱密码暂不阻断登录；`USER-DECISION:2026-08-28`。
- **选项：** 仅需求点；部分入口；所有密码写入口统一。
- **推荐：** 注册、管理员新增、Excel 导入、管理员重置和个人中心改密全部调用同一服务端策略。
- **结论：** 所有新写入密码统一校验；存量弱密码在后续改密或重置时收敛。
- **原因：** 任一未覆盖入口都会继续把弱密码写入数据库。
- **影响工件：** CONTEXT / Spec / Ticket / Goal Plan
- **约束或不变量：** 不对登录请求本身执行新强度拒绝；服务端是权威，前端规则不能替代。
- **后续：** D-013 决定非法配置失败合同。
- **替代/被替代：** 无

## LOG-010 — 2026-08-28T10:22:53+0800 — 独立临时密码权限
- **设计树节点：** D-011
- **轮次与依赖：** round 1 / D-001, D-003
- **状态：** confirmed
- **问题：** 临时密码签发是否复用永久密码重置权限。
- **事实与来源：** 用户选择新增 `system:user:temporaryPassword`，用于独立授权和审计；`USER-DECISION:2026-08-28`。
- **选项：** 复用 `system:user:resetPwd`；新增独立权限。
- **推荐：** 独立权限并在后端、菜单数据和前端按钮三处保持一致。
- **结论：** 临时密码签发使用 `system:user:temporaryPassword`，不由重置权限隐式授予。
- **原因：** 签发可直接用于登录的凭据应遵循最小权限和独立审计。
- **影响工件：** CONTEXT / Spec / Ticket / SQL migration
- **约束或不变量：** 前端隐藏不是授权边界；后端必须校验权限和用户数据范围。
- **后续：** 下游 DML 增加功能权限并按明确角色授权。
- **替代/被替代：** 无

## LOG-011 — 2026-08-28T10:22:53+0800 — 存量弱密码渐进收敛
- **设计树节点：** D-017
- **轮次与依赖：** round 2 / D-009
- **状态：** confirmed
- **问题：** 新策略启用后是否立即拒绝已有弱密码登录。
- **事实与来源：** 用户在 Q3 中明确存量弱密码暂不阻断登录，并在下一次改密时收敛；`USER-DECISION:2026-08-28`。
- **选项：** 立即阻断；后台批量迁移明文不可行；在下一次密码写入时收敛。
- **推荐：** 保持已有 BCrypt 校验兼容，仅对新写入执行强策略。
- **结论：** 存量弱密码可以继续登录；个人改密或管理员重置时必须满足新策略。
- **原因：** 系统无法从 BCrypt 哈希恢复或安全判断全部字符类别，立即阻断会在没有恢复流程时锁死账号。
- **影响工件：** CONTEXT / Spec / Ticket / Migration
- **约束或不变量：** 兼容只适用于已有哈希，不允许任何新入口继续写入弱密码。
- **后续：** 验收覆盖存量弱密码可登录及改密后策略收敛。
- **替代/被替代：** 无

## LOG-012 — 2026-08-28T11:36:10+0800 — 临时认证后使用普通会话
- **设计树节点：** D-008
- **轮次与依赖：** round 2 / D-007
- **状态：** confirmed
- **问题：** 临时密码认证成功后是否需要特殊会话标记或限制。
- **事实与来源：** 用户明确临时密码只在密码认证阶段临时生效，登录后一切与普通登录一致，不需要标记、限制或权限变化；`USER-DECISION:2026-08-28`。
- **选项：** 受限会话；短时会话；无标记普通会话。
- **推荐：** 原推荐为受限会话；用户基于管理员临时登录指定账号排障的业务目的，明确选择普通会话。
- **结论：** 临时密码认证成功后复用普通登录会话签发流程，不增加任何临时会话语义。
- **原因：** 临时性只属于认证凭据，不属于账号身份、权限或登录后的业务能力。
- **影响工件：** ADR / CONTEXT / Spec / Ticket / Goal Plan
- **约束或不变量：** 认证前仍执行用户状态、验证码、Client 和登录域准入；认证成功后的 Token 生命周期按目标 Client 正常配置。
- **后续：** 验收证明两种密码认证产生等价会话属性。
- **替代/被替代：** 无

## LOG-013 — 2026-08-28T11:36:10+0800 — 服务端生成可编辑重置候选密码
- **设计树节点：** D-010
- **轮次与依赖：** round 2 / D-004, D-006
- **状态：** confirmed
- **问题：** 随机重置密码在生成、编辑和最终写入之间如何交互。
- **事实与来源：** 用户要求服务端直接生成候选密码，并允许管理员编辑后再确认提交；`USER-DECISION:2026-08-28`。
- **选项：** 生成即重置；前端生成后提交；服务端生成候选值、编辑后最终提交。
- **推荐：** 原推荐为服务端生成即重置；用户选择两阶段确认以保留管理员编辑能力。
- **结论：** 生成候选值不改变账户；最终确认提交时才重置永久密码，服务端重新校验编辑后的最终值。
- **原因：** 保持服务端生成规则权威，同时满足管理员确认与调整密码的操作需要。
- **影响工件：** CONTEXT / Spec / Ticket / API contract / frontend UX
- **约束或不变量：** 前端不得自行生成；候选值不在服务端持久化为账户密码；最终值必须满足有效策略；数据库审计日志不得记录明文请求或响应。
- **后续：** D-012 决定最终重置后已有会话的处理。
- **替代/被替代：** 无

## LOG-014 — 2026-08-28T11:36:10+0800 — 返回稳定且详细的密码规则错误
- **设计树节点：** D-013
- **轮次与依赖：** round 2 / D-006, D-009
- **状态：** confirmed
- **问题：** 密码或策略不合规时错误合同应提供多少细节。
- **事实与来源：** 用户要求按不同原因详细提示，例如长度不足或缺少具体字符类别，使用户和管理员能准确修正；`USER-DECISION:2026-08-28`。
- **选项：** 单一笼统消息；自由文本；稳定的原因代码与对应明细消息。
- **推荐：** 使用稳定、可区分的原因代码和本地化明细消息，服务端统一产生并保持失败关闭。
- **结论：** 校验响应必须区分长度、大写、小写、数字、特殊字符等失败原因，不得只返回“密码不符合规则”。
- **原因：** 精确反馈能直接指导修正，并避免各前端重复推断规则。
- **影响工件：** CONTEXT / Spec / Ticket / API contract / tests
- **约束或不变量：** 错误不得泄露 fixed 密码、随机字符池或其他生成内部参数；非法配置不得静默回退到 123456。
- **后续：** Spec 定义稳定代码、顺序、多错误表达和各入口映射。
- **替代/被替代：** 无

## LOG-015 — 2026-08-28T11:36:10+0800 — 临时密码采用用户级 Client 作用域
- **设计树节点：** D-015
- **轮次与依赖：** round 2 / D-007
- **状态：** confirmed
- **问题：** 临时密码是否绑定单个 Client。
- **事实与来源：** 用户明确临时密码只是密码授权，账户本身不变，用户原本能访问哪些 Client 就可在哪些 Client 使用；`USER-DECISION:2026-08-28`。
- **选项：** 绑定签发 Client；用户级且继续执行目标 Client 准入；绕过 Client 准入。
- **推荐：** 原推荐为绑定目标 Client；用户明确选择用户级凭据并保留既有 Client 准入。
- **结论：** 临时密码以用户为作用域，不绑定某个 Client；在任一目标 Client 登录时仍执行该用户原有准入规则。
- **原因：** 临时凭据只替代密码校验，不扩张也不收缩账户已有的 Client 访问资格。
- **影响工件：** ADR / CONTEXT / Spec / Ticket / authentication tests
- **约束或不变量：** 新签发在用户级覆盖旧值；临时密码绝不能绕过目标 Client 的用户类型、状态或登录域校验。
- **后续：** 跨 Client 正向与负向矩阵纳入验收。
- **替代/被替代：** 无

## LOG-016 — 2026-08-28T11:36:10+0800 — 公开端点仅投影非敏感密码规则
- **设计树节点：** D-016
- **轮次与依赖：** round 2 / D-006
- **状态：** confirmed
- **问题：** 未认证页面如何取得动态规则而不泄露安全配置。
- **事实与来源：** 用户接受只公开最小长度、必需字符类别、允许特殊字符等非敏感规则，并明确 fixed 密码和生成内部参数永不下发；`USER-DECISION:2026-08-28`。
- **选项：** 下发完整配置；前端硬编码；下发安全投影。
- **推荐：** 通过公开认证上下文提供最小安全投影，服务端保持最终权威。
- **结论：** 注册和管理表单消费非敏感策略投影；fixed 密码及随机生成内部配置不得通过公开端点返回。
- **原因：** 前端需要动态反馈所需信息，但不应获得可用于推断默认或生成密码的内部配置。
- **影响工件：** ADR / CONTEXT / Spec / Ticket / API contract
- **约束或不变量：** 公开投影不能替代服务端校验；配置变化后服务端立即按最新有效策略裁决。
- **后续：** Spec 定义公开 DTO 与缓存刷新行为。
- **替代/被替代：** 无

## LOG-017 — 2026-08-28T11:52:23+0800 — 永久密码重置保持既有会话行为
- **设计树节点：** D-012
- **轮次与依赖：** round 3 / D-010
- **状态：** confirmed
- **问题：** 管理员重置永久密码后是否新增会话失效行为。
- **事实与来源：** 用户要求沿用当前系统操作；当前 `SysUserController.resetPwd` 只哈希并调用 `SysUserServiceImpl.resetUserPwd` 更新 `sys_user.password`，该服务不调用 `ClientSessionService` 或 Sa-Token 注销；`USER-DECISION:2026-08-28`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysUserController.java</Path>`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>`。
- **选项：** 全部踢出；只踢部分 Client；保持当前不踢会话。
- **推荐：** 原推荐为全部踢出；用户选择保持当前行为以避免本 change 隐式改变会话合同。
- **结论：** 永久密码重置成功后不主动注销既有会话，未消费临时密码也不因该操作额外删除。
- **原因：** 本 change 只优化密码生成与校验，不改变当前重置密码后的会话生命周期。
- **影响工件：** CONTEXT / Spec / Ticket / compatibility tests
- **约束或不变量：** 新登录必须使用重置后的永久密码；临时密码仍按自身 60 秒、覆盖和单次消费合同运行。
- **后续：** 验收固定“重置后原 Token 仍可用”的兼容行为。
- **替代/被替代：** 无

## LOG-018 — 2026-08-28T11:52:23+0800 — 权限变更使授权快照失效
- **设计树节点：** D-018
- **轮次与依赖：** round 3 / 无
- **状态：** confirmed
- **问题：** 角色权限或用户角色变化后是否允许旧登录快照继续使用。
- **事实与来源：** 用户要求调整角色菜单权限或用户角色时，使既有会话失效并要求重新登录；当前权限、角色和数据权限在登录时写入 `LoginUser` 快照，运行时 `SaPermissionImpl` 直接读取该快照；角色权限接口同时修改菜单权限与数据权限；`USER-DECISION:2026-08-28`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysLoginService.java</Path>`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/service/SaPermissionImpl.java</Path>`。
- **选项：** 等待过期；原地刷新 Token；强制重新登录生成新快照。
- **推荐：** 强制所有受影响授权快照失效，保持登录时快照单一来源。
- **结论：** 角色菜单/数据权限和用户角色关系变化都必须触发受影响会话注销。
- **原因：** 旧 Token 持有变更前权限，继续使用会造成权限收回延迟或新增权限不可见。
- **影响工件：** CONTEXT / ADR / Spec / Ticket / security tests
- **约束或不变量：** 不在旧 Token 中做局部补丁；新会话只能通过重新登录按最新数据库状态生成。
- **后续：** D-020 决定跨 Client 的精确失效范围。
- **替代/被替代：** 无

## LOG-019 — 2026-08-28T11:52:23+0800 — Redis 与全部 JVM 本地缓存同步失效
- **设计树节点：** D-019
- **轮次与依赖：** round 3 / D-018
- **状态：** confirmed
- **问题：** 注销 Token 是否足以立即消除多级缓存中的旧授权快照。
- **事实与来源：** 用户明确要求清理 Redis 和内存缓存；当前 `PlusSaTokenDao` 在 Redis 前有 5 秒静态 Caffeine，`SYS_ROLE_CUSTOM` Spring Cache 使用 Redis 加 30 秒本地 Caffeine，删除只显式失效执行 JVM，仓库未发现跨实例缓存失效广播；`USER-DECISION:2026-08-28`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path>`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/manager/CaffeineCacheDecorator.java</Path>`。
- **选项：** 等待 TTL；仅清 Redis；仅清当前 JVM；Redis 与全部 JVM 同步失效。
- **推荐：** 通过集中会话失效边界和集群可传播的本地缓存失效机制同时清理，不依赖自然过期。
- **结论：** 权限变更完成时，相关 Redis Token/Session、权限数据缓存和所有实例的本地副本都必须立即失效。
- **原因：** 只删除 Redis 仍可能被其他实例的本地 Caffeine 短暂读到旧 Token 或旧数据权限。
- **影响工件：** CONTEXT / ADR / Spec / Ticket / common-satoken / common-redis / integration tests
- **约束或不变量：** 使用 Sa-Token/缓存抽象完成删除，不在业务服务散落硬编码 key；清理不成功不能静默报告完全成功。
- **后续：** D-020 关闭后在 ADR 固化精确作用域与跨实例机制。
- **替代/被替代：** 无

## LOG-020 — 2026-08-28T11:56:34+0800 — 按受影响 Client 精确注销会话
- **设计树节点：** D-020
- **轮次与依赖：** round 4 / D-018, D-019
- **状态：** confirmed
- **问题：** 权限变化后是否连带注销权限未变化的其他 Client 会话。
- **事实与来源：** 用户选择按受影响 Client 精确失效；当前角色归属单一 Client，角色权限更新通过 `kickoutClient` 注销该 Client 会话，用户角色更新通过 `kickoutUserClient` 注销指定用户在目标 Client 的会话；`USER-DECISION:2026-08-28`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysRoleController.java</Path>`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientSessionService.java</Path>`。
- **选项：** 受影响 Client 精确失效；受影响用户全部 Client 失效；全局全部会话失效。
- **推荐：** 按权限归属精确失效，避免其他 Client 无关会话中断。
- **结论：** 角色菜单/数据权限变化注销角色所属 Client 的全部会话；用户角色关系变化注销该用户在目标 Client 的全部会话；其他 Client 会话不受影响。
- **原因：** 角色、菜单和运行时权限快照按 Client 隔离，其他 Client 的权限状态没有变化。
- **影响工件：** ADR / CONTEXT / Spec / Ticket / cross-client tests
- **约束或不变量：** “精确”仅限制 Client 范围，不放宽 D-019；目标范围内的 Redis 与全部实例本地缓存仍必须完整清理。
- **后续：** D-014 确认完整验收矩阵。
- **替代/被替代：** 无

## LOG-021 — 2026-08-28T11:58:58+0800 — 完整验收矩阵达成共识
- **设计树节点：** D-014
- **轮次与依赖：** round 5 / D-008, D-009, D-010, D-011, D-012, D-013, D-015, D-016, D-017, D-018, D-019, D-020
- **状态：** confirmed
- **问题：** 已关闭的设计分支能否共同构成下游规格与实施计划的完整验收合同。
- **事实与来源：** 用户明确确认最终十一项验收矩阵；`USER-DECISION:2026-08-28`。
- **选项：** 补充遗漏后继续 Grill；确认并进入规格阶段。
- **推荐：** 确认矩阵并进入 `specdev/spec`，把行为、错误、接口、安全、迁移和验证写成可执行规格。
- **结论：** 所有适用设计分支已经走过并达成共识，design tree 转为 `consensus`。
- **原因：** 密码策略、临时凭据、重置兼容、Client 准入、权限变更会话失效、缓存一致性和验收证据均已有明确结论。
- **影响工件：** design-tree / CONTEXT / ADR / downstream Spec
- **约束或不变量：** Grill 共识不构成产品实现、提交、集成或部署授权。
- **后续：** 路由到 `<Path>{roots.workflows}/specdev/S-spec/S-spec.md</Path>`。
- **替代/被替代：** 无

## LOG-022 — 2026-08-28T14:39:51+0800 — 前端最新架构基线复核
- **设计树节点：** 不适用
- **轮次与依赖：** planning refresh / LOG-021
- **状态：** confirmed
- **问题：** 原规划工件形成后，前端继续完成共享导航运行时与 manifest-only 导航优化，需确认密码策略与用户凭据计划仍落在当前 owner 和真实验证入口。
- **事实与来源：** 当前 frontend 为 `main@efb8e0d7fae86cfd09c1f55204e8b486a499a3cc` 且 clean；`domain-admin` 仍拥有 `/auth/client/context` 与注册服务，注册和 profile 仍是 Admin App 私有静态页面；`domain-system` 拥有用户 HTTP 服务，`web-domain-system` 的 `createSystemWebDomain` manifest 拥有用户页组件键和 `system:user:*` 权限声明；`adminManifestRegistry.ts` 只负责显式组合 manifest 与注入 `SystemWebRuntime`，业务页面不得回写 `router/index.ts` 静态路由。Admin 国际化目录为 `apps/admin-web/src/lang/**`，不是不存在的 `src/locales/**`。来源：`CODE:<Path>plus-ui-namewta/packages/domains/admin/src/index.ts</Path>`、`CODE:<Path>plus-ui-namewta/packages/domains/system/src/index.ts</Path>`、`CODE:<Path>plus-ui-namewta/packages/web-domains/system/src/index.ts</Path>`、`CODE:<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>`、`CODE:<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>`、`GIT:plus-ui-namewta@efb8e0d`。
- **选项：** 保留旧路径与旧 SHA；把注册/profile 迁入 web-domain；按当前 owner 刷新路径、命令和 manifest 约束。
- **推荐：** 保持现有领域与 App 边界，只刷新 T-06/T-07 及上游投影，不发动额外页面迁移。
- **结论：** T-06 继续由 `domain-admin` + Admin 私有注册/profile 页面消费公开策略，国际化写入 `src/lang/**`；T-07 在 `domain-system`、`web-domain-system` manifest 和 Admin runtime 组合入口交付用户凭据 UI，不新增静态业务路由。OpenAPI 漂移检查使用真实命令 `pnpm --filter @namewta/tooling-openapi openapi:check`。
- **原因：** 该边界与当前公开导出、manifest-only 架构检查和后端模块映射一致，且无需改变已确认外部行为。
- **影响工件：** Spec / T-06 / T-07 / Tickets Map / Goal Plan
- **约束或不变量：** 不手改生成 OpenAPI；不深层导入；权限声明仍由 web-domain manifest 拥有；App 只负责选择与宿主端口注入；本轮不授权产品实现。
- **后续：** 重新运行 SpecDev spec/tickets/goal-plan validator 与前端架构/OpenAPI 只读门禁。
- **替代/被替代：** 补充 LOG-006 的前端规划基线；不替代任何用户决定或 ADR。

## LOG-023 — 2026-08-28T18:52:00+0800 — Goal Plan 本地实现与 G-50 完成
- **设计树节点：** 不适用
- **轮次与依赖：** implementation / LOG-022
- **状态：** confirmed
- **问题：** 八张 Ticket 是否已用真实代码、clean candidate 和 required E2E 完成本地交付。
- **事实与来源：** backend result `98f1e76e7945b7bd003abf6833d0efecea416a12`、frontend result `8aa184b353c5a37ee555feb8be808fe9ba885297`；T-01 至 T-08 Evidence；真实 MySQL/Redis/HTTP、双 JVM、多 Client、并发 CAS、migration 和浏览器 Gate 均已执行。
- **选项：** 在存在 skipped-as-passed 或未集成 source 时提前关闭；保持 active；在所有 Gate 和父仓精确组合通过后关闭。
- **推荐：** 仅接受第三项。
- **结论：** `AC-001` 至 `AC-024` 全部闭合，G-00 至 G-50 完成；父仓仅组合本 change 工件、长期定制地图和两个验证过的 gitlink。
- **原因：** 每张 Ticket 均有非空 source、通过的 candidate、子模块 main result 和 Lead Evidence；最终 backend 190 tests、真实 MySQL/Redis、双 bundle、frontend 全门禁及 48 项 Playwright 绿色。
- **影响工件：** Goal Plan / Tickets Map / T-07 / T-08 / change status / Evidence / customization map / parent gitlinks
- **约束或不变量：** Evidence 不记录凭据或 Token；生产 DML、部署、角色授权、push 与 worktree cleanup 未授权且未执行。
- **后续：** 生产发布若另行批准，必须重新冻结 SHA、备份并按 T-08 发布/补偿合同执行。
- **替代/被替代：** 完成 LOG-022 后续实现，不替代任何产品决定或永久 ADR。

## LOG-024 — 2026-08-28T19:08:06+0800 — 完成审计补正 Ticket 提交祖先关系
- **设计树节点：** 不适用
- **轮次与依赖：** completion audit / LOG-023
- **状态：** confirmed
- **问题：** T-04 历史 source 与 result 虽然 parent/tree 相同，但 source 并非 result 祖先，不满足 required workspace 的提交可追溯合同。
- **事实与来源：** source `29cf4dce083afe18aa59d68b44ae41885ee62cd9` 与 replay `966d426d0e94e5ff69cf62c8ec5a71018d911af6` 均以 `65dfb11f96c1ec2ee7a40927bf43fc4ced205ff1` 为父且 tree 同为 `24b503d87cbaaf1e06458e5b65e033ee6dacf6ca`；最终 backend tree 始终为 `a8ac9a101f1719f0df640a782491e9072b9388b3`。
- **结论：** 创建双父 T-04 candidate `94b0fb7c61307bab073a6a273c1d2b4eba9e4f94`，并以最终 backend result `42e06c0f713e0d724813800505e5bb5b40ab563b` 显式包含所有 Ticket source/candidate；两次 merge 均为零产品 diff。
- **验证：** 最终 tree 上 `./mvnw test` 为 190 tests、0 failure/error；full/core 两个 clean bundle 均通过；所有 backend source 与 T-04 candidate 的 `merge-base --is-ancestor` 均返回成功。
- **影响工件：** T-04/T-08 Evidence / Goal Plan / Tickets Map / change status / parent backend gitlink
- **约束或不变量：** 不改产品 tree，不吸收前后端和父仓无关 dirty 内容；不清理 worktree，不推送、部署或执行生产 DML。
- **替代/被替代：** 补正 LOG-023 的最终 backend result 与提交拓扑，不改变其产品验收结论。

## LOG-025 — 2026-08-28T23:42:03+0800 — 已完成 worktree 全面清理
- **设计树节点：** 不适用
- **轮次与依赖：** post-completion cleanup / LOG-024
- **状态：** confirmed
- **问题：** 已完成 change 的 source/candidate worktree 与本地分支是否仍需保留。
- **事实与来源：** 用户明确要求合并已完成 worktree 并全面清理；本 change 的 16 个 source/candidate worktree 均 clean，且其 HEAD 均已被对应子仓库 `main` 包含。
- **结论：** 无需创建空合并提交；移除 16 个 worktree 和 16 条对应本地分支。
- **验证：** 前后端 `git worktree list` 均仅剩各自 `main`；任务/集成分支零匹配；`specdev-worktree` 路径不存在。
- **影响工件：** `.status.json` / Goal Plan / T-01 至 T-08 Evidence
- **约束或不变量：** 保留所有 source/candidate/result SHA 与验证 Evidence；不修改主检出的既有 dirty 内容，不推送、不部署、不归档。
