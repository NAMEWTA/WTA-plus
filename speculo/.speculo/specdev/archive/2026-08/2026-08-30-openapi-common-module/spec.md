---
schema_version: 3
artifact: spec
change: 2026-08-30-openapi-common-module
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:grill-consensus-2026-08-31
  - DESIGN-TREE:consensus-round-9
  - ADR:change-local-001-through-021
  - CODE-BASELINE:parent-6de68845bdd9-frontend-381918e7c2c3-backend-e5cef5a616ce
---

# Spec: 基于现有 Sa-Token 权限链的完整 OpenAPI 开放平台

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

当前第三方调用需要登录的业务接口时，必须模拟某个前端 Client 登录、维护浏览器 Token，并携带该 Client 的请求上下文。同一后端服务多个前端 Client 时，这会迫使调用方维护与动态路由无关的多套登录状态，也无法形成稳定、可审计的机器调用合同。

系统还缺少完整的开放平台体验：开发者不能只通过方法级注解显式开放真实 Controller 方法；用户和超级管理员没有统一的凭据管理入口；第三方没有由真实 Spring MVC/SpringDoc 元数据生成并按目标用户权限过滤的接口目录与调用示例。

### 目标用户与场景

- **第三方集成方：** 使用用户提供的全局 AppKey/AppSecret 签名调用被显式开放的方法，无需登录和维护普通 Token。
- **普通授权用户：** 在个人信息的“开放应用”Tab 管理自己的唯一凭据，并查看自己当前可调用的接口、详情和调用文档。
- **超级管理员：** 在“系统管理 > 应用开放管理”中管理任意用户的凭据，并按目标用户查看其可调用接口。
- **后端开发者：** 在 Controller 方法逐个添加 `@OpenApi`，复用方法已有的 Sa-Token 权限、角色和数据权限声明。
- **运维与扩展实现：** 默认关闭该能力，配置密钥保护、时间窗口、限流和会话生命周期，并通过受管调用事件扩展统计或计费。

### 成功标准

- 每个用户最多拥有一条只绑定 `userId` 的全局开放凭据，不因前端 Client 数量增加而重复创建。
- 只有方法级 `@OpenApi` 对真实 Spring MVC 映射生效；未标注方法不能通过签名通道调用。
- 每次开放调用先完成 HMAC、时间窗口、nonce、凭据状态和限流校验，再复用标准 `LoginUser`、Sa-Token Session、权限注解和数据权限链。
- Redis 命中时直接复用权限快照；未命中时只读装载数据库已有授权，不新增、补写或授予任何权限。
- 目录、详情、本人预览、管理员目标用户预览和真实调用使用同一注册表、授权解析器和权限判断语义。
- 管理页和个人 Tab 共享 system domain/web-domain 能力，前端可见性不替代后端鉴权。
- 普通 Token 登录、Client 校验、动态路由、普通会话和未标注 Controller 的行为保持不变。

### 非目标

- 不兼容来源项目的 MD5 协议、旧 AppKey、旧表或旧调用方。
- 不提供每 Client 多凭据、每应用多凭据、服务账号、OAuth2 或浏览器 Token 替代方案。
- 不支持类级批量开放，也不自动开放 Controller 新增方法。
- 不在首期开放依赖单一 `clientPk/clientKey` 的接口，包括 OSS 上传和 Client 级路径、IP、业务策略。
- 不提供框架级 `Idempotency-Key`、业务响应重放或通用写操作幂等；目标业务接口自行声明幂等合同。
- 不实现 IP 白名单、计费业务或双 secret 无停机轮换；只保留受管调用事件扩展点。
- 不生成 OpenAPI 动态路由树，不改变普通前端 Client 的授权裁剪。
- 不引入 MQ、requestCode、CommandBus、通用命令分发器、平行 Principal、平行权限注解或平行 Redis 权限缓存。

## 2. 解决方案与外部行为

### 解决方案摘要

在 `ruoyi-common` 聚合下新增 `ruoyi-common-openapi`。它拥有方法级注解、启停配置、NAMEWTA v1 签名验证、防重放与限流编排、请求会话桥接、开放映射注册表和窄类型化 SPI；不直接访问 system Mapper 或业务实体。

`ruoyi-system` 拥有凭据持久化、密钥生命周期、全局授权快照、管理 API、目录 API、调用事件实现和 SQL。`ruoyi-admin` 只负责依赖与 Bean 装配。前端在现有 `system` domain/web-domain 中提供领域合同、共享组件和动态管理页，由 `admin-web` 显式组合，并在 App 自有个人信息静态页增加“开放应用”Tab。

