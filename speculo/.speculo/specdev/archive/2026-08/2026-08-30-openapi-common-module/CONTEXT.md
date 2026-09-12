# OpenAPI Platform

**开放凭据**：每个用户最多拥有一条只绑定 userId 的全局 AppKey/AppSecret 组合，用于机器调用而不是替代浏览器 Token；secret 仅创建或重置时显示一次。
_Avoid_: 绑定 clientPk、每 Client 多密钥、Bearer Token、可重复查看的 secret

**开放接口**：Controller 方法上显式标注 `@OpenApi` 后进入开放注册表的 HTTP 接口；类级注解不产生开放语义。
_Avoid_: 开放 Controller、类级批量开放

**NAMEWTA 签名协议**：不兼容来源 MD5 方案的版本化 HMAC-SHA256 请求认证协议，签名覆盖请求语义并使用 nonce 防重放；HTTP 与 HTTPS 均可承载，TLS 只负责可选的传输机密性。
_Avoid_: MD5 签名、URL 参数认证、把签名等同于加密

**应用开放管理**：本 change 在“系统管理”菜单下交付、归属 `ruoyi-system` 的管理员 OpenAPI 管理页与后端 API；当前前端 Client 的既有菜单/按钮权限决定谁能进入和执行管理操作，service 层仍校验目标用户范围。超级管理员可管理全部用户的唯一凭据，并可按目标用户查看可调用接口。
_Avoid_: 独立 ALL/ROLES 准入配置、仅靠页面隐藏授权

**开放应用 Tab**：个人中心/个人信息现有 Tabs 中新增的同级“开放应用”入口；只面向当前登录用户，提供本人唯一凭据的创建与管理、本人可调用接口、接口详情和调用文档。Tab 可见性与操作继续受既有菜单/按钮权限控制，后端仍独立鉴权。
_Avoid_: 把个人入口放入系统管理菜单、允许选择其他用户、为 Tab 新建一套 API 或权限算法

**双入口单能力**：“系统管理 > 应用开放管理”和“个人信息 > 开放应用”使用同一 OpenAPI domain/web-domain、领域服务、开放接口注册表和授权解析器，仅调用 scope 分别为 target user 与 current user。
_Avoid_: 复制管理员页形成个人页、复制 transport 类型和缓存状态、按页面分别过滤接口

**管理面与调用面**：管理面负责创建、重新创建/重置、禁用、删除和查看凭据；调用面只处理已经创建的凭据，以 AppKey/HMAC 验证后恢复调用用户身份。两者共享领域服务与授权解析器，但管理菜单权限绝不等于开放接口调用权限。
_Avoid_: 创建凭据时固化一份永久权限、用管理者权限执行目标用户调用

**OpenAPI 运行时端口**：由 `ruoyi-common-openapi` 声明、由 `ruoyi-system` 提供唯一实现的窄类型化 SPI，用于解析凭据、构建 `ruoyi-api` 已有 `LoginUser` 调用身份和发布调用事件；`common-satoken` 已依赖 `ruoyi-api`，不得为跨层复用再复制身份模型。
_Avoid_: common 直连 Mapper、MQ/Command 分发、通用 Object 调用

**开放接口目录**：从实际 Spring MVC 映射和 SpringDoc 模型生成、按用户 OpenAPI 全局权限并集过滤的开放接口列表与详情合同。
_Avoid_: 独立类路径扫描结果、手写参数模型

**OpenAPI 全局身份**：独立于 `sys_client` 的机器调用身份；调用方无需登录、无需发送 `clientid`，服务端聚合用户在所有合法 Client 下的当前有效角色、权限字符和数据权限，但不加载动态路由树。
_Avoid_: 伪装前端 Client、复用某个前端 clientPk、聚合所有启用 Client 的默认权限

**前端 Client 身份**：普通 Sa-Token 登录使用的单 Client 身份，负责约束动态路由、菜单树、Token 请求头一致性、访问路径和 IP 等规则；OpenAPI 全局身份不改变它。
_Avoid_: 因 OpenAPI 特例取消普通请求的 Client 校验

**OpenAPI 机器会话**：AppKey 验签后由服务端关联的内部 Sa-Token TokenSession；其中仍保存标准 `LoginUser`，通过现有 Redis DAO 复用，并把内部 Token 仅写入当前请求 Storage 以驱动既有注解链。
_Avoid_: 返回内部 Token、绕过每次验签、另建权限注解、缓存 miss 时自动授权

