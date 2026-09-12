---
schema_version: 3
artifact: ticket
change: 2026-09-01-admin-runtime-capability-reconciliation
id: T-02
title: 显式启用 OpenAPI 发布配置并验收 Admin 最终体验
status: done
planning_depth: deep
planning_depth_reason: 变更双实例安全配置透传并在现有开发环境滚动启用涉及共享 KEK、启动失败关闭、运行可用性与真实浏览器权限投影。
ready: true
risk: critical
blocked_by: [T-01]
contract_ids: [AC-001, AC-002, AC-003, AC-004, AC-012, AC-014, AC-015, AC-016]
owner: codex:/root
expected_changes:
  - "<Path>release-artifacts/docker/docker-compose-backend.yml</Path>"
  - "<Path>release-artifacts/.env.example</Path>"
  - "<Path>release-artifacts/tests/release-config.test.mjs</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/config/OpenApiAutoConfiguration.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/assembly/OpenApiAssemblyContextTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/SysOpenApiCredentialController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/SysOpenApiCredentialControllerContractTest.java</Path>"
writable_paths:
  - "<Path>release-artifacts/docker/docker-compose-backend.yml</Path>"
  - "<Path>release-artifacts/.env.example</Path>"
  - "<Path>release-artifacts/tests/release-config.test.mjs</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/config/OpenApiAutoConfiguration.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/assembly/OpenApiAssemblyContextTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/SysOpenApiCredentialController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/SysOpenApiCredentialControllerContractTest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/**</Path>"
shared_paths:
  - "<Path>release-artifacts/docker/docker-compose-backend.yml</Path>"
  - "<Path>release-artifacts/.env.example</Path>"
  - "<Path>release-artifacts/tests/release-config.test.mjs</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/config/OpenApiAutoConfiguration.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/assembly/OpenApiAssemblyContextTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/SysOpenApiCredentialController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/SysOpenApiCredentialControllerContractTest.java</Path>"
shared_path_owners:
  - "<Path>release-artifacts/docker/docker-compose-backend.yml</Path> => T-02"
  - "<Path>release-artifacts/.env.example</Path> => T-02"
  - "<Path>release-artifacts/tests/release-config.test.mjs</Path> => T-02"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/config/OpenApiAutoConfiguration.java</Path> => T-02"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/assembly/OpenApiAssemblyContextTest.java</Path> => T-02"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/SysOpenApiCredentialController.java</Path> => T-02"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/SysOpenApiCredentialControllerContractTest.java</Path> => T-02"
---

# Ticket T-02: 显式启用 OpenAPI 发布配置并验收 Admin 最终体验

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/02-enable-openapi-release-and-verify-admin.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 让受管发布显式、同源地为两个 admin 实例提供 OpenAPI 配置，同时保留未配置安装的 default-off 安全模型，并证明最终 Admin 页面/菜单行为。
- **可观察产出：** Compose 两实例接收相同的 `OPENAPI_ENABLED`、`OPENAPI_KEK_VERSION`、`OPENAPI_KEK`；公开示例只有关闭默认值和占位符；目标开发环境滚动启用后本人接口目录不再业务 404，侧栏与个人设置符合最新产品决定。
- **来源：** `US-001`、`US-002`、`US-003`、`US-004`、`US-006`、`AC-001` 至 `AC-004`、`AC-012`、`AC-014` 至 `AC-016`、`ADR-001`、`DIAG-001`。
- **当前事实：** release Compose 已同源透传三项变量，Actuator 双 `RequestMappingHandlerMapping` 注入歧义与缺失凭据语义均已回归修复；目标两实例运行最终镜像且 healthy/0 重启，登录浏览器四项体验全部通过。
- **Planning Depth 原因：** KEK 配置与双实例一致性直接影响凭据可解密性和启动安全，最终验收跨 Compose、Spring、DB、Redis、动态菜单和浏览器。

## 2. 决策状态

### 已锁定决策

- 代码与公开 `.env.example` 保持 `OPENAPI_ENABLED=false`；只有目标私密 `.env` 显式为 true。
- 两个 admin 实例必须通过共享环境锚点取得同一 `OPENAPI_ENABLED`、`OPENAPI_KEK_VERSION` 和 `OPENAPI_KEK`。
- KEK 示例只能是占位符；真实 KEK 必须为合法 32-byte Base64，存在被忽略且权限 `0600` 的部署配置，不进入 Git、SpecDev、命令输出、日志或浏览器。
- 滚动顺序固定为 server1 -> 健康/路由/日志验证 -> server2 -> 健康/路由/日志验证；第一个失败不推进第二个。
- 不修改 OpenAPI HTTP/wire contract、不改变 Nacos 独立登录、不恢复生成器 frontend/backend。

