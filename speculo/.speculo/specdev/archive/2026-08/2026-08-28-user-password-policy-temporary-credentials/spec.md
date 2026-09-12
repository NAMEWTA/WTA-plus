---
schema_version: 3
artifact: spec
change: 2026-08-28-user-password-policy-temporary-credentials
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-08-28-final-consensus
  - ADR-001
  - ADR-002
  - ADR-003
  - ADR-004
  - CODE:current-admin-web-and-ruoyi-auth-system-implementation
  - CODE:plus-ui-main-efb8e0d-manifest-only-navigation
---

# Spec: 统一密码策略、临时登录凭据与权限变更会话失效

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/CONTEXT.md</Path>`
- **规划深度：** Deep。该变更同时触及公开注册、管理员凭据操作、密码认证、Redis 一次性状态、Client 隔离和集群缓存一致性。

## 1. 问题与目标

### 问题陈述

当前密码入口的规则不一致：公开注册允许 5 位密码，管理员新增和重置的前端只校验 5 至 20 位，个人改密校验 6 至 20 位，服务端多个写入入口没有统一强度校验；`sys.user.initPassword` 仍为 `123456`，管理员新增、Excel 导入和重置因此可以继续写入弱密码。前端规则即使加强，也无法阻止相邻接口、导入或直接 API 调用写入弱密码。

当前前端已采用 manifest-only 业务导航：`domain-admin` 拥有公开 Client 认证上下文与注册命令，`domain-system` 拥有用户服务，`web-domain-system` manifest 拥有用户管理组件键与权限声明，Admin 的 `adminManifestRegistry.ts` 只负责选择 manifest 和注入宿主运行时。公开注册与个人中心仍是 Admin App 私有静态页面；本变更沿用这些 owner，不把业务页重新写入静态路由，也不跨包深层导入。

管理员排查指定用户问题时，目前只能重置永久密码，既打断用户原有凭据，又不能表达一分钟、单次使用的临时授权。另一方面，角色权限和用户角色关系在登录时已固化进 `LoginUser` 快照；数据库变更后若 Redis Token/Session 与各 JVM 本地缓存没有同步清除，旧权限可在既有会话中继续生效。

本变更需要用一份服务端权威策略统一所有新密码写入，以可编辑的服务端候选值替代 `123456`，增加不修改永久密码的一分钟临时登录凭据，并让授权变化只在受影响 Client 内立即强制重新登录。

### 目标用户与场景

- **用户管理员：** 在用户管理页获得合规的新增/重置候选密码，可编辑后确认；在独立授权下签发一分钟临时密码，用指定账号临时登录排障而不修改永久密码。
- **公开注册用户：** 在输入时看到与服务端一致的规则和具体失败原因，不再提交后才得到笼统错误。
- **普通已登录用户：** 修改个人密码时使用同一强度规则；存量弱密码仍可登录，但下一次写入必须收敛。
- **权限管理员：** 调整角色菜单/数据权限或用户角色关系后，受影响 Client 的旧权限快照立即失效，而其他 Client 会话不受影响。
- **安全与运维人员：** 通过 `sys_config` 管理可验证的 random/fixed 策略，审计谁为谁签发了临时凭据，同时不在普通业务审计中记录明文密码。

### 成功标准

- 所有新写入密码统一满足 8 至 30 位、至少一个大写字母、一个小写字母、一个数字和一个允许的特殊字符，且只包含策略允许的字符。
- 公开注册、管理员新增、Excel 新用户导入、管理员永久密码重置和个人改密都由同一服务端策略校验；前端仅提供一致的即时反馈。
- 默认策略采用服务端安全随机生成，不再产生 `123456`；运维可在 `sys_config` 中选择 random 或合规 fixed 模式。
- 管理员重置时先取得服务端候选值，允许编辑，最终提交才改永久密码；最终值仍由服务端重新校验。
- 临时密码有效 60 秒、用户级、单次使用、重新签发覆盖旧值，成功认证后生成与普通密码登录等价的无标记会话。
- 临时凭据签发由 `system:user:temporaryPassword` 独立授权并可审计。
- 密码规则失败返回稳定、可区分、可直接展示的原因；公开上下文不返回 fixed 值或随机生成内部参数。
- 角色权限变化只注销该角色所属 Client 的全部会话；用户角色关系变化只注销该用户在目标 Client 的全部会话；Redis 和全部 JVM 本地副本在成功响应前完成失效。
- 管理员永久密码重置继续不注销既有会话，保持当前兼容行为。

### 非目标

- **OOS-001：** 不迁移或批量重置存量弱密码，不在登录读取路径按新强度拒绝已有 BCrypt 密码。
- **OOS-002：** 不把临时登录变成受限会话、短会话、带标记会话、强制改密会话或管理员代理会话。
- **OOS-003：** 不扩大目标用户在任何 Client、登录域、角色、菜单、数据范围或业务接口上的既有权限。
- **OOS-004：** 不新增密码策略数据库表，不把策略放入 `sys_dict`，不保存可逆临时密码。
- **OOS-005：** 不改变永久密码重置或个人改密后的既有会话生命周期。
- **OOS-006：** 不把所有授权变化扩大为全用户、全 Client 注销；只处理已确认的角色菜单/数据权限和用户角色关系变化。
- **OOS-007：** 不替换当前 BCrypt 永久密码存储算法，不引入密码历史、定期过期、首次登录强制改密或找回密码流程。

## 2. 解决方案与外部行为

### 解决方案摘要

服务端以 `sys_config` 中的一份类型化密码策略为唯一权威，提供三类能力：校验密码并返回结构化违规原因；依据内部生成器产生合规候选值；向公开认证上下文投影非敏感规则。所有密码写入口在 BCrypt 前调用同一校验合同，所有默认密码和临时密码由同一安全随机生成器产生。

永久重置仍沿用现有确认接口和会话语义，但增加服务端候选值获取。临时密码签发后只在 Redis 保存不可逆校验值与 60 秒 TTL；密码登录先校验永久 BCrypt，永久密码失败时才尝试临时凭据，在用户状态、验证码、Client 和登录域准入通过后 compare-and-delete 原子消费，再进入普通令牌签发流程。两类凭据共同复用现有失败计数语义。

权限变化继续以登录快照重建为原则：角色权限操作按角色所属 Client 注销，用户角色关系操作按目标用户与目标 Client 注销；统一失效能力同步处理 Redis 会话/权限缓存和全部应用实例的本地缓存。

### 主要流程

#### 管理员永久密码重置

```text
打开重置密码
  -> 服务端按当前有效策略生成候选值
  -> 对目标用户执行既有 allowed 与 data scope 校验
  -> 前端在确认框展示候选值并允许编辑
  -> 前端按公开规则即时提示
  -> 管理员确认提交最终值
  -> 服务端按当前策略重新校验
  -> 通过后 BCrypt 并更新 sys_user.password
  -> 既有会话保持当前行为，不主动注销
