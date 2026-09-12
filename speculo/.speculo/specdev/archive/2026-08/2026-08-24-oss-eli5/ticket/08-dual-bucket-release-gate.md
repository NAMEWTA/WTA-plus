---
schema_version: 3
artifact: ticket
change: 2026-08-24-oss-eli5
id: T-08
title: 完成双 Bucket E2E 与发布门禁
status: done
planning_depth: deep
planning_depth_reason: 最终验证跨 MySQL、MinIO、HTTP、浏览器和迁移恢复的安全发布合同，并形成生产人工批准点。
ready: true
risk: high
blocked_by: [T-04, T-05, T-06, T-07]
contract_ids: [AC-005, AC-006, AC-008, AC-009, AC-016, AC-019, AC-020, AC-022]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/MinioOssClientIntegrationTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/release/**</Path>"
  - "<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>"
  - "<Path>docs/oss-public-private-operations.md</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/MinioOssClientIntegrationTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/release/**</Path>"
  - "<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>"
  - "<Path>docs/oss-public-private-operations.md</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/MinioOssClientIntegrationTest.java</Path>"
  - "<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>"
  - "<Path>docs/oss-public-private-operations.md</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/MinioOssClientIntegrationTest.java</Path> => T-08"
  - "<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path> => T-08"
  - "<Path>docs/oss-public-private-operations.md</Path> => T-08"
---

# Ticket T-08: 完成双 Bucket E2E 与发布门禁

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/08-dual-bucket-release-gate.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>`

## 1. 战略与来源

- **目标：** 在完整父分支候选状态证明公共匿名读/禁止写、私有短时访问、策略上传、迁移恢复和现有 OSS 回归可共同成立。
- **可观察产出：** 可重复的双 Bucket 测试矩阵、管理端浏览器回归、发布/回滚 Runbook 和明确的生产启用人工 Gate。
- **来源：** `US-001` 至 `US-007`、`AC-005`、`AC-006`、`AC-008`、`AC-009`、`AC-016`、`AC-019`、`AC-020`、`AC-022`、`NFR-001` 至 `NFR-005`。
- **当前事实：** 已有单 Bucket MinIO 集成和 system-resources Playwright，但尚无公共/私有双桶、过期、迁移和 readiness 完整矩阵。
- **Planning Depth 原因：** 这是高风险发布收缩 Gate，低层单元证据不能替代真实 Provider/DB/browser 组合证据。

## 2. 决策状态

### 已锁定决策

- 双 Bucket fixture 明确准备 PRIVATE 与 PUBLIC_READ，二者都拒绝匿名写；测试不由应用修改 Policy。
- E2E 从命名 policy 上传开始，验证 Ticket/service/Bucket、resolveAccessUrl、匿名 HTTP、过期和管理端行为。
- 未授权业务路径必须证明不调用签名 Service；知道 ossId 或私有原始 URL 不能得到对象/配置信息。
- 迁移 E2E 必须覆盖成功、每阶段失败、重试/回滚、ossId/ref 不变和源清理未提前发生。
- 外部 MinIO/MySQL/浏览器环境缺失时 Ticket 不得 Done；必须记录为 infrastructure blocker，不能用 mock 宣称 E2E 通过。
- Runbook 明确 Provider/domain 先行、应用默认关闭、dry-run、批量迁移、监控、回滚和 cleanup 独立批准。

### 已采用的低影响假设

- MinIO 使用短测试 TTL 和隔离 Bucket；生产 Provider 差异仍需部署方执行同一 HTTP 能力矩阵。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 双桶 MinIO/MySQL/HTTP E2E、Playwright 总回归、静态收缩扫描、运维 Runbook | T-01 至 T-07 实现和定向测试、现有 integration 属性门控 | 修复核心实现、生产部署、生产迁移、Policy/CDN/DNS 自动化 |

## 4. 要构建什么

Lead 在完整候选状态准备隔离 MySQL、公共/私有 MinIO Bucket 和 admin server。测试分别上传、解析、匿名访问、等待私有签名过期、执行迁移成功/故障恢复，并通过浏览器确认管理列表不携带 URL、下载与配置 UI 正确。所有证据和生产操作顺序汇总到 Runbook；任何核心失败退回所属 Ticket，不在 Gate 中改实现路径。

## 5. 实现契约

- **入口或接缝：** MinIO integration、MySQL fixture、匿名 HTTP client、后端管理/上传 API、Playwright、静态扫描和 Runbook。
- **输入与输出：** 完整候选 + 隔离环境 -> pass/fail 矩阵、HTTP 状态、DB/Bucket 查询和发布决策。
- **公共接口变化：** 无新产品合同；只增加测试与运维文档。
- **不变量：** 公共只读、私有拒绝原始读取、客户端不选存储/TTL、ossId/ref 稳定、管理列表 URL=null。
- **状态或数据流：** prepare infra -> readiness -> upload -> resolve/access -> migration/fault -> UI regression -> release decision。
- **错误与失败行为：** 产品失败归类为本 change 回归；环境缺失/权限失败单独记录并阻止 Gate，不跳过关键断言。
- **兼容要求：** SINGLE/MULTIPART、TEMP、引用、删除、通知附件、FileUpload/ImageUpload/Editor 和管理权限回归保持。
- **安全与隐私要求：** Evidence 清洗 Secret 和完整签名查询；测试 Bucket/DB 与生产隔离，结束后清理需另行授权。

## 6. 执行路线

1. 扩展现有 MinIO 与 Playwright 验收，使双类型、nullable expiry、POST 配置和匿名矩阵先失败。
2. 建立完整候选的隔离 MySQL/双 Bucket/canary/browser 环境，记录基线和配置来源。
3. 执行上传、访问、过期、未授权和 readiness 全链 E2E。
4. 执行迁移成功及分阶段故障/重试/回滚，查询 DB/ref/Bucket 不变量。
5. 运行后端/前端全量适用门禁和禁止能力扫描。
6. 完成 Runbook、残余 Provider 差异和生产人工批准点，给出 release/no-release 结论。

## 7. 路径访问契约

- **预计修改点/可写范围：** 现有 MinIO integration、system-resources E2E、新 release tests 与 Runbook。
- **只读上下文：** 所有产品实现、SQL 和前端 domain/web-domain。
- **共享路径：** 最终 MinIO、Playwright 总体验收和 Runbook 只由 T-08 修改。
- **保留或不动：** T-01 至 T-07 产品代码；发现失败必须按 deviation/所属 Ticket 修订所有权。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 双桶访问/过期/写拒绝 | MinIO + anonymous HTTP | 运行 `MinioOssClientIntegrationTest` 和 release suite | public GET/HEAD 成功、写拒绝；private 原始拒绝、签名过期 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| 上传与迁移 | MySQL + MinIO | 运行完整 OSS release integration | 路由正确；迁移/恢复保持 ossId/ref/源对象安全 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| 管理端回归 | Playwright | `pnpm exec playwright test e2e/system-resources.spec.ts e2e/oss-config-access-policy.spec.ts` | 配置、下载、权限和错误反馈通过 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| 后端回归 | Maven gates | `./mvnw -pl ruoyi-admin -am -Dmaven.test.skip=false test` 与 package | 既有 OSS/system 组装通过，外部 skipped 如实报告 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| 前端回归 | workspace gates | `pnpm architecture:check && pnpm lint && pnpm typecheck && pnpm test && pnpm build:prod` | 架构、类型、测试和生产构建通过 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| 禁止能力/发布文档 | scan + review | 扫描匿名 lookup、public-write/custom、客户端路由/TTL、URL 持久化 | 零调用且 Runbook 含发布/恢复/批准点 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |

- **Workspace checks：** source/current workspace 只运行静态与非 E2E门禁；最终组合验证必须由 Lead 在 current workspace 或 parent-candidate 执行。
- **E2E disposition：** required：这是本 change 的最终真实跨边界 release Gate。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；隔离 MySQL、双 Bucket MinIO、backend、admin browser。
- **Integration evidence：** 所有上游 result SHA、最终 candidate/result SHA、环境清单、命令/退出码、HTTP/DB/Bucket 证据和父分支重读。

## 9. 发布、迁移与恢复

- **迁移顺序：** Provider/域名/Policy 验证 -> SQL 备份/upgrade -> 默认关闭代码 -> readiness -> dry-run -> 小批迁移 -> 观察 -> 扩大 -> cleanup 另批批准。
- **兼容窗口：** 全部历史对象保持 PRIVATE；公共只对新策略和审核迁移项启用；旧 Service 方法持续兼容。
- **监控信号：** readiness、匿名能力、URL/策略失败、上传路由、迁移阶段/重试/回滚和 Provider 5xx/延迟。
- **回滚或前向恢复：** 停止公共策略与新迁移、将配置置 NOT_SERVING、恢复旧 service；保留 additive schema 和源对象前向修复。
- **不可逆操作与批准点：** 生产 SQL、流量启用、正式迁移、rollback、源 cleanup、远程/部署均需独立人工批准，本 Ticket 不执行。
- **收缩条件：** 全部 AC/NFR Gate 通过、禁止调用点为零、无 active/failed-unhandled 迁移、Runbook 审核完成。

## 10. 验收标准

- [x] frontmatter 所列 8 个 AC 和其余 AC 的回归投影在完整候选上通过。
- [x] 真实 MySQL/MinIO/browser 环境证据完整；未运行项不被描述为通过。
- [x] Runbook 明确部署顺序、监控、回滚、残余风险和全部人工批准点。
- [x] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>`。
- [x] 实际修改未超出 writable_paths，共享路径只由 T-08 修改。
- [x] 形成非空 implementation/source commit，经 candidate 验证并记录 result SHA。
- [x] 未发生未批准偏差，Ticket、Map 与 Evidence 一致。
