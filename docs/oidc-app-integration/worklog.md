# Admin/Home 外部 OIDC 接入

## Goal

将 Admin/Home 作为标准第三方 OIDC 客户端接入现有 SSO；保留本地注册登录。全部 SSO/OIDC/第三方认证业务配置由 MySQL 页面管理、Redis 缓存。支持一个后端三个前端及独立后端部署，完成关联账号、Home 自动建号与两种退出。

## Decisions

- Admin/Home 分别登记 OIDC 应用；业务 Client 与 OIDC Client ID 分开。
- 复用 social 登录编排与业务 Sa-Token；协议 Token 不进入前端业务传输。
- Admin 仅绑定已有用户。Home 按注册开关自动创建用户，缺手机号先补填，冲突要求绑定已有账号。
- 配置按 Provider 与业务 Client 分层；MySQL 权威，提交后刷新 Redis，保留版本与缓存失败恢复。
- Secret/私钥/状态键加密持久化，仅根密钥与基础设施引导配置保留在环境。
- Issuer/Cookie 等结构配置待维护重启激活，日常配置热读；首次未配 SSO 不影响本地管理员登录。
- 全局退出限定当前中央 sid；持久撤销和退出通知解决跨业务后端及并发登录。
- SQL 仅修改 canonical 10/50 基座；现有库不重放完整基座。

## Current status

实现与验收完成。初始工作树干净。前后端质量门禁、最终产物在共享/独立后端的真实三端 OIDC 流程、旧 SSO 兼容、数据库升级和 HTTP/TLS 发布代理验证均已通过。任务自有进程、3 个数据库/Redis 容器及对应端口已清理；未执行真实环境部署、提交或推送。

## Ownership

- root：OIDC RP 协议适配、Auth 登录/建号/绑定/业务会话、发布配置、集成、文档与验证。
- cors_password_audit：System 外部认证配置、加密组件、公开配置 API、配置 SQL/测试。
- workflow_audit：SSO/OIDC 服务配置、密钥、中央会话、退出通知、所属 SQL/测试。
- profile_ui_audit：前端管理页面、登录/回调/资料补填/绑定/退出、前端测试。

## Remaining work

无本次实现与验收待办。真实环境安装/升级、认证配置和启用按 README 执行，未在本次自动发布。

## Verification

- 初始 `git status --short`：退出 0，无改动。
- `backend/`：`./mvnw -pl wta-admin -am compile -DskipTests`：退出 0。
- `backend/`：RP 定向测试与 full package：退出 0，日志 `/tmp/oidc-rp-package.log`。此前三条测试因 Mockito 对未配置 Long 返回 0 而走错夹具分支，显式设置未绑定 null 后通过。
- System 外部配置：30 单元 + 8 真实 MySQL/Redis 集成全部通过；日志 `/tmp/external-auth-integration-tests.log`。
- 外部建号并发：6 真实 MySQL/Redis 测试通过；验证手机号/邮箱等价冲突、提交持锁、唯一身份回滚、失败解锁和绑定/解绑所有权，日志 `/tmp/external-account-integration-tests.log`。
- Provider/SSO：OIDC 23 单元与 SSO 23 测试通过；6 原 OIDC 持久化场景 + 2 新中央退出事务场景在真实 MySQL 显式启用通过（无跳过），日志 `/tmp/oidc-real-tests.log`。
- `frontend/`：受影响 9 个包/App 定向类型/测试、lint/architecture 和 `pnpm build:prod` 退出 0；全工作区结果见下文。
- 发布 Node 测试首轮 36 通过；全量仅旧 LB 代理数量断言因新增三条 SSO 路由失败，更新精确数量并保持各路由转发头断言后专项通过。最终全量结果见下文。
- 首次实际启动遇到无密码 Redis 的 AUTH 配置问题；另建带密码的任务自有 Redis 后启动成功，验证本地登录、MySQL 密钥生成 API 可用。该问题未通过修改真实业务配置规避。
- 运行资源仅任务自有数据库、Redis、端口；未执行生产部署、提交或推送。

- `backend/`：完整 `./mvnw -B -ntp -pl wta-admin -am test` 退出 0；1397 总数，1121 执行通过，276 因外部 fixture/显式集成开关默认跳过，无失败/错误。相关真实数据库场景已另行显式开启验证；日志 `/tmp/oidc-backend-full-tests.log`。
- `backend/`：core/full package 与 `verify-admin-bundle.sh` 两种组成检查退出 0。首次不 clean 切换 profile 触发 Maven Jar 增量复用，由组成检查发现后使用 `-Dmaven.jar.forceCreation=true` 重建；最终 full 产物正确，日志 `/tmp/oidc-backend-{core,full}-package.log`。
- `frontend/`：根 `pnpm typecheck`、`pnpm test`、`pnpm lint` 全工作区退出 0，日志 `/tmp/external-auth-workspace-{typecheck,test,lint}.log`。
- 发布 Node 全量：133 项通过、0 跳过，日志 `/tmp/oidc-all-release-tests-final.log`；Skill facts 校验退出 0。
- 真实 Nginx HTTP/TLS：两种模板 `nginx -t` 与 40 次 curl 通过（同域三个前端、API路径、OIDC错误/重定向状态、三个独立上游及转发头），日志 `/tmp/oidc-nginx-transport.log`。
- 旧库升级：108→116 表，8 表/7 列/唯一索引/13 菜单，581 条旧记录逐值保留，与 fresh 结构一致；详见 `upgrade-verification.md`。
- 独立 Home 后端实际启动时验证：Redis Pub/Sub 不随逻辑 DB 隔离，需要独立 `redisson.key-prefix` 或独立 Redis 实例；调整任务自有基础设施配置后启动正常，已写入部署说明。

