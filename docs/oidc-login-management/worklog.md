# OIDC 登录接入统一管理工作记录

## Goal

Admin 统一管理各业务 App 的外部 OIDC 登录；区分自有 Provider 与业务 App 接入外部身份源的职责。复用现有配置、登录、绑定和退出，完善表单配置、协议兼容和按需启动。

## Decisions

- Admin 必选，Home、SSO Web 可选；业务后端共享同一个 MySQL 业务库。参与认证的节点共享 Redis DB/key-prefix、AUTH_CONFIG_ROOT_KEY 和兼容的 Sa-Token 配置。
- 标准机密 OIDC Code + PKCE S256 + RS256，认证方式 basic/post；保留本地认证与 JustAuth 兼容。
- 使用现有 provider/registration 表和 options，不新增表；管理专用跨 App 目录不放宽普通业务 RBAC。
- 不按邮箱/手机号自动合并账户；OIDC 显式绑定兼容 pairwise subject；无 sid 时保留发起 RP 退出的能力。
- 不执行业务环境部署、提交、推送或存量数据库升级。

## Current status

实现、构建、管理界面、自建/第三方登录及同域前缀验收均已完成。任务独占的两个后端进程、两个网关进程及四个容器已清理；未操作已有业务环境。

## Implementation

- System 管理接口限定 Admin Client，并保留操作权限检查；提供跨 App 目录、最小身份源目录、配置/有效入口计数、公开接入参数和实时 Discovery 检测。
- 管理 UI 可搜索身份源/App，配置认证方式、App/API 公开地址及精确回调；保存后显示可复制的接入信息，编辑时密钥留空保留旧值。配置成功与实际登录验证分别表达。
- 接入保留版本冲突处理、共享缓存失效和身份字段稳定约束。保存仅做静态校验，不在数据库写事务中请求远端。
- 修复 UserInfo 标准资料合并、pairwise 显式绑定、无 sid 的加密 RP 退出状态；退出文案准确表达其他应用依赖有效后台通知。
- Client 列表展示接入数量并跳转对应过滤页；使用现有动态路由的规范路径，避免菜单名称附加 ID 后无法导航。沿用现有 UI 组件、主题和图标。
- 发布库存与运行集合分离：NAMEWTA_ENABLED_APPS 选择启用 App，Admin 必选。禁用 App 的 Compose 服务、Nginx upstream/路由和 TLS 依赖从生成产物移除；旧三端 manifest 保持兼容。
- canonical DML 仅调整相关菜单名称；配置、升级和部署说明见 [接入指南](../oidc-app-integration/README.md)。

## Verification

以下命令均退出 0。临时日志位于当前工作环境 /tmp，不是仓库长期交付文件。

| 范围 | 工作目录与命令 | 结果 |
| --- | --- | --- |
| 后端默认测试 | backend：`./mvnw test` | 1493 项：1200 执行通过，293 项按环境条件跳过，0 失败/错误 |
| 协议及真实存储 | backend：下方定向 Maven 命令 | 44 项全部执行通过，0 跳过；真实 MySQL/Redis |
| full 打包 | backend：`./mvnw clean package -DskipTests`；根目录 `bash scripts/ci/verify-admin-bundle.sh full` | 通过 |
| core 打包 | backend：`./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`；根目录 `bash scripts/ci/verify-admin-bundle.sh core` | 通过 |
| 前端质量 | frontend：`pnpm lint`、`pnpm typecheck`、`pnpm test` | 741 项、121 测试文件通过 |
| 前端合同 | frontend：`pnpm architecture:check`、`pnpm --filter @namewta/tooling-openapi openapi:check` | 通过 |
| 前端构建 | frontend：`pnpm build:prod` | 三个 App 均通过 |
| 导航修复后增量 | frontend：web-domain-system 的 lint/typecheck/test，以及 `pnpm --filter @namewta/admin-web build:prod` | 90 项通过，Admin 最终构建通过 |
| 同域前缀构建 | Admin/Home 目录使用 Vite production，分别设置 /console/、/home/ 的 context 与 API base | 两端通过；Admin 导航修复后重新构建 |
| 发布合同 | 根目录：`bash release-artifacts/scripts/verify-release.sh` | 145 项通过 |
| 真实 Nginx | 根目录：`node --test release-artifacts/tests/enabled-apps-nginx.e2e.mjs` | 四种 App 集合 × HTTP/TLS，共 8 项通过 |
| 工程规范 | skill facts、skill 校验、agent handbooks、OIDC layered 模块合同及 docs/fm 校验 | 通过 |
| 差异检查 | 根目录：`git diff --check` | 通过 |