```

生成候选值不修改用户。管理员取消、关闭或请求失败时，永久密码不变。

#### 一分钟临时密码

```text
具有 system:user:temporaryPassword 的管理员选择用户
  -> 服务端检查目标用户与数据范围
  -> 使用安全随机生成器产生合规密码
  -> Redis 写入用户级不可逆校验值，TTL=60 秒
  -> 覆盖该用户尚未消费的旧临时值
  -> 响应仅返回本次明文和 expiresInSeconds=60

用户在任一原本可准入的 Client 提交 password grant
  -> 验证码、用户存在和用户状态检查
  -> 永久 BCrypt 成功：按普通流程登录，不消费临时值
  -> 永久 BCrypt 失败：校验临时值但暂不删除
  -> 继续目标 Client/登录域准入
  -> 准入通过后 compare-and-delete
  -> 只有成功删除者可继续普通令牌签发
```

临时凭据只替代密码校验步骤。成功后令牌时长、角色、权限、数据范围、Client 标识和退出行为与同一账号使用永久密码登录完全一致。

#### 新增、导入、注册和个人改密

- 管理员新增用户时，现有用户初始化响应中的候选密码改为服务端按有效策略生成；管理员可编辑，新增提交时再次校验。
- Excel 新用户导入时，每个新用户独立生成合规密码；更新已有用户且导入内容不写密码时不改变原密码。导入结果保持现有逐行成功/失败摘要，不回传明文密码；需要交付凭据时由管理员使用永久重置流程。
- 公开注册从 Client 认证上下文取得公开策略投影，在浏览器给出即时规则反馈；服务端在注册事务写入前再次校验。
- 个人改密在旧密码校验和“新旧不可相同”判断之外，对新密码执行同一策略；会话行为保持现状。

#### 权限变化后的精确失效

```text
角色菜单或数据权限保存成功
  -> 解析角色所属 clientPk/clientId
  -> 注销该 Client 的全部 Token/Session
  -> 清理该 Client 的 Redis 权限数据缓存
  -> 广播并确认全部 JVM 本地相关缓存失效
  -> 返回成功

用户角色授予/撤销/覆盖保存成功
  -> 使用请求中已确认的目标 Client
  -> 注销目标用户在该 Client 的全部 Token/Session
  -> 清理该用户在该 Client 的 Redis 权限数据缓存
  -> 广播并确认全部 JVM 本地相关缓存失效
  -> 返回成功
