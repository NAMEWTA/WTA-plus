---
schema_version: 3
artifact: ticket
change: 2026-08-24-oss-eli5
id: T-07
title: 配对更新 OpenAPI 与 OSS 管理端
status: done
planning_depth: deep
planning_depth_reason: 更新后端 wire format、不可手改的 OpenAPI 快照/生成物和前端共享 transport/UI，属于跨仓库公共合同迁移。
ready: true
risk: medium
blocked_by: [T-02, T-04, T-06]
contract_ids: [AC-003, AC-010, AC-012, AC-013, AC-014, AC-018]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/readiness/OssStorageReadinessRegistry.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/readiness/OssStorageReadinessRegistryUnitTest.java</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/openapi/**</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/generated/openapi.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-types.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-service.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-service.test.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/oss-config/OssConfigPage.vue</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/oss-config/**.test.ts</Path>"
  - "<Path>plus-ui-namewta/e2e/oss-config-access-policy.spec.ts</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/readiness/OssStorageReadinessRegistry.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/readiness/OssStorageReadinessRegistryUnitTest.java</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/openapi/**</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/generated/openapi.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-types.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-service.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-service.test.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/oss-config/OssConfigPage.vue</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/oss-config/**.test.ts</Path>"
  - "<Path>plus-ui-namewta/e2e/oss-config-access-policy.spec.ts</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssConfigController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssMigrationController.java</Path>"
  - "<Path>plus-ui-namewta/tooling/openapi/**</Path>"
  - "<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>"
shared_paths:
  - "<Path>plus-ui-namewta/packages/api-contracts/openapi/**</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/generated/openapi.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-types.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-service.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/oss-config/OssConfigPage.vue</Path>"
shared_path_owners:
  - "<Path>plus-ui-namewta/packages/api-contracts/openapi/**</Path> => T-07"
  - "<Path>plus-ui-namewta/packages/api-contracts/generated/openapi.ts</Path> => T-07"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-types.ts</Path> => T-07"
  - "<Path>plus-ui-namewta/packages/domains/system/src/resource-service.ts</Path> => T-07"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/oss-config/OssConfigPage.vue</Path> => T-07"
---

# Ticket T-07: 配对更新 OpenAPI 与 OSS 管理端

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/07-openapi-admin-oss-ui.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>`

## 1. 战略与来源

- **目标：** 将后端双类型、类型化下载和 POST 配置管理合同配对投影到 OpenAPI、system domain 与 admin 配置页。
- **可观察产出：** 管理员只看到 PRIVATE/PUBLIC_READ；公共配置明确提示生产 domainUrl 要求；下载可以处理 PUBLIC 的空 expiresAt；配置变更请求与后端 POST 路由一致。
- **来源：** `US-005`、`US-007`、`AC-003`、`AC-010`、`AC-012`、`AC-013`、`AC-014`、`AC-018`、`ADR-003`、`ADR-008`、`ADR-012`。
- **当前事实：** 页面显示 private/public/custom 且默认 `1`；domain 的 expiresAt 必填；配置 update/delete/status 仍发 PUT/DELETE；生成 OpenAPI 含旧合同。
- **Planning Depth 原因：** OpenAPI snapshot/generated 是共享 wire contract，必须由工具生成并与后端固定点配对。
- **执行偏差：** 集成后端在固定提交启动 `/v3/api-docs` 时发现 `OssStorageReadinessRegistry` 的双构造器缺少生产注入标记，完整 Spring 容器无法启动；启动后又确认公共响应的 `expiresAt=null` 未投影为 OpenAPI 可空类型。T-07 仅增加该启动前置的最小构造器标记、容器回归测试和现有 wire 字段的空值注解，再从修复后的固定提交抓取合同；不改变 OSS 行为或 T-03 readiness 所有权。

## 2. 决策状态

### 已锁定决策

- 从已集成后端固定 commit 获取 OpenAPI snapshot，记录 provenance，再运行离线 generator；禁止手改 generated/openapi.ts。
- domain `OssDownloadUrl` 增加 `accessType: 'PUBLIC' | 'PRIVATE'`，`expiresAt` 改为可空；安全 URL 校验对两类保持一致。
- 配置 accessPolicy 使用精确联合 `PRIVATE/PUBLIC_READ` 或与后端物理编码明确映射，不再使用 public/custom 标签。
- admin 页面将 `status` 文案保持“是否默认”，公共配置不能被设为默认；PUBLIC_READ 表单要求/说明 domainUrl 的生产门禁。
- 配置 update/delete/changeStatus 请求改为后端批准的 POST 路径；不迁移无关资源 CRUD。
- 管理 list/listByIds transport 不信任存量 URL，仍通过有权限 download-url 获取实际访问结果。
- 不增加迁移管理 UI；T-06 管理 API 只进入 OpenAPI 合同，本 change 不虚构门户 App。

### 已采用的低影响假设

- web-domain 添加相邻组件测试；Playwright 只覆盖配置类型、默认限制和 POST transport，不模拟真实 Provider。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| OpenAPI fetch/generate、domain 精确类型/服务、配置页面、组件/transport/E2E 测试 | 平台 HttpClient、安全 URL 校验、SystemWebRuntime、现有 manifest | 手改生成物、门户页面、迁移 UI、前端授权替代后端、全仓 CRUD 方法迁移 |

## 4. 要构建什么

管理员打开配置页面时只见两种明确访问类型和“是否默认”语义；选择公共类型时页面约束 domainUrl 并禁止设为默认。保存、删除、切换默认走新 POST 合同。文件下载响应可以安全处理公共无 expiry 与私有有 expiry，而不从 URL 查询参数猜测类型。

## 5. 实现契约

- **入口或接缝：** 后端 OpenAPI fixed point、api-contracts tooling、system domain resource service、OssConfigPage。
- **输入与输出：** 后端 schemas/routes -> generated types -> domain narrowing -> typed Vue form/下载行为。
- **公共接口变化：** 前端类型收敛访问策略、download accessType/nullable expiresAt 和配置 POST route。
- **不变量：** 生成物可重现；页面不定义重复 transport shape；权限指令不替代后端鉴权；客户端无 storageConfigKey/TTL。
- **状态或数据流：** fetch snapshot/provenance -> generate -> domain mapping/validation -> web-domain interaction -> App runtime request。
- **错误与失败行为：** 非法 accessType/URL、公共缺 domain、默认公共和后端拒绝均可见且不提交第二次副作用。
- **兼容要求：** URL/fileName 保持；现有下载消费者允许新增 accessType；私有 expiresAt 仍存在，公共为空。
- **安全与隐私要求：** 不显示/记录完整签名 URL或 Secret；页面可见性不放宽后端权限。

## 6. 执行路线

1. 在后端 result commit 上获取 OpenAPI snapshot，验证 provenance 并离线生成，先让 drift check 失败再恢复。
2. 收窄 domain 类型并更新 resource service POST/nullable/accessType 合同与失败测试。
3. 更新配置页面选项、默认约束、domainUrl 校验和精确反馈，不扩大到其他页面。
4. 添加 web-domain 组件测试和定向 Playwright，验证权限/错误/POST 方法。
5. 运行 OpenAPI check、domain/web-domain lint/typecheck/test、架构检查与 admin build。

## 7. 路径访问契约

- **预计修改点/可写范围：** frontmatter 所列 snapshot/generated、system domain/web-domain 和新定向 E2E。
- **只读上下文：** 三个后端 Controller、OpenAPI tooling 和现有 system-resources E2E。
- **共享路径：** OpenAPI、resource types/service 和 OssConfigPage 由 T-07 唯一修改；T-08 只运行其结果。
- **保留或不动：** tooling 源码、App 组合、其他 system CRUD、现有总体验收 spec。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| OpenAPI 可重现 | tooling check | `pnpm --filter @namewta/tooling-openapi openapi:check` | snapshot provenance 有效且生成物无 drift | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| domain transport | Vitest/typecheck | `pnpm --filter @namewta/domain-system test && pnpm --filter @namewta/domain-system typecheck` | POST、accessType、nullable expiry、unsafe URL 合同通过 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| 配置 UI 正常/失败 | web-domain tests | `pnpm --filter @namewta/web-domain-system test` | 只显示两类、默认公共被阻止、错误可恢复 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| 浏览器合同 | Playwright | Lead 运行 `pnpm exec playwright test e2e/oss-config-access-policy.spec.ts` | 页面/POST/权限/失败反馈成立 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| 架构/构建回归 | workspace gates | `pnpm architecture:check && pnpm typecheck && pnpm build:prod` | App -> web-domain -> domain -> platform 边界保持 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |

- **Workspace checks：** current workspace 或 source worktree 运行 OpenAPI、unit、lint/typecheck/architecture/build；浏览器 E2E 仅 Lead。
- **E2E disposition：** required：管理页面与真实 browser transport 方法是跨 Vue/runtime 边界行为。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；使用 admin dev/preview server 与受控 mock API。
- **Integration evidence：** 后端 source/result SHA、snapshot provenance、前端 implementation/source/candidate/result SHA 和父指针包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 后端 T-02/T-04/T-06 集成 -> fetch/generate -> domain -> UI；前后端配对发布。
- **兼容窗口：** 后端旧方法保留；旧前端 PUT/DELETE 与歧义标签必须在此 Ticket 收缩为零。
- **监控信号：** OpenAPI drift、前端 4xx/5xx、配置 validation 和下载 accessType；不采集完整 URL。
- **回滚或前向恢复：** 前端可回滚到不展示公共配置，但不得恢复 public/custom 或错误 HTTP 方法；合同变化优先前向修复。
- **不可逆操作与批准点：** 无部署或生产配置变更；OpenAPI fetch 仅本地合同写入。
- **收缩条件：** generated check 通过，前端扫描无 custom/public-write/旧配置 PUT/DELETE，所有下载类型已处理 nullable expiry。

## 10. 验收标准

- [x] frontmatter 所列 6 个 AC 的 transport、UI、权限和失败行为通过。
- [x] OpenAPI 生成物来自带 provenance 的后端固定点且未手改。
- [x] 页面只显示 PRIVATE/PUBLIC_READ，客户端无 storageConfigKey/TTL。
- [x] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>`。
- [x] 实际修改未超出 writable_paths，共享路径只由 T-07 修改。
- [x] 形成非空 implementation/source commit，经 direct-parent 或 candidate 验证并记录 result SHA。
- [x] 已批准启动前置偏差、Ticket、Map 与 Evidence 一致。