定向真实服务命令使用任务独占的两个 MySQL 库和 Redis，不连接已有业务环境：

```bash
./mvnw -pl wta-admin -am test \
  -Dtest=OidcProtocolClientTest,ExternalAuthSessionStoreTest,ExternalAuthAccountTransactionServiceTest,ExternalAuthSessionRedisIntegrationTest,ExternalAuthAccountMySqlIntegrationTest,ExternalAuthConfigurationMySqlIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dexternal.auth.mysql.integration=true \
  '-Dexternal.auth.mysql.url=jdbc:mysql://127.0.0.1:45406/namewta_external_auth_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  -Dexternal.account.mysql.integration=true \
  '-Dexternal.account.mysql.url=jdbc:mysql://127.0.0.1:45406/namewta_external_account_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  -Dexternal.session.redis.integration=true \
  -Dexternal.session.redis.port=45479
```

主要日志：`/tmp/oidc-login-management-backend-final-test.log`、`/tmp/oidc-login-management-protocol-real-tests.log`、`/tmp/oidc-login-management-backend-final-{full,core}-package.log`、`/tmp/oidc-login-management-frontend-final-{lint,typecheck,test,build}.log`、`/tmp/oidc-login-management-admin-navigation-build.log`、`/tmp/namewta-enabled-apps-{full-release,nginx}-tests.log`。

## Real browser acceptance

最终前端构建与 full Jar 运行在任务独占 loopback 环境：MySQL 45406、测试 Redis 45479、浏览器 Redis 45481、官方 Keycloak 26.4.7/45480，业务节点 A/B 分别 18888/18889，共享同一业务库、Redis 与根密钥。

真实网关将授权请求送至 A，登录回调、注册及业务令牌签发送至 B；提供方后台退出通知直接送至 A。两套完整验收都证明 A 收到通知后能撤销 B 签发的两端令牌。没有 mock OIDC 响应。

私有证据根目录：`/tmp/wta-oidc-login-management-owned-zsf0mvl6`。夹具和凭据权限受限，不提交仓库。

浏览器验收共 31 组通过；跨节点配置版本失效另行验证通过。临时环境清理记录为同目录 `cleanup-result.json`，非敏感结果与截图保留用于本次审查。

| 验收 | 结果与证据 |
| --- | --- |
| Admin 真实管理 UI | 8 组通过，management-ui/management-result.json；桌面/移动截图包含 Discovery、接入参数、Client 计数和导航。首次运行实际创建身份源/Admin 接入；修复导航后恢复已有记录，并新建 Home 接入后完成全套验收 |
| 自建 Provider | 9/9，evidence-self-provider/shared-result.json，6 张截图 |
| 外部 Keycloak | 9/9，keycloak-login-ui/keycloak-shared-store-cross-node-result.json，6 张截图；Admin 使用 basic，Home 使用 post |
| 跨节点配置失效 | cross-node-config-result.json：A 发起授权后修改配置，B 在换码前拒绝旧事务；随后恢复原业务字段，已有密钥保持 |
| 同域不同前缀 | 5/5，keycloak-prefix-ui/keycloak-same-origin-prefix-result.json，4 张截图；Admin /console、Home /home，验证已有绑定、本地/OIDC 登录、中央会话复用、当前应用退出和后台联动退出 |

完整登录套件覆盖：本地登录保留、显式绑定、Admin OIDC 登录、Home 中央会话复用、当前 App 退出、中央退出与后台联动、缺手机号补填、联系信息冲突引导绑定、Admin 禁止未绑定自动建号、伪造事务拒绝。管理与登录命令及夹具合同见 [浏览器验收说明](../../frontend/e2e/README-external-auth-real.md)。

## Limits

- 默认测试的 293 项条件跳过不记为通过；本轮相关 MySQL/Redis 用例已另外真实执行。
- 已验证自建 Provider 与 Keycloak 的受支持 OIDC 流程，不宣称所有提供方、SAML/CAS 或任意算法/认证方式均兼容。
- 发布合同测试使用隔离编译夹具；未从干净提交执行真实发布构建，也未部署到业务服务器。
- 正式环境仍须按实际域名配置精确回调和后台退出地址，并按接入指南保持共享存储及认证参数一致。
