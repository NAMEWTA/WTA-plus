# 项目画像

## 身份

- Project: `WTA-plus`
- Repository root: 当前包含本 Skill 的 Git 工作区根目录
- Topology: 单一 monorepo；`backend/` 与 `frontend/` 合入，不以 git submodule 为默认交付

## 事实来源

- `README.md`：本仓聚合 `frontend` 与 `backend`，同源开发与发布。
- `.agents/skills/**`、`AGENTS.md`：项目开发 Skill 只在本仓集中维护。
- `frontend/package.json`、`pnpm-workspace.yaml`、`pnpm-lock.yaml`、各包 `package.json`/`tsconfig.json`：Vue 3、TypeScript 6、Vite 8、Pinia 4、pnpm 10、Node `>=20.19.0` 的多 App monorepo。
- `frontend/apps/{admin-web,home-web,sso-web}/src/main.ts`：三个已激活可构建的浏览器 App；`release-artifacts/apps.json` 显式登记三 App 发布面；发布门禁交叉校验 package、Compose/Nginx/env/端口与入口，不从可构建推导可部署。
- `frontend/packages/{domains,web-domains,platform,adapters,web-kit}/**`：headless domain、Vue Web 表现、平台端口、运行时适配器与共享 Web 机制的依赖方向。
- `frontend/packages/api-contracts/**`、`tooling/openapi/**`：生成 transport、不可变快照、来源与漂移检查；domain model 由各领域独立拥有。
- `frontend/tooling/architecture/**`：使用 AST/SFC/YAML 结构化检查工作区、公开入口、依赖方向、终端纯度、占位目录和基线漂移。
- `docs/fm/**`：与已删除的运行时代码生成器解耦的静态 CRUD 模板资产，是 AI 与开发者实现 Java、Vue、React、MyBatis XML、MySQL 菜单片段、树结构、状态/排序和前后端合同时的当前参考基线；SQL 模板只支持 MySQL。
- `backend/pom.xml`、`backend/mvnw`、各模块 `pom.xml`：Java 21、Spring Boot 4.1.0、Maven Wrapper；POM 数量按当前源码盘点；根 reactor 经聚合 POM 组装项目，`bundle-full/core` 控制最终 admin fat jar 的业务模块集合。
- `backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java`、`wta-modules/wta-demo/**` 及 `wta-modules/wta-system/**`：Spring MVC、BO/VO/entity、service、mapper、Bean Validation、数据权限、事务和 Sa-Token 主导实践。
- `backend/pom.xml`、`wta-common/wta-common-mybatis/**`：dynamic-datasource 4.5.0、`@DSTransactional`、`BaseEntity` 自动填充字段、VO mapper 与链式查询的公共基础设施合同。
- `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql` 的 `test_demo`、`wta-modules/wta-demo/**/TestDemo*`：新建项目自有业务表的乐观锁、审计字段、逻辑删除及 entity 映射基线。
- `backend/wta-common/wta-common-translation/**`：批量翻译和 JSON 响应增强的公共基础设施合同。
- `.github/workflows/quality-gates.yml`、`scripts/ci/**`：仓内候选配置 Skill/分层/发布合同检查、前端静态/单元/E2E/构建、后端测试与双 bundle 打包，以及 Redis/MySQL/MinIO 真实服务验收。远程运行记录需在提交推送后由 GitHub Actions 产生。

## 源码库存口径

POM、激活 App 与测试源码以当前工作树为准，不把历史数量当门禁阈值。以下命令在仓根列出事实来源（包括本次未提交源码，排除构建输出）；Java 源文件数量不是 JUnit 测试用例数量，运行数/skip 以当次 Surefire 报告为准。

```bash
rg --files backend -g pom.xml -g '!**/target/**'
rg --files frontend/apps -g package.json -g '!**/node_modules/**' -g '!**/dist/**'
rg --files backend -g '**/src/test/java/**/*.java' -g '!**/target/**'
```

当前实现、可构建、发布清单和已部署是不同事实：App 是否参与构建看 `frontend/package.json`；是否登记发布看 `release-artifacts/apps.json`；实际交付需有干净源码版本、manifest、真实服务验收与部署记录。仓内 CI 配置和历史预览图均不能代替这些记录。

## 工具链与质量门禁