```

同一用户在其他 Client 的 Token、Session 和本地投影保持有效。重新登录时按最新数据库关系重建完整 `LoginUser` 快照。

### 边界、失败与稳定错误行为

- 密码违规使用现有 `R` 失败 envelope，并在 `data.violations` 返回一个或多个 `{ reason, message }`；`msg` 为可展示摘要，服务端不把 fixed 值、字符池或提交密码回显到错误中。
- `reason` 的稳定集合为 `PASSWORD_TOO_SHORT`、`PASSWORD_TOO_LONG`、`PASSWORD_MISSING_UPPERCASE`、`PASSWORD_MISSING_LOWERCASE`、`PASSWORD_MISSING_DIGIT`、`PASSWORD_MISSING_SPECIAL`、`PASSWORD_CONTAINS_DISALLOWED_CHARACTER`。多项违规按该顺序返回；消息包含当前公开阈值或允许的特殊字符，能够说明具体修正方式。
- 两次输入不一致仍是前端表单错误，不属于服务端密码策略违规；服务端只接收最终密码。
- 策略 JSON 畸形、缺少必需字段、random 生成器不可能满足规则、fixed 值不合规或缓存中的策略不可用时，所有新密码写入、候选生成和临时签发失败关闭，并返回稳定原因 `PASSWORD_POLICY_UNAVAILABLE`；永久密码登录和已经签发且仍在 TTL 内的临时凭据验证继续可用。服务端日志记录配置定位信息但不记录 fixed 值或字符池内容。
- 公开认证上下文只有在 Client 可用时返回策略投影；畸形策略不得返回猜测的弱规则，公开注册提交同样失败关闭。
- 临时密码错误、已过期、被覆盖或已消费时，对登录调用方继续使用现有“账号或密码错误”语义，不披露临时凭据是否存在。
- 永久和临时校验属于同一次 password grant 判定：只有两者都失败时才记一次密码登录失败；临时凭据成功不得先记录一次“永久密码失败”，账号锁定/失败计数阈值继续沿用现有语义。
- 临时密码签发或验证期间 Redis 不可用时失败关闭临时能力；不得回退为永久密码写入、进程内临时存储或可重复使用。永久 BCrypt 验证不依赖该临时分支成功。
- 错误临时密码不消费有效凭据；并发提交同一个正确临时密码时，只有 compare-and-delete 成功的一次能够登录。
- 临时密码签发继续执行与永久重置相同的超级管理员保护、目标用户存在、用户可操作和数据范围检查；独立权限不绕过数据权限。
- Client 或登录域不允许目标用户登录时，即使临时密码正确也拒绝登录且不消费凭据；准入通过并原子消费后若后续令牌签发异常，凭据不恢复，管理员需要重新签发。
- 权限数据库写入成功但跨节点会话/缓存失效未完成时，接口不得报告完整成功；记录可检索失败并允许同一失效操作幂等重试。不得以等待 5 秒或 30 秒本地 TTL 作为成功条件。

### 状态转换与不变量

```text
临时凭据：不存在 --签发--> 有效(60s)
有效 --重新签发--> 新值有效(60s)，旧值无效
有效 --错误校验--> 有效
有效 --首次正确校验且原子删除成功--> 已消费
有效 --TTL 到期--> 已过期
已消费/已过期 --登录--> 通用凭据失败
```

- 永久密码和临时密码独立；签发、覆盖、消费或过期均不得写 `sys_user.password`。
- 永久密码优先校验；使用正确永久密码不得消耗尚未使用的临时密码。
- 临时凭据以 `userId` 为作用域，不绑定签发时所在 Client；每次登录仍以请求的 OAuth `clientId` 做准入。
- OAuth `clientId` 字符串与数据库 `clientPk` 不得混用；权限失效边界以已解析的目标 Client 身份为准。
- 新密码只在明文策略校验成功后 BCrypt；存量 BCrypt 登录不进行强度反推或阻断。
- 密码候选值不是保留或锁定状态；生成与最终确认之间策略若变化，以确认时策略为准。
- 授权变更后的旧 Token 不得通过 Redis 或任一 JVM 本地副本继续读取旧权限快照。

## 3. 用户故事

- **US-001：** 作为用户管理员，我希望重置框默认获得服务端生成的合规密码且仍可编辑，以便不用手工编造强密码，也能在确认前按实际需要修改。
- **US-002：** 作为具有独立临时签发权限的管理员，我希望为指定用户签发一分钟单次密码，以便不修改该用户永久密码就能临时登录排障。
- **US-003：** 作为被临时登录的账号，我希望认证后的会话与普通登录完全一致，以便账号原有 Client、角色和业务权限不被附加规则改变。
- **US-004：** 作为公开注册或修改密码的用户，我希望所有入口使用一致规则并告诉我每一项不合规原因，以便一次修正成功。
- **US-005：** 作为系统管理员，我希望管理员新增和 Excel 导入也不能绕过强度规则，以便相邻入口不再写入弱密码。
- **US-006：** 作为运维人员，我希望在 `sys_config` 中选择 random 或合规 fixed 默认策略，并安全刷新生效，以便不改代码即可管理默认凭据规则。
- **US-007：** 作为存量用户，我希望旧弱密码在上线后仍可登录，但下次改密必须使用新规则，以便安全收敛不造成批量锁号。
- **US-008：** 作为权限管理员，我希望角色权限或用户角色关系修改后仅让受影响 Client 的相关会话失效，以便权限立即生效又不打断无关 Client。
- **US-009：** 作为审计人员，我希望临时凭据签发能独立授权并记录操作者、目标和结果，同时不保存明文密码，以便追责而不扩大秘密暴露。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 默认 `sys.user.passwordPolicy` 为有效 random 配置 | 获取新增用户初始化信息或永久重置候选值 | 服务端返回 8 至 30 位且包含四类必需字符的候选值；连续样本不是固定 `123456`，生成本身不修改用户 | 密码策略单元测试；用户 API 集成测试 |
| AC-002 | 管理员已取得重置候选值 | 编辑为另一合规值并确认 `PUT /system/user/resetPwd` | 最终编辑值通过服务端校验后被 BCrypt 写入；候选值本身未被隐式使用 | Controller/service 集成测试；用户页组件/E2E |
| AC-003 | 管理员在重置框提交一项或多项不合规密码 | 调用永久重置 | 密码不变；失败 envelope 返回稳定、按序的具体 `violations`，UI 展示具体原因 | 密码错误合同测试；用户页组件/E2E |
| AC-004 | 管理员完成永久密码重置且目标用户存在多个既有会话 | 使用旧会话继续访问 | 会话行为与变更前一致，不因本功能主动注销；旧永久密码不能再新登录，新密码可登录 | 会话兼容集成测试 |
| AC-005 | 操作者只有 `system:user:resetPwd`，没有 `system:user:temporaryPassword` | 请求签发临时密码 | 返回权限拒绝，不创建 Redis 临时凭据 | 权限注解/API 集成测试 |
| AC-006 | 操作者有独立权限且通过目标用户/data scope 校验 | `POST /system/user/temporaryPassword` | 返回 `{password, expiresInSeconds: 60}`；密码合规；Redis 只存不可逆校验值并设置 60 秒 TTL；数据库永久密码不变 | API、Redis 集成测试；用户页 E2E |
| AC-007 | 用户已有未消费临时密码 | 管理员再次签发 | 新值立即覆盖并重新开始 60 秒 TTL；旧值登录失败，新值可进入认证 | Redis/认证集成测试 |
| AC-008 | 有效临时密码尚未使用 | 先提交错误密码，再提交正确密码 | 错误提交不消费；正确提交仅成功一次并删除 Redis 值 | 认证策略与真实 Redis 集成测试 |
| AC-009 | 两个请求并发提交同一有效临时密码 | 同时执行 password grant | 只有一个请求通过原子消费并获得令牌，另一个得到通用凭据失败 | 并发 Redis 集成测试 |
| AC-010 | 临时密码已过 60 秒、已消费或已被覆盖 | 执行 password grant | 均返回与普通密码错误一致的稳定响应，不披露具体临时状态 | 认证 API 集成测试 |
| AC-011 | 同一用户可准入多个 Client | 在不同 Client 使用新签发临时密码 | 未获准 Client 拒绝且不消费；首个满足验证码、用户状态、Client 和登录域准入的成功认证原子消费凭据 | 多 Client 认证集成/E2E |
| AC-012 | 临时凭据认证成功 | 检查返回令牌与后续授权 | 令牌无临时标记，时长、角色、权限、数据范围和业务访问与同账号普通登录等价 | LoginUser/令牌集成测试；跨 Client E2E |
| AC-013 | 有效临时密码存在 | 使用正确永久密码登录 | 永久登录成功且临时密码未被消费，随后仍可被使用一次 | 认证策略与 Redis 集成测试 |
| AC-014 | Redis 或密码策略分别不可用 | 分别尝试临时签发、已有临时凭据认证和永久密码认证 | Redis 不可用时临时认证失败关闭；策略不可用时禁止新签发但已有临时凭据仍可验证；正确永久密码始终按原合同认证；不得写永久密码或进程内兜底 | 故障注入集成测试 |
| AC-015 | 公开 Client 可用 | `GET /auth/client/context` | 在原字段外返回 `passwordPolicy` 非敏感投影；不包含 fixed 值、mode、生成长度或字符池 | AuthController 合同测试；admin domain transport 测试 |
| AC-016 | 注册者输入不合规密码 | 前端校验或绕过前端直接 `POST /auth/register` | 前端按投影即时提示；服务端拒绝持久化并返回相同稳定违规原因 | 注册表单测试；注册 API 集成/E2E |
| AC-017 | 管理员新增用户、Excel 导入新用户或用户个人改密 | 分别提交/生成密码 | 每个入口使用同一策略；弱值不写入，合规值写入；Excel random 模式逐个用户独立生成且不在导入摘要返回明文 | 服务/导入监听器测试；前端组件/E2E |
| AC-018 | 存量用户永久 BCrypt 对应弱密码 | 上线后登录，再进行个人改密或管理员重置 | 原弱密码仍可登录；下一次密码写入拒绝弱值并接受合规值 | 登录兼容与密码写入集成测试 |
| AC-019 | 配置管理员保存 random/fixed 策略 | 提交合法与非法配置 | 合法配置刷新后用于所有入口；弱 fixed、空必需字符池、生成长度越界、特殊池超出允许集合或畸形 JSON 被拒绝，旧有效策略继续生效 | 配置服务/缓存集成测试 |
| AC-020 | 角色属于 Client A，系统还存在 Client B 会话 | 保存角色菜单或数据权限 | Client A 全部既有会话立即失效并清除相关 Redis/JVM 缓存；Client B 会话保持有效 | 双 Client、双 JVM 集成测试 |
| AC-021 | 同一用户在 Client A 与 B 各有多个 Token | 在 Client A 授予、撤销或覆盖角色 | 只注销该用户在 Client A 的全部 Token/Session 并清其权限缓存；Client B 所有 Token 保持有效 | 用户角色 API、双 Client 集成测试 |
| AC-022 | 角色权限或用户角色关系写入已完成 | 某个 JVM/Redis 失效步骤失败 | API 不报告完整成功，失败可检索且失效操作可幂等重试；不得依赖本地 TTL 自然过期 | 故障注入与集群一致性测试 |
| AC-023 | 临时密码签发成功或失败 | 查询业务操作审计 | 审计包含操作者、目标用户、动作、结果和 TTL，不含请求/响应明文密码或 Redis 校验值 | `@Log` 审计集成测试与日志字段断言 |
| AC-024 | 永久密码失败但临时密码成功，或两种凭据都失败 | 执行 password grant | 临时成功按一次成功登录记录且不增加失败计数；两者都失败只增加一次现有密码失败计数并遵循同一锁定阈值 | 登录服务与认证 API 集成测试 |

## 5. 范围

### IN

- `sys_config` 类型化密码策略的解析、保存校验、缓存刷新、公开投影、服务端校验和安全随机生成。
- 公开注册、管理员新增、Excel 新用户导入、管理员永久重置、个人改密五类密码写入口的统一收敛。
- 管理员用户页的随机重置候选值、可编辑确认、临时密码签发按钮、独立权限可见性和具体错误展示。
- 注册页和个人改密页基于公开策略投影的动态表单校验；服务端继续最终裁决。
- Redis 用户级一分钟一次性临时凭据及现有 password grant 的后备验证。
- 角色菜单/数据权限与用户角色关系变化后的 Client 精确会话及多层缓存失效。
- NAMEWTA 增量 DML：策略配置、旧弱默认值退役说明及可独立分配的临时密码菜单权限。
- OpenAPI/工作区 transport 类型、领域服务、manifest 权限声明和相邻测试同步。

### REUSE

- 复用 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/impl/PasswordAuthStrategy.java</Path>` 的验证码、登录失败记录、Client/登录域准入和普通令牌签发主流程。
- 复用 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientSessionService.java</Path>` 的按用户/Client 会话定位语义，并扩展为全部缓存层可确认失效的统一边界。
- 复用 `RedisUtils`、`LoginHelper`、现有 BCrypt 和现有 `R` envelope；业务代码不硬编码基础设施 key 或直接绕过 common helper。
- 复用 `<Path>plus-ui-namewta/packages/domains/system/</Path>` 的系统领域服务、`<Path>plus-ui-namewta/packages/web-domains/system/</Path>` 的用户管理页面与 manifest 权限所有权，以及 `<Path>plus-ui-namewta/packages/domains/admin/</Path>` 的公开认证上下文 transport；Admin 仅在 `adminManifestRegistry.ts` 注入宿主 port，注册/profile 保持 App 私有静态页面。
- 复用当前用户 allowed、data scope、超级管理员保护、角色 Client 归属和用户角色目标 Client 校验。
- 复用当前永久重置 `PUT /system/user/resetPwd`，不为同一确认动作建立兼容双轨。

### OUT

- **OOS-001** 至 **OOS-007** 全部适用。
- 本变更不向公开认证上下文暴露完整 `sys_config`、默认 fixed 密码、random/fixed mode、随机长度或字符池。
- 本变更不让 Excel 文件携带或更新密码，不在导入结果中返回随机明文密码。
- 本变更不增加“查看当前临时密码”“延长 TTL”“撤销后恢复”“多枚并存”或“绑定签发 Client”的能力。
- 本变更不改变短信、社交等非 password grant 的凭据语义。
- 本变更不清理与授权快照无关的所有 Redis/Caffeine 数据；只清目标会话与权限数据缓存。

## 6. 已锁定实现约束

- **DEC-001：** 统一密码策略由 `sys_config` 承载，不使用 `sys_dict`；服务端是所有写入口、生成和公开投影的唯一权威。来源：`ADR-001`。
- **DEC-002：** 新配置键为 `sys.user.passwordPolicy`，值为 versioned JSON；现有 `sys.user.initPassword` 不再被新代码读取。默认迁移为 random 模式，旧键在部署时改为一次生成的合规随机兼容值并标记退役，避免滚动窗口中的旧节点继续写 `123456`。来源：`ADR-001`、`CODE:<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`。
- **DEC-003：** v1 策略对外强度固定为 8 至 30 个 ASCII 字符，必需类为 `UPPERCASE`、`LOWERCASE`、`DIGIT`、`SPECIAL`，默认允许特殊字符为 `@$!%*?&`；配置不能关闭四类基线。来源：`USER-DECISION:密码强度基线`、`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/constant/RegexConstants.java</Path>`、现有注册上限。
- **DEC-004：** v1 内部配置对象包含 `version`、`minimumLength`、`maximumLength`、四类必需标志、`allowedSpecialCharacters`、`generator.length` 与四类 generator 字符池，以及 `defaultPassword.mode`；`RANDOM` 使用 generator，`FIXED` 额外使用 `fixedValue`。临时密码无论默认模式为何始终使用 generator。来源：`ADR-001`、`USER-DECISION:可配置随机或固定默认密码`。
- **DEC-005：** generator 长度必须在策略范围内，四个字符池非空且字符属于对应类别，特殊池是允许特殊字符的子集；fixed 值必须通过同一密码校验。随机生成使用密码学安全随机源并构造性保证每个必需类别至少出现一次。来源：`ADR-001`。
- **DEC-006：** 公开 `passwordPolicy` 投影只含 `minimumLength`、`maximumLength`、`requiredCharacterClasses` 和 `allowedSpecialCharacters`；不得返回 mode、fixedValue、generator.length 或任一字符池。来源：`USER-DECISION:公开最小非敏感规则`、`ADR-001`。
- **DEC-007：** 临时密码只保存不可逆校验值到 Redis，TTL 固定 60 秒，以用户为作用域；重新签发覆盖旧值，首次成功通过 compare-and-delete 原子消费。来源：`ADR-002`。
- **DEC-008：** 永久 BCrypt 优先于临时凭据；只有永久校验失败才尝试临时值，正确永久密码不消费临时值。来源：`ADR-002` 与永久密码兼容不变量。
- **DEC-009：** 临时凭据认证后复用普通登录会话，禁止新增标记、缩短时长、限制权限或强制改密。来源：`ADR-003`。
- **DEC-010：** `system:user:temporaryPassword` 独立控制临时签发；永久重置权限不隐式包含它。来源：`USER-DECISION:独立授权与审计`。
- **DEC-011：** 永久密码重置继续只更新密码，不主动注销任何既有会话。来源：`USER-DECISION:沿用当前行为`、`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>`。
- **DEC-012：** 角色菜单或数据权限变更注销角色所属 Client 全部会话；用户角色关系变更注销该用户在目标 Client 的全部会话；其他 Client 保持。来源：`ADR-004`、`USER-DECISION:按受影响Client精确失效`。
- **DEC-013：** 会话失效必须通过统一抽象清理 Redis Token/Session、权限数据缓存和所有 JVM 本地副本；成功响应不得先于失效完成，也不得散落硬编码 Sa-Token key。来源：`ADR-004`。
- **DEC-014：** 新增写操作使用 POST 并使用 `@Log`；临时签发的数据库业务审计关闭请求和响应数据保存，避免记录明文。当前专用 raw HTTP credential logger 依永久 ADR-0017 保持既有受控行为，本 change 不扩大其 sink 或保留策略。来源：工程规范、永久 ADR-0017。
- **DEC-015：** NAMEWTA 数据变更只追加到 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>`，不得修改上游 `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`。来源：工程规范。

