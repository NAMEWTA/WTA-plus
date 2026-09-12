# 项目画像

## 身份

- Project: `wta-vue-plus-docs`
- Repository root: 当前包含本 Skill 的 Git 工作区根目录
- Topology: 多根、多语言聚合仓库；父仓库通过 Git Submodule 管理两个独立产品仓库

## 事实来源

- `README.md`、`.gitmodules`：父仓库只聚合 `plus-ui-namewta` 与 `wta-vue-plus-namewta`，两个子模块独立开发和发布。
- `.agents/skills/**`、`AGENTS.md`：项目开发 Skill 只在父工作区集中维护；两个产品子仓库不保留 `.claude` 或 `.codex` Skill/Agent 副本。
- `docs/upstream/customization-map.md`：NAMEWTA 相对上游的认证、权限、Client、菜单、SQL 和前端契约权威清单。
- `plus-ui-namewta/package.json`、`pnpm-workspace.yaml`、`pnpm-lock.yaml`、各包 `package.json`/`tsconfig.json`：Vue 3、TypeScript 6、Vite 8、Pinia 4、pnpm 10、Node `>=20.19.0` 的多 App monorepo。
- `plus-ui-namewta/apps/admin-web/src/main.ts`：当前唯一可构建和部署的浏览器 App；其他未激活终端仅保留 README 占位。
- `plus-ui-namewta/packages/{domains,web-domains,platform,adapters,web-kit}/**`：headless domain、Vue Web 表现、平台端口、运行时适配器与共享 Web 机制的依赖方向。
- `plus-ui-namewta/packages/api-contracts/**`、`tooling/openapi/**`：生成 transport、不可变快照、来源与漂移检查；domain model 由各领域独立拥有。
- `plus-ui-namewta/tooling/architecture/**`：使用 AST/SFC/YAML 结构化检查工作区、公开入口、依赖方向、终端纯度、占位目录和基线漂移。
- `docs/fm/**`：与已删除的运行时代码生成器解耦的静态 CRUD 模板资产，是 AI 与开发者实现 Java、Vue、React、MyBatis XML、MySQL 菜单片段、树结构、状态/排序和前后端合同时的当前参考基线；SQL 模板只支持 MySQL。
- `wta-vue-plus-namewta/pom.xml`、`mvnw`、各模块 `pom.xml`：Java 21、Spring Boot 4.1.0、Maven Wrapper、46 个 POM 描述符；根 reactor 构建 46 projects，`bundle-full/core` 控制最终 admin fat jar 的业务模块集合。
- `wta-vue-plus-namewta/wta-admin/src/main/java/org/dromara/DromaraApplication.java`、`wta-modules/wta-demo/**` 及 `wta-modules/wta-system/**`：Spring MVC、BO/VO/entity、service、mapper、Bean Validation、数据权限、事务和 Sa-Token 主导实践。
- `wta-vue-plus-namewta/pom.xml`、`wta-common/wta-common-mybatis/**`：dynamic-datasource 4.5.0、`@DSTransactional`、`BaseEntity` 自动填充字段、VO mapper 与链式查询的公共基础设施合同。
- `release-artifacts/docker/infrastructure/mysql/init/10-wta-base.sql` 的 `test_demo`、`wta-modules/wta-demo/**/TestDemo*`：新建项目自有业务表的乐观锁、审计字段、逻辑删除及 entity 映射基线。
- `wta-vue-plus-namewta/wta-common/wta-common-translation/**`：批量翻译和 JSON 响应增强的公共基础设施合同。
- `plan/update.md`：当前端到端构建命令、提交拆分、前后端顺序和禁止全仓无关格式化的项目决策证据。
- `.github/workflows/quality-gates.yml`、`scripts/ci/**`：父仓库配置子模块快照校验、前端静态/单元/E2E/构建、后端测试与双 bundle 打包，以及 Redis/MySQL/MinIO 真实服务验收。远程运行记录需在提交推送后由 GitHub Actions 产生。

## 工具链与质量门禁

