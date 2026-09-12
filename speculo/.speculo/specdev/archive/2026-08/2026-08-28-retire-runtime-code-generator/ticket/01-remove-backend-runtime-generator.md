---
schema_version: 3
artifact: ticket
change: 2026-08-28-retire-runtime-code-generator
id: T-01
title: 删除后端运行时代码生成器
status: done
planning_depth: deep
planning_depth_reason: 删除公开 HTTP 接口、Springdoc 扫描面和共享 Maven 模块图，目录物理删除后必须保持两种 Admin bundle 可构建
ready: true
risk: high
blocked_by: []
contract_ids: [AC-001, AC-002]
owner: codex:/root
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-gen/**</Path>", "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>", "<Path>scripts/README.md</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-gen/**</Path>", "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>", "<Path>scripts/README.md</Path>"]
read_only_paths: ["<Path>docs/fm/**</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/**</Path>", "<Path>plus-ui-namewta/packages/api-contracts/openapi/**</Path>"]
shared_paths: ["<Path>ruoyi-vue-plus-namewta/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>", "<Path>scripts/README.md</Path>"]
shared_path_owners: ["<Path>ruoyi-vue-plus-namewta/pom.xml</Path> => T-01", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path> => T-01", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path> => T-01", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path> => T-01", "<Path>scripts/ci/verify-admin-bundle.sh</Path> => T-01", "<Path>scripts/README.md</Path> => T-01"]
---

# Ticket T-01: 删除后端运行时代码生成器

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/01-remove-backend-runtime-generator.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 从 Maven reactor、Admin 组合和运行源码中完整退役 `ruoyi-gen`，使基座不再编译、打包或暴露在线代码生成能力。
- **可观察产出：** 默认/full 与 core Admin 均能构建，活动源码和装配中没有 `ruoyi-gen` 或 `/tool/gen`。
- **来源：** `US-001`、`AC-001`、`AC-002`、`ADR-001`、`USER-DECISION:base-hard-retirement`、当前 Maven/Java/YAML 事实。
- **当前事实：** 根 POM dependencyManagement、modules reactor、Admin full profile 和 Springdoc packages-to-scan 仍装配生成器；模块源码仍提供 `/tool/gen`。模块模板资源已有用户发起的删除，实施必须保留该删除意图。
- **Planning Depth 原因：** 本 Ticket 删除公开 HTTP 能力和共享构建图，事故会表现为接口残留或整个后端无法构建。

## 2. 决策状态

### 已锁定决策

- 物理删除整个 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-gen/**</Path>`，不保留空模块、stub、redirect 或 deprecated endpoint。
- 只删除生成器装配；其他业务模块、bundle-full/core 语义和 Springdoc 扫描保持不变。
- `<Path>docs/fm/**</Path>` 是独立静态资产，不移动回后端 classpath。

### 已采用的低影响假设

- 实施前重新扫描确认没有生成器之外的 Java 消费者；若出现真实消费者，按偏差控制停止而非扩大删除。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 删除模块、根 dependencyManagement、reactor、Admin full 依赖和 Springdoc 扫描项 | 现有 Maven wrapper、bundle-full/core profiles、其他模块测试 | SQL 菜单/表删除、前端删除、OpenAPI revision、`docs/fm` 内容 |

## 4. 要构建什么

维护者从目标基座执行后端测试和两种 bundle 构建时，Maven 不再解析 `ruoyi-gen`，Admin 产物也不携带生成器 controller、模型或模板。任何 `/tool/gen` 请求不再有专用映射，按应用既有无匹配路由行为处理。若存在遗漏引用，静态扫描或构建必须失败，不能通过空包或跳过模块掩盖。

## 5. 实现契约

- **入口或接缝：** 根/模块/Admin POM、Admin Springdoc 配置、生成器 Java 源码与 Maven 构建。
- **输入与输出：** 输入为当前基座源码；输出为无 `ruoyi-gen` 的 reactor 和 Admin 构建产物。
- **公共接口变化：** 删除全部 `/tool/gen*` HTTP 路径及对应 Java 请求/响应类型；无替代接口。
- **不变量：** bundle-core 保持现有最小组合；bundle-full 只少生成器；冻结 SQL 与 `docs/fm` 不变。
- **状态或数据流：** 编译期依赖链与运行时请求映射同时收缩，不新增运行状态。
- **错误与失败行为：** 遗漏依赖由 Maven 或扫描显式失败；未知 URL 沿用全局 404/无映射处理。
- **兼容要求：** 不适用：用户确认基座硬退役，不保留旧 consumer。
- **安全与隐私要求：** 必须删除服务端入口，不能只依赖菜单或 UI 隐藏。

## 6. 执行路线

1. 记录当前 Maven/Java/YAML 命中，确认消费者仅属于生成器纵向能力。
2. 从根 dependencyManagement、modules reactor、Admin full profile 和 Springdoc 扫描中移除生成器装配。
3. 物理删除模块全部受跟踪源码、资源和模块 POM，并保留用户已发起的模板删除。
4. 运行定向残留扫描、后端测试和默认/full 与 core clean package，形成可提交落点。
5. 记录 implementation/source commit 与 Goal Plan 选定父分支验证结果。

## 7. 路径访问契约

- **预计修改点：** frontmatter `expected_changes` 列出的模块目录、三个 POM 与 Springdoc 配置。
- **可写范围：** 仅 frontmatter `writable_paths`；发现其他引用时停止并登记偏差。
- **只读上下文：** `docs/fm`、冻结/NAMEWTA SQL 和当前 OpenAPI。
- **共享路径：** 四个全局装配文件由 T-01 唯一修改。
- **保留或不动：** `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`、其他业务模块和历史业务生成代码。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Maven reactor 与 Admin bundles | 在 `<Path>ruoyi-vue-plus-namewta/**</Path>` 运行 `./mvnw test`、`./mvnw clean package -DskipTests`、`./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` | 三项成功且不构建/打包 `ruoyi-gen` | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-01.md</Path>` |
| 失败路径 | 活动源码/装配扫描 | 对 POM、YAML 与 Java 源码扫描 `ruoyi-gen`、`org.dromara.generator`、`/tool/gen` | 零意外命中；任何命中阻塞完成 | 同上 |
| 回归 | bundle 差异 review | 对比删除前后 module/profile 列表和构建日志 | 仅生成器组合消失，其他模块保持 | 同上 |

- **Workspace checks：** 按 Goal Plan 在 current workspace 或 source worktree 运行上述非 E2E 扫描和 Maven 门禁。
- **E2E disposition：** not-required：本 Ticket 的边界由源码映射消失和双 bundle 构建直接证明；当前 OpenAPI 的跨仓库合同由 T-04 验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；本 Ticket 无独立 E2E 场景。
- **Integration evidence：** 非空 implementation/source commit、parent before、适用 candidate/result SHA、验证命令与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先收缩装配引用，再删除模块，随后以构建证明不存在悬空消费者。
- **兼容窗口：** 不适用：基座不保留旧 API。
- **监控信号：** 不适用：不存在生产运行期；构建和合同扫描是验收信号。
- **回滚或前向恢复：** 实施提交前可恢复本地删除；获批集成后只修正遗漏消费者，不恢复运行生成器兼容面。
- **不可逆操作与批准点：** 产品代码删除由 Git 可追溯；用户已批准物理删除，实际 implementation commit/integration 仍需 I/Goal Plan 明确授权。
- **收缩条件：** 活动 Maven、YAML、Java 源码与构建日志中的生成器引用为零。

## 10. 验收标准

- [x] `AC-001`：Maven reactor、dependencyManagement、Admin profiles 与产物不含 `ruoyi-gen`，两种 bundle 构建通过。
- [x] `AC-002`：活动后端源码不再定义 `/tool/gen*` 或 `GenTable*` 合同。
- [x] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-01.md</Path>`。
- [x] 实际项目修改未超出 `writable_paths`，shared path 由 T-01 修改。
- [x] Ticket 已按 Goal Plan 策略形成非空 implementation/source commit，direct-parent 或 candidate 验证通过且父分支 result 已记录。
- [x] E2E disposition 已执行；未发生未批准偏差；Ticket、Map 和 Evidence 状态一致。
