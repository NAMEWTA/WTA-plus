# Admin/Home 对接独立 SSO

Admin、Home 保留本地登录和原有注册策略，通过「第三方登录」接入标准 OIDC。SSO 即使与业务后端运行在同一个进程里，接入方仍只调用 Discovery、Token、JWKS、UserInfo 和退出 HTTP 接口，不读取 SSO 的数据库或会话缓存。

## 部署与地址

支持一个后端加三个前端，也支持三个 App 分别部署在不同服务器、不同域名，并为各 App 配置独立后端。共享域名时，每个前端使用不同静态前缀，SSO 的协议端点仍位于该域名根路径 `/oidc/*`、`/.well-known/*` 和 `/sso/*`。前缀不能占用 `sso`、`oidc` 等保留路由。

发布环境的 `ADMIN_WEB_ORIGIN`、`HOME_WEB_ORIGIN`、`SSO_WEB_ORIGIN` 可以相同。每个 App 的 `*_WEB_BACKEND_SERVER1/2` 优先于公共 `BACKEND_SERVER1/2`；只有一个后端时，两项填同一地址。构建选择前端与路径，管理页登记实际外部访问地址，两者应一致。

独立业务后端应使用各自的数据库、Redis 实例或 Redis key prefix。仅切换 Redis 逻辑 DB 不能隔离 Pub/Sub 频道；共用 Redis 实例时同时设置不同的 `redisson.key-prefix`，同一业务集群各实例保持一致。

浏览器访问自己的 App/API 入口，业务后端必须能访问 SSO 的 Issuer/Discovery/JWKS，SSO 后端必须能访问业务后端登记的后台退出地址。共享域名不等于共享 App 令牌；Admin/Home 分别保存业务会话。`WEB_CORS_ALLOWED_ORIGINS` 继续支持精确 IP/域名、主机与端口通配，以及 `*`，与本次 OIDC 接入互不替代。

## 首次配置

1. 安装最新 canonical MySQL 基座并启动后端。认证服务初始停用，没有默认 Client Secret、私钥或 Issuer，本地管理员登录仍然可用。
2. 使用 `node scripts/oidc/init-keys.mjs --directory /absolute/new/secret-directory` 生成权限为 `0600` 的 `auth.env`，将其中 `AUTH_CONFIG_ROOT_KEY` 注入后端环境。它是 32 字节 Base64 根密钥，用于加密数据库中的客户端凭据、签名私钥、状态键和短期 RP 状态。连接同一数据库的实例使用同一个根密钥；备份数据库时受控备份根密钥，不要在重启时重新生成。
3. 本地登录 Admin，打开「系统管理 → 认证服务配置」。保持服务停用，填写固定 Issuer、SSO 页面地址、中央服务 Origin、静态路径、Cookie 与期限。生产使用 HTTPS；HTTP 仅在 `local/dev` 开发环境显式允许。
4. 在同一页面生成 `SIGNING` 和 `STATE` 密钥。界面只展示版本与状态，材料不回显。签名密钥轮换保留旧公钥；状态键保留旧版本解密既有授权。
5. 首次结构配置保存后维护重启后端，再启用中央 SSO 与 OIDC。Issuer、SSO 页面位置、Cookie 等结构参数在启动时冻结，页面会显示待重启状态；普通启停、期限、接入配置和应用策略可热更新。
6. 在「单点登录」中分别创建 Admin、Home 两个机密 OIDC 应用，保留 PKCE S256。复制各自生成的 `client_id` 与一次性显示的 Secret。登记下表的精确回调及退出回跳，开放需要的账户资料（Home 自动建号使用手机号）。
7. 打开「外部身份源」，新增 `providerKey`（例如 `company-sso`）、显示名称、图标、协议 `OIDC`、固定 Issuer。
8. 打开「身份接入配置」，分别选择 Admin 和 Home 的**业务 Client ID**，填入步骤 6 对应的 **OIDC Client ID/Secret**。这两个 ID 不是同一字段。选择 scopes `openid profile email phone`；Admin 选择 `BIND_ONLY`，Home 选择 `AUTO_REGISTER`，同时在原客户端管理中按需开启 Home 注册及登录域、默认角色。
9. 保存后，使用接入记录 ID 组成后台退出地址，形如 `{业务API外部地址}/auth/social/backchannel/{registrationId}`。将其填入 SSO 对应应用的后台退出 URI，要求 `sid`。独立后端各自保存 Provider/接入配置，分别登记可达的后台退出地址。

| 登记字段 | Admin 示例 | Home 示例 |
| --- | --- | --- |
| 登录回调 | `https://apps.example.test/console/social-callback` | `https://apps.example.test/home/social-callback` |
| 退出回跳 | `https://apps.example.test/console/logout/callback` | `https://apps.example.test/home/logout/callback` |
| 业务 API 入口 | `https://apps.example.test/console/prod-api` | `https://apps.example.test/home/prod-api` |
| 首次登录策略 | 绑定已有本地账号 | 按注册开关创建本地账号 |