开放调用成功验签后取得只在服务端使用的机器 Token。现有 TokenSession 继续用标准 key 保存 `ruoyi-api` 的 `LoginUser`；当前请求只向 Sa-Token request Storage 注入该 Token，因此目标方法原有 `@SaCheckPermission`、`@SaCheckRole`、`LoginHelper`、数据权限和敏感字段判断无需另建分支。

### NAMEWTA v1 调用协议

第三方请求使用以下 HTTP header：

| Header | 合同 |
|---|---|
| `X-OpenAPI-Version` | 固定为 `v1` |
| `X-App-Key` | 创建凭据时返回的公开标识 |
| `X-Timestamp` | UTC Unix 秒的十进制字符串 |
| `X-Nonce` | 每次请求重新生成的至少 128 bit 随机值，Base64URL 无填充编码 |
| `X-Signature` | HMAC-SHA256 结果的 Base64URL 无填充编码 |

AppSecret 是至少 256 bit 的 CSPRNG 随机值，以 Base64URL 无填充形式一次性展示；签名时先解码为 HMAC key，AppSecret 本身不进入请求。

Canonical request 使用 UTF-8 和 LF，字段顺序固定为：

```text
NAMEWTA-HMAC-SHA256
v1
{appKey}
{timestamp}
{nonce}
{UPPERCASE_HTTP_METHOD}
{canonicalPath}
{canonicalQuery}
{lowercaseHexSha256OfExactBodyBytes}
```

- `canonicalPath` 是绝对 URI path；保留 `/`，不合并重复斜杠、不改写尾斜杠，只解码 RFC 3986 unreserved 字符，其余字节按 UTF-8 和大写十六进制百分号编码。
- `canonicalQuery` 保留重复参数和空值；参数名和值先按 UTF-8/RFC 3986 编码，再按编码后的名称、值升序排序，以 `name=value` 和 `&` 连接；没有 query 时为空行。
- body hash 对进入 Controller 前的原始 HTTP body 字节计算；无正文时对零字节计算 SHA-256。
- 服务端以常量时间比较签名。固定跨语言测试向量是该规范的可执行权威，不允许客户端和服务端分别猜测规范化细节。
- 默认允许时钟偏差和 nonce TTL 均为 60 秒，可配置。Redis 以 AppKey + nonce 原子登记；重复 nonce 只允许一个请求成功。
- 网络重试必须生成新的 timestamp、nonce 和签名。nonce 只防认证重放，不代表写接口业务幂等。
- 同一请求同时携带普通认证 Token 和完整 OpenAPI 签名材料时失败关闭，不合并两种身份或权限。
- HTTP 与 HTTPS 均可承载；服务端不强制 TLS，但管理页和调用文档必须明确生产环境推荐 HTTPS。

### 凭据创建与管理

1. OpenAPI 默认关闭。关闭时签名入口、管理 API 和目录 API 不可用，前端入口失败关闭；普通请求不受影响。
2. 本人操作要求当前 Client 会话具备 `system:openApi:self`；后端从当前登录用户取得 owner，拒绝请求体或路径篡改 owner。
3. 管理页面要求 system 菜单/按钮权限；跨用户 target-user 操作还必须由后端确认当前用户是超级管理员。权限字符分别为 `system:openApi:list`、`system:openApi:query`、`system:openApi:add`、`system:openApi:edit`、`system:openApi:remove`。
4. 创建输入包含应用名称、可选过期时间和可选备注。未设置过期时间表示永久有效。
5. 创建产生新的 AppKey/AppSecret；数据库唯一约束保证一个用户最多一条未删除凭据。只有成功响应显示一次 AppSecret。
6. 查询只返回 AppKey、应用名称、状态、过期时间、备注和审计时间，不返回可恢复 secret 或密文。
7. 重置只轮换 AppSecret 并保留 AppKey；旧 secret 与旧机器 Session 立即失效。删除后重新创建会产生新的 AppKey/AppSecret。
8. 禁用期间所有签名调用失败；重新启用保留当前 secret，但下一次有效调用重新建立机器 Session。
9. 所有变更 API 使用 POST 和安全的 `@Log`；请求/响应中的 secret、签名和内部 Token 不得进入操作日志。

### 开放接口调用