| Scope | Working directory | Command | Responsibility | Source | Status |
|---|---|---|---|---|---|
| `workspace-parent` | `.` | `node scripts/ci/verify-agent-handbooks.mjs` | 检查每个manifest的最近有效导读、中文标题、本地链接及嵌套CLAUDE副本；独有硬约束仍需逐文件评审 | `scripts/ci/verify-agent-handbooks.mjs` | active local gate / CI candidate |
| `module:frontend` | `frontend` | `pnpm architecture:check` / `pnpm architecture:test` | 检查工作区依赖方向、公开入口、终端纯度、占位和基线合同 | `package.json`, `tooling/architecture/**` | active local gate / CI candidate |
| `module:frontend` | `frontend` | `pnpm --filter @namewta/tooling-openapi openapi:check` | 检查生成合同与提交快照漂移 | `tooling/openapi/package.json` | active local gate / CI candidate |
| `module:frontend` | `frontend` | `pnpm lint` | 根配置与各激活工作区包的 Oxlint 检查 | `package.json`, `.oxlintrc.json`, package scripts | active local gate / CI candidate |
| `module:frontend` | `frontend` | `pnpm typecheck` | 各激活 App/包的 TypeScript/Vue 非写入式检查 | `package.json`, package scripts/tsconfig | active local gate / CI candidate |
| `module:frontend` | `frontend` | `pnpm test` | 各激活 App/包的 Vitest 测试 | `package.json`, `**/*.test.ts` | active local gate / CI candidate |
| `module:frontend` | `frontend` | `pnpm test:e2e` | Playwright Admin 浏览器验收；默认配置排除SSO专用用例，不能据此宣称SSO验收 | `playwright.config.ts`, `e2e/**` | risk-based local gate / CI candidate |
| `module:frontend` | `frontend` | `pnpm build:dev` / `pnpm build:prod` | 开发配置和生产配置的工作区构建 | `package.json`, App/package scripts | active local gate |
| `module:frontend` | `frontend` | `pnpm fmt` | Oxfmt 写入式格式化受管前端路径 | `package.json` | active tool, not a check gate |
| `module:backend` | `backend` | `./mvnw test` | 默认执行 JUnit/Surefire 测试 | `pom.xml`, Maven Wrapper | active local gate / CI candidate |
| `module:backend` | `backend` | `./mvnw clean package -DskipTests` | `bundle-full` 全量组合打包 | `pom.xml`, `wta-admin/pom.xml` | active local gate / CI candidate |
| `module:backend` | `backend` | `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` | 核心平台组合打包；clean 防止 profile 产物污染，测试由前置 `./mvnw test` 承担 | `wta-admin/pom.xml` | active local gate / CI candidate |
| `workspace-parent` | `.` | `scripts/ci/verify-admin-bundle.sh full\|core` | 断言最终 admin jar 的必需/可选模块集合 | `.github/workflows/quality-gates.yml` | active local gate / CI candidate |
| `workspace-parent` | `.` | `scripts/ci/run-external-services.sh` | Docker 启动 Redis 8、MySQL 8.4、MinIO 并运行真实集成测试 | `.github/workflows/quality-gates.yml` | CI candidate; requires working Docker daemon |

父仓库已有 GitHub Actions 工作流候选，但只有提交、推送并在仓库分支保护中设为 required check 后，才能称为远程合并门禁。当前本地可执行门禁与 CI 命令保持同源；是否能执行外部服务应在当前环境检查 Docker daemon，不能把旧机器的“无 Docker”作为仓库事实。

## 排除与冻结区域

- 依赖/缓存/构建输出：`.git/**`、`.pnpm-store/**`、`**/node_modules/**`、`**/dist/**`、`**/target/**`、`**/.flattened-pom.xml`、coverage 和工具缓存。
- 生成声明：`frontend/apps/*/src/types/{auto-imports,components}.d.ts` 及同类 Vite 插件输出；修改生成器配置后重新生成，不手改结果。
- 前端 `packages/api-contracts` 生成结果由 `tooling/openapi` 与已提交快照维护；`tooling/generators` 当前为 README-only 占位。运行时代码生成器及其前端管理面已从基座物理删除；CRUD 实现参考父仓库 `docs/fm/**` 静态模板，不存在 `wta-gen` classpath 或可编辑运行源码。
- 产品变更进入本仓 `main`。
- `release-artifacts/docker/infrastructure/mysql/init/` 是六份 MySQL 8.4 完整初始化基座的唯一事实源；后端仓库不保存 `script/`、SQL 副本或其他数据库方言。
- NAMEWTA 自有表结构只合并到 `10-cde-base-ddl.sql`；初始化数据、菜单和回填只合并到 `50-cde-base-dml.sql`。两者是完整可重建基座，不是按时间无限追加的迁移日志。
- SnailJob、WarmFlow 等上游或第三方拥有的 schema 不是 NAMEWTA 表结构整治目标；只在项目明确接管其 schema 时应用项目自有建表规则。