| Scope | Working directory | Command | Responsibility | Source | Status |
|---|---|---|---|---|---|
| `module:plus-ui-namewta` | `plus-ui-namewta` | `pnpm architecture:check` / `pnpm architecture:test` | 检查工作区依赖方向、公开入口、终端纯度、占位和基线合同 | `package.json`, `tooling/architecture/**` | active local/CI gate |
| `module:plus-ui-namewta` | `plus-ui-namewta` | `pnpm lint` | 根配置与各激活工作区包的 Oxlint 检查 | `package.json`, `.oxlintrc.json`, package scripts | active local/CI gate |
| `module:plus-ui-namewta` | `plus-ui-namewta` | `pnpm typecheck` | 各激活 App/包的 TypeScript/Vue 非写入式检查 | `package.json`, package scripts/tsconfig | active local/CI gate |
| `module:plus-ui-namewta` | `plus-ui-namewta` | `pnpm test` | 各激活 App/包的 Vitest 测试 | `package.json`, `**/*.test.ts` | active local/CI gate |
| `module:plus-ui-namewta` | `plus-ui-namewta` | `pnpm test:e2e` | Playwright Admin 浏览器验收；第二个 App 激活后扩展隔离场景 | `playwright.config.ts`, `e2e/**` | risk-based local/CI gate |
| `module:plus-ui-namewta` | `plus-ui-namewta` | `pnpm build:dev` / `pnpm build:prod` | 开发配置和生产配置的工作区构建 | `package.json`, App/package scripts | active local gate |
| `module:plus-ui-namewta` | `plus-ui-namewta` | `pnpm fmt` | Oxfmt 写入式格式化受管前端路径 | `package.json` | active tool, not a check gate |
| `module:wta-vue-plus-namewta` | `wta-vue-plus-namewta` | `./mvnw test` | 默认执行 JUnit/Surefire 测试 | `pom.xml`, Maven Wrapper | active local/CI gate |
| `module:wta-vue-plus-namewta` | `wta-vue-plus-namewta` | `./mvnw clean package -DskipTests` | `bundle-full` 全量组合打包 | `pom.xml`, `wta-admin/pom.xml` | active local/CI gate |
| `module:wta-vue-plus-namewta` | `wta-vue-plus-namewta` | `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` | 核心平台组合打包；clean 防止 profile 产物污染，测试由前置 `./mvnw test` 承担 | `wta-admin/pom.xml` | active local/CI gate |
| `workspace-parent` | `.` | `scripts/ci/verify-admin-bundle.sh full\|core` | 断言最终 admin jar 的必需/可选模块集合 | `.github/workflows/quality-gates.yml` | active local/CI gate |
| `workspace-parent` | `.` | `scripts/ci/verify-submodules.sh` | 校验父仓库 gitlink 与检出子模块 SHA 一致 | `.github/workflows/quality-gates.yml` | active CI gate; local dirty tree预期失败 |
| `workspace-parent` | `.` | `scripts/ci/run-external-services.sh` | Docker 启动 Redis 8、MySQL 8.4、MinIO 并运行真实集成测试 | `.github/workflows/quality-gates.yml` | active CI gate; local Docker unavailable |

父仓库已有 GitHub Actions 工作流候选，但只有提交、推送并在仓库分支保护中设为 required check 后，才能称为远程合并门禁。当前本地可执行门禁与 CI 命令保持同源；外部服务 job 因本机无 Docker 尚未本地实跑。

## 排除与冻结区域

- 依赖/缓存/构建输出：`.git/**`、`.pnpm-store/**`、`**/node_modules/**`、`**/dist/**`、`**/target/**`、`**/.flattened-pom.xml`、coverage 和工具缓存。
- 生成声明：`plus-ui-namewta/apps/*/src/types/{auto-imports,components}.d.ts` 及同类 Vite 插件输出；修改生成器配置后重新生成，不手改结果。
- 前端 `packages/api-contracts` 生成结果由 `tooling/openapi` 与已提交快照维护；`tooling/generators` 当前为 README-only 占位。运行时代码生成器及其前端管理面已从基座物理删除；CRUD 实现参考父仓库 `docs/fm/**` 静态模板，不存在 `wta-gen` classpath 或可编辑运行源码。
- 上游冻结分支：后端 `6.X`、前端 `6.X-Vue` 只允许 fast-forward，不承载业务提交；产品变更进入各自 `main`。
- 上游基线标签 `namewta-base-upstream-6x`、`namewta-base-upstream-6x-vue` 不移动。
- `release-artifacts/docker/infrastructure/mysql/init/` 是六份 MySQL 8.4 完整初始化基座的唯一事实源；后端仓库不保存 `script/`、SQL 副本或其他数据库方言。
- NAMEWTA 自有表结构只合并到 `50-namewta-ddl.sql`；初始化数据、菜单和回填只合并到 `60-namewta-dml.sql`。两者是完整可重建基座，不是按时间无限追加的迁移日志。
- SnailJob、WarmFlow、AI 等上游或第三方拥有的 schema 不是 NAMEWTA 表结构整治目标；只在项目明确接管其 schema 时应用项目自有建表规则。

## 未知与冲突

- `pending-decision`: GitHub Actions 工作流尚未通过提交后的远程运行验证，也尚未确认分支保护 required checks；在此之前不得报告为已强制的远程合并门禁。
- `pending-decision`: 前端仍缺少非写入式 format-check 与统一覆盖率阈值；现有 lint/typecheck/unit/E2E/build 已形成可执行基线。
- `verification-gap`: 外部服务验收脚本需要 Docker；本机未安装 Docker，当前只完成脚本语法、测试编译和属性门控验证。
- `ratchet`: `tsconfig.json` 开启 `strict`，但关闭 `noImplicitAny`、`strictFunctionTypes`、`strictNullChecks`；`.oxlintrc.json` 同时关闭显式 any 等规则。新/修改边界收紧，存量不全仓重写。
- 当前没有声明统一覆盖率、ArchUnit、Java formatter、Checkstyle/SpotBugs；不得假装这些工具已启用。E2E 已覆盖 Client 上下文真假值边界，但不是全业务回归套件。