1. 请求入口验证 header 格式、协议版本和目标 Spring MVC HandlerMethod。
2. 只有目标方法直接声明方法级 `@OpenApi` 才进入开放通道；类级标注无效。
3. 服务端检查 AppKey 对应凭据存在、启用、未过期，owner 用户有效，并完成签名、时间窗口、nonce 和两级限流。
4. 两级限流必须同时满足：每 AppKey 默认 1000 次/分钟；每 AppKey + 开放接口默认 100 次/分钟。阈值可配置，Redis 不可用时失败关闭。
5. 验签后查找该凭据的机器 TokenSession。命中时复用其中标准 `LoginUser`；未命中时在并发控制下调用 system 授权快照端口并写入 TokenSession。
6. 当前请求注入机器 Token 后继续执行目标方法原有权限、角色、数据权限和敏感字段链；请求完成后清理 request Storage。内部 Token 不进入协议、响应、文档或日志。
7. 方法没有权限/角色要求时，任何持有有效凭据且 owner 状态正常的用户均可调用；复杂 AND/OR 权限语义必须与原 Sa-Token 注解一致。
8. 全局 `LoginUser.clientPk/clientKey` 保持为空。目标方法或下游能力需要单一 Client 时失败关闭；调用方不能传 Client，服务端不能猜测或填充默认 Client。

### 全局授权快照与缓存失效

1. 合法 Client 必须同时满足：Client 正常、已关联正常登录域、用户持有该登录域关系。
2. 对每个合法 Client 纳入正常的自动默认角色和用户显式角色，只纳入状态正常菜单的角色/权限字符；集合去重。
3. `dataScopeRoleMap` 按权限字符汇总所有实际生效角色 ID，由现有数据权限链联合计算。超级管理员继续使用 `superadmin` 和 `*:*:*`。
4. 在线 Session 不是授权事实。Redis miss 只从 system 权威用户、Client、登录域、角色、菜单和数据权限关系重建。
5. 缓存重建不按当前请求路径新增权限；目标权限不存在时仍返回现有权限拒绝结果。
6. 凭据状态、用户状态或任何影响全局授权集合的 system 写操作，在权威变更完成后、成功响应前注销受影响用户的机器 Session。
7. 会话注销复用现有活动 Token 查找、`LoginUser` 匹配、`logoutByTokenValue`、失败重试和 `PlusSaTokenDao` 集群失效；禁止直接删除 Redis key，也不新增授权 revision 缓存。

### 接口目录与调用文档

1. 开放注册表以 Spring MVC 实际 HandlerMapping 为路由权威，仅收录方法级 `@OpenApi` 方法。
2. SpringDoc 提供 path/query/header/body 参数、schema、必填性、请求和响应模型；注册表补充真实 method/path、注解说明和原始权限/角色要求。
3. 本人目录以当前用户 ID，管理员预览以显式 `targetUserId` 调用同一只读授权快照解析器；预览不伪造登录、不创建机器 Session，也不继承查看者的接口权限。
4. 目录只显示目标用户按当前权威授权可调用的方法。目标用户尚未创建凭据不影响理论目录预览。
5. 详情提供请求/响应示例、cURL 和 Java 示例，示例使用占位凭据并严格遵循 NAMEWTA v1；已保存 AppSecret 永不回显。
6. 注册表、目录、详情、示例和真实调用必须保持 method/path/权限/schema 一致；无法解析的映射或 schema 失败关闭，不生成猜测文档。

### 前端双入口

#### 系统管理 > 应用开放管理

- 由 system web-domain manifest 注册动态页面，服务端菜单和按钮权限决定可见操作。
- 超级管理员可搜索用户，查看凭据安全字段，代建、重置、启用、禁用、删除凭据，并查看目标用户可调用接口。
- 页面所有目录结果都以被选用户为主体，不以管理员自身权限替代。

#### 个人信息 > 开放应用

- 个人信息仍是 `admin-web` 自有静态路由；在现有同级 Tabs 中增加“开放应用”，不创建动态个人菜单。
- Tab 只接受 current-user scope，不显示用户选择器或跨用户命令。
- 用户可创建、查看、重置、启用、禁用、删除自己的唯一凭据，并查看本人接口目录、详情和调用示例。

两个入口复用同一 system domain service、web-domain 组件和局部状态。App 只提供 runtime、当前会话、权限求值、反馈与剪贴板等宿主能力；不得在 App 中复制 transport、领域模型、owner-scope 或目录过滤逻辑。创建/重置后的 secret 使用一次性确认视图，离开后不能再次查看。

### 边界、失败与稳定错误行为

- OpenAPI 关闭或启用时缺少有效 KEK、唯一 SPI 或必要配置时失败关闭；启用配置无效必须阻止 OpenAPI 能力进入可用状态。
- 缺失/格式错误/不支持版本的签名材料、未知/禁用/过期 AppKey、owner 无效、签名错误、超时和重放返回同一认证失败类别，不泄露具体失败阶段或凭据存在性。
- 超过任一级额度返回统一限流失败；失败请求不能绕过 AppKey 总额度或接口额度。
- HandlerMethod 未标注 `@OpenApi` 时签名通道拒绝；普通 Token 仍按原规则访问该方法。
- 验签成功但权限或角色不足时沿用现有 Sa-Token 禁止访问行为，Redis 中不新增目标权限。
- Client 相关下游调用、授权快照不可构建、Redis/KEK/system SPI 不可用或机器 Session 无法安全创建/注销时失败关闭。
- 并发创建只允许一条凭据成功；其他请求得到统一冲突结果，不返回第二个 secret。
- 管理 API 继续使用项目现有 `R` envelope 和全局异常映射，不新增平行错误包装。
- 日志、统计和计费扩展失败不得改变已经确定的业务响应；安全认证和会话失效失败不属于可忽略日志失败。

