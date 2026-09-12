---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-12
title: 完成默认关闭装配、失败关闭启动门禁与发布回归
status: done
planning_depth: deep
planning_depth_reason: 最终装配决定模块是否进入 full/core 产物、关闭态兼容性、启用态依赖校验和跨前后端发布证据。
ready: true
risk: critical
blocked_by: [T-07, T-08, T-09, T-11]
contract_ids: [AC-001, AC-018, AC-023, AC-024, AC-025, AC-026, AC-028, AC-029, AC-030]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/config/OpenApiAutoConfiguration.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application*.yml</Path>"
  - "<Path>docs/upstream/customization-map.md</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/config/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/README.md</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application*.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/assembly/**</Path>"
  - "<Path>docs/upstream/customization-map.md</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/open-api/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application*.yml</Path>"
  - "<Path>docs/upstream/customization-map.md</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</Path> => T-12"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application*.yml</Path> => T-12"
  - "<Path>docs/upstream/customization-map.md</Path> => T-12"
---

# Ticket T-12: 完成默认关闭装配、失败关闭启动门禁与发布回归

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/12-openapi-assembly-release-gate.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>`

## 1. 战略与来源

- **目标：** 将已验证能力安全装配进 full/core，保持默认关闭，并以启动门禁和跨前后端回归证明可发布性。
- **可观察产出：** 关闭态无端点/无普通登录回归；启用态缺 KEK/Redis/SPI 直接启动失败；配置完备时全链与双 UI 合同通过。
- **来源：** `AC-001`、`AC-018`、`AC-023` 至 `AC-026`、`AC-028` 至 `AC-030`、发布/迁移约束。
- **当前事实：** T-01 已把模块纳入依赖图，前置票交付协议、registry、credential、网关、失效链和 UI；本票独占最终自动配置与环境开关。
- **Planning Depth 原因：** 自动配置条件错误会导致安全能力意外开启、静默降级或 full/core 构建分叉。

## 2. 决策状态

### 已锁定决策

- `openapi.enabled=false` 为所有环境默认值；关闭时不注册网关和 OpenAPI 管理端点。
- 启用时 KEK、Redis、唯一 credential/session/authorization/registry SPI 任一缺失或无效都阻止启动。
- 不允许回退为本地 nonce/限流、明文 secret、空权限或单节点 Session。
- full/core 都必须解析相同 common-openapi artifact；最终配置与上游定制点写入 customization map。

### 已采用的低影响假设

- 环境变量名称沿用项目配置命名体系；具体 secret provider key 在实现时以现有绑定约定确定。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| conditional auto-config、startup validation、default config、module README/customization map、full/core 与前后端回归 | T-01 至 T-11 全部产出、现有 build profiles | 生产启用、真实 DDL 执行、KEK 下发、全量 Playwright、真实多节点压测 |

## 4. 要构建什么

完成唯一自动配置入口和配置属性校验，以默认关闭方式接线 registry、credential、网关、Session、Redis 和事件。建立关闭/启用错误/启用成功三类应用上下文测试，并跑 full/core 后端与前端集成门禁，形成可审计发布 Evidence。

## 5. 实现契约

- **入口或接缝：** Spring Boot AutoConfiguration imports、admin application 配置与现有 Maven profiles。
- **输入与输出：** 配置 + beans -> 明确关闭、启动失败或完整启用上下文。
- **公共接口变化：** 新增 documented OpenAPI 配置项；默认值不改变现有部署行为。
- **不变量：** 关闭态无管理/签名入口；启用态绝不缺组件运行；普通 Token 登录与现有 HTTP 行为不变。
- **状态或数据流：** property binding -> validate dependencies/secrets -> register beans -> startup probe/tests。
- **错误与失败行为：** 配置错误给出不含 secret 的 actionable startup error；禁止静默 disabled 或降级。
- **兼容要求：** full/core 构建均通过，默认部署无需新增环境变量即可保持原行为。
- **安全与隐私要求：** README/config 示例只写变量占位符，不提交真实 KEK/credential/signature。

## 6. 执行路线

1. 写 disabled/missing dependency/valid context 的 ApplicationContextRunner 或等价测试。
2. 实现条件自动配置、properties validation 与 imports；补默认关闭 application 配置。
3. 更新 module README 和 customization map，记录配置、SQL、菜单、前端 manifest 定制点。
4. 跑后端 common/system/admin 定向测试、full/core package 与前端完整门禁。
5. 汇总所有 Ticket Evidence、基线/候选 SHA、未执行真实基础设施验证与生产批准点。

## 7. 路径访问契约

- **预计修改点/可写范围：** common-openapi config/imports/README、admin 环境配置/assembly tests 和 customization map。
- **只读上下文：** 全部前置实现、父 POM/admin POM 与前端 UI。
- **共享路径：** imports、application 配置、customization map 由 T-12 独占。
- **保留或不动：** 前置业务实现、DDL/DML、真实环境 secret 与部署系统。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 默认关闭兼容 | application context + MockMvc | `./mvnw -pl ruoyi-admin -am test -Dtest='*OpenApiAssembly*'` | 无 OpenAPI 入口，普通认证回归通过 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>` |
| 启用态失败关闭 | context matrix | 同上 | 缺 KEK/Redis/SPI 均启动失败且信息脱敏 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>` |
| 后端全链与 profiles | Maven gates | `./mvnw test && ./mvnw -Pbundle-full -DskipTests package && ./mvnw -Pbundle-core -Dmaven.test.skip=true package` | tests/full/core 全绿且包含同一模块 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>` |
| 前端双入口回归 | workspace gates | `pnpm architecture:check && pnpm test && pnpm typecheck && pnpm lint && pnpm build:prod` | domain/web-domain/App 与全量构建通过 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>` |
| 应用边界验签 | parent-candidate E2E | 运行 T-09 MockMvc E2E 与 T-11 集成测试 | 签名调用、catalog 与双 scope UI 合同成立 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>` |

- **Workspace checks：** source-worktree 先跑定向；parent-candidate 跑 Maven full/core 与 pnpm 全门禁。
- **E2E disposition：** required：以 Spring 应用上下文/MockMvc 和前端组件/App 集成组成发布边界 E2E；测试 double 不冒充真实 MySQL、Redis、多节点或完整 Playwright。
- **E2E owner/environment：** Lead / parent-candidate。
- **Integration evidence：** 所有 implementation/source、parent before、candidate/result SHA、父分支包含关系和命令原始结果摘要。

## 9. 发布、迁移与恢复

- **迁移顺序：** 备份 -> T-06 additive DDL/DML -> 部署默认关闭代码 -> 配置 Redis/KEK -> 单独批准启用。
- **兼容窗口：** 默认关闭无限期兼容；启用只在依赖完整环境执行。
- **监控信号：** 启动失败类别、OpenAPI error code、nonce/限流/Session 失效、普通登录错误率。
- **回滚或前向恢复：** 先将开关置 false；保留 additive schema/菜单，修复配置或代码后前向恢复。
- **不可逆操作与批准点：** 生产 DDL/DML、KEK 下发和 `enabled=true` 均需另行批准；本票不执行。
- **收缩条件：** default-off/fail-closed/full/core/前端全门禁及 Evidence 全部成立。

## 10. 验收标准

- [x] 关闭态完全兼容，启用态缺任一安全依赖即启动失败。
- [x] full/core 后端产物、前端 architecture/test/typecheck/lint/build 全部通过。
- [x] `AC-001`、`AC-018`、`AC-023` 至 `AC-030` 的最终装配/回归证据齐全。
- [x] customization map、README、迁移/回滚/生产批准点与测试基础设施限制如实记录。