## 7. 数据、接口与兼容

### 公共 HTTP 合同

- **扩展 `GET /auth/client/context`：** 原 `data.clientEnabled`、`data.registerEnabled` 保持；新增：

```json
{
  "passwordPolicy": {
    "minimumLength": 8,
    "maximumLength": 30,
    "requiredCharacterClasses": ["UPPERCASE", "LOWERCASE", "DIGIT", "SPECIAL"],
    "allowedSpecialCharacters": "@$!%*?&"
  }
}
```

- **新增 `POST /system/user/resetPwd/candidate`：** 权限 `system:user:resetPwd`；加密请求 `{ "userId": "..." }`；响应 `data.password`。执行目标用户 allowed/data scope 校验，不修改用户，响应设置 no-store 语义。
- **保留 `PUT /system/user/resetPwd`：** 请求 `{ userId, password }` 不变；新增服务端策略校验与详细违规响应；成功后的既有会话保持。
- **新增 `POST /system/user/temporaryPassword`：** 权限 `system:user:temporaryPassword`；加密请求 `{ "userId": "..." }`；响应 `{ "password": "...", "expiresInSeconds": 60 }`，并设置 no-store 语义。
- **扩展 `POST /auth/register`、`POST /system/user`、`PUT /system/user/profile/updatePwd`：** 成功请求形状保持，新增统一策略拒绝合同。
- **保留 `POST /auth/login` 请求形状：** 不增加 `temporary` 标志；password grant 自动接受永久或有效临时凭据。
- **失败 envelope：** 继续使用项目现有 `R` code/msg/data 外壳；密码违规时 `data.violations` 为稳定原因数组。非密码业务错误保持现有合同。