### 状态转换与不变量

`不存在 -> 启用`：创建新 AppKey/AppSecret，一次性显示 secret。

`启用 -> 启用`：重置 secret，AppKey 不变；旧 secret 和机器 Session 立即失效。

`启用 -> 禁用 -> 启用`：禁用期间拒绝调用；重新启用后使用当前 secret，Session 按需重建。

`启用/禁用 -> 删除 -> 不存在`：凭据和 Session 不再可用；再次创建产生全新 AppKey/AppSecret。

始终成立：

- 每用户最多一条未删除凭据，凭据只绑定 `userId`。
- AppSecret 仅创建或重置成功时显示一次，持久化只保存 AES-256-GCM 密文及 KEK 版本元数据。
- 每次开放调用都重新验签、登记 nonce 并执行限流；缓存只复用授权快照。
- Redis 不是授权事实源；缓存 miss 不产生授权写入。
- 管理权限不等于开放接口调用权限；管理员预览不继承管理员自身权限。
- OpenAPI 不绑定或伪装前端 Client；普通 Client 认证授权行为不因 OpenAPI 放宽。

## 3. 用户故事

- **US-001**：作为后端开发者，我希望逐方法使用 `@OpenApi`，以便显式开放接口并自动获得与真实路由一致的文档。
- **US-002**：作为第三方集成方，我希望用唯一 AppKey/AppSecret 的版本化签名调用接口，以便不模拟登录或维护普通 Token。
- **US-003**：作为凭据 owner，我希望在个人信息中管理自己的唯一凭据并一次性取得 secret，以便安全接入外部应用。
- **US-004**：作为凭据 owner，我希望查看仅按我当前授权过滤的接口目录与示例，以便正确调用可用能力。
- **US-005**：作为超级管理员，我希望管理任意用户的凭据并预览目标用户目录，以便治理开放接入。
- **US-006**：作为多 Client 用户，我希望 OpenAPI 使用我在所有合法 Client 下的当前有效权限，以便同一后端不需要多套 key。
- **US-007**：作为安全维护者，我希望 OpenAPI 复用标准 LoginUser、Sa-Token Session 和现有权限链，以便避免第二套授权事实源。
- **US-008**：作为运维人员，我希望能力默认关闭、凭据可撤销、调用可限流且日志不泄密，以便控制生产风险。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | OpenAPI 默认关闭 | 启动应用并请求管理 API 或签名接口 | OpenAPI 不可用，普通 Token 行为不变 | Spring context + controller module test |
| AC-002 | 方法 A 有方法级 `@OpenApi`，同类方法 B 无 | 用合法签名分别请求 | A 进入开放通道，B 被拒绝；类级标注不改变结果 | HandlerMapping registry test |
| AC-003 | 固定请求向量 | Java 与另一语言实现生成签名 | canonical request 和签名完全一致 | fixed cross-language vectors |
| AC-004 | 合法签名 | 篡改 method/path/query/body/timestamp/nonce 任一字段 | 认证失败且不泄露具体原因 | signature unit matrix |
| AC-005 | 同一 AppKey + nonce 并发请求 | 同时提交 | 只有一次通过 nonce 登记 | Redis contract test double |
| AC-006 | 合法请求达到任一级限额 | 继续调用 | 返回限流失败；全局和接口额度均不可绕过 | rate-limit module test |
| AC-007 | 网络重试 | 使用新/旧 nonce 分别重签 | 新 nonce 可按业务合同重试，旧签名被拒绝；无响应重放 | protocol test |
| AC-008 | 用户无凭据 | 本人和管理员并发代建 | 最多一条成功，只有成功响应得到一次性 secret | persistence/service module test |
| AC-009 | 凭据已创建 | 查询详情或刷新页面 | 返回安全字段，不返回 secret、密文或内部 Token | controller + transport test |
| AC-010 | 凭据启用 | 重置后分别使用旧/新 secret | AppKey 不变，旧 secret 拒绝，新 secret 可用，旧 Session 注销 | credential lifecycle test |
| AC-011 | 凭据启用/禁用/删除 | 依次调用 | 状态转换和拒绝行为符合合同；重新创建产生新 AppKey | lifecycle state test |
| AC-012 | Session 命中 | 连续发起两个不同 nonce 请求 | 两次都验签，第二次复用同一标准 LoginUser，不重复查权威关系 | Sa-Token bridge spy test |
| AC-013 | Session 缺失 | 首次有效调用 | 从 system 只读装载已有授权并写 TokenSession，不补写权限 | cache-aside module test |
| AC-014 | 用户无目标权限 | 有效签名调用 | 沿用现有权限链拒绝，Redis 不出现目标权限 | Sa-Token authorization test |
| AC-015 | Client A 有目标权限、Client B 无 | 有效签名调用 | 合法 Client 权限并集允许调用，且不要求 clientid | authorization matrix |
| AC-016 | Client/登录域/关系/角色/菜单任一停用 | 重建快照 | 停用部分不进入 roles、permissions 或 dataScopeRoleMap | authorization matrix |
| AC-017 | 超级管理员 owner | 有效签名调用开放接口 | 使用 `superadmin` 与 `*:*:*` 语义 | superadmin module test |
| AC-018 | 权威授权或凭据变化 | 写操作成功 | 目标机器 Session 已经注销；失败时写操作显式失败 | session invalidation interaction test |
| AC-019 | 标注方法读取单一 Client | 通过 OpenAPI 调用 | `clientPk/clientKey` 为空并失败关闭，不选择默认 Client | negative Client-dependency test |
| AC-020 | 目标用户没有凭据 | 本人或超级管理员查看接口目录 | 仍可预览理论可调用接口，不创建机器 Session | catalog service test |
| AC-021 | 管理员权限高于目标用户 | 查看目标用户目录 | 仅显示目标用户可调用接口 | target-user catalog test |
| AC-022 | 目录中的接口 | 比较详情、SpringDoc 与真实调用 | method/path/权限/schema 和调用结果一致 | registry/SpringDoc contract test |
| AC-023 | 普通用户有 `system:openApi:self` | 打开个人信息 | 显示同级“开放应用”Tab，只能操作本人 | domain/web-domain component test |
| AC-024 | 普通用户无 self 权限 | 打开个人信息并直接请求 API | Tab 不显示且后端拒绝 | permission component + controller test |
| AC-025 | 超级管理员具有管理权限 | 从系统菜单进入应用开放管理 | 可选择目标用户并执行完整生命周期与目录预览 | manifest + page integration test |
| AC-026 | 非超管伪造 targetUserId | 调用管理员 API | 后端拒绝，不因页面隐藏而放行 | owner-scope controller test |
| AC-027 | 创建或重置成功 | 关闭一次性 secret 视图后再次查看 | secret 无法恢复；剪贴板失败不泄密 | web-domain workflow test |
| AC-028 | 请求含凭据和敏感 JSON | 检查运行日志和操作日志 | 凭据原值永不出现，JSON 在上限内递归脱敏 | `SysLogFilterTest` extension |
| AC-029 | 请求/响应为非 JSON、文件、二进制或流 | 检查日志 | 只记录媒体类型、长度、状态、耗时等元数据 | HTTP logging module test |
| AC-030 | 普通浏览器 Token 请求 | 回归 Client header/path/IP、动态路由和权限 | 既有行为保持且不进入 OpenAPI Session | admin security regression |

