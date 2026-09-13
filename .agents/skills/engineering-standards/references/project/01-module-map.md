# 模块地图

## 顶层 Scope

| ID | Path | Language / framework / runtime | Build | Source / test roots | Public entrypoint | Quality gates | Evidence |
|---|---|---|---|---|---|---|---|
| `workspace-parent` | `.` | Markdown、Git 治理 | Git, GitHub Actions | `docs/**`, `scripts/ci/**` | `README.md` | frontend/backend/external-services jobs | `README.md`, `.github/workflows/quality-gates.yml`; high |
| `plus-ui` | `frontend` | TypeScript、Vue 3、Pinia、Browser，可扩展多 App monorepo | pnpm workspace、Vite、Oxlint、Vitest、Playwright | `apps/admin-web/src`、`packages/**/src`、`tooling/**/src`、相邻 `*.test.ts`、`e2e/**` | `apps/admin-web/src/main.ts` | architecture check/test、lint、typecheck、workspace test、双模式 build、按风险 E2E | `package.json`、`pnpm-workspace.yaml`、`tooling/architecture/**`、`playwright.config.ts`; high |
| `backend-root` | `backend` | Java 21, Spring Boot 4, JVM | Maven Wrapper | 46 Maven projects below; 176 tracked Java test source files | `wta-admin` and three extension applications | default test; bundle-full + bundle-core package | root `pom.xml`, `wta-admin/pom.xml`; high |

## 后端 Maven 模块

下表每一行都是唯一 `module:<path>` scope。未另行标注时，语言为 Java、运行时为 JVM、构建为 Maven，源码根为 `src/main/java`（存在资源时同时含 `src/main/resources`），测试根为空，generated path 为空，证据为该路径 `pom.xml` 与源码，置信度 high。