### 已采用的低影响假设

- 目标环境当前 Compose 和私密 `.env` 仍由 `/data/namewta-data` 下的既有 takeover 布局管理；执行前用只读发现重新定位，不能靠猜测路径覆盖。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| release Compose/env 示例/静态测试、MVC HandlerMapping 选择修复、缺失凭据 404 语义及回归、Spring/前端回归、开发环境私密配置、双实例滚动与浏览器验收 | 现有 OpenAPI conditional assembly、Redis/SPI、Admin manifest/profile、Nacos iframe | 其他协议/API 改造、真实 secret 入库、普通角色自动授权、远程 push、CDE/生产发布 |

## 4. 要构建什么

发布维护者可从公开示例看到 OpenAPI 的显式关闭开关和 secret 占位符，Compose 将同一配置透传给两个实例。目标开发环境在 T-01 数据库最终态后，以权限受限的私密配置滚动启用；每个实例先通过健康、未登录路由边界和脱敏日志检查。最终重新登录 Admin 后，个人 OpenAPI 可连接、系统管理出现 OpenAPI管理、系统监控出现 Nacos配置中心，系统工具/代码生成不再出现。

## 5. 实现契约

- **入口或接缝：** release `.env` -> Compose `x-admin-environment` -> Spring `application.yml` -> OpenAPI conditional beans/controllers；Admin 动态菜单与个人 profile。
- **输入与输出：** 三个环境变量和已收敛 DB -> default-off 或完整启用的双实例；合法登录/权限 -> 正确页面与菜单。
- **公共接口变化：** 无；只让目标环境装配既有 `/system/openApi/**`，未配置环境仍无该路由。
- **不变量：** 两实例 KEK/version 一致；关闭态无 OpenAPI beans；启用错误 fail closed；Nacos component/permission 和 OpenAPI permissions 不变。
- **状态或数据流：** public placeholder/static gate -> private target config -> server1 recreate/verify -> server2 recreate/verify -> cache/session refresh -> API/menu/browser final gate。
- **错误与失败行为：** 缺失/占位/非法 KEK 不启动或不允许 rollout；路由仍业务 404、日志出现 secret、健康失败或实例配置不一致都停止推进并恢复该实例为 disabled/上一配置。
- **兼容要求：** 默认 Compose 不要求真实 KEK 且保持功能关闭；既有普通 Token、full/core、frontend build 和 Nacos 页面合同不退化。
- **安全与隐私要求：** 所有输出对 KEK、Token、AppSecret、数据库/Redis 凭据脱敏；浏览器只验证状态与标签，不捕获凭据正文。

## 6. 执行路线

1. 先扩展 Node release config test，证明三项变量当前未透传并锁定 default-off/placeholder/双实例同源合同。
2. 修改 Compose 共享 admin environment 与公开 `.env.example`，运行静态测试和 Compose 解析正反向场景。
3. 重跑 OpenAPI assembly、Admin manifest/profile、前端 typecheck/build 和受影响 release gates，形成 parent implementation commit。
4. 若真实启用暴露既有 assembly 与目标运行时差异，先以最小回归测试复现并修复同一 OpenAPI 自动配置接缝，重新构建候选镜像；不得跳过首实例门禁。
5. 只读接管目标发布，登记当前 image/config/compose 定位、实例健康和精确回滚命令；确认 T-01 数据库 Gate 已关闭。
6. 在远端权限受限 `.env` 生成或注入同一 KEK/version，逐实例重建并分别验证健康、路由不再业务 404和日志脱敏。
7. 刷新菜单缓存/重新登录，用 Admin 浏览器验收个人 OpenAPI、两个菜单位置名称及生成器入口消失。
8. 汇总数据库、HTTP、Compose、日志与浏览器 Evidence，执行 final secret scan 和工作树/commit 核对。

## 7. 路径访问契约