## 5. 范围

### IN

- `ruoyi-common-openapi` Maven 子模块、BOM/聚合声明、方法级注解、运行时配置、签名、防重放、限流编排、注册表、会话桥接和类型化 SPI。
- `ruoyi-system` 凭据表、Mapper/service、全局授权快照、管理 API、目录/详情 API、会话失效和调用事件。
- `ruoyi-admin` 依赖与 Bean 装配、OpenAPI 环境配置。
- `script/sql/namewta/DDL.sql` append-only 表/索引和 `DML.sql` 菜单/按钮权限初始化。
- `plus-ui-namewta` 的 system domain transport/模型/service、system web-domain 共享组件与动态管理页、manifest 注册、admin runtime 组合和个人信息“开放应用”Tab。
- cURL/Java 调用示例、接口详情、一次性 secret 交互和前后端权限失败状态。
- 固定签名向量、后端单元/模块测试、前端聚焦测试、架构检查、类型检查、lint 和构建。

### REUSE

- `ruoyi-api` 标准 `LoginUser`、角色 DTO 和现有跨模块类型边界。
- `common-satoken` 的 `LoginHelper`、`SaPermissionImpl`、TokenSession、request Storage、`PlusSaTokenDao`。
- `common-redis` 的 Redis 工具、限流能力和现有集群缓存失效协调器；不复用普通 Token 型 `@RepeatSubmit` key 作为 OpenAPI 协议。
- system 的用户、Client、登录域、默认/显式角色、菜单、数据权限权威关系和会话失效模式。
- Spring MVC HandlerMapping、`common-doc`/SpringDoc 和项目统一响应/异常体系。
- `common-web` 的有界 HTTP 正文采集、媒体策略与递归脱敏。
- 前端现有 `@namewta/domain-system`、`@namewta/web-domain-system`、WebDomainManifest、权限 evaluator/runtime 和 App 静态个人信息壳。