### 配置与持久化

新配置 `sys.user.passwordPolicy` 的 v1 逻辑形状为：

```json
{
  "version": 1,
  "minimumLength": 8,
  "maximumLength": 30,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecial": true,
  "allowedSpecialCharacters": "@$!%*?&",
  "generator": {
    "length": 12,
    "uppercaseCharacters": "ABCDEFGHJKLMNPQRSTUVWXYZ",
    "lowercaseCharacters": "abcdefghijkmnopqrstuvwxyz",
    "digitCharacters": "23456789",
    "specialCharacters": "@$!%*?&"
  },
  "defaultPassword": {
    "mode": "RANDOM"
  }
}
```

fixed 模式把 `defaultPassword` 改为 `{ "mode": "FIXED", "fixedValue": "合规值" }`；generator 仍必需，因为临时密码永远随机。配置值必须适配 `sys_config.config_value` 现有 500 字符容量；超长配置在保存边界拒绝，不变更表结构。

临时凭据只在 Redis 中存在，不新增数据库列或表。业务审计只保存元数据，不保存明文或校验值。

### 兼容与迁移

- 存量 BCrypt 不迁移、不解密、不按强度扫描；登录保持兼容。
- `sys.user.initPassword` 作为旧配置保留以支持滚动窗口和回滚识别，但新代码和新 UI 不再读取；DML 使用数据库随机源一次生成满足基线的兼容值替换 `123456` 并标记退役，不提交跨环境共用的固定明文。新默认 JSON 使用 random 模式。
- 增量 DML 以 fresh/upgrade 可判断方式追加新配置和 `system:user:temporaryPassword` 菜单按钮；固定主键、执行前置、重复执行和回滚说明必须完整。除超级管理员既有通配语义外，不把该权限隐式并入 `system:user:resetPwd`，由角色菜单配置独立授予。
- 前端 OpenAPI schema、admin transport 和 system domain 同步扩展；旧调用者未读取 `passwordPolicy` 时不受新增响应字段影响。
- 现有重置、注册、用户新增和个人改密路径/方法不变；弱密码调用方会按本 Spec 明确失败，这是有意安全收紧。
- 滚动发布期间先部署能识别新配置且保持永久登录兼容的后端，再发布依赖公开投影和新接口的前端；集群内旧节点不得继续产生 `123456`，因此迁移窗口需要阻止旧写路径或一次性完成后端节点切换。