接入配置编辑时 Secret 留空表示保留旧值；版本冲突要求刷新后重试。已有绑定后的身份源标识/Issuer、接入关联的 Provider/Client 身份保持稳定，避免把已有账号关联到另一个身份源。停用、删除的接入保留退出验证所需的墓碑记录。

## 账号与退出行为

- 首次 Admin SSO 登录不会自动获得管理员身份。用户先用原有本地账号登录，在「第三方账号」绑定相应外部身份，再从登录页使用 SSO。
- Home 已绑定身份直接登录；未绑定且允许注册时创建新的本地账号，并按已有登录域/默认角色机制授权。手机号缺失或不符合本地规则时先显示补填表单；邮箱或手机号已属于本地账号时提示登录已有账号后绑定，不自动合并。
- 自动创建的账号使用随机不可预测的本地密码，不是 `123456`；需要本地密码时使用管理员重置或已启用的密码找回流程。
- 外部身份唯一键为精确 `issuer + sub`，不能按用户名、邮箱或手机自动认定同一人。登录后仍检查本地账户状态和当前业务 Client 的准入。
- 「退出当前应用」撤销当前业务令牌，中央会话保留，下次 SSO 可继续复用。「退出全部应用」先结束当前 App 会话，再跳转中央退出确认页，完成后由后台退出通知撤销同一中央 `sid` 关联的其他应用会话；其他设备和本地密码登录会话不受影响。
- 中央撤销事实和待发送任务写入同一事务。通知失败自动退避重试，可在「认证服务配置」查看结果并重试。其他应用在下一次 API 调用时感知失效，浏览器空闲页面不承诺即时刷新。
- RP 按中央 `sid` 串行登记/撤销，并保存短期撤销记录，提前到达的退出通知不会被迟到的登录回调覆盖。

Home 个人中心的企业认证、实名认证继续使用现有档案与审核流程；本次登录方案不改业务审核权限。Admin/Home/SSO 使用现有共享 UI 主题、图标与组件。

## 配置迁移与兼容

旧 JustAuth 前端须随本次版本一次性切换到 `POST /auth/social/authorize`。旧 `GET /auth/binding/{source}` 仅返回更新客户端并重新发起的提示，不再生成授权地址；切换前尚未完成的旧回调也须从当前登录页重新开始。

运行时不再读取 `justauth.*`、`namewta.sso.*`、`namewta.oidc.*` 或旧 `OIDC_*` 业务环境变量。旧 JustAuth 配置可在「身份接入配置」选择 YAML/JSON 文件，预览选中的条目与业务 Client 后显式导入；不会启动时自动写库或覆盖已有配置，密钥不在预览和日志中回显。迁移后的 JustAuth 同样通过新的授权入口携带浏览器事务凭据；旧前端须切换到新入口，缺少 `transactionKey` 的回调要求重新发起，不能降级绕过事务校验。不支持自动导入的供应商专用参数会给出提示，需人工核对。

旧签名 JWK 和状态密钥可通过密钥管理接口导入，再检查公开 JWKS。密钥导入不迁移已存在的中央浏览器会话；切换时安排现有会话退出并重新认证。原 `/sso/oauth2/*` 协议继续保留，新 Admin/Home 登录按钮统一走 OIDC；旧前端 `/sso/callback` 保留已发起第一方授权的处理能力，新入口使用 `/social-callback`。

新版本业务基座共 116 张表；新增 8 表，扩展 `sys_social` 的 `issuer/subject/identity_key`、OIDC 应用后台退出字段与授权关闭事实，并补齐菜单权限。全新数据库使用正式初始化器。已有数据库按 canonical `10-cde-base-ddl.sql` / `50-cde-base-dml.sql` 的审阅差异升级，不重放含重建表的完整文件；升级演练见 [记录](upgrade-verification.md)。不要覆盖既有账号、业务 Client、绑定或自定义菜单。

回退前停用新的入口并完成关联会话退出，恢复应用版本与其兼容的数据库备份及根密钥；保留新增表可避免删除业务数据。涉及数据库恢复和真实环境发布需按项目发布流程单独执行。本次实现不等于已部署。

## 验证

执行记录见 [worklog.md](worklog.md)，三端真实浏览器夹具说明见 [前端验收说明](../../frontend/e2e/README-external-auth-real.md)。协议依据 [OIDC Core](https://openid.net/specs/openid-connect-core-1_0.html)、[RP-Initiated Logout](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) 和 [Back-Channel Logout](https://openid.net/specs/openid-connect-backchannel-1_0.html)。
