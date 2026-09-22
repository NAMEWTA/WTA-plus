# WTA SSO 目标验收矩阵

**状态：验收设计，全部产品项未执行。** 本轮仅文档/源码评审。以下条目作为最终 Spec 的候选输入；涉及自托管 UI、refresh、外部全局退出和独立进程的条目随 G 决定冻结，不能提前标为通过或静默删除。

## 验收环境

使用隔离的 MySQL 8.4、Redis、真实 System 用户/注册服务、真实 Sa-Token 签发、三 App 构建产物及真实反向代理。种子数据或明确的测试用例创建用户，不调用生产账户，不依赖真实用户密码。至少提供一个外部 public RP 和一个外部 confidential RP，使用标准 OIDC 客户端库而不是 WTA 私有换票代码。

保留现有 fixture 测试以验证 Cookie、协议组件和发布路由，但最终整体验收不能替换账户认证、注册、RBAC、业务 Token 或撤销实现。测试记录源码版本、环境配置摘要、命令、退出码、用例数与 skip；敏感参数仅保存脱敏引用。

## 行为合同

| ID | 验收场景 | 通过标准与关键负向情况 | Evidence |
|---|---|---|---|
| AC-001 | 内部与外部同一 OP | Admin/Home/public RP/confidential RP 使用同一 issuer、标准端点与真实账户；无私有 R 包装依赖 | 四客户端协议轨迹与浏览器旅程 |
| AC-002 | Admin 首位 WTA SSO 入口 | 点击跳 sso-web；真实密码认证后回到原目标页面；Token 校验完成后才获取身份/菜单 | 浏览器操作、URL 脱敏记录、真实接口断言 |
| AC-003 | Home 同一中央会话 | 同一浏览器从 Home 进入，满足策略时免二次密码；`prompt=login` 必须重认证；`prompt=none` 不显示 UI，不满足条件返回标准错误；`max_age` 与 `auth_time` 体现真实认证时间 | 两 App 旅程与会话断言 |
| AC-004 | Client 隔离 | Admin/Home Token clientid/clientPk 正确；交叉使用被拒；缺少 Client 上下文失败关闭，超管也不例外 | 真实业务 API 正负向测试 |
| AC-005 | SSO 注册到 sys_user | 新用户只生成一个 sys_user；必要登录域关系同事务；注册后用该身份走同一 code 流程 | SQL 计数/同一 user_id、协议轨迹 |
| AC-006 | 用户管理可见 | 授权管理者在现有用户列表/详情找到该 user_id，可修改资料、停用和重置；受限管理员按范围拒绝 | 管理 UI + 数据权限测试 |
| AC-007 | 不因注册提权 | 注册提交额外 role/userType/admin/其他 client 参数不能提升权限；普通用户不能进入 Admin 管理域 | HTTP 参数篡改及权限拒绝 |
| AC-008 | 已有账户跨 Client | 不为新 Client 复制账户；无准入关系时按最终策略申请/授权/拒绝，受限域不能自助越权 | 单账户跨 Client 旅程 |
| AC-009 | 并发注册唯一 | 同一规范化标识并发请求只创建一名用户；其他请求可恢复失败；事务失败无孤儿关系 | 真实数据库并发及故障注入 |
| AC-010 | 发现与固定 issuer | 公开 URL metadata/JWKS 可达；所有端点与配置一致；伪造 Host/forwarded 不改变 issuer | 真实 Nginx/HTTP 与元数据校验 |
| AC-011 | 标准令牌与 ID Token | form 请求、标准字段/状态码/cache headers；签名、算法、iss/aud/azp/exp/nonce 验证；错误 issuer/受众/过期/错 nonce 全拒 | 外部库互通与篡改测试 |
| AC-012 | code/PKCE/state 绑定 | code 过期、重放、错 Client/redirect/verifier、重复参数、非法 PKCE 被拒；并发只消费一次；失败不带用户到未登记地址 | 单元+真实数据库+浏览器负向 |
| AC-013 | confidential 客户端认证 | 无/错/其他 Client secret 不能换票和执行受保护令牌操作；secret 轮换遵守明确失效/重叠策略；public 不靠 secret 认证 | 标准 token/revoke/introspection 测试 |
| AC-014 | scope、同意与最小 claims | 请求/拒绝/撤销/新增 scope 有实际效果；未知 scope 拒绝；仅身份 token 不能调用未授权 WTA API；未验证邮箱/手机不能宣称已验证 | 授权页面、claims 与资源 API 测试 |
| AC-015 | 稳定 subject | 改用户名/邮箱/密码不改变 sub；删除后新账户不能继承旧 sub；UserInfo 与 ID Token sub 一致 | 账户修改/删除再注册场景 |
| AC-016 | 密钥持久化与轮换 | 重启 issuer/key 不意外变化；新 token 使用新 kid；过渡期旧 token 可按约定验证，过期后移除旧 key；私钥从不公开 | 重启/轮换/公钥端点测试 |
| AC-017 | 默认 UI 完整旅程 | 注册、挑战、找回/重置、登录、同意/拒绝、账号切换、过期/取消均有可恢复 UI；后端策略驱动必填项和开关 | sso-web 真实浏览器验收 |
| AC-018 | 选定的自定义 UI | 与默认 UI 产生相同协议语义与同一用户；不跳过准入/同意；事务/继续凭证不能被另一 Client 或浏览器注入；若仅受控 OP UI，不冒充已支持任意域采密码 | 两种 UI 旅程和交互威胁测试 |
| AC-019 | 账户生命周期失效 | 停用/删除/重置密码/撤销登录域后，旧 sid/code/refresh 与业务 token 按承诺时限失效；重新启用不复活被撤销会话 | 真实管理 API + 失效时延记录 |
| AC-020 | Client 生命周期失效 | Client 停用/删除、回调/认证材料/权限变化按策略生效；原请求不能在变更后绕过新约束 | 管理面变更与在途事务测试 |
| AC-021 | refresh 轮换 | 若纳入最终范围：原子轮换、并发与重放检测、family 撤销、过期、Client 绑定、scope 不扩张；public 支持必须有真实实现依据；`offline_access` 有明确同意与期限，不能由 autoConsent 自动无限续期 | 真实存储并发与外部 RP 测试 |
| AC-022 | 注销与撤销 | 当前 App 退出、中央退出、授权撤销及全设备动作按明确合同生效；无效 token 撤销响应不枚举；不能撤销其他 Client 令牌；退出回调白名单/CSRF有效 | 多应用旅程与标准端点测试 |
| AC-023 | 外部 RP 退出传播 | 若纳入：签名 logout token、重放/幂等、有界重试与失败可观察；明确离线/不协作 RP 限制；不承诺删除外部 Cookie | 独立 RP 服务和故障恢复测试 |
| AC-024 | Cookie/CORS/CSRF | HTTPS、host-only、HttpOnly、Secure、SameSite 属性及删除一致；无需第三方 Cookie也完成流程；外部 SPA 仅获必要端点 CORS，非白名单 Origin/登录 CSRF拒绝 | 真实跨 Origin 浏览器矩阵 |
| AC-025 | 无本地登录旁路 | 选定 `authMode=sso` 时后端拒绝未授权本地认证；SSO 入口不因本地验证码故障失效；`both/local` 兼容按配置执行 | mode 组合与故障测试 |
| AC-026 | 登录安全与隐私 | 密码错误、停用/不存在对外不枚举；挑战/失败限流不能旁路；日志无密码、code、verifier、token、secret 或个人资料原文 | 负向测试和日志抽查 |
| AC-027 | 故障与恢复 | Redis/DB/key provider 不可用时失败关闭；签发/消费/业务会话部分失败有明确恢复且不重复授权；刷新后端故障不凭缓存扩大权限 | 故障注入与重启验证 |
| AC-028 | 初始化和升级 | 新库按六份基座可创建；存量预检、唯一性迁移、备份/差异和隔离恢复通过；不删除或合并用户凑过索引 | 隔离数据库建库/升级/恢复记录 |
| AC-029 | 完整交付 | full/core、三个 App、真实发布 Origin/callback/key 配置对应同一源码；旧消费者迁移清单闭合 | 构建 manifest 与发布合同 |
| AC-030 | 外部开发者可独立接入 | 配置交付页和示例包含 issuer/client/认证方法/回调/scopes/退出；标准 RP 无 WTA 内部源码也可完成接入 | 外部接入样例与操作记录 |