### 发布或运维影响

- 需要执行 NAMEWTA DML、校验 `sys.user.passwordPolicy`、刷新配置缓存并确认新菜单权限可被独立授予。
- Redis 是临时凭据和会话精确失效的必需依赖；上线前必须在与生产相同拓扑验证 Lua/CAS 或等价原子操作、key TTL、广播和双 JVM 本地缓存清理。
- 发布后监控策略解析失败、临时签发/消费结果、跨节点失效失败和 password grant 失败率；指标与日志不得带密码。
- 当前 raw HTTP 凭据日志属于已接受的永久 ADR-0017 风险边界；生产运维继续执行该 ADR 的独立文件、1 MiB 轮转和访问控制要求。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 所有密码生成使用密码学安全随机源；明文仅在必要请求生命周期和一次响应中存在。应用日志、异常、指标、数据库业务审计、Redis key/value 和缓存事件不得包含明文、fixed 值或生成池；只有永久 ADR-0017 已批准的专用 raw HTTP sink 维持原行为。
- **NFR-002 原子性与并发：** 同一临时凭据在任意数量应用实例和并发请求下最多成功一次；重新签发后旧值立即不可用。错误校验不得删除当前值，永久成功不得删除临时值。
- **NFR-003 可用性与可靠性：** 密码策略缓存更新采用“新值完整验证后替换旧值”；无有效策略时新写入和临时能力失败关闭，永久登录不受影响。授权失效操作幂等，失败可重试且不以本地 TTL 代替确认。
- **NFR-004 Client 隔离：** 角色权限和用户角色关系变化只影响已解析的目标 Client；多 Token、多设备、多 JVM 下结果一致，其他 Client 零注销。
- **NFR-005 性能与容量：** 永久密码成功路径不增加 Redis 访问；只有 BCrypt 永久校验失败时访问临时凭据。Client 会话扫描与缓存广播不得在请求线程无限等待，并需在生产规模下记录耗时和失败范围；本 Spec 不虚构固定延迟阈值。
- **NFR-006 可观测性与审计：** 临时签发记录操作者、目标用户、请求 Client 上下文、结果、60 秒 TTL 和关联请求标识；消费只记录成功/失败类别与用户/Client，不记录凭据。权限失效记录目标类型、Client、Token 数、缓存层、节点确认和失败原因。
- **NFR-007 可访问性与交互：** 候选密码默认可见且可编辑，临时密码只在签发响应后展示一次，并提供现有图标库的复制操作和明确剩余有效期；按钮受权限控制且不因动态内容改变表格布局。键盘可完成确认、取消和复制。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 密码策略解析、校验、投影与生成 | 后端纯单元/性质测试 | AC-001、AC-003、AC-015、AC-017、AC-019 | `./mvnw -pl ruoyi-modules/ruoyi-system -am test`；表驱动覆盖每个违规原因及 random/fixed 边界 | JUnit 输出、样本不变量与退出码 |
| 注册与普通密码登录 | 后端服务/API 集成 | AC-013 至 AC-018 | `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/</Path>` 相邻认证先例；`./mvnw -pl ruoyi-admin -am test` | MockMvc/服务集成结果与错误 envelope 断言 |
| 用户新增、重置、个人改密与导入 | 后端 Controller/service 集成 | AC-001 至 AC-004、AC-017、AC-018、AC-023 | `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/</Path>` 的 password 聚合测试；`./mvnw -pl ruoyi-modules/ruoyi-system,ruoyi-admin -am test` | DB 前后状态、审计脱敏与退出码 |
| 临时凭据存储与认证 | 真实 Redis 集成、并发 | AC-006 至 AC-014 | 复用 RedisUtils 和认证集成测试基础；外部服务环境运行定向 Maven tests | TTL、覆盖、CAS、并发唯一成功、故障注入 Evidence |
| Client 精确会话/缓存失效 | 双 Client、双 JVM 集成 | AC-020 至 AC-022 | Sa-Token/ClientSession 相邻测试；真实 Redis + 两个独立本地缓存实例 | Token/Session/缓存前后快照、节点确认、故障注入 |
| admin domain 公开认证 transport | 前端领域单元 | AC-015、AC-016、AC-019 | `<Path>plus-ui-namewta/packages/domains/admin/src/index.test.ts</Path>`、`transport` 测试；`pnpm test` | Vitest 输出与敏感字段拒绝/忽略断言 |
| system domain 用户凭据 API | 前端领域单元 | AC-001 至 AC-006、AC-023 | `<Path>plus-ui-namewta/packages/domains/system/src/index.test.ts</Path>` | URL、method、加密 header、payload/response 投影断言 |
| 用户管理 Web Domain | Vue 组件/领域集成 | AC-001 至 AC-006、AC-017、AC-023 | `<Path>plus-ui-namewta/packages/web-domains/system/src/index.test.ts</Path>`；定向 Vitest | 权限可见性、候选可编辑、错误与一次展示交互 |
| 注册与个人改密 App 页面 | Vue 组件/App 集成 | AC-003、AC-015 至 AC-018 | `<Path>plus-ui-namewta/apps/admin-web/src/views/register.vue</Path>`、profile resetPwd 相邻测试；App Vitest/typecheck | 动态规则、详细反馈、直接 API 失败回显 |
| NAMEWTA DML 与权限声明 | 静态/迁移检查 | AC-005、AC-019、AC-023 | DML 尾部结构检查、固定主键冲突扫描、fresh/upgrade 数据核对；`pnpm architecture:check` | SQL dry-run、行数据查询、权限 manifest 零遗漏 |
| Admin 浏览器主流程 | 端到端 | AC-001 至 AC-018、AC-020、AC-021 | `<Path>plus-ui-namewta/e2e/client-auth-context.spec.ts</Path>`、`system-identity.spec.ts`；`pnpm test:e2e` | Playwright 报告、跨 Client 场景与截图/网络断言 |
| 前端工作区全量门禁 | 静态、单元、构建 | 所有前端合同 | `pnpm --filter @namewta/tooling-openapi openapi:check`；`pnpm architecture:check`；`pnpm architecture:test`；`pnpm lint`；`pnpm typecheck`；`pnpm test`；`pnpm build:dev`；`pnpm build:prod` | cwd、命令、退出码和摘要 |
| 后端全量回归 | 单元/集成/打包 | 所有后端合同 | `./mvnw test` 及适用的全量 package；真实 Redis/MySQL 外部服务验证单独记录 | Maven 报告、环境说明、未运行项与残余风险 |