## 发布产物合同

`release-manage.sh` 通过 `release-state.mjs` 从同一干净 Git 归档完成全量构建，生成 `builds/versions/<ID>` 不可变目录及逐文件来源/摘要 manifest。局部构建仅写 development；显式 stage 校验后一次替换 current 符号链接。docker-manage 每次命令固定版本路径，显式重建容器；数据、日志、证书留在独立运行目录。运行时多容器切换不承诺原子性，源 SQL 仍只有六份基座。三端 Origin 和 callback/authorize 矩阵进入 manifest，消费时拒绝运行参数漂移；生产 SSO 使用独立 hostname/HTTPS 入口及根 /sso API，不进入业务 LB。SSO_WEB_BASE_PATH由版本prefix派生，真实AuthController的clientContext包含同一base授权地址。

## 未知与冲突

- `pending-decision`: GitHub Actions 工作流尚未通过提交后的远程运行验证，也尚未确认分支保护 required checks；在此之前不得报告为已强制的远程合并门禁。
- `pending-decision`: 前端仍缺少非写入式 format-check 与统一覆盖率阈值；现有 lint/typecheck/unit/E2E/build 已形成可执行基线。
- `verification-gap`: 外部服务验收脚本需要 Docker daemon；只有提供真实服务参数且未跳过的当前测试结果才能关闭验证缺口。
- `ratchet`: 根 `tsconfig.json` 开启 `strict`，但关闭 `noImplicitAny`、`strictFunctionTypes`、`strictNullChecks`；platform http/auth/app-runtime/permission、axios/OSS 上传 adapter、admin/system/profile/third domain、profile web-domain 与 Home App 的 12 个 tsconfig 已显式开启三项检查。其余包按实际诊断渐进收紧；`.oxlintrc.json` 仍有存量 any 例外，新/修改边界不得扩大，不全仓重写。
- 当前没有声明统一覆盖率、ArchUnit、Java formatter、Checkstyle/SpotBugs；已有自有源码架构合同测试与分层检查，不等于已启用这些工具。E2E 已覆盖 Client 上下文真假值边界，但不是全业务回归套件。

SSO 发布验证入口：`bash scripts/sso-hard-e2e.sh --release-origin --evidence <新JSON路径>` 串行构建三 App并使用临时 Nginx/Redis/MySQL/Chrome；SSO是真实实现，System身份/菜单与业务token签发是显式fixture。普通 `pnpm test:e2e` 不覆盖该专用配置。

Admin/Home 浏览器传输统一为 HTTPS 上的普通 JSON/二进制合同；不再包含共享响应私钥、ECB 包装、crypto-browser 适配器或后端 API 加解密过滤器。机器 HMAC、OSS 签名与数据库字段加密保持各自合同。前后端须使用同一发布版本，切换与恢复需先隔离流量，不承诺跨容器原子热更新。

当前 AI 退出边界：wta-ai / wta-common-ai 仅保留 Maven 占位；三个可部署 Java 应用为 Admin、Monitor、SnailJob。六份 SQL 中 40-cde-ai.sql 仅 SET NAMES utf8mb4;；新业务库 103 张表，旧 AI 数据保留，不重放基座或迁移。

AI 平台项目归属（2026-09-20 用户决定）：完整 AI Agent 平台已转由独立公开仓库 [NAMEWTA/wta-ai](https://github.com/NAMEWTA/wta-ai) 承接，拥有自己的前后端、release-artifacts 与 scripts；不再规划于本仓 backend/wta-extend。本仓现有 Java AI Maven 占位保持原状，WTA-plus 可作为外部 API 消费者；独立平台的数据库、工程规范与发布门禁由新项目维护。