### OUT

- **OOS-001**：类级开放和自动批量开放，避免未来新增方法意外暴露。
- **OOS-002**：旧 MD5 协议、旧表或旧凭据迁移；当前不存在兼容负担。
- **OOS-003**：每 Client/每应用多凭据、服务账号、双 secret 平滑轮换和 OAuth2。
- **OOS-004**：依赖唯一 Client 上下文的开放接口；必须先形成独立 Client 无关合同。
- **OOS-005**：框架级业务幂等、`Idempotency-Key` 和响应重放。
- **OOS-006**：IP 白名单、计费业务和独立原始正文审计表。
- **OOS-007**：client-web、mobile-web、miniapp-taro 或第二个可部署 App；它们仍是占位。
- **OOS-008**：真实 MySQL、真实 Redis、多进程集群和全量 Playwright E2E 作为首期强制发布门禁；这些保留为显式残余风险。
- **OOS-009**：修改普通 SecurityConfig 以全局绕过 Client 检查，或恢复已经退出组合的 `gen`。

## 6. 已锁定实现约束

- **DEC-001**：首期同步交付后端与 admin-web 双入口，不拆成后续前端 change。来源：`ADR-020`。
- **DEC-002**：凭据每用户唯一，只绑定 `userId`，不绑定 `clientPk`。来源：`ADR-007`、`ADR-008`。
- **DEC-003**：只支持方法级 `@OpenApi`，逐接口显式开放。来源：`ADR-008`。
- **DEC-004**：NAMEWTA v1 使用 HMAC-SHA256、完整请求语义签名、原子 nonce 和两级限流；HTTP/HTTPS 均允许，生产推荐 HTTPS。来源：`ADR-002`、`ADR-017`。
- **DEC-005**：common 运行时 + system 业务实现 + admin 装配，不引入 Command/MQ 抽象。来源：`ADR-003`。
- **DEC-006**：管理面使用当前 Client 菜单/按钮权限和 service owner 校验；调用面使用全局身份和目标方法原权限。来源：`ADR-004`、`ADR-011`。
- **DEC-007**：复用标准 `LoginUser` 和现有 Sa-Token 注解链，不建平行 Principal、注解或缓存。来源：`ADR-009`、`ADR-010`、`ADR-014`。
- **DEC-008**：内部机器 Token 仅服务端持有并只注入 request Storage，每次调用仍重新验签。来源：`ADR-005`、`ADR-009`。
- **DEC-009**：AppSecret 使用 CSPRNG、AES-256-GCM 和外部版本化 KEK，只显示一次；重置立即切换。来源：`ADR-006`。
- **DEC-010**：真实调用、本人目录和目标用户预览共享注册表、授权解析器和权限匹配函数。来源：`ADR-012`。
- **DEC-011**：全局授权只聚合用户可合法登录的有效 Client、默认/显式角色和正常菜单。来源：`ADR-018`。
- **DEC-012**：OpenAPI 首期只允许 Client 无关方法，机器身份不提供 Client fallback。来源：`ADR-019`。
- **DEC-013**：授权/凭据变化复用 Sa-Token DAO 注销机器 Session，不建授权 revision。来源：`ADR-016`。
- **DEC-014**：正文审计只记录有界脱敏 JSON；其他媒体只记元数据。来源：`ADR-015`。
- **DEC-015**：两个前端入口复用 system domain/web-domain；管理页为 manifest 动态页面，个人中心保持 App 静态壳。来源：`ADR-013`、`ADR-014`、`ADR-020`。
- **DEC-016**：首期采用含前端门禁的轻量模块级验证，不将模拟测试宣称为真实基础设施验证。来源：`ADR-021`。

## 7. 数据、接口与兼容

### 公共 HTTP 接口

后端新增 `/system/openApi` 资源。只读接口使用 GET，状态变化使用 POST：