临时凭据原子性、Client 精确失效和全 JVM 缓存清理必须使用真实 Redis 与至少两个独立本地缓存实例验证；mock 或单 JVM 单元测试不能替代该 Evidence。浏览器测试不能替代直接 API 绕过前端规则的服务端断言。

## 10. 风险、假设与未决问题

### 风险

- **RISK-001 滚动发布弱写入：** 旧后端节点仍读取 `sys.user.initPassword` 时可能继续写入旧默认值。缓解方式是 DML 先消除 `123456`、阻止旧写路径，并将新后端作为一个受控批次完成切换后再发布前端。
- **RISK-002 配置漂移或失效：** 通用配置页面可写入畸形 JSON。缓解方式是在保存和使用边界双重校验，只有完整合法的新值才能替换缓存中的旧策略，并对不可用状态告警。
- **RISK-003 临时凭据竞态：** 先 get、后 delete 会让并发请求重复成功。缓解方式是不可逆值校验后以存储值 compare-and-delete，只有删除成功者进入令牌签发。
- **RISK-004 BCrypt 成本放大：** 永久密码失败后再校验临时 hash 会增加登录失败成本。缓解方式是沿用失败计数/限流语义、只在临时 key 存在时执行第二次昂贵校验，并监控失败率。
- **RISK-005 Client 失效过宽或过窄：** 混用 `clientId` 与 `clientPk`、只按显式角色关系枚举或遗漏默认角色会造成无关注销或旧权限残留。缓解方式是角色权限按角色所属 Client 全注销，用户角色按用户+目标 Client 注销，并建立多 Client/默认角色测试矩阵。
- **RISK-006 多级缓存残留：** 当前 Sa-Token 与 Spring Cache 都有 JVM Caffeine 前置层，单节点 evict 无法立即清其他节点。缓解方式是统一失效协议、节点确认、真实双实例测试和成功响应后置。
- **RISK-007 敏感明文暴露：** 候选值和临时密码需要返回前端，而当前永久 ADR 允许专用 raw HTTP 凭据日志。缓解方式是不扩大该已接受边界，业务审计完全关闭请求/响应保存，限制一次展示、no-store 和日志访问权限。
- **RISK-008 Excel random 凭据交付：** 每用户随机密码不会在导入摘要回传，管理员无法用旧固定值批量告知用户。该行为是安全收紧；需要凭据交付时使用单用户永久重置，不通过导入结果泄露整批密码。

### 已采用的低影响假设

- **ASSUMPTION-001：** 默认 generator 使用去除易混淆字符的 12 位字符池；它不改变公开强度合同，运维可通过合法配置调整。验证：默认 DML 解析、生成性质测试和人工配置测试。
- **ASSUMPTION-002：** 违规消息使用当前中文交互语言，`reason` 才是稳定机器合同；后续国际化可在不改 reason 的前提下替换 message。验证：API 合同测试断言 reason 与顺序，UI 测试断言可展示文本。
- **ASSUMPTION-003：** no-store 通过现有 HTTP 基础设施设置等价的 `Cache-Control: no-store` 响应；具体 helper 由 Ticket 选择。验证：API 集成测试检查响应 header。
- **ASSUMPTION-004：** 集群本地缓存失效采用项目现有 Redis 能力上的广播或版本化命名空间，具体协议由 Ticket 在满足“全节点确认后成功”合同下确定。验证：双 JVM 故障注入和幂等重试测试。

### 未决问题

无。