| ID / path suffix under `backend/` | Role / framework | Public entrypoint | Test / generated notes |
|---|---|---|---|
| `wta-admin` | Spring Boot 可部署主应用，组装 api/common/modules | `org.namewta.DromaraApplication` | `src/test/java`; JUnit 示例/基础测试 |
| `wta-api` | 跨业务模块公开 API/DTO 合同 | `org.namewta.system.api.*` | none |
| `wta-common` | common 聚合 POM | none | no source root |
| `wta-common/wta-common-bom` | common 版本/BOM 合同 | none | no source root |
| `wta-common/wta-common-ai` | AI 公共配置/适配 | package surface | none |
| `wta-common/wta-common-core` | Spring 核心类型、配置、通用合同 | package surface | none |
| `wta-common/wta-common-doc` | SpringDoc/Javadoc 适配 | package surface | none |
| `wta-common/wta-common-elasticsearch` | Elasticsearch 适配 | package surface | none |
| `wta-common/wta-common-encrypt` | Spring/MyBatis 加解密适配 | package surface | none |
| `wta-common/wta-common-excel` | Excel 公共 API | package surface | none |
| `wta-common/wta-common-job` | Spring 调度集成 | package surface | none |
| `wta-common/wta-common-json` | Spring/Jackson JSON 合同 | package surface | none |
| `wta-common/wta-common-liteflow` | Spring LiteFlow 集成 | package surface | none |
| `wta-common/wta-common-log` | 日志/审计切面 | package surface | none |
| `wta-common/wta-common-notify` | 渠道无关通知契约、分发、幂等和附件快照 | `NotifyDispatcher`, `NotifyClient`, adapter SPI | tests through consumers/admin |
| `wta-common/wta-common-mail` | 邮件适配 | package surface | none |
| `wta-common/wta-common-mcp` | MCP 集成 | package surface | none |
| `wta-common/wta-common-mqtt` | Spring MQTT 集成 | package surface | none |
| `wta-common/wta-common-mybatis` | Spring/MyBatis 数据访问基础 | package surface | none |
| `wta-common/wta-common-nacos` | Nacos 配置解密与启动集成 | package surface | none |
| `wta-common/wta-common-openapi` | 默认开启、可由 `OPENAPI_ENABLED=false` 显式关闭的机器调用协议、注册表、网关与 Sa-Token Session 桥 | `@OpenApi`, OpenAPI SPI, auto-configuration | `src/test/java`; protocol/registry/gateway/assembly tests |
| `wta-common/wta-common-oss` | 对象存储适配 | package surface | none |
| `wta-common/wta-common-push` | Spring 推送/WebSocket/SSE 基础 | package surface | none |
| `wta-common/wta-common-redis` | Spring/Redis 缓存、锁与限流 | package surface | none |
| `wta-common/wta-common-satoken` | Spring/Sa-Token 认证基础 | package surface | none |
| `wta-common/wta-common-security` | 安全注解/权限合同 | package surface | none |
| `wta-common/wta-common-sensitive` | 敏感数据处理 | package surface | none |
| `wta-common/wta-common-sms` | Spring SMS 集成 | package surface | none |
| `wta-common/wta-common-social` | 社交登录适配 | package surface | none |
| `wta-common/wta-common-translation` | 翻译/字典适配 | package surface | none |
| `wta-common/wta-common-web` | Spring MVC、错误映射、Actuator 基础 | package surface | none |
| `wta-extend` | 独立应用聚合 POM | none | no source root |
| `wta-extend/wta-monitor-admin` | Spring Boot Monitor 可部署应用 | `MonitorAdminApplication` | none |
| `wta-extend/wta-snailai-server` | Spring Boot SnailAI 可部署应用 | `SnailAiServerApplication` | none |
| `wta-extend/wta-snailjob-server` | Spring Boot SnailJob 可部署应用 | `SnailJobServerApplication` | none |
| `wta-modules` | 业务模块聚合 POM | none | no source root |
| `wta-modules/wta-ai` | AI 业务能力 | package surface | none |
| `wta-modules/wta-demo` | 示例/集成演示能力 | package surface | none |
| `wta-modules/wta-job` | 业务任务执行器 | package surface | none |
| `wta-modules/wta-notify` | 通知公告、收件箱、渠道投递编排和通知配置 | `controller/admin`、`controller/anonymous`、`usecase` | `src/test/java`; layered module; `validate-module-mode` required |
| `wta-modules/wta-profile` | 账号资料业务聚合 POM | none | no source root |
| `wta-modules/wta-profile/wta-profile-bom` | profile 子模块版本/BOM 合同 | none | no source root |
| `wta-modules/wta-profile/wta-profile-person` | 个人资料与认证能力 | controller/service/mapper contracts | `src/test/java` |
| `wta-modules/wta-profile/wta-profile-enterprise` | 企业资料与认证能力 | controller/service/mapper contracts | `src/test/java` |
| `wta-modules/wta-system` | 用户、Client、角色、菜单、权限等核心系统能力 | controller/service/mapper contracts | none |
| `wta-modules/wta-workflow` | WarmFlow 工作流能力 | controller/service contracts | none |
| `wta-modules/wta-third` | 第三方 HTTP Provider/Endpoint 管理、凭据安全、RestClient Gateway、显式适配器 SPI、限流与出站观测 | `org.namewta.third.api.ThirdPartyGateway`；管理 API 位于 `org.namewta.third.controller.admin` | `src/test/java`; crypto/path security tests; external MySQL/Redis/HTTP/browser gates are recorded by the change release evidence |
| `wta-modules/wta-sso` | 第一方 SSO Authorization Code + PKCE、SSO 域会话 | `controller/anonymous` authorize/token/revoke/login | `src/test/java`; layered module; `validate-module-mode` required |

## 依赖方向