| Scope | Method 与路径 | 行为 |
|---|---|---|
| self | `GET /system/openApi/self/credential` | 查询本人凭据安全字段 |
| self | `POST /system/openApi/self/credential/{action}` | 本人凭据生命周期；action 为 `create`、`reset`、`enable`、`disable`、`delete` |
| self | `GET /system/openApi/self/interfaces` | 本人可调用接口目录 |
| self | `GET /system/openApi/self/interfaces/{interfaceId}` | 本人接口详情与示例 |
| admin | `GET /system/openApi/users` | 超管检索用户与凭据摘要 |
| admin | `GET /system/openApi/users/{userId}/credential` | 查询目标用户凭据安全字段 |
| admin | `POST /system/openApi/users/{userId}/credential/{action}` | 超管代管生命周期；action 为 `create`、`reset`、`enable`、`disable`、`delete` |
| admin | `GET /system/openApi/users/{userId}/interfaces` | 目标用户目录 |
| admin | `GET /system/openApi/users/{userId}/interfaces/{interfaceId}` | 目标用户接口详情与示例 |

具体响应沿用项目 `R`/分页 envelope。状态变化 API 必须配置准确且不保存敏感正文的 `@Log`。机器调用不新增业务路径，而是在原始 `@OpenApi` 方法真实路径上使用 NAMEWTA v1 header。

### 数据模型与持久化

system 拥有一张 OpenAPI 凭据表，至少包含：

- 主键、owner `user_id`、公开 `app_key`、应用名称、状态、可选过期时间和备注；
- AppSecret 的 AES-256-GCM 密文、nonce/tag 与 `kek_version`；
- 乐观锁、创建/更新者、创建/更新时间和项目标准逻辑删除字段。

数据库对未删除 owner 和 AppKey 建立唯一约束，使并发创建最终收敛。AppSecret、签名和内部机器 Token 不以明文进入业务表。NAMEWTA 结构与初始化数据只追加到 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>` 和 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>`；不修改上游 `ry_vue.sql`。

### 前端公共合同

- system domain 对 transport 做运行时缩窄并映射为领域自有模型，不向 Web 层暴露密文或可恢复 secret。
- system web-domain 公开可由 current-user/target-user scope 组合的 OpenAPI 组件和 manifest registration。
- `admin-web` 只从包公开 exports 导入，提供 runtime 并显式组合；禁止深层导入、跨包相对导入和 App 私有 API 副本。

### 兼容与迁移

- 这是全新 NAMEWTA v1 能力，没有旧协议、旧凭据或旧表迁移。
- OpenAPI 默认关闭；启用前必须完成 DDL/DML、配置有效 KEK 版本和 Redis。
- 普通 Token、`clientid` 一致性、Client 路径/IP、动态路由和既有 Controller 合同保持不变。
- KEK 轮换必须保留旧版本解密能力，直到所有密文迁移到新版本；active version 只用于新建和重置。
- 新增 system 菜单、按钮权限和 manifest component key 必须同步交付，未知或未选择注册失败关闭。

### 发布与运维

- `open-api.enabled` 默认 `false`，可通过环境配置启用；KEK 明文只来自批准的 secret provider，不提交、不进浏览器 bundle或日志。
- 时间窗口、nonce TTL、两级限流和机器 Session TTL 均可配置；前四项安全默认值为 60 秒、60 秒、每 AppKey 1000 次/分钟、每 AppKey + 接口 100 次/分钟，机器 Session TTL 沿用部署配置。
- OpenAPI 启用但 KEK、Redis 或唯一 system SPI 无效时应用启动失败，不进入部分可用状态。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 使用至少 128 bit AppKey/nonce、256 bit AppSecret、AES-256-GCM、HMAC-SHA256、常量时间比较和原子防重放。secret、签名、Token、Cookie 和内部机器 Token 不进入日志、文档、异常或浏览器持久化。
- **NFR-002 性能与容量：** 正常调用验签后命中 Redis TokenSession，避免逐请求权限查库；Session miss 并发重建必须单写收敛。注册表在启动期构建，不逐请求扫描类路径。正文日志默认每方向最多 1 MiB。
- **NFR-003 可用性与可靠性：** 认证、nonce、Redis、system SPI、KEK、Client 依赖和会话失效失败均失败关闭。日志/统计事件失败不改变业务响应；安全状态变更失败必须显式传播。
- **NFR-004 可观测性与运营：** 调用事件记录不含敏感值的凭据安全引用、owner、接口、结果类别、耗时、请求 ID 和时间。异步扩展使用 Spring 管理的执行器与有界队列，不复制原始正文。
- **NFR-005 可维护性与上游同步：** 定制优先集中在新 common 模块、system 适配器和新增前端领域资源；不得重写普通登录主链、`LoginUser`、`LoginHelper`、`SaPermissionImpl` 或 `PlusSaTokenDao`。共有模块扩展必须最小、通用、隔离并说明 WHY。
- **NFR-006 前端体验：** 两入口对加载、空状态、禁用、过期、一次性 secret、复制失败、权限拒绝和服务不可用提供明确状态；动态内容不得造成布局跳动或覆盖现有个人信息 Tabs。