- 新标准 OIDC 真实浏览器：共享后端与独立 Home 后端两轮各 9 项全部通过。独立 Home 使用独立 MySQL、Redis DB/前缀、根密钥，中央服务仍只有一个；同时覆盖 Basic/Post 两种客户端认证。证据 `/tmp/namewta-external-auth-real/evidence-{shared,independent}/*-result.json`。
- 真实页面覆盖 Admin 本地登录→显式绑定→SSO、Home 免二次密码、当前退出、全局退出（两端旧业务令牌拒绝、中央需重新密码）、缺手机补填、手机号冲突、Admin 禁建号、伪回调先于换票拒绝；无协议/身份/菜单 mock。
- 公共 UI Playwright：99 项通过，覆盖明暗主题、320/768/1440、200%缩放重排、对比度与键盘/错误提示；日志 `/tmp/external-auth-a11y-final.log`。
- RP 实际 Redis 并发追加 4 项通过，0 跳过，确认退出先到、持锁签发和撤销、sid/client/issuer隔离、重复通知；日志 `/tmp/external-session-redis-tests.log`。
- 联调第一轮旧固定 Jar 缺最新 `findForLogout` XML 导致退出失败；改用已通过组成验证的最终 full Jar 后，两轮真实全局退出均通过。
- 最终独立审查追加收口：退出接口基础设施失败返回标准 HTTP 503 以支持重试；JustAuth 不得删除事务凭据降级；旧解绑入口统一走账号锁，登录在同一锁内重查绑定。对应回归均通过，具体结果见下文。

- 最终账号边界回归：24 项通过、0 跳过，包含 9 项真实 MySQL/Redis 测试；覆盖缺事务凭据拒绝、旧解绑委派、登录与解绑竞争及失败解锁。日志 `/tmp/external-auth-boundary-fix-tests.log`。
- T06 旧协议及退出 HTTP 合同定向：17 项通过、0 跳过，包含 2 个实际 Chrome HTTP/HTTPS 场景，以及 JWKS/Redis 故障返回 503、无效退出返回 400 的断言。日志 `/tmp/oidc-old-sso-security-tests.log`。
- T08 旧发布协议兼容：5 个 Chrome 场景全部通过，Nginx、MySQL、Redis 真实，身份校验与业务 Token 签发使用测试替身；它补充第一方协议兼容验证，不替代上面的完整 OIDC 端到端验收。8 个专属容器与网络已清理，证据 `/tmp/oidc-old-sso-release-20261001-pass.json`。
- Home 菜单最终修复：按 Admin 的单叶子菜单展示规则处理后端 Layout 壳，并规范相对路由路径。Home typecheck/lint/test/build 退出 0（20 项测试），共享/独立后端真实侧栏和待办点击验证通过，桌面/移动截图 `/tmp/namewta-external-auth-real/menu-evidence/`；未改菜单权限。

- 最终带测试的 `package` 首轮发现两项原测试合同问题，未视为通过：旧 `@SaIgnore` Controller 解绑将鉴权下移后，其隔离 fixture 的 service mock 绕过了匿名检查；`SystemUserAvatarOssOwnerUnitTest` 的默认 Converter 只扫描 class 目录，在 package 阶段依赖转为 Jar 时找不到真实生成 Mapper。分别恢复 Controller 显式登录检查并校正测试 Mapper 注册/全局状态隔离，不修改原断言或生产转换工具；修复后 HTTP/解绑/后台退出 4 项与头像 Jar 场景 1 项均通过，日志 `/tmp/oidc-crud-unbind-fix-tests.log`、`/tmp/oidc-avatar-package-fix.log`；完整 package 重跑已通过，结果见下文。

- 最终发布 Node 回归：`node --test release-artifacts/tests/*.test.mjs scripts/oidc/init-keys.test.mjs`，退出 0，134 项通过、0 跳过，日志 `/tmp/oidc-release-final-pass.log`。`node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs` 退出 0，5 个 Skill / 76 个引用校验通过。

- 最终完整后端：在 `backend/` 执行 `./mvnw -B -ntp -pl wta-admin -am package -Dmaven.jar.forceCreation=true`，退出 0；1414 项总数，1131 执行通过、283 默认外部 fixture 跳过，0 失败/错误。日志 `/tmp/oidc-backend-final-package-pass.log`。随后根目录 `scripts/ci/verify-admin-bundle.sh full` 退出 0；最后产物已复制到任务运行目录用于浏览器复验，避免构建过程覆写正在运行的 Jar。

- 最终 Jar 三端复验：共享后端 9/9、独立 Home 后端 9/9，均退出 0；使用新增等价中央账号，在两个业务库中实际完成首次手机号补填，既有 Admin 绑定保持有效。证据 `/tmp/namewta-external-auth-real/evidence-final-{shared,independent}/*-result.json` 与对应截图。
- 清理完成：2 个 Java 后端与前端网关停止，3 个任务自有 MySQL/Redis 容器删除；18888/18889/19441/19442/19443/45406/45479/45480 均确认关闭。验收报告、日志与截图保留，最终产物摘要见 `/tmp/namewta-external-auth-real/final-verification.json`。
- 最终 `git diff --check` 与 Skill facts 校验退出 0。默认跳过的其他模块外部 fixture 未声称已执行；本次相关真实配置、账户并发、中央退出、RP Redis、SSO/OIDC 浏览器及发布代理场景已按上文单独启用验证。