- **预计修改点：** release backend Compose、公开 env 示例、Node release test、OpenAPI MVC 自动配置、凭据缺失查询语义及其 Admin 回归测试。
- **可写范围：** 仅 frontmatter `writable_paths`；真实启用已证明后端 assembly 对 Actuator 双 HandlerMapping 不兼容，因此按 deviation control 在本 Ticket 内升级该精确接缝，其他后端与前端源码仍保持只读。
- **只读上下文：** Spring property 配置、Admin manifest/profile 和动态路由实现；system Controller 仅开放上述缺失凭据契约文件。
- **共享路径：** 三个 release 文件由 T-02 唯一修改。
- **保留或不动：** application default false、OpenAPI protocol/credential schema、Nacos auth、普通角色权限和所有生成器源码退役决定。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 发布配置合同 | Node static test + Compose config | `node --test release-artifacts/tests/release-config.test.mjs`，并用 placeholder/valid private env 解析 backend Compose | 两实例同源透传；默认 false；公开文件无真实 secret | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| default-off/fail-closed | Spring assembly tests | `./mvnw -pl ruoyi-admin -am test -Dtest='*OpenApiAssembly*' -Dsurefire.failIfNoSpecifiedTests=false` | 关闭态无路由；错误配置失败关闭且不泄密；合法配置装配唯一能力 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| 前端运行面 | Vitest + typecheck/build | Admin manifest/profile 定向 Vitest，随后 admin-web typecheck/build | component 可解析，个人 tab 与动态菜单恢复合同不退化，生成器仍关闭 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| 双实例滚动 | 真实 Compose/HTTP/log | 逐实例 recreate、health、`/system/openApi/self/interfaces` 未登录探针和脱敏日志扫描 | 两实例都不返回业务 404；无 secret；首实例失败不推进 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| Admin 最终体验 | 登录浏览器 smoke | 重新登录后打开个人设置、系统管理、系统监控 | OpenAPI可用；OpenAPI管理和Nacos配置中心位置正确；无系统工具/代码生成 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |

- **Workspace checks：** Lead 在 current workspace 运行 Node/Compose、Maven assembly、Vitest、admin-web typecheck/build 和 secret diff scan。
- **E2E disposition：** required：问题发生在真实双实例、动态菜单和浏览器；静态 Compose/Spring/Vitest 不能替代目标环境 HTTP 与登录验收。
- **E2E owner/environment：** Lead / current-workspace，目标为现有 NAMEWTA 开发服务器和 Admin 浏览器；不触碰同机 CDE。
- **Integration evidence：** aggregate implementation/result SHA、parent-before、Compose 渲染摘要、双实例状态/HTTP、脱敏日志与浏览器断言。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01 数据库最终态 -> release 静态门 -> 私密配置准备 -> server1 -> server2 -> 会话/菜单刷新 -> 浏览器验收。
- **兼容窗口：** 公开/default 部署无限期保持 off；目标环境两个实例滚动期间允许短暂一开一关，但 LB 可用性和旧实例不受影响。
- **监控信号：** 容器健康、startup error category、HTTP business code、OpenAPI Controller mapping、日志 secret scan、菜单/API 浏览器状态。
- **回滚或前向恢复：** 当前实例失败时恢复上一 `.env`/image 或将 `OPENAPI_ENABLED=false` 后重建；数据库保留 T-01 最终态，修复配置后前向再启用。
- **不可逆操作与批准点：** 开发环境私密配置和逐实例重建已由用户本次执行决定授权，但只有 T-01 用户无备份批准、数据库最终态 Gate 与当前发布/实例回滚登记都通过后生效；生产/CDE仍未授权。
- **收缩条件：** 两实例配置/健康/路由一致，最终登录体验通过，且所有输出与 Git secret scan 为零命中。

## 10. 验收标准

- [x] `AC-001` 至 `AC-004`、`AC-012`、`AC-014` 至 `AC-016` 全部有 Lead 可复查 Evidence。
- [x] Compose 两实例共享三项 OpenAPI 配置，公开默认关闭且无真实 secret。
- [x] 目标两个实例逐个启用并通过健康、路由和脱敏日志门禁。
- [x] Admin 实际页面和菜单与用户四项问题全部一致。
- [x] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>`。
- [x] 实际项目修改未超出 `writable_paths`，shared path 仅由 T-02 修改。
- [x] 非空 implementation commit、direct-parent result 和 E2E result 已记录。
- [x] 未发生未批准的远程 push、CDE、生产、权限或 secret 偏差。
