# 验证与交付

## 验证顺序

1. 静态检查目录、访问面、模板、Mapper/XML、权限字符串和 SQL owner。
2. 运行受影响测试类或 Maven 模块，快速验证编译和行为。
3. 按风险运行后端根级 `test` 与双 bundle package。
4. 跨前端/API/数据库/部署变化时运行对应工作区门禁和真实服务测试。
5. 记录工作目录、命令、退出码、跳过原因和未验证项。

## 快速 Maven 反馈

从 `backend` 执行：

```bash
./mvnw -pl :<artifact-id> -am test
./mvnw -pl :<artifact-id> -am -DskipTests compile
./mvnw -DskipTests validate
```

运行单个测试时使用 Surefire 的 `-Dtest=<ClassName>`，并确认 `-am` 上游模块是否实际包含测试。只 compile 不能证明权限、事务、SQL 或并发行为。

## 根级门禁

```bash
./mvnw test
./mvnw clean package -DskipTests
./mvnw clean package -Pbundle-core -Dmaven.test.skip=true
```

- 默认全量与 core bundle 都从 clean 构建，防止 profile/target 产物互相污染。
- core 的 `maven.test.skip=true` 只用于已由本次根级 `./mvnw test` 验证后的组装阶段。
- 修改 bundle/POM 后运行父仓库 `scripts/ci/verify-admin-bundle.sh full` 和 `core`。
- Redis、MySQL、MinIO 属性门控测试必须区分 skipped、真实服务已验证和未运行；不能把 skipped 报告为通过真实集成。

## Skill 与模板自检

从父仓库根目录执行，脚本统一使用 Node.js：

```bash
node --check .agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs
node .agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs
node --check docs/fm/scripts/validate.mjs
node docs/fm/scripts/validate.mjs
```

`validate-skill.mjs` 检查 frontmatter、reference/链接、精确模板索引、system 规范证据和 50/60 SQL owner；`docs/fm/scripts/validate.mjs` 检查模板目录、MySQL-only catalog、HTTP method 与模板静态合同。

## 目录与 Controller 静态检查

先读取模块模式登记表。classic 只检查兼容目录和新增依赖不扩散；layered 额外运行 `validate-module-mode.mjs` 检查五层目录、入口/API Adapter、port/adapter/listener/support/domain 边界、事务注解位置、Service 同层互调、MyBatis import 和 Row 命名。

针对受影响模块检查：

```bash
find <module>/src/main/java -type f | sort
find <module>/src/main/java -type d \
  \( -path '*/service/*' ! -path '*/service/impl' ! -path '*/service/impl/*' \)
rg -n 'class .*DataSupport|package .*\.(repository|manager);' <module>/src/main/java
rg -n '@SaIgnore|@SaCheckPermission|@GetMapping|@PostMapping|@PutMapping|@PatchMapping|@DeleteMapping' <module>/src/main/java
rg -n '@Log|@RepeatSubmit|@DSTransactional|@Transactional' <module>/src/main/java
```

逐项确认：

- 管理端在 `controller/admin`，匿名端在 `controller/anonymous`；没有无真实客户端的预建目录。
- classic `service` 没有 `impl` 之外的生产子目录，也没有包装单一 Mapper 的 `*DataSupport`、DAO、Repository 或 Manager；layered `service`、`dao`、`usecase` 只保留语义明确的 owner，DAO 不调用其他 DAO/Service/外部端口。
- layered `port` 只声明外部能力合同，`adapter/{api,gateway,provider,store}` 只实现入口或端口，`listener` 只能调用 UseCase，`support` 不得声明 Spring Bean 或 I/O；`domain/model/read` 使用单数 `<Capability>Row`/`<Capability>Projection`。仅 Profile 存量迁移暂时 allowlist `PersonAdminRows`、`EnterpriseAdminRows` 两个精确路径，禁止以此为新命名模板，拆分完成后移除例外。
- layered Service/DAO/Mapper 不得持有 `@DSTransactional`；事务命令必须由 UseCase 持有，Service 不得导入 `PageQuery`、MyBatis `Page` 或直接调用另一个 Service。
- 每个 `@SaIgnore` 端点仍有签名/重放/幂等/审计/脱敏合同。
- CRUD 查询 GET、变更 POST；每个 POST 的 `@Log`、`BusinessType` 和敏感保存配置准确。
- 新增/实质修改业务事务没有误用 Spring `@Transactional`。

## Mapper、XML 与 SQL 静态检查

```bash
rg -n '@(Select|Insert|Update|Delete)' <module>/src/main/java
rg -n 'BaseMapperPlus|QueryBuilder|lambdaJoin|MPJBaseMapper|DataPermission|DataColumn' <module>/src/main/java
find <module>/src/main/resources/mapper -type f -name '*.xml' -print
rg -n 'CREATE TABLE|ALTER TABLE|INSERT INTO|UPDATE ' release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql
```

人工核对注解 SQL 是否确属短静态例外；复杂查询是否在 wrapper/MPJ/XML；namespace/statement/参数/alias 是否一致；数据权限是否覆盖真实 SQL；DDL/DML 是否进入 50/60 且后端无副本。

## 风险测试矩阵

| 变化 | 最低验证 |
|---|---|
| CRUD 字段/校验 | add/edit/list/detail，null/空/边界，映射与序列化 |
| 权限/Client/匿名 | 未登录、无权限、跨 Client、签名错误、过期时间戳、nonce 重放、重复提交 |
| Mapper/XML | 参数/alias、空条件、分页/排序、数据范围正负例、真实 MySQL SQL |
| 事务/关系 | 中途失败回滚、空集合语义、重复/无效引用、缓存失效、提交阶段副作用 |
| 并发/状态 | 唯一冲突、乐观锁/条件更新 0 行、重复审批/提交、锁超时与释放 |
| 树结构 | root/child、移动、环拒绝、后代 ancestors、含子节点删除 |
| POM/bundle | reactor validate、模块测试、full/core clean package、jar 内容断言 |
| SQL 基座 | 全新 MySQL 按 10-60 初始化；已有库差异升级/回滚隔离演练 |

## 交付审计

- 未无意修改 Maven Wrapper、BOM、无关 POM、依赖版本或上游镜像分支。
- `target`、`.flattened-pom.xml`、外部服务数据和敏感配置未进入变更。
- 跨模块只通过 `wta-api`/common SPI；前端调用方、OpenAPI、权限菜单和部署资产与后端合同一致。
- 结构重构提供旧路径到新路径、路由、调用方、测试、SQL/配置的完整映射；仅移动与行为变化可以分别审查。
- SQL 只在父仓库六份 MySQL 基座，NAMEWTA 变化归入 50/60；没有恢复其他方言或后端 `script/`。
- 实际命令、工作目录、退出码、失败、跳过和环境限制均准确记录。

根级命令和 CI 状态以工程规范的[项目画像](../../../engineering-standards/references/project/00-project-profile.md)为权威；本页只说明后端任务的选择顺序。
