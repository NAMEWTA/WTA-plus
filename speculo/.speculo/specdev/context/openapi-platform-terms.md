# OpenAPI 平台术语

- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-30-openapi-common-module/CONTEXT.md</Path>`
- **Source:** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-01-admin-runtime-capability-reconciliation/ADR.md</Path>` ADR-001、ADR-003
- **Graduated:** 2026-09-02

**开放凭据**：每个用户最多一条、只绑定 userId 的全局 AppKey/AppSecret 组合；用于机器调用，secret 仅创建或重置时显示一次并以 KEK 加密保存。
_Avoid_: 绑定 clientPk、每 Client 多密钥、可重复查看的 secret

**开放接口**：Controller 方法显式标注 `@OpenApi` 后进入开放注册表的 HTTP 接口；类级注解不产生开放语义。
_Avoid_: 开放 Controller、类级批量开放

**NAMEWTA 签名协议**：不兼容旧 MD5 方案的版本化 HMAC-SHA256 请求认证协议，签名覆盖请求语义并使用 timestamp 和 nonce 防重放。
_Avoid_: MD5 签名、URL 参数认证、把签名等同于加密

**OpenAPI 管理面**：“系统管理 > OpenAPI管理”中的管理员管理页及后端 API；使用当前前端 Client 的菜单/按钮权限，并在 service 层校验目标用户范围。
_Avoid_: 应用开放管理、独立 ALL/ROLES 准入、仅靠页面隐藏授权

**开放应用 Tab**：个人中心/个人信息现有 Tabs 中面向当前用户的同级入口，提供本人凭据生命周期、可调用接口、详情和调用文档。
_Avoid_: 放入系统管理菜单、允许选择其他用户、另建一套 API

**双入口单能力**：“系统管理 > OpenAPI管理”和个人信息“开放应用”复用同一 OpenAPI domain/web-domain、领域服务、注册表和授权解析器，只以 target-user 或 current-user scope 区分 owner。
_Avoid_: 复制管理员页形成个人页、按页面分别过滤接口

**管理面与调用面**：管理面在浏览器登录态下管理凭据；调用面只接受有效凭据，在验签后恢复 owner 身份。管理权限和接口调用权限互不推导。
_Avoid_: 用 AppKey 管理凭据、用管理者权限执行目标用户调用

**OpenAPI 运行时端口**：由 `ruoyi-common-openapi` 声明、由 `ruoyi-system` 提供唯一实现的窄类型化 SPI，用于解析凭据、构建标准 `LoginUser` 和发布调用事件。
_Avoid_: common 直连 Mapper、通用 Object 调用、CommandBus

**开放接口目录**：从实际 Spring MVC 映射和 SpringDoc 模型生成，并按目标用户 OpenAPI 全局权限过滤的接口列表与详情合同。
_Avoid_: 独立类路径扫描、手写参数模型、第二份权限清单

**OpenAPI 全局身份**：独立于 `sys_client` 的机器调用身份，聚合用户在所有合法 Client 下的当前角色、权限字符和数据权限，但不加载动态路由树。
_Avoid_: 伪装前端 Client、由调用方选择 clientPk

**OpenAPI 机器会话**：每次 AppKey 验签后由服务端关联的内部 Sa-Token TokenSession；保存标准 `LoginUser`，内部 Token 只进入当前 request Storage。
_Avoid_: 返回内部 Token、绕过逐请求验签、另建权限注解

**调用期权限缓存**：凭据已创建后的 cache-aside 身份恢复机制；Redis 命中有效 `LoginUser` 时复用，未命中时从 system 权威关系重建后再执行原有权限链。
_Avoid_: Redis 是授权事实源、缓存 miss 自动授予权限

**授权快照重建**：机器会话未命中或失效时，从当前权威用户、角色、菜单、Client 与数据权限关系只读装载已有授权的动作。
_Avoid_: 根据请求路径补写权限、从历史在线 Session 推断

**可调用接口预览**：以目标用户 ID、真实调用所用授权解析器和同一开放注册表计算的只读目录；不继承查看者权限，也不创建机器会话。
_Avoid_: 管理端专用过滤 SQL、按管理员权限过滤目标用户目录

**复用优先适配层**：位于 `ruoyi-common-openapi`、只编排现有 Sa-Token 公共 API 与窄 system SPI 的薄桥接层；OpenAPI 专属信息进入 Token extra 或 Session sidecar。
_Avoid_: 重写普通登录主链、复制 LoginUser、平行 Redis 权限缓存

**OpenAPI 授权会话失效**：权威授权或凭据状态变化后注销受影响用户机器 Session，并经现有 Sa-Token DAO 和集群协调器确认各节点本地缓存失效。
_Avoid_: 直接删除 Redis key、容忍固定旧值窗口、另建 revision 缓存

**前端承接位置**：OpenAPI transport 和领域模型归 system domain，共享组件与动态管理页归 system web-domain；admin App 只组合动态管理页和个人信息静态页中的共享 Tab。
_Avoid_: 恢复 gen、在 App 内复制 system transport、把个人页改成动态路由

**OpenAPI 防重放**：Redis 在默认 60 秒窗口内原子登记 `AppKey + nonce`；相同 nonce 只能成功一次，重试必须重新生成 timestamp、nonce 和签名。
_Avoid_: 把 nonce 当 Idempotency-Key、重用原签名重试

**OpenAPI 两级限流**：每 AppKey 全局额度和每 AppKey + 开放接口额度必须同时满足，默认分别为 1000 次/分钟和 100 次/分钟，并允许配置覆盖。
_Avoid_: 只按 IP 限流、让单接口绕过凭据总额度

**合法 OpenAPI Client 集合**：状态正常、配置正常登录域且用户持有该登录域关系的 Client，以及其中实际生效的正常默认角色、显式角色和菜单权限。
_Avoid_: 所有启用 Client、当前在线 Session 的 Client、只查询 sys_user_role

**Client 无关开放接口**：执行语义不需要唯一 `clientPk/clientKey` 的方法级 Spring MVC 接口；当前只有此类方法可以使用 `@OpenApi`。
_Avoid_: 调用方选择 Client、按权限猜测 Client、填充默认 Client

**受管 OpenAPI 启用态**：代码和公开样例默认关闭，由受管环境显式提供 enable、KEK version 和私密 KEK；安全依赖无效时应用启动失败。
_Avoid_: 默认开启、只有 enable 没有 KEK、缺失安全依赖后降级运行