**调用期权限缓存**：凭据已经创建后的 cache-aside 身份恢复机制。Redis 中存在有效 `LoginUser` 就直接复用；不存在或会话已被授权变更注销时，才从 system 权威用户、角色、菜单和数据权限关系重新组装并写回 TokenSession，随后执行同一套权限注解判断。
_Avoid_: 把“自动获取权限”解释为申请、授予或补写权限

**授权快照重建**：调用期权限缓存未命中或失效时的只读装载动作；它只能复制权威数据中已经存在的授权，目标权限不在结果中时保持 403。
_Avoid_: 根据当前请求路径补写权限、把 Redis 缓存当授权事实源

**可调用接口预览**：使用目标用户 ID、运行时同一授权快照解析器和同一开放接口注册表计算出的只读目录；超级管理员查看某用户时不继承管理员自己的权限，也不创建机器会话。
_Avoid_: 单独编写管理端过滤 SQL、按查看者权限过滤目标用户目录

**复用优先适配层**：位于 `ruoyi-common-openapi`、只编排现有 Sa-Token 公共 API 与窄类型化 system SPI 的薄桥接层；标准 `LoginUser` 继续作为权限快照，OpenAPI 专属信息优先放 Token extra/TokenSession sidecar。
_Avoid_: 重写普通登录主链、修改 LoginUser 以承载凭据领域、平行 Redis 权限缓存、通用 CommandBus

**OpenAPI 授权会话失效**：system 权威授权或凭据状态变化后注销受影响用户的 OpenAPI 机器 Session；删除继续经过现有 Sa-Token DAO，而该 DAO 已复用 `ClusterCacheInvalidationCoordinator` 完成跨节点确认和本地 Caffeine 精确失效。下一次有效验签只能 cache miss 后重建。
_Avoid_: 另建授权 revision 缓存、直接删除 Redis key、容忍旧的“多节点 5 秒窗口”描述

**安全正文审计**：复用 `common-web` 现有 HTTP 日志的每方向默认 1 MiB 有界采集与 JSON 递归脱敏；密码、Token、Cookie、AppKey、AppSecret、签名和内部机器 Token 永不以原值落日志，非法或截断 JSON 失败关闭，非 JSON 文本、multipart、二进制、文件、音视频和流式正文只记录元数据。OpenAPI 调用事件只补充可计量元数据，不复制第二份原始正文。
_Avoid_: 原样记录凭据、因“完整正文”绕过脱敏、再建一套无界 body 日志

**前端承接位置**：本 change 在当前 admin App 显式组合的 `system` domain/web-domain 中实现 OpenAPI 领域能力；“系统管理 > 应用开放管理”进入 system web-domain 的动态菜单清单，个人信息页仍是 App 自有静态页，并在现有 Tabs 中组合 system web-domain 提供的“开放应用”组件。已退出当前组合的 `gen` 不属于该能力。
_Avoid_: 恢复 gen、把个人中心改为动态菜单、在 App 内复制 system transport 与状态

**OpenAPI 防重放**：Redis 在默认 60 秒接受窗口内原子登记 AppKey 与 nonce 组合；同一 nonce 只能成功一次，网络重试必须生成新的 timestamp、nonce 和签名。nonce 只证明请求未被重放，不代表写操作具有业务幂等性。
_Avoid_: 把 nonce 当 Idempotency-Key、重用原签名重试

**OpenAPI 两级限流**：每 AppKey 全局额度和每 AppKey + 开放接口额度必须同时满足；默认分别为 1000 次/分钟和 100 次/分钟，均由 OpenAPI 配置覆盖。
_Avoid_: 只按 IP 限流、让单接口绕过凭据总额度

**合法 OpenAPI Client 集合**：仅包含状态正常、配置正常登录域且用户持有该登录域关系的 Client；每个合法 Client 的正常默认角色、正常显式角色和正常菜单权限共同构成全局授权输入。
_Avoid_: 所有启用 Client、当前在线 Session 的 Client、只看 sys_user_role

**Client 无关开放接口**：执行语义不需要唯一 `clientPk/clientKey` 的方法级 Spring MVC 接口；首期只有此类方法可以使用 `@OpenApi`，全局机器身份不提供 Client fallback。
_Avoid_: 调用方选择 Client、按权限猜测 Client、填充默认 Client

**OpenAPI 轻量模块级门禁**：后端以单元/模块测试和基础设施测试替身冻结安全合同，不把真实 MySQL、真实 Redis、多进程集群或全量 E2E 作为首期强制条件；前端因属于交付范围，必须通过架构检查及受影响 system domain、system web-domain、admin App 的 lint、test、typecheck、build，并覆盖双入口和 owner scope 的聚焦组件/集成测试。
_Avoid_: 把模拟测试宣称为真实集群验证、因选择轻量门禁而跳过前端验证
