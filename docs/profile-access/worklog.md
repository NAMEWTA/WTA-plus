# Profile 公共访问与认证注解

## Goal

在现有 Profile API 上增加当前账户访问、薄静态入口、个人/企业认证注解；兼容既有查询语义并完成真实数据库、代理、完整后端与双 bundle 验证。

## Decisions

- 当前账户适配归 `wta-common-satoken`，只依赖已有 `wta-api`，不新增模块/表/接口/页面。
- `ProfileAccess` 为唯一核心；静态工具和注解共用它。要求来自可信普通账户，校验 Token 与 LoginUser 的主体、登录域和 Client。
- 两种注解累加 AND；超管不豁免，错误不伪装未认证；代理限制公开说明。
- 保留有效绑定、单企业负责人、明确字段和证件期限原语义，不增加缓存。
- 公共查询组合器中立装配，旧配置保留导入桥和工厂签名。

## Current status

已完成实现、装配审查、真实 MySQL、最终完整回归和双 bundle 验证。初始工作树干净；变更保留在工作树，未提交或发布。最终产物保留 full 构建：`backend/wta-admin/target/wta-admin.jar`。

## Ownership

- root：自动配置、兼容迁移、文档事实、环境、整体审查与验证。
- cors_password_audit：ProfileAccess、ProfileHelper、访问边界测试。
- workflow_audit：注解、Advisor、真实 Spring 代理与 HTTP 合同测试。
- profile_ui_audit：任务自有 MySQL 的完整档案查询链路与状态/字段/批量测试。

## Files changed

- `backend/wta-common/wta-common-satoken`：访问组件、静态入口、两种注解、Advisor、两项自动配置及注册清单；仅新增测试依赖和对应单元/容器测试。
- `backend/wta-modules/wta-profile/wta-profile-person/.../config/ProfileApiConfiguration.java`：兼容桥延迟导入中立查询配置，保留工厂签名；增加替换/并存测试。
- `backend/wta-admin/src/test/java/org/namewta/test/profile/`：真实 HTTP 边界与独占 MySQL 查询链测试。
- `docs/profile-access/` 与相关 Skill 事实：调用方式、行为边界、兼容和验证证据。没有改动前端、业务表、公开 API/DTO 或既有接口门禁。

## Remaining work

无。本任务自有测试表、容器、端口和临时凭据均已清理，日志与 XML 测试证据保留。

## Verification

- 初始 `git status --short` 退出 0，工作树干净。
- MySQL 8.4.9 使用本任务独占容器、loopback 45416 端口、独占空 schema `namewta_profile_access_test_20261001`；密码仅通过环境变量传递。测试结束已确认库中表数为 0，移除独占容器并确认端口关闭；不访问已有业务库。
- 定向测试含真实 MySQL：退出 0，80 项中 78 项通过、2 项既有条件用例跳过；本次 MySQL 类 10 项全部执行通过。`verify-external-tests.mjs` 对保存的本轮报告核验为 1 类、10 项、零跳过。
- 装配顺序回归：新增裸容器中普通 `@Configuration/@Bean` 替换测试，修复前两项稳定失败；兼容桥使用 `@ImportAutoConfiguration` 后 14 项配置测试全部通过。
- 首轮默认完整后端测试退出 0；兼容桥修复后最终默认测试也退出 0，316 个报告类、1,473 项，其中 1,182 项通过、291 项条件跳过、零失败/错误。原始 XML 已保存至 `final-reports/`，计数与本轮起始时间见 `final-test-counts.json`。
- 两个 Profile 模块 layered 校验均退出 0（person 186 / enterprise 147 个 Java 文件）；Skill 事实校验退出 0（5 个 Skill、76 篇引用）；已跟踪及新增文件的空白检查通过。

本轮日志在 `/tmp/wta-profile-access-20261001/`：

| 命令（Maven cwd 为 `backend/`，Node/Git cwd 为仓根） | 退出码 | 证据 |
|---|---:|---|
| `./mvnw -B -ntp -pl wta-admin -am test -Dtest=ProfileAccessTest,ProfileHelperTest,ProfileVerificationAdvisorTest,ProfileVerificationHttpBoundaryTest,ProfileQueryAutoConfigurationTest,ProfileApiConfigurationTest,ProfileApiContractTest,PersonDisclosureTest,EnterpriseDisclosureTest,ProfileAccessMySqlIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dprofile.access.mysql.integration=true -Dprofile.access.mysql.url=<独占库 JDBC URL> -Dprofile.access.mysql.username=root` | 0 | `targeted-mysql.log`、`targeted-reports/`；密码来自 `PROFILE_ACCESS_MYSQL_PASSWORD` |
| `./mvnw -B -ntp -pl wta-modules/wta-profile/wta-profile-person -am test -Dtest=ProfileApiConfigurationTest,ProfileQueryAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false` | 0 | `config-order-fixed.log`；14 项零跳过 |
| `./mvnw -B -ntp test`（修复前首次完整回归） | 0 | `backend-test.log` |
| `./mvnw -B -ntp test`（最终候选） | 0 | `backend-test-final.log`；1,182 通过、291 跳过 |
| `./mvnw -B -ntp clean package -Pbundle-core -Dmaven.test.skip=true` | 0 | `bundle-core.log`；复用已保存的独立测试证据 |
| `bash scripts/ci/verify-admin-bundle.sh core` | 0 | core 必需模块齐全，可选模块未打入 |
| `./mvnw -B -ntp clean package -DskipTests` | 0 | `bundle-full.log`；复用已保存的独立测试证据 |
| `bash scripts/ci/verify-admin-bundle.sh full` | 0 | full 模块组成通过；另核对内嵌 common-satoken Jar 包含 7 个新增类型及两项自动配置注册 |
| `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-profile/wta-profile-person --mode layered` | 0 | 分层通过 |
| 上一命令模块路径替换为 `wta-profile-enterprise` | 0 | 分层通过 |
| `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs` | 0 | 事实及链接通过 |
| `git diff --check`，另逐一检查新增文件 | 0 | 无空白错误 |

首轮失败均已定位和修复，未删除断言或放宽规则：`ProfileHelperTest` 改为 mock 静态方法实际声明类；HTTP 成功夹具改用 `R.data` 数据重载；兼容桥使用延迟导入解决自定义 Bean 注册顺序。对应失败证据为 `targeted-tests.log`、`targeted-pass.log`、`config-order-reproduction.log`。

## Verification boundaries

- 真实 MySQL 测试使用生产 Mapper XML、DAO、Service、UseCase、贡献者和组合器，以及真实 SaToken 登录上下文、Spring Advisor 代理；覆盖有效/无绑定、草稿共存、暂停/解绑/撤销、转移后归属、字段白名单、100 用户固定两次 SELECT、同 Token 下一次读取生效、跨 Client 一致性和无效上下文零查询。
- 状态与转移结果由 SQL 夹具构造，未声称执行完整认证 Workflow 或转移命令端到端流程。
- 装配测试验证零/单/双贡献者、自定义替换、旧入口和自动配置并存及真实 Spring 代理。完整业务模块仍保留既有其他服务依赖。
- 默认测试中的外部服务条件跳过不算通过；本次独占 MySQL 证据单独保存。未修改前端，不需要前端构建；未启动完整部署栈、运行远程 CI、提交或发布。