## 9. 验证策略

本 change 使用用户确认的轻量模块级发布门禁。真实 MySQL、真实 Redis、多进程集群和全量 Playwright E2E 不作为首期强制条件，任何模拟测试不得报告为真实基础设施验证。

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 注解、注册表与 SpringDoc | unit + Spring context | AC-001、AC-002、AC-020 至 AC-022 | common-doc/Spring MVC 测试形态 | handler/schema inventory |
| canonical request 与签名 | pure unit + fixed vector | AC-003、AC-004、AC-007 | common-openapi 模块测试 | 固定输入/输出向量 |
| nonce 与两级限流 | module test + Redis test double | AC-005、AC-006 | common-redis 能力测试形态 | 原子调用与计数断言 |
| 凭据生命周期与加密 | service/controller module test | AC-008 至 AC-011 | system service/MockMvc 形态 | 状态与响应断言 |
| LoginUser 与 Session 桥接 | Sa-Token module test | AC-012 至 AC-019 | `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/authorization/session/ClientSessionServiceUnitTest.java</Path>` | Session/调用交互 |
| 目录与 owner scope | service/controller contract test | AC-020 至 AC-022、AC-026 | system controller/service 测试形态 | target/current user 矩阵 |
| HTTP 日志安全 | common-web unit test | AC-028、AC-029 | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/test/java/org/dromara/common/web/logging/SysLogFilterTest.java</Path>` | 捕获日志事件 |
| system domain | Vitest | AC-009、AC-020 至 AC-027 | `<Path>plus-ui-namewta/packages/domains/system/src/index.test.ts</Path>`、transport tests | 请求与领域映射 |
| system web-domain | Vitest component/workflow | AC-023 至 AC-027 | `<Path>plus-ui-namewta/packages/web-domains/system/src/index.test.ts</Path>` | manifest、scope、状态 |
| admin App 组合与个人页 | Vitest integration | AC-023 至 AC-027、AC-030 | `<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>` | 注册与页面组合 |
| 普通认证回归 | backend/frontend module regression | AC-030 | 现有 authorization 与 navigation tests | 不变合同 |

后端至少执行受影响测试和 reactor 打包：

```bash
./mvnw -pl ruoyi-common/ruoyi-common-openapi,ruoyi-modules/ruoyi-system,ruoyi-admin -am test
./mvnw -pl ruoyi-admin -am package -DskipTests
```

前端至少执行架构检查、目标包测试/类型检查，以及项目现有 lint 与生产构建：

```bash
pnpm architecture:check
pnpm --filter @namewta/domain-system test
pnpm --filter @namewta/web-domain-system test
pnpm --filter @namewta/admin-web test
pnpm typecheck
pnpm lint
pnpm build:prod
```

## 10. 风险、假设与未决问题

### 风险

- **RISK-001：真实基础设施未作为强制门禁。** 测试替身不能证明 MySQL 唯一约束竞争、Redis 原子 nonce/限流或多节点 Session 确认失效；Evidence 必须明确标为未验证，不得宣称集群通过。
- **RISK-002：全局权限并集扩大机器身份能力。** 状态过滤、默认角色、显式角色、菜单和数据权限任一实现偏差都可能放大授权，必须以完整负向矩阵覆盖。
- **RISK-003：授权写路径遗漏 Session 注销。** 任一遗漏都可能保留旧快照；Ticket 必须建立权威变更到受影响 userId 的完整映射。
- **RISK-004：自定义 canonicalization 跨语言漂移。** 固定向量必须先于客户端示例和服务端发布形成，并覆盖重复 query、空值、Unicode、编码 path 和空/二进制 body。
- **RISK-005：每用户单 secret 不支持无停机轮换。** 重置会立即使旧调用方失败，这是已接受的首期产品取舍。
- **RISK-006：HTTP 允许明文传输。** HMAC 不提供正文机密性；生产文档必须推荐 HTTPS，但服务端不强制。

### 已采用的低影响假设

- **ASM-001：** 内部机器登录命名空间与浏览器 userType/device 不碰撞，具体常量不对第三方公开。
- **ASM-002：** 目录在用户未创建凭据时仍可只读预览，因为目录表达授权能力而非凭据状态。
- **ASM-003：** 前端 OpenAPI 资源目录按稳定后端 base path 使用 `open-api` kebab-case，具体组件名遵循所在包现有命名。
- **ASM-004：** AppKey、AppSecret、nonce 的视觉前缀可以由实现者选择，只要熵、编码和固定向量合同不变。

### 未决问题

无。