## 现有测试与边界

| 可复用入口 | 本轮状态 | 能证明什么 / 不能证明什么 |
|---|---|---|
| <Path>frontend/e2e/sso-three-gates.spec.ts</Path> | 未运行 | 原入口/会话复用/Client 隔离的既有验收结构；不能证明完整 OIDC |
| <Path>frontend/e2e/sso-callback-journey.spec.ts</Path> | 未运行 | 回调失败/过期/重放恢复结构 |
| <Path>frontend/e2e/sso-release-origin.spec.ts</Path> | 未运行 | HTTPS/前缀/Cookie 路由结构 |
| <Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path> | 未运行 | 使用身份与 token 替身；不足以证明 AC-005/006/019 |
| <Path>release-artifacts/tests/fixtures/sso-release-origin.py</Path> | 仅审查 | 明确是部分真实服务加 System fixture，不作为最终全部完成证据 |

## 后续实施门禁

以下是仓内已有真实命令，列出供实现阶段执行；本轮没有运行这些产品门禁。新 OIDC 专项用例和命令应由 S/T 根据最终合同登记，不能将本表当作新增测试已经存在。

| cwd | 命令/入口 | 作用 |
|---|---|---|
| 仓根 | `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs` | 工程事实同步 |
| 仓根 | `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-sso --mode layered` | 分层边界 |
| frontend | `pnpm architecture:check` 与 `pnpm architecture:test` | 前端依赖与 App 组合 |
| frontend | `pnpm --filter @namewta/tooling-openapi openapi:check` | 生成合同与快照漂移 |
| frontend | `pnpm lint`、`pnpm typecheck`、`pnpm test` | 静态和单元门禁 |
| frontend | `pnpm build:dev` 与 `pnpm build:prod` | 三 App 构建 |
| backend | `./mvnw -pl wta-modules/wta-sso -am test`，受影响 System/Admin/API 定向测试及 `./mvnw test` | 后端协议/账户/组合门禁；记录 skip |
| backend / 仓根 | `./mvnw clean package -DskipTests` 后 `bash scripts/ci/verify-admin-bundle.sh full`；core 使用 `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` 后校验 core | 同版双 bundle |
| 仓根 | `bash release-artifacts/scripts/verify-release.sh` | 发布清单/配置合同 |
| 仓根 | <Path>scripts/ci/run-external-services.sh</Path> | 隔离真实依赖；运行前核对资源归属 |
| 仓根 | <Path>scripts/sso-hard-e2e.sh</Path> 的 release-origin 模式 | 现有 SSO 专项回归；仍需新增真实 System 与标准 RP 验收 |

完成出口要求全部适用 AC 有可回读证据、失败已修复、未验证项明确；实际部署与远程写入不由本 G 文档自动授权。