- 本仓 `backend/` 与 `frontend/` 合入；前后端源码不能通过隐式路径形成错误的构建依赖。
- 前端依赖后端 HTTP/JSON 合同，不深耦合 Java 类型或数据库 schema；OpenAPI 生成 transport 位于 `packages/api-contracts`，各 `packages/domains/*` 在边界处映射为领域自有模型。
- 前端方向为 App -> web-domain -> domain -> platform，以及 App -> adapter/web-kit；App 显式组合所需能力，App 之间不得互相依赖，禁止包深层导入和跨工作区相对导入。
- 后端 Maven 方向为：聚合/可部署应用 -> `wta-modules`/`wta-api`/`wta-common-*`；业务模块可依赖 `wta-api` 和所需 common 能力，common 不反向依赖业务模块。
- `wta-api` 是跨业务模块合同面；`wta-common-*` 只承载可复用基础能力，禁止成为绕过业务边界的容器。
- `wta-admin` 负责组装，不承载可复用领域实现；默认 `bundle-full` 接入 job/ai/demo/workflow/profile，显式 `bundle-core` 保留平台基础依赖及 profile person/enterprise，排除 job/ai/demo/workflow。运行时代码生成器不在 Maven 模块图或任一 bundle 中。
- 认证/权限/菜单跨层契约以 `AGENTS.md`、本 Skill 安全/评审规则，以及 `namewta-fullstack-development` 的 permission-routing、contract-mapping 与 backend/architecture 为准。

## 实现基线与成熟样例

| Scope | Baseline | Mature implementation evidence | Usage |
|---|---|---|---|
| 前端领域纵切片 | 后端 controller/BO/VO/OpenAPI 合同 | `packages/domains/demo/**`、`packages/web-domains/demo/**`、Admin 显式组合 | transport -> domain mapper/model/service -> web-domain 页面/manifest -> App 选择 |
| 前端树表 CRUD | 后端业务合同，无本地根级模板 | `packages/domains/demo/**`、`packages/web-domains/demo/**` | 保持非分页列表、树转换、父节点选择和展开状态语义，并由领域测试与页面测试分别验证 |
| 前端复用状态 | 无单一模板替代 | 各 `web-domain` 的局部 composable、稳定 `web-kit`、App 私有 hooks | 状态先留在真实 owner；只有多消费者形成稳定合同时才提取 |
| 标准 CRUD 模板 | `docs/fm/{java,vue,react,xml,sql}/**` | classic 后端 `wta-modules/wta-demo/**/TestDemo*`；layered 后端 `wta-profile`；前端 demo domain/web-domain | 先按[后端模块模式登记表](03-backend-module-modes.md)选模式，再对照资源纵切片、entity/BO/VO/mapper/service/controller 或 UseCase/Service/DAO/Mapper 全链路及数据权限覆盖 |
| 后端复杂系统能力 | 静态模板只提供 CRUD 起点 | `wta-modules/wta-system/**`、`wta-modules/wta-workflow/**` | 关系表、缓存、Client、事务、条件装配和工作流逻辑按同模块成熟实现增量修改 |
| 数据访问与翻译 | 公共 API 本身 | `wta-common-mybatis`、`wta-common-translation` | 复用 `BaseMapperPlus`、`QueryBuilder`、fresh chain wrapper 与批量翻译合同 |

## 路由

- `path:frontend/**` -> 通用相关规则 + TypeScript core + 代码组织/命名/注释 + Vue + Browser。
- `path:frontend/packages/domains/**`、`packages/web-domains/**`、`packages/api-contracts/**` 或领域 CRUD 页面 -> 追加前端 CRUD/API 实现规范。
- `path:backend/**` -> 通用相关规则 + Java core；Spring 应用/配置/Web scope 再加 Spring Boot；事务、数据源切换和 DDL/schema scope 追加数据源事务与建表规范。
- `path:backend/wta-modules/**` 中的 CRUD/mapper/service/controller，以及 `wta-common-mybatis`、`wta-common-translation` -> 追加后端 CRUD/查询实现规范；模板修改单独路由到 `path:docs/fm/**`。
- `path:docs/fm/**` -> 静态 CRUD 模板规范。
- SQL/表结构变化 -> 安全数据 + Java/Spring contract + 数据源事务与建表；新建项目自有表应用基础字段基线，直接修改本仓六份 MySQL 8.4 完整基座中的对应文件。
- 跨前后端 API 变化 -> 同时加载 TypeScript、Java、测试、安全和交付规则，并以后端兼容合同先行。
